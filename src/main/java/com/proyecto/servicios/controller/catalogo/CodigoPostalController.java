package com.proyecto.servicios.controller.catalogo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.model.catalogo.CatalogoItemDto;
import com.proyecto.servicios.model.common.GenericResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.Serializable;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/catalogos")
@Tag(name = "Catálogo de Códigos Postales y Colonias", description = "Resolución de Entidad Federativa, Municipio y Catálogo dinámico de Colonias")
public class CodigoPostalController {

    private static final Set<String> PALABRAS_RESERVADAS = Set.of(
            "EXECUTE", "EXEC", "SELECT", "INSERT", "UPDATE", "DELETE", "DROP", "ALTER",
            "CREATE", "TRUNCATE", "SCRIPT", "JAVASCRIPT", "NULL", "UNDEFINED", "TRUE",
            "FALSE", "SI", "NO", "EVAL", "WHERE", "FROM", "TABLE", "JOIN", "UNION",
            "GRANT", "REVOKE", "FETCH", "FUNCTION", "PROCEDURE", "DECLARE"
    );

    private static final List<String> COLONIAS_BASE = Arrays.asList(
            "Centro", "Zona Centro", "Del Valle", "Reforma", "Juarez",
            "Hidalgo", "Morelos", "Moderna", "Obrera", "Jardines",
            "Independencia", "Lomas", "Vista Hermosa", "Alameda", "Bellavista"
    );

    private static final Map<String, List<String>> COLONIAS_POR_CP = new HashMap<>();
    private static final Map<String, List<String>> COLONIAS_POR_MUNICIPIO = new HashMap<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DomicilioCpDto implements Serializable {
        private String codigoPostal;
        private String pais;
        private String estado;
        private String municipio;

        public DomicilioCpDto(String codigoPostal, String estado, String municipio) {
            this(codigoPostal, "México", estado, municipio);
        }
    }

    private static final Map<String, DomicilioCpDto> CP_MAP = new HashMap<>();

    static {
        // Códigos postales comunes mexicanos
        CP_MAP.put("64000", new DomicilioCpDto("64000", "Nuevo Leon", "Monterrey"));
        CP_MAP.put("66220", new DomicilioCpDto("66220", "Nuevo Leon", "San Pedro Garza Garcia"));
        CP_MAP.put("06000", new DomicilioCpDto("06000", "Ciudad de Mexico", "Cuauhtemoc"));
        CP_MAP.put("06600", new DomicilioCpDto("06600", "Ciudad de Mexico", "Cuauhtemoc"));
        CP_MAP.put("01000", new DomicilioCpDto("01000", "Ciudad de Mexico", "Alvaro Obregon"));
        CP_MAP.put("03100", new DomicilioCpDto("03100", "Ciudad de Mexico", "Benito Juarez"));
        CP_MAP.put("44100", new DomicilioCpDto("44100", "Jalisco", "Guadalajara"));
        CP_MAP.put("45050", new DomicilioCpDto("45050", "Jalisco", "Zapopan"));
        CP_MAP.put("72000", new DomicilioCpDto("72000", "Puebla", "Puebla"));
        CP_MAP.put("76000", new DomicilioCpDto("76000", "Queretaro", "Queretaro"));
        CP_MAP.put("77500", new DomicilioCpDto("77500", "Quintana Roo", "Cancun"));
        CP_MAP.put("97000", new DomicilioCpDto("97000", "Yucatan", "Merida"));
        CP_MAP.put("22000", new DomicilioCpDto("22000", "Baja California", "Tijuana"));
        CP_MAP.put("31000", new DomicilioCpDto("31000", "Chihuahua", "Chihuahua"));
        CP_MAP.put("25000", new DomicilioCpDto("25000", "Coahuila", "Saltillo"));

        COLONIAS_POR_CP.put("37900", Arrays.asList("San Luis de la Paz Centro", "La Banda", "El Calvario", "La Espiga", "Montebello", "Del Bosque", "Magisterial"));
        COLONIAS_POR_CP.put("37914", Arrays.asList("Mision de Chichimecas", "La Joya", "San Isidro", "Lomas de San Luis"));
        COLONIAS_POR_CP.put("37800", Arrays.asList("Centro", "Lindavista", "San Juan", "Mariano Balleza", "Independencia", "Fraccionamiento Guadalupe"));
        COLONIAS_POR_CP.put("37700", Arrays.asList("Zona Centro", "San Antonio", "Guadalupe", "Olimpo", "Los Frailes"));
        COLONIAS_POR_CP.put("36000", Arrays.asList("Zona Centro", "Marfil", "Noria Alta", "San Javier", "Pastita"));
        COLONIAS_POR_CP.put("37000", Arrays.asList("Leon Centro", "Andrade", "La Martinica", "Jardines del Moral", "Obregon"));
        COLONIAS_POR_CP.put("64000", Arrays.asList("Centro", "Obispado", "Mitras Centro", "Del Norte", "Chepevera"));
        COLONIAS_POR_CP.put("06000", Arrays.asList("Centro", "Juarez", "Roma Norte", "Condesa", "Doctores"));
        COLONIAS_POR_CP.put("06600", Arrays.asList("Juarez", "Cuauhtemoc", "Roma Norte", "Centro"));
        COLONIAS_POR_CP.put("44100", Arrays.asList("Guadalajara Centro", "Americana", "Moderna", "Arcos Vallarta"));

        COLONIAS_POR_MUNICIPIO.put("SAN LUIS DE LA PAZ", Arrays.asList(
                "San Luis de la Paz Centro", "La Banda", "El Calvario", "La Espiga", "Montebello",
                "Del Bosque", "Magisterial", "Mision de Chichimecas", "La Joya", "San Isidro", "Lomas de San Luis"));
        COLONIAS_POR_MUNICIPIO.put("DOLORES HIDALGO CUNA DE LA INDEPENDENCIA NACIONAL", Arrays.asList(
                "Centro", "Lindavista", "San Juan", "Mariano Balleza", "Independencia", "Fraccionamiento Guadalupe", "San Pablo"));
        COLONIAS_POR_MUNICIPIO.put("SAN MIGUEL DE ALLENDE", Arrays.asList(
                "Zona Centro", "San Antonio", "Guadalupe", "Olimpo", "Los Frailes", "Ignacio Ramirez", "La Lejona"));
        COLONIAS_POR_MUNICIPIO.put("GUANAJUATO", Arrays.asList(
                "Zona Centro", "Marfil", "Noria Alta", "San Javier", "Pastita", "Embajadoras", "Valenciana"));
        COLONIAS_POR_MUNICIPIO.put("LEON", Arrays.asList(
                "Leon Centro", "Andrade", "La Martinica", "Jardines del Moral", "Obregon", "San Isidro", "Coecillo"));
        COLONIAS_POR_MUNICIPIO.put("IRAPUATO", Arrays.asList(
                "Zona Centro", "Las Reynas", "Moderna", "Jardines de Irapuato", "San Roque"));
        COLONIAS_POR_MUNICIPIO.put("CELAYA", Arrays.asList(
                "Celaya Centro", "Alameda", "Jardines de Celaya", "Las Insurgentes", "Renacimiento"));
        COLONIAS_POR_MUNICIPIO.put("SAN JOSE ITURBIDE", Arrays.asList(
                "Zona Centro", "Loma de Guadalupe", "El Capulin", "Prados del Rosario", "Jardines"));
        COLONIAS_POR_MUNICIPIO.put("DOCTOR MORA", Arrays.asList(
                "Zona Centro", "Loma Bonita", "Ejido Vagui", "El Lindero"));
        COLONIAS_POR_MUNICIPIO.put("VICTORIA", Arrays.asList(
                "Victoria Centro", "El Cerrito", "Loma de Buenavista", "San Juan"));
        COLONIAS_POR_MUNICIPIO.put("SAN DIEGO DE LA UNION", Arrays.asList(
                "Zona Centro", "San Juan", "El Calvario", "La Presita"));
        COLONIAS_POR_MUNICIPIO.put("MONTERREY", Arrays.asList(
                "Centro", "Obispado", "Mitras Centro", "Del Norte", "Chepevera", "Tecnologico"));
        COLONIAS_POR_MUNICIPIO.put("CUAUHTEMOC", Arrays.asList(
                "Centro", "Juarez", "Roma Norte", "Roma Sur", "Condesa", "Doctores"));
        COLONIAS_POR_MUNICIPIO.put("GUADALAJARA", Arrays.asList(
                "Guadalajara Centro", "Americana", "Moderna", "Arcos Vallarta", "Providencia"));
        COLONIAS_POR_MUNICIPIO.put("QUERETARO", Arrays.asList(
                "Centro", "Alamos", "Carretas", "Milenio", "Juriquilla"));
    }

    @GetMapping(value = "/colonias", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Listar catálogo dinámico de colonias correspondientes al código postal y municipio")
    public ResponseEntity<GenericResponse<List<CatalogoItemDto>>> listarColonias(
            @RequestParam(name = "cp", required = false) String cp,
            @RequestParam(name = "municipio", required = false) String municipio) {
        String cpLimpio = cp != null ? cp.trim() : "";
        String municipioLimpio = municipio != null ? municipio.trim() : "";
        Set<String> nombresUnicos = new LinkedHashSet<>();

        String municipioObjetivo = municipioLimpio;
        if (municipioObjetivo.isEmpty() && cpLimpio.matches("^\\d{5}$")) {
            DomicilioCpDto dto = CP_MAP.get(cpLimpio);
            if (dto != null) {
                municipioObjetivo = dto.getMunicipio();
            } else {
                municipioObjetivo = resolverMunicipioSepomex(Integer.parseInt(cpLimpio), cpLimpio.substring(0, 2));
            }
        }

        if (cpLimpio.matches("^\\d{5}$")) {
            nombresUnicos.addAll(consultarColoniasSepomexPorCp(cpLimpio));
        }

        if (!municipioObjetivo.isBlank()) {
            nombresUnicos.addAll(consultarColoniasSepomexPorMunicipio(municipioObjetivo));
        }

        if (cpLimpio.matches("^\\d{5}$")) {
            List<String> localesCp = COLONIAS_POR_CP.get(cpLimpio);
            if (localesCp != null) {
                for (String col : localesCp) {
                    String sanitizada = sanitizarNombreColonia(col);
                    if (esColoniaValida(sanitizada)) {
                        nombresUnicos.add(sanitizada);
                    }
                }
            }
        }

        if (!municipioObjetivo.isBlank()) {
            String claveMun = normalizarClaveMunicipio(municipioObjetivo);
            List<String> localesMun = COLONIAS_POR_MUNICIPIO.get(claveMun);
            if (localesMun != null) {
                for (String col : localesMun) {
                    String sanitizada = sanitizarNombreColonia(col);
                    if (esColoniaValida(sanitizada)) {
                        nombresUnicos.add(sanitizada);
                    }
                }
            }
        }

        // Solo si se solicitó un CP o municipio válido y no hubo coincidencias remotas ni locales, dar respaldo mínimo municipal
        if (nombresUnicos.isEmpty() && (cpLimpio.matches("^\\d{5}$") || !municipioObjetivo.isBlank())) {
            String sufijoMun = !municipioObjetivo.isBlank() ? sanitizarNombreColonia(municipioObjetivo + " Centro") : "Centro";
            if (esColoniaValida(sufijoMun)) {
                nombresUnicos.add(sufijoMun);
            }
            nombresUnicos.add("Zona Centro");
        }

        List<CatalogoItemDto> items = new ArrayList<>();
        long idx = 1L;
        LocalDateTime ahora = LocalDateTime.now();
        for (String nombre : nombresUnicos) {
            items.add(CatalogoItemDto.builder()
                    .id(idx)
                    .codigo(String.format("COL%03d", idx))
                    .descripcion(nombre)
                    .activo(true)
                    .fechaCreacion(ahora)
                    .build());
            idx++;
        }

        return ResponseEntity.ok(GenericResponse.exito(items, "Catálogo de colonias consultado exitosamente"));
    }

    private String normalizarClaveMunicipio(String texto) {
        if (texto == null) return "";
        String sinAcentos = Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return sinAcentos.replaceAll("\\s+", " ").toUpperCase();
    }

    private List<String> consultarColoniasSepomexPorCp(String cp) {
        return ejecutarConsultaSepomex("https://sepomex.kurenn.dev/api/v1/zip_codes?zip_code=" + cp + "&per_page=100", null);
    }

    private List<String> consultarColoniasSepomexPorMunicipio(String municipio) {
        String encoded = URLEncoder.encode(municipio.trim(), StandardCharsets.UTF_8);
        return ejecutarConsultaSepomex(
                "https://sepomex.kurenn.dev/api/v1/zip_codes?municipality=" + encoded + "&per_page=100",
                normalizarClaveMunicipio(municipio)
        );
    }

    private List<String> ejecutarConsultaSepomex(String url, String filtroMunicipioClave) {
        List<String> resultado = new ArrayList<>();
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(3))
                    .build();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(4))
                    .GET()
                    .build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200 && resp.body() != null) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(resp.body());
                JsonNode zipCodes = root.get("zip_codes");
                if (zipCodes != null && zipCodes.isArray()) {
                    for (JsonNode item : zipCodes) {
                        if (filtroMunicipioClave != null && !filtroMunicipioClave.isEmpty()) {
                            JsonNode mnpio = item.get("d_mnpio");
                            if (mnpio != null && !normalizarClaveMunicipio(mnpio.asText()).equals(filtroMunicipioClave)) {
                                continue;
                            }
                        }
                        JsonNode asenta = item.get("d_asenta");
                        if (asenta != null && !asenta.asText().isBlank()) {
                            String limpia = sanitizarNombreColonia(asenta.asText());
                            if (esColoniaValida(limpia)) {
                                resultado.add(limpia);
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // Fallback limpio al catálogo municipal del servidor cuando no hay enlace externo
        }
        return resultado;
    }

    private String sanitizarNombreColonia(String raw) {
        if (raw == null) return "";
        String limpia = raw.replaceAll("[^a-zA-ZáéíóúÁÉÍÓÚñÑüÜ ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (limpia.length() > 60) {
            limpia = limpia.substring(0, 60).trim();
        }
        return limpia;
    }

    private boolean esColoniaValida(String val) {
        if (val == null || val.length() < 3 || val.length() > 60) return false;
        if (!val.matches("^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ ]+$")) return false;
        for (String palabra : val.split("\\s+")) {
            if (PALABRAS_RESERVADAS.contains(palabra.toUpperCase())) {
                return false;
            }
        }
        return true;
    }

    @GetMapping(value = "/codigo-postal/{cp}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Resolver País, Estado y Municipio a partir del Código Postal")
    public ResponseEntity<GenericResponse<DomicilioCpDto>> resolverCp(@PathVariable String cp) {
        String cpLimpio = cp != null ? cp.trim() : "";

        if (!cpLimpio.matches("^\\d{5}$")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(GenericResponse.error(HttpStatus.NOT_FOUND.value(), "Código postal inválido. Debe contener 5 dígitos."));
        }

        String prefijo = cpLimpio.substring(0, 2);
        String estadoLocal = switch (prefijo) {
            case "01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16" -> "Ciudad de Mexico";
            case "20" -> "Aguascalientes";
            case "21", "22" -> "Baja California";
            case "23" -> "Baja California Sur";
            case "24" -> "Campeche";
            case "25", "26", "27" -> "Coahuila";
            case "28" -> "Colima";
            case "29", "30" -> "Chiapas";
            case "31", "32", "33" -> "Chihuahua";
            case "34", "35" -> "Durango";
            case "36", "37", "38" -> "Guanajuato";
            case "39", "40", "41" -> "Guerrero";
            case "42", "43" -> "Hidalgo";
            case "44", "45", "46", "47", "48", "49" -> "Jalisco";
            case "50", "51", "52", "53", "54", "55", "56", "57" -> "Estado de Mexico";
            case "58", "59", "60", "61" -> "Michoacan";
            case "62" -> "Morelos";
            case "63" -> "Nayarit";
            case "64", "65", "66", "67" -> "Nuevo Leon";
            case "68", "69", "70", "71" -> "Oaxaca";
            case "72", "73", "74", "75" -> "Puebla";
            case "76" -> "Queretaro";
            case "77" -> "Quintana Roo";
            case "78", "79" -> "San Luis Potosi";
            case "80", "81", "82" -> "Sinaloa";
            case "83", "84", "85" -> "Sonora";
            case "86" -> "Tabasco";
            case "87", "88", "89" -> "Tamaulipas";
            case "90" -> "Tlaxcala";
            case "91", "92", "93", "94", "95", "96" -> "Veracruz";
            case "97" -> "Yucatan";
            case "98", "99" -> "Zacatecas";
            default -> null;
        };

        if (estadoLocal == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(GenericResponse.error(HttpStatus.NOT_FOUND.value(), "No se encontraron resultados para el código postal ingresado."));
        }

        // Consultar SEPOMEX para validar existencia exacta y obtener Estado y Municipio oficiales
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(3))
                    .build();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://sepomex.kurenn.dev/api/v1/zip_codes?zip_code=" + cpLimpio))
                    .timeout(Duration.ofSeconds(4))
                    .GET()
                    .build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200 && resp.body() != null) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(resp.body());
                JsonNode zipCodes = root.get("zip_codes");
                if (zipCodes != null && zipCodes.isArray()) {
                    if (!zipCodes.isEmpty()) {
                        JsonNode first = zipCodes.get(0);
                        String estRemoto = sanitizarNombreColonia(first.path("d_estado").asText(""));
                        String munRemoto = sanitizarNombreColonia(first.path("d_mnpio").asText(""));
                        String estFinal = esColoniaValida(estRemoto) ? estRemoto : estadoLocal;
                        String munFinal = esColoniaValida(munRemoto)
                                ? munRemoto
                                : resolverMunicipioSepomex(Integer.parseInt(cpLimpio), prefijo);
                        DomicilioCpDto dto = new DomicilioCpDto(cpLimpio, "México", estFinal, munFinal);
                        return ResponseEntity.ok(GenericResponse.exito(dto, "Código postal localizado"));
                    } else if (!CP_MAP.containsKey(cpLimpio) && !COLONIAS_POR_CP.containsKey(cpLimpio)) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(GenericResponse.error(HttpStatus.NOT_FOUND.value(), "No se encontraron resultados para el código postal ingresado."));
                    }
                }
            }
        } catch (Exception ignored) {
            // Si el servicio externo no responde, usar resolución local SEPOMEX
        }

        DomicilioCpDto encontrado = CP_MAP.get(cpLimpio);
        if (encontrado == null) {
            int cpNum = Integer.parseInt(cpLimpio);
            String municipio = resolverMunicipioSepomex(cpNum, prefijo);
            encontrado = new DomicilioCpDto(cpLimpio, "México", estadoLocal, municipio);
        }

        return ResponseEntity.ok(GenericResponse.exito(encontrado, "Código postal localizado"));
    }

    private String resolverMunicipioSepomex(int cpNum, String prefijo) {
        if (cpNum >= 36000 && cpNum <= 38999) {
            if (cpNum <= 36099 || (cpNum >= 36200 && cpNum <= 36269)) return "Guanajuato";
            if ((cpNum >= 36100 && cpNum <= 36199) || (cpNum >= 36270 && cpNum <= 36299)) return "Silao de la Victoria";
            if (cpNum >= 36300 && cpNum <= 36399) return "San Francisco del Rincon";
            if (cpNum >= 36400 && cpNum <= 36469) return "Purisima del Rincon";
            if (cpNum >= 36470 && cpNum <= 36499) return "Manuel Doblado";
            if ((cpNum >= 36500 && cpNum <= 36699) || (cpNum >= 36800 && cpNum <= 36849)) return "Irapuato";
            if ((cpNum >= 36700 && cpNum <= 36799) || (cpNum >= 36850 && cpNum <= 36889)) return "Salamanca";
            if (cpNum >= 36890 && cpNum <= 36899) return "Pueblo Nuevo";
            if (cpNum >= 36900 && cpNum <= 36959) return "Penjamo";
            if (cpNum >= 36960 && cpNum <= 36969) return "Cueramaro";
            if (cpNum >= 36970 && cpNum <= 36989) return "Abasolo";
            if (cpNum >= 36990 && cpNum <= 36999) return "Huanimaro";
            if (cpNum >= 37000 && cpNum <= 37599) return "Leon";
            if (cpNum >= 37600 && cpNum <= 37629) return "San Felipe";
            if (cpNum >= 37630 && cpNum <= 37699) return "Ocampo";
            if (cpNum >= 37700 && cpNum <= 37799) return "San Miguel de Allende";
            if (cpNum >= 37800 && cpNum <= 37849) return "Dolores Hidalgo Cuna de la Independencia Nacional";
            if (cpNum >= 37850 && cpNum <= 37899) return "San Diego de la Unión";
            if (cpNum >= 37900 && cpNum <= 37919) return "San Luis de la Paz";
            if (cpNum >= 37920 && cpNum <= 37929) return "Victoria";
            if (cpNum >= 37930 && cpNum <= 37939) return "Xichu";
            if (cpNum >= 37940 && cpNum <= 37949) return "Atarjea";
            if (cpNum >= 37950 && cpNum <= 37959) return "Santa Catarina";
            if (cpNum >= 37960 && cpNum <= 37969) return "Doctor Mora";
            if (cpNum >= 37970 && cpNum <= 37979) return "Tierra Blanca";
            if (cpNum >= 37980 && cpNum <= 37999) return "San Jose Iturbide";
            if (cpNum >= 38000 && cpNum <= 38159) return "Celaya";
            if (cpNum >= 38160 && cpNum <= 38199) return "Apaseo el Grande";
            if (cpNum >= 38200 && cpNum <= 38259) return "Comonfort";
            if (cpNum >= 38260 && cpNum <= 38299) return "Villagran";
            if (cpNum >= 38300 && cpNum <= 38399) return "Cortazar";
            if (cpNum >= 38400 && cpNum <= 38469) return "Valle de Santiago";
            if (cpNum >= 38470 && cpNum <= 38499) return "Jaral del Progreso";
            if (cpNum >= 38500 && cpNum <= 38559) return "Apaseo el Alto";
            if (cpNum >= 38560 && cpNum <= 38599) return "Jerecuaro";
            if (cpNum >= 38600 && cpNum <= 38699) return "Acambaro";
            if (cpNum >= 38700 && cpNum <= 38759) return "Tarimoro";
            if (cpNum >= 38760 && cpNum <= 38779) return "Coroneo";
            if (cpNum >= 38780 && cpNum <= 38799) return "Tarandacuao";
            if (cpNum >= 38800 && cpNum <= 38889) return "Moroleon";
            if (cpNum >= 38890 && cpNum <= 38899) return "Uriangato";
            if (cpNum >= 38900 && cpNum <= 38969) return "Salvatierra";
            if (cpNum >= 38970 && cpNum <= 38979) return "Santiago Maravatio";
            return "Yuriria";
        }
        return switch (prefijo) {
            case "01" -> "Alvaro Obregon";
            case "02" -> "Azcapotzalco";
            case "03" -> "Benito Juarez";
            case "04" -> "Coyoacan";
            case "05" -> "Cuajimalpa de Morelos";
            case "06" -> "Cuauhtemoc";
            case "07" -> "Gustavo A Madero";
            case "08" -> "Iztacalco";
            case "09" -> "Iztapalapa";
            case "10" -> "La Magdalena Contreras";
            case "11" -> "Miguel Hidalgo";
            case "12" -> "Milpa Alta";
            case "13" -> "Tlahuac";
            case "14" -> "Tlalpan";
            case "15" -> "Venustiano Carranza";
            case "16" -> "Xochimilco";
            case "20" -> "Aguascalientes";
            case "21" -> "Mexicali";
            case "22" -> "Tijuana";
            case "23" -> "La Paz";
            case "24" -> "Campeche";
            case "25" -> "Saltillo";
            case "26" -> "Piedras Negras";
            case "27" -> "Torreon";
            case "28" -> "Colima";
            case "29" -> "Tuxtla Gutierrez";
            case "30" -> "Tapachula";
            case "31" -> "Chihuahua";
            case "32" -> "Juarez";
            case "33" -> "Delicias";
            case "34" -> "Durango";
            case "35" -> "Gomez Palacio";
            case "39" -> "Chilpancingo de los Bravo";
            case "40", "41" -> "Acapulco de Juarez";
            case "42", "43" -> "Pachuca de Soto";
            case "44" -> "Guadalajara";
            case "45" -> "Zapopan";
            case "46", "47" -> "Tepatitlan de Morelos";
            case "48", "49" -> "Puerto Vallarta";
            case "50", "51", "52" -> "Toluca";
            case "53" -> "Naucalpan de Juarez";
            case "54" -> "Tlalnepantla de Baz";
            case "55" -> "Ecatepec de Morelos";
            case "56", "57" -> "Nezahualcoyotl";
            case "58" -> "Morelia";
            case "59" -> "Zamora";
            case "60", "61" -> "Uruapan";
            case "62" -> "Cuernavaca";
            case "63" -> "Tepic";
            case "64" -> "Monterrey";
            case "65" -> "Apodaca";
            case "66" -> "San Pedro Garza Garcia";
            case "67" -> "Guadalupe";
            case "68", "69", "70", "71" -> "Oaxaca de Juarez";
            case "72", "73", "74" -> "Puebla";
            case "75" -> "Tehuacan";
            case "76" -> cpNum >= 76800 ? "San Juan del Rio" : "Queretaro";
            case "77" -> cpNum < 77500 ? "Othon P Blanco" : "Benito Juarez";
            case "78" -> "San Luis Potosi";
            case "79" -> "Ciudad Valles";
            case "80", "81" -> "Culiacan";
            case "82" -> "Mazatlan";
            case "83" -> "Hermosillo";
            case "84" -> "Nogales";
            case "85" -> "Cajeme";
            case "86" -> "Centro";
            case "87" -> "Victoria";
            case "88" -> "Reynosa";
            case "89" -> "Tampico";
            case "90" -> "Tlaxcala";
            case "91" -> "Xalapa";
            case "92", "93" -> "Tuxpan";
            case "94" -> "Cordoba";
            case "95", "96" -> "Coatzacoalcos";
            case "97" -> "Merida";
            case "98" -> "Zacatecas";
            case "99" -> "Fresnillo";
            default -> "Guanajuato";
        };
    }
}
