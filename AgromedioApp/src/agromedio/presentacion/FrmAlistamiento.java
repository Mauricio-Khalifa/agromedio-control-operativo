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
    private final Map<Integer, Producto> productos = new HashMap<>();

    private DefaultTableModel modelo;
    private List<DetalleAlistamiento> detalles = List.of();
    private Alistamiento alistamientoActual;
    private boolean cargando = false;

    public FrmAlistamiento() {
        super("Alistamiento / Produccion", true, true, true, true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1000, 600);
        setLayout(new BorderLayout(8, 8));

        for (Producto p : productoDAO.listar()) {
            productos.put(p.getIdProducto(), p);
        }

        modelo = new DefaultTableModel(
                new Object[]{"ID", "Producto", "Solicitado", "U.", "Equivalente U. compra",
                        "Alistada", "Completado"}, 0) {
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
        scroll.setBorder(BorderFactory.createTitledBorder(
                "Fraccionamiento de la mercancia - RF07 (conversion) y RF08 (avance)"));
        add(scroll, BorderLayout.CENTER);

        JPanel norte = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        norte.add(new JLabel("Mercado:"));
        norte.add(cmbPlantilla);
        JButton btnIniciar = UiUtil.boton("Iniciar alistamiento");
        norte.add(btnIniciar);
        norte.add(lblEstado);
        add(norte, BorderLayout.NORTH);

        JButton btnGuardar = UiUtil.boton("Guardar avance");
        JButton btnCompletar = UiUtil.boton("Completar alistamiento");
        JPanel sur = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 10));
        sur.add(btnGuardar);
        sur.add(btnCompletar);
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
            lblEstado.setText("No hay plantillas en recepcion/alistamiento.");
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
            modelo.addRow(new Object[]{d.getIdDetalleAlistamiento(), d.getNombreProducto(),
                    d.getCantidadSolicitada(), d.getAbreviatura(), equivalente,
                    d.getCantidadAlistada(), d.isCompletado()});
        }
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
                d.setCantidadAlistada(cant == null ? 0 : Double.parseDouble(cant.toString().replace(',', '.')));
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
                UiUtil.mensaje(this, "Alistamiento COMPLETO. El mercado paso a estado LISTA (listo para despacho).");
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
            d.setCantidadAlistada(cant == null ? 0 : Double.parseDouble(cant.toString().replace(',', '.')));
            d.setCompletado(Boolean.TRUE.equals(comp));
            servicio.registrarAvance(alistamientoActual.getIdAlistamiento(), d);
        }
    }
}
