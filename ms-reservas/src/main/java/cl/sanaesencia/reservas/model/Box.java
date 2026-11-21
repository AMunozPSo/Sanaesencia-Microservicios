package cl.sanaesencia.reservas.model;
import jakarta.persistence.*;
@Entity
@Table(name="boxes")
public class Box {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @Column(nullable=false, unique=true, length=60) public String nombre;
    @Column(nullable=false, length=300) public String descripcion = "";
    @Column(nullable=false) public boolean activo = true;
    public Box() {}
    public Box(String nombre) { this.nombre = nombre; }
}
