package agromedio.modelo;

public class Compra {
    public static final String REGISTRADA = "REGISTRADA";
    public static final String RECIBIDA = "RECIBIDA";
    public static final String ANULADA = "ANULADA";

    private int idCompra;
    private int idPlantilla;
    private String fechaCompra;
    private String estado;
    private double costoTotal;
    private String observaciones;
    private int idProveedor;
    private String nombreProveedor;

    // Campos de presentacion
    private String nombreMercado;

    public Compra() {}

    public Compra(int idCompra, int idPlantilla, String fechaCompra, String estado) {
        this.idCompra = idCompra;
        this.idPlantilla = idPlantilla;
        this.fechaCompra = fechaCompra;
        this.estado = estado;
    }

    public int getIdCompra() { return idCompra; }
    public void setIdCompra(int idCompra) { this.idCompra = idCompra; }
    public int getIdPlantilla() { return idPlantilla; }
    public void setIdPlantilla(int idPlantilla) { this.idPlantilla = idPlantilla; }
    public String getFechaCompra() { return fechaCompra; }
    public void setFechaCompra(String fechaCompra) { this.fechaCompra = fechaCompra; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public double getCostoTotal() { return costoTotal; }
    public void setCostoTotal(double costoTotal) { this.costoTotal = costoTotal; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
    public int getIdProveedor() { return idProveedor; }
    public void setIdProveedor(int idProveedor) { this.idProveedor = idProveedor; }
    public String getNombreProveedor() { return nombreProveedor; }
    public void setNombreProveedor(String nombreProveedor) { this.nombreProveedor = nombreProveedor; }
    public String getNombreMercado() { return nombreMercado; }
    public void setNombreMercado(String nombreMercado) { this.nombreMercado = nombreMercado; }

    @Override
    public String toString() { return "Compra #" + idCompra + " - " + nombreMercado; }
}
