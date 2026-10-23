# Casos de Uso

**Proyecto:** Sistema de Control Operativo AGROMEDIO
**Versión:** 1.1 – octubre 2026

**Alcance de actores:** los actores son roles operativos del proceso, no cuentas de usuario. Esta versión es mono-usuario y no implementa autenticación ni permisos (decisión D06); el control de acceso se delega al sistema operativo.

**Actores principales**

| Actor | Rol |
|---|---|
| Coordinador de operaciones | Carga las instrucciones de administración y vigila el avance del día |
| Gerente de compras | Consolida y registra las compras con proveedores |
| Operador de recepción | Registra la mercancía que ingresa a planta |
| Operador de alistamiento | Fracciona y arma los productos de cada mercado |
| Operador de despacho | Verifica ítem por ítem la salida de cada mercado |
| Sistema | Bloquea transiciones inválidas y calcula faltantes y estados |

---

## CU01 – Cargar instrucciones desde archivo (Módulo 0)

| Campo | Contenido |
|---|---|
| Actor | Coordinador de operaciones |
| Precondición | Existe un archivo `.xlsx`, `.xls`, `.csv` o `.txt` enviado por administración, con las columnas Cliente, Contacto, Teléfono, Mercado, Fecha, Producto y Cantidad |
| Disparador | Inicio de jornada |
| Flujo principal | 1. Menú **Operación → Carga desde archivo** · 2. Selecciona el archivo · 3. Pulsa **Cargar** · 4. El sistema registra la carga, crea las plantillas, los clientes/mercados, los productos faltantes y los detalles · 5. Muestra el resumen de filas procesadas y productos nuevos |
| Flujo alternativo | Archivo con columnas inválidas: el sistema informa el error y no crea la plantilla. Archivo ya cargado: muestra **"Carga ya realizada"** y no duplica la demanda. En CSV/TXT se detecta `;`, `,` o tabulador y se admite UTF-8/BOM. La cantidad puede ser numérica o descriptiva (`9 unidades`, `1 de 300 gr`, `400 gramos`, `1 libra`) |
| Postcondición | Carga registrada en `carga_plantilla` y plantilla(s) en estado `CARGADA` con sus cantidades listas para consolidar; los productos nuevos quedan en el catálogo con la unidad detectada (RF01, RF02) |
| Reglas de negocio | Un archivo válido = una carga con identificador único; varias plantillas pueden pertenecer a la misma carga (RNF02) |

## CU02 – Consultar y consolidar productos (Módulo 1)

| Campo | Contenido |
|---|---|
| Actor | Gerente de compras |
| Precondición | Existe al menos una plantilla en estado `CARGADA` |
| Flujo principal | 1. **Operación → Compras** · 2. Selecciona la plantilla · 3. El consolidado muestra por producto la suma de todas las cantidades solicitadas · 4. Compara contra el saldo pendiente |
| Postcondición | Lista de insumos consolidados visible (RF03) |
| Reglas de negocio | Consolidación = suma de `detalle_plantilla` de plantillas activas por producto |

## CU03 – Registrar una compra (Módulo 1)

| Campo | Contenido |
|---|---|
| Actor | Gerente de compras |
| Precondición | Consolidado visible (CU02) |
| Flujo principal | 1. Selecciona proveedor y fecha · 2. Agrega productos con cantidad y unidad de compra · 3. **Registrar compra** · 4. El sistema guarda compra y detalles · 5. La plantilla pasa a `EN_COMPRA` |
| Flujo alternativo A | Sin proveedor → mensaje "La compra debe tener un proveedor" y no guarda |
| Flujo alternativo B | Producto repetido en la compra → mensaje de producto duplicado y no guarda |
| Postcondición | `compra` + `detalle_compra` persistidos; plantilla en `EN_COMPRA` (RF04) |

## CU04 – Registrar entradas de mercancía (Módulo 2)

| Campo | Contenido |
|---|---|
| Actor | Operador de recepción |
| Precondición | Compra registrada (CU03) |
| Flujo principal | 1. **Operación → Recepción** · 2. Selecciona la compra · 3. Registra por producto la cantidad recibida en unidad de compra · 4. El sistema acumula lo recibido por detalle · 5. El disparador calcula el faltante · 6. La plantilla pasa a `RECIBIENDO` |
| Flujo alternativo A | Entrega parcial: el faltante queda explícito por producto (caso 77 vs 70 litros) |
| Flujo alternativo B | Segunda entrega: se registra sobre el mismo detalle, se acumula lo recibido y el faltante se recalcula |
| Postcondición | `recepcion` + `detalle_recepcion` con `cantidad_faltante` acumulada; puede haber varias entregas por compra (RF05, RF06) |

## CU05 – Cerrar la recepción (Módulo 2)

| Campo | Contenido |
|---|---|
| Actor | Operador de recepción |
| Precondición | Entradas registradas (CU04) |
| Flujo principal | 1. **Cerrar recepción** · 2. Confirmación · 3. La plantilla pasa a `ALISTANDO` y habilita el alistamiento |
| Postcondición | Recepción cerrada; si queda faltante acumulado, se conserva para el reporte y para la novedad de despacho (RF05, RF06) |

## CU06 – Alistar los productos (Módulo 3)

| Campo | Contenido |
|---|---|
| Actor | Operador de alistamiento |
| Precondición | Plantilla en `ALISTANDO` |
| Flujo principal | 1. **Operación → Alistamiento / Producción** · 2. Selecciona la plantilla · 3. El sistema copia los productos como tareas · 4. El operador marca cada producto completado · 5. La barra de avance se actualiza · 6. **Completar alistamiento** → plantilla en `LISTA` |
| Flujo alternativo | Quedan ítems sin marcar, se intenta alistar más de lo recibido o un producto no tiene recepción → se bloquea el avance/cierre y se indica el motivo |
| Postcondición | `alistamiento` + `detalle_alistamiento`; plantilla `LISTA`; un faltante parcial cerrado se conserva para la novedad de despacho (RF07, RF08) |

## CU07 – Verificar y despachar un mercado (Módulo 4)

| Campo | Contenido |
|---|---|
| Actor | Operador de despacho |
| Precondición | Plantilla en `LISTA` |
| Flujo principal | 1. **Operación → Despacho** · 2. Selecciona la plantilla; el sistema crea la lista de chequeo con la cantidad solicitada · 3. El operador verifica cada ítem al colocarlo en la bolsa/cesta · 4. **Cerrar despacho** con todo verificado y cantidades completas → plantilla `DESPACHADA`, avance 100 % |
| Flujo alternativo A | Hay ítems sin verificar → se bloquea el cierre y se lista lo pendiente |
| Flujo alternativo B | Hay un faltante parcial → el operador marca **Faltante aceptado**, registra la novedad obligatoria y el sistema despacha dejando el mercado “con novedad” |
| Flujo alternativo C | Hay un faltante no aceptado, una cantidad 0 o una cantidad superior a la solicitada → se bloquea el cierre con el motivo |
| Postcondición | `despacho` + `detalle_despacho` con verificación ítem por ítem, `faltante_aceptado` cuando aplique y novedad registrada (RF09, RF10, RN-09, RN-10) |
| Reglas de negocio | Ningún mercado sale sin confirmación total; solo se admite faltante parcial aceptado y justificado (RF10, D01) |

## CU08 – Generar reportes operativos

| Campo | Contenido |
|---|---|
| Actor | Coordinador de operaciones / gerente |
| Precondición | Datos de operación registrados |
| Flujo principal | 1. **Consultas y reportes → Reportes operativos** · 2. Elige "Faltantes de recepción" (con proveedor), "Saldo del mercado" (con fecha) o "Consolidado de productos" · 3. Vista previa en pantalla · 4. **Imprimir reporte** |
| Postcondición | Información de soporte para la decisión (RF03, RF06) |

## CU09 – Administrar catálogos y productos

| Campo | Contenido |
|---|---|
| Actor | Coordinador de operaciones |
| Precondición | Ninguna |
| Flujo principal | Menú **Catálogos** → Clientes / Categorías / Unidades de medida / Proveedores; o **Catálogos → Productos**. Alta, edición y baja con confirmación |
| Postcondición | Maestros actualizados que alimentan los módulos operativos |

---

## Diagrama de actores y casos de uso (resumen)

```
Coordinador ─┬─ CU01 Carga archivo ┐
             ├─ CU08 Reportes      │
             └─ CU09 Catálogos     │
Gerente ─────┼─ CU02 Consolidado   ├──▶ Sistema de Control Operativo AGROMEDIO
             └─ CU03 Compra        │
Recepción ──┼─ CU04 Entradas      │
             └─ CU05 Cierre recv.  │
Alistamiento┼─ CU06 Alistamiento   │
Despacho ───┴─ CU07 Despacho      ┘
```

## Matriz caso de uso → RF

| Caso de uso | RF cubiertos |
|---|---|
| CU01 | RF01, RF02 |
| CU02 | RF03 |
| CU03 | RF04 |
| CU04 | RF05, RF06 |
| CU05 | RF05 |
| CU06 | RF07, RF08 |
| CU07 | RF09, RF10 |
| CU08 | RF03, RF06 |
| CU09 | RNF01 (catálogos reutilizables) |
