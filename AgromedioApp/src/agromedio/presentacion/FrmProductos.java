package agromedio.presentacion;

import agromedio.datos.CategoriaDAO;
import agromedio.datos.CategoriaDAOSqlite;
import agromedio.datos.ProductoDAO;
import agromedio.datos.ProductoDAOSqlite;
import agromedio.datos.UnidadMedidaDAO;
import agromedio.datos.UnidadMedidaDAOSqlite;
import agromedio.modelo.Categoria;
import agromedio.modelo.Producto;
import agromedio.modelo.UnidadMedida;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

/** Catalogo de productos: define la conversion entre unidad de compra y de despacho. */
public class FrmProductos extends JInternalFrame {

    private final ProductoDAO productoDAO = new ProductoDAOSqlite();
    private final CategoriaDAO categoriaDAO = new CategoriaDAOSqlite();
    private final UnidadMedidaDAO unidadDAO = new UnidadMedidaDAOSqlite();

    private final DefaultTableModel modelo;
    private final JTable tabla;
    private final JTextField txtNombre = UiUtil.campo();
    private final JTextField txtFactor = UiUtil.campo();
    private final JComboBox<Categoria> cmbCategoria = new JComboBox<>();
    private final JComboBox<UnidadMedida> cmbCompra = new JComboBox<>();
    private final JComboBox<UnidadMedida> cmbDespacho = new JComboBox<>();
    private final List<Producto>[] cache = new List[1];
    private int idSeleccionado = 0;
    private boolean cargando = false;

    @SuppressWarnings("unchecked")
    public FrmProductos() {
        super("Productos", true, true, true, true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(900, 560);
        setLayout(new BorderLayout(8, 8));

        modelo = UiUtil.modelo(new Object[]{"ID", "Producto", "Categoria", "U. compra", "U. despacho", "Factor"}, false);
        tabla = new JTable(modelo);
        UiUtil.prepararTabla(tabla);
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createTitledBorder("Catalogo de productos"));
        add(scroll, BorderLayout.CENTER);

        for (Categoria c : categoriaDAO.listar()) {
            cmbCategoria.addItem(c);
        }
        List<UnidadMedida> unidades = unidadDAO.listar();
        for (UnidadMedida u : unidades) {
            cmbCompra.addItem(u);
            cmbDespacho.addItem(u);
        }

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Datos del producto"));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;

        g.gridx = 0; g.gridy = 0;
        form.add(new JLabel("Nombre"), g);
        g.gridx = 1; g.weightx = 1;
        form.add(txtNombre, g);

        g.gridx = 0; g.gridy = 1; g.weightx = 0;
        form.add(new JLabel("Categoria"), g);
        g.gridx = 1; g.weightx = 1;
        form.add(cmbCategoria, g);

        g.gridx = 0; g.gridy = 2; g.weightx = 0;
        form.add(new JLabel("Se compra en"), g);
        g.gridx = 1; g.weightx = 1;
        form.add(cmbCompra, g);

        g.gridx = 0; g.gridy = 3; g.weightx = 0;
        form.add(new JLabel("Se despacha en"), g);
        g.gridx = 1; g.weightx = 1;
        form.add(cmbDespacho, g);

        g.gridx = 0; g.gridy = 4; g.weightx = 0;
        form.add(new JLabel("Factor conversion (1 compra = N despacho)"), g);
        g.gridx = 1; g.weightx = 1;
        form.add(txtFactor, g);

        JButton btnNuevo = UiUtil.boton("Nuevo");
        JButton btnGuardar = UiUtil.boton("Guardar");
        JButton btnEliminar = UiUtil.boton("Eliminar");
        JPanel botones = new JPanel();
        botones.add(btnNuevo);
        botones.add(btnGuardar);
        botones.add(btnEliminar);

        JPanel sur = new JPanel(new BorderLayout());
        sur.add(form, BorderLayout.CENTER);
        sur.add(botones, BorderLayout.SOUTH);
        add(sur, BorderLayout.SOUTH);

        btnNuevo.addActionListener(e -> limpiar());
        btnGuardar.addActionListener(e -> guardar());
        btnEliminar.addActionListener(e -> eliminar());
        tabla.getSelectionModel().addListSelectionListener((ListSelectionEvent ev) -> seleccionar());

        refrescar();
        UiUtil.fuenteRaiz(this);
    }

    private void limpiar() {
        idSeleccionado = 0;
        txtNombre.setText("");
        txtFactor.setText("1");
        tabla.clearSelection();
        txtNombre.requestFocusInWindow();
    }

    private void seleccionar() {
        if (cargando || tabla.getSelectedRow() < 0) {
            return;
        }
        int fila = tabla.getSelectedRow();
        idSeleccionado = cache[0].get(fila).getIdProducto();
        Producto p = productoDAO.obtener(idSeleccionado);
        if (p == null) {
            return;
        }
        txtNombre.setText(p.getNombre());
        txtFactor.setText(String.valueOf(p.getFactorConversion()));
        cmbCategoria.setSelectedItem(categoriaDAO.listar().stream()
                .filter(c -> c.getIdCategoria() == p.getIdCategoria()).findFirst().orElse(null));
        for (int i = 0; i < cmbCompra.getItemCount(); i++) {
            if (cmbCompra.getItemAt(i).getIdUnidad() == p.getIdUnidadCompra()) {
                cmbCompra.setSelectedIndex(i);
            }
            if (cmbDespacho.getItemAt(i).getIdUnidad() == p.getIdUnidadDespacho()) {
                cmbDespacho.setSelectedIndex(i);
            }
        }
    }

    private void guardar() {
        try {
            Producto p = new Producto();
            p.setIdProducto(idSeleccionado);
            p.setNombre(txtNombre.getText().trim());
            if (p.getNombre().isEmpty()) {
                throw new IllegalArgumentException("El nombre del producto es obligatorio.");
            }
            Categoria cat = (Categoria) cmbCategoria.getSelectedItem();
            UnidadMedida uc = (UnidadMedida) cmbCompra.getSelectedItem();
            UnidadMedida ud = (UnidadMedida) cmbDespacho.getSelectedItem();
            if (cat == null || uc == null || ud == null) {
                throw new IllegalArgumentException("Seleccione categoria y unidades de medida.");
            }
            double factor;
            try {
                factor = Double.parseDouble(txtFactor.getText().trim().replace(',', '.'));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("El factor de conversion debe ser un numero (ej. 50).");
            }
            if (factor <= 0) {
                throw new IllegalArgumentException("El factor de conversion debe ser mayor que cero.");
            }
            p.setIdCategoria(cat.getIdCategoria());
            p.setIdUnidadCompra(uc.getIdUnidad());
            p.setIdUnidadDespacho(ud.getIdUnidad());
            p.setFactorConversion(factor);
            productoDAO.guardar(p);
            UiUtil.mensaje(this, "Producto guardado correctamente.");
            refrescar();
        } catch (Exception e) {
            UiUtil.error(this, e);
        }
    }

    private void eliminar() {
        if (idSeleccionado == 0) {
            UiUtil.error(this, "Seleccione un producto de la tabla.");
            return;
        }
        if (UiUtil.confirmar(this, "¿Eliminar el producto seleccionado?")) {
            try {
                productoDAO.eliminar(idSeleccionado);
                idSeleccionado = 0;
                refrescar();
            } catch (Exception e) {
                UiUtil.error(this, e);
            }
        }
    }

    private void refrescar() {
        cargando = true;
        modelo.setRowCount(0);
        cache[0] = productoDAO.listar();
        for (Producto p : cache[0]) {
            modelo.addRow(new Object[]{p.getIdProducto(), p.getNombre(), p.getNombreCategoria(),
                    p.getAbreviaturaCompra(), p.getAbreviaturaDespacho(), p.getFactorConversion()});
        }
        cargando = false;
        limpiar();
    }
}
