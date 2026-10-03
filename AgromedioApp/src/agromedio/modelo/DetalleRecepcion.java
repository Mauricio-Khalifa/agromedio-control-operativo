package agromedio.modelo;

public class DetalleRecepcion {
    private int idDetalleRecepcion;
    private int idRecepcion;
    private int idDetalleCompra;
    private double cantidadRecibida;
    private double cantidadFaltante;

    // Campos de presentacion
    private String nombreProducto;
    private String nombreProveedor;
    private double cantidadComprada;
    private String abreviatura;

    public DetalleRecepcion() {}

    public DetalleRecepcion(int idDetalleRecepcion, int idRecepcion, int idDetalleCompra,
                            double cantidadRecibida, double cantidadFaltante) {
        this.idDetalleRecepcion = idDetalleRecepcion;
        this.idRecepcion = idRecepcion;
        this.idDetalleCompra = idDetalleCompra;
        this.cantidadRecibida = cantidadRecibida;
        this.cantidadFaltante = cantidadFaltante;
    }

    public int getIdDetalleRecepcion() { return idDetalleRecepcion; }
    public void setIdDetalleRecepcion(int idDetalleRecepcion) { this.idDetalleRecepcion = idDetalleRecepcion; }
    public int getIdRecepcion() { return idRecepcion; }
    public void setIdRecepcion(int idRecepcion) { this.idRecepcion = idRecepcion; }
    public int getIdDetalleCompra() { return idDetalleCompra; }
    public void setIdDetalleCompra(int idDetalleCompra) { this.idDetalleCompra = idDetalleCompra; }
    public double getCantidadRecibida() { return cantidadRecibida; }
    public void setCantidadRecibida(double cantidadRecibida) { this.cantidadRecibida = cantidadRecibida; }
    public double getCantidadFaltante() { return cantidadFaltante; }
    public void setCantidadFaltante(double cantidadFaltante) { this.cantidadFaltante = cantidadFaltante; }
    public String getNombreProducto() { return nombreProducto; }
    public void setNombreProducto(String nombreProducto) { this.nombreProducto = nombreProducto; }
    public String getNombreProveedor() { return nombreProveedor; }
    public void setNombreProveedor(String nombreProveedor) { this.nombreProveedor = nombreProveedor; }
    public double getCantidadComprada() { return cantidadComprada; }
    public void setCantidadComprada(double cantidadComprada) { this.cantidadComprada = cantidadComprada; }
    public String getAbreviatura() { return abreviatura; }
    public void setAbreviatura(String abreviatura) { this.abreviatura = abreviatura; }
}
