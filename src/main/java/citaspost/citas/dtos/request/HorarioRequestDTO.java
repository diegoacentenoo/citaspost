package citaspost.citas.dtos.request;

import citaspost.citas.enums.DiaSemana;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalTime;

@Data
public class HorarioRequestDTO {
    @NotNull(message = "El psicólogo es obligatorio")
    private Long psicologoId;
    @NotNull(message = "El día de la semana es obligatorio")
    private DiaSemana diaSemana;
    @NotNull(message = "La hora de inicio es obligatoria")
    private LocalTime horaInicio;
    @NotNull(message = "La hora de fin es obligatoria")
    private LocalTime horaFin;
}