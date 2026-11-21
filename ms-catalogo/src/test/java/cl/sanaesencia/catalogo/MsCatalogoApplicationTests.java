package cl.sanaesencia.catalogo;

import cl.sanaesencia.catalogo.client.UsuariosClient;
import cl.sanaesencia.catalogo.dto.UsuarioAutenticadoResponse;
import cl.sanaesencia.catalogo.model.*;
import cl.sanaesencia.catalogo.repository.*;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MsCatalogoApplicationTests {
    @Autowired MockMvc mvc;
    @Autowired ServicioTerapiaRepository servicios;
    @Autowired EspecialistaRepository especialistas;
    @MockitoBean UsuariosClient usuariosClient;

    @BeforeEach
    void autenticarContraUsuariosSimulado() {
        when(usuariosClient.validarCredenciales(anyString(), anyString())).thenAnswer(invocacion -> {
            String correo = invocacion.getArgument(0);
            String password = invocacion.getArgument(1);
            if (!"clave-test".equals(password)) throw new BadCredentialsException("Credenciales incorrectas");
            if (correo.equals("admin@example.com"))
                return new UsuarioAutenticadoResponse(1L, correo, "ADMIN", true);
            if (correo.equals("paciente@example.com"))
                return new UsuarioAutenticadoResponse(2L, correo, "PACIENTE", true);
            throw new BadCredentialsException("Credenciales incorrectas");
        });
    }

    private String solicitudServicio(String nombre, boolean online) {
        return """
                {"nombre":"%s","descripcion":"Descripción de prueba","precio":30000,
                 "duracionMinutos":60,"permiteOnline":%s}
                """.formatted(nombre, online);
    }

    private ServicioTerapia crearServicio(String nombre, boolean online) {
        ServicioTerapia servicio = new ServicioTerapia();
        servicio.setNombre(nombre);
        servicio.setDescripcion("Descripción");
        servicio.setPrecio(new BigDecimal("30000"));
        servicio.setDuracionMinutos(60);
        servicio.setPermiteOnline(online);
        return servicios.saveAndFlush(servicio);
    }

    private String solicitudEspecialista(String nombre, Long servicioId, boolean presencial, boolean online) {
        return """
                {"nombre":"%s","especialidad":"Psicología","descripcion":"",
                 "permitePresencial":%s,"permiteOnline":%s,"servicioIds":[%d]}
                """.formatted(nombre, presencial, online, servicioId);
    }

    private ResultActions admin(HttpMethod metodo, String ruta, String cuerpo) throws Exception {
        return mvc.perform(request(metodo, ruta)
                .with(httpBasic("admin@example.com", "clave-test"))
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    private long id(ResultActions resultado) throws Exception {
        return ((Number) JsonPath.read(resultado.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
    }

    @Test void listadoPublicoNoNecesitaUsuarios() throws Exception {
        mvc.perform(get("/api/servicios")).andExpect(status().isOk()).andExpect(content().json("[]"));
        verifyNoInteractions(usuariosClient);
    }

    @Test void cicloCompletoDeServicioConservaRegistro() throws Exception {
        long id = id(admin(HttpMethod.POST, "/api/servicios", solicitudServicio("Terapia", true))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.precio").value(30000)));
        admin(HttpMethod.PUT, "/api/servicios/" + id, solicitudServicio("Terapia actualizada", true))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value("Terapia actualizada"));
        admin(HttpMethod.DELETE, "/api/servicios/" + id, "").andExpect(status().isNoContent());
        assertTrue(servicios.existsById(id));
        assertFalse(servicios.findById(id).orElseThrow().isActivo());
        mvc.perform(get("/api/servicios")).andExpect(content().json("[]"));
        mvc.perform(get("/api/servicios/" + id)).andExpect(jsonPath("$.activo").value(false));
        admin(HttpMethod.PATCH, "/api/servicios/" + id + "/estado", "{\"activo\":true}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.activo").value(true));
    }

    @Test void rechazaDuplicadosAlCrearYActualizar() throws Exception {
        ServicioTerapia primero = crearServicio("Primero", true);
        crearServicio("Segundo", true);
        admin(HttpMethod.POST, "/api/servicios", solicitudServicio("primero", true))
                .andExpect(status().isConflict());
        admin(HttpMethod.PUT, "/api/servicios/" + primero.getId(), solicitudServicio("Segundo", true))
                .andExpect(status().isConflict());
        assertEquals("Primero", servicios.findById(primero.getId()).orElseThrow().getNombre());
    }

    @ParameterizedTest
    @CsvSource({"GET,/api/servicios/999999", "PUT,/api/servicios/999999",
            "DELETE,/api/servicios/999999", "PATCH,/api/servicios/999999/estado"})
    void serviciosInexistentesDevuelven404(String metodo, String ruta) throws Exception {
        String cuerpo = metodo.equals("PATCH") ? "{\"activo\":true}" : solicitudServicio("No existe", true);
        admin(HttpMethod.valueOf(metodo), ruta, cuerpo).andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @CsvSource({"POST,/api/servicios", "PUT,/api/servicios/1", "DELETE,/api/servicios/1",
            "PATCH,/api/servicios/1/estado", "POST,/api/especialistas",
            "PUT,/api/especialistas/1", "DELETE,/api/especialistas/1",
            "PATCH,/api/especialistas/1/estado", "POST,/api/admin/catalogo/importar-centro",
            "GET,/api/admin/catalogo/especialistas", "GET,/api/admin/catalogo/servicios"})
    void escrituraYAdministracionRequierenAdmin(String metodo, String ruta) throws Exception {
        mvc.perform(request(HttpMethod.valueOf(metodo), ruta).contentType(MediaType.APPLICATION_JSON)
                .content("{}")).andExpect(status().isUnauthorized());
        mvc.perform(request(HttpMethod.valueOf(metodo), ruta)
                .with(httpBasic("paciente@example.com", "clave-test"))
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"nombre\":\"\",\"descripcion\":\"D\",\"precio\":30000,\"duracionMinutos\":60,\"permiteOnline\":true}",
            "{\"nombre\":\"A\",\"descripcion\":\"D\",\"precio\":-1,\"duracionMinutos\":60,\"permiteOnline\":true}",
            "{\"nombre\":\"A\",\"descripcion\":\"D\",\"precio\":30000.123,\"duracionMinutos\":60,\"permiteOnline\":true}",
            "{\"nombre\":\"A\",\"descripcion\":\"D\",\"precio\":30000,\"duracionMinutos\":0,\"permiteOnline\":true}",
            "{\"nombre\":\"A\",\"descripcion\":\"D\",\"precio\":30000,\"duracionMinutos\":60}",
            "{\"nombre\":"
    })
    void rechazaDatosInvalidosSinGuardar(String cuerpo) throws Exception {
        admin(HttpMethod.POST, "/api/servicios", cuerpo).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value(400));
        assertEquals(0, servicios.count());
    }

    @Test void distingueFalloDeCredencialesDeCaidaDeUsuarios() throws Exception {
        mvc.perform(post("/api/servicios").with(httpBasic("admin@example.com", "incorrecta"))
                .contentType(MediaType.APPLICATION_JSON).content(solicitudServicio("A", true)))
                .andExpect(status().isUnauthorized());
        doThrow(new AuthenticationServiceException("Servicio caído")).when(usuariosClient)
                .validarCredenciales("admin@example.com", "clave-test");
        admin(HttpMethod.POST, "/api/servicios", solicitudServicio("A", true))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.estado").value(503));
        assertEquals(0, servicios.count());
    }

    @Test void rechazaRespuestaDeUsuariosConRolOIdentidadIncorrecta() throws Exception {
        when(usuariosClient.validarCredenciales("admin@example.com", "clave-test"))
                .thenReturn(new UsuarioAutenticadoResponse(1L, "otra@example.com", "ADMIN", true));
        admin(HttpMethod.POST, "/api/servicios", solicitudServicio("A", true)).andExpect(status().isUnauthorized());
        when(usuariosClient.validarCredenciales("admin@example.com", "clave-test"))
                .thenReturn(new UsuarioAutenticadoResponse(1L, "admin@example.com", "SUPERADMIN", true));
        admin(HttpMethod.POST, "/api/servicios", solicitudServicio("A", true)).andExpect(status().isUnauthorized());
        when(usuariosClient.validarCredenciales("admin@example.com", "clave-test"))
                .thenReturn(new UsuarioAutenticadoResponse(1L, "admin@example.com", "ADMIN", false));
        admin(HttpMethod.POST, "/api/servicios", solicitudServicio("A", true)).andExpect(status().isUnauthorized());
    }

    @Test void cicloCompletoDeEspecialistaConFiltroYBajaLogica() throws Exception {
        ServicioTerapia servicio = crearServicio("Psicología", true);
        long id = id(admin(HttpMethod.POST, "/api/especialistas",
                solicitudEspecialista("Especialista", servicio.getId(), true, true))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.servicios[0].id").value(servicio.getId())));
        mvc.perform(get("/api/especialistas").param("servicioId", servicio.getId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(id));
        admin(HttpMethod.PUT, "/api/especialistas/" + id,
                solicitudEspecialista("Nombre actualizado", servicio.getId(), false, true))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value("Nombre actualizado"));
        admin(HttpMethod.DELETE, "/api/especialistas/" + id, "").andExpect(status().isNoContent());
        mvc.perform(get("/api/especialistas")).andExpect(content().json("[]"));
        assertTrue(especialistas.existsById(id));
        admin(HttpMethod.GET, "/api/admin/catalogo/especialistas", "")
                .andExpect(jsonPath("$[0].activo").value(false));
        admin(HttpMethod.PATCH, "/api/especialistas/" + id + "/estado", "{\"activo\":true}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.activo").value(true));
    }

    @Test void validaRelacionesYModalidadesDeEspecialistas() throws Exception {
        ServicioTerapia presencial = crearServicio("Presencial", false);
        admin(HttpMethod.POST, "/api/especialistas", solicitudEspecialista("Sin modalidad",
                presencial.getId(), false, false)).andExpect(status().isBadRequest());
        admin(HttpMethod.POST, "/api/especialistas", solicitudEspecialista("Solo online",
                presencial.getId(), false, true)).andExpect(status().isBadRequest());
        admin(HttpMethod.POST, "/api/especialistas", solicitudEspecialista("Sin servicio",
                999999L, true, false)).andExpect(status().isNotFound());
        presencial.setActivo(false);
        servicios.saveAndFlush(presencial);
        admin(HttpMethod.POST, "/api/especialistas", solicitudEspecialista("Servicio inactivo",
                presencial.getId(), true, false)).andExpect(status().isConflict());
        assertEquals(0, especialistas.count());
    }

    @Test void especialistaRequiereDatosCompletos() throws Exception {
        admin(HttpMethod.POST, "/api/especialistas", "{}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.nombre").exists())
                .andExpect(jsonPath("$.errores.servicioIds").exists());
    }

    @Test void noPermiteQuitarOnlineSiUnEspecialistaDependeDeEllo() throws Exception {
        ServicioTerapia servicio = crearServicio("Online", true);
        admin(HttpMethod.POST, "/api/especialistas",
                solicitudEspecialista("Online profesional", servicio.getId(), false, true))
                .andExpect(status().isCreated());
        admin(HttpMethod.PUT, "/api/servicios/" + servicio.getId(), solicitudServicio("Online", false))
                .andExpect(status().isConflict());
        assertTrue(servicios.findById(servicio.getId()).orElseThrow().isPermiteOnline());
    }

    @Test void noReactivaEspecialistaConServiciosDesactivados() throws Exception {
        ServicioTerapia servicio = crearServicio("Atención", true);
        long id = id(admin(HttpMethod.POST, "/api/especialistas",
                solicitudEspecialista("Profesional", servicio.getId(), true, true)));
        admin(HttpMethod.DELETE, "/api/especialistas/" + id, "").andExpect(status().isNoContent());
        admin(HttpMethod.DELETE, "/api/servicios/" + servicio.getId(), "").andExpect(status().isNoContent());
        admin(HttpMethod.PATCH, "/api/especialistas/" + id + "/estado", "{\"activo\":true}")
                .andExpect(status().isConflict());
        mvc.perform(get("/api/especialistas").param("servicioId", servicio.getId().toString()))
                .andExpect(content().json("[]"));
    }

    @ParameterizedTest
    @CsvSource({"GET,/api/especialistas/999999", "PUT,/api/especialistas/999999",
            "DELETE,/api/especialistas/999999", "PATCH,/api/especialistas/999999/estado"})
    void especialistaInexistenteDevuelve404(String metodo, String ruta) throws Exception {
        String cuerpo = metodo.equals("PATCH") ? "{\"activo\":true}"
                : solicitudEspecialista("No existe", 999999L, true, true);
        admin(HttpMethod.valueOf(metodo), ruta, cuerpo).andExpect(status().isNotFound());
    }

    @Test void importacionEsIdempotenteYConservaEdiciones() throws Exception {
        admin(HttpMethod.POST, "/api/admin/catalogo/importar-centro", "")
                .andExpect(status().isOk()).andExpect(jsonPath("$.serviciosCreados").value(12))
                .andExpect(jsonPath("$.especialistasCreados").value(30));
        assertEquals(23, especialistas.findByActivoTrueOrderByNombreAsc().size());
        Especialista pausado = especialistas.findByNombreIgnoreCase("Nataly Godoy").orElseThrow();
        assertFalse(pausado.isActivo());
        Especialista online = especialistas.findByNombreIgnoreCase("Dra. Camila Tabilo").orElseThrow();
        assertFalse(online.isPermitePresencial());
        assertTrue(online.isPermiteOnline());
        ServicioTerapia editado = servicios.findAll().get(0);
        editado.setPrecio(new BigDecimal("35000"));
        servicios.saveAndFlush(editado);
        admin(HttpMethod.POST, "/api/admin/catalogo/importar-centro", "")
                .andExpect(jsonPath("$.serviciosCreados").value(0))
                .andExpect(jsonPath("$.especialistasCreados").value(0));
        assertEquals(12, servicios.count());
        assertEquals(30, especialistas.count());
        assertEquals(0, new BigDecimal("35000").compareTo(servicios.findById(editado.getId()).orElseThrow().getPrecio()));
    }

    @Test void parametrosInvalidosYEstadoAusenteDevuelven400() throws Exception {
        mvc.perform(get("/api/servicios/no-es-numero")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value(400));
        admin(HttpMethod.PATCH, "/api/servicios/1/estado", "{}").andExpect(status().isBadRequest());
        mvc.perform(get("/api/especialistas").param("servicioId", "999999"))
                .andExpect(status().isNotFound());
    }
}
