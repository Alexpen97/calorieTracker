package com.nutritrack.food.service.search;

import static org.assertj.core.api.Assertions.assertThat;

import com.nutritrack.food.domain.Product;
import com.nutritrack.food.domain.ProductRepository;
import com.nutritrack.food.domain.ProductSource;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:candidate_searcher;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
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
class ProductCandidateSearcherTest {

  private final SearchQueryNormalizer normalizer = new SearchQueryNormalizer();

  @Autowired private ProductCandidateSearcher candidateSearcher;
  @Autowired private ProductRepository productRepository;

  @BeforeEach
  void cleanProducts() {
    productRepository.deleteAll();
  }

  @Test
  void h2TokenizedSearchRequiresEveryQueryToken() {
    Product match = saveProduct("Oat Milk Barista", "Oatly");
    Product secondMatch = saveProduct("Oat Milk Original", "Oatly");
    Product thirdMatch = saveProduct("Oat Milk Unsweetened", "Oatly");
    Product missingMilk = saveProduct("Oat Granola", "Breakfast Co");
    Product missingOat = saveProduct("Almond Milk", "Almond Co");

    List<Product> candidates = candidateSearcher.findCandidates(normalizer.normalize("oat milk"), 10);

    assertThat(candidates)
        .extracting(Product::getId)
        .contains(match.getId(), secondMatch.getId(), thirdMatch.getId());
    assertThat(candidates).extracting(Product::getId).doesNotContain(missingMilk.getId(), missingOat.getId());
  }

  @Test
  void h2FuzzyThinPathKeepsFirstTokenCandidatesWhenAndSearchIsThin() {
    Product match = saveProduct("Oat Chocolate Drink", "Cocoa Co");
    Product missingAnchorToken = saveProduct("Plain Chocolate Bar", "Cocoa Co");

    List<Product> candidates = candidateSearcher.findCandidates(normalizer.normalize("oat choclte"), 10);

    assertThat(candidates).extracting(Product::getId).contains(match.getId());
    assertThat(candidates).extracting(Product::getId).doesNotContain(missingAnchorToken.getId());
  }

  @Test
  void h2SingleTokenFuzzySearchFindsTypoCandidate() {
    Product match = saveProduct("Nutella", "Ferrero");
    Product unrelated = saveProduct("Chocolate Spread", "Acme");

    List<Product> candidates = candidateSearcher.findCandidates(normalizer.normalize("nutela"), 10);

    assertThat(candidates).extracting(Product::getId).contains(match.getId());
    assertThat(candidates).extracting(Product::getId).doesNotContain(unrelated.getId());
  }

  private Product saveProduct(String name, String brand) {
    Product product = new Product();
    product.setId(UUID.randomUUID());
    product.setSource(ProductSource.OFF);
    product.setName(name);
    product.setBrand(brand);
    product.refreshSearchDocument();
    return productRepository.saveAndFlush(product);
  }
}
