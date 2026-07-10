# Zakra TV

Aplicación **ligera** para **Android TV** (Fire Stick, cajas chinas, Android TV boxes).

- Catálogo **TMDb** con metadatos en **español** por defecto  
- Enlaces vía **Torrentio + Real-Debrid** (prioridad **Premium / Cached**)  
- Reproductor **Media3 ExoPlayer** optimizado para **4K HDR** en enlaces directos  
- UI oscura, letras grandes (≥20–24sp), mando a distancia  

Paquete: `com.zakratv.app` · Nombre visible: **Zakra TV**

---

## Lo que necesitas de ti (solo una vez)

Para que funcione al 100% **sin menús de configuración** en el televisor:

| Clave | Dónde obtenerla | Dónde ponerla |
|-------|-----------------|---------------|
| **TMDB_API_KEY** | [themoviedb.org/settings/api](https://www.themoviedb.org/settings/api) (gratis) | `local.properties` |
| **REAL_DEBRID_TOKEN** | [real-debrid.com/apitoken](https://real-debrid.com/apitoken) (tu cuenta Premium) | `local.properties` |

Copia el ejemplo y rellena:

```bash
copy local.properties.example local.properties
```

Ejemplo de `local.properties`:

```properties
sdk.dir=C:\\Users\\TU_USUARIO\\AppData\\Local\\Android\\Sdk
TMDB_API_KEY=tu_clave_tmdb
REAL_DEBRID_TOKEN=tu_token_real_debrid
```

Con el token de Real-Debrid embebido en el APK, la app **arranca ya conectada** (no hace falta autorizar con el mando).  
Si no lo pones al compilar, puedes pegarlo en **Ajustes** dentro de la app.

---

## Compilar el APK (Android Studio)

### Requisitos
- Android Studio Ladybug o más reciente  
- JDK 17  
- Android SDK 35  

### Pasos
1. Abre la carpeta del proyecto en **Android Studio**.  
2. Crea/edita `local.properties` como arriba.  
3. Espera a que Gradle sincronice.  
4. Menú **Build → Build Bundle(s) / APK(s) → Build APK(s)**.  
5. El APK debug queda en:

```text
app/build/outputs/apk/debug/app-debug.apk
```

### Línea de comandos

```bash
# Windows (PowerShell)
.\gradlew.bat :app:assembleDebug

# Release (minify + R8)
.\gradlew.bat :app:assembleRelease
```

---

## Firmar el APK (release)

1. Genera un keystore (una sola vez):

```bash
keytool -genkey -v -keystore keystore/zakratv-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias zakratv
```

2. Añade a `local.properties` (o `gradle.properties` local, **no lo subas a Git**):

```properties
RELEASE_STORE_FILE=../keystore/zakratv-release.jks
RELEASE_STORE_PASSWORD=tu_password
RELEASE_KEY_ALIAS=zakratv
RELEASE_KEY_PASSWORD=tu_password
```

3. Compila:

```bash
.\gradlew.bat :app:assembleRelease
```

APK firmado:

```text
app/build/outputs/apk/release/app-release.apk
```

---

## Publicar en GitHub (Release + Downloader)

1. Crea un repositorio (ej. `ZakraTV`).  
2. Sube el código (**sin** `local.properties` ni keystore).  
3. **Releases → Create a new release**  
   - Tag: `v1.0.0`  
   - Adjunta `app-release.apk`  
4. Copia la **URL directa** del APK del release, por ejemplo:

```text
https://github.com/TU_USUARIO/ZakraTV/releases/download/v1.0.0/app-release.apk
```

### Instalar con **Downloader** (Fire Stick / Android TV)

1. En el Fire Stick instala **Downloader** (Amazon Appstore).  
2. Abre Downloader e introduce la URL del APK **o** un código corto si usas un acortador.  
3. Alternativa con código corto:  
   - Sube el APK a un host directo (GitHub Release, Cloudflare R2, etc.).  
   - Crea un enlace corto en [https://www.aftvnews.com/sideload/](https://www.aftvnews.com/sideload/) o similar.  
   - En Downloader escribe el **código numérico** y descarga.  
4. Activa **Orígenes desconocidos** / instalar apps desconocidas para Downloader.  
5. Instala el APK → abre **Zakra TV** desde el launcher.

---

## Uso en el televisor

1. Navega con el **DPAD**: Inicio, Películas, Series, Trending, Mi Lista, Buscar, Ajustes.  
2. Entra en un título → **Reproducir** (o elige episodio).  
3. Los enlaces con **⚡ RD** son Premium/Cached y van primero.  
4. El reproductor acepta 4K HDR si el dispositivo y el enlace lo soportan.

---

## Arquitectura (ligera)

| Pieza | Rol |
|-------|-----|
| TMDb API | Catálogo, posters, sinopsis `es-ES` |
| Torrentio + RD | Descubrimiento de streams; RD cached primero |
| Media3 ExoPlayer | Reproducción directa HTTPS, buffers largos |
| DataStore | Token RD + Mi Lista |
| R8 minify | APK release reducido |

Paquete principal: `com.zakratv.app`

---

## Tests

```bash
.\gradlew.bat :app:testDebugUnitTest
```

Cubren el ranking **español primero** y **cached/premium primero** sobre el código real de `LanguagePreference` y `StreamRanker`.

---

## Notas

- No se usa `largeHeap`; Coil limita caché de imágenes (~12% RAM / 40 MB disco).  
- Los índices públicos pueden variar; si no hay enlace cached, la app degrada sin crashear.  
- Respeta los Términos de Real-Debrid y de TMDb; usa tu propia cuenta y API key.
