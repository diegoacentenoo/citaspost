package citaspost.citas.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String username; // Aquí guardaremos el correo o la cédula

    @Column(nullable = false)
    private String password;

    // --- RELACIÓN 1 a 1 CON PERSONA ---
    @OneToOne
    @JoinColumn(name = "persona_id", referencedColumnName = "id", nullable = false)
    private Persona persona;
}
