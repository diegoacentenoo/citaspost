// CitaResponseDTO.java
package citaspost.citas.dtos.response;

import citaspost.citas.enums.EstadoCita;
import citaspost.citas.enums.EstadoPago;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CitaResponseDTO {
    private Long id;

    // Nombres concatenados
    private String psicologoNombre;
    private String pacienteNombre;

    private LocalDate fecha;
    private LocalTime horaInicio;
    private LocalTime horaFin;

    private String tipoTarifa;
    private BigDecimal montoTarifa;

    private EstadoPago estadoPago;
    private EstadoCita estadoCita;
}