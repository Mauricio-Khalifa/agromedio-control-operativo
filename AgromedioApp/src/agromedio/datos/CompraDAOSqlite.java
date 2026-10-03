package agromedio.datos;

import agromedio.modelo.Compra;
import agromedio.modelo.DetalleCompra;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CompraDAOSqlite implements CompraDAO {

    private static final String SELECT_BASE =
            "SELECT c.id_compra, c.id_plantilla, c.fecha_compra, c.estado, p.nombre_mercado "
          + "FROM compra c JOIN plantilla_operativa p ON p.id_plantilla = c.id_plantilla ";

    private static Compra mapear(ResultSet rs) throws SQLException {
        Compra c = new Compra(rs.getInt(1), rs.getInt(2), rs.getString(3), rs.getString(4));
        c.setNombreMercado(rs.getString(5));
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
        String sqlInsert = "INSERT INTO compra (id_plantilla, fecha_compra, estado) VALUES (?, ?, ?)";
        String sqlUpdate = "UPDATE compra SET id_plantilla = ?, fecha_compra = ?, estado = ? WHERE id_compra = ?";
        try (Connection c = ConexionBD.obtener()) {
            if (compra.getIdCompra() == 0) {
                try (PreparedStatement ps = c.prepareStatement(sqlInsert, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, compra.getIdPlantilla());
                    ps.setString(2, compra.getFechaCompra());
                    ps.setString(3, compra.getEstado() == null ? Compra.REGISTRADA : compra.getEstado());
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
                ps.setInt(1, compra.getIdPlantilla());
                ps.setString(2, compra.getFechaCompra());
                ps.setString(3, compra.getEstado());
                ps.setInt(4, compra.getIdCompra());
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
        String sql = "INSERT INTO detalle_compra (id_compra, id_producto, id_proveedor, cantidad_comprada) VALUES (?, ?, ?, ?)";
        try (Connection c = ConexionBD.obtener(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, d.getIdCompra());
            ps.setInt(2, d.getIdProducto());
            ps.setInt(3, d.getIdProveedor());
            ps.setDouble(4, d.getCantidadComprada());
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

    private static final String DETALLE_SQL =
            "SELECT dc.id_detalle_compra, dc.id_compra, dc.id_producto, dc.id_proveedor, dc.cantidad_comprada, "
          + "pr.nombre, pv.nombre, um.abreviatura, c.id_plantilla "
          + "FROM detalle_compra dc "
          + "JOIN compra c ON c.id_compra = dc.id_compra "
          + "JOIN producto pr ON pr.id_producto = dc.id_producto "
          + "JOIN proveedor pv ON pv.id_proveedor = dc.id_proveedor "
          + "JOIN unidad_medida um ON um.id_unidad = pr.id_unidad_compra ";

    private static DetalleCompra mapearDetalle(ResultSet rs) throws SQLException {
        DetalleCompra d = new DetalleCompra(rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getInt(4), rs.getDouble(5));
        d.setNombreProducto(rs.getString(6));
        d.setNombreProveedor(rs.getString(7));
        d.setAbreviatura(rs.getString(8));
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
