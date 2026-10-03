package citaspost.citas.services;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void enviarCodigoRecuperacion(String destinatario, String codigo) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setTo(destinatario);
        mensaje.setSubject("Código de Recuperación - Dentalis");
        mensaje.setText("Hola,\n\nTu código de seguridad para recuperar el acceso a la plataforma Dentalis es: " 
                + codigo + "\n\nSi no solicitaste esto, ignora este mensaje.");
        mailSender.send(mensaje);
    }
}