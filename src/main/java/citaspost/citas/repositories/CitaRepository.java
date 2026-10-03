package citaspost.citas.repositories;

import citaspost.citas.entities.Cita;
import citaspost.citas.enums.EstadoCita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CitaRepository extends JpaRepository<Cita, Long> {

    List<Cita> findByOdontologoIdAndFechaAndEstadoCitaNot(Long odontologoId, LocalDate fecha, EstadoCita estadoCita);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Cita c " +
            "WHERE c.odontologo.id = :odontologoId AND c.fecha = :fecha AND c.estadoCita != 'CANCELADA' " +
            "AND ((c.horaInicio < :horaFin AND c.horaFin > :horaInicio))")
    boolean existsByOdontologoAndFechaAndHorarioSolapado(
            @Param("odontologoId") Long odontologoId, @Param("fecha") LocalDate fecha,
            @Param("horaInicio") LocalTime horaInicio, @Param("horaFin") LocalTime horaFin);

    // 🚀 NUEVO: Buscar la próxima cita del paciente
    Optional<Cita> findFirstByPacienteIdAndFechaGreaterThanEqualAndEstadoCitaNotOrderByFechaAscHoraInicioAsc(
            Long pacienteId, LocalDate fecha, EstadoCita estadoCita);
}