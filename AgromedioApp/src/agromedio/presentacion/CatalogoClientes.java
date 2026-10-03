package agromedio.presentacion;

import agromedio.datos.ClienteDAO;
import agromedio.datos.ClienteDAOSqlite;
import agromedio.modelo.Cliente;

import java.util.ArrayList;
import java.util.List;

public class CatalogoClientes implements AdaptadorCatalogo {

    private final ClienteDAO dao = new ClienteDAOSqlite();
    private List<Cliente> cache = new ArrayList<>();

    @Override
    public String titulo() { return "Clientes"; }

    @Override
    public String[] columnasTabla() {
        return new String[]{"ID", "Entidad", "Contacto", "Telefono"};
    }

    @Override
    public List<Object[]> filas() {
        cache = dao.listar();
        List<Object[]> filas = new ArrayList<>();
        for (Cliente c : cache) {
            filas.add(new Object[]{c.getIdCliente(), c.getNombreEntidad(), c.getContacto(), c.getTelefono()});
        }
        return filas;
    }

    @Override
    public int idFila(int fila) {
        return cache.get(fila).getIdCliente();
    }

    @Override
    public String[] etiquetasCampos() {
        return new String[]{"Nombre de la entidad", "Contacto", "Telefono"};
    }

    @Override
    public String[] camposFila(int fila) {
        Cliente c = cache.get(fila);
        return new String[]{c.getNombreEntidad(), c.getContacto(), c.getTelefono()};
    }

    @Override
    public void guardar(int id, String[] campos) throws Exception {
        Cliente c = new Cliente(id, campos[0], campos[1], campos[2]);
        if (c.getNombreEntidad() == null || c.getNombreEntidad().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de la entidad es obligatorio.");
        }
        dao.guardar(c);
    }

    @Override
    public void eliminar(int id) throws Exception {
        dao.eliminar(id);
    }
}
