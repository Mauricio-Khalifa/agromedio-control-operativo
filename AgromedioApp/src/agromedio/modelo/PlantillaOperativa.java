package agromedio.modelo;

public class PlantillaOperativa {
    public static final String ESTADO_CARGADA = "CARGADA";
    public static final String ESTADO_EN_COMPRA = "EN_COMPRA";
    public static final String ESTADO_RECIBIENDO = "RECIBIENDO";
    public static final String ESTADO_ALISTANDO = "ALISTANDO";
    public static final String ESTADO_LISTA = "LISTA";
    public static final String ESTADO_DESPACHADA = "DESPACHADA";

    private int idPlantilla;
    private int idCliente;
    private String nombreMercado;
    private String fechaProgramada;
    private String estado;
    private String fechaCarga;

    // Campo de presentacion
    private String nombreCliente;

    public PlantillaOperativa() {}

    public PlantillaOperativa(int idPlantilla, int idCliente, String nombreMercado,
                              String fechaProgramada, String estado, String fechaCarga) {
        this.idPlantilla = idPlantilla;
        this.idCliente = idCliente;
        this.nombreMercado = nombreMercado;
        this.fechaProgramada = fechaProgramada;
        this.estado = estado;
        this.fechaCarga = fechaCarga;
    }

    public int getIdPlantilla() { return idPlantilla; }
    public void setIdPlantilla(int idPlantilla) { this.idPlantilla = idPlantilla; }
    public int getIdCliente() { return idCliente; }
    public void setIdCliente(int idCliente) { this.idCliente = idCliente; }
    public String getNombreMercado() { return nombreMercado; }
    public void setNombreMercado(String nombreMercado) { this.nombreMercado = nombreMercado; }
    public String getFechaProgramada() { return fechaProgramada; }
    public void setFechaProgramada(String fechaProgramada) { this.fechaProgramada = fechaProgramada; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getFechaCarga() { return fechaCarga; }
    public void setFechaCarga(String fechaCarga) { this.fechaCarga = fechaCarga; }
    public String getNombreCliente() { return nombreCliente; }
    public void setNombreCliente(String nombreCliente) { this.nombreCliente = nombreCliente; }

    @Override
    public String toString() { return "#" + idPlantilla + " " + nombreMercado; }
}
