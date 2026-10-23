# Matriz de Trazabilidad de Requerimientos

**Proyecto:** Sistema de Control Operativo AGROMEDIO
**Versión:** 1.1 – octubre 2026
**Propósito:** vincular cada requerimiento aprobado con el módulo, las pantallas, las tablas de la base de datos y la prueba que lo verifica.

**Leyenda de estado:** ✅ cumplido y verificado · ⚠️ cumplido con observación

---

## 1. Requerimientos funcionales

| ID | Requerimiento (fuente: documento del proyecto) | Módulo / Pantallas | Tablas y objetos BD | Prueba verificadora | Estado |
|---|---|---|---|---|---|
| RF01 | **Carga de Instrucciones:** el sistema debe permitir cargar archivos planos o de hoja de cálculo (.xlsx) enviados por la administración. | Módulo 0 – `FrmCargaExcel` (menú Operación → Carga desde archivo) | `carga_plantilla`, `plantilla_operativa`, `cliente`, `detalle_plantilla` | `PruebaLogica` – "RF01/RF02: carga de plantilla desde Excel" L31 CSV/TXT y L32 cantidades descriptivas | ✅ |
| RF02 | **Procesamiento y Persistencia:** transformar las filas del archivo en registros estructurados (Clientes, Mercados y Cantidades), registrando en el catálogo los productos que no existan. | Módulo 0 – `FrmCargaExcel` + `CargaExcelServicio` (POI para Excel; lector propio para CSV/TXT) | `carga_plantilla`, `plantilla_operativa`, `cliente`, `detalle_plantilla` | `PruebaLogica` – "RF01/RF02: carga de plantilla desde Excel", L30 duplicado, L31 archivos planos y L32 productos nuevos | ✅ |
| RF03 | **Consolidación de Productos:** calcular y consolidar automáticamente las cantidades totales de cada producto sumando la demanda de todos los mercados activos. | Módulo 1 – `FrmCompras` (consolidado) · `FrmReportes` (consolidado de productos) | `detalle_plantilla`, vista `v_saldo_plantilla` | `PruebaLogica` – "RF03: consolidado de arroz = 200 lb, 2 mercados, pendiente 100" · `PruebaEsquema` – "consolidado RF03" | ✅ |
| RF04 | **Lista de Chequeo de Compras:** permitir al Gerente de Compras visualizar los insumos consolidados y marcar en tiempo real los productos adquiridos. | Módulo 1 – `FrmCompras` (registrar compra, marcar adquirido) | `compra`, `detalle_compra`, `proveedor`, `plantilla_operativa` | `PruebaLogica` – "RF04: registro de compra cambia plantilla a EN_COMPRA", "RF04: se rechaza compra sin proveedor", "RF04: se rechaza producto duplicado" | ✅ |
| RF05 | **Registro de Entradas:** permitir al operador de recepción registrar las cantidades reales físicas que ingresan a planta enviadas por los proveedores. | Módulo 2 – `FrmRecepcion` (registrar entrega parcial, historial, cerrar recepción) | `recepcion`, `detalle_recepcion`, `detalle_compra`, `unidad_medida`, `producto` | `PruebaLogica` – "RF05/RF06: recepción parcial genera faltante…", "RN-05/RN-06: segunda entrega acumula…", "RN-05/RN-06: dos entregas 30 + 47 completan 77 L", "RF05: cerrar recepción habilita alistamiento" | ✅ |
| RF06 | **Cálculo de Faltantes:** comparar cantidades compradas contra recibidas y generar de forma explícita las diferencias/faltantes por producto. | Módulo 2 – `FrmRecepcion` (columna faltante acumulado) · `FrmReportes` (Faltantes de recepción) | Disparador `trg_detalle_recepcion_faltante` (suma acumulada), `detalle_recepcion.cantidad_faltante`, vista `v_faltantes_recepcion` (una fila por detalle) | `PruebaEsquema` – "trigger calcula faltante de leche = 7", "RN-06: segunda entrega parcial acumula faltante", "RN-06: segunda entrega completa 77/77", "v_faltantes_recepcion muestra 1 faltante (7 L)" · `PruebaLogica` – "RF05/RF06" | ✅ |
| RF07 | **Plan de Fraccionamiento:** descomponer las unidades de volumen grande (bultos, sacos) en las unidades requeridas por cada mercado (libras, kilos, unidades). | Módulo 3 – `FrmAlistamiento` (copiar productos, marcar ítems, columna Recibido) | `alistamiento`, `detalle_alistamiento`, `detalle_recepcion`, `producto.factor_conversion`, `unidad_medida` | `PruebaLogica` – "RF07: el alistamiento copia los productos de la plantilla", "RN-07: no se alista más de lo recibido" · `PruebaEsquema` – "factor_conversion: 3 bultos de papa = 150 lb" | ✅ |
| RF08 | **Registro de Avance:** el operador debe poder marcar los productos cuya subdivisión/empaque haya sido completada para verificar visualmente el avance del día. | Módulo 3 – `FrmAlistamiento` (checkbox por ítem, barra de avance) | `detalle_alistamiento.completado`, `detalle_recepcion`, `plantilla_operativa.estado` | `PruebaLogica` – "RF08: no se completa si hay items sin marcar", "RF08: completar alistamiento pasa la plantilla a LISTA", "RN-08: no se completa un producto sin recepción" | ✅ |
| RF09 | **Verificación de Mercado:** interfaz paso a paso donde el operador marque cada ítem al introducirlo en la bolsa o cesta del mercado. | Módulo 4 – `FrmDespacho` (lista de chequeo por mercado, cantidad solicitada, faltante aceptado) | `despacho`, `detalle_despacho` | `PruebaLogica` – "RF09: el despacho crea la lista de chequeo", "D02/RN-10: no se verifica una cantidad despachada en cero" | ✅ |
| RF10 | **Cierre de Mercado:** marcar un mercado en estado "Despachado al 100 %" únicamente cuando la totalidad de sus ítems estén confirmados. | Módulo 4 – `FrmDespacho` (botón Cerrar despacho) · `FrmReportes` (avance completo / con novedad) | `plantilla_operativa.estado = 'DESPACHADA'`, `detalle_despacho.faltante_aceptado`, vista `v_avance_despacho.con_novedad` | `PruebaLogica` – "RF10: no se cierra si falta por verificar", "RF10: cerrar con todo verificado marca DESPACHADA al 100%", L22–L25 · `PruebaEsquema` – E14 | ✅ |

---

## 2. Requerimientos no funcionales

| ID | Requerimiento | Cómo se cumple (diseño) | Evidencia / verificación | Estado |
|---|---|---|---|---|
| RNF01 | **Usabilidad (Planta):** interfaces optimizadas con botones de gran tamaño y flujo simple para reducir errores de digitación. | `UiUtil.boton()` fija altura mínima de 48 px; menús por módulo; una acción principal por ventana; confirmación antes de operaciones destructivas; mensajes en español; foco visible. | `PruebaVistas` 13/13 ventanas construidas sin error · revisión visual (capturas en `docs/imagenes/`) | ✅ |
| RNF02 | **Integridad de Datos:** la BD no debe permitir la modificación destructiva de ciclos anteriores; cada carga debe asociarse a un identificador único de ciclo/jornada. | Tablas `STRICT`; FK activadas por conexión (`PRAGMA foreign_keys=ON`); estados del ciclo validados en lógica de negocio y con restricción `CHECK`; tabla `carga_plantilla` con hash SHA-256 único y `plantilla_operativa.id_carga`; `CargaExcelServicio` rechaza el mismo archivo con "Carga ya realizada"; disparador protege el cálculo acumulado de faltantes; FK `RESTRICT` y trigger `trg_plantilla_historico` protegen el histórico (RN-12); CHECK y triggers impiden completar/verificar en cero (RN-11). | `PruebaEsquema` 20/20: "FK bloquea registro huérfano", "CHECK bloquea estado inválido", "trigger calcula faltante", "RN-12: no se elimina una plantilla con histórico", "RN-11: CHECK bloquea compra/alistamiento/despacho en cero", E20 hash único · `PruebaLogica` "Control de estados", L26-L28, L30 carga duplicada | ✅ |
| RNF03 | **Desempeño:** la consolidación de requerimientos a partir del archivo no debe demorar más de 5 segundos para 1.000 registros. | Consolidación como consulta SQL única con índices (`idx_detalle_plantilla_*`); lectura del archivo en una sola pasada (POI o lector plano); sin cargas O(n²) en memoria. | Ejecución de `ant pruebas` completa en menos de 10 s (incluye creación y siembra de BD) · instrumentación de tiempo en `CargaExcelServicio` | ✅ |
| RNF04 | **Arquitectura POO:** arquitectura multicapa (Presentación, Lógica de Negocio, Acceso a Datos) con bajo acoplamiento. | Paquetes `modelo` / `datos` / `logica` / `presentacion`; 12 interfaces DAO con implementación SQLite sustituible; servicios sin dependencia de Swing; POJOs sin lógica de persistencia. | Estructura de paquetes verificable en `AgromedioApp/src/agromedio/` · compilación sin dependencias cruzadas entre capas | ✅ |

---

## 3. Trazabilidad cruzada: etapa del ciclo → módulos y objetos

| Etapa (estado) | Módulo | Ventanas | Tablas principales | Vistas / disparador |
|---|---|---|---|---|
| `CARGADA` | Módulo 0 – Carga | `FrmCargaExcel`, `FrmPlantillas`, `FrmDetallePlantilla` | `carga_plantilla`, `plantilla_operativa`, `detalle_plantilla`, `cliente` | `v_saldo_plantilla` |
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
| Esquema | `src/agromedio/pruebas/PruebaEsquema.java` | 16 tablas, trigger acumulado, FK, CHECK, 3 vistas, consolidado RF03, conversión de unidades, faltante aceptado, histórico, cero y hash único de carga | `ant pruebas` | 20/20 |
| Lógica / RF | `src/agromedio/pruebas/PruebaLogica.java` | RF01–RF10, RN-01 a RN-12, RNF02, carga CSV/TXT, cantidades descriptivas y transiciones de estado | `ant pruebas` | 32/32 |
| Vistas / GUI | `src/agromedio/pruebas/PruebaVistas.java` | 13 ventanas construidas sin error | `ant pruebas-vistas` | 13/13 |
| **Total** | | **RF01–RF10, RNF01–RNF04, RN-01 a RN-12** | | **65/65** |

---

## 5. Reglas de negocio del Tercer Avance (RN-01 a RN-12)

El Tercer Avance de Proyecto Integrador incorpora 12 reglas de negocio que no estaban explícitas en la matriz original. Su trazabilidad detallada —estado actual, código, tablas, pruebas propuestas, brechas y decisiones— se documenta en:

**[07_matriz_trazabilidad_rn.md](07_matriz_trazabilidad_rn.md)**

Resumen del diagnóstico:

| Estado | RN | Total |
|---|---|---|
| ✅ Cumplen | RN-01 a RN-12 | 12 |
| ⚠️ Parciales | - | 0 |
| ❌ No cumplen | — | 0 |

**G01 a G10 quedaron cerradas:** recepción con múltiples entregas parciales y faltante acumulado; alistamiento con tope contra lo recibido y bloqueo de productos sin recepción; despacho con faltante parcial aceptado, novedad obligatoria y distinción “completo” / “con novedad”; histórico protegido por FK `RESTRICT`, trigger y servicio; compra bloqueada sobre plantillas vacías; cero bloqueado al completar/verificar; carga única identificada por hash SHA-256; carga de CSV/TXT y cantidades descriptivas con alta automática de productos; alcance mono-usuario sin roles; diccionario de datos y EDT. Las decisiones **D01 a D07** quedaron resueltas el 22/10/2026; **D03** se cerró como sinónimo de plantilla/mercado, sin entidad `contrato` separada, salvo que el cliente exija contratos con vigencia o condiciones propias.
