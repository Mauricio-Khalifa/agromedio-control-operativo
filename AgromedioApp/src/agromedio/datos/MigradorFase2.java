package agromedio.datos;

import java.sql.Connection;
import java.sql.Statement;
import java.nio.file.Files;
import java.nio.file.Paths;

public class MigradorFase2 {
    public static void main(String[] args) {
        String sqlPath = "AgromedioApp/src/sql/migracion_fase2.sql";
        try (Connection conn = ConexionBD.obtener()) {
            String sql = new String(Files.readAllBytes(Paths.get(sqlPath)));
            Statement stmt = conn.createStatement();
            // Ejecutar como script completo (SQLite soporta múltiples comandos si se manejan correctamente)
            for (String query : sql.split(";")) {
                if (!query.trim().isEmpty()) {
                    stmt.execute(query);
                }
            }
            System.out.println("Migración aplicada exitosamente.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
