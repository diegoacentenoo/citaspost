package citaspost.citas.dtos.response; // o en un paquete general dtos

import citaspost.citas.enums.TipoTarifa;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TarifaDTO {
    private Long id;
    private TipoTarifa tipoTarifa;
    private BigDecimal monto;
    private Boolean activa;
}