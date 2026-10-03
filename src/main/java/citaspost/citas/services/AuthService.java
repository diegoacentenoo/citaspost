package citaspost.citas.services;

import citaspost.citas.dtos.request.RegistroRequestDTO;
import citaspost.citas.entities.Persona;
import citaspost.citas.entities.Usuario;
import citaspost.citas.exception.BusinessException;
import citaspost.citas.exception.ResourceNotFoundException;
import citaspost.citas.repositories.PersonaRepository;
import citaspost.citas.repositories.UsuarioRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import citaspost.citas.services.EmailService;

import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PersonaRepository personaRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    // Almacén temporal en memoria para los códigos (Para producción, usar Redis o Base de Datos)
    private final Map<String, String> codigosRecuperacion = new ConcurrentHashMap<>();

    @Transactional
    public Usuario registrarUsuario(RegistroRequestDTO dto) {
        if (usuarioRepository.existsByUsername(dto.getUsername())) {
            throw new BusinessException("El nombre de usuario / correo ya se encuentra en uso.");
        }

        // 1. Guardar los datos personales
        Persona persona = Persona.builder()
                .nombre(dto.getNombre())
                .apellido(dto.getApellido())
                .cedula(dto.getCedula())
                .telefono(dto.getTelefono())
                .correo(dto.getCorreo())
                .rol(dto.getRol())
                .activo(true)
                .build();

        Persona personaGuardada = personaRepository.save(persona);

        // 2. Guardar las credenciales cifradas con la relación 1 a 1
        Usuario usuario = Usuario.builder()
                .username(dto.getUsername())
                .password(passwordEncoder.encode(dto.getPassword()))
                .persona(personaGuardada)
                .build();

        return usuarioRepository.save(usuario);
    }

    public Optional<Usuario> autenticar(String username, String password) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByUsername(username);
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            if (passwordEncoder.matches(password, usuario.getPassword())) {
                return Optional.of(usuario);
            }
        }
        return Optional.empty();
    }

    // --- MÉTODOS DE RECUPERACIÓN ---

    public void generarYEnviarCodigo(String cedula) {
        Persona persona = personaRepository.findByCedula(cedula)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró ningún usuario con esa cédula."));

        // Generar código de 4 dígitos
        String codigo = String.format("%04d", new Random().nextInt(10000));
        codigosRecuperacion.put(cedula, codigo);

        // Enviar por correo
        emailService.enviarCodigoRecuperacion(persona.getCorreo(), codigo);
    }

    public void validarCodigo(String cedula, String codigoIngresado) {
        String codigoReal = codigosRecuperacion.get(cedula);
        if (codigoReal == null || !codigoReal.equals(codigoIngresado)) {
            throw new BusinessException("El código ingresado es incorrecto o ha expirado.");
        }
    }

    @Transactional
    public void actualizarCredenciales(String cedula, String nuevoUsername, String nuevaClave) {
        Persona persona = personaRepository.findByCedula(cedula)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la cédula."));

        Usuario usuario = persona.getUsuario();

        // Verificar que el nuevo username no esté ocupado por otra persona
        if (!usuario.getUsername().equals(nuevoUsername) && usuarioRepository.existsByUsername(nuevoUsername)) {
            throw new BusinessException("El nuevo nombre de usuario ya está en uso.");
        }

        usuario.setUsername(nuevoUsername);
        usuario.setPassword(passwordEncoder.encode(nuevaClave));
        usuarioRepository.save(usuario);

        // Limpiar el código de seguridad
        codigosRecuperacion.remove(cedula);
    }
}