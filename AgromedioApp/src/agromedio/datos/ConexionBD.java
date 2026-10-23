package agromedio.datos;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Acceso unico a la base de datos SQLite.
 * - La ruta puede definirse con -Dagromedio.db=ruta\archivo.db
 * - En el primer arranque crea y siembra la BD a partir de los guiones
 *   /sql/schema.sql y /sql/seed.sql incluidos en el classpath.
 */
public final class ConexionBD {

    private static final String RUTA_DEFECTO = "agromedio.db";
    private static String ruta;

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

    /** Abre una conexion con llaves foraneas activas (RNF02 - integridad). */
    public static Connection obtener() throws SQLException {
        Connection c = DriverManager.getConnection("jdbc:sqlite:" + rutaBD());
        try (Statement s = c.createStatement()) {
            s.execute("PRAGMA foreign_keys = ON");
        }
        return c;
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
