# Cronograma y Acta de Entrega

**Proyecto:** Sistema de Control Operativo AGROMEDIO
**Versión:** 1.0 – septiembre 2026

---

## 1. Cronograma por fases

| Fase | Actividades | Entregable | Fecha inicio | Fecha fin | Estado |
|---|---|---|---|---|---|
| F1 – Levantamiento | Preparación de visita, entrevistas por función, observación en planta, registro de hallazgos (ENT/DOC/OBS) | Guion de entrevista, notas de visita, flujo actual | 04/09/2026 | 11/09/2026 | ✅ Completada |
| F2 – Análisis y diseño | RF01–RF10 y RNF01–RNF04, diagrama entidad-relación aprobado, máquina de estados del ciclo, casos de uso | Documento de requisitos, ER, matriz de trazabilidad | 08/09/2026 | 18/09/2026 | ✅ Completada |
| F3 – Diseño de BD | 15 tablas, disparador de faltantes, 3 vistas, índices, semilla de demostración | `schema.sql`, `seed.sql` | 16/09/2026 | 23/09/2026 | ✅ Completada |
| F4 – Construcción | Capas modelo/datos/lógica, módulos 0–4, reportes, interfaz Swing, carga Excel con POI | Aplicación funcional (`dist/AgromedioApp.jar`) | 18/09/2026 | 30/09/2026 | ✅ Completada |
| F5 – Verificación | Suites de esquema, lógica y vistas; smoke visual; corrección de hallazgos | Plan de pruebas con resultados 37/37 | 28/09/2026 | 02/10/2026 | ✅ Completada |
| F6 – Documentación y entrega | Informe técnico, manual de usuario, paquete estándar PSI, acta de aceptación | Carpeta `docs/` y paquete de entrega | 01/10/2026 | 04/10/2026 | ✅ Completada |

**Cronograma visual (semanas de septiembre–octubre 2026)**

```
F1 Levantamiento   |####|
F2 Análisis        |  #####|
F3 Diseño BD       |     ####|
F4 Construcción    |      #########|
F5 Verificación    |           ####|
F6 Entrega         |              ####|
                   04  11  18  25  02  04
                   sep             oct
```

## 2. Paquete de entregables

| # | Entregable | Ubicación | Verificado |
|---|---|---|---|
| 1 | Aplicación ejecutable + dependencias | `AgromedioApp/dist/AgromedioApp.jar`, `dist/lib/` | ✅ |
| 2 | Lanzador de doble clic | `AgromedioApp/ejecutar.bat` | ✅ |
| 3 | Código fuente completo (70 archivos Java) | `AgromedioApp/src/` | ✅ |
| 4 | Guiones de base de datos | `docs/base_de_datos/schema.sql`, `seed.sql` | ✅ |
| 5 | Informe técnico | `docs/01_informe_tecnico.md` | ✅ |
| 6 | Matriz de trazabilidad RF/RNF | `docs/02_matriz_trazabilidad.md` | ✅ |
| 7 | Plan de pruebas | `docs/03_plan_pruebas.md` | ✅ |
| 8 | Casos de uso | `docs/04_casos_uso.md` | ✅ |
| 9 | Manual de usuario | `docs/05_manual_usuario.md` | ✅ |
| 10 | Cronograma y acta de entrega | `docs/06_cronograma_y_acta.md` | ✅ |
| 11 | Capturas del sistema | `docs/imagenes/` | ✅ |
| 12 | Resultados de verificación | `ant pruebas` (10/10 + 14/14), `ant pruebas-vistas` (13/13) | ✅ |

## 3. Resultados de verificación de la entrega

| Verificación | Resultado |
|---|---|
| Compilación limpia (`ant clean jar`) | ✅ |
| Suite de esquema | 10/10 ✅ |
| Suite funcional RF01–RF10 | 14/14 ✅ |
| Suite de ventanas | 13/13 ✅ |
| Ejecución de la aplicación (smoke visual) | ✅ |
| **Total** | **37/37** |

---

# ACTA DE ENTREGA Y ACEPTACIÓN DEL PROTOTIPO

**Entregable:** Sistema de Control Operativo AGROMEDIO – prototipo funcional de escritorio con módulos de carga de datos, compras, recepción, alistamiento, despacho y reportes, acompañado del paquete documental.

**Fecha de entrega:** ____ de ______________ de 2026

**Lugar:** ______________________________________________

### 1. Alcance entregado

- Aplicación de escritorio Java/Swing empaquetada en `dist/AgromedioApp.jar` con lanzador `ejecutar.bat`.
- Base de datos SQLite con 15 tablas, disparador de cálculo de faltantes, 3 vistas e integridad referencial.
- Módulos: Carga Excel (RF01–RF02), Compras (RF03–RF04), Recepción (RF05–RF06), Alistamiento (RF07–RF08), Despacho (RF09–RF10).
- Cumplimiento de RNF01–RNF04 según matriz de trazabilidad.
- Informe técnico, matriz de trazabilidad, plan de pruebas, casos de uso, manual de usuario, cronograma y guiones SQL.

### 2. Criterios de aceptación

| # | Criterio | Cumple (Sí/No/Observación) |
|---|---|---|
| A1 | La aplicación inicia desde `ejecutar.bat` sin asistencia técnica | |
| A2 | Se carga una plantilla desde un archivo `.xlsx` real | |
| A3 | El consolidado de compras refleja la suma correcta por producto | |
| A4 | La recepción muestra el faltante por producto de forma explícita | |
| A5 | El alistamiento exige completar todos los ítems para cerrar | |
| A6 | El despacho no cierra con ítems sin verificar | |
| A7 | Los reportes de faltantes y saldo muestran información real | |
| A8 | Se entregan la documentación y los guiones de base de datos | |
| A9 | Pruebas automatizadas en verde (37/37) | |

### 3. Observaciones del cliente

_______________________________________________________________________
_______________________________________________________________________
_______________________________________________________________________

### 4. Plan de acción acordado

_______________________________________________________________________
_______________________________________________________________________

### 5. Firmas

| Entregado por (Equipo AGROMEDIO – UTS) | Recibido y aceptado por (Cliente) |
|---|---|
| | |
| Santiago Colmenares Parra | Nombre: ____________________________ |
| Califa Hernández Iván Mauricio | Cargo: ____________________________ |
| Jorge Andrés Correa Morales | Firma: ____________________________ |
| | Fecha: ____________________________ |

**Tutores / docentes evaluadores**

| Nombre | Firma |
|---|---|
| Elsa Patricia Carvajal Valero | ____________________________ |
| Laura Cristina Duarte Quintero | ____________________________ |
| Carlos Adolfo Beltran Castro | ____________________________ |
