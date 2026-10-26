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
import agromedio.modelo.PlantillaOperativa;
import agromedio.modelo.Recepcion;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
     * RF05 - RF06 / RN-05 - RN-06: registra una entrega parcial o total de la
     * compra. Se pueden hacer varias entregas mientras la compra no este
     * cerrada. Las cantidades se acumulan por detalle de compra y el trigger
     * trg_detalle_recepcion_faltante calcula el faltante pendiente.
     *
     * D02: las lineas con cantidad 0 no se persisten; si todas son 0 se
     * rechaza la entrega.
     *
     * @return faltantes pendientes despues de la entrega (una fila por producto)
     */
    public List<DetalleRecepcion> registrarRecepcion(int idCompra, String fecha, String observaciones,
                                                     List<Double> cantidadesRecibidas) {
        Compra compra = compraDAO.obtener(idCompra);
        if (compra == null) {
            throw new IllegalArgumentException("La compra no existe.");
        }
        if (Compra.RECIBIDA.equals(compra.getEstado())) {
            throw new IllegalStateException("La compra ya fue cerrada; no admite nuevas recepciones.");
        }
        // H-03: una compra anulada no puede recibirse.
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
            if (recibida < 0) {
                throw new IllegalArgumentException("La cantidad recibida no puede ser negativa.");
            }
            DetalleRecepcion acumulado = acumulados.get(dc.getIdDetalleCompra());
            double pendiente = acumulado == null ? dc.getCantidadComprada() : acumulado.getCantidadFaltante();
            if (recibida > pendiente + EPS) {
                throw new IllegalArgumentException("La cantidad recibida de " + dc.getNombreProducto()
                        + " supera el pendiente (" + String.format("%.2f", pendiente) + " "
                        + dc.getAbreviatura() + ").");
            }
            if (recibida > 0) {
                lineasConCantidad++;
            }
        }
        if (lineasConCantidad == 0) {
            throw new IllegalArgumentException("Registre al menos una cantidad mayor que cero.");
        }

        Recepcion recepcion = new Recepcion();
        recepcion.setIdCompra(idCompra);
        recepcion.setFechaRecepcion(fecha == null || fecha.isEmpty() ? LocalDate.now().toString() : fecha);
        recepcion.setObservaciones(observaciones);

        // C-3: cabecera de recepcion + detalle + estado de la plantilla atómicos.
        ConexionBD.enTransaccion(() -> {
            recepcionDAO.guardar(recepcion);
            for (int i = 0; i < detalles.size(); i++) {
                double recibida = cantidadesRecibidas.get(i) == null ? 0 : cantidadesRecibidas.get(i);
                if (recibida <= 0) {
                    continue;   // D02: el cero no se persiste
                }
                DetalleRecepcion dr = new DetalleRecepcion();
                dr.setIdRecepcion(recepcion.getIdRecepcion());
                dr.setIdDetalleCompra(detalles.get(i).getIdDetalleCompra());
                dr.setCantidadRecibida(recibida);
                recepcionDAO.agregarDetalle(dr);
            }

            PlantillaOperativa plantilla = plantillaDAO.obtener(compra.getIdPlantilla());
            if (plantilla != null && (PlantillaOperativa.ESTADO_EN_COMPRA.equals(plantilla.getEstado())
                    || PlantillaOperativa.ESTADO_CARGADA.equals(plantilla.getEstado())
                    || PlantillaOperativa.ESTADO_RECIBIENDO.equals(plantilla.getEstado()))) {
                plantillaDAO.actualizarEstado(plantilla.getIdPlantilla(), PlantillaOperativa.ESTADO_RECIBIENDO);
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
        for (DetalleRecepcion d : estadoRecepcion(idCompra)) {
            if (d.getCantidadFaltante() > 0) {
                pendientes.add(d);
            }
        }
        return pendientes;
    }

    /** Cierra la recepcion de la compra y habilita el alistamiento (produccion). */
    public void cerrarRecepcion(int idCompra) {
        Compra compra = compraDAO.obtener(idCompra);
        if (compra == null) {
            throw new IllegalArgumentException("La compra no existe.");
        }
        if (Compra.ANULADA.equals(compra.getEstado())) {
            throw new IllegalStateException("La compra esta anulada; no se puede cerrar la recepcion.");
        }
        List<Recepcion> recepciones = recepcionDAO.listarPorCompra(idCompra);
        if (recepciones.isEmpty()) {
            throw new IllegalStateException("Primero debe registrar la entrada de mercancia.");
        }
        // C-3: cierre de la compra y avance de la plantilla de forma atomica.
        ConexionBD.enTransaccion(() -> {
            compraDAO.actualizarEstado(idCompra, Compra.RECIBIDA);
            plantillaDAO.actualizarEstado(compra.getIdPlantilla(), PlantillaOperativa.ESTADO_ALISTANDO);
        });
    }

    public List<Recepcion> recepcionesDe(int idCompra) {
        return recepcionDAO.listarPorCompra(idCompra);
    }

    public List<DetalleRecepcion> detalle(int idRecepcion) {
        return recepcionDAO.listarDetalle(idRecepcion);
    }

    public List<Compra> comprasPendientes() {
        return compraDAO.listarPorEstado(Compra.REGISTRADA);
    }

    public List<DetalleCompra> detalleCompra(int idCompra) {
        return compraDAO.listarDetalle(idCompra);
    }
}
