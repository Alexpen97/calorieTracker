package com.nutritrack.nevo.config;

import com.nutritrack.nevo.translate.NoOpTranslationClient;
import com.nutritrack.nevo.translate.RestLibreTranslateClient;
import com.nutritrack.nevo.translate.TranslationClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class TranslationClientConfig {

  @Bean
  TranslationClient translationClient(NevoProperties properties) {
    NevoProperties.Translate translate = properties.translate();
    if (!translate.enabled()) {
      return new NoOpTranslationClient();
    }
    JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory();
    factory.setReadTimeout(translate.timeout());
    RestClient client =
        RestClient.builder().baseUrl(translate.baseUrl()).requestFactory(factory).build();
    return new RestLibreTranslateClient(client, translate);
  }
}
