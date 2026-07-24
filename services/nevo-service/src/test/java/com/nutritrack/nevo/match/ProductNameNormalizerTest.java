package com.nutritrack.nevo.match;

import static org.assertj.core.api.Assertions.assertThat;

import com.nutritrack.nevo.domain.NevoAlias;
import com.nutritrack.nevo.domain.NevoAliasRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ProductNameNormalizerTest {

  @Test
  void stripsBrandPackSizeAndMarketingWordsButKeepsModifiers() {
    NevoAliasRepository aliases = Mockito.mock(NevoAliasRepository.class);
    NevoAlias yoghurt = new NevoAlias();
    yoghurt.setId(UUID.randomUUID());
    yoghurt.setAliasTerm("yoghurt");
    yoghurt.setCanonicalTerm("yogurt");
    Mockito.when(aliases.findAll()).thenReturn(List.of(yoghurt));

    ProductNameNormalizer normalizer = new ProductNameNormalizer(aliases);
    var result =
        normalizer.normalize(
            "AH Biologisch Griekse stijl yoghurt 0% vet 450g",
            "AH",
            List.of("en:yogurts"),
            "milk, cultures");

    assertThat(result.cleanedName()).contains("griekse").contains("yogurt");
    assertThat(result.cleanedName()).doesNotContain("450").doesNotContain("biologisch");
    assertThat(result.queryTerms()).isNotEmpty();
  }
}
