package cl.sanaesencia.reservas.model;
import jakarta.persistence.*;
/** Fila única que serializa cambios de agenda y reserva en la base de datos. */
@Entity @Table(name="bloqueo_agenda")
public class BloqueoAgenda {
    @Id public Long id;
    public BloqueoAgenda() {}
    public BloqueoAgenda(Long id) { this.id=id; }
}
