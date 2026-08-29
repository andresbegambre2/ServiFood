# ServiFood

ServiFood es una plataforma web integral para restaurantes que conecta la compra del cliente con la operación diaria del negocio. Resuelve la fragmentación entre menú digital, toma de pedidos, cocina, pagos, inventario, fidelización y reportes mediante una sola aplicación con reglas autoritativas en el backend.

El repositorio contiene una tienda pública demostrable, un panel interno por roles y una API REST lista para ejecutarse localmente con datos de muestra. La implementación prioriza consistencia transaccional, seguridad, accesibilidad, diseño responsive y una separación clara por capas.

La marca de demostración de la tienda es **Distrito Smash**. ServiFood continúa siendo el nombre del software y ninguna regla funcional depende de esa identidad visual.

## Módulos

- tienda, catálogo, personalización, carrito y checkout como invitado;
- seguimiento privado de pedidos;
- operación de pedidos, pagos y comprobantes;
- vista de producción para cocina;
- productos, categorías, promociones y configuración del negocio;
- inventario, recetas, movimientos y alertas;
- clientes, puntos, cupones e historial de compra;
- analítica, reportes por fecha y exportaciones CSV.

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
| `/admin/kitchen` | Producción para Cocina y Administración |
| `/admin/orders` | Gestión de pedidos |
| `/admin/orders/:publicNumber` | Detalle, pago y cambio de estado |
| `/admin/payments` | Cola de revisión de pagos |
| `/admin/products` | Consulta para CASHIER; administración para ADMIN |
| `/admin/inventory` | Consulta para CASHIER; inventario y recetas para ADMIN |
| `/admin/customers` | Clientes, historial, puntos y repetición de pedidos |
| `/admin/coupons` | Configuración de puntos y administración de cupones (ADMIN) |
| `/admin/analytics` | Métricas, comparaciones y gráficas del negocio (ADMIN) |
| `/admin/reports` | Reportes filtrables y exportaciones CSV (ADMIN) |
| `/admin/categories` | ADMIN |
| `/admin/promotions` | ADMIN |
| `/admin/settings` | ADMIN |

El rol de Administración tiene acceso completo; Caja opera pedidos y pagos y consulta los módulos operativos; Cocina solo accede a la vista de producción y puede iniciar o finalizar la preparación. El perfil `dev` crea usuarios demostrativos para los tres roles usando exclusivamente el valor local de `DEMO_ADMIN_PASSWORD`.

## Clientes, puntos y cupones

Cada cliente se identifica por su teléfono y conserva direcciones, historial, productos frecuentes y saldo de puntos. La regla inicial otorga un punto por cada $1.000 COP efectivamente pagados en productos. El abono sucede una sola vez al marcar el pedido como `DELIVERED`; las redenciones se reservan al crear el pedido y se devuelven si se cancela. Todos los cambios quedan en un historial auditable y los ajustes administrativos exigen motivo.

Los cupones se validan exclusivamente en el backend por vigencia, compra mínima, límite total y límite por cliente. La creación del pedido vuelve a calcular precios, disponibilidad, cupón y puntos bajo bloqueo transaccional. La repetición de pedidos utiliza precios y disponibilidad actuales, nunca los valores históricos.

| Método y ruta | Uso |
| --- | --- |
| `GET /api/v1/admin/customers` | Listado y métricas de clientes |
| `GET /api/v1/admin/customers/{id}` | Perfil, direcciones, historial y puntos |
| `POST /api/v1/admin/customers/{id}/points` | Ajuste auditable de puntos (ADMIN) |
| `GET /api/v1/admin/customers/{id}/orders/{number}/repeat` | Reconstruir pedido con catálogo actual |
| `GET/POST/PUT /api/v1/admin/coupons` | Consultar y administrar cupones |
| `GET/PUT /api/v1/admin/loyalty/settings` | Configurar equivalencia y redención |

## Reportes y analítica

La analítica avanzada calcula ventas diarias, semanales y mensuales, comparación con períodos anteriores, pedidos, cancelaciones, ticket, demanda por producto, categoría y hora, métodos de pago, entrega, clientes, descuentos, cupones, puntos y alertas de inventario. Las gráficas usan exclusivamente las series y distribuciones retornadas por el backend.

Las métricas se obtienen mediante agregaciones SQL acotadas por fecha; no cargan el historial completo ni recorren relaciones pedido a pedido. La migración V7 agrega índices para los rangos temporales de pedidos, pagos, cupones y movimientos de puntos.

| Método y ruta | Uso |
| --- | --- |
| `GET /api/v1/admin/analytics?from=&to=` | Dashboard analítico avanzado |
| `GET /api/v1/admin/reports/{type}?from=&to=` | Reportes de ventas, pedidos, productos, clientes, promociones, cupones y pagos |
| `GET /api/v1/admin/reports/{type}/csv?from=&to=` | CSV de ventas, pedidos, productos, clientes o cupones |

## Inventario y recetas

El inventario registra ingredientes en gramos, mililitros o unidades, sus existencias, mínimo, costo opcional y estado. Las recetas de productos y extras definen el consumo autoritativo. Al pasar un pedido confirmado a `PREPARING`, el backend bloquea el pedido y los ingredientes en orden estable, valida las existencias y registra un único movimiento de consumo dentro de la misma transacción. Si el pedido se cancela después, repone exactamente las cantidades consumidas y registra movimientos de reversión.

Los ajustes manuales requieren un motivo y quedan identificados como entrada o ajuste. La API nunca permite stock negativo. El catálogo público considera la receta y oculta productos o extras que no puedan prepararse. El perfil `dev` incluye ingredientes y recetas de Distrito Smash, además de un insumo bajo el mínimo para demostrar las alertas.

| Método y ruta | Uso |
| --- | --- |
| `GET /api/v1/admin/inventory` | Resumen, ingredientes, recetas y últimos movimientos |
| `POST /api/v1/admin/inventory/ingredients` | Crear ingrediente (ADMIN) |
| `PUT /api/v1/admin/inventory/ingredients/{id}` | Actualizar datos y mínimo (ADMIN) |
| `POST /api/v1/admin/inventory/ingredients/{id}/adjustments` | Registrar entrada o ajuste con motivo (ADMIN) |
| `PUT /api/v1/admin/inventory/recipes/products/{id}` | Reemplazar receta de producto (ADMIN) |
| `PUT /api/v1/admin/inventory/recipes/extras/{id}` | Reemplazar receta de extra (ADMIN) |

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
│       ├── api/                     Cliente y contratos de catálogo
│       ├── components/              Componentes reutilizables
│       ├── features/cart/           Estado, persistencia y UI del carrito
│       ├── layouts/                 Layouts público y administrativo
│       ├── pages/                   Tienda, operación, cocina y reportes
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

1. Copia `.env.example` como `.env` y ajusta únicamente tus credenciales locales. El backend importa este archivo opcionalmente al ejecutarse desde `backend` o desde la raíz.
2. Inicia MySQL con `docker compose up -d mysql` o utiliza una instancia equivalente.
3. Define al menos `SPRING_PROFILES_ACTIVE=dev`, `DEMO_ADMIN_PASSWORD` y un `TRACKING_SECRET` local de 32 caracteres o más.
4. Desde `backend`, ejecuta `./mvnw spring-boot:run` (`mvnw.cmd spring-boot:run` en Windows).
5. Desde `frontend`, ejecuta `npm install` y `npm run dev`.

La tienda queda disponible en `http://localhost:5173` y la API en `http://localhost:8080`.

El perfil `dev` ejecuta Flyway y crea, sobre una base vacía, un catálogo demostrativo con cinco categorías, productos, extras, una promoción, horarios, transferencia, ingredientes, recetas, cupones y la configuración pública de Distrito Smash. Si la base ya contiene el catálogo, completa los datos demostrativos de forma no destructiva. `DEMO_ADMIN_PASSWORD` siempre debe definirse: no existe una contraseña predeterminada ni se documentan credenciales reales.

Usuarios creados en desarrollo:

| Rol | Correo demo | Contraseña |
| --- | --- | --- |
| Administración | `admin@servifood.local` | Valor local de `DEMO_ADMIN_PASSWORD` |
| Caja | `cashier@servifood.local` | Valor local de `DEMO_ADMIN_PASSWORD` |
| Cocina | `kitchen@servifood.local` | Valor local de `DEMO_ADMIN_PASSWORD` |

Para probar pedidos fuera del horario configurado se puede definir temporalmente `ALLOW_ORDERS_WHEN_CLOSED=true`; no se recomienda fuera de una prueba local. Los comprobantes se guardan por defecto en `backend/var/receipts`, ruta ignorada por Git y no servida por HTTP.

Variables de entorno:

| Variable | Uso |
| --- | --- |
| `TRACKING_SECRET` | Secreto de al menos 32 caracteres para firmar accesos de seguimiento; obligatorio en producción |
| `ALLOW_ORDERS_WHEN_CLOSED` | Excepción explícita para pruebas locales; predeterminado `false` |
| `RECEIPTS_DIRECTORY` | Directorio privado y configurable para comprobantes |
| `RECEIPTS_MAX_BYTES` | Tamaño máximo del archivo; predeterminado 5 MiB |
| `IMAGES_DIRECTORY` | Directorio configurable de imágenes administradas |
| `IMAGES_MAX_BYTES` | Tamaño máximo para imágenes; predeterminado 5 MiB |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Conexión MySQL; la contraseña no tiene valor predeterminado |
| `DB_ROOT_PASSWORD` | Contraseña local del contenedor MySQL; solo la consume Docker Compose |
| `SERVER_PORT` | Puerto de la API; predeterminado `8080` |
| `CORS_ALLOWED_ORIGIN` | Origen exacto autorizado para el frontend |
| `SPRING_PROFILES_ACTIVE` | `dev` para datos demo; omitir o usar `prod` en despliegues |
| `DEMO_ADMIN_PASSWORD` | Contraseña local compartida por los usuarios demo; solo perfil `dev` |
| `SESSION_TIMEOUT` | Duración máxima de la sesión administrativa; predeterminado `8h` |
| `SESSION_COOKIE_SECURE` | Exige HTTPS para la cookie; debe ser `true` fuera de desarrollo |
| `VITE_API_URL` | URL pública de la API consumida durante el build del frontend |

## Base de datos y Flyway

MySQL 8.4 es la base principal. Hibernate opera con `ddl-auto=validate`: el esquema se crea y evoluciona únicamente mediante las migraciones inmutables de `backend/src/main/resources/db/migration`. Las pruebas ejecutan las mismas migraciones sobre H2 en modo compatible con MySQL. Para una instalación nueva basta con iniciar una base vacía; Flyway aplica las versiones en orden antes de que JPA valide el modelo.

## Seguridad

- autenticación interna con sesión, BCrypt y protección frente a fijación de sesión proporcionada por Spring Security;
- CSRF obligatorio para operaciones administrativas y excepción acotada a la creación pública de pedidos;
- CORS con origen explícito y credenciales controladas;
- autorización por rol aplicada en backend, incluida una API mínima para Cocina sin datos personales ni pagos;
- cookies `HttpOnly`, `SameSite=Lax` y `Secure` obligatorio con el perfil de producción;
- seguimiento mediante token firmado, comparación en tiempo constante, respuestas sin caché y política sin referente;
- comprobantes privados e imágenes validados por tamaño, tipo, extensión, firma y ruta normalizada;
- DTOs, Bean Validation y recálculo backend para precios, descuentos, puntos, cupones y disponibilidad;
- errores de producción sin mensajes internos ni trazas, y límite global de errores en React para evitar pantallas blancas.

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

Las pruebas del backend usan H2 efímero en modo compatible con MySQL, ejecutan las migraciones Flyway y levantan el contexto de Spring sin credenciales reales. Cubren dominio, persistencia, domicilio, recogida, totales, snapshots, extras, pagos, comprobantes, idempotencia, seguimiento, autenticación, permisos, cocina, inventario concurrente, fidelización, métricas y exportaciones. Las pruebas del frontend cubren carrito, checkout, recuperación ante errores, modales accesibles, login administrativo, rutas protegidas, roles, dashboard, filtros, clientes, analítica, inventario y recetas.

El workflow `.github/workflows/ci.yml` ejecuta en Pull Requests a `main` y pushes relevantes: suite backend con Java 21, instalación reproducible, lint, tests y build frontend con Node 22, además de verificación de espacios en los cambios. CI usa H2 y no requiere secretos.

Los detalles de entidades, relaciones, snapshots históricos y horarios se documentan en [`docs/domain-model.md`](docs/domain-model.md). Las decisiones de checkout, idempotencia, privacidad y comprobantes están en [`docs/order-checkout.md`](docs/order-checkout.md).

## Capturas para portafolio

Los espacios recomendados para capturas finales son:

1. portada y menú responsive de Distrito Smash;
2. checkout con cotización, cupón y puntos;
3. panel operativo y detalle del pedido;
4. tablero de Cocina;
5. inventario con alertas y movimientos;
6. analítica y reportes.

Las capturas deben generarse con datos demo, sin comprobantes reales, teléfonos personales ni credenciales visibles. Hasta definir el entorno público definitivo, esta lista funciona como guía de reemplazo para las imágenes del caso de estudio.

## Roadmap

- despliegue demostrativo con dominio y observabilidad;
- recuperación de acceso y administración avanzada de usuarios internos;
- notificaciones transaccionales por canales configurables;
- paginación del catálogo y de listados internos para volúmenes superiores;
- pruebas de extremo a extremo en navegador dentro de CI.
