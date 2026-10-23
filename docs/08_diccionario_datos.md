# Diccionario de Datos - Sistema de Control Operativo AGROMEDIO

**Proyecto:** Sistema de Control Operativo AGROMEDIO  
**Motor:** SQLite 3.46, tablas `STRICT`  
**Esquema fuente:** `AgromedioApp/src/sql/schema.sql`  
**Copia documental:** `docs/base_de_datos/schema.sql`

Este diccionario describe las 16 tablas del modelo, sus columnas, restricciones y relaciones. Los tipos corresponden a los tipos de almacenamiento de SQLite (`INTEGER`, `REAL`, `TEXT`); la validación de dominio se refuerza con `CHECK`, llaves foráneas y disparadores.

## 1. Convenciones

| Convención | Significado |
|---|---|
| PK | Llave primaria |
| FK | Llave foránea |
| NN | `NOT NULL` |
| UQ | `UNIQUE` |
| AI | `AUTOINCREMENT` |
| DEF | Valor por defecto |
| CK | Restricción `CHECK` |

Las fechas se almacenan como texto ISO (`aaaa-mm-dd`) o marca de tiempo local (`aaaa-mm-dd hh:mm:ss`). Las cantidades se almacenan en `REAL` en la unidad indicada por el producto o por el detalle correspondiente.

## 2. Catálogos

### 2.1 `cliente`

Entidad a la que pertenece un mercado o contrato operativo.

| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `id_cliente` | INTEGER | PK, AI | Identificador del cliente |
| `nombre_entidad` | TEXT | NN | Nombre de la entidad o mercado |
| `contacto` | TEXT | — | Persona de contacto |
| `telefono` | TEXT | — | Teléfono de contacto |

### 2.2 `categoria`

Agrupación del catálogo de productos.

| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `id_categoria` | INTEGER | PK, AI | Identificador de la categoría |
| `nombre` | TEXT | NN, UQ | Nombre de la categoría |

### 2.3 `unidad_medida`

Unidades de compra, despacho o venta.

| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `id_unidad` | INTEGER | PK, AI | Identificador de la unidad |
| `nombre` | TEXT | NN, UQ | Nombre completo |
| `abreviatura` | TEXT | NN, UQ | Abreviatura usada en la interfaz |

### 2.4 `proveedor`

Proveedor que suministra los productos comprados.

| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `id_proveedor` | INTEGER | PK, AI | Identificador del proveedor |
| `nombre` | TEXT | NN | Razón social o nombre |
| `contacto` | TEXT | — | Persona de contacto |
| `telefono` | TEXT | — | Teléfono de contacto |

### 2.5 `producto`

Producto maestro y reglas de conversión entre unidades.

| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `id_producto` | INTEGER | PK, AI | Identificador del producto |
| `nombre` | TEXT | NN, UQ | Nombre del producto |
| `id_categoria` | INTEGER | NN, FK | Categoría del producto |
| `id_unidad_compra` | INTEGER | NN, FK | Unidad en que se compra |
| `id_unidad_despacho` | INTEGER | NN, FK | Unidad en que se despacha |
| `factor_conversion` | REAL | NN, CK > 0 | Equivalencia entre unidad de compra y despacho |

**Relaciones:** `id_categoria` → `categoria`; `id_unidad_compra` e `id_unidad_despacho` → `unidad_medida`.

## 3. Carga y plantilla operativa

### 3.1 `carga_plantilla`

Registro de cada archivo procesado. Evita que un mismo archivo duplique la demanda.

| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `id_carga` | INTEGER | PK, AI | Identificador único de la carga |
| `fecha_carga` | TEXT | NN, DEF `datetime('now','localtime')` | Fecha y hora del procesamiento |
| `nombre_archivo` | TEXT | NN | Nombre original del archivo |
| `hash_archivo` | TEXT | NN, UQ | Huella SHA-256 del contenido |
| `usuario` | TEXT | — | Usuario del sistema operativo que cargó |
| `plantillas_creadas` | INTEGER | NN, DEF 0, CK >= 0 | Plantillas generadas por la carga |
| `productos_registrados` | INTEGER | NN, DEF 0, CK >= 0 | Detalles de producto generados |

### 3.2 `plantilla_operativa`

Instrucción de un mercado para una fecha; unidad de trabajo del ciclo.

| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `id_plantilla` | INTEGER | PK, AI | Identificador de la plantilla |
| `id_cliente` | INTEGER | NN, FK | Cliente/entidad del mercado |
| `nombre_mercado` | TEXT | NN | Nombre del mercado |
| `fecha_programada` | TEXT | NN | Fecha de ejecución |
| `estado` | TEXT | NN, CK | `CARGADA`, `EN_COMPRA`, `RECIBIENDO`, `ALISTANDO`, `LISTA` o `DESPACHADA` |
| `fecha_carga` | TEXT | NN, DEF | Fecha de creación |
| `id_carga` | INTEGER | FK, nullable | Carga que originó la plantilla |

**Relaciones:** `id_cliente` → `cliente`; `id_carga` → `carga_plantilla` con `ON DELETE RESTRICT`.

### 3.3 `detalle_plantilla`

Producto y cantidad solicitada en una plantilla.

| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `id_detalle_plantilla` | INTEGER | PK, AI | Identificador del detalle |
| `id_plantilla` | INTEGER | NN, FK | Plantilla a la que pertenece |
| `id_producto` | INTEGER | NN, FK | Producto solicitado |
| `cantidad_solicitada` | REAL | NN, CK > 0 | Cantidad requerida en unidad de despacho |

**Relaciones:** `id_plantilla` → `plantilla_operativa` con `ON DELETE CASCADE`; `id_producto` → `producto`.

## 4. Compras y recepción

### 4.1 `compra`

Cabecera de la compra asociada a una plantilla.

| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `id_compra` | INTEGER | PK, AI | Identificador de la compra |
| `id_plantilla` | INTEGER | NN, FK | Plantilla abastecida |
| `fecha_compra` | TEXT | NN | Fecha de registro |
| `estado` | TEXT | NN, CK | `REGISTRADA`, `RECIBIDA` o `ANULADA` |

**Relaciones:** `id_plantilla` → `plantilla_operativa` con `ON DELETE RESTRICT`.

### 4.2 `detalle_compra`

Producto, proveedor y cantidad comprada.

| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `id_detalle_compra` | INTEGER | PK, AI | Identificador del detalle |
| `id_compra` | INTEGER | NN, FK | Compra a la que pertenece |
| `id_producto` | INTEGER | NN, FK | Producto comprado |
| `id_proveedor` | INTEGER | NN, FK | Proveedor seleccionado |
| `cantidad_comprada` | REAL | NN, CK > 0 | Cantidad en unidad de compra |

**Relaciones:** `id_compra` → `compra` con `ON DELETE CASCADE`; `id_producto` → `producto`; `id_proveedor` → `proveedor`.

### 4.3 `recepcion`

Cabecera de una entrega física del proveedor. Una compra puede tener varias recepciones parciales.

| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `id_recepcion` | INTEGER | PK, AI | Identificador de la entrega |
| `id_compra` | INTEGER | NN, FK | Compra recibida |
| `fecha_recepcion` | TEXT | NN | Fecha de la entrega |
| `observaciones` | TEXT | — | Notas de la entrega |

**Relaciones:** `id_compra` → `compra` con `ON DELETE CASCADE`.

### 4.4 `detalle_recepcion`

Cantidad recibida por detalle de compra y faltante acumulado.

| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `id_detalle_recepcion` | INTEGER | PK, AI | Identificador del detalle |
| `id_recepcion` | INTEGER | NN, FK | Entrega a la que pertenece |
| `id_detalle_compra` | INTEGER | NN, FK | Detalle de compra recibido |
| `cantidad_recibida` | REAL | NN, CK > 0 | Cantidad física recibida |
| `cantidad_faltante` | REAL | NN, DEF 0, CK >= 0 | Pendiente acumulado calculado por trigger |

**Relaciones:** `id_recepcion` → `recepcion` con `ON DELETE CASCADE`; `id_detalle_compra` → `detalle_compra`.

**Trigger:** `trg_detalle_recepcion_faltante` recalcula el faltante como `cantidad_comprada - SUM(cantidad_recibida)` para todas las filas del mismo detalle de compra.

## 5. Alistamiento y despacho

### 5.1 `alistamiento`

Cabecera del fraccionamiento y armado de un mercado.

| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `id_alistamiento` | INTEGER | PK, AI | Identificador del alistamiento |
| `id_plantilla` | INTEGER | NN, FK | Plantilla que se alista |
| `fecha_alistamiento` | TEXT | NN | Fecha de inicio |
| `estado` | TEXT | NN, CK | `EN_PROCESO` o `COMPLETO` |

**Relaciones:** `id_plantilla` → `plantilla_operativa` con `ON DELETE RESTRICT`.

### 5.2 `detalle_alistamiento`

Cantidad alistada por producto y marca de completitud.

| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `id_detalle_alistamiento` | INTEGER | PK, AI | Identificador del detalle |
| `id_alistamiento` | INTEGER | NN, FK | Alistamiento al que pertenece |
| `id_producto` | INTEGER | NN, FK | Producto alistado |
| `cantidad_alistada` | REAL | NN, CK >= 0 | Cantidad preparada |
| `completado` | INTEGER | NN, DEF 0, CK 0/1 | Indica si el ítem terminó |

**Restricción adicional:** `CHECK (completado = 0 OR cantidad_alistada > 0)`.

**Relaciones:** `id_alistamiento` → `alistamiento` con `ON DELETE CASCADE`; `id_producto` → `producto`.

### 5.3 `despacho`

Cabecera de la lista de chequeo de salida del mercado.

| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `id_despacho` | INTEGER | PK, AI | Identificador del despacho |
| `id_plantilla` | INTEGER | NN, FK | Plantilla despachada |
| `fecha_despacho` | TEXT | NN | Fecha de despacho |
| `estado` | TEXT | NN, CK | `EN_VERIFICACION` o `DESPACHADA` |
| `novedades` | TEXT | — | Observaciones o justificación del faltante aceptado |

**Relaciones:** `id_plantilla` → `plantilla_operativa` con `ON DELETE RESTRICT`.

### 5.4 `detalle_despacho`

Producto, cantidad despachada, verificación y aceptación del faltante.

| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `id_detalle_despacho` | INTEGER | PK, AI | Identificador del detalle |
| `id_despacho` | INTEGER | NN, FK | Despacho al que pertenece |
| `id_producto` | INTEGER | NN, FK | Producto despachado |
| `cantidad_despachada` | REAL | NN, CK >= 0 | Cantidad que sale del mercado |
| `verificado` | INTEGER | NN, DEF 0, CK 0/1 | Indica si el ítem fue chequeado |
| `faltante_aceptado` | INTEGER | NN, DEF 0, CK 0/1 | Indica aceptación explícita del faltante |

**Restricción adicional:** `CHECK (verificado = 0 OR cantidad_despachada > 0)`.

**Relaciones:** `id_despacho` → `despacho` con `ON DELETE CASCADE`; `id_producto` → `producto`.

## 6. Vistas y disparadores

| Objeto | Tipo | Función |
|---|---|---|
| `v_saldo_plantilla` | Vista | Solicitado, despachado y saldo por plantilla/producto |
| `v_faltantes_recepcion` | Vista | Faltante acumulado por detalle de compra, sin duplicar entregas |
| `v_avance_despacho` | Vista | Porcentaje verificado y marca `con_novedad` |
| `trg_detalle_recepcion_faltante` | Trigger | Recalcula el faltante acumulado |
| `trg_plantilla_historico` | Trigger | Bloquea el borrado de plantillas con histórico |
| `trg_alistamiento_cantidad_ins` / `_upd` | Triggers | Bloquean completar con cantidad 0 |
| `trg_despacho_cantidad_ins` / `_upd` | Triggers | Bloquean verificar con cantidad 0 |

## 7. Índices de apoyo

| Índice | Tabla | Columnas |
|---|---|---|
| `idx_detalle_plantilla_planta` | `detalle_plantilla` | `id_plantilla` |
| `idx_detalle_plantilla_prod` | `detalle_plantilla` | `id_producto` |
| `idx_plantilla_carga` | `plantilla_operativa` | `id_carga` |
| `idx_compra_plantilla` | `compra` | `id_plantilla` |
| `idx_det_compra_compra` | `detalle_compra` | `id_compra` |
| `idx_recepcion_compra` | `recepcion` | `id_compra` |
| `idx_det_recepcion_rec` | `detalle_recepcion` | `id_recepcion` |
| `idx_det_recepcion_detcomp` | `detalle_recepcion` | `id_detalle_compra` |
| `idx_despacho_plantilla` | `despacho` | `id_plantilla` |
| `idx_det_despacho_desp` | `detalle_despacho` | `id_despacho` |
| `idx_alistamiento_plantilla` | `alistamiento` | `id_plantilla` |

## 8. Reglas de integridad resumidas

- Las tablas son `STRICT`: no aceptan tipos distintos a los declarados.
- Las llaves foráneas se activan en cada conexión con `PRAGMA foreign_keys = ON`.
- Las plantillas con compras, alistamientos o despachos no se eliminan (`RESTRICT` + trigger).
- Los detalles de plantilla, compra, recepción, alistamiento y despacho se eliminan con su cabecera cuando corresponde (`CASCADE`).
- Las cantidades completadas o verificadas deben ser mayores que cero.
- Cada archivo cargado tiene un hash SHA-256 único y un `id_carga`.
