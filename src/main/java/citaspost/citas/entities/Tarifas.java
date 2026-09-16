package citaspost.citas.entities;

import citaspost.citas.enums.TipoTarifa;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "tarifas")
public class Tarifas {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_tarifa", nullable = false, unique = true)
    private TipoTarifa tipoTarifa;

    @Column(nullable = false, precision = 20, scale = 4)
    private BigDecimal monto;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activa = true;
}
