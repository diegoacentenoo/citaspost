package citaspost.citas.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DatosPudiente {

    @Column(nullable = false, length = 100)
    private String nombreCompleto;

    @Column(nullable = false, length = 20)
    private String cedula;

    @Column(nullable = false, length = 20)
    private String telefono;

    @Column(nullable = false, length = 40)
    private String parentesco; // Ej: Madre, Padre, Representante Legal, Acompañante
}