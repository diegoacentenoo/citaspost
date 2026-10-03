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
import org.springframework.security.core.Authentication; // <-- NUEVO
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/citas")
@RequiredArgsConstructor
public class CitaController {

    private final CitaService citaService;
    private final UsuarioRepository usuarioRepository; // <-- NUEVO

    @PostMapping
    @PreAuthorize("hasAnyRole('PACIENTE', 'RECEPCIONISTA', 'ADMINISTRADOR')")
    public ResponseEntity<CitaResponseDTO> agendarCita(
            @Valid @RequestBody CitaRequestDTO requestDTO,
            Authentication authentication) { // <-- Spring inyecta automáticamente los datos del Token aquí

        String usernameToken = authentication.getName(); // El correo/cédula que viene en el JWT
        String rolToken = authentication.getAuthorities().iterator().next().getAuthority(); // Ej: ROLE_PACIENTE

        Long pacienteIdSeguro = requestDTO.getPacienteId();

        // 🛡️ CANDADO DE SEGURIDAD: Si es un paciente, ignoramos el ID del JSON
        // y forzamos a que use el ID asociado a su propio Token.
        if (rolToken.equals("ROLE_PACIENTE")) {
            Usuario usuarioActual = usuarioRepository.findByUsername(usernameToken)
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado en la base de datos"));

            pacienteIdSeguro = usuarioActual.getPersona().getId();
        }

        // Llamamos al servicio con el pacienteId validado
        Cita nuevaCita = citaService.agendarCita(
                requestDTO.getOdontologoId(),
                pacienteIdSeguro, // <-- Usamos la variable segura
                requestDTO.getTarifaId(),
                requestDTO.getFecha(),
                requestDTO.getHoraInicio(),
                requestDTO.getHoraFin()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(mapearAResponseDTO(nuevaCita));
    }

    @GetMapping("/odontologo/{odontologoId}/fecha/{fecha}")
    @PreAuthorize("hasAnyRole('ODONTOLOGO', 'RECEPCIONISTA', 'ADMINISTRADOR')")
    public ResponseEntity<List<CitaResponseDTO>> obtenerAgendaOdontologo(
            @PathVariable Long odontologoId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {

        List<Cita> citas = citaService.obtenerCitasPorOdontologoYFecha(odontologoId, fecha);

        List<CitaResponseDTO> responseList = citas.stream()
                .map(this::mapearAResponseDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responseList);
    }

    private CitaResponseDTO mapearAResponseDTO(Cita cita) {
        return CitaResponseDTO.builder()
                .id(cita.getId())
                .odontologoNombre(cita.getOdontologo().getNombre() + " " + cita.getOdontologo().getApellido())
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

    // 🚀 NUEVO ENDPOINT (Añádelo al final de tu CitaController)
    @GetMapping("/paciente/proxima")
    @PreAuthorize("hasRole('PACIENTE')")
    public ResponseEntity<CitaResponseDTO> obtenerProximaCitaPaciente(Authentication authentication) {

        String usernameToken = authentication.getName();
        Usuario usuarioActual = usuarioRepository.findByUsername(usernameToken)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        Long pacienteId = usuarioActual.getPersona().getId();

        Cita proximaCita = citaService.obtenerProximaCitaPaciente(pacienteId);

        if (proximaCita == null) {
            return ResponseEntity.ok().build(); // Retorna 200 OK pero vacío (sin body)
        }

        return ResponseEntity.ok(mapearAResponseDTO(proximaCita));
    }
}