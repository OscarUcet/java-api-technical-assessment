package com.example.TestTEch.service;

import com.example.TestTEch.client.WorldBankClient;
import com.example.TestTEch.dto.CountryResponse;
import com.example.TestTEch.exception.ApiException;
import com.example.TestTEch.model.Country;
import com.example.TestTEch.repository.CountryRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Locale;

@Service
public class CountryService {
    private static final Logger log = LoggerFactory.getLogger(CountryService.class);
    private final WorldBankClient client;
    private final CountryRepository repository;
    private final TransactionTemplate transactions;
    private final Object importLock = new Object();

    public CountryService(WorldBankClient client, CountryRepository repository, TransactionTemplate transactions) {
        this.client = client;
        this.repository = repository;
        this.transactions = transactions;
    }

    public ImportResult importCountry(String code) {
        if (code == null || !code.matches("[a-zA-Z]{2,3}")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El código debe contener 2 o 3 letras");
        }
        CountryResponse data = client.fetch(code.toUpperCase(Locale.ROOT));
        // Hold the lock through transaction commit: CO and COL must update the same row.
        synchronized (importLock) {
            return transactions.execute(status -> {
                var existing = repository.findByIso2Code(data.iso2Code());
                Country country = existing.orElseGet(Country::new);
                country.update(data);
                Country saved = repository.saveAndFlush(country);
                log.info("Country {} {}", data.iso2Code(), existing.isEmpty() ? "created" : "updated");
                return new ImportResult(saved.response(), existing.isEmpty());
            });
        }
    }

    public List<CountryResponse> list() {
        return repository.findAll(Sort.by("iso2Code")).stream().map(Country::response).toList();
    }

    public CountryResponse find(Long id) {
        return repository.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "País almacenado no encontrado")).response();
    }

    public record ImportResult(CountryResponse country, boolean created) {}
}
