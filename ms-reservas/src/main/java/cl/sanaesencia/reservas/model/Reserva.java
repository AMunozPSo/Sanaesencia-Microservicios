package cl.sanaesencia.reservas.model;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;
@Entity
@Table(name="reservas", indexes={
    @Index(name="idx_reserva_intervalo", columnList="estado,inicio,fin"),
    @Index(name="idx_reserva_paciente", columnList="pacienteId"),
    @Index(name="idx_reserva_especialista", columnList="especialistaId")
})
public class Reserva {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @Column(nullable=false) public Long pacienteId;
    @Column(nullable=false, length=220) public String pacienteNombre;
    @Column(nullable=false) public Long especialistaId;
    @Column(nullable=false, length=150) public String especialistaNombre;
    @Column(nullable=false) public Long servicioId;
    @Column(nullable=false, length=150) public String servicioNombre;
    @Column(nullable=false, precision=10, scale=2) public BigDecimal precio;
    @Column(nullable=false) public LocalDateTime inicio;
    @Column(nullable=false) public LocalDateTime fin;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=12) public Modalidad modalidad;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=12) public EstadoReserva estado=EstadoReserva.CONFIRMADA;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="box_id") public Box box;
    public Double distanciaKm;
    @Column(nullable=false) public boolean onlineDisponible;
    @Column(nullable=false) public LocalDateTime creadaEn;
}
