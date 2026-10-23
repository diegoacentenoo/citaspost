package citaspost.citas.services;

import citaspost.citas.dtos.request.CitaRequestDTO;
import citaspost.citas.dtos.response.CitaResponseDTO;
import citaspost.citas.dtos.response.CupoDisponibleDTO;
import citaspost.citas.entities.*;
import citaspost.citas.enums.DiaSemana;
import citaspost.citas.enums.EstadoCita;
import citaspost.citas.enums.EstadoPago;
import citaspost.citas.enums.Rol;
import citaspost.citas.enums.TipoTarifa;
import citaspost.citas.exception.BusinessException;
import citaspost.citas.exception.ResourceNotFoundException;
import citaspost.citas.repositories.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
    private final CitaComprobanteFotoRepository fotoRepository;
    private final ValidadorComprobanteService validadorComprobanteService;

    private void validarFechaPermitidaSegunHoraActual(LocalDate fechaSolicitada) {
        LocalDate hoy = LocalDate.now();
        LocalTime ahora = LocalTime.now();
        LocalTime horaCorte = LocalTime.of(17, 0);

        if (!fechaSolicitada.isAfter(hoy)) {
            throw new BusinessException("No se permite agendar citas para el mismo día ni en fechas pasadas.");
        }

        if (!ahora.isBefore(horaCorte) && fechaSolicitada.equals(hoy.plusDays(1))) {
            throw new BusinessException("Pasadas las 5:00 PM no se permite agendar citas para el día siguiente. Debe agendar a partir del " + hoy.plusDays(2) + ".");
        }
    }

    @Transactional
    public CitaResponseDTO agendarCitaConComprobante(CitaRequestDTO dto, MultipartFile imagen) throws IOException {
        validarFechaPermitidaSegunHoraActual(dto.getFecha());

        if (!dto.getHoraFin().isAfter(dto.getHoraInicio())) {
            throw new BusinessException("La hora de fin debe ser posterior a la hora de inicio.");
        }

        validadorComprobanteService.validarImagenDePago(imagen);

        Persona psicologo = personaRepository.findById(dto.getPsicologoId())
                .orElseThrow(() -> new ResourceNotFoundException("Psicólogo no encontrado."));
        if (psicologo.getRol() != Rol.PSICOLOGO || !Boolean.TRUE.equals(psicologo.getActivo())) {
            throw new BusinessException("La persona seleccionada no es un psicólogo activo.");
        }

        Persona paciente = personaRepository.findById(dto.getPacienteId())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado."));

        // Asignación automática de tarifa: Estudiante -> ESPECIAL | No estudiante -> GENERAL
        TipoTarifa tipoTarifaCorrespondiente = Boolean.TRUE.equals(paciente.getEsEstudiante())
                ? TipoTarifa.ESPECIAL
                : TipoTarifa.GENERAL;

        Tarifa tarifa = tarifaRepository.findByTipoTarifa(tipoTarifaCorrespondiente)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró configurada la tarifa " + tipoTarifaCorrespondiente));

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

        boolean hayCruce = citaRepository.existsByPsicologoAndFechaAndHorarioSolapado(
                psicologo.getId(), dto.getFecha(), dto.getHoraInicio(), dto.getHoraFin());
        if (hayCruce) {
            throw new BusinessException("Ese cupo ya se encuentra ocupado. Por favor seleccione otra hora.");
        }

        List<DatosPudiente> pudientesCita = new ArrayList<>();
        if (dto.getPudientes() != null) {
            for (DatosPudiente p : dto.getPudientes()) {
                if (p.getNombreCompleto() != null && !p.getNombreCompleto().isBlank()) {
                    pudientesCita.add(p);
                }
            }
        }

        if (paciente.isMenorDeEdad()) {
            if (pudientesCita.isEmpty() && paciente.getPudientes() != null) {
                pudientesCita.addAll(paciente.getPudientes());
            }
            if (pudientesCita.isEmpty() || pudientesCita.size() > 2) {
                throw new BusinessException("El paciente es menor de edad y requiere tener 1 o 2 pudientes asignados a la cita.");
            }
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
                .pudientes(pudientesCita)
                .build();

        Cita citaGuardada = citaRepository.save(nuevaCita);

        CitaComprobanteFoto foto = new CitaComprobanteFoto();
        foto.setCita(citaGuardada);
        foto.setFotoBytes(imagen.getBytes());
        foto.setMimeType(imagen.getContentType());
        fotoRepository.save(foto);

        return mapearADTO(citaGuardada);
    }

    public List<CupoDisponibleDTO> obtenerCuposDisponibles(Long psicologoId, LocalDate fecha) {
        LocalDate hoy = LocalDate.now();
        LocalTime ahora = LocalTime.now();
        if (!fecha.isAfter(hoy) || (!ahora.isBefore(LocalTime.of(17, 0)) && fecha.equals(hoy.plusDays(1)))) {
            return List.of();
        }

        DiaSemana dia = convertirDiaSemana(fecha.getDayOfWeek());
        List<HorarioPsicologo> bloquesHorario = horarioRepository
                .findByPsicologoIdAndDiaSemanaAndActivoTrue(psicologoId, dia);

        if (bloquesHorario.isEmpty()) return List.of();

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
    public CitaResponseDTO anexarPudienteACita(Long citaId, DatosPudiente nuevoPudiente) {
        Cita cita = citaRepository.findById(citaId)
                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada."));

        if (cita.getPudientes().size() >= 2) {
            throw new BusinessException("La cita ya cuenta con el máximo de 2 pudientes/acompañantes registrados.");
        }
        cita.getPudientes().add(nuevoPudiente);
        return mapearADTO(citaRepository.save(cita));
    }

    public CitaComprobanteFoto obtenerComprobante(Long citaId) {
        return fotoRepository.findById(citaId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró comprobante para la cita #" + citaId));
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

    @Transactional
    public CitaResponseDTO actualizarEstadoCita(Long citaId, EstadoCita nuevoEstado, Boolean asistio, String notasClinicas) {
        Cita cita = citaRepository.findById(citaId)
                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada con ID: " + citaId));
        if (nuevoEstado != null) cita.setEstadoCita(nuevoEstado);
        if (asistio != null) cita.setAsistio(asistio);
        else if (nuevoEstado == EstadoCita.REALIZADA) cita.setAsistio(true);
        if (notasClinicas != null) cita.setNotasClinicas(notasClinicas.trim());
        return mapearADTO(citaRepository.save(cita));
    }

    @Transactional
    public CitaResponseDTO actualizarEstadoPago(Long citaId, EstadoPago nuevoEstadoPago) {
        Cita cita = citaRepository.findById(citaId)
                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada con ID: " + citaId));
        cita.setEstadoPago(nuevoEstadoPago);
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
                .stream().map(this::mapearADTO).toList();
    }

    public List<CitaResponseDTO> obtenerCitasPorPsicologoYFecha(Long psicologoId, LocalDate fecha) {
        return citaRepository.findByPsicologoIdAndFechaAndEstadoCitaNotOrderByHoraInicioAsc(
                        psicologoId, fecha, EstadoCita.CANCELADA)
                .stream().map(this::mapearADTO).toList();
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

    private CitaResponseDTO mapearADTO(Cita c) {
        Persona pac = c.getPaciente();
        return CitaResponseDTO.builder()
                .id(c.getId())
                .psicologoNombre(c.getPsicologo().getNombre() + " " + c.getPsicologo().getApellido())
                .pacienteNombre(pac.getNombre() + " " + pac.getApellido())
                .pacienteMenorDeEdad(pac.isMenorDeEdad())
                .pacienteEstudiante(pac.getEsEstudiante())
                .fecha(c.getFecha())
                .horaInicio(c.getHoraInicio())
                .horaFin(c.getHoraFin())
                .tipoTarifa(c.getTarifa().getTipoTarifa().name())
                .montoTarifa(c.getTarifa().getMonto())
                .estadoPago(c.getEstadoPago())
                .estadoCita(c.getEstadoCita())
                .asistio(c.getAsistio())
                .notasClinicas(c.getNotasClinicas())
                .pudientes(c.getPudientes())
                .tieneComprobante(fotoRepository.existsById(c.getId()))
                .build();
    }
}