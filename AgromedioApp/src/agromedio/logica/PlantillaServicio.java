package agromedio.logica;

import agromedio.datos.PlantillaDAO;
import agromedio.datos.PlantillaDAOSqlite;
import agromedio.modelo.PlantillaOperativa;

import java.util.List;

/** Reglas del ciclo operativo: estados y detalle de la plantilla. */
public class PlantillaServicio {

    private final PlantillaDAO plantillaDAO;

    public PlantillaServicio() {
        this(new PlantillaDAOSqlite());
    }

    public PlantillaServicio(PlantillaDAO plantillaDAO) {
        this.plantillaDAO = plantillaDAO;
    }

    public int crear(int idCliente, String nombreMercado, String fechaProgramada) {
        if (idCliente <= 0) {
            throw new IllegalArgumentException("Debe seleccionar un cliente.");
        }
        if (nombreMercado == null || nombreMercado.trim().isEmpty()) {
            throw new IllegalArgumentException("Debe indicar el nombre del mercado.");
        }
        if (fechaProgramada == null || fechaProgramada.trim().isEmpty()) {
            throw new IllegalArgumentException("Debe indicar la fecha programada.");
        }
        PlantillaOperativa p = new PlantillaOperativa();
        p.setIdCliente(idCliente);
        p.setNombreMercado(nombreMercado.trim());
        p.setFechaProgramada(fechaProgramada.trim());
        p.setEstado(PlantillaOperativa.ESTADO_CARGADA);
        return plantillaDAO.guardar(p);
    }

    public void agregarProducto(int idPlantilla, int idProducto, double cantidad) {
        PlantillaOperativa p = plantillaDAO.obtener(idPlantilla);
        if (p == null) {
            throw new IllegalArgumentException("La plantilla no existe.");
        }
        if (!editable(p)) {
            throw new IllegalStateException("Solo se puede modificar una plantilla en estado CARGADA.");
        }
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad solicitada debe ser mayor que cero.");
        }
        List<agromedio.modelo.DetallePlantilla> existentes = plantillaDAO.listarDetalle(idPlantilla);
        for (agromedio.modelo.DetallePlantilla d : existentes) {
            if (d.getIdProducto() == idProducto) {
                throw new IllegalArgumentException("El producto ya esta en la plantilla. Edite o elimine el registro existente.");
            }
        }
        agromedio.modelo.DetallePlantilla det = new agromedio.modelo.DetallePlantilla();
        det.setIdPlantilla(idPlantilla);
        det.setIdProducto(idProducto);
        det.setCantidadSolicitada(cantidad);
        plantillaDAO.agregarDetalle(det);
    }

    public void eliminarProducto(int idDetallePlantilla) {
        agromedio.modelo.DetallePlantilla d = plantillaDAO.obtenerDetalle(idDetallePlantilla);
        if (d == null) {
            throw new IllegalArgumentException("El producto de la plantilla no existe.");
        }
        PlantillaOperativa p = plantillaDAO.obtener(d.getIdPlantilla());
        if (!editable(p)) {
            throw new IllegalStateException("Solo se puede modificar una plantilla en estado CARGADA.");
        }
        plantillaDAO.eliminarDetalle(idDetallePlantilla);
    }

    public List<agromedio.modelo.DetallePlantilla> detalle(int idPlantilla) {
        return plantillaDAO.listarDetalle(idPlantilla);
    }

    public PlantillaOperativa obtener(int idPlantilla) {
        return plantillaDAO.obtener(idPlantilla);
    }

    /**
     * RN-12: una plantilla con compras, recepciones, alistamientos o despachos
     * no se elimina. Solo se permite borrar una plantilla CARGADA sin historico,
     * junto con su detalle editable.
     */
    public void eliminar(int idPlantilla) {
        PlantillaOperativa p = plantillaDAO.obtener(idPlantilla);
        if (p == null) {
            throw new IllegalArgumentException("La plantilla no existe.");
        }
        if (plantillaDAO.tieneHistorico(idPlantilla)) {
            throw new IllegalStateException("No se puede eliminar la plantilla "
                    + p.getNombreMercado() + ": tiene compras, recepciones, alistamientos o despachos.");
        }
        if (!editable(p)) {
            throw new IllegalStateException("Solo se puede eliminar una plantilla en estado CARGADA.");
        }
        plantillaDAO.eliminar(idPlantilla);
    }

    /** Solo se puede editar la plantilla mientras no haya iniciado la compra. */
    public boolean editable(PlantillaOperativa p) {
        return p != null && PlantillaOperativa.ESTADO_CARGADA.equals(p.getEstado());
    }
}
