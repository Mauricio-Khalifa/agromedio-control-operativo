package agromedio.modelo;

public class Recepcion {
    private int idRecepcion;
    private int idCompra;
    private String fechaRecepcion;
    private String observaciones;

    public Recepcion() {}

    public Recepcion(int idRecepcion, int idCompra, String fechaRecepcion, String observaciones) {
        this.idRecepcion = idRecepcion;
        this.idCompra = idCompra;
        this.fechaRecepcion = fechaRecepcion;
        this.observaciones = observaciones;
    }

    public int getIdRecepcion() { return idRecepcion; }
    public void setIdRecepcion(int idRecepcion) { this.idRecepcion = idRecepcion; }
    public int getIdCompra() { return idCompra; }
    public void setIdCompra(int idCompra) { this.idCompra = idCompra; }
    public String getFechaRecepcion() { return fechaRecepcion; }
    public void setFechaRecepcion(String fechaRecepcion) { this.fechaRecepcion = fechaRecepcion; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
}
