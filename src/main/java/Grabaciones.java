import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;

public class Grabaciones {

    private final Drive SERVICEDR;

    public Grabaciones ()
            throws GeneralSecurityException, IOException {
        this.SERVICEDR = ApiAuth.getDriveService();
    }


    void moverGrabaciones (String carpetaGr, String type)
            throws IOException {
        List<File> grabaciones = obtenerArchivos(carpetaGr, type, 10);

        if (grabaciones.isEmpty())
            System.out.println("***No hay archivos para mover");

        for (File arch: grabaciones) {
            String fecha = obtenerFecha(
                    arch.getName().substring(14, 24),
                    "yyyy-MM-dd",
                    "EEEE dd/MM/yyyy"
            );

            String carpetaDest = buscarPorNombre(fecha).getId();
            moverArchivo(arch.getId(), carpetaDest);
            System.out.println("  Movido: " + arch.getName());

            String ordenNombre = posicionGrabacion(arch.getName(), fecha);
            File nuevoArch = cambiarNombre(arch.getId(), ordenNombre);

            System.out.println("   Renombrado a: " + nuevoArch.getName());
        }
    }


    // Types: https://gist.github.com/kionay/b7838e7974a7f41a255b329d7da94ae2
    List<File> obtenerArchivos (String carpetaId, String type) throws IOException {
        return obtenerArchivos(carpetaId, type, 10);
    }
    List<File> obtenerArchivos (String carpetaId) throws IOException {
        return obtenerArchivos(carpetaId, null, 1000);
    }
    List<File> obtenerArchivos (String carpetaId, int pageSize) throws IOException {
        return obtenerArchivos(carpetaId, null, pageSize);
    }
    List<File> obtenerArchivos (String carpetaId, String type, int pageSize) throws IOException {

        String query = "'" + carpetaId + "' in parents" +
                ((type!=null) ? " and mimeType='" + type + "'" : "");

        FileList result = SERVICEDR.files().list()
                .setQ(query)
                .setFields("files(id, name)")
                .setOrderBy("modifiedTime desc")
                .setPageSize(pageSize)
                .execute();

        return result.getFiles();
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


    File moverArchivo (String archivoId, String carpetaId)
            throws IOException {
        // Retrieve the existing parents to remove
        File file = SERVICEDR.files().get(archivoId).setFields("parents").execute();

        StringBuilder previousParents = new StringBuilder();
        for (String parent : file.getParents()) {
            previousParents.append(parent);
            previousParents.append(',');
        }

        return SERVICEDR.files().update(archivoId, null)
                .setAddParents(carpetaId)
                .setRemoveParents(previousParents.toString())
                .setFields("id, parents")
                .execute();
    }


    File cambiarNombre (String archivoId, String nuevo)
            throws IOException {
        File archivoNew = new File()
                .setName(nuevo);

        return SERVICEDR.files()
                .update(archivoId, archivoNew)
                .execute();
    }

    String obtenerFecha (String fechaString, String in, String out) { //in = "yyyy-MM-dd"
        // Parsear la fecha original
        DateTimeFormatter format = DateTimeFormatter.ofPattern(in);
        LocalDate fechaOriginal = LocalDate.parse(fechaString, format);

        return obtenerFecha(fechaOriginal, out);
    }
    String obtenerFechaActual (String pattern) { //"EEEE dd/MM/yy"
        // Obtener la fecha actual
        LocalDate fechaActual = LocalDate.now();

        return obtenerFecha(fechaActual, pattern);
    }
    String obtenerFecha (int dia, int mes, int año, String pattern) {
        // Crear un objeto LocalDate con los parámetros proporcionados
        LocalDate fecha = LocalDate.of(año, mes, dia);

        return obtenerFecha(fecha, pattern);
    }
    private String obtenerFecha (LocalDate fecha, String pattern) {
        // Formatear la fecha en el patrón deseado
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        String fechaFormateada = fecha.format(formatter);

        return fechaFormateada.toUpperCase();
    }


    String posicionGrabacion (String cadena, String fecha) {
        String nombre = "";
        
        switch (cadena.substring(25, 27)) {
            case "06":
                nombre = fecha + " (PRIMERA GRABACION)";
                break;
            case "10":
                nombre = fecha + " (SEGUNDA GRABACION)";
                break;
            case "14":
                nombre = fecha + " (TERCERA GRABACION)";
                break;
            default:
                System.out.println("   " + cadena + " no cambiado");
                return null;
        }
        return nombre;
    }


    public static void main (String[] args)
            throws GeneralSecurityException, IOException {

        Grabaciones gr = new Grabaciones();

        //gr.moverArchivo("110M-Y6sXCr-3yZw3PAlFXC1KgyeAs1WO","1o_LU7aRTg6UZcEdk_5ugFZfqwgc7thvX");
        //gr.cambiarNombre("110M-Y6sXCr-3yZw3PAlFXC1KgyeAs1WO", "HOLA MUNDO");

        //String dato = gr.posicionGrabacion("iei-nwif-zcp (2024-12-27 10:55 GMT-5)", "zzz");
        //System.out.println(dato);

        gr.moverGrabaciones("1st1Oc-C5rk6CDwPKycEt7ivola5PAvD8", "video/mp4");
    }
}
