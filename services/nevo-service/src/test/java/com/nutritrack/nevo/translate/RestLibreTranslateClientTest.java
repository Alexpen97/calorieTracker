package com.nutritrack.nevo.translate;

import static org.assertj.core.api.Assertions.assertThat;

import com.nutritrack.nevo.config.NevoProperties;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestClient;

class RestLibreTranslateClientTest {

  @Test
  void translatesViaLibreTranslateApi() {
    RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:5000");
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    server
        .expect(MockRestRequestMatchers.requestTo("http://localhost:5000/translate"))
        .andExpect(MockRestRequestMatchers.method(org.springframework.http.HttpMethod.POST))
        .andExpect(MockRestRequestMatchers.jsonPath("$.q").value("magere kwark met aardbei"))
        .andExpect(MockRestRequestMatchers.jsonPath("$.source").value("auto"))
        .andExpect(MockRestRequestMatchers.jsonPath("$.target").value("en"))
        .andRespond(
            MockRestResponseCreators.withSuccess(
                "{\"translatedText\":\"low fat quark with strawberry\"}",
                MediaType.APPLICATION_JSON));

    NevoProperties.Translate settings =
        new NevoProperties.Translate(true, "http://localhost:5000", "auto", "en", Duration.ofSeconds(3));
    Optional<String> translated =
        new RestLibreTranslateClient(builder.build(), settings)
            .translate("magere kwark met aardbei");

    assertThat(translated).contains("low fat quark with strawberry");
    server.verify();
  }

  @Test
  void disabledReturnsEmpty() {
    NevoProperties.Translate settings =
        new NevoProperties.Translate(
            false, "http://localhost:5000", "auto", "en", Duration.ofSeconds(3));
    assertThat(
            new RestLibreTranslateClient(RestClient.create(), settings).translate("kwark"))
        .isEmpty();
  }
}
