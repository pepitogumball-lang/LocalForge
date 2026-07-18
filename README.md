# LocalForge

**LocalForge** es una aplicación Android nativa que convierte tu dispositivo en un servidor local configurable con persistencia y capacidades de gestión de archivos mediante una API compatible con OpenAI.

## Características

- **Servidor HTTP Embebido**: Utiliza Ktor (CIO) para levantar un servidor local.
- **API Compatible con OpenAI**: Endpoints `/v1/models` y `/v1/chat/completions`.
- **Gestión de Workspace**: Selección de carpeta raíz mediante Storage Access Framework (SAF).
- **Persistencia**: Guarda configuración de puerto, ruta y auto-inicio usando DataStore.
- **Foreground Service**: Mantiene el servidor activo incluso cuando la app está en segundo plano.
- **GitHub Actions**: Workflow configurado para compilación automática.

## Requisitos de Build

- Android Studio Iguana o superior.
- JDK 17.
- Gradle 8.2.

## Cómo usar

1. Clona el repositorio.
2. Abre el proyecto en Android Studio.
3. Compila e instala en una tablet o smartphone Android (API 26+).
4. Abre la app, selecciona una carpeta de trabajo y un puerto.
5. Pulsa **Start Server**.
6. Conecta tu cliente (agente) a `http://<IP_DEL_DISPOSITIVO>:<PUERTO>/v1`.

## Endpoints Soportados

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/health` | Verifica el estado del servidor |
| GET | `/v1/models` | Lista los modelos disponibles |
| POST | `/v1/chat/completions` | Interacción con el agente local |

## Seguridad

- Todas las operaciones de archivos están restringidas a la carpeta seleccionada (Workspace).
- Se requieren permisos de `FOREGROUND_SERVICE` y `POST_NOTIFICATIONS` en Android 13+.

## GitHub Actions

El proyecto incluye un workflow en `.github/workflows/android.yml` que compila automáticamente la aplicación en cada push a la rama `main`.
