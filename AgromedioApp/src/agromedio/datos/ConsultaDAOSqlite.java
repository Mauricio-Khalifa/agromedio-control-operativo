package agromedio.datos;

import agromedio.modelo.FilaAvance;
import agromedio.modelo.FilaConsolidado;
import agromedio.modelo.FilaFaltante;
import agromedio.modelo.FilaSaldo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ConsultaDAOSqlite implements ConsultaDAO {

    private static final String SQL_SALDO =
            "SELECT id_plantilla, nombre_mercado, fecha_programada, estado, id_producto, producto, unidad_despacho, "
          + "solicitado, alistado, comprado, recibido, despachado, por_despachar "
          + "FROM v_saldo_plantilla ";

    @Override
    public List<FilaSaldo> saldoPlantilla(int idPlantilla) {
        List<FilaSaldo> filas = new ArrayList<>();
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(SQL_SALDO + "WHERE id_plantilla = ? ORDER BY producto")) {
            ps.setInt(1, idPlantilla);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    filas.add(mapearSaldo(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar saldo: " + e.getMessage(), e);
        }
        return filas;
    }

    @Override
    public List<FilaSaldo> saldoTotal() {
        List<FilaSaldo> filas = new ArrayList<>();
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(SQL_SALDO + "ORDER BY fecha_programada DESC, nombre_mercado, producto")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    filas.add(mapearSaldo(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar saldos: " + e.getMessage(), e);
        }
        return filas;
    }

    private static FilaSaldo mapearSaldo(ResultSet rs) throws SQLException {
        FilaSaldo f = new FilaSaldo();
        f.setIdPlantilla(rs.getInt(1));
        f.setNombreMercado(rs.getString(2));
        f.setFechaProgramada(rs.getString(3));
        f.setEstado(rs.getString(4));
        f.setIdProducto(rs.getInt(5));
        f.setProducto(rs.getString(6));
        f.setUnidadDespacho(rs.getString(7));
        f.setSolicitado(rs.getDouble(8));
        f.setAlistado(rs.getDouble(9));
        f.setComprado(rs.getDouble(10));
        f.setRecibido(rs.getDouble(11));
        f.setDespachado(rs.getDouble(12));
        f.setPorDespachar(rs.getDouble(13));
        return f;
    }

    @Override
    public List<FilaFaltante> faltantesRecepcion() {
        List<FilaFaltante> filas = new ArrayList<>();
        String sql = "SELECT id_recepcion, fecha_recepcion, id_compra, id_plantilla, nombre_mercado, id_detalle_compra, "
                + "producto, proveedor, unidad_compra, cantidad_comprada, cantidad_recibida, cantidad_faltante "
                + "FROM v_faltantes_recepcion ORDER BY fecha_recepcion DESC, producto";
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                FilaFaltante f = new FilaFaltante();
                f.setIdRecepcion(rs.getInt(1));
                f.setFechaRecepcion(rs.getString(2));
                f.setIdCompra(rs.getInt(3));
                f.setIdPlantilla(rs.getInt(4));
                f.setNombreMercado(rs.getString(5));
                f.setIdDetalleCompra(rs.getInt(6));
                f.setProducto(rs.getString(7));
                f.setProveedor(rs.getString(8));
                f.setUnidadCompra(rs.getString(9));
                f.setCantidadComprada(rs.getDouble(10));
                f.setCantidadRecibida(rs.getDouble(11));
                f.setCantidadFaltante(rs.getDouble(12));
                filas.add(f);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar faltantes: " + e.getMessage(), e);
        }
        return filas;
    }

    @Override
    public List<FilaConsolidado> consolidarDemanda(String fechaProgramada) {
        List<FilaConsolidado> filas = new ArrayList<>();
        String sql =
                "SELECT pr.id_producto, pr.nombre, umd.abreviatura, umc.abreviatura, pr.factor_conversion, "
              + "SUM(dp.cantidad_solicitada) AS total_solicitado, "
              + "ROUND(SUM(dp.cantidad_solicitada) / pr.factor_conversion, 2) AS total_unidad_compra, "
              + "COUNT(DISTINCT dp.id_plantilla) AS mercados, "
              + "ROUND(COALESCE(SUM(comp.cantidad), 0), 2) AS total_comprado "
              + "FROM detalle_plantilla dp "
              + "JOIN plantilla_operativa p ON p.id_plantilla = dp.id_plantilla "
              + "JOIN producto pr ON pr.id_producto = dp.id_producto "
              + "JOIN unidad_medida umd ON umd.id_unidad = pr.id_unidad_despacho "
              + "JOIN unidad_medida umc ON umc.id_unidad = pr.id_unidad_compra "
              + "LEFT JOIN ( "
              + "    SELECT c.id_plantilla, dc.id_producto, "
              + "           SUM(dc.cantidad_comprada * pr2.factor_conversion) AS cantidad "
              + "    FROM compra c "
              + "    JOIN detalle_compra dc ON dc.id_compra = c.id_compra "
              + "    JOIN producto pr2 ON pr2.id_producto = dc.id_producto "
              + "    WHERE c.estado <> 'ANULADA' "
              + "    GROUP BY c.id_plantilla, dc.id_producto "
              + ") comp ON comp.id_plantilla = dp.id_plantilla AND comp.id_producto = dp.id_producto "
              + "WHERE p.fecha_programada = ? "
              + "AND p.estado IN ('CARGADA','EN_COMPRA','RECIBIENDO','ALISTANDO','LISTA') "
              + "GROUP BY pr.id_producto "
              + "ORDER BY pr.nombre";
        try (Connection c = ConexionBD.obtener(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, fechaProgramada);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    FilaConsolidado f = new FilaConsolidado();
                    f.setIdProducto(rs.getInt(1));
                    f.setProducto(rs.getString(2));
                    f.setUnidadDespacho(rs.getString(3));
                    f.setUnidadCompra(rs.getString(4));
                    f.setFactorConversion(rs.getDouble(5));
                    f.setTotalSolicitado(rs.getDouble(6));
                    f.setTotalEnUnidadCompra(rs.getDouble(7));
                    f.setMercados(rs.getInt(8));
                    f.setTotalComprado(rs.getDouble(9));
                    f.setPendiente(Math.max(0,
                            Math.round((f.getTotalSolicitado() - f.getTotalComprado()) * 100.0) / 100.0));
                    filas.add(f);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al consolidar demanda: " + e.getMessage(), e);
        }
        return filas;
    }

    @Override
    public List<FilaAvance> avanceDespacho() {
        List<FilaAvance> filas = new ArrayList<>();
        String sql = "SELECT id_despacho, id_plantilla, nombre_mercado, fecha_despacho, estado, items, verificados, porcentaje "
                + "FROM v_avance_despacho ORDER BY fecha_despacho DESC, nombre_mercado";
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                FilaAvance f = new FilaAvance();
                f.setIdDespacho(rs.getInt(1));
                f.setIdPlantilla(rs.getInt(2));
                f.setNombreMercado(rs.getString(3));
                f.setFechaDespacho(rs.getString(4));
                f.setEstado(rs.getString(5));
                f.setItems(rs.getInt(6));
                f.setVerificados(rs.getInt(7));
                f.setPorcentaje(rs.getDouble(8));
                filas.add(f);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar avance de despacho: " + e.getMessage(), e);
        }
        return filas;
    }
}
