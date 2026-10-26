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
    private final JPanel tarjetas = new JPanel(new GridLayout(1, 0, 8, 0));
    private final JPanel etapas = new JPanel(new GridLayout(1, 0, 8, 0));
    private final GraficoDonut graficoEtapas = new GraficoDonut();
    private final GraficoBarrasEtapas graficoBarras = new GraficoBarrasEtapas();
    private final DefaultTableModel modelo = UiUtil.modelo(
            new Object[]{"Mercado", "Fecha carga", "Fecha programada", "Etapa", "Compra", "Recepción",
                    "Alistamiento", "Despacho", "Atención"}, false);
    private final JTable tabla = new JTable(modelo);
    private final DefaultTableModel modeloTareas = UiUtil.modelo(
            new Object[]{"Prioridad", "Mercado", "Tarea", "Estado"}, false);
    private final JTable tablaTareas = new JTable(modeloTareas);
    private final JLabel lblActualizacion = UiUtil.subtitulo("");
    private final JButton btnActualizar = UiUtil.botonSecundario("Actualizar",
            Iconos.accion(Iconos.Accion.REFRESCAR, Tema.PRIMARIO));

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
        ocuparAncho(tarjetas);
        ocuparAncho(graficos);
        ocuparAncho(panelEtapas);
        ocuparAncho(panelProcesos);
        ocuparAncho(panelTareas);
        JPanel cuerpo = new JPanel();
        cuerpo.setOpaque(false);
        cuerpo.setLayout(new BoxLayout(cuerpo, BoxLayout.Y_AXIS));
        cuerpo.setBorder(BorderFactory.createEmptyBorder(0, 10, 8, 10));
        cuerpo.add(tarjetas);
        cuerpo.add(Box.createVerticalStrut(8));
        cuerpo.add(graficos);
        cuerpo.add(Box.createVerticalStrut(8));
        cuerpo.add(panelEtapas);
        cuerpo.add(Box.createVerticalStrut(8));
        cuerpo.add(panelProcesos);
        cuerpo.add(Box.createVerticalStrut(8));
        cuerpo.add(panelTareas);
        JScrollPane scrollCuerpo = new JScrollPane(cuerpo);
        scrollCuerpo.setBorder(null);
        scrollCuerpo.getViewport().setOpaque(false);
        scrollCuerpo.setOpaque(false);
        scrollCuerpo.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollCuerpo, BorderLayout.CENTER);

        btnActualizar.addActionListener(e -> refrescar());
        refrescar();
        UiUtil.fuenteRaiz(this);
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
        etapas.setPreferredSize(new java.awt.Dimension(0, 58));
        panel.add(etapas, BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearTablaProcesos() {
        JPanel panel = new JPanel(new BorderLayout(0, 5));
        panel.setOpaque(false);
        JLabel titulo = UiUtil.subtitulo("Seguimiento por mercado");
        titulo.setFont(Tema.PEQUENA.deriveFont(Font.BOLD));
        panel.add(titulo, BorderLayout.NORTH);
        UiUtil.prepararTabla(tabla);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setRowHeight(34);
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createLineBorder(Tema.BORDE));
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearPanelTareas() {
        JPanel panel = new JPanel(new BorderLayout(0, 5));
        panel.setOpaque(false);
        panel.setPreferredSize(new java.awt.Dimension(0, 190));
        JLabel titulo = UiUtil.subtitulo("Tareas que requieren atención");
        titulo.setFont(Tema.PEQUENA.deriveFont(Font.BOLD));
        panel.add(titulo, BorderLayout.NORTH);
        UiUtil.prepararTabla(tablaTareas);
        tablaTareas.setRowHeight(30);
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
                    cargarTarjetas(datos.resumen());
                    cargarGraficos(datos.resumen());
                    cargarEtapas(datos.resumen());
                    cargarProcesos(datos.filas());
                    cargarTareas(datos.tareas());
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

    private void cargarTarjetas(ResumenProgreso resumen) {
        tarjetas.removeAll();
        tarjetas.add(tarjeta("Plantillas abiertas", resumen.getPlantillasAbiertas(), Tema.PRIMARIO));
        tarjetas.add(tarjeta("En recepción", resumen.getRecibiendo(), Tema.INFO));
        tarjetas.add(tarjeta("En alistamiento", resumen.getAlistando(), Tema.ACENTO));
        tarjetas.add(tarjeta("Listas para despacho", resumen.getListas(), Tema.EXITO));
        tarjetas.add(tarjeta("Con faltante", resumen.getRecepcionesConFaltante(), Tema.ADVERTENCIA));
        tarjetas.revalidate();
        tarjetas.repaint();
    }

    private JPanel tarjeta(String titulo, int valor, Color color) {
        JPanel panel = new JPanel(new BorderLayout(6, 0));
        panel.setBackground(Tema.SUPERFICIE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, color),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        JLabel numero = new JLabel(String.valueOf(valor));
        numero.setFont(Tema.fuente(Font.BOLD, 24));
        numero.setForeground(color);
        JLabel nombre = new JLabel(titulo);
        nombre.setFont(Tema.PEQUENA);
        nombre.setForeground(Tema.TEXTO_SUAVE);
        panel.add(numero, BorderLayout.WEST);
        panel.add(nombre, BorderLayout.CENTER);
        return panel;
    }

    private void cargarGraficos(ResumenProgreso resumen) {
        graficoEtapas.setDatos(List.of(
                new GraficoDonut.Segmento("Cargadas", resumen.getCargadas(), Tema.TEXTO_SUAVE),
                new GraficoDonut.Segmento("En compra", resumen.getEnCompra(), Tema.INFO),
                new GraficoDonut.Segmento("Recibiendo", resumen.getRecibiendo(), Tema.ADVERTENCIA),
                new GraficoDonut.Segmento("Alistando", resumen.getAlistando(), Tema.ACENTO),
                new GraficoDonut.Segmento("Listas", resumen.getListas(), Tema.EXITO)));
        graficoBarras.setDatos(List.of(
                new GraficoBarrasEtapas.Barra("Cargadas", resumen.getCargadas(), Tema.TEXTO_SUAVE),
                new GraficoBarrasEtapas.Barra("En compra", resumen.getEnCompra(), Tema.INFO),
                new GraficoBarrasEtapas.Barra("Recibiendo", resumen.getRecibiendo(), Tema.ADVERTENCIA),
                new GraficoBarrasEtapas.Barra("Alistando", resumen.getAlistando(), Tema.ACENTO),
                new GraficoBarrasEtapas.Barra("Listas", resumen.getListas(), Tema.EXITO)));
    }

    private void cargarEtapas(ResumenProgreso resumen) {
        etapas.removeAll();
        etapas.add(etapa("CARGADA", resumen.getCargadas(), Tema.TEXTO_SUAVE));
        etapas.add(etapa("EN COMPRA", resumen.getEnCompra(), Tema.INFO));
        etapas.add(etapa("RECIBIENDO", resumen.getRecibiendo(), Tema.ADVERTENCIA));
        etapas.add(etapa("ALISTANDO", resumen.getAlistando(), Tema.ACENTO));
        etapas.add(etapa("LISTA", resumen.getListas(), Tema.EXITO));
        etapas.revalidate();
        etapas.repaint();
    }

    private JPanel etapa(String nombre, int cantidad, Color color) {
        JPanel panel = new JPanel(new BorderLayout(2, 0));
        panel.setBackground(Tema.SUPERFICIE);
        panel.setBorder(BorderFactory.createLineBorder(Tema.BORDE));
        JLabel numero = new JLabel(String.valueOf(cantidad), JLabel.CENTER);
        numero.setFont(Tema.fuente(Font.BOLD, 18));
        numero.setForeground(color);
        JLabel texto = new JLabel(nombre, JLabel.CENTER);
        texto.setFont(Tema.PEQUENA.deriveFont(Font.BOLD));
        texto.setForeground(Tema.TEXTO_SUAVE);
        panel.add(numero, BorderLayout.CENTER);
        panel.add(texto, BorderLayout.SOUTH);
        return panel;
    }

    private void cargarProcesos(List<FilaProgreso> filas) {
        modelo.setRowCount(0);
        for (FilaProgreso fila : filas) {
            modelo.addRow(new Object[]{fila.getNombreMercado(), fila.getFechaCarga(), fila.getFechaProgramada(),
                    Formato.estado(fila.getEstado()), barra(fila.getPorcentajeCompra(), Tema.INFO),
                    barra(fila.getPorcentajeRecepcion(), Tema.ADVERTENCIA),
                    barra(fila.getPorcentajeAlistamiento(), Tema.ACENTO),
                    barra(fila.getPorcentajeDespacho(), Tema.EXITO), atencion(fila)});
        }
        tabla.getColumnModel().getColumn(4).setCellRenderer(new BarraRenderer(Tema.INFO));
        tabla.getColumnModel().getColumn(5).setCellRenderer(new BarraRenderer(Tema.ADVERTENCIA));
        tabla.getColumnModel().getColumn(6).setCellRenderer(new BarraRenderer(Tema.ACENTO));
        tabla.getColumnModel().getColumn(7).setCellRenderer(new BarraRenderer(Tema.EXITO));
    }

    private void cargarTareas(List<TareaProgreso> tareas) {
        modeloTareas.setRowCount(0);
        for (TareaProgreso tarea : tareas) {
            modeloTareas.addRow(new Object[]{tarea.getPrioridad(), tarea.getNombreMercado(),
                    tarea.getDescripcion(), Formato.estado(tarea.getEstado())});
        }
    }

    private static String barra(double valor, Color color) {
        return String.format(java.util.Locale.US, "%.1f%%", valor);
    }

    private static String atencion(FilaProgreso fila) {
        if (fila.isTieneNovedad()) return "Novedad";
        if (fila.isTieneFaltante()) return "Faltante";
        return "Normal";
    }

    private static final class BarraRenderer extends BarraProgreso implements TableCellRenderer {
        private final Color color;

        private BarraRenderer(Color color) {
            this.color = color;
            setColor(color);
        }

        @Override
        public Component getTableCellRendererComponent(javax.swing.JTable table, Object value, boolean selected,
                                                       boolean focused, int row, int column) {
            setValor(valor(value));
            setBackground(selected ? Tema.PRIMARIO_SUAVE : Tema.SUPERFICIE);
            return this;
        }

        private static double valor(Object value) {
            if (value == null) return 0;
            try {
                return Double.parseDouble(value.toString().replace("%", ""));
            } catch (NumberFormatException ex) {
                return 0;
            }
        }
    }
}
