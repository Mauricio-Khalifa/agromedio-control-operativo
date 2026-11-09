package agromedio.datos;

import agromedio.modelo.FilaAvance;
import agromedio.modelo.FilaConsolidado;
import agromedio.modelo.FilaFaltante;
import agromedio.modelo.FilaProgreso;
import agromedio.modelo.FilaSaldo;
import agromedio.modelo.ResumenProgreso;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ConsultaDAOSqlite implements ConsultaDAO {

    /** Saldo por plantilla y producto, tomado de la vista v_saldo_plantilla. */
    private static final String SQL_SALDO =
            "SELECT id_plantilla, nombre_mercado, fecha_programada, estado, id_producto, producto, unidad_despacho, "
          + "solicitado, alistado, comprado, recibido, despachado, por_despachar "
          + "FROM v_saldo_plantilla ";

    /**
     * Procesos abiertos: una fila por plantilla con la etapa real y los
     * porcentajes de compra, recepcion, alistamiento y despacho.
     * Los saldos salen de v_saldo_plantilla para que las compras consolidadas
     * (distribuidas por distribucion_compra) se atribuyan a cada mercado.
     */
    private static final String SQL_PROGRESO =
            "WITH saldo AS ("
          + " SELECT id_plantilla,"
          + "        SUM(solicitado) AS solicitado,"
          + "        SUM(comprado)   AS comprado,"
          + "        SUM(recibido)   AS recibido,"
          + "        SUM(alistado)   AS alistado"
          + " FROM v_saldo_plantilla GROUP BY id_plantilla"
          + "), plantillas_compra AS ("
          + " SELECT c.id_compra, c.id_plantilla AS id_plantilla"
          + " FROM compra c"
          + " WHERE c.estado <> 'ANULADA' AND c.id_plantilla IS NOT NULL"
          + " UNION"
          + " SELECT c.id_compra, dcp.id_plantilla"
          + " FROM compra c"
          + " JOIN detalle_compra dc ON dc.id_compra = c.id_compra"
          + " JOIN distribucion_compra dcp ON dcp.id_detalle_compra = dc.id_detalle_compra"
          + " WHERE c.estado <> 'ANULADA'"
          + "), recepcion_resumen AS ("
          + " SELECT pc.id_plantilla,"
          + "        MAX(CASE WHEN r.id_recepcion IS NOT NULL THEN 1 ELSE 0 END) AS hubo_recepcion,"
          + "        MAX(CASE WHEN c.estado = 'RECIBIDA' THEN 1 ELSE 0 END) AS cerrada"
          + " FROM compra c"
          + " JOIN plantillas_compra pc ON pc.id_compra = c.id_compra"
          + " LEFT JOIN recepcion r ON r.id_compra = c.id_compra"
          + " WHERE c.estado <> 'ANULADA'"
          + " GROUP BY pc.id_plantilla"
          + "), alistamiento_resumen AS ("
          + " SELECT a.id_plantilla,"
          + "        MAX(CASE WHEN a.estado = 'COMPLETO' THEN 1 ELSE 0 END) AS completo,"
          + "        MAX(CASE WHEN a.id_alistamiento IS NOT NULL THEN 1 ELSE 0 END) AS existe"
          + " FROM alistamiento a GROUP BY a.id_plantilla"
          + "), despacho_resumen AS ("
          + " SELECT d.id_plantilla,"
          + "        MAX(d.estado) AS estado,"
          + "        MAX(CASE WHEN TRIM(COALESCE(d.novedades, '')) <> '' THEN 1 ELSE 0 END) AS novedad,"
          + "        MAX(CASE WHEN d.estado = 'DESPACHADA' THEN 1 ELSE 0 END) AS cerrado,"
          + "        MAX(COALESCE(v.porcentaje, 0)) AS porcentaje"
          + " FROM despacho d LEFT JOIN v_avance_despacho v ON v.id_despacho = d.id_despacho"
          + " GROUP BY d.id_plantilla"
          + "), base AS ("
          + " SELECT p.id_plantilla, p.nombre_mercado, p.fecha_carga, p.fecha_programada,"
          + "        p.estado AS estado_guardado,"
          + "        COALESCE(s.solicitado, 0) AS solicitado,"
          + "        COALESCE(s.comprado, 0)   AS comprado,"
          + "        COALESCE(s.recibido, 0)   AS recibido,"
          + "        COALESCE(s.alistado, 0)   AS alistado,"
          + "        COALESCE(rec.hubo_recepcion, 0) AS hub_recepcion,"
          + "        COALESCE(rec.cerrada, 0)        AS recepcion_cerrada,"
          + "        COALESCE(als.existe, 0)         AS alistamiento_existe,"
          + "        COALESCE(als.completo, 0)       AS alistamiento_completo,"
          + "        COALESCE(d.cerrado, 0)          AS despacho_cerrado,"
          + "        MAX(d.estado)                   AS estado_despacho,"
          + "        COALESCE(MAX(d.porcentaje), 0)  AS pct_despacho,"
          + "        COALESCE(MAX(d.novedad), 0)     AS tiene_novedad,"
          + "        CASE WHEN EXISTS ("
          + "            SELECT 1 FROM v_faltantes_recepcion f"
          + "            WHERE f.id_plantilla = p.id_plantilla"
          + "        ) THEN 1 ELSE 0 END AS tiene_faltante"
          + " FROM plantilla_operativa p"
          + " LEFT JOIN saldo s ON s.id_plantilla = p.id_plantilla"
          + " LEFT JOIN recepcion_resumen rec ON rec.id_plantilla = p.id_plantilla"
          + " LEFT JOIN alistamiento_resumen als ON als.id_plantilla = p.id_plantilla"
          + " LEFT JOIN despacho_resumen d ON d.id_plantilla = p.id_plantilla"
          + " GROUP BY p.id_plantilla, p.nombre_mercado, p.fecha_carga, p.fecha_programada, p.estado,"
          + "          s.solicitado, s.comprado, s.recibido, s.alistado,"
          + "          rec.hubo_recepcion, rec.cerrada, als.existe, als.completo, d.cerrado"
          + ") "
          + "SELECT * FROM ("
          + " SELECT id_plantilla, nombre_mercado, fecha_carga, fecha_programada,"
          + " CASE"
          + "   WHEN despacho_cerrado = 1 OR estado_guardado = 'DESPACHADA' THEN 'DESPACHADA'"
          + "   WHEN alistamiento_completo = 1 OR estado_guardado = 'LISTA' THEN 'LISTA'"
          + "   WHEN alistamiento_existe = 1 THEN 'ALISTANDO'"
          + "   WHEN hub_recepcion = 1 OR recibido > 0 OR estado_guardado IN ('RECIBIENDO', 'ALISTANDO') THEN 'RECIBIENDO'"
          + "   WHEN comprado > 0 OR estado_guardado = 'EN_COMPRA' THEN 'EN_COMPRA'"
          + "   ELSE 'CARGADA'"
          + " END AS estado_real,"
          + " CASE WHEN solicitado <= 0 THEN 0"
          + "      ELSE MIN(100.0, ROUND(100.0 * comprado / solicitado, 1)) END AS pct_compra,"
          + " CASE WHEN comprado <= 0 THEN 0"
          + "      ELSE MIN(100.0, ROUND(100.0 * recibido / comprado, 1)) END AS pct_recepcion,"
          + " CASE WHEN solicitado <= 0 THEN 0"
          + "      ELSE MIN(100.0, ROUND(100.0 * alistado / solicitado, 1)) END AS pct_alistamiento,"
          + " pct_despacho, estado_despacho, tiene_faltante, tiene_novedad"
          + " FROM base"
          + ") r "
          + "WHERE r.estado_real <> 'DESPACHADA' "
          + "ORDER BY CASE r.estado_real "
          + " WHEN 'RECIBIENDO' THEN 1 WHEN 'ALISTANDO' THEN 2 WHEN 'LISTA' THEN 3 "
          + " WHEN 'EN_COMPRA' THEN 4 WHEN 'CARGADA' THEN 5 ELSE 6 END, "
          + " r.fecha_programada, r.nombre_mercado";

    @Override
    public List<FilaProgreso> progresoAbierto() {
        List<FilaProgreso> filas = new ArrayList<>();
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(SQL_PROGRESO);
             ResultSet rs = ps.executeQuery()) {

            // Una sola fila por plantilla (seguridad ante duplicados por JOINs)
            Map<Integer, FilaProgreso> mapaFilas = new LinkedHashMap<>();
            while (rs.next()) {
                int idPlantilla = rs.getInt(1);
                if (mapaFilas.containsKey(idPlantilla)) continue;
                FilaProgreso f = new FilaProgreso();
                f.setIdPlantilla(idPlantilla);
                f.setNombreMercado(rs.getString(2));
                f.setFechaCarga(rs.getString(3));
                f.setFechaProgramada(rs.getString(4));
                f.setEstado(rs.getString(5));
                f.setPorcentajeCompra(rs.getDouble(6));
                f.setPorcentajeRecepcion(rs.getDouble(7));
                f.setPorcentajeAlistamiento(rs.getDouble(8));
                f.setPorcentajeDespacho(rs.getDouble(9));
                f.setEstadoDespacho(rs.getString(10));
                f.setTieneFaltante(rs.getInt(11) == 1);
                f.setTieneNovedad(rs.getInt(12) == 1);
                mapaFilas.put(idPlantilla, f);
            }
            filas.addAll(mapaFilas.values());

        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar procesos abiertos: " + e.getMessage(), e);
        }
        return filas;
    }

    @Override
    public ResumenProgreso resumenProgreso() {
        ResumenProgreso resumen = new ResumenProgreso();
        for (FilaProgreso fila : progresoAbierto()) {
            resumen.setPlantillasAbiertas(resumen.getPlantillasAbiertas() + 1);
            switch (fila.getEstado()) {
                case "CARGADA" -> resumen.setCargadas(resumen.getCargadas() + 1);
                case "EN_COMPRA" -> resumen.setEnCompra(resumen.getEnCompra() + 1);
                case "RECIBIENDO" -> resumen.setRecibiendo(resumen.getRecibiendo() + 1);
                case "ALISTANDO" -> resumen.setAlistando(resumen.getAlistando() + 1);
                case "LISTA" -> resumen.setListas(resumen.getListas() + 1);
                default -> { }
            }
            if ("EN_VERIFICACION".equals(fila.getEstadoDespacho())) {
                resumen.setDespachosEnVerificacion(resumen.getDespachosEnVerificacion() + 1);
            }
            if (fila.isTieneFaltante()) {
                resumen.setRecepcionesConFaltante(resumen.getRecepcionesConFaltante() + 1);
            }
        }
        return resumen;
    }

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
                "WITH demanda AS ("
              + "SELECT CASE WHEN INSTR(pr.nombre, ' (') > 0 THEN SUBSTR(pr.nombre, 1, INSTR(pr.nombre, ' (') - 1) ELSE pr.nombre END AS producto_base, "
              + "pr.id_unidad_despacho, umd.abreviatura AS unidad_despacho, umc.abreviatura AS unidad_compra, "
              + "MIN(pr.id_producto) AS id_producto, MIN(pr.factor_conversion) AS factor_conversion, "
              + "SUM(dp.cantidad_solicitada) AS solicitado, "
              + "COUNT(DISTINCT p.nombre_mercado) AS mercados, "
              + "GROUP_CONCAT(DISTINCT p.nombre_mercado) AS nombres_mercados "
              + "FROM detalle_plantilla dp JOIN plantilla_operativa p ON p.id_plantilla = dp.id_plantilla "
              + "JOIN producto pr ON pr.id_producto = dp.id_producto "
              + "JOIN unidad_medida umd ON umd.id_unidad = pr.id_unidad_despacho "
              + "JOIN unidad_medida umc ON umc.id_unidad = pr.id_unidad_compra "
              + "WHERE p.fecha_programada = ? AND p.estado IN ('CARGADA','EN_COMPRA','RECIBIENDO','ALISTANDO','LISTA') "
              + "GROUP BY producto_base, pr.id_unidad_despacho, umd.abreviatura, umc.abreviatura), "
              + "comprado AS ("
              + "SELECT CASE WHEN INSTR(pr.nombre, ' (') > 0 THEN SUBSTR(pr.nombre, 1, INSTR(pr.nombre, ' (') - 1) ELSE pr.nombre END AS producto_base, "
              + "pr.id_unidad_despacho, SUM(dcp.cantidad_asignada * pr.factor_conversion) AS cantidad "
              + "FROM compra c JOIN detalle_compra dc ON dc.id_compra = c.id_compra "
              + "JOIN distribucion_compra dcp ON dcp.id_detalle_compra = dc.id_detalle_compra "
              + "JOIN producto pr ON pr.id_producto = dc.id_producto WHERE c.estado <> 'ANULADA' "
              + "GROUP BY producto_base, pr.id_unidad_despacho) "
              + "SELECT d.id_producto, d.producto_base, d.unidad_despacho, d.unidad_compra, d.factor_conversion, "
              + "d.solicitado, ROUND(d.solicitado / d.factor_conversion, 2), d.mercados, d.nombres_mercados, "
              + "ROUND(COALESCE(c.cantidad, 0), 2) "
              + "FROM demanda d LEFT JOIN comprado c ON c.producto_base = d.producto_base AND c.id_unidad_despacho = d.id_unidad_despacho "
              + "WHERE ROUND(d.solicitado - COALESCE(c.cantidad, 0), 2) > 0 ORDER BY d.producto_base";
        try (Connection c = ConexionBD.obtener(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, normalizarFecha(fechaProgramada));
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
                    f.setNombresMercados(rs.getString(9));
                    f.setTotalComprado(rs.getDouble(10));
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

    /**
     * Acepta la fecha en ISO (aaaa-mm-dd) o en el formato visible de la
     * aplicacion (dd/MM/yyyy) para que el consolidado no devuelva 0 filas
     * cuando el usuario escribe la fecha tal como se ve en las pantallas.
     */
    private static String normalizarFecha(String fecha) {
        if (fecha == null) return null;
        String t = fecha.trim();
        if (t.matches("\\d{1,2}[/-]\\d{1,2}[/-]\\d{4}")) {
            String[] p = t.split("[/-]");
            return String.format("%04d-%02d-%02d",
                    Integer.parseInt(p[2]), Integer.parseInt(p[1]), Integer.parseInt(p[0]));
        }
        return t;
    }

    /**
     * Primera fecha programada de los procesos abiertos que todavia tiene
     * demanda por comprar; si ninguna tiene, la fecha mas proxima abierta y,
     * en ultimo caso, la de hoy. Sirve para que el consolidado muestre datos
     * sin que el usuario tenga que adivinar la fecha.
     */
    @Override
    public String fechaConsolidadoPendiente() {
        List<String> fechas = new ArrayList<>();
        String sqlFechas = "SELECT DISTINCT fecha_programada FROM plantilla_operativa "
                + "WHERE estado <> 'DESPACHADA' ORDER BY fecha_programada";
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(sqlFechas);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) fechas.add(rs.getString(1));
        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar fechas del consolidado: " + e.getMessage(), e);
        }
        for (String fecha : fechas) {
            if (!consolidarDemanda(fecha).isEmpty()) return fecha;
        }
        if (!fechas.isEmpty()) return fechas.get(0);
        return java.time.LocalDate.now().toString();
    }

    @Override
    public List<FilaAvance> avanceDespacho() {
        List<FilaAvance> filas = new ArrayList<>();
        String sql = "SELECT id_despacho, id_plantilla, nombre_mercado, fecha_despacho, estado, items, verificados, "
                + "porcentaje, con_novedad FROM v_avance_despacho ORDER BY fecha_despacho DESC, nombre_mercado";
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
                f.setConNovedad(rs.getInt(9) == 1);
                filas.add(f);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar avance de despacho: " + e.getMessage(), e);
        }
        return filas;
    }
}
