package agromedio.modelo;

public class DetalleDespacho {
    private int idDetalleDespacho;
    private int idDespacho;
    private int idProducto;
    private double cantidadDespachada;
    private boolean verificado;

    // Campos de presentacion
    private String nombreProducto;
    private String abreviatura;
    private double cantidadSolicitada;

    public DetalleDespacho() {}

    public DetalleDespacho(int idDetalleDespacho, int idDespacho, int idProducto,
                           double cantidadDespachada, boolean verificado) {
        this.idDetalleDespacho = idDetalleDespacho;
        this.idDespacho = idDespacho;
        this.idProducto = idProducto;
        this.cantidadDespachada = cantidadDespachada;
        this.verificado = verificado;
    }

    public int getIdDetalleDespacho() { return idDetalleDespacho; }
    public void setIdDetalleDespacho(int idDetalleDespacho) { this.idDetalleDespacho = idDetalleDespacho; }
    public int getIdDespacho() { return idDespacho; }
    public void setIdDespacho(int idDespacho) { this.idDespacho = idDespacho; }
    public int getIdProducto() { return idProducto; }
    public void setIdProducto(int idProducto) { this.idProducto = idProducto; }
    public double getCantidadDespachada() { return cantidadDespachada; }
    public void setCantidadDespachada(double cantidadDespacho) { this.cantidadDespachada = cantidadDespacho; }
    public boolean isVerificado() { return verificado; }
    public void setVerificado(boolean verificado) { this.verificado = verificado; }
    public String getNombreProducto() { return nombreProducto; }
    public void setNombreProducto(String nombreProducto) { this.nombreProducto = nombreProducto; }
    public String getAbreviatura() { return abreviatura; }
    public void setAbreviatura(String abreviatura) { this.abreviatura = abreviatura; }
    public double getCantidadSolicitada() { return cantidadSolicitada; }
    public void setCantidadSolicitada(double cantidadSolicitada) { this.cantidadSolicitada = cantidadSolicitada; }
}
