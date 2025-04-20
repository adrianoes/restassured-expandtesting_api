package tests;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

public class Health {

    @BeforeClass
    public void setup() {
        RestAssured.baseURI = "https://practice.expandtesting.com/notes/api";
    }

    @Test(groups = {"health"})
    public void validarHealthCheck() {
        given()
                .accept(ContentType.JSON) // Header: Accept: application/json
                .when()
                .get("/health-check")
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("status", equalTo(200))
                .body("message", equalTo("Notes API is Running"));
    }
}
