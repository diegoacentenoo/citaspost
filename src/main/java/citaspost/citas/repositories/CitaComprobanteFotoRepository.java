package citaspost.citas.repositories;

import citaspost.citas.entities.CitaComprobanteFoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CitaComprobanteFotoRepository extends JpaRepository<CitaComprobanteFoto, Long> {
}