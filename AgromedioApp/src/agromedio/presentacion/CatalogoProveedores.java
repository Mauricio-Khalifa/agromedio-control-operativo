package agromedio.presentacion;

import agromedio.datos.ProveedorDAO;
import agromedio.datos.ProveedorDAOSqlite;
import agromedio.modelo.Proveedor;

import java.util.ArrayList;
import java.util.List;

public class CatalogoProveedores implements AdaptadorCatalogo {

    private final ProveedorDAO dao = new ProveedorDAOSqlite();
    private List<Proveedor> cache = new ArrayList<>();

    @Override
    public String titulo() { return "Proveedores"; }

    @Override
    public String[] columnasTabla() {
        return new String[]{"ID", "Proveedor", "Contacto", "Telefono"};
    }

    @Override
    public List<Object[]> filas() {
        cache = dao.listar();
        List<Object[]> filas = new ArrayList<>();
        for (Proveedor p : cache) {
            filas.add(new Object[]{p.getIdProveedor(), p.getNombre(), p.getContacto(), p.getTelefono()});
        }
        return filas;
    }

    @Override
    public int idFila(int fila) {
        return cache.get(fila).getIdProveedor();
    }

    @Override
    public String[] etiquetasCampos() {
        return new String[]{"Nombre del proveedor", "Contacto", "Telefono"};
    }

    @Override
    public String[] camposFila(int fila) {
        Proveedor p = cache.get(fila);
        return new String[]{p.getNombre(), p.getContacto(), p.getTelefono()};
    }

    @Override
    public void guardar(int id, String[] campos) throws Exception {
        Proveedor p = new Proveedor(id, campos[0], campos[1], campos[2]);
        if (p.getNombre() == null || p.getNombre().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del proveedor es obligatorio.");
        }
        dao.guardar(p);
    }

    @Override
    public void eliminar(int id) throws Exception {
        dao.eliminar(id);
    }
}
