package com.servifood.infrastructure.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import com.servifood.application.CheckoutException;
import com.servifood.application.ReceiptStorage;

@Component
public class LocalReceiptStorage implements ReceiptStorage {
    private static final Map<String, String> TYPES = Map.of(
            "image/jpeg", "jpg", "image/png", "png", "image/webp", "webp");
    private final Path baseDirectory;
    private final long maxBytes;

    public LocalReceiptStorage(@Value("${app.uploads.receipts-directory}") String directory,
            @Value("${app.uploads.receipts-max-bytes}") long maxBytes) {
        this.baseDirectory = Path.of(directory).toAbsolutePath().normalize(); this.maxBytes = maxBytes;
    }

    @Override
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) throw invalid("El comprobante es obligatorio para transferencias.");
        if (file.getSize() > maxBytes) throw invalid("El comprobante supera el tamaño máximo permitido.");
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        String extension = TYPES.get(contentType);
        if (extension == null || !hasMatchingExtension(file.getOriginalFilename(), extension)) throw invalid("Formato de comprobante no permitido.");
        try {
            byte[] signature;
            try (InputStream input = file.getInputStream()) { signature = input.readNBytes(12); }
            if (!matchesSignature(signature, extension)) throw invalid("El contenido del comprobante no coincide con su formato.");
            Files.createDirectories(baseDirectory);
            String internalName = UUID.randomUUID() + "." + extension;
            Path target = baseDirectory.resolve(internalName).normalize();
            if (!target.startsWith(baseDirectory)) throw invalid("Ruta de comprobante inválida.");
            try (InputStream input = file.getInputStream()) { Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING); }
            return internalName;
        } catch (IOException exception) {
            throw new CheckoutException(HttpStatus.INTERNAL_SERVER_ERROR, "RECEIPT_STORAGE_ERROR", "No pudimos guardar el comprobante.");
        }
    }

    @Override public void deleteQuietly(String storedPath) {
        if (storedPath == null) return;
        try { Path target = baseDirectory.resolve(storedPath).normalize(); if (target.startsWith(baseDirectory)) Files.deleteIfExists(target); }
        catch (IOException ignored) { }
    }

    @Override
    public StoredFile read(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) throw new CheckoutException(HttpStatus.NOT_FOUND, "RECEIPT_NOT_FOUND", "Comprobante no encontrado.");
        Path target = baseDirectory.resolve(storedPath).normalize();
        if (!target.startsWith(baseDirectory) || !Files.isRegularFile(target)) throw new CheckoutException(HttpStatus.NOT_FOUND, "RECEIPT_NOT_FOUND", "Comprobante no encontrado.");
        try { return new StoredFile(Files.readAllBytes(target), contentType(storedPath)); }
        catch (IOException exception) { throw new CheckoutException(HttpStatus.INTERNAL_SERVER_ERROR, "RECEIPT_STORAGE_ERROR", "No pudimos leer el comprobante."); }
    }

    private String contentType(String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".webp")) return "image/webp";
        return "image/jpeg";
    }

    private boolean hasMatchingExtension(String name, String expected) {
        if (name == null) return false; String lower = name.toLowerCase(Locale.ROOT);
        return lower.endsWith("." + expected) || (expected.equals("jpg") && lower.endsWith(".jpeg"));
    }
    private boolean matchesSignature(byte[] bytes, String extension) {
        String hex = HexFormat.of().formatHex(bytes);
        return switch (extension) {
            case "jpg" -> hex.startsWith("ffd8ff");
            case "png" -> hex.startsWith("89504e470d0a1a0a");
            case "webp" -> bytes.length >= 12 && new String(bytes, 0, 4, StandardCharsets.US_ASCII).equals("RIFF") && new String(bytes, 8, 4, StandardCharsets.US_ASCII).equals("WEBP");
            default -> false;
        };
    }
    private CheckoutException invalid(String message) { return new CheckoutException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "INVALID_RECEIPT", message); }
}
