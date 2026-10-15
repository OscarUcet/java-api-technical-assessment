package com.example.TestTEch.model;

import com.example.TestTEch.dto.CountryResponse;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "countries", uniqueConstraints = @UniqueConstraint(name = "uk_country_iso2", columnNames = "iso2_code"))
public class Country {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "iso2_code", nullable = false, length = 2)
    private String iso2Code;
    @Column(nullable = false, length = 3)
    private String iso3Code;
    @Column(nullable = false)
    private String name;
    private String capital;
    private String region;
    private String incomeLevel;
    @Column(precision = 12, scale = 8)
    private BigDecimal latitude;
    @Column(precision = 12, scale = 8)
    private BigDecimal longitude;

    public Country() {}

    public void update(CountryResponse data) {
        iso2Code = data.iso2Code();
        iso3Code = data.iso3Code();
        name = data.name();
        capital = data.capital();
        region = data.region();
        incomeLevel = data.incomeLevel();
        latitude = data.latitude();
        longitude = data.longitude();
    }

    public CountryResponse response() {
        return new CountryResponse(id, iso2Code, iso3Code, name, capital, region, incomeLevel, latitude, longitude);
    }
}
