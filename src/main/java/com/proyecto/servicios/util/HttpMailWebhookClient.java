package com.proyecto.servicios.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Cliente HTTPS (puerto 443) para despacho seguro de correos mediante variable de entorno MAIL_WEBHOOK_URL
 * cuando el proveedor de infraestructura en la nube bloquea los puertos salientes SMTP (25/465/587).
 * No almacena credenciales ni URLs en código fuente.
 */
@Slf4j
@Component
public class HttpMailWebhookClient {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClientNoRedirect = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final HttpClient httpClientFollowGet = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Value("${mail.webhook.url:${MAIL_WEBHOOK_URL:}}")
    private String webhookUrl;

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public boolean estaConfigurado() {
        return webhookUrl != null && !webhookUrl.isBlank() && webhookUrl.trim().startsWith("https://");
    }

    public boolean enviarCorreo(String destinatario, String asunto, String textoPlano, String htmlBody) {
        if (!estaConfigurado() || destinatario == null || destinatario.isBlank()) {
            return false;
        }

        try {
            Map<String, String> payload = new LinkedHashMap<>();
            payload.put("to", destinatario.trim());
            payload.put("subject", asunto != null ? asunto : "");
            payload.put("textBody", textoPlano != null ? textoPlano : "");
            payload.put("htmlBody", htmlBody != null ? htmlBody : "");
            payload.put("fromName", "Banca Digital GestoPago");

            String jsonBody = objectMapper.writeValueAsString(payload);

            HttpRequest postReq = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl.trim()))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClientNoRedirect.send(
                    postReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            int status = response.statusCode();
            // Google Apps Script ejecuta doPost y responde 302 hacia script.googleusercontent.com con el resultado JSON
            if (status == 301 || status == 302 || status == 303) {
                String location = response.headers().firstValue("Location").orElse(null);
                if (location != null && !location.isBlank()) {
                    HttpRequest getRedirect = HttpRequest.newBuilder()
                            .uri(URI.create(location))
                            .timeout(Duration.ofSeconds(15))
                            .header("Accept", "application/json")
                            .GET()
                            .build();
                    response = httpClientFollowGet.send(
                            getRedirect, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                    status = response.statusCode();
                } else {
                    log.info("Correo despachado exitosamente por canal HTTPS (302) a '{}'", destinatario.trim());
                    return true;
                }
            } else if (status == 307 || status == 308) {
                String location = response.headers().firstValue("Location").orElse(null);
                if (location != null && !location.isBlank()) {
                    HttpRequest postRedirect = HttpRequest.newBuilder()
                            .uri(URI.create(location))
                            .timeout(Duration.ofSeconds(20))
                            .header("Content-Type", "application/json; charset=UTF-8")
                            .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                            .build();
                    response = httpClientFollowGet.send(
                            postRedirect, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                    status = response.statusCode();
                }
            }

            String body = response.body() != null ? response.body() : "";
            if (status >= 200 && status < 300 && !body.contains("\"ok\":false")) {
                log.info("Correo enviado exitosamente por canal HTTPS a '{}'", destinatario.trim());
                return true;
            }

            log.warn("Canal HTTPS de correo respondió estado {} para '{}'", status, destinatario.trim());
            return false;
        } catch (Exception ex) {
            log.warn("Fallo envío por canal HTTPS para '{}': {}", destinatario.trim(), ex.getMessage());
            return false;
        }
    }
}
