package agromedio.datos;

import agromedio.modelo.Proveedor;

import java.util.List;

public interface ProveedorDAO {
    List<Proveedor> listar();
    Proveedor obtener(int id);
    void guardar(Proveedor proveedor);
    void eliminar(int id);
}
