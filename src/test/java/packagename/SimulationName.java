package packagename;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

public class SimulationName extends Simulation {

  public static final int vu = Integer.getInteger("vu", 1);
  public String secret = "";

  {
    String secretName = "my-secret-name";
    Region region = Region.of("eu-west-3");

    SecretsManagerClient client = SecretsManagerClient.builder().region(region).build();

    GetSecretValueRequest getSecretValueRequest =
        GetSecretValueRequest.builder().secretId(secretName).build();
    GetSecretValueResponse getSecretValueResponse;

    try {
      getSecretValueResponse = client.getSecretValue(getSecretValueRequest);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    secret = getSecretValueResponse.secretString();
  }

  HttpProtocolBuilder httpProtocol =
      http.baseUrl("https://computer-database.gatling.io")
          .acceptHeader("application/json")
          .userAgentHeader(
              "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:109.0) Gecko/20100101 Firefox/119.0");

  ScenarioBuilder users =
      scenario("Scenario 1")
          .exec(
              exec(
                  session -> {
                    if (secret != null) {
                      System.out.println("Retrieved secret string: " + secret);
                    } else {
                      System.out.println("No secret retrieved");
                      exitHere();
                    }
                    return session;
                  }),
              http("Home").get("/computers"));

  {
    setUp(users.injectOpen(atOnceUsers(vu))).protocols(httpProtocol);
  }
}
