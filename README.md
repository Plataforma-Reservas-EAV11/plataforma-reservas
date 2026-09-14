# Plataforma de Reservas de Servicios — Caso 14

Equipo **EAV11** — CodeF@ctory, Universidad de Antioquia (Fábrica-Escuela).

## Equipo

- Daniel Mesa Patiño
- Juan Andrés Ramírez Patiño
- Ricardo Rodríguez Loaiza
- María del Carmen Segura Ortiz
- Valeria Isabel Vásquez Barragán

## Descripción del caso

Plataforma que permite a proveedores de servicios (clínicas, consultorios, salones de belleza, centros deportivos, etc.) definir agendas y horarios disponibles, y a usuarios crear y cancelar reservas sobre esos horarios, evitando dobles reservas y sobreocupación.

## Sprint 1 — alcance

- **Definir agenda y horarios disponibles** implementada (entidades, repositorios, servicio, controlador, DTOs, excepciones y endpoint REST)
- **Crear una reserva**  implementada (entidades, repositorios, servicio con reglas de negocio y endpoint REST)
- Cancelar una reserva

## Arquitectura

Monolito modular: un único backend Spring Boot, con un solo despliegue y una sola base de datos PostgreSQL, organizado internamente por paquetes de dominio (`usuarios`, `proveedores`, `agendas`, `recursos`, `reservas`, `notificaciones`), cada uno con sus propias capas (`model` / `repository` / `service` / `controller` / `dto` / `exception`).

## Stack tecnológico

| Componente | Versión |
|---|---|
| Java (JDK) | 17 (Eclipse Temurin 17.0.20.1) |
| Spring Boot | 4.1.1 |
| Lenguaje | Java |
| Gestor de build | Maven |
| Base de datos | PostgreSQL (Supabase, vía connection pooling / Supavisor) |
| Empaquetado | Jar |
| Group | `com.udea.reservas` |
| Artifact | `plataforma-reservas` |
| Package base | `com.udea.reservas.plataformareservas` |

## Dependencias (backend)

| Dependencia | Artefacto Maven | Uso |
|---|---|---|
| Spring Web | `spring-boot-starter-webmvc` | Exponer la API REST |
| Spring Data JPA | `spring-boot-starter-data-jpa` | Persistencia ORM |
| PostgreSQL Driver | `org.postgresql:postgresql` | Conexión a la base de datos |
| Validation | `spring-boot-starter-validation` | Validación de payloads de entrada (`@NotNull`, `@Valid`) |
| Lombok | `org.projectlombok:lombok` | Reducir código repetitivo (getters/setters/constructores) |
| Spring Boot Actuator | `spring-boot-starter-actuator` | Endpoint de salud `/actuator/health` |

> Las versiones de estas dependencias las administra el Spring Boot Parent POM (4.1.1); no se fijan manualmente, para garantizar compatibilidad entre ellas.

**Pendiente para próximos sprints:** Spring Security (HU de autenticación y roles, ubicada en el Sprint 2).

## HU: Crear reserva

Endpoint: `POST /api/reservas`

**Request body:**
```json
{
  "usuarioId": 1,
  "horarioId": 1,
  "fecha": "2026-09-20",
  "hora": "09:00:00"
}
```

**Respuestas:**
- `201 Created` — reserva creada exitosamente.
- `400 Bad Request` — horario no habilitado por el proveedor (agenda inactiva), fecha/hora fuera del rango del horario, o fecha/hora ya pasada.
- `404 Not Found` — el usuario o el horario indicado no existe.
- `409 Conflict` — el horario ya tiene una reserva activa para esa fecha (evita doble reserva).


## HU: Definir agenda y horarios disponibles

Endpoint: `POST /api/agendas/{agendaId}/horarios`

**Request body:**
```json
{
  "diaSemana": 1,
  "horaInicio": "08:00:00",
  "horaFin": "12:00:00",
  "duracionSlotMin": 30
}
```
**Respuestas:**
- `200 OK` — bloque de disponibilidad guardado exitosamente.
- `400 Bad Request` — el horario se solapa con uno existente o el rango de horas es inválido.
- `404 Not Found` — la agenda indicada no existe.
## Estructura del repositorio

```
plataforma-reservas/
├── README.md
└── backend/                          # Proyecto Spring Boot
    ├── pom.xml
    ├── mvnw / mvnw.cmd
    └── src/
        ├── main/
        │   ├── java/com/udea/reservas/plataformareservas/
        │   │   ├── usuarios/       (model, repository)
        │   │   ├── proveedores/    (model)
        │   │   ├── recursos/      (model)
        │   │   ├── agendas/        (model: Agenda, HorarioDisponible — repository, service, controller, dto, exception)
        │   │   └── reservas/       (model, repository, service, controller, dto, exception)
        │   └── resources/
        │       ├── application.properties
        │       └── db/schema.sql
        └── test/java/com/udea/reservas/plataformareservas/
```

## Cómo ejecutar el proyecto

1. Clonar el repositorio:
```
   git clone https://github.com/juanramirez50-beep/plataforma-reservas.git
```
2. Entrar a la carpeta del backend:
```
   cd plataforma-reservas/backend
```
3. Configurar la conexión a la base de datos en `src/main/resources/application.properties` (usa el **connection pooling** de Supabase, no la conexión directa, porque la conexión directa requiere IPv6):
```properties
   spring.datasource.url=jdbc:postgresql://<host-del-pooler>:6543/postgres?prepareThreshold=0
   spring.datasource.username=postgres.<project-ref>
   spring.datasource.password=${DB_PASSWORD}
   spring.jpa.hibernate.ddl-auto=validate
```
   La contraseña real **nunca** se escribe aquí ni se sube al repositorio: se pasa como variable de entorno `DB_PASSWORD` (en IntelliJ: Run/Debug Configurations → Modify options → Environment variables).
4. Ejecutar el proyecto:
```
   ./mvnw spring-boot:run
```
   o desde IntelliJ, correr la clase principal `PlataformaReservasApplication`.
5. Verificar que el servicio está arriba:
```
   GET /actuator/health
```

## Contexto académico

CodeF@ctory — Fábrica-Escuela, Universidad de Antioquia. Caso 14: Plataforma de Reservas de Servicios.
