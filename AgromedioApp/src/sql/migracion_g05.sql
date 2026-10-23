-- ---------------------------------------------------------------------
-- Migracion G05 (RNF02): identificador unico de carga/ciclo
-- La columna plantilla_operativa.id_carga se agrega desde ConexionBD
-- porque SQLite requiere verificar su existencia antes del ALTER TABLE.
-- ---------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS carga_plantilla (
    id_carga                INTEGER PRIMARY KEY AUTOINCREMENT,
    fecha_carga             TEXT NOT NULL DEFAULT (datetime('now','localtime')),
    nombre_archivo          TEXT NOT NULL,
    hash_archivo            TEXT NOT NULL UNIQUE,
    usuario                 TEXT,
    plantillas_creadas      INTEGER NOT NULL DEFAULT 0 CHECK (plantillas_creadas >= 0),
    productos_registrados   INTEGER NOT NULL DEFAULT 0 CHECK (productos_registrados >= 0)
) STRICT;
