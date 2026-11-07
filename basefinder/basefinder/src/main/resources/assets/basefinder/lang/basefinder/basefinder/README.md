# Base Finder (Fabric, cliente)

Tecla **B** = activar / desactivar.

Dibuja a traves de paredes:
- Cofres (naranja), ender chests (cian), shulkers (magenta), barriles/hoppers/hornos... (amarillo), spawners (verde)
- Bloques transparentes (cristal, paneles, tintado): **borde violeta**
- Zonas con pinta de base (celda de 50x50): **caja roja** + decoracion (camas, banners, alfombras, lamparas...) en blanco

## Compilar
1. Instala **JDK 25** y **Gradle 9.5+** (o copia `gradlew`, `gradlew.bat` y `gradle-wrapper.jar` de cualquier plantilla Fabric 26.x).
2. Abre `gradle.properties` y rellena las 4 versiones desde https://fabricmc.net/develop
   (Minecraft 26.3 -> te da loader, loom y fabric-api).
3. En esta carpeta:  `gradle build`   (o `./gradlew build`)
4. El jar queda en `build/libs/basefinder-1.0.0.jar`  -> a tu carpeta `mods/` junto con **Fabric API**.

## Ajustes
Todo en `src/main/java/com/basefinder/Config.java` (radio, altura maxima, umbral de base, etc.).
Si el render falla al compilar en 26.3, solo hay que tocar `Renderer.java`.

## Compilar sin instalar nada (GitHub)
1. Crea un repo en github.com y sube TODOS estos archivos (incluida la carpeta `.github`).
2. Ve a la pestana **Actions** -> abre la ultima ejecucion "build".
3. Abajo, en **Artifacts**, descarga `basefinder-jar` (dentro esta el .jar). Si falla, copia el error.
