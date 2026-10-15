package com.example.TestTEch;

import com.example.TestTEch.client.WorldBankClient;
import com.example.TestTEch.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import java.net.SocketTimeoutException;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class WorldBankClientTest {
    MockRestServiceServer server;
    WorldBankClient client;
    static final String COUNTRY = """
        [{"total":1},[{"id":"COL","iso2Code":"CO","name":"Colombia",
        "capitalCity":"Bogota","region":{"value":"Latin America & Caribbean "},
        "incomeLevel":{"value":"Upper middle income"},"latitude":"4.60987","longitude":"-74.082"}]]
        """;

    @BeforeEach void setup() {
        var builder = RestClient.builder().baseUrl("https://example.test/v2");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new WorldBankClient(builder.build());
    }

    void response(String body) {
        server.expect(requestTo("https://example.test/v2/country/CO?format=json"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    }

    @Test void mapsNestedResponse() {
        response(COUNTRY);
        var country = client.fetch("CO");
        assertEquals("COL", country.iso3Code());
        assertEquals("Latin America & Caribbean", country.region());
        assertEquals("4.60987", country.latitude().toPlainString());
        server.verify();
    }

    @Test void mapsErrorInsideHttp200To404() {
        response("[{\"message\":[{\"id\":\"120\",\"key\":\"Invalid value\"}]}]");
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ApiException.class, () -> client.fetch("CO")).status());
    }

    @Test void mapsEmptyResultTo404() {
        response("[{\"total\":0},null]");
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ApiException.class, () -> client.fetch("CO")).status());
    }

    @Test void rejectsMalformedResponse() {
        response("{\"unexpected\":true}");
        assertEquals(HttpStatus.BAD_GATEWAY, assertThrows(ApiException.class, () -> client.fetch("CO")).status());
    }

    @Test void rejectsInvalidJson() {
        response("not json");
        assertEquals(HttpStatus.BAD_GATEWAY, assertThrows(ApiException.class, () -> client.fetch("CO")).status());
    }

    @Test void rejectsIncompleteCountry() {
        response("[{\"total\":1},[{\"id\":\"COL\",\"iso2Code\":\"CO\"}]]");
        assertEquals(HttpStatus.BAD_GATEWAY, assertThrows(ApiException.class, () -> client.fetch("CO")).status());
    }

    @Test void mapsHttpFailureTo502() {
        server.expect(requestTo("https://example.test/v2/country/CO?format=json")).andRespond(withServerError());
        assertEquals(HttpStatus.BAD_GATEWAY, assertThrows(ApiException.class, () -> client.fetch("CO")).status());
    }

    @Test void mapsTimeoutTo502() {
        server.expect(requestTo("https://example.test/v2/country/CO?format=json")).andRespond(withException(new SocketTimeoutException()));
        assertEquals(HttpStatus.BAD_GATEWAY, assertThrows(ApiException.class, () -> client.fetch("CO")).status());
    }
}
