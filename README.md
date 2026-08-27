# ServiFood

Plataforma web para centralizar la experiencia de clientes y la operación de restaurantes. El repositorio incluye una tienda pública demostrable, una API REST y el modelo de dominio que servirá de base para las siguientes fases.

La marca de demostración de la tienda es **Distrito Smash**. ServiFood continúa siendo el nombre del software y ninguna regla funcional depende de esa identidad visual.

## Experiencia pública

El cliente puede recorrer el flujo completo `Inicio → Menú → Producto → Personalización → Carrito → Checkout → Pago → Confirmación`:

- portada dinámica con categorías, productos destacados, promoción, datos y horarios del negocio;
- menú con búsqueda instantánea y filtro por categorías activas;
- detalle de producto con extras permitidos, cantidad, observaciones y total informativo;
- carrito en drawer y página dedicada, con edición de cantidades, eliminación y vaciado;
- checkout como invitado para domicilio o recogida, con total autoritativo del backend;
- efectivo, pago al recoger y transferencia configurada por el negocio;
- carga privada de comprobantes y confirmación con seguimiento protegido;
- persistencia local versionada y validada entre recargas;
- estados de carga, error, catálogo vacío, producto agotado e imagen faltante;
- navegación adaptada para escritorio, tableta y móvil.

El carrito se limpia únicamente después de una creación exitosa. Si cambia un precio o falla la solicitud, el cliente conserva sus líneas y puede revisar el nuevo total antes de reintentar.

## Rutas del frontend

| Ruta | Descripción |
| --- | --- |
| `/` | Inicio público del restaurante |
| `/menu` | Catálogo, búsqueda y categorías |
| `/menu/:slug` | Detalle y personalización de producto |
| `/cart` | Vista completa del carrito |
| `/checkout` | Datos, entrega, pago, comprobante y creación del pedido |
| `/order/:publicNumber` | Confirmación y seguimiento mediante acceso privado guardado en la sesión |

La búsqueda se ejecuta en el navegador sobre el catálogo público ya cargado. Para el volumen actual evita nuevas solicitudes en cada tecla y ofrece respuesta inmediata; si el catálogo crece, el contrato podrá evolucionar a búsqueda y paginación del lado del servidor.

## Panel administrativo

El equipo interno dispone de un panel responsive independiente del storefront:

- login y logout con sesión segura, contraseñas BCrypt y protección CSRF;
- dashboard operativo con ventas, pedidos, ticket promedio, pagos por revisar y productos destacados;
- filtros, detalle, timeline y transiciones controladas de pedidos;
- revisión de transferencias y acceso protegido a comprobantes;
- administración de productos, imágenes, categorías y promociones mediante desactivación segura;
- configuración de datos públicos, transferencias, QR y horarios del negocio;
- estados de carga, vacío, error, sesión expirada y acceso restringido.

| Ruta | Acceso |
| --- | --- |
| `/admin/login` | Usuarios internos |
| `/admin` | Dashboard para ADMIN y CASHIER |
| `/admin/orders` | Gestión de pedidos |
| `/admin/orders/:publicNumber` | Detalle, pago y cambio de estado |
| `/admin/payments` | Cola de revisión de pagos |
| `/admin/products` | Consulta para CASHIER; administración para ADMIN |
| `/admin/categories` | ADMIN |
| `/admin/promotions` | ADMIN |
| `/admin/settings` | ADMIN |

El rol `ADMIN` tiene acceso completo; `CASHIER` opera pedidos y pagos y consulta productos; `KITCHEN` no puede entrar todavía al panel general. El perfil `dev` crea usuarios demostrativos para los tres roles usando exclusivamente el valor local de `DEMO_ADMIN_PASSWORD`.

## Pantalla de cocina

La ruta `/kitchen` ofrece un tablero operativo separado del panel administrativo, diseñado para pantallas táctiles, tabletas y móviles. Solo los roles `KITCHEN` y `ADMIN` pueden abrirlo; `CASHIER` mantiene el acceso denegado.

- muestra pedidos confirmados como **Nuevos**, además de **En preparación** y **Listos**;
- conserva los pedidos más antiguos primero y destaca los que llevan 20 minutos o más;
- presenta entrega, cantidades, extras, observaciones por producto y nota general;
- permite únicamente `Nuevo → En preparación → Listo`, con actualización optimista y reversión visible si falla;
- consulta novedades cada 12 segundos, pausa las consultas mientras la pestaña está oculta y refresca al volver;
- excluye transferencias pendientes o rechazadas; solo una transferencia aprobada habilita la preparación.

La visibilidad de cocina comienza cuando Caja o Administración confirma el pedido. El botón de pantalla completa es opcional y el tablero continúa funcionando cuando el navegador no ofrece esa capacidad.

| Ruta o API | Acceso |
| --- | --- |
| `/kitchen` | KITCHEN y ADMIN |
| `GET /api/v1/kitchen/orders` | KITCHEN y ADMIN |
| `PATCH /api/v1/kitchen/orders/{publicNumber}/stage` | KITCHEN y ADMIN, con CSRF |

## API pública

La API pública usa DTOs y se encuentra bajo `/api/v1/public`:

| Método y ruta | Contenido público |
| --- | --- |
| `GET /business` | Identidad, contacto, moneda, redes y horarios |
| `GET /categories` | Categorías activas ordenadas |
| `GET /products` | Productos activos y disponibles |
| `GET /products/featured` | Productos destacados disponibles |
| `GET /products/{slug}` | Producto, categoría y extras permitidos |
| `GET /promotions` | Promociones activas y vigentes |
| `POST /orders/quote` | Cotización autoritativa del carrito |
| `POST /orders` | Creación idempotente del pedido y pago (`multipart/form-data`) |
| `GET /orders/{publicNumber}?token=…` | Seguimiento protegido del pedido |

La API no expone entidades JPA, usuarios, hashes, teléfonos, correos ni campos administrativos en seguimiento. Spring Security permite explícitamente las operaciones públicas declaradas y mantiene denegado el resto. Los precios enviados por el navegador sirven solo para detectar cambios: disponibilidad, promoción, domicilio y total se calculan de nuevo en backend.

## Arquitectura

```text
ServiFood/
├── backend/                         Java 21 + Spring Boot
│   └── src/main/java/com/servifood/
│       ├── presentation/            Controllers REST y DTOs
│       ├── application/             Casos de uso y servicios
│       ├── domain/                  Entidades y reglas de negocio
│       ├── infrastructure/          Persistencia y adaptadores
│       └── config/                  Seguridad y configuración transversal
├── frontend/                        React + TypeScript + Vite
│   ├── public/images/               Assets gastronómicos locales optimizados
│   └── src/
│       ├── api/                     Clientes públicos, administrativos y de cocina
│       ├── components/              Componentes reutilizables
│       ├── features/cart/           Estado, persistencia y UI del carrito
│       ├── features/kitchen/        Estado, polling y reglas visuales de cocina
│       ├── layouts/                 Layouts y controles de acceso
│       ├── pages/                   Tienda, panel y tablero de cocina
│       ├── types/                   DTOs TypeScript
│       └── utils/                   Moneda y utilidades
├── docs/domain-model.md             Decisiones del dominio
├── docker-compose.yml               MySQL local
└── .env.example                     Variables requeridas sin secretos
```

## Tecnologías

- Java 21, Spring Boot 4.1 y Spring Web MVC
- Spring Security, Spring Data JPA y Bean Validation
- MySQL 8.4 y Flyway
- React 19, TypeScript estricto, React Router y Vite
- JUnit, Mockito, MockMvc y Vitest

## Ejecución local

Requisitos: JDK 21, Node.js 22 o superior y MySQL 8 compatible. Maven Wrapper está incluido.

1. Copia `.env.example` como `.env` y ajusta únicamente tus credenciales locales.
2. Inicia MySQL con `docker compose up -d mysql` o utiliza una instancia equivalente.
3. Define al menos `SPRING_PROFILES_ACTIVE=dev`, `DEMO_ADMIN_PASSWORD` y un `TRACKING_SECRET` local de 32 caracteres o más.
4. Desde `backend`, ejecuta `./mvnw spring-boot:run` (`mvnw.cmd spring-boot:run` en Windows).
5. Desde `frontend`, ejecuta `npm install` y `npm run dev`.

La tienda queda disponible en `http://localhost:5173` y la API en `http://localhost:8080`.

El perfil `dev` crea, sobre una base vacía, un catálogo demostrativo con cinco categorías, productos, extras, una promoción, horarios, transferencia y la configuración pública de Distrito Smash. Si la base demo ya contiene el catálogo, completa de forma no destructiva la nueva configuración de checkout. `DEMO_ADMIN_PASSWORD` siempre debe definirse: no existe una contraseña predeterminada ni se documentan credenciales reales.

Para probar pedidos fuera del horario configurado se puede definir temporalmente `ALLOW_ORDERS_WHEN_CLOSED=true`; no se recomienda fuera de una prueba local. Los comprobantes se guardan por defecto en `backend/var/receipts`, ruta ignorada por Git y no servida por HTTP.

Variables nuevas:

| Variable | Uso |
| --- | --- |
| `TRACKING_SECRET` | Secreto de al menos 32 caracteres para firmar accesos de seguimiento; obligatorio en producción |
| `ALLOW_ORDERS_WHEN_CLOSED` | Excepción explícita para pruebas locales; predeterminado `false` |
| `RECEIPTS_DIRECTORY` | Directorio privado y configurable para comprobantes |
| `RECEIPTS_MAX_BYTES` | Tamaño máximo del archivo; predeterminado 5 MiB |
| `IMAGES_DIRECTORY` | Directorio configurable de imágenes administradas |
| `IMAGES_MAX_BYTES` | Tamaño máximo para imágenes; predeterminado 5 MiB |

## Carrito y dinero

Los importes se manejan como unidades monetarias menores enteras en el frontend. El carrito conserva cada configuración como una línea independiente, incluso si comparte producto con otra, y guarda un documento local con versión de esquema. Los datos restaurados se validan antes de usarse.

Los precios mostrados son informativos: el almacenamiento local y cualquier valor enviado por el navegador no se consideran fuentes confiables. El backend reconstruye el pedido y persiste snapshots históricos de nombres, precios, extras y dirección.

## Pruebas y calidad

```bash
cd backend
./mvnw test

cd ../frontend
npm run lint
npm run test
npm run build
```

Las pruebas del backend usan H2 efímero en modo compatible con MySQL, ejecutan las migraciones Flyway y levantan el contexto de Spring sin credenciales reales. Cubren dominio, persistencia, delivery, pickup, totales, snapshots, extras, pagos, comprobantes, idempotencia, seguimiento, autenticación, permisos, operaciones administrativas y el flujo protegido de cocina. Las pruebas del frontend cubren carrito, checkout, login administrativo, rutas protegidas, roles, dashboard, filtros, manejo de sesión, tablero de cocina, orden cronológico, transiciones y polling visible.

El workflow `.github/workflows/ci.yml` ejecuta en Pull Requests a `main` y pushes relevantes: suite backend con Java 21, instalación reproducible, lint, tests y build frontend con Node 22, además de verificación de espacios en los cambios. CI usa H2 y no requiere secretos.

Los detalles de entidades, relaciones, snapshots históricos y horarios se documentan en [`docs/domain-model.md`](docs/domain-model.md). Las decisiones de checkout, idempotencia, privacidad y comprobantes están en [`docs/order-checkout.md`](docs/order-checkout.md).
