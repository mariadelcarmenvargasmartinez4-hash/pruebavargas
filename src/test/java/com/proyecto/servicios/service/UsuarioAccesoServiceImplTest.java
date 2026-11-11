package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.cliente.ClienteEntity;
import com.proyecto.servicios.entity.cliente.UsuarioAccesoEntity;
import com.proyecto.servicios.exception.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.PasswordInvalidaException;
import com.proyecto.servicios.exception.ReglaNegocioException;
import com.proyecto.servicios.exception.UsuarioNoEncontradoException;
import com.proyecto.servicios.exception.UsuarioYaExisteException;
import com.proyecto.servicios.model.cliente.UsuarioAccesoDto;
import com.proyecto.servicios.model.cliente.UsuarioConsultaFiltroRecord;
import com.proyecto.servicios.exception.CorreoDuplicadoException;
import com.proyecto.servicios.model.cliente.UsuarioAgregarRequestDto;
import com.proyecto.servicios.model.common.PaginaResponseRecord;
import com.proyecto.servicios.model.usuario.UsuarioActualizacionRequestDto;
import com.proyecto.servicios.model.usuario.UsuarioBusquedaRequestDto;
import com.proyecto.servicios.model.usuario.UsuarioCambioPasswordRequestDto;
import com.proyecto.servicios.model.usuario.UsuarioCreacionRequestDto;
import com.proyecto.servicios.model.usuario.UsuarioDetalleRequestDto;
import com.proyecto.servicios.model.usuario.UsuarioEstadoRequestDto;
import com.proyecto.servicios.model.usuario.UsuarioResponseDto;
import com.proyecto.servicios.repositorys.cliente.ClienteRepository;
import com.proyecto.servicios.repositorys.cliente.UsuarioAccesoRepository;
import com.proyecto.servicios.service.impl.UsuarioAccesoServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import com.proyecto.servicios.util.TestCredentials;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioAccesoServiceImplTest {

    @Mock
    private UsuarioAccesoRepository usuarioRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioAccesoServiceImpl usuarioService;

    @Test
    @DisplayName("crearUsuario - Éxito con cliente asociado")
    void crearUsuarioExitosoConCliente() {
        ClienteEntity cliente = ClienteEntity.builder()
                .id(1L)
                .correoElectronico("cliente@empresa.com")
                .activo(true)
                .build();

        UsuarioCreacionRequestDto request = UsuarioCreacionRequestDto.builder()
                .username("cliente@empresa.com")
                .password(TestCredentials.altPass())
                .rol("CLIENTE")
                .clienteId(1L)
                .build();

        UsuarioAccesoEntity usuarioGuardado = UsuarioAccesoEntity.builder()
                .id(10L)
                .cliente(cliente)
                .username("cliente@empresa.com")
                .passwordHash(TestCredentials.testPassHash())
                .rol("CLIENTE")
                .activo(true)
                .fechaCreacion(LocalDateTime.now())
                .fechaActualizacion(LocalDateTime.now())
                .build();

        when(usuarioRepository.existsByUsername("cliente@empresa.com")).thenReturn(false);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(usuarioRepository.existsByClienteId(1L)).thenReturn(false);
        when(passwordEncoder.encode(TestCredentials.altPass())).thenReturn(TestCredentials.testPassHash());
        when(usuarioRepository.save(any(UsuarioAccesoEntity.class))).thenReturn(usuarioGuardado);

        UsuarioResponseDto resultado = usuarioService.crearUsuario(request);

        assertNotNull(resultado);
        assertEquals(10L, resultado.getId());
        assertEquals("cliente@empresa.com", resultado.getUsername());
        assertEquals("CLIENTE", resultado.getRol());
        assertTrue(resultado.getActivo());
    }

    @Test
    @DisplayName("crearUsuario - Nombre de usuario duplicado lanza UsuarioYaExisteException")
    void crearUsuarioDuplicadoLanzaExcepcion() {
        UsuarioCreacionRequestDto request = UsuarioCreacionRequestDto.builder()
                .username("existente@empresa.com")
                .password(TestCredentials.altPass())
                .rol("ADMIN")
                .build();

        when(usuarioRepository.existsByUsername("existente@empresa.com")).thenReturn(true);

        assertThrows(UsuarioYaExisteException.class, () -> usuarioService.crearUsuario(request));
    }

    @Test
    @DisplayName("crearUsuario - Cliente inactivo lanza ReglaNegocioException")
    void crearUsuarioClienteInactivoLanzaExcepcion() {
        ClienteEntity clienteInactivo = ClienteEntity.builder()
                .id(2L)
                .activo(false)
                .build();

        UsuarioCreacionRequestDto request = UsuarioCreacionRequestDto.builder()
                .username("cliente2@empresa.com")
                .password(TestCredentials.altPass())
                .rol("CLIENTE")
                .clienteId(2L)
                .build();

        when(usuarioRepository.existsByUsername("cliente2@empresa.com")).thenReturn(false);
        when(clienteRepository.findById(2L)).thenReturn(Optional.of(clienteInactivo));

        assertThrows(ReglaNegocioException.class, () -> usuarioService.crearUsuario(request));
    }

    @Test
    @DisplayName("crearUsuario - Cliente no encontrado lanza ClienteNoEncontradoException")
    void crearUsuarioClienteNoEncontradoLanzaExcepcion() {
        UsuarioCreacionRequestDto request = UsuarioCreacionRequestDto.builder()
                .username("cliente3@empresa.com")
                .password(TestCredentials.altPass())
                .rol("CLIENTE")
                .clienteId(999L)
                .build();

        when(usuarioRepository.existsByUsername("cliente3@empresa.com")).thenReturn(false);
        when(clienteRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ClienteNoEncontradoException.class, () -> usuarioService.crearUsuario(request));
    }

    @Test
    @DisplayName("buscarUsuariosDinamico - Búsqueda paginada retorna resultados")
    void buscarUsuariosDinamicoRetornaResultados() {
        UsuarioBusquedaRequestDto filtro = UsuarioBusquedaRequestDto.builder()
                .username("admin")
                .page(0)
                .size(10)
                .sortBy("id")
                .sortDir("DESC")
                .build();

        UsuarioAccesoEntity usuario = UsuarioAccesoEntity.builder()
                .id(1L)
                .username("admin@empresa.com")
                .rol("ADMIN")
                .activo(true)
                .build();

        Page<UsuarioAccesoEntity> paginaMock = new PageImpl<>(List.of(usuario));
        when(usuarioRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(paginaMock);

        PaginaResponseRecord<UsuarioResponseDto> resultado = usuarioService.buscarUsuariosDinamico(filtro);

        assertNotNull(resultado);
        assertEquals(1, resultado.totalElementos());
        assertEquals("admin@empresa.com", resultado.contenido().get(0).getUsername());
    }

    @Test
    @DisplayName("obtenerDetalleUsuario - Búsqueda por ID existente")
    void obtenerDetalleUsuarioPorId() {
        UsuarioDetalleRequestDto request = UsuarioDetalleRequestDto.builder()
                .id(5L)
                .build();

        UsuarioAccesoEntity usuario = UsuarioAccesoEntity.builder()
                .id(5L)
                .username("detalle@empresa.com")
                .rol("CLIENTE")
                .activo(true)
                .build();

        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(usuario));

        UsuarioResponseDto resultado = usuarioService.obtenerDetalleUsuario(request);

        assertNotNull(resultado);
        assertEquals(5L, resultado.getId());
        assertEquals("detalle@empresa.com", resultado.getUsername());
    }

    @Test
    @DisplayName("obtenerDetalleUsuario - Usuario inexistente lanza UsuarioNoEncontradoException")
    void obtenerDetalleUsuarioInexistenteLanzaExcepcion() {
        UsuarioDetalleRequestDto request = UsuarioDetalleRequestDto.builder()
                .id(999L)
                .build();

        when(usuarioRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(UsuarioNoEncontradoException.class, () -> usuarioService.obtenerDetalleUsuario(request));
    }

    @Test
    @DisplayName("actualizarUsuario - Actualización exitosa")
    void actualizarUsuarioExitoso() {
        UsuarioActualizacionRequestDto request = UsuarioActualizacionRequestDto.builder()
                .id(1L)
                .username("nuevo.admin@empresa.com")
                .rol("ADMIN")
                .build();

        UsuarioAccesoEntity usuarioExistente = UsuarioAccesoEntity.builder()
                .id(1L)
                .username("viejo.admin@empresa.com")
                .rol("CLIENTE")
                .activo(true)
                .build();

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioExistente));
        when(usuarioRepository.existsByUsername("nuevo.admin@empresa.com")).thenReturn(false);
        when(usuarioRepository.save(any(UsuarioAccesoEntity.class))).thenAnswer(i -> i.getArgument(0));

        UsuarioResponseDto resultado = usuarioService.actualizarUsuario(request);

        assertNotNull(resultado);
        assertEquals("nuevo.admin@empresa.com", resultado.getUsername());
        assertEquals("ADMIN", resultado.getRol());
    }

    @Test
    @DisplayName("cambiarPassword - Éxito con contraseña correcta")
    void cambiarPasswordExitoso() {
        UsuarioCambioPasswordRequestDto request = UsuarioCambioPasswordRequestDto.builder()
                .id(1L)
                .passwordActual(TestCredentials.oldPass())
                .nuevaPassword(TestCredentials.newPass())
                .build();

        UsuarioAccesoEntity usuario = UsuarioAccesoEntity.builder()
                .id(1L)
                .passwordHash(TestCredentials.testPassHash())
                .build();

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(TestCredentials.oldPass(), TestCredentials.testPassHash())).thenReturn(true);
        when(passwordEncoder.encode(TestCredentials.newPass())).thenReturn(TestCredentials.testPassHash());

        usuarioService.cambiarPassword(request);

        verify(usuarioRepository).save(usuario);
        assertEquals(TestCredentials.testPassHash(), usuario.getPasswordHash());
    }

    @Test
    @DisplayName("cambiarPassword - Contraseña actual incorrecta lanza PasswordInvalidaException")
    void cambiarPasswordIncorrectaLanzaExcepcion() {
        UsuarioCambioPasswordRequestDto request = UsuarioCambioPasswordRequestDto.builder()
                .id(1L)
                .passwordActual(TestCredentials.wrongPass())
                .nuevaPassword(TestCredentials.newPass())
                .build();

        UsuarioAccesoEntity usuario = UsuarioAccesoEntity.builder()
                .id(1L)
                .passwordHash(TestCredentials.testPassHash())
                .build();

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(TestCredentials.wrongPass(), TestCredentials.testPassHash())).thenReturn(false);

        assertThrows(PasswordInvalidaException.class, () -> usuarioService.cambiarPassword(request));
    }

    @Test
    @DisplayName("desactivarUsuario - Éxito")
    void desactivarUsuarioExitoso() {
        UsuarioEstadoRequestDto request = UsuarioEstadoRequestDto.builder()
                .id(1L)
                .motivo("Inactividad")
                .build();

        UsuarioAccesoEntity usuario = UsuarioAccesoEntity.builder()
                .id(1L)
                .activo(true)
                .build();

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(UsuarioAccesoEntity.class))).thenAnswer(i -> i.getArgument(0));

        UsuarioResponseDto resultado = usuarioService.desactivarUsuario(request);

        assertFalse(resultado.getActivo());
    }

    @Test
    @DisplayName("desactivarUsuario - Ya inactivo lanza ReglaNegocioException")
    void desactivarUsuarioYaInactivoLanzaExcepcion() {
        UsuarioEstadoRequestDto request = UsuarioEstadoRequestDto.builder()
                .id(1L)
                .build();

        UsuarioAccesoEntity usuario = UsuarioAccesoEntity.builder()
                .id(1L)
                .activo(false)
                .build();

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        assertThrows(ReglaNegocioException.class, () -> usuarioService.desactivarUsuario(request));
    }

    @Test
    @DisplayName("reactivarUsuario - Éxito")
    void reactivarUsuarioExitoso() {
        UsuarioEstadoRequestDto request = UsuarioEstadoRequestDto.builder()
                .id(1L)
                .motivo("Reactivación")
                .build();

        UsuarioAccesoEntity usuario = UsuarioAccesoEntity.builder()
                .id(1L)
                .activo(false)
                .build();

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(UsuarioAccesoEntity.class))).thenAnswer(i -> i.getArgument(0));

        UsuarioResponseDto resultado = usuarioService.reactivarUsuario(request);

        assertTrue(resultado.getActivo());
    }

    @Test
    @DisplayName("agregarUsuario - Compatibilidad previa")
    void agregarUsuarioPersisteLaContrasenaCifradaYAsignacionCliente() {
        ClienteEntity cliente = ClienteEntity.builder()
                .id(7L)
                .correoElectronico("cliente@example.com")
                .activo(true)
                .build();
        UsuarioAgregarRequestDto request = new UsuarioAgregarRequestDto(7L, TestCredentials.validPass());
        UsuarioAccesoEntity usuarioGuardado = UsuarioAccesoEntity.builder()
                .id(12L)
                .cliente(cliente)
                .username("cliente@example.com")
                .passwordHash(TestCredentials.testPassHash())
                .rol("CLIENTE")
                .activo(true)
                .build();

        when(clienteRepository.findById(7L)).thenReturn(Optional.of(cliente));
        when(usuarioRepository.existsByClienteId(7L)).thenReturn(false);
        when(usuarioRepository.existsByUsername("cliente@example.com")).thenReturn(false);
        when(passwordEncoder.encode(TestCredentials.validPass())).thenReturn(TestCredentials.testPassHash());
        when(usuarioRepository.save(any(UsuarioAccesoEntity.class))).thenReturn(usuarioGuardado);

        UsuarioAccesoDto response = usuarioService.agregarUsuario(request);

        assertEquals("cliente@example.com", response.getUsername());
        assertEquals(true, response.getActivo());
        verify(passwordEncoder).encode(TestCredentials.validPass());
        verify(usuarioRepository).save(any(UsuarioAccesoEntity.class));
    }

    @Test
    @DisplayName("buscarUsuariosPaginados - Retorna lista paginada")
    void buscarUsuariosPaginadosRetornaPagina() {
        UsuarioAccesoEntity usuario = UsuarioAccesoEntity.builder()
                .id(1L)
                .username("juan@example.com")
                .activo(true)
                .build();
        org.springframework.data.domain.Page<UsuarioAccesoEntity> page = new org.springframework.data.domain.PageImpl<>(List.of(usuario));
        org.mockito.ArgumentCaptor<Specification<UsuarioAccesoEntity>> captor = org.mockito.ArgumentCaptor.forClass(Specification.class);
        when(usuarioRepository.findAll(captor.capture(), any(Pageable.class))).thenReturn(page);

        UsuarioConsultaFiltroRecord filtro = new UsuarioConsultaFiltroRecord("juan", true, 0, 10, "id", "ASC", null);
        PaginaResponseRecord<UsuarioAccesoDto> resultado = usuarioService.buscarUsuariosPaginados(filtro);

        assertEquals(1, resultado.contenido().size());
        assertEquals("juan@example.com", resultado.contenido().get(0).getUsername());

        Specification<UsuarioAccesoEntity> spec = captor.getValue();
        jakarta.persistence.criteria.Root<UsuarioAccesoEntity> root = org.mockito.Mockito.mock(jakarta.persistence.criteria.Root.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
        jakarta.persistence.criteria.CriteriaQuery<?> query = org.mockito.Mockito.mock(jakarta.persistence.criteria.CriteriaQuery.class);
        jakarta.persistence.criteria.CriteriaBuilder cb = org.mockito.Mockito.mock(jakarta.persistence.criteria.CriteriaBuilder.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);

        spec.toPredicate(root, query, cb);
    }

    @Test
    @DisplayName("filtrarUsuarios - Retorna lista de DTOs")
    void filtrarUsuariosRetornaLista() {
        UsuarioAccesoEntity usuario = UsuarioAccesoEntity.builder()
                .id(2L)
                .username("filtro@example.com")
                .activo(true)
                .build();
        org.springframework.data.domain.Page<UsuarioAccesoEntity> page = new org.springframework.data.domain.PageImpl<>(List.of(usuario));
        when(usuarioRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        List<UsuarioAccesoDto> resultado = usuarioService.filtrarUsuarios("filtro", true);

        assertEquals(1, resultado.size());
        assertEquals("filtro@example.com", resultado.get(0).getUsername());
    }

    @Test
    @DisplayName("agregarUsuario - Lanza ClienteNoEncontradoException")
    void agregarUsuarioLanzaClienteNoEncontrado() {
        UsuarioAgregarRequestDto request = new UsuarioAgregarRequestDto(999L, TestCredentials.validPass());
        when(clienteRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ClienteNoEncontradoException.class, () -> usuarioService.agregarUsuario(request));
    }

    @Test
    @DisplayName("agregarUsuario - Lanza ReglaNegocioException si cliente está inactivo")
    void agregarUsuarioLanzaExcepcionClienteInactivo() {
        ClienteEntity clienteInactivo = ClienteEntity.builder().id(5L).activo(false).build();
        UsuarioAgregarRequestDto request = new UsuarioAgregarRequestDto(5L, TestCredentials.validPass());
        when(clienteRepository.findById(5L)).thenReturn(Optional.of(clienteInactivo));

        assertThrows(ReglaNegocioException.class, () -> usuarioService.agregarUsuario(request));
    }

    @Test
    @DisplayName("agregarUsuario - Lanza CorreoDuplicadoException si ya tiene usuario")
    void agregarUsuarioLanzaCorreoDuplicado() {
        ClienteEntity cliente = ClienteEntity.builder()
                .id(8L)
                .correoElectronico("existente@example.com")
                .activo(true)
                .build();
        UsuarioAgregarRequestDto request = new UsuarioAgregarRequestDto(8L, TestCredentials.validPass());
        when(clienteRepository.findById(8L)).thenReturn(Optional.of(cliente));
        when(usuarioRepository.existsByClienteId(8L)).thenReturn(true);

        assertThrows(CorreoDuplicadoException.class, () -> usuarioService.agregarUsuario(request));
    }

    @Test
    @DisplayName("reactivarUsuario - Lanza ReglaNegocioException si ya está activo")
    void reactivarUsuarioYaActivoLanzaExcepcionNegocio() {
        UsuarioEstadoRequestDto request = new UsuarioEstadoRequestDto();
        request.setId(11L);
        UsuarioAccesoEntity usuario = UsuarioAccesoEntity.builder().id(11L).activo(true).build();
        when(usuarioRepository.findById(11L)).thenReturn(Optional.of(usuario));

        assertThrows(ReglaNegocioException.class, () -> usuarioService.reactivarUsuario(request));
    }

    @Test
    @DisplayName("actualizarUsuario - Lanza UsuarioYaExisteException si nuevo username ya existe")
    void actualizarUsuarioUsernameDuplicado() {
        UsuarioActualizacionRequestDto request = new UsuarioActualizacionRequestDto();
        request.setId(30L);
        request.setUsername("duplicado@example.com");

        UsuarioAccesoEntity usuario = UsuarioAccesoEntity.builder().id(30L).username("original@example.com").build();
        when(usuarioRepository.findById(30L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.existsByUsername("duplicado@example.com")).thenReturn(true);

        assertThrows(UsuarioYaExisteException.class, () -> usuarioService.actualizarUsuario(request));
    }

    }
