package agromedio.datos;

import agromedio.modelo.Producto;

import java.util.List;

public interface ProductoDAO {
    List<Producto> listar();
    Producto obtener(int id);
    Producto buscarPorNombre(String nombre);
    void guardar(Producto producto);
    void eliminar(int id);
}
