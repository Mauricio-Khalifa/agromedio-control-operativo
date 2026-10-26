package agromedio.presentacion;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

/** Barra de porcentaje vectorial para una etapa de una plantilla. */
public class BarraProgreso extends JComponent {
    private double valor;
    private Color color = Tema.PRIMARIO;

    public BarraProgreso() {
        setOpaque(false);
        setPreferredSize(new Dimension(110, 18));
        setMinimumSize(new Dimension(30, 18));
    }

    public void setValor(double valor) {
        this.valor = Math.max(0, Math.min(100, valor));
        repaint();
    }

    public void setColor(Color color) {
        if (color == null) throw new IllegalArgumentException("El color es obligatorio");
        this.color = color;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int alto = Math.min(10, getHeight() - 4);
            int y = (getHeight() - alto) / 2;
            int ancho = Math.max(1, getWidth());
            g.setColor(getBackground() == null ? Tema.SUPERFICIE_ALT : getBackground());
            g.fill(new RoundRectangle2D.Double(0, y, ancho, alto, alto, alto));
            if (valor > 0) {
                g.setColor(color);
                g.fill(new RoundRectangle2D.Double(0, y, Math.max(alto, ancho * valor / 100.0), alto, alto, alto));
            }
        } finally {
            g.dispose();
        }
    }
}
