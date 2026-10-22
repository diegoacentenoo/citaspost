package citaspost.citas.entities;

import citaspost.citas.enums.TipoTarifa;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "tarifas")
public class Tarifa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING) // <-- ASEGÚRATE DE TENER ESTO
    @Column(name = "tipo_tarifa", nullable = false, unique = true, length = 30)
    private TipoTarifa tipoTarifa;

    @Column(nullable = false, precision = 20, scale = 4)
    private BigDecimal monto;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activa = true;
}