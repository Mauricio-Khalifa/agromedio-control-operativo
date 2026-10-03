package agromedio.datos;

import agromedio.modelo.Cliente;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ClienteDAOSqlite implements ClienteDAO {

    @Override
    public List<Cliente> listar() {
        List<Cliente> lista = new ArrayList<>();
        String sql = "SELECT id_cliente, nombre_entidad, contacto, telefono FROM cliente ORDER BY nombre_entidad";
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(new Cliente(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4)));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar clientes: " + e.getMessage(), e);
        }
        return lista;
    }

    @Override
    public Cliente obtener(int id) {
        String sql = "SELECT id_cliente, nombre_entidad, contacto, telefono FROM cliente WHERE id_cliente = ?";
        try (Connection c = ConexionBD.obtener(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new Cliente(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4)) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener cliente: " + e.getMessage(), e);
        }
    }

    @Override
    public void guardar(Cliente cliente) {
        String sqlInsert = "INSERT INTO cliente (nombre_entidad, contacto, telefono) VALUES (?, ?, ?)";
        String sqlUpdate = "UPDATE cliente SET nombre_entidad = ?, contacto = ?, telefono = ? WHERE id_cliente = ?";
        try (Connection c = ConexionBD.obtener()) {
            if (cliente.getIdCliente() == 0) {
                try (PreparedStatement ps = c.prepareStatement(sqlInsert, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, cliente.getNombreEntidad());
                    ps.setString(2, cliente.getContacto());
                    ps.setString(3, cliente.getTelefono());
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            cliente.setIdCliente(rs.getInt(1));
                        }
                    }
                }
            } else {
                try (PreparedStatement ps = c.prepareStatement(sqlUpdate)) {
                    ps.setString(1, cliente.getNombreEntidad());
                    ps.setString(2, cliente.getContacto());
                    ps.setString(3, cliente.getTelefono());
                    ps.setInt(4, cliente.getIdCliente());
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al guardar cliente: " + e.getMessage(), e);
        }
    }

    @Override
    public void eliminar(int id) {
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement("DELETE FROM cliente WHERE id_cliente = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo eliminar el cliente (tiene plantillas asociadas): " + e.getMessage(), e);
        }
    }
}
