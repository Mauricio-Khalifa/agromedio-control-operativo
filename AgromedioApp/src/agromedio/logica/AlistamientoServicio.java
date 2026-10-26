package agromedio.logica;

import agromedio.datos.AlistamientoDAO;
import agromedio.datos.AlistamientoDAOSqlite;
import agromedio.datos.ConexionBD;
import agromedio.datos.PlantillaDAO;
import agromedio.datos.PlantillaDAOSqlite;
import agromedio.modelo.Alistamiento;
import agromedio.modelo.DetalleAlistamiento;
import agromedio.modelo.DetallePlantilla;
import agromedio.modelo.PlantillaOperativa;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Modulo 3 - Produccion / Alistamiento: fraccionamiento (RF07) y avance (RF08). */
public class AlistamientoServicio {

    private static final double EPS = 0.0001;

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
                PlantillaOperativa.ESTADO_LISTA);
        if (!estadosValidos.contains(plantilla.getEstado())) {
            throw new IllegalStateException("La plantilla esta en estado " + plantilla.getEstado()
                    + ". Debe cerrar la recepcion antes de alistamiento.");
        }

        Alistamiento existente = alistamientoDAO.obtenerPorPlantilla(idPlantilla);
        if (existente != null) {
            return existente;
        }
        Alistamiento a = new Alistamiento();
        a.setIdPlantilla(idPlantilla);
        a.setFechaAlistamiento(LocalDate.now().toString());
        a.setEstado(Alistamiento.EN_PROCESO);
        return ConexionBD.enTransaccion(() -> {
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
        });
    }

    /**
     * RF08 / RN-07 / RN-08: registra el avance de fraccionamiento por producto.
     * No permite alistar mas de lo recibido y no permite completar un producto
     * sin recepcion registrada (recibido acumulado = 0).
     */
    public void registrarAvance(int idAlistamiento, DetalleAlistamiento detalle) {
        if (detalle == null) {
            throw new IllegalArgumentException("Seleccione el producto a actualizar.");
        }
        // H-2: un alistamiento completado es inmutable.
        Alistamiento encabezado = alistamientoDAO.obtener(idAlistamiento);
        if (encabezado != null && Alistamiento.COMPLETO.equals(encabezado.getEstado())) {
            throw new IllegalStateException("El alistamiento ya esta completo; no admite cambios.");
        }
        if (detalle.getCantidadAlistada() < 0) {
            throw new IllegalArgumentException("La cantidad alistada no puede ser negativa.");
        }
        DetalleAlistamiento oficial = buscarDetalle(idAlistamiento, detalle.getIdDetalleAlistamiento());
        if (oficial == null) {
            throw new IllegalArgumentException("El producto no pertenece al alistamiento indicado.");
        }

        double solicitado = oficial.getCantidadSolicitada();
        double recibido = oficial.getCantidadRecibida();
        double disponible = Math.min(solicitado, recibido);

        if (detalle.getCantidadAlistada() > disponible + EPS) {
            throw new IllegalArgumentException("La cantidad alistada de " + oficial.getNombreProducto()
                    + " supera lo disponible (" + fmt(disponible) + " " + oficial.getAbreviatura() + ").");
        }
        if (detalle.isCompletado()) {
            if (recibido <= EPS) {
                throw new IllegalStateException("No se puede completar " + oficial.getNombreProducto()
                        + ": no tiene recepcion registrada.");
            }
            if (detalle.getCantidadAlistada() <= EPS) {
                throw new IllegalArgumentException("Para completar " + oficial.getNombreProducto()
                        + " registre una cantidad mayor que cero.");
            }
            if (detalle.getCantidadAlistada() + EPS < disponible) {
                throw new IllegalArgumentException("Para completar " + oficial.getNombreProducto()
                        + " aliste al menos " + fmt(disponible) + " " + oficial.getAbreviatura() + ".");
            }
        }

        detalle.setCantidadSolicitada(solicitado);
        detalle.setCantidadRecibida(recibido);
        alistamientoDAO.actualizarDetalle(detalle);
    }

    /**
     * Marca el alistamiento como COMPLETO si todos los productos fueron
     * fraccionados, tienen recepcion y no superan lo disponible. Luego pasa
     * la plantilla a estado LISTA.
     */
    public boolean completar(int idAlistamiento, int idPlantilla) {
        // C-1: no se puede completar el alistamiento si la plantilla ya avanzo
        // a un estado terminal o no esta en produccion. Evita revertir una
        // plantilla DESPACHADA a LISTA (y habilitar un re-despacho).
        PlantillaOperativa plantilla = plantillaDAO.obtener(idPlantilla);
        if (plantilla == null) {
            throw new IllegalArgumentException("La plantilla no existe.");
        }
        if (!PlantillaOperativa.ESTADO_ALISTANDO.equals(plantilla.getEstado())
                && !PlantillaOperativa.ESTADO_LISTA.equals(plantilla.getEstado())) {
            throw new IllegalStateException("No se puede completar el alistamiento: la plantilla esta en estado "
                    + plantilla.getEstado() + ".");
        }

        List<DetalleAlistamiento> detalles = alistamientoDAO.listarDetalle(idAlistamiento);
        if (detalles.isEmpty()) {
            throw new IllegalStateException("El alistamiento no tiene productos.");
        }

        List<String> sinRecepcion = new ArrayList<>();
        for (DetalleAlistamiento d : detalles) {
            if (d.getCantidadRecibida() <= EPS) {
                sinRecepcion.add(d.getNombreProducto());
            }
        }
        if (!sinRecepcion.isEmpty()) {
            throw new IllegalStateException("No se puede completar el alistamiento: sin recepcion registrada en "
                    + String.join(", ", sinRecepcion) + ".");
        }

        for (DetalleAlistamiento d : detalles) {
            if (!d.isCompletado()) {
                return false;
            }
            double disponible = Math.min(d.getCantidadSolicitada(), d.getCantidadRecibida());
            if (d.getCantidadAlistada() <= EPS) {
                throw new IllegalArgumentException("El producto " + d.getNombreProducto()
                        + " esta completado pero no tiene cantidad alistada.");
            }
            if (d.getCantidadAlistada() + EPS < disponible) {
                throw new IllegalArgumentException("El producto " + d.getNombreProducto()
                        + " esta marcado completo, pero solo se alistaron " + fmt(d.getCantidadAlistada())
                        + " de " + fmt(disponible) + " " + d.getAbreviatura() + ".");
            }
            if (d.getCantidadAlistada() > disponible + EPS) {
                throw new IllegalArgumentException("El producto " + d.getNombreProducto()
                        + " tiene alistado mas de lo disponible.");
            }
        }
        ConexionBD.enTransaccion(() -> {
            alistamientoDAO.actualizarEstado(idAlistamiento, Alistamiento.COMPLETO);
            plantillaDAO.actualizarEstado(idPlantilla, PlantillaOperativa.ESTADO_LISTA);
        });
        return true;
    }

    private DetalleAlistamiento buscarDetalle(int idAlistamiento, int idDetalleAlistamiento) {
        for (DetalleAlistamiento d : alistamientoDAO.listarDetalle(idAlistamiento)) {
            if (d.getIdDetalleAlistamiento() == idDetalleAlistamiento) {
                return d;
            }
        }
        return null;
    }

    private static String fmt(double valor) {
        return String.format(java.util.Locale.US, "%.2f", valor);
    }

    public Alistamiento actual(int idPlantilla) {
        return alistamientoDAO.obtenerPorPlantilla(idPlantilla);
    }

    public List<DetalleAlistamiento> detalle(int idAlistamiento) {
        return alistamientoDAO.listarDetalle(idAlistamiento);
    }
}
