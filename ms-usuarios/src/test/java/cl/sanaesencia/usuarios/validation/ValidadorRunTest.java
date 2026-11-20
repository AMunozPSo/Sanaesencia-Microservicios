package cl.sanaesencia.usuarios.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidadorRunTest {

    @Test
    void aceptaRunsConVerificadorCorrecto() {
        assertTrue(ValidadorRun.esValido("12345678-5"));
        assertTrue(ValidadorRun.esValido("1000005-K"));
        assertTrue(ValidadorRun.esValido("10000004-0"));
    }

    @Test
    void aceptaKMinuscula() {
        assertTrue(ValidadorRun.esValido("1000005-k"));
    }

    @Test
    void rechazaVerificadoresIncorrectos() {
        assertFalse(ValidadorRun.esValido("12345678-9"));
        assertFalse(ValidadorRun.esValido("1000005-0"));
    }

    @Test
    void rechazaFormatosIncorrectos() {
        assertFalse(ValidadorRun.esValido("123456785"));
        assertFalse(ValidadorRun.esValido("12.345.678-5"));
        assertFalse(ValidadorRun.esValido("123456789-5"));
        assertFalse(ValidadorRun.esValido("abcdefgh-K"));
        assertFalse(ValidadorRun.esValido("123456-0"));
    }

    @Test
    void rechazaValoresVacios() {
        assertFalse(ValidadorRun.esValido(null));
        assertFalse(ValidadorRun.esValido(""));
        assertFalse(ValidadorRun.esValido("   "));
    }

    @Test
    void rechazaNumeroCero() {
        assertFalse(ValidadorRun.esValido("00000000-0"));
        assertFalse(ValidadorRun.esValido("0000000-0"));
    }
}
