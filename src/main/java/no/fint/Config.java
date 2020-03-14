package no.fint;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class Config {
    @Bean
    public WebClient webClient(
            @Value("${fint.audit.baseurl}") String baseurl,
            @Value("${fint.audit.username}") String username,
            @Value("${fint.audit.password}") String password) {
        return WebClient
                .builder()
                .baseUrl(baseurl)
                .defaultHeaders(headers -> headers.setBasicAuth(username, password))
                .build();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .setDateFormat(new StdDateFormat())
                .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
                .enable(SerializationFeature.INDENT_OUTPUT);
    }
}
