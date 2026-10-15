package com.example.TestTEch;

import com.example.TestTEch.client.WorldBankClient;
import com.example.TestTEch.dto.CountryResponse;
import com.example.TestTEch.exception.ApiException;
import com.example.TestTEch.model.Country;
import com.example.TestTEch.repository.CountryRepository;
import com.example.TestTEch.service.CountryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CountryServiceTest {
    @Mock WorldBankClient client;
    @Mock CountryRepository repository;
    @Mock TransactionTemplate transactions;
    CountryService service;
    final CountryResponse data = new CountryResponse(null, "CO", "COL", "Colombia", "Bogota", "Region", "Income", null, null);

    @BeforeEach void setup() { service = new CountryService(client, repository, transactions); }

    @SuppressWarnings("unchecked")
    void transactionRuns() {
        when(transactions.execute(any())).thenAnswer(invocation ->
                ((org.springframework.transaction.support.TransactionCallback<Object>) invocation.getArgument(0)).doInTransaction(null));
    }

    @Test void createsCountryAndNormalizesCode() {
        transactionRuns();
        when(client.fetch("CO")).thenReturn(data);
        when(repository.findByIso2Code("CO")).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        var result = service.importCountry("co");
        assertTrue(result.created());
        assertEquals("Colombia", result.country().name());
    }

    @Test void updatesExistingCountryUsingIso2() {
        transactionRuns();
        var existing = new Country();
        existing.update(data);
        when(client.fetch("COL")).thenReturn(data);
        when(repository.findByIso2Code("CO")).thenReturn(Optional.of(existing));
        when(repository.saveAndFlush(existing)).thenReturn(existing);
        assertFalse(service.importCountry("COL").created());
        verify(repository).saveAndFlush(existing);
    }

    @Test void rejectsInvalidCodeBeforeCallingExternalApi() {
        for (String code : new String[]{"", "1", "ABCD", " CO", "C1", "ññ"}) {
            assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ApiException.class, () -> service.importCountry(code)).status());
        }
        verifyNoInteractions(client, repository, transactions);
    }

    @Test void doesNotPersistWhenExternalCountryDoesNotExist() {
        when(client.fetch("ZZ")).thenThrow(new ApiException(HttpStatus.NOT_FOUND, "missing"));
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ApiException.class, () -> service.importCountry("ZZ")).status());
        verifyNoInteractions(repository, transactions);
    }

    @Test void propagatesUpstreamFailureWithoutPersisting() {
        when(client.fetch("CO")).thenThrow(new ApiException(HttpStatus.BAD_GATEWAY, "unavailable"));
        assertEquals(HttpStatus.BAD_GATEWAY, assertThrows(ApiException.class, () -> service.importCountry("CO")).status());
        verifyNoInteractions(repository, transactions);
    }
}
