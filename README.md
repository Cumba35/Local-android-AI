### LLM local — IA sin conexión en Android

Ejecuta Llama 3.1 8B Instruct (o cualquier modelo GGUF) completamente en tu teléfono Android, sin necesidad de Internet después de descargar el modelo por primera vez.

### Características

* 🦙 Motor de inferencia llama.cpp, compilado de forma nativa para ARM64.

* ⚡ Aceleración de GPU mediante Vulkan activada de forma predeterminada, con posibilidad de recurrir automáticamente a la CPU.

* 📥 Descarga automática del modelo desde Hugging Face en el primer inicio (aproximadamente 4,7 GB).

* ⏸ Reanudación de descargas: continúa desde el punto en el que se interrumpió la descarga.

* 💬 Interfaz de chat oscura y sencilla, con generación progresiva de texto.

* 🛑 Permite detener la generación en mitad de una respuesta.

### Requisitos

* Android 9.0 o superior (API 28).

* Procesador ARM64 (Snapdragon, MediaTek, etc.).

* Aproximadamente 6–8 GB de RAM libre para el modelo de 8B.

* Aproximadamente 5 GB de almacenamiento para el archivo del modelo.

* Conexión a Internet para la primera descarga del modelo.

### Compilación mediante GitHub Actions

* Haz un fork del repositorio o súbelo a tu cuenta de GitHub.

* Entra en Actions → Build LocalLLM APK → Run workflow (Ejecutar flujo de trabajo).

* Espera entre 20 y 40 minutos a que termine la compilación.

* Descarga el APK desde la sección Artifacts (Artefactos).

* Instala el APK en tu teléfono, habilitando la opción de instalar aplicaciones de fuentes desconocidas.

El flujo de trabajo realiza automáticamente las siguientes tareas:

* Clona la versión más reciente de `llama.cpp`.

* Compila las bibliotecas nativas con Android NDK para la arquitectura `arm64-v8a`.

* Habilita Vulkan si el NDK lo encuentra disponible.

* Empaqueta todo en un único APK.

### Cómo cambiar el modelo

Edita el archivo `ModelDownloader.kt`:

```kotlin
const val DEFAULT_MODEL_NAME = "tu-modelo.gguf"
const val DEFAULT_MODEL_URL  = "https://huggingface.co/repositorio/resolve/main/tu-modelo.gguf"
```

Se puede utilizar cualquier modelo en formato GGUF. Modelos probados:

| Modelo                   | Tamaño (Q4_K_M) | Velocidad (Snapdragon 8 Gen 2) |
| ------------------------ | --------------- | ------------------------------ |
| Llama 3.1 8B Instruct    | 4,7 GB          | ~8–12 tokens/s                 |
| Mistral 7B Instruct v0.3 | 4,1 GB          | ~10–14 tokens/s                |
| Llama 3.2 3B Instruct    | 2,0 GB          | ~20–28 tokens/s                |

### Formato del prompt

La aplicación utiliza de forma predeterminada el formato Llama 3 Instruct.

Si cambias a Mistral, actualiza la función `buildPrompt()` del archivo `LlamaEngine.kt` para utilizar el formato `[INST]...[/INST]`.

### Capas de GPU

De forma predeterminada, la aplicación intenta transferir todas las capas a la GPU Vulkan (`nGpuLayers = 99`).

Si tu teléfono no tiene suficiente memoria de GPU, reduce el valor en `LlamaEngine.kt`:

```kotlin
engine.loadModel(modelPath, nGpuLayers = 20)  // descarga parcial de capas a la GPU
```

### Licencia

Apache 2.0, igual que `llama.cpp`.

Importante para tu Sony H3213: tu teléfono tiene 4 GB de RAM, por lo que el modelo Llama 3.1 8B de 4,7 GB no es una opción práctica. Para tu proyecto Nova, el modelo `LFM2.5-350M-Q4_K_M.gguf` que ya utilizas es mucho más adecuado por su tamaño.

Además, el texto describe un procedimiento de compilación, pero no incluye el enlace al repositorio. Por tanto, todavía no permite comprobar si el proyecto compila correctamente ni si la aceleración Vulkan funciona en tu dispositivo.
