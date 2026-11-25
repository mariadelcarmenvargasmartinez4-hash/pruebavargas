package com.proyecto.servicios.service.impl;

import com.proyecto.servicios.client.GestoPagoAuthClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.mapper.GestoPagoTokenMapper;
import com.proyecto.servicios.model.gestopago.GestoPagoAuthResponse;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoTokenRepository;
import com.proyecto.servicios.service.GestoPagoTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class GestoPagoTokenServiceImpl implements GestoPagoTokenService {

    private final GestoPagoAuthClient gestoPagoAuthClient;
    private final GestoPagoTokenRepository tokenRepository;
    private final GestoPagoTokenMapper tokenMapper;

    @Value("${gestopago.auth.id-distribuidor:83}")
    private Integer idDistribuidor;

    @Value("${gestopago.auth.codigo-dispositivo:GPS83-TPV-17}")
    private String codigoDispositivo;

    @Value("${gestopago.auth.password:12345678}")
    private String password;

    public GestoPagoTokenServiceImpl(GestoPagoAuthClient gestoPagoAuthClient,
                                     GestoPagoTokenRepository tokenRepository,
                                     GestoPagoTokenMapper tokenMapper) {
        this.gestoPagoAuthClient = gestoPagoAuthClient;
        this.tokenRepository = tokenRepository;
        this.tokenMapper = tokenMapper;
    }

    @Override
    @Scheduled(fixedRateString = "${gestopago.auth.refresh-rate-ms:3600000}", initialDelay = 0)
    public void renovarToken() {
        log.info("Renovando token GestoPago para distribuidor={}", idDistribuidor);
        try {
            GestoPagoAuthResponse response = gestoPagoAuthClient.authenticate(
                    idDistribuidor, codigoDispositivo, password);

            Optional.ofNullable(response)
                    .map(GestoPagoAuthResponse::getToken)
                    .ifPresentOrElse(tok -> {
                        GestoPagoToken tokenEntity = tokenRepository
                                .findByIdDistribuidorAndCodigoDispositivo(idDistribuidor, codigoDispositivo)
                                .map(existing -> {
                                    tokenMapper.updateEntity(response, existing);
                                    if (existing.getExpiresIn() == null || existing.getExpiresIn() <= 0) {
                                        existing.setExpiresIn(3600L);
                                    }
                                    existing.setActivo(true);
                                    existing.setFechaActualizacion(java.time.LocalDateTime.now());
                                    return existing;
                                })
                                .orElseGet(() -> {
                                    GestoPagoToken nuevo = tokenMapper.toEntity(response);
                                    nuevo.setIdDistribuidor(idDistribuidor);
                                    nuevo.setCodigoDispositivo(codigoDispositivo);
                                    nuevo.setActivo(true);
                                    if (nuevo.getExpiresIn() == null || nuevo.getExpiresIn() <= 0) {
                                        nuevo.setExpiresIn(3600L);
                                    }
                                    nuevo.setFechaCreacion(java.time.LocalDateTime.now());
                                    nuevo.setFechaActualizacion(java.time.LocalDateTime.now());
                                    return nuevo;
                                });

                        tokenRepository.save(tokenEntity);
                        log.info("Token GestoPago renovado correctamente");
                    }, () -> log.error("La respuesta de GestoPago no contiene token"));

        } catch (Exception e) {
            log.error("Error al renovar token GestoPago: {}", e.getMessage(), e);
        }
    }

    @Override
    public Optional<GestoPagoToken> obtenerTokenActivo(Integer idDistribuidor, String codigoDispositivo) {
        Optional<GestoPagoToken> tokenOpt = tokenRepository.findByIdDistribuidorAndCodigoDispositivo(idDistribuidor, codigoDispositivo);
        if (tokenOpt.isEmpty() || !tokenOpt.get().esVigente()) {
            log.info("Token GestoPago ausente o vencido en BD para distribuidor={}. Ejecutando renovación inmediata...", idDistribuidor);
            renovarToken();
            tokenOpt = tokenRepository.findByIdDistribuidorAndCodigoDispositivo(idDistribuidor, codigoDispositivo);
        }
        return tokenOpt;
    }
}
