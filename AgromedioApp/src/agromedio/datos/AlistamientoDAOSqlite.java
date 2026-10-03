package agromedio.datos;

import agromedio.modelo.Alistamiento;
import agromedio.modelo.DetalleAlistamiento;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AlistamientoDAOSqlite implements AlistamientoDAO {

    @Override
    public Alistamiento obtenerPorPlantilla(int idPlantilla) {
        String sql = "SELECT id_alistamiento, id_plantilla, fecha_alistamiento, estado FROM alistamiento "
                + "WHERE id_plantilla = ? ORDER BY id_alistamiento DESC LIMIT 1";
        try (Connection c = ConexionBD.obtener(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idPlantilla);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new Alistamiento(rs.getInt(1), rs.getInt(2), rs.getString(3), rs.getString(4)) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar alistamiento: " + e.getMessage(), e);
        }
    }

    @Override
    public int guardar(Alistamiento a) {
        String sql = "INSERT INTO alistamiento (id_plantilla, fecha_alistamiento, estado) VALUES (?, ?, ?)";
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, a.getIdPlantilla());
            ps.setString(2, a.getFechaAlistamiento());
            ps.setString(3, a.getEstado() == null ? Alistamiento.EN_PROCESO : a.getEstado());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    a.setIdAlistamiento(rs.getInt(1));
                }
            }
            return a.getIdAlistamiento();
        } catch (SQLException e) {
            throw new RuntimeException("Error al guardar alistamiento: " + e.getMessage(), e);
        }
    }

    @Override
    public void actualizarEstado(int idAlistamiento, String estado) {
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement("UPDATE alistamiento SET estado = ? WHERE id_alistamiento = ?")) {
            ps.setString(1, estado);
            ps.setInt(2, idAlistamiento);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar alistamiento: " + e.getMessage(), e);
        }
    }

    private static final String DETALLE_SQL =
            "SELECT da.id_detalle_alistamiento, da.id_alistamiento, da.id_producto, da.cantidad_alistada, da.completado, "
          + "pr.nombre, um.abreviatura, "
          + "(SELECT dp.cantidad_solicitada FROM detalle_plantilla dp "
          + " WHERE dp.id_plantilla = a.id_plantilla AND dp.id_producto = da.id_producto) AS solicitado "
          + "FROM detalle_alistamiento da "
          + "JOIN alistamiento a ON a.id_alistamiento = da.id_alistamiento "
          + "JOIN producto pr ON pr.id_producto = da.id_producto "
          + "JOIN unidad_medida um ON um.id_unidad = pr.id_unidad_despacho ";

    @Override
    public List<DetalleAlistamiento> listarDetalle(int idAlistamiento) {
        List<DetalleAlistamiento> lista = new ArrayList<>();
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(DETALLE_SQL + "WHERE da.id_alistamiento = ? ORDER BY pr.nombre")) {
            ps.setInt(1, idAlistamiento);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DetalleAlistamiento d = new DetalleAlistamiento(rs.getInt(1), rs.getInt(2), rs.getInt(3),
                            rs.getDouble(4), rs.getInt(5) == 1);
                    d.setNombreProducto(rs.getString(6));
                    d.setAbreviatura(rs.getString(7));
                    d.setCantidadSolicitada(rs.getDouble(8));
                    lista.add(d);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar detalle de alistamiento: " + e.getMessage(), e);
        }
        return lista;
    }

    @Override
    public void agregarDetalle(DetalleAlistamiento d) {
        String sql = "INSERT INTO detalle_alistamiento (id_alistamiento, id_producto, cantidad_alistada, completado) "
                + "VALUES (?, ?, ?, ?)";
        try (Connection c = ConexionBD.obtener(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, d.getIdAlistamiento());
            ps.setInt(2, d.getIdProducto());
            ps.setDouble(3, d.getCantidadAlistada());
            ps.setInt(4, d.isCompletado() ? 1 : 0);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al agregar producto al alistamiento: " + e.getMessage(), e);
        }
    }

    @Override
    public void actualizarDetalle(DetalleAlistamiento d) {
        String sql = "UPDATE detalle_alistamiento SET cantidad_alistada = ?, completado = ? WHERE id_detalle_alistamiento = ?";
        try (Connection c = ConexionBD.obtener(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDouble(1, d.getCantidadAlistada());
            ps.setInt(2, d.isCompletado() ? 1 : 0);
            ps.setInt(3, d.getIdDetalleAlistamiento());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar alistamiento: " + e.getMessage(), e);
        }
    }
}
