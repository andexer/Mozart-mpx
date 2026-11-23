# Ficha y Declaraciones para Google Play Console — Mozart mpx

## 1. Metadatos de la ficha (Play Store)

- **Nombre de la app (máx. 30 caracteres):**
  `Mozart mpx`

- **Descripción breve (máx. 80 caracteres):**
  `Reproductor de música local Hi-Fi sin conexión, ecualizador y cola inteligente.`

- **Descripción completa:**
  Mozart mpx es un reproductor de música local diseñado para escuchar tu colección de archivos en alta fidelidad sin depender de la nube ni de conexión a internet.

  Disfruta una interfaz oscura, ágil y minimalista que adapta automáticamente su paleta de colores a la portada de cada álbum que reproduces.

  Características principales:
  • Reproducción 100% local y sin conexión: reproduce tus archivos FLAC, WAV, ALAC, MP3, M4A, OGG y Opus almacenados en el dispositivo.
  • Transiciones fluidas sin interrupciones: reproducción gapless (sin pausas) y crossfade ajustable de 1 a 10 segundos para mantener el ritmo constante.
  • Cola inteligente coherente: al terminar una canción, recomienda automáticamente artistas y ritmos afines dentro de tu propia biblioteca local.
  • Organizado como prefieres: navega primero por Artistas, explora sus Álbumes y canciones, accede a Carpetas locales o crea tus propias Listas de reproducción.
  • Ecualizador integrado y ficha Hi-Fi: ajusta bandas de frecuencia, presets por género, refuerzo de graves (Bass Boost), virtualizador y consulta los metadatos técnicos de cada pista.
  • Control total de tu biblioteca: oculta canciones permanentemente en la app o elimina archivos del dispositivo con confirmación segura.
  • Privacidad total: sin anuncios, sin cuentas y sin acceso a internet.

---

## 2. Declaraciones obligatorias en Google Play Console

### Seguridad de datos (Data Safety)
- **¿Tu app recopila o comparte alguno de los tipos de datos de usuario obligatorios?** → **No**.
- Motivo: Mozart mpx funciona 100% sin conexión y no declara permiso `INTERNET`.

### Política de privacidad (URL)
1. En GitHub (`https://github.com/andexer/Mozart-mpx`), ve a **Settings → Pages**.
2. En *Branch*, selecciona **`main`** y carpeta **`/docs`**, luego guarda.
3. Usa esta URL en Google Play Console:
   `https://andexer.github.io/Mozart-mpx/privacy.html`

### Permiso de servicio en primer plano (`FOREGROUND_SERVICE_MEDIA_PLAYBACK`)
- **Caso de uso seleccionado:** Reproducción multimedia (*Media playback*).
- **Explicación sugerida para la revisión:**
  "Mozart mpx es un reproductor de música local sin conexión. Utiliza un servicio en primer plano de tipo mediaPlayback mediante AndroidX Media3 (MediaSessionService) para continuar reproduciendo los archivos de audio locales elegidos por el usuario cuando cambia de aplicación o apaga la pantalla, mostrando controles de reproducción persistentes en la notificación."

### Categoría sugerida
- **Categoría:** Música y audio
- **Etiquetas:** Reproductor de música, Sin conexión, Audio, Ecualizador
