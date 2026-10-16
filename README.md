# Gira y Decide

Aplicación independiente para Wear OS que permite crear ruletas personalizadas y elegir una opción al azar desde el reloj.

## Funciones

- Ruleta inicial del 1 al 6, lista para girar al tocar **GIRA**.
- Ruletas guardadas con nombre y fecha de creación, ordenadas desde la más reciente.
- Entre 2 y 16 opciones por ruleta, con hasta 48 caracteres por opción y 32 por nombre de ruleta.
- Nombres y opciones editables mediante el teclado del reloj. Confirmación con ✓, Enter o **Ingresar**, sin necesidad de seleccionar autocompletado.
- Sectores con los nombres de las opciones, animación de giro y resultado completo debajo.
- Opciones editables al final de cada ruleta. Borrado de ruletas con confirmación desde el menú principal.
- Historial de los últimos 20 resultados, con borrado individual o completo. El botón **Vaciar historial** aparece arriba, antes de los resultados, y solicita confirmación.
- Celebración con confeti, colores múltiples o un color seleccionable y vibración configurables en **Opciones**.
- Ayuda al mantener pulsados los controles y conservación del desplazamiento al editar.
- Guardado local de las ruletas, el historial, las preferencias y el borrador.

La aplicación funciona sin conexión y no solicita permisos. La vibración depende del hardware y de los ajustes del sistema.

## Requisitos

- Android Studio con JDK 17 o 21 (se puede usar el integrado).
- Android SDK Platform 35 y Build Tools 35.0.0.
- Reloj o emulador **Wear OS con API 30 o superior**.
- Internet para descargar las dependencias durante la primera sincronización.

El proyecto incluye Gradle Wrapper 8.10.2, Android Gradle Plugin 8.6.0 y Kotlin 2.1.20. Versión de la app: **1.7.0**.

## Ejecutar en Android Studio

1. Selecciona **Open** y abre la carpeta que contiene `settings.gradle.kts`.
2. Espera a que termine la sincronización de Gradle. Android Studio generará `local.properties` con la ruta del SDK de tu equipo.
3. En **Tools → Device Manager**, crea un dispositivo Wear OS o conecta un reloj.
4. Selecciona el dispositivo, el módulo **app** y pulsa **Run ▶**.

Para compilar y comprobar desde una terminal de Windows:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
```

El APK de depuración se genera en `app/build/outputs/apk/debug/app-debug.apk`. En macOS/Linux se puede usar `bash ./gradlew` en lugar de `.\gradlew.bat`.

Si la ruta de Windows es demasiado larga, se puede elegir otra carpeta para los resultados de compilación:

```powershell
.\gradlew.bat -PgiraBuildRoot=C:/gira-build assembleDebug
```

## Uso

1. Toca **GIRA** para usar la ruleta inicial o desliza hacia arriba para abrir el menú.
2. Selecciona **Crear ruleta** y toca el nombre verde o su lápiz para asignar un nombre.
3. Pulsa **+** para escribir cada opción. Puedes corregirla tocando el nombre o el lápiz, y quitarla con **×**.
4. Con al menos dos opciones, toca el icono de guardar situado a la izquierda del **+**.
5. Toca **GIRA** dentro de la ruleta. El resultado aparece debajo; al final están las opciones editables.

Desde el menú principal puedes abrir o eliminar ruletas anteriores, cambiar las preferencias en **Opciones** y consultar **Historial** al final. Las ruletas antiguas sin fecha registrada muestran «Fecha no disponible».

## Pruebas

Las pruebas unitarias comprueban límites, opciones duplicadas y correspondencia entre el resultado y el puntero. Las pruebas instrumentadas cubren la entrada de texto, composición del teclado, cancelación y edición.

Con un reloj o emulador conectado:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

Consulta `VALIDACION.md` para conocer las verificaciones realizadas y sus limitaciones.

## Estructura

- `app/src/main/java/com/giraydecide/wear/`: pantallas, entrada de texto, ruleta, almacenamiento, preferencias y respuesta táctil.
- `app/src/main/res/`: tema e icono de la aplicación.
- `app/src/test/`: pruebas unitarias.
- `app/src/androidTest/`: pruebas instrumentadas de entrada de texto.
- `gradle/wrapper/`: distribución y configuración del Gradle Wrapper.

## Subir a GitHub

Esta carpeta contiene el proyecto fuente. `.gitignore` excluye compilaciones, claves, cachés y rutas locales; el Gradle Wrapper debe incluirse en el repositorio.

Crea un repositorio vacío en GitHub. Desde esta carpeta puedes preparar el primer commit:

```powershell
git add .
git commit -m "Agregar Gira y Decide para Wear OS"
```

Después utiliza las instrucciones de GitHub para conectar tu repositorio remoto y subir la rama. También puedes seleccionar esta carpeta desde **GitHub Desktop → Add local repository** y publicarla. No se necesita incluir archivos APK para compartir el proyecto.