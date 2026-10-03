package citaspost.citas.repositories;

import citaspost.citas.entities.Citas;
import citaspost.citas.entities.Persona;
import citaspost.citas.enums.EstadoCita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CitasRepository extends JpaRepository<Citas, Long> {

    List<Citas> findByPaciente(Persona paciente);

    List<Citas> findByPsicologoAndFecha(Persona psicologo, LocalDate fecha);

    List<Citas> findByEstadoCita(EstadoCita estadoCita);
}