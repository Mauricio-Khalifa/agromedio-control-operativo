package agromedio.presentacion;

import agromedio.datos.PlantillaDAO;
import agromedio.datos.PlantillaDAOSqlite;
import agromedio.datos.ProductoDAO;
import agromedio.datos.ProductoDAOSqlite;
import agromedio.datos.ProveedorDAO;
import agromedio.datos.ProveedorDAOSqlite;
import agromedio.datos.ConsultaDAOSqlite;
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
import java.awt.Color;
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
    private final DefaultTableModel modeloPedidos = UiUtil.modelo(
            new Object[]{"Pedido", "Fecha", "Proveedor", "Mercado", "Costo total", "Estado"}, false);
    private final JTable tablaPedidos = new JTable(modeloPedidos);
    private final DefaultTableModel modeloSeguimiento = UiUtil.modelo(
            new Object[]{"Plantilla / mercado", "Producto", "Solicitado", "Comprado", "Recibido", "Alistado", "Despachado", "Por despachar"}, false);
    private final JTable tablaSeguimiento = new JTable(modeloSeguimiento);
    private final JTable tablaConsolidado = new JTable(modeloConsolidado);
    private final JLabel lblConsolidado = UiUtil.subtitulo("");
    private final JButton btnPreparar = UiUtil.botonPrimario("Preparar compra pendiente",
            Iconos.accion(Iconos.Accion.ABRIR, Color.WHITE));

    // Pestaña registro
    private final JComboBox<PlantillaOperativa> cmbPlantilla = new JComboBox<>();
    private final JComboBox<Producto> cmbProducto = new JComboBox<>();
    private final JComboBox<Proveedor> cmbProveedor = new JComboBox<>();
    private final JTextField txtCantidad = UiUtil.campo();
    private final JTextField txtCostoTotal = UiUtil.campo();
    private final JLabel lblProveedoresPedido = UiUtil.subtitulo("Costo por proveedor pendiente");
    private final DefaultTableModel modeloItems = UiUtil.modelo(
            new Object[]{"Producto / presentación", "Proveedor", "Cantidad", "U. compra"}, false);
    private final JTable tablaItems = new JTable(modeloItems);
    private final List<DetalleCompra> items = new ArrayList<>();
    private List<FilaConsolidado> consolidado = new ArrayList<>();
    private int idPlantillaItems = 0;

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
        pestanias.addTab("Productos comprados / Pedidos", crearTabPedidos());
        pestanias.addTab("Seguimiento por plantilla", crearTabSeguimiento());
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
        JButton btnActualizar = UiUtil.botonSecundario("Actualizar consolidado",
                Iconos.accion(Iconos.Accion.REFRESCAR, Tema.PRIMARIO));
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

        btnActualizar.addActionListener(e -> {
            cargarPlantillas();
            consolidar();
        });
        btnPreparar.addActionListener(e -> prepararCompra());
        // Refresca las plantillas al entrar a la pestaña de registro (RF04):
        // permite ver las plantillas cargadas despues de abrir la ventana.
        pestanias.addChangeListener(e -> {
            if (pestanias.getSelectedIndex() == 1) {
                cargarPlantillas();
            }
        });
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

    private JPanel crearTabSeguimiento() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(Tema.FONDO);
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JPanel norte = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        norte.setOpaque(false);
        JButton actualizar = UiUtil.botonSecundario("Actualizar seguimiento",
                Iconos.accion(Iconos.Accion.REFRESCAR, Tema.PRIMARIO));
        norte.add(actualizar);
        panel.add(norte, BorderLayout.NORTH);
        UiUtil.prepararTabla(tablaSeguimiento);
        panel.add(new JScrollPane(tablaSeguimiento), BorderLayout.CENTER);
        Runnable cargar = () -> {
            modeloSeguimiento.setRowCount(0);
            for (agromedio.modelo.FilaSaldo f : new ConsultaDAOSqlite().saldoTotal()) {
                modeloSeguimiento.addRow(new Object[]{f.getNombreMercado(), f.getProducto(), f.getSolicitado(),
                        f.getComprado(), f.getRecibido(), f.getAlistado(), f.getDespachado(), f.getPorDespachar()});
            }
        };
        actualizar.addActionListener(e -> cargar.run());
        cargar.run();
        return panel;
    }

    private JPanel crearTabPedidos() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(Tema.FONDO);
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JPanel norte = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        norte.setOpaque(false);
        JButton actualizar = UiUtil.botonSecundario("Actualizar pedidos",
                Iconos.accion(Iconos.Accion.REFRESCAR, Tema.PRIMARIO));
        JButton detalle = UiUtil.botonSecundario("Ver detalle",
                Iconos.accion(Iconos.Accion.ABRIR, Tema.PRIMARIO));
        JButton anular = UiUtil.botonSecundario("Anular pedido",
                Iconos.accion(Iconos.Accion.QUITAR, Tema.PRIMARIO));
        norte.add(actualizar); norte.add(detalle); norte.add(anular);
        panel.add(norte, BorderLayout.NORTH);
        UiUtil.prepararTabla(tablaPedidos);
        panel.add(new JScrollPane(tablaPedidos), BorderLayout.CENTER);
        Runnable cargar = () -> {
            modeloPedidos.setRowCount(0);
            for (agromedio.modelo.Compra c : new agromedio.datos.CompraDAOSqlite().listar()) {
                modeloPedidos.addRow(new Object[]{c.getIdCompra(), c.getFechaCompra(),
                        c.getNombreProveedor() == null ? "Sin proveedor" : c.getNombreProveedor(),
                        c.getNombreMercado() == null ? "Consolidado" : c.getNombreMercado(),
                        c.getCostoTotal(), c.getEstado()});
            }
        };
        actualizar.addActionListener(e -> cargar.run());
        detalle.addActionListener(e -> {
            int fila = tablaPedidos.getSelectedRow();
            if (fila < 0) { UiUtil.error(this, "Seleccione un pedido."); return; }
            int id = ((Number) modeloPedidos.getValueAt(fila, 0)).intValue();
            StringBuilder sb = new StringBuilder("Detalle del pedido #").append(id).append("\\n\\n");
            for (DetalleCompra d : servicio.detalle(id)) {
                sb.append(d.getNombreProducto()).append(" | ").append(d.getCantidadComprada())
                  .append(" ").append(d.getAbreviatura()).append(" | Subtotal: ").append(d.getSubtotal()).append("\\n");
            }
            UiUtil.mensaje(this, sb.toString());
        });
        anular.addActionListener(e -> {
            int fila = tablaPedidos.getSelectedRow();
            if (fila < 0) { UiUtil.error(this, "Seleccione un pedido."); return; }
            int id = ((Number) modeloPedidos.getValueAt(fila, 0)).intValue();
            String estado = String.valueOf(modeloPedidos.getValueAt(fila, 5));
            if (!agromedio.modelo.Compra.REGISTRADA.equals(estado)) {
                UiUtil.error(this, "Solo se pueden anular pedidos registrados y sin recepción."); return;
            }
            new agromedio.datos.CompraDAOSqlite().actualizarEstado(id, agromedio.modelo.Compra.ANULADA);
            cargar.run(); consolidar();
        });
        cargar.run();
        return panel;
    }

    private JPanel crearTabRegistro() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(Tema.FONDO);
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        for (Producto p : productoDAO.listar()) {
            cmbProducto.addItem(p);
        }
        cmbProducto.setRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Producto) {
                    setText(textoProducto((Producto) value));
                }
                return this;
            }
        });
        // El producto no se selecciona manualmente: siempre llega desde una
        // fila elegida en el consolidado mediante "Preparar compra".
        cmbProducto.setEnabled(false);
        cmbProducto.setToolTipText("Seleccione el producto desde el consolidado de demanda.");
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
        form.add(new JLabel("Pedido consolidado"), g);
        g.gridx = 1; g.weightx = 1;
        form.add(new JLabel("Los productos se toman del consolidado y se distribuyen internamente por plantilla."), g);

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

        g.gridx = 0; g.gridy = 4; g.weightx = 0;
        form.add(new JLabel("Costo total del pedido"), g);
        g.gridx = 1; g.weightx = 1;
        form.add(txtCostoTotal, g);
        g.gridx = 2; g.gridy = 4; g.weightx = 1;
        form.add(lblProveedoresPedido, g);

        JButton btnAgregar = UiUtil.botonSecundario("Agregar item",
                Iconos.accion(Iconos.Accion.AGREGAR, Tema.PRIMARIO));
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
        JButton btnQuitar = UiUtil.botonSecundario("Quitar item",
                Iconos.accion(Iconos.Accion.QUITAR, Tema.PRIMARIO));
        JButton btnGuardar = UiUtil.botonPrimario("Guardar compra",
                Iconos.accion(Iconos.Accion.GUARDAR, Color.WHITE));
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
                Producto p = (Producto) cmbProducto.getSelectedItem();
                if (p == null) {
                    throw new IllegalArgumentException("Seleccione el producto desde el consolidado de demanda.");
                }
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
                modeloItems.addRow(new Object[]{textoProducto(p), pv.getNombre(), cant, p.getAbreviaturaCompra()});
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
                if (items.isEmpty()) {
                    throw new IllegalArgumentException("Seleccione un producto desde el consolidado y agréguelo al pedido.");
                }
                double costo = txtCostoTotal.getText().trim().isEmpty()
                        ? 0 : Formato.decimal(txtCostoTotal.getText());
                java.util.Map<Integer, Double> costos = new java.util.LinkedHashMap<>();
                for (DetalleCompra d : items) {
                    costos.put(d.getIdProveedor(), costo);
                }
                java.util.List<Integer> pedidos = servicio.registrarPedidosPorProveedor(null, costos, new ArrayList<>(items));
                items.clear();
                modeloItems.setRowCount(0);
                txtCostoTotal.setText("");
                idPlantillaItems = 0;
                UiUtil.mensaje(this, pedidos.size() + " pedido(s) registrado(s), agrupado(s) automáticamente por proveedor.");
                cargarPlantillas();
                consolidar();
            } catch (Exception ex) {
                UiUtil.error(this, ex);
            }
        });

        return contenedor;
    }

    private String textoProducto(Producto p) {
        String nombre = p.getNombre();
        String unidad = p.getAbreviaturaCompra();
        if (unidad == null || unidad.trim().isEmpty()) {
            unidad = p.getNombreUnidadCompra();
        }
        return nombre + " [" + unidad + ", factor " + Formato.numero(p.getFactorConversion()) + "]";
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
                modeloConsolidado.addRow(new Object[]{"(sin demanda para la fecha indicada)", "", "", "", "", ""});
                UiUtil.mensaje(this, "No hay plantillas activas con demanda para la fecha "
                        + txtFecha.getText().trim() + ".\\n"
                        + "Verifique la fecha (debe coincidir con la fecha programada de la plantilla) "
                        + "o cargue una plantilla con esa fecha.");
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
