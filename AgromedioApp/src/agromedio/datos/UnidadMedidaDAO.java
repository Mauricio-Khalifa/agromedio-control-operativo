package agromedio.datos;

import agromedio.modelo.UnidadMedida;

import java.util.List;

public interface UnidadMedidaDAO {
    List<UnidadMedida> listar();
    void guardar(UnidadMedida unidad);
    void eliminar(int id);
}
