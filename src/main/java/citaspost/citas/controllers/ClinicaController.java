package citaspost.citas.controllers;

import citaspost.citas.dtos.request.HorarioRequestDTO;
import citaspost.citas.dtos.response.HorarioResponseDTO;
import citaspost.citas.dtos.response.PersonaResponseDTO;
import citaspost.citas.dtos.response.TarifaDTO;
import citaspost.citas.enums.Rol;
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
        return ResponseEntity.ok(clinicaService.listarPorRol(Rol.PSICOLOGO));
    }

    @GetMapping("/pacientes")
    public ResponseEntity<List<PersonaResponseDTO>> listarPacientes() {
        return ResponseEntity.ok(clinicaService.listarPorRol(Rol.PACIENTE));
    }

    // --- NUEVO ENDPOINT PARA CONSULTAR FICHA, MAYORÍA DE EDAD Y PUDIENTES ---
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
    public ResponseEntity<Map<String, String>> eliminarHorario(@PathVariable Long horarioId) {
        clinicaService.eliminarHorario(horarioId);
        return ResponseEntity.ok(Map.of("message", "Horario eliminado correctamente"));
    }
}