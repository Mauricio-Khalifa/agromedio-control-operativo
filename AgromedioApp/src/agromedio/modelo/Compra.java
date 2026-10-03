package agromedio.modelo;

public class Compra {
    public static final String REGISTRADA = "REGISTRADA";
    public static final String RECIBIDA = "RECIBIDA";
    public static final String ANULADA = "ANULADA";

    private int idCompra;
    private int idPlantilla;
    private String fechaCompra;
    private String estado;

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
    public String getNombreMercado() { return nombreMercado; }
    public void setNombreMercado(String nombreMercado) { this.nombreMercado = nombreMercado; }

    @Override
    public String toString() { return "Compra #" + idCompra + " - " + nombreMercado; }
}
