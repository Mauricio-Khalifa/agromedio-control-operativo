package agromedio.logica;

import agromedio.datos.ConexionBD;
import agromedio.datos.DespachoDAO;
import agromedio.datos.DespachoDAOSqlite;
import agromedio.datos.PlantillaDAO;
import agromedio.datos.PlantillaDAOSqlite;
import agromedio.modelo.DetalleDespacho;
import agromedio.modelo.DetallePlantilla;
import agromedio.modelo.Despacho;
import agromedio.modelo.PlantillaOperativa;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Modulo 4 - Despacho: verificacion paso a paso (RF09) y cierre real (RF10 / RN-09 / RN-10). */
public class DespachoServicio {

    private static final double EPS = 0.0001;

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

        // C-3: creacion del despacho y su lista de chequeo en una sola transaccion.
        ConexionBD.enTransaccion(() -> {
            despachoDAO.guardar(d);
            for (DetallePlantilla dp : plantillaDAO.listarDetalle(idPlantilla)) {
                DetalleDespacho dd = new DetalleDespacho();
                dd.setIdDespacho(d.getIdDespacho());
                dd.setIdProducto(dp.getIdProducto());
                dd.setCantidadDespachada(dp.getCantidadSolicitada());
                dd.setVerificado(false);
                dd.setFaltanteAceptado(false);
                despachoDAO.agregarDetalle(dd);
            }
            // La cantidad inicial es lo realmente alistado; si hay faltante,
            // el operador debe aceptarlo explicitamente antes de cerrar.
            for (DetalleDespacho dd : despachoDAO.listarDetalle(d.getIdDespacho())) {
                double disponible = disponible(dd);
                if (disponible + EPS < dd.getCantidadDespachada()) {
                    dd.setCantidadDespachada(disponible);
                    despachoDAO.actualizarDetalle(dd);
                }
            }
        });
        return d;
    }

    /**
     * RF09 / D01 / D02: confirma un item de la bolsa/cesta del mercado.
     * La cantidad despachada debe estar entre 0 y lo solicitado. Para verificar
     * se exige una cantidad mayor que cero; un faltante aceptado tambien exige
     * una cantidad parcial mayor que cero.
     */
    public void marcarItem(DetalleDespacho detalle) {
        if (detalle == null) {
            throw new IllegalArgumentException("Seleccione el producto a actualizar.");
        }
        if (!Double.isFinite(detalle.getCantidadDespachada())
                || detalle.getCantidadDespachada() < 0) {
            throw new IllegalArgumentException("La cantidad despachada no puede ser negativa.");
        }
        DetalleDespacho oficial = buscarDetalle(detalle.getIdDespacho(), detalle.getIdDetalleDespacho());
        if (oficial == null) {
            throw new IllegalArgumentException("El producto no pertenece al despacho indicado.");
        }
        // H-2: un despacho cerrado es inmutable.
        Despacho encabezado = despachoDAO.obtener(detalle.getIdDespacho());
        if (encabezado != null && Despacho.DESPACHADA.equals(encabezado.getEstado())) {
            throw new IllegalStateException("El despacho ya esta cerrado; no admite cambios.");
        }
        double solicitado = oficial.getCantidadSolicitada();
        double disponible = disponible(oficial);
        if (detalle.getCantidadDespachada() > disponible + EPS) {
            throw new IllegalArgumentException("La cantidad despachada de " + oficial.getNombreProducto()
                    + " supera lo disponible (" + fmt(disponible) + " " + oficial.getAbreviatura() + ").");
        }
        if (detalle.getCantidadDespachada() <= EPS) {
            if (detalle.isVerificado()) {
                throw new IllegalArgumentException("Para verificar " + oficial.getNombreProducto()
                        + " registre una cantidad mayor que cero.");
            }
            if (detalle.isFaltanteAceptado()) {
                throw new IllegalArgumentException("No se puede aceptar un faltante de "
                        + oficial.getNombreProducto() + " con cantidad despachada 0.");
            }
            detalle.setFaltanteAceptado(false);
        } else if (detalle.getCantidadDespachada() + EPS >= solicitado) {
            detalle.setFaltanteAceptado(false);
        }
        detalle.setCantidadSolicitada(solicitado);
        despachoDAO.actualizarDetalle(detalle);
    }

    /**
     * RF10 / RN-09 / RN-10: cierra el despacho SOLO cuando todos los items
     * estan verificados, cada cantidad es mayor que cero, no supera lo requerido
     * y cualquier faltante parcial esta aceptado. Si hay faltante aceptado, la
     * novedad es obligatoria. Sin faltante, la novedad es opcional.
     *
     * @return lista de productos pendientes por verificar (vacia si se pudo cerrar).
     */
    public List<DetalleDespacho> cerrar(int idDespacho, int idPlantilla, String novedades) {
        // H-2/H-6: solo se cierra una vez; evita sobrescribir novedades.
        Despacho encabezado = despachoDAO.obtener(idDespacho);
        if (encabezado == null) {
            throw new IllegalArgumentException("El despacho no existe.");
        }
        if (!Despacho.EN_VERIFICACION.equals(encabezado.getEstado())) {
            throw new IllegalStateException("El despacho ya esta cerrado; no admite un nuevo cierre.");
        }
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

        List<String> errores = new ArrayList<>();
        boolean conFaltanteAceptado = false;
        for (DetalleDespacho d : detalles) {
            double solicitado = d.getCantidadSolicitada();
            double disponible = disponible(d);
            double despachado = d.getCantidadDespachada();
            if (despachado <= EPS) {
                errores.add(d.getNombreProducto() + ": cantidad despachada 0.");
                continue;
            }
            if (despachado > disponible + EPS) {
                errores.add(d.getNombreProducto() + ": cantidad despachada supera lo disponible ("
                        + fmt(disponible) + ").");
                continue;
            }
            if (despachado + EPS < solicitado) {
                if (!d.isFaltanteAceptado()) {
                    errores.add(d.getNombreProducto() + ": faltante no aceptado ("
                            + fmt(solicitado - despachado) + " " + d.getAbreviatura() + ").");
                } else {
                    conFaltanteAceptado = true;
                }
            }
        }
        if (!errores.isEmpty()) {
            throw new IllegalStateException("No se puede cerrar el despacho:\n - "
                    + String.join("\n - ", errores));
        }
        String textoNovedades = novedades == null ? null : novedades.trim();
        if (conFaltanteAceptado && (textoNovedades == null || textoNovedades.isEmpty())) {
            throw new IllegalArgumentException("Debe registrar una novedad para el faltante aceptado.");
        }
        // C-3: cierre del despacho y avance de la plantilla de forma atomica.
        ConexionBD.enTransaccion(() -> {
            despachoDAO.actualizarEstado(idDespacho, Despacho.DESPACHADA, textoNovedades);
            plantillaDAO.actualizarEstado(idPlantilla, PlantillaOperativa.ESTADO_DESPACHADA);
        });
        return List.of();
    }

    public Despacho actual(int idPlantilla) {
        return despachoDAO.obtenerPorPlantilla(idPlantilla);
    }

    public List<DetalleDespacho> detalle(int idDespacho) {
        return despachoDAO.listarDetalle(idDespacho);
    }

    /** Indica si el despacho tiene al menos un faltante aceptado. */
    public boolean tieneFaltanteAceptado(int idDespacho) {
        return despachoDAO.listarDetalle(idDespacho).stream()
                .anyMatch(DetalleDespacho::isFaltanteAceptado);
    }

    /** Items verificados / total. */
    public int[] progreso(int idDespacho) {
        List<DetalleDespacho> detalles = despachoDAO.listarDetalle(idDespacho);
        int verificados = (int) detalles.stream().filter(DetalleDespacho::isVerificado).count();
        return new int[]{verificados, detalles.size()};
    }

    /** Cantidad que fisicamente puede salir: lo alistado, acotado por lo solicitado. */
    private static double disponible(DetalleDespacho d) {
        double alistado = d.getCantidadAlistada();
        if (alistado > EPS) {
            return Math.min(d.getCantidadSolicitada(), alistado);
        }
        return d.getCantidadSolicitada();
    }

    private DetalleDespacho buscarDetalle(int idDespacho, int idDetalleDespacho) {
        for (DetalleDespacho d : despachoDAO.listarDetalle(idDespacho)) {
            if (d.getIdDetalleDespacho() == idDetalleDespacho) {
                return d;
            }
        }
        return null;
    }

    private static String fmt(double valor) {
        return String.format(java.util.Locale.US, "%.2f", valor);
    }
}
