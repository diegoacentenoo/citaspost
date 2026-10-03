package citaspost.citas.dtos.request;

import lombok.Data;

@Data
public class UsuarioRequestDTO {
    private String username;
    private String password;
    private Long personaId;
}