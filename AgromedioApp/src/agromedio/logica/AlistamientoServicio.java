package agromedio.logica;

import agromedio.datos.AlistamientoDAO;
import agromedio.datos.AlistamientoDAOSqlite;
import agromedio.datos.PlantillaDAO;
import agromedio.datos.PlantillaDAOSqlite;
import agromedio.modelo.Alistamiento;
import agromedio.modelo.DetalleAlistamiento;
import agromedio.modelo.DetallePlantilla;
import agromedio.modelo.PlantillaOperativa;

import java.time.LocalDate;
import java.util.List;

/** Modulo 3 - Produccion / Alistamiento: fraccionamiento (RF07) y avance (RF08). */
public class AlistamientoServicio {

    private final AlistamientoDAO alistamientoDAO;
    private final PlantillaDAO plantillaDAO;

    public AlistamientoServicio() {
        this(new AlistamientoDAOSqlite(), new PlantillaDAOSqlite());
    }

    public AlistamientoServicio(AlistamientoDAO alistamientoDAO, PlantillaDAO plantillaDAO) {
        this.alistamientoDAO = alistamientoDAO;
        this.plantillaDAO = plantillaDAO;
    }

    /** Inicia (o retoma) el alistamiento de una plantilla. RF07. */
    public Alistamiento iniciar(int idPlantilla) {
        PlantillaOperativa plantilla = plantillaDAO.obtener(idPlantilla);
        if (plantilla == null) {
            throw new IllegalArgumentException("La plantilla no existe.");
        }
        List<String> estadosValidos = List.of(
                PlantillaOperativa.ESTADO_ALISTANDO,
                PlantillaOperativa.ESTADO_RECIBIENDO,
                PlantillaOperativa.ESTADO_LISTA);
        if (!estadosValidos.contains(plantilla.getEstado())) {
            throw new IllegalStateException("La plantilla esta en estado " + plantilla.getEstado()
                    + ". Debe completar la recepcion antes de alistamiento.");
        }

        Alistamiento existente = alistamientoDAO.obtenerPorPlantilla(idPlantilla);
        if (existente != null) {
            return existente;
        }
        Alistamiento a = new Alistamiento();
        a.setIdPlantilla(idPlantilla);
        a.setFechaAlistamiento(LocalDate.now().toString());
        a.setEstado(Alistamiento.EN_PROCESO);
        alistamientoDAO.guardar(a);

        for (DetallePlantilla dp : plantillaDAO.listarDetalle(idPlantilla)) {
            DetalleAlistamiento da = new DetalleAlistamiento();
            da.setIdAlistamiento(a.getIdAlistamiento());
            da.setIdProducto(dp.getIdProducto());
            da.setCantidadAlistada(0);
            da.setCompletado(false);
            alistamientoDAO.agregarDetalle(da);
        }
        plantillaDAO.actualizarEstado(idPlantilla, PlantillaOperativa.ESTADO_ALISTANDO);
        return a;
    }

    /** RF08: registra el avance de fraccionamiento por producto. */
    public void registrarAvance(int idAlistamiento, DetalleAlistamiento detalle) {
        if (detalle.getCantidadAlistada() < 0) {
            throw new IllegalArgumentException("La cantidad alistada no puede ser negativa.");
        }
        alistamientoDAO.actualizarDetalle(detalle);
    }

    /**
     * Marca el alistamiento como COMPLETO si todos los productos fueron
     * fraccionados y pasa la plantilla a estado LISTA.
     */
    public boolean completar(int idAlistamiento, int idPlantilla) {
        List<DetalleAlistamiento> detalles = alistamientoDAO.listarDetalle(idAlistamiento);
        if (detalles.isEmpty()) {
            throw new IllegalStateException("El alistamiento no tiene productos.");
        }
        for (DetalleAlistamiento d : detalles) {
            if (!d.isCompletado()) {
                return false;
            }
        }
        alistamientoDAO.actualizarEstado(idAlistamiento, Alistamiento.COMPLETO);
        plantillaDAO.actualizarEstado(idPlantilla, PlantillaOperativa.ESTADO_LISTA);
        return true;
    }

    public Alistamiento actual(int idPlantilla) {
        return alistamientoDAO.obtenerPorPlantilla(idPlantilla);
    }

    public List<DetalleAlistamiento> detalle(int idAlistamiento) {
        return alistamientoDAO.listarDetalle(idAlistamiento);
    }
}
