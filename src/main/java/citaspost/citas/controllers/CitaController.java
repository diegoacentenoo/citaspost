package citaspost.citas.controllers;

import citaspost.citas.dtos.request.CitaRequestDTO;
import citaspost.citas.dtos.response.CitaResponseDTO;
import citaspost.citas.dtos.response.CupoDisponibleDTO;
import citaspost.citas.services.AuthService;
import citaspost.citas.services.CitaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/citas")
@RequiredArgsConstructor
public class CitaController {

    private final CitaService citaService;
    private final AuthService authService;

    @PostMapping
    public ResponseEntity<CitaResponseDTO> agendarCita(@Valid @RequestBody CitaRequestDTO dto) {
        return ResponseEntity.ok(citaService.agendarCita(dto));
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
        if (idFinal == null) {
            return ResponseEntity.ok(null);
        }
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

    @GetMapping("/psicologo/{psicologoId}/fecha/{fecha}")
    public ResponseEntity<List<CitaResponseDTO>> obtenerAgendaPsicologo(
            @PathVariable Long psicologoId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(citaService.obtenerCitasPorPsicologoYFecha(psicologoId, fecha));
    }

    @PatchMapping("/{citaId}/estado")
    public ResponseEntity<CitaResponseDTO> cambiarEstadoCita(
            @PathVariable Long citaId,
            @RequestBody java.util.Map<String, Object> body) {

        citaspost.citas.enums.EstadoCita estado = body.get("estadoCita") != null
                ? citaspost.citas.enums.EstadoCita.valueOf(body.get("estadoCita").toString())
                : null;
        Boolean asistio = body.get("asistio") != null
                ? Boolean.valueOf(body.get("asistio").toString())
                : null;
        String notasClinicas = body.get("notasClinicas") != null
                ? body.get("notasClinicas").toString()
                : null;

        return ResponseEntity.ok(citaService.actualizarEstadoCita(citaId, estado, asistio, notasClinicas));
    }

    @PatchMapping("/{citaId}/pago")
    public ResponseEntity<CitaResponseDTO> cambiarEstadoPago(
            @PathVariable Long citaId,
            @RequestBody java.util.Map<String, String> body) {
        citaspost.citas.enums.EstadoPago estadoPago = citaspost.citas.enums.EstadoPago.valueOf(body.get("estadoPago"));
        return ResponseEntity.ok(citaService.actualizarEstadoPago(citaId, estadoPago));
    }
}