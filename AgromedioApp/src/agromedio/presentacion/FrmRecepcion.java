package agromedio.presentacion;

import agromedio.logica.RecepcionServicio;
import agromedio.modelo.Compra;
import agromedio.modelo.DetalleCompra;
import agromedio.modelo.DetalleRecepcion;
import agromedio.modelo.Recepcion;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;

/** Modulo 2 - Recepcion: entradas fisicas (RF05) y faltantes (RF06). */
public class FrmRecepcion extends JInternalFrame {

    private final RecepcionServicio servicio = new RecepcionServicio();

    private final JComboBox<Compra> cmbCompra = new JComboBox<>();
    private final JTextField txtObservaciones = UiUtil.campo();

    private DefaultTableModel modeloDetalle;
    private DefaultTableModel modeloFaltantes;
    private List<DetalleCompra> detalles = new ArrayList<>();
    private boolean yaRegistrada = false;

    public FrmRecepcion() {
        super("Recepcion de mercancia", true, true, true, true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(980, 640);
        setLayout(new BorderLayout(8, 8));

        modeloDetalle = new DefaultTableModel(
                new Object[]{"ID", "Producto", "Proveedor", "Comprado", "U.", "Recibida"}, 0) {
            @Override
            public boolean isCellEditable(int f, int c) {
                return c == 5 && !yaRegistrada;
            }
        };
        modeloFaltantes = UiUtil.modelo(
                new Object[]{"Producto", "Proveedor", "Comprado", "Recibido", "Faltante", "U."}, false);

        JTable tablaDetalle = new JTable(modeloDetalle);
        UiUtil.prepararTabla(tablaDetalle);
        JScrollPane scrollDetalle = new JScrollPane(tablaDetalle);
        scrollDetalle.setBorder(BorderFactory.createTitledBorder(
                "Detalle de la compra - edite la columna 'Recibida' si llego menos de lo comprado (RF05)"));

        JTable tablaFaltantes = new JTable(modeloFaltantes);
        UiUtil.prepararTabla(tablaFaltantes);
        JScrollPane scrollFaltantes = new JScrollPane(tablaFaltantes);
        scrollFaltantes.setBorder(BorderFactory.createTitledBorder(
                "Faltantes detectados automaticamente por la base de datos (RF06)"));

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollDetalle, scrollFaltantes);
        split.setResizeWeight(0.6);
        add(split, BorderLayout.CENTER);

        JPanel norte = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        norte.add(new JLabel("Compra pendiente:"));
        norte.add(cmbCompra);
        norte.add(new JLabel("Observaciones:"));
        txtObservaciones.setPreferredSize(new java.awt.Dimension(340, 34));
        norte.add(txtObservaciones);
        add(norte, BorderLayout.NORTH);

        JButton btnRegistrar = UiUtil.boton("Registrar entrada (RF05)");
        JButton btnCerrar = UiUtil.boton("Cerrar recepcion");
        JButton btnRefrescar = UiUtil.boton("Refrescar");
        JPanel sur = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 10));
        sur.add(btnRegistrar);
        sur.add(btnCerrar);
        sur.add(btnRefrescar);
        add(sur, BorderLayout.SOUTH);

        cmbCompra.addActionListener(e -> seleccionarCompra());
        btnRegistrar.addActionListener(e -> registrar());
        btnCerrar.addActionListener(e -> cerrar());
        btnRefrescar.addActionListener(e -> cargarCompras());

        cargarCompras();
        UiUtil.fuenteRaiz(this);
    }

    private void cargarCompras() {
        cmbCompra.removeAllItems();
        for (Compra c : servicio.comprasPendientes()) {
            cmbCompra.addItem(c);
        }
        modeloDetalle.setRowCount(0);
        modeloFaltantes.setRowCount(0);
        yaRegistrada = false;
        if (cmbCompra.getItemCount() == 0) {
            txtObservaciones.setText("No hay compras pendientes de recepcion.");
        }
    }

    private void seleccionarCompra() {
        modeloDetalle.setRowCount(0);
        modeloFaltantes.setRowCount(0);
        Compra compra = (Compra) cmbCompra.getSelectedItem();
        if (compra == null) {
            return;
        }
        detalles = servicio.detalleCompra(compra.getIdCompra());
        yaRegistrada = false;

        // Si ya existe una recepcion cargada, se muestra en modo lectura
        List<Recepcion> previas = servicio.recepcionesDe(compra.getIdCompra());
        if (!previas.isEmpty()) {
            yaRegistrada = true;
            Recepcion ultima = previas.get(previas.size() - 1);
            txtObservaciones.setText(ultima.getObservaciones() == null ? "" : ultima.getObservaciones());
            for (DetalleRecepcion dr : servicio.detalle(ultima.getIdRecepcion())) {
                modeloDetalle.addRow(new Object[]{dr.getIdDetalleCompra(), dr.getNombreProducto(),
                        dr.getNombreProveedor(), dr.getCantidadComprada(), dr.getAbreviatura(),
                        dr.getCantidadRecibida()});
                if (dr.getCantidadFaltante() > 0) {
                    modeloFaltantes.addRow(new Object[]{dr.getNombreProducto(), dr.getNombreProveedor(),
                            dr.getCantidadComprada(), dr.getCantidadRecibida(), dr.getCantidadFaltante(),
                            dr.getAbreviatura()});
                }
            }
        } else {
            for (DetalleCompra d : detalles) {
                modeloDetalle.addRow(new Object[]{d.getIdDetalleCompra(), d.getNombreProducto(),
                        d.getNombreProveedor(), d.getCantidadComprada(), d.getAbreviatura(),
                        d.getCantidadComprada()});
            }
        }
    }

    private void registrar() {
        try {
            Compra compra = (Compra) cmbCompra.getSelectedItem();
            if (compra == null) {
                throw new IllegalArgumentException("Seleccione una compra pendiente.");
            }
            if (yaRegistrada) {
                throw new IllegalStateException("Esta compra ya tiene recepcion registrada; use 'Cerrar recepcion'.");
            }
            List<Double> recibidas = new ArrayList<>();
            for (int i = 0; i < modeloDetalle.getRowCount(); i++) {
                Object v = modeloDetalle.getValueAt(i, 5);
                recibidas.add(v == null ? 0.0 : Double.parseDouble(v.toString().replace(',', '.')));
            }
            List<DetalleRecepcion> faltantes = servicio.registrarRecepcion(
                    compra.getIdCompra(), null, txtObservaciones.getText(), recibidas);
            yaRegistrada = true;
            modeloFaltantes.setRowCount(0);
            for (DetalleRecepcion f : faltantes) {
                modeloFaltantes.addRow(new Object[]{f.getNombreProducto(), f.getNombreProveedor(),
                        f.getCantidadComprada(), f.getCantidadRecibida(), f.getCantidadFaltante(),
                        f.getAbreviatura()});
            }
            if (faltantes.isEmpty()) {
                UiUtil.mensaje(this, "Entrada registrada completa: no hay faltantes.");
            } else {
                UiUtil.mensaje(this, "Entrada registrada con " + faltantes.size()
                        + " faltante(s). Revise la tabla inferior (RF06).");
            }
            seleccionarCompra();
        } catch (Exception e) {
            UiUtil.error(this, e);
        }
    }

    private void cerrar() {
        try {
            Compra compra = (Compra) cmbCompra.getSelectedItem();
            if (compra == null) {
                throw new IllegalArgumentException("Seleccione una compra pendiente.");
            }
            servicio.cerrarRecepcion(compra.getIdCompra());
            UiUtil.mensaje(this, "Recepcion cerrada. La plantilla paso a ALISTANDO (produccion).");
            cargarCompras();
        } catch (Exception e) {
            UiUtil.error(this, e);
        }
    }
}
