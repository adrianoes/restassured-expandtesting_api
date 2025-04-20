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

        String randomNumber = FakerUtils.generateRandomNumber();

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
        String filename = "src/test/fixtures/userData-" + randomNumber + ".json";
        File file = new File(filename);

        objectMapper.writeValue(file, userData);
        System.out.println("User data successfully saved to " + file.getAbsolutePath());

        // Login
        Support.loginUserFromFile(file.getAbsolutePath());

        // Delete
        Support.deleteUserFromFile(file.getAbsolutePath());

        // Delete json file
        Support.deleteUserDataFile(filename);
    }

    @Test
    public void loginUser() throws IOException {
        RestAssured.baseURI = "https://practice.expandtesting.com/notes/api";

        String filename = "src/test/fixtures/userData-" + System.currentTimeMillis() + ".json";

        // Register
        Support.registerUserToFile(filename);

        ObjectMapper objectMapper = new ObjectMapper();
        File file = new File(filename);
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
        Support.deleteUserFromFile(filename);

        // Delete json file
        Support.deleteUserDataFile(filename);
    }

    @Test
    public void deleteUser() throws IOException {
        RestAssured.baseURI = "https://practice.expandtesting.com/notes/api";

        String filename = "src/test/fixtures/userData-" + System.currentTimeMillis() + ".json";

        // Register
        Support.registerUserToFile(filename);

        // Login
        Support.loginUserFromFile(filename);

        ObjectMapper objectMapper = new ObjectMapper();
        File file = new File(filename);
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
        Support.deleteUserDataFile(filename);
    }




}
