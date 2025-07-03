# Guía de Desarrollo para Indexer Mod

Esta guía está destinada a desarrolladores que deseen compilar, modificar o probar el mod Indexer para Minecraft 1.20.1.

## Requisitos Previos

- JDK 17 o superior
- Gradle 7.6 o superior (incluido en el wrapper del proyecto)
- IDE recomendado: IntelliJ IDEA, Eclipse con soporte para Gradle, o Visual Studio Code con extensiones para Java
- Git (opcional, para control de versiones)

## Configuración del Entorno de Desarrollo

### Clonar el Repositorio

```bash
git clone https://github.com/tu-usuario/Indexer-mod.git
cd Indexer-mod
```

### Configurar el Entorno de Forge

El proyecto utiliza Gradle para gestionar dependencias y tareas de compilación. Para configurar el entorno de desarrollo de Forge:

```bash
# En Windows
gradlew setupDecompWorkspace
gradlew genIntellijRuns  # Para IntelliJ IDEA
# o
gradlew genEclipseRuns   # Para Eclipse

# En Linux/macOS
./gradlew setupDecompWorkspace
./gradlew genIntellijRuns  # Para IntelliJ IDEA
# o
./gradlew genEclipseRuns   # Para Eclipse
```

## Compilación del Mod

Para compilar el mod y generar el archivo JAR:

```bash
# En Windows
gradlew build

# En Linux/macOS
./gradlew build
```

El archivo JAR compilado se encontrará en la carpeta `build/libs/`.

## Ejecución y Pruebas

### Ejecutar en el Entorno de Desarrollo

Puedes ejecutar el mod directamente desde tu IDE:

- **IntelliJ IDEA**: Ejecuta la configuración "runClient" que se generó automáticamente.
- **Eclipse**: Ejecuta la configuración de lanzamiento "runClient" que se generó automáticamente.

### Pruebas Manuales

Para probar el mod manualmente:

1. Compila el mod como se indicó anteriormente.
2. Copia el archivo JAR generado (`build/libs/indexer-<version>.jar`) a la carpeta `mods` de tu instalación de Minecraft con Forge 1.20.1.
3. Inicia Minecraft con Forge instalado.
4. Verifica que el mod aparezca en la lista de mods cargados.
5. Prueba la funcionalidad en el juego:
   - Crea los bloques del mod (Controlador Indexador, Tubería Indexadora, Conector Indexador)
   - Configura un sistema básico de indexación
   - Verifica que los ítems se distribuyan correctamente según los filtros configurados

## Estructura del Proyecto

```
src/
├── main/
│   ├── java/         # Código fuente Java
│   │   └── com/
│   │       └── agustinbenitez/
│   │           └── indexer/
│   │               ├── Indexer.java                    # Clase principal del mod
│   │               ├── ModBlocks.java                  # Registro de bloques
│   │               ├── ModBlockEntities.java           # Registro de entidades de bloque
│   │               ├── ModItems.java                   # Registro de ítems
│   │               ├── block/                          # Definiciones de bloques
│   │               ├── blockentity/                    # Entidades de bloque
│   │               ├── item/                           # Definiciones de ítems
│   │               └── screen/                         # Interfaces de usuario
│   └── resources/   # Recursos del mod (modelos, texturas, traducciones, etc.)
│       ├── META-INF/
│       │   └── mods.toml                              # Archivo de configuración del mod
│       └── assets/
│           └── indexer/
│               ├── blockstates/                       # Estados de bloques
│               ├── lang/                              # Archivos de traducción
│               ├── models/                            # Modelos 3D
│               └── textures/                          # Texturas
└── test/            # Pruebas unitarias (si las hay)
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
gradlew build

# En Linux/macOS
./gradlew build
```

El archivo JAR resultante en `build/libs/` puede distribuirse a los usuarios finales para su instalación en la carpeta `mods` de Minecraft con Forge.

## Solución de Problemas Comunes

### Errores de Compilación

- Verifica que estás utilizando JDK 17.
- Ejecuta `gradlew clean` y luego `gradlew build` para limpiar archivos temporales.
- Asegúrate de que todas las dependencias estén correctamente configuradas en `build.gradle`.

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