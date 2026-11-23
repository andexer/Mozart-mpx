# Política de Privacidad — Mozart mpx

**Última actualización:** 2026

**Mozart mpx** es un reproductor de música estrictamente local y sin conexión (*offline*). Respetamos completamente tu privacidad: la aplicación **no recopila, almacena en servidores externos, vende ni comparte ningún dato personal ni información sobre tu biblioteca musical**.

---

## 1. Datos recopilados

- **Datos enviados a internet:** Ninguno. La aplicación ni siquiera solicita el permiso `INTERNET` en Android.
- **Cuentas de usuario:** No requiere registro ni inicio de sesión.
- **Analíticas y publicidad:** No contiene rastreadores, SDKs de telemetría ni anuncios.

## 2. Almacenamiento estrictamente local

Toda la información necesaria para el funcionamiento de Mozart mpx se guarda exclusivamente en la memoria interna de tu dispositivo:
- Índice local de tus archivos de audio y metadatos.
- Listas de reproducción creadas por ti, historial reciente, pistas ocultas y afinidad musical local para la cola inteligente.
- Preferencias de ecualizador, *crossfade* y reproducción.

Puedes borrar todos estos datos en cualquier momento desde los ajustes de Android (*Almacenamiento → Borrar datos*) o desinstalando la aplicación.

## 3. Permisos de Android utilizados

- **Acceso a archivos de audio (`READ_MEDIA_AUDIO` / `READ_EXTERNAL_STORAGE` en Android ≤ 12):** Se utiliza únicamente para detectar, listar y reproducir los archivos de música almacenados localmente en tu teléfono.
- **Servicio en primer plano de reproducción multimedia (`FOREGROUND_SERVICE_MEDIA_PLAYBACK`):** Permite continuar escuchando música con la pantalla apagada o mientras usas otras apps.
- **Notificaciones (`POST_NOTIFICATIONS`):** Muestra los controles de reproducción (pausa, siguiente, anterior) en la barra de notificaciones.
- **Gestión de archivos locales (opcional):** Si decides eliminar un archivo de audio desde la app, Android te solicitará confirmación explícita antes de borrarlo del dispositivo.

## 4. Contacto

Si tienes dudas sobre esta política de privacidad, puedes abrir un *issue* en el repositorio oficial:
https://github.com/andexer/Mozart-mpx

---

# Privacy Policy — Mozart mpx (English Summary)

**Mozart mpx** is a 100% offline local music player. It does **not** request internet access and does **not** collect, transmit, sell, or share any personal data, analytics, or media information. All playlists, equalizer preferences, playback history, and hidden-track settings remain strictly on your device. Permissions (`READ_MEDIA_AUDIO`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `POST_NOTIFICATIONS`) are used exclusively to scan and play local audio files and display playback controls.
