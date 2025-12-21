package mg.etu3273.framework.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;


public class FileUpload {
    private String name;   
     private String fieldName;          
    private byte[] content;        
    private String contentType;    
    private long size;             
    
    public FileUpload() {
    }
    
    public FileUpload(String name, byte[] content) {
        this.name = name;
        this.content = content;
        this.size = content != null ? content.length : 0;
    }

    public FileUpload(String name, String fieldName, byte[] content, String contentType) {
        this.name = name;
        this.fieldName = fieldName;
        this.content = content;
        this.contentType = contentType;
        this.size = content != null ? content.length : 0;
    }
    
    public FileUpload(String name, byte[] content, String contentType) {
        this.name = name;
        this.content = content;
        this.contentType = contentType;
        this.size = content != null ? content.length : 0;
    }
    

    public String saveTo(String directoryPath) throws IOException {
        if (content == null || content.length == 0) {
            throw new IOException("Aucun contenu à sauvegarder");
        }
        
        File directory = new File(directoryPath);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        
        File file = new File(directory, name);
        
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(content);
        }
        
        System.out.println("Fichier sauvegardé: " + file.getAbsolutePath());
        
        return file.getAbsolutePath();
    }
    

    public String saveTo(String directoryPath, String newFileName) throws IOException {
        String originalName = this.name;
        this.name = newFileName;
        String path = saveTo(directoryPath);
        this.name = originalName;
        return path;
    }
    
    public String save(String directory) throws IOException {
        return saveTo(directory);
    }

    public String save(String directory, String customName) throws IOException {
        return saveTo(directory, customName);
    }

    public String getExtension() {
        if (name == null || !name.contains(".")) {
            return "";
        }
        return name.substring(name.lastIndexOf("."));
    }
    
    public boolean isEmpty() {
        return content == null || content.length == 0;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public byte[] getContent() {
        return content;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public void setContent(byte[] content) {
        this.content = content;
        this.size = content != null ? content.length : 0;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getFormattedSize() {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        } else {
            return String.format("%.2f MB", size / (1024.0 * 1024.0));
        }
    }

    public boolean isImage() {
        if (contentType == null) return false;
        return contentType.startsWith("image/");
    }

    public boolean isPDF() {
        return "application/pdf".equals(contentType);
    }

    @Override
    public String toString() {
        return "FileUpload{" +
                "name='" + name + '\'' +
                ", size=" + getFormattedSize() +
                ", contentType='" + contentType + '\'' +
                '}';
    }
}