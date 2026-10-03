package agromedio.logica;

import java.util.ArrayList;
import java.util.List;

/** Resultado de la carga de un archivo .xlsx (RF01 - RF02). */
public class ResultadoCarga {
    private int lineasLeidas;
    private int plantillasCreadas;
    private int productosRegistrados;
    private final List<String> errores = new ArrayList<>();

    public int getLineasLeidas() { return lineasLeidas; }
    public void setLineasLeidas(int lineasLeidas) { this.lineasLeidas = lineasLeidas; }
    public int getPlantillasCreadas() { return plantillasCreadas; }
    public void setPlantillasCreadas(int plantillasCreadas) { this.plantillasCreadas = plantillasCreadas; }
    public int getProductosRegistrados() { return productosRegistrados; }
    public void setProductosRegistrados(int productosRegistrados) { this.productosRegistrados = productosRegistrados; }
    public List<String> getErrores() { return errores; }

    public boolean hayErrores() { return !errores.isEmpty(); }

    @Override
    public String toString() {
        return "Lineas leidas: " + lineasLeidas
                + "\nPlantillas creadas: " + plantillasCreadas
                + "\nProductos registrados: " + productosRegistrados
                + "\nErrores: " + errores.size();
    }
}
