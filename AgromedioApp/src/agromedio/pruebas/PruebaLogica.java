package agromedio.pruebas;

import agromedio.datos.ConexionBD;
import agromedio.logica.AlistamientoServicio;
import agromedio.logica.CargaExcelServicio;
import agromedio.logica.CompraServicio;
import agromedio.logica.DespachoServicio;
import agromedio.logica.PlantillaServicio;
import agromedio.logica.RecepcionServicio;
import agromedio.logica.ResultadoCarga;
import agromedio.modelo.Alistamiento;
import agromedio.modelo.DetalleAlistamiento;
import agromedio.modelo.DetalleCompra;
import agromedio.modelo.DetalleDespacho;
import agromedio.modelo.DetalleRecepcion;
import agromedio.modelo.Despacho;
import agromedio.modelo.FilaConsolidado;
import agromedio.modelo.PlantillaOperativa;
import agromedio.modelo.Producto;
import agromedio.datos.ProductoDAO;
import agromedio.datos.ProductoDAOSqlite;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Pruebas integradas de la logica de negocio (RF01 - RF10).
 * Ejecutar: java -cp build/classes;lib/* agromedio.pruebas.PruebaLogica
 */
public class PruebaLogica {

    private static int total = 0;
    private static int fallos = 0;

    public static void main(String[] args) throws Exception {
        Path bd = Paths.get("build", "prueba_logica.db");
        Files.createDirectories(bd.toAbsolutePath().getParent());
        Files.deleteIfExists(bd);
        ConexionBD.fijarRuta(bd.toString());
        ConexionBD.inicializar();

        PlantillaServicio plantillas = new PlantillaServicio();
        CompraServicio compras = new CompraServicio();
        RecepcionServicio recepciones = new RecepcionServicio();
        AlistamientoServicio alistamientos = new AlistamientoServicio();
        DespachoServicio despachos = new DespachoServicio();
        ProductoDAO productos = new ProductoDAOSqlite();

        // ---------------- RF01 - RF02: carga de archivo .xlsx ----------------
        verificar("RF01/RF02: carga de plantilla desde Excel", () -> {
            File xlsx = new File("build/plantilla_prueba.xlsx");
            Files.createDirectories(Paths.get("build"));
            try (XSSFWorkbook wb = new XSSFWorkbook()) {
                var hoja = wb.createSheet("Plantilla");
                var enc = hoja.createRow(0);
                String[] cols = {"Cliente", "Contacto", "Telefono", "Mercado", "Fecha", "Producto", "Cantidad"};
                for (int i = 0; i < cols.length; i++) {
                    enc.createCell(i).setCellValue(cols[i]);
                }
                String fecha = java.time.LocalDate.now().plusDays(3).toString();
                Object[][] datos = {
                        {"Mercado Villa Norte", "Ana Perez", "606000111", "Mercado Villa Norte", fecha, "Leche entera pasteurizada", 50},
                        {"Mercado Villa Norte", "Ana Perez", "606000111", "Mercado Villa Norte", fecha, "Papa criolla", 200},
                        {"Mercado Villa Norte", "Ana Perez", "606000111", "Mercado Villa Norte", fecha, "Producto Inexistente", 10},
                        {"Mercado Villa Norte", "Ana Perez", "606000111", "Mercado Villa Norte", fecha, "Arroz blanco", -5}
                };
                for (int i = 0; i < datos.length; i++) {
                    var fila = hoja.createRow(i + 1);
                    for (int j = 0; j < datos[i].length; j++) {
                        if (datos[i][j] instanceof Number n) {
                            fila.createCell(j).setCellValue(n.doubleValue());
                        } else {
                            fila.createCell(j).setCellValue((String) datos[i][j]);
                        }
                    }
                }
                try (var out = Files.newOutputStream(xlsx.toPath())) {
                    wb.write(out);
                }
            }
            ResultadoCarga r = new CargaExcelServicio().cargar(xlsx);
            return r.getPlantillasCreadas() == 1
                    && r.getProductosRegistrados() == 2
                    && r.getErrores().size() == 2;   // producto inexistente + cantidad negativa
        });

        // ---------------- RF03: consolidacion de demanda ----------------
        verificar("RF03: consolidado de arroz = 200 lb, 2 mercados, pendiente 100", () -> {
            String fecha = java.time.LocalDate.now().plusDays(1).toString();
            List<FilaConsolidado> filas = compras.consolidar(fecha);
            return filas.stream()
                    .filter(f -> f.getProducto().equals("Arroz blanco"))
                    .anyMatch(f -> f.getTotalSolicitado() == 200.0
                            && f.getMercados() == 2
                            && f.getTotalComprado() == 100.0
                            && f.getPendiente() == 100.0);
        });

        // ---------------- RF04: registro de compra ----------------
        int[] idPlantilla = {0};
        verificar("RF04: registro de compra cambia plantilla a EN_COMPRA", () -> {
            PlantillaServicio ps = new PlantillaServicio();
            int id = ps.crear(1, "Mercado de prueba logica", java.time.LocalDate.now().plusDays(5).toString());
            idPlantilla[0] = id;
            ps.agregarProducto(id, productos.buscarPorNombre("Frijol cargamonton").getIdProducto(), 100);
            ps.agregarProducto(id, productos.buscarPorNombre("Azucar blanca").getIdProducto(), 50);

            List<DetalleCompra> items = new ArrayList<>();
            items.add(detalleCompra(id, productos.buscarPorNombre("Frijol cargamonton"), 1, 100));
            items.add(detalleCompra(id, productos.buscarPorNombre("Azucar blanca"), 1, 50));
            compras.registrarCompra(id, null, items);

            PlantillaOperativa p = new agromedio.datos.PlantillaDAOSqlite().obtener(id);
            return PlantillaOperativa.ESTADO_EN_COMPRA.equals(p.getEstado());
        });

        verificar("RF04: se rechaza compra sin proveedor", () -> {
            List<DetalleCompra> items = new ArrayList<>();
            DetalleCompra d = new DetalleCompra();
            d.setIdProducto(productos.buscarPorNombre("Leche entera pasteurizada").getIdProducto());
            d.setCantidadComprada(10);
            items.add(d);
            try {
                compras.registrarCompra(idPlantilla[0], null, items);
                return false;
            } catch (IllegalArgumentException e) {
                return e.getMessage().contains("proveedor");
            }
        });

        verificar("RF04: se rechaza producto duplicado en plantilla", () -> {
            try {
                plantillas.agregarProducto(idPlantilla[0], productos.buscarPorNombre("Frijol cargamonton").getIdProducto(), 5);
                return false;
            } catch (IllegalArgumentException e) {
                return e.getMessage().contains("ya esta");
            }
        });

        // ---------------- RF05 - RF06: recepcion y faltantes ----------------
        
        verificar("RF05/RF06: recepcion parcial genera faltante y plantilla en RECIBIENDO", () -> {
            int idCompra = compras.comprasDe(idPlantilla[0]).get(0).getIdCompra();
            // El detalle se lista ordenado por producto: Azucar blanca primero, Frijol despues
            List<Double> recibidas = List.of(50.0, 90.0);   // falto 10 lb de frijol (100 comprados)
            List<DetalleRecepcion> faltantes = recepciones.registrarRecepcion(idCompra, null, "Prueba", recibidas);
            boolean ok = faltantes.size() == 1 && faltantes.get(0).getCantidadFaltante() == 10.0;
            PlantillaOperativa p = new agromedio.datos.PlantillaDAOSqlite().obtener(idPlantilla[0]);
            return ok && PlantillaOperativa.ESTADO_RECIBIENDO.equals(p.getEstado());
        });

        verificar("RF05: cerrar recepcion habilita alistamiento", () -> {
            int idCompra = compras.comprasDe(idPlantilla[0]).get(0).getIdCompra();
            recepciones.cerrarRecepcion(idCompra);
            PlantillaOperativa p = new agromedio.datos.PlantillaDAOSqlite().obtener(idPlantilla[0]);
            return PlantillaOperativa.ESTADO_ALISTANDO.equals(p.getEstado());
        });

        // ---------------- RF07 - RF08: alistamiento ----------------
        verificar("RF07: el alistamiento copia los productos de la plantilla", () -> {
            Alistamiento a = alistamientos.iniciar(idPlantilla[0]);
            return alistamientos.detalle(a.getIdAlistamiento()).size() == 2;
        });

        verificar("RF08: no se completa si hay items sin marcar", () -> {
            Alistamiento a = alistamientos.actual(idPlantilla[0]);
            return !alistamientos.completar(a.getIdAlistamiento(), idPlantilla[0]);
        });

        verificar("RF08: completar alistamiento pasa la plantilla a LISTA", () -> {
            Alistamiento a = alistamientos.actual(idPlantilla[0]);
            for (DetalleAlistamiento d : alistamientos.detalle(a.getIdAlistamiento())) {
                d.setCantidadAlistada(d.getCantidadSolicitada());
                d.setCompletado(true);
                alistamientos.registrarAvance(a.getIdAlistamiento(), d);
            }
            boolean completo = alistamientos.completar(a.getIdAlistamiento(), idPlantilla[0]);
            PlantillaOperativa p = new agromedio.datos.PlantillaDAOSqlite().obtener(idPlantilla[0]);
            return completo && PlantillaOperativa.ESTADO_LISTA.equals(p.getEstado());
        });

        // ---------------- RF09 - RF10: despacho ----------------
        verificar("RF09: el despacho crea la lista de chequeo", () -> {
            Despacho d = despachos.iniciar(idPlantilla[0]);
            return despachos.detalle(d.getIdDespacho()).size() == 2;
        });

        verificar("RF10: no se cierra si falta por verificar", () -> {
            Despacho d = despachos.actual(idPlantilla[0]);
            List<DetalleDespacho> pendientes = despachos.cerrar(d.getIdDespacho(), idPlantilla[0], null);
            return pendientes.size() == 2;
        });

        verificar("RF10: cerrar con todo verificado marca DESPACHADA al 100%", () -> {
            Despacho d = despachos.actual(idPlantilla[0]);
            for (DetalleDespacho dd : despachos.detalle(d.getIdDespacho())) {
                dd.setVerificado(true);
                despachos.marcarItem(dd);
            }
            List<DetalleDespacho> pendientes = despachos.cerrar(d.getIdDespacho(), idPlantilla[0], "Sin novedades");
            PlantillaOperativa p = new agromedio.datos.PlantillaDAOSqlite().obtener(idPlantilla[0]);
            int[] progreso = despachos.progreso(d.getIdDespacho());
            return pendientes.isEmpty()
                    && PlantillaOperativa.ESTADO_DESPACHADA.equals(p.getEstado())
                    && progreso[0] == 2 && progreso[1] == 2;
        });

        verificar("Control de estados: no se puede despachar una plantilla CARGADA", () -> {
            try {
                despachos.iniciar(3);   // plantilla 3 del seed esta CARGADA
                return false;
            } catch (IllegalStateException e) {
                return e.getMessage().contains("LISTA");
            }
        });

        System.out.println();
        System.out.println("RESULTADO: " + (total - fallos) + "/" + total + " pruebas correctas");
        if (fallos > 0) {
            System.exit(1);
        }
    }

    private static DetalleCompra detalleCompra(int idPlantilla, Producto p, int idProveedor, double cantidad) {
        DetalleCompra d = new DetalleCompra();
        d.setIdProducto(p.getIdProducto());
        d.setIdProveedor(idProveedor);
        d.setCantidadComprada(cantidad);
        return d;
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
            System.out.println("[FAIL] " + nombre + " -> " + e);
        }
    }
}
