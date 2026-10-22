package citaspost.citas.services;

import citaspost.citas.dtos.request.CitaRequestDTO;
import citaspost.citas.dtos.response.CitaResponseDTO;
import citaspost.citas.dtos.response.CupoDisponibleDTO;
import citaspost.citas.entities.Cita;
import citaspost.citas.entities.HorarioPsicologo;
import citaspost.citas.entities.Persona;
import citaspost.citas.entities.Tarifa;
import citaspost.citas.enums.DiaSemana;
import citaspost.citas.enums.EstadoCita;
import citaspost.citas.enums.EstadoPago;
import citaspost.citas.enums.Rol;
import citaspost.citas.exception.BusinessException;
import citaspost.citas.exception.ResourceNotFoundException;
import citaspost.citas.repositories.CitaRepository;
import citaspost.citas.repositories.HorarioPsicologoRepository;
import citaspost.citas.repositories.PersonaRepository;
import citaspost.citas.repositories.TarifaRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CitaService {

    private final CitaRepository citaRepository;
    private final PersonaRepository personaRepository;
    private final TarifaRepository tarifaRepository;
    private final HorarioPsicologoRepository horarioRepository;

    @Transactional
    public CitaResponseDTO agendarCita(CitaRequestDTO dto) {
        if (dto.getFecha().isBefore(LocalDate.now())) {
            throw new BusinessException("No se pueden agendar citas en fechas pasadas.");
        }
        if (!dto.getHoraFin().isAfter(dto.getHoraInicio())) {
            throw new BusinessException("La hora de fin debe ser posterior a la hora de inicio.");
        }

        Persona psicologo = personaRepository.findById(dto.getPsicologoId())
                .orElseThrow(() -> new ResourceNotFoundException("Psicólogo no encontrado."));
        if (psicologo.getRol() != Rol.PSICOLOGO || !Boolean.TRUE.equals(psicologo.getActivo())) {
            throw new BusinessException("La persona seleccionada no es un psicólogo activo.");
        }

        Persona paciente = personaRepository.findById(dto.getPacienteId())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado."));

        Tarifa tarifa = tarifaRepository.findById(dto.getTarifaId())
                .orElseThrow(() -> new ResourceNotFoundException("Tarifa no encontrada."));

        // 1. Verificar que el psicólogo trabaje ese día y dentro de ese rango horario
        DiaSemana diaSemana = convertirDiaSemana(dto.getFecha().getDayOfWeek());
        List<HorarioPsicologo> horariosDia = horarioRepository
                .findByPsicologoIdAndDiaSemanaAndActivoTrue(psicologo.getId(), diaSemana);

        if (horariosDia.isEmpty()) {
            throw new BusinessException("El psicólogo no tiene horario de atención los días " + diaSemana + ".");
        }

        boolean dentroDeHorario = horariosDia.stream().anyMatch(h ->
                !dto.getHoraInicio().isBefore(h.getHoraInicio()) && !dto.getHoraFin().isAfter(h.getHoraFin())
        );

        if (!dentroDeHorario) {
            throw new BusinessException("La hora seleccionada está fuera del horario laboral del psicólogo.");
        }

        // 2. Verificar que no exista solapamiento con otra cita activa
        boolean hayCruce = citaRepository.existsByPsicologoAndFechaAndHorarioSolapado(
                psicologo.getId(), dto.getFecha(), dto.getHoraInicio(), dto.getHoraFin());

        if (hayCruce) {
            throw new BusinessException("Ese cupo ya se encuentra ocupado. Por favor seleccione otra hora.");
        }

        Cita nuevaCita = Cita.builder()
                .psicologo(psicologo)
                .paciente(paciente)
                .fecha(dto.getFecha())
                .horaInicio(dto.getHoraInicio())
                .horaFin(dto.getHoraFin())
                .tarifa(tarifa)
                .estadoPago(EstadoPago.NOPAGADO)
                .estadoCita(EstadoCita.PENDIENTE)
                .asistio(false)
                .build();

        return mapearADTO(citaRepository.save(nuevaCita));
    }

    /**
     * Genera los bloques de 1 hora según el horario del psicólogo para esa fecha
     * y marca cuáles están libres y cuáles ya están reservados.
     */
    public List<CupoDisponibleDTO> obtenerCuposDisponibles(Long psicologoId, LocalDate fecha) {
        DiaSemana dia = convertirDiaSemana(fecha.getDayOfWeek());
        List<HorarioPsicologo> bloquesHorario = horarioRepository
                .findByPsicologoIdAndDiaSemanaAndActivoTrue(psicologoId, dia);

        if (bloquesHorario.isEmpty()) {
            return List.of();
        }

        List<Cita> citasDelDia = citaRepository
                .findByPsicologoIdAndFechaAndEstadoCitaNotOrderByHoraInicioAsc(
                        psicologoId, fecha, EstadoCita.CANCELADA);

        List<CupoDisponibleDTO> cupos = new ArrayList<>();

        for (HorarioPsicologo bloque : bloquesHorario) {
            LocalTime cursor = bloque.getHoraInicio();
            while (cursor.plusHours(1).compareTo(bloque.getHoraFin()) <= 0) {
                LocalTime inicioSlot = cursor;
                LocalTime finSlot = cursor.plusHours(1);

                boolean ocupado = citasDelDia.stream().anyMatch(c ->
                        c.getHoraInicio().isBefore(finSlot) && c.getHoraFin().isAfter(inicioSlot)
                );

                // Si la fecha es hoy, deshabilitar horas que ya pasaron
                if (fecha.equals(LocalDate.now()) && inicioSlot.isBefore(LocalTime.now())) {
                    ocupado = true;
                }

                cupos.add(CupoDisponibleDTO.builder()
                        .horaInicio(inicioSlot)
                        .horaFin(finSlot)
                        .disponible(!ocupado)
                        .build());

                cursor = finSlot;
            }
        }
        return cupos;
    }

    @Transactional
    public CitaResponseDTO cancelarCita(Long citaId) {
        Cita cita = citaRepository.findById(citaId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la cita con ID: " + citaId));

        if (cita.getEstadoCita() == EstadoCita.CANCELADA) {
            throw new BusinessException("La cita ya se encuentra cancelada.");
        }
        if (cita.getEstadoCita() == EstadoCita.REALIZADA) {
            throw new BusinessException("No se puede cancelar una cita que ya fue realizada.");
        }

        cita.setEstadoCita(EstadoCita.CANCELADA);
        return mapearADTO(citaRepository.save(cita));
    }

    public CitaResponseDTO obtenerProximaCitaPaciente(Long pacienteId) {
        return citaRepository
                .findFirstByPacienteIdAndFechaGreaterThanEqualAndEstadoCitaNotOrderByFechaAscHoraInicioAsc(
                        pacienteId, LocalDate.now(), EstadoCita.CANCELADA)
                .map(this::mapearADTO)
                .orElse(null);
    }

    public List<CitaResponseDTO> obtenerMisCitasPaciente(Long pacienteId) {
        return citaRepository.findByPacienteIdOrderByFechaDescHoraInicioDesc(pacienteId)
                .stream()
                .map(this::mapearADTO)
                .toList();
    }

    public List<CitaResponseDTO> obtenerCitasPorPsicologoYFecha(Long psicologoId, LocalDate fecha) {
        return citaRepository.findByPsicologoIdAndFechaAndEstadoCitaNotOrderByHoraInicioAsc(
                        psicologoId, fecha, EstadoCita.CANCELADA)
                .stream()
                .map(this::mapearADTO)
                .toList();
    }

    private DiaSemana convertirDiaSemana(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> DiaSemana.LUNES;
            case TUESDAY -> DiaSemana.MARTES;
            case WEDNESDAY -> DiaSemana.MIERCOLES;
            case THURSDAY -> DiaSemana.JUEVES;
            case FRIDAY -> DiaSemana.VIERNES;
            case SATURDAY -> DiaSemana.SABADO;
            case SUNDAY -> DiaSemana.DOMINGO;
        };
    }

    @Transactional
    public CitaResponseDTO actualizarEstadoCita(Long citaId, EstadoCita nuevoEstado, Boolean asistio, String notasClinicas) {
        Cita cita = citaRepository.findById(citaId)
                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada con ID: " + citaId));

        if (nuevoEstado != null) {
            cita.setEstadoCita(nuevoEstado);
        }
        if (asistio != null) {
            cita.setAsistio(asistio);
        } else if (nuevoEstado == EstadoCita.REALIZADA) {
            cita.setAsistio(true);
        }
        if (notasClinicas != null) {
            cita.setNotasClinicas(notasClinicas.trim());
        }

        return mapearADTO(citaRepository.save(cita));
    }

    private CitaResponseDTO mapearADTO(Cita c) {
        return CitaResponseDTO.builder()
                .id(c.getId())
                .psicologoNombre(c.getPsicologo().getNombre() + " " + c.getPsicologo().getApellido())
                .pacienteNombre(c.getPaciente().getNombre() + " " + c.getPaciente().getApellido())
                .fecha(c.getFecha())
                .horaInicio(c.getHoraInicio())
                .horaFin(c.getHoraFin())
                .tipoTarifa(c.getTarifa().getTipoTarifa().name())
                .montoTarifa(c.getTarifa().getMonto())
                .estadoPago(c.getEstadoPago())
                .estadoCita(c.getEstadoCita())
                .asistio(c.getAsistio())
                .notasClinicas(c.getNotasClinicas())
                .build();
    }

    @Transactional
    public CitaResponseDTO actualizarEstadoPago(Long citaId, EstadoPago nuevoEstadoPago) {
        Cita cita = citaRepository.findById(citaId)
                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada con ID: " + citaId));

        cita.setEstadoPago(nuevoEstadoPago);
        return mapearADTO(citaRepository.save(cita));
    }
}