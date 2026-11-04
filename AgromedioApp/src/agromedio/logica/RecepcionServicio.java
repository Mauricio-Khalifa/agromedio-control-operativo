package agromedio.logica;

import agromedio.datos.CompraDAO;
import agromedio.datos.CompraDAOSqlite;
import agromedio.datos.ConexionBD;
import agromedio.datos.PlantillaDAO;
import agromedio.datos.PlantillaDAOSqlite;
import agromedio.datos.RecepcionDAO;
import agromedio.datos.RecepcionDAOSqlite;
import agromedio.modelo.Compra;
import agromedio.modelo.DetalleCompra;
import agromedio.modelo.DetalleRecepcion;
import agromedio.modelo.DistribucionCompra;
import agromedio.modelo.PlantillaOperativa;
import agromedio.modelo.Recepcion;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Modulo 2 - Recepcion: registro de entradas (RF05) y calculo de faltantes (RF06). */
public class RecepcionServicio {

    private final RecepcionDAO recepcionDAO;
    private final CompraDAO compraDAO;
    private final PlantillaDAO plantillaDAO;

    public RecepcionServicio() {
        this(new RecepcionDAOSqlite(), new CompraDAOSqlite(), new PlantillaDAOSqlite());
    }

    public RecepcionServicio(RecepcionDAO recepcionDAO, CompraDAO compraDAO, PlantillaDAO plantillaDAO) {
        this.recepcionDAO = recepcionDAO;
        this.compraDAO = compraDAO;
        this.plantillaDAO = plantillaDAO;
    }

    private static final double EPS = 0.0001;

    /**
     * RF05 - RF06 / RN-05 - RN-06: registra una entrega parcial o total.
     * Cuando la compra es consolidada, actualiza el estado de cada plantilla
     * asociada mediante distribucion_compra.
     */
    public List<DetalleRecepcion> registrarRecepcion(int idCompra, String fecha, String observaciones,
                                                     List<Double> cantidadesRecibidas) {
        Compra compra = compraDAO.obtener(idCompra);
        if (compra == null) throw new IllegalArgumentException("La compra no existe.");
        if (Compra.RECIBIDA.equals(compra.getEstado())) {
            throw new IllegalStateException("La compra ya fue cerrada; no admite nuevas recepciones.");
        }
        if (Compra.ANULADA.equals(compra.getEstado())) {
            throw new IllegalStateException("La compra esta anulada; no admite recepcion.");
        }
        List<DetalleCompra> detalles = compraDAO.listarDetalle(idCompra);
        if (cantidadesRecibidas == null || cantidadesRecibidas.size() != detalles.size()) {
            throw new IllegalArgumentException("Debe indicar la cantidad recibida de cada producto.");
        }

        Map<Integer, DetalleRecepcion> acumulados = new HashMap<>();
        for (DetalleRecepcion d : recepcionDAO.listarAcumuladoPorCompra(idCompra)) {
            acumulados.put(d.getIdDetalleCompra(), d);
        }
        int lineasConCantidad = 0;
        for (int i = 0; i < detalles.size(); i++) {
            DetalleCompra dc = detalles.get(i);
            double recibida = cantidadesRecibidas.get(i) == null ? 0 : cantidadesRecibidas.get(i);
            if (recibida < 0) throw new IllegalArgumentException("La cantidad recibida no puede ser negativa.");
            DetalleRecepcion acumulado = acumulados.get(dc.getIdDetalleCompra());
            double pendiente = acumulado == null ? dc.getCantidadComprada() : acumulado.getCantidadFaltante();
            if (recibida > pendiente + EPS) {
                throw new IllegalArgumentException("La cantidad recibida de " + dc.getNombreProducto()
                        + " supera el pendiente (" + String.format("%.2f", pendiente) + " "
                        + dc.getAbreviatura() + ").");
            }
            if (recibida > 0) lineasConCantidad++;
        }
        if (lineasConCantidad == 0) throw new IllegalArgumentException("Registre al menos una cantidad mayor que cero.");

        Recepcion recepcion = new Recepcion();
        recepcion.setIdCompra(idCompra);
        recepcion.setFechaRecepcion(fecha == null || fecha.isEmpty() ? LocalDate.now().toString() : fecha);
        recepcion.setObservaciones(observaciones);

        ConexionBD.enTransaccion(() -> {
            recepcionDAO.guardar(recepcion);
            for (int i = 0; i < detalles.size(); i++) {
                double recibida = cantidadesRecibidas.get(i) == null ? 0 : cantidadesRecibidas.get(i);
                if (recibida <= 0) continue;
                DetalleRecepcion dr = new DetalleRecepcion();
                dr.setIdRecepcion(recepcion.getIdRecepcion());
                dr.setIdDetalleCompra(detalles.get(i).getIdDetalleCompra());
                dr.setCantidadRecibida(recibida);
                recepcionDAO.agregarDetalle(dr);
            }
            for (int idPlantilla : plantillasDeCompra(compra)) {
                PlantillaOperativa plantilla = plantillaDAO.obtener(idPlantilla);
                if (plantilla != null && (PlantillaOperativa.ESTADO_EN_COMPRA.equals(plantilla.getEstado())
                        || PlantillaOperativa.ESTADO_CARGADA.equals(plantilla.getEstado())
                        || PlantillaOperativa.ESTADO_RECIBIENDO.equals(plantilla.getEstado()))) {
                    plantillaDAO.actualizarEstado(idPlantilla, PlantillaOperativa.ESTADO_RECIBIENDO);
                }
            }
        });
        return faltantesPendientes(idCompra);
    }

    /** Estado acumulado de todos los detalles de la compra (recibido y pendiente). */
    public List<DetalleRecepcion> estadoRecepcion(int idCompra) {
        return recepcionDAO.listarAcumuladoPorCompra(idCompra);
    }

    /** Faltantes que aun no se han recibido, una fila por producto. */
    public List<DetalleRecepcion> faltantesPendientes(int idCompra) {
        List<DetalleRecepcion> pendientes = new ArrayList<>();
        for (DetalleRecepcion d : estadoRecepcion(idCompra)) if (d.getCantidadFaltante() > 0) pendientes.add(d);
        return pendientes;
    }

    /** Cierra la recepción y habilita el alistamiento para todas las plantillas asignadas al pedido. */
    public void cerrarRecepcion(int idCompra) {
        Compra compra = compraDAO.obtener(idCompra);
        if (compra == null) throw new IllegalArgumentException("La compra no existe.");
        if (Compra.ANULADA.equals(compra.getEstado())) {
            throw new IllegalStateException("La compra esta anulada; no se puede cerrar la recepcion.");
        }
        List<Recepcion> recepciones = recepcionDAO.listarPorCompra(idCompra);
        if (recepciones.isEmpty()) throw new IllegalStateException("Primero debe registrar la entrada de mercancia.");

        List<Integer> plantillas = plantillasDeCompra(compra);
        ConexionBD.enTransaccion(() -> {
            compraDAO.actualizarEstado(idCompra, Compra.RECIBIDA);
            for (int idPlantilla : plantillas) {
                plantillaDAO.actualizarEstado(idPlantilla, PlantillaOperativa.ESTADO_ALISTANDO);
            }
        });
    }

    /** Obtiene todas las plantillas implicadas tanto en compras consolidadas como antiguas. */
    private List<Integer> plantillasDeCompra(Compra compra) {
        Set<Integer> ids = new LinkedHashSet<>();
        if (compra.getIdPlantilla() > 0) ids.add(compra.getIdPlantilla());
        for (DistribucionCompra distribucion : compraDAO.listarDistribucionPorCompra(compra.getIdCompra())) {
            if (distribucion.getIdPlantilla() > 0) ids.add(distribucion.getIdPlantilla());
        }
        return new ArrayList<>(ids);
    }

    public List<Recepcion> recepcionesDe(int idCompra) { return recepcionDAO.listarPorCompra(idCompra); }
    public List<DetalleRecepcion> detalle(int idRecepcion) { return recepcionDAO.listarDetalle(idRecepcion); }
    public List<Compra> comprasPendientes() { return compraDAO.listarPorEstado(Compra.REGISTRADA); }
    public List<DetalleCompra> detalleCompra(int idCompra) { return compraDAO.listarDetalle(idCompra); }
}
