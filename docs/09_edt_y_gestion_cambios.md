# EDT y Gestión de Cambios - Sistema de Control Operativo AGROMEDIO

**Proyecto:** Sistema de Control Operativo AGROMEDIO  
**Versión:** 1.1 - cierre de brechas, octubre de 2026  
**Propósito:** presentar la estructura de desglose del trabajo (EDT/WBS), el cronograma, los roles, la gestión de cambios y los riesgos del proyecto, alineados con el alcance real implementado en Java/Swing y SQLite.

## 1. Alcance del proyecto

El sistema cubre el ciclo operativo de un mercado atendido por AGROMEDIO:

1. Carga administrativa de la demanda desde Excel o archivos planos.
2. Consolidación de la demanda y registro de compras.
3. Recepción total o parcial, con cálculo de faltantes.
4. Alistamiento y fraccionamiento de productos.
5. Despacho con lista de chequeo, verificación y aceptación de faltantes.
6. Reportes de faltantes, saldo y avance.

**Fuera de alcance de esta versión:**

- Autenticación, usuarios y permisos por rol.
- Operación multiusuario concurrente sobre la misma base local.
- Integración con facturación, contabilidad o proveedores externos.
- Aplicación web o móvil.
- Migración a PostgreSQL u otro motor cliente-servidor.

La base de datos es SQLite y la aplicación se ejecuta como cliente de escritorio. El control de acceso se delega al sistema operativo del equipo donde se instala.

## 2. Estructura de desglose del trabajo (EDT)

| Código | Paquete de trabajo | Actividades principales | Entregable | Responsable |
|---|---|---|---|---|
| 1 | Gestión del proyecto | Planificación, seguimiento, actas, control de cambios | Cronograma, acta, matriz de decisiones | Líder de proyecto |
| 2 | Levantamiento | Entrevistas, observación, registro ENT/DOC/OBS | Guion, notas, flujo actual | Analista |
| 3 | Análisis y diseño | RF/RNF, reglas de negocio, casos de uso, modelo ER, máquina de estados | Informe, matriz de trazabilidad, casos de uso | Analista / arquitecto |
| 4 | Diseño de base de datos | Tablas, relaciones, restricciones, vistas, triggers, semilla | `schema.sql`, `seed.sql`, migraciones | Arquitecto de datos |
| 5 | Construcción | Implementación por capas y módulos | Código fuente y aplicación ejecutable | Equipo de desarrollo |
| 5.1 | Capa modelo | Entidades del dominio | Clases de modelo | Desarrollo |
| 5.2 | Capa datos | DAO, conexión, migraciones | Interfaces e implementaciones SQLite | Desarrollo |
| 5.3 | Capa lógica | Servicios, validaciones RN-01 a RN-12 | Servicios de negocio | Desarrollo |
| 5.4 | Capa presentación | Ventanas Swing, formularios, diálogos | Módulos 0 a 4 | Desarrollo |
| 5.5 | Reportes | Faltantes, saldo, avance | Vistas y pantalla de reportes | Desarrollo |
| 6 | Verificación | Pruebas de esquema, lógica y vistas; smoke visual | Suites automatizadas en verde | QA / desarrollo |
| 7 | Documentación | Informe, manual, matriz, casos de uso, diccionario, EDT | Carpeta `docs/` | Analista / líder |
| 8 | Entrega | Empaquetado, lanzador, acta, capacitación | `dist/`, acta firmada | Líder de proyecto |

## 3. Cronograma base

| Fase | Inicio | Fin | Estado |
|---|---|---|---|
| F1 - Levantamiento | 04/09/2026 | 11/09/2026 | Completada |
| F2 - Análisis y diseño | 08/09/2026 | 18/09/2026 | Completada |
| F3 - Diseño de base de datos | 16/09/2026 | 23/09/2026 | Completada |
| F4 - Construcción | 18/09/2026 | 30/09/2026 | Completada |
| F5 - Verificación | 28/09/2026 | 02/10/2026 | Completada |
| F6 - Documentación y entrega | 01/10/2026 | 04/10/2026 | Completada |
| F7 - Cierre de brechas de alcance | 22/10/2026 | 22/10/2026 | Completada en repositorio |

La fase F7 no reemplaza el cronograma académico: registra el trabajo adicional solicitado para cerrar las brechas G01-G10 después de la revisión del tercer avance.

## 4. Roles y responsabilidades (RACI)

| Actividad | Líder | Analista | Desarrollo | QA | Cliente |
|---|---|---|---|---|---|
| Definir alcance y decisiones | A | R | C | I | C |
| Levantar reglas de negocio | A | R | I | I | C |
| Diseñar modelo y esquema | A | C | R | I | I |
| Implementar módulos | A | I | R | C | I |
| Ejecutar pruebas | A | I | C | R | I |
| Documentar | A | R | C | C | I |
| Aceptar entrega | R | I | I | I | A |

**Convención:** R = responsable, A = aprueba, C = consultado, I = informado.

**Actores operativos del sistema:** coordinador de operaciones, compras, recepción, alistamiento y despacho. Son roles funcionales del proceso, no cuentas de usuario ni perfiles de seguridad.

## 5. Gestión de cambios

### 5.1 Flujo de control

1. Registrar la solicitud con identificador `CR-###`.
2. Describir el cambio y la necesidad de negocio.
3. Analizar impacto en alcance, modelo, código, pruebas, documentación y cronograma.
4. Clasificar el cambio: correctivo, adaptativo, evolutivo o de alcance.
5. Aprobar o rechazar por el líder y el cliente.
6. Implementar en una rama o cambio controlado.
7. Ejecutar pruebas de regresión.
8. Actualizar trazabilidad, manual y versión.
9. Comunicar el cierre del cambio.

### 5.2 Registro de cambios del cierre de brechas

| ID | Cambio | Tipo | Impacto | Estado |
|---|---|---|---|---|
| CR-01 | Recepciones parciales y faltante acumulado (G01) | Correctivo | Esquema, DAO, servicio, UI, pruebas | Cerrado |
| CR-02 | Tope de alistamiento contra lo recibido (G02) | Correctivo | Servicio, DAO, UI, pruebas | Cerrado |
| CR-03 | Faltante aceptado y cierre de despacho (G03) | Correctivo | Esquema, servicio, UI, reportes, pruebas | Cerrado |
| CR-04 | Protección del histórico (G04) | Correctivo | Esquema, migración, servicio, pruebas | Cerrado |
| CR-05 | Carga única con hash SHA-256 (G05) | Evolutivo | Esquema, carga, UI, pruebas | Cerrado |
| CR-06 | Plantilla mínima para comprar (G06) | Correctivo | Servicio, pruebas | Cerrado |
| CR-07 | Cero no válido en operaciones completadas (G07) | Correctivo | Esquema, servicios, UI, pruebas | Cerrado |
| CR-08 | Carga de CSV/TXT y cantidades descriptivas (G08) | Evolutivo | Servicio de carga, catálogo, UI, pruebas, manual | Cerrado |
| CR-09 | Alcance mono-usuario sin roles (G09) | Alcance | Documentación, UI informativa | Cerrado |
| CR-10 | Diccionario de datos y EDT (G10) | Documental | `docs/08`, `docs/09` | Cerrado en repositorio |
| CR-11 | Reemplazo del PDF externo | Documental | PDF del tercer avance | Pendiente de acción externa |

## 6. Riesgos

| ID | Riesgo | Probabilidad | Impacto | Mitigación | Estado |
|---|---|---|---|---|---|
| R-01 | Variación en el formato de los archivos administrativos | Media | Alto | Validación de encabezados, errores por fila, soporte CSV/TXT y Excel | Mitigado |
| R-02 | Duplicidad de demanda por recarga del mismo archivo | Media | Alto | Hash SHA-256 único y mensaje "Carga ya realizada" | Mitigado |
| R-03 | Faltantes mal calculados en entregas parciales | Media | Alto | Trigger acumulado y pruebas E11/E12 | Mitigado |
| R-04 | Despacho con faltantes sin justificación | Media | Alto | `faltante_aceptado`, novedad obligatoria y pruebas | Mitigado |
| R-05 | Borrado accidental de histórico | Baja | Alto | FK `RESTRICT`, trigger y validación de servicio | Mitigado |
| R-06 | Base local sin copias de seguridad | Media | Medio | Copia periódica de `agromedio.db`, documentada en el manual | Abierto |
| R-07 | Ausencia de autenticación | Alta | Medio | Alcance mono-usuario, control por sistema operativo, módulo de usuarios para v2 | Aceptado |
| R-08 | Desalineación entre el PDF y el repositorio | Alta | Medio | Este EDT, diccionario y matriz; reemplazo del PDF externo | Parcial |
| R-09 | Cambio de unidad o factor de conversión mal configurado | Baja | Alto | Catálogo de productos y validación `factor_conversion > 0` | Mitigado |
| R-10 | Rendimiento con archivos grandes | Baja | Medio | Prueba de 1.000 filas y procesamiento por fila | Mitigado |

## 7. Correcciones al documento externo (Tercer Avance)

El PDF del tercer avance no está dentro del repositorio, por lo que no puede editarse desde este proyecto. Antes de la entrega final debe reemplazarse por una versión que incluya:

| Punto | Texto anterior | Texto corregido |
|---|---|---|
| Portada | "Sistema de Control de Inventario" | "Sistema de Control Operativo AGROMEDIO" |
| EDT | "Desarrollo de conexión con PostgreSQL" | "Desarrollo de conexión con SQLite" |
| Modelo | Conteo o alcance anterior | 16 tablas, 3 vistas, 1 trigger de faltantes y 5 triggers de integridad |
| Pruebas | 30/30 o 63/63 | 20/20 esquema + 32/32 lógica + 13/13 vistas = 65/65 |
| Documentación | Sin diccionario ni EDT | Anexar `docs/08_diccionario_datos.md` y `docs/09_edt_y_gestion_cambios.md` |
| Actores | Roles ambiguos | Aclarar que son roles operativos; no hay autenticación en esta versión |
| Formatos | Solo Excel | Excel `.xlsx/.xls`, CSV y TXT; cantidades numéricas o descriptivas y alta automática de productos faltantes |

## 8. Estado de cierre

- G01 a G09 cerradas y verificadas.
- G10 cerrada en el repositorio: diccionario de datos y EDT disponibles.
- D01 a D07 cerradas; D03 se interpreta como plantilla/mercado, sin entidad `contrato` separada.
- Acción externa pendiente: reemplazar el PDF del tercer avance con las correcciones de la sección 7.
- Suite de verificación: **65/65**.
- No se ha realizado commit de estos cambios.
