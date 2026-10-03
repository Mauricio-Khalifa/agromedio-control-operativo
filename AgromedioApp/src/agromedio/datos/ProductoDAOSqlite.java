package agromedio.datos;

import agromedio.modelo.Producto;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductoDAOSqlite implements ProductoDAO {

    private static final String SELECT_BASE =
            "SELECT p.id_producto, p.nombre, p.id_categoria, p.id_unidad_compra, p.id_unidad_despacho, "
          + "p.factor_conversion, c.nombre, uc.nombre, uc.abreviatura, ud.nombre, ud.abreviatura "
          + "FROM producto p "
          + "JOIN categoria c       ON c.id_categoria = p.id_categoria "
          + "JOIN unidad_medida uc  ON uc.id_unidad = p.id_unidad_compra "
          + "JOIN unidad_medida ud  ON ud.id_unidad = p.id_unidad_despacho ";

    private static Producto mapear(ResultSet rs) throws SQLException {
        Producto p = new Producto(rs.getInt(1), rs.getString(2), rs.getInt(3), rs.getInt(4),
                rs.getInt(5), rs.getDouble(6));
        p.setNombreCategoria(rs.getString(7));
        p.setNombreUnidadCompra(rs.getString(8));
        p.setAbreviaturaCompra(rs.getString(9));
        p.setNombreUnidadDespacho(rs.getString(10));
        p.setAbreviaturaDespacho(rs.getString(11));
        return p;
    }

    @Override
    public List<Producto> listar() {
        List<Producto> lista = new ArrayList<>();
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(SELECT_BASE + "ORDER BY p.nombre");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar productos: " + e.getMessage(), e);
        }
        return lista;
    }

    @Override
    public Producto obtener(int id) {
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(SELECT_BASE + "WHERE p.id_producto = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener producto: " + e.getMessage(), e);
        }
    }

    @Override
    public Producto buscarPorNombre(String nombre) {
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(SELECT_BASE + "WHERE LOWER(p.nombre) = LOWER(?)")) {
            ps.setString(1, nombre == null ? "" : nombre.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar producto: " + e.getMessage(), e);
        }
    }

    @Override
    public void guardar(Producto producto) {
        String sqlInsert = "INSERT INTO producto (nombre, id_categoria, id_unidad_compra, id_unidad_despacho, factor_conversion) "
                + "VALUES (?, ?, ?, ?, ?)";
        String sqlUpdate = "UPDATE producto SET nombre = ?, id_categoria = ?, id_unidad_compra = ?, "
                + "id_unidad_despacho = ?, factor_conversion = ? WHERE id_producto = ?";
        try (Connection c = ConexionBD.obtener()) {
            if (producto.getIdProducto() == 0) {
                try (PreparedStatement ps = c.prepareStatement(sqlInsert, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, producto.getNombre());
                    ps.setInt(2, producto.getIdCategoria());
                    ps.setInt(3, producto.getIdUnidadCompra());
                    ps.setInt(4, producto.getIdUnidadDespacho());
                    ps.setDouble(5, producto.getFactorConversion());
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            producto.setIdProducto(rs.getInt(1));
                        }
                    }
                }
            } else {
                try (PreparedStatement ps = c.prepareStatement(sqlUpdate)) {
                    ps.setString(1, producto.getNombre());
                    ps.setInt(2, producto.getIdCategoria());
                    ps.setInt(3, producto.getIdUnidadCompra());
                    ps.setInt(4, producto.getIdUnidadDespacho());
                    ps.setDouble(5, producto.getFactorConversion());
                    ps.setInt(6, producto.getIdProducto());
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al guardar producto: " + e.getMessage(), e);
        }
    }

    @Override
    public void eliminar(int id) {
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement("DELETE FROM producto WHERE id_producto = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo eliminar el producto (ya se usa en el ciclo operativo): " + e.getMessage(), e);
        }
    }
}
