package agromedio.presentacion;

import agromedio.datos.PlantillaDAO;
import agromedio.datos.PlantillaDAOSqlite;
import agromedio.logica.DespachoServicio;
import agromedio.modelo.DetalleDespacho;
import agromedio.modelo.Despacho;
import agromedio.modelo.PlantillaOperativa;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;

/** Modulo 4 - Despacho: checklist paso a paso (RF09) y cierre al 100% (RF10). */
public class FrmDespacho extends JInternalFrame {

    private final DespachoServicio servicio = new DespachoServicio();
    private final PlantillaDAO plantillaDAO = new PlantillaDAOSqlite();

    private final JComboBox<PlantillaOperativa> cmbPlantilla = new JComboBox<>();
    private final JLabel lblProgreso = new JLabel("0 / 0 items verificados (0%)");
    private final DefaultTableModel modelo;
    private final JButton btnVerificar = UiUtil.boton("VERIFICAR ITEM");
    private final JButton btnCerrar = UiUtil.boton("CERRAR DESPACHO");

    private JTable tabla;
    private Despacho despachoActual;
    private List<DetalleDespacho> detalles = List.of();
    private boolean cargando = false;

    public FrmDespacho() {
        super("Despacho de mercados", true, true, true, true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1000, 620);
        setLayout(new BorderLayout(8, 8));

        modelo = new DefaultTableModel(
                new Object[]{"ID", "Producto", "Cantidad a despachar", "U.", "Verificado"}, 0) {
            @Override
            public boolean isCellEditable(int f, int c) {
                return despachoActual != null && (c == 2 || c == 4)
                        && !Despacho.DESPACHADA.equals(despachoActual.getEstado());
            }

            @Override
            public Class<?> getColumnClass(int c) {
                return c == 4 ? Boolean.class : Object.class;
            }
        };
        tabla = new JTable(modelo);
        UiUtil.prepararTabla(tabla);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createTitledBorder(
                "Lista de chequeo digital: verifique cada item al meterlo en la bolsa (RF09)"));
        add(scroll, BorderLayout.CENTER);

        JPanel norte = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        norte.add(new JLabel("Mercado:"));
        norte.add(cmbPlantilla);
        JButton btnIniciar = UiUtil.boton("Abrir lista");
        norte.add(btnIniciar);
        lblProgreso.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblProgreso.setForeground(new Color(0, 102, 0));
        norte.add(lblProgreso);
        add(norte, BorderLayout.NORTH);

        btnVerificar.setFont(UiUtil.FUENTE_BOTON.deriveFont(Font.BOLD, 18f));
        btnCerrar.setFont(UiUtil.FUENTE_BOTON.deriveFont(Font.BOLD, 18f));
        JPanel sur = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 12));
        sur.add(btnVerificar);
        JButton btnDesmarcar = UiUtil.boton("Desmarcar item");
        sur.add(btnDesmarcar);
        sur.add(btnCerrar);
        add(sur, BorderLayout.SOUTH);

        cmbPlantilla.addActionListener(e -> cargar());
        btnIniciar.addActionListener(e -> iniciar());
        btnVerificar.addActionListener(e -> verificar(true));
        btnDesmarcar.addActionListener(e -> verificar(false));
        btnCerrar.addActionListener(e -> cerrar());

        cargarPlantillas();
        UiUtil.fuenteRaiz(this);
    }

    private void cargarPlantillas() {
        cmbPlantilla.removeAllItems();
        for (PlantillaOperativa p : plantillaDAO.listarPorEstado(
                PlantillaOperativa.ESTADO_LISTA,
                PlantillaOperativa.ESTADO_DESPACHADA)) {
            cmbPlantilla.addItem(p);
        }
        if (cmbPlantilla.getItemCount() == 0) {
            lblProgreso.setText("No hay mercados listos para despacho.");
        }
    }

    private void iniciar() {
        try {
            PlantillaOperativa p = (PlantillaOperativa) cmbPlantilla.getSelectedItem();
            if (p == null) {
                throw new IllegalArgumentException("Seleccione un mercado.");
            }
            despachoActual = servicio.iniciar(p.getIdPlantilla());
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
        despachoActual = servicio.actual(p.getIdPlantilla());
        if (despachoActual == null) {
            lblProgreso.setText("Presione 'Abrir lista' para iniciar la verificacion");
            cargando = false;
            return;
        }
        detalles = servicio.detalle(despachoActual.getIdDespacho());
        for (DetalleDespacho d : detalles) {
            modelo.addRow(new Object[]{d.getIdDetalleDespacho(), d.getNombreProducto(),
                    d.getCantidadDespachada(), d.getAbreviatura(), d.isVerificado()});
        }
        actualizarProgreso();
        cargando = false;
    }

    private void actualizarProgreso() {
        if (despachoActual == null) {
            return;
        }
        int[] progreso = servicio.progreso(despachoActual.getIdDespacho());
        double pct = progreso[1] == 0 ? 0 : Math.round(1000.0 * progreso[0] / progreso[1]) / 10.0;
        lblProgreso.setText(progreso[0] + " / " + progreso[1] + " items verificados (" + pct + "%)");
        boolean cerrado = Despacho.DESPACHADA.equals(despachoActual.getEstado());
        btnVerificar.setEnabled(!cerrado);
        btnCerrar.setEnabled(!cerrado);
        if (cerrado) {
            lblProgreso.setText("DESPACHADO AL 100% - mercado entregado");
        }
    }

    private void verificar(boolean marcar) {
        try {
            if (despachoActual == null) {
                throw new IllegalStateException("Primero abra la lista del mercado.");
            }
            int fila = tablaSeleccionada();
            if (fila < 0) {
                throw new IllegalArgumentException("Seleccione el producto a verificar.");
            }
            guardarFila(fila);
            DetalleDespacho d = detalles.get(fila);
            d.setVerificado(marcar);
            servicio.marcarItem(d);
            modelo.setValueAt(marcar, fila, 4);
            actualizarProgreso();
        } catch (Exception e) {
            UiUtil.error(this, e);
        }
    }

    private int tablaSeleccionada() {
        for (java.awt.Component c : getComponents()) {
            if (c instanceof JScrollPane sp && sp.getViewport().getView() instanceof JTable t) {
                return t.getSelectedRow();
            }
        }
        return -1;
    }

    private void guardarFila(int fila) {
        DetalleDespacho d = detalles.get(fila);
        Object cant = modelo.getValueAt(fila, 2);
        d.setCantidadDespachada(cant == null ? 0 : Double.parseDouble(cant.toString().replace(',', '.')));
        servicio.marcarItem(d);
    }

    private void cerrar() {
        try {
            if (despachoActual == null) {
                throw new IllegalStateException("Primero abra la lista del mercado.");
            }
            for (int i = 0; i < modelo.getRowCount(); i++) {
                guardarFila(i);
            }
            PlantillaOperativa p = (PlantillaOperativa) cmbPlantilla.getSelectedItem();
            JTextField txtNovedades = UiUtil.campo();
            txtNovedades.setPreferredSize(new java.awt.Dimension(420, 36));
            int op = JOptionPane.showConfirmDialog(this,
                    new Object[]{"Novedades del despacho (opcional):", txtNovedades},
                    "Cerrar despacho (RF10)", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            String novedades = op == JOptionPane.OK_OPTION ? txtNovedades.getText() : null;
            if (op != JOptionPane.OK_OPTION) {
                return;
            }
            List<DetalleDespacho> pendientes = servicio.cerrar(
                    despachoActual.getIdDespacho(), p.getIdPlantilla(), novedades);
            if (!pendientes.isEmpty()) {
                StringBuilder sb = new StringBuilder("No se puede cerrar. Faltan por verificar:\n");
                for (DetalleDespacho d : pendientes) {
                    sb.append(" - ").append(d.getNombreProducto()).append("\n");
                }
                UiUtil.error(this, sb.toString());
                return;
            }
            UiUtil.mensaje(this, "DESPACHADO AL 100%: el mercado fue cerrado y entregado a distribucion.");
            cargarPlantillas();
            cargar();
        } catch (Exception e) {
            UiUtil.error(this, e);
        }
    }
}
