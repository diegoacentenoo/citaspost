package citaspost.citas.dtos.request;

import citaspost.citas.enums.EstadoPago;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class CitaRequestDTO {
    private Long psicologoId;
    private Long pacienteId;
    private LocalDate fecha;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private Long tarifaId;
    private EstadoPago estadoPago;
}