-- =====================================================================
-- Migracion G03: faltante aceptado y distincion "completo / con novedad"
-- Se ejecuta solo sobre bases de datos creadas antes de G03. La columna
-- se agrega desde ConexionBD y este guion actualiza la vista.
-- =====================================================================

DROP VIEW IF EXISTS v_avance_despacho;

CREATE VIEW v_avance_despacho AS
SELECT
    de.id_despacho,
    de.id_plantilla,
    pl.nombre_mercado,
    de.fecha_despacho,
    de.estado,
    COUNT(dd.id_detalle_despacho)                                        AS items,
    COALESCE(SUM(CASE WHEN dd.verificado = 1 THEN 1 ELSE 0 END), 0)     AS verificados,
    CASE WHEN COUNT(dd.id_detalle_despacho) > 0
         THEN ROUND(100.0 * SUM(CASE WHEN dd.verificado = 1 THEN 1 ELSE 0 END)
                         / COUNT(dd.id_detalle_despacho), 1)
         ELSE 0 END                                                      AS porcentaje,
    CASE WHEN COALESCE(SUM(CASE WHEN dd.faltante_aceptado = 1 THEN 1 ELSE 0 END), 0) > 0
         THEN 1 ELSE 0 END                                              AS con_novedad
FROM despacho de
JOIN plantilla_operativa pl ON pl.id_plantilla = de.id_plantilla
LEFT JOIN detalle_despacho dd ON dd.id_despacho = de.id_despacho
GROUP BY de.id_despacho;
