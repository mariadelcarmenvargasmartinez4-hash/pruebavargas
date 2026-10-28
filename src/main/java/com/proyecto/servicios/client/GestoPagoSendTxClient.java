package com.proyecto.servicios.client;

import com.proyecto.servicios.model.gestopago.GestoPagoSendTxRequest;
import com.proyecto.servicios.model.gestopago.GestoPagoSendTxResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * FeignClient 3 (Envío de Transacciones - SendTx):
 * Cliente OpenFeign para enviar los datos del pago firmado y encriptado con AES hacia el proveedor GestoPago.
 */
@FeignClient(name = "gestoPagoSendTxClient", url = "${gestopago.sendtx.url:${productos.api.url}}")
public interface GestoPagoSendTxClient {

    /**
     * Envía la transacción firmada y encriptada hacia el endpoint SendTx del proveedor.
     *
     * @param authorization Token de autorización con prefijo Bearer
     * @param request Datos de la transacción de pago con payload encriptado en AES
     * @return Respuesta del proveedor evaluando el código de confirmación (código 01 = éxito)
     */
    @PostMapping(value = "/sistema/service/sendTx.do",
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    GestoPagoSendTxResponse sendTx(
            @RequestHeader("Authorization") String authorization,
            @RequestBody GestoPagoSendTxRequest request
    );
}
