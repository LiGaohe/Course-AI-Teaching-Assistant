package org.example.ta.index;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.intellij.openapi.application.PathManager;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Manager class for handling index document paths persistence using FastJSON.
 * This class manages reading and writing document paths to a JSON file.
 */
public class IndexFileManager {
    private static final String INDEX_FILE_NAME = "indexed-documents.json";
    private final File indexFile;

    public IndexFileManager() {
        // Create the index file in the plugin's config directory
        String configPath = PathManager.getConfigPath();
        this.indexFile = new File(configPath, INDEX_FILE_NAME);
    }

    /**
     * Get the full path to the index file.
     *
     * @return The absolute path to the index file
     */
    public String getIndexFilePath() {
        return indexFile.getAbsolutePath();
    }

    /**
     * Load document paths from the JSON file.
     *
     * @return List of document paths
     */
    public List<String> loadDocumentPaths() {
        List<String> paths = new ArrayList<>();
        
        if (!indexFile.exists()) {
            // Create the file with an empty array if it doesn't exist
            saveDocumentPaths(paths);
            return paths;
        }

        try {
            String content = new String(Files.readAllBytes(Paths.get(indexFile.toURI())));
            JSONArray jsonArray = JSON.parseArray(content);
            if (jsonArray != null) {
                for (int i = 0; i < jsonArray.size(); i++) {
                    paths.add(jsonArray.getString(i));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return paths;
    }

    /**
     * Save document paths to the JSON file.
     *
     * @param paths List of document paths to save
     */
    public void saveDocumentPaths(List<String> paths) {
        try {
            if (!indexFile.getParentFile().exists()) {
                indexFile.getParentFile().mkdirs();
            }

            JSONArray jsonArray = new JSONArray();
            jsonArray.addAll(paths);

            try (FileWriter writer = new FileWriter(indexFile)) {
                JSON.writeJSONString(writer, jsonArray);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Add a new document path to the stored paths.
     *
     * @param path The new path to add
     */
    public void addDocumentPath(String path) {
        List<String> paths = loadDocumentPaths();
        if (!paths.contains(path)) {
            paths.add(path);
            saveDocumentPaths(paths);
        }
    }
}