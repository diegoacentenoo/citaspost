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
    public Cita agendarCita(Long odontologoId, Long pacienteId, Long tarifaId,
                            LocalDate fecha, LocalTime horaInicio, LocalTime horaFin) {

        // 1. Validar lógica de horas
        if (horaFin.isBefore(horaInicio) || horaFin.equals(horaInicio)) {
            throw new BusinessException("La hora de fin debe ser posterior a la hora de inicio.");
        }

        // 2. Obtener entidades relacionadas (Lanza ResourceNotFoundException 404 si fallan)
        Persona odontologo = personaRepository.findById(odontologoId)
                .orElseThrow(() -> new ResourceNotFoundException("Odontólogo no encontrado con ID: " + odontologoId));

        Persona paciente = personaRepository.findById(pacienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado con ID: " + pacienteId));

        Tarifa tarifa = tarifaRepository.findById(tarifaId)
                .orElseThrow(() -> new ResourceNotFoundException("Tarifa no encontrada con ID: " + tarifaId));

        // 3. Validar cruce de horarios (Lanza BusinessException 400 si falla)
        boolean hayCruce = citaRepository.existsByOdontologoAndFechaAndHorarioSolapado(
                odontologoId, fecha, horaInicio, horaFin);

        if (hayCruce) {
            throw new BusinessException("El odontólogo ya tiene una cita programada o un cruce de horario en la fecha solicitada.");
        }

        // 4. Construir y guardar la nueva cita
        Cita nuevaCita = Cita.builder()
                .odontologo(odontologo)
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

    // Añade este método en CitaService.java
    public List<Cita> obtenerCitasPorOdontologoYFecha(Long odontologoId, LocalDate fecha) {
        // Validación opcional de existencia del odontólogo
        personaRepository.findById(odontologoId)
                .orElseThrow(() -> new ResourceNotFoundException("Odontólogo no encontrado con ID: " + odontologoId));

        return citaRepository.findByOdontologoIdAndFechaAndEstadoCitaNot(
                odontologoId,
                fecha,
                EstadoCita.CANCELADA // No queremos mostrar en agenda las citas que se cancelaron
        );
    }

    // 🚀 NUEVO MÉTODO
    public Cita obtenerProximaCitaPaciente(Long pacienteId) {
        return citaRepository.findFirstByPacienteIdAndFechaGreaterThanEqualAndEstadoCitaNotOrderByFechaAscHoraInicioAsc(
                pacienteId, LocalDate.now(), EstadoCita.CANCELADA).orElse(null);
    }
}