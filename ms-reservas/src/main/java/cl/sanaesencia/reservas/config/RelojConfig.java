package cl.sanaesencia.reservas.config;
import java.time.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
@Configuration
public class RelojConfig {
    @Bean Clock reloj(@Value("${centro.zona}") String zona) { return Clock.system(ZoneId.of(zona)); }
}
