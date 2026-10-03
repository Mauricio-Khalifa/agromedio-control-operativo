package agromedio.datos;

import agromedio.modelo.UnidadMedida;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UnidadMedidaDAOSqlite implements UnidadMedidaDAO {

    @Override
    public List<UnidadMedida> listar() {
        List<UnidadMedida> lista = new ArrayList<>();
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement("SELECT id_unidad, nombre, abreviatura FROM unidad_medida ORDER BY nombre");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(new UnidadMedida(rs.getInt(1), rs.getString(2), rs.getString(3)));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar unidades: " + e.getMessage(), e);
        }
        return lista;
    }

    @Override
    public void guardar(UnidadMedida unidad) {
        String sqlInsert = "INSERT INTO unidad_medida (nombre, abreviatura) VALUES (?, ?)";
        String sqlUpdate = "UPDATE unidad_medida SET nombre = ?, abreviatura = ? WHERE id_unidad = ?";
        try (Connection c = ConexionBD.obtener()) {
            if (unidad.getIdUnidad() == 0) {
                try (PreparedStatement ps = c.prepareStatement(sqlInsert, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, unidad.getNombre());
                    ps.setString(2, unidad.getAbreviatura());
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            unidad.setIdUnidad(rs.getInt(1));
                        }
                    }
                }
            } else {
                try (PreparedStatement ps = c.prepareStatement(sqlUpdate)) {
                    ps.setString(1, unidad.getNombre());
                    ps.setString(2, unidad.getAbreviatura());
                    ps.setInt(3, unidad.getIdUnidad());
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al guardar unidad de medida: " + e.getMessage(), e);
        }
    }

    @Override
    public void eliminar(int id) {
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement("DELETE FROM unidad_medida WHERE id_unidad = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo eliminar la unidad (esta en uso por productos): " + e.getMessage(), e);
        }
    }
}
