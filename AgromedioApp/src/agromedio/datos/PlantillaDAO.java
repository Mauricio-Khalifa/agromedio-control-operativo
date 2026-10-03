package agromedio.datos;

import agromedio.modelo.DetallePlantilla;
import agromedio.modelo.PlantillaOperativa;

import java.util.List;

public interface PlantillaDAO {
    List<PlantillaOperativa> listar();
    List<PlantillaOperativa> listarPorFecha(String fecha);
    List<PlantillaOperativa> listarPorEstado(String... estados);
    PlantillaOperativa obtener(int id);
    int guardar(PlantillaOperativa plantilla);
    void actualizarEstado(int idPlantilla, String estado);

    List<DetallePlantilla> listarDetalle(int idPlantilla);
    void agregarDetalle(DetallePlantilla detalle);
    void eliminarDetalle(int idDetallePlantilla);
}
