package agromedio.presentacion;

import agromedio.datos.PlantillaDAO;
import agromedio.datos.PlantillaDAOSqlite;
import agromedio.logica.DespachoServicio;
import agromedio.modelo.DetalleDespacho;
import agromedio.modelo.Despacho;
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
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

/** Modulo 4 - Despacho: checklist paso a paso (RF09) y cierre al 100% (RF10). */
public class FrmDespacho extends JInternalFrame {

    private static final int COL_PRODUCTO = 0;
    private static final int COL_CANTIDAD = 2;
    private static final int COL_VERIFICADO = 4;
    private static final int COL_FALTANTE = 5;

    private final DespachoServicio servicio = new DespachoServicio();
    private final PlantillaDAO plantillaDAO = new PlantillaDAOSqlite();

    private final JComboBox<PlantillaOperativa> cmbPlantilla = new JComboBox<>();
    private final JLabel lblProgreso = new JLabel("0 / 0 ítems verificados (0%)");
    private final JLabel lblSeleccion = UiUtil.subtitulo("Seleccione un ítem para verificar.");
    private final DefaultTableModel modelo;
    private final JButton btnVerificar = UiUtil.botonPrimario("Verificar ítem",
            Iconos.accion(Iconos.Accion.VERIFICAR, Color.WHITE));
    private final JButton btnDesmarcar = UiUtil.botonSecundario("Desmarcar ítem",
            Iconos.accion(Iconos.Accion.QUITAR, Tema.PRIMARIO));
    private final JButton btnCerrar = UiUtil.botonSecundario("Cerrar despacho",
            Iconos.accion(Iconos.Accion.CERRAR, Tema.PRIMARIO));

    private JTable tabla;
    private Despacho despachoActual;
    private List<DetalleDespacho> detalles = List.of();
    private boolean cargando = false;

    public FrmDespacho() {
        super("Despacho de mercados", true, true, true, true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1120, 700);
        setLayout(new BorderLayout(8, 8));
        getContentPane().setBackground(Tema.FONDO);

        modelo = new DefaultTableModel(
                new Object[]{"Producto", "Solicitado", "Cantidad a despachar", "U.", "Verificado",
                        "Faltante aceptado"}, 0) {
            @Override
            public boolean isCellEditable(int f, int c) {
                return despachoActual != null
                        && (c == COL_CANTIDAD || c == COL_VERIFICADO || c == COL_FALTANTE)
                        && !Despacho.DESPACHADA.equals(despachoActual.getEstado());
            }

            @Override
            public Class<?> getColumnClass(int c) {
                return c == COL_VERIFICADO || c == COL_FALTANTE ? Boolean.class : Object.class;
            }
        };
        tabla = new JTable(modelo);
        UiUtil.prepararTabla(tabla);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createLineBorder(Tema.BORDE));
        add(scroll, BorderLayout.CENTER);

        add(crearNorte(), BorderLayout.NORTH);

        JPanel sur = new JPanel(new BorderLayout(8, 0));
        sur.setOpaque(false);
        sur.setBorder(BorderFactory.createEmptyBorder(4, 10, 8, 10));
        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        acciones.setOpaque(false);
        acciones.add(btnVerificar);
        acciones.add(btnDesmarcar);
        acciones.add(btnCerrar);
        sur.add(acciones, BorderLayout.WEST);
        sur.add(lblSeleccion, BorderLayout.EAST);
        add(sur, BorderLayout.SOUTH);

        cmbPlantilla.addActionListener(e -> cargar());
        btnVerificar.addActionListener(e -> verificar(true));
        btnDesmarcar.addActionListener(e -> verificar(false));
        btnCerrar.addActionListener(e -> cerrar());
        tabla.getSelectionModel().addListSelectionListener(e -> actualizarBotones());

        btnVerificar.setEnabled(false);
        btnDesmarcar.setEnabled(false);
        btnCerrar.setEnabled(false);
        cargarPlantillas();
        UiUtil.fuenteRaiz(this);
    }

    private JPanel crearNorte() {
        JPanel norte = new JPanel(new BorderLayout(8, 6));
        norte.setOpaque(false);
        norte.setBorder(BorderFactory.createEmptyBorder(8, 10, 2, 10));
        norte.add(UiUtil.encabezado("Despacho de mercados",
                "Checklist de salida. Marque cada ítem y cierre solo cuando esté 100% verificado."),
                BorderLayout.NORTH);

        JPanel barra = new JPanel(new GridBagLayout());
        barra.setBackground(Tema.SUPERFICIE);
        barra.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Tema.BORDE),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 6, 4, 6);
        g.anchor = GridBagConstraints.WEST;

        g.gridx = 0; g.gridy = 0;
        barra.add(new JLabel("Mercado listo"), g);
        g.gridx = 1; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL;
        cmbPlantilla.setPreferredSize(new java.awt.Dimension(380, 34));
        barra.add(cmbPlantilla, g);

        g.gridx = 2; g.weightx = 0; g.fill = GridBagConstraints.NONE;
        JButton btnIniciar = UiUtil.botonSecundario("Abrir lista",
                Iconos.accion(Iconos.Accion.ABRIR, Tema.PRIMARIO));
        barra.add(btnIniciar, g);

        g.gridx = 3;
        lblProgreso.setFont(Tema.BASE_NEGRITA);
        lblProgreso.setForeground(Tema.PRIMARIO_OSCURO);
        barra.add(lblProgreso, g);

        btnIniciar.addActionListener(e -> iniciar());
        norte.add(barra, BorderLayout.CENTER);
        return norte;
    }

    private void cargarPlantillas() {
        cmbPlantilla.removeAllItems();
        for (PlantillaOperativa p : plantillaDAO.listarPorEstado(
                PlantillaOperativa.ESTADO_LISTA,
                PlantillaOperativa.ESTADO_DESPACHADA)) {
            cmbPlantilla.addItem(p);
        }
        if (cmbPlantilla.getItemCount() == 0) {
            modelo.setRowCount(0);
            despachoActual = null;
            lblProgreso.setText("No hay mercados listos para despacho.");
            actualizarBotones();
        }
    }

    private void iniciar() {
        try {
            PlantillaOperativa p = (PlantillaOperativa) cmbPlantilla.getSelectedItem();
            if (p == null) {
                throw new IllegalArgumentException("Seleccione un mercado.");
            }
            despachoActual = servicio.iniciar(p.getIdPlantilla());
            cargar();
        } catch (Exception e) {
            UiUtil.error(this, e);
        }
    }

    private void cargar() {
        cargando = true;
        modelo.setRowCount(0);
        PlantillaOperativa p = (PlantillaOperativa) cmbPlantilla.getSelectedItem();
        if (p == null) {
            despachoActual = null;
            cargando = false;
            actualizarBotones();
            return;
        }
        despachoActual = servicio.actual(p.getIdPlantilla());
        if (despachoActual == null) {
            lblProgreso.setText("Presione 'Abrir lista' para iniciar la verificación.");
            cargando = false;
            actualizarBotones();
            return;
        }
        detalles = servicio.detalle(despachoActual.getIdDespacho());
        for (DetalleDespacho d : detalles) {
            modelo.addRow(new Object[]{d.getNombreProducto(),
                    d.getCantidadSolicitada(), d.getCantidadDespachada(), d.getAbreviatura(),
                    d.isVerificado(), d.isFaltanteAceptado()});
        }
        actualizarProgreso();
        cargando = false;
        actualizarBotones();
    }

    private void actualizarProgreso() {
        if (despachoActual == null) {
            lblProgreso.setText("0 / 0 ítems verificados (0%)");
            return;
        }
        int[] progreso = servicio.progreso(despachoActual.getIdDespacho());
        double pct = progreso[1] == 0 ? 0 : Math.round(1000.0 * progreso[0] / progreso[1]) / 10.0;
        boolean cerrado = Despacho.DESPACHADA.equals(despachoActual.getEstado());
        if (cerrado) {
            boolean conNovedad = servicio.tieneFaltanteAceptado(despachoActual.getIdDespacho());
            lblProgreso.setText(conNovedad
                    ? "DESPACHADO CON NOVEDAD - faltante parcial aceptado"
                    : "DESPACHADO AL 100% - mercado entregado");
            lblProgreso.setForeground(conNovedad ? Tema.ADVERTENCIA : Tema.EXITO);
        } else {
            lblProgreso.setText(progreso[0] + " / " + progreso[1] + " ítems verificados (" + pct + "%)");
            lblProgreso.setForeground(Tema.PRIMARIO_OSCURO);
        }
        actualizarBotones();
    }

    private void actualizarBotones() {
        boolean activo = !cargando && despachoActual != null
                && !Despacho.DESPACHADA.equals(despachoActual.getEstado());
        boolean seleccion = tabla.getSelectedRow() >= 0;
        btnVerificar.setEnabled(activo && seleccion);
        btnDesmarcar.setEnabled(activo && seleccion);

        int[] progreso = despachoActual == null ? new int[]{0, 0}
                : servicio.progreso(despachoActual.getIdDespacho());
        boolean completo = activo && progreso[1] > 0 && progreso[0] == progreso[1];
        btnCerrar.setEnabled(completo);
        btnCerrar.setToolTipText(completo ? "Cerrar el despacho con el checklist completo."
                : "Verifique todos los ítems antes de cerrar.");

        if (!activo) {
            lblSeleccion.setText(despachoActual == null
                    ? "Seleccione un mercado y abra la lista."
                    : "Checklist cerrado.");
        } else if (!seleccion) {
            lblSeleccion.setText("Seleccione un ítem para verificar.");
        } else {
            lblSeleccion.setText("Ítem seleccionado: " + tabla.getValueAt(tabla.getSelectedRow(), COL_PRODUCTO));
        }
    }

    private void verificar(boolean marcar) {
        try {
            if (despachoActual == null) {
                throw new IllegalStateException("Primero abra la lista del mercado.");
            }
            int fila = tabla.getSelectedRow();
            if (fila < 0) {
                throw new IllegalArgumentException("Seleccione el producto a verificar.");
            }
            guardarFila(fila);
            DetalleDespacho d = detalles.get(fila);
            d.setVerificado(marcar);
            servicio.marcarItem(d);
            modelo.setValueAt(marcar, fila, COL_VERIFICADO);
            actualizarProgreso();
        } catch (Exception e) {
            UiUtil.error(this, e);
        }
    }

    private void guardarFila(int fila) {
        DetalleDespacho d = detalles.get(fila);
        d.setCantidadDespachada(valor(modelo.getValueAt(fila, COL_CANTIDAD)));
        d.setVerificado(Boolean.TRUE.equals(modelo.getValueAt(fila, COL_VERIFICADO)));
        d.setFaltanteAceptado(Boolean.TRUE.equals(modelo.getValueAt(fila, COL_FALTANTE)));
        servicio.marcarItem(d);
        modelo.setValueAt(d.isFaltanteAceptado(), fila, COL_FALTANTE);
    }

    private void cerrar() {
        try {
            if (despachoActual == null) {
                throw new IllegalStateException("Primero abra la lista del mercado.");
            }
            for (int i = 0; i < modelo.getRowCount(); i++) {
                guardarFila(i);
            }
            PlantillaOperativa p = (PlantillaOperativa) cmbPlantilla.getSelectedItem();
            if (p == null) {
                throw new IllegalStateException("Seleccione el mercado del despacho.");
            }
            boolean conNovedad = servicio.tieneFaltanteAceptado(despachoActual.getIdDespacho());
            JTextField txtNovedades = UiUtil.campo();
            txtNovedades.setPreferredSize(new java.awt.Dimension(420, 36));
            String etiqueta = conNovedad
                    ? "Novedades del despacho (obligatorias: hay faltante aceptado):"
                    : "Novedades del despacho (opcional):";
            int op = JOptionPane.showConfirmDialog(this,
                    new Object[]{etiqueta, txtNovedades},
                    "Cerrar despacho (RF10)", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (op != JOptionPane.OK_OPTION) {
                return;
            }
            String novedades = txtNovedades.getText();
            List<DetalleDespacho> pendientes = servicio.cerrar(
                    despachoActual.getIdDespacho(), p.getIdPlantilla(), novedades);
            if (!pendientes.isEmpty()) {
                StringBuilder sb = new StringBuilder("No se puede cerrar. Faltan por verificar:\n");
                for (DetalleDespacho d : pendientes) {
                    sb.append(" - ").append(d.getNombreProducto()).append("\n");
                }
                UiUtil.error(this, sb.toString());
                return;
            }
            UiUtil.mensaje(this, conNovedad
                    ? "DESPACHADO CON NOVEDAD: el mercado fue cerrado y la novedad quedó registrada."
                    : "DESPACHADO AL 100%: el mercado fue cerrado y entregado a distribución.");
            cargarPlantillas();
            cargar();
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
