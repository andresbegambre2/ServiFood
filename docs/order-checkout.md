# Checkout público y pedidos

## Contrato público

El frontend usa tres operaciones bajo `/api/v1/public/orders`:

- `POST /quote` recibe el tipo de entrega y las líneas del carrito. Devuelve productos y totales recalculados.
- `POST /` recibe `multipart/form-data`: la parte JSON `order` y, solo para transferencias, la parte opcional `receipt`.
- `GET /{publicNumber}?token={trackingToken}` devuelve el estado y los snapshots que el cliente necesita para seguir su pedido.

Los contratos son DTOs; ninguna entidad JPA se serializa directamente. El navegador envía identificadores, cantidades y precios esperados únicamente para detectar cambios. El backend consulta nuevamente productos, extras, disponibilidad, promociones y configuración del negocio antes de persistir subtotal, descuento, domicilio y total.

## Número público, idempotencia y privacidad

El número visible usa `SF-yyMMdd-XXXXXX`: fecha local del restaurante y seis caracteres hexadecimales aleatorios. Existe una restricción única en base de datos y se reintenta la generación ante una colisión previa a la escritura.

Cada intento de checkout genera un `clientRequestId` UUID estable en el frontend. La columna tiene una restricción única; si la misma solicitud llega otra vez, el servicio responde con el pedido ya creado y no duplica líneas, pago ni cliente.

El seguimiento exige el número público y un token HMAC-SHA-256 no secuencial. Solo se guarda el hash SHA-256 del token. El secreto HMAC proviene de `TRACKING_SECRET`, es obligatorio en producción y no forma parte del repositorio. El token se conserva en `sessionStorage`, nunca en la URL de confirmación, y el endpoint responde únicamente snapshots, estado, importes y contacto público del negocio; no expone teléfono, correo, IDs internos ni rutas de archivos.

## Cliente invitado

Se reutiliza de forma conservadora el primer cliente cuyo teléfono normalizado coincida exactamente. El nombre y correo suministrados se conservan como snapshots del pedido. Para domicilio se agrega una dirección nueva; no se intenta deduplicar ni sobrescribir direcciones anteriores. Así el checkout sigue siendo rápido y no modifica perfiles de manera invasiva.

## Comprobantes

`ReceiptStorage` define el puerto de almacenamiento y `LocalReceiptStorage` es el adaptador de desarrollo. Esto permite migrar después a almacenamiento de objetos sin cambiar el caso de uso.

El adaptador local:

- limita el tamaño con `RECEIPTS_MAX_BYTES` y con los límites multipart de Spring;
- acepta JPG/JPEG, PNG y WEBP;
- valida extensión, MIME declarado y firma binaria;
- descarta el nombre original y genera un UUID como nombre interno;
- normaliza la ruta y exige que permanezca bajo `RECEIPTS_DIRECTORY`;
- almacena fuera de los recursos públicos y nunca publica un endpoint para servir el archivo.

La transferencia requiere comprobante y crea el pago en estado `UNDER_REVIEW`. Efectivo y pago al recoger quedan en `PENDING`.

## Horario y zona temporal

La creación consulta los horarios configurados usando `business_settings.time_zone`. El demo usa `America/Bogota`. `ALLOW_ORDERS_WHEN_CLOSED=true` omite esta regla únicamente cuando se activa de forma explícita para pruebas manuales; su valor predeterminado es `false`.

## Errores esperados

Los errores públicos incluyen un código estable y un mensaje seguro. Entre ellos están `PRODUCT_UNAVAILABLE`, `EXTRA_UNAVAILABLE`, `PRICE_CHANGED`, `RESTAURANT_CLOSED`, `INVALID_ADDRESS`, `INVALID_PAYMENT_METHOD` e `INVALID_RECEIPT`. `PRICE_CHANGED` adjunta una cotización actual para que el cliente revise y confirme nuevamente. Cualquier fallo conserva el carrito.
