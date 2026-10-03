# Plan de Pruebas

**Proyecto:** Sistema de Control Operativo AGROMEDIO
**Versión:** 1.0 – septiembre 2026

## 1. Alcance y enfoque

El plan verifica los diez requerimientos funcionales (RF01–RF10), los cuatro no funcionales (RNF01–RNF04) y las reglas de negocio del ciclo operativo. Se aplican tres niveles:

1. **Pruebas de base de datos** (`PruebaEsquema`): estructura, integridad, disparador y vistas.
2. **Pruebas funcionales de lógica de negocio** (`PruebaLogica`): ejecución del ciclo completo contra una base desechable, con asserts sobre estados y cantidades.
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

### 3.1 Suite de esquema (`PruebaEsquema`) — 10/10 ✅

| # | Prueba | Criterio de aceptación | Resultado |
|---|---|---|---|
| E01 | Creación de esquema | Las 15 tablas se crean | ✅ |
| E02 | Siembra del ciclo | 6 tablas del ciclo + catálogos con datos | ✅ |
| E03 | Disparador de faltantes | Recibir 70 de 77 L genera faltante = 7 | ✅ |
| E04 | Integridad referencial | Un `INSERT` con FK inexistente es rechazado | ✅ |
| E05 | Restricción de estados | Un estado fuera del catálogo es rechazado | ✅ |
| E06 | Vista de faltantes | `v_faltantes_recepcion` muestra 1 faltante (7 L) | ✅ |
| E07 | Vista de saldo | Solicitado 77 − despachado 70 = 7 | ✅ |
| E08 | Vista de avance | 5 ítems, 100 % | ✅ |
| E09 | Consolidado RF03 | Arroz = 200 lb en 2 mercados | ✅ |
| E10 | Conversión de unidades | 3 bultos de papa = 150 lb | ✅ |

### 3.2 Suite funcional (`PruebaLogica`) — 14/14 ✅

| # | Prueba | RF | Criterio de aceptación | Resultado |
|---|---|---|---|---|
| L01 | Carga desde Excel | RF01/RF02 | Crea plantilla, cliente y detalles desde `.xlsx` | ✅ |
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

### 3.3 Suite de interfaz (`PruebaVistas`) — 13/13 ✅

Construcción y muestra sin error de: Catálogo Clientes, Catálogo Categorías, Catálogo Unidades, Catálogo Proveedores, Productos, Plantillas, Detalle de plantilla, Carga Excel, Compras, Recepción, Alistamiento, Despacho, Reportes.

## 4. Pruebas manuales de interfaz

| # | Caso | Pasos | Resultado esperado | Estado |
|---|---|---|---|---|
| M01 | Inicio de aplicación | Doble clic en `ejecutar.bat` | Ventana principal con menús y ruta de BD en la barra de estado | ✅ |
| M02 | Apertura y cascada | Abrir varias ventanas desde los menús | Se abren superpuestas en cascada, sin salirse del escritorio | ✅ |
| M03 | Confirmación destructiva | Intentar eliminar un registro con datos | Diálogo de confirmación en español | ✅ |
| M04 | Mensajes de error | Generar un error de negocio (compra sin proveedor) | Mensaje claro, sin excepción técnica visible | ✅ |
| M05 | Reportes | Generar "Faltantes de recepción" y "Saldo del mercado" | Vista previa con datos y botón Imprimir habilitado | ✅ |
| M06 | Acerca del sistema | Menú Ayuda → Acerca del sistema | Diálogo con versión, materias y ruta de BD | ✅ |

## 5. Pruebas de RNF

| RNF | Prueba | Resultado |
|---|---|---|
| RNF01 | Revisión visual de botones ≥ 48 px, flujos de una ventana por módulo, confirmaciones | ✅ |
| RNF02 | FK, `STRICT`, `CHECK`, disparador y control de estados (suite E04, E05, L14) | ✅ |
| RNF03 | `ant pruebas` completo < 10 s; consolidación en consulta única con índices | ✅ |
| RNF04 | Compilación de las cuatro capas por separado; DAO sustituible (se ejecuta contra BD desechables) | ✅ |

## 6. Registro de ejecución

| Fecha | Suite | Comando | Resultado |
|---|---|---|---|
| 2026-09-12 | PruebaEsquema | `ant pruebas` | 10/10 ✅ |
| 2026-09-12 | PruebaLogica | `ant pruebas` | 14/14 ✅ |
| 2026-09-12 | PruebaVistas | `ant pruebas-vistas` | 13/13 ✅ |
| 2026-09-12 | Smoke visual | Ejecución con `-Dagromedio.demo=true` + captura | ✅ |

**Resultado global: 37/37 pruebas automatizadas satisfactorias · 0 defectos abiertos.**
