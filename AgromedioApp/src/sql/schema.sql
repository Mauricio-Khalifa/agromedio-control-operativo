-- =====================================================================
-- SISTEMA DE CONTROL OPERATIVO AGROMEDIO
-- Script de creacion del esquema - Motor SQLite (STRICT tables)
-- Proyecto Integrador UT Santander - 2026
-- Basado en el Diagrama Entidad-Relacion aprobado por el cliente
-- =====================================================================

PRAGMA foreign_keys = ON;

-- ---------------------------------------------------------------------
-- ELIMINACION EN ORDEN DE DEPENDENCIA (reset del esquema)
-- ---------------------------------------------------------------------
DROP VIEW IF EXISTS v_avance_despacho;
DROP VIEW IF EXISTS v_faltantes_recepcion;
DROP VIEW IF EXISTS v_saldo_plantilla;
DROP TRIGGER IF EXISTS trg_detalle_recepcion_faltante;
DROP TABLE IF EXISTS detalle_despacho;
DROP TABLE IF EXISTS despacho;
DROP TABLE IF EXISTS detalle_recepcion;
DROP TABLE IF EXISTS recepcion;
DROP TABLE IF EXISTS detalle_compra;
DROP TABLE IF EXISTS compra;
DROP TABLE IF EXISTS detalle_alistamiento;
DROP TABLE IF EXISTS alistamiento;
DROP TABLE IF EXISTS detalle_plantilla;
DROP TABLE IF EXISTS plantilla_operativa;
DROP TABLE IF EXISTS producto;
DROP TABLE IF EXISTS proveedor;
DROP TABLE IF EXISTS unidad_medida;
DROP TABLE IF EXISTS categoria;
DROP TABLE IF EXISTS cliente;

-- ---------------------------------------------------------------------
-- CATALOGOS
-- ---------------------------------------------------------------------
CREATE TABLE cliente (
    id_cliente      INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre_entidad  TEXT NOT NULL,
    contacto        TEXT,
    telefono        TEXT
) STRICT;

CREATE TABLE categoria (
    id_categoria    INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre          TEXT NOT NULL UNIQUE
) STRICT;

CREATE TABLE unidad_medida (
    id_unidad       INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre          TEXT NOT NULL UNIQUE,
    abreviatura     TEXT NOT NULL UNIQUE
) STRICT;

CREATE TABLE proveedor (
    id_proveedor    INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre          TEXT NOT NULL,
    contacto        TEXT,
    telefono        TEXT
) STRICT;

CREATE TABLE producto (
    id_producto         INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre              TEXT NOT NULL UNIQUE,
    id_categoria        INTEGER NOT NULL,
    id_unidad_compra    INTEGER NOT NULL,
    id_unidad_despacho  INTEGER NOT NULL,
    factor_conversion   REAL NOT NULL CHECK (factor_conversion > 0),
    FOREIGN KEY (id_categoria)       REFERENCES categoria (id_categoria),
    FOREIGN KEY (id_unidad_compra)   REFERENCES unidad_medida (id_unidad),
    FOREIGN KEY (id_unidad_despacho) REFERENCES unidad_medida (id_unidad)
) STRICT;

-- ---------------------------------------------------------------------
-- CICLO OPERATIVO: PLANTILLA (carga administrativa) -> DETALLE
-- ---------------------------------------------------------------------
CREATE TABLE plantilla_operativa (
    id_plantilla        INTEGER PRIMARY KEY AUTOINCREMENT,
    id_cliente          INTEGER NOT NULL,
    nombre_mercado      TEXT NOT NULL,
    fecha_programada    TEXT NOT NULL,
    estado              TEXT NOT NULL DEFAULT 'CARGADA'
                        CHECK (estado IN ('CARGADA','EN_COMPRA','RECIBIENDO','ALISTANDO','LISTA','DESPACHADA')),
    fecha_carga         TEXT NOT NULL DEFAULT (datetime('now','localtime')),
    FOREIGN KEY (id_cliente) REFERENCES cliente (id_cliente)
) STRICT;

CREATE TABLE detalle_plantilla (
    id_detalle_plantilla    INTEGER PRIMARY KEY AUTOINCREMENT,
    id_plantilla            INTEGER NOT NULL,
    id_producto             INTEGER NOT NULL,
    cantidad_solicitada     REAL NOT NULL CHECK (cantidad_solicitada > 0),
    FOREIGN KEY (id_plantilla) REFERENCES plantilla_operativa (id_plantilla) ON DELETE CASCADE,
    FOREIGN KEY (id_producto)  REFERENCES producto (id_producto)
) STRICT;

-- ---------------------------------------------------------------------
-- MODULO 3: ALISTAMIENTO / PRODUCCION (fraccionamiento)
-- ---------------------------------------------------------------------
CREATE TABLE alistamiento (
    id_alistamiento     INTEGER PRIMARY KEY AUTOINCREMENT,
    id_plantilla        INTEGER NOT NULL,
    fecha_alistamiento  TEXT NOT NULL,
    estado              TEXT NOT NULL DEFAULT 'EN_PROCESO'
                        CHECK (estado IN ('EN_PROCESO','COMPLETO')),
    FOREIGN KEY (id_plantilla) REFERENCES plantilla_operativa (id_plantilla) ON DELETE CASCADE
) STRICT;

CREATE TABLE detalle_alistamiento (
    id_detalle_alistamiento INTEGER PRIMARY KEY AUTOINCREMENT,
    id_alistamiento         INTEGER NOT NULL,
    id_producto             INTEGER NOT NULL,
    cantidad_alistada       REAL NOT NULL CHECK (cantidad_alistada >= 0),
    completado              INTEGER NOT NULL DEFAULT 0 CHECK (completado IN (0,1)),
    FOREIGN KEY (id_alistamiento) REFERENCES alistamiento (id_alistamiento) ON DELETE CASCADE,
    FOREIGN KEY (id_producto)     REFERENCES producto (id_producto)
) STRICT;

-- ---------------------------------------------------------------------
-- MODULO 1: COMPRAS
-- ---------------------------------------------------------------------
CREATE TABLE compra (
    id_compra       INTEGER PRIMARY KEY AUTOINCREMENT,
    id_plantilla    INTEGER NOT NULL,
    fecha_compra    TEXT NOT NULL,
    estado          TEXT NOT NULL DEFAULT 'REGISTRADA'
                    CHECK (estado IN ('REGISTRADA','RECIBIDA','ANULADA')),
    FOREIGN KEY (id_plantilla) REFERENCES plantilla_operativa (id_plantilla) ON DELETE CASCADE
) STRICT;

CREATE TABLE detalle_compra (
    id_detalle_compra   INTEGER PRIMARY KEY AUTOINCREMENT,
    id_compra           INTEGER NOT NULL,
    id_producto         INTEGER NOT NULL,
    id_proveedor        INTEGER NOT NULL,
    cantidad_comprada   REAL NOT NULL CHECK (cantidad_comprada > 0),
    FOREIGN KEY (id_compra)    REFERENCES compra (id_compra) ON DELETE CASCADE,
    FOREIGN KEY (id_producto)  REFERENCES producto (id_producto),
    FOREIGN KEY (id_proveedor) REFERENCES proveedor (id_proveedor)
) STRICT;

-- ---------------------------------------------------------------------
-- MODULO 2: RECEPCION
-- ---------------------------------------------------------------------
CREATE TABLE recepcion (
    id_recepcion    INTEGER PRIMARY KEY AUTOINCREMENT,
    id_compra       INTEGER NOT NULL,
    fecha_recepcion TEXT NOT NULL,
    observaciones   TEXT,
    FOREIGN KEY (id_compra) REFERENCES compra (id_compra) ON DELETE CASCADE
) STRICT;

CREATE TABLE detalle_recepcion (
    id_detalle_recepcion    INTEGER PRIMARY KEY AUTOINCREMENT,
    id_recepcion            INTEGER NOT NULL,
    id_detalle_compra       INTEGER NOT NULL,
    cantidad_recibida       REAL NOT NULL CHECK (cantidad_recibida >= 0),
    cantidad_faltante       REAL NOT NULL DEFAULT 0 CHECK (cantidad_faltante >= 0),
    FOREIGN KEY (id_recepcion)      REFERENCES recepcion (id_recepcion) ON DELETE CASCADE,
    FOREIGN KEY (id_detalle_compra) REFERENCES detalle_compra (id_detalle_compra)
) STRICT;

-- RF06: el faltante lo calcula y garantiza la base de datos
CREATE TRIGGER trg_detalle_recepcion_faltante
AFTER INSERT ON detalle_recepcion
FOR EACH ROW
BEGIN
    UPDATE detalle_recepcion
    SET cantidad_faltante = MAX(0, ROUND(
            (SELECT dc.cantidad_comprada
               FROM detalle_compra dc
              WHERE dc.id_detalle_compra = NEW.id_detalle_compra)
            - NEW.cantidad_recibida, 2))
    WHERE id_detalle_recepcion = NEW.id_detalle_recepcion;
END;

-- ---------------------------------------------------------------------
-- MODULO 4: DESPACHO (lista de chequeo)
-- ---------------------------------------------------------------------
CREATE TABLE despacho (
    id_despacho     INTEGER PRIMARY KEY AUTOINCREMENT,
    id_plantilla    INTEGER NOT NULL,
    fecha_despacho  TEXT NOT NULL,
    estado          TEXT NOT NULL DEFAULT 'EN_VERIFICACION'
                    CHECK (estado IN ('EN_VERIFICACION','DESPACHADA')),
    novedades       TEXT,
    FOREIGN KEY (id_plantilla) REFERENCES plantilla_operativa (id_plantilla) ON DELETE CASCADE
) STRICT;

CREATE TABLE detalle_despacho (
    id_detalle_despacho     INTEGER PRIMARY KEY AUTOINCREMENT,
    id_despacho             INTEGER NOT NULL,
    id_producto             INTEGER NOT NULL,
    cantidad_despachada     REAL NOT NULL CHECK (cantidad_despachada >= 0),
    verificado              INTEGER NOT NULL DEFAULT 0 CHECK (verificado IN (0,1)),
    FOREIGN KEY (id_despacho) REFERENCES despacho (id_despacho) ON DELETE CASCADE,
    FOREIGN KEY (id_producto) REFERENCES producto (id_producto)
) STRICT;

-- ---------------------------------------------------------------------
-- INDICES (desempeno - RNF03)
-- ---------------------------------------------------------------------
CREATE INDEX idx_detalle_plantilla_planta ON detalle_plantilla (id_plantilla);
CREATE INDEX idx_detalle_plantilla_prod   ON detalle_plantilla (id_producto);
CREATE INDEX idx_compra_plantilla         ON compra (id_plantilla);
CREATE INDEX idx_det_compra_compra        ON detalle_compra (id_compra);
CREATE INDEX idx_recepcion_compra         ON recepcion (id_compra);
CREATE INDEX idx_det_recepcion_rec        ON detalle_recepcion (id_recepcion);
CREATE INDEX idx_det_recepcion_detcomp    ON detalle_recepcion (id_detalle_compra);
CREATE INDEX idx_despacho_plantilla       ON despacho (id_plantilla);
CREATE INDEX idx_det_despacho_desp        ON detalle_despacho (id_despacho);
CREATE INDEX idx_alistamiento_plantilla   ON alistamiento (id_plantilla);

-- ---------------------------------------------------------------------
-- VISTAS DE CONSULTA / REPORTES
-- ---------------------------------------------------------------------

-- Saldo integral de una plantilla: solicitado vs alistado vs comprado
-- vs recibido vs despachado (cantidades expresadas en unidad de despacho)
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
LEFT JOIN (
    SELECT a.id_plantilla, da.id_producto, SUM(da.cantidad_alistada) AS alistado
    FROM alistamiento a
    JOIN detalle_alistamiento da ON da.id_alistamiento = a.id_alistamiento
    GROUP BY a.id_plantilla, da.id_producto
) al ON al.id_plantilla = pl.id_plantilla AND al.id_producto = det.id_producto
LEFT JOIN (
    SELECT c.id_plantilla, dc.id_producto,
           ROUND(SUM(dc.cantidad_comprada * pr2.factor_conversion), 2) AS comprado
    FROM compra c
    JOIN detalle_compra dc ON dc.id_compra = c.id_compra
    JOIN producto pr2      ON pr2.id_producto = dc.id_producto
    GROUP BY c.id_plantilla, dc.id_producto
) c ON c.id_plantilla = pl.id_plantilla AND c.id_producto = det.id_producto
LEFT JOIN (
    SELECT c.id_plantilla, dc.id_producto,
           ROUND(SUM(dr.cantidad_recibida * pr2.factor_conversion), 2) AS recibido
    FROM compra c
    JOIN detalle_compra dc     ON dc.id_compra = c.id_compra
    JOIN detalle_recepcion dr  ON dr.id_detalle_compra = dc.id_detalle_compra
    JOIN producto pr2          ON pr2.id_producto = dc.id_producto
    GROUP BY c.id_plantilla, dc.id_producto
) r ON r.id_plantilla = pl.id_plantilla AND r.id_producto = det.id_producto
LEFT JOIN (
    SELECT de.id_plantilla, dd.id_producto, SUM(dd.cantidad_despachada) AS despachado
    FROM despacho de
    JOIN detalle_despacho dd ON dd.id_despacho = de.id_despacho
    GROUP BY de.id_plantilla, dd.id_producto
) d ON d.id_plantilla = pl.id_plantilla AND d.id_producto = det.id_producto;

-- Faltantes de recepcion (RF06) en unidad de compra
CREATE VIEW v_faltantes_recepcion AS
SELECT
    r.id_recepcion,
    r.fecha_recepcion,
    c.id_compra,
    c.id_plantilla,
    pl.nombre_mercado,
    dc.id_detalle_compra,
    pr.nombre                           AS producto,
    pv.nombre                           AS proveedor,
    umc.abreviatura                     AS unidad_compra,
    dc.cantidad_comprada,
    dr.cantidad_recibida,
    dr.cantidad_faltante
FROM detalle_recepcion dr
JOIN recepcion r       ON r.id_recepcion = dr.id_recepcion
JOIN compra c          ON c.id_compra = r.id_compra
JOIN plantilla_operativa pl ON pl.id_plantilla = c.id_plantilla
JOIN detalle_compra dc ON dc.id_detalle_compra = dr.id_detalle_compra
JOIN producto pr       ON pr.id_producto = dc.id_producto
JOIN proveedor pv      ON pv.id_proveedor = dc.id_proveedor
JOIN unidad_medida umc ON umc.id_unidad = pr.id_unidad_compra
WHERE dr.cantidad_faltante > 0;

-- Avance del despacho por mercado (RF10)
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
         ELSE 0 END                                                      AS porcentaje
FROM despacho de
JOIN plantilla_operativa pl ON pl.id_plantilla = de.id_plantilla
LEFT JOIN detalle_despacho dd ON dd.id_despacho = de.id_despacho
GROUP BY de.id_despacho;
