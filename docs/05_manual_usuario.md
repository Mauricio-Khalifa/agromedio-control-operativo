# Manual de Usuario

**Sistema de Control Operativo AGROMEDIO**
**Versión 1.0 – septiembre 2026**

---

## 1. Requisitos previos

- Computador con **Windows** y **Java 17 o superior** instalado (`java -version` en una terminal debe responder).
- El sistema no necesita servidor de base de datos: la base de datos es un archivo que se crea solo en el primer arranque.

## 2. Instalación

1. Copie la carpeta `AgromedioApp` completa al equipo de planta (o utilice la copia ya instalada).
2. Verifique que existan `dist\AgromedioApp.jar` y la carpeta `dist\lib` con los archivos `.jar`.
3. Si no existe, genere el paquete desde una terminal en la carpeta del proyecto:
   ```
   "C:\Program Files\NetBeans-21\netbeans\extide\ant\bin\ant.bat" jar
   ```

## 3. Inicio del sistema

1. Doble clic en **`ejecutar.bat`** (o doble clic en `dist\AgromedioApp.jar`).
2. Espere unos segundos: en el primer arranque se crea la base de datos con datos de demostración.
3. Aparece la ventana principal:

![Ventana principal con ventanas internas](imagenes/ventana-principal.png)

*Figura 1. Ventana principal: menús **Catálogos**, **Operación**, **Consultas y reportes** y **Ayuda**, con las ventanas internas abiertas en cascada. La barra inferior muestra la ruta de la base de datos y el nombre de la organización.*

**Barra de estado:** muestra `Base de datos: ...` con la ruta del archivo `.db` que está utilizando el sistema.

## 4. Estructura de los menús

| Menú | Opciones | Ventana que abre |
|---|---|---|
| **Catálogos** | Clientes · Categorías · Unidades de medida · Proveedores · Productos | Ventana de consulta/alta/edición |
| **Operación** | Plantillas operativas · Carga desde Excel (Módulo 0) · Compras (Módulo 1) · Recepción (Módulo 2) · Alistamiento / Producción (Módulo 3) · Despacho (Módulo 4) | Ventana del módulo |
| **Consultas y reportes** | Reportes operativos | Ventana de reportes |
| **Ayuda** | Acerca del sistema | Diálogo con versión y ruta de BD |

> Las ventanas se pueden abrir varias a la vez; se organizan en cascada y cada una tiene sus botones de minimizar, maximizar y cerrar (X).

## 5. Flujo operativo paso a paso

El sistema acompaña el ciclo de un día de trabajo en este orden:

```
CARGADA → EN_COMPRA → RECIBIENDO → ALISTANDO → LISTA → DESPACHADA
   (0)        (1)         (2)           (3)        (4)
```

### 5.1 Paso 0 – Cargar la instrucción de administración (Módulo 0)

1. Menú **Operación → Carga desde Excel (Módulo 0)**.
2. Pulse **Examinar…** y seleccione el archivo `.xlsx` enviado por administración.
3. Pulse **Cargar**. El sistema informa las filas procesadas y cualquier fila con observación.
4. Verifique la carga en **Operación → Plantillas operativas**; seleccione la plantilla y pulse **Ver detalle** para revisar mercado por mercado.

### 5.2 Paso 1 – Compras (Módulo 1)

1. Menú **Operación → Compras**.
2. Seleccione la plantilla del día: verá el **consolidado** de cada producto (suma de todas las cantidades).
3. Pulse **Registrar compra**: elija proveedor y fecha, agregue los productos con su cantidad y unidad de compra.
4. Pulse **Guardar**. La plantilla pasa a `EN_COMPRA`.
5. A medida que compre, marque cada producto como adquirido (lista de chequeo en tiempo real).

> **Importante:** sin proveedor la compra no se guarda; un producto repetido dentro de la misma compra tampoco.

### 5.3 Paso 2 – Recepción (Módulo 2)

1. Menú **Operación → Recepción**.
2. Seleccione la compra correspondiente.
3. Por cada producto, registre la cantidad **real** que entrega el proveedor (en la unidad de compra: bultos, sacos, cajas…).
4. El sistema convierte a la unidad de despacho y muestra la columna **Faltante**. Ejemplo del caso base: solicitados 77 litros de leche, recibidos 70 → **faltante 7 litros**.
5. Si el proveedor entrega en varias tandas, registre cada una; el faltante se recalcula solo.
6. Termine con **Cerrar recepción** (confirme el diálogo). La plantilla pasa a `ALISTANDO`.

> Los faltantes también se consultan en **Consultas y reportes → Reportes operativos → Faltantes de recepción**.

### 5.4 Paso 3 – Alistamiento / Producción (Módulo 3)

1. Menú **Operación → Alistamiento / Produccion**.
2. Seleccione la plantilla: el sistema **copia los productos** de la plantilla como tareas de alistamiento.
3. Conforme fraccione y empaque, marque cada producto como completado. La barra de avance se actualiza.
4. Pulse **Completar alistamiento**. Si queda algún ítem sin marcar, el sistema lo bloquea y le indica cuántos faltan.
5. Al completarlo, la plantilla pasa a `LISTA`.

### 5.5 Paso 4 – Despacho (Módulo 4)

1. Menú **Operación → Despacho**.
2. Seleccione la plantilla: se crea la **lista de chequeo** con todos los ítems.
3. Vaya marcando cada ítem **en el momento en que lo coloque** en la bolsa o cesta del mercado.
4. Pulse **Cerrar despacho**:
   - Si falta algún ítem → se bloquea y se lista lo pendiente.
   - Si todo está verificado → la plantilla pasa a **`DESPACHADA`** con avance del **100 %**.

### 5.6 Reportes

Menú **Consultas y reportes → Reportes operativos**:

| Reporte | Parámetros | Uso |
|---|---|---|
| Faltantes de recepción | Proveedor | Ver qué entregó incompleto un proveedor |
| Saldo del mercado | Fecha | Ver qué falta por despachar de una fecha |
| Consolidado de productos | (según plantilla) | Ver la demanda total por producto |

Pulse el botón del reporte para verlo en la vista previa y **Imprimir reporte** para sacarlo en papel.

## 6. Catálogos (mantenimiento)

Menú **Catálogos**: Clientes, Categorías, Unidades de medida y Proveedores; y **Catálogos → Productos** para el maestro de productos con su unidad de compra, unidad de despacho y factor de conversión.

- **Agregar**: botón *Nuevo*, llene los campos, *Guardar*.
- **Modificar**: seleccione la fila, edite, *Guardar*.
- **Eliminar**: seleccione la fila, *Eliminar* y confirme. El sistema no deja borrar datos referenciados por la operación (integridad referencial).

## 7. Preguntas frecuentes

| Pregunta | Respuesta |
|---|---|
| ¿Dónde quedan los datos? | En el archivo indicado en la barra de estado (`...\build\demo_smoke.db` o `agromedio.db` según el arranque). Copie ese archivo para respaldar. |
| ¿Cómo restauro un respaldo? | Reemplace el archivo `.db` por la copia con el sistema cerrado. |
| ¿Puedo tener el sistema abierto en dos equipos? | No: es de escritorio con archivo único. Para concurrencia multi-sede se requiere la versión con servidor. |
| ¿Y si borré una plantilla por error? | Las operaciones destructivas piden confirmación; si confirmó, el registro se eliminó y debe recargarse desde el Excel original. |
| No encuentra Java | Instale un JDK 17+ o pida soporte; `ejecutar.bat` avisa si no encuentra el ejecutable. |
| Mensaje de Log4j al cargar Excel | Es un aviso cosmético del lector de Excel; no afecta la operación. |

## 8. Cierre y buenas prácticas

1. Termine siempre el ciclo en el paso que le corresponde: no pase a despacho con plantilla en `RECIBIENDO`.
2. Registre cada entrega de proveedor el mismo día en que ocurre, para que el faltante sea confiable.
3. Haga respaldo diario del archivo de base de datos al cerrar la jornada.
4. Ante un error inesperado, cierre la aplicación y vuelva a abrirla; si persiste, adjunte la captura del mensaje al soporte.
