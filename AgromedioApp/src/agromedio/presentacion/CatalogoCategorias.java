package agromedio.presentacion;

import agromedio.datos.CategoriaDAO;
import agromedio.datos.CategoriaDAOSqlite;
import agromedio.modelo.Categoria;

import java.util.ArrayList;
import java.util.List;

public class CatalogoCategorias implements AdaptadorCatalogo {

    private final CategoriaDAO dao = new CategoriaDAOSqlite();
    private List<Categoria> cache = new ArrayList<>();

    @Override
    public String titulo() { return "Categorías de producto"; }

    @Override
    public String[] columnasTabla() {
        return new String[]{"Nombre"};
    }

    @Override
    public List<Object[]> filas() {
        cache = dao.listar();
        List<Object[]> filas = new ArrayList<>();
        for (Categoria c : cache) {
            filas.add(new Object[]{c.getNombre()});
        }
        return filas;
    }

    @Override
    public int idFila(int fila) {
        return cache.get(fila).getIdCategoria();
    }

    @Override
    public String[] etiquetasCampos() {
        return new String[]{"Nombre de la categoría"};
    }

    @Override
    public String[] camposFila(int fila) {
        return new String[]{cache.get(fila).getNombre()};
    }

    @Override
    public void guardar(int id, String[] campos) throws Exception {
        Categoria c = new Categoria(id, campos[0]);
        if (c.getNombre() == null || c.getNombre().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de la categoría es obligatorio.");
        }
        dao.guardar(c);
    }

    @Override
    public void eliminar(int id) throws Exception {
        dao.eliminar(id);
    }
}
