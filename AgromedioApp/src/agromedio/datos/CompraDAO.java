package agromedio.datos;

import agromedio.modelo.Compra;
import agromedio.modelo.DetalleCompra;

import java.util.List;

public interface CompraDAO {
    List<Compra> listar();
    List<Compra> listarPorPlantilla(int idPlantilla);
    List<Compra> listarPorEstado(String estado);
    Compra obtener(int id);
    int guardar(Compra compra);
    void actualizarEstado(int idCompra, String estado);

    List<DetalleCompra> listarDetalle(int idCompra);
    List<DetalleCompra> listarDetallePorPlantilla(int idPlantilla);
    int agregarDetalle(DetalleCompra detalle);
    void eliminarDetalle(int idDetalleCompra);
}
