package agromedio.presentacion;

import agromedio.datos.PlantillaDAO;
import agromedio.datos.PlantillaDAOSqlite;
import agromedio.datos.ProductoDAO;
import agromedio.datos.ProductoDAOSqlite;
import agromedio.logica.AlistamientoServicio;
import agromedio.modelo.Alistamiento;
import agromedio.modelo.DetalleAlistamiento;
import agromedio.modelo.PlantillaOperativa;
import agromedio.modelo.Producto;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Modulo 3 - Produccion / Alistamiento: fraccionamiento (RF07) y avance (RF08). */
public class FrmAlistamiento extends JInternalFrame {

    private final AlistamientoServicio servicio = new AlistamientoServicio();
    private final PlantillaDAO plantillaDAO = new PlantillaDAOSqlite();
    private final ProductoDAO productoDAO = new ProductoDAOSqlite();

    private final JComboBox<PlantillaOperativa> cmbPlantilla = new JComboBox<>();
    private final JLabel lblEstado = new JLabel("Sin alistamiento");
    private final JLabel lblResumen = UiUtil.subtitulo("Seleccione un mercado para iniciar.");
    private final Map<Integer, Producto> productos = new HashMap<>();

    private DefaultTableModel modelo;
    private List<DetalleAlistamiento> detalles = List.of();
    private Alistamiento alistamientoActual;
    private boolean cargando = false;

    public FrmAlistamiento() {
        super("Alistamiento / Producción", true, true, true, true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1120, 680);
        setLayout(new BorderLayout(8, 8));
        getContentPane().setBackground(Tema.FONDO);

        for (Producto p : productoDAO.listar()) {
            productos.put(p.getIdProducto(), p);
        }

        modelo = new DefaultTableModel(
                new Object[]{"Producto", "Solicitado", "Recibido", "U.",
                        "Equivalente U. compra", "Alistada", "Completado"}, 0) {
            @Override
            public boolean isCellEditable(int f, int c) {
                return (c == 5 || c == 6) && alistamientoActual != null
                        && !Alistamiento.COMPLETO.equals(alistamientoActual.getEstado());
            }

            @Override
            public Class<?> getColumnClass(int c) {
                return c == 6 ? Boolean.class : Object.class;
            }
        };
        JTable tabla = new JTable(modelo);
        UiUtil.prepararTabla(tabla);
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createLineBorder(Tema.BORDE));
        add(scroll, BorderLayout.CENTER);

        JPanel norte = new JPanel(new BorderLayout(8, 6));
        norte.setOpaque(false);
        norte.setBorder(BorderFactory.createEmptyBorder(8, 10, 2, 10));
        norte.add(UiUtil.encabezado("Alistamiento y producción",
                "Fraccione lo recibido y marque cada producto. No se puede alistar más de lo disponible."),
                BorderLayout.NORTH);

        JPanel barra = new JPanel(new GridBagLayout());
        barra.setBackground(Tema.SUPERFICIE);
        barra.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Tema.BORDE),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 6, 4, 6);
        g.anchor = GridBagConstraints.WEST;
        g.gridx = 0; g.gridy = 0;
        barra.add(new JLabel("Mercado"), g);
        g.gridx = 1; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL;
        cmbPlantilla.setPreferredSize(new java.awt.Dimension(380, 34));
        barra.add(cmbPlantilla, g);
        g.gridx = 2; g.weightx = 0; g.fill = GridBagConstraints.NONE;
        JButton btnIniciar = UiUtil.botonSecundario("Iniciar alistamiento");
        barra.add(btnIniciar, g);
        g.gridx = 3;
        lblEstado.setFont(Tema.PEQUENA.deriveFont(java.awt.Font.BOLD));
        lblEstado.setForeground(Tema.PRIMARIO_OSCURO);
        barra.add(lblEstado, g);
        norte.add(barra, BorderLayout.CENTER);
        add(norte, BorderLayout.NORTH);

        JButton btnGuardar = UiUtil.botonPrimario("Guardar avance");
        JButton btnCompletar = UiUtil.botonSecundario("Completar alistamiento");
        JPanel sur = new JPanel(new BorderLayout(8, 0));
        sur.setOpaque(false);
        sur.setBorder(BorderFactory.createEmptyBorder(4, 10, 8, 10));
        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        acciones.setOpaque(false);
        acciones.add(btnGuardar);
        acciones.add(btnCompletar);
        sur.add(acciones, BorderLayout.WEST);
        sur.add(lblResumen, BorderLayout.EAST);
        add(sur, BorderLayout.SOUTH);

        cmbPlantilla.addActionListener(e -> cargar());
        btnIniciar.addActionListener(e -> iniciar());
        btnGuardar.addActionListener(e -> guardar());
        btnCompletar.addActionListener(e -> completar());

        cargarPlantillas();
        UiUtil.fuenteRaiz(this);
    }

    private void cargarPlantillas() {
        cmbPlantilla.removeAllItems();
        for (PlantillaOperativa p : plantillaDAO.listarPorEstado(
                PlantillaOperativa.ESTADO_RECIBIENDO,
                PlantillaOperativa.ESTADO_ALISTANDO,
                PlantillaOperativa.ESTADO_LISTA)) {
            cmbPlantilla.addItem(p);
        }
        if (cmbPlantilla.getItemCount() == 0) {
            lblEstado.setText("No hay plantillas en recepción/alistamiento.");
        }
    }

    private void iniciar() {
        try {
            PlantillaOperativa p = (PlantillaOperativa) cmbPlantilla.getSelectedItem();
            if (p == null) {
                throw new IllegalArgumentException("Seleccione un mercado.");
            }
            alistamientoActual = servicio.iniciar(p.getIdPlantilla());
            cargar();
        } catch (Exception e) {
            UiUtil.error(this, e);
        }
    }

    private void cargar() {
        cargando = true;
        modelo.setRowCount(0);
        PlantillaOperativa p = (PlantillaOperativa) cmbPlantilla.getSelectedItem();
        if (p == null) {
            cargando = false;
            return;
        }
        alistamientoActual = servicio.actual(p.getIdPlantilla());
        if (alistamientoActual == null) {
            lblEstado.setText("Estado: SIN INICIAR - presione 'Iniciar alistamiento'");
            cargando = false;
            return;
        }
        lblEstado.setText("Estado: " + alistamientoActual.getEstado());
        detalles = servicio.detalle(alistamientoActual.getIdAlistamiento());
        for (DetalleAlistamiento d : detalles) {
            Producto pr = productos.get(d.getIdProducto());
            String equivalente = pr == null ? "" :
                    String.format("%.2f %s", d.getCantidadSolicitada() / pr.getFactorConversion(),
                            pr.getAbreviaturaCompra());
            modelo.addRow(new Object[]{d.getNombreProducto(),
                    d.getCantidadSolicitada(), d.getCantidadRecibida(), d.getAbreviatura(),
                    equivalente, d.getCantidadAlistada(), d.isCompletado()});
        }
        int completos = 0;
        for (DetalleAlistamiento d : detalles) {
            if (d.isCompletado()) {
                completos++;
            }
        }
        lblResumen.setText(detalles.size() + " productos  |  " + completos + " completados");
        cargando = false;
    }

    private void guardar() {
        try {
            if (alistamientoActual == null) {
                throw new IllegalStateException("Primero inicie el alistamiento.");
            }
            for (int i = 0; i < modelo.getRowCount(); i++) {
                DetalleAlistamiento d = detalles.get(i);
                Object cant = modelo.getValueAt(i, 5);
                Object comp = modelo.getValueAt(i, 6);
                d.setCantidadAlistada(valor(cant));
                d.setCompletado(Boolean.TRUE.equals(comp));
                servicio.registrarAvance(alistamientoActual.getIdAlistamiento(), d);
            }
            UiUtil.mensaje(this, "Avance guardado.");
            cargar();
        } catch (Exception e) {
            UiUtil.error(this, e);
        }
    }

    private void completar() {
        try {
            if (alistamientoActual == null) {
                throw new IllegalStateException("Primero inicie el alistamiento.");
            }
            guardarSilencioso();
            boolean ok = servicio.completar(alistamientoActual.getIdAlistamiento(),
                    ((PlantillaOperativa) cmbPlantilla.getSelectedItem()).getIdPlantilla());
            if (ok) {
                UiUtil.mensaje(this, "Alistamiento COMPLETO. El mercado pasó a estado LISTA (listo para despacho).");
            } else {
                UiUtil.error(this, "Quedan productos sin marcar como completados.");
            }
            cargar();
        } catch (Exception e) {
            UiUtil.error(this, e);
        }
    }

    private void guardarSilencioso() {
        for (int i = 0; i < modelo.getRowCount(); i++) {
            DetalleAlistamiento d = detalles.get(i);
            Object cant = modelo.getValueAt(i, 5);
            Object comp = modelo.getValueAt(i, 6);
            d.setCantidadAlistada(valor(cant));
            d.setCompletado(Boolean.TRUE.equals(comp));
            servicio.registrarAvance(alistamientoActual.getIdAlistamiento(), d);
        }
    }

    private static double valor(Object v) {
        if (v == null) {
            return 0;
        }
        if (v instanceof Number n) {
            return n.doubleValue();
        }
        return Formato.decimal(v.toString());
    }
}
