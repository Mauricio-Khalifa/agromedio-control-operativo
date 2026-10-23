package agromedio.presentacion;

import agromedio.logica.CargaExcelServicio;
import agromedio.logica.ResultadoCarga;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.File;
import java.util.concurrent.ExecutionException;

/** Modulo 0 - Carga de instrucciones administrativas desde Excel o archivos planos (RF01 - RF02). */
public class FrmCargaExcel extends JInternalFrame {

    private final CargaExcelServicio servicio = new CargaExcelServicio();
    private final JTextArea areaResultado = new JTextArea();
    private final JLabel lblArchivo = new JLabel("Ningún archivo seleccionado");
    private final JButton btnSeleccionar = UiUtil.botonSecundario("Seleccionar archivo");
    private final JButton btnCargar = UiUtil.botonPrimario("Cargar en el sistema");
    private final JProgressBar progreso = new JProgressBar();
    private File archivoSeleccionado;

    public FrmCargaExcel() {
        super("Carga de plantillas desde archivo", true, true, true, true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(960, 620);
        setLayout(new BorderLayout(8, 8));
        getContentPane().setBackground(Tema.FONDO);

        JPanel norte = new JPanel(new BorderLayout(8, 6));
        norte.setOpaque(false);
        norte.setBorder(BorderFactory.createEmptyBorder(8, 10, 2, 10));
        norte.add(UiUtil.encabezado("Carga de plantillas desde archivo",
                "Importe el archivo administrativo. El sistema valida, crea productos faltantes y registra la carga."),
                BorderLayout.NORTH);
        norte.add(crearSelector(), BorderLayout.CENTER);
        add(norte, BorderLayout.NORTH);

        areaResultado.setFont(Tema.MONO);
        areaResultado.setEditable(false);
        areaResultado.setLineWrap(true);
        areaResultado.setBackground(Tema.SUPERFICIE);
        areaResultado.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        JScrollPane scroll = new JScrollPane(areaResultado);
        scroll.setBorder(BorderFactory.createLineBorder(Tema.BORDE));
        add(scroll, BorderLayout.CENTER);

        btnSeleccionar.addActionListener(e -> seleccionar());
        btnCargar.addActionListener(e -> cargar());
        btnCargar.setEnabled(false);

        areaResultado.setText("Seleccione el archivo .xlsx, .xls, .csv o .txt enviado por el área administrativa.\n\n"
                + "Formato esperado: Cliente | Contacto | Teléfono | Mercado | Fecha | Producto | Cantidad\n"
                + "Fecha: aaaa-mm-dd o dd/mm/aaaa.\n"
                + "Cantidad: número (9) o texto (9 unidades, 1 de 300 gr, 2 de 1000 ml, 400 gramos, 1 libra, 1 rama).\n"
                + "Si el producto no existe, se crea automáticamente con la unidad detectada y la presentación entre paréntesis.\n\n"
                + "Archivos CSV/TXT: UTF-8 con o sin BOM; delimitador ; , o tabulador; campos entre comillas.");
        UiUtil.fuenteRaiz(this);
    }

    private JPanel crearSelector() {
        JPanel tarjeta = new JPanel(new BorderLayout(12, 8));
        tarjeta.setBackground(Tema.SUPERFICIE);
        tarjeta.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Tema.BORDE),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));

        JPanel archivo = new JPanel(new BorderLayout(0, 2));
        archivo.setOpaque(false);
        JLabel etiqueta = UiUtil.subtitulo("ARCHIVO DE ENTRADA");
        lblArchivo.setFont(Tema.BASE_NEGRITA);
        lblArchivo.setForeground(Tema.TEXTO);
        archivo.add(etiqueta, BorderLayout.NORTH);
        archivo.add(lblArchivo, BorderLayout.SOUTH);

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        acciones.setOpaque(false);
        acciones.add(btnSeleccionar);
        acciones.add(btnCargar);

        progreso.setIndeterminate(false);
        progreso.setStringPainted(true);
        progreso.setString("Listo para seleccionar un archivo");
        progreso.setVisible(true);

        JPanel superior = new JPanel(new BorderLayout(12, 0));
        superior.setOpaque(false);
        superior.add(archivo, BorderLayout.CENTER);
        superior.add(acciones, BorderLayout.EAST);

        tarjeta.add(superior, BorderLayout.CENTER);
        tarjeta.add(progreso, BorderLayout.SOUTH);
        return tarjeta;
    }

    private void seleccionar() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Seleccionar archivo de plantillas");
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setFileFilter(new FileNameExtensionFilter(
                "Archivos soportados (*.xlsx, *.xls, *.csv, *.txt)",
                "xlsx", "xls", "csv", "txt"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            archivoSeleccionado = chooser.getSelectedFile();
            lblArchivo.setText(archivoSeleccionado.getName());
            lblArchivo.setToolTipText(archivoSeleccionado.getAbsolutePath());
            btnCargar.setEnabled(true);
            progreso.setIndeterminate(false);
            progreso.setString("Archivo listo: " + archivoSeleccionado.getName());
            areaResultado.setText("Archivo seleccionado:\n" + archivoSeleccionado.getAbsolutePath()
                    + "\n\nPresione 'Cargar en el sistema' para procesarlo.");
        }
    }

    private void cargar() {
        if (archivoSeleccionado == null) {
            UiUtil.error(this, "Primero debe seleccionar un archivo .xlsx, .xls, .csv o .txt.");
            return;
        }
        final long inicio = System.currentTimeMillis();
        btnSeleccionar.setEnabled(false);
        btnCargar.setEnabled(false);
        progreso.setIndeterminate(true);
        progreso.setString("Procesando archivo...");
        areaResultado.setText("Procesando " + archivoSeleccionado.getName() + "...\n\n"
                + "No cierre esta ventana mientras se valida el archivo.");

        new SwingWorker<ResultadoCarga, Void>() {
            @Override
            protected ResultadoCarga doInBackground() throws Exception {
                return servicio.cargar(archivoSeleccionado);
            }

            @Override
            protected void done() {
                try {
                    ResultadoCarga r = get();
                    mostrarResultado(r, System.currentTimeMillis() - inicio);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    UiUtil.error(FrmCargaExcel.this, "La carga fue interrumpida.");
                } catch (ExecutionException ex) {
                    Throwable causa = ex.getCause();
                    if (causa instanceof Exception e) {
                        UiUtil.error(FrmCargaExcel.this, e);
                    } else {
                        UiUtil.error(FrmCargaExcel.this, String.valueOf(causa));
                    }
                } finally {
                    btnSeleccionar.setEnabled(true);
                    btnCargar.setEnabled(archivoSeleccionado != null);
                    progreso.setIndeterminate(false);
                }
            }
        }.execute();
    }

    private void mostrarResultado(ResultadoCarga r, long ms) {
        StringBuilder sb = new StringBuilder();
        if (r.isDuplicada()) {
            sb.append("CARGA NO REALIZADA\n\n");
            for (String err : r.getErrores()) {
                sb.append(" - ").append(err).append("\n");
            }
            sb.append("\nSeleccione un archivo nuevo o modifique el contenido antes de volver a cargarlo.");
            areaResultado.setText(sb.toString());
            areaResultado.setCaretPosition(0);
            progreso.setString("Archivo duplicado: no se volvió a cargar");
            if (!r.getErrores().isEmpty()) {
                UiUtil.mensaje(this, r.getErrores().get(0));
            }
            return;
        }

        sb.append("RESULTADO DE LA CARGA (").append(ms).append(" ms)\n");
        sb.append(r.toString()).append("\n");
        if (r.hayErrores()) {
            sb.append("\nDETALLE DE ERRORES:\n");
            for (String err : r.getErrores()) {
                sb.append(" - ").append(err).append("\n");
            }
            progreso.setString("Carga completada con " + r.getErrores().size() + " error(es)");
        } else {
            sb.append("\nTodas las líneas fueron procesadas sin errores.\n");
            progreso.setString("Carga completada sin errores");
        }
        areaResultado.setText(sb.toString());
        areaResultado.setCaretPosition(0);
    }
}
