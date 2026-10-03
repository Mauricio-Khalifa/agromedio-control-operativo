package agromedio.presentacion;

import agromedio.logica.CargaExcelServicio;
import agromedio.logica.ResultadoCarga;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.io.File;

/** Modulo 0 - Carga de instrucciones administrativas desde .xlsx (RF01 - RF02). */
public class FrmCargaExcel extends JInternalFrame {

    private final CargaExcelServicio servicio = new CargaExcelServicio();
    private final JTextArea areaResultado = new JTextArea();
    private File archivoSeleccionado;

    public FrmCargaExcel() {
        super("Carga de plantillas desde Excel", true, true, true, true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(860, 520);
        setLayout(new BorderLayout(8, 8));

        JButton btnSeleccionar = UiUtil.boton("1. Seleccionar archivo .xlsx");
        JButton btnCargar = UiUtil.boton("2. Cargar en el sistema");

        JPanel norte = new JPanel();
        norte.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        norte.add(btnSeleccionar);
        norte.add(btnCargar);
        add(norte, BorderLayout.NORTH);

        areaResultado.setFont(UiUtil.FUENTE);
        areaResultado.setEditable(false);
        areaResultado.setLineWrap(true);
        JScrollPane scroll = new JScrollPane(areaResultado);
        scroll.setBorder(BorderFactory.createTitledBorder(
                "Resultado - Columnas requeridas: Cliente | Contacto | Telefono | Mercado | Fecha (aaaa-mm-dd) | Producto | Cantidad"));
        add(scroll, BorderLayout.CENTER);

        btnSeleccionar.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new FileNameExtensionFilter("Libro de Excel (*.xlsx, *.xls)", "xlsx", "xls"));
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                archivoSeleccionado = chooser.getSelectedFile();
                areaResultado.setText("Archivo seleccionado: " + archivoSeleccionado.getName()
                        + "\nPresione '2. Cargar en el sistema' para procesarlo.");
            }
        });

        btnCargar.addActionListener(e -> {
            if (archivoSeleccionado == null) {
                UiUtil.error(this, "Primero debe seleccionar un archivo .xlsx.");
                return;
            }
            try {
                long inicio = System.currentTimeMillis();
                ResultadoCarga r = servicio.cargar(archivoSeleccionado);
                long ms = System.currentTimeMillis() - inicio;
                StringBuilder sb = new StringBuilder();
                sb.append("RESULTADO DE LA CARGA (").append(ms).append(" ms)\n");
                sb.append(r.toString()).append("\n");
                if (r.hayErrores()) {
                    sb.append("\nDETALLE DE ERRORES:\n");
                    for (String err : r.getErrores()) {
                        sb.append(" - ").append(err).append("\n");
                    }
                } else {
                    sb.append("\nTodas las lineas fueron procesadas sin errores.\n");
                }
                areaResultado.setText(sb.toString());
            } catch (Exception ex) {
                UiUtil.error(this, ex);
            }
        });

        areaResultado.setText("Seleccione el archivo .xlsx enviado por el area administrativa.");
        UiUtil.fuenteRaiz(this);
    }
}
