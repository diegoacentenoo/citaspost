package citaspost.citas.controllers;

import citaspost.citas.config.JwtUtil;
import citaspost.citas.dtos.request.LoginRequestDTO;
import citaspost.citas.dtos.request.RegistroRequestDTO;
import citaspost.citas.dtos.response.LoginResponseDTO;
import citaspost.citas.entities.Usuario;
import citaspost.citas.services.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO request) {
        Optional<Usuario> usuarioOpt = authService.autenticar(request.getUsername(), request.getPassword());

        if (usuarioOpt.isPresent()) {
            Usuario user = usuarioOpt.get();

            // Validar si el usuario está activo
            if (!user.getPersona().getActivo()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Su usuario se encuentra inactivo. Contacte al administrador."));
            }

            String token = jwtUtil.generarToken(user.getUsername());

            LoginResponseDTO response = LoginResponseDTO.builder()
                    .token(token)
                    .id(user.getId())
                    .username(user.getUsername())
                    .rol(user.getPersona().getRol().name())
                    .nombre(user.getPersona().getNombre())
                    .apellido(user.getPersona().getApellido())
                    .build();

            return ResponseEntity.ok(response);
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Credenciales incorrectas"));
    }

    @PostMapping("/registro")
    public ResponseEntity<?> registrar(@Valid @RequestBody RegistroRequestDTO request) {
        Usuario nuevoUsuario = authService.registrarUsuario(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("mensaje", "Usuario registrado exitosamente con ID: " + nuevoUsuario.getId()));
    }

    // --- ENDPOINTS DE RECUPERACIÓN ---

    @PostMapping("/recuperar/solicitar-codigo")
    public ResponseEntity<?> solicitarCodigo(@RequestBody Map<String, String> body) {
        authService.generarYEnviarCodigo(body.get("cedula"));
        return ResponseEntity.ok(Map.of("mensaje", "Código enviado al correo registrado."));
    }

    @PostMapping("/recuperar/validar-codigo")
    public ResponseEntity<?> validarCodigo(@RequestBody Map<String, String> body) {
        authService.validarCodigo(body.get("cedula"), body.get("codigo"));
        return ResponseEntity.ok(Map.of("mensaje", "Código válido."));
    }

    @PostMapping("/recuperar/actualizar")
    public ResponseEntity<?> actualizarCredenciales(@RequestBody Map<String, String> body) {
        authService.actualizarCredenciales(body.get("cedula"), body.get("username"), body.get("password"));
        return ResponseEntity.ok(Map.of("mensaje", "Credenciales actualizadas exitosamente."));
    }
}