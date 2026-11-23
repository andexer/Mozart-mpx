# Mozart mpx

Reproductor de música local de alta fidelidad (Hi-Fi) para Android, diseñado para funcionar **100% sin conexión a internet**, con interfaz minimalista oscura, paleta de colores dinámica extraída de cada portada y transiciones continuas sin interrupciones.

---

## Características principales

- **100% Offline y Privado:** Sin nube, sin cuentas, sin anuncios y sin permiso de acceso a internet. Tu música permanece siempre en tu dispositivo.
- **Paleta dinámica desde la portada:** Extrae automáticamente los tonos dominantes de cada álbum para ambientar las superficies de la interfaz de manera coherente.
- **Reproducción continua (Gapless & Crossfade):** Motor dual basado en **Jetpack Media3 / ExoPlayer** con reproducción sin pausas (`0s`) y *crossfade* configurable de `1s` a `10s`.
- **Cola inteligente por afinidad musical:** Continúa automáticamente con canciones de artistas, géneros y familias musicales coherentes (por ejemplo, salsa clásica y ritmos tropicales), aprendiendo localmente de tus reproducciones y saltos en Room.
- **Jerarquía centrada en Artistas y Álbumes:** Acceso directo a tus **Artistas** (mostrando sus álbumes y canciones), **Álbumes**, **Carpetas locales** (con soporte SAF para importar directorios externos) y **Listas de reproducción** personalizadas.
- **Ecualizador integrado y ficha Hi-Fi:** Bandas ajustables en tiempo real, presets por género, refuerzo de graves (*Bass Boost*), virtualizador y detalles técnicos de formato, frecuencia de muestreo y tasa de bits.
- **Control total de tu biblioteca local:** Permite **ocultar** pistas permanentemente dentro de la app (y restaurarlas desde *Carpetas*) o **eliminar** el archivo real del almacenamiento del dispositivo mediante la confirmación segura de Android.

---

## Arquitectura y tecnologías

- **Lenguaje y UI:** Kotlin, Jetpack Compose, Material Design 3, Navigation Compose con rutas tipadas (`kotlinx.serialization`).
- **Audio:** AndroidX Media3 (`ExoPlayer` + `MediaSessionService` en primer plano `mediaPlayback`) y `android.media.audiofx` (`Equalizer`, `BassBoost`, `Virtualizer`).
- **Persistencia local:** Room Database (biblioteca, afinidad, listas, pistas ocultas) y Jetpack DataStore (preferencias de audio y ecualizador).
- **Imágenes y color:** Coil y AndroidX Palette API.
- **Requisitos:** Android 7.0+ (`minSdk 24`), `targetSdk 36`.

---

## Permisos utilizados

| Permiso | Propósito |
| :--- | :--- |
| `READ_MEDIA_AUDIO` *(Android 13+)* / `READ_EXTERNAL_STORAGE` *(hasta Android 12)* | Leer y reproducir únicamente los archivos de audio locales del dispositivo. |
| `FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Mantener la reproducción activa en segundo plano y con la pantalla apagada. |
| `POST_NOTIFICATIONS` | Mostrar los controles de reproducción en la notificación del sistema. |
| `WAKE_LOCK` | Evitar cortes de audio durante la reproducción con la pantalla apagada. |

---

## Política de privacidad

Consulta la política de privacidad completa en [`PRIVACY.md`](PRIVACY.md) o en versión web en `docs/privacy.html`.
