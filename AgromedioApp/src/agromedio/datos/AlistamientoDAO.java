package agromedio.datos;

import agromedio.modelo.Alistamiento;
import agromedio.modelo.DetalleAlistamiento;

import java.util.List;

public interface AlistamientoDAO {
    Alistamiento obtenerPorPlantilla(int idPlantilla);
    int guardar(Alistamiento alistamiento);
    void actualizarEstado(int idAlistamiento, String estado);

    List<DetalleAlistamiento> listarDetalle(int idAlistamiento);
    void agregarDetalle(DetalleAlistamiento detalle);
    void actualizarDetalle(DetalleAlistamiento detalle);
}
