package cl.sanaesencia.usuarios.validation;

public final class ValidadorRun {

    private ValidadorRun() {
    }

    public static boolean esValido(String run) {

        if (run == null || !run.matches("^[0-9]{7,8}-[0-9kK]$")) {
            return false;
        }

        String[] partes = run.split("-");
        String numero = partes[0];

        if (Integer.parseInt(numero) == 0) {
            return false;
        }

        char verificadorRecibido =
                Character.toUpperCase(partes[1].charAt(0));

        int suma = 0;
        int multiplicador = 2;

        for (int i = numero.length() - 1; i >= 0; i--) {
            int digito = numero.charAt(i) - '0';

            suma += digito * multiplicador;

            multiplicador++;

            if (multiplicador > 7) {
                multiplicador = 2;
            }
        }

        int resultado = 11 - (suma % 11);

        char verificadorEsperado;

        if (resultado == 11) {
            verificadorEsperado = '0';
        } else if (resultado == 10) {
            verificadorEsperado = 'K';
        } else {
            verificadorEsperado = (char) ('0' + resultado);
        }

        return verificadorRecibido == verificadorEsperado;
    }
}
