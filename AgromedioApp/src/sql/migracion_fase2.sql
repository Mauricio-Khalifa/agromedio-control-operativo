-- Script de Migracion: Fase 2 - Optimizacion de Rendimiento
PRAGMA foreign_keys = ON;

-- 1. Creacion de indices faltantes para optimizar JOINs en las vistas
CREATE INDEX IF NOT EXISTS idx_distribucion_plantilla_prod ON distribucion_compra(id_plantilla);
CREATE INDEX IF NOT EXISTS idx_detalle_compra_prod ON detalle_compra(id_producto);
CREATE INDEX IF NOT EXISTS idx_detalle_despacho_prod ON detalle_despacho(id_despacho, id_producto);
CREATE INDEX IF NOT EXISTS idx_detalle_alistamiento_prod ON detalle_alistamiento(id_alistamiento, id_producto);

-- 2. Recreacion de v_saldo_plantilla optimizada
DROP VIEW IF EXISTS v_saldo_plantilla;

CREATE VIEW v_saldo_plantilla AS
SELECT
    pl.id_plantilla,
    pl.nombre_mercado,
    pl.fecha_programada,
    pl.estado,
    det.id_producto,
    pr.nombre                          AS producto,
    umd.abreviatura                    AS unidad_despacho,
    pr.factor_conversion,
    det.cantidad_solicitada            AS solicitado,
    COALESCE(al.alistado, 0)           AS alistado,
    COALESCE(c.comprado, 0)            AS comprado,
    COALESCE(r.recibido, 0)            AS recibido,
    COALESCE(d.despachado, 0)          AS despachado,
    ROUND(det.cantidad_solicitada - COALESCE(d.despachado, 0), 2) AS por_despachar
FROM detalle_plantilla det
JOIN plantilla_operativa pl ON pl.id_plantilla = det.id_plantilla
JOIN producto pr            ON pr.id_producto = det.id_producto
LEFT JOIN unidad_medida umd ON umd.id_unidad = pr.id_unidad_despacho
-- Optimizacion: Subconsultas agregadas agrupadas por id_plantilla e id_producto
LEFT JOIN (
    SELECT id_plantilla, id_producto, SUM(cantidad_alistada) AS alistado
    FROM alistamiento a
    JOIN detalle_alistamiento da ON da.id_alistamiento = a.id_alistamiento
    GROUP BY id_plantilla, id_producto
) al ON al.id_plantilla = pl.id_plantilla AND al.id_producto = det.id_producto
LEFT JOIN (
    SELECT dcp.id_plantilla, dc.id_producto,
           ROUND(SUM(dcp.cantidad_asignada * pr.factor_conversion), 2) AS comprado
    FROM compra c
    JOIN detalle_compra dc ON dc.id_compra = c.id_compra
    JOIN distribucion_compra dcp ON dcp.id_detalle_compra = dc.id_detalle_compra
    JOIN producto pr ON pr.id_producto = dc.id_producto
    WHERE c.estado <> 'ANULADA'
    GROUP BY dcp.id_plantilla, dc.id_producto
) c ON c.id_plantilla = pl.id_plantilla AND c.id_producto = det.id_producto
LEFT JOIN (
    SELECT dcp.id_plantilla, dc.id_producto,
           ROUND(SUM(dr.cantidad_recibida * pr.factor_conversion), 2) AS recibido
    FROM compra c
    JOIN detalle_compra dc     ON dc.id_compra = c.id_compra
    JOIN distribucion_compra dcp ON dcp.id_detalle_compra = dc.id_detalle_compra
    JOIN detalle_recepcion dr  ON dr.id_detalle_compra = dc.id_detalle_compra
    JOIN producto pr          ON pr.id_producto = dc.id_producto
    WHERE c.estado <> 'ANULADA'
    GROUP BY dcp.id_plantilla, dc.id_producto
) r ON r.id_plantilla = pl.id_plantilla AND r.id_producto = det.id_producto
LEFT JOIN (
    SELECT id_plantilla, id_producto, SUM(cantidad_despachada) AS despachado
    FROM despacho de
    JOIN detalle_despacho dd ON dd.id_despacho = de.id_despacho
    GROUP BY id_plantilla, id_producto
) d ON d.id_plantilla = pl.id_plantilla AND d.id_producto = det.id_producto;
