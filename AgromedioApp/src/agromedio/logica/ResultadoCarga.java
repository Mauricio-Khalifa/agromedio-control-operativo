package agromedio.logica;

import java.util.ArrayList;
import java.util.List;

/** Resultado de la carga de un archivo .xlsx (RF01 - RF02). */
public class ResultadoCarga {
    private int idCarga;
    private int lineasLeidas;
    private int plantillasCreadas;
    private int productosRegistrados;
    private int productosCreados;
    private boolean duplicada;
    private final List<String> errores = new ArrayList<>();

    public int getIdCarga() { return idCarga; }
    public void setIdCarga(int idCarga) { this.idCarga = idCarga; }
    public int getLineasLeidas() { return lineasLeidas; }
    public void setLineasLeidas(int lineasLeidas) { this.lineasLeidas = lineasLeidas; }
    public int getPlantillasCreadas() { return plantillasCreadas; }
    public void setPlantillasCreadas(int plantillasCreadas) { this.plantillasCreadas = plantillasCreadas; }
    public int getProductosRegistrados() { return productosRegistrados; }
    public void setProductosRegistrados(int productosRegistrados) { this.productosRegistrados = productosRegistrados; }
    public int getProductosCreados() { return productosCreados; }
    public void setProductosCreados(int productosCreados) { this.productosCreados = productosCreados; }
    public boolean isDuplicada() { return duplicada; }
    public void setDuplicada(boolean duplicada) { this.duplicada = duplicada; }
    public List<String> getErrores() { return errores; }

    public boolean hayErrores() { return !errores.isEmpty(); }

    @Override
    public String toString() {
        return "Carga #: " + idCarga
                + "\nLineas leidas: " + lineasLeidas
                + "\nPlantillas creadas: " + plantillasCreadas
                + "\nProductos registrados: " + productosRegistrados
                + "\nProductos nuevos en catalogo: " + productosCreados
                + "\nErrores: " + errores.size();
    }
}
