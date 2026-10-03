package agromedio.modelo;

public class DetallePlantilla {
    private int idDetallePlantilla;
    private int idPlantilla;
    private int idProducto;
    private double cantidadSolicitada;

    // Campos de presentacion
    private String nombreProducto;
    private String abreviatura;

    public DetallePlantilla() {}

    public DetallePlantilla(int idDetallePlantilla, int idPlantilla, int idProducto, double cantidadSolicitada) {
        this.idDetallePlantilla = idDetallePlantilla;
        this.idPlantilla = idPlantilla;
        this.idProducto = idProducto;
        this.cantidadSolicitada = cantidadSolicitada;
    }

    public int getIdDetallePlantilla() { return idDetallePlantilla; }
    public void setIdDetallePlantilla(int idDetallePlantilla) { this.idDetallePlantilla = idDetallePlantilla; }
    public int getIdPlantilla() { return idPlantilla; }
    public void setIdPlantilla(int idPlantilla) { this.idPlantilla = idPlantilla; }
    public int getIdProducto() { return idProducto; }
    public void setIdProducto(int idProducto) { this.idProducto = idProducto; }
    public double getCantidadSolicitada() { return cantidadSolicitada; }
    public void setCantidadSolicitada(double cantidadSolicitada) { this.cantidadSolicitada = cantidadSolicitada; }
    public String getNombreProducto() { return nombreProducto; }
    public void setNombreProducto(String nombreProducto) { this.nombreProducto = nombreProducto; }
    public String getAbreviatura() { return abreviatura; }
    public void setAbreviatura(String abreviatura) { this.abreviatura = abreviatura; }
}
