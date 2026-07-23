package com.nutritrack.enrichment.fdc;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:fdcclient;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.flyway.enabled=true",
      "spring.flyway.locations=classpath:db/migration",
      "spring.jpa.hibernate.ddl-auto=validate",
      "nutritrack.enrichment.internal-api-key=test-internal",
      "nutritrack.enrichment.fdc.api-key=DEMO_KEY"
    })
class FdcClientIntegrationTest {

  private static final HttpServer SERVER;
  private static final int PORT;
  private static final AtomicInteger SEARCH_HITS = new AtomicInteger();

  static {
    try {
      SERVER = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      PORT = SERVER.getAddress().getPort();
      SERVER.createContext(
          "/foods/search",
          exchange -> {
            SEARCH_HITS.incrementAndGet();
            String query = exchange.getRequestURI().getQuery();
            byte[] body;
            if (query != null && query.contains("force429")) {
              exchange.sendResponseHeaders(429, -1);
              exchange.close();
              return;
            }
            if (query != null && query.contains("force500")) {
              exchange.sendResponseHeaders(500, -1);
              exchange.close();
              return;
            }
            body =
                """
                {
                  "foods": [
                    {
                      "fdcId": 2262074,
                      "description": "NUTELLA",
                      "brandOwner": "Ferrero",
                      "gtinUpc": "009800860022",
                      "dataType": "Branded"
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
      SERVER.createContext(
          "/food/",
          exchange -> {
            String path = exchange.getRequestURI().getPath();
            byte[] body;
            if (path.contains("2262074")) {
              body =
                  """
                  {
                    "fdcId": 2262074,
                    "description": "NUTELLA",
                    "dataType": "Branded",
                    "foodNutrients": [
                      {
                        "amount": 80,
                        "nutrient": { "number": "301", "name": "Calcium, Ca", "unitName": "mg" }
                      }
                    ]
                  }
                  """
                      .getBytes(StandardCharsets.UTF_8);
            } else {
              exchange.sendResponseHeaders(404, -1);
              exchange.close();
              return;
            }
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

  @Autowired private FdcClient fdcClient;

  @AfterAll
  static void stop() {
    SERVER.stop(0);
  }

  @DynamicPropertySource
  static void fdcBaseUrl(DynamicPropertyRegistry registry) {
    registry.add("nutritrack.enrichment.fdc.base-url", () -> "http://127.0.0.1:" + PORT);
  }

  @Test
  void searchFoodsDeserializesViaRestClient() {
    List<FdcSearchHit> hits = fdcClient.searchFoods("nutella", List.of("Branded"), null, 10);
    assertThat(hits).singleElement().extracting(FdcSearchHit::fdcId).isEqualTo(2262074L);
  }

  @Test
  void getFoodMapsNutrients() {
    Optional<FdcFoodDetail> food = fdcClient.getFood(2262074L);
    assertThat(food).isPresent();
    assertThat(food.get().nutrients())
        .anySatisfy(n -> assertThat(n.code()).isEqualTo("calcium"));
  }

  @Test
  void search4xxReturnsEmpty() {
    assertThat(fdcClient.searchFoods("force429", List.of("Branded"), null, 10)).isEmpty();
  }
}
