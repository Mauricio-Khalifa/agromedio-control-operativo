-- =====================================================================
-- SISTEMA DE CONTROL OPERATIVO AGROMEDIO
-- Datos iniciales de catálogos (sin datos operativos de prueba)
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
 (8, 'Galon',    'Gal'),
 (9, 'Gramo',    'g'),
 (10, 'Mililitro', 'ml');

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
 (1, 'Lacteos del Magdalena S.A.', 'Pedro Lopez',   '6063154455'),
 (2, 'Distribuidora Andes del Centro', 'Sandra Gil', '6063201122'),
 (3, 'Carnes del Oriente',          'Luis Herrera',  '6063315566'),
 (4, 'Abastos Proveedores Unidos',  'Rocio Sanabria','6063426677');

-- Producto: unidad_compra vs unidad_despacho + factor_conversion
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
