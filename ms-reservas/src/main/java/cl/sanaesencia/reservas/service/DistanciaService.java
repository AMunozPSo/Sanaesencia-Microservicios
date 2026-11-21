package cl.sanaesencia.reservas.service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import static cl.sanaesencia.reservas.service.ReglasAgenda.error;
@Service
public class DistanciaService {
    private final double latitud,longitud,umbral;
    public DistanciaService(@Value("${centro.latitud}") double latitud,
            @Value("${centro.longitud}") double longitud,
            @Value("${centro.umbral-online-km}") double umbral) {
        this.latitud=latitud; this.longitud=longitud; this.umbral=umbral;
    }
    public Double calcular(Double lat,Double lon) {
        if(lat==null&&lon==null)return null;
        if(lat==null||lon==null||!Double.isFinite(lat)||!Double.isFinite(lon)
                ||Math.abs(lat)>90||Math.abs(lon)>180)
            throw error(HttpStatus.BAD_REQUEST,"Coordenadas inválidas");
        double dLat=Math.toRadians(lat-latitud),dLon=Math.toRadians(lon-longitud);
        double a=Math.pow(Math.sin(dLat/2),2)+Math.cos(Math.toRadians(latitud))
            *Math.cos(Math.toRadians(lat))*Math.pow(Math.sin(dLon/2),2);
        return 6371.0088*2*Math.atan2(Math.sqrt(Math.min(1,a)),Math.sqrt(Math.max(0,1-a)));
    }
    public double umbral() { return umbral; }
}
