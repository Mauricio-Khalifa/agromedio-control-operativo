package agromedio.presentacion;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/** Contexto operativo compartido por el shell: modulo activo y estado de la jornada. */
public final class ContextoOperativo {

    public static final String MODULO_ACTUAL = "moduloActual";
    public static final String ESTADO = "estado";

    private final PropertyChangeSupport cambios = new PropertyChangeSupport(this);
    private String moduloActual = "Sin módulo activo";
    private String estado = "Listo";

    public String getModuloActual() {
        return moduloActual;
    }

    public void setModuloActual(String moduloActual) {
        String anterior = this.moduloActual;
        this.moduloActual = moduloActual == null || moduloActual.isBlank()
                ? "Sin modulo activo" : moduloActual;
        cambios.firePropertyChange(MODULO_ACTUAL, anterior, this.moduloActual);
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        String anterior = this.estado;
        this.estado = estado == null ? "Listo" : estado;
        cambios.firePropertyChange(ESTADO, anterior, this.estado);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        cambios.addPropertyChangeListener(listener);
    }
}
