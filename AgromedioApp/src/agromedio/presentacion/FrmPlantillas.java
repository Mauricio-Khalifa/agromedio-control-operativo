package agromedio.presentacion;

import agromedio.datos.ClienteDAO;
import agromedio.datos.ClienteDAOSqlite;
import agromedio.datos.PlantillaDAO;
import agromedio.datos.PlantillaDAOSqlite;
import agromedio.logica.PlantillaServicio;
import agromedio.modelo.Cliente;
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
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.util.List;

/** Panel de plantillas operativas: alta manual y acceso al detalle. */
public class FrmPlantillas extends JInternalFrame {

    private final PlantillaDAO plantillaDAO = new PlantillaDAOSqlite();
    private final ClienteDAO clienteDAO = new ClienteDAOSqlite();
    private final PlantillaServicio servicio = new PlantillaServicio();

    private final DefaultTableModel modelo;
    private final JTable tabla;
    private final JLabel lblResumen = UiUtil.subtitulo("");
    private List<PlantillaOperativa> cache;
    private boolean cargando = false;
    private int idSeleccionado = 0;
    private Navegador navegador;

    public FrmPlantillas() {
        super("Plantillas operativas", true, true, true, true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1020, 620);
        setLayout(new BorderLayout(8, 8));
        getContentPane().setBackground(Tema.FONDO);

        JPanel norte = new JPanel(new BorderLayout());
        norte.setOpaque(false);
        norte.setBorder(BorderFactory.createEmptyBorder(8, 10, 2, 10));
        norte.add(UiUtil.encabezado("Plantillas operativas",
                "Programación por mercado, cliente y fecha. Doble clic para ver el detalle."),
                BorderLayout.WEST);
        add(norte, BorderLayout.NORTH);

        modelo = UiUtil.modelo(new Object[]{"Mercado", "Cliente", "Fecha programada", "Estado"}, false);
        tabla = new JTable(modelo);
        UiUtil.prepararTabla(tabla);
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createLineBorder(Tema.BORDE));
        add(scroll, BorderLayout.CENTER);

        JButton btnNueva = UiUtil.botonPrimario("Nueva plantilla",
                Iconos.accion(Iconos.Accion.NUEVO, Color.WHITE));
        JButton btnDetalle = UiUtil.botonSecundario("Ver detalle",
                Iconos.accion(Iconos.Accion.VER, Tema.PRIMARIO));
        JButton btnRefrescar = UiUtil.botonSecundario("Refrescar",
                Iconos.accion(Iconos.Accion.REFRESCAR, Tema.PRIMARIO));
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        botones.setOpaque(false);
        botones.setBorder(BorderFactory.createEmptyBorder(6, 10, 8, 10));
        botones.add(btnNueva);
        botones.add(btnDetalle);
        botones.add(btnRefrescar);
        botones.add(lblResumen);
        add(botones, BorderLayout.SOUTH);

        btnNueva.addActionListener(e -> nuevaPlantilla());
        btnDetalle.addActionListener(e -> abrirDetalle());
        btnRefrescar.addActionListener(e -> refrescar());
        tabla.getSelectionModel().addListSelectionListener((ListSelectionEvent e) -> {
            if (!cargando && tabla.getSelectedRow() >= 0) {
                idSeleccionado = cache.get(tabla.getSelectedRow()).getIdPlantilla();
            }
        });
        tabla.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    abrirDetalle();
                }
            }
        });

        refrescar();
        UiUtil.fuenteRaiz(this);
    }

    void setNavegador(Navegador navegador) {
        this.navegador = navegador;
    }

    private void nuevaPlantilla() {
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.anchor = GridBagConstraints.WEST;

        JComboBox<Cliente> cmbCliente = new JComboBox<>();
        for (Cliente c : clienteDAO.listar()) {
            cmbCliente.addItem(c);
        }
        JTextField txtMercado = UiUtil.campo();
        JTextField txtFecha = UiUtil.campo();
        txtFecha.setText(LocalDate.now().toString());

        g.gridx = 0; g.gridy = 0;
        form.add(new JLabel("Cliente"), g);
        g.gridx = 1;
        form.add(cmbCliente, g);
        g.gridx = 0; g.gridy = 1;
        form.add(new JLabel("Nombre del mercado"), g);
        g.gridx = 1;
        form.add(txtMercado, g);
        g.gridx = 0; g.gridy = 2;
        form.add(new JLabel("Fecha programada (aaaa-mm-dd)"), g);
        g.gridx = 1;
        form.add(txtFecha, g);

        int opcion = JOptionPane.showConfirmDialog(this, form, "Nueva plantilla operativa",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (opcion != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            Cliente cli = (Cliente) cmbCliente.getSelectedItem();
            servicio.crear(cli == null ? 0 : cli.getIdCliente(), txtMercado.getText(), txtFecha.getText());
            UiUtil.mensaje(this, "Plantilla creada. Abra el detalle para agregar los productos solicitados.");
            refrescar();
        } catch (Exception e) {
            UiUtil.error(this, e);
        }
    }

    private void abrirDetalle() {
        if (idSeleccionado == 0) {
            UiUtil.error(this, "Seleccione una plantilla de la tabla.");
            return;
        }
        int id = idSeleccionado;
        if (navegador != null) {
            navegador.abrir("detalle-plantilla-" + id, () -> new FrmDetallePlantilla(id));
        } else if (getDesktopPane() != null) {
            getDesktopPane().add(new FrmDetallePlantilla(id));
        }
    }

    final void refrescar() {
        cargando = true;
        modelo.setRowCount(0);
        cache = plantillaDAO.listar();
        for (PlantillaOperativa p : cache) {
            modelo.addRow(new Object[]{p.getNombreMercado(), p.getNombreCliente(),
                    p.getFechaProgramada(), p.getEstado()});
        }
        cargando = false;
        idSeleccionado = 0;
        tabla.clearSelection();
        lblResumen.setText("  " + cache.size() + " plantillas registradas");
    }
}
