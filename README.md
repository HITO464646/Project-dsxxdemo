# Project DSX Demo

Launcher/menú horizontal para Android (Kotlin + Jetpack Compose) con estilos inspirados en
Xbox 360, Xbox One, Xbox Series, PS4, PS5 y Steam Deck.

## Cómo compilar
1. Abre la carpeta en **Android Studio** (Koala o más nuevo). Sincroniza Gradle.
2. Conecta tu teléfono/tablet (o emulador) y pulsa **Run**.
3. Opcional: en Ajustes de Android > Apps > Aplicación de inicio, elige "Project DSX Demo".

## Qué hace
- **Intro al abrir**: reproduce el video que elijas para la consola del arranque (si no hay, animación de texto). Toca para saltar.
- **Ajustes > Arranque**: una consola distinta cada vez / al azar / siempre la misma.
- **Ajustes > Videos de intro**: eliges un archivo de video por consola.
- **Ajustes > Fondo**: el fondo toma el arte de un juego Android instalado.
- **Steam**: login OpenID en WebView + tu Steam Web API Key -> tu catálogo aparece en el menú.
- **GeForce NOW**: los juegos de Steam compatibles llevan etiqueta GFN; al abrirlos pregunta
  "¿Jugar en GeForce NOW?" y abre la app de GFN directo en ese juego.
- Navegable con control/mando (foco + clic) y con táctil.

## Límites importantes
- **Videos y logos originales de las consolas NO están incluidos** (son propiedad de Microsoft/Sony/Valve).
  Usa tus propios videos o los que tengas derecho a usar. Los menús son recreaciones propias, no copias.
- **GeForce NOW no tiene API oficial** para leer tu biblioteca ni para abrir juegos desde otra app.
  La app usa la lista pública de juegos compatibles y un enlace profundo no oficial
  (`play.geforcenow.com/mall/#/deeplink?game-id=...`) que NVIDIA puede cambiar. Si la app de GFN no
  entra directo, solo abre el servicio.
- El login de Steam no verifica la firma OpenID (válido para un launcher personal).
- Código escrito sin compilar aquí: puede requerir pequeños ajustes de versiones en Android Studio.
