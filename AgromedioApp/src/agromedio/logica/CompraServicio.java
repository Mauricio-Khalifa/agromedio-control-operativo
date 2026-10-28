package agromedio.logica;

import agromedio.datos.CompraDAO;
import agromedio.datos.CompraDAOSqlite;
import agromedio.datos.ConexionBD;
import agromedio.datos.ConsultaDAO;
import agromedio.datos.ConsultaDAOSqlite;
import agromedio.datos.PlantillaDAO;
import agromedio.datos.PlantillaDAOSqlite;
import agromedio.modelo.Compra;
import agromedio.modelo.DetalleCompra;
import agromedio.modelo.DistribucionCompra;
import agromedio.modelo.FilaConsolidado;
import agromedio.modelo.PlantillaOperativa;

import java.time.LocalDate;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        return registrarCompra(idPlantilla, fechaCompra, items, 0);
    }

    public int registrarCompra(int idPlantilla, String fechaCompra, List<DetalleCompra> items, double costoTotal) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Debe agregar al menos un producto a la compra.");
        }
        PlantillaOperativa plantilla = plantillaDAO.obtener(idPlantilla);
        if (plantilla == null) {
            throw new IllegalArgumentException("La plantilla no existe.");
        }
        // H-04: las compras solo tienen sentido antes de cerrar la recepcion.
        if (PlantillaOperativa.ESTADO_ALISTANDO.equals(plantilla.getEstado())
                || PlantillaOperativa.ESTADO_LISTA.equals(plantilla.getEstado())
                || PlantillaOperativa.ESTADO_DESPACHADA.equals(plantilla.getEstado())) {
            throw new IllegalStateException("La plantilla ya avanzo en el ciclo y no admite nuevas compras.");
        }
        if (plantillaDAO.listarDetalle(idPlantilla).isEmpty()) {
            throw new IllegalStateException("La plantilla no tiene productos. "
                    + "Agregue al menos un producto antes de registrar la compra.");
        }
        // Una sola lÃ­nea por producto y presentaciÃ³n. Dos productos con el mismo
        // nombre comercial pueden ser presentaciones distintas (por ejemplo,
        // 500 ml y 1000 ml), por lo que se diferencian por su registro de catÃ¡logo.
        Set<Integer> productosVistos = new HashSet<>();
        for (DetalleCompra d : items) {
            if (d.getCantidadComprada() <= 0) {
                throw new IllegalArgumentException("Todas las cantidades compradas deben ser mayores que cero.");
            }
            if (d.getIdProveedor() <= 0) {
                throw new IllegalArgumentException("Cada producto debe tener proveedor asignado.");
            }
            if (!productosVistos.add(d.getIdProducto())) {
                throw new IllegalArgumentException("El producto esta repetido en la compra. "
                        + "Registre una sola linea por producto.");
            }
        }

        Compra compra = new Compra();
        compra.setIdPlantilla(idPlantilla);
        compra.setCostoTotal(Math.max(0, costoTotal));
        compra.setFechaCompra(fechaCompra == null || fechaCompra.isEmpty() ? LocalDate.now().toString() : fechaCompra);
        compra.setEstado(Compra.REGISTRADA);

        // C-3: cabecera + detalles + estado de la plantilla de forma atomica.
        return ConexionBD.enTransaccion(() -> {
            compraDAO.guardar(compra);
            for (DetalleCompra d : items) {
                d.setIdCompra(compra.getIdCompra());
                compraDAO.agregarDetalle(d);
                DistribucionCompra distribucion = new DistribucionCompra();
                distribucion.setIdDetalleCompra(d.getIdDetalleCompra());
                distribucion.setIdPlantilla(idPlantilla);
                distribucion.setCantidadAsignada(d.getCantidadComprada());
                distribucion.setCostoAsignado(d.getSubtotal());
                compraDAO.agregarDistribucion(distribucion);
            }
            if (PlantillaOperativa.ESTADO_CARGADA.equals(plantilla.getEstado())) {
                plantillaDAO.actualizarEstado(idPlantilla, PlantillaOperativa.ESTADO_EN_COMPRA);
            }
            return compra.getIdCompra();
        });
    }

    public List<Compra> comprasDe(int idPlantilla) {
        return compraDAO.listarPorPlantilla(idPlantilla);
    }

    /** Registra un pedido consolidado; la relaciÃ³n con plantillas vive solo en distribucion_compra. */
    public int registrarPedidoConsolidado(String fechaCompra, int idProveedor, double costoTotal,
            List<DetalleCompra> items, List<DistribucionCompra> distribuciones) {
        if (items == null || items.isEmpty()) throw new IllegalArgumentException("Debe agregar al menos un producto al pedido.");
        if (idProveedor <= 0) throw new IllegalArgumentException("Seleccione el proveedor del pedido.");
        if (costoTotal < 0) throw new IllegalArgumentException("El costo total no puede ser negativo.");
        Set<Integer> vistos = new HashSet<>();
        for (DetalleCompra d : items) {
            if (d.getCantidadComprada() <= 0) throw new IllegalArgumentException("Las cantidades deben ser mayores que cero.");
            if (d.getIdProveedor() != idProveedor) throw new IllegalArgumentException("Un pedido solo puede tener un proveedor.");
            if (!vistos.add(d.getIdProducto())) throw new IllegalArgumentException("El producto esta repetido en el pedido.");
            d.setSubtotal(0);
        }
        Compra compra = new Compra();
        compra.setIdProveedor(idProveedor);
        compra.setCostoTotal(Math.round(costoTotal * 100.0) / 100.0);
        compra.setFechaCompra(fechaCompra == null || fechaCompra.isEmpty() ? LocalDate.now().toString() : fechaCompra);
        compra.setEstado(Compra.REGISTRADA);
        return ConexionBD.enTransaccion(() -> {
            compraDAO.guardar(compra);
            for (DetalleCompra d : items) {
                d.setIdCompra(compra.getIdCompra());
                compraDAO.agregarDetalle(d);
                double asignado = 0;
                if (distribuciones != null) for (DistribucionCompra x : distribuciones) {
                    if (x.getIdDetalleCompra() == 0 &&
                            (x.getIdProducto() == d.getIdProducto()
                            || (x.getIdProducto() == 0 && x.getNombreProducto() != null
                            && x.getNombreProducto().equals(d.getNombreProducto())))) {
                        x.setIdDetalleCompra(d.getIdDetalleCompra());
                        x.setCostoAsignado(Math.round(d.getSubtotal() * x.getCantidadAsignada() / d.getCantidadComprada() * 100.0) / 100.0);
                        compraDAO.agregarDistribucion(x);
                        asignado += x.getCantidadAsignada();
                    }
                }
                if (asignado + 0.0001 < d.getCantidadComprada()) {
                    try (Connection c = ConexionBD.obtener(); PreparedStatement ps = c.prepareStatement(
                            "SELECT dp.id_plantilla, MAX(dp.cantidad_solicitada - COALESCE(x.comprado, 0)) "
                            + "FROM detalle_plantilla dp LEFT JOIN (SELECT dcp.id_plantilla, dc.id_producto, SUM(dcp.cantidad_asignada) AS comprado "
                            + "FROM detalle_compra dc JOIN distribucion_compra dcp ON dcp.id_detalle_compra = dc.id_detalle_compra "
                            + "JOIN compra co ON co.id_compra = dc.id_compra AND co.estado <> 'ANULADA' "
                            + "WHERE dc.id_producto = ? GROUP BY dcp.id_plantilla, dc.id_producto) x "
                            + "ON x.id_plantilla = dp.id_plantilla AND x.id_producto = dp.id_producto "
                            + "WHERE dp.id_producto = ? GROUP BY dp.id_plantilla ORDER BY dp.id_plantilla")) {
                        ps.setInt(1, d.getIdProducto()); ps.setInt(2, d.getIdProducto());
                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next() && asignado + 0.0001 < d.getCantidadComprada()) {
                                double cantidad = Math.min(Math.max(0, rs.getDouble(2)), d.getCantidadComprada() - asignado);
                                if (cantidad > 0) {
                                    DistribucionCompra x = new DistribucionCompra();
                                    x.setIdDetalleCompra(d.getIdDetalleCompra()); x.setIdPlantilla(rs.getInt(1));
                                    x.setCantidadAsignada(cantidad); x.setCostoAsignado(0);
                                    compraDAO.agregarDistribucion(x); asignado += cantidad;
                                }
                            }
                        }
                    } catch (java.sql.SQLException e) { throw new RuntimeException(e); }
                }
                if (asignado + 0.0001 < d.getCantidadComprada()) {
                    throw new IllegalArgumentException("La cantidad comprada supera la demanda pendiente del producto.");
                }
            }
            return compra.getIdCompra();
        });
    }

    /** Registra varios pedidos en una operación, uno por cada proveedor. */
    public List<Integer> registrarPedidosPorProveedor(String fechaCompra,
            java.util.Map<Integer, Double> costosPorProveedor, List<DetalleCompra> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Debe agregar al menos un producto al pedido.");
        }
        java.util.Map<Integer, List<DetalleCompra>> grupos = new java.util.LinkedHashMap<>();
        for (DetalleCompra item : items) {
            grupos.computeIfAbsent(item.getIdProveedor(), k -> new ArrayList<>()).add(item);
        }
        List<Integer> ids = new ArrayList<>();
        for (java.util.Map.Entry<Integer, List<DetalleCompra>> grupo : grupos.entrySet()) {
            Double costo = costosPorProveedor.get(grupo.getKey());
            if (costo == null) throw new IllegalArgumentException("Falta el costo del pedido de un proveedor.");
            ids.add(registrarPedidoConsolidado(fechaCompra, grupo.getKey(), costo,
                    grupo.getValue(), new ArrayList<>()));
        }
        return ids;
    }

    /** Compatibilidad temporal con el flujo anterior. */
    public int registrarPedido(int idPlantilla, String fechaCompra, int idProveedor, double costoTotal, List<DetalleCompra> items, List<DistribucionCompra> distribuciones) {
        if (items == null || items.isEmpty()) throw new IllegalArgumentException("Debe agregar al menos un producto al pedido.");
        if (idProveedor <= 0) throw new IllegalArgumentException("Seleccione el proveedor del pedido.");
        if (costoTotal < 0) throw new IllegalArgumentException("El costo total no puede ser negativo.");
        Set<Integer> vistos = new HashSet<>();
        double suma = 0;
        for (DetalleCompra d : items) {
            if (d.getCantidadComprada() <= 0) throw new IllegalArgumentException("Las cantidades deben ser mayores que cero.");
            if (d.getIdProveedor() != idProveedor) throw new IllegalArgumentException("Un pedido solo puede tener un proveedor.");
            if (!vistos.add(d.getIdProducto())) throw new IllegalArgumentException("El producto esta repetido en el pedido.");
            d.setSubtotal(Math.round(d.getCantidadComprada() * d.getCostoUnitario() * 100.0) / 100.0);
            suma += d.getSubtotal();
        }
        if (Math.abs(suma - costoTotal) > 0.01) throw new IllegalArgumentException("El costo total no coincide con la suma de los productos.");
        Compra compra = new Compra();
        compra.setIdPlantilla(idPlantilla);
        compra.setIdProveedor(idProveedor);
        compra.setCostoTotal(Math.round(costoTotal * 100.0) / 100.0);
        compra.setFechaCompra(fechaCompra == null || fechaCompra.isEmpty() ? LocalDate.now().toString() : fechaCompra);
        compra.setEstado(Compra.REGISTRADA);
        return ConexionBD.enTransaccion(() -> {
            compraDAO.guardar(compra);
            for (DetalleCompra d : items) {
                d.setIdCompra(compra.getIdCompra());
                compraDAO.agregarDetalle(d);
                if (distribuciones != null) {
                    for (DistribucionCompra distribucion : distribuciones) {
                        if (distribucion.getIdDetalleCompra() == 0
                                && distribucion.getIdPlantilla() == idPlantilla
                                && Math.abs(distribucion.getCantidadAsignada() - d.getCantidadComprada()) < 0.0001) {
                            distribucion.setIdDetalleCompra(d.getIdDetalleCompra());
                            distribucion.setCostoAsignado(Math.round(d.getSubtotal() * 100.0) / 100.0);
                            compraDAO.agregarDistribucion(distribucion);
                            break;
                        }
                    }
                }
            }
            if (idPlantilla > 0 && PlantillaOperativa.ESTADO_CARGADA.equals(plantillaDAO.obtener(idPlantilla).getEstado()))
                plantillaDAO.actualizarEstado(idPlantilla, PlantillaOperativa.ESTADO_EN_COMPRA);
            return compra.getIdCompra();
        });
    }

    public List<Compra> comprasPendientes() {
        return compraDAO.listarPorEstado(Compra.REGISTRADA);
    }

    public List<DetalleCompra> detalle(int idCompra) {
        return compraDAO.listarDetalle(idCompra);
    }
}
