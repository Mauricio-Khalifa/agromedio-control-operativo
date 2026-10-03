package agromedio.logica;

import agromedio.datos.DespachoDAO;
import agromedio.datos.DespachoDAOSqlite;
import agromedio.datos.PlantillaDAO;
import agromedio.datos.PlantillaDAOSqlite;
import agromedio.modelo.DetalleDespacho;
import agromedio.modelo.DetallePlantilla;
import agromedio.modelo.Despacho;
import agromedio.modelo.PlantillaOperativa;

import java.time.LocalDate;
import java.util.List;

/** Modulo 4 - Despacho: verificacion paso a paso (RF09) y cierre al 100% (RF10). */
public class DespachoServicio {

    private final DespachoDAO despachoDAO;
    private final PlantillaDAO plantillaDAO;

    public DespachoServicio() {
        this(new DespachoDAOSqlite(), new PlantillaDAOSqlite());
    }

    public DespachoServicio(DespachoDAO despachoDAO, PlantillaDAO plantillaDAO) {
        this.despachoDAO = despachoDAO;
        this.plantillaDAO = plantillaDAO;
    }

    /** Abre (o retoma) la lista de chequeo del mercado. */
    public Despacho iniciar(int idPlantilla) {
        PlantillaOperativa plantilla = plantillaDAO.obtener(idPlantilla);
        if (plantilla == null) {
            throw new IllegalArgumentException("La plantilla no existe.");
        }
        Despacho existente = despachoDAO.obtenerPorPlantilla(idPlantilla);
        if (existente != null) {
            return existente;
        }
        if (!PlantillaOperativa.ESTADO_LISTA.equals(plantilla.getEstado())) {
            throw new IllegalStateException("La plantilla esta en estado " + plantilla.getEstado()
                    + ". Debe estar LISTA (alistamiento completo) para despachar.");
        }
        Despacho d = new Despacho();
        d.setIdPlantilla(idPlantilla);
        d.setFechaDespacho(LocalDate.now().toString());
        d.setEstado(Despacho.EN_VERIFICACION);
        despachoDAO.guardar(d);

        for (DetallePlantilla dp : plantillaDAO.listarDetalle(idPlantilla)) {
            DetalleDespacho dd = new DetalleDespacho();
            dd.setIdDespacho(d.getIdDespacho());
            dd.setIdProducto(dp.getIdProducto());
            dd.setCantidadDespachada(dp.getCantidadSolicitada());
            dd.setVerificado(false);
            despachoDAO.agregarDetalle(dd);
        }
        return d;
    }

    /** RF09: confirma un item de la bolsa/cesta del mercado. */
    public void marcarItem(DetalleDespacho detalle) {
        if (detalle.getCantidadDespachada() < 0) {
            throw new IllegalArgumentException("La cantidad despachada no puede ser negativa.");
        }
        despachoDAO.actualizarDetalle(detalle);
    }

    /**
     * RF10: cierra el despacho SOLO cuando el 100% de los items estan verificados.
     * @return lista de productos pendientes (vacia si se pudo cerrar).
     */
    public List<DetalleDespacho> cerrar(int idDespacho, int idPlantilla, String novedades) {
        List<DetalleDespacho> detalles = despachoDAO.listarDetalle(idDespacho);
        if (detalles.isEmpty()) {
            throw new IllegalStateException("El despacho no tiene productos.");
        }
        List<DetalleDespacho> pendientes = detalles.stream()
                .filter(d -> !d.isVerificado())
                .toList();
        if (!pendientes.isEmpty()) {
            return pendientes;
        }
        despachoDAO.actualizarEstado(idDespacho, Despacho.DESPACHADA, novedades);
        plantillaDAO.actualizarEstado(idPlantilla, PlantillaOperativa.ESTADO_DESPACHADA);
        return pendientes;
    }

    public Despacho actual(int idPlantilla) {
        return despachoDAO.obtenerPorPlantilla(idPlantilla);
    }

    public List<DetalleDespacho> detalle(int idDespacho) {
        return despachoDAO.listarDetalle(idDespacho);
    }

    /** Items verificados / total. */
    public int[] progreso(int idDespacho) {
        List<DetalleDespacho> detalles = despachoDAO.listarDetalle(idDespacho);
        int verificados = (int) detalles.stream().filter(DetalleDespacho::isVerificado).count();
        return new int[]{verificados, detalles.size()};
    }
}
