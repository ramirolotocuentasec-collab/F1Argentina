# F1 Argentina — compilar APK desde el celular con GitHub Actions

Este proyecto incluye `.github/workflows/build-apk.yml`, que hace que GitHub compile la APK automáticamente en sus servidores.

## Desde el celular

1. Entrá a https://github.com/ e iniciá sesión o creá una cuenta gratuita.
2. Creá un repositorio nuevo llamado `F1Argentina`.
3. Recomendado para este uso personal: dejalo **Public** para aprovechar los runners gratuitos de GitHub Actions.
4. Desde el navegador, abrí el repositorio y elegí **Add file → Upload files**.
5. Subí **el contenido de esta carpeta**, no la carpeta `F1Argentina` como una carpeta anidada. Deben quedar en la raíz del repositorio archivos como `settings.gradle.kts`, `build.gradle.kts` y la carpeta `app`.
6. Confirmá el commit.
7. Abrí la pestaña **Actions**. El workflow `Build F1 Argentina APK` se ejecutará automáticamente al hacer push a `main` o `master`.
8. Esperá a que termine con una marca verde.
9. Entrá en la ejecución terminada y, en **Artifacts**, descargá `F1Argentina-debug-apk`.
10. Descomprimí el ZIP descargado y abrí `app-debug.apk` en tu teléfono para instalarla.

También podés ejecutar la compilación manualmente desde **Actions → Build F1 Argentina APK → Run workflow**.

## Importante

- No necesitás Android Studio.
- No necesitás instalar Gradle en el teléfono.
- No necesitás crear un token de GitHub para este workflow.
- La APK es una build de depuración, suficiente para uso personal.
- GitHub Actions genera y conserva el APK como un artifact del workflow.
