package agromedio.modelo;

/** Fila de la vista v_faltantes_recepcion (RF06). */
public class FilaFaltante {
    private int idRecepcion;
    private String fechaRecepcion;
    private int idCompra;
    private int idPlantilla;
    private String nombreMercado;
    private int idDetalleCompra;
    private String producto;
    private String proveedor;
    private String unidadCompra;
    private double cantidadComprada;
    private double cantidadRecibida;
    private double cantidadFaltante;

    public int getIdRecepcion() { return idRecepcion; }
    public void setIdRecepcion(int idRecepcion) { this.idRecepcion = idRecepcion; }
    public String getFechaRecepcion() { return fechaRecepcion; }
    public void setFechaRecepcion(String fechaRecepcion) { this.fechaRecepcion = fechaRecepcion; }
    public int getIdCompra() { return idCompra; }
    public void setIdCompra(int idCompra) { this.idCompra = idCompra; }
    public int getIdPlantilla() { return idPlantilla; }
    public void setIdPlantilla(int idPlantilla) { this.idPlantilla = idPlantilla; }
    public String getNombreMercado() { return nombreMercado; }
    public void setNombreMercado(String nombreMercado) { this.nombreMercado = nombreMercado; }
    public int getIdDetalleCompra() { return idDetalleCompra; }
    public void setIdDetalleCompra(int idDetalleCompra) { this.idDetalleCompra = idDetalleCompra; }
    public String getProducto() { return producto; }
    public void setProducto(String producto) { this.producto = producto; }
    public String getProveedor() { return proveedor; }
    public void setProveedor(String proveedor) { this.proveedor = proveedor; }
    public String getUnidadCompra() { return unidadCompra; }
    public void setUnidadCompra(String unidadCompra) { this.unidadCompra = unidadCompra; }
    public double getCantidadComprada() { return cantidadComprada; }
    public void setCantidadComprada(double cantidadComprada) { this.cantidadComprada = cantidadComprada; }
    public double getCantidadRecibida() { return cantidadRecibida; }
    public void setCantidadRecibida(double cantidadRecibida) { this.cantidadRecibida = cantidadRecibida; }
    public double getCantidadFaltante() { return cantidadFaltante; }
    public void setCantidadFaltante(double cantidadFaltante) { this.cantidadFaltante = cantidadFaltante; }
}
