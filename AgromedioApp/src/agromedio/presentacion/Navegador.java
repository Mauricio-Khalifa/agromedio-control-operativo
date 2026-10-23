package agromedio.presentacion;

import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.Dimension;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Administra una sola instancia visible por modulo dentro del escritorio. */
public final class Navegador {

    private final JDesktopPane escritorio;
    private final Map<String, JInternalFrame> abiertos = new LinkedHashMap<>();
    private Consumer<String> alAbrir = titulo -> { };
    private Consumer<String> alCerrar = clave -> { };

    public Navegador(JDesktopPane escritorio) {
        this.escritorio = escritorio;
    }

    public void setAlAbrir(Consumer<String> alAbrir) {
        this.alAbrir = alAbrir == null ? titulo -> { } : alAbrir;
    }

    public void setAlCerrar(Consumer<String> alCerrar) {
        this.alCerrar = alCerrar == null ? clave -> { } : alCerrar;
    }

    public JInternalFrame abrir(String clave, Supplier<? extends JInternalFrame> fabrica) {
        JInternalFrame existente = abiertos.get(clave);
        if (existente != null && existente.isDisplayable() && !existente.isClosed()) {
            mostrar(existente);
            return existente;
        }

        JInternalFrame frame = fabrica.get();
        frame.setFrameIcon(null);
        abiertos.put(clave, frame);
        frame.addInternalFrameListener(new InternalFrameAdapter() {
            @Override
            public void internalFrameClosed(InternalFrameEvent e) {
                abiertos.remove(clave);
                alCerrar.accept(clave);
            }
        });
        escritorio.add(frame);
        frame.setVisible(true);
        mostrar(frame);
        return frame;
    }

    public void cerrarTodo() {
        for (JInternalFrame frame : abiertos.values().toArray(new JInternalFrame[0])) {
            frame.dispose();
        }
        abiertos.clear();
    }

    public int cantidadAbierta() {
        return abiertos.size();
    }

    private void mostrar(JInternalFrame frame) {
        try {
            frame.setMaximum(true);
            frame.setSelected(true);
        } catch (java.beans.PropertyVetoException ex) {
            Dimension tam = escritorio.getSize();
            frame.setBounds(0, 0, Math.max(tam.width, 800), Math.max(tam.height, 600));
        }
        alAbrir.accept(frame.getTitle());
    }
}
