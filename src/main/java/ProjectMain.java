import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class ProjectMain {

    public static void main (String[] args) {
        System.out.println("Holi mundo");
    }



    public static String getDateTime(int anio, int mes, int dia, int hora, int minuto) {
        LocalDateTime dateTime = LocalDateTime.of(anio, mes, dia, hora, minuto);
        LocalDateTime dateTimeConDiferencia = dateTime.plusHours(5);

        // Convertir a formato Zulú (UTC)
        String formattedDateTime = dateTimeConDiferencia
                .atOffset(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"));

        return formattedDateTime;
    }


    public static String getDateFormat (String pattern) {   //yyyy-MM-dd
        Date fechaActual = new Date();          //LocalDate fechaHoy = LocalDate.now();
        SimpleDateFormat formatoFecha = new SimpleDateFormat(pattern);  // Para el formato día-mes-año
        // Formatear la fecha actual con el formato deseado
        return formatoFecha.format(fechaActual);
    }


}