package agromedio.datos;

import agromedio.modelo.Categoria;

import java.util.List;

public interface CategoriaDAO {
    List<Categoria> listar();
    void guardar(Categoria categoria);
    void eliminar(int id);
}
