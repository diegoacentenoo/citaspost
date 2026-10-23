package citaspost.citas.controllers;

import citaspost.citas.dtos.request.CitaRequestDTO;
import citaspost.citas.dtos.response.CitaResponseDTO;
import citaspost.citas.dtos.response.CupoDisponibleDTO;
import citaspost.citas.entities.CitaComprobanteFoto;
import citaspost.citas.entities.DatosPudiente;
import citaspost.citas.enums.EstadoCita;
import citaspost.citas.enums.EstadoPago;
import citaspost.citas.exception.BusinessException;
import citaspost.citas.services.AuthService;
import citaspost.citas.services.CitaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/citas")
@RequiredArgsConstructor
public class CitaController {

    private final CitaService citaService;
    private final AuthService authService;

    @PostMapping(value = "/guardar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> agendarCita(
            @Valid @RequestPart("cita") CitaRequestDTO citaDTO,
            @RequestPart("imagen") MultipartFile imagen) {

        if (imagen == null || imagen.isEmpty() || imagen.getContentType() == null || !imagen.getContentType().startsWith("image/")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Es obligatorio subir una imagen válida del comprobante de pago."));
        }

        try {
            CitaResponseDTO respuesta = citaService.agendarCitaConComprobante(citaDTO, imagen);
            return ResponseEntity.ok(respuesta);
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage(), "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Error interno: " + e.getMessage()));
        }
    }

    @GetMapping("/{citaId}/comprobante")
    public ResponseEntity<byte[]> verComprobante(@PathVariable Long citaId) {
        CitaComprobanteFoto foto = citaService.obtenerComprobante(citaId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(foto.getMimeType()))
                .body(foto.getFotoBytes());
    }

    @PatchMapping("/{citaId}/pudientes")
    public ResponseEntity<CitaResponseDTO> anexarPudiente(
            @PathVariable Long citaId,
            @RequestBody DatosPudiente pudiente) {
        return ResponseEntity.ok(citaService.anexarPudienteACita(citaId, pudiente));
    }

    @GetMapping("/cupos-disponibles")
    public ResponseEntity<List<CupoDisponibleDTO>> obtenerCupos(
            @RequestParam Long psicologoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(citaService.obtenerCuposDisponibles(psicologoId, fecha));
    }

    @GetMapping("/paciente/proxima")
    public ResponseEntity<CitaResponseDTO> obtenerProximaCita(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(required = false) Long pacienteId) {
        Long idFinal = (pacienteId != null) ? pacienteId : authService.obtenerPersonaIdDesdeToken(authHeader);
        if (idFinal == null) return ResponseEntity.ok(null);
        return ResponseEntity.ok(citaService.obtenerProximaCitaPaciente(idFinal));
    }

    @GetMapping("/paciente/{pacienteId}")
    public ResponseEntity<List<CitaResponseDTO>> listarCitasDePaciente(@PathVariable Long pacienteId) {
        return ResponseEntity.ok(citaService.obtenerMisCitasPaciente(pacienteId));
    }

    @PatchMapping("/{citaId}/cancelar")
    public ResponseEntity<CitaResponseDTO> cancelarCita(@PathVariable Long citaId) {
        return ResponseEntity.ok(citaService.cancelarCita(citaId));
    }

    @PatchMapping("/{citaId}/estado")
    public ResponseEntity<CitaResponseDTO> cambiarEstadoCita(
            @PathVariable Long citaId,
            @RequestBody Map<String, Object> body) {
        EstadoCita estado = body.get("estadoCita") != null ? EstadoCita.valueOf(body.get("estadoCita").toString()) : null;
        Boolean asistio = body.get("asistio") != null ? Boolean.valueOf(body.get("asistio").toString()) : null;
        String notasClinicas = body.get("notasClinicas") != null ? body.get("notasClinicas").toString() : null;
        return ResponseEntity.ok(citaService.actualizarEstadoCita(citaId, estado, asistio, notasClinicas));
    }

    @PatchMapping("/{citaId}/pago")
    public ResponseEntity<CitaResponseDTO> cambiarEstadoPago(
            @PathVariable Long citaId,
            @RequestBody Map<String, String> body) {
        EstadoPago estadoPago = EstadoPago.valueOf(body.get("estadoPago"));
        return ResponseEntity.ok(citaService.actualizarEstadoPago(citaId, estadoPago));
    }

    @GetMapping("/psicologo/{psicologoId}/fecha/{fecha}")
    public ResponseEntity<List<CitaResponseDTO>> obtenerAgendaPsicologo(
            @PathVariable Long psicologoId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(citaService.obtenerCitasPorPsicologoYFecha(psicologoId, fecha));
    }
}