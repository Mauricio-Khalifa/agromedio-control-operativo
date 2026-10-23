package agromedio.presentacion;

import agromedio.datos.ConexionBD;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/** Shell principal: navegacion lateral, escritorio de modulos y contexto operativo. */
public class FrmPrincipal extends JFrame {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final JDesktopPane escritorio = new JDesktopPane();
    private final Navegador navegador = new Navegador(escritorio);
    private final ContextoOperativo contexto = new ContextoOperativo();
    private final JLabel lblContexto = new JLabel("Sin módulo activo");
    private final JLabel lblActividad = new JLabel("Listo");
    private final JLabel lblHora = new JLabel();
    private final Map<String, JButton> botonesMenu = new HashMap<>();
    private String moduloActivo = "";

    public FrmPrincipal() {
        super("AGROMEDIO | Control Operativo");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1180, 740));
        setSize(1420, 880);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Tema.FONDO);

        setJMenuBar(crearMenu());
        configurarContexto();
        escritorio.setBackground(Tema.FONDO);
        escritorio.setBorder(BorderFactory.createLineBorder(Tema.BORDE));

        JPanel raiz = new JPanel(new BorderLayout());
        raiz.setBackground(Tema.FONDO);
        raiz.add(crearBarraSuperior(), BorderLayout.NORTH);
        raiz.add(crearSidebar(), BorderLayout.WEST);
        raiz.add(crearCentro(), BorderLayout.CENTER);
        raiz.add(crearBarraEstado(), BorderLayout.SOUTH);
        add(raiz, BorderLayout.CENTER);

        Timer reloj = new Timer(30_000, e -> actualizarHora());
        reloj.start();
        actualizarHora();
        setLocationRelativeTo(null);
    }

    private void configurarContexto() {
        navegador.setAlAbrir(titulo -> {
            contexto.setModuloActual(titulo);
            lblActividad.setText("Módulo abierto: " + titulo);
        });
        navegador.setAlCerrar(clave -> {
            if (clave.equals(moduloActivo)) {
                marcarActivo("");
                contexto.setModuloActual(null);
                lblActividad.setText("Listo");
            }
        });
        contexto.addPropertyChangeListener(evt -> {
            if (ContextoOperativo.MODULO_ACTUAL.equals(evt.getPropertyName())) {
                lblContexto.setText("Módulo activo: " + evt.getNewValue());
            }
        });
    }

    private JPanel crearBarraSuperior() {
        JPanel barra = new JPanel(new BorderLayout(16, 0));
        barra.setBackground(Tema.SUPERFICIE);
        barra.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Tema.BORDE),
                new EmptyBorder(10, 18, 10, 18)));

        JPanel marca = new JPanel();
        marca.setLayout(new BoxLayout(marca, BoxLayout.Y_AXIS));
        marca.setOpaque(false);
        JLabel nombre = new JLabel("AGROMEDIO");
        nombre.setFont(Tema.fuente(Font.BOLD, 20));
        nombre.setForeground(Tema.PRIMARIO_OSCURO);
        JLabel descripcion = new JLabel("Control operativo | Asociación Agropecuaria y Ambiental del Magdalena Medio");
        descripcion.setFont(Tema.PEQUENA);
        descripcion.setForeground(Tema.TEXTO_SUAVE);
        marca.add(nombre);
        marca.add(descripcion);

        JPanel derecha = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        derecha.setOpaque(false);
        lblContexto.setFont(Tema.PEQUENA.deriveFont(Font.BOLD));
        lblContexto.setForeground(Tema.PRIMARIO_OSCURO);
        lblContexto.setOpaque(true);
        lblContexto.setBackground(Tema.PRIMARIO_SUAVE);
        lblContexto.setBorder(new EmptyBorder(6, 10, 6, 10));
        lblHora.setFont(Tema.PEQUENA);
        lblHora.setForeground(Tema.TEXTO_SUAVE);
        derecha.add(lblContexto);
        derecha.add(lblHora);

        barra.add(marca, BorderLayout.WEST);
        barra.add(derecha, BorderLayout.EAST);
        return barra;
    }

    private JPanel crearSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(Tema.SIDEBAR);
        sidebar.setPreferredSize(new Dimension(236, 0));

        JPanel menu = new JPanel();
        menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));
        menu.setBackground(Tema.SIDEBAR);
        menu.setBorder(new EmptyBorder(12, 10, 12, 10));

        agregarGrupo(menu, "Operación");
        menu.add(botonMenu("Carga de archivo", "carga", FrmCargaExcel::new));
        menu.add(botonMenu("Plantillas", "plantillas", () -> {
            FrmPlantillas frame = new FrmPlantillas();
            frame.setNavegador(navegador);
            return frame;
        }));
        menu.add(botonMenu("Compras", "compras", FrmCompras::new));
        menu.add(botonMenu("Recepción", "recepcion", FrmRecepcion::new));
        menu.add(botonMenu("Alistamiento", "alistamiento", FrmAlistamiento::new));
        menu.add(botonMenu("Despacho", "despacho", FrmDespacho::new));

        menu.add(Box.createVerticalStrut(4));
        menu.add(new JSeparator(SwingConstants.HORIZONTAL));
        menu.add(Box.createVerticalStrut(4));
        agregarGrupo(menu, "Catálogos");
        menu.add(botonMenu("Productos", "productos", FrmProductos::new));
        menu.add(botonMenu("Clientes", "cat-clientes", () -> new FrmCatalogos(new CatalogoClientes())));
        menu.add(botonMenu("Categorías", "cat-categorias", () -> new FrmCatalogos(new CatalogoCategorias())));
        menu.add(botonMenu("Unidades", "cat-unidades", () -> new FrmCatalogos(new CatalogoUnidades())));
        menu.add(botonMenu("Proveedores", "cat-proveedores", () -> new FrmCatalogos(new CatalogoProveedores())));

        menu.add(Box.createVerticalStrut(4));
        menu.add(new JSeparator(SwingConstants.HORIZONTAL));
        menu.add(Box.createVerticalStrut(4));
        agregarGrupo(menu, "Análisis");
        menu.add(botonMenu("Reportes", "reportes", FrmReportes::new));
        menu.add(Box.createVerticalGlue());

        JScrollPane scroll = new JScrollPane(menu);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Tema.SIDEBAR);
        scroll.getVerticalScrollBar().setUnitIncrement(14);
        sidebar.add(scroll, BorderLayout.CENTER);

        JLabel version = new JLabel("Operador único | v1.0");
        version.setFont(Tema.PEQUENA);
        version.setForeground(new Color(156, 181, 164));
        version.setBorder(new EmptyBorder(8, 18, 12, 18));
        sidebar.add(version, BorderLayout.SOUTH);
        return sidebar;
    }

    private void agregarGrupo(JPanel menu, String titulo) {
        JLabel grupo = new JLabel(titulo.toUpperCase());
        grupo.setFont(Tema.PEQUENA.deriveFont(Font.BOLD));
        grupo.setForeground(new Color(156, 181, 164));
        grupo.setBorder(new EmptyBorder(14, 8, 5, 8));
        grupo.setAlignmentX(Component.LEFT_ALIGNMENT);
        menu.add(grupo);
    }

    private JButton botonMenu(String texto, String clave, Supplier<? extends javax.swing.JInternalFrame> fabrica) {
        JButton boton = new JButton(texto);
        boton.setFont(Tema.BASE);
        boton.setForeground(Tema.SIDEBAR_TEXTO);
        boton.setBackground(Tema.SIDEBAR);
        boton.setOpaque(true);
        boton.setFocusPainted(false);
        boton.setBorder(new EmptyBorder(9, 14, 9, 14));
        boton.setHorizontalAlignment(SwingConstants.LEFT);
        boton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        boton.setAlignmentX(Component.LEFT_ALIGNMENT);
        boton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        boton.addActionListener(e -> abrirModulo(clave, fabrica));
        boton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                boton.setBackground(Tema.SIDEBAR_HOVER);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                boton.setBackground(clave.equals(moduloActivo) ? Tema.SIDEBAR_HOVER : Tema.SIDEBAR);
            }
        });
        botonesMenu.put(clave, boton);
        return boton;
    }

    private void marcarActivo(String clave) {
        moduloActivo = clave;
        for (Map.Entry<String, JButton> entrada : botonesMenu.entrySet()) {
            JButton boton = entrada.getValue();
            boolean activo = entrada.getKey().equals(clave);
            boton.setBackground(activo ? Tema.SIDEBAR_HOVER : Tema.SIDEBAR);
            boton.setFont(activo ? Tema.BASE_NEGRITA : Tema.BASE);
            boton.setBorder(activo
                    ? BorderFactory.createCompoundBorder(
                            BorderFactory.createMatteBorder(0, 3, 0, 0, Tema.ACENTO),
                            new EmptyBorder(9, 11, 9, 14))
                    : new EmptyBorder(9, 14, 9, 14));
        }
    }

    private JPanel crearCentro() {
        JPanel centro = new JPanel(new BorderLayout());
        centro.setBackground(Tema.FONDO);
        centro.setBorder(new EmptyBorder(12, 12, 12, 12));
        centro.add(escritorio, BorderLayout.CENTER);
        return centro;
    }

    private JPanel crearBarraEstado() {
        JPanel barra = new JPanel(new BorderLayout());
        barra.setBackground(Tema.SUPERFICIE);
        barra.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Tema.BORDE),
                new EmptyBorder(5, 14, 5, 14)));

        JPanel izquierda = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        izquierda.setOpaque(false);
        JLabel punto = new JLabel("●");
        punto.setForeground(Tema.EXITO);
        punto.setFont(Tema.PEQUENA);
        JLabel base = new JLabel("Base local conectada");
        base.setFont(Tema.PEQUENA);
        base.setForeground(Tema.TEXTO_SUAVE);
        base.setToolTipText(ConexionBD.rutaBD());
        lblActividad.setFont(Tema.PEQUENA);
        lblActividad.setForeground(Tema.TEXTO_SUAVE);
        izquierda.add(punto);
        izquierda.add(base);
        izquierda.add(new JLabel("  |  "));
        izquierda.add(lblActividad);

        JPanel derecha = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        derecha.setOpaque(false);
        JLabel modo = new JLabel("Operador único | sin autenticación");
        modo.setFont(Tema.PEQUENA);
        modo.setForeground(Tema.TEXTO_SUAVE);
        derecha.add(modo);

        barra.add(izquierda, BorderLayout.WEST);
        barra.add(derecha, BorderLayout.EAST);
        return barra;
    }

    private JMenuBar crearMenu() {
        JMenuBar barra = new JMenuBar();

        JMenu mOperacion = new JMenu("Operación");
        mOperacion.add(item("Plantillas operativas", e -> abrirModulo("plantillas", () -> {
            FrmPlantillas frame = new FrmPlantillas();
            frame.setNavegador(navegador);
            return frame;
        })));
        mOperacion.add(item("Carga desde archivo (Módulo 0)",
                e -> abrirModulo("carga", FrmCargaExcel::new)));
        mOperacion.add(item("Compras (Módulo 1)", e -> abrirModulo("compras", FrmCompras::new)));
        mOperacion.add(item("Recepción (Módulo 2)", e -> abrirModulo("recepcion", FrmRecepcion::new)));
        mOperacion.add(item("Alistamiento / Producción (Módulo 3)",
                e -> abrirModulo("alistamiento", FrmAlistamiento::new)));
        mOperacion.add(item("Despacho (Módulo 4)", e -> abrirModulo("despacho", FrmDespacho::new)));

        JMenu mCatalogos = new JMenu("Catálogos");
        mCatalogos.add(item("Clientes", e -> abrirModulo("cat-clientes",
                () -> new FrmCatalogos(new CatalogoClientes()))));
        mCatalogos.add(item("Categorías", e -> abrirModulo("cat-categorias",
                () -> new FrmCatalogos(new CatalogoCategorias()))));
        mCatalogos.add(item("Unidades de medida", e -> abrirModulo("cat-unidades",
                () -> new FrmCatalogos(new CatalogoUnidades()))));
        mCatalogos.add(item("Proveedores", e -> abrirModulo("cat-proveedores",
                () -> new FrmCatalogos(new CatalogoProveedores()))));
        mCatalogos.add(item("Productos", e -> abrirModulo("productos", FrmProductos::new)));

        JMenu mConsultas = new JMenu("Consultas y reportes");
        mConsultas.add(item("Reportes operativos", e -> abrirModulo("reportes", FrmReportes::new)));

        JMenu mVentana = new JMenu("Ventana");
        mVentana.add(item("Cerrar todas las ventanas", e -> navegador.cerrarTodo()));

        JMenu mAyuda = new JMenu("Ayuda");
        mAyuda.add(item("Acerca del sistema", e -> UiUtil.mensaje(this,
                "Sistema de Control Operativo Agromedio\n"
                        + "Proyecto Integrador - UT Santander 2026\n"
                        + "Materias: Planeación de Sistemas Informáticos, "
                        + "Motores de Bases de Datos y Programación Orientada a Objetos.\n"
                        + "Base de datos: SQLite (" + ConexionBD.rutaBD() + ")\n"
                        + "Modo de operación: operador único, sin autenticación en esta versión.")));

        barra.add(mOperacion);
        barra.add(mCatalogos);
        barra.add(mConsultas);
        barra.add(mVentana);
        barra.add(mAyuda);
        return barra;
    }

    private JMenuItem item(String texto, ActionListener accion) {
        JMenuItem i = new JMenuItem(texto);
        i.setFont(Tema.BASE);
        i.addActionListener(accion);
        return i;
    }

    private void abrirModulo(String clave, Supplier<? extends javax.swing.JInternalFrame> fabrica) {
        marcarActivo(clave);
        navegador.abrir(clave, fabrica);
    }

    private void actualizarHora() {
        lblHora.setText(LocalTime.now().format(HORA));
    }

    /** Abre ventanas de ejemplo (uso: capturas de pantalla y demostracion). */
    public void abrirDemostracion() {
        SwingUtilities.invokeLater(() -> {
            abrirModulo("plantillas", () -> {
                FrmPlantillas frame = new FrmPlantillas();
                frame.setNavegador(navegador);
                return frame;
            });
            abrirModulo("compras", FrmCompras::new);
            abrirModulo("reportes", FrmReportes::new);
        });
    }
}
