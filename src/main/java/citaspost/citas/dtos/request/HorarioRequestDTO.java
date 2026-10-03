package citaspost.citas.dtos.request;

import citaspost.citas.enums.DiaSemana;
import lombok.Data;
import java.time.LocalTime;

@Data
public class HorarioRequestDTO {
    private Long psicologoId;
    private DiaSemana diaSemana;
    private LocalTime horaInicio;
    private LocalTime horaFin;
}