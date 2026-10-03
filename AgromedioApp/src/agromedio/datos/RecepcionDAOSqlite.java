package agromedio.datos;

import agromedio.modelo.DetalleRecepcion;
import agromedio.modelo.Recepcion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RecepcionDAOSqlite implements RecepcionDAO {

    @Override
    public List<Recepcion> listarPorCompra(int idCompra) {
        List<Recepcion> lista = new ArrayList<>();
        String sql = "SELECT id_recepcion, id_compra, fecha_recepcion, observaciones FROM recepcion "
                + "WHERE id_compra = ? ORDER BY id_recepcion";
        try (Connection c = ConexionBD.obtener(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idCompra);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Recepcion(rs.getInt(1), rs.getInt(2), rs.getString(3), rs.getString(4)));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar recepciones: " + e.getMessage(), e);
        }
        return lista;
    }

    @Override
    public int guardar(Recepcion r) {
        String sql = "INSERT INTO recepcion (id_compra, fecha_recepcion, observaciones) VALUES (?, ?, ?)";
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, r.getIdCompra());
            ps.setString(2, r.getFechaRecepcion());
            ps.setString(3, r.getObservaciones());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    r.setIdRecepcion(rs.getInt(1));
                }
            }
            return r.getIdRecepcion();
        } catch (SQLException e) {
            throw new RuntimeException("Error al guardar recepcion: " + e.getMessage(), e);
        }
    }

    @Override
    public void agregarDetalle(DetalleRecepcion d) {
        String sql = "INSERT INTO detalle_recepcion (id_recepcion, id_detalle_compra, cantidad_recibida, cantidad_faltante) "
                + "VALUES (?, ?, ?, ?)";
        try (Connection c = ConexionBD.obtener()) {
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, d.getIdRecepcion());
                ps.setInt(2, d.getIdDetalleCompra());
                ps.setDouble(3, d.getCantidadRecibida());
                ps.setDouble(4, d.getCantidadFaltante());
                ps.executeUpdate();
            }
            // El trigger recalculo el faltante: se recupera el valor oficial de la BD (RF06)
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT cantidad_faltante FROM detalle_recepcion "
                    + "WHERE id_recepcion = ? AND id_detalle_compra = ?")) {
                ps.setInt(1, d.getIdRecepcion());
                ps.setInt(2, d.getIdDetalleCompra());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        d.setCantidadFaltante(rs.getDouble(1));
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al registrar recepcion: " + e.getMessage(), e);
        }
    }

    @Override
    public List<DetalleRecepcion> listarDetalle(int idRecepcion) {
        List<DetalleRecepcion> lista = new ArrayList<>();
        String sql = "SELECT dr.id_detalle_recepcion, dr.id_recepcion, dr.id_detalle_compra, "
                + "dr.cantidad_recibida, dr.cantidad_faltante, pr.nombre, pv.nombre, dc.cantidad_comprada, um.abreviatura "
                + "FROM detalle_recepcion dr "
                + "JOIN detalle_compra dc ON dc.id_detalle_compra = dr.id_detalle_compra "
                + "JOIN producto pr ON pr.id_producto = dc.id_producto "
                + "JOIN proveedor pv ON pv.id_proveedor = dc.id_proveedor "
                + "JOIN unidad_medida um ON um.id_unidad = pr.id_unidad_compra "
                + "WHERE dr.id_recepcion = ? ORDER BY pr.nombre";
        try (Connection c = ConexionBD.obtener(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idRecepcion);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DetalleRecepcion d = new DetalleRecepcion(rs.getInt(1), rs.getInt(2), rs.getInt(3),
                            rs.getDouble(4), rs.getDouble(5));
                    d.setNombreProducto(rs.getString(6));
                    d.setNombreProveedor(rs.getString(7));
                    d.setCantidadComprada(rs.getDouble(8));
                    d.setAbreviatura(rs.getString(9));
                    lista.add(d);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar detalle de recepcion: " + e.getMessage(), e);
        }
        return lista;
    }
}
