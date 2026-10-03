-- =====================================================================
-- SISTEMA DE CONTROL OPERATIVO AGROMEDIO
-- Datos iniciales (semilla) para demostracion y pruebas
-- Ejecutar DESPUES de schema.sql
-- =====================================================================
PRAGMA foreign_keys = ON;

-- ---------------------------------------------------------------------
-- CATALOGOS
-- ---------------------------------------------------------------------
INSERT INTO unidad_medida (id_unidad, nombre, abreviatura) VALUES
 (1, 'Litro',    'L'),
 (2, 'Kilogramo','kg'),
 (3, 'Libra',    'lb'),
 (4, 'Bulto',    'Bulto'),
 (5, 'Caja',     'Caja'),
 (6, 'Unidad',   'Und'),
 (7, 'Quintal',  'Qtl'),
 (8, 'Galon',    'Gal');

INSERT INTO categoria (id_categoria, nombre) VALUES
 (1, 'Lacteos'),
 (2, 'Tuberculos y raices'),
 (3, 'Carnes y proteinas'),
 (4, 'Legumbres y cereales'),
 (5, 'Abarrotes'),
 (6, 'Frutas y verduras'),
 (7, 'Huevos');

INSERT INTO cliente (id_cliente, nombre_entidad, contacto, telefono) VALUES
 (1, 'Comedor Escolar La Salle Bucaramanga', 'Martha Rojas',   '6063451234'),
 (2, 'ICBF Regional Santander',              'Carlos Gomez',   '6063389090'),
 (3, 'Poblacion Penitenciaria C-Max',        'Ana Martinez',   '6063700700');

INSERT INTO proveedor (id_proveedor, nombre, contacto, telefono) VALUES
 (1, 'Lacteos del Magdalena S.A.', 'Pedro Lopez',  '6063154455'),
 (2, 'Distribuidora Andes del Centro', 'Sandra Gil', '6063201122'),
 (3, 'Carnes del Oriente',          'Luis Herrera', '6063315566'),
 (4, 'Abastos Proveedores Unidos',  'Rocio Sanabria','6063426677');

-- Producto: unidad_compra vs unidad_despacho + factor_conversion
-- (factor = cuantas unidades de despacho equivale 1 unidad de compra)
INSERT INTO producto (id_producto, nombre, id_categoria, id_unidad_compra, id_unidad_despacho, factor_conversion) VALUES
 (1,  'Leche entera pasteurizada', 1, 1, 1, 1.00),
 (2,  'Queso campesino',           1, 2, 3, 2.20),
 (3,  'Papa criolla',              2, 4, 3, 50.00),
 (4,  'Yuca',                      2, 7, 3, 100.00),
 (5,  'Arroz blanco',              4, 7, 3, 100.00),
 (6,  'Aceite vegetal',            5, 5, 6, 12.00),
 (7,  'Frijol cargamonton',        4, 4, 3, 100.00),
 (8,  'Azucar blanca',             5, 4, 3, 50.00),
 (9,  'Carne de res',              3, 2, 3, 2.20),
 (10, 'Pollo entero',              3, 2, 3, 2.20),
 (11, 'Huevos AA',                 7, 5, 6, 360.00),
 (12, 'Spaghetti',                 5, 5, 6, 20.00),
 (13, 'Sal iodizada',              5, 4, 3, 25.00),
 (14, 'Limon',                     6, 2, 6, 20.00);

-- ---------------------------------------------------------------------
-- CICLO 1 (HOY): flujo COMPLETO con faltante real de 7 litros de leche
--   Plantilla 1 -> Compra 1 (RECIBIDA) -> Recepcion 1 (faltante 7 L)
--   -> Alistamiento 1 (COMPLETO) -> Despacho 1 (DESPACHADA)
-- ---------------------------------------------------------------------
INSERT INTO plantilla_operativa (id_plantilla, id_cliente, nombre_mercado, fecha_programada, estado, fecha_carga) VALUES
 (1, 2, 'Mercado 1 - ICBF Bucaramanga Norte', date('now','localtime'),             'DESPACHADA', datetime('now','localtime','-1 day')),
 (2, 1, 'Mercado 2 - Comedor La Salle',       date('now','localtime','+1 day'),     'EN_COMPRA',  datetime('now','localtime','-6 hour')),
 (3, 3, 'Mercado 3 - Penitenciaria C-Max',    date('now','localtime','+1 day'),     'CARGADA',    datetime('now','localtime','-1 hour'));

INSERT INTO detalle_plantilla (id_detalle_plantilla, id_plantilla, id_producto, cantidad_solicitada) VALUES
 (1,  1, 1, 77),    -- Leche 77 L
 (2,  1, 3, 150),   -- Papa 150 lb
 (3,  1, 5, 200),   -- Arroz 200 lb
 (4,  1, 6, 24),    -- Aceite 24 und
 (5,  1, 9, 44),    -- Carne 44 lb
 (6,  2, 7, 100),   -- Frijol 100 lb
 (7,  2, 8, 50),    -- Azucar 50 lb
 (8,  2, 10, 22),   -- Pollo 22 lb
 (9,  2, 5, 100),   -- Arroz 100 lb  (consolidar con plantilla 3)
 (10, 3, 1, 40),    -- Leche 40 L
 (11, 3, 11, 10),   -- Huevos 10 und (1/36 de caja)
 (12, 3, 12, 15),   -- Spaghetti 15 und
 (13, 3, 5, 100);   -- Arroz 100 lb  (consolidar con plantilla 2)

-- Compra del ciclo 1 (todos los productos, con recepcion parcial)
INSERT INTO compra (id_compra, id_plantilla, fecha_compra, estado) VALUES
 (1, 1, date('now','localtime','-1 day'), 'RECIBIDA'),
 (2, 2, date('now','localtime'),          'REGISTRADA');

INSERT INTO detalle_compra (id_detalle_compra, id_compra, id_producto, id_proveedor, cantidad_comprada) VALUES
 (1, 1, 1,  1, 77),   -- Leche 77 L      (prov. Lacteos)
 (2, 1, 3,  2, 3),    -- Papa 3 bultos   (= 150 lb)
 (3, 1, 5,  2, 2),    -- Arroz 2 quintales (= 200 lb)
 (4, 1, 6,  4, 2),    -- Aceite 2 cajas  (= 24 und)
 (5, 1, 9,  3, 20),   -- Carne 20 kg     (= 44 lb)
 (6, 2, 7,  2, 1),    -- Frijol 1 bulto  (= 100 lb)
 (7, 2, 8,  4, 1),    -- Azucar 1 bulto  (= 50 lb)
 (8, 2, 10, 3, 10),   -- Pollo 10 kg     (= 22 lb)
 (9, 2, 5,  2, 1);    -- Arroz 1 quintal (= 100 lb)

-- Recepcion ciclo 1: el proveedor de leche entrega 70 L de los 77 -> faltante 7
-- (el trigger trg_detalle_recepcion_faltante recalcula y garantiza el faltante)
INSERT INTO recepcion (id_recepcion, id_compra, fecha_recepcion, observaciones) VALUES
 (1, 1, date('now','localtime','-1 day'), 'Entrega parcial de leche: faltan 7 litros por reclamar al proveedor.');

INSERT INTO detalle_recepcion (id_detalle_recepcion, id_recepcion, id_detalle_compra, cantidad_recibida, cantidad_faltante) VALUES
 (1, 1, 1, 70, 0),   -- Leche: 70 de 77 -> faltante 7 (calculado por trigger)
 (2, 1, 2, 3,  0),
 (3, 1, 3, 2,  0),
 (4, 1, 4, 2,  0),
 (5, 1, 5, 20, 0);

-- Alistamiento (produccion) ciclo 1: fraccionamiento completo
INSERT INTO alistamiento (id_alistamiento, id_plantilla, fecha_alistamiento, estado) VALUES
 (1, 1, date('now','localtime','-1 day'), 'COMPLETO');

INSERT INTO detalle_alistamiento (id_detalle_alistamiento, id_alistamiento, id_producto, cantidad_alistada, completado) VALUES
 (1, 1, 1, 70, 1),    -- solo se alisto lo recibido (70 L de 77)
 (2, 1, 3, 150, 1),
 (3, 1, 5, 200, 1),
 (4, 1, 6, 24, 1),
 (5, 1, 9, 44, 1);

-- Despacho ciclo 1: checklist verificado al 100%
INSERT INTO despacho (id_despacho, id_plantilla, fecha_despacho, estado, novedades) VALUES
 (1, 1, date('now','localtime','-1 day'), 'DESPACHADA', 'Novedad: se despacharon 70 L de leche de los 77 solicitados (faltante del proveedor).');

INSERT INTO detalle_despacho (id_detalle_despacho, id_despacho, id_producto, cantidad_despachada, verificado) VALUES
 (1, 1, 1, 70, 1),
 (2, 1, 3, 150, 1),
 (3, 1, 5, 200, 1),
 (4, 1, 6, 24, 1),
 (5, 1, 9, 44, 1);
