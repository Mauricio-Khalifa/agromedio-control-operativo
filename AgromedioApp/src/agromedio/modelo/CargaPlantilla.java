package agromedio.modelo;

/** Registro de una carga de archivo Excel (G05 / RNF02). */
public class CargaPlantilla {

    private int idCarga;
    private String fechaCarga;
    private String nombreArchivo;
    private String hashArchivo;
    private String usuario;
    private int plantillasCreadas;
    private int productosRegistrados;

    public CargaPlantilla() {}

    public CargaPlantilla(int idCarga, String fechaCarga, String nombreArchivo,
                          String hashArchivo, String usuario,
                          int plantillasCreadas, int productosRegistrados) {
        this.idCarga = idCarga;
        this.fechaCarga = fechaCarga;
        this.nombreArchivo = nombreArchivo;
        this.hashArchivo = hashArchivo;
        this.usuario = usuario;
        this.plantillasCreadas = plantillasCreadas;
        this.productosRegistrados = productosRegistrados;
    }

    public int getIdCarga() { return idCarga; }
    public void setIdCarga(int idCarga) { this.idCarga = idCarga; }
    public String getFechaCarga() { return fechaCarga; }
    public void setFechaCarga(String fechaCarga) { this.fechaCarga = fechaCarga; }
    public String getNombreArchivo() { return nombreArchivo; }
    public void setNombreArchivo(String nombreArchivo) { this.nombreArchivo = nombreArchivo; }
    public String getHashArchivo() { return hashArchivo; }
    public void setHashArchivo(String hashArchivo) { this.hashArchivo = hashArchivo; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public int getPlantillasCreadas() { return plantillasCreadas; }
    public void setPlantillasCreadas(int plantillasCreadas) { this.plantillasCreadas = plantillasCreadas; }
    public int getProductosRegistrados() { return productosRegistrados; }
    public void setProductosRegistrados(int productosRegistrados) { this.productosRegistrados = productosRegistrados; }

    @Override
    public String toString() {
        return "Carga #" + idCarga + " - " + nombreArchivo + " (" + fechaCarga + ")";
    }
}
