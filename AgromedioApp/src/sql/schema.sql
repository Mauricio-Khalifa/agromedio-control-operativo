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
DROP TABLE IF EXISTS distribucion_compra;
DROP TABLE IF EXISTS detalle_compra;
DROP TABLE IF EXISTS compra;
DROP TABLE IF EXISTS detalle_alistamiento;
DROP TABLE IF EXISTS alistamiento;
DROP TABLE IF EXISTS detalle_plantilla;
DROP TABLE IF EXISTS plantilla_operativa;
DROP TABLE IF EXISTS carga_plantilla;
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
-- CONTROL DE CARGAS (G05 / RNF02)
-- ---------------------------------------------------------------------
CREATE TABLE carga_plantilla (
    id_carga                INTEGER PRIMARY KEY AUTOINCREMENT,
    fecha_carga             TEXT NOT NULL DEFAULT (datetime('now','localtime')),
    nombre_archivo          TEXT NOT NULL,
    hash_archivo            TEXT NOT NULL UNIQUE,
    usuario                 TEXT,
    plantillas_creadas      INTEGER NOT NULL DEFAULT 0 CHECK (plantillas_creadas >= 0),
    productos_registrados   INTEGER NOT NULL DEFAULT 0 CHECK (productos_registrados >= 0)
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
    cantidad_mercados   INTEGER NOT NULL DEFAULT 1 CHECK (cantidad_mercados > 0),
    id_carga            INTEGER,
    FOREIGN KEY (id_cliente) REFERENCES cliente (id_cliente),
    FOREIGN KEY (id_carga)   REFERENCES carga_plantilla (id_carga) ON DELETE RESTRICT
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
    FOREIGN KEY (id_plantilla) REFERENCES plantilla_operativa (id_plantilla) ON DELETE RESTRICT
) STRICT;

CREATE TABLE detalle_alistamiento (
    id_detalle_alistamiento INTEGER PRIMARY KEY AUTOINCREMENT,
    id_alistamiento         INTEGER NOT NULL,
    id_producto             INTEGER NOT NULL,
    cantidad_alistada       REAL NOT NULL CHECK (cantidad_alistada >= 0),
    completado              INTEGER NOT NULL DEFAULT 0 CHECK (completado IN (0,1)),
    CHECK (completado = 0 OR cantidad_alistada > 0),
    FOREIGN KEY (id_alistamiento) REFERENCES alistamiento (id_alistamiento) ON DELETE CASCADE,
    FOREIGN KEY (id_producto)     REFERENCES producto (id_producto)
) STRICT;

-- ---------------------------------------------------------------------
-- MODULO 1: COMPRAS
-- ---------------------------------------------------------------------
CREATE TABLE compra (
    id_compra       INTEGER PRIMARY KEY AUTOINCREMENT,
    id_plantilla    INTEGER,
    id_proveedor    INTEGER,
    fecha_compra    TEXT NOT NULL,
    estado          TEXT NOT NULL DEFAULT 'REGISTRADA'
                    CHECK (estado IN ('REGISTRADA','RECIBIDA','ANULADA')),
    costo_total     REAL NOT NULL DEFAULT 0 CHECK (costo_total >= 0),
    observaciones   TEXT,
    FOREIGN KEY (id_plantilla) REFERENCES plantilla_operativa (id_plantilla) ON DELETE RESTRICT,
    FOREIGN KEY (id_proveedor) REFERENCES proveedor (id_proveedor) ON DELETE RESTRICT
) STRICT;

CREATE TABLE detalle_compra (
    id_detalle_compra   INTEGER PRIMARY KEY AUTOINCREMENT,
    id_compra           INTEGER NOT NULL,
    id_producto         INTEGER NOT NULL,
    id_proveedor        INTEGER NOT NULL,
    cantidad_comprada   REAL NOT NULL CHECK (cantidad_comprada > 0),
    costo_unitario      REAL NOT NULL DEFAULT 0 CHECK (costo_unitario >= 0),
    subtotal            REAL NOT NULL DEFAULT 0 CHECK (subtotal >= 0),
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

CREATE TABLE distribucion_compra (
    id_distribucion     INTEGER PRIMARY KEY AUTOINCREMENT,
    id_detalle_compra   INTEGER NOT NULL,
    id_plantilla        INTEGER NOT NULL,
    cantidad_asignada   REAL NOT NULL CHECK (cantidad_asignada > 0),
    costo_asignado      REAL NOT NULL DEFAULT 0 CHECK (costo_asignado >= 0),
    FOREIGN KEY (id_detalle_compra) REFERENCES detalle_compra (id_detalle_compra) ON DELETE CASCADE,
    FOREIGN KEY (id_plantilla) REFERENCES plantilla_operativa (id_plantilla) ON DELETE RESTRICT,
    UNIQUE (id_detalle_compra, id_plantilla)
) STRICT;

CREATE TABLE detalle_recepcion (
    id_detalle_recepcion    INTEGER PRIMARY KEY AUTOINCREMENT,
    id_recepcion            INTEGER NOT NULL,
    id_detalle_compra       INTEGER NOT NULL,
    cantidad_recibida       REAL NOT NULL CHECK (cantidad_recibida > 0),
    cantidad_faltante       REAL NOT NULL DEFAULT 0 CHECK (cantidad_faltante >= 0),
    FOREIGN KEY (id_recepcion)      REFERENCES recepcion (id_recepcion) ON DELETE CASCADE,
    FOREIGN KEY (id_detalle_compra) REFERENCES detalle_compra (id_detalle_compra)
) STRICT;

-- RF06 / RN-06: el faltante se calcula sobre lo recibido ACUMULADO por detalle
-- de compra, no sobre una entrega aislada. Se actualizan todas las filas del
-- mismo detalle para que la columna refleje el pendiente vigente.
CREATE TRIGGER trg_detalle_recepcion_faltante
AFTER INSERT ON detalle_recepcion
FOR EACH ROW
BEGIN
    UPDATE detalle_recepcion
    SET cantidad_faltante = MAX(0, ROUND(
            (SELECT dc.cantidad_comprada
               FROM detalle_compra dc
              WHERE dc.id_detalle_compra = NEW.id_detalle_compra)
            - (SELECT COALESCE(SUM(dr.cantidad_recibida), 0)
                 FROM detalle_recepcion dr
                WHERE dr.id_detalle_compra = NEW.id_detalle_compra), 2))
    WHERE id_detalle_compra = NEW.id_detalle_compra;
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
    FOREIGN KEY (id_plantilla) REFERENCES plantilla_operativa (id_plantilla) ON DELETE RESTRICT
) STRICT;

CREATE TABLE detalle_despacho (
    id_detalle_despacho     INTEGER PRIMARY KEY AUTOINCREMENT,
    id_despacho             INTEGER NOT NULL,
    id_producto             INTEGER NOT NULL,
    cantidad_despachada     REAL NOT NULL CHECK (cantidad_despachada >= 0),
    verificado              INTEGER NOT NULL DEFAULT 0 CHECK (verificado IN (0,1)),
    faltante_aceptado       INTEGER NOT NULL DEFAULT 0 CHECK (faltante_aceptado IN (0,1)),
    CHECK (verificado = 0 OR cantidad_despachada > 0),
    FOREIGN KEY (id_despacho) REFERENCES despacho (id_despacho) ON DELETE CASCADE,
    FOREIGN KEY (id_producto) REFERENCES producto (id_producto)
) STRICT;

-- RN-12: una plantilla con historico no se puede eliminar. Ademas de las FK
-- RESTRICT, el trigger protege bases migradas donde SQLite no permite alterar
-- la clausula ON DELETE de una FK existente.
CREATE TRIGGER trg_plantilla_historico
BEFORE DELETE ON plantilla_operativa
FOR EACH ROW
WHEN EXISTS (SELECT 1 FROM compra WHERE id_plantilla = OLD.id_plantilla)
  OR EXISTS (SELECT 1 FROM alistamiento WHERE id_plantilla = OLD.id_plantilla)
  OR EXISTS (SELECT 1 FROM despacho WHERE id_plantilla = OLD.id_plantilla)
BEGIN
    SELECT RAISE(ABORT, 'RN-12: no se puede eliminar una plantilla con historico');
END;

-- RN-11 / D02: el cero solo vale como pendiente; no se puede completar ni
-- verificar una cantidad en cero.
CREATE TRIGGER trg_alistamiento_cantidad_ins
BEFORE INSERT ON detalle_alistamiento
FOR EACH ROW
WHEN NEW.completado = 1 AND NEW.cantidad_alistada <= 0
BEGIN
    SELECT RAISE(ABORT, 'RN-11: un producto completado debe tener cantidad mayor que cero');
END;

CREATE TRIGGER trg_alistamiento_cantidad_upd
BEFORE UPDATE OF completado, cantidad_alistada ON detalle_alistamiento
FOR EACH ROW
WHEN NEW.completado = 1 AND NEW.cantidad_alistada <= 0
BEGIN
    SELECT RAISE(ABORT, 'RN-11: un producto completado debe tener cantidad mayor que cero');
END;

CREATE TRIGGER trg_despacho_cantidad_ins
BEFORE INSERT ON detalle_despacho
FOR EACH ROW
WHEN NEW.verificado = 1 AND NEW.cantidad_despachada <= 0
BEGIN
    SELECT RAISE(ABORT, 'RN-11: un producto verificado debe tener cantidad mayor que cero');
END;

CREATE TRIGGER trg_despacho_cantidad_upd
BEFORE UPDATE OF verificado, cantidad_despachada ON detalle_despacho
FOR EACH ROW
WHEN NEW.verificado = 1 AND NEW.cantidad_despachada <= 0
BEGIN
    SELECT RAISE(ABORT, 'RN-11: un producto verificado debe tener cantidad mayor que cero');
END;

-- ---------------------------------------------------------------------
-- INDICES (desempeno - RNF03)
-- ---------------------------------------------------------------------
CREATE INDEX idx_detalle_plantilla_planta ON detalle_plantilla (id_plantilla);
CREATE INDEX idx_detalle_plantilla_prod   ON detalle_plantilla (id_producto);
CREATE INDEX idx_plantilla_carga          ON plantilla_operativa (id_carga);
CREATE INDEX idx_compra_plantilla         ON compra (id_plantilla);
CREATE INDEX idx_det_compra_compra        ON detalle_compra (id_compra);
CREATE INDEX idx_distribucion_detalle     ON distribucion_compra (id_detalle_compra);
CREATE INDEX idx_distribucion_plantilla   ON distribucion_compra (id_plantilla);
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
    SELECT dcp.id_plantilla, dc.id_producto,
           ROUND(SUM(dcp.cantidad_asignada * pr2.factor_conversion), 2) AS comprado
    FROM compra c
    JOIN detalle_compra dc ON dc.id_compra = c.id_compra
    JOIN distribucion_compra dcp ON dcp.id_detalle_compra = dc.id_detalle_compra
    JOIN producto pr2      ON pr2.id_producto = dc.id_producto
    WHERE c.estado <> 'ANULADA'
    GROUP BY dcp.id_plantilla, dc.id_producto
) c ON c.id_plantilla = pl.id_plantilla AND c.id_producto = det.id_producto
LEFT JOIN (
    SELECT dcp.id_plantilla, dc.id_producto,
           ROUND(SUM(dr.cantidad_recibida * pr2.factor_conversion), 2) AS recibido
    FROM compra c
    JOIN detalle_compra dc     ON dc.id_compra = c.id_compra
    JOIN distribucion_compra dcp ON dcp.id_detalle_compra = dc.id_detalle_compra
    JOIN detalle_recepcion dr  ON dr.id_detalle_compra = dc.id_detalle_compra
    JOIN producto pr2          ON pr2.id_producto = dc.id_producto
    WHERE c.estado <> 'ANULADA'
    GROUP BY dcp.id_plantilla, dc.id_producto
) r ON r.id_plantilla = pl.id_plantilla AND r.id_producto = det.id_producto
LEFT JOIN (
    SELECT de.id_plantilla, dd.id_producto, SUM(dd.cantidad_despachada) AS despachado
    FROM despacho de
    JOIN detalle_despacho dd ON dd.id_despacho = de.id_despacho
    GROUP BY de.id_plantilla, dd.id_producto
) d ON d.id_plantilla = pl.id_plantilla AND d.id_producto = det.id_producto;

-- Faltantes acumulados de recepcion (RF06 / RN-06) en unidad de compra.
-- Una sola fila por detalle de compra: no duplica el faltante cuando hay
-- varias entregas parciales.
CREATE VIEW v_faltantes_recepcion AS
SELECT
    MAX(dr.id_recepcion)                AS id_recepcion,
    MAX(r.fecha_recepcion)              AS fecha_recepcion,
    c.id_compra,
    c.id_plantilla,
    pl.nombre_mercado,
    dc.id_detalle_compra,
    pr.nombre                           AS producto,
    pv.nombre                           AS proveedor,
    umc.abreviatura                     AS unidad_compra,
    dc.cantidad_comprada,
    ROUND(COALESCE(SUM(dr.cantidad_recibida), 0), 2) AS cantidad_recibida,
    ROUND(MAX(0, dc.cantidad_comprada - COALESCE(SUM(dr.cantidad_recibida), 0)), 2) AS cantidad_faltante
FROM detalle_compra dc
JOIN compra c          ON c.id_compra = dc.id_compra
JOIN plantilla_operativa pl ON pl.id_plantilla = c.id_plantilla
JOIN producto pr       ON pr.id_producto = dc.id_producto
JOIN proveedor pv      ON pv.id_proveedor = dc.id_proveedor
JOIN unidad_medida umc ON umc.id_unidad = pr.id_unidad_compra
LEFT JOIN detalle_recepcion dr ON dr.id_detalle_compra = dc.id_detalle_compra
LEFT JOIN recepcion r           ON r.id_recepcion = dr.id_recepcion
WHERE EXISTS (SELECT 1 FROM recepcion r2 WHERE r2.id_compra = c.id_compra)
GROUP BY dc.id_detalle_compra, c.id_compra, c.id_plantilla, pl.nombre_mercado,
         pr.nombre, pv.nombre, umc.abreviatura, dc.cantidad_comprada
HAVING COALESCE(SUM(dr.cantidad_recibida), 0) < dc.cantidad_comprada;

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
         ELSE 0 END                                                      AS porcentaje,
    CASE WHEN COALESCE(SUM(CASE WHEN dd.faltante_aceptado = 1 THEN 1 ELSE 0 END), 0) > 0
         THEN 1 ELSE 0 END                                              AS con_novedad
FROM despacho de
JOIN plantilla_operativa pl ON pl.id_plantilla = de.id_plantilla
LEFT JOIN detalle_despacho dd ON dd.id_despacho = de.id_despacho
GROUP BY de.id_despacho;
