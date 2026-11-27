package com.proyecto.servicios.service.impl;

import com.proyecto.servicios.model.pago.PagoServicioResponseDto;
import com.proyecto.servicios.util.HttpMailWebhookClient;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio backend encargado de enviar automáticamente el comprobante de pago o compra
 * al correo electrónico registrado del usuario, evitando notificaciones duplicadas por UPE.
 */
@Slf4j
@Service
public class NotificacionComprobanteEmailService {

    public static final String ASUNTO_COMPROBANTE = "Comprobante de pago - GestoPago";
    private static final String ATENCION_CLIENTES_DEFAULT = "800-437-8672";
    private static final String GATEWAY_DEFAULT = "GestoPago Gateway 2.0";

    private final Set<String> upesNotificados = ConcurrentHashMap.newKeySet();

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Autowired(required = false)
    private HttpMailWebhookClient httpMailWebhookClient;

    @Value("${spring.mail.username:}")
    private String mailFrom;

    public void setMailSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void setMailFrom(String mailFrom) {
        this.mailFrom = mailFrom;
    }

    /**
     * Envía de forma asíncrona la notificación del comprobante garantizando que nunca se envíe
     * más de un correo para la misma llave de transacción (UPE).
     */
    public void notificarOperacionAsync(String correoDestino,
                                        String nombreUsuario,
                                        PagoServicioResponseDto comprobante,
                                        boolean esCompra) {
        if (correoDestino == null || correoDestino.isBlank() || comprobante == null) {
            return;
        }
        String upeKey = comprobante.getUpe() != null ? comprobante.getUpe().trim().toUpperCase(Locale.ROOT) : "";
        if (!upeKey.isEmpty() && !upesNotificados.add(upeKey)) {
            log.info("Notificación duplicada omitida para la transacción con UPE '{}'", upeKey);
            return;
        }

        CompletableFuture.runAsync(() -> enviarCorreoComprobante(correoDestino.trim(), nombreUsuario, comprobante, esCompra));
    }

    public boolean yaFueNotificado(String upe) {
        if (upe == null || upe.isBlank()) {
            return false;
        }
        return upesNotificados.contains(upe.trim().toUpperCase(Locale.ROOT));
    }

    void enviarCorreoComprobante(String destinatario,
                                 String nombreUsuario,
                                 PagoServicioResponseDto comprobante,
                                 boolean esCompra) {
        String textoPlano = construirTextoPlano(nombreUsuario, comprobante, esCompra);
        String htmlBody = construirHtmlComprobante(nombreUsuario, comprobante, esCompra);

        if (httpMailWebhookClient != null && httpMailWebhookClient.estaConfigurado()) {
            if (httpMailWebhookClient.enviarCorreo(destinatario, ASUNTO_COMPROBANTE, textoPlano, htmlBody)) {
                log.info("Comprobante de operación ({}) enviado exitosamente por canal HTTPS a '{}' [UPE: {}]",
                        comprobante.getEstatus(), destinatario, comprobante.getUpe());
                return;
            }
        }

        if (mailSender == null) {
            log.warn("Canal SMTP no configurado (spring.mail.* ausente): no fue posible enviar el comprobante de la operación UPE '{}' a '{}'",
                    comprobante.getUpe(), destinatario);
            return;
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            String remitente = (mailFrom != null && !mailFrom.isBlank()) ? mailFrom.trim() : "noreply@gestopago.local";
            helper.setFrom(remitente, "Banca Digital GestoPago");
            helper.setTo(destinatario);
            helper.setSubject(ASUNTO_COMPROBANTE);
            helper.setText(textoPlano, htmlBody);

            mailSender.send(mimeMessage);
            log.info("Comprobante de operación ({}) enviado exitosamente por correo a '{}' [UPE: {}]",
                    comprobante.getEstatus(), destinatario, comprobante.getUpe());
        } catch (Exception ex) {
            log.warn("Error al enviar correo de comprobante a '{}' [UPE: {}]: {}",
                    destinatario, comprobante.getUpe(), ex.getMessage());
        }
    }

    public static boolean detectarSiEsCompra(String nombreProducto, String nombreServicio) {
        String texto = ((nombreProducto != null ? nombreProducto : "") + " " + (nombreServicio != null ? nombreServicio : ""))
                .toUpperCase(Locale.ROOT);
        return texto.contains("AMAZON")
                || texto.contains("NETFLIX")
                || texto.contains("SPOTIFY")
                || texto.contains("XBOX")
                || texto.contains("PLAYSTATION")
                || texto.contains("NINTENDO")
                || texto.contains("STEAM")
                || texto.contains("GOOGLE PLAY")
                || texto.contains("APPLE")
                || texto.contains("ITUNES")
                || texto.contains("CINEPOLIS")
                || texto.contains("CINEMEX")
                || texto.contains("STARBUCKS")
                || texto.contains("UBER")
                || texto.contains("DISNEY")
                || texto.contains("PARAMOUNT")
                || texto.contains("CRUNCHYROLL")
                || texto.contains("ROBLOX")
                || texto.contains("FREE FIRE")
                || texto.contains("CERTIFICADO")
                || texto.contains("TARJETA")
                || texto.contains("MEMBRESIA")
                || texto.contains("PIN ");
    }

    private String construirTextoPlano(String nombreUsuario, PagoServicioResponseDto c, boolean esCompra) {
        String estatusRaw = c.getEstatus() != null ? c.getEstatus().trim().toUpperCase(Locale.ROOT) : "";
        boolean aprobado = "APLICADO".equals(estatusRaw) || "APROBADO".equals(estatusRaw) || "PAGADO".equals(estatusRaw);
        boolean rechazado = estatusRaw.startsWith("RECHAZAD");

        String moneda = (c.getMoneda() != null && !c.getMoneda().isBlank()) ? c.getMoneda().trim() : "MXN";
        BigDecimal monto = c.getMonto() != null ? c.getMonto() : BigDecimal.ZERO;
        String montoFormateado = String.format(Locale.ROOT, "$%.2f %s", monto, moneda);
        String producto = obtenerTextoNoVacio(c.getNombreProducto(), c.getNombreServicio(), c.getTipoOperacion());
        String folioMostrar = c.getId() != null ? "#" + c.getId() : obtenerTextoNoVacio(c.getFolioTransaccion(), "-");
        String fecha = c.getFechaOperacion() != null ? c.getFechaOperacion().toString() : "";
        String atencion = obtenerTextoNoVacio(c.getAtencionClientes(), ATENCION_CLIENTES_DEFAULT);

        String estatusVisual;
        String encabezadoEstado;
        String mensajeFinal;
        if (aprobado) {
            estatusVisual = "PAGADO / APROBADO";
            encabezadoEstado = esCompra
                    ? "Su compra ha sido aprobada y procesada exitosamente."
                    : "Su pago ha sido aprobado y procesado exitosamente.";
            mensajeFinal = "Transacción procesada y liquidada a través de GestoPago. Conserve este comprobante. Para cualquier duda o aclaración al " + atencion + ".";
        } else if (rechazado) {
            estatusVisual = "RECHAZADO";
            encabezadoEstado = esCompra
                    ? "Le informamos que su compra fue rechazada."
                    : "Le informamos que la operación de pago fue rechazada.";
            if (Boolean.TRUE.equals(c.getConfirmadoSinCargo())) {
                mensajeFinal = "La operación fue rechazada por el proveedor. No se aplicó ningún cargo a su cuenta. Para cualquier duda o aclaración al " + atencion + ".";
            } else {
                mensajeFinal = obtenerTextoNoVacio(c.getMensaje(), "La operación fue rechazada.") + " Para cualquier duda o aclaración comuníquese al " + atencion + ".";
            }
        } else {
            estatusVisual = "PENDIENTE / EN CONCILIACIÓN";
            encabezadoEstado = esCompra
                    ? "Su compra se encuentra pendiente de confirmación (en conciliación) con el proveedor."
                    : "Su operación se encuentra pendiente de confirmación (en conciliación) con el proveedor.";
            mensajeFinal = "La operación quedó en proceso de conciliación ante GestoPago. Conserve este comprobante y verifique el estado antes de reintentar. Atención a clientes: " + atencion + ".";
        }

        StringBuilder sb = new StringBuilder();
        if (nombreUsuario != null && !nombreUsuario.isBlank()) {
            sb.append("Estimado(a) ").append(nombreUsuario.trim()).append(",\n\n");
        } else {
            sb.append("Estimado cliente,\n\n");
        }
        sb.append(encabezadoEstado).append("\n\n");
        sb.append(aprobado ? "COMPROBANTE OFICIAL DE PAGO" : (rechazado ? "COMPROBANTE: SERVICIO RECHAZADO" : "COMPROBANTE EN CONCILIACIÓN")).append("\n");
        sb.append(obtenerTextoNoVacio(c.getNombreGateway(), GATEWAY_DEFAULT)).append("\n");
        sb.append("--------------------------------------------------\n");
        sb.append(esCompra ? "Producto adquirido: " : "Producto / Servicio: ").append(producto).append("\n");
        sb.append(esCompra ? "Importe total: " : "Importe: ").append(montoFormateado).append("\n");
        if (c.getIdProducto() != null) {
            sb.append("Tipo: ").append(c.getIdProducto()).append("\n");
        }
        sb.append("Folio Operación: ").append(folioMostrar).append("\n");
        if (c.getUpe() != null && !c.getUpe().isBlank()) {
            sb.append("UPC (Llave de Transacción): ").append(c.getUpe()).append("\n");
        }
        if (c.getNumAutorizacion() != null && !c.getNumAutorizacion().isBlank()) {
            sb.append("No. Autorización GestoPago: ").append(c.getNumAutorizacion()).append("\n");
        }
        if (c.getIdTxProveedor() != null && !c.getIdTxProveedor().isBlank()) {
            sb.append("ID Tx Proveedor: ").append(c.getIdTxProveedor()).append("\n");
        }
        if (c.getDestinoPago() != null && !c.getDestinoPago().isBlank()) {
            sb.append("Teléfono / Referencia: ").append(c.getDestinoPago()).append("\n");
        }
        sb.append("Atención a Clientes: Dudas/Aclaraciones: ").append(atencion).append("\n");
        if (!fecha.isEmpty()) {
            sb.append("Fecha Envío Tx (dateSendTx): ").append(fecha).append("\n");
        }
        sb.append("Estatus: ").append(estatusVisual).append("\n");
        if (c.getMensaje() != null && !c.getMensaje().isBlank()) {
            sb.append("Detalle del resultado: ").append(c.getMensaje()).append("\n");
        }
        sb.append("--------------------------------------------------\n");
        sb.append(mensajeFinal).append("\n");
        return sb.toString();
    }

    private String construirHtmlComprobante(String nombreUsuario, PagoServicioResponseDto c, boolean esCompra) {
        String estatusRaw = c.getEstatus() != null ? c.getEstatus().trim().toUpperCase(Locale.ROOT) : "";
        boolean aprobado = "APLICADO".equals(estatusRaw) || "APROBADO".equals(estatusRaw) || "PAGADO".equals(estatusRaw);
        boolean rechazado = estatusRaw.startsWith("RECHAZAD");

        String subtituloHeader = aprobado
                ? "Comprobante Oficial de Operación"
                : (rechazado ? "Comprobante de Operación Rechazada" : "Comprobante en Conciliación");
        String gateway = escapar(obtenerTextoNoVacio(c.getNombreGateway(), GATEWAY_DEFAULT));
        String moneda = escapar((c.getMoneda() != null && !c.getMoneda().isBlank()) ? c.getMoneda().trim() : "MXN");
        BigDecimal monto = c.getMonto() != null ? c.getMonto() : BigDecimal.ZERO;
        String montoTexto = String.format(Locale.ROOT, "$%.2f %s", monto, moneda);
        String estiloMonto = rechazado
                ? "font-size: 30px; font-weight: 800; color: #D32F2F; text-decoration: line-through; margin: 4px 0 0 0;"
                : "font-size: 30px; font-weight: 800; color: #1A237E; margin: 4px 0 0 0;";

        String tipoLegible = obtenerTipoOperacionLegible(c.getTipoOperacion(), esCompra);
        String productoNombre = escapar(obtenerTextoNoVacio(c.getNombreProducto(), c.getNombreServicio(), tipoLegible));
        String servicioProveedor = escapar(obtenerTextoNoVacio(c.getNombreServicio(), ""));
        String badgeProveedor = escapar(obtenerSiglasProveedor(c.getNombreServicio(), c.getNombreProducto()));
        String colorBadge = obtenerColorProveedor(c.getNombreServicio(), c.getNombreProducto());

        String folioMostrar = escapar(c.getId() != null ? "#" + c.getId() : obtenerTextoNoVacio(c.getFolioTransaccion(), "-"));
        String atencion = escapar(obtenerTextoNoVacio(c.getAtencionClientes(), ATENCION_CLIENTES_DEFAULT));
        String fecha = formatearFechaLegible(c.getFechaOperacion());

        String estatusVisual = aprobado ? "PAGADO / APROBADO" : (rechazado ? "RECHAZADO" : "PENDIENTE / EN CONCILIACIÓN");
        String colorEstado = aprobado ? "#2E7D32" : (rechazado ? "#D32F2F" : "#EF6C00");
        String bgEstado = aprobado ? "#E8F5E9" : (rechazado ? "#FFEBEE" : "#FFF8E1");
        String borderEstado = aprobado ? "#A5D6A7" : (rechazado ? "#EF9A9A" : "#FFE082");
        String iconoEstado = aprobado ? "&#10004;" : (rechazado ? "&#10006;" : "&#9203;");

        StringBuilder filas = new StringBuilder();
        if (nombreUsuario != null && !nombreUsuario.isBlank()) {
            filas.append(filaHtml("Titular de la Cuenta", escapar(nombreUsuario.trim()), "#212121", true, false, true));
        }
        filas.append(filaHtml("Folio de Operación", folioMostrar, "#212121", true, false, true));
        if (c.getUpe() != null && !c.getUpe().isBlank()) {
            filas.append(filaHtml("UPC (Llave de Transacción)", escapar(c.getUpe()), "#1A237E", true, true, true));
        }
        if (c.getNumAutorizacion() != null && !c.getNumAutorizacion().isBlank()) {
            filas.append(filaHtml("No. Autorización GestoPago", escapar(c.getNumAutorizacion()), colorEstado, true, false, true));
        }
        if (c.getIdTxProveedor() != null && !c.getIdTxProveedor().isBlank()) {
            filas.append(filaHtml("ID Tx Proveedor", escapar(c.getIdTxProveedor()), "#212121", true, false, true));
        }
        if (c.getDestinoPago() != null && !c.getDestinoPago().isBlank()) {
            filas.append(filaHtml("Teléfono / Referencia", escapar(c.getDestinoPago()), "#212121", true, false, true));
        }
        if (!fecha.isEmpty()) {
            filas.append(filaHtml("Fecha y Hora", escapar(fecha), "#212121", false, false, true));
        }
        filas.append(filaHtml("Atención a Clientes", atencion, "#0077B6", true, false, true));
        filas.append(filaHtml("Estado de la Operación", estatusVisual, colorEstado, true, false, false));

        String textoTipoChip = c.getIdProducto() != null
                ? escapar(tipoLegible) + " &bull; ID " + c.getIdProducto()
                : escapar(tipoLegible);
        String tipoBadgeHtml = "<div style=\"display:inline-block; margin-top:10px; background-color:#E8EAF6; color:#1A237E; font-size:11.5px; font-weight:700; padding:5px 12px; border-radius:8px;\">"
                + textoTipoChip + "</div>";

        String subProveedorHtml = (!servicioProveedor.isEmpty() && !servicioProveedor.equalsIgnoreCase(productoNombre))
                ? "<div style=\"color:#757575; font-size:12px; font-weight:500; margin-top:3px;\">Proveedor: " + servicioProveedor + "</div>"
                : "";

        String recuadroInferiorHtml;
        if (aprobado) {
            recuadroInferiorHtml = "<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"background-color:#E8F5E9; border-left:4px solid #2E7D32; border-radius:8px;\"><tr><td style=\"padding:12px 15px; font-size:12px; line-height:1.5; color:#1B5E20;\">"
                    + "<strong>&#10004; Operación liquidada:</strong> Transacción procesada y liquidada exitosamente a través de GestoPago. Conserve este comprobante. Dudas o aclaraciones al " + atencion + "."
                    + "</td></tr></table>";
        } else if (rechazado) {
            if (Boolean.TRUE.equals(c.getConfirmadoSinCargo())) {
                recuadroInferiorHtml = "<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"background-color:#FFEBEE; border-left:4px solid #D32F2F; border-radius:8px;\"><tr><td style=\"padding:12px 15px; font-size:12px; line-height:1.5; color:#B71C1C;\">"
                        + "<strong>&#9432; COMPROBANTE: SERVICIO RECHAZADO:</strong> La operación fue rechazada por el proveedor. No se aplicó ningún cargo a su cuenta. Dudas o aclaraciones al " + atencion + "."
                        + "</td></tr></table>";
            } else {
                String detalle = escapar(obtenerTextoNoVacio(c.getMensaje(), "La operación fue rechazada."));
                recuadroInferiorHtml = "<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"background-color:#FFEBEE; border-left:4px solid #D32F2F; border-radius:8px;\"><tr><td style=\"padding:12px 15px; font-size:12px; line-height:1.5; color:#B71C1C;\">"
                        + "<strong>&#9432; COMPROBANTE: SERVICIO RECHAZADO:</strong> " + detalle + " Para cualquier duda o aclaración comuníquese al " + atencion + "."
                        + "</td></tr></table>";
            }
        } else {
            recuadroInferiorHtml = "<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"background-color:#FFF8E1; border-left:4px solid #EF6C00; border-radius:8px;\"><tr><td style=\"padding:12px 15px; font-size:12px; line-height:1.5; color:#E65100;\">"
                    + "<strong>&#9888; En conciliación:</strong> Operación en proceso de conciliación con el proveedor. Conserve este comprobante y verifique el estatus antes de reintentar. Atención: " + atencion + "."
                    + "</td></tr></table>";
        }

        return "<!DOCTYPE html>"
                + "<html lang=\"es\"><head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">"
                + "<title>" + ASUNTO_COMPROBANTE + "</title></head>"
                + "<body style=\"margin:0; padding:0; background-color:#F4F6F9; font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif; color:#212121;\">"
                + "<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"background-color:#F4F6F9; padding:30px 12px;\"><tr><td align=\"center\">"
                + "<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"max-width:520px; background-color:#FFFFFF; border-radius:16px; overflow:hidden; box-shadow:0 8px 26px rgba(26,35,126,0.10); border:1px solid #E2E8F0;\">"
                // Encabezado corporativo idéntico a la identidad visual de la app (#1A237E -> #0077B6)
                + "<tr><td style=\"background:linear-gradient(135deg, #1A237E 0%, #0077B6 100%); padding:22px 24px;\">"
                + "<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\"><tr>"
                + "<td width=\"56\" valign=\"middle\">"
                + "<div style=\"width:48px; height:48px; background-color:#FFFFFF; border-radius:12px; text-align:center; line-height:48px; font-weight:900; font-size:12.5px; color:" + colorBadge + ";\">" + badgeProveedor + "</div>"
                + "</td>"
                + "<td valign=\"middle\" style=\"padding-left:12px;\">"
                + "<div style=\"color:#FFFFFF; font-size:15px; font-weight:800; letter-spacing:0.4px;\">BANCA DIGITAL GESTOPAGO</div>"
                + "<div style=\"color:rgba(255,255,255,0.88); font-size:12px; margin-top:3px;\">" + subtituloHeader + " &bull; " + gateway + "</div>"
                + "</td></tr></table>"
                + "</td></tr>"
                // Cuerpo blanco con pastilla de estado y tarjeta de importe
                + "<tr><td style=\"padding:22px 24px 10px 24px; text-align:center;\">"
                + "<div style=\"display:inline-block; background-color:" + bgEstado + "; border:1px solid " + borderEstado + "; color:" + colorEstado + "; font-size:12px; font-weight:800; padding:6px 15px; border-radius:20px; letter-spacing:0.3px;\">"
                + iconoEstado + " " + estatusVisual + "</div>"
                + "<div style=\"margin-top:16px; background-color:#F4F6F9; border:1px solid #E2E8F0; border-radius:12px; padding:18px 16px;\">"
                + "<div style=\"color:#757575; font-size:11px; font-weight:700; letter-spacing:0.8px;\">IMPORTE DE LA OPERACIÓN</div>"
                + "<p style=\"" + estiloMonto + "\">" + montoTexto + "</p>"
                + "<div style=\"color:#0077B6; font-size:16px; font-weight:700; margin-top:8px;\">" + productoNombre + "</div>"
                + subProveedorHtml
                + tipoBadgeHtml
                + "</div>"
                + "</td></tr>"
                // Tabla de detalles
                + "<tr><td style=\"padding:10px 24px 16px 24px;\">"
                + "<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"background-color:#FFFFFF; border:1px solid #E2E8F0; border-radius:12px; overflow:hidden;\">"
                + filas
                + "</table>"
                + "</td></tr>"
                // Recuadro informativo
                + "<tr><td style=\"padding:0 24px 22px 24px;\">" + recuadroInferiorHtml + "</td></tr>"
                // Pie institucional
                + "<tr><td style=\"background-color:#F8FAFC; padding:16px 24px; text-align:center; border-top:1px solid #E2E8F0;\">"
                + "<p style=\"font-size:11px; color:#757575; margin:0 0 4px 0;\">Notificación automática emitida por Banca Digital GestoPago.</p>"
                + "<p style=\"font-size:11px; color:#94A3B8; margin:0;\">&copy; 2026 Banca Digital GestoPago S.A. de C.V.</p>"
                + "</td></tr>"
                + "</table>"
                + "</td></tr></table></body></html>";
    }

    private String filaHtml(String etiqueta, String valor, String colorValor, boolean negrita, boolean monospace, boolean conBordeInferior) {
        String weight = negrita ? "700" : "600";
        String fontFam = monospace ? "font-family:'Courier New',Courier,monospace; " : "";
        String border = conBordeInferior ? "border-bottom:1px solid #F1F5F9; " : "";
        return "<tr>"
                + "<td style=\"padding:10px 14px; " + border + "color:#757575; font-size:12.5px; vertical-align:top;\">" + etiqueta + "</td>"
                + "<td align=\"right\" style=\"padding:10px 14px; " + border + fontFam + "color:" + colorValor + "; font-size:12.8px; font-weight:" + weight + "; vertical-align:top; word-break:break-word;\">" + valor + "</td>"
                + "</tr>";
    }

    private static String obtenerTipoOperacionLegible(String tipoOperacion, boolean esCompra) {
        if (esCompra) return "Compra Digital";
        if (tipoOperacion == null || tipoOperacion.isBlank()) return "Operación GestoPago";
        String upper = tipoOperacion.toUpperCase(Locale.ROOT);
        if (upper.contains("RECARGA")) return "Recarga Telefónica";
        if (upper.contains("SERVICIO")) return "Pago de Servicio";
        return tipoOperacion;
    }

    private static String formatearFechaLegible(java.time.LocalDateTime fecha) {
        if (fecha == null) return "";
        String raw = fecha.toString().replace('T', ' ');
        int dot = raw.indexOf('.');
        return dot > 0 ? raw.substring(0, dot) : raw;
    }

    private static String obtenerSiglasProveedor(String servicio, String producto) {
        String full = ((servicio != null ? servicio : "") + " " + (producto != null ? producto : "")).trim().toUpperCase(Locale.ROOT);
        if (full.contains("AT&T") || full.contains("AT-T")) return "AT&T";
        if (full.contains("TELCEL")) return "TELCEL";
        if (full.contains("MOVISTAR")) return "MOV";
        if (full.contains("CFE") || full.contains("LUZ")) return "CFE";
        if (full.contains("TELMEX")) return "TELMEX";
        if (full.contains("AMAZON")) return "AMZN";
        if (full.contains("NETFLIX")) return "NFLX";
        if (full.contains("SPOTIFY")) return "SPOT";
        if (full.contains("TOTALPLAY")) return "TPLAY";
        if (full.contains("IZZI")) return "IZZI";
        if (full.contains("ABIB")) return "ABIB";
        if (servicio != null && !servicio.isBlank()) {
            String limpio = servicio.trim().toUpperCase(Locale.ROOT);
            return limpio.length() > 5 ? limpio.substring(0, 5) : limpio;
        }
        return "GP";
    }

    private static String obtenerColorProveedor(String servicio, String producto) {
        String full = ((servicio != null ? servicio : "") + " " + (producto != null ? producto : "")).toUpperCase(Locale.ROOT);
        if (full.contains("AT&T") || full.contains("AT-T")) return "#00A8E0";
        if (full.contains("TELCEL")) return "#002F6C";
        if (full.contains("MOVISTAR")) return "#019DF4";
        if (full.contains("CFE")) return "#007A3D";
        if (full.contains("NETFLIX")) return "#E50914";
        if (full.contains("SPOTIFY")) return "#1DB954";
        if (full.contains("AMAZON")) return "#232F3E";
        return "#0F172A";
    }

    private static String obtenerTextoNoVacio(String... candidatos) {
        for (String c : candidatos) {
            if (c != null && !c.isBlank()) {
                return c.trim();
            }
        }
        return "";
    }

    private static String escapar(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
