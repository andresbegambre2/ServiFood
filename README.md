# ServiFood

Plataforma web para centralizar la experiencia de clientes y la operación de restaurantes. El repositorio incluye una tienda pública demostrable, una API REST y el modelo de dominio que servirá de base para las siguientes fases.

La marca de demostración de la tienda es **Distrito Smash**. ServiFood continúa siendo el nombre del software y ninguna regla funcional depende de esa identidad visual.

## Experiencia pública

El cliente puede recorrer el flujo `Inicio → Menú → Producto → Personalización → Carrito`:

- portada dinámica con categorías, productos destacados, promoción, datos y horarios del negocio;
- menú con búsqueda instantánea y filtro por categorías activas;
- detalle de producto con extras permitidos, cantidad, observaciones y total informativo;
- carrito en drawer y página dedicada, con edición de cantidades, eliminación y vaciado;
- persistencia local versionada y validada entre recargas;
- estados de carga, error, catálogo vacío, producto agotado e imagen faltante;
- navegación adaptada para escritorio, tableta y móvil.

El checkout en `/checkout` es únicamente un punto de continuación visual. La recepción de pedidos y el cálculo autoritativo de precios se implementarán en una fase posterior.

## Rutas del frontend

| Ruta | Descripción |
| --- | --- |
| `/` | Inicio público del restaurante |
| `/menu` | Catálogo, búsqueda y categorías |
| `/menu/:slug` | Detalle y personalización de producto |
| `/cart` | Vista completa del carrito |
| `/checkout` | Próximo paso informativo, todavía sin crear pedidos |

La búsqueda se ejecuta en el navegador sobre el catálogo público ya cargado. Para el volumen actual evita nuevas solicitudes en cada tecla y ofrece respuesta inmediata; si el catálogo crece, el contrato podrá evolucionar a búsqueda y paginación del lado del servidor.

## API pública

Todos los endpoints son de solo lectura, usan DTOs y se encuentran bajo `/api/v1/public`:

| Método y ruta | Contenido público |
| --- | --- |
| `GET /business` | Identidad, contacto, moneda, redes y horarios |
| `GET /categories` | Categorías activas ordenadas |
| `GET /products` | Productos activos y disponibles |
| `GET /products/featured` | Productos destacados disponibles |
| `GET /products/{slug}` | Producto, categoría y extras permitidos |
| `GET /promotions` | Promociones activas y vigentes |

La API no expone entidades JPA, usuarios, hashes ni campos administrativos. Spring Security permite explícitamente las lecturas públicas y mantiene denegado el resto de rutas no declaradas.

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
│       ├── layouts/                 Layout público responsive
│       ├── pages/                   Páginas de la tienda
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
3. Define `DEMO_ADMIN_PASSWORD` y, desde `backend`, ejecuta `./mvnw spring-boot:run` (`mvnw.cmd spring-boot:run` en Windows).
4. Desde `frontend`, ejecuta `npm install` y `npm run dev`.

La tienda queda disponible en `http://localhost:5173` y la API en `http://localhost:8080`.

El perfil `dev` crea, solo sobre una base vacía, un catálogo demostrativo con cinco categorías, productos principales, tres combos, acompañamientos, bebidas, postres, extras, una promoción vigente y la configuración pública de Distrito Smash. Las imágenes se sirven desde el propio frontend y sus rutas provienen de los productos.

## Carrito y dinero

Los importes se manejan como unidades monetarias menores enteras en el frontend. El carrito conserva cada configuración como una línea independiente, incluso si comparte producto con otra, y guarda un documento local con versión de esquema. Los datos restaurados se validan antes de usarse.

Los precios mostrados son informativos: el almacenamiento local y cualquier valor enviado por el navegador no se consideran fuentes confiables. El backend deberá recalcular el pedido cuando se implemente checkout.

## Pruebas y calidad

```bash
cd backend
./mvnw test

cd ../frontend
npm run lint
npm run test
npm run build
```

Las pruebas del backend usan H2 efímero en modo compatible con MySQL, ejecutan las migraciones Flyway y levantan el contexto de Spring sin credenciales reales. Las pruebas del frontend cubren reglas puras del carrito, extras, cantidades, líneas independientes y restauración segura.

Los detalles de entidades, relaciones, snapshots históricos y horarios se documentan en [`docs/domain-model.md`](docs/domain-model.md).
