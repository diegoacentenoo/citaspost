package citaspost.citas.dtos.request;

import citaspost.citas.entities.DatosPudiente;
import citaspost.citas.enums.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class RegistroRequestDTO {
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;
    @NotBlank(message = "El apellido es obligatorio")
    private String apellido;
    @NotBlank(message = "La cédula es obligatoria")
    private String cedula;
    @NotBlank(message = "El teléfono es obligatorio")
    private String telefono;
    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "Formato de correo inválido")
    private String correo;
    @NotNull(message = "La fecha de nacimiento es obligatoria")
    private LocalDate fechaNacimiento;
    private Boolean esEstudiante = false;
    @NotBlank(message = "El usuario es obligatorio")
    private String username;
    @NotBlank(message = "La contraseña es obligatoria")
    private String password;
    @NotNull(message = "El rol es obligatorio")
    private Rol rol;

    // Obligatorio (1 o 2) si la persona es menor de 18 años
    private List<DatosPudiente> pudientes;
}