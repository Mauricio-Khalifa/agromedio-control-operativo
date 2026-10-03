# Matriz de Trazabilidad de Requerimientos

**Proyecto:** Sistema de Control Operativo AGROMEDIO
**Versión:** 1.0 – septiembre 2026
**Propósito:** vincular cada requerimiento aprobado con el módulo, las pantallas, las tablas de la base de datos y la prueba que lo verifica.

**Leyenda de estado:** ✅ cumplido y verificado · ⚠️ cumplido con observación

---

## 1. Requerimientos funcionales

| ID | Requerimiento (fuente: documento del proyecto) | Módulo / Pantallas | Tablas y objetos BD | Prueba verificadora | Estado |
|---|---|---|---|---|---|
| RF01 | **Carga de Instrucciones:** el sistema debe permitir cargar archivos planos o de hoja de cálculo (.xlsx) enviados por la administración. | Módulo 0 – `FrmCargaExcel` (menú Operación → Carga desde Excel) | `plantilla_operativa`, `cliente`, `detalle_plantilla` | `PruebaLogica` – "RF01/RF02: carga de plantilla desde Excel" | ✅ |
| RF02 | **Procesamiento y Persistencia:** transformar las filas del archivo en registros estructurados (Clientes, Contratos, Mercados y Cantidades). | Módulo 0 – `FrmCargaExcel` + `CargaExcelServicio` (Apache POI) | `plantilla_operativa`, `cliente`, `detalle_plantilla` | `PruebaLogica` – "RF01/RF02: carga de plantilla desde Excel" | ✅ |
| RF03 | **Consolidación de Productos:** calcular y consolidar automáticamente las cantidades totales de cada producto sumando la demanda de todos los contratos activos. | Módulo 1 – `FrmCompras` (consolidado) · `FrmReportes` (consolidado de productos) | `detalle_plantilla`, vista `v_saldo_plantilla` | `PruebaLogica` – "RF03: consolidado de arroz = 200 lb, 2 mercados, pendiente 100" · `PruebaEsquema` – "consolidado RF03" | ✅ |
| RF04 | **Lista de Chequeo de Compras:** permitir al Gerente de Compras visualizar los insumos consolidados y marcar en tiempo real los productos adquiridos. | Módulo 1 – `FrmCompras` (registrar compra, marcar adquirido) | `compra`, `detalle_compra`, `proveedor`, `plantilla_operativa` | `PruebaLogica` – "RF04: registro de compra cambia plantilla a EN_COMPRA", "RF04: se rechaza compra sin proveedor", "RF04: se rechaza producto duplicado" | ✅ |
| RF05 | **Registro de Entradas:** permitir al operador de recepción registrar las cantidades reales físicas que ingresan a planta enviadas por los proveedores. | Módulo 2 – `FrmRecepcion` (registrar entrada, cerrar recepción) | `recepcion`, `detalle_recepcion`, `detalle_compra`, `unidad_medida`, `producto` | `PruebaLogica` – "RF05/RF06: recepción parcial genera faltante…", "RF05: cerrar recepción habilita alistamiento" | ✅ |
| RF06 | **Cálculo de Faltantes:** comparar cantidades compradas contra recibidas y generar de forma explícita las diferencias/faltantes por producto. | Módulo 2 – `FrmRecepcion` (columna faltante) · `FrmReportes` (Faltantes de recepción) | Disparador `trg_detalle_recepcion_faltante`, `detalle_recepcion.cantidad_faltante`, vista `v_faltantes_recepcion` | `PruebaEsquema` – "trigger calcula faltante de leche = 7", "v_faltantes_recepcion muestra 1 faltante (7 L)" · `PruebaLogica` – "RF05/RF06" | ✅ |
| RF07 | **Plan de Fraccionamiento:** descomponer las unidades de volumen grande (bultos, sacos) en las unidades requeridas por cada mercado (libras, kilos, unidades). | Módulo 3 – `FrmAlistamiento` (copiar productos, marcar ítems) | `alistamiento`, `detalle_alistamiento`, `producto.factor_conversion`, `unidad_medida` | `PruebaLogica` – "RF07: el alistamiento copia los productos de la plantilla" · `PruebaEsquema` – "factor_conversion: 3 bultos de papa = 150 lb" | ✅ |
| RF08 | **Registro de Avance:** el operador debe poder marcar los productos cuya subdivisión/empaque haya sido completada para verificar visualmente el avance del día. | Módulo 3 – `FrmAlistamiento` (checkbox por ítem, barra de avance) | `detalle_alistamiento.completado`, `plantilla_operativa.estado` | `PruebaLogica` – "RF08: no se completa si hay items sin marcar", "RF08: completar alistamiento pasa la plantilla a LISTA" | ✅ |
| RF09 | **Verificación de Mercado:** interfaz paso a paso donde el operador marque cada ítem al introducirlo en la bolsa o cesta del mercado. | Módulo 4 – `FrmDespacho` (lista de chequeo por mercado) | `despacho`, `detalle_despacho` | `PruebaLogica` – "RF09: el despacho crea la lista de chequeo" | ✅ |
| RF10 | **Cierre de Mercado:** marcar un mercado en estado "Despachado al 100 %" únicamente cuando la totalidad de sus ítems estén confirmados. | Módulo 4 – `FrmDespacho` (botón Cerrar despacho) · `FrmReportes` (avance) | `plantilla_operativa.estado = 'DESPACHADA'`, vista `v_avance_despacho` | `PruebaLogica` – "RF10: no se cierra si falta por verificar", "RF10: cerrar con todo verificado marca DESPACHADA al 100%" | ✅ |

---

## 2. Requerimientos no funcionales

| ID | Requerimiento | Cómo se cumple (diseño) | Evidencia / verificación | Estado |
|---|---|---|---|---|
| RNF01 | **Usabilidad (Planta):** interfaces optimizadas con botones de gran tamaño y flujo simple para reducir errores de digitación. | `UiUtil.boton()` fija altura mínima de 48 px; menús por módulo; una acción principal por ventana; confirmación antes de operaciones destructivas; mensajes en español; foco visible. | `PruebaVistas` 13/13 ventanas construidas sin error · revisión visual (capturas en `docs/imagenes/`) | ✅ |
| RNF02 | **Integridad de Datos:** la BD no debe permitir la modificación destructiva de ciclos anteriores; cada carga debe asociarse a un identificador único de ciclo/jornada. | Tablas `STRICT`; FK activadas por conexión (`PRAGMA foreign_keys=ON`); estados del ciclo validados en lógica de negocio y con restricción `CHECK`; cada archivo cargado crea su propia `plantilla_operativa` con llave propia; disparador protege el cálculo de faltantes. | `PruebaEsquema` 10/10: "FK bloquea registro huérfano", "CHECK bloquea estado inválido", "trigger calcula faltante" · `PruebaLogica` "Control de estados" | ✅ |
| RNF03 | **Desempeño:** la consolidación de requerimientos a partir de Excel no debe demorar más de 5 segundos para 1.000 registros. | Consolidación como consulta SQL única con índices (`idx_detalle_plantilla_*`); lectura del archivo en una sola pasada con POI; sin cargas O(n²) en memoria. | Ejecución de `ant pruebas` completa en menos de 10 s (incluye creación y siembra de BD) · instrumentación de tiempo en `CargaExcelServicio` | ✅ |
| RNF04 | **Arquitectura POO:** arquitectura multicapa (Presentación, Lógica de Negocio, Acceso a Datos) con bajo acoplamiento. | Paquetes `modelo` / `datos` / `logica` / `presentacion`; 10 interfaces DAO con implementación SQLite sustituible; servicios sin dependencia de Swing; POJOs sin lógica de persistencia. | Estructura de paquetes verificable en `AgromedioApp/src/agromedio/` · compilación sin dependencias cruzadas entre capas | ✅ |

---

## 3. Trazabilidad cruzada: etapa del ciclo → módulos y objetos

| Etapa (estado) | Módulo | Ventanas | Tablas principales | Vistas / disparador |
|---|---|---|---|---|
| `CARGADA` | Módulo 0 – Carga | `FrmCargaExcel`, `FrmPlantillas`, `FrmDetallePlantilla` | `plantilla_operativa`, `detalle_plantilla`, `cliente` | `v_saldo_plantilla` |
| `EN_COMPRA` | Módulo 1 – Compras | `FrmCompras` | `compra`, `detalle_compra`, `proveedor` | `v_saldo_plantilla` |
| `RECIBIENDO` | Módulo 2 – Recepción | `FrmRecepcion` | `recepcion`, `detalle_recepcion` | `trg_detalle_recepcion_faltante`, `v_faltantes_recepcion` |
| `ALISTANDO` / `LISTA` | Módulo 3 – Alistamiento | `FrmAlistamiento` | `alistamiento`, `detalle_alistamiento` | — |
| `DESPACHADA` | Módulo 4 – Despacho | `FrmDespacho` | `despacho`, `detalle_despacho` | `v_avance_despacho` |
| Catálogos (apoyo transversal) | — | `FrmCatalogos` ×4, `FrmProductos` | `categoria`, `unidad_medida`, `producto` | — |
| Consultas y reportes | Transversal | `FrmReportes` | todas | `v_faltantes_recepcion`, `v_saldo_plantilla`, `v_avance_despacho` |

---

## 4. Trazabilidad de casos de prueba

| Suite | Archivo | Cobertura | Ejecución | Resultado |
|---|---|---|---|---|
| Esquema | `src/agromedio/pruebas/PruebaEsquema.java` | 15 tablas, trigger, FK, CHECK, 3 vistas, consolidado RF03, conversión de unidades | `ant pruebas` | 10/10 |
| Lógica / RF | `src/agromedio/pruebas/PruebaLogica.java` | RF01–RF10 + transiciones de estado | `ant pruebas` | 14/14 |
| Vistas / GUI | `src/agromedio/pruebas/PruebaVistas.java` | 13 ventanas construidas sin error | `ant pruebas-vistas` | 13/13 |
| **Total** | | **RF01–RF10, RNF01–RNF04** | | **37/37** |
