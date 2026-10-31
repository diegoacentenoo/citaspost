package citaspost.citas.entities;

import citaspost.citas.enums.Rol;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "personas")
public class Persona {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String nombre;

    @Column(nullable = false, length = 50)
    private String apellido;

    @Column(nullable = false, unique = true, length = 20)
    private String cedula;

    @Column(nullable = false, unique = true, length = 20)
    private String telefono;

    @Column(nullable = false, unique = true, length = 100)
    private String correo;

    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimiento;

    @Column(name = "es_estudiante", nullable = false)
    @Builder.Default
    private Boolean esEstudiante = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(30)")
    private Rol rol;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true; // Si es PSICOLOGO y pasa a false = Renunció / De baja

    // Psicólogo fijo asignado tras la primera cita
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "psicologo_asignado_id")
    @ToString.Exclude
    private Persona psicologoAsignado;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "persona_pudientes", joinColumns = @JoinColumn(name = "persona_id"))
    @Builder.Default
    private List<DatosPudiente> pudientes = new ArrayList<>();

    @OneToOne(mappedBy = "persona", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    private Usuario usuario;

    @Transient
    public int getEdad() {
        if (this.fechaNacimiento == null) return 18;
        return Period.between(this.fechaNacimiento, LocalDate.now()).getYears();
    }

    @Transient
    public boolean isMenorDeEdad() {
        return getEdad() < 18;
    }
}