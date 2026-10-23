# Plan de Pruebas

**Proyecto:** Sistema de Control Operativo AGROMEDIO
**Versión:** 1.1 – octubre 2026

## 1. Alcance y enfoque

El plan verifica los diez requerimientos funcionales (RF01–RF10), los cuatro no funcionales (RNF01–RNF04) y las reglas de negocio del ciclo operativo. Se aplican tres niveles:

1. **Pruebas de base de datos** (`PruebaEsquema`): estructura, integridad, disparador y vistas.
2. **Pruebas funcionales de lógica de negocio** (`PruebaLogica`): ejecución del ciclo completo contra una base desechable, con asserts sobre estados, cantidades y carga de Excel/CSV/TXT.
3. **Pruebas de interfaz** (`PruebaVistas`): construcción y muestra de cada ventana, más verificación visual manual.

**Criterio de aceptación global:** 100 % de las pruebas automatizadas en verde y ningún defecto crítico o alto abierto.

## 2. Entorno de prueba

| Elemento | Valor |
|---|---|
| Java | JDK 21, compilación `--release 17` |
| Build | Apache Ant con `build.xml` del proyecto |
| Base de datos | SQLite, archivo desechable por suite (`build/prueba_*.db`) |
| Datos | Semilla `seed.sql` con plantilla de demostración, caso de faltante de leche (77 solicitados / 70 recibidos / 7 de faltante) y despacho cerrado |
| Comando | `ant pruebas` (esquema + lógica) y `ant pruebas-vistas` |

## 3. Pruebas automatizadas y resultados

### 3.1 Suite de esquema (`PruebaEsquema`) — 20/20 ✅

| # | Prueba | Criterio de aceptación | Resultado |
|---|---|---|---|
| E01 | Creación de esquema | Las 16 tablas se crean | ✅ |
| E02 | Siembra del ciclo | 6 tablas del ciclo + catálogos con datos | ✅ |
| E03 | Disparador de faltantes | Recibir 70 de 77 L genera faltante = 7 | ✅ |
| E04 | Integridad referencial | Un `INSERT` con FK inexistente es rechazado | ✅ |
| E05 | Restricción de estados | Un estado fuera del catálogo es rechazado | ✅ |
| E06 | Vista de faltantes | `v_faltantes_recepcion` muestra 1 faltante (7 L) | ✅ |
| E07 | Vista de saldo | Solicitado 77 − despachado 70 = 7 | ✅ |
| E08 | Vista de avance | 5 ítems, 100 % | ✅ |
| E09 | Consolidado RF03 | Arroz = 200 lb en 2 mercados | ✅ |
| E10 | Conversión de unidades | 3 bultos de papa = 150 lb | ✅ |
| E11 | Faltante acumulado parcial | Segunda entrega de 5 L deja 75/77 y faltante 2, sin duplicar la fila | ✅ |
| E12 | Faltante acumulado completo | Segunda entrega de 7 L deja 77/77 y faltante 0 | ✅ |
| E13 | Cero en recepción | El `CHECK` rechaza un detalle con cantidad recibida 0 | ✅ |
| E14 | Faltante aceptado en despacho | El seed marca leche 70/77 como `faltante_aceptado = 1` y `v_avance_despacho.con_novedad = 1` | ✅ |
| E15 | Histórico protegido | `DELETE` de una plantilla con compra/alistamiento/despacho falla por trigger/FK | ✅ |
| E16 | Borrado limpio | Una plantilla `CARGADA` sin histórico se elimina junto con su `detalle_plantilla` | ✅ |
| E17 | Cero en alistamiento | El `CHECK`/trigger rechaza `completado = 1` con `cantidad_alistada = 0` | ✅ |
| E18 | Cero en despacho | El `CHECK`/trigger rechaza `verificado = 1` con `cantidad_despachada = 0` | ✅ |
| E19 | Cero en compra | El `CHECK` rechaza un `detalle_compra` con `cantidad_comprada = 0` | ✅ |
| E20 | Hash de carga único | `carga_plantilla` rechaza un segundo registro con el mismo `hash_archivo` | ✅ |

### 3.2 Suite funcional (`PruebaLogica`) — 32/32 ✅

| # | Prueba | RF | Criterio de aceptación | Resultado |
|---|---|---|---|---|
| L01 | Carga desde Excel | RF01/RF02 | Crea plantilla, cliente y detalles desde `.xlsx`; registra productos faltantes en el catálogo | ✅ |
| L02 | Consolidado de producto | RF03 | Arroz = 200 lb, 2 mercados, pendiente 100 | ✅ |
| L03 | Registro de compra | RF04 | La plantilla pasa a `EN_COMPRA` | ✅ |
| L04 | Compra sin proveedor | RF04 | Se rechaza con error de negocio | ✅ |
| L05 | Producto duplicado | RF04 | Se rechaza con error de negocio | ✅ |
| L06 | Recepción parcial | RF05/RF06 | Genera faltante y plantilla en `RECIBIENDO` | ✅ |
| L07 | Cierre de recepción | RF05 | Habilita el alistamiento | ✅ |
| L08 | Copia a alistamiento | RF07 | Copia los productos de la plantilla | ✅ |
| L09 | Completitud de alistamiento | RF08 | No se completa con ítems sin marcar | ✅ |
| L10 | Cierre de alistamiento | RF08 | Pasa la plantilla a `LISTA` | ✅ |
| L11 | Lista de chequeo | RF09 | El despacho crea los ítems a verificar | ✅ |
| L12 | Cierre incompleto | RF10 | No se cierra si falta ítem por verificar | ✅ |
| L13 | Cierre completo | RF10 | Marca `DESPACHADA` al 100 % | ✅ |
| L14 | Control de estados | RNF02 | No se puede despachar una plantilla `CARGADA` | ✅ |
| L15 | Exceso de recepción | RN-05 | Rechaza recibir más de lo pendiente | ✅ |
| L16 | Entrega sin cantidades | D02 | Rechaza una entrega con todas las líneas en 0 | ✅ |
| L17 | Segunda entrega parcial | RN-05/RN-06 | Acumula 100/100 y elimina el faltante | ✅ |
| L18 | Dos entregas 30 + 47 | RN-05/RN-06 | Completar 77 L deja faltante 0 | ✅ |
| L19 | Tope de alistamiento | RN-07 | Alistar 80 cuando se recibieron 70 se rechaza | ✅ |
| L20 | Producto sin recepción | RN-08 | No se completa ni se cierra el alistamiento si un producto tiene recibido 0 | ✅ |
| L21 | Faltante parcial cerrado | RN-07/RN-08 | Se alista por lo recibido (70/77) y el mercado pasa a `LISTA` para despacho con novedad | ✅ |
| L22 | Faltante no aceptado | RN-09/RN-10 | El cierre se bloquea si la cantidad es parcial y `faltante_aceptado = 0` | ✅ |
| L23 | Cantidad cero al verificar | D02/RN-10 | Se rechaza verificar un ítem con cantidad despachada 0 | ✅ |
| L24 | Faltante aceptado sin novedad | RN-10 | El cierre se bloquea si hay faltante aceptado y la novedad está vacía | ✅ |
| L25 | Faltante aceptado con novedad | RN-09/RN-10 | Cierra, marca la plantilla `DESPACHADA` y conserva `faltante_aceptado = 1` | ✅ |
| L26 | Histórico con compras/recepciones | RN-12 | El servicio rechaza eliminar una plantilla con historico | ✅ |
| L27 | Borrado de plantilla limpia | RN-12 | Una plantilla `CARGADA` sin historico se elimina con su detalle | ✅ |
| L28 | Cero al completar alistamiento | D02/RN-11 | Se rechaza completar un producto con cantidad alistada 0 | ✅ |
| L29 | Compra sobre plantilla vacía | RN-01 | Se rechaza la compra y la plantilla permanece en `CARGADA` | ✅ |
| L30 | Carga duplicada | G05/RNF02 | El mismo archivo se rechaza con "Carga ya realizada", sin crear plantillas | ✅ |
| L31 | Carga de archivos planos | G08/RF01 | CSV con `;`, CSV con `,`/BOM/campos entre comillas y TXT con tabulador crean carga y plantilla sin errores | ✅ |
| L32 | Cantidades descriptivas y productos nuevos | G08/RF02 | “9 unidades”, “1 de 300 gr”, “400 gramos”, “1 libra” y “1 rama” se interpretan; crea productos faltantes con unidad y presentación detectadas | ✅ |

### 3.3 Suite de interfaz (`PruebaVistas`) — 13/13 ✅

Construcción y muestra sin error de: Catálogo Clientes, Catálogo Categorías, Catálogo Unidades, Catálogo Proveedores, Productos, Plantillas, Detalle de plantilla, Carga de archivos, Compras, Recepción, Alistamiento, Despacho, Reportes.

## 4. Pruebas manuales de interfaz

| # | Caso | Pasos | Resultado esperado | Estado |
|---|---|---|---|---|
| M01 | Inicio de aplicación | Doble clic en `ejecutar.bat` | Ventana principal con menús y ruta de BD en la barra de estado | ✅ |
| M02 | Apertura y cascada | Abrir varias ventanas desde los menús | Se abren superpuestas en cascada, sin salirse del escritorio | ✅ |
| M03 | Confirmación destructiva | Intentar eliminar un registro con datos | Diálogo de confirmación en español | ✅ |
| M04 | Mensajes de error | Generar un error de negocio (compra sin proveedor) | Mensaje claro, sin excepción técnica visible | ✅ |
| M05 | Reportes | Generar "Faltantes de recepción" y "Saldo del mercado" | Vista previa con datos y botón Imprimir habilitado | ✅ |
| M06 | Acerca del sistema | Menú Ayuda → Acerca del sistema | Diálogo con versión, materias, ruta de BD y aviso de operación mono-usuario sin autenticación | ✅ |

## 5. Pruebas de RNF

| RNF | Prueba | Resultado |
|---|---|---|
| RNF01 | Revisión visual de botones ≥ 48 px, flujos de una ventana por módulo, confirmaciones | ✅ |
| RNF02 | FK, `STRICT`, `CHECK`, disparador, histórico, control de estados y hash único de carga (suite E04, E05, E15, E17, E18, E19, E20, L14, L26, L28, L30) | ✅ |
| RNF03 | `ant pruebas` completo < 10 s; consolidación en consulta única con índices | ✅ |
| RNF04 | Compilación de las cuatro capas por separado; DAO sustituible (se ejecuta contra BD desechables) | ✅ |

## 6. Registro de ejecución

| Fecha | Suite | Comando | Resultado |
|---|---|---|---|
| 2026-09-12 | PruebaEsquema | `ant pruebas` | 10/10 ✅ |
| 2026-09-12 | PruebaLogica | `ant pruebas` | 14/14 ✅ |
| 2026-09-12 | PruebaVistas | `ant pruebas-vistas` | 13/13 ✅ |
| 2026-09-12 | Smoke visual | Ejecución con `-Dagromedio.demo=true` + captura | ✅ |
| 2026-10-22 | PruebaEsquema (RN-05/RN-06) | `ant pruebas` | 13/13 ✅ |
| 2026-10-22 | PruebaLogica (RN-05/RN-06) | `ant pruebas` | 18/18 ✅ |
| 2026-10-22 | PruebaVistas | `ant pruebas-vistas` | 13/13 ✅ |
| 2026-10-22 | PruebaLogica (RN-07/RN-08) | `ant pruebas` | 21/21 ✅ |
| 2026-10-22 | PruebaEsquema (RN-09/RN-10) | `ant pruebas` | 14/14 ✅ |
| 2026-10-22 | PruebaLogica (RN-09/RN-10) | `ant pruebas` | 25/25 ✅ |
| 2026-10-22 | PruebaVistas | `ant pruebas-vistas` | 13/13 ✅ |
| 2026-10-22 | PruebaEsquema (G04/G07) | `ant pruebas` | 19/19 ✅ |
| 2026-10-22 | PruebaLogica (G04/G07) | `ant pruebas` | 28/28 ✅ |
| 2026-10-22 | PruebaVistas | `ant pruebas-vistas` | 13/13 ✅ |
| 2026-10-22 | PruebaLogica (G06) | `ant pruebas` | 29/29 ✅ |
| 2026-10-22 | PruebaEsquema (G05) | `ant pruebas` | 20/20 ✅ |
| 2026-10-22 | PruebaLogica (G05) | `ant pruebas` | 30/30 ✅ |
| 2026-10-22 | PruebaVistas | `ant pruebas-vistas` | 13/13 ✅ |
| 2026-10-22 | PruebaLogica (G08, antes de L32) | `ant clean pruebas` | 31/31 ✅ |
| 2026-10-22 | PruebaLogica (G08 cantidades) | `ant clean pruebas` | 32/32 ✅ |
| 2026-10-22 | Suite completa | `ant clean pruebas jar pruebas-vistas` | 20/20 + 32/32 + 13/13 = 65/65 ✅ |

**Resultado global: 65/65 pruebas automatizadas satisfactorias · 0 defectos abiertos.**

## 7. Pruebas de reglas de negocio

El Tercer Avance incorpora 12 reglas de negocio (RN-01 a RN-12). **Las 12 reglas ya tienen pruebas automatizadas** (E11–E20 y L06, L15–L32) y están incluidas en el resultado 65/65. G06 cerró la brecha de RN-01 con la prueba L29, G05 cerró RNF02 con L30 y G08 cerró la carga de archivos planos con L31 y las cantidades descriptivas con L32.

Las decisiones **D01** (faltante parcial aceptado con novedad obligatoria), **D02** (cero solo como estado pendiente; no se persiste en recepción), **D04** (se mantiene SQLite), **D05** (título Control Operativo), **D06** (mono-usuario sin roles) y **D07** (CSV/TXT) quedaron resueltas el 22/10/2026. Las pruebas P-RN01, P-RN09, P-RN09b, P-RN10, P-RN10b, P-RN11, P-RN12, P-RNF02 y P-G08 ya están implementadas (L22–L32 y E15–E20).
