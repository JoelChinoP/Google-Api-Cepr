import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Temas {
    private final Sheets SERVICESH;
    private final Drive SERVICEDR;
    private final String RANGE;

    public Temas ()
            throws GeneralSecurityException, IOException {
        this.SERVICESH = ApiAuth.getSheetsService();
        this.SERVICEDR = ApiAuth.getDriveService();
        this.RANGE = "INGENIERÍAS!C3:P3";
    }


    void pasarTemas (String sheetId)
            throws IOException {
        Map<String, Integer> cursos = new HashMap<String, Integer>() {{
            put("BIOLOGIA", 0);
            put("CIVICA", 1);
            put("QUIMICA", 2);
            put("LITERATURA", 3);
            put("PSICOLOGIA", 4);
            put("MATEMATICA", 5);
            put("GEOGRAFIA Y ECONOMIA", 6);
            put("FISICA", 7);
            put("FILOSOFIA", 8);
            put("LENGUAJE", 9);
            put("INGLES", 10);
            put("HISTORIA", 11);
            put("RAZ MATEMATICO", 12);
            put("RAZ VERBAL", 13);
        }};


        //String nombreParte = obtenerFechaActual("dd-MM-yy EEEE");
        String nombreParte = obtenerFecha(
                "13-01-2024",
                "dd-MM-yyyy",
                "dd-MM-yyyy EEEE");

        File file = buscarPorNombre(nombreParte);
        List<List<Object>> celdasParte = obtenerValores(file.getId(), "PARTE!E11:H22");
        List<List<Object>> celdasTema = obtenerValores(sheetId);

        for (int i=0; i<celdasParte.size(); i+=2) {
            String tema = celdasParte.get(i).get(3).toString();
            String temaSig = celdasParte.get(i+1).get(3).toString();
            String curso = celdasParte.get(i).get(0).toString();

            if (!tema.equals(temaSig))
                tema += " , " + temaSig;

            pasarTema(celdasTema, tema, cursos.get(curso));
        }

        actualizarValores(sheetId, celdasTema);
        pasarTemasPortapapeles(celdasTema);

        //System.out.println(celdasParte);
        System.out.println("----");
        System.out.println(celdasTema);

    }


    void pasarTema (List<List<Object>> celdasTema, String tema, int column)
            throws IOException {
        String ant = celdasTema.get(0).get(column).toString();

        if (ant.length()>5)
            ant += " / ";

        String fecha = obtenerFechaActual("dd/MM/yyyy");

        // Modificar el valor en la posición específica utilizando set
        Object nuevoValor = ant + tema + " (" + fecha + ")";
        celdasTema.get(0).set(column, nuevoValor);

    }

    private void colocarFecha (List<List<Object>> celdasTema, int column) {

    }


    List<List<Object>> obtenerValores (String reporteId)
            throws IOException {
        return obtenerValores(reporteId, RANGE);
    }
    List<List<Object>> obtenerValores (String reporteId, String range)
            throws IOException {

        ValueRange response = SERVICESH.spreadsheets().values()
                .get(reporteId, range)
                .execute();
        return response.getValues();
    }


    void actualizarValores (String spreadsheetId, List<List<Object>> values)
            throws IOException {
        // Updates the values in the specified range.
        ValueRange body = new ValueRange()
                .setValues(values);

        SERVICESH.spreadsheets().values().update(spreadsheetId, RANGE, body)
                .setValueInputOption("RAW")
                .execute();
    }


    String obtenerFechaActual (String pattern) { //"EEEE dd/MM/yy"
        // Obtener la fecha actual
        LocalDate fechaActual = LocalDate.now();

        return obtenerFecha(fechaActual, pattern);
    }
    String obtenerFecha (String fechaString, String in, String out) { //in = "yyyy-MM-dd"
        // Parsear la fecha original
        DateTimeFormatter format = DateTimeFormatter.ofPattern(in);
        LocalDate fechaOriginal = LocalDate.parse(fechaString, format);

        return obtenerFecha(fechaOriginal, out);
    }
    private String obtenerFecha (LocalDate fecha, String pattern) {
        // Formatear la fecha en el patrón deseado
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        String fechaFormateada = fecha.format(formatter);

        return fechaFormateada.toUpperCase();
    }


    File buscarPorNombre(String fileName) throws IOException {

        String pageToken = null;
        do {
            FileList result = SERVICEDR.files().list()
                    .setQ("name='" + fileName + "'")
                    .setSpaces("drive")
                    .setFields("nextPageToken, files(id, name)")
                    .setPageToken(pageToken)
                    .execute();

            List<File> files = result.getFiles();
            if (files != null && !files.isEmpty()) {
                File foundFile = files.get(0);
                return foundFile; // Retorna el primer archivo encontrado
            }

            pageToken = result.getNextPageToken();
        } while (pageToken != null);

        return null; // Retorna null si no se encuentra ningún archivo
    }


    void pasarTemasPortapapeles (List<List<Object>> celdasTema) {
        String contenido = "";
        for (Object o: celdasTema.get(0)) {
            contenido += o.toString() + "\t";
        }
        copiarPortapapeles(contenido);
    }
    void copiarPortapapeles(String contenido) {
        StringSelection selection = new StringSelection(contenido);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, null);
    }



    public static void main (String[] args)
            throws GeneralSecurityException, IOException {

        Temas tm = new Temas();

        tm.pasarTemas("1uIYT5r4Ar1b3zZ1XYQMn2jTYMbNXfWz8p8m7Rgk-kSM");


    }
}
