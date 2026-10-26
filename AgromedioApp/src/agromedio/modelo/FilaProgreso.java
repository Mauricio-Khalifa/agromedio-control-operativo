package agromedio.modelo;

/** Resumen operativo de una plantilla que aun no ha sido despachada. */
public class FilaProgreso {
    private int idPlantilla;
    private String nombreMercado;
    private String fechaCarga;
    private String fechaProgramada;
    private String estado;
    private double porcentajeCompra;
    private double porcentajeRecepcion;
    private double porcentajeAlistamiento;
    private double porcentajeDespacho;
    private String estadoDespacho;
    private boolean tieneFaltante;
    private boolean tieneNovedad;

    public int getIdPlantilla() { return idPlantilla; }
    public void setIdPlantilla(int idPlantilla) { this.idPlantilla = idPlantilla; }
    public String getNombreMercado() { return nombreMercado; }
    public void setNombreMercado(String nombreMercado) { this.nombreMercado = nombreMercado; }
    public String getFechaCarga() { return fechaCarga; }
    public void setFechaCarga(String fechaCarga) { this.fechaCarga = fechaCarga; }
    public String getFechaProgramada() { return fechaProgramada; }
    public void setFechaProgramada(String fechaProgramada) { this.fechaProgramada = fechaProgramada; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public double getPorcentajeCompra() { return porcentajeCompra; }
    public void setPorcentajeCompra(double porcentajeCompra) { this.porcentajeCompra = porcentajeCompra; }
    public double getPorcentajeRecepcion() { return porcentajeRecepcion; }
    public void setPorcentajeRecepcion(double porcentajeRecepcion) { this.porcentajeRecepcion = porcentajeRecepcion; }
    public double getPorcentajeAlistamiento() { return porcentajeAlistamiento; }
    public void setPorcentajeAlistamiento(double porcentajeAlistamiento) { this.porcentajeAlistamiento = porcentajeAlistamiento; }
    public double getPorcentajeDespacho() { return porcentajeDespacho; }
    public void setPorcentajeDespacho(double porcentajeDespacho) { this.porcentajeDespacho = porcentajeDespacho; }
    public String getEstadoDespacho() { return estadoDespacho; }
    public void setEstadoDespacho(String estadoDespacho) { this.estadoDespacho = estadoDespacho; }
    public boolean isTieneFaltante() { return tieneFaltante; }
    public void setTieneFaltante(boolean tieneFaltante) { this.tieneFaltante = tieneFaltante; }
    public boolean isTieneNovedad() { return tieneNovedad; }
    public void setTieneNovedad(boolean tieneNovedad) { this.tieneNovedad = tieneNovedad; }
}
