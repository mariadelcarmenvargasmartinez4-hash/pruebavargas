package com.proyecto.servicios.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

// Cliente Feign para consultar catalogo de productos de GestoPago
@FeignClient(name = "gestoPagoProductClient", url = "${productos.api.url}")
public interface GestoPagoProductClient {

    // Consume endpoint getProductList.do enviando Bearer token en Authorization
    @GetMapping(value = "/sistema/service/getProductList.do")
    String getProductList(
            @RequestHeader("Authorization") String authorization
    );
}
