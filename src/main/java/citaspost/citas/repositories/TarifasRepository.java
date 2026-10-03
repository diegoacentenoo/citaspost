package citaspost.citas.repositories;

import citaspost.citas.entities.Tarifas;
import citaspost.citas.enums.TipoTarifa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TarifasRepository extends JpaRepository<Tarifas, Long> {

    Optional<Tarifas> findByTipoTarifa(TipoTarifa tipoTarifa);

}