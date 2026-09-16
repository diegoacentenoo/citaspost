package citaspost.citas.entities;


import citaspost.citas.enums.EstadoCita;
import citaspost.citas.enums.EstadoPago;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "citas")
public class Citas {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // El psicólogo que atiende
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "psicologo_id", nullable = false)
    private Persona psicologo;

    // El paciente que asiste
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Persona paciente;

    @Column(nullable = false)
    private LocalDate fecha; // Ej: 2026-10-15

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    // La tarifa aplicada a esta cita
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tarifa_id", nullable = false)
    private Tarifas tarifa;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_pago", nullable = false)
    private EstadoPago estadoPago;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_cita", nullable = false)
    @Builder.Default
    private EstadoCita estadoCita = EstadoCita.PENDIENTE;

    @Column(nullable = false)
    @Builder.Default
    private Boolean asistio = false;
}