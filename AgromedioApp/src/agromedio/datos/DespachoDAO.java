package agromedio.datos;

import agromedio.modelo.DetalleDespacho;
import agromedio.modelo.Despacho;

import java.util.List;

public interface DespachoDAO {
    Despacho obtenerPorPlantilla(int idPlantilla);
    int guardar(Despacho despacho);
    void actualizarEstado(int idDespacho, String estado, String novedades);

    List<DetalleDespacho> listarDetalle(int idDespacho);
    void agregarDetalle(DetalleDespacho detalle);
    void actualizarDetalle(DetalleDespacho detalle);
}
