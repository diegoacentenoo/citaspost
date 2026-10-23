package citaspost.citas.dtos.response;

import citaspost.citas.entities.DatosPudiente;
import citaspost.citas.enums.Rol;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PersonaResponseDTO {
    private Long id;
    private String nombre;
    private String apellido;
    private String cedula;
    private String telefono;
    private String correo;
    private LocalDate fechaNacimiento;
    private Integer edad;
    private Boolean menorDeEdad;
    private Boolean esEstudiante;
    private List<DatosPudiente> pudientes;
    private Rol rol;
    private Boolean activo;
}