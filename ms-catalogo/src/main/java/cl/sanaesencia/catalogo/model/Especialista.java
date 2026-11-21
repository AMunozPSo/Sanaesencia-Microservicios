package cl.sanaesencia.catalogo.model;

import jakarta.persistence.*;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "especialistas")
public class Especialista {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 150)
    private String nombre;
    @Column(nullable = false, length = 200)
    private String especialidad;
    @Column(nullable = false, length = 2000)
    private String descripcion = "";
    @Column(nullable = false)
    private boolean permitePresencial;
    @Column(nullable = false)
    private boolean permiteOnline;
    @Column(nullable = false)
    private boolean activo = true;
    @ManyToMany
    @JoinTable(name = "especialista_servicios",
            joinColumns = @JoinColumn(name = "especialista_id"),
            inverseJoinColumns = @JoinColumn(name = "servicio_id"))
    private Set<ServicioTerapia> servicios = new LinkedHashSet<>();

    public Especialista() {}
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getEspecialidad() { return especialidad; }
    public void setEspecialidad(String especialidad) { this.especialidad = especialidad; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public boolean isPermitePresencial() { return permitePresencial; }
    public void setPermitePresencial(boolean valor) { permitePresencial = valor; }
    public boolean isPermiteOnline() { return permiteOnline; }
    public void setPermiteOnline(boolean valor) { permiteOnline = valor; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public Set<ServicioTerapia> getServicios() { return servicios; }
    public void setServicios(Set<ServicioTerapia> servicios) {
        this.servicios = new LinkedHashSet<>(servicios);
    }
}
