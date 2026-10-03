package agromedio.modelo;

public class Alistamiento {
    public static final String EN_PROCESO = "EN_PROCESO";
    public static final String COMPLETO = "COMPLETO";

    private int idAlistamiento;
    private int idPlantilla;
    private String fechaAlistamiento;
    private String estado;

    public Alistamiento() {}

    public Alistamiento(int idAlistamiento, int idPlantilla, String fechaAlistamiento, String estado) {
        this.idAlistamiento = idAlistamiento;
        this.idPlantilla = idPlantilla;
        this.fechaAlistamiento = fechaAlistamiento;
        this.estado = estado;
    }

    public int getIdAlistamiento() { return idAlistamiento; }
    public void setIdAlistamiento(int idAlistamiento) { this.idAlistamiento = idAlistamiento; }
    public int getIdPlantilla() { return idPlantilla; }
    public void setIdPlantilla(int idPlantilla) { this.idPlantilla = idPlantilla; }
    public String getFechaAlistamiento() { return fechaAlistamiento; }
    public void setFechaAlistamiento(String fechaAlistamiento) { this.fechaAlistamiento = fechaAlistamiento; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
