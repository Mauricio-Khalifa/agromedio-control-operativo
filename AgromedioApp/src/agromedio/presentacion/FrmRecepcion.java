package agromedio.presentacion;

import agromedio.logica.RecepcionServicio;
import agromedio.modelo.Compra;
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
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

/** Modulo 2 - Recepcion: entregas parciales (RF05 / RN-05) y faltante acumulado (RF06 / RN-06). */
public class FrmRecepcion extends JInternalFrame {

    private static final int COL_PRODUCTO = 0;
    private static final int COL_COMPRADO = 2;
    private static final int COL_RECIBIDO = 4;
    private static final int COL_FALTANTE = 5;
    private static final int COL_RECIBIR = 6;

    private final RecepcionServicio servicio = new RecepcionServicio();

    private final JComboBox<Compra> cmbCompra = new JComboBox<>();
    private final JTextField txtObservaciones = UiUtil.campo();
    private final JButton btnRegistrar = UiUtil.botonPrimario("Registrar entrega",
            Iconos.accion(Iconos.Accion.GUARDAR, Color.WHITE));
    private final JLabel lblResumen = UiUtil.subtitulo("Seleccione una compra pendiente.");

    private final DefaultTableModel modeloDetalle;
    private final DefaultTableModel modeloHistorial;
    private JTable tablaDetalle;

    public FrmRecepcion() {
        super("Recepción de mercancía", true, true, true, true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1160, 720);
        setLayout(new BorderLayout(8, 8));
        getContentPane().setBackground(Tema.FONDO);

        modeloDetalle = new DefaultTableModel(
                new Object[]{"Producto", "Proveedor", "Comprado", "U.", "Recibido acum.",
                        "Faltante (máx.)", "Recibir ahora"}, 0) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                if (columna != COL_RECIBIR) {
                    return false;
                }
                Object faltante = getValueAt(fila, COL_FALTANTE);
                return faltante instanceof Number n && n.doubleValue() > 0;
            }
        };
        modeloHistorial = UiUtil.modelo(
                new Object[]{"Entrega", "Fecha", "Producto", "Cantidad recibida", "Observaciones"}, false);

        tablaDetalle = new JTable(modeloDetalle);
        UiUtil.prepararTabla(tablaDetalle);
        // Habilita el boton "Registrar entrega" mientras se escribe (sin esperar Enter).
        JTextField editorCantidad = new JTextField();
        TableColumn colRecibir = tablaDetalle.getColumnModel().getColumn(COL_RECIBIR);
        colRecibir.setCellEditor(new javax.swing.DefaultCellEditor(editorCantidad));
        colRecibir.setPreferredWidth(150);
        colRecibir.setMinWidth(100);
        editorCantidad.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { actualizarResumen(); }
            @Override
            public void removeUpdate(DocumentEvent e) { actualizarResumen(); }
            @Override
            public void changedUpdate(DocumentEvent e) { actualizarResumen(); }
        });
        tablaDetalle.setToolTipText("Escriba solo las cantidades que llegan en esta entrega. El faltante es el máximo permitido.");
        // Permite iniciar la edicion con un solo clic (mas evidente para el usuario).
        // Permite iniciar la edicion con un solo clic (mas evidente para el usuario).
        tablaDetalle.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                int fila = tablaDetalle.rowAtPoint(e.getPoint());
                int columna = tablaDetalle.columnAtPoint(e.getPoint());
                if (fila >= 0 && columna == COL_RECIBIR
                        && tablaDetalle.isCellEditable(fila, columna)
                        && !tablaDetalle.isEditing()) {
                    tablaDetalle.editCellAt(fila, columna);
                    Component editor = tablaDetalle.getEditorComponent();
                    if (editor != null) {
                        editor.requestFocusInWindow();
                    }
                }
            }
        });
        tablaDetalle.setSurrendersFocusOnKeystroke(true);
        JScrollPane scrollDetalle = new JScrollPane(tablaDetalle);
        scrollDetalle.setBorder(BorderFactory.createLineBorder(Tema.BORDE));

        JTable tablaHistorial = new JTable(modeloHistorial);
        UiUtil.prepararTabla(tablaHistorial);
        JScrollPane scrollHistorial = new JScrollPane(tablaHistorial);
        scrollHistorial.setBorder(BorderFactory.createLineBorder(Tema.BORDE));

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollDetalle, scrollHistorial);
        split.setResizeWeight(0.68);
        split.setBorder(null);
        split.setOpaque(false);
        add(split, BorderLayout.CENTER);

        add(crearNorte(), BorderLayout.NORTH);

        JButton btnCerrar = UiUtil.botonSecundario("Cerrar recepción",
                Iconos.accion(Iconos.Accion.CERRAR, Tema.PRIMARIO));
        JButton btnRefrescar = UiUtil.botonSecundario("Refrescar",
                Iconos.accion(Iconos.Accion.REFRESCAR, Tema.PRIMARIO));
        JPanel sur = new JPanel(new BorderLayout(8, 0));
        sur.setOpaque(false);
        sur.setBorder(BorderFactory.createEmptyBorder(4, 10, 8, 10));
        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        acciones.setOpaque(false);
        acciones.add(btnRegistrar);
        acciones.add(btnCerrar);
        acciones.add(btnRefrescar);
        sur.add(acciones, BorderLayout.WEST);
        JLabel ayuda = UiUtil.subtitulo("Haga clic en 'Recibir ahora' para ingresar la cantidad recibida de cada producto.");
        JPanel este = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        este.setOpaque(false);
        este.add(ayuda);
        este.add(lblResumen);
        sur.add(este, BorderLayout.EAST);
        add(sur, BorderLayout.SOUTH);

        cmbCompra.addActionListener(e -> seleccionarCompra());
        btnRegistrar.addActionListener(e -> registrar());
        btnCerrar.addActionListener(e -> cerrar());
        btnRefrescar.addActionListener(e -> cargarCompras());
        modeloDetalle.addTableModelListener(e -> actualizarResumen());

        btnRegistrar.setEnabled(false);
        cargarCompras();
        UiUtil.fuenteRaiz(this);
    }

    private JPanel crearNorte() {
        JPanel norte = new JPanel(new BorderLayout(8, 6));
        norte.setOpaque(false);
        norte.setBorder(BorderFactory.createEmptyBorder(8, 10, 2, 10));
        norte.add(UiUtil.encabezado("Recepción de mercancía",
                "Registre entregas parciales. El sistema acumula lo recibido y conserva el faltante."),
                BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Tema.SUPERFICIE);
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Tema.BORDE),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 6, 4, 6);
        g.anchor = GridBagConstraints.WEST;

        g.gridx = 0; g.gridy = 0;
        form.add(new JLabel("Compra pendiente"), g);
        g.gridx = 1; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL;
        cmbCompra.setPreferredSize(new java.awt.Dimension(330, 34));
        form.add(cmbCompra, g);

        g.gridx = 0; g.gridy = 1; g.weightx = 0; g.fill = GridBagConstraints.NONE;
        form.add(new JLabel("Observaciones"), g);
        g.gridx = 1; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL;
        txtObservaciones.setPreferredSize(new java.awt.Dimension(520, 34));
        form.add(txtObservaciones, g);

        norte.add(form, BorderLayout.CENTER);
        return norte;
    }

    private void cargarCompras() {
        modeloDetalle.setRowCount(0);
        modeloHistorial.setRowCount(0);
        txtObservaciones.setText("");
        cmbCompra.removeAllItems();
        for (Compra c : servicio.comprasPendientes()) {
            cmbCompra.addItem(c);
        }
        boolean hay = cmbCompra.getItemCount() > 0;
        cmbCompra.setEnabled(hay);
        txtObservaciones.setEnabled(hay);
        if (hay) {
            cmbCompra.setSelectedIndex(0);
            seleccionarCompra();
        } else {
            lblResumen.setText("No hay compras pendientes de recepción.");
            btnRegistrar.setEnabled(false);
        }
    }

    private void seleccionarCompra() {
        modeloDetalle.setRowCount(0);
        modeloHistorial.setRowCount(0);
        Compra compra = (Compra) cmbCompra.getSelectedItem();
        btnRegistrar.setEnabled(false);
        if (compra == null) {
            lblResumen.setText("Seleccione una compra pendiente.");
            return;
        }

        // Una fila por producto con el acumulado de todas las entregas.
        for (DetalleRecepcion d : servicio.estadoRecepcion(compra.getIdCompra())) {
            modeloDetalle.addRow(new Object[]{d.getNombreProducto(), d.getNombreProveedor(),
                    d.getCantidadComprada(), d.getAbreviatura(), d.getCantidadRecibida(),
                    d.getCantidadFaltante(), 0.0});
        }

        List<Recepcion> previas = servicio.recepcionesDe(compra.getIdCompra());
        for (Recepcion r : previas) {
            List<DetalleRecepcion> detalleEntrega = servicio.detalle(r.getIdRecepcion());
            if (detalleEntrega.isEmpty()) {
                modeloHistorial.addRow(new Object[]{r.getIdRecepcion(), r.getFechaRecepcion(),
                        "-", 0.0, r.getObservaciones()});
            } else {
                for (DetalleRecepcion dr : detalleEntrega) {
                    modeloHistorial.addRow(new Object[]{r.getIdRecepcion(), r.getFechaRecepcion(),
                            dr.getNombreProducto(), dr.getCantidadRecibida(), r.getObservaciones()});
                }
            }
        }
        txtObservaciones.setText("");
        actualizarResumen();
    }

    private void actualizarResumen() {
        int lineas = modeloDetalle.getRowCount();
        int pendientes = 0;
        int conCantidad = 0;
        for (int i = 0; i < lineas; i++) {
            Object faltante = modeloDetalle.getValueAt(i, COL_FALTANTE);
            if (faltante instanceof Number n && n.doubleValue() > 0) {
                pendientes++;
            }
            double cantidadFila = valor(modeloDetalle.getValueAt(i, COL_RECIBIR));
            if (tablaDetalle != null && tablaDetalle.isEditing()
                    && tablaDetalle.getEditingRow() == i
                    && tablaDetalle.getEditingColumn() == COL_RECIBIR) {
                Component editor = tablaDetalle.getEditorComponent();
                if (editor instanceof JTextField tf) {
                    cantidadFila = valor(tf.getText());
                }
            }
            if (cantidadFila > 0) {
                conCantidad++;
            }
        }
        lblResumen.setText(lineas == 0
                ? "Seleccione una compra con detalle."
                : lineas + " líneas  |  " + pendientes + " con faltante  |  "
                + conCantidad + " por registrar");
        // El boton se mantiene habilitado mientras haya lineas para que el clic
        // pueda confirmar la edicion en curso; la validacion real ocurre en registrar().
        btnRegistrar.setEnabled(lineas > 0);
        // Permite confirmar la edicion en curso con un solo clic en el boton.
        btnRegistrar.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (tablaDetalle.isEditing()) {
                    tablaDetalle.getCellEditor().stopCellEditing();
                }
            }
        });
    }

    private void registrar() {
        try {
            if (tablaDetalle.isEditing() && tablaDetalle.getCellEditor() != null) {
                tablaDetalle.getCellEditor().stopCellEditing();
            }
            Compra compra = (Compra) cmbCompra.getSelectedItem();
            if (compra == null) {
                throw new IllegalArgumentException("Seleccione una compra pendiente.");
            }
            List<Double> recibidas = new ArrayList<>();
            for (int i = 0; i < modeloDetalle.getRowCount(); i++) {
                double cantidad = valor(modeloDetalle.getValueAt(i, COL_RECIBIR));
                double maximo = valor(modeloDetalle.getValueAt(i, COL_FALTANTE));
                if (cantidad < 0) {
                    throw new IllegalArgumentException("La cantidad recibida no puede ser negativa.");
                }
                if (cantidad > maximo + 0.0001) {
                    throw new IllegalArgumentException("La cantidad de "
                            + modeloDetalle.getValueAt(i, COL_PRODUCTO)
                            + " supera el faltante permitido (" + Formato.numero(maximo) + ").");
                }
                recibidas.add(cantidad);
            }
            List<DetalleRecepcion> pendientes = servicio.registrarRecepcion(
                    compra.getIdCompra(), null, txtObservaciones.getText(), recibidas);
            if (pendientes.isEmpty()) {
                UiUtil.mensaje(this, "Entrega registrada. La compra quedó completa.");
            } else {
                UiUtil.mensaje(this, "Entrega registrada. Quedan " + pendientes.size()
                        + " producto(s) con faltante acumulado. Puede registrar otra entrega.");
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
            List<DetalleRecepcion> pendientes = servicio.faltantesPendientes(compra.getIdCompra());
            if (!pendientes.isEmpty()) {
                boolean continuar = UiUtil.confirmar(this,
                        "Quedan " + pendientes.size() + " producto(s) con faltante acumulado.\n"
                        + "Si cierra la recepción, el faltante quedará registrado para el reporte.\n"
                        + "¿Desea cerrar de todos modos?");
                if (!continuar) {
                    return;
                }
            }
            servicio.cerrarRecepcion(compra.getIdCompra());
            UiUtil.mensaje(this, "Recepción cerrada. La plantilla pasó a ALISTANDO (producción).");
            cargarCompras();
        } catch (Exception e) {
            UiUtil.error(this, e);
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
