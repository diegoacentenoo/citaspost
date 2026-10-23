package citaspost.citas.services;

import citaspost.citas.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;

@Service
public class ValidadorComprobanteService {

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public void validarImagenDePago(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Es obligatorio adjuntar la foto de la factura o comprobante de pago.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException("El archivo debe ser una imagen válida (JPG, PNG, WEBP).");
        }

        try {
            byte[] bytes = file.getBytes();

            // 1. Verificar que los bytes correspondan a una imagen real con dimensiones legibles
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null || image.getWidth() < 150 || image.getHeight() < 150) {
                throw new BusinessException("La imagen subida es inválida o demasiado pequeña para ser un comprobante legible.");
            }

            // 2. Si hay API Key de Gemini configurada, verificamos por IA que sea una factura/comprobante y no un perro u otra foto
            if (geminiApiKey != null && !geminiApiKey.isBlank() && !geminiApiKey.equals("TU_API_KEY_DE_GEMINI_AQUI")) {
                verificarConGeminiVision(bytes, contentType);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("No se pudo procesar la imagen del comprobante: " + e.getMessage());
        }
    }

    private void verificarConGeminiVision(byte[] imageBytes, String mimeType) {
        try {
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            String prompt = "Analiza esta imagen. ¿Es una factura, recibo, capture de transferencia bancaria, pago móvil o comprobante de pago válido? " +
                    "Si es un comprobante o factura responde exactamente SI. Si es otra cosa (como un perro, mascota, persona, paisaje, meme u objeto) responde exactamente NO seguido de qué es.";

            String jsonBody = """
                {
                  "contents": [{
                    "parts": [
                      {"text": "%s"},
                      {"inline_data": {"mime_type": "%s", "data": "%s"}}
                    ]
                  }]
                }
                """.formatted(prompt, mimeType, base64Image);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + geminiApiKey.trim()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 && response.body().contains("\"text\"")) {
                String bodyUpper = response.body().toUpperCase();
                if (bodyUpper.contains("\"TEXT\": \"NO") || bodyUpper.contains("\"TEXT\":\"NO")) {
                    throw new BusinessException("La imagen rechazada no es una factura ni comprobante de pago válido. Por favor suba únicamente la foto del pago.");
                }
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception ignored) {
            // Si falla la red externa no bloqueamos el hilo local, habiendo pasado ya la validación de BufferedImage
        }
    }
}