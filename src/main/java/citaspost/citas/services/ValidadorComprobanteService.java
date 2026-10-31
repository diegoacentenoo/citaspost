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
            .connectTimeout(Duration.ofSeconds(12))
            .build();

    public void validarImagenYNumeroComprobante(MultipartFile file, String numeroComprobante) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Es obligatorio adjuntar la foto de la factura o comprobante de pago.");
        }
        if (numeroComprobante == null || numeroComprobante.trim().length() < 4) {
            throw new BusinessException("Debe ingresar un número de comprobante o referencia válido (mínimo 4 caracteres).");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException("El archivo debe ser una imagen válida (JPG, PNG, WEBP).");
        }

        try {
            byte[] bytes = file.getBytes();

            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null || image.getWidth() < 150 || image.getHeight() < 150) {
                throw new BusinessException("La imagen subida es inválida o demasiado pequeña para ser un comprobante legible.");
            }

            if (geminiApiKey != null && !geminiApiKey.isBlank() && !geminiApiKey.equals("TU_API_KEY_DE_GEMINI_AQUI")) {
                verificarConGeminiVision(bytes, contentType, numeroComprobante.trim());
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("No se pudo procesar la imagen del comprobante: " + e.getMessage());
        }
    }

    private void verificarConGeminiVision(byte[] imageBytes, String mimeType, String numeroReferencia) {
        try {
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            String prompt = "Analiza esta imagen rigurosamente y verifica dos cosas: " +
                    "1) ¿Es una factura, recibo, capture de transferencia bancaria o comprobante de pago móvil real? " +
                    "2) ¿Aparece visible en la imagen el número de comprobante/referencia '" + numeroReferencia + "'? " +
                    "Si NO es un comprobante de pago (ej: perro, persona, paisaje, objeto), responde exactamente: NO_ES_PAGO. " +
                    "Si ES un comprobante de pago pero el número '" + numeroReferencia + "' NO coincide con el de la imagen, responde exactamente: NUMERO_NO_COINCIDE. " +
                    "Si es un comprobante válido y el número coincide, responde exactamente: VALIDO.";

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
                if (bodyUpper.contains("NO_ES_PAGO")) {
                    throw new BusinessException("La imagen subida NO es una factura ni comprobante de pago válido.");
                }
                if (bodyUpper.contains("NUMERO_NO_COINCIDE")) {
                    throw new BusinessException("El número de comprobante ingresado (" + numeroReferencia + ") no concuerda con el número visible en la foto del recibo.");
                }
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception ignored) {
        }
    }
}