package com.nutritrack.food.off;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

/**
 * Exercises the real {@link OffClient} RestClient path (not mocked). This catches Jackson 2/3
 * deserialization mismatches that unit tests with {@code @MockitoBean OffClient} miss.
 */
@SpringBootTest
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:offclient;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.flyway.enabled=true",
      "spring.flyway.locations=classpath:db/migration",
      "spring.jpa.hibernate.ddl-auto=validate",
      "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/unused",
      "nutritrack.food.cache.redis-enabled=false",
      "spring.batch.jdbc.initialize-schema=always",
      "spring.batch.job.enabled=false",
      "spring.autoconfigure.exclude=org.springframework.boot.data.redis.autoconfigure.RedisAutoConfiguration,org.springframework.boot.data.redis.autoconfigure.RedisRepositoriesAutoConfiguration"
    })
class OffClientIntegrationTest {

  private static final HttpServer SERVER;
  private static final int PORT;

  static {
    try {
      SERVER = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      PORT = SERVER.getAddress().getPort();
      SERVER.createContext(
          "/api/v2/product/",
          exchange -> {
            String path = exchange.getRequestURI().getPath();
            String barcode = path.substring(path.lastIndexOf('/') + 1);
            byte[] body;
            if ("3017620422003".equals(barcode)) {
              body =
                  """
                  {
                    "status": 1,
                    "product": {
                      "product_name": "Nutella",
                      "brands": "Ferrero",
                      "quantity": "400 g",
                      "serving_size": "15 g",
                      "nutrition_grades": "e",
                      "ingredients_text": "sugar, palm oil",
                      "allergens_tags": ["en:milk"],
                      "image_url": "https://example.test/n.jpg",
                      "nutriments": {
                        "energy-kcal_100g": 539,
                        "proteins_100g": 6.3
                      }
                    }
                  }
                  """
                      .getBytes(StandardCharsets.UTF_8);
            } else {
              body =
                  """
                  {"code":"%s","status":0,"status_verbose":"product not found"}
                  """
                      .formatted(barcode)
                      .getBytes(StandardCharsets.UTF_8);
            }
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
              os.write(body);
            }
          });
      SERVER.createContext(
          "/cgi/search.pl",
          exchange -> {
            byte[] body =
                """
                {
                  "products": [
                    {
                      "code": "3017620422003",
                      "product_name": "Nutella",
                      "brands": "Ferrero",
                      "nutriments": { "energy-kcal_100g": 539 }
                    }
                  ]
                }
                """
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
              os.write(body);
            }
          });
      SERVER.setExecutor(Executors.newCachedThreadPool());
      SERVER.start();
    } catch (IOException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  @Autowired private OffClient offClient;

  @AfterAll
  static void stopOffStub() {
    SERVER.stop(0);
  }

  @DynamicPropertySource
  static void offBaseUrl(DynamicPropertyRegistry registry) {
    registry.add("nutritrack.food.off.base-url", () -> "http://127.0.0.1:" + PORT);
  }

  @Test
  void fetchByBarcodeDeserializesOffJsonViaRestClient() {
    Optional<NormalizedOffProduct> product = offClient.fetchByBarcode("3017620422003");

    assertThat(product).isPresent();
    assertThat(product.get().name()).isEqualTo("Nutella");
    assertThat(product.get().brand()).isEqualTo("Ferrero");
    assertThat(product.get().nutrients())
        .anySatisfy(n -> assertThat(n.code()).isEqualTo("energy_kcal"));
  }

  @Test
  void fetchByBarcodeSoftMissReturnsEmpty() {
    assertThat(offClient.fetchByBarcode("0000000000000")).isEmpty();
  }

  @Test
  void searchByNameDeserializesOffJsonViaRestClient() {
    assertThat(offClient.searchByName("nutella", 1))
        .singleElement()
        .satisfies(
            p -> {
              assertThat(p.barcode()).isEqualTo("3017620422003");
              assertThat(p.name()).isEqualTo("Nutella");
            });
  }
}
