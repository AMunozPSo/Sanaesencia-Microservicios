package cl.sanaesencia.catalogo.seed;
import java.util.List;
/** Datos del catálogo Android consultados el 19/11/2026. No crea cuentas. */
public final class CatalogoCentroData {
private CatalogoCentroData() {}
public record Area(String clave, String nombre, boolean online) {}
public record Profesional(String nombre, String especialidad, List<String> areas, boolean pausado, boolean soloOnline) {}
public static final List<Area> AREAS = List.of(
new Area("medicina", "Medicina general y salud mental", true),
new Area("psicologiaInfantil", "Psicología infantojuvenil", true),
new Area("psicologiaAdultos", "Psicología de adolescentes y adultos", true),
new Area("ocupacional", "Terapia ocupacional", false),
new Area("fonoaudiologia", "Fonoaudiología", false),
new Area("nutricion", "Nutrición integral", true),
new Area("floral", "Terapia floral", true),
new Area("aromaterapia", "Aromaterapia", false),
new Area("integrativa", "Terapia integrativa", false),
new Area("acupuntura", "Acupuntura clínica", false),
new Area("quiropraxia", "Quiropraxia clínica", false),
new Area("matroneria", "Matronería y salud femenina", false)
);
public static final List<Profesional> PROFESIONALES = List.of(
new Profesional("Dra. Keilen Sánchez Ríos", "Medicina general y salud mental", List.of("medicina"), false, false),
new Profesional("Dra. Camila Tabilo", "Medicina general y salud mental", List.of("medicina"), false, true),
new Profesional("Samantha Bastías", "Psicología clínica y educativa", List.of("psicologiaInfantil"), false, false),
new Profesional("Sebastián Olivares", "Psicología clínica y educativa", List.of("psicologiaInfantil"), false, false),
new Profesional("Nataly Godoy", "Psicología y terapia breve infantil", List.of("psicologiaInfantil"), true, false),
new Profesional("Anabel Guerra", "Psicología cognitivo conductual", List.of("psicologiaInfantil"), true, false),
new Profesional("Kristell Pinilla", "Neuropsicología infantil", List.of("psicologiaInfantil"), true, false),
new Profesional("Yanira Ferreira", "Psicología y psicoanálisis relacional", List.of("psicologiaAdultos"), false, false),
new Profesional("María José Mendicoa", "Psicología y psicoanálisis", List.of("psicologiaAdultos"), false, false),
new Profesional("Camila Soto", "Psicología con enfoque inclusivo", List.of("psicologiaAdultos"), false, true),
new Profesional("Francisca Villarroel", "Psicología y journals terapéuticos", List.of("psicologiaAdultos"), false, false),
new Profesional("Janis Herrera", "Psicología integrativa", List.of("psicologiaAdultos"), false, false),
new Profesional("Francisca Tornel", "Psicología clínica integral", List.of("psicologiaAdultos"), true, false),
new Profesional("Paula Orellana", "Psicología cognitivo conductual", List.of("psicologiaAdultos"), true, false),
new Profesional("Jeaseth Alam", "Terapia ocupacional", List.of("ocupacional"), false, false),
new Profesional("Pamela Vergara", "Terapia ocupacional", List.of("ocupacional"), false, false),
new Profesional("Sebastián Acuña", "Terapia ocupacional", List.of("ocupacional"), false, false),
new Profesional("Yanari Ramírez", "Terapia ocupacional", List.of("ocupacional"), false, false),
new Profesional("Natalia Rojas", "Fonoaudiología infantojuvenil", List.of("fonoaudiologia"), false, false),
new Profesional("Macarena González", "Fonoaudiología infantil", List.of("fonoaudiologia"), false, false),
new Profesional("Natalia Ortiz", "Fonoaudiología", List.of("fonoaudiologia"), false, false),
new Profesional("Gabriela Muñoz", "Fonoaudiología infantojuvenil", List.of("fonoaudiologia"), true, false),
new Profesional("Camila Collao C.", "Fonoaudiología", List.of("fonoaudiologia"), true, false),
new Profesional("Catalina Cobs", "Nutrición de adultos", List.of("nutricion"), false, false),
new Profesional("Betzabé Codoceo", "Nutrición infantil y materna", List.of("nutricion"), false, false),
new Profesional("Carolina Vega B.", "Nutrición", List.of("nutricion"), false, false),
new Profesional("Maria Ignacia", "Terapia floral, aromaterapia e integrativa", List.of("floral", "aromaterapia", "integrativa"), false, false),
new Profesional("Byron Sandoval", "Acupuntura", List.of("acupuntura"), false, false),
new Profesional("Daniel Díaz", "Quiropraxia clínica", List.of("quiropraxia"), false, false),
new Profesional("Carmen Paz Astudillo", "Matronería", List.of("matroneria"), false, false)
);
}
