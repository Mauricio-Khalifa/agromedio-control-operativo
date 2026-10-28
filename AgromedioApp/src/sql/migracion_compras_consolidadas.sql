-- Migración idempotente para pedidos consolidados y costos por pedido.
-- Las columnas nuevas se agregan desde ConexionBD solo cuando no existen.
INSERT OR IGNORE INTO unidad_medida (id_unidad, nombre, abreviatura) VALUES (9, 'Gramo', 'g');
INSERT OR IGNORE INTO unidad_medida (id_unidad, nombre, abreviatura) VALUES (10, 'Mililitro', 'ml');
CREATE TABLE IF NOT EXISTS distribucion_compra (
    id_distribucion     INTEGER PRIMARY KEY AUTOINCREMENT,
    id_detalle_compra   INTEGER NOT NULL,
    id_plantilla        INTEGER NOT NULL,
    cantidad_asignada   REAL NOT NULL CHECK (cantidad_asignada > 0),
    costo_asignado      REAL NOT NULL DEFAULT 0 CHECK (costo_asignado >= 0),
    FOREIGN KEY (id_detalle_compra) REFERENCES detalle_compra (id_detalle_compra) ON DELETE CASCADE,
    FOREIGN KEY (id_plantilla) REFERENCES plantilla_operativa (id_plantilla) ON DELETE RESTRICT,
    UNIQUE (id_detalle_compra, id_plantilla)
);
CREATE INDEX IF NOT EXISTS idx_distribucion_detalle ON distribucion_compra (id_detalle_compra);
CREATE INDEX IF NOT EXISTS idx_distribucion_plantilla ON distribucion_compra (id_plantilla);
