package agromedio.modelo;

public class UnidadMedida {
    private int idUnidad;
    private String nombre;
    private String abreviatura;

    public UnidadMedida() {}

    public UnidadMedida(int idUnidad, String nombre, String abreviatura) {
        this.idUnidad = idUnidad;
        this.nombre = nombre;
        this.abreviatura = abreviatura;
    }

    public int getIdUnidad() { return idUnidad; }
    public void setIdUnidad(int idUnidad) { this.idUnidad = idUnidad; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getAbreviatura() { return abreviatura; }
    public void setAbreviatura(String abreviatura) { this.abreviatura = abreviatura; }

    @Override
    public String toString() { return nombre + " (" + abreviatura + ")"; }
}
