package agromedio.datos;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.function.Supplier;

/**
 * Acceso unico a la base de datos SQLite.
 * - La ruta puede definirse con -Dagromedio.db=ruta\archivo.db
 * - En el primer arranque crea y siembra la BD a partir de los guiones
 *   /sql/schema.sql y /sql/seed.sql incluidos en el classpath.
 */
public final class ConexionBD {

    private static final String RUTA_DEFECTO = "agromedio.db";
    private static String ruta;
    /** Conexion activa de la transaccion en curso (una por hilo). */
    private static final ThreadLocal<Connection> TRANSACCION = new ThreadLocal<>();

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError("No se encontro el driver sqlite-jdbc: " + e.getMessage());
        }
    }

    private ConexionBD() {}

    public static String rutaBD() {
        if (ruta == null) {
            ruta = System.getProperty("agromedio.db", RUTA_DEFECTO);
        }
        return ruta;
    }

    public static void fijarRuta(String nuevaRuta) {
        ruta = nuevaRuta;
    }

    /**
     * Abre una conexion con llaves foraneas activas (RNF02 - integridad).
     * Si hay una transaccion en curso, devuelve un envoltorio de esa misma
     * conexion cuyo {@code close()} no la cierra (los DAO la usan con
     * try-with-resources y no deben romper la transaccion).
     */
    public static Connection obtener() throws SQLException {
        Connection activa = TRANSACCION.get();
        if (activa != null) {
            return envolverSinCierre(activa);
        }
        return conexionCruda();
    }

    private static Connection conexionCruda() throws SQLException {
        Connection c = DriverManager.getConnection("jdbc:sqlite:" + rutaBD());
        try (Statement s = c.createStatement()) {
            s.execute("PRAGMA foreign_keys = ON");
        }
        return c;
    }

    /**
     * Ejecuta el trabajo dentro de una unica transaccion: si algo falla, se
     * revierte todo (atomicidad de operaciones que escriben en varias tablas).
     * Las llamadas anidadas reutilizan la transaccion en curso.
     */
    public static <T> T enTransaccion(Supplier<T> trabajo) {
        if (trabajo == null) {
            throw new IllegalArgumentException("El trabajo es obligatorio");
        }
        if (TRANSACCION.get() != null) {
            return trabajo.get();
        }
        Connection c = null;
        try {
            c = conexionCruda();
            c.setAutoCommit(false);
            TRANSACCION.set(c);
            T resultado = trabajo.get();
            c.commit();
            return resultado;
        } catch (RuntimeException e) {
            revertir(c);
            throw e;
        } catch (Exception e) {
            revertir(c);
            throw new RuntimeException(e.getMessage() == null ? e.toString() : e.getMessage(), e);
        } finally {
            TRANSACCION.remove();
            cerrarTransaccion(c);
        }
    }

    public static void enTransaccion(Runnable trabajo) {
        if (trabajo == null) {
            throw new IllegalArgumentException("El trabajo es obligatorio");
        }
        enTransaccion(() -> {
            trabajo.run();
            return null;
        });
    }

    private static void revertir(Connection c) {
        if (c == null) {
            return;
        }
        try {
            c.rollback();
        } catch (SQLException ignore) {
            // la transaccion ya no es recuperable; se propaga el error original
        }
    }

    private static void cerrarTransaccion(Connection c) {
        if (c == null) {
            return;
        }
        try {
            c.setAutoCommit(true);
        } catch (SQLException ignore) {
            // el cierre se intenta de todos modos
        }
        try {
            c.close();
        } catch (SQLException ignore) {
            // nada que hacer
        }
    }

    private static Connection envolverSinCierre(Connection real) {
        return (Connection) Proxy.newProxyInstance(
                ConexionBD.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, metodo, argumentos) -> {
                    if ("close".equals(metodo.getName())) {
                        return null;
                    }
                    try {
                        return metodo.invoke(real, argumentos);
                    } catch (InvocationTargetException e) {
                        throw e.getCause();
                    }
                });
    }

    /** Crea la BD con datos de demostracion si no existe; si existe, aplica las migraciones G03/G04/G05/G07. */
    public static synchronized void inicializar() throws SQLException {
        Path p = Paths.get(rutaBD());
        boolean existeConDatos = false;
        if (Files.exists(p)) {
            try {
                existeConDatos = Files.size(p) > 0;
            } catch (Exception ignore) {
                // si no se puede leer el tamano, se intenta recrear
            }
        }
        if (existeConDatos) {
            migrarEsquemaG03();
            migrarEsquemaG04G07();
            migrarEsquemaG05();
            migrarVistas();
            return;
        }
        Path padre = p.toAbsolutePath().getParent();
        if (padre != null) {
            try {
                Files.createDirectories(padre);
            } catch (Exception e) {
                throw new SQLException("No se pudo crear el directorio de la BD: " + e.getMessage());
            }
        }
        ejecutarGuion("/sql/schema.sql");
        ejecutarGuion("/sql/seed.sql");
    }

    /**
     * Migracion idempotente para bases creadas antes de G03: agrega
     * detalle_despacho.faltante_aceptado y actualiza la vista de avance.
     * Los despachos historicos parciales que ya tenian novedad se marcan
     * como aceptados para conservar la coherencia del reporte.
     */
    private static void migrarEsquemaG03() throws SQLException {
        boolean columnaNueva = false;
        boolean vistaDesactualizada = false;
        try (Connection c = obtener(); Statement st = c.createStatement()) {
            boolean existeTabla = false;
            try (ResultSet rs = st.executeQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name='detalle_despacho'")) {
                existeTabla = rs.next();
            }
            if (!existeTabla) {
                return;
            }
            columnaNueva = true;
            try (ResultSet rs = st.executeQuery("PRAGMA table_info(detalle_despacho)")) {
                while (rs.next()) {
                    if ("faltante_aceptado".equalsIgnoreCase(rs.getString("name"))) {
                        columnaNueva = false;
                        break;
                    }
                }
            }
            if (columnaNueva) {
                st.execute("ALTER TABLE detalle_despacho ADD COLUMN faltante_aceptado "
                        + "INTEGER NOT NULL DEFAULT 0 CHECK (faltante_aceptado IN (0,1))");
            }
            try (ResultSet rs = st.executeQuery(
                    "SELECT sql FROM sqlite_master WHERE type='view' AND name='v_avance_despacho'")) {
                if (!rs.next() || rs.getString(1) == null
                        || !rs.getString(1).toLowerCase().contains("con_novedad")) {
                    vistaDesactualizada = true;
                }
            }
        }
        if (vistaDesactualizada) {
            ejecutarGuion("/sql/migracion_g03.sql");
        }
        if (columnaNueva) {
            try (Connection c = obtener(); Statement st = c.createStatement()) {
                st.executeUpdate("UPDATE detalle_despacho SET faltante_aceptado = 1 "
                        + "WHERE cantidad_despachada > 0 AND EXISTS ("
                        + "SELECT 1 FROM despacho d "
                        + "JOIN detalle_plantilla dp ON dp.id_plantilla = d.id_plantilla "
                        + "WHERE d.id_despacho = detalle_despacho.id_despacho "
                        + "AND dp.id_producto = detalle_despacho.id_producto "
                        + "AND d.estado = 'DESPACHADA' "
                        + "AND detalle_despacho.cantidad_despachada < dp.cantidad_solicitada "
                        + "AND d.novedades IS NOT NULL AND TRIM(d.novedades) <> '')");
            }
        }
    }

    /**
     * Migracion idempotente para bases anteriores a G04/G07. SQLite no permite
     * alterar CHECK ni ON DELETE, por lo que se instalan triggers equivalentes.
     */
    private static void migrarEsquemaG04G07() throws SQLException {
        ejecutarGuion("/sql/migracion_g04_g07.sql");
    }

    /**
     * Migracion idempotente para bases anteriores a G05: crea carga_plantilla,
     * agrega plantilla_operativa.id_carga y su indice.
     */
    private static void migrarEsquemaG05() throws SQLException {
        boolean existePlantilla = false;
        try (Connection c = obtener(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT name FROM sqlite_master WHERE type='table' AND name='plantilla_operativa'")) {
            existePlantilla = rs.next();
        }
        if (!existePlantilla) {
            return;
        }
        ejecutarGuion("/sql/migracion_g05.sql");

        boolean columnaNueva = false;
        try (Connection c = obtener(); Statement st = c.createStatement()) {
            try (ResultSet rs = st.executeQuery("PRAGMA table_info(plantilla_operativa)")) {
                boolean existeColumna = false;
                while (rs.next()) {
                    if ("id_carga".equalsIgnoreCase(rs.getString("name"))) {
                        existeColumna = true;
                        break;
                    }
                }
                columnaNueva = !existeColumna;
            }
            if (columnaNueva) {
                st.execute("ALTER TABLE plantilla_operativa ADD COLUMN id_carga INTEGER "
                        + "REFERENCES carga_plantilla(id_carga)");
            }
            st.execute("CREATE INDEX IF NOT EXISTS idx_plantilla_carga "
                    + "ON plantilla_operativa (id_carga)");
        }
    }

    /**
     * Migracion idempotente de vistas: recrea v_saldo_plantilla para incluir el
     * filtro de compras ANULADA (consistencia con el resto de consultas).
     */
    private static void migrarVistas() {
        String sql = "DROP VIEW IF EXISTS v_saldo_plantilla; "
                + "CREATE VIEW v_saldo_plantilla AS "
                + "SELECT pl.id_plantilla, pl.nombre_mercado, pl.fecha_programada, pl.estado, "
                + "det.id_producto, pr.nombre AS producto, umd.abreviatura AS unidad_despacho, "
                + "pr.factor_conversion, det.cantidad_solicitada AS solicitado, "
                + "COALESCE(al.alistado, 0) AS alistado, COALESCE(c.comprado, 0) AS comprado, "
                + "COALESCE(r.recibido, 0) AS recibido, COALESCE(d.despachado, 0) AS despachado, "
                + "ROUND(det.cantidad_solicitada - COALESCE(d.despachado, 0), 2) AS por_despachar "
                + "FROM detalle_plantilla det "
                + "JOIN plantilla_operativa pl ON pl.id_plantilla = det.id_plantilla "
                + "JOIN producto pr ON pr.id_producto = det.id_producto "
                + "LEFT JOIN unidad_medida umd ON umd.id_unidad = pr.id_unidad_despacho "
                + "LEFT JOIN (SELECT a.id_plantilla, da.id_producto, SUM(da.cantidad_alistada) AS alistado "
                + "  FROM alistamiento a JOIN detalle_alistamiento da ON da.id_alistamiento = a.id_alistamiento "
                + "  GROUP BY a.id_plantilla, da.id_producto) al "
                + "  ON al.id_plantilla = pl.id_plantilla AND al.id_producto = det.id_producto "
                + "LEFT JOIN (SELECT c.id_plantilla, dc.id_producto, "
                + "  ROUND(SUM(dc.cantidad_comprada * pr2.factor_conversion), 2) AS comprado "
                + "  FROM compra c JOIN detalle_compra dc ON dc.id_compra = c.id_compra "
                + "  JOIN producto pr2 ON pr2.id_producto = dc.id_producto "
                + "  WHERE c.estado <> 'ANULADA' "
                + "  GROUP BY c.id_plantilla, dc.id_producto) c "
                + "  ON c.id_plantilla = pl.id_plantilla AND c.id_producto = det.id_producto "
                + "LEFT JOIN (SELECT c.id_plantilla, dc.id_producto, "
                + "  ROUND(SUM(dr.cantidad_recibida * pr2.factor_conversion), 2) AS recibido "
                + "  FROM compra c JOIN detalle_compra dc ON dc.id_compra = c.id_compra "
                + "  JOIN detalle_recepcion dr ON dr.id_detalle_compra = dc.id_detalle_compra "
                + "  JOIN producto pr2 ON pr2.id_producto = dc.id_producto "
                + "  WHERE c.estado <> 'ANULADA' "
                + "  GROUP BY c.id_plantilla, dc.id_producto) r "
                + "  ON r.id_plantilla = pl.id_plantilla AND r.id_producto = det.id_producto "
                + "LEFT JOIN (SELECT de.id_plantilla, dd.id_producto, SUM(dd.cantidad_despachada) AS despachado "
                + "  FROM despacho de JOIN detalle_despacho dd ON dd.id_despacho = de.id_despacho "
                + "  GROUP BY de.id_plantilla, dd.id_producto) d "
                + "  ON d.id_plantilla = pl.id_plantilla AND d.id_producto = det.id_producto;";
        try (Connection c = obtener(); Statement st = c.createStatement()) {
            for (String sentencia : sql.split(";")) {
                if (!sentencia.isBlank()) {
                    st.execute(sentencia.trim());
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo migrar la vista v_saldo_plantilla: " + e.getMessage(), e);
        }
    }

    /** Ejecuta un guion SQL multipaso ubicado en el classpath. */
    public static void ejecutarGuion(String recurso) throws SQLException {
        String contenido;
        try (InputStream in = ConexionBD.class.getResourceAsStream(recurso)) {
            if (in == null) {
                throw new SQLException("Recurso SQL no encontrado: " + recurso);
            }
            contenido = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (SQLException e) {
            throw e;
        } catch (Exception e) {
            throw new SQLException("No se pudo leer " + recurso + ": " + e.getMessage());
        }
        try (Connection c = obtener(); Statement st = c.createStatement()) {
            for (String sentencia : partirGuion(contenido)) {
                if (!sentencia.isBlank()) {
                    st.execute(sentencia);
                }
            }
        }
    }

    /**
     * Divide un guion SQL multipaso en sentencias individuales.
     * Respeta comentarios (--), textos entre comillas simples y los
     * bloques BEGIN...END de los triggers (cuyo interior usa ';' tambien).
     */
    static java.util.List<String> partirGuion(String sql) {
        java.util.List<String> sentencias = new java.util.ArrayList<>();
        StringBuilder actual = new StringBuilder();
        boolean enComentario = false;
        boolean enTexto = false;
        int i = 0;
        while (i < sql.length()) {
            char c = sql.charAt(i);
            if (enComentario) {
                actual.append(c);
                if (c == '\n') {
                    enComentario = false;
                }
                i++;
                continue;
            }
            if (enTexto) {
                actual.append(c);
                if (c == '\'') {
                    if (i + 1 < sql.length() && sql.charAt(i + 1) == '\'') {
                        actual.append('\'');
                        i += 2;
                        continue;
                    }
                    enTexto = false;
                }
                i++;
                continue;
            }
            if (c == '-' && i + 1 < sql.length() && sql.charAt(i + 1) == '-') {
                enComentario = true;
                actual.append(c).append('-');
                i += 2;
                continue;
            }
            if (c == '\'') {
                enTexto = true;
                actual.append(c);
                i++;
                continue;
            }
            if (c == ';') {
                String hastaAhora = actual.toString();
                boolean esTrigger = hastaAhora.toUpperCase().contains("CREATE TRIGGER");
                String sinEspacios = hastaAhora.trim();
                boolean terminaEnEnd = sinEspacios.toUpperCase().endsWith("END");
                if (esTrigger && !terminaEnEnd) {
                    actual.append(c);
                } else {
                    sentencias.add(actual.toString());
                    actual.setLength(0);
                }
                i++;
                continue;
            }
            actual.append(c);
            i++;
        }
        sentencias.add(actual.toString());
        return sentencias;
    }
}
