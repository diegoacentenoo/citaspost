package citaspost.citas.repositories;

import citaspost.citas.entities.HorarioPsicologo;
import citaspost.citas.entities.Persona;
import citaspost.citas.enums.DiaSemana;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HorarioPsicologoRepository extends JpaRepository<HorarioPsicologo, Long> {

    List<HorarioPsicologo> findByPsicologo(Persona psicologo);
    List<HorarioPsicologo> findByPsicologoAndDiaSemanaAndActivoTrue(Persona psicologo, DiaSemana diaSemana);
}