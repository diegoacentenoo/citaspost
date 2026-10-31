package citaspost.citas.controllers;

import citaspost.citas.dtos.request.HorarioRequestDTO;
import citaspost.citas.dtos.response.HorarioResponseDTO;
import citaspost.citas.dtos.response.PersonaResponseDTO;
import citaspost.citas.dtos.response.TarifaDTO;
import citaspost.citas.services.ClinicaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/clinica")
@RequiredArgsConstructor
public class ClinicaController {

    private final ClinicaService clinicaService;

    @GetMapping("/psicologos")
    public ResponseEntity<List<PersonaResponseDTO>> listarPsicologos() {
        return ResponseEntity.ok(clinicaService.listarPsicologosActivos());
    }

    @GetMapping("/psicologos/todos")
    public ResponseEntity<List<PersonaResponseDTO>> listarTodosLosPsicologos() {
        return ResponseEntity.ok(clinicaService.listarTodosLosPsicologos());
    }

    @PatchMapping("/psicologos/{id}/estado")
    public ResponseEntity<PersonaResponseDTO> cambiarEstadoPsicologo(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body) {
        boolean activo = Boolean.TRUE.equals(body.get("activo"));
        return ResponseEntity.ok(clinicaService.cambiarEstadoActivoPsicologo(id, activo));
    }

    @GetMapping("/pacientes")
    public ResponseEntity<List<PersonaResponseDTO>> listarPacientes() {
        return ResponseEntity.ok(clinicaService.listarPacientesYPsicologos());
    }

    @GetMapping("/personas/{id}")
    public ResponseEntity<PersonaResponseDTO> obtenerPersonaPorId(@PathVariable Long id) {
        return ResponseEntity.ok(clinicaService.obtenerPersonaPorId(id));
    }

    @GetMapping("/tarifas")
    public ResponseEntity<List<TarifaDTO>> listarTarifas() {
        return ResponseEntity.ok(clinicaService.listarTarifasActivas());
    }

    @GetMapping("/tarifas/todas")
    public ResponseEntity<List<TarifaDTO>> listarTodasLasTarifas() {
        return ResponseEntity.ok(clinicaService.listarTodasLasTarifas());
    }

    @PostMapping("/tarifas")
    public ResponseEntity<TarifaDTO> guardarOActualizarTarifa(@RequestBody TarifaDTO dto) {
        return ResponseEntity.ok(clinicaService.guardarOActualizarTarifa(dto));
    }

    @GetMapping("/horarios/psicologo/{psicologoId}")
    public ResponseEntity<List<HorarioResponseDTO>> listarHorarios(@PathVariable Long psicologoId) {
        return ResponseEntity.ok(clinicaService.listarHorariosPsicologo(psicologoId));
    }

    @PostMapping("/horarios")
    public ResponseEntity<HorarioResponseDTO> guardarHorario(@Valid @RequestBody HorarioRequestDTO dto) {
        return ResponseEntity.ok(clinicaService.registrarHorario(dto));
    }

    @DeleteMapping("/horarios/{horarioId}")
    public ResponseEntity<?> eliminarHorario(@PathVariable Long horarioId) {
        clinicaService.eliminarHorario(horarioId);
        return ResponseEntity.ok(Map.of("message", "Horario eliminado correctamente"));
    }

    @PatchMapping("/pacientes/{pacienteId}/psicologo-asignado")
    public ResponseEntity<PersonaResponseDTO> cambiarPsicologoAsignado(
            @PathVariable Long pacienteId,
            @RequestBody Map<String, Long> body) {
        return ResponseEntity.ok(clinicaService.cambiarPsicologoAsignadoPaciente(pacienteId, body.get("psicologoId")));
    }
}