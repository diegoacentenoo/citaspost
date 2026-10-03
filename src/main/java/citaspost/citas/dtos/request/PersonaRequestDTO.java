package citaspost.citas.dtos.request;

import citaspost.citas.enums.Rol;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PersonaRequestDTO {
    private String nombre;
    private String apellido;
    private String cedula;
    private String telefono;
    private String correo;
    private Rol rol;
}