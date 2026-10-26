package agromedio.presentacion;

import javax.swing.JComponent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.util.List;

/** Grafico circular vectorial para distribuciones pequeñas de estados. */
public final class GraficoDonut extends JComponent {
    private List<Segmento> segmentos = List.of();
    private int total;

    public GraficoDonut() {
        setOpaque(false);
        setPreferredSize(new Dimension(190, 190));
        setMinimumSize(new Dimension(120, 120));
    }

    public void setDatos(List<Segmento> segmentos) {
        this.segmentos = segmentos == null ? List.of() : List.copyOf(segmentos);
        total = this.segmentos.stream().mapToInt(Segmento::valor).sum();
        StringBuilder descripcion = new StringBuilder("Distribución: ");
        for (Segmento segmento : this.segmentos) {
            if (descripcion.length() > 15) descripcion.append(", ");
            descripcion.append(segmento.nombre()).append(" ").append(segmento.valor());
        }
        setToolTipText(descripcion.toString());
        revalidate();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int size = Math.min(getWidth(), getHeight()) - 24;
            if (size <= 0) return;
            int x = (getWidth() - size) / 2;
            int y = (getHeight() - size) / 2;
            g.setStroke(new BasicStroke(16f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
            if (total == 0) {
                g.setColor(Tema.BORDE);
                g.draw(new Arc2D.Double(x, y, size, size, 0, 360, Arc2D.OPEN));
            } else {
                double inicio = 90;
                for (Segmento segmento : segmentos) {
                    if (segmento.valor() <= 0) continue;
                    double angulo = 360.0 * segmento.valor() / total;
                    g.setColor(segmento.color());
                    g.draw(new Arc2D.Double(x, y, size, size, inicio, -angulo, Arc2D.OPEN));
                    inicio -= angulo;
                }
            }
            g.setColor(Tema.SUPERFICIE);
            g.fillOval(x + 22, y + 22, size - 44, size - 44);
            g.setColor(Tema.TEXTO);
            g.setFont(Tema.fuente(Font.BOLD, 26));
            String valor = String.valueOf(total);
            int ancho = g.getFontMetrics().stringWidth(valor);
            g.drawString(valor, (getWidth() - ancho) / 2, getHeight() / 2 + 8);
            g.setColor(Tema.TEXTO_SUAVE);
            g.setFont(Tema.PEQUENA);
            String texto = "abiertos";
            int anchoTexto = g.getFontMetrics().stringWidth(texto);
            g.drawString(texto, (getWidth() - anchoTexto) / 2, getHeight() / 2 + 27);
        } finally {
            g.dispose();
        }
    }

    public record Segmento(String nombre, int valor, Color color) {
        public Segmento {
            if (nombre == null || nombre.isBlank() || valor < 0 || color == null) {
                throw new IllegalArgumentException("Segmento de donut invalido");
            }
        }
    }
}
