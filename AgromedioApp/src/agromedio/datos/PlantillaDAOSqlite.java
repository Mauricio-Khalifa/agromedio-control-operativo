package agromedio.datos;

import agromedio.modelo.DetallePlantilla;
import agromedio.modelo.PlantillaOperativa;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PlantillaDAOSqlite implements PlantillaDAO {

    private static final String SELECT_BASE =
            "SELECT p.id_plantilla, p.id_cliente, p.nombre_mercado, p.fecha_programada, p.estado, p.fecha_carga, c.nombre_entidad "
          + "FROM plantilla_operativa p JOIN cliente c ON c.id_cliente = p.id_cliente ";

    private static PlantillaOperativa mapear(ResultSet rs) throws SQLException {
        PlantillaOperativa p = new PlantillaOperativa(rs.getInt(1), rs.getInt(2), rs.getString(3),
                rs.getString(4), rs.getString(5), rs.getString(6));
        p.setNombreCliente(rs.getString(7));
        return p;
    }

    @Override
    public List<PlantillaOperativa> listar() {
        return consultar(SELECT_BASE + "ORDER BY p.fecha_programada DESC, p.id_plantilla DESC");
    }

    @Override
    public List<PlantillaOperativa> listarPorFecha(String fecha) {
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(SELECT_BASE + "WHERE p.fecha_programada = ? ORDER BY p.id_plantilla")) {
            ps.setString(1, fecha);
            return ejecutar(ps);
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar plantillas por fecha: " + e.getMessage(), e);
        }
    }

    @Override
    public List<PlantillaOperativa> listarPorEstado(String... estados) {
        if (estados.length == 0) {
            return listar();
        }
        String in = String.join(",", java.util.Collections.nCopies(estados.length, "?"));
        String sql = SELECT_BASE + "WHERE p.estado IN (" + in + ") ORDER BY p.fecha_programada, p.id_plantilla";
        try (Connection c = ConexionBD.obtener(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < estados.length; i++) {
                ps.setString(i + 1, estados[i]);
            }
            return ejecutar(ps);
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar plantillas por estado: " + e.getMessage(), e);
        }
    }

    @Override
    public PlantillaOperativa obtener(int id) {
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(SELECT_BASE + "WHERE p.id_plantilla = ?")) {
            ps.setInt(1, id);
            List<PlantillaOperativa> res = ejecutar(ps);
            return res.isEmpty() ? null : res.get(0);
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener plantilla: " + e.getMessage(), e);
        }
    }

    @Override
    public int guardar(PlantillaOperativa p) {
        String sqlInsert = "INSERT INTO plantilla_operativa (id_cliente, nombre_mercado, fecha_programada, estado, fecha_carga) "
                + "VALUES (?, ?, ?, ?, datetime('now','localtime'))";
        String sqlUpdate = "UPDATE plantilla_operativa SET id_cliente = ?, nombre_mercado = ?, fecha_programada = ? WHERE id_plantilla = ?";
        try (Connection c = ConexionBD.obtener()) {
            if (p.getIdPlantilla() == 0) {
                try (PreparedStatement ps = c.prepareStatement(sqlInsert, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, p.getIdCliente());
                    ps.setString(2, p.getNombreMercado());
                    ps.setString(3, p.getFechaProgramada());
                    ps.setString(4, p.getEstado() == null ? PlantillaOperativa.ESTADO_CARGADA : p.getEstado());
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            p.setIdPlantilla(rs.getInt(1));
                        }
                    }
                    return p.getIdPlantilla();
                }
            }
            try (PreparedStatement ps = c.prepareStatement(sqlUpdate)) {
                ps.setInt(1, p.getIdCliente());
                ps.setString(2, p.getNombreMercado());
                ps.setString(3, p.getFechaProgramada());
                ps.setInt(4, p.getIdPlantilla());
                ps.executeUpdate();
                return p.getIdPlantilla();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al guardar plantilla: " + e.getMessage(), e);
        }
    }

    @Override
    public void actualizarEstado(int idPlantilla, String estado) {
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement("UPDATE plantilla_operativa SET estado = ? WHERE id_plantilla = ?")) {
            ps.setString(1, estado);
            ps.setInt(2, idPlantilla);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar estado de plantilla: " + e.getMessage(), e);
        }
    }

    @Override
    public List<DetallePlantilla> listarDetalle(int idPlantilla) {
        List<DetallePlantilla> lista = new ArrayList<>();
        String sql = "SELECT dp.id_detalle_plantilla, dp.id_plantilla, dp.id_producto, dp.cantidad_solicitada, "
                + "pr.nombre, um.abreviatura "
                + "FROM detalle_plantilla dp "
                + "JOIN producto pr ON pr.id_producto = dp.id_producto "
                + "JOIN unidad_medida um ON um.id_unidad = pr.id_unidad_despacho "
                + "WHERE dp.id_plantilla = ? ORDER BY pr.nombre";
        try (Connection c = ConexionBD.obtener(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idPlantilla);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DetallePlantilla d = new DetallePlantilla(rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getDouble(4));
                    d.setNombreProducto(rs.getString(5));
                    d.setAbreviatura(rs.getString(6));
                    lista.add(d);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar detalle de plantilla: " + e.getMessage(), e);
        }
        return lista;
    }

    @Override
    public void agregarDetalle(DetallePlantilla d) {
        String sql = "INSERT INTO detalle_plantilla (id_plantilla, id_producto, cantidad_solicitada) VALUES (?, ?, ?)";
        try (Connection c = ConexionBD.obtener(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, d.getIdPlantilla());
            ps.setInt(2, d.getIdProducto());
            ps.setDouble(3, d.getCantidadSolicitada());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al agregar producto a la plantilla: " + e.getMessage(), e);
        }
    }

    @Override
    public void eliminarDetalle(int idDetallePlantilla) {
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement("DELETE FROM detalle_plantilla WHERE id_detalle_plantilla = ?")) {
            ps.setInt(1, idDetallePlantilla);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar producto de la plantilla: " + e.getMessage(), e);
        }
    }

    private List<PlantillaOperativa> consultar(String sql) {
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(sql)) {
            return ejecutar(ps);
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar plantillas: " + e.getMessage(), e);
        }
    }

    private List<PlantillaOperativa> ejecutar(PreparedStatement ps) throws SQLException {
        List<PlantillaOperativa> lista = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        }
        return lista;
    }
}
