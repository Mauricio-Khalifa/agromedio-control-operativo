# Matriz de Trazabilidad de Reglas de Negocio (RN)

**Proyecto:** Sistema de Control Operativo AGROMEDIO
**Fuente:** Tercer Avance de Proyecto Integrador (12/09/2026), sección 1.3, Tabla 2 – Reglas de negocio
**Versión:** 1.0 – octubre 2026
**Propósito:** dejar explícito qué regla de negocio está cubierta, con qué código, con qué tabla y con qué prueba, y qué falta antes de modificar la aplicación.

**Leyenda de estado:**

| Símbolo | Significado |
|---|---|
| ✅ | Cumple y está verificado por una prueba o por una restricción de base de datos |
| ⚠️ | Cumple parcialmente: existe una parte, pero falta una validación, una prueba o una decisión |
| ❌ | No cumple con la regla tal como está redactada |
| 🔵 | Requiere decisión de negocio antes de implementar |

---

## 1. Matriz RN → módulo → base de datos → código → prueba

| RN | Regla (resumen) | Módulo / pantalla | Objetos de base de datos | Implementación actual | Prueba actual | Estado |
|---|---|---|---|---|---|---|
| **RN-01** | Una plantilla debe tener al menos un producto y una cantidad mayor que cero | Módulo 0 – `FrmPlantillas`, `FrmDetallePlantilla` | `plantilla_operativa`, `detalle_plantilla.cantidad_solicitada > 0` | Implementada (G06): `agregarProducto()` exige cantidad `> 0` y evita duplicados; `CompraServicio.registrarCompra()` exige al menos un detalle antes de crear la compra y pasar a `EN_COMPRA` | `PruebaLogica` – "RN-01: no se registra compra de una plantilla vacía" (L29); "RF04: se rechaza producto duplicado en plantilla" (L05) | ✅ |
| **RN-02** | Cada producto debe tener categoría y unidad de medida | Catálogos – `FrmProductos` | `producto.id_categoria`, `id_unidad_compra`, `id_unidad_despacho` con FK `NOT NULL` | `ProductoDAOSqlite.guardar()` inserta/actualiza con las tres referencias | No hay prueba específica de producto sin categoría/unidad; la restricción `NOT NULL` + FK está en el esquema | ✅ |
| **RN-03** | Una compra debe estar relacionada con una plantilla | Módulo 1 – `FrmCompras` | `compra.id_plantilla` FK `NOT NULL` | `CompraServicio.registrarCompra()` valida que la plantilla exista y guarda la compra | `PruebaLogica` – L03 cubre el flujo; la FK está en el esquema | ✅ |
| **RN-04** | Una recepción debe estar relacionada con una compra existente | Módulo 2 – `FrmRecepcion` | `recepcion.id_compra` FK `NOT NULL` | `RecepcionServicio.registrarRecepcion()` valida `compra == null` antes de guardar | `PruebaLogica` – L06 cubre el flujo; la FK está en el esquema | ✅ |
| **RN-05** | Una compra puede recibirse en una o varias entregas parciales | Módulo 2 – `FrmRecepcion` | `recepcion` (varias filas por `id_compra`), `detalle_recepcion` | Implementada: `RecepcionServicio.registrarRecepcion()` permite N entregas mientras la compra no esté `RECIBIDA`; `FrmRecepcion` muestra el acumulado, el historial y una columna “Recibir ahora”; valida que no se reciba más de lo pendiente | `PruebaLogica` – "RN-05: no se recibe más de lo pendiente", "RN-05/RN-06: segunda entrega acumula…", "RN-05/RN-06: dos entregas 30 + 47 completan 77 L" | ✅ |
| **RN-06** | El faltante se calcula con la cantidad comprada menos la recibida **acumulada** | Módulo 2 y reportes – `FrmRecepcion`, `FrmReportes` | `trg_detalle_recepcion_faltante` (suma acumulada), `detalle_recepcion.cantidad_faltante`, vistas `v_faltantes_recepcion` (una fila por detalle) y `v_saldo_plantilla` | Implementada: el trigger suma todas las entregas del mismo `id_detalle_compra`; la vista agrupa por detalle y no duplica el faltante; el servicio omite las líneas en 0 (D02) | `PruebaEsquema` – E11 "segunda entrega parcial acumula 75/77 -> 2", E12 "segunda entrega completa 77/77 -> 0"; `PruebaLogica` – L17 y L18 | ✅ |
| **RN-07** | No se puede alistar más de lo disponible o recibido | Módulo 3 – `FrmAlistamiento` | `detalle_alistamiento.cantidad_alistada`; acumulado de `detalle_recepcion` convertido a unidad de despacho | Implementada: el DAO expone `cantidadRecibida` por producto; `AlistamientoServicio.registrarAvance()` exige `cantidad_alistada <= min(solicitado, recibido)` y la UI muestra la columna “Recibido” | `PruebaLogica` – "RN-07: no se alista más de lo recibido (80 > 70)" | ✅ |
| **RN-08** | Un producto pendiente de recepción no puede marcarse completo en alistamiento | Módulo 3 – `FrmAlistamiento` | `detalle_alistamiento.completado`, `detalle_recepcion`, `detalle_compra` | Implementada: no se permite `completado = true` si el producto tiene recibido acumulado 0; `completar()` bloquea el alistamiento si algún producto no tiene recepción. Un faltante parcial ya cerrado se alista por lo recibido y se gestiona como novedad en despacho (D01/D02) | `PruebaLogica` – "RN-08: no se completa un producto sin recepción (recibido 0)", "RN-07/RN-08: faltante parcial cerrado se alista por lo recibido" | ✅ |
| **RN-09** | Un mercado no puede pasar a despacho con productos faltantes **no aceptados** o sin verificar | Módulos 3 y 4 – `FrmAlistamiento`, `FrmDespacho` | `plantilla_operativa.estado`, `despacho`, `detalle_despacho.faltante_aceptado` | Implementada: `DespachoServicio.iniciar()` exige estado `LISTA`; `cerrar()` bloquea ítems sin verificar y faltantes no aceptados. Un faltante parcial aceptado se permite solo con cantidad `> 0` y novedad obligatoria (D01) | `PruebaLogica` – L22, L25; el seed despacha 70 de 77 con `faltante_aceptado = 1` y novedad | ✅ |
| **RN-10** | Un despacho solo se confirma cuando los productos requeridos están completos o con **faltante aceptado**, y todos están verificados | Módulo 4 – `FrmDespacho` | `detalle_despacho.cantidad_despachada`, `detalle_despacho.verificado`, `detalle_despacho.faltante_aceptado`; vista `v_avance_despacho.con_novedad` | Implementada: `DespachoServicio.cerrar()` compara cada cantidad contra lo solicitado, exige `> 0` al verificar, bloquea faltante no aceptado y exige novedad si hay faltante aceptado. `FrmDespacho` muestra “Solicitado”, “Faltante aceptado” y distingue el cierre con novedad | `PruebaLogica` – L22, L23, L24, L25; `PruebaEsquema` – E14; reporte de avance muestra `CON NOVEDAD` | ✅ |
| **RN-11** | Las cantidades no pueden ser negativas ni cero en operaciones completadas o verificadas de compra, recepción, alistamiento o despacho | Módulos 1–4 | `detalle_compra.cantidad_comprada > 0`; `detalle_recepcion.cantidad_recibida > 0`; `detalle_alistamiento.cantidad_alistada >= 0` con `CHECK (completado = 0 OR cantidad_alistada > 0)`; `detalle_despacho.cantidad_despachada >= 0` con `CHECK (verificado = 0 OR cantidad_despachada > 0)`; triggers para bases migradas | Implementada (G07): compra y recepción exigen `> 0`; recepción omite líneas 0 y rechaza una entrega toda en 0; alistamiento y despacho permiten 0 solo como pendiente y bloquean completar/verificar en 0 en servicio, CHECK y trigger | `PruebaEsquema` – E13, E17, E18, E19; `PruebaLogica` – L16, L23, L28 | ✅ |
| **RN-12** | Los registros históricos no se eliminan si ya están relacionados con compras, recepciones, alistamientos o despachos | Módulos 0–4 y catálogos | `plantilla_operativa` con `ON DELETE RESTRICT` desde `compra`, `alistamiento` y `despacho`; `detalle_plantilla` conserva `CASCADE`; trigger `trg_plantilla_historico` | Implementada (G04): el esquema nuevo usa `RESTRICT`; las bases migradas instalan el trigger; `PlantillaServicio.eliminar()` bloquea si hay histórico y solo permite borrar una plantilla `CARGADA` sin compras/recepciones/alistamientos/despachos | `PruebaEsquema` – E15, E16; `PruebaLogica` – L26, L27 | ✅ |

**Resumen:** 12 reglas cumplen (RN-01 a RN-12) y 0 están parciales. G01 a G10 quedaron cerradas: las brechas de reglas se resolvieron con G01-G07 y las brechas de alcance con G08 (CSV/TXT y cantidades descriptivas), G09 (mono-usuario sin roles) y G10 (diccionario de datos y EDT). D03 se cerró como sinónimo de plantilla/mercado, sin entidad `contrato` separada. La única acción externa pendiente es reemplazar el PDF del tercer avance.

---

## 2. Brechas y acciones necesarias

| ID | RN | Brecha | Acción propuesta | Archivos impactados | Criterio de aceptación |
|---|---|---|---|---|---|
| **G01** | RN-05, RN-06 | La UI no permite varias entregas parciales y el faltante no es acumulado | ✅ Implementada: recepción con N entregas por compra, faltante como `comprado − SUM(recibido)`, historial de entregas y vista sin duplicados | `src/sql/schema.sql`, `docs/base_de_datos/schema.sql`, `RecepcionDAO.java`, `RecepcionDAOSqlite.java`, `RecepcionServicio.java`, `FrmRecepcion.java`, `ConsultaDAOSqlite.java`, `PruebaEsquema.java`, `PruebaLogica.java` | Dos recepciones de 30 y 47 L sobre una compra de 77 L producen acumulado 77 y faltante 0; una de 70 produce faltante 7; el reporte no duplica el faltante |
| **G02** | RN-07, RN-08 | El alistamiento no tiene tope contra lo recibido ni bloqueo por pendientes | ✅ Implementada: expone la cantidad recibida por producto, valida `cantidad_alistada <= min(solicitado, recibido)`, impide completar con recibido 0 y valida en `completar()`; `iniciar()` exige recepción cerrada | `AlistamientoServicio.java`, `AlistamientoDAOSqlite.java`, `DetalleAlistamiento.java`, `FrmAlistamiento.java`, `PruebaLogica.java` | Alistar 80 cuando se recibieron 70 se rechaza; un producto sin recepción no se completa; un faltante parcial cerrado se alista por lo recibido y queda listo para despacho con novedad |
| **G03** | RN-09, RN-10 | `LISTA` se alcanza con faltantes y el cierre solo mira el flag `verificado` | ✅ Implementada: `detalle_despacho.faltante_aceptado`, validación de cantidad contra lo solicitado, bloqueo de faltante no aceptado/cantidad 0, novedad obligatoria y distinción “completo” / “con novedad” en UI y reporte | `DespachoServicio.java`, `FrmDespacho.java`, `DetalleDespacho.java`, `DespachoDAOSqlite.java`, `schema.sql`, `FilaAvance.java`, `ConsultaDAOSqlite.java`, `ReporteServicio.java`, `PruebaLogica.java`, `PruebaEsquema.java` | Un mercado con faltante no aceptado no se despacha; con novedad aceptada sí; no se cierra con cantidad 0 o superior a la requerida; el seed conserva el caso 70/77 |
| **G04** | RN-12 | El esquema permite borrado en cascada del histórico | ✅ Implementada: FK `RESTRICT` en compra, alistamiento y despacho; cascada solo en `detalle_plantilla`; trigger `trg_plantilla_historico` para bases migradas; `PlantillaServicio.eliminar()` valida estado e histórico | `src/sql/schema.sql`, `docs/base_de_datos/schema.sql`, `src/sql/migracion_g04_g07.sql`, `PlantillaDAO.java`, `PlantillaDAOSqlite.java`, `PlantillaServicio.java`, `ConexionBD.java`, `PruebaEsquema.java`, `PruebaLogica.java` | Borrar una plantilla con compras/recepciones/alistamientos/despachos falla; borrar una plantilla `CARGADA` sin histórico funciona |
| **G05** | RNF02 (asociada) | No existe identificador único de carga/ciclo y una carga repetida duplica la demanda | ✅ Implementada: tabla `carga_plantilla` con hash SHA-256 único, fecha, archivo, usuario y resumen; `plantilla_operativa.id_carga`; `CargaExcelServicio` rechaza el mismo archivo con "Carga ya realizada"; migración idempotente para bases existentes | `src/sql/schema.sql`, `src/sql/migracion_g05.sql`, `docs/base_de_datos/schema.sql`, `CargaPlantilla.java`, `CargaPlantillaDAO.java`, `CargaPlantillaDAOSqlite.java`, `CargaExcelServicio.java`, `PlantillaOperativa.java`, `PlantillaDAOSqlite.java`, `FrmCargaExcel.java`, `ConexionBD.java`, `PruebaEsquema.java`, `PruebaLogica.java` | Cada carga tiene un ID único; cargar el mismo archivo dos veces muestra "Carga ya realizada" y no crea plantillas; las plantillas quedan agrupadas por `id_carga` |
| **G06** | RN-01 | Se puede crear y comprar sobre una plantilla vacía | ✅ Implementada: `registrarCompra()` valida que la plantilla tenga al menos un detalle antes de crear la compra; la plantilla permanece en `CARGADA` y no avanza a `EN_COMPRA` | `CompraServicio.java`, `PruebaLogica.java` | Una plantilla sin productos no avanza a compra; el mensaje indica que debe agregar al menos un producto |
| **G07** | RN-11 | Recepción, alistamiento y despacho aceptan cero | ✅ Implementada: recepción omite líneas 0 y rechaza entrega vacía; alistamiento/despacho permiten 0 solo como pendiente; `CHECK` en el esquema y triggers en bases migradas bloquean completar/verificar en 0; servicio y UI validan antes de guardar | `schema.sql`, `docs/base_de_datos/schema.sql`, `src/sql/migracion_g04_g07.sql`, `ConexionBD.java`, `RecepcionServicio.java`, `AlistamientoServicio.java`, `DespachoServicio.java`, `PruebaEsquema.java`, `PruebaLogica.java` | Cero y negativos se rechazan al completar/verificar; las pruebas cubren cada módulo |
| **G08** | RF01 (asociada) | Solo se leían `.xlsx`/`.xls`, no archivos planos, y la cantidad debía ser numérica | ✅ Implementada: `CargaExcelServicio` acepta `.csv` y `.txt` además de Excel; detecta `;`, `,` o tabulador, admite UTF-8/BOM y campos entre comillas; interpreta cantidades descriptivas (“9 unidades”, “1 de 300 gr”, “400 gramos”, “1 libra”) y crea productos faltantes con su unidad y presentación; `FrmCargaExcel` ajusta filtro y mensajes | `CargaExcelServicio.java`, `FrmCargaExcel.java`, `PruebaLogica.java`, `docs/01_informe_tecnico.md`, `docs/05_manual_usuario.md` | Un CSV y un TXT con las mismas columnas se cargan correctamente (L31); las cantidades descriptivas y los productos nuevos se procesan sin errores (L32); el manual documenta formatos y cantidades |
| **G09** | Actores | No hay autenticación ni roles para “Administrador del sistema” y “Proveedor” | ✅ Cerrada por decisión: la versión es mono-usuario y sin autenticación; los actores del documento son roles operativos, no cuentas. La UI lo informa y se deja módulo de usuarios para v2 | `FrmPrincipal.java`, `docs/01_informe_tecnico.md`, `docs/04_casos_uso.md`, `docs/09_edt_y_gestion_cambios.md` | La decisión queda registrada; el alcance real está reflejado en UI y documentación |
| **G10** | Documentación | El EDT del PDF incluye PostgreSQL, diccionario de datos y gestión de cambios/riesgos que no están en el repo | ✅ Cerrada en repositorio: diccionario de datos y EDT/RACI/riesgos agregados; documentación alineada a SQLite, 16 tablas, CSV/TXT y 65/65. Queda acción externa de reemplazar el PDF | `docs/01_informe_tecnico.md`, `docs/02_matriz_trazabilidad.md`, `docs/06_cronograma_y_acta.md`, `docs/08_diccionario_datos.md`, `docs/09_edt_y_gestion_cambios.md`, `docs/README.md`, `README.md` | Los documentos del repo coinciden con el alcance real; el PDF externo se reemplaza con la sección 7 del EDT |

---

## 3. Plan de pruebas de las RN

Las pruebas **P-RN01, P-RN05, P-RN06, P-RN06b, P-RN07, P-RN08, P-RN09, P-RN09b, P-RN10, P-RN10b, P-RN11 y P-RN12 ya están implementadas** en `PruebaLogica` (L06, L17-L29) y `PruebaEsquema` (E11-E19). **P-RNF02 (G05) también está cubierta** por L30 y E20: carga duplicada rechazada con "Carga ya realizada" y hash único en `carga_plantilla`. **P-G08** está cubierta por L31 (carga de un CSV con `;`, un CSV con `,`/BOM/campos entre comillas y un TXT con tabulador) y por L32 (cantidades descriptivas y creación automática de productos faltantes).

| ID | RN | Caso de prueba | Precondición | Resultado esperado |
|---|---|---|---|---|
| P-RN01 | RN-01 | Crear plantilla sin productos e intentar registrar compra | Plantilla vacía en `CARGADA` | Se rechaza con mensaje “agregue al menos un producto” |
| P-RN05 | RN-05 | Registrar dos recepciones parciales sobre la misma compra | Compra de 77 L en `REGISTRADA` | Se guardan dos `recepcion`; la compra sigue abierta hasta el cierre |
| P-RN06 | RN-06 | Recibir 30 L y luego 47 L | Compra de 77 L | Faltante acumulado: 47 después de la primera, 0 después de la segunda |
| P-RN06b | RN-06 | Recibir 70 L de 77 L | Compra de 77 L | Faltante acumulado: 7 L, una sola fila en el reporte |
| P-RN07 | RN-07 | Alistar 80 lb cuando se recibieron 70 lb | Producto con 70 lb recibidas | Se rechaza por superar lo disponible |
| P-RN08 | RN-08 | Marcar completo un producto sin recepción (recibido 0) | Producto con recibido acumulado 0 | Se rechaza y se indica que no tiene recepción; un faltante parcial cerrado sí se alista por lo recibido |
| P-RN09 | RN-09 | Cerrar despacho de un mercado con faltante no aceptado | Plantilla con faltante | Se bloquea el cierre y se indica “faltante no aceptado” |
| P-RN09b | RN-09 | Cerrar despacho de un mercado con faltante parcial aceptado y novedad | Plantilla con novedad y `faltante_aceptado = 1` | Se permite, dejando la novedad registrada y el mercado marcado “con novedad” |
| P-RN10 | RN-10 | Cerrar despacho con un ítem verificado pero cantidad 0 | Despacho en verificación | Se bloquea la verificación/cierre |
| P-RN10b | RN-10 | Cerrar despacho con todos los ítems y cantidades correctas | Despacho en verificación | Se marca `DESPACHADA` y avance 100 % |
| P-RN11 | RN-11 | Registrar cantidad 0 o negativa en compra, recepción, alistamiento y despacho | Cada módulo | Se rechaza al completar/verificar; en recepción la línea 0 no se persiste; 0 solo se acepta como pendiente en alistamiento/despacho |
| P-RN12 | RN-12 | Intentar borrar una plantilla con histórico | Plantilla con compra/recepción/despacho | La operación falla; el histórico se conserva |
| P-RNF02 | RNF02 | Cargar dos veces el mismo archivo | Archivo `.xlsx` válido | Se rechaza con "Carga ya realizada" y no se duplica la demanda (L30) |
| P-G08 | RF01 | Cargar CSV con `;`, CSV con `,`/BOM y TXT con tabulador; cargar cantidades descriptivas con productos nuevos | Todos con las 7 columnas; los productos pueden existir o no | Cada archivo crea una carga y una plantilla; las cantidades se interpretan y los productos faltantes se registran; no hay errores (L31, L32) |
| P-RNF03 | RNF03 | Cargar un archivo de 1.000 filas | Archivo de prueba | La consolidación termina en menos de 5 segundos |

---

## 4. Decisiones: estado y resolución

| ID | Tema | Estado | Resolución |
|---|---|---|---|
| **D01** | RN-09 / RN-10 y novedades | ✅ Resuelta – 22/10/2026 | Un mercado con faltante **sí** puede despacharse, pero solo si el faltante es **parcial** (`cantidad_despachada > 0`), está **aceptado explícitamente** a nivel de detalle y tiene una **novedad obligatoria**. Un faltante no aceptado o un producto con cantidad 0 bloquea el despacho. RN-09 se interpreta como “faltantes no aceptados” y RN-10 como “productos completos o con faltante aceptado y verificado”. |
| **D02** | RN-11 y el cero | ✅ Resuelta – 22/10/2026 | El cero **no es una cantidad válida** para una operación completada o verificada. En recepción, las líneas con 0 no se persisten; si todas son 0, se rechaza la recepción. En alistamiento y despacho, 0 solo significa “pendiente”; para completar o verificar se exige `> 0`. Un producto con 0 recibido no puede alistarse ni despacharse y bloquea el mercado. |
| **D03** | “Contratos” en RF02/RF03 | ✅ Resuelta – 22/10/2026 | Para este prototipo, “contrato” se interpreta como la programación cliente–mercado representada por `plantilla_operativa` (`id_cliente`, `nombre_mercado`, `fecha_programada`); **no se crea una entidad `contrato` separada**. La consolidación RF03 se calcula por producto sobre los detalles de la plantilla. Si el cliente exige contratos con vigencia o condiciones comerciales propias, se trata como cambio de alcance. |
| **D04** | PostgreSQL vs. SQLite | ✅ Resuelta – 22/10/2026 | **Se mantiene SQLite** como motor oficial del proyecto. No se migra a PostgreSQL. El EDT corregido está en `docs/09_edt_y_gestion_cambios.md`; el PDF externo debe reemplazar “Desarrollo de conexión con PostgreSQL” por “SQLite”. |
| **D05** | Título del proyecto | ✅ Resuelta – 22/10/2026 | Se adopta **“Sistema de Control Operativo AGROMEDIO”** porque el alcance cubre compras, recepción, alistamiento, despacho y reportes, no solo inventario. Se unifica en portada, README, UI, informe y acta. El PDF externo debe actualizarse. |
| **D06** | Roles/actores | ✅ Resuelta – 22/10/2026 | **No se implementan usuarios ni roles en esta versión.** La aplicación es mono-usuario y el acceso se controla con la sesión del sistema operativo. Los actores del documento son roles operativos, no cuentas. Autenticación, permisos y bitácora quedan para una versión futura. |
| **D07** | Archivos planos | ✅ Resuelta – 22/10/2026 | **Se aceptan CSV y TXT** además de `.xlsx`/`.xls`. Codificación UTF-8 (con o sin BOM), delimitador detectado entre `;`, `,` y tabulador, campos entre comillas y la misma estructura de 7 columnas. Implementado en L31. |

### 4.1 Consecuencias de D01 (despacho con faltante aceptado)

- El faltante aceptable es solo **parcial**: `cantidad_despachada > 0`. Un producto con cantidad 0 no se puede despachar.
- El faltante debe marcarse a nivel de `detalle_despacho` con `faltante_aceptado = 1` y una observación obligatoria.
- Todos los ítems deben estar verificados.
- Si el faltante no está aceptado, el despacho se bloquea.
- El reporte y la UI deben distinguir “despachado completo” de “despachado con novedad”.
- El caso 77/70 del seed sigue siendo válido y ya está marcado con `faltante_aceptado = 1` y su novedad.

### 4.2 Consecuencias de D02 (cantidad cero)

| Módulo | Regla acordada |
|---|---|
| Compra | `cantidad_comprada > 0` (ya existe en el esquema). |
| Recepción | Las líneas con 0 no se persisten; si todas son 0, se rechaza la recepción. `detalle_recepcion.cantidad_recibida > 0`. |
| Alistamiento | `cantidad_alistada = 0` solo es válido con `completado = 0` (pendiente). Para `completado = 1` se exige `> 0`. Implementado: `CHECK (completado = 0 OR cantidad_alistada > 0)` y triggers para bases migradas. |
| Despacho | `cantidad_despachada = 0` solo es válido con `verificado = 0` (pendiente). Para `verificado = 1` se exige `> 0`. Implementado: `CHECK (verificado = 0 OR cantidad_despachada > 0)` y triggers para bases migradas. |
| Producto no recibido | Si el acumulado recibido es 0, el producto no puede alistarse ni despacharse; el mercado queda bloqueado. |

### 4.3 Consecuencias de D04 (SQLite)

- SQLite se mantiene como motor oficial; no se modifica la capa de datos ni el empaquetado.
- El EDT del Tercer Avance debe corregirse en la próxima revisión del documento.
- `docs/01_informe_tecnico.md` ya justifica SQLite frente a PostgreSQL/MySQL; esa justificación se conserva.

### 4.4 Consecuencias de D05 (título)

- El nombre oficial es **Sistema de Control Operativo AGROMEDIO**.
- Se usa en portada, README, barra de título, informe, acta y documentación de soporte.
- El PDF del tercer avance debe reemplazar “Control de Inventario” por el nombre oficial.

### 4.5 Consecuencias de D06 (roles/actores)

- No hay pantalla de login, tabla de usuarios ni permisos por perfil.
- La aplicación opera con un usuario único por equipo y base local.
- Los actores de los casos de uso son roles del proceso: coordinador, compras, recepción, alistamiento y despacho.
- La UI informa el modo mono-usuario en el diálogo “Acerca de”.
- La autenticación y la bitácora se documentan como evolución futura, no como deuda de esta entrega.

### 4.6 Consecuencias de D07 (archivos planos)

- Formatos aceptados: `.xlsx`, `.xls`, `.csv` y `.txt`.
- Codificación: UTF-8; se ignora el BOM si está presente.
- Delimitadores admitidos: punto y coma, coma o tabulador; se detectan en el encabezado.
- Se admiten campos entre comillas y comillas escapadas (`""`).
- La estructura es la misma del Excel: `Cliente`, `Contacto`, `Telefono`, `Mercado`, `Fecha`, `Producto`, `Cantidad`.
- La deduplicación SHA-256 aplica también a CSV/TXT.

---

## 5. Relación con los requerimientos existentes

| Requerimiento | Relación con las RN |
|---|---|
| RF01, RF02 | RN-01, RN-02, RNF02 (carga, ciclo, plantilla mínima) |
| RF03, RF04 | RN-01, RN-03, RN-11 (consolidación y compra) |
| RF05, RF06 | RN-04, RN-05, RN-06, RN-11 (recepción y faltantes) |
| RF07, RF08 | RN-07, RN-08, RN-11 (alistamiento) |
| RF09, RF10 | RN-09, RN-10, RN-11, RN-12 (despacho y cierre) |
| RNF01 | No tiene RN asociada; se mantiene la usabilidad de planta |
| RNF02 | RN-12 y G05 (ciclo/carga e histórico) |
| RNF03 | P-RNF03 (prueba de 1.000 filas) |
| RNF04 | La matriz no cambia la arquitectura por capas |

---

## 6. Hallazgos de desalineación documental

Al preparar esta matriz se encontraron afirmaciones de la documentación existente que no coinciden con el código. Conviene corregirlas antes de la entrega, incluso si no se implementan las RN:

| Documento | Afirmación actual | Realidad en el código | Acción |
|---|---|---|---|
| `docs/04_casos_uso.md` – CU04 | “Segunda entrega: se registra sobre el mismo detalle y el faltante se recalcula” | ✅ Ahora coincide: G01 eliminó `yaRegistrada` y el trigger acumula el faltante | ✅ Corregido con G01 el 22/10/2026 |
| `docs/04_casos_uso.md` – CU03 | “Producto repetido en la compra → mensaje de producto duplicado y no guarda” | `CompraServicio.registrarCompra()` no valida duplicados; solo `PlantillaServicio.agregarProducto()` lo hace | Agregar validación en compra o corregir el caso de uso |
| `docs/02_matriz_trazabilidad.md` – RNF02 | “Cada archivo cargado crea su propia plantilla con llave propia” | ✅ Ahora coincide: G05 agregó `carga_plantilla` y `plantilla_operativa.id_carga`; un archivo puede crear varias plantillas bajo la misma carga | ✅ Corregido con G05 el 22/10/2026 |
| `docs/01_informe_tecnico.md` | “10 interfaces DAO” | Hay 12 interfaces DAO (G05 agregó `CargaPlantillaDAO`) | ✅ Corregido el 22/10/2026; actualizado con G05 |
| `docs/01_informe_tecnico.md` | “14 ventanas internas” | Hay 13 instancias de ventana registradas | ✅ Corregido el 22/10/2026 |
| PDF Tercer Avance – portada | “Sistema de Control de Inventario” | El repositorio usa “Sistema de Control Operativo AGROMEDIO” | ✅ D05 resuelta; reemplazar el título en el PDF |
| PDF Tercer Avance – EDT | “Desarrollo de conexión con PostgreSQL” | El proyecto usa SQLite y D04 confirmó que se mantiene | Corregir el EDT a SQLite en la próxima revisión del PDF |
| `docs/01_informe_tecnico.md` | “Solo carga Excel” | G08 agregó CSV/TXT | ✅ Corregido el 22/10/2026 |
| Documentación | Sin diccionario ni EDT | G10 agregó `docs/08_diccionario_datos.md` y `docs/09_edt_y_gestion_cambios.md` | ✅ Cerrado en repositorio |

---

## 7. Conclusión

La matriz deja claro que **el proyecto cumple los RF/RNF** y que **G01 a G10 quedaron cerradas**:

1. **G01-G07:** reglas de negocio y robustez del ciclo operativo.
2. **G08:** carga de CSV/TXT además de Excel, cantidades descriptivas y alta automática de productos faltantes.
3. **G09:** alcance mono-usuario sin autenticación, documentado y visible en la UI.
4. **G10:** diccionario de datos, EDT, RACI, riesgos y gestión de cambios en el repositorio.

**D01 a D07 quedaron resueltas** el 22/10/2026. **D03** (“contratos”) se cerró como sinónimo de plantilla/mercado: no se crea una entidad separada; si el cliente exige contratos con vigencia o condiciones propias, debe tratarse como cambio de alcance. SQLite se mantiene como motor oficial.

**Acción externa pendiente:** reemplazar el PDF del tercer avance con las correcciones de la sección 7 de `docs/09_edt_y_gestion_cambios.md`.
