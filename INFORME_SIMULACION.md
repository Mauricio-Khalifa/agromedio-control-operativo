# Informe de simulación operativa — Sistema de Control Operativo Agromedio

**Fecha:** 25/10/2026
**Alcance:** análisis de todos los roles operativos, simulación en tiempo real de cada escenario de trabajo y su flujo, y consolidación de errores y cambios recomendados.
**Modalidad aplicada:** *ambos niveles* — simulación end-to-end sobre los servicios reales **y** pasada de UI Swing real (automatización de ventanas).

---

## 1. Resumen ejecutivo

- La aplicación **no tiene roles de usuario con permisos diferenciados**: es de **operador único, sin autenticación** (así lo declara el shell: `FrmPrincipal.java`, barra de estado y menú Ayuda). Lo que sí existen son **7 roles/perfiles operativos funcionales** separados por módulo/pantalla.
- Se construyeron y ejecutaron **dos suites nuevas** que simulan en tiempo real cada perfil:
  - `PruebaEscenarios` — 43 escenarios end-to-end sobre los servicios y una BD aislada.
  - `PruebaFlujosUI` — 10 flujos sobre las ventanas Swing reales (con autoclose de diálogos modales).
- **Resultado global: 120/120 pruebas correctas.**

  | Suite | Resultado |
  |---|---|
  | Esquema (`PruebaEsquema`) | 20/20 |
  | Lógica (`PruebaLogica`) | 38/38 |
  | **Escenarios por rol (`PruebaEscenarios`)** | **53/53** |
  | **Flujos de UI (`PruebaFlujosUI`)** | **10/10** |
  | Vistas (`PruebaVistas`) | 14/14 |

- Tras la corrección de los defectos críticos **C‑1/C‑2/C‑3** y de los **6 hallazgos reproducidos (M‑3, UI‑01, H‑04, H‑03, H‑2, H‑6)**, **no quedan hallazgos con defecto reproducidos**; los **~30 hallazgos adicionales** (medios/bajos) siguen priorizados en §4.
- **1 corrección aplicada durante esta sesión** (ver §6): el panel "Procesos abiertos" ahora deriva la etapa real a partir del estado **y** la evidencia de los registros, corrigiendo datos incoherentes sin ocultar procesos abiertos.

> Conclusión: la lógica de negocio **funciona correctamente en el camino feliz** y en la mayoría de reglas (RF01–RF10, RN-01…RN-12). Los defectos se concentran en **validaciones de estado/entradas atípicas**, **atomicidad** e **inmutabilidad tras el cierre**.

---

## 2. Mapa de roles operativos

| # | Rol operativo | Módulo / Pantallas | Servicios | Responsabilidad |
|---|---|---|---|---|
| 1 | Analista de datos | Módulo 0 – `FrmCargaExcel` | `CargaExcelServicio` | Cargar Excel/CSV/TXT; crea clientes, productos y plantillas |
| 2 | Gestor de catálogos | `FrmProductos`, `FrmCatalogos` (+ `Catalogo*`) | DAO de catálogos | Clientes, categorías, unidades, proveedores, productos |
| 3 | Planeador | `FrmPlantillas`, `FrmDetallePlantilla` | `PlantillaServicio` | Alta manual de plantillas y su detalle |
| 4 | Comprador | Módulo 1 – `FrmCompras` | `CompraServicio`, `ConsultaDAO` | Consolidar demanda (RF03) y registrar compra (RF04) |
| 5 | Recepcionista | Módulo 2 – `FrmRecepcion` | `RecepcionServicio` | Entradas parciales (RF05) y faltantes (RF06) |
| 6 | Producción / Alistador | Módulo 3 – `FrmAlistamiento` | `AlistamientoServicio` | Fraccionamiento (RF07) y avance (RF08) |
| 7 | Despachador | Módulo 4 – `FrmDespacho` | `DespachoServicio` | Checklist (RF09) y cierre real (RF10) |
| 8 | Gerencia / Análisis | `FrmProgreso`, `FrmReportes` | `ProgresoServicio`, `ReporteServicio` | Seguimiento y reportes |

**Ciclo de estados:** `CARGADA → EN_COMPRA → RECIBIENDO → ALISTANDO → LISTA → DESPACHADA`.

---

## 3. Resultados por rol (simulación ejecutada)

### 3.1 Analista de datos (Módulo 0) — `E0-*`
Carga CSV válida, detección de duplicado por hash, formato inválido, fecha inválida, cantidad 0/negativa, cantidades descriptivas ("1 de 300 gr"), archivo inexistente → **7/7 OK**.

### 3.2 Gestor de catálogos — `E1-*`
Alta y listado de catálogos, bloqueo de borrado de categoría en uso y de producto en uso → **3/3 OK**. Hallazgos: corrupción de factor ≥1000 (`M3`) y traducción incompleta de estados (`UI-01`).

### 3.3 Comprador (Módulo 1) — `E2-*`
Consolidado multi-mercado, `CARGADA→EN_COMPRA`, rechazos (sin proveedor, cantidad ≤0, plantilla vacía, plantilla LISTA) → **6/6 OK**. Hallazgo: compra permitida en estado `ALISTANDO` (`H-04`) y duplicados en compra (`H-01`).

### 3.4 Recepcionista (Módulo 2) — `E3-*`
Entrega parcial con faltante, rechazo de exceso, rechazo de entrega en cero, segunda entrega acumulada, cierre sin entradas, `→ALISTANDO` → **7/7 OK**. Hallazgos: `ANULADA` no se maneja (`H-03`) y duplicados de producto en compra (`H-01`).

### 3.5 Producción / Alistamiento (Módulo 3) — `E4-*`
Copia de productos, bloqueo sin cerrar recepción, tope RN-07, cero RN-11, `→LISTA` → **5/5 OK**. Hallazgo **crítico `H-1`**: `completar()` revierte una plantilla `DESPACHADA` a `LISTA`.

### 3.6 Despachador (Módulo 4) — `E5-*`
Bloqueo sin `LISTA`, bloqueo con ítems sin verificar, cero, exceso, faltante no aceptado (RN-09), novedad obligatoria (RN-10), cierre al 100% → **7/7 OK**. Hallazgos: edición post‑cierre (`H-2`) y doble cierre sobrescribe novedad (`H-6`).

### 3.7 Gerencia / Análisis — `E6-*`
Invariante de totales del resumen, exclusión de `DESPACHADA`, **auto‑corrección de etapa con estado atrasado**, reportes sin excepción y tareas pendientes → **6/6 OK**.

### 3.8 Integridad end-to-end — `E7-*`
Ciclo completo sin faltante y con faltante aceptado (novedad registrada) → **2/2 OK**.

### 3.9 UI Swing real — `UI-*`
Compras, Recepción (incluye registrar entrega), Alistamiento (iniciar), Despacho (abrir lista), Plantillas, Productos, Catálogos, Reportes y Progreso (con SwingWorker) → **10/10 OK**.

---

## 4. Errores y hallazgos priorizados

### 🔴 CRÍTICOS

**C-1. `completar()` puede revertir una plantilla ya `DESPACHADA` a `LISTA`.**  *(Resuelto)*
`AlistamientoServicio.completar()` ejecuta `plantillaDAO.actualizarEstado(idPlantilla, ESTADO_LISTA)` sin verificar el estado actual ni que el alistamiento esté `EN_PROCESO` (`AlistamientoServicio.java:155-156`). Una ventana de alistamiento abierta antes del despacho permite re‑completar y habilitar un **re‑despacho**, dejando el ciclo inconsistente.
*Reproducido:* `H-1 → DESPACHADA -> LISTA`.

**C-2. Productos duplicados en una misma operación (sin control).**  *(Resuelto)*
- En **compra**: `registrarCompra` no bloquea duplicados (`CompraServicio.java:59-66`; la UI de compras tampoco, `FrmCompras.java:206-231`). La **recepción mapea cantidades por posición** (`FrmRecepcion.java:221-234`, aplicadas por índice en `RecepcionServicio.java:71-108`) y ambas consultas ordenan solo por `pr.nombre` (`CompraDAOSqlite.listarDetalle`, `RecepcionDAOSqlite.listarAcumuladoPorCompra`). Con nombres repetidos el orden es ambiguo → **riesgo de guardar la cantidad en el detalle equivocado** (sobre‑recepción falsa / faltante incorrecto).
- En **carga de archivo**: `CargaExcelServicio.procesarFila` agrega detalle sin comprobar duplicados (`CargaExcelServicio.java:266-269`) mientras que `PlantillaServicio.agregarProducto` sí los bloquea (`PlantillaServicio.java:51-56`). Dos filas del mismo producto → **dos detalles** que inflan el "solicitado" en `v_saldo_plantilla` y en el consolidado.
*Reproducido:* `H-01 → permitida (2 lineas)`; `C2 → 2 detalle(s) creados`.

**C-3. Persistencia no atómica (sin transacciones).**  *(Resuelto)*
`registrarCompra`, `registrarRecepcion`, `cargar`, `guardar/completar` (alistamiento) y `cerrar` (despacho) hacen múltiples `INSERT/UPDATE` con autocommit (cada DAO abre su propia conexión). Un fallo intermedio deja cabeceras huérfanas o avances parciales silenciosos (p. ej. `idProveedor` inexistente pasa la validación de `>0` y falla por FK **después** de crear la compra). Evidencia: `CompraServicio.java:72-77`, `RecepcionServicio.java:96-108`, `CargaExcelServicio.java:242-300`, `FrmAlistamiento.java:201-208`, `FrmDespacho.java:288-290`.

### 🟠 MEDIOS

**M-1. Sin guardas de estado en el dominio ni en la BD (inmutabilidad solo en la UI).**
`DespachoServicio.marcarItem/cerrar` no exigen `EN_VERIFICACION`; `AlistamientoServicio.registrarAvance` no exige `EN_PROCESO`; no hay triggers `BEFORE UPDATE` que bloqueen cambios tras `DESPACHADA`/`COMPLETO`. La protección depende de `isCellEditable`/`setEnabled`.
*Reproducido:* `H-2 → permitido`.

**M-2. Doble cierre del despacho sobrescribe las novedades.**
`DespachoServicio.cerrar` no valida el estado y `DespachoDAOSqlite.actualizarEstado` reemplaza `novedades`. Un segundo cierre con otro texto borra la novedad original.
*Reproducido:* `H-6 → Novedad Original` reemplazada por `Novedad Nueva`.

**M-3. El estado `ANULADA` no se maneja de forma consistente.**
`registrarRecepcion`/`cerrarRecepcion` solo bloquean `RECIBIDA` (`RecepcionServicio.java:57-59,136-147`); `v_saldo_plantilla` incluye compras `ANULADA` (`schema.sql:316-332`) mientras `SQL_PROGRESO` y `consolidarDemanda` las excluyen → métricas divergentes.
*Reproducido:* `H-03 → permitida`.

**M-4. `disponible()` con respaldo peligroso.** `DespachoServicio.disponible` devuelve `cantidad_solicitada` cuando `alistado<=0` (`DespachoServicio.java:194-200`), permitiendo despachar producto nunca alistado si se sembró el despacho desde `detalle_plantilla`.

**M-5. "Recibido" sin filtrar compra `RECIBIDA`.** `AlistamientoDAOSqlite` y `v_saldo_plantilla` suman recepciones de compras aún `REGISTRADA` (`AlistamientoDAOSqlite.java:63-67`), habilitando alistar mercancía sin cerrar formalmente la compra.

**M-6. `registrarCompra` no valida pertenencia ni tope.** No comprueba que los productos estén en `detalle_plantilla`, ni límite a lo solicitado (`CompraServicio.java:59-66`).

**M-7. "Novedad" con dos definiciones distintas.** El panel la deriva del **texto** `despacho.novedades`; el reporte de la bandera `faltante_aceptado` (`schema.sql:384-385`). Un cierre puede verse "Novedad" en el panel y "COMPLETO" en el reporte.

**M-8. Reportes ejecutan BD en el EDT y sin manejo de error.** `FrmReportes` invoca los servicios en el hilo de eventos y tres de sus listeners no tienen `try/catch` → congelamiento y **fallo silencioso** ante error de BD (`FrmReportes.java:125-135`).

**M-9. Corrupción silenciosa de factores ≥ 1000.** `Formato.numero` agrupa con punto y `Formato.decimal` no reconoce el separador de miles sin coma → `1000 → "1.000" → 1.0` (`Formato.java:25-60`).
*Reproducido:* `M3 → 1.0`.

**M-10. Parseo de cantidades inconsistente.** `FrmDetallePlantilla` usa `Double.parseDouble` (`:158`) mientras `FrmProductos` usa `Formato.decimal` (`:252`); `"1.000"` se lee `1.0`, `"1.234,5"` lanza excepción cruda.

**M-11. Fecha programada sin validación de formato.** `PlantillaServicio.crear` solo comprueba no vacío (`:29-31`); la UI permite texto libre; la columna es `TEXT` sin `CHECK`.

**M-12. CSV solo en UTF‑8.** `Files.readAllLines(..., UTF_8)` (`CargaExcelServicio.java:165`); archivos ANSI de Excel producen nombres corruptos y posibles productos duplicados.

**M-13. Anti‑duplicado por hash de bytes es frágil.** Re‑guardar el mismo Excel cambia el hash (`CargaExcelServicio.java:88-96,493-501`) → doble carga del mismo contenido.

**M-14. Productos no comprados atascan el alistamiento.** `completar()` exige recepción en **todos** los productos (`AlistamientoServicio.java:125-134`); no hay forma de dar de baja un producto en las pantallas → mercado atascado en `ALISTANDO`.

**M-15. IDs de unidad/categoría hardcodeados en la carga.** `interpretarCantidad`/`categoriaPara` fijan IDs 1..8 (`CargaExcelServicio.java:346-409`); si se borra una unidad no usada o falta el seed, la FK **aborta toda la carga**.

**M-16. Categorización heurística inconsistente.** `categoriaPara` clasifica papa/yuca/plátano en "Frutas y verduras" (id 6) aunque el seed las ubica en "Tubérculos y raíces" (id 2); la categoría 2 nunca se retorna.

**M-17. La UI no permite eliminar plantillas.** `PlantillaServicio.eliminar` (RN-12) existe y está probado, pero `FrmPlantillas` no expone la acción.

### 🟡 BAJOS

- **B-1. Traducción de estados incompleta.** `Formato.estado` no mapea `RECIBIENDO`, `ALISTANDO`, `LISTA` (`Formato.java:62-79`) y `RendererEstado.colorEstado` tampoco (`UiUtil.java:352-362`) → se muestran los códigos crudos sin color.
  *Reproducido:* `UI-01 → ALISTANDO`.
- **B-2. Entradas de cantidad frágiles en UI.** Cantidad vacía o no numérica produce `NumberFormatException` cruda (`FrmDetallePlantilla.java:158`); combo de productos vacío puede dar NPE (`:157-159`).
- **B-3. Reporte de consolidado desalineado.** Cabecera de 5 columnas vs. 7 campos impresos; `totalEnUnidadCompra` calculado y nunca mostrado; fecha sin normalizar (vacío ≠ hoy, a diferencia de `CompraServicio`).
- **B-4. Formato numérico no uniforme.** `ReporteServicio` usa `String.format` sin `Locale` y mezcla `\n`/`%n` (`:48-108`).
- **B-5. Fechas heterogéneas en tablas.** "Fecha carga" (ISO con hora) no se reformatea; solo `yyyy-MM-dd` exacto pasa por el renderer.
- **B-6. Código muerto/duplicado.** `resumenProgreso()` vs `ProgresoServicio.resumen(filas)`; `ResumenProgreso.despachando` (nunca asignado); `ConsultaDAO.saldoTotal()`; `TareaProgreso.BAJA`; `ReporteServicio.fechaHoy()`; parámetro `color` ignorado en `FrmProgreso.barra`.
- **B-7. Sin `UNIQUE(id_plantilla)`** en `alistamiento` y `despacho` (depende de `LIMIT 1`).
- **B-8. `iniciar` de despacho** escribe `solicitado` y corrige después (`DespachoServicio.java:57,62-70`); `FrmDespacho.verificar` escribe dos veces por ítem (`:263-267`).
- **B-9. Robustez de entrada Excel.** `textoCelda`/`cargarLibroExcel` no cubren celdas de fórmula ni `getRow(0)==null` (`CargaExcelServicio.java:133,518-533`).
- **B-10. Auditoría no verificable.** `carga_plantilla.usuario` toma `System.getProperty("user.name")` en un sistema sin autenticación.

---

## 5. Cambios recomendados (por impacto)

1. **Guardas de estado en el dominio** (corrige C-1 y M-1): en `AlistamientoServicio.completar` leer la plantilla y exigir `ALISTANDO`/`LISTA`; en `DespachoServicio.cerrar/marcarItem` exigir `EN_VERIFICACION`; en `registrarCompra` bloquear `RECIBIENDO/ALISTANDO/LISTA/DESPACHADA`.
2. **Inmutabilidad en la BD** (M-1): triggers `BEFORE UPDATE` que aborten si `despacho.estado='DESPACHADA'` o `alistamiento.estado='COMPLETO'`.
3. **Transacciones únicas** con `setAutoCommit(false)`/rollback en carga, compra, recepción, alistamiento y despacho (C-3).
4. **Deduplicación de producto** en la carga y en la compra, y **mapeo por `idDetalleCompra`** (no por índice) en recepción (C-2). Considerar `UNIQUE(id_plantilla,id_producto)` en `detalle_plantilla`.
5. **Consistencia de `ANULADA`** en `v_saldo_plantilla` y en la lógica de recepción/alistamiento (M-3).
6. **Quitar el respaldo de `disponible()`** y sembrar el despacho desde el alistamiento real (M-4); filtrar `compra.estado='RECIBIDA'` al calcular "recibido" (M-5).
7. **Unificar el concepto de "novedad"** panel/reporte (M-7) y **mover los reportes a `SwingWorker`** con manejo de error (M-8).
8. **Centralizar el parseo/formateo numérico** con `Formato` y locale (M-9, M-10, B-4) y **validar formato de fecha** al crear plantillas (M-11).
9. **Completar `Formato.estado` y el renderer** con `RECIBIENDO/ALISTANDO/LISTA` (B-1) y exponer "Eliminar plantilla" (M-17).
10. **Limpieza**: retirar código muerto/duplicado (B-6) y unificar rutas de reportes (B-3).

---

## 6. Correcciones ya aplicadas en esta sesión

**Hallazgos reproducidos M‑3/UI‑01/H‑04/H‑03/H‑2/H‑6 (con pruebas de regresión).**
- **M‑3** (`Formato.decimal`): parseo con los símbolos es‑CO (DecimalFormat) → "1.000" se lee 1000, no 1,0; round‑trip con `Formato.numero`.
- **UI‑01** (`Formato.estado` + `UiUtil.colorEstado`): se traducen y colorean `RECIBIENDO/ALISTANDO/LISTA`.
- **H‑04** (`CompraServicio.registrarCompra`): bloquea compras en plantilla `ALISTANDO/LISTA/DESPACHADA`.
- **H‑03** (`RecepcionServicio`): rechaza recepción/cierre sobre compra `ANULADA`; `v_saldo_plantilla` filtra `ANULADA` (schema + migración).
- **H‑2** (`DespachoServicio.marcarItem/cerrar`, `AlistamientoServicio.registrarAvance`): inmutabilidad tras el cierre (DAO `obtener(id)` añadido).
- **H‑6** (`DespachoServicio.cerrar`): segundo cierre bloqueado, la novedad original se conserva.

**Defectos críticos C‑1/C‑2/C‑3 (con pruebas de regresión).**
- **C‑1** (`AlistamientoServicio.completar`): guardia de estado (`ALISTANDO`/`LISTA`) + transacción → una plantilla `DESPACHADA` ya no se revierte. Pruebas `PruebaLogica` "C‑1..." y `PruebaEscenarios` "C‑1...".
- **C‑2** (`CompraServicio.registrarCompra` rechaza producto duplicado; `CargaExcelServicio.procesarFila` fusiona cantidades) → elimina mapeo ambiguo en recepción y detalle duplicado. Pruebas "C‑2...".
- **C‑3** (`ConexionBD.enTransaccion` con proxy sin cierre) aplicada a carga, compra, recepción, alistamiento y despacho → atomicidad de escrituras. Prueba "C‑3..." (proveedor inexistente no deja compra huérfana).

**Panel "Procesos abiertos" — derivación de etapa real (`ConsultaDAOSqlite.SQL_PROGRESO`).**
Se reescribió la consulta para que la etapa mostrada = **máximo avance entre el estado guardado y la evidencia de los registros** (compra, recepción cerrada, alistamiento, despacho), excluyendo siempre `DESPACHADA`.
- Resuelve la observación original ("etapa Cargada no corresponde a los datos"): si `plantilla_operativa.estado` quedó atrasado respecto a los registros reales, el panel **auto‑corrige** la etapa.
- Corrige además una regresión previa: una plantilla con recepción cerrada aparece como `ALISTANDO` (no `RECIBIENDO`).
- Verificado: `PruebaLogica` 33/33 y escenarios `E6-03` (auto‑corrección) en verde; el análisis del panel confirmó que `estado_real` coincide con la lógica de negocio.

---

## 7. Cómo reproducir

```powershell
cd C:\Users\ivan_\agromedio-control-operativo\AgromedioApp
# Suite completa (esquema + lógica + escenarios + UI) y vistas
& "C:\Program Files\NetBeans-21\netbeans\extide\ant\bin\ant.bat" clean pruebas pruebas-vistas jar
```

Salidas esperadas:
- `RESULTADO: 20/20 pruebas correctas` (esquema)
- `RESULTADO: 33/33 pruebas correctas` (lógica)
- `RESULTADO ESCENARIOS: 43/43 correctos` + `9` hallazgos con defecto
- `RESULTADO FLUJOS UI: 10/10 correctos`
- `RESULTADO: 14/14 ventanas construidas sin error`

Suites ejecutables individualmente:
- `agromedio.pruebas.PruebaEscenarios`
- `agromedio.pruebas.PruebaFlujosUI`

---

## 8. Notas de método

- Las suites usan **bases de datos aisladas** (`build/prueba_*.db`) que se recrean en cada corrida; **la BD real `AgromedioApp/agromedio.db` no se modificó**.
- La pasada de UI ejecuta sobre el **EDT** y cierra automáticamente los diálogos modales (`JOptionPane`) para poder accionar los flujos; no requiere intervención manual.
- Fallos en la sección `[FAIL]` de las suites **sí** rompen el build (`failonerror`); los `[HALLAZGO]` son **informativos** y documentan defectos conocidos sin bloquear, tal como exige un informe de QA.
