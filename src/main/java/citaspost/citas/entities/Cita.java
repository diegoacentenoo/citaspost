package citaspost.citas.entities;

import citaspost.citas.enums.EstadoCita;
import citaspost.citas.enums.EstadoPago;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "citas")
public class Cita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "psicologo_id", nullable = false)
    private Persona psicologo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Persona paciente;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tarifa_id", nullable = false)
    private Tarifa tarifa;

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

    @Column(name = "notas_clinicas", columnDefinition = "TEXT")
    private String notasClinicas;

    // Pudientes del menor o acompañante voluntario del mayor de edad (o anexado por Psicólogo/Admin/Recepción)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "cita_pudientes", joinColumns = @JoinColumn(name = "cita_id"))
    @Builder.Default
    private List<DatosPudiente> pudientes = new ArrayList<>();
}