package agromedio.logica;

import agromedio.datos.CompraDAO;
import agromedio.datos.CompraDAOSqlite;
import agromedio.datos.ConsultaDAO;
import agromedio.datos.ConsultaDAOSqlite;
import agromedio.datos.PlantillaDAO;
import agromedio.datos.PlantillaDAOSqlite;
import agromedio.modelo.Compra;
import agromedio.modelo.DetalleCompra;
import agromedio.modelo.FilaConsolidado;
import agromedio.modelo.PlantillaOperativa;

import java.time.LocalDate;
import java.util.List;

/** Modulo 1 - Compras: consolidacion de demanda (RF03) y registro (RF04). */
public class CompraServicio {

    private final CompraDAO compraDAO;
    private final PlantillaDAO plantillaDAO;
    private final ConsultaDAO consultaDAO;

    public CompraServicio() {
        this(new CompraDAOSqlite(), new PlantillaDAOSqlite(), new ConsultaDAOSqlite());
    }

    public CompraServicio(CompraDAO compraDAO, PlantillaDAO plantillaDAO, ConsultaDAO consultaDAO) {
        this.compraDAO = compraDAO;
        this.plantillaDAO = plantillaDAO;
        this.consultaDAO = consultaDAO;
    }

    /** RF03: demanda total por producto de todas las plantillas activas de la fecha. */
    public List<FilaConsolidado> consolidar(String fechaProgramada) {
        if (fechaProgramada == null || fechaProgramada.isEmpty()) {
            fechaProgramada = LocalDate.now().toString();
        }
        return consultaDAO.consolidarDemanda(fechaProgramada);
    }

    /** RF04: registra la compra de los productos marcados para una plantilla. */
    public int registrarCompra(int idPlantilla, String fechaCompra, List<DetalleCompra> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Debe agregar al menos un producto a la compra.");
        }
        PlantillaOperativa plantilla = plantillaDAO.obtener(idPlantilla);
        if (plantilla == null) {
            throw new IllegalArgumentException("La plantilla no existe.");
        }
        if (PlantillaOperativa.ESTADO_DESPACHADA.equals(plantilla.getEstado())
                || PlantillaOperativa.ESTADO_LISTA.equals(plantilla.getEstado())) {
            throw new IllegalStateException("La plantilla ya avanzo en el ciclo y no admite nuevas compras.");
        }
        for (DetalleCompra d : items) {
            if (d.getCantidadComprada() <= 0) {
                throw new IllegalArgumentException("Todas las cantidades compradas deben ser mayores que cero.");
            }
            if (d.getIdProveedor() <= 0) {
                throw new IllegalArgumentException("Cada producto debe tener proveedor asignado.");
            }
        }

        Compra compra = new Compra();
        compra.setIdPlantilla(idPlantilla);
        compra.setFechaCompra(fechaCompra == null || fechaCompra.isEmpty() ? LocalDate.now().toString() : fechaCompra);
        compra.setEstado(Compra.REGISTRADA);
        compraDAO.guardar(compra);

        for (DetalleCompra d : items) {
            d.setIdCompra(compra.getIdCompra());
            compraDAO.agregarDetalle(d);
        }
        if (PlantillaOperativa.ESTADO_CARGADA.equals(plantilla.getEstado())) {
            plantillaDAO.actualizarEstado(idPlantilla, PlantillaOperativa.ESTADO_EN_COMPRA);
        }
        return compra.getIdCompra();
    }

    public List<Compra> comprasDe(int idPlantilla) {
        return compraDAO.listarPorPlantilla(idPlantilla);
    }

    public List<Compra> comprasPendientes() {
        return compraDAO.listarPorEstado(Compra.REGISTRADA);
    }

    public List<DetalleCompra> detalle(int idCompra) {
        return compraDAO.listarDetalle(idCompra);
    }
}
