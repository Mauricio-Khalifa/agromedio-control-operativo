package agromedio.datos;

import agromedio.modelo.Cliente;

import java.util.List;

public interface ClienteDAO {
    List<Cliente> listar();
    Cliente obtener(int id);
    void guardar(Cliente cliente);
    void eliminar(int id);
}
