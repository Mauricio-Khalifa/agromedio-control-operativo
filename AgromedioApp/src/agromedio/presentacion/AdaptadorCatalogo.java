package agromedio.presentacion;

import java.util.List;

/**
 * Contrato entre la ventana generica de catalogos y cada catalogo concreto.
 * Ejemplo de polimorfismo (POO): una sola ventana atiende clientes,
 * categorias, unidades de medida y proveedores.
 */
public interface AdaptadorCatalogo {

    String titulo();

    String[] columnasTabla();

    /** Valores visibles de cada fila (sin el id interno). */
    List<Object[]> filas();

    /** Id del registro asociado a una fila de la tabla. */
    int idFila(int fila);

    /** Etiquetas de los campos del formulario. */
    String[] etiquetasCampos();

    /** Valores del formulario para editar la fila (null si es registro nuevo). */
    String[] camposFila(int fila);

    /** Guarda el registro: id == 0 crea, si no actualiza. */
    void guardar(int id, String[] campos) throws Exception;

    void eliminar(int id) throws Exception;
}
