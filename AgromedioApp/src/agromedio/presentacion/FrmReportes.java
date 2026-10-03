package agromedio.presentacion;

import agromedio.datos.PlantillaDAO;
import agromedio.datos.PlantillaDAOSqlite;
import agromedio.modelo.PlantillaOperativa;
import agromedio.logica.ReporteServicio;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
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
        setSize(960, 620);
        setLayout(new BorderLayout(8, 8));

        JPanel norte = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        JButton btnFaltantes = UiUtil.boton("Faltantes de recepcion");
        JButton btnSaldo = UiUtil.boton("Saldo del mercado");
        JButton btnConsolidado = UiUtil.boton("Consolidado de demanda");
        JButton btnAvance = UiUtil.boton("Avance de despachos");
        norte.add(btnFaltantes);
        norte.add(cmbPlantilla);
        norte.add(btnSaldo);
        norte.add(txtFecha);
        norte.add(btnConsolidado);
        norte.add(btnAvance);
        add(norte, BorderLayout.NORTH);

        for (PlantillaOperativa p : plantillaDAO.listar()) {
            cmbPlantilla.addItem(p);
        }
        txtFecha.setText(LocalDate.now().toString());

        area.setEditable(false);
        area.setFont(new java.awt.Font("Consolas", java.awt.Font.PLAIN, 15));
        JScrollPane scroll = new JScrollPane(area);
        scroll.setBorder(BorderFactory.createTitledBorder("Vista previa del reporte"));
        add(scroll, BorderLayout.CENTER);

        JButton btnImprimir = UiUtil.boton("Imprimir reporte");
        JButton btnLimpiar = UiUtil.boton("Limpiar");
        JPanel sur = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 10));
        sur.add(btnImprimir);
        sur.add(btnLimpiar);
        add(sur, BorderLayout.SOUTH);

        btnFaltantes.addActionListener(e -> mostrar(servicio.reporteFaltantes()));
        btnSaldo.addActionListener(e -> {
            PlantillaOperativa p = (PlantillaOperativa) cmbPlantilla.getSelectedItem();
            if (p != null) {
                mostrar(servicio.reporteSaldo(p.getIdPlantilla()));
            }
        });
        btnConsolidado.addActionListener(e -> mostrar(servicio.reporteConsolidado(txtFecha.getText().trim())));
        btnAvance.addActionListener(e -> mostrar(servicio.reporteAvance()));
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

    private void mostrar(String texto) {
        area.setText(texto);
        area.setCaretPosition(0);
    }
}
