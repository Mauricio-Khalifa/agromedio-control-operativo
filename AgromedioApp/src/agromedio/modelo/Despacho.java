package agromedio.modelo;

public class Despacho {
    public static final String EN_VERIFICACION = "EN_VERIFICACION";
    public static final String DESPACHADA = "DESPACHADA";

    private int idDespacho;
    private int idPlantilla;
    private String fechaDespacho;
    private String estado;
    private String novedades;

    // Campo de presentacion
    private String nombreMercado;

    public Despacho() {}

    public Despacho(int idDespacho, int idPlantilla, String fechaDespacho, String estado, String novedades) {
        this.idDespacho = idDespacho;
        this.idPlantilla = idPlantilla;
        this.fechaDespacho = fechaDespacho;
        this.estado = estado;
        this.novedades = novedades;
    }

    public int getIdDespacho() { return idDespacho; }
    public void setIdDespacho(int idDespacho) { this.idDespacho = idDespacho; }
    public int getIdPlantilla() { return idPlantilla; }
    public void setIdPlantilla(int idPlantilla) { this.idPlantilla = idPlantilla; }
    public String getFechaDespacho() { return fechaDespacho; }
    public void setFechaDespacho(String fechaDespacho) { this.fechaDespacho = fechaDespacho; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getNovedades() { return novedades; }
    public void setNovedades(String novedades) { this.novedades = novedades; }
    public String getNombreMercado() { return nombreMercado; }
    public void setNombreMercado(String nombreMercado) { this.nombreMercado = nombreMercado; }

    @Override
    public String toString() { return "Despacho #" + idDespacho + " - " + nombreMercado; }
}
