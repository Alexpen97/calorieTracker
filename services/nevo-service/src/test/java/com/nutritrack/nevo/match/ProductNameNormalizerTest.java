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

  @Test
  void expandSearchTermsIncludesWholeStringAndTokenAliases() {
    NevoAliasRepository aliases = Mockito.mock(NevoAliasRepository.class);
    NevoAlias paprika = alias("paprika", "sweet pepper");
    NevoAlias bellPepper = alias("bell pepper", "sweet pepper");
    Mockito.when(aliases.findAll()).thenReturn(List.of(paprika, bellPepper));

    ProductNameNormalizer normalizer = new ProductNameNormalizer(aliases);

    assertThat(normalizer.expandSearchTerms("paprika")).containsExactly("paprika", "sweet pepper");
    assertThat(normalizer.expandSearchTerms("red bell pepper"))
        .containsExactly("red bell pepper", "red sweet pepper");
  }

  private static NevoAlias alias(String aliasTerm, String canonicalTerm) {
    NevoAlias alias = new NevoAlias();
    alias.setId(UUID.randomUUID());
    alias.setAliasTerm(aliasTerm);
    alias.setCanonicalTerm(canonicalTerm);
    return alias;
  }
}
