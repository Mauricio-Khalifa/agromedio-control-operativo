package agromedio.presentacion;

import agromedio.datos.PlantillaDAO;
import agromedio.datos.PlantillaDAOSqlite;
import agromedio.modelo.PlantillaOperativa;
import agromedio.logica.ReporteServicio;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;

/** Reportes operativos listos para imprimir (faltantes, saldos, consolidado, avance). */
public class FrmReportes extends JInternalFrame {

    private final ReporteServicio servicio = new ReporteServicio();
    private final PlantillaDAO plantillaDAO = new PlantillaDAOSqlite();
    private final JTextArea area = new JTextArea();
    private final JTextField txtFecha = UiUtil.campo();
    private final JComboBox<PlantillaOperativa> cmbPlantilla = new JComboBox<>();

    public FrmReportes() {
        super("Reportes y consultas", true, true, true, true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1120, 700);
        setLayout(new BorderLayout(8, 8));
        getContentPane().setBackground(Tema.FONDO);

        JPanel norte = new JPanel(new BorderLayout(8, 6));
        norte.setOpaque(false);
        norte.setBorder(BorderFactory.createEmptyBorder(8, 10, 2, 10));
        norte.add(UiUtil.encabezado("Reportes y consultas",
                "Genere una vista previa, revise los datos y luego imprima el reporte."),
                BorderLayout.NORTH);
        norte.add(crearControles(), BorderLayout.CENTER);
        add(norte, BorderLayout.NORTH);

        for (PlantillaOperativa p : plantillaDAO.listar()) {
            cmbPlantilla.addItem(p);
        }
        txtFecha.setText(LocalDate.now().toString());

        area.setEditable(false);
        area.setFont(Tema.MONO);
        area.setBackground(Tema.SUPERFICIE);
        area.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        JScrollPane scroll = new JScrollPane(area);
        scroll.setBorder(BorderFactory.createLineBorder(Tema.BORDE));
        add(scroll, BorderLayout.CENTER);

        JButton btnImprimir = UiUtil.botonPrimario("Imprimir reporte",
                Iconos.accion(Iconos.Accion.IMPRIMIR, Color.WHITE));
        JButton btnLimpiar = UiUtil.botonSecundario("Limpiar",
                Iconos.accion(Iconos.Accion.LIMPIAR, Tema.PRIMARIO));
        JPanel sur = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        sur.setOpaque(false);
        sur.setBorder(BorderFactory.createEmptyBorder(0, 10, 6, 10));
        sur.add(btnImprimir);
        sur.add(btnLimpiar);
        add(sur, BorderLayout.SOUTH);

        btnLimpiar.addActionListener(e -> area.setText(""));
        btnImprimir.addActionListener(e -> {
            try {
                area.print();
            } catch (Exception ex) {
                UiUtil.error(this, ex);
            }
        });

        area.setText("Seleccione el reporte que desea generar.");
        UiUtil.fuenteRaiz(this);
    }

    private JPanel crearControles() {
        JPanel tarjeta = new JPanel(new GridBagLayout());
        tarjeta.setBackground(Tema.SUPERFICIE);
        tarjeta.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Tema.BORDE),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));

        JButton btnFaltantes = UiUtil.botonSecundario("Ver faltantes",
                Iconos.accion(Iconos.Accion.VER, Tema.PRIMARIO));
        JButton btnSaldo = UiUtil.botonSecundario("Ver saldo",
                Iconos.accion(Iconos.Accion.VER, Tema.PRIMARIO));
        JButton btnConsolidado = UiUtil.botonSecundario("Ver consolidado",
                Iconos.accion(Iconos.Accion.VER, Tema.PRIMARIO));
        JButton btnAvance = UiUtil.botonSecundario("Ver avance",
                Iconos.accion(Iconos.Accion.VER, Tema.PRIMARIO));

        JPanel grupoFaltantes = grupo("Faltantes de recepción", btnFaltantes);
        JPanel grupoSaldo = grupo("Saldo del mercado", cmbPlantilla, btnSaldo);
        JPanel grupoConsolidado = grupo("Consolidado por fecha", txtFecha, btnConsolidado);
        JPanel grupoAvance = grupo("Avance de despachos", btnAvance);

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 6, 4, 6);
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1;
        g.gridx = 0; g.gridy = 0;
        tarjeta.add(grupoFaltantes, g);
        g.gridx = 1;
        tarjeta.add(grupoSaldo, g);
        g.gridx = 0; g.gridy = 1;
        tarjeta.add(grupoConsolidado, g);
        g.gridx = 1;
        tarjeta.add(grupoAvance, g);

        cmbPlantilla.setPreferredSize(new java.awt.Dimension(230, 34));
        txtFecha.setPreferredSize(new java.awt.Dimension(130, 34));

        btnFaltantes.addActionListener(e -> mostrar(servicio.reporteFaltantes()));
        btnSaldo.addActionListener(e -> {
            PlantillaOperativa p = (PlantillaOperativa) cmbPlantilla.getSelectedItem();
            if (p == null) {
                UiUtil.error(this, "Seleccione un mercado.");
                return;
            }
            mostrar(servicio.reporteSaldo(p.getIdPlantilla()));
        });
        btnConsolidado.addActionListener(e -> mostrar(servicio.reporteConsolidado(txtFecha.getText().trim())));
        btnAvance.addActionListener(e -> mostrar(servicio.reporteAvance()));
        return tarjeta;
    }

    private JPanel grupo(String etiqueta, java.awt.Component... componentes) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        panel.setOpaque(false);
        panel.add(new JLabel(etiqueta));
        for (java.awt.Component componente : componentes) {
            panel.add(componente);
        }
        return panel;
    }

    private void mostrar(String texto) {
        area.setText(texto);
        area.setCaretPosition(0);
    }
}
