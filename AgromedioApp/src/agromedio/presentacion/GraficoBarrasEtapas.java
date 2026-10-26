package agromedio.presentacion;

import javax.swing.JComponent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;

/** Barras horizontales vectoriales para comparar carga por etapa. */
public final class GraficoBarrasEtapas extends JComponent {
    private List<Barra> barras = List.of();

    public GraficoBarrasEtapas() {
        setOpaque(false);
        setPreferredSize(new Dimension(360, 190));
        setMinimumSize(new Dimension(240, 150));
    }

    public void setDatos(List<Barra> barras) {
        this.barras = barras == null ? List.of() : List.copyOf(barras);
        revalidate();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int max = barras.stream().mapToInt(Barra::valor).max().orElse(1);
            int alto = 28;
            int y = 12;
            int inicioBarra = 104;
            int anchoBarra = Math.max(80, getWidth() - inicioBarra - 38);
            for (Barra barra : barras) {
                g.setColor(Tema.TEXTO_SUAVE);
                g.setFont(Tema.PEQUENA.deriveFont(Font.BOLD));
                g.drawString(barra.nombre(), 0, y + 18);
                g.setColor(Tema.SUPERFICIE_ALT);
                g.fillRoundRect(inicioBarra, y + 5, anchoBarra, 14, 7, 7);
                int ancho = barra.valor() == 0 ? 0 : Math.max(8, anchoBarra * barra.valor() / max);
                g.setColor(barra.color());
                g.fillRoundRect(inicioBarra, y + 5, ancho, 14, 7, 7);
                g.setColor(Tema.TEXTO);
                g.drawString(String.valueOf(barra.valor()), inicioBarra + anchoBarra + 10, y + 18);
                y += alto;
            }
            g.setColor(Tema.BORDE);
            g.setStroke(new BasicStroke(1f));
            g.drawLine(inicioBarra, y + 1, inicioBarra + anchoBarra, y + 1);
        } finally {
            g.dispose();
        }
    }

    public record Barra(String nombre, int valor, Color color) {
        public Barra {
            if (nombre == null || nombre.isBlank() || valor < 0 || color == null) {
                throw new IllegalArgumentException("Barra de etapa invalida");
            }
        }
    }
}
