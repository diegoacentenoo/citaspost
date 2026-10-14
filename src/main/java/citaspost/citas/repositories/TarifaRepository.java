package citaspost.citas.repositories;

import citaspost.citas.entities.Tarifa;
import citaspost.citas.enums.TipoTarifa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TarifaRepository extends JpaRepository<Tarifa, Long> {

    Optional<Tarifa> findByTipoTarifa(TipoTarifa tipoTarifa);

}