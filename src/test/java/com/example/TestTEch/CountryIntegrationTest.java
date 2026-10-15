package com.example.TestTEch;

import com.example.TestTEch.client.WorldBankClient;
import com.example.TestTEch.dto.CountryResponse;
import com.example.TestTEch.exception.ApiException;
import com.example.TestTEch.repository.CountryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class CountryIntegrationTest {
    @Autowired WebApplicationContext context;
    @Autowired CountryRepository repository;
    @MockitoBean WorldBankClient client;
    MockMvc mvc;

    @BeforeEach void setup() {
        repository.deleteAll();
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test void importsUpdatesListsAndFindsWithoutDuplicates() throws Exception {
        var data = new CountryResponse(null, "CO", "COL", "Colombia", "Bogota", "Region", "Income", null, null);
        when(client.fetch("CO")).thenReturn(data);
        when(client.fetch("COL")).thenReturn(new CountryResponse(null, "CO", "COL", "Colombia", "New capital", "Region", "Income", null, null));
        mvc.perform(get("/api/countries")).andExpect(status().isOk()).andExpect(content().json("[]"));
        var created = mvc.perform(post("/api/countries/import").param("code", "co"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.iso2Code").value("CO")).andReturn();
        mvc.perform(post("/api/countries/import").param("code", "COL"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.capital").value("New capital"));
        assertEquals(1, repository.count());
        mvc.perform(get(created.getResponse().getHeader("Location"))).andExpect(status().isOk()).andExpect(jsonPath("$.iso3Code").value("COL"));
        mvc.perform(get("/api/countries")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
    }

    @Test void returnsExpectedErrors() throws Exception {
        mvc.perform(post("/api/countries/import").param("code", "1")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        mvc.perform(post("/api/countries/import")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/countries/import").param("code", "")).andExpect(status().isBadRequest());
        when(client.fetch("ZZ")).thenThrow(new ApiException(HttpStatus.NOT_FOUND, "País inexistente"));
        mvc.perform(post("/api/countries/import").param("code", "ZZ")).andExpect(status().isNotFound()).andExpect(jsonPath("$.detail").value("País inexistente"));
        when(client.fetch("CO")).thenThrow(new ApiException(HttpStatus.BAD_GATEWAY, "Proveedor no disponible"));
        mvc.perform(post("/api/countries/import").param("code", "CO")).andExpect(status().isBadGateway());
        mvc.perform(get("/api/countries/999999")).andExpect(status().isNotFound());
        mvc.perform(get("/api/countries/invalid")).andExpect(status().isBadRequest());
        assertEquals(0, repository.count());
    }

    @Test void publishesOpenApiContractAndSwaggerUi() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Countries API"))
                .andExpect(jsonPath("$.paths['/api/countries/import'].post.parameters[0].name").value("code"))
                .andExpect(jsonPath("$.paths['/api/countries/import'].post.parameters[0].required").value(true))
                .andExpect(jsonPath("$.paths['/api/countries/import'].post.responses['201'].headers.Location").exists())
                .andExpect(jsonPath("$.paths['/api/countries/import'].post.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/api/countries/import'].post.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/api/countries/import'].post.responses['404']").exists())
                .andExpect(jsonPath("$.paths['/api/countries/import'].post.responses['502']").exists())
                .andExpect(jsonPath("$.paths['/api/countries'].get.responses['200'].content['application/json'].schema.type").value("array"))
                .andExpect(jsonPath("$.paths['/api/countries/{id}'].get.responses['404']").exists());
        mvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
        verifyNoInteractions(client);
    }
}
