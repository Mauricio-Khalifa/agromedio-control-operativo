package agromedio.pruebas;

import agromedio.datos.ConexionBD;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Prueba automatizada del esquema (Materia: Motores de Bases de Datos).
 * Valida estructura, integridad, disparador de faltantes y vistas.
 * Ejecutar: java -cp build/classes agromedio.pruebas.PruebaEsquema
 */
public class PruebaEsquema {

    private static int total = 0;
    private static int fallos = 0;

    public static void main(String[] args) throws Exception {
        Path bd = Paths.get("build", "prueba_esquema.db");
        Files.createDirectories(bd.toAbsolutePath().getParent());
        Files.deleteIfExists(bd);
        ConexionBD.fijarRuta(bd.toString());
        ConexionBD.inicializar();

        try (Connection c = ConexionBD.obtener(); Statement st = c.createStatement()) {
            verificar("15 tablas creadas", () -> contar(st,
                    "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'") == 15);

            verificar("6 tablas del ciclo + catalogos con datos", () ->
                    contar(st, "SELECT COUNT(*) FROM detalle_plantilla") == 13);

            // RF06: el faltante lo calcula el TRIGGER de la BD
            verificar("trigger calcula faltante de leche = 7", () -> {
                double f = valor(st, "SELECT cantidad_faltante FROM detalle_recepcion WHERE id_detalle_recepcion = 1");
                return f == 7.0;
            });

            // Integridad referencial activa
            verificar("FK bloquea registro huerfano", () -> {
                try (Statement s2 = c.createStatement()) {
                    s2.execute("INSERT INTO detalle_plantilla (id_plantilla, id_producto, cantidad_solicitada) VALUES (999, 1, 5)");
                    return false;
                } catch (SQLException e) {
                    return e.getMessage().toLowerCase().contains("foreign key");
                }
            });

            // CHECK de estados
            verificar("CHECK bloquea estado invalido", () -> {
                try (Statement s2 = c.createStatement()) {
                    s2.execute("UPDATE plantilla_operativa SET estado='CUALQUIERA' WHERE id_plantilla = 1");
                    return false;
                } catch (SQLException e) {
                    return e.getMessage().toLowerCase().contains("check");
                }
            });

            // Vista de faltantes
            verificar("v_faltantes_recepcion muestra 1 faltante (7 L)", () -> {
                if (contar(st, "SELECT COUNT(*) FROM v_faltantes_recepcion") != 1) return false;
                return valor(st, "SELECT cantidad_faltante FROM v_faltantes_recepcion") == 7.0;
            });

            // Vista de saldo integral
            verificar("v_saldo_plantilla: solicitado 77 - despachado 70 = 7", () -> {
                double sol = valor(st, "SELECT solicitado FROM v_saldo_plantilla WHERE id_plantilla=1 AND id_producto=1");
                double des = valor(st, "SELECT despachado FROM v_saldo_plantilla WHERE id_plantilla=1 AND id_producto=1");
                double por = valor(st, "SELECT por_despachar FROM v_saldo_plantilla WHERE id_plantilla=1 AND id_producto=1");
                return sol == 77.0 && des == 70.0 && por == 7.0;
            });

            // Vista de avance de despacho (RF10)
            verificar("v_avance_despacho: 5 items, 100%", () -> {
                double items = valor(st, "SELECT items FROM v_avance_despacho WHERE id_despacho=1");
                double pct = valor(st, "SELECT porcentaje FROM v_avance_despacho WHERE id_despacho=1");
                return items == 5.0 && pct == 100.0;
            });

            // Consolidacion de demanda (RF03) para el dia de manana
            verificar("consolidado RF03: arroz = 200 lb en 2 mercados", () -> {
                double total = valor(st, "SELECT SUM(dp.cantidad_solicitada) FROM detalle_plantilla dp "
                        + "JOIN plantilla_operativa p ON p.id_plantilla = dp.id_plantilla "
                        + "JOIN producto pr ON pr.id_producto = dp.id_producto "
                        + "WHERE pr.nombre = 'Arroz blanco' AND p.fecha_programada = date('now','localtime','+1 day') "
                        + "AND p.estado IN ('CARGADA','EN_COMPRA','RECIBIENDO')");
                double mercados = valor(st, "SELECT COUNT(DISTINCT dp.id_plantilla) FROM detalle_plantilla dp "
                        + "JOIN plantilla_operativa p ON p.id_plantilla = dp.id_plantilla "
                        + "JOIN producto pr ON pr.id_producto = dp.id_producto "
                        + "WHERE pr.nombre = 'Arroz blanco' AND p.fecha_programada = date('now','localtime','+1 day') "
                        + "AND p.estado IN ('CARGADA','EN_COMPRA','RECIBIENDO')");
                return total == 200.0 && mercados == 2.0;
            });

            // Conversion de unidades (unidad compra -> despacho)
            verificar("factor_conversion: 3 bultos de papa = 150 lb", () -> {
                double libras = valor(st, "SELECT ROUND(3 * factor_conversion, 2) FROM producto WHERE id_producto = 3");
                return libras == 150.0;
            });
        }

        System.out.println();
        System.out.println("RESULTADO: " + (total - fallos) + "/" + total + " pruebas correctas");
        if (fallos > 0) {
            System.exit(1);
        }
    }

    private static int contar(Statement st, String sql) throws SQLException {
        try (ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : -1;
        }
    }

    private static double valor(Statement st, String sql) throws SQLException {
        try (ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getDouble(1) : Double.NaN;
        }
    }

    private interface Prueba { boolean ok() throws Exception; }

    private static void verificar(String nombre, Prueba p) {
        total++;
        try {
            if (p.ok()) {
                System.out.println("[OK]   " + nombre);
            } else {
                fallos++;
                System.out.println("[FAIL] " + nombre);
            }
        } catch (Exception e) {
            fallos++;
            System.out.println("[FAIL] " + nombre + " -> " + e.getMessage());
        }
    }
}
