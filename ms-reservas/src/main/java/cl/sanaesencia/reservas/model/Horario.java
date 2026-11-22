package cl.sanaesencia.reservas.model;
import jakarta.persistence.*;
import java.time.*;
@Entity @Table(name="horarios", indexes=@Index(name="idx_horario_especialista", columnList="especialistaId"))
public class Horario {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @Column(nullable=false) public Long especialistaId;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=12) public DayOfWeek dia;
    @Column(nullable=false) public LocalTime desde;
    @Column(nullable=false) public LocalTime hasta;
    public LocalDate fecha;
    @Column(nullable=false) public boolean cupoPublicado;
}
