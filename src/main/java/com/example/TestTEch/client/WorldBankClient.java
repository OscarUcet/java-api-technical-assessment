package com.example.TestTEch.client;

import com.example.TestTEch.dto.CountryResponse;
import com.example.TestTEch.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;

@Component
public class WorldBankClient {
    private final RestClient client;

    public WorldBankClient(RestClient worldBankRestClient) { this.client = worldBankRestClient; }

    public CountryResponse fetch(String code) {
        try {
            JsonNode body = client.get().uri("/country/{code}?format=json", code).retrieve().body(JsonNode.class);
            if (body == null || !body.isArray() || body.isEmpty() || !body.get(0).isObject()) throw malformed();
            JsonNode messages = body.get(0).path("message");
            if (messages.isArray()) {
                for (JsonNode message : messages) {
                    if ("120".equals(message.path("id").asText())) throw missing();
                }
                throw malformed();
            }
            if (body.size() != 2 || !body.get(0).path("total").isIntegralNumber()) throw malformed();
            JsonNode countries = body.get(1);
            if (body.get(0).path("total").asInt() == 0 && (countries.isNull() || countries.isArray() && countries.isEmpty())) throw missing();
            if (!countries.isArray() || countries.size() != 1) throw malformed();
            JsonNode country = countries.get(0);
            String iso2 = required(country, "iso2Code");
            String iso3 = required(country, "id");
            if (!iso2.matches("[A-Z]{2}") || !iso3.matches("[A-Z]{3}") || !(code.equals(iso2) || code.equals(iso3))) throw malformed();
            if ("Aggregates".equals(country.path("region").path("value").asText())) throw missing();
            return new CountryResponse(null, iso2, iso3, required(country, "name"),
                    text(country, "capitalCity"), required(country.path("region"), "value"),
                    required(country.path("incomeLevel"), "value"), coordinate(country, "latitude", 90), coordinate(country, "longitude", 180));
        } catch (RestClientException | IllegalArgumentException ex) {
            throw malformed();
        }
    }

    private static String required(JsonNode node, String field) {
        String value = text(node, field);
        if (value == null || value.isBlank()) throw malformed();
        return value;
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) return null;
        if (!value.isTextual()) throw malformed();
        return value.asText().trim();
    }

    private static BigDecimal coordinate(JsonNode node, String field, int limit) {
        String value = text(node, field);
        if (value == null || value.isEmpty()) return null;
        BigDecimal number = new BigDecimal(value);
        if (number.abs().compareTo(BigDecimal.valueOf(limit)) > 0) throw malformed();
        return number;
    }

    private static ApiException missing() { return new ApiException(HttpStatus.NOT_FOUND, "País no encontrado en el Banco Mundial"); }
    private static ApiException malformed() { return new ApiException(HttpStatus.BAD_GATEWAY, "No fue posible obtener una respuesta válida del Banco Mundial"); }
}
