package ru.teamidea.hybris.boilerplategen.core;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Timofey Klyubin on 15.03.18
 */
public class CoreExtensionFinder {

    private static final Logger LOG = Logger.getLogger(CoreExtensionFinder.class);

    private static final String CORE_NAME = "core";
    private static final int MAX_CANDIDATES = 2;

    private final Path platformPath;

    public CoreExtensionFinder(Path platformPath) {
        this.platformPath = platformPath;
    }

    public List<Path> findBaseCustomCorePath() {

        List<Path> candidates = new LinkedList<>();
        try {
            Files.walk(platformPath.resolve("../custom"), 2, FileVisitOption.FOLLOW_LINKS).forEach(p -> {
                if (Files.isDirectory(p) && isCandidateTo(p.getFileName().toString())) {
                    candidates.add(p);
                }
            });

            List<Path> strongCandidates = candidates.stream().filter(p -> p.toString().endsWith(CORE_NAME))
                    .collect(Collectors.toList());
            if (strongCandidates.size() == 1) {
                return Collections.singletonList(strongCandidates.iterator().next());
            }

            if (strongCandidates.size() > 1) {
                return strongCandidates;
            }

            return candidates.stream()
                    .sorted(Comparator.comparingInt(p -> p.getFileName().toString().length() - p.getFileName().toString().lastIndexOf(CORE_NAME)))
                    .limit(MAX_CANDIDATES)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    public Path getSrcRoot(Path extensionPath) {
        return extensionPath.resolve("src");
    }

    private boolean isCandidateTo(String path) {
        return !path.equals(CORE_NAME) && path.contains(CORE_NAME);
    }
}
