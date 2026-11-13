# Traductor de voz (Android)

App que traduce **español ↔ inglés (EE. UU. y Reino Unido), francés, alemán, portugués, italiano, chino, japonés, coreano y árabe**.

> Nota: el inglés británico cambia el acento del reconocimiento de voz y de la voz de salida; el texto traducido es el mismo inglés para ambas variantes.

- Botón **Hablar**: usa el micrófono y muestra en pantalla lo que dijiste (transcripción en vivo).
- Selectores de **idioma de entrada y salida** + botón para **invertir** idiomas.
- La traducción aparece en pantalla y se **reproduce por la bocina** automáticamente.
- Botón **Escuchar**: repite la traducción cuando quieras.
- Uno de los dos idiomas siempre es español (si eliges, por ejemplo, francés → italiano, la app ajusta el otro lado a español).

## Cómo abrirlo y probarlo

1. Instala [Android Studio](https://developer.android.com/studio).
2. `File > Open` y elige esta carpeta (`Traductor`). Espera a que termine el *Gradle Sync* (necesita internet la primera vez).
3. Conecta tu teléfono Android (con depuración USB activada) o crea un emulador con servicios de Google.
4. Pulsa **Run ▶**.

Para generar un APK instalable: `Build > Build Bundle(s) / APK(s) > Build APK(s)`.

## Cómo funciona

| Función | Tecnología | ¿Necesita internet? |
|---|---|---|
| Voz a texto | `SpeechRecognizer` de Android | Normalmente sí (depende del teléfono; algunos tienen reconocimiento sin conexión) |
| Traducción | Google ML Kit Translate | Solo la primera vez por idioma (descarga un modelo de ~30 MB); luego funciona sin conexión |
| Texto a voz | `TextToSpeech` de Android | No, pero el teléfono debe tener instalada la voz del idioma (Ajustes > Idioma > Salida de texto a voz) |

## Estructura

- `app/src/main/java/com/example/traductor/MainActivity.kt` — toda la lógica.
- `app/src/main/res/layout/activity_main.xml` — pantalla.
- `app/src/main/AndroidManifest.xml` — permiso de micrófono.
- `app/build.gradle.kts` — dependencias (ML Kit Translate, Material).

## Ideas para mejorar

- Modo conversación (alternar automáticamente entre los dos idiomas).
- Historial de traducciones.
- Botón para escuchar también el texto original.
- Cambiar `applicationId` (`com.example.traductor`) si vas a publicarla en Google Play.
