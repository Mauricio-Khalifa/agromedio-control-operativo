package agromedio.modelo;

/** Fila del consolidado de demanda diaria (RF03) - unidad de despacho. */
public class FilaConsolidado {
    private int idProducto;
    private String producto;
    private String unidadDespacho;
    private String unidadCompra;
    private double factorConversion;
    private double totalSolicitado;
    private double totalEnUnidadCompra;
    private int mercados;
    private double totalComprado;
    private double pendiente;

    public int getIdProducto() { return idProducto; }
    public void setIdProducto(int idProducto) { this.idProducto = idProducto; }
    public String getProducto() { return producto; }
    public void setProducto(String producto) { this.producto = producto; }
    public String getUnidadDespacho() { return unidadDespacho; }
    public void setUnidadDespacho(String unidadDespacho) { this.unidadDespacho = unidadDespacho; }
    public String getUnidadCompra() { return unidadCompra; }
    public void setUnidadCompra(String unidadCompra) { this.unidadCompra = unidadCompra; }
    public double getFactorConversion() { return factorConversion; }
    public void setFactorConversion(double factorConversion) { this.factorConversion = factorConversion; }
    public double getTotalSolicitado() { return totalSolicitado; }
    public void setTotalSolicitado(double totalSolicitado) { this.totalSolicitado = totalSolicitado; }
    public double getTotalEnUnidadCompra() { return totalEnUnidadCompra; }
    public void setTotalEnUnidadCompra(double totalEnUnidadCompra) { this.totalEnUnidadCompra = totalEnUnidadCompra; }
    public int getMercados() { return mercados; }
    public void setMercados(int mercados) { this.mercados = mercados; }
    public double getTotalComprado() { return totalComprado; }
    public void setTotalComprado(double totalComprado) { this.totalComprado = totalComprado; }
    public double getPendiente() { return pendiente; }
    public void setPendiente(double pendiente) { this.pendiente = pendiente; }
}
