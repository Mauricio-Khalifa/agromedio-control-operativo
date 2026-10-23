package agromedio.datos;

import agromedio.modelo.CargaPlantilla;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class CargaPlantillaDAOSqlite implements CargaPlantillaDAO {

    private static final String SELECT_BASE =
            "SELECT id_carga, fecha_carga, nombre_archivo, hash_archivo, usuario, "
          + "plantillas_creadas, productos_registrados FROM carga_plantilla ";

    private static CargaPlantilla mapear(ResultSet rs) throws SQLException {
        return new CargaPlantilla(rs.getInt(1), rs.getString(2), rs.getString(3),
                rs.getString(4), rs.getString(5), rs.getInt(6), rs.getInt(7));
    }

    @Override
    public CargaPlantilla buscarPorHash(String hashArchivo) {
        if (hashArchivo == null || hashArchivo.isBlank()) {
            return null;
        }
        String sql = SELECT_BASE + "WHERE hash_archivo = ?";
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, hashArchivo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar carga por hash: " + e.getMessage(), e);
        }
    }

    @Override
    public CargaPlantilla obtener(int idCarga) {
        String sql = SELECT_BASE + "WHERE id_carga = ?";
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idCarga);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener carga: " + e.getMessage(), e);
        }
    }

    @Override
    public int guardar(CargaPlantilla carga) {
        String sql = "INSERT INTO carga_plantilla "
                + "(fecha_carga, nombre_archivo, hash_archivo, usuario, plantillas_creadas, productos_registrados) "
                + "VALUES (datetime('now','localtime'), ?, ?, ?, ?, ?)";
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, carga.getNombreArchivo());
            ps.setString(2, carga.getHashArchivo());
            ps.setString(3, carga.getUsuario());
            ps.setInt(4, carga.getPlantillasCreadas());
            ps.setInt(5, carga.getProductosRegistrados());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    carga.setIdCarga(rs.getInt(1));
                }
            }
            return carga.getIdCarga();
        } catch (SQLException e) {
            throw new RuntimeException("Error al guardar la carga: " + e.getMessage(), e);
        }
    }

    @Override
    public void actualizarResumen(int idCarga, int plantillasCreadas, int productosRegistrados) {
        String sql = "UPDATE carga_plantilla SET plantillas_creadas = ?, productos_registrados = ? "
                + "WHERE id_carga = ?";
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, plantillasCreadas);
            ps.setInt(2, productosRegistrados);
            ps.setInt(3, idCarga);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar el resumen de la carga: " + e.getMessage(), e);
        }
    }
}
