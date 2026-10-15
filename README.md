# Countries API — prueba técnica Java

API REST que importa países desde World Bank Countries API v2, guarda los datos con Spring Data JPA y permite consultarlos sin llamar nuevamente al proveedor. Incluye Swagger UI, errores con Problem Details, pruebas y Docker Compose con PostgreSQL.

## Versiones y requisitos

| Componente | Versión / comportamiento |
| --- | --- |
| Java de la aplicación | Java 17; compilación con `--release 17` y UTF-8 |
| Spring Boot | 3.5.16, dentro de la línea 3.x exigida |
| Gradle Wrapper | 8.14.3; no requiere instalar Gradle |
| Cliente HTTP | Spring RestClient |
| Documentación | springdoc-openapi 2.9.1 y Swagger UI |
| Base predeterminada | H2 en memoria |
| Base con Docker | PostgreSQL 17 con volumen persistente |

Para ejecutar desde la terminal utiliza **JDK 17 o JDK 21**. Se genera bytecode Java 17 aunque Gradle se ejecute con JDK 21. No ejecutes este Wrapper con Java 25: Gradle 8.14.3 no admite esa combinación. El ejercicio requiere Java 17+; Docker compila y ejecuta específicamente con Java 17.

Necesitas Internet para la primera descarga de Gradle/dependencias y para importar países reales. Las pruebas simulan al proveedor y no necesitan Internet una vez descargadas las dependencias. No se requiere API key. Para Docker necesitas Docker Desktop o Docker Engine iniciado, contenedores Linux y Docker Compose; no necesitas instalar un JDK en el host.

Referencias: [requisitos de Spring Boot 3.5](https://docs.spring.io/spring-boot/3.5/system-requirements.html), [compatibilidad Java/Gradle](https://docs.gradle.org/current/userguide/compatibility.html), [springdoc para Spring Boot 3](https://springdoc.org/v2/).

## Inicio rápido en Windows: H2

Abre PowerShell en la carpeta que contiene `build.gradle` y `gradlew.bat`:

```powershell
java -version
.\gradlew.bat --version
.\gradlew.bat clean test bootJar --console=plain
.\gradlew.bat bootRun
```

Deja esa terminal abierta. La aplicación escucha en `http://localhost:8080`. Para detenerla utiliza `Ctrl+C`; los datos de H2 se pierden al detener la aplicación.

El puerto de la aplicación es **8080**, tanto en local como en Docker. Ejecuta una sola instancia a la vez. Para pasar de Docker a la ejecución local, detén únicamente el contenedor de la aplicación:

```powershell
docker compose stop app
.\gradlew.bat bootRun
```

PostgreSQL permanece activo. El comando local anterior usa H2; para utilizar PostgreSQL selecciona el perfil indicado más abajo. En Linux/macOS utiliza `./gradlew` en lugar de `.\gradlew.bat`; si hace falta, ejecuta `chmod +x gradlew`.

### Git Bash: Wrapper y puerto ocupado

Desde Git Bash usa el script de shell `./gradlew`. El archivo `gradlew.bat` corresponde a Windows y también puede ejecutarse desde Git Bash, pero no desde un Bash de Linux/WSL como un script de shell.

```bash
./gradlew --version
./gradlew test --console=plain
docker compose stop app
./gradlew bootRun
```

Swagger está en `http://localhost:8080/swagger-ui.html`. Si aparece `Port 8080 was already in use`, comprueba `docker compose ps`: el contenedor `app` puede estar utilizando ese puerto. Si prefieres utilizar la aplicación de Docker, no ejecutes `bootRun`; inicia los servicios y utiliza directamente la misma URL:

```bash
docker compose up -d
```

La ejecución local anterior usa H2. Si quieres utilizar la base PostgreSQL de Docker, mantenla activa y selecciona el perfil:

```bash
docker compose up -d --wait postgres
docker compose stop app
./gradlew bootRun --args='--spring.profiles.active=postgres'
```

La colisión de puerto es un error de arranque: cambiar el Wrapper o las dependencias no libera el puerto. Detén la ejecución local con `Ctrl+C`.

## Probar Swagger

Con la aplicación iniciada, ejecuta en **otra terminal**:

```powershell
Start-Process "http://localhost:8080/swagger-ui.html"
```

También puedes pegar esa URL en el navegador.

| Recurso | URL |
| --- | --- |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` |
| OpenAPI YAML | `http://localhost:8080/v3/api-docs.yaml` |

Verifica sus respuestas desde PowerShell:

```powershell
curl.exe -i "http://localhost:8080/swagger-ui/index.html"
curl.exe -i "http://localhost:8080/v3/api-docs"
curl.exe -i "http://localhost:8080/v3/api-docs.yaml"
```

Estas tres peticiones deben responder **200**. `/swagger-ui.html` redirige a la interfaz; esa redirección es normal.

En Swagger abre la sección **Países** y prueba:

1. `POST /api/countries/import` → **Try it out** → `code=CO` → **Execute**. Devuelve 201 si Colombia aún no existe y 200 si ya estaba guardada.
2. Repite con `code=COL`: devuelve 200 y conserva el mismo ID.
3. Ejecuta `GET /api/countries`: devuelve un arreglo de países.
4. Ejecuta `GET /api/countries/{id}` con el ID devuelto al importar.
5. Importa `ZZ` para obtener 404, y `1` para obtener 400.

No necesitas autenticación ni body. Una importación real puede devolver 502 si el Banco Mundial falla o supera los timeouts.

## PostgreSQL con Docker Compose

Desde la raíz del proyecto:

```powershell
docker compose config --quiet
docker compose up --build -d
docker compose ps
docker compose logs --tail 50 app
```

Cuando los logs indiquen `Started TestTEchApplication`, abre Swagger en la URL del puerto 8080. El Dockerfile compila con JDK 17, ejecuta con JRE 17 y usa un usuario sin privilegios.

Compose espera a que PostgreSQL esté listo mediante `pg_isready` antes de iniciar la aplicación. El servicio `app` activa el perfil `postgres` y utiliza internamente `jdbc:postgresql://postgres:5432/countries`.

Para detener y volver a iniciar los servicios conservando los países:

```powershell
docker compose down
docker compose up -d
```

Los datos se conservan en el volumen `countries-data`, cuyo nombre real incluye el prefijo del proyecto Compose. No añadas `--volumes` a `down` si quieres conservarlos.

La aplicación conserva el puerto **8080**. Antes de iniciar el contenedor `app`, detén con `Ctrl+C` cualquier ejecución local de `bootRun`. Si solo está ocupado el puerto de PostgreSQL, puedes cambiar ese puerto sin cambiar el de la aplicación:

```powershell
$env:POSTGRES_PORT = "5433"
docker compose up --build -d
```

Swagger permanece en `http://localhost:8080/swagger-ui.html`. El contenedor de la aplicación sigue usando el puerto interno 5432 de PostgreSQL. La variable anterior se aplica a la sesión actual de PowerShell.

### Solo PostgreSQL en Docker y aplicación local

```powershell
docker compose up -d --wait postgres
.\gradlew.bat bootRun --args="--spring.profiles.active=postgres"
```

Si el contenedor `app` está activo, detenlo primero con `docker compose stop app` para liberar el puerto 8080. Si cambiaste el puerto local de PostgreSQL a 5433, configura también `$env:DB_URL = "jdbc:postgresql://localhost:5433/countries"` antes de iniciar la aplicación local.

## Endpoints para Thunder Client

Usa estas URLs con la aplicación en el puerto 8080. No envíes body ni credenciales.

| Método | URL | Resultado esperado |
| --- | --- | --- |
| POST | `http://localhost:8080/api/countries/import?code=CO` | 201 al crear; 200 al actualizar |
| POST | `http://localhost:8080/api/countries/import?code=COL` | El mismo registro que CO |
| POST | `http://localhost:8080/api/countries/import?code=PE` | Importa Perú |
| POST | `http://localhost:8080/api/countries/import?code=MX` | Importa México |
| POST | `http://localhost:8080/api/countries/import?code=ES` | Importa España |
| POST | `http://localhost:8080/api/countries/import?code=JP` | Importa Japón |
| GET | `http://localhost:8080/api/countries` | 200; arreglo ordenado por ISO alpha-2 |
| GET | `http://localhost:8080/api/countries/1` | 200 si existe ese ID; 404 en otro caso |
| POST | `http://localhost:8080/api/countries/import?code=ZZ` | 404: país inexistente |
| POST | `http://localhost:8080/api/countries/import?code=1` | 400: formato inválido |
| POST | `http://localhost:8080/api/countries/import?code=` | 400: código vacío |
| POST | `http://localhost:8080/api/countries/import` | 400: falta el parámetro |
| GET | `http://localhost:8080/api/countries/999999` | 404 si ese ID no existe |

Las mismas peticiones desde PowerShell:

```powershell
curl.exe -i -X POST "http://localhost:8080/api/countries/import?code=CO"
curl.exe -i -X POST "http://localhost:8080/api/countries/import?code=COL"
curl.exe -i "http://localhost:8080/api/countries"
curl.exe -i "http://localhost:8080/api/countries/1"
curl.exe -i -X POST "http://localhost:8080/api/countries/import?code=ZZ"
curl.exe -i -X POST "http://localhost:8080/api/countries/import?code=1"
```

Se aceptan 2 o 3 letras ASCII, sin espacios, y se normalizan a mayúsculas. Al crear se incluye `Location: /api/countries/{id}`. Usa ese ID para consultar; no se garantiza que el primero sea siempre 1. Un listado sin registros devuelve `[]`.

Ejemplo ilustrativo; los valores proceden del proveedor y pueden cambiar:

```json
{
  "id": 1,
  "iso2Code": "CO",
  "iso3Code": "COL",
  "name": "Colombia",
  "capital": "Bogota",
  "region": "Latin America & Caribbean",
  "incomeLevel": "Upper middle income",
  "latitude": 4.60987,
  "longitude": -74.082
}
```

Los errores utilizan Problem Details con `status` y `detail`: 400 para formato inválido, 404 para país inexistente, y 502 para timeout, HTTP de error o respuesta externa no interpretable. No se exponen trazas internas.

```json
{
  "type": "about:blank",
  "title": "Not Found",
  "status": 404,
  "detail": "País no encontrado en el Banco Mundial"
}
```

## Pruebas y empaquetado

```powershell
# Todas las pruebas
.\gradlew.bat test --console=plain

# Integración de endpoints y Swagger
.\gradlew.bat test --tests "com.example.TestTEch.CountryIntegrationTest" --console=plain

# Compilación limpia, pruebas y JAR ejecutable
.\gradlew.bat clean test bootJar --console=plain --warning-mode all

# Ejecutar el JAR con H2
java -jar .\build\libs\TestTEch-0.0.1-SNAPSHOT.jar
```

El reporte HTML queda en `build/reports/tests/test/index.html`; los resultados XML, en `build/test-results/test/`.

Las 17 pruebas cubren:

- Servicio: creación, actualización, normalización, validación y errores sin persistir datos inválidos.
- RestClient con MockRestServiceServer: mapeo del JSON, mensaje 120 con HTTP 200, resultado vacío, JSON inválido, datos incompletos, HTTP 500 y timeout.
- Integración con Spring y H2: creación y actualización del mismo registro, Location, listado, consulta por ID y errores 400/404/502.
- OpenAPI y Swagger UI: contrato, parámetro requerido, respuestas documentadas y recursos de la interfaz.
- Arranque del contexto Spring.

También se verificó Docker con Java 17 y PostgreSQL 17: Swagger/OpenAPI/listado con HTTP 200, importación real con 201, actualización con 200 y conservación del mismo país e ID tras recrear los contenedores manteniendo el volumen. Esta comprobación manual es independiente de las pruebas automatizadas.

## Configuración

| Variable | Valor predeterminado | Uso |
| --- | --- | --- |
| `WORLD_BANK_BASE_URL` | `https://api.worldbank.org/v2` | URL del proveedor |
| `WORLD_BANK_CONNECT_TIMEOUT` | `3s` | Timeout de conexión positivo |
| `WORLD_BANK_READ_TIMEOUT` | `5s` | Timeout de lectura positivo |
| `SPRING_PROFILES_ACTIVE` | Sin perfil explícito: H2 | Usa `postgres` para PostgreSQL |
| `DB_URL` | `jdbc:postgresql://localhost:5432/countries` | Perfil PostgreSQL; Compose usa el host interno `postgres` |
| `DB_USERNAME` | `countries` | Usuario de PostgreSQL |
| `DB_PASSWORD` | `countries_local` | Contraseña de desarrollo local |
| `APP_PORT` | `8080` | Puerto publicado por Compose |
| `POSTGRES_PORT` | `5432` | Puerto de PostgreSQL publicado por Compose |

Las credenciales predeterminadas son para desarrollo local. Los puertos de Compose se publican en `127.0.0.1`. Cambiar las credenciales de inicialización no modifica los usuarios de un volumen ya creado. Al ejecutar la aplicación local, ajusta `DB_URL` si cambias el puerto de PostgreSQL.

## Arquitectura y decisiones

```text
src/main/java/com/example/TestTEch/
  controller/   Endpoints HTTP y documentación de operaciones
  service/      Importación, consultas y transacción de persistencia
  client/       RestClient y validación del formato externo
  repository/   Spring Data JPA
  model/        Entidad Country
  dto/          CountryResponse como record
  config/       Timeouts, cliente HTTP y metadatos OpenAPI
  exception/    Excepciones y respuestas Problem Details
```

- El recurso externo es `GET {base-url}/country/{code}?format=json`, según la [documentación del Banco Mundial](https://datahelpdesk.worldbank.org/knowledgebase/articles/898590-country-api-queries). Se interpreta su arreglo de metadatos y resultados y el mensaje 120 para valores inexistentes. Otros mensajes o estructuras inesperadas producen 502; los agregados regionales no se importan como países.
- La API expone su propio DTO. Las coordenadas son BigDecimal; las vacías se representan como null. Se recortan textos y se rechazan coordenadas fuera de rango.
- ISO alpha-2 tiene una restricción única de base de datos. La importación actualiza el registro existente aunque se solicite con alpha-3.
- Las escrituras se serializan en una instancia con un bloqueo hasta el commit. La consulta HTTP ocurre antes del bloqueo. Un despliegue con varias instancias necesitaría un upsert o reintento transaccional coordinado con la base.
- H2 usa `ddl-auto=create-drop` y pierde datos al detenerse. PostgreSQL usa `ddl-auto=update` y conserva el volumen. Para producción se deben usar migraciones versionadas.
- Hay logs de creación/actualización por código y errores internos en el servidor. No se devuelven trazas ni credenciales al cliente.
- Se implementaron los aspectos adicionales: Swagger/OpenAPI, records, logs, Docker Compose con PostgreSQL, integración del endpoint principal y estrategia de concurrencia. No hay funcionalidades obligatorias pendientes conocidas.

## Error de VS Code: Unsupported class file major version 69

La versión de clase 69 corresponde a Java 25. Los logs mostraban que el importador iniciaba Gradle 8.14.3 con el JRE 25 incluido en la extensión Java. Esa combinación falla antes de resolver dependencias y causa diagnósticos derivados como `The import io.swagger cannot be resolved`, aunque la terminal compile correctamente.

Se ajustó **solo** `.vscode/settings.json` de este proyecto:

```json
{
  "java.configuration.updateBuildConfiguration": "automatic",
  "files.encoding": "utf8",
  "java.project.encoding": "setDefault",
  "java.import.gradle.wrapper.enabled": true,
  "java.import.gradle.java.home": "C:/Users/klkbl/AppData/Local/Programs/Eclipse Adoptium/jdk-21.0.9.10-hotspot"
}
```

La ruta es específica de este equipo; en otra máquina debe apuntar a su JDK 17 o 21 instalado. `.vscode/` contiene configuración local y está excluida de Git. La extensión Java puede seguir usando su JRE 25 para el servidor de lenguaje: este ajuste selecciona el JDK de **Gradle**, manteniendo Java 17 como versión objetivo del proyecto.

Si el editor conserva diagnósticos anteriores:

1. Abre la carpeta que contiene directamente `build.gradle`.
2. Pulsa `Ctrl+Shift+P` y ejecuta **Java: Reload Projects**. Espera a que termine la importación.
3. Si conserva el estado anterior, ejecuta **Java: Clean Java Language Server Workspace** y acepta el reinicio solicitado por el editor.
4. Comprueba `java -version` y `.\gradlew.bat --version` en una terminal nueva; Gradle debe usar JDK 17 o 21.
5. Ejecuta `.\gradlew.bat clean test bootJar --console=plain`.

El aviso `The build file has been changed and may need reload` indica que debe sincronizarse el build; se configuró la actualización automática. La [configuración oficial de la extensión Gradle](https://github.com/microsoft/vscode-gradle#gradle--java-settings) distingue el JDK del servidor de lenguaje del JDK del daemon de Gradle.
