package agromedio.presentacion;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/** Formatos visibles de la aplicacion (Colombia): fechas y cantidades. */
public final class Formato {

    private static final Locale COLOMBIA = new Locale("es", "CO");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DecimalFormat NUMERO;

    static {
        DecimalFormatSymbols simbolos = DecimalFormatSymbols.getInstance(COLOMBIA);
        NUMERO = new DecimalFormat("#,##0.##", simbolos);
        NUMERO.setGroupingUsed(true);
    }

    private Formato() {}

    public static String numero(Number valor) {
        if (valor == null) {
            return "";
        }
        return NUMERO.format(valor.doubleValue());
    }

    public static String numero(double valor) {
        return NUMERO.format(valor);
    }

    public static String fecha(LocalDate valor) {
        return valor == null ? "" : FECHA.format(valor);
    }

    public static String fecha(String valor) {
        if (valor == null || valor.isBlank()) {
            return "";
        }
        try {
            return FECHA.format(LocalDate.parse(valor));
        } catch (DateTimeParseException ex) {
            return valor;
        }
    }

    public static double decimal(String texto) {
        if (texto == null || texto.isBlank()) {
            return 0.0;
        }
        // M-3: parseo inverse de numero() usando los mismos simbolos (es-CO),
        // de modo que "1.000" se lea como 1000 y no como 1,0.
        try {
            DecimalFormat df = new DecimalFormat("#,##0.##", DecimalFormatSymbols.getInstance(COLOMBIA));
            df.setParseBigDecimal(true);
            return df.parse(texto.trim()).doubleValue();
        } catch (ParseException e) {
            String limpio = texto.trim();
            if (limpio.contains(",")) {
                limpio = limpio.replace(".", "").replace(',', '.');
            }
            return Double.parseDouble(limpio);
        }
    }

    public static String estado(String codigo) {
        if (codigo == null) {
            return "";
        }
        return switch (codigo.trim().toUpperCase(Locale.ROOT)) {
            case "CARGADA" -> "Cargada";
            case "EN_COMPRA" -> "En compra";
            case "RECIBIENDO" -> "Recibiendo";
            case "ALISTANDO" -> "Alistando";
            case "LISTA" -> "Lista";
            case "COMPRADA" -> "Comprada";
            case "RECIBIDA" -> "Recibida";
            case "ALISTADA" -> "Alistada";
            case "DESPACHADA" -> "Despachada";
            case "CERRADA" -> "Cerrada";
            case "ANULADA" -> "Anulada";
            case "BORRADOR" -> "Borrador";
            case "CONFIRMADA" -> "Confirmada";
            default -> codigo;
        };
    }
}
