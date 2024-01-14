import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.drive.model.File;
import com.google.api.services.sheets.v4.model.ValueRange;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Asistencia {

    private final String SURNAMES_FILE_PATH;
    private final String RANGE;
    private final Sheets SERVICESH;
    private final Drive SERVICEDR;

    public Asistencia ()
            throws IOException, GeneralSecurityException {
        this.SURNAMES_FILE_PATH = "/apellidos.txt";
        this.RANGE = "Participantes!A1:D";
        this.SERVICESH = ApiAuth.getSheetsService();
        this.SERVICEDR = ApiAuth.getDriveService();
    }


    void tomarAsistencia (String reporteId, String destino, int horaMin)
            throws IOException {

        if (reporteId!=null) {
            String line, contenido = "";
            List<List<Object>> values = obtenerValores(reporteId);
            List<List<Object>> columAsistencia = new ArrayList<>();
            InputStream in = Asistencia.class.getResourceAsStream(SURNAMES_FILE_PATH);
            BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));

            System.out.println("*PRESENTES: ");
            while ((line = br.readLine()) != null) {
                String invaf = buscarAlumno(values, line, horaMin)!=null ? "A": "F";
                contenido += invaf + "\n";
                columAsistencia.add(Arrays.asList(invaf));
            }
            // Copiar al portapapeles
            contenido = contenido.substring(0,contenido.length()-2); //BUSTAMANTE BEJARANO
            copiarPortapapeles(contenido);

            actualizarCeldas(destino, getRango("101-I"), columAsistencia);
        }
    }


    List<List<Object>> obtenerValores (String reporteId)
            throws IOException {

        ValueRange response = SERVICESH.spreadsheets().values()
                .get(reporteId, RANGE)
                .execute();
        return response.getValues();
    }


    void actualizarCeldas (String sheetsId, String rango, List<List<Object>> valores)
            throws IOException {
        // Updates the values in the specified range.
        ValueRange body = new ValueRange().setValues(valores);

        SERVICESH.spreadsheets().values().update(sheetsId, rango, body)
                .setValueInputOption("RAW")
                .execute();

        System.out.println("Celdas actualizadas: " + rango);
    }


    public static String buscarAlumno (List<List<Object>> values, String dato, int horaMin) {
        String apellido, alumno;

        for (List<Object> fila: values) {
            apellido = fila.get(0).toString().toUpperCase();
            alumno = fila.get(1).toString().toUpperCase();
            int time = fila.get(3).toString().charAt(0) - 48 - horaMin;

            if (dato.equals(apellido) && time>=0) {
                values.remove(fila);
                System.out.println("   " + alumno + " " + apellido + "\tt: " + fila.get(3).toString());
                return apellido;
            }
        }
        return null;
    }


    String buscarReporteId (String carpetaId)
            throws IOException {
        int niveles = 2;
        String validar, fechaActual;

        for (int i=0; i<niveles; i++) {
            FileList result = SERVICEDR.files().list()
                    .setQ("'" + carpetaId + "' in parents")
                    .setFields("files(id, name)")
                    .setOrderBy("modifiedTime desc")
                    .setPageSize(5)
                    .execute();

            fechaActual = ProjectMain.getDateFormat("yyyy-MM-dd");
            //fechaActual = "2024-01-12"; // para alguna otra fecha
            validar = buscarReporteId(result.getFiles(), fechaActual); // Obtiene la ultima carpeta modificada

            if (validar == null) {
                System.out.println("Archivo no encontrado: " + carpetaId);
                return null;
            }
            carpetaId = validar;
        }
        return carpetaId;   //retorna el id del sheet
    }
    private String buscarReporteId (List<File> archivos, String fecha) {

        for (File arch: archivos) {
            if (arch.getName().contains(fecha))
                return arch.getId();
        }
        return null;
    }


    String getRango (String hoja) {
        String column = getColumnaActual();
        return hoja + "!" + column + 6 + ":" + column + 51;
    }
    String getColumnaActual() {
        LocalDate fechActual = LocalDate.now();
        int mes = fechActual.getMonthValue();
        int dia = fechActual.getDayOfMonth() + (mes-1)*31+9;

        return getColumna(dia);
    }
    String getColumna (int numColumna) {
        String columnActual = "";

        while (numColumna > 0) {
            numColumna--;
            char letter = (char) ('A' + numColumna % 26);
            columnActual = letter + columnActual;
            numColumna /= 26;
        }
        return columnActual;
    }


    void copiarPortapapeles(String contenido) {
        StringSelection selection = new StringSelection(contenido);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, null);
    }

    public static void main (String[] args)
            throws IOException, GeneralSecurityException {
        System.out.println("HOLA MUNDO");


        Asistencia asist = new Asistencia();
        String sheetId = asist.buscarReporteId("1hr2-fDadKv_qGbO3v5Yb9uUeE2QVisNZ");
        String asistenciaId = "18NwiCJf1cBMphEeth4sUax79YbwkRgMBC7Tlt52DNhg";

        asist.tomarAsistencia(
                sheetId,
                asistenciaId,
                0
        );

    }
}
