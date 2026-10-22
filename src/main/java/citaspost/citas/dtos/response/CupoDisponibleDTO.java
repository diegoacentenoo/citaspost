package citaspost.citas.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CupoDisponibleDTO {
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private boolean disponible;
}