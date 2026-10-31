package citaspost.citas.dtos.request;

import citaspost.citas.entities.DatosPudiente;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

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
    @NotBlank(message = "El número de comprobante de pago es obligatorio")
    private String numeroComprobante;

    private List<DatosPudiente> pudientes;
}