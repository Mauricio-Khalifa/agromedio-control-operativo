package agromedio.presentacion;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.Icon;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Locale;

/** Utilidades visuales comunes: botones, formularios, tablas y mensajes. */
public final class UiUtil {

    public static final Font FUENTE = Tema.BASE;
    public static final Font FUENTE_NEGRITA = Tema.BASE_NEGRITA;
    public static final Font FUENTE_TITULO = Tema.TITULO;
    public static final Font FUENTE_BOTON = Tema.BASE_NEGRITA;

    private UiUtil() {}

    public static JButton boton(String texto) {
        JButton b = new JButton(texto);
        b.setFont(FUENTE_BOTON);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.putClientProperty("JButton.buttonType", "roundRect");
        b.setMargin(new Insets(7, 14, 7, 14));
        b.setPreferredSize(new Dimension(Math.max(116, texto.length() * 9 + 44), 38));
        return b;
    }

    public static JButton botonPrimario(String texto) {
        JButton b = boton(texto);
        b.setBackground(Tema.PRIMARIO);
        b.setForeground(Color.WHITE);
        b.setOpaque(true);
        return b;
    }

    public static JButton botonPrimario(String texto, Icon icono) {
        return conIcono(botonPrimario(texto), icono);
    }

    public static JButton botonSecundario(String texto) {
        JButton b = boton(texto);
        b.setBackground(Tema.SUPERFICIE);
        b.setForeground(Tema.TEXTO);
        return b;
    }

    public static JButton botonSecundario(String texto, Icon icono) {
        return conIcono(botonSecundario(texto), icono);
    }

    public static JButton botonPeligro(String texto) {
        JButton b = boton(texto);
        b.setBackground(Tema.PELIGRO);
        b.setForeground(Color.WHITE);
        b.setOpaque(true);
        return b;
    }

    public static JButton botonPeligro(String texto, Icon icono) {
        return conIcono(botonPeligro(texto), icono);
    }

    private static JButton conIcono(JButton boton, Icon icono) {
        if (icono == null) {
            throw new IllegalArgumentException("El icono del boton es obligatorio");
        }
        boton.setIcon(icono);
        boton.setIconTextGap(8);
        return boton;
    }

    public static JTextField campo() {
        JTextField t = new JTextField();
        t.setFont(FUENTE);
        t.setPreferredSize(new Dimension(180, 34));
        return t;
    }

    public static JLabel titulo(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(Tema.TITULO);
        l.setForeground(Tema.TEXTO);
        return l;
    }

    public static JLabel subtitulo(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(Tema.PEQUENA);
        l.setForeground(Tema.TEXTO_SUAVE);
        return l;
    }

    /** Encabezado de ventana con titulo y linea de contexto. */
    public static JPanel encabezado(String titulo, String subtitulo) {
        JPanel panel = new JPanel(new BorderLayout(0, 2));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(4, 2, 8, 2));
        panel.add(titulo(titulo), BorderLayout.NORTH);
        if (subtitulo != null && !subtitulo.isBlank()) {
            panel.add(subtitulo(subtitulo), BorderLayout.SOUTH);
        }
        return panel;
    }

    /**
     * Integra el cierre de la ventana en su cabecera y elimina la necesidad de
     * mostrar una barra de titulo interna duplicada.
     *
     * @return true si se pudo instalar el control de cierre
     */
    public static boolean instalarCierre(JInternalFrame frame) {
        if (frame == null) {
            return false;
        }
        if (!(frame.getContentPane().getLayout() instanceof BorderLayout raiz)) {
            return false;
        }
        Component norte = raiz.getLayoutComponent(BorderLayout.NORTH);
        if (!(norte instanceof JPanel panel) || !(panel.getLayout() instanceof BorderLayout layout)) {
            return false;
        }

        JButton cerrar = botonCerrar(frame);
        Component cabecera = layout.getLayoutComponent(BorderLayout.NORTH);
        if (cabecera instanceof JPanel cabeceraPanel) {
            cabeceraPanel.add(cerrar, BorderLayout.EAST);
        } else {
            Component este = layout.getLayoutComponent(BorderLayout.EAST);
            if (este instanceof JPanel estePanel && estePanel.getLayout() instanceof FlowLayout) {
                estePanel.add(cerrar);
            } else if (este != null) {
                JPanel contenedor = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
                contenedor.setOpaque(false);
                contenedor.add(este);
                contenedor.add(cerrar);
                panel.add(contenedor, BorderLayout.EAST);
            } else {
                panel.add(cerrar, BorderLayout.EAST);
            }
        }

        frame.getRootPane().registerKeyboardAction(e -> frame.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_DOWN_MASK),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        panel.revalidate();
        panel.repaint();
        return true;
    }

    private static JButton botonCerrar(JInternalFrame frame) {
        JButton boton = new JButton("\u00D7");
        boton.setFont(Tema.fuente(Font.PLAIN, 19));
        boton.setForeground(Tema.TEXTO_SUAVE);
        boton.setPreferredSize(new Dimension(30, 30));
        boton.setMinimumSize(new Dimension(30, 30));
        boton.setMaximumSize(new Dimension(30, 30));
        boton.setMargin(new Insets(0, 0, 0, 0));
        boton.setBorder(BorderFactory.createEmptyBorder());
        boton.setContentAreaFilled(false);
        boton.setFocusPainted(false);
        boton.setOpaque(false);
        boton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        boton.setToolTipText("Cerrar ventana (Ctrl+W)");
        boton.getAccessibleContext().setAccessibleName("Cerrar ventana");
        boton.addActionListener(e -> frame.dispose());
        boton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                boton.setForeground(Tema.PELIGRO);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                boton.setForeground(Tema.TEXTO_SUAVE);
            }
        });
        return boton;
    }

    public static JPanel tarjeta(java.awt.LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setBackground(Tema.SUPERFICIE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Tema.BORDE),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        return panel;
    }

    /** Modelo de tabla que solo permite editar las filas marcadas. */
    public static DefaultTableModel modelo(Object[] columnas, boolean editable) {
        return new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return editable;
            }
        };
    }

    public static void prepararTabla(JTable tabla) {
        tabla.setFont(Tema.TABLA);
        tabla.setRowHeight(32);
        tabla.setShowGrid(false);
        tabla.setIntercellSpacing(new Dimension(0, 1));
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setFillsViewportHeight(true);
        tabla.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        tabla.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        tabla.setDefaultRenderer(Object.class, new RendererGeneral());
        tabla.getTableHeader().setFont(Tema.TABLA_ENCABEZADO);
        tabla.getTableHeader().setReorderingAllowed(false);
        tabla.getTableHeader().setPreferredSize(new Dimension(0, 36));

        TableColumnModel cm = tabla.getColumnModel();
        for (int i = 0; i < cm.getColumnCount(); i++) {
            TableColumn columna = cm.getColumn(i);
            String nombre = tabla.getColumnName(i);
            int ancho = i == 0 ? 210 : 120;
            if (nombre != null && nombre.toLowerCase(Locale.ROOT).contains("observ")) {
                ancho = 240;
            }
            columna.setPreferredWidth(ancho);
            columna.setMinWidth(70);
            if (nombre != null && nombre.toLowerCase(Locale.ROOT).contains("estado")) {
                columna.setCellRenderer(new RendererEstado());
            } else {
                columna.setCellRenderer(new RendererGeneral());
            }
        }
    }

    public static void mensaje(Component padre, String texto) {
        JOptionPane.showMessageDialog(padre, texto, "Agromedio", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void error(Component padre, String texto) {
        JOptionPane.showMessageDialog(padre, texto, "Agromedio - Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void error(Component padre, Exception e) {
        String msg;
        if (e instanceof SQLException) {
            msg = "No se pudo completar la operacion en la base de datos.\n\nDetalle: " + e.getMessage();
        } else if (e instanceof NumberFormatException) {
            msg = "Ingrese un numero valido. Ejemplo: 12,5 o 12.5";
        } else if (e instanceof NullPointerException) {
            msg = "Faltan datos obligatorios para completar la operacion.";
        } else {
            msg = e.getMessage() == null ? e.toString() : e.getMessage();
        }
        error(padre, msg);
    }

    public static boolean confirmar(Component padre, String texto) {
        return JOptionPane.showConfirmDialog(padre, texto, "Agromedio",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }

    /** Aplica solo la fuente base al contenedor; no aplasta la jerarquia de sus hijos. */
    public static void fuenteRaiz(JComponent raiz) {
        raiz.setFont(FUENTE);
    }

    private static final class RendererGeneral extends DefaultTableCellRenderer {
        private final JCheckBox check = new JCheckBox();

        RendererGeneral() {
            setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        }

        @Override
        public Component getTableCellRendererComponent(JTable tabla, Object valor, boolean seleccion,
                                                       boolean foco, int fila, int columna) {
            if (valor instanceof Boolean b) {
                check.setSelected(b);
                check.setOpaque(true);
                check.setHorizontalAlignment(JCheckBox.CENTER);
                check.setBackground(fondo(tabla, seleccion, fila));
                check.setForeground(Tema.TEXTO);
                return check;
            }

            super.getTableCellRendererComponent(tabla, valor, seleccion, foco, fila, columna);
            setFont(Tema.TABLA);
            setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
            setBackground(fondo(tabla, seleccion, fila));
            setForeground(seleccion ? tabla.getSelectionForeground() : Tema.TEXTO);
            setHorizontalAlignment(valor instanceof Number ? RIGHT : LEFT);
            if (valor instanceof Number n) {
                setText(Formato.numero(n));
            } else if (valor instanceof LocalDate fecha) {
                setText(Formato.fecha(fecha));
            } else if (valor instanceof String texto && texto.matches("\\d{4}-\\d{2}-\\d{2}")) {
                setText(Formato.fecha(texto));
            } else {
                setText(valor == null ? "" : valor.toString());
            }
            return this;
        }

        private Color fondo(JTable tabla, boolean seleccion, int fila) {
            if (seleccion) {
                return tabla.getSelectionBackground();
            }
            return fila % 2 == 0 ? Tema.SUPERFICIE : Tema.SUPERFICIE_ALT;
        }
    }

    private static final class RendererEstado extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable tabla, Object valor, boolean seleccion,
                                                       boolean foco, int fila, int columna) {
            super.getTableCellRendererComponent(tabla, valor, seleccion, foco, fila, columna);
            setText(Formato.estado(valor == null ? "" : valor.toString()));
            setFont(Tema.TABLA.deriveFont(Font.BOLD));
            setHorizontalAlignment(CENTER);
            setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
            setBackground(seleccion ? tabla.getSelectionBackground()
                    : (fila % 2 == 0 ? Tema.SUPERFICIE : Tema.SUPERFICIE_ALT));
            setForeground(seleccion ? tabla.getSelectionForeground() : colorEstado(valor));
            return this;
        }

        private Color colorEstado(Object valor) {
            String estado = valor == null ? "" : valor.toString().toUpperCase(Locale.ROOT);
            return switch (estado) {
                case "CARGADA", "BORRADOR" -> Tema.TEXTO_SUAVE;
                case "EN_COMPRA", "COMPRADA" -> Tema.ADVERTENCIA;
                case "RECIBIENDO", "RECIBIDA", "ALISTADA" -> Tema.INFO;
                case "ALISTANDO", "LISTA" -> Tema.ACENTO;
                case "DESPACHADA", "CERRADA", "CONFIRMADA" -> Tema.EXITO;
                case "ANULADA" -> Tema.PELIGRO;
                default -> Tema.TEXTO;
            };
        }
    }
}
