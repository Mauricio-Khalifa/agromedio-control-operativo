package agromedio.datos;

import agromedio.modelo.Categoria;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CategoriaDAOSqlite implements CategoriaDAO {

    @Override
    public List<Categoria> listar() {
        List<Categoria> lista = new ArrayList<>();
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement("SELECT id_categoria, nombre FROM categoria ORDER BY nombre");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(new Categoria(rs.getInt(1), rs.getString(2)));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar categorias: " + e.getMessage(), e);
        }
        return lista;
    }

    @Override
    public void guardar(Categoria categoria) {
        String sqlInsert = "INSERT INTO categoria (nombre) VALUES (?)";
        String sqlUpdate = "UPDATE categoria SET nombre = ? WHERE id_categoria = ?";
        try (Connection c = ConexionBD.obtener()) {
            if (categoria.getIdCategoria() == 0) {
                try (PreparedStatement ps = c.prepareStatement(sqlInsert, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, categoria.getNombre());
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            categoria.setIdCategoria(rs.getInt(1));
                        }
                    }
                }
            } else {
                try (PreparedStatement ps = c.prepareStatement(sqlUpdate)) {
                    ps.setString(1, categoria.getNombre());
                    ps.setInt(2, categoria.getIdCategoria());
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al guardar categoria: " + e.getMessage(), e);
        }
    }

    @Override
    public void eliminar(int id) {
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement("DELETE FROM categoria WHERE id_categoria = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo eliminar la categoria (tiene productos asociados): " + e.getMessage(), e);
        }
    }
}
