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
import static org.hamcrest.Matchers.equalTo;

public class Users {

    public static String user_id;

    @Test
    public void registerUser() throws IOException {
        RestAssured.baseURI = "https://practice.expandtesting.com/notes/api";

        // Register
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

        user_id = response.path("data.id");

        UserData userData = new UserData(user_name, user_email, user_id, user_password);

        ObjectMapper objectMapper = new ObjectMapper();
        String filePath = "src/test/fixtures/userData-" + System.currentTimeMillis() + ".json";
        File file = new File(filePath);

        objectMapper.writeValue(file, userData);
        System.out.println("User data successfully saved to " + file.getAbsolutePath());

        // Login
        Support.loginUserFromFile(file.getAbsolutePath());

        // Delete
        Support.deleteUserFromFile(file.getAbsolutePath());

        // Delete json file
        Support.deleteUserDataFile(filePath);
    }

    @Test
    public void loginUser() throws IOException {
        RestAssured.baseURI = "https://practice.expandtesting.com/notes/api";
        String filePath = "src/test/fixtures/userData-" + System.currentTimeMillis() + ".json";

        // Register
        Support.registerUserToFile(filePath);

        ObjectMapper objectMapper = new ObjectMapper();
        File file = new File(filePath);
        UserData userData = objectMapper.readValue(file, UserData.class);

        // Login
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

        // Delete
        Support.deleteUserFromFile(filePath);

        // Delete json file
        Support.deleteUserDataFile(filePath);
    }

    @Test
    public void retrieveUser() throws IOException {
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

        // Retrieve
        Response retrieveResponse = given()
                .header("accept", "application/json")
                .header("x-auth-token", userData.token)
                .log().all()
                .when()
                .get("/users/profile")
                .then()
                .log().all()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("status", equalTo(200))
                .body("message", equalTo("Profile successful"))
                .body("data.id", equalTo(userData.id))
                .body("data.name", equalTo(userData.name))
                .body("data.email", equalTo(userData.email))
                .extract()
                .response();

        // Delete
        Support.deleteUserFromFile(file.getAbsolutePath());

        // Delete json file
        Support.deleteUserDataFile(filePath);
    }

    @Test
    public void updateUser() throws IOException {
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

        // Generate update data
        Map<String, Object> updateData = FakerUtils.generateUserData();
        String updatedName = updateData.get("updatedName").toString();
        String updatedPhone = updateData.get("phone").toString();
        String updatedCompany = updateData.get("company").toString();

        // Update
        Response updateResponse = given()
                .header("accept", "application/json")
                .header("x-auth-token", userData.token)
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .formParam("name", updatedName)
                .formParam("phone", updatedPhone)
                .formParam("company", updatedCompany)
                .log().all()
                .when()
                .patch("/users/profile")
                .then()
                .log().all()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("status", equalTo(200))
                .body("message", equalTo("Profile updated successful"))
                .body("data.id", equalTo(userData.id))
                .body("data.email", equalTo(userData.email))
                .body("data.name", equalTo(updatedName))
                .body("data.phone", equalTo(updatedPhone))
                .body("data.company", equalTo(updatedCompany))
                .extract()
                .response();

        // Delete
        Support.deleteUserFromFile(file.getAbsolutePath());

        // Delete json file
        Support.deleteUserDataFile(filePath);
    }

    @Test
    public void updateUserPassword() throws IOException {
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

        // Generate updated password
        Map<String, Object> updatedData = FakerUtils.generateUserData();
        String updatedPassword = updatedData.get("updatedPassword").toString();

        // Change password
        Response changePasswordResponse = given()
                .header("accept", "application/json")
                .header("x-auth-token", userData.token)
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .formParam("currentPassword", userData.password)
                .formParam("newPassword", updatedPassword)
                .log().all()
                .when()
                .post("/users/change-password")
                .then()
                .log().all()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("status", equalTo(200))
                .body("message", equalTo("The password was successfully updated"))
                .extract()
                .response();

        // Delete
        Support.deleteUserFromFile(file.getAbsolutePath());

        // Delete json file
        Support.deleteUserDataFile(filePath);
    }

    @Test
    public void logoutAndReLoginUser() throws IOException {
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

        // Logout
        Response logoutResponse = given()
                .header("accept", "application/json")
                .header("x-auth-token", userData.token)
                .log().all()
                .when()
                .delete("/users/logout")
                .then()
                .log().all()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("status", equalTo(200))
                .body("message", equalTo("User has been successfully logged out"))
                .extract()
                .response();

        // Login novamente para renovar token
        Support.loginUserFromFile(file.getAbsolutePath());

        // Delete após novo login
        Support.deleteUserFromFile(file.getAbsolutePath());

        // Deletar JSON
        Support.deleteUserDataFile(filePath);
    }

    @Test
    public void deleteUser() throws IOException {
        RestAssured.baseURI = "https://practice.expandtesting.com/notes/api";

        String filePath = "src/test/fixtures/userData-" + System.currentTimeMillis() + ".json";

        // Register
        Support.registerUserToFile(filePath);

        // Login
        Support.loginUserFromFile(filePath);

        ObjectMapper objectMapper = new ObjectMapper();
        File file = new File(filePath);
        UserData userData = objectMapper.readValue(file, UserData.class);

        // Delete
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

        //Delete json file
        Support.deleteUserDataFile(filePath);
    }

}
