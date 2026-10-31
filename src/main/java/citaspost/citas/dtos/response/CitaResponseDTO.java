package citaspost.citas.dtos.response;

import citaspost.citas.entities.DatosPudiente;
import citaspost.citas.enums.EstadoCita;
import citaspost.citas.enums.EstadoPago;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CitaResponseDTO {
    private Long id;
    private Long psicologoId;
    private String psicologoNombre;
    private Long pacienteId;
    private String pacienteNombre;
    private Boolean pacienteMenorDeEdad;
    private Boolean pacienteEstudiante;
    private LocalDate fecha;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private String tipoTarifa;
    private BigDecimal montoTarifa;
    private String numeroComprobante;
    private EstadoPago estadoPago;
    private EstadoCita estadoCita;
    private Boolean asistio;
    private String notasClinicas;
    private List<DatosPudiente> pudientes;
    private Boolean tieneComprobante;
    private Boolean puedeMarcarRealizada; // True solo entre la hora de la cita y 24h después
}