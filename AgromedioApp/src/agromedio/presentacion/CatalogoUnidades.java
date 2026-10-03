package agromedio.presentacion;

import agromedio.datos.UnidadMedidaDAO;
import agromedio.datos.UnidadMedidaDAOSqlite;
import agromedio.modelo.UnidadMedida;

import java.util.ArrayList;
import java.util.List;

public class CatalogoUnidades implements AdaptadorCatalogo {

    private final UnidadMedidaDAO dao = new UnidadMedidaDAOSqlite();
    private List<UnidadMedida> cache = new ArrayList<>();

    @Override
    public String titulo() { return "Unidades de medida"; }

    @Override
    public String[] columnasTabla() {
        return new String[]{"ID", "Nombre", "Abreviatura"};
    }

    @Override
    public List<Object[]> filas() {
        cache = dao.listar();
        List<Object[]> filas = new ArrayList<>();
        for (UnidadMedida u : cache) {
            filas.add(new Object[]{u.getIdUnidad(), u.getNombre(), u.getAbreviatura()});
        }
        return filas;
    }

    @Override
    public int idFila(int fila) {
        return cache.get(fila).getIdUnidad();
    }

    @Override
    public String[] etiquetasCampos() {
        return new String[]{"Nombre (ej. Litro)", "Abreviatura (ej. L)"};
    }

    @Override
    public String[] camposFila(int fila) {
        UnidadMedida u = cache.get(fila);
        return new String[]{u.getNombre(), u.getAbreviatura()};
    }

    @Override
    public void guardar(int id, String[] campos) throws Exception {
        UnidadMedida u = new UnidadMedida(id, campos[0], campos[1]);
        if (u.getNombre() == null || u.getNombre().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de la unidad es obligatorio.");
        }
        if (u.getAbreviatura() == null || u.getAbreviatura().trim().isEmpty()) {
            throw new IllegalArgumentException("La abreviatura es obligatoria.");
        }
        dao.guardar(u);
    }

    @Override
    public void eliminar(int id) throws Exception {
        dao.eliminar(id);
    }
}
