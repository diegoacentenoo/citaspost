package citaspost.citas.repositories;

import citaspost.citas.entities.Persona;
import citaspost.citas.enums.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PersonaRepository extends JpaRepository<Persona, Long> {

    Optional<Persona> findByCedula (String cedula);

    Optional<Persona> findByCorreo(String correo);

    List<Persona> findByRolAndActivoTrue(Rol rol);

}
