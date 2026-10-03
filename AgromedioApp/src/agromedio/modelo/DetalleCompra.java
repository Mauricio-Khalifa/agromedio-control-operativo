package agromedio.modelo;

public class DetalleCompra {
    private int idDetalleCompra;
    private int idCompra;
    private int idProducto;
    private int idProveedor;
    private double cantidadComprada;

    // Campos de presentacion
    private String nombreProducto;
    private String nombreProveedor;
    private String abreviatura;

    public DetalleCompra() {}

    public DetalleCompra(int idDetalleCompra, int idCompra, int idProducto, int idProveedor, double cantidadComprada) {
        this.idDetalleCompra = idDetalleCompra;
        this.idCompra = idCompra;
        this.idProducto = idProducto;
        this.idProveedor = idProveedor;
        this.cantidadComprada = cantidadComprada;
    }

    public int getIdDetalleCompra() { return idDetalleCompra; }
    public void setIdDetalleCompra(int idDetalleCompra) { this.idDetalleCompra = idDetalleCompra; }
    public int getIdCompra() { return idCompra; }
    public void setIdCompra(int idCompra) { this.idCompra = idCompra; }
    public int getIdProducto() { return idProducto; }
    public void setIdProducto(int idProducto) { this.idProducto = idProducto; }
    public int getIdProveedor() { return idProveedor; }
    public void setIdProveedor(int idProveedor) { this.idProveedor = idProveedor; }
    public double getCantidadComprada() { return cantidadComprada; }
    public void setCantidadComprada(double cantidadComprada) { this.cantidadComprada = cantidadComprada; }
    public String getNombreProducto() { return nombreProducto; }
    public void setNombreProducto(String nombreProducto) { this.nombreProducto = nombreProducto; }
    public String getNombreProveedor() { return nombreProveedor; }
    public void setNombreProveedor(String nombreProveedor) { this.nombreProveedor = nombreProveedor; }
    public String getAbreviatura() { return abreviatura; }
    public void setAbreviatura(String abreviatura) { this.abreviatura = abreviatura; }
}
