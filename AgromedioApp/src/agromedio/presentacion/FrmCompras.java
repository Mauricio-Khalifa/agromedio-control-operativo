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
    private final JTabbedPane pestanias = new JTabbedPane();
    private final JTable tablaConsolidado = new JTable(modeloConsolidado);
    private final JLabel lblConsolidado = UiUtil.subtitulo("");
    private final JButton btnPreparar = UiUtil.botonPrimario("Preparar compra pendiente");

    // Pestaña registro
    private final JComboBox<PlantillaOperativa> cmbPlantilla = new JComboBox<>();
    private final JComboBox<Producto> cmbProducto = new JComboBox<>();
    private final JComboBox<Proveedor> cmbProveedor = new JComboBox<>();
    private final JTextField txtCantidad = UiUtil.campo();
    private final DefaultTableModel modeloItems = UiUtil.modelo(
            new Object[]{"Producto", "Proveedor", "Cantidad", "U. compra"}, false);
    private final JTable tablaItems = new JTable(modeloItems);
    private final List<DetalleCompra> items = new ArrayList<>();
    private List<FilaConsolidado> consolidado = new ArrayList<>();

    public FrmCompras() {
        super("Compras", true, true, true, true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1120, 680);
        setLayout(new BorderLayout(8, 8));
        getContentPane().setBackground(Tema.FONDO);

        JPanel norte = new JPanel(new BorderLayout());
        norte.setOpaque(false);
        norte.setBorder(BorderFactory.createEmptyBorder(8, 10, 0, 10));
        norte.add(UiUtil.encabezado("Compras",
                "Consolide la demanda del día y registre la compra del mercado seleccionado."),
                BorderLayout.WEST);
        add(norte, BorderLayout.NORTH);

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
        panel.setBackground(Tema.FONDO);
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel norte = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 4));
        norte.setOpaque(false);
        norte.add(new JLabel("Fecha programada"));
        txtFecha.setText(LocalDate.now().toString());
        txtFecha.setPreferredSize(new java.awt.Dimension(150, 34));
        norte.add(txtFecha);
        JButton btnActualizar = UiUtil.botonSecundario("Actualizar consolidado");
        norte.add(btnActualizar);
        norte.add(btnPreparar);
        panel.add(norte, BorderLayout.NORTH);

        UiUtil.prepararTabla(tablaConsolidado);
        JScrollPane scroll = new JScrollPane(tablaConsolidado);
        scroll.setBorder(BorderFactory.createLineBorder(Tema.BORDE));
        panel.add(scroll, BorderLayout.CENTER);

        JPanel pie = new JPanel(new BorderLayout());
        pie.setOpaque(false);
        JLabel ayuda = UiUtil.subtitulo("Doble clic o 'Preparar compra' para llevar el pendiente a la pestaña de registro.");
        pie.add(ayuda, BorderLayout.WEST);
        pie.add(lblConsolidado, BorderLayout.EAST);
        panel.add(pie, BorderLayout.SOUTH);

        btnActualizar.addActionListener(e -> consolidar());
        btnPreparar.addActionListener(e -> prepararCompra());
        btnPreparar.setEnabled(false);
        tablaConsolidado.getSelectionModel().addListSelectionListener(e -> actualizarBotonPreparar());
        tablaConsolidado.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    prepararCompra();
                }
            }
        });
        return panel;
    }

    private JPanel crearTabRegistro() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(Tema.FONDO);
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        for (Producto p : productoDAO.listar()) {
            cmbProducto.addItem(p);
        }
        for (Proveedor pr : proveedorDAO.listar()) {
            cmbProveedor.addItem(pr);
        }

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Tema.SUPERFICIE);
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Tema.BORDE),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 6, 5, 6);
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

        JButton btnAgregar = UiUtil.botonSecundario("Agregar item");
        JPanel sur = new JPanel(new BorderLayout());
        sur.setOpaque(false);
        sur.add(form, BorderLayout.CENTER);
        JPanel filaBotones = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 6, 4));
        filaBotones.setOpaque(false);
        filaBotones.add(btnAgregar);
        sur.add(filaBotones, BorderLayout.SOUTH);

        UiUtil.prepararTabla(tablaItems);
        JScrollPane scroll = new JScrollPane(tablaItems);
        scroll.setBorder(BorderFactory.createLineBorder(Tema.BORDE));

        JPanel botonesFinales = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 4));
        botonesFinales.setOpaque(false);
        JButton btnQuitar = UiUtil.botonSecundario("Quitar item");
        JButton btnGuardar = UiUtil.botonPrimario("Guardar compra");
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
                if (p == null || pv == null) {
                    throw new IllegalArgumentException("Seleccione producto y proveedor.");
                }
                double cant = Formato.decimal(txtCantidad.getText());
                if (cant <= 0) {
                    throw new IllegalArgumentException("La cantidad debe ser mayor que cero.");
                }
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
                UiUtil.mensaje(this, "Compra #" + idCompra + " registrada. La plantilla pasó a estado EN_COMPRA.");
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
            consolidado = servicio.consolidar(txtFecha.getText().trim());
            for (FilaConsolidado f : consolidado) {
                modeloConsolidado.addRow(new Object[]{f.getProducto(),
                        f.getTotalSolicitado(), f.getUnidadDespacho(),
                        f.getTotalComprado(), f.getMercados(),
                        f.getPendiente()});
            }
            lblConsolidado.setText(consolidado.size() + " producto(s) en el consolidado");
            if (consolidado.isEmpty()) {
                modeloConsolidado.addRow(new Object[]{"(sin demanda para la fecha)", "", "", "", "", ""});
            }
            tablaConsolidado.clearSelection();
            actualizarBotonPreparar();
        } catch (Exception e) {
            UiUtil.error(this, e);
        }
    }

    private void actualizarBotonPreparar() {
        int fila = tablaConsolidado.getSelectedRow();
        boolean disponible = fila >= 0 && fila < consolidado.size()
                && consolidado.get(fila).getPendiente() > 0;
        btnPreparar.setEnabled(disponible);
    }

    private void prepararCompra() {
        int fila = tablaConsolidado.getSelectedRow();
        if (fila < 0 || consolidado.isEmpty()) {
            UiUtil.error(this, "Seleccione un producto del consolidado.");
            return;
        }
        FilaConsolidado f = consolidado.get(fila);
        if (f.getPendiente() <= 0) {
            UiUtil.error(this, "El producto seleccionado no tiene pendiente por comprar.");
            return;
        }
        for (int i = 0; i < cmbProducto.getItemCount(); i++) {
            if (cmbProducto.getItemAt(i).getIdProducto() == f.getIdProducto()) {
                cmbProducto.setSelectedIndex(i);
                break;
            }
        }
        double cantidadCompra = f.getFactorConversion() > 0
                ? f.getPendiente() / f.getFactorConversion() : f.getPendiente();
        txtCantidad.setText(Formato.numero(cantidadCompra));
        pestanias.setSelectedIndex(1);
        txtCantidad.requestFocusInWindow();
    }
}
