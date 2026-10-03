package agromedio.presentacion;

import agromedio.datos.ConsultaDAO;
import agromedio.datos.ConsultaDAOSqlite;
import agromedio.datos.PlantillaDAO;
import agromedio.datos.PlantillaDAOSqlite;
import agromedio.datos.ProductoDAO;
import agromedio.datos.ProductoDAOSqlite;
import agromedio.logica.PlantillaServicio;
import agromedio.modelo.DetallePlantilla;
import agromedio.modelo.FilaSaldo;
import agromedio.modelo.PlantillaOperativa;
import agromedio.modelo.Producto;

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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/** Detalle de la plantilla: productos solicitados y saldo integral (trazabilidad). */
public class FrmDetallePlantilla extends JInternalFrame {

    private final int idPlantilla;
    private final PlantillaDAO plantillaDAO = new PlantillaDAOSqlite();
    private final ProductoDAO productoDAO = new ProductoDAOSqlite();
    private final ConsultaDAO consultaDAO = new ConsultaDAOSqlite();
    private final PlantillaServicio servicio = new PlantillaServicio();

    private final DefaultTableModel modeloDetalle;
    private final DefaultTableModel modeloSaldo;
    private final JTable tablaDetalle;
    private final JTable tablaSaldo;
    private final JComboBox<Producto> cmbProducto = new JComboBox<>();
    private final JTextField txtCantidad = UiUtil.campo();
    private final JButton btnAgregar = UiUtil.boton("Agregar");
    private final JButton btnQuitar = UiUtil.boton("Quitar");
    private final JLabel lblEstado = new JLabel();
    private DetallePlantilla[] cache = new DetallePlantilla[0];
    private int idDetalleSel = 0;

    public FrmDetallePlantilla(int idPlantilla) {
        super("Detalle de plantilla", true, true, true, true);
        this.idPlantilla = idPlantilla;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(980, 620);
        setLayout(new BorderLayout(8, 8));

        JPanel norte = new JPanel(new BorderLayout());
        norte.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        JLabel lblTitulo = new JLabel();
        lblTitulo.setFont(UiUtil.FUENTE_TITULO);
        norte.add(lblTitulo, BorderLayout.CENTER);
        norte.add(lblEstado, BorderLayout.EAST);
        add(norte, BorderLayout.NORTH);

        modeloDetalle = UiUtil.modelo(new Object[]{"ID", "Producto", "Cantidad solicitada", "Unidad"}, false);
        tablaDetalle = new JTable(modeloDetalle);
        UiUtil.prepararTabla(tablaDetalle);
        JScrollPane scrollDetalle = new JScrollPane(tablaDetalle);
        scrollDetalle.setBorder(BorderFactory.createTitledBorder("Productos solicitados"));

        modeloSaldo = UiUtil.modelo(new Object[]{"Producto", "Solicitado", "Comprado", "Recibido",
                "Alistado", "Despachado", "Por despachar"}, false);
        tablaSaldo = new JTable(modeloSaldo);
        UiUtil.prepararTabla(tablaSaldo);
        JScrollPane scrollSaldo = new JScrollPane(tablaSaldo);
        scrollSaldo.setBorder(BorderFactory.createTitledBorder("Saldo integral (vista v_saldo_plantilla)"));

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollDetalle, scrollSaldo);
        split.setResizeWeight(0.55);
        add(split, BorderLayout.CENTER);

        for (Producto p : productoDAO.listar()) {
            cmbProducto.addItem(p);
        }
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Agregar producto (solo mientras la plantilla este CARGADA)"));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.anchor = GridBagConstraints.WEST;
        g.gridx = 0; g.gridy = 0;
        form.add(new JLabel("Producto"), g);
        g.gridx = 1; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL;
        form.add(cmbProducto, g);
        g.gridx = 2; g.weightx = 0; g.fill = GridBagConstraints.NONE;
        form.add(new JLabel("Cantidad"), g);
        g.gridx = 3;
        txtCantidad.setPreferredSize(new java.awt.Dimension(120, 34));
        form.add(txtCantidad, g);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT));
        botones.add(btnAgregar);
        botones.add(btnQuitar);

        JPanel sur = new JPanel(new BorderLayout());
        sur.add(form, BorderLayout.CENTER);
        sur.add(botones, BorderLayout.SOUTH);
        add(sur, BorderLayout.SOUTH);

        btnAgregar.addActionListener(e -> agregar());
        btnQuitar.addActionListener(e -> quitar());
        tablaDetalle.getSelectionModel().addListSelectionListener(e -> {
            int fila = tablaDetalle.getSelectedRow();
            if (fila >= 0) {
                idDetalleSel = cache[fila].getIdDetallePlantilla();
            }
        });

        refrescar();
        UiUtil.fuenteRaiz(this);
        lblTitulo.setText("Plantilla #" + idPlantilla);
    }

    private boolean editable() {
        PlantillaOperativa p = plantillaDAO.obtener(idPlantilla);
        boolean ok = servicio.editable(p);
        btnAgregar.setEnabled(ok);
        btnQuitar.setEnabled(ok);
        if (p != null) {
            lblEstado.setText("Estado: " + p.getEstado() + "  |  " + p.getNombreMercado()
                    + "  |  " + p.getFechaProgramada());
        }
        return ok;
    }

    private void agregar() {
        try {
            if (!editable()) {
                throw new IllegalStateException("La plantilla ya no es editable (estado distinto de CARGADA).");
            }
            Producto p = (Producto) cmbProducto.getSelectedItem();
            double cant = Double.parseDouble(txtCantidad.getText().trim().replace(',', '.'));
            servicio.agregarProducto(idPlantilla, p.getIdProducto(), cant);
            txtCantidad.setText("");
            refrescar();
        } catch (Exception e) {
            UiUtil.error(this, e);
        }
    }

    private void quitar() {
        try {
            if (!editable()) {
                throw new IllegalStateException("La plantilla ya no es editable (estado distinto de CARGADA).");
            }
            if (idDetalleSel == 0) {
                throw new IllegalArgumentException("Seleccione el producto a quitar.");
            }
            servicio.eliminarProducto(idDetalleSel);
            idDetalleSel = 0;
            refrescar();
        } catch (Exception e) {
            UiUtil.error(this, e);
        }
    }

    private void refrescar() {
        editable();
        modeloDetalle.setRowCount(0);
        cache = servicio.detalle(idPlantilla).toArray(new DetallePlantilla[0]);
        for (DetallePlantilla d : cache) {
            modeloDetalle.addRow(new Object[]{d.getIdDetallePlantilla(), d.getNombreProducto(),
                    d.getCantidadSolicitada(), d.getAbreviatura()});
        }
        modeloSaldo.setRowCount(0);
        for (FilaSaldo f : consultaDAO.saldoPlantilla(idPlantilla)) {
            modeloSaldo.addRow(new Object[]{f.getProducto(), f.getSolicitado(), f.getComprado(),
                    f.getRecibido(), f.getAlistado(), f.getDespachado(), f.getPorDespachar()});
        }
        idDetalleSel = 0;
        tablaDetalle.clearSelection();
    }
}
