package citaspost.citas.dtos.response;

import citaspost.citas.enums.Rol;
import lombok.Data;

@Data
public class AuthResponseDTO {
    private String token;
    private String username;
    private Rol rol;
}
