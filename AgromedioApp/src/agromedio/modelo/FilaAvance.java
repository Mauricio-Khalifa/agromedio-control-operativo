package agromedio.modelo;

/** Fila del avance de despacho por mercado (vista v_avance_despacho, RF10). */
public class FilaAvance {
    private int idDespacho;
    private int idPlantilla;
    private String nombreMercado;
    private String fechaDespacho;
    private String estado;
    private int items;
    private int verificados;
    private double porcentaje;

    public int getIdDespacho() { return idDespacho; }
    public void setIdDespacho(int idDespacho) { this.idDespacho = idDespacho; }
    public int getIdPlantilla() { return idPlantilla; }
    public void setIdPlantilla(int idPlantilla) { this.idPlantilla = idPlantilla; }
    public String getNombreMercado() { return nombreMercado; }
    public void setNombreMercado(String nombreMercado) { this.nombreMercado = nombreMercado; }
    public String getFechaDespacho() { return fechaDespacho; }
    public void setFechaDespacho(String fechaDespacho) { this.fechaDespacho = fechaDespacho; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public int getItems() { return items; }
    public void setItems(int items) { this.items = items; }
    public int getVerificados() { return verificados; }
    public void setVerificados(int verificados) { this.verificados = verificados; }
    public double getPorcentaje() { return porcentaje; }
    public void setPorcentaje(double porcentaje) { this.porcentaje = porcentaje; }
}
