# Case study — Electrodental NEA

## Descripción general del proyecto

Desarrollo de una tienda online para Electrodental NEA, empresa dedicada a la comercialización de equipamiento odontológico, insumos dentales, repuestos y servicio técnico para profesionales de la salud dental.

## Objetivo del sitio

Centralizar la presentación del catálogo, la consulta de productos y la venta online en una experiencia clara y profesional, reduciendo la dependencia de la atención manual para tomar pedidos y ofreciendo un flujo de compra con pago y envío integrados.

## Público objetivo

- Odontólogos y consultorios particulares.
- Clínicas odontológicas.
- Laboratorios dentales.
- Profesionales y comercios que necesitan insumos, repuestos o equipamiento técnico.

## Alcance funcional

- Catálogo de productos con categorías, subcategorías, marcas y filtros por nombre y precio.
- Fichas de producto con imágenes, características, stock, ofertas y productos relacionados.
- Carrito de compra y checkout en pasos.
- Registro de datos del comprador y selección de modalidad de entrega.
- Pago online con Mercado Pago Checkout Pro.
- Cotización y creación de envíos mediante OCA e-Pak.
- Reserva y liberación de stock web según el estado del pago.
- Webhook y consulta de estados reales de Mercado Pago.
- Panel administrativo para productos, categorías, clientes, descuentos, configuración, pedidos y envíos.
- Dashboard operativo y vista imprimible para preparar pedidos.
- Landing de servicio técnico DentTech y página de contacto.

## Secciones del sitio

- Inicio.
- Catálogo / productos.
- Detalle de producto.
- Ofertas.
- Carrito y finalizar compra.
- Servicio técnico DentTech.
- Contacto.
- Panel de administración.

## Tecnologías usadas

- Java 21.
- Spring Boot 4.0.6.
- Spring MVC y Thymeleaf.
- Spring Data JPA / Hibernate.
- Spring Security y OAuth2 Client.
- MySQL.
- HTML, CSS y JavaScript.
- Tailwind CSS y Material Symbols en la interfaz.
- Mercado Pago Checkout Pro y Webhooks.
- OCA e-Pak para cotización, creación de envíos y etiquetas.
- Apache POI para importación/exportación de productos en Excel.
- Maven.

## Qué hice yo exactamente

Diseñé y desarrollé la solución end-to-end: modelé el dominio de productos, clientes, pedidos, pagos, envíos, descuentos y stock; implementé la lógica de negocio y los controladores; construí las vistas públicas y el panel administrativo; integré Mercado Pago y OCA; desarrollé el checkout y el control de reservas de stock; agregué gestión de imágenes, configuración de tienda, estados operativos y pruebas automatizadas; y trabajé la interfaz responsive, el SEO básico y los estados de error.

## Problema que resolvía

La operación necesitaba pasar de una gestión principalmente manual de consultas y pedidos a un canal digital propio, con catálogo actualizado, disponibilidad de stock, pagos online y seguimiento operativo de los pedidos. También era necesario ordenar la administración interna y conectar la venta con la logística.

## Solución implementada

Una plataforma de e-commerce modular con arquitectura por capas. El cliente puede descubrir productos, filtrarlos, agregarlos al carrito, completar sus datos, elegir entrega y pagar. El sistema crea el pedido, reserva stock y sincroniza el resultado del pago con Mercado Pago mediante retorno y webhook. Para la logística, calcula el costo y crea el envío con OCA. El equipo administra el catálogo y procesa pedidos desde un panel protegido.

## Beneficios para el negocio

- Nuevo canal de venta disponible las 24 horas.
- Menos carga operativa para tomar y organizar pedidos.
- Mayor claridad de catálogo, precios, ofertas y stock disponible.
- Cobros online centralizados y trazables.
- Integración entre pedido, reserva de stock y envío.
- Mejor presentación de la marca y de sus proveedores.
- Base preparada para escalar el catálogo y la operación.

## Integraciones o links

- [Sitio real](https://electrodentalnea.com.ar)
- [Repositorio del proyecto](https://github.com/DevSantiValenti/electrodentalNea)
- [Mercado Pago Checkout Pro](https://www.mercadopago.com.ar/developers/es/docs/checkout-pro/create-payment-preference)
- [OCA e-Pak](https://www.oca.com.ar/)
- [Servicio técnico DentTech](/servicio-tecnico)

## Estado del proyecto

Implementado y preparado para operación productiva, con configuración de producción para el dominio de Electrodental NEA, Mercado Pago y OCA. El repositorio continúa en evolución con mejoras de UX, operación y cobertura de pruebas.

## Año

2026.

## URL demo o sitio real

[https://electrodentalnea.com.ar](https://electrodentalnea.com.ar)

## Screenshots adicionales

- [Home / inicio](stitch_tienda_online_electrodentalnea/home_electrodentalnea/screen.png)
- [Catálogo](stitch_tienda_online_electrodentalnea/cat_logo_electrodentalnea/screen.png)
- [Detalle de producto](stitch_tienda_online_electrodentalnea/producto_electrodentalnea/screen.png)
- [Finalizar compra](stitch_tienda_online_electrodentalnea/finalizar_compra_electrodentalnea/screen.png)
- [Panel de administración](stitch_tienda_online_electrodentalnea/panel_de_administraci_n_electrodentalnea/screen.png)
- [Landing de servicio técnico DentTech](stitch_tienda_online_electrodentalnea/servicio_t_cnico_denttech_landing_page/screen.png)
