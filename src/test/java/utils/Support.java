package utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.response.Response;
import java.io.File;
import java.io.IOException;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public class Support {

    public static Response registerUserToFile(String filePath) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        Map<String, Object> user = FakerUtils.generateUserData();
        String user_name = user.get("name").toString();
        String user_email = user.get("email").toString();
        String user_password = user.get("password").toString();

        Response response = given()
                .header("accept", "application/json")
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .formParam("name", user_name)
                .formParam("email", user_email)
                .formParam("password", user_password)
                .log().all()
                .when()
                .post("/users/register")
                .then()
                .log().all()
                .statusCode(201)
                .body("success", equalTo(true))
                .body("status", equalTo(201))
                .body("message", equalTo("User account created successfully"))
                .body("data.name", equalTo(user_name))
                .body("data.email", equalTo(user_email))
                .extract()
                .response();

        String user_id = response.path("data.id");

        UserData userData = new UserData(user_name, user_email, user_id, user_password);
        objectMapper.writeValue(new File(filePath), userData);

        System.out.println("User data successfully saved to " + filePath);
        return response;
    }

    public static Response loginUserFromFile(String filePath) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        File file = new File(filePath);

        UserData userData = objectMapper.readValue(file, UserData.class);

        Response loginResponse = given()
                .header("accept", "application/json")
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .formParam("email", userData.email)
                .formParam("password", userData.password)
                .log().all()
                .when()
                .post("/users/login")
                .then()
                .log().all()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("status", equalTo(200))
                .body("message", equalTo("Login successful"))
                .body("data.id", equalTo(userData.id))
                .body("data.name", equalTo(userData.name))
                .body("data.email", equalTo(userData.email))
                .extract()
                .response();

        String token = loginResponse.path("data.token");
        userData.token = token;
        objectMapper.writeValue(file, userData);

        System.out.println("User token successfully saved to " + file.getAbsolutePath());
        return loginResponse;
    }

    public static Response deleteUserFromFile(String filePath) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        File file = new File(filePath);

        UserData userData = objectMapper.readValue(file, UserData.class);

        Response deleteResponse = given()
                .header("accept", "application/json")
                .header("x-auth-token", userData.token)
                .log().all()
                .when()
                .delete("/users/delete-account")
                .then()
                .log().all()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("status", equalTo(200))
                .body("message", equalTo("Account successfully deleted"))
                .extract()
                .response();

        System.out.println("User account successfully deleted.");
        return deleteResponse;
    }

    public static void deleteUserDataFile(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            if (file.delete()) {
                System.out.println("User data file " + filePath + " successfully deleted.");
            } else {
                System.out.println("Failed to delete user data file " + filePath);
            }
        } else {
            System.out.println("File not found: " + filePath);
        }
    }

    public static Response createNoteForUserFromFile(String filePath) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        File file = new File(filePath);

        // Lê os dados do usuário do arquivo
        UserData userData = objectMapper.readValue(file, UserData.class);

        // Gerar dados da nota
        Map<String, Object> noteData = FakerUtils.generateUserData();
        String noteTitle = noteData.get("noteTitle").toString();
        String noteDescription = noteData.get("noteDescription").toString();
        String noteCategory = noteData.get("noteCategory").toString();

        // Criar nota
        Response createNoteResponse = given()
                .header("accept", "application/json")
                .header("x-auth-token", userData.token)
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .formParam("title", noteTitle)
                .formParam("description", noteDescription)
                .formParam("category", noteCategory)
                .log().all()
                .when()
                .post("/notes")
                .then()
                .log().all()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("status", equalTo(200))
                .body("message", equalTo("Note successfully created"))
                .body("data.title", equalTo(noteTitle))
                .body("data.description", equalTo(noteDescription))
                .body("data.category", equalTo(noteCategory))
                .body("data.completed", equalTo(false))
                .body("data.user_id", equalTo(userData.id))
                .extract()
                .response();

        // Extrair dados da nota
        String noteId = createNoteResponse.path("data.id");

        // Atualizar o usuário com o ID da nota
        userData.note_id = noteId;
        userData.note_title = noteTitle;
        userData.note_description = noteDescription;
        userData.note_category = noteCategory;
        userData.note_created_at = createNoteResponse.path("data.created_at");
        userData.note_updated_at = createNoteResponse.path("data.updated_at");
        userData.note_completed = createNoteResponse.path("data.completed");

        // Salvar os dados de volta no arquivo
        objectMapper.writeValue(file, userData);

        return createNoteResponse;
    }


    public static class UserData {
        public String name;
        public String email;
        public String id;
        public String password;
        public String token;

        public String note_id;
        public String note_title;
        public String note_description;
        public String note_category;
        public String note_created_at;
        public String note_updated_at;
        public boolean note_completed;

        public UserData() {}

        public UserData(String name, String email, String id, String password) {
            this.name = name;
            this.email = email;
            this.id = id;
            this.password = password;
        }
    }

}
