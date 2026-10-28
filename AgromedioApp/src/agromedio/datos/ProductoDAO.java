package agromedio.datos;

import agromedio.modelo.Producto;

import java.util.List;

public interface ProductoDAO {
    List<Producto> listar();
    /** Busca por nombre de producto, categoría o unidad de medida. */
    List<Producto> buscar(String texto);
    Producto obtener(int id);
    Producto buscarPorNombre(String nombre);
    void guardar(Producto producto);
    void eliminar(int id);
}
