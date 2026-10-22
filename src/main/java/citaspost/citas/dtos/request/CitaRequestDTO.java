package citaspost.citas.dtos.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class CitaRequestDTO {
    @NotNull(message = "Debe seleccionar un psicólogo")
    private Long psicologoId;
    @NotNull(message = "Debe indicar el paciente")
    private Long pacienteId;
    @NotNull(message = "Debe seleccionar una tarifa")
    private Long tarifaId;
    @NotNull(message = "La fecha es obligatoria")
    private LocalDate fecha;
    @NotNull(message = "La hora de inicio es obligatoria")
    private LocalTime horaInicio;
    @NotNull(message = "La hora de fin es obligatoria")
    private LocalTime horaFin;
}