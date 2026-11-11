package com.proyecto.servicios.service;

import com.proyecto.servicios.client.GestoPagoAuthClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.mapper.GestoPagoTokenMapper;
import com.proyecto.servicios.model.gestopago.GestoPagoAuthResponse;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoTokenRepository;
import com.proyecto.servicios.service.impl.GestoPagoTokenServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GestoPagoTokenServiceImplTest {

    @Mock
    private GestoPagoAuthClient gestoPagoAuthClient;

    @Mock
    private GestoPagoTokenRepository tokenRepository;

    @Mock
    private GestoPagoTokenMapper tokenMapper;

    @InjectMocks
    private GestoPagoTokenServiceImpl tokenService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(tokenService, "idDistribuidor", 83);
        ReflectionTestUtils.setField(tokenService, "codigoDispositivo", "GPS83-TPV-17");
        ReflectionTestUtils.setField(tokenService, "password", "test-pass");
    }

    @Test
    void testRenovarToken_NuevoToken_GuardaExitosamente() {
        GestoPagoAuthResponse response = new GestoPagoAuthResponse();
        response.setToken("nuevo-token-jwt");
        response.setExpiresIn(3600L);

        GestoPagoToken nuevoToken = GestoPagoToken.builder().build();

        when(gestoPagoAuthClient.authenticate(83, "GPS83-TPV-17", "test-pass")).thenReturn(response);
        when(tokenRepository.findByIdDistribuidorAndCodigoDispositivo(83, "GPS83-TPV-17")).thenReturn(Optional.empty());
        when(tokenMapper.toEntity(response)).thenReturn(nuevoToken);

        tokenService.renovarToken();

        verify(tokenRepository).save(nuevoToken);
        assertEquals(83, nuevoToken.getIdDistribuidor());
        assertEquals("GPS83-TPV-17", nuevoToken.getCodigoDispositivo());
        assertTrue(nuevoToken.getActivo());
    }

    @Test
    void testRenovarToken_TokenExistente_ActualizaExitosamente() {
        GestoPagoAuthResponse response = new GestoPagoAuthResponse();
        response.setToken("token-actualizado");

        GestoPagoToken existente = GestoPagoToken.builder()
                .id(1)
                .idDistribuidor(83)
                .codigoDispositivo("GPS83-TPV-17")
                .token("token-viejo")
                .build();

        when(gestoPagoAuthClient.authenticate(83, "GPS83-TPV-17", "test-pass")).thenReturn(response);
        when(tokenRepository.findByIdDistribuidorAndCodigoDispositivo(83, "GPS83-TPV-17")).thenReturn(Optional.of(existente));

        tokenService.renovarToken();

        verify(tokenMapper).updateEntity(response, existente);
        verify(tokenRepository).save(existente);
    }

    @Test
    void testRenovarToken_RespuestaSinToken_NoGuarda() {
        GestoPagoAuthResponse response = new GestoPagoAuthResponse();
        response.setToken(null);

        when(gestoPagoAuthClient.authenticate(83, "GPS83-TPV-17", "test-pass")).thenReturn(response);

        tokenService.renovarToken();

        verify(tokenRepository, never()).save(any());
    }

    @Test
    void testRenovarToken_FallaCliente_CapturaExcepcion() {
        when(gestoPagoAuthClient.authenticate(anyInt(), anyString(), anyString())).thenThrow(new RuntimeException("Error conexion"));

        tokenService.renovarToken();

        verify(tokenRepository, never()).save(any());
    }

    @Test
    void testObtenerTokenActivo_RetornaResultadoRepositorio() {
        GestoPagoToken token = GestoPagoToken.builder()
                .token("tok-valido")
                .activo(true)
                .build();

        when(tokenRepository.findByIdDistribuidorAndCodigoDispositivo(83, "GPS83-TPV-17")).thenReturn(Optional.of(token));

        Optional<GestoPagoToken> resultado = tokenService.obtenerTokenActivo(83, "GPS83-TPV-17");

        assertTrue(resultado.isPresent());
        assertEquals("tok-valido", resultado.get().getToken());
    }

    @Test
    void testObtenerTokenActivo_NoExiste_RetornaVacio() {
        when(tokenRepository.findByIdDistribuidorAndCodigoDispositivo(83, "GPS83-TPV-17")).thenReturn(Optional.empty());

        Optional<GestoPagoToken> resultado = tokenService.obtenerTokenActivo(83, "GPS83-TPV-17");

        assertFalse(resultado.isPresent());
    }
}
