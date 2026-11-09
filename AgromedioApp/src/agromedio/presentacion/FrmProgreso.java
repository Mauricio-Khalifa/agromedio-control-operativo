package agromedio.presentacion;

import agromedio.logica.ProgresoServicio;
import agromedio.modelo.FilaProgreso;
import agromedio.modelo.ResumenProgreso;
import agromedio.modelo.TareaProgreso;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.List;
import java.util.ArrayList;

/** Panel visual de procesos abiertos; conserva las plantillas hasta su despacho formal. */
public class FrmProgreso extends JInternalFrame {

    private final ProgresoServicio servicio = new ProgresoServicio();
    private final javax.swing.Timer timerActualizacion = new javax.swing.Timer(15000, e -> refrescar());
    private final JPanel tarjetas = new JPanel(new GridLayout(1, 0, 8, 0));
    private final JPanel etapas = new JPanel(new GridLayout(1, 0, 8, 0));
    private final GraficoDonut graficoEtapas = new GraficoDonut();
    private final GraficoBarrasEtapas graficoBarras = new GraficoBarrasEtapas();
    private final DefaultTableModel modeloResumen = UiUtil.modelo(
            new Object[]{"Mercado", "Fecha prog.", "Etapa", "Atención"}, false);
    private final JTable tablaResumen = new JTable(modeloResumen);
    private final DefaultTableModel modeloTareas = UiUtil.modelo(
            new Object[]{"Prioridad", "Mercado", "Tarea", "Estado"}, false);
    private final JTable tablaTareas = new JTable(modeloTareas);
    private final JLabel lblActualizacion = UiUtil.subtitulo("");
    private final JButton btnActualizar = UiUtil.botonSecundario("Actualizar",
            Iconos.accion(Iconos.Accion.REFRESCAR, Tema.PRIMARIO));
    private JPanel panelDetalle; // Referencia directa
    private List<FilaProgreso> filasActuales = new ArrayList<>();

    public FrmProgreso() {
        super("Procesos abiertos", true, true, true, true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1420, 900);
        setMinimumSize(new java.awt.Dimension(1180, 760));
        setLayout(new BorderLayout(8, 8));
        getContentPane().setBackground(Tema.FONDO);

        JPanel norte = new JPanel(new BorderLayout(8, 6));
        norte.setOpaque(false);
        norte.setBorder(BorderFactory.createEmptyBorder(8, 10, 2, 10));
        JPanel encabezado = UiUtil.encabezado("Procesos abiertos",
                "Plantillas cargadas que aún no han completado su ciclo operativo.");
        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        acciones.setOpaque(false);
        acciones.add(lblActualizacion);
        acciones.add(btnActualizar);
        norte.add(encabezado, BorderLayout.CENTER);
        norte.add(acciones, BorderLayout.EAST);
        add(norte, BorderLayout.NORTH);

        tarjetas.setOpaque(false);
        JPanel graficos = crearPanelGraficos();
        JPanel panelEtapas = crearPanelEtapas();
        JPanel panelProcesos = crearTablaProcesos();
        JPanel panelTareas = crearPanelTareas();
        // Layout central usando pestañas para organizar la información
        javax.swing.JTabbedPane pestañas = new javax.swing.JTabbedPane();
        pestañas.setOpaque(false);

        // Tab 1: Resumen General
        JPanel panelResumen = new JPanel();
        panelResumen.setOpaque(false);
        panelResumen.setLayout(new BoxLayout(panelResumen, BoxLayout.Y_AXIS));
        panelResumen.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panelResumen.add(tarjetas);
        panelResumen.add(Box.createVerticalStrut(8));
        panelResumen.add(graficos);
        panelResumen.add(Box.createVerticalStrut(8));
        panelResumen.add(panelEtapas);
        JScrollPane scrollResumen = new JScrollPane(panelResumen);
        scrollResumen.setBorder(null);
        scrollResumen.setOpaque(false);
        scrollResumen.getViewport().setOpaque(false);
        pestañas.addTab("Resumen General", scrollResumen);

        // Tab 2: Seguimiento detallado
        pestañas.addTab("Procesos", panelProcesos);

        // Tab 3: Tareas urgentes
        pestañas.addTab("Atención", panelTareas);

        add(pestañas, BorderLayout.CENTER);

        btnActualizar.addActionListener(e -> refrescar());
        refrescar();
        timerActualizacion.start();
        UiUtil.fuenteRaiz(this);
    }

    @Override
    public void dispose() {
        timerActualizacion.stop();
        super.dispose();
    }

    private static void ocuparAncho(JPanel panel) {
        panel.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE,
                panel.getPreferredSize().height));
    }

    private JPanel crearPanelGraficos() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 8, 0));
        panel.setOpaque(false);
        panel.add(graficoEnTarjeta("Distribución de procesos", graficoEtapas));
        panel.add(graficoEnTarjeta("Carga por etapa", graficoBarras));
        return panel;
    }

    private JPanel graficoEnTarjeta(String titulo, javax.swing.JComponent grafico) {
        JPanel panel = new JPanel(new BorderLayout(8, 4));
        panel.setBackground(Tema.SUPERFICIE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Tema.BORDE),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        JLabel etiqueta = UiUtil.subtitulo(titulo);
        etiqueta.setFont(Tema.PEQUENA.deriveFont(Font.BOLD));
        panel.add(etiqueta, BorderLayout.NORTH);
        panel.add(grafico, BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearPanelEtapas() {
        JPanel panel = new JPanel(new BorderLayout(0, 5));
        panel.setOpaque(false);
        JLabel titulo = UiUtil.subtitulo("Distribución por etapa");
        titulo.setFont(Tema.PEQUENA.deriveFont(Font.BOLD));
        panel.add(titulo, BorderLayout.NORTH);
        etapas.setOpaque(false);
        // Usamos un layout que permita ajustar el flujo y salto de línea
        etapas.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 8));
        panel.add(etapas, BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearTablaProcesos() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);
        JLabel titulo = UiUtil.subtitulo("Seguimiento por mercado");
        titulo.setFont(Tema.BASE.deriveFont(Font.BOLD, 16f));
        panel.add(titulo, BorderLayout.NORTH);
        
        // Nueva estructura: Tabla resumen arriba y Detalle abajo
        JPanel centro = new JPanel(new GridLayout(2, 1, 0, 10));
        centro.setOpaque(false);
        
        // 1. Tabla Resumen (Simplificada)
        UiUtil.prepararTabla(tablaResumen);
        tablaResumen.setRowHeight(35);
        tablaResumen.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        JScrollPane scrollResumen = new JScrollPane(tablaResumen);
        scrollResumen.setBorder(BorderFactory.createLineBorder(Tema.BORDE));
        scrollResumen.setPreferredSize(new java.awt.Dimension(0, 200));
        
        // 2. Panel de Detalle (Donde se muestran los porcentajes al seleccionar)
        panelDetalle = new JPanel(new BorderLayout());
        panelDetalle.setBackground(Tema.SUPERFICIE);
        panelDetalle.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Tema.BORDE),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)));
        
        JLabel lblDetalle = new JLabel("Seleccione un mercado para ver el avance detallado", JLabel.CENTER);
        lblDetalle.setFont(Tema.PEQUENA);
        lblDetalle.setForeground(Tema.TEXTO_SUAVE);
        panelDetalle.add(lblDetalle, BorderLayout.CENTER);
        
        // Listener para actualizar el detalle
        tablaResumen.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = tablaResumen.getSelectedRow();
                if (row != -1 && row < filasActuales.size()) {
                    actualizarDetalleMercado(panelDetalle, filasActuales.get(row));
                }
            }
        });
        
        // Corrigiendo el panel centro a BorderLayout para mejor control
        JPanel panelContenedor = new JPanel(new BorderLayout(0, 10));
        panelContenedor.setOpaque(false);
        panelContenedor.add(scrollResumen, BorderLayout.NORTH);
        panelContenedor.add(panelDetalle, BorderLayout.CENTER);
        
        panel.add(panelContenedor, BorderLayout.CENTER);
        
        return panel;
    }

    private JPanel crearPanelTareas() {
        JPanel panel = new JPanel(new BorderLayout(0, 8)); // Más espacio
        panel.setOpaque(false);
        panel.setPreferredSize(new java.awt.Dimension(0, 240)); // Más altura inicial
        JLabel titulo = UiUtil.subtitulo("Tareas que requieren atención");
        titulo.setFont(Tema.BASE.deriveFont(Font.BOLD, 16f)); // Fuente más grande
        panel.add(titulo, BorderLayout.NORTH);
        UiUtil.prepararTabla(tablaTareas);
        tablaTareas.setRowHeight(35); // Un poco más alto
        JScrollPane scroll = new JScrollPane(tablaTareas);
        scroll.setBorder(BorderFactory.createLineBorder(Tema.BORDE));
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private void refrescar() {
        btnActualizar.setEnabled(false);
        lblActualizacion.setText("Actualizando...");
        new SwingWorker<DatosProgreso, Void>() {
            @Override
            protected DatosProgreso doInBackground() {
                List<FilaProgreso> filas = servicio.procesosAbiertos();
                return new DatosProgreso(filas, servicio.resumen(filas), servicio.tareasPendientes(filas));
            }

            @Override
            protected void done() {
                try {
                    DatosProgreso datos = get();
                    cargarTarjetas(datos.resumen(), datos.filas());
                    cargarGraficos(datos.resumen());
                    cargarEtapas(datos.resumen());
                    cargarProcesos(datos.filas());
                    cargarTareas(datos.tareas());
                    
                    // Limpiar detalle al refrescar usando la referencia directa
                    panelDetalle.removeAll();
                    panelDetalle.add(new JLabel("Seleccione un mercado para ver el avance detallado", JLabel.CENTER), BorderLayout.CENTER);
                    panelDetalle.revalidate();
                    panelDetalle.repaint();
                    
                    lblActualizacion.setText("Actualizado: " + java.time.LocalDateTime.now()
                            .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM HH:mm")));
                } catch (Exception ex) {
                    lblActualizacion.setText("No se pudo actualizar");
                    Exception error = ex.getCause() instanceof Exception causa ? causa : ex;
                    UiUtil.error(thisFrame(), error);
                } finally {
                    btnActualizar.setEnabled(true);
                }
            }
        }.execute();
    }

    private JInternalFrame thisFrame() {
        return this;
    }

    private record DatosProgreso(List<FilaProgreso> filas, ResumenProgreso resumen,
                                 List<TareaProgreso> tareas) {
    }

    private void cargarTarjetas(ResumenProgreso resumen, List<FilaProgreso> filas) {
        tarjetas.removeAll();
        tarjetas.add(tarjeta("Plantillas", resumen.getPlantillasAbiertas(), Tema.PRIMARIO));
        tarjetas.add(tarjeta("Compra promedio", (int) Math.round(promedio(filas, "COMPRA")), Tema.INFO, true));
        tarjetas.add(tarjeta("Recepción promedio", (int) Math.round(promedio(filas, "RECEPCION")), Tema.INFO, true));
        tarjetas.add(tarjeta("Alistamiento promedio", (int) Math.round(promedio(filas, "ALISTAMIENTO")), Tema.ACENTO, true));
        tarjetas.add(tarjeta("Listas", resumen.getListas(), Tema.EXITO));
        tarjetas.add(tarjeta("Faltantes", resumen.getRecepcionesConFaltante(), Tema.ADVERTENCIA));
        tarjetas.revalidate();
        tarjetas.repaint();
    }

    /** Promedio real de avance de compra, recepcion o alistamiento entre los procesos abiertos. */
    private static double promedio(List<FilaProgreso> filas, String tipo) {
        if (filas == null || filas.isEmpty()) return 0;
        double suma = 0;
        for (FilaProgreso fila : filas) {
            if (fila == null) continue;
            suma += switch (tipo) {
                case "COMPRA" -> fila.getPorcentajeCompra();
                case "RECEPCION" -> fila.getPorcentajeRecepcion();
                case "ALISTAMIENTO" -> fila.getPorcentajeAlistamiento();
                default -> 0;
            };
        }
        return suma / filas.size();
    }

    private JPanel tarjeta(String titulo, int valor, Color color, boolean esPorcentaje) {
        JPanel panel = new JPanel(new BorderLayout(6, 0));
        panel.setBackground(Tema.SUPERFICIE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, color),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        
        JLabel numero = new JLabel(String.valueOf(valor) + (esPorcentaje ? "%" : ""));
        numero.setFont(Tema.fuente(Font.BOLD, 22));
        numero.setForeground(color);
        JLabel nombre = new JLabel(titulo);
        nombre.setFont(Tema.PEQUENA);
        nombre.setForeground(Tema.TEXTO_SUAVE);
        panel.add(numero, BorderLayout.WEST);
        panel.add(nombre, BorderLayout.CENTER);
        return panel;
    }

    private JPanel tarjeta(String titulo, int valor, Color color) {
        return tarjeta(titulo, valor, color, false);
    }

    private void cargarGraficos(ResumenProgreso resumen) {
        int total = resumen.getPlantillasAbiertas();
        if (total == 0) total = 1; // Evitar división por cero

        // Depuración: Asegurarnos de que los valores individuales no sumen más de lo que deben
        int cargadas = resumen.getCargadas();
        int enCompra = resumen.getEnCompra();
        int recibiendo = resumen.getRecibiendo();
        int alistando = resumen.getAlistando();
        int verificando = resumen.getDespachosEnVerificacion();
        int listas = resumen.getListas();

        graficoEtapas.setDatos(List.of(
                new GraficoDonut.Segmento("Cargadas", cargadas, Tema.TEXTO_SUAVE),
                new GraficoDonut.Segmento("Compra", enCompra, Tema.INFO),
                new GraficoDonut.Segmento("Recepcion", recibiendo, Tema.ADVERTENCIA),
                new GraficoDonut.Segmento("Alistamiento", alistando, Tema.ACENTO),
                new GraficoDonut.Segmento("Verificando", verificando, Tema.EXITO),
                new GraficoDonut.Segmento("Listas", listas, Tema.EXITO)));
        
        // Calculamos el porcentaje sobre el total para el gráfico de barras
        graficoBarras.setDatos(List.of(
                new GraficoBarrasEtapas.Barra("Cargadas", (cargadas * 100) / total, Tema.TEXTO_SUAVE),
                new GraficoBarrasEtapas.Barra("Compra", (enCompra * 100) / total, Tema.INFO),
                new GraficoBarrasEtapas.Barra("Recepcion", (recibiendo * 100) / total, Tema.ADVERTENCIA),
                new GraficoBarrasEtapas.Barra("Alistamiento", (alistando * 100) / total, Tema.ACENTO),
                new GraficoBarrasEtapas.Barra("Verificando", (verificando * 100) / total, Tema.EXITO),
                new GraficoBarrasEtapas.Barra("Listas", (listas * 100) / total, Tema.EXITO)));
    }

    private void cargarEtapas(ResumenProgreso resumen) {
        etapas.removeAll();
        etapas.add(etapa("CARGADA", resumen.getCargadas(), Tema.TEXTO_SUAVE));
        etapas.add(etapa("EN COMPRA", resumen.getEnCompra(), Tema.INFO));
        etapas.add(etapa("RECIBIENDO", resumen.getRecibiendo(), Tema.ADVERTENCIA));
        etapas.add(etapa("ALISTANDO", resumen.getAlistando(), Tema.ACENTO));
        etapas.add(etapa("VERIFICANDO", resumen.getDespachosEnVerificacion(), Tema.EXITO));
        etapas.add(etapa("LISTA", resumen.getListas(), Tema.EXITO));
        etapas.revalidate();
        etapas.repaint();
    }

    private JPanel etapa(String nombre, int cantidad, Color color) {
        JPanel panel = new JPanel(new BorderLayout(2, 0));
        panel.setBackground(Tema.SUPERFICIE);
        panel.setBorder(BorderFactory.createLineBorder(Tema.BORDE));
        panel.setPreferredSize(new java.awt.Dimension(90, 45));
        JLabel numero = new JLabel(String.valueOf(cantidad), JLabel.CENTER);
        numero.setFont(Tema.fuente(Font.BOLD, 16));
        numero.setForeground(color);
        JLabel texto = new JLabel(nombre, JLabel.CENTER);
        texto.setFont(Tema.PEQUENA.deriveFont(Font.BOLD, 9f));
        texto.setForeground(Tema.TEXTO_SUAVE);
        panel.add(numero, BorderLayout.CENTER);
        panel.add(texto, BorderLayout.SOUTH);
        return panel;
    }

    private void actualizarDetalleMercado(JPanel panelDetalle, FilaProgreso fila) {
        panelDetalle.removeAll();
        panelDetalle.setLayout(new GridLayout(2, 2, 15, 15));
        
        panelDetalle.add(barraConEtiqueta("Compra", (int)fila.getPorcentajeCompra(), Tema.INFO));
        panelDetalle.add(barraConEtiqueta("Recepción", (int)fila.getPorcentajeRecepcion(), Tema.ADVERTENCIA));
        panelDetalle.add(barraConEtiqueta("Alistamiento", (int)fila.getPorcentajeAlistamiento(), Tema.ACENTO));
        panelDetalle.add(barraConEtiqueta("Despacho", (int)fila.getPorcentajeDespacho(), Tema.EXITO));
        
        panelDetalle.revalidate();
        panelDetalle.repaint();
    }

    private JPanel barraConEtiqueta(String titulo, int porcentaje, Color color) {
        JPanel panel = new JPanel(new BorderLayout(5, 2));
        panel.setOpaque(false);
        
        JLabel lblTitulo = new JLabel(titulo + " (" + porcentaje + "%)");
        lblTitulo.setFont(Tema.PEQUENA.deriveFont(Font.BOLD));
        lblTitulo.setForeground(Tema.TEXTO_SUAVE);
        
        BarraProgreso barra = new BarraProgreso();
        barra.setColor(color);
        barra.setValor(porcentaje);
        barra.setPreferredSize(new java.awt.Dimension(0, 15));
        
        panel.add(lblTitulo, BorderLayout.NORTH);
        panel.add(barra, BorderLayout.CENTER);
        return panel;
    }

    private void cargarProcesos(List<FilaProgreso> filas) {
        this.filasActuales = filas;
        modeloResumen.setRowCount(0);
        for (FilaProgreso fila : filas) {
            modeloResumen.addRow(new Object[]{
                    fila.getNombreMercado(),
                    fila.getFechaProgramada(),
                    Formato.estado(fila.getEstado()),
                    atencion(fila)
            });
        }
    }

    private static final class BarraConTextoRenderer extends javax.swing.JPanel implements TableCellRenderer {
        private final BarraProgreso barra;
        private final JLabel label;

        public BarraConTextoRenderer(Color color) {
            setLayout(new BorderLayout(5, 0));
            setOpaque(true);
            barra = new BarraProgreso();
            barra.setColor(color);
            label = new JLabel("", JLabel.RIGHT);
            label.setFont(Tema.PEQUENA.deriveFont(Font.BOLD));
            label.setPreferredSize(new java.awt.Dimension(45, 20));
            add(barra, BorderLayout.CENTER);
            add(label, BorderLayout.EAST);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                        boolean focused, int row, int column) {
            String valStr = value.toString();
            double val = 0;
            try {
                val = Double.parseDouble(valStr.replace("%", ""));
            } catch (NumberFormatException e) { }
            barra.setValor(val);
            label.setText(valStr);
            Color bg = selected ? Tema.PRIMARIO_SUAVE : Tema.SUPERFICIE;
            setBackground(bg);
            barra.setBackground(bg);
            return this;
        }
    }

    private void cargarTareas(List<TareaProgreso> tareas) {
        modeloTareas.setRowCount(0);
        for (TareaProgreso tarea : tareas) {
            modeloTareas.addRow(new Object[]{tarea.getPrioridad(), tarea.getNombreMercado(),
                    tarea.getDescripcion(), Formato.estado(tarea.getEstado())});
        }
    }

    private static String atencion(FilaProgreso fila) {
        if (fila.isTieneNovedad()) return "Novedad";
        if (fila.isTieneFaltante()) return "Faltante";
        return "Normal";
    }
}
