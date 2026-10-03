package agromedio.presentacion;

import agromedio.datos.PlantillaDAO;
import agromedio.datos.PlantillaDAOSqlite;
import agromedio.datos.ProductoDAO;
import agromedio.datos.ProductoDAOSqlite;
import agromedio.datos.ProveedorDAO;
import agromedio.datos.ProveedorDAOSqlite;
import agromedio.logica.CompraServicio;
import agromedio.modelo.DetalleCompra;
import agromedio.modelo.FilaConsolidado;
import agromedio.modelo.PlantillaOperativa;
import agromedio.modelo.Producto;
import agromedio.modelo.Proveedor;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Modulo 1 - Compras: consolidacion de demanda (RF03) y registro (RF04). */
public class FrmCompras extends JInternalFrame {

    private final CompraServicio servicio = new CompraServicio();
    private final PlantillaDAO plantillaDAO = new PlantillaDAOSqlite();
    private final ProductoDAO productoDAO = new ProductoDAOSqlite();
    private final ProveedorDAO proveedorDAO = new ProveedorDAOSqlite();

    // Pestaña consolidado
    private final DefaultTableModel modeloConsolidado = UiUtil.modelo(
            new Object[]{"Producto", "Demanda", "U.", "En compra", "Mercados", "Pendiente"}, false);
    private final JTextField txtFecha = UiUtil.campo();

    // Pestaña registro
    private final JComboBox<PlantillaOperativa> cmbPlantilla = new JComboBox<>();
    private final JComboBox<Producto> cmbProducto = new JComboBox<>();
    private final JComboBox<Proveedor> cmbProveedor = new JComboBox<>();
    private final JTextField txtCantidad = UiUtil.campo();
    private final DefaultTableModel modeloItems = UiUtil.modelo(
            new Object[]{"Producto", "Proveedor", "Cantidad", "U. compra"}, false);
    private final List<DetalleCompra> items = new ArrayList<>();

    public FrmCompras() {
        super("Compras", true, true, true, true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(980, 620);
        setLayout(new BorderLayout());

        JTabbedPane pestanias = new JTabbedPane();
        pestanias.setFont(UiUtil.FUENTE);
        pestanias.addTab("Consolidado de demanda (RF03)", crearTabConsolidado());
        pestanias.addTab("Registrar compra (RF04)", crearTabRegistro());
        add(pestanias, BorderLayout.CENTER);

        cargarPlantillas();
        consolidar();
        UiUtil.fuenteRaiz(this);
    }

    private JPanel crearTabConsolidado() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel norte = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.anchor = GridBagConstraints.WEST;
        norte.add(new JLabel("Fecha programada (aaaa-mm-dd)"), g);
        txtFecha.setText(LocalDate.now().toString());
        norte.add(txtFecha, g);
        JButton btnActualizar = UiUtil.boton("Actualizar consolidado");
        norte.add(btnActualizar, g);
        panel.add(norte, BorderLayout.NORTH);

        JTable tabla = new JTable(modeloConsolidado);
        UiUtil.prepararTabla(tabla);
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createTitledBorder(
                "Demanda total de todos los mercados del dia (los items pendientes se compran aqui)"));
        panel.add(scroll, BorderLayout.CENTER);

        JLabel pie = new JLabel("RF03: el sistema suma automaticamente la demanda de todos los contratos activos del dia.");
        pie.setFont(UiUtil.FUENTE_NEGRITA);
        panel.add(pie, BorderLayout.SOUTH);

        btnActualizar.addActionListener(e -> consolidar());
        return panel;
    }

    private JPanel crearTabRegistro() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        for (Producto p : productoDAO.listar()) {
            cmbProducto.addItem(p);
        }
        for (Proveedor pr : proveedorDAO.listar()) {
            cmbProveedor.addItem(pr);
        }

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Producto a comprar"));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;

        g.gridx = 0; g.gridy = 0;
        form.add(new JLabel("Plantilla / mercado"), g);
        g.gridx = 1; g.weightx = 1;
        form.add(cmbPlantilla, g);

        g.gridx = 0; g.gridy = 1; g.weightx = 0;
        form.add(new JLabel("Producto"), g);
        g.gridx = 1; g.weightx = 1;
        form.add(cmbProducto, g);

        g.gridx = 0; g.gridy = 2; g.weightx = 0;
        form.add(new JLabel("Proveedor"), g);
        g.gridx = 1; g.weightx = 1;
        form.add(cmbProveedor, g);

        g.gridx = 0; g.gridy = 3; g.weightx = 0;
        form.add(new JLabel("Cantidad (en su unidad de compra)"), g);
        g.gridx = 1; g.weightx = 1;
        form.add(txtCantidad, g);

        JButton btnAgregar = UiUtil.boton("Agregar item");
        JPanel sur = new JPanel(new BorderLayout());
        sur.add(form, BorderLayout.CENTER);
        JPanel filaBotones = new JPanel();
        filaBotones.add(btnAgregar);
        sur.add(filaBotones, BorderLayout.SOUTH);

        JTable tablaItems = new JTable(modeloItems);
        UiUtil.prepararTabla(tablaItems);
        JScrollPane scroll = new JScrollPane(tablaItems);
        scroll.setBorder(BorderFactory.createTitledBorder("Items de la compra"));

        JPanel botonesFinales = new JPanel();
        JButton btnQuitar = UiUtil.boton("Quitar item");
        JButton btnGuardar = UiUtil.boton("Guardar compra");
        botonesFinales.add(btnQuitar);
        botonesFinales.add(btnGuardar);

        JPanel contenedor = new JPanel(new BorderLayout(8, 8));
        JPanel centro = new JPanel(new BorderLayout(8, 8));
        centro.add(sur, BorderLayout.NORTH);
        centro.add(scroll, BorderLayout.CENTER);
        contenedor.add(centro, BorderLayout.CENTER);
        contenedor.add(botonesFinales, BorderLayout.SOUTH);

        btnAgregar.addActionListener(e -> {
            try {
                PlantillaOperativa pl = (PlantillaOperativa) cmbPlantilla.getSelectedItem();
                if (pl == null) {
                    throw new IllegalArgumentException("Seleccione la plantilla del mercado.");
                }
                Producto p = (Producto) cmbProducto.getSelectedItem();
                Proveedor pv = (Proveedor) cmbProveedor.getSelectedItem();
                double cant = Double.parseDouble(txtCantidad.getText().trim().replace(',', '.'));
                DetalleCompra d = new DetalleCompra();
                d.setIdProducto(p.getIdProducto());
                d.setIdProveedor(pv.getIdProveedor());
                d.setCantidadComprada(cant);
                items.add(d);
                modeloItems.addRow(new Object[]{p.getNombre(), pv.getNombre(), cant, p.getAbreviaturaCompra()});
                txtCantidad.setText("");
            } catch (Exception ex) {
                UiUtil.error(this, ex);
            }
        });

        btnQuitar.addActionListener(e -> {
            int fila = tablaItems.getSelectedRow();
            if (fila >= 0) {
                items.remove(fila);
                modeloItems.removeRow(fila);
            }
        });

        btnGuardar.addActionListener(e -> {
            try {
                PlantillaOperativa pl = (PlantillaOperativa) cmbPlantilla.getSelectedItem();
                if (pl == null) {
                    throw new IllegalArgumentException("Seleccione la plantilla del mercado.");
                }
                int idCompra = servicio.registrarCompra(pl.getIdPlantilla(), null, new ArrayList<>(items));
                items.clear();
                modeloItems.setRowCount(0);
                UiUtil.mensaje(this, "Compra #" + idCompra + " registrada. La plantilla paso a estado EN_COMPRA.");
                cargarPlantillas();
                consolidar();
            } catch (Exception ex) {
                UiUtil.error(this, ex);
            }
        });

        return contenedor;
    }

    private void cargarPlantillas() {
        PlantillaOperativa sel = (PlantillaOperativa) cmbPlantilla.getSelectedItem();
        cmbPlantilla.removeAllItems();
        for (PlantillaOperativa p : plantillaDAO.listarPorEstado(
                PlantillaOperativa.ESTADO_CARGADA,
                PlantillaOperativa.ESTADO_EN_COMPRA,
                PlantillaOperativa.ESTADO_RECIBIENDO)) {
            cmbPlantilla.addItem(p);
        }
        if (sel != null) {
            for (int i = 0; i < cmbPlantilla.getItemCount(); i++) {
                if (cmbPlantilla.getItemAt(i).getIdPlantilla() == sel.getIdPlantilla()) {
                    cmbPlantilla.setSelectedIndex(i);
                }
            }
        }
    }

    private void consolidar() {
        try {
            modeloConsolidado.setRowCount(0);
            List<FilaConsolidado> filas = servicio.consolidar(txtFecha.getText().trim());
            for (FilaConsolidado f : filas) {
                modeloConsolidado.addRow(new Object[]{f.getProducto(),
                        String.format("%.2f", f.getTotalSolicitado()), f.getUnidadDespacho(),
                        String.format("%.2f", f.getTotalComprado()), f.getMercados(),
                        String.format("%.2f", f.getPendiente())});
            }
            if (filas.isEmpty()) {
                modeloConsolidado.addRow(new Object[]{"(sin demanda para la fecha)", "", "", "", "", ""});
            }
        } catch (Exception e) {
            UiUtil.error(this, e);
        }
    }
}
