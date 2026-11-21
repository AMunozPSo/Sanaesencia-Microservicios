package cl.sanaesencia.catalogo.service;

import cl.sanaesencia.catalogo.dto.*;
import cl.sanaesencia.catalogo.model.*;
import cl.sanaesencia.catalogo.repository.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@Validated
public class EspecialistaService {
    private final EspecialistaRepository repositorio;
    private final ServicioTerapiaRepository servicios;

    public EspecialistaService(EspecialistaRepository repositorio, ServicioTerapiaRepository servicios) {
        this.repositorio = repositorio;
        this.servicios = servicios;
    }

    @Transactional
    public EspecialistaResponse registrar(@NotNull @Valid EspecialistaRequest solicitud) {
        Especialista especialista = new Especialista();
        aplicar(especialista, solicitud);
        return guardar(especialista);
    }

    @Transactional
    public EspecialistaResponse actualizar(Long id, @NotNull @Valid EspecialistaRequest solicitud) {
        Especialista especialista = buscar(id);
        aplicar(especialista, solicitud);
        return guardar(especialista);
    }

    private void aplicar(Especialista especialista, EspecialistaRequest solicitud) {
        if (!solicitud.permitePresencial() && !solicitud.permiteOnline()) {
            throw error(HttpStatus.BAD_REQUEST, "Selecciona al menos una modalidad de atención");
        }
        String nombre = solicitud.nombre().strip();
        boolean duplicado = especialista.getId() == null
                ? repositorio.findByNombreIgnoreCase(nombre).isPresent()
                : repositorio.existsByNombreIgnoreCaseAndIdNot(nombre, especialista.getId());
        if (duplicado) throw error(HttpStatus.CONFLICT, "Ya existe un especialista con ese nombre");

        Set<ServicioTerapia> seleccionados = new LinkedHashSet<>(
                servicios.findAllById(solicitud.servicioIds()));
        if (seleccionados.size() != solicitud.servicioIds().size()) {
            throw error(HttpStatus.NOT_FOUND, "Uno o más servicios no existen");
        }
        if (seleccionados.stream().anyMatch(s -> !s.isActivo())) {
            throw error(HttpStatus.CONFLICT, "No puedes asignar servicios desactivados");
        }
        if (!solicitud.permitePresencial()
                && seleccionados.stream().anyMatch(s -> !s.isPermiteOnline())) {
            throw error(HttpStatus.BAD_REQUEST,
                    "Un especialista solo online requiere servicios que permitan atención online");
        }
        especialista.setNombre(nombre);
        especialista.setEspecialidad(solicitud.especialidad().strip());
        especialista.setDescripcion(solicitud.descripcion().strip());
        especialista.setPermitePresencial(solicitud.permitePresencial());
        especialista.setPermiteOnline(solicitud.permiteOnline());
        especialista.setServicios(seleccionados);
    }

    private EspecialistaResponse guardar(Especialista especialista) {
        try {
            return EspecialistaResponse.desde(repositorio.saveAndFlush(especialista));
        } catch (DataIntegrityViolationException error) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se pudo guardar: el nombre del especialista ya existe", error);
        }
    }

    @Transactional(readOnly = true)
    public List<EspecialistaResponse> listarActivos(Long servicioId) {
        if (servicioId != null && !servicios.existsById(servicioId)) {
            throw error(HttpStatus.NOT_FOUND, "Servicio no encontrado");
        }
        List<Especialista> resultado = servicioId == null
                ? repositorio.findByActivoTrueOrderByNombreAsc()
                : repositorio.buscarPorServicio(servicioId);
        return resultado.stream().map(EspecialistaResponse::desde).toList();
    }

    @Transactional(readOnly = true)
    public List<EspecialistaResponse> listarTodos() {
        return repositorio.findAllByOrderByNombreAsc().stream().map(EspecialistaResponse::desde).toList();
    }

    @Transactional(readOnly = true)
    public EspecialistaResponse obtenerPorId(Long id) {
        return EspecialistaResponse.desde(buscar(id));
    }

    @Transactional
    public EspecialistaResponse cambiarEstado(Long id, boolean activo) {
        Especialista especialista = buscar(id);
        if (activo && especialista.getServicios().stream().noneMatch(s -> s.isActivo()
                && (especialista.isPermitePresencial()
                || (especialista.isPermiteOnline() && s.isPermiteOnline())))) {
            throw error(HttpStatus.CONFLICT, "El especialista requiere al menos un servicio activo compatible");
        }
        especialista.setActivo(activo);
        return guardar(especialista);
    }

    @Transactional
    public void desactivar(Long id) {
        cambiarEstado(id, false);
    }

    private Especialista buscar(Long id) {
        return repositorio.findById(id).orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Especialista no encontrado"));
    }

    private ResponseStatusException error(HttpStatus estado, String mensaje) {
        return new ResponseStatusException(estado, mensaje);
    }
}
