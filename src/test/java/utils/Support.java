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

    public static class UserData {
        public String name;
        public String email;
        public String id;
        public String password;
        public String token;

        public UserData() {}

        public UserData(String name, String email, String id, String password) {
            this.name = name;
            this.email = email;
            this.id = id;
            this.password = password;
        }
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

}
