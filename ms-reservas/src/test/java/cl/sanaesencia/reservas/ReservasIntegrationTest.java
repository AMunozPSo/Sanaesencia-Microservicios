package cl.sanaesencia.reservas;

import cl.sanaesencia.reservas.client.*;
import cl.sanaesencia.reservas.dto.Cuenta;
import cl.sanaesencia.reservas.model.*;
import cl.sanaesencia.reservas.repository.*;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.*;
import org.springframework.http.*;
import org.springframework.security.authentication.*;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.*;
import org.springframework.transaction.annotation.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
@Import(ReservasIntegrationTest.RelojPruebas.class)
@Transactional
class ReservasIntegrationTest {
    @TestConfiguration static class RelojPruebas {
        @Bean @Primary Clock relojFijo() {
            return Clock.fixed(Instant.parse("2026-11-21T15:00:00Z"),ZoneId.of("America/Santiago"));
        }
    }
    @Autowired MockMvc mvc;
    @Autowired ReservaRepository reservas;
    @Autowired HorarioRepository horarios;
    @Autowired BoxRepository boxes;
    @MockitoBean UsuariosClient usuarios;
    @MockitoBean CatalogoClient catalogo;
    static final String FECHA="2026-11-23";
    static final String AGENDA="{\"horarios\":[{\"dia\":\"MONDAY\",\"desde\":\"09:00\",\"hasta\":\"18:00\"}]}";

    CatalogoClient.Perfil perfil(Long id,boolean presencial,boolean online,boolean servicioOnline) {
        return new CatalogoClient.Perfil(id,"Profesional "+id,true,presencial,online,
                List.of(new CatalogoClient.Servicio(100L,"Terapia",new BigDecimal("30000"),60,servicioOnline,true)));
    }
    @BeforeEach void preparar() {
        when(usuarios.validarCredenciales(anyString(),anyString())).thenAnswer(i->{
            String correo=i.getArgument(0);
            if(!"test".equals(i.getArgument(1))) throw new BadCredentialsException("Incorrecta");
            if(correo.equals("admin@test.cl"))return new Cuenta(10L,correo,"Admin","","ADMIN",null,true);
            if(correo.equals("esp@test.cl"))return new Cuenta(20L,correo,"Especialista","","ESPECIALISTA",101L,true);
            if(correo.equals("sinvinculo@test.cl"))return new Cuenta(21L,correo,"Sin vínculo","","ESPECIALISTA",null,true);
            if(correo.matches("p[1-4]@test.cl"))
                return new Cuenta(Long.parseLong(correo.substring(1,2)),correo,"Paciente",correo.substring(1,2),"PACIENTE",null,true);
            throw new BadCredentialsException("Incorrecta");
        });
        when(catalogo.perfil(anyLong())).thenAnswer(i->perfil(i.getArgument(0),true,true,true));
        when(catalogo.atencion(anyLong(),eq(100L))).thenAnswer(i-> {
            var p=perfil(i.getArgument(0),true,true,true);
            return new CatalogoClient.Atencion(p,p.servicios().get(0));
        });
    }
    ResultActions llamada(String correo,HttpMethod metodo,String ruta,String cuerpo) throws Exception {
        return mvc.perform(request(metodo,ruta).with(httpBasic(correo,"test"))
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }
    long id(ResultActions r) throws Exception {
        return ((Number)JsonPath.read(r.andReturn().getResponse().getContentAsString(),"$.id")).longValue();
    }
    String crearJson(long especialista,String hora,String modalidad) {
        return """
                {"servicioId":100,"especialistaId":%d,"inicio":"%sT%s:00","modalidad":"%s",
                 "latitud":-33.45,"longitud":-70.66}
                """.formatted(especialista,FECHA,hora,modalidad);
    }
    void agenda(long especialista) throws Exception {
        llamada("admin@test.cl",HttpMethod.PUT,"/api/agenda/"+especialista,AGENDA).andExpect(status().isOk());
    }
    long reservar(String correo,long especialista,String hora,String modalidad) throws Exception {
        return id(llamada(correo,HttpMethod.POST,"/api/reservas",crearJson(especialista,hora,modalidad))
                .andExpect(status().isCreated()));
    }

    @Test void estadoEsPublicoYBoxesRequierenIdentidad() throws Exception {
        mvc.perform(get("/api/estado")).andExpect(status().isOk());
        mvc.perform(get("/api/boxes")).andExpect(status().isUnauthorized());
        llamada("p1@test.cl",HttpMethod.GET,"/api/boxes","").andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test void reservaPresencialTieneBoxYDuracionDelCatalogo() throws Exception {
        agenda(101);
        long id=reservar("p1@test.cl",101,"09:00","PRESENCIAL");
        Reserva r=reservas.findById(id).orElseThrow();
        assertNotNull(r.box);
        assertEquals(1L,r.pacienteId);
        assertEquals(LocalDateTime.parse(FECHA+"T10:00"),r.fin);
        assertEquals(0,new BigDecimal("30000").compareTo(r.precio));
        llamada("p1@test.cl",HttpMethod.GET,"/api/reservas/"+id,"")
                .andExpect(jsonPath("$.boxNombre").exists()).andExpect(jsonPath("$.sugiereOnline").value(true));
    }

    @Test void onlineNoOcupaBoxNiFuerzaLaEleccionPorDistancia() throws Exception {
        agenda(101);
        long online=reservar("p1@test.cl",101,"09:00","ONLINE");
        assertNull(reservas.findById(online).orElseThrow().box);
        long presencial=reservar("p1@test.cl",101,"10:00","PRESENCIAL");
        assertNotNull(reservas.findById(presencial).orElseThrow().box);
    }

    @Test void rechazaCrucesDeEspecialistaYDePacienteAunqueCambieModalidad() throws Exception {
        agenda(101); agenda(102);
        reservar("p1@test.cl",101,"09:00","PRESENCIAL");
        llamada("p2@test.cl",HttpMethod.POST,"/api/reservas",crearJson(101,"09:30","ONLINE"))
                .andExpect(status().isConflict());
        llamada("p1@test.cl",HttpMethod.POST,"/api/reservas",crearJson(102,"09:00","ONLINE"))
                .andExpect(status().isConflict());
        reservar("p2@test.cl",101,"10:00","ONLINE");
        assertEquals(2,reservas.count());
    }

    @Test void capacidadDeBoxesYCancelacionLiberanRecursos() throws Exception {
        for(long e=101;e<=104;e++)agenda(e);
        List<Long> ids=new ArrayList<>();
        for(int i=1;i<=3;i++)ids.add(reservar("p"+i+"@test.cl",100+i,"09:00","PRESENCIAL"));
        Set<Long> asignados=new HashSet<>();
        ids.forEach(id->asignados.add(reservas.findById(id).orElseThrow().box.id));
        assertEquals(3,asignados.size());
        llamada("p4@test.cl",HttpMethod.POST,"/api/reservas",crearJson(104,"09:00","PRESENCIAL"))
                .andExpect(status().isConflict());
        Long liberado=reservas.findById(ids.get(0)).orElseThrow().box.id;
        llamada("p1@test.cl",HttpMethod.DELETE,"/api/reservas/"+ids.get(0),"")
                .andExpect(status().isNoContent());
        long nueva=reservar("p4@test.cl",104,"09:00","PRESENCIAL");
        assertEquals(liberado,reservas.findById(nueva).orElseThrow().box.id);
        assertEquals(EstadoReserva.CANCELADA,reservas.findById(ids.get(0)).orElseThrow().estado);
    }

    @Test void protegePrivacidadYVistaMensualDeEspecialista() throws Exception {
        agenda(101); agenda(102);
        long propia=reservar("p1@test.cl",101,"09:00","ONLINE");
        reservar("p2@test.cl",102,"09:00","ONLINE");
        llamada("p2@test.cl",HttpMethod.GET,"/api/reservas/"+propia,"").andExpect(status().isForbidden());
        llamada("p2@test.cl",HttpMethod.DELETE,"/api/reservas/"+propia,"").andExpect(status().isForbidden());
        llamada("p1@test.cl",HttpMethod.GET,"/api/reservas?mes=2026-11","")
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].pacienteId").value(1));
        llamada("esp@test.cl",HttpMethod.GET,"/api/reservas?mes=2026-11","")
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].especialistaId").value(101));
        llamada("admin@test.cl",HttpMethod.GET,"/api/reservas?mes=2026-11","")
                .andExpect(jsonPath("$.length()").value(2));
        llamada("sinvinculo@test.cl",HttpMethod.GET,"/api/reservas?mes=2026-11","")
                .andExpect(status().isForbidden());
    }

    @Test void noPermiteSuplantarPacienteNiReservarConRolAdmin() throws Exception {
        agenda(101);
        llamada("admin@test.cl",HttpMethod.POST,"/api/reservas",crearJson(101,"09:00","ONLINE"))
                .andExpect(status().isForbidden());
        llamada("esp@test.cl",HttpMethod.POST,"/api/reservas",crearJson(101,"09:00","ONLINE"))
                .andExpect(status().isForbidden());
    }

    @Test void reprogramaSinDuplicarYRevierteAnteConflicto() throws Exception {
        agenda(101);
        long r1=reservar("p1@test.cl",101,"09:00","PRESENCIAL");
        reservar("p2@test.cl",101,"10:00","ONLINE");
        llamada("p1@test.cl",HttpMethod.PUT,"/api/reservas/"+r1,crearJson(101,"10:00","ONLINE"))
                .andExpect(status().isConflict());
        assertEquals(LocalDateTime.parse(FECHA+"T09:00"),reservas.findById(r1).orElseThrow().inicio);
        llamada("p1@test.cl",HttpMethod.PUT,"/api/reservas/"+r1,crearJson(101,"11:00","ONLINE"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.boxId").isEmpty());
        assertEquals(2,reservas.count());
        llamada("esp@test.cl",HttpMethod.PUT,"/api/reservas/"+r1,crearJson(101,"12:00","ONLINE"))
                .andExpect(status().isForbidden());
    }

    @Test void disponibilidadSeActualizaYRespetaLaAgendaCompleta() throws Exception {
        agenda(101);
        String ruta="/api/disponibilidad?especialistaId=101&servicioId=100&fecha="+FECHA+"&modalidad=PRESENCIAL";
        llamada("p1@test.cl",HttpMethod.GET,ruta,"").andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(17));
        reservar("p1@test.cl",101,"09:00","PRESENCIAL");
        llamada("p2@test.cl",HttpMethod.GET,ruta,"").andExpect(jsonPath("$[0].inicio").value(FECHA+"T10:00:00"));
        llamada("p1@test.cl",HttpMethod.POST,"/api/reservas",crearJson(101,"17:30","ONLINE"))
                .andExpect(status().isConflict());
    }

    @Test void agendaSoloSeGestionaPorSuEspecialistaOAdmin() throws Exception {
        llamada("p1@test.cl",HttpMethod.PUT,"/api/agenda/101",AGENDA).andExpect(status().isForbidden());
        llamada("esp@test.cl",HttpMethod.PUT,"/api/agenda/102",AGENDA).andExpect(status().isForbidden());
        llamada("esp@test.cl",HttpMethod.PUT,"/api/agenda/101",AGENDA).andExpect(status().isOk());
        reservar("p1@test.cl",101,"09:00","ONLINE");
        llamada("admin@test.cl",HttpMethod.PUT,"/api/agenda/101",
                "{\"horarios\":[{\"dia\":\"MONDAY\",\"desde\":\"10:00\",\"hasta\":\"18:00\"}]}")
                .andExpect(status().isConflict());
        llamada("admin@test.cl",HttpMethod.PUT,"/api/agenda/101",
                "{\"horarios\":[{\"dia\":\"MONDAY\",\"desde\":\"09:00\",\"hasta\":\"12:00\"},{\"dia\":\"MONDAY\",\"desde\":\"11:00\",\"hasta\":\"18:00\"}]}")
                .andExpect(status().isBadRequest());
    }

    @Test void protegeBoxConReservasYPermiteGestionAdmin() throws Exception {
        agenda(101); long reserva=reservar("p1@test.cl",101,"09:00","PRESENCIAL");
        long box=reservas.findById(reserva).orElseThrow().box.id;
        llamada("admin@test.cl",HttpMethod.PATCH,"/api/admin/boxes/"+box+"/estado","{\"activo\":false}")
                .andExpect(status().isConflict());
        llamada("p1@test.cl",HttpMethod.PATCH,"/api/admin/boxes/"+box+"/estado","{\"activo\":false}")
                .andExpect(status().isForbidden());
        llamada("p1@test.cl",HttpMethod.DELETE,"/api/reservas/"+reserva,"").andExpect(status().isNoContent());
        llamada("admin@test.cl",HttpMethod.PATCH,"/api/admin/boxes/"+box+"/estado","{\"activo\":false}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.activo").value(false));
        llamada("admin@test.cl",HttpMethod.PATCH,"/api/admin/boxes/"+box+"/estado","{\"activo\":true}")
                .andExpect(status().isOk());
    }

    @Test void sugerenciaDe20KmNoObligaNiInventaUbicacion() throws Exception {
        String ruta="/api/ubicacion/sugerencia?especialistaId=101&servicioId=100";
        llamada("p1@test.cl",HttpMethod.GET,ruta+"&latitud=-33.45&longitud=-70.66","")
                .andExpect(jsonPath("$.sugiereOnline").value(true));
        llamada("p1@test.cl",HttpMethod.GET,ruta+"&latitud=-31.909825&longitud=-71.512501","")
                .andExpect(jsonPath("$.sugiereOnline").value(false)).andExpect(jsonPath("$.distanciaKm").value(0));
        llamada("p1@test.cl",HttpMethod.GET,ruta,"").andExpect(jsonPath("$.distanciaKm").isEmpty());
        llamada("p1@test.cl",HttpMethod.GET,ruta+"&latitud=999&longitud=0","").andExpect(status().isBadRequest());
    }

    @Test void rechazaModalidadesNoOfrecidas() throws Exception {
        var p=perfil(101L,false,true,true);
        when(catalogo.atencion(101L,100L)).thenReturn(new CatalogoClient.Atencion(p,p.servicios().get(0)));
        agenda(101);
        llamada("p1@test.cl",HttpMethod.POST,"/api/reservas",crearJson(101,"09:00","PRESENCIAL"))
                .andExpect(status().isBadRequest());
        var presencial=perfil(101L,true,false,false);
        when(catalogo.atencion(101L,100L)).thenReturn(new CatalogoClient.Atencion(presencial,presencial.servicios().get(0)));
        llamada("p1@test.cl",HttpMethod.POST,"/api/reservas",crearJson(101,"09:00","ONLINE"))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest @ValueSource(strings={"08:00","09:15","18:00"})
    void rechazaHorasFueraDeAgendaOGrilla(String hora) throws Exception {
        agenda(101);
        llamada("p1@test.cl",HttpMethod.POST,"/api/reservas",crearJson(101,hora,"ONLINE"))
                .andExpect(status().is4xxClientError());
        assertEquals(0,reservas.count());
    }

    @Test void validaEstadoYNoResucitaCanceladas() throws Exception {
        agenda(101);long id=reservar("p1@test.cl",101,"09:00","ONLINE");
        llamada("p1@test.cl",HttpMethod.PATCH,"/api/reservas/"+id+"/estado","{\"estado\":\"ATENDIDA\"}")
                .andExpect(status().isForbidden());
        llamada("esp@test.cl",HttpMethod.PATCH,"/api/reservas/"+id+"/estado","{\"estado\":\"ATENDIDA\"}")
                .andExpect(status().isConflict());
        llamada("p1@test.cl",HttpMethod.DELETE,"/api/reservas/"+id,"").andExpect(status().isNoContent());
        llamada("p1@test.cl",HttpMethod.PUT,"/api/reservas/"+id,crearJson(101,"10:00","ONLINE"))
                .andExpect(status().isConflict());
        llamada("p1@test.cl",HttpMethod.PATCH,"/api/reservas/"+id+"/estado","{\"estado\":\"CONFIRMADA\"}")
                .andExpect(status().isConflict());
    }

    @Test void erroresDeEntradaYDependenciasSonClaros() throws Exception {
        llamada("p1@test.cl",HttpMethod.POST,"/api/reservas","{}").andExpect(status().isBadRequest());
        llamada("p1@test.cl",HttpMethod.GET,"/api/reservas?mes=mal","").andExpect(status().isBadRequest());
        llamada("p1@test.cl",HttpMethod.GET,"/api/reservas/99999","").andExpect(status().isNotFound());
        doThrow(new AuthenticationServiceException("No disponible")).when(usuarios).validarCredenciales("p1@test.cl","test");
        llamada("p1@test.cl",HttpMethod.GET,"/api/reservas","").andExpect(status().isServiceUnavailable());
    }

    @ParameterizedTest @ValueSource(booleans={true,false})
    @Transactional(propagation=Propagation.NOT_SUPPORTED)
    void dosPeticionesSimultaneasNoDuplicanEspecialistaOBox(boolean mismoEspecialista) throws Exception {
        agenda(104); agenda(103);
        if(!mismoEspecialista) {
            var lista=boxes.findAll();
            for(int n=1;n<lista.size();n++)lista.get(n).activo=false;
            boxes.saveAllAndFlush(lista);
        }
        ExecutorService ejecutor=Executors.newFixedThreadPool(2);
        CountDownLatch inicio=new CountDownLatch(1);
        try {
            List<Future<Integer>> resultados=new ArrayList<>();
            for(int i=1;i<=2;i++) {
                final int paciente=i;
                resultados.add(ejecutor.submit(()->{
                    inicio.await();
                    return llamada("p"+paciente+"@test.cl",HttpMethod.POST,"/api/reservas",
                            crearJson(mismoEspecialista?104:102+paciente,"15:00","PRESENCIAL"))
                            .andReturn().getResponse().getStatus();
                }));
            }
            inicio.countDown();
            List<Integer> estados=new ArrayList<>();
            for(var futuro:resultados)estados.add(futuro.get(20,TimeUnit.SECONDS));
            Collections.sort(estados);
            assertEquals(List.of(201,409),estados);
        } finally {
            ejecutor.shutdownNow();
            reservas.deleteAll();
            horarios.deleteAll();
            var lista=boxes.findAll();
            lista.forEach(box->box.activo=true);
            boxes.saveAllAndFlush(lista);
        }
    }

    @Test void especialistaPuedeCompletarSesionTerminadaSinPermitirCancelarla() throws Exception {
        agenda(101);
        long id=reservar("p1@test.cl",101,"09:00","ONLINE");
        Reserva reserva=reservas.findById(id).orElseThrow();
        reserva.inicio=LocalDateTime.of(2026,11,21,9,0);
        reserva.fin=LocalDateTime.of(2026,11,21,10,0);
        reservas.saveAndFlush(reserva);
        llamada("p1@test.cl",HttpMethod.DELETE,"/api/reservas/"+id,"").andExpect(status().isConflict());
        llamada("esp@test.cl",HttpMethod.PATCH,"/api/reservas/"+id+"/estado","{\"estado\":\"ATENDIDA\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("ATENDIDA"));
        assertEquals(EstadoReserva.ATENDIDA,reservas.findById(id).orElseThrow().estado);
    }
}
