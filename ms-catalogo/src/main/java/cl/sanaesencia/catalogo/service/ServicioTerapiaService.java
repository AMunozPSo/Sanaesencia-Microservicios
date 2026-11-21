package cl.sanaesencia.catalogo.service;

import cl.sanaesencia.catalogo.dto.ServicioTerapiaRequest;
import cl.sanaesencia.catalogo.dto.ServicioTerapiaResponse;
import cl.sanaesencia.catalogo.model.ServicioTerapia;
import cl.sanaesencia.catalogo.repository.ServicioTerapiaRepository;
import cl.sanaesencia.catalogo.repository.EspecialistaRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Validated
public class ServicioTerapiaService {

    private final ServicioTerapiaRepository repositorio;
    private final EspecialistaRepository especialistas;

    public ServicioTerapiaService(
            ServicioTerapiaRepository repositorio,
            EspecialistaRepository especialistas
    ) {
        this.repositorio = repositorio;
        this.especialistas = especialistas;
    }

    @Transactional
    public ServicioTerapiaResponse registrar(
            @NotNull @Valid ServicioTerapiaRequest solicitud
    ) {
        String nombre = solicitud.nombre().strip();

        if (repositorio.existsByNombreIgnoreCase(nombre)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ya existe un servicio con ese nombre"
            );
        }

        ServicioTerapia servicio = new ServicioTerapia();

        servicio.setNombre(nombre);
        servicio.setDescripcion(
                solicitud.descripcion().strip()
        );
        servicio.setPrecio(solicitud.precio());
        servicio.setDuracionMinutos(
                solicitud.duracionMinutos()
        );
        servicio.setPermiteOnline(
                solicitud.permiteOnline()
        );

        try {
            ServicioTerapia guardado =
                    repositorio.saveAndFlush(servicio);

            return ServicioTerapiaResponse.desde(guardado);

        } catch (DataIntegrityViolationException error) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se pudo registrar el servicio: revisa si el nombre ya existe",
                    error
            );
        }
    }

    @Transactional(readOnly = true)
    public List<ServicioTerapiaResponse> listarActivos() {
        return repositorio.findByActivoTrue()
                .stream()
                .map(ServicioTerapiaResponse::desde)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ServicioTerapiaResponse> listarTodos() {
        return repositorio.findAll().stream()
                .map(ServicioTerapiaResponse::desde).toList();
    }

    @Transactional
    public ServicioTerapiaResponse cambiarEstado(Long id, boolean activo) {
        ServicioTerapia servicio = repositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Servicio no encontrado"));
        servicio.setActivo(activo);
        return ServicioTerapiaResponse.desde(repositorio.saveAndFlush(servicio));
    }

    @Transactional(readOnly = true)
    public ServicioTerapiaResponse obtenerPorId(Long id) {
        ServicioTerapia servicio = repositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Servicio no encontrado"
                ));

        return ServicioTerapiaResponse.desde(servicio);
    }
    @Transactional
    public ServicioTerapiaResponse actualizar(
            Long id,
            @NotNull @Valid ServicioTerapiaRequest solicitud
    ) {
        ServicioTerapia servicio = repositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Servicio no encontrado"
                ));

        String nombre = solicitud.nombre().strip();

        if (repositorio.existsByNombreIgnoreCaseAndIdNot(
                nombre,
                id
        )) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ya existe otro servicio con ese nombre"
            );
        }

        if (!solicitud.permiteOnline()
                && especialistas.existsByActivoTrueAndPermitePresencialFalseAndServicios_Id(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El servicio tiene especialistas activos que atienden solo online");
        }

        servicio.setNombre(nombre);
        servicio.setDescripcion(
                solicitud.descripcion().strip()
        );
        servicio.setPrecio(solicitud.precio());
        servicio.setDuracionMinutos(
                solicitud.duracionMinutos()
        );
        servicio.setPermiteOnline(
                solicitud.permiteOnline()
        );

        try {
            ServicioTerapia guardado =
                    repositorio.saveAndFlush(servicio);

            return ServicioTerapiaResponse.desde(guardado);

        } catch (DataIntegrityViolationException error) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se pudo actualizar: revisa si el nombre ya existe",
                    error
            );
        }
    }

    @Transactional
    public void desactivar(Long id) {
        ServicioTerapia servicio = repositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Servicio no encontrado"
                ));

        servicio.setActivo(false);

        repositorio.save(servicio);
    }
}
