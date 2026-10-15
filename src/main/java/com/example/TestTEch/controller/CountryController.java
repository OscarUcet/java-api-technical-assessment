package com.example.TestTEch.controller;

import com.example.TestTEch.dto.CountryResponse;
import com.example.TestTEch.service.CountryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/countries")
@Tag(name = "Países", description = "Importación y consulta de países almacenados")
public class CountryController {
    private final CountryService service;
    public CountryController(CountryService service) { this.service = service; }

    @PostMapping("/import")
    @Operation(summary = "Importar o actualizar un país",
            description = "Acepta un código ISO alpha-2 o alpha-3, sin espacios y sin distinguir mayúsculas. ISO alpha-2 evita duplicados.")
    @ApiResponse(responseCode = "201", description = "País creado",
            headers = @Header(name = "Location", description = "Ruta del país almacenado", schema = @Schema(type = "string")),
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CountryResponse.class)))
    @ApiResponse(responseCode = "200", description = "País existente actualizado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CountryResponse.class)))
    @ApiResponse(responseCode = "400", description = "Código ausente, vacío o con formato inválido",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "País no encontrado en el Banco Mundial",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "502", description = "Proveedor no disponible, timeout o respuesta no interpretable",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<CountryResponse> importCountry(
            @Parameter(description = "Código ISO del país", example = "CO", required = true,
                    schema = @Schema(type = "string", pattern = "[a-zA-Z]{2,3}", minLength = 2, maxLength = 3))
            @RequestParam String code) {
        var result = service.importCountry(code);
        return result.created()
                ? ResponseEntity.created(URI.create("/api/countries/" + result.country().id())).body(result.country())
                : ResponseEntity.ok(result.country());
    }

    @GetMapping(produces = "application/json")
    @Operation(summary = "Listar países almacenados", description = "Ordenados por ISO alpha-2. Devuelve un arreglo vacío si no hay registros.")
    @ApiResponse(responseCode = "200", description = "Listado de países almacenados")
    public List<CountryResponse> list() { return service.list(); }

    @GetMapping(value = "/{id}", produces = "application/json")
    @Operation(summary = "Consultar un país por su ID local")
    @ApiResponse(responseCode = "200", description = "País almacenado")
    @ApiResponse(responseCode = "400", description = "El ID debe ser un número entero",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "ID no encontrado",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public CountryResponse find(@Parameter(description = "ID generado al guardar el país", example = "1")
            @PathVariable Long id) { return service.find(id); }
}
