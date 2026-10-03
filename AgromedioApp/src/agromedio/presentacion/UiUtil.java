package agromedio.presentacion;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

/** Utilidades de interfaz: estilo de planta (RNF01) y mensajes uniformes. */
public final class UiUtil {

    public static final Font FUENTE = new Font("Segoe UI", Font.PLAIN, 16);
    public static final Font FUENTE_NEGRITA = new Font("Segoe UI", Font.BOLD, 16);
    public static final Font FUENTE_TITULO = new Font("Segoe UI", Font.BOLD, 20);
    public static final Font FUENTE_BOTON = new Font("Segoe UI", Font.BOLD, 16);

    private UiUtil() {}

    /** Boton de gran tamano para operadores de planta (RNF01). */
    public static JButton boton(String texto) {
        JButton b = new JButton(texto);
        b.setFont(FUENTE_BOTON);
        b.setPreferredSize(new Dimension(Math.max(170, texto.length() * 12), 48));
        return b;
    }

    public static JTextField campo() {
        JTextField t = new JTextField();
        t.setFont(FUENTE);
        return t;
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
        tabla.setFont(FUENTE);
        tabla.setRowHeight(28);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.getTableHeader().setFont(FUENTE_NEGRITA);
        TableColumnModel cm = tabla.getColumnModel();
        for (int i = 0; i < cm.getColumnCount(); i++) {
            cm.getColumn(i).setPreferredWidth(140);
        }
    }

    public static void mensaje(Component padre, String texto) {
        JOptionPane.showMessageDialog(padre, texto, "Agromedio", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void error(Component padre, String texto) {
        JOptionPane.showMessageDialog(padre, texto, "Agromedio - Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void error(Component padre, Exception e) {
        String msg = e.getMessage() == null ? e.toString() : e.getMessage();
        error(padre, msg);
    }

    public static boolean confirmar(Component padre, String texto) {
        return JOptionPane.showConfirmDialog(padre, texto, "Agromedio",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }

    /** Aplica la fuente base a todos los componentes de un panel. */
    public static void fuenteRaiz(JComponent raiz) {
        aplicar(raiz);
    }

    private static void aplicar(JComponent c) {
        c.setFont(FUENTE);
        for (java.awt.Component hijo : c.getComponents()) {
            if (hijo instanceof JComponent jc) {
                aplicar(jc);
            } else {
                hijo.setFont(FUENTE);
            }
        }
    }
}
