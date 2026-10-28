package agromedio.datos;

import agromedio.modelo.Compra;
import agromedio.modelo.DetalleCompra;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CompraDAOSqlite implements CompraDAO {

    private static final String SELECT_BASE =
            "SELECT c.id_compra, c.id_plantilla, c.fecha_compra, c.estado, p.nombre_mercado, "
          + "c.id_proveedor, c.costo_total, c.observaciones, pv.nombre "
          + "FROM compra c LEFT JOIN plantilla_operativa p ON p.id_plantilla = c.id_plantilla "
          + "LEFT JOIN proveedor pv ON pv.id_proveedor = c.id_proveedor ";

    private static Compra mapear(ResultSet rs) throws SQLException {
        Compra c = new Compra(rs.getInt(1), rs.getInt(2), rs.getString(3), rs.getString(4));
        c.setNombreMercado(rs.getString(5));
        c.setIdProveedor(rs.getInt(6));
        c.setCostoTotal(rs.getDouble(7));
        c.setObservaciones(rs.getString(8));
        c.setNombreProveedor(rs.getString(9));
        return c;
    }

    @Override
    public List<Compra> listar() {
        return consultar(SELECT_BASE + "ORDER BY c.fecha_compra DESC, c.id_compra DESC");
    }

    @Override
    public List<Compra> listarPorPlantilla(int idPlantilla) {
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(SELECT_BASE + "WHERE c.id_plantilla = ? ORDER BY c.id_compra")) {
            ps.setInt(1, idPlantilla);
            return ejecutar(ps);
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar compras de la plantilla: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Compra> listarPorEstado(String estado) {
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(SELECT_BASE + "WHERE c.estado = ? ORDER BY c.fecha_compra, c.id_compra")) {
            ps.setString(1, estado);
            return ejecutar(ps);
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar compras por estado: " + e.getMessage(), e);
        }
    }

    @Override
    public Compra obtener(int id) {
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(SELECT_BASE + "WHERE c.id_compra = ?")) {
            ps.setInt(1, id);
            List<Compra> res = ejecutar(ps);
            return res.isEmpty() ? null : res.get(0);
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener compra: " + e.getMessage(), e);
        }
    }

    @Override
    public int guardar(Compra compra) {
        String sqlInsert = "INSERT INTO compra (id_plantilla, id_proveedor, fecha_compra, estado, costo_total, observaciones) VALUES (?, ?, ?, ?, ?, ?)";
        String sqlUpdate = "UPDATE compra SET id_plantilla = ?, id_proveedor = ?, fecha_compra = ?, estado = ?, costo_total = ?, observaciones = ? WHERE id_compra = ?";
        try (Connection c = ConexionBD.obtener()) {
            if (compra.getIdCompra() == 0) {
                try (PreparedStatement ps = c.prepareStatement(sqlInsert, Statement.RETURN_GENERATED_KEYS)) {
                    if (compra.getIdPlantilla() > 0) ps.setInt(1, compra.getIdPlantilla()); else ps.setNull(1, Types.INTEGER);
                    if (compra.getIdProveedor() > 0) ps.setInt(2, compra.getIdProveedor()); else ps.setNull(2, Types.INTEGER);
                    ps.setString(3, compra.getFechaCompra());
                    ps.setString(4, compra.getEstado() == null ? Compra.REGISTRADA : compra.getEstado());
                    ps.setDouble(5, compra.getCostoTotal());
                    ps.setString(6, compra.getObservaciones());
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            compra.setIdCompra(rs.getInt(1));
                        }
                    }
                    return compra.getIdCompra();
                }
            }
            try (PreparedStatement ps = c.prepareStatement(sqlUpdate)) {
                if (compra.getIdPlantilla() > 0) ps.setInt(1, compra.getIdPlantilla()); else ps.setNull(1, Types.INTEGER);
                if (compra.getIdProveedor() > 0) ps.setInt(2, compra.getIdProveedor()); else ps.setNull(2, Types.INTEGER);
                ps.setString(3, compra.getFechaCompra());
                ps.setString(4, compra.getEstado());
                ps.setDouble(5, compra.getCostoTotal());
                ps.setString(6, compra.getObservaciones());
                ps.setInt(7, compra.getIdCompra());
                ps.executeUpdate();
                return compra.getIdCompra();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al guardar compra: " + e.getMessage(), e);
        }
    }

    @Override
    public void actualizarEstado(int idCompra, String estado) {
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement("UPDATE compra SET estado = ? WHERE id_compra = ?")) {
            ps.setString(1, estado);
            ps.setInt(2, idCompra);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar compra: " + e.getMessage(), e);
        }
    }

    @Override
    public List<DetalleCompra> listarDetalle(int idCompra) {
        String sql = DETALLE_SQL + "WHERE dc.id_compra = ? ORDER BY pr.nombre";
        try (Connection c = ConexionBD.obtener(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idCompra);
            return ejecutarDetalle(ps);
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar detalle de compra: " + e.getMessage(), e);
        }
    }

    @Override
    public List<DetalleCompra> listarDetallePorPlantilla(int idPlantilla) {
        String sql = DETALLE_SQL + "WHERE c.id_plantilla = ? ORDER BY c.id_compra, pr.nombre";
        try (Connection c = ConexionBD.obtener(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idPlantilla);
            return ejecutarDetalle(ps);
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar detalle de compra: " + e.getMessage(), e);
        }
    }

    @Override
    public int agregarDetalle(DetalleCompra d) {
        String sql = "INSERT INTO detalle_compra (id_compra, id_producto, id_proveedor, cantidad_comprada, costo_unitario, subtotal) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection c = ConexionBD.obtener(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, d.getIdCompra());
            ps.setInt(2, d.getIdProducto());
            ps.setInt(3, d.getIdProveedor());
            ps.setDouble(4, d.getCantidadComprada());
            ps.setDouble(5, d.getCostoUnitario());
            ps.setDouble(6, d.getSubtotal());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    d.setIdDetalleCompra(rs.getInt(1));
                }
            }
            return d.getIdDetalleCompra();
        } catch (SQLException e) {
            throw new RuntimeException("Error al agregar producto a la compra: " + e.getMessage(), e);
        }
    }

    @Override
    public void eliminarDetalle(int idDetalleCompra) {
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement("DELETE FROM detalle_compra WHERE id_detalle_compra = ?")) {
            ps.setInt(1, idDetalleCompra);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar producto de la compra: " + e.getMessage(), e);
        }
    }

    @Override
    public int agregarDistribucion(agromedio.modelo.DistribucionCompra d) {
        String sql = "INSERT INTO distribucion_compra (id_detalle_compra, id_plantilla, cantidad_asignada, costo_asignado) VALUES (?, ?, ?, ?)";
        try (Connection c = ConexionBD.obtener(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, d.getIdDetalleCompra());
            ps.setInt(2, d.getIdPlantilla());
            ps.setDouble(3, d.getCantidadAsignada());
            ps.setDouble(4, d.getCostoAsignado());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) { if (rs.next()) d.setIdDistribucion(rs.getInt(1)); }
            return d.getIdDistribucion();
        } catch (SQLException e) { throw new RuntimeException("Error al guardar distribución: " + e.getMessage(), e); }
    }

    @Override
    public List<agromedio.modelo.DistribucionCompra> listarDistribucionPorCompra(int idCompra) {
        String sql = "SELECT d.id_distribucion, d.id_detalle_compra, d.id_plantilla, d.cantidad_asignada, d.costo_asignado, p.nombre_mercado, pr.nombre "
                + "FROM distribucion_compra d JOIN detalle_compra dc ON dc.id_detalle_compra = d.id_detalle_compra "
                + "JOIN plantilla_operativa p ON p.id_plantilla = d.id_plantilla JOIN producto pr ON pr.id_producto = dc.id_producto "
                + "WHERE dc.id_compra = ? ORDER BY d.id_plantilla";
        List<agromedio.modelo.DistribucionCompra> lista = new ArrayList<>();
        try (Connection c = ConexionBD.obtener(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idCompra);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) {
                agromedio.modelo.DistribucionCompra d = new agromedio.modelo.DistribucionCompra();
                d.setIdDistribucion(rs.getInt(1)); d.setIdDetalleCompra(rs.getInt(2)); d.setIdPlantilla(rs.getInt(3));
                d.setCantidadAsignada(rs.getDouble(4)); d.setCostoAsignado(rs.getDouble(5));
                d.setNombreMercado(rs.getString(6)); d.setNombreProducto(rs.getString(7)); lista.add(d);
            }}
            return lista;
        } catch (SQLException e) { throw new RuntimeException("Error al listar distribución: " + e.getMessage(), e); }
    }

    private static final String DETALLE_SQL =
            "SELECT dc.id_detalle_compra, dc.id_compra, dc.id_producto, dc.id_proveedor, dc.cantidad_comprada, dc.costo_unitario, dc.subtotal, "
          + "pr.nombre, pv.nombre, um.abreviatura, c.id_plantilla "
          + "FROM detalle_compra dc "
          + "JOIN compra c ON c.id_compra = dc.id_compra "
          + "JOIN producto pr ON pr.id_producto = dc.id_producto "
          + "JOIN proveedor pv ON pv.id_proveedor = dc.id_proveedor "
          + "JOIN unidad_medida um ON um.id_unidad = pr.id_unidad_compra ";

    private static DetalleCompra mapearDetalle(ResultSet rs) throws SQLException {
        DetalleCompra d = new DetalleCompra(rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getInt(4), rs.getDouble(5));
        d.setCostoUnitario(rs.getDouble(6));
        d.setSubtotal(rs.getDouble(7));
        d.setNombreProducto(rs.getString(8));
        d.setNombreProveedor(rs.getString(9));
        d.setAbreviatura(rs.getString(10));
        return d;
    }

    private List<DetalleCompra> ejecutarDetalle(PreparedStatement ps) throws SQLException {
        List<DetalleCompra> lista = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapearDetalle(rs));
            }
        }
        return lista;
    }

    private List<Compra> consultar(String sql) {
        try (Connection c = ConexionBD.obtener(); PreparedStatement ps = c.prepareStatement(sql)) {
            return ejecutar(ps);
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar compras: " + e.getMessage(), e);
        }
    }

    private List<Compra> ejecutar(PreparedStatement ps) throws SQLException {
        List<Compra> lista = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        }
        return lista;
    }
}
