package com.servifood.infrastructure.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import com.servifood.application.CheckoutException;
import com.servifood.application.ImageStorage;

@Component
public class LocalImageStorage implements ImageStorage {
    private static final Map<String, String> TYPES = Map.of("image/jpeg", "jpg", "image/png", "png", "image/webp", "webp");
    private final Path baseDirectory;
    private final long maxBytes;

    public LocalImageStorage(@Value("${app.uploads.images-directory}") String directory, @Value("${app.uploads.images-max-bytes}") long maxBytes) {
        this.baseDirectory = Path.of(directory).toAbsolutePath().normalize(); this.maxBytes = maxBytes;
    }

    @Override
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) throw invalid("La imagen es obligatoria.");
        if (file.getSize() > maxBytes) throw invalid("La imagen supera el tamaño máximo permitido.");
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        String extension = TYPES.get(contentType);
        if (extension == null || !hasMatchingExtension(file.getOriginalFilename(), extension)) throw invalid("Formato de imagen no permitido.");
        try {
            byte[] signature;
            try (InputStream input = file.getInputStream()) { signature = input.readNBytes(12); }
            if (!matchesSignature(signature, extension)) throw invalid("El contenido de la imagen no coincide con su formato.");
            Files.createDirectories(baseDirectory);
            String internalName = UUID.randomUUID() + "." + extension;
            Path target = safePath(internalName);
            try (InputStream input = file.getInputStream()) { Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING); }
            return internalName;
        } catch (IOException exception) { throw new CheckoutException(HttpStatus.INTERNAL_SERVER_ERROR, "IMAGE_STORAGE_ERROR", "No pudimos guardar la imagen."); }
    }

    @Override
    public StoredImage read(String internalName) {
        Path target = safePath(internalName);
        if (!Files.isRegularFile(target)) throw new CheckoutException(HttpStatus.NOT_FOUND, "IMAGE_NOT_FOUND", "Imagen no encontrada.");
        try { return new StoredImage(Files.readAllBytes(target), contentType(internalName)); }
        catch (IOException exception) { throw new CheckoutException(HttpStatus.INTERNAL_SERVER_ERROR, "IMAGE_STORAGE_ERROR", "No pudimos leer la imagen."); }
    }

    private Path safePath(String name) {
        if (name == null || !name.matches("[0-9a-f-]{36}\\.(jpg|png|webp)")) throw new CheckoutException(HttpStatus.NOT_FOUND, "IMAGE_NOT_FOUND", "Imagen no encontrada.");
        Path target = baseDirectory.resolve(name).normalize();
        if (!target.startsWith(baseDirectory)) throw new CheckoutException(HttpStatus.NOT_FOUND, "IMAGE_NOT_FOUND", "Imagen no encontrada.");
        return target;
    }
    private boolean hasMatchingExtension(String name, String expected) { if (name == null) return false; String lower = name.toLowerCase(Locale.ROOT); return lower.endsWith("." + expected) || (expected.equals("jpg") && lower.endsWith(".jpeg")); }
    private boolean matchesSignature(byte[] bytes, String extension) { String hex = HexFormat.of().formatHex(bytes); return switch (extension) { case "jpg" -> hex.startsWith("ffd8ff"); case "png" -> hex.startsWith("89504e470d0a1a0a"); case "webp" -> bytes.length >= 12 && new String(bytes, 0, 4, StandardCharsets.US_ASCII).equals("RIFF") && new String(bytes, 8, 4, StandardCharsets.US_ASCII).equals("WEBP"); default -> false; }; }
    private String contentType(String path) { if (path.endsWith(".png")) return "image/png"; if (path.endsWith(".webp")) return "image/webp"; return "image/jpeg"; }
    private CheckoutException invalid(String message) { return new CheckoutException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "INVALID_IMAGE", message); }
}
