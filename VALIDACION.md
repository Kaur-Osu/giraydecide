# Validación de Gira y Decide 1.7.0

## Verificaciones de esta entrega

- Compilación del código Kotlin de la aplicación aprobada, omitiendo el enlace nativo de recursos por la limitación indicada abajo.
- Las cuatro pruebas unitarias de reglas se ejecutaron sobre los archivos actuales en un proyecto JVM de comprobación: cuatro aprobadas, cero errores. Cubren duplicados, límites y correspondencia entre el resultado y los sectores.
- El manifiesto contiene únicamente la característica de reloj y las dos actividades de la aplicación. No declara permisos ni consultas de servicios externos.
- El editor tiene dos acciones: guardar y agregar una opción escrita. Se conservaron la entrada de texto, las ruletas guardadas, el historial, la celebración, los colores y la respuesta táctil.
- Se revisó que los archivos fuente no incluyan claves privadas ni credenciales evidentes. La clave de depuración, las rutas locales, la configuración de Android Studio, las cachés y los resultados antiguos de compilación se apartaron del proyecto.
- El proyecto incluye Gradle Wrapper y reglas de exclusión para que Android Studio pueda regenerar sus archivos locales sin incorporarlos al repositorio.

## Límites de la validación

La ejecución conjunta de `assembleDebug`, `testDebugUnitTest` y `lintDebug` se detuvo en `processDebugResources`: AAPT2 terminó inesperadamente durante el enlace. Es una limitación ya observada en este entorno automatizado. No se presenta la comprobación de Kotlin ni las pruebas JVM como una compilación completa del APK.

La suite instrumentada completa y la vibración en un reloj físico quedan pendientes. Las pantallas y la entrada de texto de la versión anterior se revisaron manualmente en el emulador; esta entrega no modifica la implementación de la captura de texto.

Para comprobar la entrega en Android Studio, sincroniza el proyecto y ejecuta los comandos de compilación y pruebas indicados en `README.md`.
## Ajuste del historial

El botón Vaciar historial aparece antes de los resultados cuando hay registros. Conserva la confirmación, el guardado del historial vacío y las ruletas del usuario. Se comprobó el orden en el código, la ausencia de botones duplicados y la compilación Kotlin tras moverlo.
