package agromedio.datos;

import agromedio.modelo.Proveedor;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProveedorDAOSqlite implements ProveedorDAO {

    @Override
    public List<Proveedor> listar() {
        List<Proveedor> lista = new ArrayList<>();
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement("SELECT id_proveedor, nombre, contacto, telefono FROM proveedor ORDER BY nombre");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(new Proveedor(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4)));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar proveedores: " + e.getMessage(), e);
        }
        return lista;
    }

    @Override
    public Proveedor obtener(int id) {
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement("SELECT id_proveedor, nombre, contacto, telefono FROM proveedor WHERE id_proveedor = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new Proveedor(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4)) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener proveedor: " + e.getMessage(), e);
        }
    }

    @Override
    public void guardar(Proveedor proveedor) {
        String sqlInsert = "INSERT INTO proveedor (nombre, contacto, telefono) VALUES (?, ?, ?)";
        String sqlUpdate = "UPDATE proveedor SET nombre = ?, contacto = ?, telefono = ? WHERE id_proveedor = ?";
        try (Connection c = ConexionBD.obtener()) {
            if (proveedor.getIdProveedor() == 0) {
                try (PreparedStatement ps = c.prepareStatement(sqlInsert, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, proveedor.getNombre());
                    ps.setString(2, proveedor.getContacto());
                    ps.setString(3, proveedor.getTelefono());
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            proveedor.setIdProveedor(rs.getInt(1));
                        }
                    }
                }
            } else {
                try (PreparedStatement ps = c.prepareStatement(sqlUpdate)) {
                    ps.setString(1, proveedor.getNombre());
                    ps.setString(2, proveedor.getContacto());
                    ps.setString(3, proveedor.getTelefono());
                    ps.setInt(4, proveedor.getIdProveedor());
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al guardar proveedor: " + e.getMessage(), e);
        }
    }

    @Override
    public void eliminar(int id) {
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement("DELETE FROM proveedor WHERE id_proveedor = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo eliminar el proveedor (tiene compras asociadas): " + e.getMessage(), e);
        }
    }
}
