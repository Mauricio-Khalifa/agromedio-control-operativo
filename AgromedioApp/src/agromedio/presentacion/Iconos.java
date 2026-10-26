package agromedio.presentacion;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

/** Iconos vectoriales del sistema, dibujados con una reticula comun de 20 px. */
public final class Iconos {

    private static final int TAMANO = 20;
    private static final float GROSOR = 1.7f;

    private Iconos() {
    }

    private interface Definicion {
    }

    public enum Tipo implements Definicion {
        CARGA,
        PLANTILLAS,
        COMPRAS,
        RECEPCION,
        ALISTAMIENTO,
        DESPACHO,
        PRODUCTOS,
        CLIENTES,
        CATEGORIAS,
        UNIDADES,
        PROVEEDORES,
        REPORTES,
        PROGRESO
    }

    public enum Accion implements Definicion {
        NUEVO,
        GUARDAR,
        ELIMINAR,
        BUSCAR,
        REFRESCAR,
        IMPRIMIR,
        LIMPIAR,
        AGREGAR,
        QUITAR,
        ABRIR,
        CERRAR,
        VER,
        VERIFICAR,
        COMPLETAR,
        CARGAR
    }

    public static Icon icono(Tipo tipo, Color color) {
        if (tipo == null) {
            throw new IllegalArgumentException("El tipo de icono es obligatorio");
        }
        return crear(tipo, color);
    }

    public static Icon accion(Accion accion, Color color) {
        if (accion == null) {
            throw new IllegalArgumentException("La accion del icono es obligatoria");
        }
        return crear(accion, color);
    }

    private static Icon crear(Definicion tipo, Color color) {
        if (color == null) {
            throw new IllegalArgumentException("El color del icono es obligatorio");
        }
        return new IconoVectorial(tipo, color);
    }

    private static final class IconoVectorial implements Icon {
        private final Definicion tipo;
        private final Color color;

        private IconoVectorial(Definicion tipo, Color color) {
            this.tipo = tipo;
            this.color = color;
        }

        @Override
        public int getIconWidth() {
            return TAMANO;
        }

        @Override
        public int getIconHeight() {
            return TAMANO;
        }

        @Override
        public void paintIcon(Component componente, Graphics graphics, int x, int y) {
            if (!(graphics instanceof Graphics2D original)) {
                throw new IllegalArgumentException("Se requiere Graphics2D para pintar iconos");
            }
            Graphics2D g = (Graphics2D) original.create();
            try {
                g.translate(x, y);
                g.setColor(color);
                g.setStroke(new BasicStroke(GROSOR, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
                dibujar(g, tipo);
            } finally {
                g.dispose();
            }
        }

        private static void dibujar(Graphics2D g, Definicion tipo) {
            if (tipo instanceof Tipo modulo) {
                switch (modulo) {
                    case CARGA -> dibujarCarga(g);
                    case PLANTILLAS -> dibujarPlantillas(g);
                    case COMPRAS -> dibujarCompras(g);
                    case RECEPCION -> dibujarRecepcion(g);
                    case ALISTAMIENTO -> dibujarAlistamiento(g);
                    case DESPACHO -> dibujarDespacho(g);
                    case PRODUCTOS -> dibujarProductos(g);
                    case CLIENTES -> dibujarClientes(g);
                    case CATEGORIAS -> dibujarCategorias(g);
                    case UNIDADES -> dibujarUnidades(g);
                    case PROVEEDORES -> dibujarProveedores(g);
                    case REPORTES -> dibujarReportes(g);
                    case PROGRESO -> dibujarProgreso(g);
                }
            } else if (tipo instanceof Accion accion) {
                switch (accion) {
                    case NUEVO -> dibujarNuevo(g);
                    case GUARDAR -> dibujarGuardar(g);
                    case ELIMINAR -> dibujarEliminar(g);
                    case BUSCAR -> dibujarBuscar(g);
                    case REFRESCAR -> dibujarRefrescar(g);
                    case IMPRIMIR -> dibujarImprimir(g);
                    case LIMPIAR -> dibujarLimpiar(g);
                    case AGREGAR -> dibujarAgregar(g);
                    case QUITAR -> dibujarQuitar(g);
                    case ABRIR -> dibujarAbrir(g);
                    case CERRAR -> dibujarCerrar(g);
                    case VER -> dibujarVer(g);
                    case VERIFICAR -> dibujarVerificar(g);
                    case COMPLETAR -> dibujarCompletar(g);
                    case CARGAR -> dibujarCargar(g);
                }
            }
        }

        private static void dibujarCarga(Graphics2D g) {
            Path2D documento = new Path2D.Double();
            documento.moveTo(4, 2.5);
            documento.lineTo(12, 2.5);
            documento.lineTo(16, 6.5);
            documento.lineTo(16, 17.5);
            documento.lineTo(4, 17.5);
            documento.closePath();
            g.draw(documento);
            g.draw(new Line2D.Double(12, 2.5, 12, 6.5));
            g.draw(new Line2D.Double(12, 6.5, 16, 6.5));
            g.draw(new Line2D.Double(10, 14.5, 10, 8.5));
            g.draw(new Line2D.Double(7.5, 11, 10, 8.5));
            g.draw(new Line2D.Double(12.5, 11, 10, 8.5));
        }

        private static void dibujarPlantillas(Graphics2D g) {
            g.draw(new RoundRectangle2D.Double(5, 2, 10, 16, 1.5, 1.5));
            g.draw(new RoundRectangle2D.Double(7.5, 1, 5, 3, 1.2, 1.2));
            g.draw(new Line2D.Double(8, 8, 12.5, 8));
            g.draw(new Line2D.Double(8, 11.5, 12.5, 11.5));
            g.draw(new Line2D.Double(8, 15, 12.5, 15));
        }

        private static void dibujarCompras(Graphics2D g) {
            g.draw(new Line2D.Double(2.5, 4, 5, 4));
            Path2D carrito = new Path2D.Double();
            carrito.moveTo(5, 4);
            carrito.lineTo(6.5, 13.5);
            carrito.lineTo(16.5, 13.5);
            carrito.lineTo(18, 7);
            carrito.lineTo(5.5, 7);
            g.draw(carrito);
            g.draw(new Ellipse2D.Double(7.5, 15, 2, 2));
            g.draw(new Ellipse2D.Double(14, 15, 2, 2));
        }

        private static void dibujarRecepcion(Graphics2D g) {
            Path2D caja = new Path2D.Double();
            caja.moveTo(3, 7);
            caja.lineTo(10, 4);
            caja.lineTo(17, 7);
            caja.lineTo(10, 10);
            caja.closePath();
            g.draw(caja);
            g.draw(new Line2D.Double(3, 7, 3, 14.5));
            g.draw(new Line2D.Double(3, 14.5, 10, 17.5));
            g.draw(new Line2D.Double(10, 10, 10, 17.5));
            g.draw(new Line2D.Double(17, 7, 17, 14.5));
            g.draw(new Line2D.Double(17, 14.5, 10, 17.5));
            g.draw(new Line2D.Double(10, 2.5, 10, 8));
            g.draw(new Line2D.Double(7.5, 5, 10, 2.5));
            g.draw(new Line2D.Double(12.5, 5, 10, 2.5));
        }

        private static void dibujarAlistamiento(Graphics2D g) {
            g.draw(new RoundRectangle2D.Double(3, 2.5, 14, 15, 1.5, 1.5));
            g.draw(new Line2D.Double(6, 6.5, 7, 7.5));
            g.draw(new Line2D.Double(7, 7.5, 9, 5.5));
            g.draw(new Line2D.Double(10.5, 6.5, 14.5, 6.5));
            g.draw(new Line2D.Double(6, 10.5, 7, 11.5));
            g.draw(new Line2D.Double(7, 11.5, 9, 9.5));
            g.draw(new Line2D.Double(10.5, 10.5, 14.5, 10.5));
            g.draw(new Line2D.Double(6, 14.5, 7, 15.5));
            g.draw(new Line2D.Double(7, 15.5, 9, 13.5));
            g.draw(new Line2D.Double(10.5, 14.5, 14.5, 14.5));
        }

        private static void dibujarDespacho(Graphics2D g) {
            Path2D camion = new Path2D.Double();
            camion.moveTo(2.5, 6);
            camion.lineTo(12, 6);
            camion.lineTo(12, 13.5);
            camion.lineTo(2.5, 13.5);
            camion.closePath();
            g.draw(camion);
            Path2D cabina = new Path2D.Double();
            cabina.moveTo(12, 8);
            cabina.lineTo(15.5, 8);
            cabina.lineTo(17.5, 10.5);
            cabina.lineTo(17.5, 13.5);
            cabina.lineTo(12, 13.5);
            g.draw(cabina);
            g.draw(new Ellipse2D.Double(5, 12.5, 3, 3));
            g.draw(new Ellipse2D.Double(14, 12.5, 3, 3));
            g.draw(new Line2D.Double(14, 10, 16, 10));
        }

        private static void dibujarProductos(Graphics2D g) {
            Path2D caja = new Path2D.Double();
            caja.moveTo(3, 6.5);
            caja.lineTo(10, 3);
            caja.lineTo(17, 6.5);
            caja.lineTo(10, 10);
            caja.closePath();
            g.draw(caja);
            g.draw(new Line2D.Double(3, 6.5, 3, 14.5));
            g.draw(new Line2D.Double(3, 14.5, 10, 18));
            g.draw(new Line2D.Double(10, 10, 10, 18));
            g.draw(new Line2D.Double(17, 6.5, 17, 14.5));
            g.draw(new Line2D.Double(17, 14.5, 10, 18));
            g.draw(new Line2D.Double(10, 3, 10, 10));
        }

        private static void dibujarClientes(Graphics2D g) {
            g.draw(new Ellipse2D.Double(7, 2.5, 6, 6));
            g.draw(new Arc2D.Double(3.5, 9, 13, 9, 200, 140, Arc2D.OPEN));
            g.draw(new Line2D.Double(5, 17.5, 15, 17.5));
        }

        private static void dibujarCategorias(Graphics2D g) {
            Path2D etiqueta = new Path2D.Double();
            etiqueta.moveTo(3, 5);
            etiqueta.lineTo(10.5, 5);
            etiqueta.lineTo(17, 11.5);
            etiqueta.lineTo(11.5, 17);
            etiqueta.lineTo(5, 10.5);
            etiqueta.closePath();
            g.draw(etiqueta);
            g.draw(new Ellipse2D.Double(6.2, 6.8, 2, 2));
        }

        private static void dibujarUnidades(Graphics2D g) {
            g.draw(new RoundRectangle2D.Double(3, 7, 14, 6, 1, 1));
            g.draw(new Line2D.Double(6, 7, 6, 10));
            g.draw(new Line2D.Double(9, 7, 9, 11));
            g.draw(new Line2D.Double(12, 7, 12, 10));
            g.draw(new Line2D.Double(15, 7, 15, 11));
            g.draw(new Line2D.Double(3, 15.5, 17, 15.5));
        }

        private static void dibujarProveedores(Graphics2D g) {
            g.draw(new Line2D.Double(3, 17.5, 17, 17.5));
            g.draw(new RoundRectangle2D.Double(5, 6, 10, 11.5, 1, 1));
            g.draw(new Line2D.Double(7.5, 6, 7.5, 3));
            g.draw(new Line2D.Double(12.5, 6, 12.5, 3));
            g.draw(new Line2D.Double(7.5, 3, 12.5, 3));
            g.draw(new Line2D.Double(8, 9, 8, 11));
            g.draw(new Line2D.Double(12, 9, 12, 11));
            g.draw(new Line2D.Double(8, 14, 12, 14));
        }

        private static void dibujarProgreso(Graphics2D g) {
            g.draw(new Line2D.Double(3, 17.5, 17.5, 17.5));
            g.draw(new Line2D.Double(3, 17.5, 3, 3));
            g.draw(new Line2D.Double(5.5, 14, 8, 11.5));
            g.draw(new Line2D.Double(8, 11.5, 11, 13));
            g.draw(new Line2D.Double(11, 13, 16.5, 6));
        }

        private static void dibujarReportes(Graphics2D g) {
            g.draw(new Line2D.Double(3, 17.5, 17.5, 17.5));
            g.draw(new Line2D.Double(3, 17.5, 3, 3));
            g.draw(new Rectangle2D.Double(5.5, 11, 2.5, 6.5));
            g.draw(new Rectangle2D.Double(9.5, 7.5, 2.5, 10));
            g.draw(new Rectangle2D.Double(13.5, 4.5, 2.5, 13));
        }

        private static void dibujarNuevo(Graphics2D g) {
            g.draw(new Line2D.Double(10, 4, 10, 16));
            g.draw(new Line2D.Double(4, 10, 16, 10));
        }

        private static void dibujarGuardar(Graphics2D g) {
            g.draw(new RoundRectangle2D.Double(3, 2.5, 14, 15, 1.5, 1.5));
            g.draw(new Rectangle2D.Double(6, 3.5, 7, 4));
            g.draw(new Rectangle2D.Double(6, 11, 8, 5));
            g.draw(new Line2D.Double(8, 12.5, 12, 12.5));
        }

        private static void dibujarEliminar(Graphics2D g) {
            g.draw(new Line2D.Double(5, 5, 15, 5));
            g.draw(new Line2D.Double(8, 2.8, 12, 2.8));
            g.draw(new RoundRectangle2D.Double(6, 6, 8, 11, 1, 1));
            g.draw(new Line2D.Double(8.5, 8.5, 8.5, 14.5));
            g.draw(new Line2D.Double(11.5, 8.5, 11.5, 14.5));
        }

        private static void dibujarBuscar(Graphics2D g) {
            g.draw(new Ellipse2D.Double(3, 3, 9, 9));
            g.draw(new Line2D.Double(11, 11, 17, 17));
        }

        private static void dibujarRefrescar(Graphics2D g) {
            g.draw(new Arc2D.Double(3, 3, 14, 14, 45, 270, Arc2D.OPEN));
            g.draw(new Line2D.Double(16.5, 4, 16.5, 8));
            g.draw(new Line2D.Double(16.5, 4, 12.5, 4));
        }

        private static void dibujarImprimir(Graphics2D g) {
            g.draw(new RoundRectangle2D.Double(4, 7, 12, 7, 1, 1));
            g.draw(new Rectangle2D.Double(6, 3, 8, 5));
            g.draw(new Rectangle2D.Double(6, 12, 8, 5));
            g.draw(new Ellipse2D.Double(13, 9, 1, 1));
        }

        private static void dibujarLimpiar(Graphics2D g) {
            g.draw(new Line2D.Double(4, 5, 16, 17));
            g.draw(new Line2D.Double(9, 4, 16, 11));
            g.draw(new Line2D.Double(5, 6, 12, 13));
            g.draw(new Line2D.Double(3, 17, 9, 15));
        }

        private static void dibujarAgregar(Graphics2D g) {
            dibujarNuevo(g);
            g.draw(new Ellipse2D.Double(2.5, 2.5, 15, 15));
        }

        private static void dibujarQuitar(Graphics2D g) {
            g.draw(new Line2D.Double(5, 10, 15, 10));
            g.draw(new Ellipse2D.Double(2.5, 2.5, 15, 15));
        }

        private static void dibujarAbrir(Graphics2D g) {
            g.draw(new RoundRectangle2D.Double(3, 5, 14, 11, 1.5, 1.5));
            g.draw(new Line2D.Double(10, 8, 10, 14));
            g.draw(new Line2D.Double(7.5, 11.5, 10, 14));
            g.draw(new Line2D.Double(12.5, 11.5, 10, 14));
        }

        private static void dibujarCerrar(Graphics2D g) {
            g.draw(new Line2D.Double(5, 5, 15, 15));
            g.draw(new Line2D.Double(15, 5, 5, 15));
        }

        private static void dibujarVer(Graphics2D g) {
            g.draw(new Ellipse2D.Double(2.5, 5, 15, 10));
            g.draw(new Ellipse2D.Double(8, 7.5, 4, 5));
        }

        private static void dibujarVerificar(Graphics2D g) {
            g.draw(new Line2D.Double(3.5, 10.5, 7.5, 14.5));
            g.draw(new Line2D.Double(7.5, 14.5, 16.5, 5.5));
            g.draw(new Ellipse2D.Double(2.5, 2.5, 15, 15));
        }

        private static void dibujarCompletar(Graphics2D g) {
            g.draw(new RoundRectangle2D.Double(3, 2.5, 14, 15, 1.5, 1.5));
            g.draw(new Line2D.Double(6, 10, 8.5, 12.5));
            g.draw(new Line2D.Double(8.5, 12.5, 14.5, 6.5));
        }

        private static void dibujarCargar(Graphics2D g) {
            g.draw(new Line2D.Double(10, 16, 10, 4));
            g.draw(new Line2D.Double(6.5, 7.5, 10, 4));
            g.draw(new Line2D.Double(13.5, 7.5, 10, 4));
            g.draw(new Line2D.Double(4, 16.5, 16, 16.5));
        }
    }
}
