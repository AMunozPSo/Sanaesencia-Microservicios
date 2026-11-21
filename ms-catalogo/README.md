# Microservicio de catálogo de SanaEsencia

Spring Boot 4.1.2, Java 17+, Maven y MySQL 8.4. Puerto local: **8081**.

Gestiona servicios, perfiles profesionales y la relación entre ambos. Las cuentas, contraseñas y roles pertenecen a `ms-usuarios`; catálogo consulta su endpoint de login para autenticar cada petición protegida. No comparte la base de datos de usuarios ni crea cuentas.

## Ejecutar en Windows

1. Iniciar MySQL y crear la base `sanaesencia_catalogo` con un usuario técnico que tenga permisos sobre ella.
2. Iniciar `ms-usuarios` en el puerto 8080.
3. Abrir PowerShell en la carpeta de este microservicio y ejecutar:

```powershell
$claveCatalogo = Read-Host "Contraseña de sana_catalogo" -AsSecureString
$env:CATALOGO_DB_PASSWORD = [System.Net.NetworkCredential]::new("", $claveCatalogo).Password
.\mvnw.cmd spring-boot:run
```

La contraseña se carga en la terminal actual; no se guarda en Git. La contraseña del usuario técnico de MySQL es distinta de la contraseña de una cuenta administradora de la aplicación.

| Variable | Valor predeterminado o uso |
|---|---|
| `CATALOGO_DB_PASSWORD` | Obligatoria al iniciar contra MySQL |
| `USUARIOS_API_URL` | `http://127.0.0.1:8080` |

La conexión local a MySQL usa `127.0.0.1:3306/sanaesencia_catalogo` y el usuario `sana_catalogo`. La configuración de despliegue debe sustituir esos valores cuando corresponda. `ddl-auto=update` se utiliza durante el desarrollo.

## Endpoints

Las rutas públicas se consultan sin Authorization. En las rutas de administración, enviar **Basic Auth** con el correo y la contraseña de una cuenta activa con rol `ADMIN` de usuarios.

| Método | Ruta | Acceso y resultado |
|---|---|---|
| GET | `/api/servicios` | Público; lista activos |
| GET | `/api/servicios/{id}` | Público; consulta incluso un registro inactivo |
| POST | `/api/servicios` | ADMIN; crea y devuelve 201 |
| PUT | `/api/servicios/{id}` | ADMIN; reemplaza los campos editables |
| DELETE | `/api/servicios/{id}` | ADMIN; baja lógica, devuelve 204 |
| PATCH | `/api/servicios/{id}/estado` | ADMIN; activa o desactiva |
| GET | `/api/especialistas` | Público; lista perfiles activos |
| GET | `/api/especialistas?servicioId={id}` | Público; perfiles activos compatibles con un servicio activo |
| GET | `/api/especialistas/{id}` | Público; consulta incluso un perfil pausado |
| POST | `/api/especialistas` | ADMIN; crea y vincula servicios existentes |
| PUT | `/api/especialistas/{id}` | ADMIN; actualiza perfil y reemplaza servicios asignados |
| DELETE | `/api/especialistas/{id}` | ADMIN; baja lógica |
| PATCH | `/api/especialistas/{id}/estado` | ADMIN; activa o desactiva |
| GET | `/api/admin/catalogo/servicios` | ADMIN; incluye inactivos |
| GET | `/api/admin/catalogo/especialistas` | ADMIN; incluye pausados |
| POST | `/api/admin/catalogo/importar-centro` | ADMIN; importa la instantánea del catálogo |

Ejemplo de servicio para POST o PUT:

```json
{
  "nombre": "Psicología",
  "descripcion": "Sesión de atención psicológica",
  "precio": 30000,
  "duracionMinutos": 60,
  "permiteOnline": true
}
```

Ejemplo de especialista; reemplazar el número de servicio por un ID existente:

```json
{
  "nombre": "Especialista de ejemplo",
  "especialidad": "Psicología",
  "descripcion": "",
  "permitePresencial": true,
  "permiteOnline": true,
  "servicioIds": [1]
}
```

Para reactivar: `{"activo": true}`. Para desactivar: `{"activo": false}`.

## Reglas

- Precio mayor que cero, hasta ocho cifras enteras y dos decimales; duración positiva en minutos.
- Nombre de servicio o especialista sin duplicados, independientemente de mayúsculas.
- Un perfil requiere al menos un servicio existente y activo, y una modalidad de atención.
- Un perfil solo online no puede vincular servicios que no admiten esa modalidad.
- No se puede quitar la modalidad online a un servicio del que dependen perfiles activos solo online.
- No se reactiva un perfil sin al menos un servicio activo compatible.
- Desactivar conserva los IDs, las relaciones y los datos. No elimina reservas ni registros.
- El `id` de un perfil es el valor que debe utilizarse como `especialistaId` al asignar el rol en usuarios. No es el ID de la cuenta y no se asume que coincida con IDs de Room en Android.
- Disponibilidad mensual, horarios, boxes, distancia y reservas pertenecen al futuro `ms-reservas`.

## Importación del centro

La instantánea contiene **12 servicios y 30 perfiles**, incluidos siete pausados y dos que atienden solo online. Reutiliza los datos de `CatalogoCentro.kt` del proyecto Android, cuya consulta a [Centro SanaEsencia](https://centrosanaesencia.cl/nuestro-profesionales/) fue el 19/11/2026. No es una sincronización en tiempo real.

Los $30.000 y 60 minutos son valores de demostración definidos para el proyecto, no tarifas verificadas del centro.

El endpoint de importación busca por nombre: crea los elementos faltantes y conserva los existentes, sus precios, sus estados y sus asignaciones. Ejecutarlo nuevamente no duplica los perfiles ni crea cuentas, correos o contraseñas.

## Errores y seguridad

Formato general:

```json
{"estado":400,"mensaje":"Revisa los datos enviados","errores":{"nombre":"El nombre es obligatorio"}}
```

- 400: datos inválidos, modalidad incompatible o JSON incorrecto.
- 401: credenciales ausentes o incorrectas, o cuenta desactivada.
- 403: cuenta autenticada sin el rol requerido.
- 404: registro o servicio relacionado inexistente.
- 409: nombre duplicado o conflicto con el estado de los servicios.
- 503: no se puede comprobar la identidad porque usuarios no responde correctamente.

Las comprobaciones de usuarios tienen límites de conexión de 3 segundos y lectura de 5 segundos. No se almacenan sus contraseñas ni hashes en catálogo. Las peticiones de escritura se protegen por rol en Spring Security. La configuración sin sesión está destinada a clientes API móviles/Postman.

Las URLs HTTP de este proyecto son para desarrollo local. En AWS deben utilizarse HTTPS y configuración de red adecuada para proteger las credenciales Basic, también entre microservicios si la comunicación sale de la misma máquina.

## Pruebas y empaquetado

```powershell
.\mvnw.cmd verify
```

Las pruebas usan H2 en memoria y un proveedor de usuarios simulado; las del cliente HTTP utilizan un servidor local temporal. No requieren la contraseña de MySQL ni modifican los datos reales. Comprueban validaciones, persistencia, relaciones, bajas y reactivaciones, importación repetible, credenciales y autorización.

El ejecutable se genera en `target/ms-catalogo-0.0.1-SNAPSHOT.jar`:

```powershell
java -jar .\target\ms-catalogo-0.0.1-SNAPSHOT.jar
```

Requiere la misma variable `CATALOGO_DB_PASSWORD` y que el puerto 8081 esté libre.

## Postman

Importar `postman/SanaEsencia-Catalogo.postman_collection.json`. Configurar en la colección las variables `adminEmail`, `adminPassword`, `pacienteEmail` y `pacientePassword`. Las contraseñas del archivo están vacías.

La primera petición ejecuta la suite completa usando ambos microservicios y MySQL. Crea un servicio y un perfil con prefijo `E2E`, comprueba su ciclo de vida y los deja inactivos. También importa el catálogo del centro y verifica que una segunda importación no cree duplicados.

Los grupos restantes contienen peticiones individuales para la demostración. Las variables `servicioId` y `especialistaId` deben apuntar a registros existentes. No exportar contraseñas reales junto a la colección.

Las pruebas automatizadas de permisos no sustituyen la prueba real de comunicación con usuarios en Postman. La cobertura exigida para Android se mide aparte; estos resultados no establecen un porcentaje de cobertura móvil.

## Verificación realizada el 20/11/2026

- `mvnw.cmd verify`: 48 pruebas aprobadas (37 de integración con H2 y 11 del cliente HTTP), sin fallos ni errores; JAR ejecutable generado.
- Postman contra usuarios y catálogo locales con MySQL: 60 comprobaciones aprobadas. Incluyen creación, consulta, actualización, desactivación, reactivación, validaciones, permisos y repetición de la importación.
- Se importaron 12 servicios y 30 perfiles del centro. Quedaron 23 perfiles reales activos; se conservaron los siete pausados y los dos solo online.
- El servicio de prueba original se conservó. El servicio y el especialista con prefijo `E2E` utilizados por la suite quedaron inactivos.
- Las contraseñas del archivo de colección están vacías. Completar las variables locales de Postman antes de repetir la suite.
