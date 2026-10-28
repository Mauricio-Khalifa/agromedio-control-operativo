package agromedio.modelo;

/** Cantidad de una línea de compra asignada a una plantilla/mercado. */
public class DistribucionCompra {
    private int idDistribucion;
    private int idDetalleCompra;
    private int idPlantilla;
    private int idProducto;
    private double cantidadAsignada;
    private double costoAsignado;
    private String nombreMercado;
    private String nombreProducto;

    public int getIdDistribucion() { return idDistribucion; }
    public void setIdDistribucion(int idDistribucion) { this.idDistribucion = idDistribucion; }
    public int getIdDetalleCompra() { return idDetalleCompra; }
    public void setIdDetalleCompra(int idDetalleCompra) { this.idDetalleCompra = idDetalleCompra; }
    public int getIdPlantilla() { return idPlantilla; }
    public void setIdPlantilla(int idPlantilla) { this.idPlantilla = idPlantilla; }
    public int getIdProducto() { return idProducto; }
    public void setIdProducto(int idProducto) { this.idProducto = idProducto; }
    public double getCantidadAsignada() { return cantidadAsignada; }
    public void setCantidadAsignada(double cantidadAsignada) { this.cantidadAsignada = cantidadAsignada; }
    public double getCostoAsignado() { return costoAsignado; }
    public void setCostoAsignado(double costoAsignado) { this.costoAsignado = costoAsignado; }
    public String getNombreMercado() { return nombreMercado; }
    public void setNombreMercado(String nombreMercado) { this.nombreMercado = nombreMercado; }
    public String getNombreProducto() { return nombreProducto; }
    public void setNombreProducto(String nombreProducto) { this.nombreProducto = nombreProducto; }
}
