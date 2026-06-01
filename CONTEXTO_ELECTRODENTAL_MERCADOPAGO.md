# Contexto ElectrodentalNea - Checkout, Mercado Pago y Stock

## Resumen del proyecto

Proyecto Spring Boot con arquitectura por capas:

- `model/domain`: entidades JPA principales.
- `model/domain/dto`: DTOs de carrito, dashboard, Andreani y Mercado Pago.
- `model/repository`: repositorios Spring Data.
- `model/service`: servicios de negocio.
- `web/controller`: controllers MVC y endpoints de integración.
- `web/config`: configuración de seguridad e integraciones.

La tienda tiene catálogo, carrito, checkout en 3 pasos, integración Mercado Pago Checkout Pro, cotización Andreani en preparación, panel admin, detalle de pedidos y vista imprimible para preparar pedidos.

## Flujo actual de checkout

1. El usuario agrega productos al carrito por AJAX.
   - Endpoint: `POST /carrito/agregar`.
   - Si el request trae `X-Requested-With: XMLHttpRequest`, responde JSON con `message` y `cartCount`.
   - Si no hay JavaScript, vuelve a la página previa.

2. Paso 1 carrito.
   - Vista: `finalizar-compra.html`.
   - Permite actualizar cantidades respetando `stockWeb`.

3. Paso 2 datos.
   - Ruta: `GET /checkout/datos`.
   - Se capturan datos del comprador: nombre, apellido, DNI, email y teléfono.
   - Se elige método de entrega:
     - `ANDREANI`
     - `SUCURSAL`
     - `VENDEDOR`
   - Los datos se guardan temporalmente en sesión como `checkoutCliente` y `checkoutDireccion`.

4. Paso 3 pago.
   - Ruta: `GET /checkout/pago`.
   - Ruta de inicio real: `POST /checkout/pagar`.
   - Al pagar se crea `Pedido PENDIENTE_PAGO`, se crea `Pago PENDIENTE`, se reserva stock web y se crea preferencia de Mercado Pago.

5. Retorno desde Mercado Pago.
   - Rutas:
     - `/checkout/mercadopago/success`
     - `/checkout/mercadopago/failure`
     - `/checkout/mercadopago/pending`
   - Si viene `payment_id`, el sistema consulta a Mercado Pago `/v1/payments/{paymentId}` y usa ese estado real.
   - Si no viene `payment_id`, muestra la vista pero no cambia estado del pedido.
   - Limpia carrito y datos temporales de checkout.

## Mercado Pago

Servicio principal:

- `MercadoPagoServiceImpl`

Controller de retorno:

- `CheckoutController`

Webhook:

- `MercadoPagoWebhookController`
- Endpoint: `/api/mercadopago/webhook`
- Acepta `POST` y `GET`.
- Procesa solo notificaciones tipo `payment` cuando el payload trae `type` o `topic`.
- Si recibe `data.id` o `payment_id`, consulta `/v1/payments/{paymentId}` con `Authorization: Bearer <access-token>`.
- Usa `external_reference` del pago real para encontrar el pedido.

Preferencia Checkout Pro:

- Endpoint usado: `POST /checkout/preferences`.
- Campos enviados:
  - `items`
  - `external_reference`
  - `notification_url`
  - `back_urls.success`
  - `back_urls.failure`
  - `back_urls.pending`
  - `auto_return=approved`

Configuración:

- `app.base-url`
- `mercadopago.api-url`
- `mercadopago.sandbox`
- `mercadopago.access-token`
- `mercadopago.public-key`
- `andreani.api-url`

El túnel Cloudflare se cambia solo en `app.base-url`. Las URLs públicas de Mercado Pago (`notification`, `success`, `failure`, `pending`) se construyen en `MercadoPagoServiceImpl` desde esa línea al crear cada preferencia.

Referencias oficiales:

- Crear preferencia Checkout Pro: https://www.mercadopago.com.ar/developers/es/docs/checkout-pro/create-payment-preference
- URLs de retorno: https://www.mercadopago.com.ar/developers/es/docs/checkout-pro/configure-back-urls
- Webhooks: https://www.mercadopago.com.ar/developers/es/docs/your-integrations/notifications/webhooks
- API de pagos: https://www.mercadopago.com.ar/developers/es/reference/payments/_payments_id/get
- Pruebas Checkout Pro: https://www.mercadopago.com.ar/developers/es/docs/checkout-pro/integration-test/test-purchases

## Estados de pago y pedido

Mapeo actual:

- `approved` o `200`
  - `Pago`: `APROBADO`
  - `Pedido`: `PAGADO`
  - No descuenta stock en este punto porque ya fue reservado al crear el pedido.

- `pending` o `in_process`
  - `Pago`: `PENDIENTE`
  - `Pedido`: `PENDIENTE_PAGO`
  - Mantiene la reserva de stock.

- `rejected`, `cancelled`, `canceled`, `failure`, `failed`
  - Libera reserva de stock si estaba activa.
  - `Pago`: `RECHAZADO`
  - `Pedido`: `CANCELADO`

- `refunded` o `charged_back`
  - `Pago`: `REEMBOLSADO`
  - `Pedido`: `CANCELADO`
  - No repone stock automáticamente porque puede depender de devolución física o despacho.

## Stock

Servicio:

- `StockServiceImpl`

Reglas actuales:

- El carrito limita cantidades a `stockWeb`.
- Al iniciar pago (`POST /checkout/pagar`) se crea el pedido y se ejecuta `stockService.reservarStockWeb`.
- La reserva registra `MovimientoStock RESERVA_WEB` y descuenta `stockWeb`.
- Si Mercado Pago rechaza/cancela/falla, se ejecuta `stockService.liberarReservaWeb` y registra `LIBERACION_RESERVA`.
- Si Mercado Pago aprueba, la reserva queda consumida como venta web.

Pendiente recomendado:

- Crear un job/scheduler para cancelar pedidos `PENDIENTE_PAGO` viejos y liberar stock reservado.
- Definir tiempo de expiración, por ejemplo 30 o 60 minutos.

## Admin y pedidos

Listado:

- `GET /admin/pedidos`
- Vista: `admin/pedidos.html`
- Fecha formateada como `dd-MM-yyyy HH:mm`.

Detalle:

- `GET /admin/pedidos/{id}`
- Vista: `admin/pedido-detalle.html`
- Muestra:
  - productos
  - imagen
  - nombre
  - cantidad
  - precio
  - total
  - método de entrega
  - estado del pago
  - datos del comprador

Vista imprimible:

- `GET /admin/pedidos/{id}/imprimir`
- Vista: `admin/pedido-print.html`
- Usa `window.print()` para imprimir o guardar PDF desde el navegador.
- Incluye logo `electrodentallarge.png`, comprador, entrega, productos y cantidades.

## Hallazgos de auditoría Mercado Pago

Corregido:

- El retorno del navegador ya no marca pedidos como aprobados solo por caer en `/success`; si hay `payment_id`, consulta Mercado Pago y usa el estado real.
- El webhook ahora ignora eventos con `type` o `topic` distinto de `payment`.
- Se agregó manejo de `charged_back`.

Correcto según flujo esperado:

- Se usa `external_reference` para asociar preferencia/pago con el pedido.
- Se usa `notification_url` para webhook.
- Se consulta `/v1/payments/{paymentId}` antes de actualizar estado en webhook.
- El webhook responde `200 OK` si procesa o ignora una notificación.

Riesgos pendientes:

- Falta validar firma de webhook (`x-signature` y `x-request-id`) con el secret configurado en Mercado Pago.
- Falta persistir `status_detail` del pago para diagnósticos finos.
- Falta una tabla/log de notificaciones recibidas para auditoría e idempotencia avanzada.
- Falta scheduler para liberar reservas de pedidos pendientes vencidos.
- Las credenciales quedaron hardcodeadas en `application.properties` por decisión local del proyecto.

## Andreani

Estado actual:

- Servicio: `AndreaniServiceImpl`.
- Solo cotización en v1.
- Usa API `/v1/tarifas`.
- Requiere configurar credenciales y contrato.

Pendiente:

- Generación de preenvío/etiqueta.
- Activar botón de descarga de ticket de envío cuando esa integración esté lista.

## Comandos útiles

Ejecutar tests:

```powershell
mvn test
```

Probar render de admin:

```powershell
Invoke-WebRequest -UseBasicParsing http://localhost:8080/admin/pedidos
Invoke-WebRequest -UseBasicParsing http://localhost:8080/admin/pedidos/15
Invoke-WebRequest -UseBasicParsing http://localhost:8080/admin/pedidos/15/imprimir
```

Probar webhook manual básico:

```powershell
Invoke-WebRequest -Method Post -UseBasicParsing http://localhost:8080/api/mercadopago/webhook -ContentType "application/json" -Body '{"type":"payment","data":{"id":"PAYMENT_ID_REAL"}}'
```

## Próximas mejoras recomendadas

1. Validar firma de webhooks con `x-signature`.
2. Guardar `status_detail`, `payment_method_id`, `payment_type_id` y `transaction_amount`.
3. Agregar scheduler de expiración de pedidos pendientes.
4. Agregar panel admin para liberar/cancelar reservas manualmente.
5. Agregar tests unitarios e integración específicos para aprobación, rechazo, pendiente, webhook duplicado y retorno sin `payment_id`.
