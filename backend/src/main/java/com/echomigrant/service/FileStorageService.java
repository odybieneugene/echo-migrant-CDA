package com.echomigrant.service;

import com.echomigrant.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");
    private static final long MAX_SIZE_BYTES = 4 * 1024 * 1024; // 4 Mo, comme la validation Laravel d'origine

    private final Path uploadsDir;

    public FileStorageService(@Value("${app.uploads.dir}") String uploadsDir) {
        this.uploadsDir = Path.of(uploadsDir, "images");
        try {
            Files.createDirectories(this.uploadsDir);
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de créer le dossier d'upload : " + this.uploadsDir, e);
        }
    }

    /** Enregistre l'image de couverture et retourne son chemin relatif (à exposer via /uploads/**). */
    public String storeImage(MultipartFile file) {
        String original = StringUtils.cleanPath(file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
        String extension = getExtension(original);

        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new BadRequestException("Format d'image non supporté. Formats acceptés : " + ALLOWED_EXTENSIONS);
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new BadRequestException("L'image dépasse la taille maximale autorisée (4 Mo)");
        }

        String filename = UUID.randomUUID() + "." + extension;
        try {
            Files.copy(file.getInputStream(), uploadsDir.resolve(filename));
        } catch (IOException e) {
            throw new IllegalStateException("Échec de l'enregistrement de l'image", e);
        }
        return "images/" + filename;
    }

    public void deleteImage(String relativePath) {
        // Rien à supprimer pour une image externe ; et "https://..." n'est pas un chemin valide sous Windows
        if (relativePath == null || relativePath.isBlank()
            || relativePath.startsWith("http://") || relativePath.startsWith("https://")) {
            return;
        }
        try {
            Files.deleteIfExists(Path.of(uploadsDir.getParent().toString(), relativePath));
        } catch (IOException ignored) {
            // suppression best-effort
        }
    }

    private String getExtension(String filename) {
        List<String> parts = List.of(filename.split("\\."));
        return parts.isEmpty() ? "" : parts.get(parts.size() - 1);
    }
}
