package agromedio.modelo;

/** Fila del reporte de saldo integral de una plantilla (vista v_saldo_plantilla). */
public class FilaSaldo {
    private int idPlantilla;
    private String nombreMercado;
    private String fechaProgramada;
    private String estado;
    private int idProducto;
    private String producto;
    private String unidadDespacho;
    private double solicitado;
    private double alistado;
    private double comprado;
    private double recibido;
    private double despachado;
    private double porDespachar;

    public int getIdPlantilla() { return idPlantilla; }
    public void setIdPlantilla(int idPlantilla) { this.idPlantilla = idPlantilla; }
    public String getNombreMercado() { return nombreMercado; }
    public void setNombreMercado(String nombreMercado) { this.nombreMercado = nombreMercado; }
    public String getFechaProgramada() { return fechaProgramada; }
    public void setFechaProgramada(String fechaProgramada) { this.fechaProgramada = fechaProgramada; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public int getIdProducto() { return idProducto; }
    public void setIdProducto(int idProducto) { this.idProducto = idProducto; }
    public String getProducto() { return producto; }
    public void setProducto(String producto) { this.producto = producto; }
    public String getUnidadDespacho() { return unidadDespacho; }
    public void setUnidadDespacho(String unidadDespacho) { this.unidadDespacho = unidadDespacho; }
    public double getSolicitado() { return solicitado; }
    public void setSolicitado(double solicitado) { this.solicitado = solicitado; }
    public double getAlistado() { return alistado; }
    public void setAlistado(double alistado) { this.alistado = alistado; }
    public double getComprado() { return comprado; }
    public void setComprado(double comprado) { this.comprado = comprado; }
    public double getRecibido() { return recibido; }
    public void setRecibido(double recibido) { this.recibido = recibido; }
    public double getDespachado() { return despachado; }
    public void setDespachado(double despachado) { this.despachado = despachado; }
    public double getPorDespachar() { return porDespachar; }
    public void setPorDespachar(double porDespachar) { this.porDespachar = porDespachar; }
}
