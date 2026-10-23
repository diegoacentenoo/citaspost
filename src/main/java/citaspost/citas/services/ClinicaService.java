package citaspost.citas.services;

import citaspost.citas.dtos.request.HorarioRequestDTO;
import citaspost.citas.dtos.response.HorarioResponseDTO;
import citaspost.citas.dtos.response.PersonaResponseDTO;
import citaspost.citas.dtos.response.TarifaDTO;
import citaspost.citas.entities.HorarioPsicologo;
import citaspost.citas.entities.Persona;
import citaspost.citas.entities.Tarifa;
import citaspost.citas.enums.Rol;
import citaspost.citas.enums.TipoTarifa;
import citaspost.citas.exception.BusinessException;
import citaspost.citas.exception.ResourceNotFoundException;
import citaspost.citas.repositories.HorarioPsicologoRepository;
import citaspost.citas.repositories.PersonaRepository;
import citaspost.citas.repositories.TarifaRepository;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClinicaService {

    private final PersonaRepository personaRepository;
    private final HorarioPsicologoRepository horarioRepository;
    private final TarifaRepository tarifaRepository;

    @PostConstruct
    public void inicializarTarifasPorDefecto() {
        if (tarifaRepository.count() == 0) {
            tarifaRepository.save(Tarifa.builder()
                    .tipoTarifa(TipoTarifa.GENERAL)
                    .monto(new BigDecimal("30.00"))
                    .activa(true)
                    .build());
            tarifaRepository.save(Tarifa.builder()
                    .tipoTarifa(TipoTarifa.ESPECIAL)
                    .monto(new BigDecimal("20.00"))
                    .activa(true)
                    .build());
        }
    }

    public List<PersonaResponseDTO> listarPorRol(Rol rol) {
        return personaRepository.findByRolAndActivoTrue(rol).stream()
                .map(this::mapearPersonaDTO)
                .toList();
    }

    public PersonaResponseDTO obtenerPersonaPorId(Long id) {
        Persona p = personaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Persona no encontrada."));
        return mapearPersonaDTO(p);
    }

    private PersonaResponseDTO mapearPersonaDTO(Persona p) {
        return PersonaResponseDTO.builder()
                .id(p.getId())
                .nombre(p.getNombre())
                .apellido(p.getApellido())
                .cedula(p.getCedula())
                .telefono(p.getTelefono())
                .correo(p.getCorreo())
                .fechaNacimiento(p.getFechaNacimiento())
                .edad(p.getEdad())
                .menorDeEdad(p.isMenorDeEdad())
                .esEstudiante(p.getEsEstudiante())
                .pudientes(p.getPudientes())
                .rol(p.getRol())
                .activo(p.getActivo())
                .build();
    }

    public List<TarifaDTO> listarTarifasActivas() {
        return tarifaRepository.findAll().stream()
                .filter(t -> Boolean.TRUE.equals(t.getActiva()))
                .map(t -> TarifaDTO.builder()
                        .id(t.getId())
                        .tipoTarifa(t.getTipoTarifa())
                        .monto(t.getMonto())
                        .activa(t.getActiva())
                        .build())
                .toList();
    }

    @Transactional
    public HorarioResponseDTO registrarHorario(HorarioRequestDTO dto) {
        if (!dto.getHoraFin().isAfter(dto.getHoraInicio())) {
            throw new BusinessException("La hora de fin debe ser mayor a la hora de inicio.");
        }

        Persona psicologo = personaRepository.findById(dto.getPsicologoId())
                .orElseThrow(() -> new ResourceNotFoundException("Psicólogo no encontrado."));

        if (psicologo.getRol() != Rol.PSICOLOGO) {
            throw new BusinessException("Solo se pueden asignar horarios a usuarios con rol PSICOLOGO.");
        }

        // Validar que no se solape con otro horario del mismo día
        List<HorarioPsicologo> existentes = horarioRepository
                .findByPsicologoIdAndDiaSemanaAndActivoTrue(psicologo.getId(), dto.getDiaSemana());

        boolean solapado = existentes.stream().anyMatch(h ->
                h.getHoraInicio().isBefore(dto.getHoraFin()) && h.getHoraFin().isAfter(dto.getHoraInicio())
        );

        if (solapado) {
            throw new BusinessException("El psicólogo ya tiene un bloque de horario que se cruza en el día " + dto.getDiaSemana());
        }

        HorarioPsicologo horario = HorarioPsicologo.builder()
                .psicologo(psicologo)
                .diaSemana(dto.getDiaSemana())
                .horaInicio(dto.getHoraInicio())
                .horaFin(dto.getHoraFin())
                .activo(true)
                .build();

        return mapearHorario(horarioRepository.save(horario));
    }

    public List<HorarioResponseDTO> listarHorariosPsicologo(Long psicologoId) {
        return horarioRepository.findByPsicologoIdAndActivoTrueOrderByDiaSemanaAscHoraInicioAsc(psicologoId)
                .stream()
                .map(this::mapearHorario)
                .toList();
    }

    @Transactional
    public void eliminarHorario(Long horarioId) {
        HorarioPsicologo horario = horarioRepository.findById(horarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Horario no encontrado."));
        horarioRepository.delete(horario);
    }

    private HorarioResponseDTO mapearHorario(HorarioPsicologo h) {
        return HorarioResponseDTO.builder()
                .id(h.getId())
                .psicologoId(h.getPsicologo().getId())
                .nombrePsicologo(h.getPsicologo().getNombre() + " " + h.getPsicologo().getApellido())
                .diaSemana(h.getDiaSemana())
                .horaInicio(h.getHoraInicio())
                .horaFin(h.getHoraFin())
                .activo(h.getActivo())
                .build();
    }

    public List<TarifaDTO> listarTodasLasTarifas() {
        return tarifaRepository.findAll().stream()
                .map(t -> TarifaDTO.builder()
                        .id(t.getId())
                        .tipoTarifa(t.getTipoTarifa())
                        .monto(t.getMonto())
                        .activa(t.getActiva())
                        .build())
                .toList();
    }

    @Transactional
    public TarifaDTO guardarOActualizarTarifa(TarifaDTO dto) {
        if (dto.getMonto() == null || dto.getMonto().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("El monto de la tarifa debe ser mayor o igual a 0.");
        }

        Tarifa tarifa = tarifaRepository.findByTipoTarifa(dto.getTipoTarifa())
                .orElse(Tarifa.builder().tipoTarifa(dto.getTipoTarifa()).build());

        tarifa.setMonto(dto.getMonto());
        tarifa.setActiva(dto.getActiva() != null ? dto.getActiva() : true);

        Tarifa guardada = tarifaRepository.save(tarifa);
        return TarifaDTO.builder()
                .id(guardada.getId())
                .tipoTarifa(guardada.getTipoTarifa())
                .monto(guardada.getMonto())
                .activa(guardada.getActiva())
                .build();
    }
}