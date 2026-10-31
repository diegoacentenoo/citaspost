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
import java.time.LocalDateTime;
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

    /**
     * Regla 2: Si ya pasaron más de 24 horas después de una cita PENDIENTE o CONFIRMADA
     * y no fue marcada como REALIZADA, pasa automáticamente a SUSPENDIDA.
     */

    @Transactional
    @org.springframework.scheduling.annotation.Scheduled(fixedRate = 60000)
    public void verificarYSuspenderCitasVencidas() {
        List<Cita> activas = citaRepository.findByEstadoCitaIn(List.of(EstadoCita.PENDIENTE, EstadoCita.CONFIRMADA));
        LocalDateTime ahora = LocalDateTime.now();

        for (Cita c : activas) {
            LocalDateTime limite24Horas = LocalDateTime.of(c.getFecha(), c.getHoraFin()).plusHours(24);
            if (ahora.isAfter(limite24Horas)) {
                c.setEstadoCita(EstadoCita.SUSPENDIDA);
                c.setAsistio(false);
                citaRepository.save(c);
            }
        }
    }

    @Transactional
    public CitaResponseDTO agendarCitaConComprobante(CitaRequestDTO dto, MultipartFile imagen, Long usuarioAutenticadoId) throws IOException {
        verificarYSuspenderCitasVencidas();
        validarFechaPermitidaSegunHoraActual(dto.getFecha());

        if (!dto.getHoraFin().isAfter(dto.getHoraInicio())) {
            throw new BusinessException("La hora de fin debe ser posterior a la hora de inicio.");
        }

        // Regla 1: Verificar que el número de comprobante no esté repetido
        String numComprobanteLimpio = dto.getNumeroComprobante().trim();
        if (citaRepository.existsByNumeroComprobante(numComprobanteLimpio)) {
            throw new BusinessException("El número de comprobante '" + numComprobanteLimpio + "' ya fue utilizado en otra cita.");
        }

        Persona psicologo = personaRepository.findById(dto.getPsicologoId())
                .orElseThrow(() -> new ResourceNotFoundException("Psicólogo no encontrado."));
        if (psicologo.getRol() != Rol.PSICOLOGO || !Boolean.TRUE.equals(psicologo.getActivo())) {
            throw new BusinessException("El psicólogo seleccionado no se encuentra activo.");
        }

        Persona paciente = personaRepository.findById(dto.getPacienteId())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado."));

        if (psicologo.getId().equals(paciente.getId())) {
            throw new BusinessException("Un psicólogo no puede agendarse una consulta consigo mismo. Seleccione otro especialista.");
        }

        // Regla 6: Máximo 2 citas pendientes por asistir
        long citasPorAsistir = citaRepository.countByPacienteIdAndEstadoCitaIn(
                paciente.getId(), List.of(EstadoCita.PENDIENTE, EstadoCita.CONFIRMADA));
        if (citasPorAsistir >= 2) {
            throw new BusinessException("Ya tienes el máximo permitido de 2 citas pendientes por asistir. Cuando asistas a una de tus citas programadas podrás agendar otra.");
        }

        // Regla 5: Bloqueo de psicólogo asignado después de la primera cita
        boolean ejecutadoPorAdmin = false;
        if (usuarioAutenticadoId != null) {
            Persona ejecutor = personaRepository.findById(usuarioAutenticadoId).orElse(null);
            if (ejecutor != null && ejecutor.getRol() == Rol.ADMINISTRADOR) {
                ejecutadoPorAdmin = true;
            }
        }

        Persona psicologoFijo = paciente.getPsicologoAsignado();
        if (psicologoFijo != null && Boolean.TRUE.equals(psicologoFijo.getActivo())) {
            if (!psicologoFijo.getId().equals(psicologo.getId()) && !ejecutadoPorAdmin) {
                throw new BusinessException("Tu psicólogo asignado es Dr(a). " + psicologoFijo.getNombre() + " " + psicologoFijo.getApellido() +
                        ". No puedes cambiar de especialista a menos que el Administrador lo autorice o el psicólogo renuncie.");
            }
        }

        // Regla 3: Tarifa ESPECIAL para Estudiantes O usuarios con rol PSICOLOGO; GENERAL para los demás
        boolean aplicaTarifaEspecial = Boolean.TRUE.equals(paciente.getEsEstudiante()) || paciente.getRol() == Rol.PSICOLOGO;
        TipoTarifa tipoTarifaCorrespondiente = aplicaTarifaEspecial ? TipoTarifa.ESPECIAL : TipoTarifa.GENERAL;

        Tarifa tarifa = tarifaRepository.findByTipoTarifa(tipoTarifaCorrespondiente)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró configurada la tarifa " + tipoTarifaCorrespondiente));

        // Validar horario laboral del psicólogo
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

        // Regla 1: Validar imagen del recibo y que el número de comprobante concuerde con la foto
        validadorComprobanteService.validarImagenYNumeroComprobante(imagen, numComprobanteLimpio);

        // Pudientes / Acompañantes
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

        // Fijar o actualizar el psicólogo asignado al paciente
        paciente.setPsicologoAsignado(psicologo);
        personaRepository.save(paciente);

        // Como el comprobante y su número fueron validados exitosamente, pasa a PAGADO y CONFIRMADA automáticamente
        Cita nuevaCita = Cita.builder()
                .psicologo(psicologo)
                .paciente(paciente)
                .fecha(dto.getFecha())
                .horaInicio(dto.getHoraInicio())
                .horaFin(dto.getHoraFin())
                .tarifa(tarifa)
                .numeroComprobante(numComprobanteLimpio)
                .estadoPago(EstadoPago.PAGADO)
                .estadoCita(EstadoCita.CONFIRMADA)
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
        verificarYSuspenderCitasVencidas();
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
        if (cita.getEstadoCita() == EstadoCita.REALIZADA || cita.getEstadoCita() == EstadoCita.SUSPENDIDA) {
            throw new BusinessException("No se puede cancelar una cita que ya fue realizada o suspendida.");
        }
        cita.setEstadoCita(EstadoCita.CANCELADA);
        return mapearADTO(citaRepository.save(cita));
    }

    /**
     * Regla 2: Solo permite marcar como REALIZADA si ya llegó la hora de la cita
     * y no han pasado más de 24 horas. Si pasaron más de 24h, la pasa a SUSPENDIDA.
     */
    @Transactional
    public CitaResponseDTO actualizarEstadoCita(Long citaId, EstadoCita nuevoEstado, Boolean asistio, String notasClinicas) {
        verificarYSuspenderCitasVencidas();

        Cita cita = citaRepository.findById(citaId)
                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada con ID: " + citaId));

        if (cita.getEstadoCita() == EstadoCita.SUSPENDIDA && nuevoEstado == EstadoCita.REALIZADA) {
            throw new BusinessException("Esta cita se encuentra SUSPENDIDA automáticamente porque transcurrieron más de 24 horas sin marcar asistencia.");
        }

        if (nuevoEstado == EstadoCita.REALIZADA) {
            LocalDateTime ahora = LocalDateTime.now();
            LocalDateTime inicioCita = LocalDateTime.of(cita.getFecha(), cita.getHoraInicio());
            LocalDateTime limite24h = LocalDateTime.of(cita.getFecha(), cita.getHoraFin()).plusHours(24);

            if (ahora.isBefore(inicioCita)) {
                throw new BusinessException("Aún no se puede marcar como REALIZADA. Solo se habilita el día de la cita una vez iniciada o cumplida la hora (" +
                        cita.getFecha() + " a las " + cita.getHoraInicio() + ").");
            }
            if (ahora.isAfter(limite24h)) {
                cita.setEstadoCita(EstadoCita.SUSPENDIDA);
                cita.setAsistio(false);
                citaRepository.save(cita);
                throw new BusinessException("Han pasado más de 24 horas desde la hora de la cita. Se ha marcado automáticamente como SUSPENDIDA.");
            }
            cita.setAsistio(true);
        }

        if (nuevoEstado != null) {
            cita.setEstadoCita(nuevoEstado);
        }
        if (asistio != null && nuevoEstado != EstadoCita.REALIZADA) {
            cita.setAsistio(asistio);
        }
        if (notasClinicas != null) {
            cita.setNotasClinicas(notasClinicas.trim());
        }
        return mapearADTO(citaRepository.save(cita));
    }

    @Transactional
    public CitaResponseDTO actualizarEstadoPago(Long citaId, EstadoPago nuevoEstadoPago) {
        Cita cita = citaRepository.findById(citaId)
                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada con ID: " + citaId));
        cita.setEstadoPago(nuevoEstadoPago);
        return mapearADTO(citaRepository.save(cita));
    }

    @Transactional
    public CitaResponseDTO obtenerProximaCitaPaciente(Long pacienteId) {
        verificarYSuspenderCitasVencidas();
        return citaRepository
                .findFirstByPacienteIdAndEstadoCitaInOrderByFechaAscHoraInicioAsc(
                        pacienteId, List.of(EstadoCita.PENDIENTE, EstadoCita.CONFIRMADA))
                .map(this::mapearADTO)
                .orElse(null);
    }

    @Transactional
    public List<CitaResponseDTO> obtenerMisCitasPaciente(Long pacienteId) {
        verificarYSuspenderCitasVencidas();
        return citaRepository.findByPacienteIdOrderByFechaDescHoraInicioDesc(pacienteId)
                .stream().map(this::mapearADTO).toList();
    }

    @Transactional
    public List<CitaResponseDTO> obtenerCitasPorPsicologoYFecha(Long psicologoId, LocalDate fecha) {
        verificarYSuspenderCitasVencidas();
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
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime inicioCita = LocalDateTime.of(c.getFecha(), c.getHoraInicio());
        LocalDateTime limite24h = LocalDateTime.of(c.getFecha(), c.getHoraFin()).plusHours(24);

        boolean enVentanaParaRealizar = (c.getEstadoCita() == EstadoCita.PENDIENTE || c.getEstadoCita() == EstadoCita.CONFIRMADA)
                && !ahora.isBefore(inicioCita)
                && !ahora.isAfter(limite24h);

        return CitaResponseDTO.builder()
                .id(c.getId())
                .psicologoId(c.getPsicologo().getId())
                .psicologoNombre(c.getPsicologo().getNombre() + " " + c.getPsicologo().getApellido())
                .pacienteId(pac.getId())
                .pacienteNombre(pac.getNombre() + " " + pac.getApellido())
                .pacienteMenorDeEdad(pac.isMenorDeEdad())
                .pacienteEstudiante(pac.getEsEstudiante())
                .fecha(c.getFecha())
                .horaInicio(c.getHoraInicio())
                .horaFin(c.getHoraFin())
                .tipoTarifa(c.getTarifa().getTipoTarifa().name())
                .montoTarifa(c.getTarifa().getMonto())
                .numeroComprobante(c.getNumeroComprobante())
                .estadoPago(c.getEstadoPago())
                .estadoCita(c.getEstadoCita())
                .asistio(c.getAsistio())
                .notasClinicas(c.getNotasClinicas())
                .pudientes(c.getPudientes())
                .tieneComprobante(fotoRepository.existsById(c.getId()))
                .puedeMarcarRealizada(enVentanaParaRealizar)
                .build();
    }
}