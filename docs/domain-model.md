# Modelo de dominio

La primera versión del dominio de ServiFood está organizada alrededor de cuatro áreas cohesionadas, sin introducir módulos o interfaces que todavía no aportan comportamiento.

## Catálogo

- `Category` clasifica productos y conserva un orden de presentación.
- `Product` pertenece a una categoría y utiliza `BigDecimal` con escala monetaria de dos decimales.
- `Extra` representa una adición con precio propio.
- La tabla intermedia `product_extras` define qué extras están permitidos para cada producto.

## Clientes y pedidos

- `Customer` conserva sus datos y administra una colección de `CustomerAddress`.
- `CustomerOrder` puede referenciar un cliente registrado, pero siempre conserva snapshots del nombre, teléfono y dirección usados en la compra.
- `OrderItem` mantiene snapshots del nombre y precio del producto.
- `OrderItemExtra` mantiene snapshots equivalentes para los extras.
- Los estados de pedido se modifican mediante operaciones de dominio que validan la transición y registran sus tiempos.

Los snapshots son deliberadamente obligatorios. Las referencias al catálogo son opcionales en los detalles históricos para permitir retirar productos sin perder la legibilidad de pedidos anteriores.

## Pagos y promociones

- `Payment` mantiene su estado separado del estado del pedido y registra quién revisó una transferencia.
- `Promotion` diferencia descuentos porcentuales y fijos. Sus validaciones no dependen de códigos, por lo que una fase posterior puede asociar cupones sin cambiar la semántica del descuento.

## Configuración y horarios

- `BusinessSettings` persiste la identidad y parámetros operativos; el frontend no debe hardcodearlos.
- `BusinessHours` utiliza una fila por día y franja, identificada por `(day_of_week, slot_number)`.

El número de franja admite los valores 1 y 2. Aunque los datos demo usan una franja diaria, esta decisión permite representar jornadas partidas —por ejemplo, almuerzo y cena— sin almacenar listas dentro de una columna ni modificar el esquema.

## Persistencia y auditoría

- Flyway es la fuente de verdad del esquema; Hibernate usa `ddl-auto=validate`.
- `AuditableEntity` centraliza `createdAt` y `updatedAt` con callbacks JPA.
- Los índices cubren búsquedas habituales por estado, fecha, categoría, disponibilidad, teléfono y estado de pago.
- Las restricciones de base de datos complementan Bean Validation para proteger los datos incluso fuera de la API.

## Datos de desarrollo

El perfil `dev` crea un catálogo, horarios, configuración y usuario administrativo únicamente cuando la base está vacía. La contraseña se lee desde `DEMO_ADMIN_PASSWORD`, se procesa con BCrypt y nunca se almacena en texto plano. El inicializador no se activa en producción ni durante las pruebas.
