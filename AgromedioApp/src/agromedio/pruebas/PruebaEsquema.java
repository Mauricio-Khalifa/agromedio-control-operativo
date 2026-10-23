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
            verificar("16 tablas creadas", () -> contar(st,
                    "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'") == 16);

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

            // RN-09 / RN-10: el seed conserva el faltante de leche como aceptado.
            verificar("RN-10: despacho seed marca faltante aceptado y con novedad", () -> {
                double conNovedad = valor(st, "SELECT con_novedad FROM v_avance_despacho WHERE id_despacho=1");
                double aceptado = valor(st, "SELECT faltante_aceptado FROM detalle_despacho "
                        + "WHERE id_despacho=1 AND id_producto=1");
                return conNovedad == 1.0 && aceptado == 1.0;
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

            // RN-06: el faltante se acumula entre entregas parciales.
            // Se prueba dentro de una transaccion y se revierte para no alterar el seed.
            verificar("RN-06: segunda entrega parcial acumula faltante (75/77 -> 2)", () -> {
                c.setAutoCommit(false);
                try (Statement s2 = c.createStatement()) {
                    s2.execute("INSERT INTO recepcion (id_compra, fecha_recepcion, observaciones) "
                            + "VALUES (1, date('now','localtime'), 'Prueba acumulado')");
                    s2.execute("INSERT INTO detalle_recepcion (id_recepcion, id_detalle_compra, "
                            + "cantidad_recibida, cantidad_faltante) "
                            + "VALUES ((SELECT MAX(id_recepcion) FROM recepcion), 1, 5, 0)");
                    int filas = contar(s2, "SELECT COUNT(*) FROM v_faltantes_recepcion WHERE id_detalle_compra = 1");
                    double recibida = valor(s2, "SELECT cantidad_recibida FROM v_faltantes_recepcion WHERE id_detalle_compra = 1");
                    double faltante = valor(s2, "SELECT cantidad_faltante FROM v_faltantes_recepcion WHERE id_detalle_compra = 1");
                    return filas == 1 && recibida == 75.0 && faltante == 2.0;
                } finally {
                    c.rollback();
                    c.setAutoCommit(true);
                }
            });

            verificar("RN-06: segunda entrega completa 77/77 -> faltante 0", () -> {
                c.setAutoCommit(false);
                try (Statement s2 = c.createStatement()) {
                    s2.execute("INSERT INTO recepcion (id_compra, fecha_recepcion, observaciones) "
                            + "VALUES (1, date('now','localtime'), 'Prueba cierre')");
                    s2.execute("INSERT INTO detalle_recepcion (id_recepcion, id_detalle_compra, "
                            + "cantidad_recibida, cantidad_faltante) "
                            + "VALUES ((SELECT MAX(id_recepcion) FROM recepcion), 1, 7, 0)");
                    int filas = contar(s2, "SELECT COUNT(*) FROM v_faltantes_recepcion WHERE id_detalle_compra = 1");
                    double faltante = valor(s2, "SELECT MAX(cantidad_faltante) FROM detalle_recepcion WHERE id_detalle_compra = 1");
                    return filas == 0 && faltante == 0.0;
                } finally {
                    c.rollback();
                    c.setAutoCommit(true);
                }
            });

            verificar("D02: CHECK bloquea detalle de recepcion con cantidad 0", () -> {
                try (Statement s2 = c.createStatement()) {
                    s2.execute("INSERT INTO detalle_recepcion (id_recepcion, id_detalle_compra, "
                            + "cantidad_recibida, cantidad_faltante) VALUES (1, 2, 0, 0)");
                    return false;
                } catch (SQLException e) {
                    return e.getMessage().toLowerCase().contains("check");
                }
            });

            // RN-12: una plantilla con historico no se elimina.
            verificar("RN-12: no se elimina una plantilla con historico", () -> {
                try (Statement s2 = c.createStatement()) {
                    s2.execute("DELETE FROM plantilla_operativa WHERE id_plantilla = 1");
                    return false;
                } catch (SQLException e) {
                    String m = e.getMessage().toLowerCase();
                    return m.contains("rn-12") || m.contains("foreign key") || m.contains("constraint");
                }
            });

            // RN-12: una plantilla CARGADA sin historico si se elimina con su detalle.
            verificar("RN-12: plantilla CARGADA sin historico se elimina", () -> {
                try (Statement s2 = c.createStatement()) {
                    s2.execute("INSERT INTO plantilla_operativa "
                            + "(id_cliente, nombre_mercado, fecha_programada, estado) "
                            + "VALUES (1, 'MERCADO TEMPORAL G04', date('now','localtime'), 'CARGADA')");
                    int id = (int) valor(s2, "SELECT id_plantilla FROM plantilla_operativa "
                            + "WHERE nombre_mercado = 'MERCADO TEMPORAL G04'");
                    s2.execute("INSERT INTO detalle_plantilla "
                            + "(id_plantilla, id_producto, cantidad_solicitada) VALUES (" + id + ", 1, 5)");
                    s2.execute("DELETE FROM plantilla_operativa WHERE id_plantilla = " + id);
                    int plantilla = contar(s2, "SELECT COUNT(*) FROM plantilla_operativa WHERE id_plantilla = " + id);
                    int detalle = contar(s2, "SELECT COUNT(*) FROM detalle_plantilla WHERE id_plantilla = " + id);
                    return plantilla == 0 && detalle == 0;
                }
            });

            // RN-11 / D02: no se puede completar un producto con cantidad 0.
            verificar("RN-11: CHECK bloquea alistamiento completado en 0", () -> {
                try (Statement s2 = c.createStatement()) {
                    s2.execute("INSERT INTO detalle_alistamiento "
                            + "(id_alistamiento, id_producto, cantidad_alistada, completado) "
                            + "VALUES ((SELECT MIN(id_alistamiento) FROM alistamiento), 1, 0, 1)");
                    return false;
                } catch (SQLException e) {
                    String m = e.getMessage().toLowerCase();
                    return m.contains("rn-11") || m.contains("check");
                }
            });

            // RN-11 / D02: no se puede verificar un producto con cantidad 0.
            verificar("RN-11: CHECK bloquea despacho verificado en 0", () -> {
                try (Statement s2 = c.createStatement()) {
                    s2.execute("INSERT INTO detalle_despacho "
                            + "(id_despacho, id_producto, cantidad_despachada, verificado, faltante_aceptado) "
                            + "VALUES ((SELECT MIN(id_despacho) FROM despacho), 1, 0, 1, 0)");
                    return false;
                } catch (SQLException e) {
                    String m = e.getMessage().toLowerCase();
                    return m.contains("rn-11") || m.contains("check");
                }
            });

            // RN-11: la compra exige cantidad mayor que cero.
            verificar("RN-11: CHECK bloquea compra con cantidad 0", () -> {
                try (Statement s2 = c.createStatement()) {
                    s2.execute("INSERT INTO detalle_compra "
                            + "(id_compra, id_producto, id_proveedor, cantidad_comprada) "
                            + "VALUES ((SELECT MIN(id_compra) FROM compra), 1, 1, 0)");
                    return false;
                } catch (SQLException e) {
                    String m = e.getMessage().toLowerCase();
                    return m.contains("check") || m.contains("constraint");
                }
            });

            // G05 / RNF02: cada carga tiene un hash unico.
            verificar("G05: carga_plantilla rechaza hash duplicado", () -> {
                st.executeUpdate("INSERT INTO carga_plantilla "
                        + "(nombre_archivo, hash_archivo, usuario) "
                        + "VALUES ('prueba.xlsx', 'hash-unico-g05', 'tester')");
                try (Statement s2 = c.createStatement()) {
                    s2.execute("INSERT INTO carga_plantilla "
                            + "(nombre_archivo, hash_archivo, usuario) "
                            + "VALUES ('copia.xlsx', 'hash-unico-g05', 'tester')");
                    return false;
                } catch (SQLException e) {
                    return e.getMessage().toLowerCase().contains("unique");
                }
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
