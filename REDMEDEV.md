# Guía de Desarrollo para Indexer Mod

Esta guía está destinada a desarrolladores que deseen compilar, modificar o probar el mod Indexer para Minecraft 1.21.1 usando Fabric.

## Requisitos Previos

- JDK 21 (el proyecto está configurado para Java 21)
- Gradle (se usa el wrapper incluido en el proyecto, no necesitas instalarlo aparte)
- IDE recomendado: IntelliJ IDEA o Visual Studio Code con soporte para Gradle/Java
- Git (opcional, para control de versiones)

## Configuración del Entorno de Desarrollo

### Clonar el Repositorio

```bash
git clone https://github.com/AgustinBeniteez/Indexer-mod.git
cd Indexer-mod
```

### Configurar el Entorno de Fabric

El proyecto utiliza Fabric Loom para gestionar dependencias y tareas de compilación.

- Abre el proyecto como proyecto Gradle en tu IDE.
- Espera a que Gradle sincronice y descargue dependencias.
- Loom generará configuraciones de ejecución como `runClient` y `runServer`.

Si quieres verificar desde la línea de comandos que todo está correcto:

```bash
# En Windows
.\gradlew build

# En Linux/macOS
./gradlew build
```

Si la compilación termina correctamente, el entorno está listo.

## Compilación del Mod

Para compilar el mod y generar el archivo JAR:

```bash
# En Windows
.\gradlew build

# En Linux/macOS
./gradlew build
```

El archivo JAR compilado se encontrará en la carpeta `build/libs/` (por ejemplo `indexer-fabric-1.21.1-1.0.7.jar`, según `gradle.properties`).

## Ejecución y Pruebas

### Ejecutar en el Entorno de Desarrollo

Puedes ejecutar el mod directamente desde tu IDE o desde Gradle:

- **IntelliJ IDEA**: Usa la configuración `runClient` generada automáticamente por Loom.
- **Visual Studio Code**:
  - Usa las tareas de Gradle (`runClient`) desde la vista de tareas, o
  - Ejecuta el comando:

    ```bash
    # En Windows
    .\gradlew runClient

    # En Linux/macOS
    ./gradlew runClient
    ```

### Pruebas Manuales

Para probar el mod manualmente en un cliente normal de Minecraft:

1. Compila el mod como se indicó anteriormente.
2. Asegúrate de tener instalado Fabric Loader 0.16.5 (o superior) para Minecraft 1.21.1.
3. Instala Fabric API compatible con 1.21.1 (por ejemplo `fabric-api-0.104.0+1.21.1`).
4. Copia el archivo JAR generado (`build/libs/indexer-fabric-1.21.1-<versión>.jar`) a la carpeta `mods` de tu instalación de Minecraft con Fabric.
5. Inicia Minecraft con el perfil de Fabric.
6. Verifica que el mod aparezca en la lista de mods cargados.
7. Prueba la funcionalidad en el juego:
   - Crea los bloques del mod (Controlador Indexador, Tubería Indexadora, Conector Indexador, DropBox, etc.)
   - Configura un sistema básico de indexación
   - Verifica que los ítems se distribuyan correctamente según los filtros configurados

## Estructura del Proyecto

```
src/
├── main/
│   ├── java/
│   │   └── com/
│   │       └── agustinbenitez/
│   │           └── indexer/
│   │               ├── IndexerMod.java          # Clase principal del mod (Fabric)
│   │               ├── init/                    # Registro de bloques, ítems, entidades, menús, etc.
│   │               ├── block/                   # Definiciones de bloques
│   │               ├── block/entity/            # Entidades de bloque
│   │               ├── item/                    # Definiciones de ítems
│   │               ├── menu/                    # Menús (containers) del servidor
│   │               ├── network/                 # Paquetes de red
│   │               └── screen/                  # Interfaces de usuario (pantallas)
│   └── resources/
│       ├── fabric.mod.json                      # Archivo de configuración del mod para Fabric
│       └── assets/
│           └── indexer/
│               ├── blockstates/
│               ├── lang/
│               ├── models/
│               └── textures/
└── test/                                        # Pruebas unitarias (si las hay)
```

## Modificación del Mod

### Añadir Nuevos Bloques o Ítems

1. Crea una nueva clase para el bloque/ítem en el paquete correspondiente.
2. Registra el nuevo bloque/ítem en `ModBlocks.java` o `ModItems.java`.
3. Añade los modelos, texturas y traducciones necesarios en la carpeta de recursos.
4. Si es necesario, crea una entidad de bloque y regístrala en `ModBlockEntities.java`.

### Modificar la Lógica de Indexación

La lógica principal de indexación se encuentra en las clases `IndexerControllerBlockEntity.java` e `IndexerConnectorBlockEntity.java`. Modifica estas clases para cambiar el comportamiento de indexación.

## Depuración

Para depurar el mod:

1. Ejecuta el cliente de Minecraft en modo de depuración desde tu IDE.
2. Utiliza puntos de interrupción en el código para seguir la ejecución.
3. Revisa los registros de Minecraft en la consola para identificar errores.

## Empaquetado y Distribución

Para crear un archivo JAR listo para distribución:

```bash
# En Windows
.\gradlew build

# En Linux/macOS
./gradlew build
```

El archivo JAR resultante en `build/libs/` puede distribuirse a los usuarios finales para su instalación en la carpeta `mods` de una instancia de Minecraft 1.21.1 con Fabric Loader y Fabric API.

## Solución de Problemas Comunes

### Errores de Compilación

- Verifica que estás utilizando JDK 21.
- Ejecuta `.\gradlew clean` y luego `.\gradlew build` para limpiar archivos temporales.
- Asegúrate de que todas las dependencias estén correctamente configuradas en `build.gradle`.

### Problemas con Gradlew en Windows

- Si recibes el error "El término 'gradlew' no se reconoce como nombre de un cmdlet, función, archivo de script o programa ejecutable", esto puede deberse a dos razones:
  1. Estás usando la sintaxis incorrecta. Asegúrate de usar `.\gradlew` en lugar de `gradlew`.
  2. El archivo `gradlew.bat` no existe en tu proyecto.

#### Generar el Wrapper de Gradle

Si el archivo `gradlew.bat` no existe en la carpeta raíz del proyecto (lo cual parece ser el caso), necesitas generar el wrapper de Gradle:

1. **Instala Gradle globalmente** (si aún no lo has hecho):
   - Descarga Gradle desde [https://gradle.org/releases/](https://gradle.org/releases/)
   - Extrae el archivo ZIP en una ubicación de tu elección (por ejemplo, `C:\Gradle`)
   - Añade la carpeta `bin` de Gradle a tu PATH del sistema
   - Verifica la instalación ejecutando `gradle -v` en una nueva ventana de terminal

2. **Genera el wrapper de Gradle**:
   - Navega a la carpeta raíz del proyecto en la terminal
   - Ejecuta el comando: `gradle wrapper --gradle-version=8.14.2`
   - Esto generará los archivos `gradlew`, `gradlew.bat` y la carpeta `gradle/wrapper`

3. **Ahora puedes usar los comandos gradlew**:
   - Ejecuta `.\gradlew build` y otros comandos como se indica en esta guía

### Errores en Tiempo de Ejecución

- Revisa los registros de Minecraft para identificar excepciones.
- Verifica que los modelos y texturas estén correctamente definidos y ubicados.
- Comprueba que los registros de bloques, ítems y entidades de bloque se realicen en el momento adecuado del ciclo de vida del mod.

## Contribuciones

Si deseas contribuir al desarrollo del mod:

1. Crea un fork del repositorio.
2. Realiza tus cambios en una rama separada.
3. Envía un pull request con una descripción detallada de los cambios realizados.

---

¡Feliz desarrollo! Si tienes preguntas o encuentras problemas, no dudes en abrir un issue en el repositorio del proyecto.
