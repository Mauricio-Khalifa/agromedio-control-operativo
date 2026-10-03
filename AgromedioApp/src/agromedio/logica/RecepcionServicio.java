package agromedio.logica;

import agromedio.datos.CompraDAO;
import agromedio.datos.CompraDAOSqlite;
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
import java.util.List;

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

    /**
     * RF05 - RF06: registra las cantidades fisicas recibidas de cada detalle de la
     * compra. La base de datos calcula el faltante con el trigger
     * trg_detalle_recepcion_faltante; aqui se devuelven las diferencias detectadas.
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
        List<DetalleCompra> detalles = compraDAO.listarDetalle(idCompra);
        if (cantidadesRecibidas == null || cantidadesRecibidas.size() != detalles.size()) {
            throw new IllegalArgumentException("Debe indicar la cantidad recibida de cada producto.");
        }

        Recepcion recepcion = new Recepcion();
        recepcion.setIdCompra(idCompra);
        recepcion.setFechaRecepcion(fecha == null || fecha.isEmpty() ? LocalDate.now().toString() : fecha);
        recepcion.setObservaciones(observaciones);
        recepcionDAO.guardar(recepcion);

        List<DetalleRecepcion> faltantes = new ArrayList<>();
        for (int i = 0; i < detalles.size(); i++) {
            double recibida = cantidadesRecibidas.get(i) == null ? 0 : cantidadesRecibidas.get(i);
            if (recibida < 0) {
                throw new IllegalArgumentException("La cantidad recibida no puede ser negativa.");
            }
            DetalleRecepcion dr = new DetalleRecepcion();
            dr.setIdRecepcion(recepcion.getIdRecepcion());
            dr.setIdDetalleCompra(detalles.get(i).getIdDetalleCompra());
            dr.setCantidadRecibida(recibida);
            recepcionDAO.agregarDetalle(dr);
            if (dr.getCantidadFaltante() > 0) {
                faltantes.add(dr);
            }
        }

        PlantillaOperativa plantilla = plantillaDAO.obtener(compra.getIdPlantilla());
        if (plantilla != null && (PlantillaOperativa.ESTADO_EN_COMPRA.equals(plantilla.getEstado())
                || PlantillaOperativa.ESTADO_CARGADA.equals(plantilla.getEstado())
                || PlantillaOperativa.ESTADO_RECIBIENDO.equals(plantilla.getEstado()))) {
            plantillaDAO.actualizarEstado(plantilla.getIdPlantilla(), PlantillaOperativa.ESTADO_RECIBIENDO);
        }
        return faltantes;
    }

    /** Cierra la recepcion de la compra y habilita el alistamiento (produccion). */
    public void cerrarRecepcion(int idCompra) {
        Compra compra = compraDAO.obtener(idCompra);
        if (compra == null) {
            throw new IllegalArgumentException("La compra no existe.");
        }
        List<Recepcion> recepciones = recepcionDAO.listarPorCompra(idCompra);
        if (recepciones.isEmpty()) {
            throw new IllegalStateException("Primero debe registrar la entrada de mercancia.");
        }
        compraDAO.actualizarEstado(idCompra, Compra.RECIBIDA);
        plantillaDAO.actualizarEstado(compra.getIdPlantilla(), PlantillaOperativa.ESTADO_ALISTANDO);
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
