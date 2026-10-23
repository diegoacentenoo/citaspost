package citaspost.citas.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "citas_comprobantes_fotos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CitaComprobanteFoto {

    @Id
    private Long idCita;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "id_cita")
    private Cita cita;

    @Lob
    @Column(nullable = false, columnDefinition = "MEDIUMBLOB")
    private byte[] fotoBytes;

    @Column(nullable = false, length = 30)
    private String mimeType;
}