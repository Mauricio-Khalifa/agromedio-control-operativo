# Sistema de Control Operativo AGROMEDIO

Prototipo de aplicación de escritorio para la gestión operativa de la Asociación Agropecuaria y Ambiental del Magdalena Medio (Agromedio): carga de instrucciones desde Excel, compras, recepción, alistamiento, despacho y reportes.

Proyecto Integrador – Unidades Tecnológicas de Santander (TSI304 Planeación de Sistemas Informáticos, TSI301 Motores de Bases de Datos, TSI302 Programación Orientada a Objetos).

## Estructura

| Carpeta | Contenido |
|---|---|
| `AgromedioApp/` | Proyecto NetBeans (código fuente Java, build de Ant, librerías en `lib/`) |
| `docs/` | Informe técnico, matriz de trazabilidad, plan de pruebas, casos de uso, manual, cronograma y acta, guiones SQL y capturas |

## Requisitos

- Java JDK 17 o superior (`java -version`)
- Para compilar: Apache Ant (incluido en NetBeans 21: `C:\Program Files\NetBeans-21\netbeans\extide\ant\bin\ant.bat`)

## Compilar y ejecutar

```powershell
cd AgromedioApp
# Compilar y generar dist/AgromedioApp.jar
& "C:\Program Files\NetBeans-21\netbeans\extide\ant\bin\ant.bat" jar

# Ejecutar
java -Dfile.encoding=UTF-8 -jar dist\AgromedioApp.jar
```

O simplemente doble clic en `AgromedioApp\ejecutar.bat`.

## Pruebas automatizadas

```powershell
cd AgromedioApp
& "C:\Program Files\NetBeans-21\netbeans\extide\ant\bin\ant.bat" pruebas          # esquema 10 + lógica 14
& "C:\Program Files\NetBeans-21\netbeans\extide\ant\bin\ant.bat" pruebas-vistas   # ventanas 13
```

Resultado actual: **37/37 pruebas en verde**.

## Abrir en NetBeans

**Archivo → Abrir proyecto…** y seleccionar la carpeta `AgromedioApp`. Clase principal: `agromedio.presentacion.Aplicacion` (Run / F6).

## Base de datos

SQLite: en el primer arranque se crea y siembra automáticamente a partir de `AgromedioApp/src/sql/schema.sql` y `seed.sql` (archivos copiados también en `docs/base_de_datos/`). No requiere instalar servidor.
