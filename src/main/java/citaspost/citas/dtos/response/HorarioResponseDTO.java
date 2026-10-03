package citaspost.citas.dtos.response;

import citaspost.citas.enums.DiaSemana;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HorarioResponseDTO {
    private Long id;
    private Long psicologoId; // Puede ser el ID o el PersonaResponseDTO, según convenga en tu frontend
    private String nombrePsicologo;
    private DiaSemana diaSemana;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private Boolean activo;
}