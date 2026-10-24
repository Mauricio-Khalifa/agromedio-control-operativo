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
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Catalogo de productos: define la conversion entre unidad de compra y de despacho. */
public class FrmProductos extends JInternalFrame {

    private final ProductoDAO productoDAO = new ProductoDAOSqlite();
    private final CategoriaDAO categoriaDAO = new CategoriaDAOSqlite();
    private final UnidadMedidaDAO unidadDAO = new UnidadMedidaDAOSqlite();

    private final DefaultTableModel modelo;
    private final JTable tabla;
    private final JTextField txtNombre = UiUtil.campo();
    private final JTextField txtFactor = UiUtil.campo();
    private final JTextField txtBuscar = UiUtil.campo();
    private final JComboBox<Categoria> cmbCategoria = new JComboBox<>();
    private final JComboBox<UnidadMedida> cmbCompra = new JComboBox<>();
    private final JComboBox<UnidadMedida> cmbDespacho = new JComboBox<>();
    private final JLabel lblContador = UiUtil.subtitulo("");
    private final TableRowSorter<DefaultTableModel> sorter;
    private List<Producto> cache = new ArrayList<>();
    private int idSeleccionado = 0;
    private boolean cargando = false;

    public FrmProductos() {
        super("Productos", true, true, true, true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1120, 620);
        setLayout(new BorderLayout(8, 8));
        getContentPane().setBackground(Tema.FONDO);

        JPanel norte = new JPanel(new BorderLayout(8, 6));
        norte.setOpaque(false);
        norte.setBorder(BorderFactory.createEmptyBorder(8, 10, 2, 10));
        norte.add(UiUtil.encabezado("Catálogo de productos",
                "Defina categoría, unidad de compra, unidad de despacho y factor de conversión."),
                BorderLayout.WEST);

        JPanel busqueda = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        busqueda.setOpaque(false);
        busqueda.add(new JLabel("Buscar:"));
        txtBuscar.setPreferredSize(new Dimension(220, 34));
        busqueda.add(txtBuscar);
        norte.add(busqueda, BorderLayout.EAST);
        add(norte, BorderLayout.NORTH);

        modelo = UiUtil.modelo(new Object[]{"Producto", "Categoría", "U. compra", "U. despacho", "Factor"}, false);
        tabla = new JTable(modelo);
        UiUtil.prepararTabla(tabla);
        sorter = new TableRowSorter<>(modelo);
        tabla.setRowSorter(sorter);
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createLineBorder(Tema.BORDE));
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
        form.setBackground(Tema.SUPERFICIE);
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Tema.BORDE),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;

        g.gridx = 0; g.gridy = 0; g.weightx = 0;
        form.add(new JLabel("Nombre"), g);
        g.gridx = 1; g.weightx = 1;
        form.add(txtNombre, g);

        g.gridx = 0; g.gridy = 1; g.weightx = 0;
        form.add(new JLabel("Categoría"), g);
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
        form.add(new JLabel("Factor de conversión"), g);
        g.gridx = 1; g.weightx = 1;
        form.add(txtFactor, g);

        JLabel ayuda = UiUtil.subtitulo("1 unidad de compra = N unidades de despacho.");
        ayuda.setBorder(BorderFactory.createEmptyBorder(8, 6, 0, 6));
        g.gridx = 0; g.gridy = 5; g.gridwidth = 2; g.weightx = 1;
        form.add(ayuda, g);

        JPanel relleno = new JPanel();
        relleno.setOpaque(false);
        g.gridx = 0; g.gridy = 6; g.gridwidth = 2;
        g.weightx = 1; g.weighty = 1; g.fill = GridBagConstraints.BOTH;
        form.add(relleno, g);

        JButton btnNuevo = UiUtil.botonSecundario("Nuevo",
                Iconos.accion(Iconos.Accion.NUEVO, Tema.TEXTO_SUAVE));
        JButton btnGuardar = UiUtil.botonPrimario("Guardar",
                Iconos.accion(Iconos.Accion.GUARDAR, Color.WHITE));
        JButton btnEliminar = UiUtil.botonPeligro("Eliminar",
                Iconos.accion(Iconos.Accion.ELIMINAR, Color.WHITE));
        JPanel botones = new JPanel(new GridLayout(0, 1, 0, 6));
        botones.setOpaque(false);
        botones.add(btnNuevo);
        botones.add(btnGuardar);
        botones.add(btnEliminar);

        JPanel lateral = new JPanel(new BorderLayout(0, 8));
        lateral.setOpaque(false);
        lateral.setPreferredSize(new Dimension(360, 0));
        lateral.add(form, BorderLayout.CENTER);
        lateral.add(botones, BorderLayout.SOUTH);
        add(lateral, BorderLayout.EAST);

        JPanel pie = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 4));
        pie.setOpaque(false);
        pie.setBorder(BorderFactory.createEmptyBorder(0, 10, 4, 10));
        pie.add(lblContador);
        add(pie, BorderLayout.SOUTH);

        btnNuevo.addActionListener(e -> limpiar());
        btnGuardar.addActionListener(e -> guardar());
        btnEliminar.addActionListener(e -> eliminar());
        tabla.getSelectionModel().addListSelectionListener((ListSelectionEvent ev) -> seleccionar());

        txtBuscar.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filtrar();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filtrar();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filtrar();
            }
        });

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
        int fila = tabla.convertRowIndexToModel(tabla.getSelectedRow());
        Producto p = cache.get(fila);
        idSeleccionado = p.getIdProducto();
        txtNombre.setText(p.getNombre());
        txtFactor.setText(Formato.numero(p.getFactorConversion()));
        seleccionarCategoria(p.getIdCategoria());
        seleccionarUnidad(cmbCompra, p.getIdUnidadCompra());
        seleccionarUnidad(cmbDespacho, p.getIdUnidadDespacho());
    }

    private void seleccionarCategoria(int idCategoria) {
        for (int i = 0; i < cmbCategoria.getItemCount(); i++) {
            if (cmbCategoria.getItemAt(i).getIdCategoria() == idCategoria) {
                cmbCategoria.setSelectedIndex(i);
                return;
            }
        }
    }

    private void seleccionarUnidad(JComboBox<UnidadMedida> combo, int idUnidad) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (combo.getItemAt(i).getIdUnidad() == idUnidad) {
                combo.setSelectedIndex(i);
                return;
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
                throw new IllegalArgumentException("Seleccione categoría y unidades de medida.");
            }
            double factor = Formato.decimal(txtFactor.getText());
            if (factor <= 0) {
                throw new IllegalArgumentException("El factor de conversión debe ser mayor que cero.");
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

    private void filtrar() {
        String texto = txtBuscar.getText().trim();
        if (texto.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(texto)));
        }
        lblContador.setText("  " + tabla.getRowCount() + " producto(s) visibles");
    }

    private void refrescar() {
        cargando = true;
        modelo.setRowCount(0);
        cache = productoDAO.listar();
        for (Producto p : cache) {
            modelo.addRow(new Object[]{p.getNombre(), p.getNombreCategoria(),
                    p.getAbreviaturaCompra(), p.getAbreviaturaDespacho(), p.getFactorConversion()});
        }
        cargando = false;
        limpiar();
        filtrar();
    }
}
