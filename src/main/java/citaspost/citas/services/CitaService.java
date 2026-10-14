package citaspost.citas.services;

import citaspost.citas.entities.Cita;
import citaspost.citas.entities.Persona;
import citaspost.citas.entities.Tarifa;
import citaspost.citas.enums.EstadoCita;
import citaspost.citas.enums.EstadoPago;
import citaspost.citas.exception.*;
import citaspost.citas.repositories.CitaRepository;
import citaspost.citas.repositories.PersonaRepository;
import citaspost.citas.repositories.TarifaRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CitaService {

    private final CitaRepository citaRepository;
    private final PersonaRepository personaRepository;
    private final TarifaRepository tarifaRepository;

    @Transactional
    public Cita agendarCita(Long psicologoId, Long pacienteId, Long tarifaId,
                            LocalDate fecha, LocalTime horaInicio, LocalTime horaFin) {

        if (horaFin.isBefore(horaInicio) || horaFin.equals(horaInicio)) {
            throw new BusinessException("La hora de fin debe ser posterior a la hora de inicio.");
        }

        Persona psicologo = personaRepository.findById(psicologoId)
                .orElseThrow(() -> new ResourceNotFoundException("Psicólogo no encontrado con ID: " + psicologoId));

        Persona paciente = personaRepository.findById(pacienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado con ID: " + pacienteId));

        Tarifa tarifa = tarifaRepository.findById(tarifaId)
                .orElseThrow(() -> new ResourceNotFoundException("Tarifa no encontrada con ID: " + tarifaId));

        boolean hayCruce = citaRepository.existsByPsicologoAndFechaAndHorarioSolapado(
                psicologoId, fecha, horaInicio, horaFin);

        if (hayCruce) {
            throw new BusinessException("El psicólogo ya tiene una cita programada o un cruce de horario en la fecha solicitada.");
        }

        Cita nuevaCita = Cita.builder()
                .psicologo(psicologo)
                .paciente(paciente)
                .fecha(fecha)
                .horaInicio(horaInicio)
                .horaFin(horaFin)
                .tarifa(tarifa)
                .estadoPago(EstadoPago.NOPAGADO)
                .estadoCita(EstadoCita.PENDIENTE)
                .asistio(false)
                .build();

        return citaRepository.save(nuevaCita);
    }

    public List<Cita> obtenerCitasPorPsicologoYFecha(Long psicologoId, LocalDate fecha) {
        personaRepository.findById(psicologoId)
                .orElseThrow(() -> new ResourceNotFoundException("Psicólogo no encontrado con ID: " + psicologoId));

        return citaRepository.findByPsicologoIdAndFechaAndEstadoCitaNot(
                psicologoId, fecha, EstadoCita.CANCELADA);
    }

    public Cita obtenerProximaCitaPaciente(Long pacienteId) {
        return citaRepository.findFirstByPacienteIdAndFechaGreaterThanEqualAndEstadoCitaNotOrderByFechaAscHoraInicioAsc(
                pacienteId, LocalDate.now(), EstadoCita.CANCELADA).orElse(null);
    }
}