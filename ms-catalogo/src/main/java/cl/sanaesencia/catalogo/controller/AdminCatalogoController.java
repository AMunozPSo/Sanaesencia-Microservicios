package cl.sanaesencia.catalogo.controller;

import cl.sanaesencia.catalogo.dto.*;
import cl.sanaesencia.catalogo.service.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/admin/catalogo")
public class AdminCatalogoController {
    private final ServicioTerapiaService servicios;
    private final EspecialistaService especialistas;
    private final ImportacionCatalogoService importacion;
    public AdminCatalogoController(ServicioTerapiaService servicios, EspecialistaService especialistas,
                                  ImportacionCatalogoService importacion) {
        this.servicios = servicios;
        this.especialistas = especialistas;
        this.importacion = importacion;
    }

    @GetMapping("/servicios")
    public List<ServicioTerapiaResponse> listarServicios() { return servicios.listarTodos(); }

    @GetMapping("/especialistas")
    public List<EspecialistaResponse> listarEspecialistas() { return especialistas.listarTodos(); }

    @PostMapping("/importar-centro")
    public ImportacionCatalogoService.Resultado importarCentro() { return importacion.importarCentro(); }
}
