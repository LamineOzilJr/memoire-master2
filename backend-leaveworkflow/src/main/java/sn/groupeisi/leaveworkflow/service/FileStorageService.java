package sn.groupeisi.leaveworkflow.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    @Value("${file.max-size:104857600}")
    private long maxSize;

    @Value("${file.allowed-extensions:pdf,jpg,jpeg,png,doc,docx}")
    private String allowedExtensions;

    public String storeFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("Fichier vide");
        }

        if (file.getSize() > maxSize) {
            throw new RuntimeException("Fichier trop grand. Taille max: " + maxSize + " bytes");
        }

        String extension = getFileExtension(file.getOriginalFilename());
        if (extension == null || extension.isEmpty()) {
            throw new RuntimeException("Le fichier doit avoir une extension");
        }

        List<String> allowed = Arrays.asList(allowedExtensions.split(","));
        if (!allowed.contains(extension)) {
            throw new RuntimeException("Format non autorisé. Formats acceptés: " + allowedExtensions);
        }

        String fileName = UUID.randomUUID() + "." + extension;

        // Create upload directory with robust error handling
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            System.out.println("Creating upload directory: " + dir.getAbsolutePath());
            boolean created = dir.mkdirs();
            if (!created) {
                throw new RuntimeException("Impossible de créer le répertoire d'upload: " + dir.getAbsolutePath());
            }
        }

        // Verify directory is writable
        if (!dir.isDirectory() || !dir.canWrite()) {
            throw new RuntimeException("Le répertoire d'upload n'est pas accessible en écriture: " + dir.getAbsolutePath());
        }

        try {
            File destFile = new File(uploadDir, fileName);
            System.out.println("Storing file: " + destFile.getAbsolutePath());
            file.transferTo(destFile);
            System.out.println("File stored successfully: " + destFile.getAbsolutePath());
            return fileName;
        } catch (IOException e) {
            System.err.println("IOException lors de l'enregistrement du fichier: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Erreur d'enregistrement du fichier: " + e.getMessage(), e);
        } catch (Exception e) {
            System.err.println("Exception lors de l'enregistrement du fichier: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Erreur inattendue lors de l'enregistrement du fichier: " + e.getMessage(), e);
        }
    }

    private String getFileExtension(String fileName) {
        if (fileName == null) return null;
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1).toLowerCase();
    }

    // New methods to support file download
    public Path getFilePath(String filename) {
        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            System.out.println("Upload dir configured: " + uploadDir);
            System.out.println("Upload path (absolute): " + uploadPath);
            Path filePath = uploadPath.resolve(filename).normalize();
            System.out.println("Resolving file: " + filename);
            System.out.println("Full file path: " + filePath);
            return filePath;
        } catch (Exception e) {
            System.err.println("Error resolving file path: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la résolution du chemin du fichier", e);
        }
    }

    public Resource loadFileAsResource(String filename) {
        try {
            System.out.println("Loading file resource: " + filename);
            Path filePath = getFilePath(filename);

            System.out.println("Checking if file exists at: " + filePath);
            if (!Files.exists(filePath)) {
                System.err.println("File not found at: " + filePath);
                System.err.println("Upload directory: " + uploadDir);
                System.err.println("Directory exists: " + Files.exists(Paths.get(uploadDir)));
                throw new RuntimeException("Fichier non trouvé à: " + filePath.toString());
            }

            System.out.println("File found, creating resource...");
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists()) {
                System.err.println("Resource does not exist: " + filePath);
                throw new RuntimeException("Ressource fichier n'existe pas: " + filename);
            }

            if (!resource.isReadable()) {
                System.err.println("Resource is not readable: " + filePath);
                throw new RuntimeException("Impossible de lire le fichier (permission denied): " + filename);
            }

            System.out.println("File resource loaded successfully");
            return resource;
        } catch (MalformedURLException ex) {
            System.err.println("MalformedURLException: " + ex.getMessage());
            ex.printStackTrace();
            throw new RuntimeException("URL invalide pour le fichier: " + filename, ex);
        } catch (RuntimeException ex) {
            System.err.println("RuntimeException loading file: " + ex.getMessage());
            ex.printStackTrace();
            throw ex;
        } catch (Exception ex) {
            System.err.println("Unexpected error loading file: " + ex.getMessage());
            ex.printStackTrace();
            throw new RuntimeException("Erreur inattendue lors du chargement du fichier: " + ex.getMessage(), ex);
        }
    }
}