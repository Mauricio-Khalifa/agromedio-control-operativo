package agromedio.modelo;

public class DetalleAlistamiento {
    private int idDetalleAlistamiento;
    private int idAlistamiento;
    private int idProducto;
    private double cantidadAlistada;
    private boolean completado;

    // Campos de presentacion
    private String nombreProducto;
    private String abreviatura;
    private double cantidadSolicitada;

    public DetalleAlistamiento() {}

    public DetalleAlistamiento(int idDetalleAlistamiento, int idAlistamiento, int idProducto,
                               double cantidadAlistada, boolean completado) {
        this.idDetalleAlistamiento = idDetalleAlistamiento;
        this.idAlistamiento = idAlistamiento;
        this.idProducto = idProducto;
        this.cantidadAlistada = cantidadAlistada;
        this.completado = completado;
    }

    public int getIdDetalleAlistamiento() { return idDetalleAlistamiento; }
    public void setIdDetalleAlistamiento(int idDetalleAlistamiento) { this.idDetalleAlistamiento = idDetalleAlistamiento; }
    public int getIdAlistamiento() { return idAlistamiento; }
    public void setIdAlistamiento(int idAlistamiento) { this.idAlistamiento = idAlistamiento; }
    public int getIdProducto() { return idProducto; }
    public void setIdProducto(int idProducto) { this.idProducto = idProducto; }
    public double getCantidadAlistada() { return cantidadAlistada; }
    public void setCantidadAlistada(double cantidadAlistada) { this.cantidadAlistada = cantidadAlistada; }
    public boolean isCompletado() { return completado; }
    public void setCompletado(boolean completado) { this.completado = completado; }
    public String getNombreProducto() { return nombreProducto; }
    public void setNombreProducto(String nombreProducto) { this.nombreProducto = nombreProducto; }
    public String getAbreviatura() { return abreviatura; }
    public void setAbreviatura(String abreviatura) { this.abreviatura = abreviatura; }
    public double getCantidadSolicitada() { return cantidadSolicitada; }
    public void setCantidadSolicitada(double cantidadSolicitada) { this.cantidadSolicitada = cantidadSolicitada; }
}
