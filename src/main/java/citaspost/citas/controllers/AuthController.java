package citaspost.citas.controllers;

import citaspost.citas.dtos.request.RegistroRequestDTO;
import citaspost.citas.dtos.response.LoginResponseDTO;
import citaspost.citas.services.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody Map<String, String> credenciales) {
        return ResponseEntity.ok(authService.login(credenciales.get("username"), credenciales.get("password")));
    }

    @PostMapping("/registro")
    public ResponseEntity<Map<String, String>> registrar(@Valid @RequestBody RegistroRequestDTO dto) {
        authService.registrarUsuario(dto);
        return ResponseEntity.ok(Map.of("message", "Usuario registrado exitosamente"));
    }

    @PostMapping("/recuperar/solicitar-codigo")
    public ResponseEntity<Map<String, String>> solicitarCodigo(@RequestBody Map<String, String> body) {
        authService.generarYEnviarCodigo(body.get("cedula"));
        return ResponseEntity.ok(Map.of("message", "Código enviado"));
    }

    @PostMapping("/recuperar/validar-codigo")
    public ResponseEntity<Map<String, String>> validarCodigo(@RequestBody Map<String, String> body) {
        authService.validarCodigo(body.get("cedula"), body.get("codigo"));
        return ResponseEntity.ok(Map.of("message", "Código verificado"));
    }

    @PostMapping("/recuperar/actualizar")
    public ResponseEntity<Map<String, String>> actualizarCredenciales(@RequestBody Map<String, String> body) {
        authService.actualizarCredenciales(body.get("cedula"), body.get("username"), body.get("password"));
        return ResponseEntity.ok(Map.of("message", "Credenciales actualizadas"));
    }
}