package sn.groupeisi.leaveworkflow.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import sn.groupeisi.leaveworkflow.dto.*;
import sn.groupeisi.leaveworkflow.model.User;
import sn.groupeisi.leaveworkflow.service.DemandeCongeService;
import sn.groupeisi.leaveworkflow.service.FileStorageService;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/demandes")
@RequiredArgsConstructor
public class DemandeCongeController {

    private final DemandeCongeService demandeCongeService;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;

    private DemandeRequest parseRequestJson(String requestJson) {
        try {
            com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(requestJson);
            if (node == null) throw new IllegalArgumentException("Corps de requête vide");

            Long typeCongeId = node.hasNonNull("typeCongeId") ? node.get("typeCongeId").asLong() : null;
            String dateDebutStr = node.hasNonNull("dateDebut") ? node.get("dateDebut").asText() : null;
            String dateFinStr = node.hasNonNull("dateFin") ? node.get("dateFin").asText() : null;
            String motif = node.hasNonNull("motif") ? node.get("motif").asText() : null;

            if (typeCongeId == null || dateDebutStr == null || dateFinStr == null) {
                throw new IllegalArgumentException("Champs requis manquants: typeCongeId, dateDebut, dateFin");
            }

            java.time.LocalDate dateDebut = java.time.LocalDate.parse(dateDebutStr);
            java.time.LocalDate dateFin = java.time.LocalDate.parse(dateFinStr);

            DemandeRequest req = new DemandeRequest();
            req.setTypeCongeId(typeCongeId);
            req.setDateDebut(dateDebut);
            req.setDateFin(dateFin);
            req.setMotif(motif);
            return req;
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException("Impossible de parser le JSON de la requête: " + ex.getMessage(), ex);
        }
    }

    private DemandeRequest buildFromParams(HttpServletRequest servletRequest) {
        String typeCongeIdStr = servletRequest.getParameter("typeCongeId");
        String dateDebutStr = servletRequest.getParameter("dateDebut");
        String dateFinStr = servletRequest.getParameter("dateFin");
        String motif = servletRequest.getParameter("motif");
        if (typeCongeIdStr == null || dateDebutStr == null || dateFinStr == null) {
            return null;
        }
        try {
            Long typeCongeId = Long.parseLong(typeCongeIdStr);
            java.time.LocalDate dateDebut = java.time.LocalDate.parse(dateDebutStr);
            java.time.LocalDate dateFin = java.time.LocalDate.parse(dateFinStr);
            DemandeRequest req = new DemandeRequest();
            req.setTypeCongeId(typeCongeId);
            req.setDateDebut(dateDebut);
            req.setDateFin(dateFin);
            req.setMotif(motif);
            return req;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Paramètres de requête invalides: " + ex.getMessage());
        }
    }

    private DemandeRequest resolveRequest(String requestJson, HttpServletRequest servletRequest) {
        if (requestJson != null) {
            return parseRequestJson(requestJson);
        }
        // try to get whole 'request' param
        String param = servletRequest.getParameter("request");
        if (param != null) {
            return parseRequestJson(param);
        }
        // try individual params
        DemandeRequest built = buildFromParams(servletRequest);
        if (built != null) return built;
        throw new IllegalArgumentException("Données de demande manquantes");
    }

    @PostMapping(consumes = {org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE})
    @PreAuthorize("hasRole('SALARIE')")
    public ResponseEntity<?> createDemande(
            Authentication authentication,
            @RequestPart(value = "request", required = false) String requestJson,
            @RequestPart(value = "justificatif", required = false) MultipartFile justificatif,
            HttpServletRequest servletRequest) {
        User user = (User) authentication.getPrincipal();
        try {
            if (requestJson == null) {
                requestJson = servletRequest.getParameter("request");
            }
            System.out.println("Content-Type: " + servletRequest.getContentType());
            System.out.println("Parameter names: " + java.util.Collections.list(servletRequest.getParameterNames()));
            DemandeRequest request = resolveRequest(requestJson, servletRequest);
            System.out.println("Resolved request: " + request);
            DemandeResponse resp = demandeCongeService.createDemande(user.getId(), request, justificatif);
            return ResponseEntity.status(HttpStatus.CREATED).body(resp);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(java.util.Collections.singletonMap("message", ex.getMessage()));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(java.util.Collections.singletonMap("message", ex.getMessage()));
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(java.util.Collections.singletonMap("message", "Erreur interne lors de la création de la demande: " + ex.getMessage()));
        }
    }

    @PostMapping(consumes = {org.springframework.http.MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('SALARIE')")
    public ResponseEntity<?> createDemandeJson(Authentication authentication,
                                               @RequestBody Map<String, Object> body) {
        User user = (User) authentication.getPrincipal();
        try {
            // parse fields manually to avoid LocalDate deserialization issues
            Object typeIdObj = body.get("typeCongeId");
            Object dateDebutObj = body.get("dateDebut");
            Object dateFinObj = body.get("dateFin");
            Object motifObj = body.get("motif");
            if (typeIdObj == null || dateDebutObj == null || dateFinObj == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(java.util.Collections.singletonMap("message", "Champs requis manquants: typeCongeId, dateDebut, dateFin"));
            }
            Long typeCongeId = Long.parseLong(typeIdObj.toString());
            java.time.LocalDate dateDebut = java.time.LocalDate.parse(dateDebutObj.toString());
            java.time.LocalDate dateFin = java.time.LocalDate.parse(dateFinObj.toString());
            String motif = motifObj != null ? motifObj.toString() : null;

            DemandeRequest req = new DemandeRequest();
            req.setTypeCongeId(typeCongeId);
            req.setDateDebut(dateDebut);
            req.setDateFin(dateFin);
            req.setMotif(motif);

            DemandeResponse resp = demandeCongeService.createDemande(user.getId(), req, null);
            return ResponseEntity.status(HttpStatus.CREATED).body(resp);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(java.util.Collections.singletonMap("message", ex.getMessage()));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(java.util.Collections.singletonMap("message", ex.getMessage()));
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(java.util.Collections.singletonMap("message", "Erreur interne lors de la création de la demande: " + ex.getMessage()));
        }
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('SALARIE', 'MANAGER')")
    public ResponseEntity<List<DemandeResponse>> getMyDemandes(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(demandeCongeService.getDemandesByUser(user.getId()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SALARIE', 'MANAGER', 'SERVICE_RH')")
    public ResponseEntity<DemandeResponse> getDemandeById(
            @PathVariable Long id,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        String role = user.getRole().toString();

        if (!demandeCongeService.isAuthorizedToView(id, user.getId(), role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(demandeCongeService.getDemandeById(id));
    }

    @GetMapping("/{id}/justificatif")
    @PreAuthorize("hasAnyRole('SALARIE', 'MANAGER', 'SERVICE_RH')")
    public ResponseEntity<?> downloadJustificatif(@PathVariable Long id, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        String role = user.getRole().toString();

        try {
            // Authorization: owner, manager, RH allowed
            if (!demandeCongeService.isAuthorizedToView(id, user.getId(), role)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            DemandeResponse demande = demandeCongeService.getDemandeById(id);
            if (demande.getJustificatif() == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(java.util.Collections.singletonMap("message", "Aucun justificatif trouvé pour cette demande"));
            }

            System.out.println("Attempting to download justificatif: " + demande.getJustificatif());
            Resource resource = fileStorageService.loadFileAsResource(demande.getJustificatif());
            String filename = demande.getJustificatif();

            String contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            try {
                Path path = fileStorageService.getFilePath(filename);
                String probe = java.nio.file.Files.probeContentType(path);
                if (probe != null) contentType = probe;
            } catch (IOException ignored) {
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .body(resource);
        } catch (RuntimeException ex) {
            System.err.println("Error downloading justificatif: " + ex.getMessage());
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(java.util.Collections.singletonMap("message", ex.getMessage()));
        } catch (Exception ex) {
            System.err.println("Unexpected error downloading justificatif: " + ex.getMessage());
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(java.util.Collections.singletonMap("message", "Erreur lors du téléchargement du fichier: " + ex.getMessage()));
        }
    }

    @PutMapping(value = "/{id}", consumes = {org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE})
    @PreAuthorize("hasRole('SALARIE')")
    public ResponseEntity<?> updateDemandeAsSalarie(
            @PathVariable Long id,
            Authentication authentication,
            @RequestPart(value = "request", required = false) String requestJson,
            @RequestPart(value = "justificatif", required = false) MultipartFile justificatif,
            HttpServletRequest servletRequest) {
        User user = (User) authentication.getPrincipal();
        try {
            if (requestJson == null) {
                requestJson = servletRequest.getParameter("request");
            }
            System.out.println("Content-Type: " + servletRequest.getContentType());
            System.out.println("Parameter names: " + java.util.Collections.list(servletRequest.getParameterNames()));
            DemandeRequest request = resolveRequest(requestJson, servletRequest);
            System.out.println("Resolved request: " + request);
            DemandeResponse resp = demandeCongeService.updateDemandeBySalarie(id, user.getId(), request, justificatif);
            return ResponseEntity.ok(resp);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(java.util.Collections.singletonMap("message", ex.getMessage()));
        } catch (RuntimeException ex) {
            System.err.println("Error updating demande: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(java.util.Collections.singletonMap("message", ex.getMessage()));
        } catch (Exception ex) {
            ex.printStackTrace();
            System.err.println("Unexpected error updating demande: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(java.util.Collections.singletonMap("message", "Erreur interne lors de la mise à jour de la demande: " + ex.getMessage()));
        }
    }

    @PostMapping(value = "/{id}/modify", consumes = {org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE})
    @PreAuthorize("hasRole('SALARIE')")
    public ResponseEntity<?> modifyDemandeAsSalarie(
            @PathVariable Long id,
            Authentication authentication,
            @RequestPart(value = "request", required = false) String requestJson,
            @RequestPart(value = "justificatif", required = false) MultipartFile justificatif,
            HttpServletRequest servletRequest) {
        User user = (User) authentication.getPrincipal();
        try {
            if (requestJson == null) {
                requestJson = servletRequest.getParameter("request");
            }
            System.out.println("Content-Type: " + servletRequest.getContentType());
            System.out.println("Parameter names: " + java.util.Collections.list(servletRequest.getParameterNames()));
            DemandeRequest request = resolveRequest(requestJson, servletRequest);
            System.out.println("Resolved request: " + request);
            DemandeResponse resp = demandeCongeService.updateDemandeBySalarie(id, user.getId(), request, justificatif);
            return ResponseEntity.ok(resp);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(java.util.Collections.singletonMap("message", ex.getMessage()));
        } catch (RuntimeException ex) {
            System.err.println("Error modifying demande: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(java.util.Collections.singletonMap("message", ex.getMessage()));
        } catch (Exception ex) {
            ex.printStackTrace();
            System.err.println("Unexpected error modifying demande: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(java.util.Collections.singletonMap("message", "Erreur interne lors de la modification de la demande: " + ex.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SALARIE')")
    public ResponseEntity<Void> deleteDemandeBySalarie(
            @PathVariable Long id,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        demandeCongeService.deleteDemandeBySalarie(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/manager/team")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<List<DemandeResponse>> getTeamDemandes(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(demandeCongeService.getDemandesForManagerTeam(user.getId()));
    }

    @PutMapping("/{id}/manager")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<DemandeResponse> updateByManager(
            @PathVariable Long id,
            Authentication authentication,
            @RequestBody UpdateDemandeManagerRequest request) {
        User user = (User) authentication.getPrincipal();
        demandeCongeService.validateManagerAuthorization(id, user.getId());
        request.setManagerId(user.getId());
        return ResponseEntity.ok(demandeCongeService.updateDemandeByManager(id, request));
    }

    @PutMapping("/{id}/rh")
    @PreAuthorize("hasRole('SERVICE_RH')")
    public ResponseEntity<DemandeResponse> updateByRh(
            @PathVariable Long id,
            @RequestBody UpdateDemandeRhRequest request) {
        return ResponseEntity.ok(demandeCongeService.updateDemandeByRh(id, request));
    }

    @GetMapping("/rh/pending")
    @PreAuthorize("hasRole('SERVICE_RH')")
    public ResponseEntity<List<DemandeResponse>> getRhPendingDemandes(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Long entrepriseId = user.getEntreprise() != null ? user.getEntreprise().getId() : null;
        return ResponseEntity.ok(demandeCongeService.getDemandesForRh(entrepriseId));
    }

    @PutMapping("/{id}/chef-service")
    @PreAuthorize("hasRole('CHEF_SERVICE')")
    public ResponseEntity<DemandeResponse> updateByChefService(
            @PathVariable Long id,
            @RequestBody UpdateDemandeChefServiceRequest request) {
        return ResponseEntity.ok(demandeCongeService.updateDemandeByChefService(id, request));
    }

    @GetMapping("/chef-service/pending")
    @PreAuthorize("hasRole('CHEF_SERVICE')")
    public ResponseEntity<List<DemandeResponse>> getChefServicePendingDemandes(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Long entrepriseId = user.getEntreprise() != null ? user.getEntreprise().getId() : null;
        return ResponseEntity.ok(demandeCongeService.getDemandesForChefService(entrepriseId));
    }

    @PutMapping("/{id}/dg")
    @PreAuthorize("hasRole('DG')")
    public ResponseEntity<DemandeResponse> updateByDg(
            @PathVariable Long id,
            @RequestBody UpdateDemandeDgRequest request) {
        return ResponseEntity.ok(demandeCongeService.updateDemandByDg(id, request));
    }

    @GetMapping("/dg/pending")
    @PreAuthorize("hasRole('DG')")
    public ResponseEntity<List<DemandeResponse>> getDgPendingDemandes(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Long entrepriseId = user.getEntreprise() != null ? user.getEntreprise().getId() : null;
        return ResponseEntity.ok(demandeCongeService.getDemandesForDg(entrepriseId));
    }
}

