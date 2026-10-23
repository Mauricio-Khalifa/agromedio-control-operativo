-- ---------------------------------------------------------------------
-- Migracion G04 (RN-12) + G07 (RN-11 / D02)
-- SQLite no permite agregar CHECK ni cambiar ON DELETE con ALTER TABLE,
-- por lo que las bases existentes se protegen con triggers idempotentes.
-- Las bases nuevas ya traen FK RESTRICT y CHECK en schema.sql.
-- ---------------------------------------------------------------------

CREATE TRIGGER IF NOT EXISTS trg_plantilla_historico
BEFORE DELETE ON plantilla_operativa
FOR EACH ROW
WHEN EXISTS (SELECT 1 FROM compra WHERE id_plantilla = OLD.id_plantilla)
  OR EXISTS (SELECT 1 FROM alistamiento WHERE id_plantilla = OLD.id_plantilla)
  OR EXISTS (SELECT 1 FROM despacho WHERE id_plantilla = OLD.id_plantilla)
BEGIN
    SELECT RAISE(ABORT, 'RN-12: no se puede eliminar una plantilla con historico');
END;

CREATE TRIGGER IF NOT EXISTS trg_alistamiento_cantidad_ins
BEFORE INSERT ON detalle_alistamiento
FOR EACH ROW
WHEN NEW.completado = 1 AND NEW.cantidad_alistada <= 0
BEGIN
    SELECT RAISE(ABORT, 'RN-11: un producto completado debe tener cantidad mayor que cero');
END;

CREATE TRIGGER IF NOT EXISTS trg_alistamiento_cantidad_upd
BEFORE UPDATE OF completado, cantidad_alistada ON detalle_alistamiento
FOR EACH ROW
WHEN NEW.completado = 1 AND NEW.cantidad_alistada <= 0
BEGIN
    SELECT RAISE(ABORT, 'RN-11: un producto completado debe tener cantidad mayor que cero');
END;

CREATE TRIGGER IF NOT EXISTS trg_despacho_cantidad_ins
BEFORE INSERT ON detalle_despacho
FOR EACH ROW
WHEN NEW.verificado = 1 AND NEW.cantidad_despachada <= 0
BEGIN
    SELECT RAISE(ABORT, 'RN-11: un producto verificado debe tener cantidad mayor que cero');
END;

CREATE TRIGGER IF NOT EXISTS trg_despacho_cantidad_upd
BEFORE UPDATE OF verificado, cantidad_despachada ON detalle_despacho
FOR EACH ROW
WHEN NEW.verificado = 1 AND NEW.cantidad_despachada <= 0
BEGIN
    SELECT RAISE(ABORT, 'RN-11: un producto verificado debe tener cantidad mayor que cero');
END;
