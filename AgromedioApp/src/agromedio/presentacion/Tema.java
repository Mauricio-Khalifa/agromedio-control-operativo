package agromedio.presentacion;

import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.UIManager;
import javax.swing.border.LineBorder;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;

/** Sistema visual unico de Agromedio: color, tipografia, densidad y componentes. */
public final class Tema {

    public static final Color FONDO = new Color(244, 246, 243);
    public static final Color SUPERFICIE = Color.WHITE;
    public static final Color SUPERFICIE_ALT = new Color(237, 241, 236);
    public static final Color BORDE = new Color(211, 221, 212);
    public static final Color TEXTO = new Color(24, 37, 30);
    public static final Color TEXTO_SUAVE = new Color(92, 107, 98);
    public static final Color PRIMARIO = new Color(47, 107, 79);
    public static final Color PRIMARIO_OSCURO = new Color(35, 79, 59);
    public static final Color PRIMARIO_SUAVE = new Color(230, 240, 232);
    public static final Color ACENTO = new Color(163, 100, 42);
    public static final Color EXITO = new Color(46, 125, 79);
    public static final Color ADVERTENCIA = new Color(168, 102, 18);
    public static final Color PELIGRO = new Color(180, 35, 24);
    public static final Color INFO = new Color(43, 108, 176);
    public static final Color SIDEBAR = new Color(28, 54, 41);
    public static final Color SIDEBAR_HOVER = new Color(41, 76, 58);
    public static final Color SIDEBAR_TEXTO = new Color(236, 244, 238);

    public static final Font BASE = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font BASE_NEGRITA = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font PEQUENA = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font TITULO = new Font("Segoe UI", Font.BOLD, 22);
    public static final Font SUBTITULO = new Font("Segoe UI", Font.BOLD, 16);
    public static final Font TABLA = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font TABLA_ENCABEZADO = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font MONO = new Font("Consolas", Font.PLAIN, 13);

    private Tema() {}

    public static void instalar() {
        try {
            FlatLightLaf.setup();
        } catch (Throwable ignore) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // se conserva el Look and Feel por defecto
            }
        }
        configurarGlobal();
    }

    private static void configurarGlobal() {
        UIManager.put("Panel.background", FONDO);
        UIManager.put("OptionPane.background", SUPERFICIE);
        UIManager.put("Viewport.background", SUPERFICIE);
        UIManager.put("InternalFrame.background", FONDO);
        UIManager.put("Label.foreground", TEXTO);
        UIManager.put("Label.font", BASE);
        UIManager.put("Button.font", BASE_NEGRITA);
        UIManager.put("Button.arc", 8);
        UIManager.put("Button.margin", new Insets(7, 14, 7, 14));
        UIManager.put("TextField.font", BASE);
        UIManager.put("TextField.margin", new Insets(6, 9, 6, 9));
        UIManager.put("ComboBox.font", BASE);
        UIManager.put("ComboBox.arc", 6);
        UIManager.put("Table.font", TABLA);
        UIManager.put("Table.rowHeight", 32);
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.showVerticalLines", false);
        UIManager.put("Table.intercellSpacing", new Dimension(0, 1));
        UIManager.put("Table.alternateRowColor", new Color(250, 251, 249));
        UIManager.put("Table.selectionBackground", PRIMARIO_SUAVE);
        UIManager.put("Table.selectionForeground", TEXTO);
        UIManager.put("TableHeader.font", TABLA_ENCABEZADO);
        UIManager.put("TableHeader.height", 36);
        UIManager.put("TableHeader.background", SUPERFICIE_ALT);
        UIManager.put("TableHeader.foreground", TEXTO);
        UIManager.put("TableHeader.separatorColor", BORDE);
        UIManager.put("TabbedPane.font", BASE_NEGRITA);
        UIManager.put("Menu.font", BASE);
        UIManager.put("MenuItem.font", BASE);
        UIManager.put("MenuBar.background", SUPERFICIE);
        UIManager.put("MenuBar.border", new LineBorder(BORDE));
        UIManager.put("ToolTip.font", PEQUENA);
        UIManager.put("ScrollBar.width", 12);
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Component.focusColor", PRIMARIO);
        UIManager.put("Component.arc", 8);
        UIManager.put("TextComponent.arc", 6);
        UIManager.put("Button.default.background", PRIMARIO);
        UIManager.put("Button.default.foreground", Color.WHITE);
        UIManager.put("Button.default.hoverBackground", PRIMARIO_OSCURO);
        UIManager.put("Button.default.pressedBackground", PRIMARIO_OSCURO);
        UIManager.put("Component.borderColor", BORDE);
        UIManager.put("Component.disabledBorderColor", BORDE);
        UIManager.put("SplitPane.background", FONDO);
        UIManager.put("SplitPane.dividerSize", 8);
        UIManager.put("SplitPaneDivider.style", "grip");
        UIManager.put("TitlePane.background", SUPERFICIE);
        UIManager.put("TitlePane.inactiveBackground", SUPERFICIE_ALT);
    }

    public static Font fuente(int estilo, float tamano) {
        return BASE.deriveFont(estilo, tamano);
    }
}
