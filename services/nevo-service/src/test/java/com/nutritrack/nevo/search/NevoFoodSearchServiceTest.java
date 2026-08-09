package com.nutritrack.nevo.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.nutritrack.nevo.domain.NevoAlias;
import com.nutritrack.nevo.domain.NevoAliasRepository;
import com.nutritrack.nevo.domain.NevoFood;
import com.nutritrack.nevo.domain.NevoFoodRepository;
import com.nutritrack.nevo.domain.NevoNutrientValue;
import com.nutritrack.nevo.domain.NevoNutrientValueRepository;
import com.nutritrack.nevo.match.ProductNameNormalizer;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class NevoFoodSearchServiceTest {

  @Test
  void searchRanksVegetableAliasHitsBeforeSpiceNoiseAndIncludesNutrients() {
    NevoFoodRepository foodRepository = Mockito.mock(NevoFoodRepository.class);
    NevoNutrientValueRepository nutrientRepository = Mockito.mock(NevoNutrientValueRepository.class);
    NevoAliasRepository aliasRepository = Mockito.mock(NevoAliasRepository.class);
    ProductNameNormalizer normalizer = new ProductNameNormalizer(aliasRepository);
    NevoFoodSearchService service =
        new NevoFoodSearchService(foodRepository, nutrientRepository, normalizer);

    NevoAlias paprikaAlias = new NevoAlias();
    paprikaAlias.setId(UUID.randomUUID());
    paprikaAlias.setAliasTerm("paprika");
    paprikaAlias.setCanonicalTerm("sweet pepper");
    when(aliasRepository.findAll()).thenReturn(List.of(paprikaAlias));

    NevoFood powder = food("1229", "Paprika powder", "Paprikapoeder", "Herbs and spices", null);
    NevoFood greenPepper =
        food("31", "Sweet pepper green raw", "Paprika groene rauw", "Vegetables", "Paprika groen rauw");
    NevoFood redPepper =
        food("884", "Sweet pepper red raw", "Paprika rode rauw", "Vegetables", "Paprika rood rauw");
    when(foodRepository.searchByTerm("paprika", 50)).thenReturn(List.of(powder, greenPepper, redPepper));
    when(foodRepository.searchByTerm("sweet pepper", 50)).thenReturn(List.of(greenPepper, redPepper));
    when(nutrientRepository.findByNevoCode("31"))
        .thenReturn(List.of(nutrient("energy_kcal", "19", "kcal")));

    var response = service.search("paprika", 10);

    assertThat(response.query()).isEqualTo("paprika");
    assertThat(response.items()).extracting("nevoCode").containsExactly("31", "884", "1229");
    assertThat(response.items().getFirst().nutrients())
        .extracting("code")
        .containsExactly("energy_kcal");
  }

  @Test
  void searchHonorsLimitAfterRanking() {
    NevoFoodRepository foodRepository = Mockito.mock(NevoFoodRepository.class);
    NevoNutrientValueRepository nutrientRepository = Mockito.mock(NevoNutrientValueRepository.class);
    NevoAliasRepository aliasRepository = Mockito.mock(NevoAliasRepository.class);
    ProductNameNormalizer normalizer = new ProductNameNormalizer(aliasRepository);
    NevoFoodSearchService service =
        new NevoFoodSearchService(foodRepository, nutrientRepository, normalizer);

    when(aliasRepository.findAll()).thenReturn(List.of());
    when(foodRepository.searchByTerm("courgette", 50))
        .thenReturn(
            List.of(
                food("922", "Courgettes raw", "Courgette rauw", "Vegetables", null),
                food("966", "Courgettes boiled", "Courgette gekookt", "Vegetables", null)));

    assertThat(service.search("courgette", 1).items()).extracting("nevoCode").containsExactly("922");
  }

  private static NevoFood food(
      String nevoCode, String nameEn, String nameNl, String foodGroup, String synonym) {
    NevoFood food = new NevoFood();
    food.setNevoCode(nevoCode);
    food.setFoodNameEn(nameEn);
    food.setFoodNameNl(nameNl);
    food.setFoodGroup(foodGroup);
    food.setSynonym(synonym);
    food.setNevoVersion("NEVO-Online 2025 9.0");
    food.setSearchDocument(String.join(" ", nameEn, nameNl, foodGroup, synonym == null ? "" : synonym));
    return food;
  }

  private static NevoNutrientValue nutrient(String code, String amount, String unit) {
    NevoNutrientValue nutrient = new NevoNutrientValue();
    nutrient.setId(UUID.randomUUID());
    nutrient.setNutrientCode(code);
    nutrient.setAmountPer100g(new BigDecimal(amount));
    nutrient.setUnit(unit);
    return nutrient;
  }
}
