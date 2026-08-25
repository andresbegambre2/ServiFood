# ServiFood

Plataforma web para centralizar la experiencia de clientes, caja, administración y cocina de una hamburguesería. Esta primera fase establece una base segura y mantenible; los módulos de negocio se incorporarán en fases posteriores.

## Arquitectura

El repositorio separa claramente las dos aplicaciones:

```text
ServiFood/
├── backend/                 API REST con Java 21 y Spring Boot
│   └── src/main/java/com/servifood/
│       ├── presentation/    Controladores REST y DTO
│       ├── application/     Casos de uso y servicios
│       ├── domain/          Entidades y reglas de negocio
│       ├── infrastructure/  Persistencia y adaptadores
│       └── config/          Seguridad y configuración transversal
├── frontend/                Aplicación React con TypeScript
├── docker-compose.yml       MySQL local
└── .env.example             Variables requeridas sin secretos
```

## Tecnologías

- Java 21, Spring Boot 4.1 y Spring Web MVC
- Spring Security, Spring Data JPA y Bean Validation
- MySQL 8.4 y migraciones Flyway
- React 19, TypeScript y Vite
- JUnit, Mockito y MockMvc
- Docker Compose para infraestructura local

## Requisitos

- JDK 21
- Node.js 22 o superior
- Docker Desktop, o una instancia compatible de MySQL 8

No es necesario instalar Maven: el repositorio incluye Maven Wrapper.

## Configuración local

1. Copia `.env.example` como `.env` y cambia las credenciales locales.
2. Inicia MySQL con `docker compose up -d mysql`.
3. Desde `backend`, ejecuta `./mvnw spring-boot:run` (en Windows, `mvnw.cmd spring-boot:run`).
4. Desde `frontend`, ejecuta `npm install` y `npm run dev`.

La web estará disponible en `http://localhost:5173`. El endpoint inicial es `GET http://localhost:8080/api/v1/public/status`.

El diseño de entidades, relaciones, snapshots históricos y horarios se describe en [`docs/domain-model.md`](docs/domain-model.md).

## Perfiles y seguridad

- Los datos sensibles se leen de variables de entorno; `.env` está excluido de Git.
- El perfil `prod` exige credenciales de base de datos externas.
- Spring Security deniega por defecto cualquier ruta no declarada pública.
- CORS permite únicamente el origen configurado.
- JPA no modifica el esquema; Flyway administra los cambios de base de datos.
- La sesión del backend es stateless. La autenticación se incorporará con el módulo administrativo.

## Pruebas y calidad

```bash
cd backend
./mvnw test

cd ../frontend
npm run lint
npm run build
```

Las pruebas del backend usan una base H2 efímera compatible con MySQL y no requieren credenciales reales.

## Estado del proyecto

Esta fase incluye el monorepo backend/frontend, configuración de MySQL y Flyway, seguridad restrictiva, endpoint REST de estado, portada responsive y pruebas básicas. Los modelos, pedidos, productos y demás módulos funcionales quedan para fases posteriores.

