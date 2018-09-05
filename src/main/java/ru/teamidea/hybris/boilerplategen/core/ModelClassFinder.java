package ru.teamidea.hybris.boilerplategen.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Timofey Klyubin on 15.03.18
 */
public final class ModelClassFinder {

    public List<File> findModelJavaFile(File platformHome, String typeCode) throws IOException {
        Path platformPath = platformHome.toPath();
        Path generatedModelsPath = platformPath.resolve("bootstrap/gensrc");
        List<Path> foundPaths = new LinkedList<>();
        Files.walk(generatedModelsPath).forEach(p -> {
            String name = p.getFileName().toString();
            if (name.equals(typeCode.concat("Model.java"))) {
                if (!Files.isDirectory(p)) {
                    foundPaths.add(p);
                }
            }
        });
        return foundPaths.stream().map(Path::toFile).collect(Collectors.toList());
    }
}
