package agromedio.pruebas;

import agromedio.datos.ConexionBD;
import agromedio.datos.ProductoDAO;
import agromedio.datos.ProductoDAOSqlite;
import agromedio.logica.AlistamientoServicio;
import agromedio.logica.CompraServicio;
import agromedio.logica.PlantillaServicio;
import agromedio.logica.RecepcionServicio;
import agromedio.modelo.Alistamiento;
import agromedio.modelo.Compra;
import agromedio.modelo.DetalleAlistamiento;
import agromedio.modelo.DetalleCompra;
import agromedio.modelo.DetalleRecepcion;
import agromedio.modelo.PlantillaOperativa;
import agromedio.modelo.Producto;
import agromedio.presentacion.CatalogoClientes;
import agromedio.presentacion.FrmAlistamiento;
import agromedio.presentacion.FrmCatalogos;
import agromedio.presentacion.FrmCompras;
import agromedio.presentacion.FrmDespacho;
import agromedio.presentacion.FrmPlantillas;
import agromedio.presentacion.FrmProductos;
import agromedio.presentacion.FrmProgreso;
import agromedio.presentacion.FrmRecepcion;
import agromedio.presentacion.FrmReportes;
import agromedio.presentacion.Tema;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JInternalFrame;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Window;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

/**
 * Flujos de interfaz en tiempo real (Swing). Abre las ventanas reales sobre una
 * base de datos preparada, localiza sus componentes, acciona botones y verifica
 * que las tablas/etiquetas reflejen el estado esperado. Los dialogos modales se
 * cierran automaticamente para no bloquear la simulacion.
 *
 * Ejecutar: java -cp build/classes;lib/* agromedio.pruebas.PruebaFlujosUI
 */
public class PruebaFlujosUI {

    private static int total = 0;
    private static int ok = 0;
    private static int fallos = 0;
    private static final List<String> detalleFallos = new ArrayList<>();
    private static volatile boolean activo = true;

    public static void main(String[] args) throws Exception {
        Path bd = Paths.get("build", "prueba_ui.db");
        Files.createDirectories(bd.toAbsolutePath().getParent());
        Files.deleteIfExists(bd);
        ConexionBD.fijarRuta(bd.toString());
        ConexionBD.inicializar();
        Tema.instalar();

        int idAlistando = prepararAlistando();
        int idLista = prepararLista();

        iniciarDismisser();

        escenarioUI("UI-01 Compras: combo de plantillas y consolidado operativos", () -> {
            FrmCompras f = new FrmCompras();
            try {
                JComboBox<?> combo = buscar(f.getContentPane(), JComboBox.class, c -> c.getItemCount() > 0);
                JTable tabla = tablaDeColumnas(f.getContentPane(), 6);
                JButton actualizar = boton(f.getContentPane(), "Actualizar consolidado");
                if (actualizar != null) actualizar.doClick();
                return combo != null && tabla != null && tabla.getRowCount() >= 1;
            } finally {
                f.dispose();
            }
        });

        escenarioUI("UI-02 Recepcion: lista las compras pendientes con su detalle", () -> {
            FrmRecepcion f = new FrmRecepcion();
            try {
                JComboBox<?> combo = buscar(f.getContentPane(), JComboBox.class,
                        c -> c.getItemCount() > 0 && c.getItemAt(0) instanceof Compra);
                JTable tabla = tablaDeColumnas(f.getContentPane(), 7);
                return combo != null && tabla != null && tabla.getRowCount() >= 1;
            } finally {
                f.dispose();
            }
        });

        int idCompraSeed = compraPendienteSeed();
        escenarioUI("UI-03 Recepcion: registrar una entrega descuenta el faltante", () -> {
            FrmRecepcion f = new FrmRecepcion();
            try {
                JTable tabla = tablaDeColumnas(f.getContentPane(), 7);
                JButton registrar = boton(f.getContentPane(), "Registrar entrega");
                if (tabla == null || registrar == null || tabla.getRowCount() == 0) {
                    return false;
                }
                tabla.setValueAt(1.0, 0, 6);
                registrar.doClick();
                List<DetalleRecepcion> estado = new RecepcionServicio().estadoRecepcion(idCompraSeed);
                return estado.stream().anyMatch(d -> d.getCantidadRecibida() > 0);
            } finally {
                f.dispose();
            }
        });

        escenarioUI("UI-04 Alistamiento: iniciar puebla la tabla de fraccionamiento", () -> {
            FrmAlistamiento f = new FrmAlistamiento();
            try {
                JComboBox<?> combo = comboPlantilla(f.getContentPane(), idAlistando);
                JButton iniciar = boton(f.getContentPane(), "Iniciar alistamiento");
                if (combo == null || iniciar == null) {
                    return false;
                }
                iniciar.doClick();
                JTable tabla = tablaDeColumnas(f.getContentPane(), 7);
                return tabla != null && tabla.getRowCount() >= 1;
            } finally {
                f.dispose();
            }
        });

        escenarioUI("UI-05 Despacho: abrir la lista de chequeo del mercado LISTA", () -> {
            FrmDespacho f = new FrmDespacho();
            try {
                JComboBox<?> combo = comboPlantilla(f.getContentPane(), idLista);
                JButton abrir = boton(f.getContentPane(), "Abrir lista");
                if (combo == null || abrir == null) {
                    return false;
                }
                abrir.doClick();
                JTable tabla = tablaDeColumnas(f.getContentPane(), 6);
                return tabla != null && tabla.getRowCount() >= 1;
            } finally {
                f.dispose();
            }
        });

        escenarioUI("UI-06 Plantillas: la tabla lista las plantillas operativas", () -> {
            FrmPlantillas f = new FrmPlantillas();
            try {
                JTable tabla = tablaDeColumnas(f.getContentPane(), 4);
                return tabla != null && tabla.getRowCount() >= 1;
            } finally {
                f.dispose();
            }
        });

        escenarioUI("UI-07 Productos: la tabla lista el catalogo de productos", () -> {
            FrmProductos f = new FrmProductos();
            try {
                List<JTable> tablas = new ArrayList<>();
                recolectar(f.getContentPane(), JTable.class, t -> true, tablas);
                return tablas.stream().anyMatch(t -> t.getRowCount() >= 1);
            } finally {
                f.dispose();
            }
        });

        escenarioUI("UI-08 Catalogos: la tabla lista los clientes", () -> {
            FrmCatalogos f = new FrmCatalogos(new CatalogoClientes());
            try {
                List<JTable> tablas = new ArrayList<>();
                recolectar(f.getContentPane(), JTable.class, t -> true, tablas);
                return tablas.stream().anyMatch(t -> t.getRowCount() >= 1);
            } finally {
                f.dispose();
            }
        });

        escenarioUI("UI-09 Reportes: 'Ver faltantes' genera el texto del reporte", () -> {
            FrmReportes f = new FrmReportes();
            try {
                JButton ver = boton(f.getContentPane(), "Ver faltantes");
                if (ver == null) {
                    return false;
                }
                ver.doClick();
                JTextArea area = buscar(f.getContentPane(), JTextArea.class, a -> true);
                return area != null && area.getText().contains("FALTANTES");
            } finally {
                f.dispose();
            }
        });

        // FrmProgreso carga en SwingWorker: se espera sin bloquear el EDT.
        FrmProgreso[] progreso = new FrmProgreso[1];
        SwingUtilities.invokeAndWait(() -> progreso[0] = new FrmProgreso());
        Thread.sleep(1500);
        AtomicInteger filasProgreso = new AtomicInteger(-1);
        SwingUtilities.invokeAndWait(() -> {
            List<JTable> tablas = new ArrayList<>();
            recolectar(progreso[0].getContentPane(), JTable.class, t -> true, tablas);
            filasProgreso.set(tablas.stream().mapToInt(JTable::getRowCount).max().orElse(-1));
        });
        SwingUtilities.invokeAndWait(() -> progreso[0].dispose());
        verificar("UI-10 Progreso: la tabla de seguimiento se puebla tras el SwingWorker",
                filasProgreso.get() >= 1);

        activo = false;
        System.out.println();
        System.out.println("====================================================================");
        System.out.println("RESULTADO FLUJOS UI: " + ok + "/" + total + " correctos");
        System.out.println("====================================================================");
        if (fallos > 0) {
            detalleFallos.forEach(f -> System.out.println("  - " + f));
            System.exit(1);
        }
        System.exit(0);
    }

    // ------------------------------------------------------------------
    // Datos de apoyo
    // ------------------------------------------------------------------
    private static Producto leche() {
        return new ProductoDAOSqlite().buscarPorNombre("Leche entera pasteurizada");
    }

    private static DetalleCompra item(Producto p, double cantidad) {
        DetalleCompra d = new DetalleCompra();
        d.setIdProducto(p.getIdProducto());
        d.setIdProveedor(1);
        d.setCantidadComprada(cantidad);
        return d;
    }

    private static int prepararAlistando() {
        PlantillaServicio pl = new PlantillaServicio();
        CompraServicio co = new CompraServicio();
        RecepcionServicio re = new RecepcionServicio();
        Producto leche = leche();
        int id = pl.crear(1, "UI Mercado Alistando", LocalDate.now().plusDays(80).toString());
        pl.agregarProducto(id, leche.getIdProducto(), 50);
        co.registrarCompra(id, null, List.of(item(leche, 50)));
        int idCompra = co.comprasDe(id).get(0).getIdCompra();
        re.registrarRecepcion(idCompra, null, "ui", List.of(40.0));
        re.cerrarRecepcion(idCompra);
        return id;
    }

    private static int prepararLista() {
        PlantillaServicio pl = new PlantillaServicio();
        CompraServicio co = new CompraServicio();
        RecepcionServicio re = new RecepcionServicio();
        AlistamientoServicio al = new AlistamientoServicio();
        Producto leche = leche();
        int id = pl.crear(1, "UI Mercado Lista", LocalDate.now().plusDays(81).toString());
        pl.agregarProducto(id, leche.getIdProducto(), 60);
        co.registrarCompra(id, null, List.of(item(leche, 60)));
        int idCompra = co.comprasDe(id).get(0).getIdCompra();
        re.registrarRecepcion(idCompra, null, "ui", List.of(60.0));
        re.cerrarRecepcion(idCompra);
        Alistamiento a = al.iniciar(id);
        DetalleAlistamiento d = al.detalle(a.getIdAlistamiento()).get(0);
        d.setCantidadAlistada(60);
        d.setCompletado(true);
        al.registrarAvance(a.getIdAlistamiento(), d);
        al.completar(a.getIdAlistamiento(), id);
        return id;
    }

    private static int compraPendienteSeed() {
        Compra c = new RecepcionServicio().comprasPendientes().get(0);
        return c.getIdCompra();
    }

    // ------------------------------------------------------------------
    // Utilidades de busqueda de componentes
    // ------------------------------------------------------------------
    private static <T extends Component> void recolectar(Container raiz, Class<T> tipo,
                                                         Predicate<T> filtro, List<T> salida) {
        for (Component c : raiz.getComponents()) {
            if (tipo.isInstance(c) && filtro.test(tipo.cast(c))) {
                salida.add(tipo.cast(c));
            }
            if (c instanceof Container cont) {
                recolectar(cont, tipo, filtro, salida);
            }
        }
    }

    private static <T extends Component> T buscar(Container raiz, Class<T> tipo, Predicate<T> filtro) {
        List<T> lista = new ArrayList<>();
        recolectar(raiz, tipo, filtro, lista);
        return lista.isEmpty() ? null : lista.get(0);
    }

    private static JButton boton(Container raiz, String texto) {
        return buscar(raiz, JButton.class, b -> b.getText() != null
                && b.getText().toLowerCase().contains(texto.toLowerCase()));
    }

    private static JTable tablaDeColumnas(Container raiz, int columnas) {
        return buscar(raiz, JTable.class, t -> t.getColumnCount() == columnas);
    }

    private static JComboBox<?> comboPlantilla(Container raiz, int idPlantilla) {
        JComboBox<?> combo = buscar(raiz, JComboBox.class,
                c -> c.getItemCount() > 0 && c.getItemAt(0) instanceof PlantillaOperativa);
        if (combo == null) {
            return null;
        }
        for (int i = 0; i < combo.getItemCount(); i++) {
            PlantillaOperativa p = (PlantillaOperativa) combo.getItemAt(i);
            if (p.getIdPlantilla() == idPlantilla) {
                combo.setSelectedIndex(i);
                break;
            }
        }
        return combo;
    }

    // ------------------------------------------------------------------
    // Auto-cierre de dialogos modales (OK / Si)
    // ------------------------------------------------------------------
    private static void iniciarDismisser() {
        Thread hilo = new Thread(() -> {
            while (activo) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    return;
                }
                for (Window w : Window.getWindows()) {
                    if (w instanceof Dialog d && d.isVisible()) {
                        SwingUtilities.invokeLater(() -> cerrar(d));
                    }
                }
            }
        });
        hilo.setDaemon(true);
        hilo.start();
    }

    private static final java.util.Set<Dialog> CERRADOS =
            java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());

    private static void cerrar(Dialog d) {
        if (!CERRADOS.add(d)) {
            return;
        }
        List<JButton> botones = new ArrayList<>();
        recolectar(d, JButton.class, b -> true, botones);
        JButton elegido = coincidencia(botones, "sí", "si", "yes");
        if (elegido == null) {
            elegido = coincidencia(botones, "aceptar", "ok", "accept");
        }
        if (elegido != null) {
            elegido.doClick();
        } else {
            d.dispose();
        }
    }

    private static JButton coincidencia(List<JButton> botones, String... textos) {
        for (String buscado : textos) {
            for (JButton b : botones) {
                String t = b.getText() == null ? "" : b.getText().trim();
                if (t.equalsIgnoreCase(buscado)) {
                    return b;
                }
            }
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Framework de reporte
    // ------------------------------------------------------------------
    private interface UiPrueba { boolean correr() throws Exception; }

    private static void escenarioUI(String nombre, UiPrueba prueba) {
        AtomicReference<Boolean> resultado = new AtomicReference<>(false);
        AtomicReference<Throwable> error = new AtomicReference<>();
        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    resultado.set(prueba.correr());
                } catch (Throwable t) {
                    error.set(t);
                }
            });
        } catch (Throwable t) {
            error.set(t);
        }
        if (error.get() != null) {
            verificar(nombre + " -> " + error.get(), false);
        } else {
            verificar(nombre, resultado.get());
        }
    }

    private static void verificar(String nombre, boolean resultado) {
        total++;
        if (resultado) {
            ok++;
            System.out.println("[OK]   " + nombre);
        } else {
            fallos++;
            detalleFallos.add(nombre);
            System.out.println("[FAIL] " + nombre);
        }
    }
}
