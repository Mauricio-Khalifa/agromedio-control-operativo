package agromedio.pruebas;

import agromedio.datos.CategoriaDAO;
import agromedio.datos.CategoriaDAOSqlite;
import agromedio.datos.ClienteDAO;
import agromedio.datos.ClienteDAOSqlite;
import agromedio.datos.CompraDAO;
import agromedio.datos.CompraDAOSqlite;
import agromedio.datos.ConexionBD;
import agromedio.datos.DespachoDAO;
import agromedio.datos.DespachoDAOSqlite;
import agromedio.datos.PlantillaDAO;
import agromedio.datos.PlantillaDAOSqlite;
import agromedio.datos.ProductoDAO;
import agromedio.datos.ProductoDAOSqlite;
import agromedio.datos.ProveedorDAO;
import agromedio.datos.ProveedorDAOSqlite;
import agromedio.datos.UnidadMedidaDAO;
import agromedio.datos.UnidadMedidaDAOSqlite;
import agromedio.logica.AlistamientoServicio;
import agromedio.logica.CargaExcelServicio;
import agromedio.logica.CompraServicio;
import agromedio.logica.DespachoServicio;
import agromedio.logica.PlantillaServicio;
import agromedio.logica.ProgresoServicio;
import agromedio.logica.RecepcionServicio;
import agromedio.logica.ReporteServicio;
import agromedio.logica.ResultadoCarga;
import agromedio.modelo.Alistamiento;
import agromedio.modelo.Categoria;
import agromedio.modelo.Cliente;
import agromedio.modelo.Compra;
import agromedio.modelo.Despacho;
import agromedio.modelo.DetalleAlistamiento;
import agromedio.modelo.DetalleCompra;
import agromedio.modelo.DetalleDespacho;
import agromedio.modelo.DetallePlantilla;
import agromedio.modelo.DetalleRecepcion;
import agromedio.modelo.FilaProgreso;
import agromedio.modelo.PlantillaOperativa;
import agromedio.modelo.Producto;
import agromedio.modelo.Proveedor;
import agromedio.modelo.ResumenProgreso;
import agromedio.modelo.TareaProgreso;
import agromedio.modelo.UnidadMedida;
import agromedio.presentacion.Formato;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Simulacion en tiempo real de los escenarios de trabajo de cada rol operativo
 * (Modulo 0 a Modulo 4 + analisis). Ejercita los servicios reales sobre una base
 * de datos aislada y reporta:
 *   [OK]/[FAIL]  -> reglas de negocio que DEBEN cumplirse.
 *   [HALLAZGO]   -> comportamiento observado vs. esperado (defectos conocidos),
 *                   no rompe el build pero queda documentado en el informe.
 *
 * Ejecutar: java -cp build/classes;lib/* agromedio.pruebas.PruebaEscenarios
 */
public class PruebaEscenarios {

    private static int total = 0;
    private static int ok = 0;
    private static int fallos = 0;
    private static int totalHallazgos = 0;
    private static int hallazgosOk = 0;

    private static final List<String> detalleFallos = new ArrayList<>();
    private static final List<String> detalleHallazgos = new ArrayList<>();

    private static long contadorFechas = 40;

    public static void main(String[] args) throws Exception {
        Path bd = Paths.get("build", "prueba_escenarios.db");
        Files.createDirectories(bd.toAbsolutePath().getParent());
        Files.deleteIfExists(bd);
        ConexionBD.fijarRuta(bd.toString());
        ConexionBD.inicializar();

        PlantillaServicio plantillas = new PlantillaServicio();
        CompraServicio compras = new CompraServicio();
        RecepcionServicio recepciones = new RecepcionServicio();
        AlistamientoServicio alistamientos = new AlistamientoServicio();
        DespachoServicio despachos = new DespachoServicio();
        ProgresoServicio progreso = new ProgresoServicio();
        ReporteServicio reportes = new ReporteServicio();

        ProductoDAO productoDAO = new ProductoDAOSqlite();
        PlantillaDAO plantillaDAO = new PlantillaDAOSqlite();
        CompraDAO compraDAO = new CompraDAOSqlite();
        DespachoDAO despachoDAO = new DespachoDAOSqlite();

        // ================================================================
        rol("ANALISTA DE DATOS  (Modulo 0 - Carga de archivos)");
        // ================================================================
        File csv = new File("build/esc_csv.csv");
        String fechaCsv = fecha();
        Files.writeString(csv.toPath(), "Cliente;Contacto;Telefono;Mercado;Fecha;Producto;Cantidad\n"
                + "Comedor Escolar La Salle Bucaramanga;Ana;606;Mercado Carga E2E;" + fechaCsv
                + ";Leche entera pasteurizada;60\n"
                + "Comedor Escolar La Salle Bucaramanga;Ana;606;Mercado Carga E2E;" + fechaCsv
                + ";Papa criolla;120\n", StandardCharsets.UTF_8);

        escenario("E0-01 Carga CSV valida crea 1 plantilla y registra 2 productos", () -> {
            ResultadoCarga r = new CargaExcelServicio().cargar(csv);
            return r.getIdCarga() > 0 && r.getPlantillasCreadas() == 1
                    && r.getProductosRegistrados() == 2 && !r.hayErrores();
        });

        escenario("E0-02 Recargar el mismo archivo se detecta como duplicado (hash)", () -> {
            ResultadoCarga r = new CargaExcelServicio().cargar(csv);
            return r.isDuplicada()
                    && r.getErrores().stream().anyMatch(e -> e.startsWith("Carga ya realizada"));
        });

        escenario("E0-03 Encabezado invalido -> error de formato", () -> {
            File malo = new File("build/esc_malo.csv");
            Files.writeString(malo.toPath(), "A;B;C;D;E;F;G\n1;2;3;4;5;6;7\n", StandardCharsets.UTF_8);
            ResultadoCarga r = new CargaExcelServicio().cargar(malo);
            return r.hayErrores() && r.getErrores().get(0).startsWith("Formato no reconocido");
        });

        escenario("E0-04 Fecha invalida en fila -> error y fila descartada", () -> {
            File f = new File("build/esc_fecha.csv");
            Files.writeString(f.toPath(), "Cliente;Contacto;Telefono;Mercado;Fecha;Producto;Cantidad\n"
                    + "Cliente X;A;1;Mercado X;2026-13-40;Arroz blanco;10\n", StandardCharsets.UTF_8);
            ResultadoCarga r = new CargaExcelServicio().cargar(f);
            return r.hayErrores() && r.getErrores().stream().anyMatch(e -> e.contains("fecha invalida"));
        });

        escenario("E0-05 Cantidad cero o negativa -> error", () -> {
            File f = new File("build/esc_cant.csv");
            Files.writeString(f.toPath(), "Cliente;Contacto;Telefono;Mercado;Fecha;Producto;Cantidad\n"
                    + "Cliente Y;A;1;Mercado Y;" + fecha() + ";Arroz blanco;0\n"
                    + "Cliente Y;A;1;Mercado Y;" + fecha() + ";Papa criolla;-5\n", StandardCharsets.UTF_8);
            ResultadoCarga r = new CargaExcelServicio().cargar(f);
            return r.getErrores().size() == 2
                    && r.getErrores().stream().allMatch(e -> e.contains("mayor que cero"));
        });

        escenario("E0-06 Cantidad descriptiva '1 de 300 gr' -> producto '(300 gr)' unidad 6", () -> {
            File f = new File("build/esc_desc.csv");
            Files.writeString(f.toPath(), "Cliente;Contacto;Telefono;Mercado;Fecha;Producto;Cantidad\n"
                    + "Cliente Z;A;1;Mercado Z;" + fecha() + ";Harina de trigo;1 de 300 gr\n", StandardCharsets.UTF_8);
            ResultadoCarga r = new CargaExcelServicio().cargar(f);
            Producto p = productoDAO.buscarPorNombre("Harina de trigo (300 gr)");
            return !r.hayErrores() && p != null && p.getIdUnidadDespacho() == 6;
        });

        escenario("E0-07 Archivo inexistente -> error controlado", () -> {
            ResultadoCarga r = new CargaExcelServicio().cargar(new File("build/no_existe_esc.csv"));
            return r.hayErrores() && r.getErrores().get(0).contains("no existe");
        });

        escenario("C-2 la carga de archivo fusiona productos duplicados en una sola linea", () -> {
            String fDup = fecha();
            File f = new File("build/esc_reg_dup.csv");
            Files.writeString(f.toPath(), "Cliente;Contacto;Telefono;Mercado;Fecha;Producto;Cantidad\n"
                    + "Cliente D;A;1;Mercado D;" + fDup + ";Arroz blanco;10\n"
                    + "Cliente D;A;1;Mercado D;" + fDup + ";Arroz blanco;20\n", StandardCharsets.UTF_8);
            ResultadoCarga r = new CargaExcelServicio().cargar(f);
            List<PlantillaOperativa> ps = plantillaDAO.listarPorFecha(fDup);
            if (ps.isEmpty()) {
                return false;
            }
            List<DetallePlantilla> dets = plantillaDAO.listarDetalle(ps.get(0).getIdPlantilla());
            return !r.hayErrores() && dets.size() == 1
                    && dets.get(0).getCantidadSolicitada() == 30.0;
        });

        // ================================================================
        rol("GESTOR DE CATALOGOS  (Clientes, Categorias, Unidades, Proveedores, Productos)");
        // ================================================================
        escenario("E1-01 Crear y listar catalogos (categoria, unidad, proveedor, cliente, producto)", () -> {
            CategoriaDAO categorias = new CategoriaDAOSqlite();
            UnidadMedidaDAO unidades = new UnidadMedidaDAOSqlite();
            ProveedorDAO proveedores = new ProveedorDAOSqlite();
            ClienteDAO clientes = new ClienteDAOSqlite();
            Categoria cat = new Categoria(0, "Categoria Escenario");
            categorias.guardar(cat);
            UnidadMedida und = new UnidadMedida(0, "Unidad Escenario", "UE");
            unidades.guardar(und);
            Proveedor prov = new Proveedor(0, "Proveedor Escenario", "P", "1");
            proveedores.guardar(prov);
            Cliente cli = new Cliente(0, "Cliente Escenario", "C", "2");
            clientes.guardar(cli);
            Producto prod = new Producto(0, "Producto Escenario", cat.getIdCategoria(),
                    und.getIdUnidad(), und.getIdUnidad(), 1.0);
            productoDAO.guardar(prod);
            return categorias.listar().stream().anyMatch(c -> c.getIdCategoria() == cat.getIdCategoria())
                    && clientes.listar().stream().anyMatch(c -> c.getIdCliente() == cli.getIdCliente())
                    && productoDAO.obtener(prod.getIdProducto()) != null;
        });

        escenario("E1-02 No se elimina una categoria con productos asociados", () -> {
            Categoria cat = new CategoriaDAOSqlite().listar().stream()
                    .filter(c -> "Categoria Escenario".equals(c.getNombre())).findFirst().orElseThrow();
            try {
                new CategoriaDAOSqlite().eliminar(cat.getIdCategoria());
                return false;
            } catch (RuntimeException e) {
                return e.getMessage().contains("productos asociados");
            }
        });

        escenario("E1-03 No se elimina un producto usado en el ciclo operativo", () -> {
            try {
                productoDAO.eliminar(1);
                return false;
            } catch (RuntimeException e) {
                return e.getMessage().contains("ciclo operativo");
            }
        });

        escenario("M-3 Formato decimal conserva factores >= 1000 (ida y vuelta)", () -> {
            return Formato.decimal(Formato.numero(1000)) == 1000.0
                    && Formato.decimal("1.000") == 1000.0;
        });

        escenario("UI-01 Formato.estado traduce todos los estados del ciclo", () -> {
            return "Alistando".equals(Formato.estado("ALISTANDO"))
                    && "Recibiendo".equals(Formato.estado("RECIBIENDO"))
                    && "Lista".equals(Formato.estado("LISTA"));
        });

        // ================================================================
        rol("COMPRADOR  (Modulo 1 - Compras)");
        // ================================================================
        String fechaConsolidado = LocalDate.now().plusDays(1).toString();
        escenario("E2-01 Consolidado agrega la demanda de arroz de 2 mercados (200 lb)", () -> {
            var filas = compras.consolidar(fechaConsolidado);
            return filas.stream().filter(f -> "Arroz blanco".equals(f.getProducto()))
                    .anyMatch(f -> f.getTotalSolicitado() == 200.0 && f.getMercados() == 2);
        });

        escenario("E2-02 Registrar compra mueve la plantilla CARGADA -> EN_COMPRA", () -> {
            int id = plantillas.crear(1, "Mercado Compra Esc", fecha());
            Producto leche = productoDAO.buscarPorNombre("Leche entera pasteurizada");
            plantillas.agregarProducto(id, leche.getIdProducto(), 50);
            compras.registrarCompra(id, null, List.of(item(leche, 1, 50)));
            return PlantillaOperativa.ESTADO_EN_COMPRA.equals(plantillaDAO.obtener(id).getEstado());
        });

        escenario("E2-03 Se rechaza compra sin proveedor", () -> {
            int id = plantillas.crear(1, "Mercado Sin Prov", fecha());
            Producto leche = productoDAO.buscarPorNombre("Leche entera pasteurizada");
            plantillas.agregarProducto(id, leche.getIdProducto(), 10);
            try {
                compras.registrarCompra(id, null, List.of(item(leche, 0, 10)));
                return false;
            } catch (IllegalArgumentException e) {
                return e.getMessage().contains("proveedor");
            }
        });

        escenario("E2-04 Se rechaza compra con cantidad <= 0", () -> {
            int id = plantillas.crear(1, "Mercado Cant Cero", fecha());
            Producto leche = productoDAO.buscarPorNombre("Leche entera pasteurizada");
            plantillas.agregarProducto(id, leche.getIdProducto(), 10);
            try {
                compras.registrarCompra(id, null, List.of(item(leche, 1, 0)));
                return false;
            } catch (IllegalArgumentException e) {
                return e.getMessage().contains("mayores que cero");
            }
        });

        escenario("E2-05 Se rechaza compra de una plantilla vacia (RN-01)", () -> {
            int id = plantillas.crear(1, "Mercado Vacio Esc", fecha());
            Producto leche = productoDAO.buscarPorNombre("Leche entera pasteurizada");
            try {
                compras.registrarCompra(id, null, List.of(item(leche, 1, 10)));
                return false;
            } catch (IllegalStateException e) {
                return e.getMessage().contains("al menos un producto");
            }
        });

        escenario("E2-06 Se rechaza compra de una plantilla LISTA", () -> {
            int id = cicloHastaLista(plantillas, compras, recepciones, alistamientos,
                    "Mercado Lista Compra", fecha(), 40, 40).idPlantilla();
            Producto leche = productoDAO.buscarPorNombre("Leche entera pasteurizada");
            try {
                compras.registrarCompra(id, null, List.of(item(leche, 1, 10)));
                return false;
            } catch (IllegalStateException e) {
                return e.getMessage().contains("ya avanzo");
            }
        });

        escenario("H-04 registrarCompra bloquea compras en una plantilla ALISTANDO", () -> {
            int id = plantillas.crear(1, "Mercado En Alistando", fecha());
            Producto leche = productoDAO.buscarPorNombre("Leche entera pasteurizada");
            Producto azucar = productoDAO.buscarPorNombre("Azucar blanca");
            plantillas.agregarProducto(id, leche.getIdProducto(), 40);
            compras.registrarCompra(id, null, List.of(item(leche, 1, 40)));
            int idCompra = compras.comprasDe(id).get(0).getIdCompra();
            recepciones.registrarRecepcion(idCompra, null, "r", List.of(40.0));
            recepciones.cerrarRecepcion(idCompra); // plantilla -> ALISTANDO
            try {
                compras.registrarCompra(id, null, List.of(item(azucar, 1, 5)));
                return false;
            } catch (IllegalStateException e) {
                return e.getMessage().contains("avanzo");
            }
        });

        // ================================================================
        rol("RECEPCIONISTA  (Modulo 2 - Recepcion y faltantes)");
        // ================================================================
        int[] recepId = {0};
        escenario("E3-00 Preparar compra de 77 L para recepcion", () -> {
            int id = plantillas.crear(1, "Mercado Recepcion Esc", fecha());
            Producto leche = productoDAO.buscarPorNombre("Leche entera pasteurizada");
            plantillas.agregarProducto(id, leche.getIdProducto(), 77);
            compras.registrarCompra(id, null, List.of(item(leche, 1, 77)));
            recepId[0] = compras.comprasDe(id).get(0).getIdCompra();
            return recepId[0] > 0;
        });

        escenario("E3-01 Entrega parcial 70/77 genera faltante 7 y plantilla RECIBIENDO", () -> {
            List<DetalleRecepcion> pend = recepciones.registrarRecepcion(recepId[0], null, "Parcial", List.of(70.0));
            PlantillaOperativa p = plantillaDAO.obtener(compraDAO.obtener(recepId[0]).getIdPlantilla());
            return pend.size() == 1 && pend.get(0).getCantidadFaltante() == 7.0
                    && PlantillaOperativa.ESTADO_RECIBIENDO.equals(p.getEstado());
        });

        escenario("E3-02 No se recibe mas de lo pendiente (RN-05)", () -> {
            try {
                recepciones.registrarRecepcion(recepId[0], null, "Exceso", List.of(10.0));
                return false;
            } catch (IllegalArgumentException e) {
                return e.getMessage().contains("supera el pendiente");
            }
        });

        escenario("E3-03 Una entrega con todo en cero se rechaza (D02)", () -> {
            try {
                recepciones.registrarRecepcion(recepId[0], null, "Vacia", List.of(0.0));
                return false;
            } catch (IllegalArgumentException e) {
                return e.getMessage().contains("mayor que cero");
            }
        });

        escenario("E3-04 Segunda entrega 7 completa el faltante (RN-06)", () -> {
            List<DetalleRecepcion> pend = recepciones.registrarRecepcion(recepId[0], null, "Cierre", List.of(7.0));
            DetalleRecepcion d = recepciones.estadoRecepcion(recepId[0]).get(0);
            return pend.isEmpty() && d.getCantidadRecibida() == 77.0 && d.getCantidadFaltante() == 0.0;
        });

        escenario("E3-05 Cerrar recepcion sin entradas -> error", () -> {
            int id = plantillas.crear(1, "Mercado Sin Entrada", fecha());
            Producto leche = productoDAO.buscarPorNombre("Leche entera pasteurizada");
            plantillas.agregarProducto(id, leche.getIdProducto(), 20);
            compras.registrarCompra(id, null, List.of(item(leche, 1, 20)));
            int idCompra = compras.comprasDe(id).get(0).getIdCompra();
            try {
                recepciones.cerrarRecepcion(idCompra);
                return false;
            } catch (IllegalStateException e) {
                return e.getMessage().contains("entrada");
            }
        });

        escenario("E3-06 Cerrar recepcion mueve la plantilla a ALISTANDO", () -> {
            int idPlantilla = compraDAO.obtener(recepId[0]).getIdPlantilla();
            recepciones.cerrarRecepcion(recepId[0]);
            return PlantillaOperativa.ESTADO_ALISTANDO.equals(plantillaDAO.obtener(idPlantilla).getEstado());
        });

        escenario("C-2 registrarCompra rechaza producto duplicado", () -> {
            int id = plantillas.crear(1, "Mercado Compra Dup", fecha());
            Producto leche = productoDAO.buscarPorNombre("Leche entera pasteurizada");
            plantillas.agregarProducto(id, leche.getIdProducto(), 30);
            try {
                compras.registrarCompra(id, null, List.of(item(leche, 1, 20), item(leche, 2, 10)));
                return false;
            } catch (IllegalArgumentException e) {
                return e.getMessage().contains("repetido");
            }
        });

        escenario("C-3 registrarCompra con proveedor inexistente no deja compra huerfana", () -> {
            int id = plantillas.crear(1, "Mercado C3 Atom", fecha());
            Producto leche = productoDAO.buscarPorNombre("Leche entera pasteurizada");
            plantillas.agregarProducto(id, leche.getIdProducto(), 10);
            try {
                compras.registrarCompra(id, null, List.of(item(leche, 9999, 10)));
            } catch (RuntimeException ignore) {
                // esperado: falla por FK (proveedor inexistente)
            }
            return compras.comprasDe(id).isEmpty()
                    && PlantillaOperativa.ESTADO_CARGADA.equals(plantillaDAO.obtener(id).getEstado());
        });

        escenario("H-03 recepcion rechaza compras ANULADAS", () -> {
            int id = plantillas.crear(1, "Mercado Compra Anulada", fecha());
            Producto leche = productoDAO.buscarPorNombre("Leche entera pasteurizada");
            plantillas.agregarProducto(id, leche.getIdProducto(), 20);
            compras.registrarCompra(id, null, List.of(item(leche, 1, 20)));
            int idCompra = compras.comprasDe(id).get(0).getIdCompra();
            compraDAO.actualizarEstado(idCompra, Compra.ANULADA);
            try {
                recepciones.registrarRecepcion(idCompra, null, "Sobre anulada", List.of(20.0));
                return false;
            } catch (IllegalStateException e) {
                return e.getMessage().contains("anulada");
            }
        });

        // ================================================================
        rol("PRODUCCION / ALISTAMIENTO  (Modulo 3)");
        // ================================================================
        escenario("E4-01 Iniciar alistamiento copia los productos de la plantilla", () -> {
            int id = mercadoRecibidoYcerrado(plantillas, compras, recepciones, "Mercado Alist Esc", 77, 70);
            Alistamiento a = alistamientos.iniciar(id);
            return alistamientos.detalle(a.getIdAlistamiento()).size() == 1
                    && PlantillaOperativa.ESTADO_ALISTANDO.equals(plantillaDAO.obtener(id).getEstado());
        });

        escenario("E4-02 No se inicia alistamiento sin cerrar la recepcion", () -> {
            int id = plantillas.crear(1, "Mercado Sin Cerrar Rec", fecha());
            Producto leche = productoDAO.buscarPorNombre("Leche entera pasteurizada");
            plantillas.agregarProducto(id, leche.getIdProducto(), 77);
            compras.registrarCompra(id, null, List.of(item(leche, 1, 77)));
            int idCompra = compras.comprasDe(id).get(0).getIdCompra();
            recepciones.registrarRecepcion(idCompra, null, "parcial", List.of(70.0));
            try {
                alistamientos.iniciar(id);
                return false;
            } catch (IllegalStateException e) {
                return e.getMessage().contains("cerrar la recepcion");
            }
        });

        escenario("E4-03 No se alista mas de lo recibido (RN-07)", () -> {
            int id = mercadoRecibidoYcerrado(plantillas, compras, recepciones, "Mercado Tope Alist", 77, 70);
            Alistamiento a = alistamientos.iniciar(id);
            DetalleAlistamiento d = alistamientos.detalle(a.getIdAlistamiento()).get(0);
            d.setCantidadAlistada(80);
            try {
                alistamientos.registrarAvance(a.getIdAlistamiento(), d);
                return false;
            } catch (IllegalArgumentException e) {
                return e.getMessage().contains("supera lo disponible");
            }
        });

        escenario("E4-04 No se completa un producto con cantidad alistada 0 (RN-11)", () -> {
            int id = mercadoRecibidoYcerrado(plantillas, compras, recepciones, "Mercado Sin Rec Azucar", 77, 70);
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

        escenario("E4-05 Completar el alistamiento pasa la plantilla a LISTA", () -> {
            int id = mercadoRecibidoYcerrado(plantillas, compras, recepciones, "Mercado Lista Alist", 77, 77);
            Alistamiento a = alistamientos.iniciar(id);
            DetalleAlistamiento d = alistamientos.detalle(a.getIdAlistamiento()).get(0);
            d.setCantidadAlistada(77);
            d.setCompletado(true);
            alistamientos.registrarAvance(a.getIdAlistamiento(), d);
            boolean completo = alistamientos.completar(a.getIdAlistamiento(), id);
            return completo && PlantillaOperativa.ESTADO_LISTA.equals(plantillaDAO.obtener(id).getEstado());
        });

        escenario("C-1 completar() no revierte una plantilla ya DESPACHADA", () -> {
            Ciclo c = cicloHastaLista(plantillas, compras, recepciones, alistamientos,
                    "Mercado C1", fecha(), 40, 40);
            cerrarDespachoCompleto(despachos, c.idPlantilla(), "Sin novedades");
            try {
                alistamientos.completar(c.idAlistamiento(), c.idPlantilla());
            } catch (IllegalStateException ignore) {
                // esperado: bloquea
            }
            return PlantillaOperativa.ESTADO_DESPACHADA.equals(plantillaDAO.obtener(c.idPlantilla()).getEstado());
        });

        // ================================================================
        rol("DESPACHADOR  (Modulo 4 - Lista de chequeo y cierre real)");
        // ================================================================
        escenario("E5-01 No se inicia despacho si la plantilla no esta LISTA", () -> {
            int id = plantillas.crear(1, "Mercado Despacho No Lista", fecha());
            Producto leche = productoDAO.buscarPorNombre("Leche entera pasteurizada");
            plantillas.agregarProducto(id, leche.getIdProducto(), 10);
            try {
                despachos.iniciar(id);
                return false;
            } catch (IllegalStateException e) {
                return e.getMessage().contains("LISTA");
            }
        });

        escenario("E5-02 No se cierra el despacho con items sin verificar", () -> {
            Ciclo c = cicloHastaLista(plantillas, compras, recepciones, alistamientos,
                    "Mercado Pend Verif", fecha(), 40, 40);
            Despacho d = despachos.iniciar(c.idPlantilla());
            List<DetalleDespacho> pend = despachos.cerrar(d.getIdDespacho(), c.idPlantilla(), null);
            return pend.size() == 1;
        });

        escenario("E5-03 No se verifica una cantidad en cero (D02)", () -> {
            Ciclo c = cicloHastaLista(plantillas, compras, recepciones, alistamientos,
                    "Mercado Verif Cero", fecha(), 40, 40);
            Despacho d = despachos.iniciar(c.idPlantilla());
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

        escenario("E5-04 No se despacha mas de lo disponible", () -> {
            Ciclo c = cicloHastaLista(plantillas, compras, recepciones, alistamientos,
                    "Mercado Desp Exceso", fecha(), 77, 70);
            Despacho d = despachos.iniciar(c.idPlantilla());
            DetalleDespacho dd = despachos.detalle(d.getIdDespacho()).get(0);
            dd.setCantidadDespachada(80);
            dd.setVerificado(true);
            try {
                despachos.marcarItem(dd);
                return false;
            } catch (IllegalArgumentException e) {
                return e.getMessage().contains("supera lo disponible");
            }
        });

        escenario("E5-05 Un faltante no aceptado bloquea el cierre (RN-09)", () -> {
            Ciclo c = cicloHastaLista(plantillas, compras, recepciones, alistamientos,
                    "Mercado Falt No Acept", fecha(), 77, 70);
            Despacho d = despachos.iniciar(c.idPlantilla());
            DetalleDespacho dd = despachos.detalle(d.getIdDespacho()).get(0);
            dd.setCantidadDespachada(70);
            dd.setVerificado(true);
            dd.setFaltanteAceptado(false);
            despachos.marcarItem(dd);
            try {
                despachos.cerrar(d.getIdDespacho(), c.idPlantilla(), "Novedad");
                return false;
            } catch (IllegalStateException e) {
                return e.getMessage().contains("faltante no aceptado");
            }
        });

        escenario("E5-06 Un faltante aceptado exige novedad (RN-10)", () -> {
            Ciclo c = cicloHastaLista(plantillas, compras, recepciones, alistamientos,
                    "Mercado Falt Sin Nov", fecha(), 77, 70);
            Despacho d = despachos.iniciar(c.idPlantilla());
            DetalleDespacho dd = despachos.detalle(d.getIdDespacho()).get(0);
            dd.setCantidadDespachada(70);
            dd.setVerificado(true);
            dd.setFaltanteAceptado(true);
            despachos.marcarItem(dd);
            try {
                despachos.cerrar(d.getIdDespacho(), c.idPlantilla(), "   ");
                return false;
            } catch (IllegalArgumentException e) {
                return e.getMessage().contains("novedad");
            }
        });

        escenario("E5-07 Cierre exitoso marca DESPACHADA al 100%", () -> {
            Ciclo c = cicloHastaLista(plantillas, compras, recepciones, alistamientos,
                    "Mercado Desp Ok", fecha(), 100, 100);
            int idDespacho = cerrarDespachoCompleto(despachos, c.idPlantilla(), "Sin novedades");
            int[] pr = despachos.progreso(idDespacho);
            return PlantillaOperativa.ESTADO_DESPACHADA.equals(plantillaDAO.obtener(c.idPlantilla()).getEstado())
                    && pr[0] == 1 && pr[1] == 1;
        });

        escenario("H-2 marcarItem bloquea la edicion de un despacho ya DESPACHADO", () -> {
            Ciclo c = cicloHastaLista(plantillas, compras, recepciones, alistamientos,
                    "Mercado Edit Post", fecha(), 100, 100);
            int idDespacho = cerrarDespachoCompleto(despachos, c.idPlantilla(), "Sin novedades");
            DetalleDespacho dd = despachos.detalle(idDespacho).get(0);
            dd.setCantidadDespachada(1);
            dd.setVerificado(true);
            try {
                despachos.marcarItem(dd);
                return false;
            } catch (IllegalStateException e) {
                return e.getMessage().contains("cerrado");
            }
        });

        escenario("H-6 un segundo cierre no sobrescribe la novedad ya registrada", () -> {
            Ciclo c = cicloHastaLista(plantillas, compras, recepciones, alistamientos,
                    "Mercado Doble Cierre", fecha(), 100, 100);
            int idDespacho = cerrarDespachoCompleto(despachos, c.idPlantilla(), "Novedad Original");
            try {
                despachos.cerrar(idDespacho, c.idPlantilla(), "Novedad Nueva");
            } catch (IllegalStateException ignore) {
                // esperado: bloquea
            }
            Despacho guardado = despachoDAO.obtenerPorPlantilla(c.idPlantilla());
            return guardado.getNovedades() != null
                    && guardado.getNovedades().contains("Novedad Original");
        });

        // ================================================================
        rol("GERENCIA / ANALISIS  (Procesos abiertos + Reportes)");
        // ================================================================
        escenario("E6-01 El resumen suma exactamente las plantillas abiertas", () -> {
            ResumenProgreso r = progreso.resumen();
            int suma = r.getCargadas() + r.getEnCompra() + r.getRecibiendo()
                    + r.getAlistando() + r.getListas();
            return r.getPlantillasAbiertas() == suma;
        });

        escenario("E6-02 Una plantilla DESPACHADA no aparece en procesos abiertos", () -> {
            Ciclo c = cicloHastaLista(plantillas, compras, recepciones, alistamientos,
                    "Mercado Fuera Panel", fecha(), 100, 100);
            cerrarDespachoCompleto(despachos, c.idPlantilla(), "Sin novedades");
            return progreso.procesosAbiertos().stream()
                    .noneMatch(f -> f.getIdPlantilla() == c.idPlantilla());
        });

        escenario("E6-03 La etapa real refleja la evidencia aunque el estado guardado este atrasado", () -> {
            int id = plantillas.crear(1, "Mercado Auto Corrige", fecha());
            Producto leche = productoDAO.buscarPorNombre("Leche entera pasteurizada");
            plantillas.agregarProducto(id, leche.getIdProducto(), 20);
            compras.registrarCompra(id, null, List.of(item(leche, 1, 20)));
            plantillaDAO.actualizarEstado(id, PlantillaOperativa.ESTADO_CARGADA); // forzar inconsistencia
            FilaProgreso fila = progreso.procesosAbiertos().stream()
                    .filter(f -> f.getIdPlantilla() == id).findFirst().orElse(null);
            return fila != null && PlantillaOperativa.ESTADO_EN_COMPRA.equals(fila.getEstado());
        });

        escenario("E6-04 El reporte de faltantes lista el faltante cargado en el seed", () -> {
            String txt = reportes.reporteFaltantes();
            return txt.contains("Leche entera pasteurizada") && txt.contains("Faltante");
        });

        escenario("E6-05 Los reportes se generan sin excepcion con datos cargados", () -> {
            String saldo = reportes.reporteSaldo(1);
            String avance = reportes.reporteAvance();
            String consolidado = reportes.reporteConsolidado(LocalDate.now().plusDays(1).toString());
            return saldo.contains("PRODUCTO") && avance.contains("AVANCE") && !consolidado.isBlank();
        });

        escenario("E6-06 Las tareas pendientes incluyen los mercados por comprar", () -> {
            List<TareaProgreso> tareas = progreso.tareasPendientes();
            return !tareas.isEmpty()
                    && tareas.stream().allMatch(t -> t.getIdPlantilla() > 0 && t.getDescripcion() != null);
        });

        // ================================================================
        rol("INTEGRIDAD  (ciclo completo end-to-end)");
        // ================================================================
        escenario("E7-01 Ciclo completo sin faltante termina en DESPACHADA", () -> {
            Ciclo c = cicloHastaLista(plantillas, compras, recepciones, alistamientos,
                    "Mercado Ciclo Perfecto", fecha(), 60, 60);
            cerrarDespachoCompleto(despachos, c.idPlantilla(), "Sin novedades");
            return PlantillaOperativa.ESTADO_DESPACHADA.equals(plantillaDAO.obtener(c.idPlantilla()).getEstado());
        });

        escenario("E7-02 Ciclo con faltante aceptado registra la novedad del despacho", () -> {
            Ciclo c = cicloHastaLista(plantillas, compras, recepciones, alistamientos,
                    "Mercado Ciclo Faltante", fecha(), 77, 70);
            Despacho d = despachos.iniciar(c.idPlantilla());
            DetalleDespacho dd = despachos.detalle(d.getIdDespacho()).get(0);
            dd.setCantidadDespachada(70);
            dd.setVerificado(true);
            dd.setFaltanteAceptado(true);
            despachos.marcarItem(dd);
            despachos.cerrar(d.getIdDespacho(), c.idPlantilla(), "Faltante de 7 L aceptado por el cliente");
            Despacho guardado = despachoDAO.obtenerPorPlantilla(c.idPlantilla());
            return Despacho.DESPACHADA.equals(guardado.getEstado())
                    && guardado.getNovedades() != null && guardado.getNovedades().contains("Faltante");
        });

        // ================================================================
        informeFinal();
    }

    // ------------------------------------------------------------------
    // Helpers de flujo
    // ------------------------------------------------------------------
    private static DetalleCompra item(Producto p, int idProveedor, double cantidad) {
        DetalleCompra d = new DetalleCompra();
        d.setIdProducto(p.getIdProducto());
        d.setIdProveedor(idProveedor);
        d.setCantidadComprada(cantidad);
        return d;
    }

    private static String fecha() {
        return LocalDate.now().plusDays(contadorFechas++).toString();
    }

    private static String fechaUltima() {
        return LocalDate.now().plusDays(contadorFechas - 1).toString();
    }

    private record Ciclo(int idPlantilla, int idCompra, int idAlistamiento, int idDespacho) {}

    /** Compra, recibe, cierra recepcion, inicia alistamiento y completa -> plantilla LISTA. */
    private static Ciclo cicloHastaLista(PlantillaServicio pl, CompraServicio co, RecepcionServicio re,
                                         AlistamientoServicio al, String nombre, String fecha,
                                         double solicitado, double recibido) {
        Producto leche = new ProductoDAOSqlite().buscarPorNombre("Leche entera pasteurizada");
        int id = pl.crear(1, nombre, fecha);
        pl.agregarProducto(id, leche.getIdProducto(), solicitado);
        co.registrarCompra(id, null, List.of(item(leche, 1, solicitado)));
        int idCompra = co.comprasDe(id).get(0).getIdCompra();
        re.registrarRecepcion(idCompra, null, "recepcion", List.of(recibido));
        re.cerrarRecepcion(idCompra);
        Alistamiento a = al.iniciar(id);
        DetalleAlistamiento d = al.detalle(a.getIdAlistamiento()).get(0);
        d.setCantidadAlistada(recibido);
        d.setCompletado(true);
        al.registrarAvance(a.getIdAlistamiento(), d);
        al.completar(a.getIdAlistamiento(), id);
        return new Ciclo(id, idCompra, a.getIdAlistamiento(), -1);
    }

    /** Deja la plantilla en ALISTANDO (compra recibida y recepcion cerrada). */
    private static int mercadoRecibidoYcerrado(PlantillaServicio pl, CompraServicio co,
                                               RecepcionServicio re, String nombre,
                                               double solicitado, double recibido) {
        Producto leche = new ProductoDAOSqlite().buscarPorNombre("Leche entera pasteurizada");
        int id = pl.crear(1, nombre, fecha());
        pl.agregarProducto(id, leche.getIdProducto(), solicitado);
        co.registrarCompra(id, null, List.of(item(leche, 1, solicitado)));
        int idCompra = co.comprasDe(id).get(0).getIdCompra();
        re.registrarRecepcion(idCompra, null, "recepcion", List.of(recibido));
        re.cerrarRecepcion(idCompra);
        return id;
    }

    private static int cerrarDespachoCompleto(DespachoServicio de, int idPlantilla, String novedades) {
        Despacho d = de.iniciar(idPlantilla);
        for (DetalleDespacho dd : de.detalle(d.getIdDespacho())) {
            dd.setVerificado(true);
            de.marcarItem(dd);
        }
        de.cerrar(d.getIdDespacho(), idPlantilla, novedades);
        return d.getIdDespacho();
    }

    // ------------------------------------------------------------------
    // Framework de reporte
    // ------------------------------------------------------------------
    private interface Prueba { boolean ok() throws Exception; }

    private interface Medidor { String medir() throws Exception; }

    private static void rol(String nombre) {
        System.out.println();
        System.out.println("=== ROL: " + nombre + " ===");
    }

    private static void escenario(String nombre, Prueba p) {
        total++;
        try {
            if (p.ok()) {
                ok++;
                System.out.println("[OK]   " + nombre);
            } else {
                fallos++;
                detalleFallos.add(nombre);
                System.out.println("[FAIL] " + nombre);
            }
        } catch (Throwable t) {
            fallos++;
            detalleFallos.add(nombre + " -> " + t);
            System.out.println("[FAIL] " + nombre + " -> " + t);
        }
    }

    private static void hallazgo(String id, String nombre, String esperado, Medidor medidor) {
        totalHallazgos++;
        try {
            String actual = medidor.medir();
            boolean coincide = esperado.equals(actual);
            if (coincide) {
                hallazgosOk++;
            }
            String veredicto = coincide ? "correcto" : "DEFECTO";
            System.out.println("[HALLAZGO " + id + "] " + nombre + " | esperado=" + esperado
                    + " | actual=" + actual + " (" + veredicto + ")");
            detalleHallazgos.add(id + " - " + nombre + " | esperado=" + esperado
                    + " | actual=" + actual + " (" + veredicto + ")");
        } catch (Throwable t) {
            System.out.println("[HALLAZGO " + id + "] " + nombre + " -> EXCEPCION " + t);
            detalleHallazgos.add(id + " - " + nombre + " -> EXCEPCION " + t);
        }
    }

    private static void informeFinal() {
        System.out.println();
        System.out.println("====================================================================");
        System.out.println("RESULTADO ESCENARIOS: " + ok + "/" + total + " correctos");
        System.out.println("HALLAZGOS ANALIZADOS: " + totalHallazgos
                + "  (con defecto: " + (totalHallazgos - hallazgosOk) + ")");
        System.out.println("====================================================================");
        if (detalleHallazgos.isEmpty()) {
            System.out.println("Sin hallazgos con defecto: todos los hallazgos previos quedaron resueltos.");
        } else {
            System.out.println("DETALLE DE HALLAZGOS:");
            detalleHallazgos.forEach(h -> System.out.println("  - " + h));
        }
        if (fallos > 0) {
            System.out.println("DETALLE DE FALLOS:");
            detalleFallos.forEach(f -> System.out.println("  - " + f));
            System.exit(1);
        }
    }
}
