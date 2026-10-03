package agromedio.datos;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
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

    /** Crea la BD con datos de demostracion solo si aun no existe. */
    public static synchronized void inicializar() throws SQLException {
        Path p = Paths.get(rutaBD());
        if (Files.exists(p)) {
            try {
                if (Files.size(p) > 0) {
                    return;
                }
            } catch (Exception ignore) {
                // continua y recrea
            }
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
