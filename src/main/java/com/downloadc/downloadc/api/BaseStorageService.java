package com.downloadc.downloadc.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

// Base class for json storage of downloads folder

public abstract class BaseStorageService {

    // Main folder where everything is stored
    public static final String STORAGE_ROOT = "downloads";


    protected final ObjectMapper objectMapper;

    // Constructor

    protected BaseStorageService(){
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT); 
    }

    // Makes sure folder exists before writing file
    public void ensureDirectory(Path filePath) throws IOException{
        Files.createDirectories(filePath.getParent());
    }

    // Builds path downloads/filename
    public Path storagePath(String filename){
        return Paths.get(STORAGE_ROOT , filename);
    }

    public abstract Path getStorageFile();

    // Abstract method
    public abstract void clearAll() throws IOException;
}
