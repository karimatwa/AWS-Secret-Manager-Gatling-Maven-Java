package packagename;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

public class SimulationName extends Simulation {

  public static final int vu = Integer.getInteger("vu", 1);

  public static final ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();

  @Override
  public void before() {
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

    String secret = getSecretValueResponse.secretString();
    queue.offer(secret);
  }

  HttpProtocolBuilder httpProtocol =
      http.baseUrl("https://computer-database.gatling.io")
          .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
          .acceptLanguageHeader("en-US,en;q=0.5")
          .acceptEncodingHeader("gzip, deflate")
          .userAgentHeader(
              "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:109.0) Gecko/20100101 Firefox/119.0");

  ScenarioBuilder users =
      scenario("Scenario 1")
          .exec(
              exec(
                  session -> {
                    String secretString = queue.poll();

                    if (secretString != null) {
                      System.out.println("Retrieved secret string: " + secretString);
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