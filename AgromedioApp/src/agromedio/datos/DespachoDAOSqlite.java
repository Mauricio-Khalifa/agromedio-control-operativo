package agromedio.datos;

import agromedio.modelo.DetalleDespacho;
import agromedio.modelo.Despacho;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DespachoDAOSqlite implements DespachoDAO {

    @Override
    public Despacho obtenerPorPlantilla(int idPlantilla) {
        String sql = "SELECT d.id_despacho, d.id_plantilla, d.fecha_despacho, d.estado, d.novedades, p.nombre_mercado "
                + "FROM despacho d JOIN plantilla_operativa p ON p.id_plantilla = d.id_plantilla "
                + "WHERE d.id_plantilla = ? ORDER BY d.id_despacho DESC LIMIT 1";
        try (Connection c = ConexionBD.obtener(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idPlantilla);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Despacho d = new Despacho(rs.getInt(1), rs.getInt(2), rs.getString(3), rs.getString(4), rs.getString(5));
                    d.setNombreMercado(rs.getString(6));
                    return d;
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar despacho: " + e.getMessage(), e);
        }
    }

    @Override
    public int guardar(Despacho d) {
        String sql = "INSERT INTO despacho (id_plantilla, fecha_despacho, estado, novedades) VALUES (?, ?, ?, ?)";
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, d.getIdPlantilla());
            ps.setString(2, d.getFechaDespacho());
            ps.setString(3, d.getEstado() == null ? Despacho.EN_VERIFICACION : d.getEstado());
            ps.setString(4, d.getNovedades());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    d.setIdDespacho(rs.getInt(1));
                }
            }
            return d.getIdDespacho();
        } catch (SQLException e) {
            throw new RuntimeException("Error al guardar despacho: " + e.getMessage(), e);
        }
    }

    @Override
    public void actualizarEstado(int idDespacho, String estado, String novedades) {
        String sql = "UPDATE despacho SET estado = ?, novedades = ? WHERE id_despacho = ?";
        try (Connection c = ConexionBD.obtener(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, estado);
            ps.setString(2, novedades);
            ps.setInt(3, idDespacho);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar despacho: " + e.getMessage(), e);
        }
    }

    private static final String DETALLE_SQL =
            "SELECT dd.id_detalle_despacho, dd.id_despacho, dd.id_producto, dd.cantidad_despachada, dd.verificado, "
          + "pr.nombre, um.abreviatura, "
          + "(SELECT dp.cantidad_solicitada FROM detalle_plantilla dp "
          + " WHERE dp.id_plantilla = d.id_plantilla AND dp.id_producto = dd.id_producto) AS solicitado "
          + "FROM detalle_despacho dd "
          + "JOIN despacho d ON d.id_despacho = dd.id_despacho "
          + "JOIN producto pr ON pr.id_producto = dd.id_producto "
          + "JOIN unidad_medida um ON um.id_unidad = pr.id_unidad_despacho ";

    @Override
    public List<DetalleDespacho> listarDetalle(int idDespacho) {
        List<DetalleDespacho> lista = new ArrayList<>();
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(DETALLE_SQL + "WHERE dd.id_despacho = ? ORDER BY pr.nombre")) {
            ps.setInt(1, idDespacho);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DetalleDespacho d = new DetalleDespacho(rs.getInt(1), rs.getInt(2), rs.getInt(3),
                            rs.getDouble(4), rs.getInt(5) == 1);
                    d.setNombreProducto(rs.getString(6));
                    d.setAbreviatura(rs.getString(7));
                    d.setCantidadSolicitada(rs.getDouble(8));
                    lista.add(d);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar detalle de despacho: " + e.getMessage(), e);
        }
        return lista;
    }

    @Override
    public void agregarDetalle(DetalleDespacho d) {
        String sql = "INSERT INTO detalle_despacho (id_despacho, id_producto, cantidad_despachada, verificado) "
                + "VALUES (?, ?, ?, ?)";
        try (Connection c = ConexionBD.obtener(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, d.getIdDespacho());
            ps.setInt(2, d.getIdProducto());
            ps.setDouble(3, d.getCantidadDespachada());
            ps.setInt(4, d.isVerificado() ? 1 : 0);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al agregar producto al despacho: " + e.getMessage(), e);
        }
    }

    @Override
    public void actualizarDetalle(DetalleDespacho d) {
        String sql = "UPDATE detalle_despacho SET cantidad_despachada = ?, verificado = ? WHERE id_detalle_despacho = ?";
        try (Connection c = ConexionBD.obtener(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDouble(1, d.getCantidadDespachada());
            ps.setInt(2, d.isVerificado() ? 1 : 0);
            ps.setInt(3, d.getIdDetalleDespacho());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar detalle de despacho: " + e.getMessage(), e);
        }
    }
}
