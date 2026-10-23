package citaspost.citas.services;

import citaspost.citas.dtos.request.RegistroRequestDTO;
import citaspost.citas.dtos.response.LoginResponseDTO;
import citaspost.citas.entities.DatosPudiente;
import citaspost.citas.entities.Persona;
import citaspost.citas.entities.Usuario;
import citaspost.citas.enums.Rol;
import citaspost.citas.exception.BusinessException;
import citaspost.citas.exception.ResourceNotFoundException;
import citaspost.citas.repositories.PersonaRepository;
import citaspost.citas.repositories.UsuarioRepository;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PersonaRepository personaRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    private final Map<String, String> codigosRecuperacion = new ConcurrentHashMap<>();
    private final Map<String, Long> sesionesActivas = new ConcurrentHashMap<>();

    @PostConstruct
    @Transactional
    public void crearAdminPorDefecto() {
        if (!usuarioRepository.existsByUsername("admin")) {
            Persona adminPersona = Persona.builder()
                    .nombre("Administrador")
                    .apellido("General")
                    .cedula("00000000")
                    .telefono("00000000000")
                    .correo("admin@psicosalud.com")
                    .fechaNacimiento(LocalDate.of(1990, 1, 1))
                    .esEstudiante(false)
                    .rol(Rol.ADMINISTRADOR)
                    .activo(true)
                    .build();

            Persona guardada = personaRepository.save(adminPersona);

            Usuario adminUsuario = Usuario.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .persona(guardada)
                    .build();

            usuarioRepository.save(adminUsuario);
        }
    }

    @Transactional
    public Usuario registrarUsuario(RegistroRequestDTO dto) {
        if (usuarioRepository.existsByUsername(dto.getUsername())) {
            throw new BusinessException("El nombre de usuario ya se encuentra en uso.");
        }
        if (personaRepository.existsByCedula(dto.getCedula())) {
            throw new BusinessException("Ya existe una persona registrada con esta cédula.");
        }
        if (personaRepository.existsByCorreo(dto.getCorreo())) {
            throw new BusinessException("El correo electrónico ya está registrado.");
        }
        if (personaRepository.existsByTelefono(dto.getTelefono())) {
            throw new BusinessException("El número de teléfono ya está registrado.");
        }
        if (dto.getFechaNacimiento().isAfter(LocalDate.now())) {
            throw new BusinessException("La fecha de nacimiento no puede ser futura.");
        }

        int edad = Period.between(dto.getFechaNacimiento(), LocalDate.now()).getYears();
        boolean esMenor = edad < 18;

        List<DatosPudiente> pudientesLimpios = new ArrayList<>();
        if (dto.getPudientes() != null) {
            for (DatosPudiente p : dto.getPudientes()) {
                if (p.getNombreCompleto() != null && !p.getNombreCompleto().isBlank()) {
                    pudientesLimpios.add(p);
                }
            }
        }

        if (esMenor) {
            if (pudientesLimpios.isEmpty() || pudientesLimpios.size() > 2) {
                throw new BusinessException("Al ser menor de edad (" + edad + " años), es obligatorio registrar entre 1 y 2 pudientes (representantes).");
            }
        }

        Persona persona = Persona.builder()
                .nombre(dto.getNombre().trim())
                .apellido(dto.getApellido().trim())
                .cedula(dto.getCedula().trim())
                .telefono(dto.getTelefono().trim())
                .correo(dto.getCorreo().trim())
                .fechaNacimiento(dto.getFechaNacimiento())
                .esEstudiante(Boolean.TRUE.equals(dto.getEsEstudiante()))
                .pudientes(pudientesLimpios)
                .rol(dto.getRol())
                .activo(true)
                .build();

        Persona personaGuardada = personaRepository.save(persona);

        Usuario usuario = Usuario.builder()
                .username(dto.getUsername().trim())
                .password(passwordEncoder.encode(dto.getPassword()))
                .persona(personaGuardada)
                .build();

        return usuarioRepository.save(usuario);
    }

    public LoginResponseDTO login(String username, String password) {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("Usuario o contraseña incorrectos."));

        if (!passwordEncoder.matches(password, usuario.getPassword())) {
            throw new BusinessException("Usuario o contraseña incorrectos.");
        }

        Persona persona = usuario.getPersona();
        if (!Boolean.TRUE.equals(persona.getActivo())) {
            throw new BusinessException("Esta cuenta se encuentra inactiva.");
        }

        String token = "PSICO_" + persona.getId() + "_" + UUID.randomUUID();
        sesionesActivas.put(token, persona.getId());

        return LoginResponseDTO.builder()
                .token(token)
                .id(persona.getId())
                .username(usuario.getUsername())
                .rol(persona.getRol().name())
                .nombre(persona.getNombre())
                .apellido(persona.getApellido())
                .build();
    }

    public Long obtenerPersonaIdDesdeToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
        String token = authHeader.substring(7).trim();
        if (sesionesActivas.containsKey(token)) return sesionesActivas.get(token);
        if (token.startsWith("PSICO_")) {
            String[] partes = token.split("_");
            if (partes.length >= 2) {
                try { return Long.parseLong(partes[1]); } catch (NumberFormatException ignored) {}
            }
        }
        return null;
    }

    public void generarYEnviarCodigo(String cedula) {
        Persona persona = personaRepository.findByCedula(cedula)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró ningún usuario con esa cédula."));
        String codigo = String.format("%04d", new Random().nextInt(10000));
        codigosRecuperacion.put(cedula, codigo);
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
        if (!usuario.getUsername().equals(nuevoUsername) && usuarioRepository.existsByUsername(nuevoUsername)) {
            throw new BusinessException("El nuevo nombre de usuario ya está en uso.");
        }
        usuario.setUsername(nuevoUsername);
        usuario.setPassword(passwordEncoder.encode(nuevaClave));
        usuarioRepository.save(usuario);
        codigosRecuperacion.remove(cedula);
    }
}