# Cómo obtener el APK sin instalar Android Studio

Se compila en la nube de GitHub (gratis). No instalas nada en tu computadora.

1. Crea una cuenta gratis en https://github.com (si no tienes).
2. Pulsa **New repository**, ponle un nombre (por ejemplo `traductor`) y créalo.
3. En el repositorio nuevo elige **uploading an existing file**.
4. Extrae `Traductor.zip` en tu computadora y **arrastra todo el contenido de la carpeta `Traductor`** a la página (incluida la carpeta oculta `.github`). Pulsa **Commit changes**.
   - Si la carpeta `.github` no aparece en el repositorio, crea el archivo a mano: **Add file > Create new file**, escribe como nombre `.github/workflows/build-apk.yml` y pega el contenido de ese archivo.
5. Abre la pestaña **Actions**. Verás una ejecución llamada **Construir APK**; tarda unos 5 minutos. Si no arranca sola, entra a **Construir APK** y pulsa **Run workflow**.
6. Cuando salga con ✔ verde, ábrela y baja hasta **Artifacts**. Descarga **traductor-apk**.
7. Extrae el zip descargado: dentro está `app-debug.apk`.
8. Pásalo a tu celular (cable, Drive, WhatsApp…) y ábrelo. Android pedirá permitir **instalar apps de esta fuente**: acéptalo. Así queda instalado como cualquier otra app.

Si la ejecución sale con ✖ rojo, abre el paso que falló y copia el mensaje de error.

---

# Bonus: la versión web con micrófono (https)

La carpeta `docs` trae la página web del traductor (`docs/index.html`). Con GitHub puedes publicarla en una dirección `https://`, donde el navegador sí deja usar el micrófono (en PC o en celular).

1. En tu repositorio abre **Settings > Pages**.
2. En **Build and deployment**, en **Source** elige **Deploy from a branch**.
3. En **Branch** elige `main` y la carpeta `/docs`, y pulsa **Save**. (En cuentas gratis el repositorio debe ser **público**.)
4. Espera cerca de 1 minuto y recarga: arriba aparece la dirección, parecida a `https://TU-USUARIO.github.io/traductor/`.
5. Ábrela en Chrome o Edge y permite el micrófono. En el celular, menú ⋮ > **Instalar app** o **Agregar a pantalla de inicio** para tenerla como un icono más.
