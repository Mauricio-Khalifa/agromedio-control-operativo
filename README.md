# Sistema de Control Operativo AGROMEDIO

Prototipo de aplicación de escritorio para la gestión operativa de la Asociación Agropecuaria y Ambiental del Magdalena Medio (Agromedio): carga de instrucciones desde Excel, CSV o TXT, compras, recepción, alistamiento, despacho y reportes. Versión mono-usuario, sin autenticación.

Proyecto Integrador – Unidades Tecnológicas de Santander (TSI304 Planeación de Sistemas Informáticos, TSI301 Motores de Bases de Datos, TSI302 Programación Orientada a Objetos).

**Equipo:** Santiago Colmenares Parra, Iván Mauricio Califa Hernández, Jorge Andrés Correa Morales y Jaider Martínez.

## Estructura

| Carpeta | Contenido |
|---|---|
| `AgromedioApp/` | Proyecto NetBeans (código fuente Java, build de Ant, librerías en `lib/`) |
| `docs/` | Informe técnico, matriz de trazabilidad, plan de pruebas, casos de uso, manual, cronograma y acta, diccionario de datos, EDT, guiones SQL y capturas |

## Requisitos

- Java JDK 17 o superior (`java -version`)
- Para compilar: Apache Ant (incluido en NetBeans 21: `C:\Program Files\NetBeans-21\netbeans\extide\ant\bin\ant.bat`)

## Clonar, compilar y ejecutar

```powershell
# 1. Clonar el repositorio (desde la carpeta donde quieras trabajarlo)
git clone https://github.com/Mauricio-Khalifa/agromedio-control-operativo.git
cd agromedio-control-operativo\AgromedioApp

# 2. Compilar y generar dist\AgromedioApp.jar
& "C:\Program Files\NetBeans-21\netbeans\extide\ant\bin\ant.bat" jar

# 3. Ejecutar (las comillas alrededor de -D son necesarias en PowerShell)
java "-Dfile.encoding=UTF-8" -jar dist\AgromedioApp.jar
```

En **CMD** (síntaxis convencional, sin comillas ni `&`):

```cmd
cd agromedio-control-operativo\AgromedioApp
"C:\Program Files\NetBeans-21\netbeans\extide\ant\bin\ant.bat" jar
java -Dfile.encoding=UTF-8 -jar dist\AgromedioApp.jar
```

O simplemente doble clic en `AgromedioApp\ejecutar.bat`.

> Si aparece `Error: Could not find or load main class .encoding=UTF-8`, es porque la consola es PowerShell y el `-Dfile...` quedó sin comillas: use exactamente `java "-Dfile.encoding=UTF-8" -jar dist\AgromedioApp.jar`.

## Pruebas automatizadas

```powershell
cd agromedio-control-operativo\AgromedioApp
& "C:\Program Files\NetBeans-21\netbeans\extide\ant\bin\ant.bat" pruebas          # esquema 20 + lógica 32
& "C:\Program Files\NetBeans-21\netbeans\extide\ant\bin\ant.bat" pruebas-vistas   # ventanas 13
```

Resultado actual: **65/65 pruebas en verde**.

## Abrir en NetBeans

**Archivo → Abrir proyecto…** y seleccionar la carpeta `AgromedioApp`. Clase principal: `agromedio.presentacion.Aplicacion` (Run / F6).

## Base de datos

SQLite: en el primer arranque se crea y siembra automáticamente a partir de `AgromedioApp/src/sql/schema.sql` y `seed.sql` (archivos copiados también en `docs/base_de_datos/`). No requiere instalar servidor.

## Formatos de entrada

- Excel: `.xlsx` y `.xls` (primera hoja).
- Archivos planos: `.csv` y `.txt` en UTF-8, con delimitador `;`, `,` o tabulador.
- Columnas: `Cliente`, `Contacto`, `Telefono`, `Mercado`, `Fecha`, `Producto`, `Cantidad`.
- La fecha admite formato de Excel, `aaaa-mm-dd` o `dd/mm/aaaa`.
- La cantidad admite números o textos: `9 unidades`, `1 de 300 gr`, `2 de 1000 ml`, `400 gramos`, `1 libra`, `0.25 libra` o `1 rama`.
- Si el producto no existe, se crea automáticamente en el catálogo con la unidad detectada y la presentación entre paréntesis cuando aplica.
- Cada archivo se deduplica por hash SHA-256; una segunda carga muestra "Carga ya realizada".
