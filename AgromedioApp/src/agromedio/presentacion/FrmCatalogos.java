package agromedio.presentacion;

import javax.swing.BorderFactory;
import javax.swing.JButton;
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
import java.util.regex.Pattern;

/** Ventana generica de CRUD para catalogos (reutilizada con polimorfismo). */
public class FrmCatalogos extends JInternalFrame {

    private final AdaptadorCatalogo catalogo;
    private final DefaultTableModel modelo;
    private final JTable tabla;
    private final JTextField[] campos;
    private final JTextField txtBuscar = UiUtil.campo();
    private final JLabel lblContador = UiUtil.subtitulo("");
    private final TableRowSorter<DefaultTableModel> sorter;
    private int idSeleccionado = 0;
    private boolean cargando = false;

    public FrmCatalogos(AdaptadorCatalogo catalogo) {
        super(catalogo.titulo(), true, true, true, true);
        this.catalogo = catalogo;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1040, 600);
        setLayout(new BorderLayout(8, 8));
        getContentPane().setBackground(Tema.FONDO);

        JPanel norte = new JPanel(new BorderLayout(8, 6));
        norte.setOpaque(false);
        norte.setBorder(BorderFactory.createEmptyBorder(8, 10, 2, 10));
        norte.add(UiUtil.encabezado(catalogo.titulo(),
                "Seleccione un registro para editar o use Nuevo para crearlo."), BorderLayout.WEST);

        JPanel busqueda = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        busqueda.setOpaque(false);
        busqueda.add(new JLabel("Buscar:"));
        txtBuscar.setPreferredSize(new Dimension(220, 34));
        busqueda.add(txtBuscar);
        norte.add(busqueda, BorderLayout.EAST);
        add(norte, BorderLayout.NORTH);

        modelo = UiUtil.modelo(catalogo.columnasTabla(), false);
        tabla = new JTable(modelo);
        UiUtil.prepararTabla(tabla);
        sorter = new TableRowSorter<>(modelo);
        tabla.setRowSorter(sorter);
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createLineBorder(Tema.BORDE));
        add(scroll, BorderLayout.CENTER);

        String[] etiquetas = catalogo.etiquetasCampos();
        campos = new JTextField[etiquetas.length];
        JPanel panelForm = new JPanel(new GridBagLayout());
        panelForm.setBackground(Tema.SUPERFICIE);
        panelForm.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Tema.BORDE),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.anchor = GridBagConstraints.WEST;
        for (int i = 0; i < etiquetas.length; i++) {
            g.gridx = 0;
            g.gridy = i;
            g.weightx = 0;
            g.fill = GridBagConstraints.NONE;
            panelForm.add(new JLabel(etiquetas[i]), g);
            campos[i] = UiUtil.campo();
            g.gridx = 1;
            g.weightx = 1;
            g.fill = GridBagConstraints.HORIZONTAL;
            panelForm.add(campos[i], g);
        }
        JPanel relleno = new JPanel();
        relleno.setOpaque(false);
        g.gridx = 0;
        g.gridy = etiquetas.length;
        g.gridwidth = 2;
        g.weightx = 1;
        g.weighty = 1;
        g.fill = GridBagConstraints.BOTH;
        panelForm.add(relleno, g);

        JButton btnNuevo = UiUtil.botonSecundario("Nuevo",
                Iconos.accion(Iconos.Accion.NUEVO, Tema.TEXTO_SUAVE));
        JButton btnGuardar = UiUtil.botonPrimario("Guardar",
                Iconos.accion(Iconos.Accion.GUARDAR, Color.WHITE));
        JButton btnEliminar = UiUtil.botonPeligro("Eliminar",
                Iconos.accion(Iconos.Accion.ELIMINAR, Color.WHITE));
        JPanel panelBotones = new JPanel(new GridLayout(0, 1, 0, 6));
        panelBotones.setOpaque(false);
        panelBotones.add(btnNuevo);
        panelBotones.add(btnGuardar);
        panelBotones.add(btnEliminar);

        JPanel lateral = new JPanel(new BorderLayout(0, 8));
        lateral.setOpaque(false);
        lateral.setPreferredSize(new Dimension(330, 0));
        lateral.add(panelForm, BorderLayout.CENTER);
        lateral.add(panelBotones, BorderLayout.SOUTH);
        add(lateral, BorderLayout.EAST);

        JPanel pie = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 4));
        pie.setOpaque(false);
        pie.setBorder(BorderFactory.createEmptyBorder(0, 10, 4, 10));
        pie.add(lblContador);
        add(pie, BorderLayout.SOUTH);

        btnNuevo.addActionListener(e -> {
            idSeleccionado = 0;
            for (JTextField c : campos) {
                c.setText("");
            }
            tabla.clearSelection();
            campos[0].requestFocusInWindow();
        });

        btnGuardar.addActionListener(e -> {
            try {
                String[] valores = new String[campos.length];
                for (int i = 0; i < campos.length; i++) {
                    valores[i] = campos[i].getText();
                }
                catalogo.guardar(idSeleccionado, valores);
                UiUtil.mensaje(this, "Registro guardado correctamente.");
                refrescar();
            } catch (Exception ex) {
                UiUtil.error(this, ex);
            }
        });

        btnEliminar.addActionListener(e -> {
            if (idSeleccionado == 0) {
                UiUtil.error(this, "Seleccione un registro de la tabla para eliminarlo.");
                return;
            }
            if (UiUtil.confirmar(this, "¿Desea eliminar el registro seleccionado?")) {
                try {
                    catalogo.eliminar(idSeleccionado);
                    idSeleccionado = 0;
                    refrescar();
                } catch (Exception ex) {
                    UiUtil.error(this, ex);
                }
            }
        });

        tabla.getSelectionModel().addListSelectionListener((ListSelectionEvent ev) -> {
            if (cargando || tabla.getSelectedRow() < 0) {
                return;
            }
            int fila = tabla.convertRowIndexToModel(tabla.getSelectedRow());
            idSeleccionado = catalogo.idFila(fila);
            String[] valores = catalogo.camposFila(fila);
            for (int i = 0; i < campos.length && i < valores.length; i++) {
                campos[i].setText(valores[i]);
            }
        });

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

    private void filtrar() {
        String texto = txtBuscar.getText().trim();
        if (texto.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(texto)));
        }
        lblContador.setText("  " + tabla.getRowCount() + " registro(s) visibles");
    }

    public final void refrescar() {
        cargando = true;
        modelo.setRowCount(0);
        for (Object[] fila : catalogo.filas()) {
            modelo.addRow(fila);
        }
        cargando = false;
        idSeleccionado = 0;
        for (JTextField c : campos) {
            c.setText("");
        }
        tabla.clearSelection();
        filtrar();
    }
}
