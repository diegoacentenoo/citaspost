package citaspost.citas.dtos.response;

import citaspost.citas.enums.EstadoCita;
import citaspost.citas.enums.EstadoPago;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CitaResponseDTO {
    private Long id;
    private PersonaResponseDTO psicologo;
    private PersonaResponseDTO paciente;
    private LocalDate fecha;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private TarifaDTO tarifa;
    private EstadoPago estadoPago;
    private EstadoCita estadoCita;
    private Boolean asistio;
}