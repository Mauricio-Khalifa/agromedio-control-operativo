package agromedio.datos;

import agromedio.modelo.DetalleRecepcion;
import agromedio.modelo.Recepcion;

import java.util.List;

public interface RecepcionDAO {
    List<Recepcion> listarPorCompra(int idCompra);
    int guardar(Recepcion recepcion);
    void agregarDetalle(DetalleRecepcion detalle);
    List<DetalleRecepcion> listarDetalle(int idRecepcion);
}
