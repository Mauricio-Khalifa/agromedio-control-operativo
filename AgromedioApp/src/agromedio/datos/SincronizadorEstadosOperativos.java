package agromedio.datos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Repara estados de plantillas que quedaron atrasados respecto a la evidencia
 * persistida en recepciones, alistamientos y despachos. Necesario para compras
 * consolidadas, cuya relacion con mercados se conserva en distribucion_compra.
 */
public final class SincronizadorEstadosOperativos {

    private SincronizadorEstadosOperativos() {}

    public static void sincronizar() throws SQLException {
        String sql = "UPDATE plantilla_operativa AS p SET estado = CASE "
                + "WHEN EXISTS (SELECT 1 FROM despacho d WHERE d.id_plantilla = p.id_plantilla AND d.estado = 'DESPACHADA') "
                + "THEN 'DESPACHADA' "
                + "WHEN EXISTS (SELECT 1 FROM alistamiento a WHERE a.id_plantilla = p.id_plantilla AND a.estado = 'COMPLETO') "
                + "THEN 'LISTA' "
                + "WHEN EXISTS (SELECT 1 FROM alistamiento a WHERE a.id_plantilla = p.id_plantilla) "
                + "OR EXISTS (SELECT 1 FROM compra c JOIN detalle_compra dc ON dc.id_compra = c.id_compra "
                + "LEFT JOIN distribucion_compra dcp ON dcp.id_detalle_compra = dc.id_detalle_compra "
                + "WHERE c.estado = 'RECIBIDA' AND (c.id_plantilla = p.id_plantilla OR dcp.id_plantilla = p.id_plantilla)) "
                + "THEN 'ALISTANDO' "
                + "WHEN EXISTS (SELECT 1 FROM recepcion r JOIN compra c ON c.id_compra = r.id_compra "
                + "JOIN detalle_compra dc ON dc.id_compra = c.id_compra "
                + "LEFT JOIN distribucion_compra dcp ON dcp.id_detalle_compra = dc.id_detalle_compra "
                + "WHERE c.estado <> 'ANULADA' AND (c.id_plantilla = p.id_plantilla OR dcp.id_plantilla = p.id_plantilla)) "
                + "THEN 'RECIBIENDO' ELSE p.estado END "
                + "WHERE p.estado <> 'DESPACHADA' OR EXISTS ("
                + "SELECT 1 FROM despacho d WHERE d.id_plantilla = p.id_plantilla AND d.estado = 'DESPACHADA')";
        try (Connection c = ConexionBD.obtener(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.executeUpdate();
        }
    }
}
