import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.*;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.classroom.Classroom;
import com.google.api.services.classroom.model.CourseWorkMaterial;
import com.google.api.services.classroom.model.DriveFile;
import com.google.api.services.classroom.model.ListCourseWorkMaterialResponse;
import com.google.api.services.classroom.model.Material;
import com.google.api.services.classroom.model.Link;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;

public class MaterialesPdf {

    private final Classroom SERVICECL;
    private final Drive SERVICEDR;

    public MaterialesPdf()
            throws GeneralSecurityException, IOException {
        this.SERVICECL = ApiAuth.getClassroomService();
        this.SERVICEDR = ApiAuth.getDriveService();
    }

    void programarMateriales
            (String salonId, String modeloId, String carpetaId, Map<String, String> mapMateriales, HashMap<String, String[]> mapCursos)
            throws IOException {

        for (String nombMat: mapMateriales.keySet()) {
            List<Material> materiales = buscarMaterial(nombMat, obtenerMateriales(modeloId));

            if (!materiales.isEmpty()) {
                HashMap<String, String> hashMap = getMapMateriales(materiales);

                System.out.println("MATERIALES PROGRAMADOS:");
                for (String key : mapCursos.keySet()) {

                    int length = nombMat.length();
                    String title = nombMat.charAt(0) + nombMat.substring(length-2, length).trim()
                            + " - " + mapCursos.get(key)[0] + " INGENIERIAS";
                    String link = obtenerLink(hashMap.get(key), carpetaId);

                    programarMaterial(
                            salonId,    // id del salon (102 Ingenierias)
                            mapCursos.get(key)[1],  // id del curso (matematica, ingles, etc)
                            title,  //titulo del material
                            link,   //enlace drive
                            mapMateriales.get(nombMat) //formato de fecha
                    );
                    System.out.println("\t" + title);
                }
            }
        }
    }


    String obtenerLink (String archivoId, String carpetaId)
            throws IOException {
        File archivo = copiarArchivo(archivoId, carpetaId);

        return "https://drive.google.com/file/d/"
                + archivo.getId()
                + "/view";
    }


    List<CourseWorkMaterial> obtenerMateriales (String modeloId, int size, String estado)
            throws IOException {

        ListCourseWorkMaterialResponse response = SERVICECL
                .courses()
                .courseWorkMaterials()
                .list(modeloId) // id de classroom modelo
                .setPageSize(size)     // tamaño de la lista
                .setFields("courseWorkMaterial(id,title,materials,state)")    // filtra solo valores necesarios
                .setOrderBy("updateTime desc")  // ultimos actualizados en orden descendente
                .setCourseWorkMaterialStates(Collections.singletonList(estado))
                .execute();

        return response.getCourseWorkMaterial();
    }
    List<CourseWorkMaterial> obtenerMateriales (String modeloId)
            throws IOException {
        return obtenerMateriales(modeloId, 5, "PUBLISHED"); // 5 ultimos
    }

    List<Material> buscarMaterial (String name, List<CourseWorkMaterial> workMaterials) {

        for (CourseWorkMaterial material: workMaterials) {
            if (name.equals(material.getTitle()))
                return material.getMaterials();
        }

        System.out.println("Material no encontrado: " + name);
        return new ArrayList<>();
    }


    HashMap<String, String> getMapMateriales (List<Material> materialesPdf) {
        HashMap<String, String> mapArchivos = new HashMap<>();

        for (Material material : materialesPdf) {
            DriveFile fileDrive = material.getDriveFile().getDriveFile();
            mapArchivos.put(fileDrive.getTitle().substring(0,2), fileDrive.getId());
        }
        return mapArchivos;
    }


    File copiarArchivo (String archivoId, String carpetaId)
            throws IOException {
        File archivo = new File();
        List<String> parents = Collections.singletonList(carpetaId);

        return SERVICEDR.files()
                .copy (
                        archivoId,
                        archivo.setParents(parents)
                )
                .execute();
    }


    CourseWorkMaterial crearMaterial
            (String courseId, String topicId, String title, String link, String state, String date)
            throws IOException {
        Link articleLink = new Link().setUrl(link);
        List<Material> materials = Collections.singletonList(new Material().setLink(articleLink));

        CourseWorkMaterial content =
            new CourseWorkMaterial()
                .setTitle(title)    // Titulo material
                .setTopicId(topicId)    // Id de topic ejem (matematica, ingles, etc)
                .setMaterials(materials)    //Archivo drive
                .setState(state)    //estado (DRAFT, PUBLISHED)
                .setScheduledTime(date);    //fecha a programar

        return SERVICECL.courses().courseWorkMaterials().create(courseId, content).execute();
    }
    CourseWorkMaterial publicarMaterial
            (String courseId, String topicId, String title, String link)
            throws IOException {
        return crearMaterial(courseId, topicId, title, link, "PUBLISHED", "");
    }
    CourseWorkMaterial programarMaterial
            (String courseId, String topicId, String title, String link, String date)
            throws IOException {
        return crearMaterial(courseId, topicId, title, link, "DRAFT", date);
    }




    public static void main (String[] args)
            throws GeneralSecurityException, IOException {
        System.out.println("HOLA MUNDO");
        MaterialesPdf nuevo = new MaterialesPdf();

        Classroom service = ApiAuth.getClassroomService();
        String modeloId = "650437997576";

        List<CourseWorkMaterial> materiales = nuevo.obtenerMateriales("650163376669", 20, "PUBLISHED");

        for (CourseWorkMaterial c: materiales) {
            System.out.println(c.getTitle() + "\t" + c.getId());
            String id = c.getId();

            //service.courses().courseWorkMaterials().delete("650163376669", id).execute();
            //CourseWorkMaterial material = service.courses().courseWorkMaterials().get("650163376669", id).execute();
            //System.out.println(material);
        }

        /*CourseWorkMaterial zz = nuevo.programarMaterial(
                "650163376669",
                "650434957400",
                "Hola mundo",
                "www.google.com",
                ProjectMain.getDateTime(2024, 1, 15, 17, 30)
        );  //String courseId, String topicId, String title, String link, String date
        System.out.println(zz.getId());*/

        try {
            service.courses().courseWorkMaterials().delete("650163376669", "654313968706").execute();
        } catch (GoogleJsonResponseException e) {
            System.out.println(e);
        }


        //System.out.println(materiales);
    }
}