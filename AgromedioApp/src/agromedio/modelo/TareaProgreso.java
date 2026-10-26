package agromedio.modelo;

/** Accion pendiente derivada del estado operativo de una plantilla abierta. */
public class TareaProgreso {
    public static final String ALTA = "ALTA";
    public static final String MEDIA = "MEDIA";
    public static final String BAJA = "BAJA";

    private int idPlantilla;
    private String nombreMercado;
    private String descripcion;
    private String prioridad;
    private String estado;

    public TareaProgreso() {
    }

    public TareaProgreso(int idPlantilla, String nombreMercado, String descripcion,
                         String prioridad, String estado) {
        this.idPlantilla = idPlantilla;
        this.nombreMercado = nombreMercado;
        this.descripcion = descripcion;
        this.prioridad = prioridad;
        this.estado = estado;
    }

    public int getIdPlantilla() { return idPlantilla; }
    public void setIdPlantilla(int idPlantilla) { this.idPlantilla = idPlantilla; }
    public String getNombreMercado() { return nombreMercado; }
    public void setNombreMercado(String nombreMercado) { this.nombreMercado = nombreMercado; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getPrioridad() { return prioridad; }
    public void setPrioridad(String prioridad) { this.prioridad = prioridad; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
