# Microservicio de reservas de SanaEsencia

Spring Boot 4.1.2, Java 17, Maven y MySQL 8.4. Puerto local: **8082**.

Gestiona reservas, horarios semanales, disponibilidad, boxes y sugerencia de modalidad online. Usa las cuentas y roles de `ms-usuarios` y consulta los servicios y especialistas de `ms-catalogo`. No comparte sus tablas ni confía en IDs de paciente enviados por el cliente.

## Carpeta y rama

Este checkout está en `C:\Users\ASMP\Documents\Sanaesencia-Reservas`, conectado al mismo repositorio remoto en la rama **ms-reservas**. Solo contiene este microservicio. Se utiliza una copia de trabajo independiente para mantener usuarios y catálogo funcionando sin cambiar sus ramas ni retirar sus archivos.

## Arranque local

Requiere MySQL activo, usuarios en 8080 y catálogo en 8081.

En esta PC se creó `sanaesencia_reservas` y el usuario técnico `sana_reservas@127.0.0.1`, limitado a esta base. Su clave de desarrollo está cifrada para el usuario de Windows en `%LOCALAPPDATA%\SanaEsencia\reservas-db.clixml`, fuera de Git.

Desde la carpeta del microservicio:

```powershell
.\iniciar-local.ps1
```

El script lee la credencial cifrada, la pasa al proceso mediante `RESERVAS_DB_PASSWORD` y ejecuta Maven. No imprime ni guarda la clave en el proyecto.

En otra PC, después de crear la base y su cuenta técnica:

```powershell
.\iniciar-local.ps1 -PedirClave
```

O cargar manualmente la variable y ejecutar:

```powershell
$claveReservas = Read-Host "Contraseña de sana_reservas" -AsSecureString
$env:RESERVAS_DB_PASSWORD = [System.Net.NetworkCredential]::new("", $claveReservas).Password
.\mvnw.cmd spring-boot:run
```

La contraseña del administrador de la aplicación es distinta de las claves técnicas de MySQL.

Para arrancar usuarios y catálogo en esta PC sin cambiar sus claves anteriores se crearon las cuentas técnicas locales `sana_usuarios_local` y `sana_catalogo_local`, cada una limitada a su base. Sus credenciales cifradas están en el mismo directorio local, como `usuarios-db-local.clixml` y `catalogo-db-local.clixml`. No se modificó el código de esos servicios.

## Configuración

| Variable | Valor predeterminado |
|---|---|
| RESERVAS_DB_PASSWORD | Obligatoria para MySQL |
| RESERVAS_DB_URL | jdbc:mysql://127.0.0.1:3306/sanaesencia_reservas |
| RESERVAS_DB_USER | sana_reservas |
| USUARIOS_API_URL | http://127.0.0.1:8080 |
| CATALOGO_API_URL | http://127.0.0.1:8081 |
| CENTRO_LATITUD | -31.909825 |
| CENTRO_LONGITUD | -71.512501 |

Las coordenadas corresponden a las usadas en Android para Caupolicán 220, Los Vilos. La zona horaria de agenda es **America/Santiago** y el umbral de sugerencia online es **más de 20 km**.

Los horarios de la API se envían como fecha y hora local del centro, sin offset: `2026-12-07T09:00:00`. No deben interpretarse como UTC. Los comienzos utilizan una grilla de 30 minutos; la duración y el precio se obtienen del catálogo.

En desarrollo se usa `ddl-auto=update`. En AWS se deben configurar URLs, credenciales, conectividad y HTTPS, incluyendo la comunicación entre servicios cuando salga de la misma máquina. HTTP Basic envía credenciales en cada petición, por lo que HTTP se limita al entorno local.

## Acceso por rol

Todas las rutas requieren Basic Auth salvo `GET /api/estado`.

- **PACIENTE**: crea sus propias reservas; consulta, reprograma y cancela sus citas futuras.
- **ESPECIALISTA**: consulta solo las citas de su perfil vinculado, gestiona su agenda y puede cancelar o marcar atenciones terminadas. No puede reprogramar citas del paciente.
- **ADMIN**: ve todas las reservas y gestiona agendas y boxes. No crea citas suplantando al paciente.

El filtro autentica contra usuarios, comprueba el estado de la cuenta y obtiene su ID y `especialistaId` desde ese servicio. No utiliza valores proporcionados en el body para decidir quién es el paciente o especialista.

## Endpoints

| Método | Ruta | Uso |
|---|---|---|
| GET | /api/estado | Estado público |
| GET | /api/boxes | Boxes activos |
| GET | /api/admin/boxes | Incluye boxes inactivos; ADMIN |
| POST | /api/admin/boxes | Crear box; ADMIN |
| PUT | /api/admin/boxes/{id} | Actualizar nombre y descripción; ADMIN |
| PATCH | /api/admin/boxes/{id}/estado | Activar/desactivar; ADMIN |
| GET | /api/agenda/{especialistaId} | Horario semanal sin información de pacientes |
| PUT | /api/agenda/{especialistaId} | Reemplazar horario; propietario especialista o ADMIN |
| GET | /api/disponibilidad | Turnos disponibles para fecha, servicio, perfil y modalidad |
| GET | /api/ubicacion/sugerencia | Distancia y modalidades posibles |
| POST | /api/reservas | Reservar como paciente autenticado |
| GET | /api/reservas?mes=yyyy-MM | Citas propias; especialista ve su perfil; ADMIN ve todas |
| GET | /api/reservas/{id} | Detalle con comprobación de pertenencia |
| PUT | /api/reservas/{id} | Reprogramar cita confirmada futura |
| DELETE | /api/reservas/{id} | Cancelar conservando historial; devuelve 204 |
| PATCH | /api/reservas/{id}/estado | CANCELADA o ATENDIDA con permisos y reglas temporales |

Las rutas de mes incluyen reservas canceladas y atendidas para conservar historial.

### Configurar una agenda

No se inventan horarios para profesionales reales. Un administrador o el especialista vinculado debe configurar su disponibilidad antes de reservar.

```json
{
  "horarios": [
    {"dia": "MONDAY", "desde": "09:00", "hasta": "13:00"},
    {"dia": "MONDAY", "desde": "14:00", "hasta": "18:00"},
    {"dia": "TUESDAY", "desde": "09:00", "hasta": "18:00"}
  ]
}
```

Se aceptan varios bloques por día, sin superposición. Un cambio no puede dejar una reserva futura confirmada fuera de la agenda. Los días usan MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY y SUNDAY.

### Consultar disponibilidad

```text
/api/disponibilidad?especialistaId=2&servicioId=3&fecha=2026-12-07&modalidad=PRESENCIAL
```

Reemplazar los IDs y la fecha por datos existentes. Cada turno devuelve `inicio`, `fin` y los boxes libres. En ONLINE no se requieren boxes. Para un paciente también se excluyen sus citas con otros especialistas.

### Crear o reprogramar una reserva

```json
{
  "servicioId": 3,
  "especialistaId": 2,
  "inicio": "2026-12-07T09:00:00",
  "modalidad": "PRESENCIAL",
  "latitud": -33.45,
  "longitud": -70.66
}
```

La ubicación es opcional, pero latitud y longitud deben enviarse juntas. Solo se conserva la distancia calculada, no las coordenadas del paciente.

La respuesta contiene el precio y duración efectivos, nombre del especialista, nombre del servicio y **boxId/boxNombre** para consultas presenciales. Para ONLINE el box es null.

El sistema sugiere online a más de 20 km únicamente si el servicio y el profesional lo permiten. No cambia la elección del paciente ni obliga a atenderse online.

### Estados

- CONFIRMADA: ocupa el horario del especialista y paciente; si es presencial también ocupa un box.
- CANCELADA: libera esos recursos y conserva el registro.
- ATENDIDA: solo especialista propio o ADMIN, cuando la sesión ya terminó.
- Una reserva cancelada o atendida no se reactiva ni reprograma.

### Boxes

Al iniciar una base vacía se crean Box 1, Box 2 y Box 3. Se asigna aleatoriamente uno de los boxes libres. El administrador puede agregar otros. Un box no puede desactivarse mientras tenga reservas pendientes.

## Concurrencia

Todas las escrituras de reservas, agendas y boxes adquieren un bloqueo pesimista sobre una fila común de MySQL (`bloqueo_agenda`) dentro de la misma transacción.

Después de obtener el bloqueo se comprueban las citas CONFIRMADAS cuyo intervalo se superpone: `inicioExistente < finNuevo && finExistente > inicioNuevo`.

Esto permite reservas contiguas y evita dobles reservas del paciente, especialista y box, incluso con peticiones simultáneas o varias instancias conectadas a la misma base. La cancelación y reprogramación usan el mismo bloqueo.

Es una decisión sencilla para el volumen del proyecto: serializa las escrituras y puede ser un cuello de botella a gran escala. El catálogo externo no forma parte de la transacción distribuida; se valida su estado al procesar cada operación.

## Errores

- 400: datos, coordenadas, mes, modalidad o grilla de tiempo inválidos.
- 401: credenciales ausentes o incorrectas, o cuenta desactivada.
- 403: rol o pertenencia no autorizados.
- 404: reserva, box o especialista inexistente.
- 409: horario ocupado, sin boxes, agenda pausada o transición incompatible.
- 503: dependencia no disponible o respuesta de catálogo inválida.

Se devuelve JSON con `estado`, `mensaje` y `errores`. No se exponen contraseñas, SQL ni trazas internas.

## Pruebas y empaquetado

```powershell
.\mvnw.cmd verify
```

Las pruebas usan H2 en memoria, reloj fijo de Chile y proveedores simulados. No necesitan MySQL ni contraseñas. Incluyen peticiones paralelas tanto para un especialista compartido como para un único box libre.

El JAR queda en `target/ms-reservas-0.0.1-SNAPSHOT.jar` y puede ejecutarse con `java -jar` usando las mismas variables de conexión.

## Postman

Importar `postman/SanaEsencia-Reservas.postman_collection.json`. Completar las variables locales `adminEmail` y `adminPassword` y ejecutar la primera petición.

La suite requiere los tres servicios y los tres boxes iniciales activos. Crea un servicio y cinco perfiles E2E, cinco cuentas ficticias con RUN válido, vincula una cuenta como especialista y prueba reservas presenciales y online, mensualidad, privacidad, boxes, reprogramación, distancias y concurrencia.

Al terminar cancela sus citas y desactiva los perfiles, servicio y cuentas ficticias, sin eliminar el historial ni cambiar los datos reales del centro. Las contraseñas de las cuentas E2E se generan al ejecutar la suite; las del archivo exportado están vacías.

Las peticiones individuales usan `correo/password` como credenciales. Los IDs pertenecen a la API del catálogo y no se asume que coincidan con Room de Android.

## Integración pendiente

Este microservicio está listo para consumo HTTP, pero todavía falta conectar sus endpoints desde Android con Retrofit y completar el despliegue AWS. La API externa exigida en la evaluación se incorpora aparte; la comunicación entre estos tres servicios propios no la sustituye.

## Verificación del 21/11/2026

- `mvnw.cmd verify` con Java 17: **30 pruebas**, sin fallos ni errores, y JAR ejecutable generado.
- Suite de Postman contra los tres microservicios y MySQL: **77 de 77 comprobaciones aprobadas**.
- Se verificó concurrencia real: dos peticiones simultáneas para el mismo especialista produjeron un 201 y un 409.
- MySQL confirmó cinco reservas ficticias CANCELADAS y tres boxes activos al terminar. No quedaron citas E2E confirmadas.
- Los cinco perfiles, el servicio y las cinco cuentas ficticias de la suite quedaron desactivados.
- No se hicieron commits ni cambios en el código de usuarios o catálogo.
