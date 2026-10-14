package citaspost.citas.controllers;

import citaspost.citas.dtos.request.CitaRequestDTO;
import citaspost.citas.dtos.response.CitaResponseDTO;
import citaspost.citas.entities.Cita;
import citaspost.citas.entities.Usuario;
import citaspost.citas.exception.ResourceNotFoundException;
import citaspost.citas.repositories.UsuarioRepository;
import citaspost.citas.services.CitaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/citas")
@RequiredArgsConstructor
public class CitaController {

    private final CitaService citaService;
    private final UsuarioRepository usuarioRepository;

    @PostMapping
    @PreAuthorize("hasAnyRole('PACIENTE', 'RECEPCIONISTA', 'ADMINISTRADOR')")
    public ResponseEntity<CitaResponseDTO> agendarCita(
            @Valid @RequestBody CitaRequestDTO requestDTO,
            Authentication authentication) { 

        String usernameToken = authentication.getName();
        String rolToken = authentication.getAuthorities().iterator().next().getAuthority();

        Long pacienteIdSeguro = requestDTO.getPacienteId();

        if (rolToken.equals("ROLE_PACIENTE")) {
            Usuario usuarioActual = usuarioRepository.findByUsername(usernameToken)
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
            pacienteIdSeguro = usuarioActual.getPersona().getId();
        }

        Cita nuevaCita = citaService.agendarCita(
                requestDTO.getPsicologoId(), 
                pacienteIdSeguro,
                requestDTO.getTarifaId(),
                requestDTO.getFecha(),
                requestDTO.getHoraInicio(),
                requestDTO.getHoraFin()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(mapearAResponseDTO(nuevaCita));
    }

    @GetMapping("/psicologo/{psicologoId}/fecha/{fecha}")
    @PreAuthorize("hasAnyRole('PSICOLOGO', 'RECEPCIONISTA', 'ADMINISTRADOR')")
    public ResponseEntity<List<CitaResponseDTO>> obtenerAgendaPsicologo(
            @PathVariable Long psicologoId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {

        List<Cita> citas = citaService.obtenerCitasPorPsicologoYFecha(psicologoId, fecha);

        List<CitaResponseDTO> responseList = citas.stream()
                .map(this::mapearAResponseDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responseList);
    }

    @GetMapping("/paciente/proxima")
    @PreAuthorize("hasRole('PACIENTE')")
    public ResponseEntity<CitaResponseDTO> obtenerProximaCitaPaciente(Authentication authentication) {

        String usernameToken = authentication.getName();
        Usuario usuarioActual = usuarioRepository.findByUsername(usernameToken)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        Long pacienteId = usuarioActual.getPersona().getId();
        Cita proximaCita = citaService.obtenerProximaCitaPaciente(pacienteId);

        if (proximaCita == null) {
            return ResponseEntity.ok().build(); 
        }

        return ResponseEntity.ok(mapearAResponseDTO(proximaCita));
    }

    private CitaResponseDTO mapearAResponseDTO(Cita cita) {
        return CitaResponseDTO.builder()
                .id(cita.getId())
                .psicologoNombre(cita.getPsicologo().getNombre() + " " + cita.getPsicologo().getApellido())
                .pacienteNombre(cita.getPaciente().getNombre() + " " + cita.getPaciente().getApellido())
                .fecha(cita.getFecha())
                .horaInicio(cita.getHoraInicio())
                .horaFin(cita.getHoraFin())
                .tipoTarifa(cita.getTarifa().getTipoTarifa().name())
                .montoTarifa(cita.getTarifa().getMonto())
                .estadoPago(cita.getEstadoPago())
                .estadoCita(cita.getEstadoCita())
                .build();
    }
}