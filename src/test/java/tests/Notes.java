package tests;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.Test;
import utils.FakerUtils;
import utils.Support;
import utils.Support.UserData;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class Notes {

    @Test
    public void createNote() throws IOException {
        RestAssured.baseURI = "https://practice.expandtesting.com/notes/api";
        String filePath = "src/test/fixtures/userData-" + System.currentTimeMillis() + ".json";

        File file = new File(filePath);

        // Register
        Support.registerUserToFile(filePath);

        // Login
        Support.loginUserFromFile(file.getAbsolutePath());

        // Read user data from file
        ObjectMapper objectMapper = new ObjectMapper();
        UserData userData = objectMapper.readValue(file, UserData.class);

        // Generate note data
        Map<String, Object> noteData = FakerUtils.generateUserData();
        String noteTitle = noteData.get("noteTitle").toString();
        String noteDescription = noteData.get("noteDescription").toString();
        String noteCategory = noteData.get("noteCategory").toString();

        // Create Note
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

        // Extract note details
        String noteId = createNoteResponse.path("data.id");
        String noteCreatedAt = createNoteResponse.path("data.created_at");
        String noteUpdatedAt = createNoteResponse.path("data.updated_at");
        boolean noteCompleted = createNoteResponse.path("data.completed");

        // Append note data to userData
        userData.note_id = noteId;
        userData.note_title = noteTitle;
        userData.note_description = noteDescription;
        userData.note_category = noteCategory;
        userData.note_created_at = noteCreatedAt;
        userData.note_updated_at = noteUpdatedAt;
        userData.note_completed = noteCompleted;

        // Save updated data back to file
        objectMapper.writeValue(file, userData);

        // Delete
        Support.deleteUserFromFile(file.getAbsolutePath());

        // Delete json file
        Support.deleteUserDataFile(filePath);
    }

    @Test
    public void getAllNotesCreatedByUser_withFullValidation() throws IOException {
        RestAssured.baseURI = "https://practice.expandtesting.com/notes/api";
        String filePath = "src/test/fixtures/userData-" + System.currentTimeMillis() + ".json";
        File file = new File(filePath);

        // Register and login user
        Support.registerUserToFile(filePath);
        Support.loginUserFromFile(file.getAbsolutePath());

        // Read user data
        ObjectMapper objectMapper = new ObjectMapper();
        UserData userData = objectMapper.readValue(file, UserData.class);

        // Generate note data using Faker
        Map<String, Object> noteData = FakerUtils.generateUserData();
        String noteTitle1 = noteData.get("noteTitle").toString();
        String noteDescription1 = noteData.get("noteDescription").toString();
        String noteCategory1 = noteData.get("noteCategory").toString();

        String noteTitle2 = noteData.get("noteTitle2").toString();
        String noteDescription2 = noteData.get("noteDescription2").toString();
        String noteCategory2 = noteData.get("noteCategory2").toString();

        // Create first note
        Response firstNoteResponse = given()
                .header("accept", "application/json")
                .header("x-auth-token", userData.token)
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .formParam("title", noteTitle1)
                .formParam("description", noteDescription1)
                .formParam("category", noteCategory1)
                .log().all()
                .when()
                .post("/notes")
                .then()
                .log().all()
                .statusCode(200)
                .extract()
                .response();

        String note1Id = firstNoteResponse.path("data.id");
        String note1CreatedAt = firstNoteResponse.path("data.created_at");
        String note1UpdatedAt = firstNoteResponse.path("data.updated_at");

        // Create second note
        Response secondNoteResponse = given()
                .header("accept", "application/json")
                .header("x-auth-token", userData.token)
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .formParam("title", noteTitle2)
                .formParam("description", noteDescription2)
                .formParam("category", noteCategory2)
                .log().all()
                .when()
                .post("/notes")
                .then()
                .log().all()
                .statusCode(200)
                .extract()
                .response();

        String note2Id = secondNoteResponse.path("data.id");
        String note2CreatedAt = secondNoteResponse.path("data.created_at");
        String note2UpdatedAt = secondNoteResponse.path("data.updated_at");

        // Retrieve all notes for user and validate
        given()
                .header("accept", "application/json")
                .header("x-auth-token", userData.token)
                .log().all()
                .when()
                .get("/notes")
                .then()
                .log().all()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("status", equalTo(200))
                .body("message", equalTo("Notes successfully retrieved"))
                .body("data.size()", greaterThanOrEqualTo(2))

                // Second note (most recent) - index [0]
                .body("data[0].id", equalTo(note2Id))
                .body("data[0].title", equalTo(noteTitle2))
                .body("data[0].description", equalTo(noteDescription2))
                .body("data[0].category", equalTo(noteCategory2))
                .body("data[0].completed", equalTo(false))
                .body("data[0].created_at", equalTo(note2CreatedAt))
                .body("data[0].updated_at", equalTo(note2UpdatedAt))
                .body("data[0].user_id", equalTo(userData.id))

                // First note - index [1]
                .body("data[1].id", equalTo(note1Id))
                .body("data[1].title", equalTo(noteTitle1))
                .body("data[1].description", equalTo(noteDescription1))
                .body("data[1].category", equalTo(noteCategory1))
                .body("data[1].completed", equalTo(false))
                .body("data[1].created_at", equalTo(note1CreatedAt))
                .body("data[1].updated_at", equalTo(note1UpdatedAt))
                .body("data[1].user_id", equalTo(userData.id));

        // Cleanup
        Support.deleteUserFromFile(file.getAbsolutePath());
        Support.deleteUserDataFile(filePath);
    }

    @Test
    public void getNoteById() throws IOException {
        RestAssured.baseURI = "https://practice.expandtesting.com/notes/api";
        String filePath = "src/test/fixtures/userData-" + System.currentTimeMillis() + ".json";
        File file = new File(filePath);

        // Register and Login User, saving data to file
        Support.registerUserToFile(filePath);
        Support.loginUserFromFile(file.getAbsolutePath());

        // Create note and save note ID and other data to file
        Support.createNoteForUserFromFile(file.getAbsolutePath());

        // Read user data from file
        ObjectMapper objectMapper = new ObjectMapper();
        UserData userData = objectMapper.readValue(file, UserData.class);

        // Get Note using the note_id from the user data
        Response getNoteResponse = given()
                .header("accept", "application/json")
                .header("x-auth-token", userData.token)
                .log().all()
                .when()
                .get("/notes/" + userData.note_id)
                .then()
                .log().all()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("status", equalTo(200))
                .body("message", equalTo("Note successfully retrieved"))
                .body("data.id", equalTo(userData.note_id))
                .body("data.title", equalTo(userData.note_title))
                .body("data.description", equalTo(userData.note_description))
                .body("data.category", equalTo(userData.note_category))
                .body("data.completed", equalTo(userData.note_completed))
                .body("data.user_id", equalTo(userData.id))
                .body("data.created_at", equalTo(userData.note_created_at))  // Assuming you want to match the exact timestamp
                .body("data.updated_at", equalTo(userData.note_updated_at))  // Assuming you want to match the exact timestamp
                .extract()
                .response();

        // Clean up after test
        Support.deleteUserFromFile(file.getAbsolutePath());
        Support.deleteUserDataFile(filePath);
    }

    @Test
    public void updateNoteById() throws IOException {
        RestAssured.baseURI = "https://practice.expandtesting.com/notes/api";
        String filePath = "src/test/fixtures/userData-" + System.currentTimeMillis() + ".json";
        File file = new File(filePath);

        // Register and Login User, saving data to file
        Support.registerUserToFile(filePath);
        Support.loginUserFromFile(file.getAbsolutePath());

        // Create note and save note ID and other data to file
        Support.createNoteForUserFromFile(file.getAbsolutePath());

        // Read user data from file
        ObjectMapper objectMapper = new ObjectMapper();
        UserData userData = objectMapper.readValue(file, UserData.class);

        // Generate note data using FakerUtils
        Map<String, Object> noteData = FakerUtils.generateUserData();
        String updatedTitle = noteData.get("noteUpdatedTitle").toString();  // from FakerUtils
        String updatedDescription = noteData.get("noteUpdatedDescription").toString();  // from FakerUtils
        String updatedCategory = noteData.get("noteUpdatedCategory").toString();  // from FakerUtils
        Boolean updatedCompleted = (Boolean) noteData.get("noteUpdatedCompleted");  // from FakerUtils

        // Perform the PUT request to update the note
        Response updateNoteResponse = given()
                .header("accept", "application/json")
                .header("x-auth-token", userData.token)  // Using userData.token directly
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .formParam("title", updatedTitle)
                .formParam("description", updatedDescription)
                .formParam("completed", updatedCompleted)
                .formParam("category", updatedCategory)
                .log().all()
                .when()
                .put("/notes/" + userData.note_id)  // Using userData.note_id directly
                .then()
                .log().all()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("status", equalTo(200))
                .body("message", equalTo("Note successfully Updated"))
                .body("data.id", equalTo(userData.note_id))  // Validate note ID directly
                .body("data.title", equalTo(updatedTitle))
                .body("data.description", equalTo(updatedDescription))
                .body("data.category", equalTo(updatedCategory))
                .body("data.completed", equalTo(updatedCompleted))
                .body("data.user_id", equalTo(userData.id))  // Validate user ID directly
                .body("data.created_at", equalTo(userData.note_created_at))  // Validate that created_at remains the same
                .body("data.updated_at", not(emptyOrNullString()))  // Ensure updated_at is not null or empty
                .extract()
                .response();

        // Clean up after test
        Support.deleteUserFromFile(file.getAbsolutePath());
        Support.deleteUserDataFile(filePath);
    }

    @Test
    public void updateNoteStatusById() throws IOException {
        RestAssured.baseURI = "https://practice.expandtesting.com/notes/api";
        String filePath = "src/test/fixtures/userData-" + System.currentTimeMillis() + ".json";
        File file = new File(filePath);

        // Register and Login User, saving data to file
        Support.registerUserToFile(filePath);
        Support.loginUserFromFile(file.getAbsolutePath());

        // Create note and save note ID and other data to file
        Support.createNoteForUserFromFile(file.getAbsolutePath());

        // Read user data from file
        ObjectMapper objectMapper = new ObjectMapper();
        UserData userData = objectMapper.readValue(file, UserData.class);

        // Generate note data using FakerUtils
        Map<String, Object> noteData = FakerUtils.generateUserData();
        Boolean updatedCompleted = (Boolean) noteData.get("noteUpdatedCompleted");  // from FakerUtils

        // Perform the PATCH request to update only the 'completed' status of the note
        Response updateNoteResponse = given()
                .header("accept", "application/json")
                .header("x-auth-token", userData.token)  // Using userData.token directly
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .formParam("completed", updatedCompleted)  // Only updating the 'completed' field
                .log().all()
                .when()
                .patch("/notes/" + userData.note_id)  // Using PATCH instead of PUT
                .then()
                .log().all()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("status", equalTo(200))
                .body("message", equalTo("Note successfully Updated"))
                .body("data.id", equalTo(userData.note_id))  // Validate note ID directly
                .body("data.completed", equalTo(updatedCompleted))  // Validate that completed is updated
                .body("data.user_id", equalTo(userData.id))  // Validate user ID directly
                .body("data.created_at", equalTo(userData.note_created_at))  // Validate that created_at remains the same
                .body("data.updated_at", not(emptyOrNullString()))  // Ensure updated_at is not null or empty
                .extract()
                .response();

        // Clean up after test
        Support.deleteUserFromFile(file.getAbsolutePath());
        Support.deleteUserDataFile(filePath);
    }

    @Test
    public void deleteNoteById() throws IOException {
        RestAssured.baseURI = "https://practice.expandtesting.com/notes/api";
        String filePath = "src/test/fixtures/userData-" + System.currentTimeMillis() + ".json";
        File file = new File(filePath);

        // Register and Login User, saving data to file
        Support.registerUserToFile(filePath);
        Support.loginUserFromFile(file.getAbsolutePath());

        // Create note and save note ID and other data to file
        Support.createNoteForUserFromFile(file.getAbsolutePath());

        // Read user data from file
        ObjectMapper objectMapper = new ObjectMapper();
        UserData userData = objectMapper.readValue(file, UserData.class);

        // Perform the DELETE request to delete the note
        Response deleteNoteResponse = given()
                .header("accept", "application/json")
                .header("x-auth-token", userData.token)  // Using userData.token directly
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .log().all()
                .when()
                .delete("/notes/" + userData.note_id)  // DELETE request to remove the note
                .then()
                .log().all()
                .statusCode(200)
                .body("success", equalTo(true))  // Ensure success is true
                .body("status", equalTo(200))  // Validate the status code
                .body("message", equalTo("Note successfully deleted"))  // Validate the success message
                .extract()
                .response();

        // Clean up after test
        Support.deleteUserFromFile(file.getAbsolutePath());
        Support.deleteUserDataFile(filePath);
    }

}
