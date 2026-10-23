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
import agromedio.modelo.DetallePlantilla;
import agromedio.modelo.DetalleRecepcion;
import agromedio.modelo.Despacho;
import agromedio.modelo.FilaConsolidado;
import agromedio.modelo.PlantillaOperativa;
import agromedio.modelo.Producto;
import agromedio.datos.PlantillaDAOSqlite;
import agromedio.datos.ProductoDAO;
import agromedio.datos.ProductoDAOSqlite;

import java.io.File;
import java.nio.charset.StandardCharsets;
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
            String fecha = java.time.LocalDate.now().plusDays(3).toString();
            try (XSSFWorkbook wb = new XSSFWorkbook()) {
                var hoja = wb.createSheet("Plantilla");
                var enc = hoja.createRow(0);
                String[] cols = {"Cliente", "Contacto", "Telefono", "Mercado", "Fecha", "Producto", "Cantidad"};
                for (int i = 0; i < cols.length; i++) {
                    enc.createCell(i).setCellValue(cols[i]);
                }
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
            List<PlantillaOperativa> cargadas = new PlantillaDAOSqlite().listarPorFecha(fecha);
            Producto creado = productos.buscarPorNombre("Producto Inexistente");
            return r.getIdCarga() > 0
                    && r.getPlantillasCreadas() == 1
                    && r.getProductosRegistrados() == 3
                    && r.getProductosCreados() == 1
                    && r.getErrores().size() == 1   // solo la cantidad negativa
                    && creado != null && creado.getIdUnidadDespacho() == 6
                    && cargadas.size() == 1
                    && cargadas.get(0).getIdCarga() == r.getIdCarga();
        });

        verificar("G05/RNF02: carga duplicada muestra 'Carga ya realizada'", () -> {
            File xlsx = new File("build/plantilla_prueba.xlsx");
            ResultadoCarga r = new CargaExcelServicio().cargar(xlsx);
            return r.getPlantillasCreadas() == 0
                    && r.getProductosRegistrados() == 0
                    && r.getErrores().stream().anyMatch(e -> e.startsWith("Carga ya realizada"));
        });

        verificar("G08/RF01: carga de archivos planos CSV y TXT", () -> {
            String fechaCsv = java.time.LocalDate.now().plusDays(5).toString();
            File csv = new File("build/plantilla_prueba.csv");
            String contenidoCsv = "Cliente;Contacto;Telefono;Mercado;Fecha;Producto;Cantidad\n"
                    + "Mercado CSV;Luis Torres;607000222;Mercado CSV;" + fechaCsv + ";Arroz blanco;25\n";
            Files.writeString(csv.toPath(), contenidoCsv, StandardCharsets.UTF_8);
            ResultadoCarga rCsv = new CargaExcelServicio().cargar(csv);

            String fechaTxt = java.time.LocalDate.now().plusDays(6).toString();
            File txt = new File("build/plantilla_prueba.txt");
            String contenidoTxt = "Cliente\tContacto\tTelefono\tMercado\tFecha\tProducto\tCantidad\n"
                    + "Mercado TXT\tAna Ruiz\t607000333\tMercado TXT\t" + fechaTxt + "\tPapa criolla\t30\n";
            Files.writeString(txt.toPath(), contenidoTxt, StandardCharsets.UTF_8);
            ResultadoCarga rTxt = new CargaExcelServicio().cargar(txt);

            String fechaBom = java.time.LocalDate.now().plusDays(7).toString();
            File csvComa = new File("build/plantilla_prueba_coma.csv");
            String contenidoComa = "\uFEFFCliente,Contacto,Telefono,Mercado,Fecha,Producto,Cantidad\n"
                    + "\"Mercado, Coma\",\"Pedro Gil\",607000444,\"Mercado Coma\"," + fechaBom
                    + ",Arroz blanco,12\n";
            Files.writeString(csvComa.toPath(), contenidoComa, StandardCharsets.UTF_8);
            ResultadoCarga rComa = new CargaExcelServicio().cargar(csvComa);

            return rCsv.getIdCarga() > 0 && rCsv.getPlantillasCreadas() == 1
                    && rCsv.getProductosRegistrados() == 1 && !rCsv.hayErrores()
                    && rTxt.getIdCarga() > 0 && rTxt.getPlantillasCreadas() == 1
                    && rTxt.getProductosRegistrados() == 1 && !rTxt.hayErrores()
                    && rComa.getIdCarga() > 0 && rComa.getPlantillasCreadas() == 1
                    && rComa.getProductosRegistrados() == 1 && !rComa.hayErrores();
        });

        verificar("G08/RF02: cantidades descriptivas y productos nuevos", () -> {
            String fecha = java.time.LocalDate.now().plusDays(8).toString();
            File xlsx = new File("build/plantilla_prueba_cantidades.xlsx");
            try (XSSFWorkbook wb = new XSSFWorkbook()) {
                var hoja = wb.createSheet("Plantilla");
                var enc = hoja.createRow(0);
                String[] cols = {"Cliente", "Contacto", "Telefono", "Mercado", "Fecha", "Producto", "Cantidad"};
                for (int i = 0; i < cols.length; i++) {
                    enc.createCell(i).setCellValue(cols[i]);
                }
                String[][] datos = {
                        {"Mercado Cantidades", "Ana Perez", "606000111", "Mercado Cantidades", fecha, "Cupcake Prueba", "9 unidades"},
                        {"Mercado Cantidades", "Ana Perez", "606000111", "Mercado Cantidades", fecha, "Mantecada Prueba", "1 de 300 gr"},
                        {"Mercado Cantidades", "Ana Perez", "606000111", "Mercado Cantidades", fecha, "Carne Prueba", "400 gramos"},
                        {"Mercado Cantidades", "Ana Perez", "606000111", "Mercado Cantidades", fecha, "Arroz Prueba", "1 libra"},
                        {"Mercado Cantidades", "Ana Perez", "606000111", "Mercado Cantidades", fecha, "Arroz Prueba", "1 de 250 gr"},
                        {"Mercado Cantidades", "Ana Perez", "606000111", "Mercado Cantidades", fecha, "Cilantro Prueba", "1 rama"}
                };
                for (int i = 0; i < datos.length; i++) {
                    var fila = hoja.createRow(i + 1);
                    for (int j = 0; j < datos[i].length; j++) {
                        fila.createCell(j).setCellValue(datos[i][j]);
                    }
                }
                try (var out = Files.newOutputStream(xlsx.toPath())) {
                    wb.write(out);
                }
            }

            ResultadoCarga r = new CargaExcelServicio().cargar(xlsx);
            List<PlantillaOperativa> cargadas = new PlantillaDAOSqlite().listarPorFecha(fecha);
            Producto cupcake = productos.buscarPorNombre("Cupcake Prueba");
            Producto mantecada = productos.buscarPorNombre("Mantecada Prueba (300 gr)");
            Producto carne = productos.buscarPorNombre("Carne Prueba");
            Producto arroz = productos.buscarPorNombre("Arroz Prueba");
            Producto arroz250 = productos.buscarPorNombre("Arroz Prueba (250 gr)");
            List<DetallePlantilla> detalle = cargadas.isEmpty()
                    ? new ArrayList<>()
                    : new PlantillaDAOSqlite().listarDetalle(cargadas.get(0).getIdPlantilla());
            return r.getIdCarga() > 0 && r.getPlantillasCreadas() == 1
                    && r.getProductosRegistrados() == 6 && r.getProductosCreados() == 6
                    && !r.hayErrores()
                    && cupcake != null && cupcake.getIdUnidadDespacho() == 6
                    && mantecada != null && mantecada.getIdUnidadDespacho() == 6
                    && carne != null && carne.getIdUnidadDespacho() == 2
                    && arroz != null && arroz.getIdUnidadDespacho() == 3
                    && arroz250 != null && arroz250.getIdUnidadDespacho() == 6
                    && detalle.stream().anyMatch(d -> d.getIdProducto() == cupcake.getIdProducto()
                            && d.getCantidadSolicitada() == 9.0)
                    && detalle.stream().anyMatch(d -> d.getIdProducto() == mantecada.getIdProducto()
                            && d.getCantidadSolicitada() == 1.0)
                    && detalle.stream().anyMatch(d -> d.getIdProducto() == carne.getIdProducto()
                            && d.getCantidadSolicitada() == 0.4)
                    && detalle.stream().anyMatch(d -> d.getIdProducto() == arroz.getIdProducto()
                            && d.getCantidadSolicitada() == 1.0)
                    && detalle.stream().anyMatch(d -> d.getIdProducto() == arroz250.getIdProducto()
                            && d.getCantidadSolicitada() == 1.0);
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
            int id = plantillas.crear(1, "Mercado duplicado G04",
                    java.time.LocalDate.now().plusDays(1).toString());
            Producto frijol = productos.buscarPorNombre("Frijol cargamonton");
            plantillas.agregarProducto(id, frijol.getIdProducto(), 100);
            try {
                plantillas.agregarProducto(id, frijol.getIdProducto(), 5);
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

        verificar("RN-05: no se recibe mas de lo pendiente", () -> {
            int idCompra = compras.comprasDe(idPlantilla[0]).get(0).getIdCompra();
            try {
                // Azucar ya esta completa (50/50); intentar recibir 1 lb mas debe fallar
                recepciones.registrarRecepcion(idCompra, null, "Exceso", List.of(1.0, 0.0));
                return false;
            } catch (IllegalArgumentException e) {
                return e.getMessage().contains("supera el pendiente");
            }
        });

        verificar("D02: no se registra una entrega con todas las cantidades en cero", () -> {
            int idCompra = compras.comprasDe(idPlantilla[0]).get(0).getIdCompra();
            try {
                recepciones.registrarRecepcion(idCompra, null, "Vacia", List.of(0.0, 0.0));
                return false;
            } catch (IllegalArgumentException e) {
                return e.getMessage().contains("mayor que cero");
            }
        });

        verificar("RN-05/RN-06: segunda entrega acumula y elimina el faltante", () -> {
            int idCompra = compras.comprasDe(idPlantilla[0]).get(0).getIdCompra();
            List<DetalleRecepcion> pendientes = recepciones.registrarRecepcion(
                    idCompra, null, "Segunda entrega", List.of(0.0, 10.0));
            DetalleRecepcion frijol = recepciones.estadoRecepcion(idCompra).stream()
                    .filter(d -> "Frijol cargamonton".equals(d.getNombreProducto()))
                    .findFirst().orElseThrow();
            return pendientes.isEmpty()
                    && frijol.getCantidadRecibida() == 100.0
                    && frijol.getCantidadFaltante() == 0.0;
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

        // ---------------- RN-09 / RN-10: cierre real del despacho ----------------
        verificar("RN-09/RN-10: faltante no aceptado bloquea el despacho", () -> {
            int id = crearMercadoListo(plantillas, compras, recepciones, alistamientos,
                    "Mercado G03 faltante no aceptado", 10, 77, 70);
            Despacho d = despachos.iniciar(id);
            DetalleDespacho dd = despachos.detalle(d.getIdDespacho()).get(0);
            boolean iniciaEnLoAlistado = dd.getCantidadDespachada() == 70.0
                    && dd.getCantidadAlistada() == 70.0;
            dd.setVerificado(true);
            dd.setFaltanteAceptado(false);
            despachos.marcarItem(dd);
            try {
                despachos.cerrar(d.getIdDespacho(), id, "Novedad");
                return false;
            } catch (IllegalStateException e) {
                return iniciaEnLoAlistado && e.getMessage().contains("faltante no aceptado");
            }
        });

        verificar("D02/RN-10: no se verifica una cantidad despachada en cero", () -> {
            int id = crearMercadoListo(plantillas, compras, recepciones, alistamientos,
                    "Mercado G03 cantidad cero", 11, 77, 70);
            Despacho d = despachos.iniciar(id);
            DetalleDespacho dd = despachos.detalle(d.getIdDespacho()).get(0);
            dd.setCantidadDespachada(0);
            dd.setVerificado(true);
            try {
                despachos.marcarItem(dd);
                return false;
            } catch (IllegalArgumentException e) {
                return e.getMessage().contains("mayor que cero");
            }
        });

        verificar("RN-10: faltante aceptado exige novedad", () -> {
            int id = crearMercadoListo(plantillas, compras, recepciones, alistamientos,
                    "Mercado G03 sin novedad", 12, 77, 70);
            Despacho d = despachos.iniciar(id);
            DetalleDespacho dd = despachos.detalle(d.getIdDespacho()).get(0);
            dd.setCantidadDespachada(70);
            dd.setVerificado(true);
            dd.setFaltanteAceptado(true);
            despachos.marcarItem(dd);
            try {
                despachos.cerrar(d.getIdDespacho(), id, "   ");
                return false;
            } catch (IllegalArgumentException e) {
                return e.getMessage().contains("novedad");
            }
        });

        verificar("RN-09/RN-10: faltante aceptado con novedad cierra y queda registrado", () -> {
            int id = crearMercadoListo(plantillas, compras, recepciones, alistamientos,
                    "Mercado G03 con novedad", 13, 77, 70);
            Despacho d = despachos.iniciar(id);
            DetalleDespacho dd = despachos.detalle(d.getIdDespacho()).get(0);
            dd.setCantidadDespachada(70);
            dd.setVerificado(true);
            dd.setFaltanteAceptado(true);
            despachos.marcarItem(dd);
            List<DetalleDespacho> pendientes = despachos.cerrar(
                    d.getIdDespacho(), id, "Faltante de 7 L aceptado por el cliente");
            PlantillaOperativa p = new agromedio.datos.PlantillaDAOSqlite().obtener(id);
            DetalleDespacho guardado = despachos.detalle(d.getIdDespacho()).get(0);
            return pendientes.isEmpty()
                    && PlantillaOperativa.ESTADO_DESPACHADA.equals(p.getEstado())
                    && guardado.isFaltanteAceptado()
                    && despachos.tieneFaltanteAceptado(d.getIdDespacho());
        });

        verificar("Control de estados: no se puede despachar una plantilla CARGADA", () -> {
            try {
                despachos.iniciar(3);   // plantilla 3 del seed esta CARGADA
                return false;
            } catch (IllegalStateException e) {
                return e.getMessage().contains("LISTA");
            }
        });

        verificar("RN-05/RN-06: dos entregas 30 + 47 completan 77 L", () -> {
            PlantillaServicio ps = new PlantillaServicio();
            int id = ps.crear(1, "Mercado parcial 77",
                    java.time.LocalDate.now().plusDays(6).toString());
            Producto leche = productos.buscarPorNombre("Leche entera pasteurizada");
            ps.agregarProducto(id, leche.getIdProducto(), 77);
            List<DetalleCompra> items = new ArrayList<>();
            items.add(detalleCompra(id, leche, 1, 77));
            compras.registrarCompra(id, null, items);
            int idCompra = compras.comprasDe(id).get(0).getIdCompra();

            recepciones.registrarRecepcion(idCompra, null, "Primera entrega", List.of(30.0));
            DetalleRecepcion primera = recepciones.estadoRecepcion(idCompra).get(0);
            boolean parcialOk = primera.getCantidadRecibida() == 30.0
                    && primera.getCantidadFaltante() == 47.0;

            List<DetalleRecepcion> pendientes = recepciones.registrarRecepcion(
                    idCompra, null, "Segunda entrega", List.of(47.0));
            DetalleRecepcion segunda = recepciones.estadoRecepcion(idCompra).get(0);
            return parcialOk && pendientes.isEmpty()
                    && segunda.getCantidadRecibida() == 77.0
                    && segunda.getCantidadFaltante() == 0.0;
        });

        // ---------------- RN-07 / RN-08: limites de alistamiento ----------------
        verificar("RN-07: no se alista mas de lo recibido (80 > 70)", () -> {
            PlantillaServicio ps = new PlantillaServicio();
            int id = ps.crear(1, "Mercado tope alistamiento",
                    java.time.LocalDate.now().plusDays(7).toString());
            Producto leche = productos.buscarPorNombre("Leche entera pasteurizada");
            ps.agregarProducto(id, leche.getIdProducto(), 77);
            List<DetalleCompra> items = new ArrayList<>();
            items.add(detalleCompra(id, leche, 1, 77));
            compras.registrarCompra(id, null, items);
            int idCompra = compras.comprasDe(id).get(0).getIdCompra();
            recepciones.registrarRecepcion(idCompra, null, "Parcial 70", List.of(70.0));
            recepciones.cerrarRecepcion(idCompra);

            Alistamiento a = alistamientos.iniciar(id);
            DetalleAlistamiento d = alistamientos.detalle(a.getIdAlistamiento()).get(0);
            boolean recibidoOk = d.getCantidadRecibida() == 70.0;
            d.setCantidadAlistada(80);
            try {
                alistamientos.registrarAvance(a.getIdAlistamiento(), d);
                return false;
            } catch (IllegalArgumentException e) {
                return recibidoOk && e.getMessage().contains("supera lo disponible");
            }
        });

        verificar("RN-08: no se completa un producto sin recepcion (recibido 0)", () -> {
            PlantillaServicio ps = new PlantillaServicio();
            int id = ps.crear(1, "Mercado sin recepcion de azucar",
                    java.time.LocalDate.now().plusDays(8).toString());
            Producto leche = productos.buscarPorNombre("Leche entera pasteurizada");
            Producto azucar = productos.buscarPorNombre("Azucar blanca");
            ps.agregarProducto(id, leche.getIdProducto(), 77);
            ps.agregarProducto(id, azucar.getIdProducto(), 50);
            List<DetalleCompra> items = new ArrayList<>();
            items.add(detalleCompra(id, leche, 1, 77));
            items.add(detalleCompra(id, azucar, 1, 50));
            compras.registrarCompra(id, null, items);
            int idCompra = compras.comprasDe(id).get(0).getIdCompra();
            // Orden de detalle por producto: Azucar primero, Leche segundo.
            recepciones.registrarRecepcion(idCompra, null, "Solo leche", List.of(0.0, 70.0));
            recepciones.cerrarRecepcion(idCompra);

            Alistamiento a = alistamientos.iniciar(id);
            DetalleAlistamiento azucarDetalle = alistamientos.detalle(a.getIdAlistamiento()).stream()
                    .filter(d -> "Azucar blanca".equals(d.getNombreProducto()))
                    .findFirst().orElseThrow();
            azucarDetalle.setCantidadAlistada(0);
            azucarDetalle.setCompletado(true);
            try {
                alistamientos.registrarAvance(a.getIdAlistamiento(), azucarDetalle);
                return false;
            } catch (IllegalStateException e) {
                boolean bloqueaAvance = e.getMessage().contains("no tiene recepcion");
                try {
                    alistamientos.completar(a.getIdAlistamiento(), id);
                    return false;
                } catch (IllegalStateException e2) {
                    return bloqueaAvance && e2.getMessage().contains("sin recepcion registrada");
                }
            }
        });

        verificar("RN-07/RN-08: faltante parcial cerrado se alista por lo recibido", () -> {
            PlantillaServicio ps = new PlantillaServicio();
            int id = ps.crear(1, "Mercado faltante cerrado",
                    java.time.LocalDate.now().plusDays(9).toString());
            Producto leche = productos.buscarPorNombre("Leche entera pasteurizada");
            ps.agregarProducto(id, leche.getIdProducto(), 77);
            List<DetalleCompra> items = new ArrayList<>();
            items.add(detalleCompra(id, leche, 1, 77));
            compras.registrarCompra(id, null, items);
            int idCompra = compras.comprasDe(id).get(0).getIdCompra();
            recepciones.registrarRecepcion(idCompra, null, "Parcial 70", List.of(70.0));
            recepciones.cerrarRecepcion(idCompra);

            Alistamiento a = alistamientos.iniciar(id);
            DetalleAlistamiento d = alistamientos.detalle(a.getIdAlistamiento()).get(0);
            d.setCantidadAlistada(70);
            d.setCompletado(true);
            alistamientos.registrarAvance(a.getIdAlistamiento(), d);
            boolean completo = alistamientos.completar(a.getIdAlistamiento(), id);
            PlantillaOperativa p = new agromedio.datos.PlantillaDAOSqlite().obtener(id);
            return completo && PlantillaOperativa.ESTADO_LISTA.equals(p.getEstado());
        });

        // ---------------- RN-12: proteccion del historico ----------------
        verificar("RN-12: el servicio no elimina una plantilla con historico", () -> {
            PlantillaServicio ps = new PlantillaServicio();
            try {
                ps.eliminar(1);
                return false;
            } catch (IllegalStateException e) {
                return e.getMessage().contains("compras, recepciones, alistamientos o despachos");
            }
        });

        verificar("RN-12: el servicio elimina una plantilla CARGADA sin historico", () -> {
            PlantillaServicio ps = new PlantillaServicio();
            int id = ps.crear(1, "Mercado G04 temporal",
                    java.time.LocalDate.now().plusDays(10).toString());
            ps.agregarProducto(id, 1, 5);
            ps.eliminar(id);
            return ps.obtener(id) == null && ps.detalle(id).isEmpty();
        });

        // ---------------- RN-01: plantilla con al menos un producto ----------------
        verificar("RN-01: no se registra compra de una plantilla vacia", () -> {
            int id = plantillas.crear(1, "Mercado G06 vacio",
                    java.time.LocalDate.now().plusDays(12).toString());
            List<DetalleCompra> items = new ArrayList<>();
            items.add(detalleCompra(id, productos.buscarPorNombre("Leche entera pasteurizada"), 1, 10));
            try {
                compras.registrarCompra(id, null, items);
                return false;
            } catch (IllegalStateException e) {
                PlantillaOperativa p = plantillas.obtener(id);
                return e.getMessage().contains("al menos un producto")
                        && compras.comprasDe(id).isEmpty()
                        && PlantillaOperativa.ESTADO_CARGADA.equals(p.getEstado());
            }
        });

        // ---------------- RN-11 / D02: cero en alistamiento ----------------
        verificar("D02/RN-11: no se completa un producto con cantidad alistada 0", () -> {
            PlantillaServicio ps = new PlantillaServicio();
            int id = ps.crear(1, "Mercado G07 alistamiento cero",
                    java.time.LocalDate.now().plusDays(11).toString());
            Producto leche = productos.buscarPorNombre("Leche entera pasteurizada");
            ps.agregarProducto(id, leche.getIdProducto(), 77);
            List<DetalleCompra> items = new ArrayList<>();
            items.add(detalleCompra(id, leche, 1, 77));
            compras.registrarCompra(id, null, items);
            int idCompra = compras.comprasDe(id).get(0).getIdCompra();
            recepciones.registrarRecepcion(idCompra, null, "Parcial G07", List.of(70.0));
            recepciones.cerrarRecepcion(idCompra);

            Alistamiento a = alistamientos.iniciar(id);
            DetalleAlistamiento d = alistamientos.detalle(a.getIdAlistamiento()).get(0);
            d.setCantidadAlistada(0);
            d.setCompletado(true);
            try {
                alistamientos.registrarAvance(a.getIdAlistamiento(), d);
                return false;
            } catch (IllegalArgumentException e) {
                return e.getMessage().contains("mayor que cero");
            }
        });

        System.out.println();
        System.out.println("RESULTADO: " + (total - fallos) + "/" + total + " pruebas correctas");
        if (fallos > 0) {
            System.exit(1);
        }
    }

    private static int crearMercadoListo(PlantillaServicio plantillas, CompraServicio compras,
                                         RecepcionServicio recepciones, AlistamientoServicio alistamientos,
                                         String nombre, int dias, double solicitado, double recibido) {
        int id = plantillas.crear(1, nombre,
                java.time.LocalDate.now().plusDays(dias).toString());
        Producto leche = new ProductoDAOSqlite().buscarPorNombre("Leche entera pasteurizada");
        plantillas.agregarProducto(id, leche.getIdProducto(), solicitado);
        List<DetalleCompra> items = new ArrayList<>();
        items.add(detalleCompra(id, leche, 1, solicitado));
        compras.registrarCompra(id, null, items);
        int idCompra = compras.comprasDe(id).get(0).getIdCompra();
        recepciones.registrarRecepcion(idCompra, null, "Parcial G03", List.of(recibido));
        recepciones.cerrarRecepcion(idCompra);

        Alistamiento a = alistamientos.iniciar(id);
        DetalleAlistamiento d = alistamientos.detalle(a.getIdAlistamiento()).get(0);
        d.setCantidadAlistada(recibido);
        d.setCompletado(true);
        alistamientos.registrarAvance(a.getIdAlistamiento(), d);
        alistamientos.completar(a.getIdAlistamiento(), id);
        return id;
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
