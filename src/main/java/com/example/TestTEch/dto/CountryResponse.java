package com.example.TestTEch.dto;

import java.math.BigDecimal;

public record CountryResponse(Long id, String iso2Code, String iso3Code, String name,
        String capital, String region, String incomeLevel, BigDecimal latitude, BigDecimal longitude) {}
