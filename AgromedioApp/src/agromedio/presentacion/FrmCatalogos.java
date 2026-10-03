package agromedio.presentacion;

import javax.swing.BorderFactory;
import javax.swing.JButton;
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

/** Ventana generica de CRUD para catalogos (reutilizada con polimorfismo). */
public class FrmCatalogos extends JInternalFrame {

    private final AdaptadorCatalogo catalogo;
    private final DefaultTableModel modelo;
    private final JTable tabla;
    private final JTextField[] campos;
    private int idSeleccionado = 0;
    private boolean cargando = false;

    public FrmCatalogos(AdaptadorCatalogo catalogo) {
        super(catalogo.titulo(), true, true, true, true);
        this.catalogo = catalogo;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(760, 520);
        setLayout(new BorderLayout(8, 8));

        modelo = UiUtil.modelo(catalogo.columnasTabla(), false);
        tabla = new JTable(modelo);
        UiUtil.prepararTabla(tabla);
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createTitledBorder("Registros"));
        add(scroll, BorderLayout.CENTER);

        String[] etiquetas = catalogo.etiquetasCampos();
        campos = new JTextField[etiquetas.length];
        JPanel panelForm = new JPanel(new GridBagLayout());
        panelForm.setBorder(BorderFactory.createTitledBorder("Datos del registro"));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.anchor = GridBagConstraints.WEST;
        for (int i = 0; i < etiquetas.length; i++) {
            g.gridx = 0;
            g.gridy = i;
            g.weightx = 0;
            panelForm.add(new JLabel(etiquetas[i]), g);
            campos[i] = UiUtil.campo();
            g.gridx = 1;
            g.weightx = 1;
            g.fill = GridBagConstraints.HORIZONTAL;
            panelForm.add(campos[i], g);
        }

        JButton btnNuevo = UiUtil.boton("Nuevo");
        JButton btnGuardar = UiUtil.boton("Guardar");
        JButton btnEliminar = UiUtil.boton("Eliminar");
        JPanel panelBotones = new JPanel();
        panelBotones.add(btnNuevo);
        panelBotones.add(btnGuardar);
        panelBotones.add(btnEliminar);

        JPanel sur = new JPanel(new BorderLayout());
        sur.add(panelForm, BorderLayout.CENTER);
        sur.add(panelBotones, BorderLayout.SOUTH);
        add(sur, BorderLayout.SOUTH);

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
            int fila = tabla.getSelectedRow();
            idSeleccionado = catalogo.idFila(fila);
            String[] valores = catalogo.camposFila(fila);
            for (int i = 0; i < campos.length && i < valores.length; i++) {
                campos[i].setText(valores[i]);
            }
        });

        refrescar();
        UiUtil.fuenteRaiz(this);
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
    }
}
