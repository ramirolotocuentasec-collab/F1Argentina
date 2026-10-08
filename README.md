# F1 Argentina

Aplicación Android personal para consultar el calendario de Fórmula 1 en hora argentina y recibir una notificación 24 horas antes de cada carrera.

## Qué hace

- Consulta automáticamente el calendario mediante la API pública Jolpica F1.
- Intenta cargar la temporada actual y la siguiente cuando corresponde.
- Convierte las fechas/horarios a `America/Argentina/Buenos_Aires`.
- Muestra carrera, clasificación, prácticas y Sprint cuando la API proporciona esos horarios.
- Guarda el calendario localmente.
- Programa una notificación 24 horas antes de cada carrera.
- Reprograma las alarmas después de reiniciar el teléfono.

La API Jolpica es un proyecto abierto que sucede a Ergast y ofrece endpoints compatibles con Ergast. La app identifica sus peticiones con `User-Agent: F1Argentina/1.0` como solicita su documentación.

## Cómo compilarla

1. En una PC con Windows, macOS, Linux o ChromeOS instala **Android Studio** desde la página oficial de Android Developers.
2. Descomprime este proyecto.
3. En Android Studio elige **Open** y selecciona la carpeta `F1Argentina`.
4. Espera a que Gradle termine de sincronizar y acepta la instalación del SDK que Android Studio solicite.
5. En el menú **Build > Build APK(s)** genera la APK de depuración.
6. Android Studio mostrará un enlace para abrir la carpeta donde quedó la APK. Normalmente será:
   `app/build/outputs/apk/debug/app-debug.apk`
7. Pasa ese archivo al teléfono e instálalo. Si Android pregunta, permite la instalación de aplicaciones desde esa fuente.

Para uso personal no hace falta publicar nada en Google Play ni crear una cuenta de desarrollador. Android Studio puede generar una APK de depuración firmada para probarla en tu propio dispositivo.

## Importante sobre las notificaciones

Al abrir la app por primera vez, Android puede pedir permiso para enviar notificaciones. Hay que aceptarlo.

En algunos teléfonos Android también puede limitarse la ejecución en segundo plano. Si el teléfono ofrece una opción de batería para `F1 Argentina`, conviene dejarla como **Sin restricciones** para maximizar la fiabilidad de las notificaciones.

## Fuente de datos

La app usa `https://api.jolpi.ca/ergast/f1/` para los datos de calendario. Si la API cambia o deja de estar disponible, la app seguirá mostrando los datos que haya podido guardar hasta la última actualización.
