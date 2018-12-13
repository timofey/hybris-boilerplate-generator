package ru.teamidea.hybris.boilerplategen.core;

import com.squareup.javapoet.JavaFile;
import com.ximpleware.*;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Created by Timofey Klyubin on 29.11.18
 */
public class SpringConfigGenerator {

    private static final Logger LOG = Logger.getLogger(SpringConfigGenerator.class);

    private final VTDGen vtdGen;
    private final Path springConfigPath;

    public static Path getSpringConfigByExtension(Path extensionPath) {
        final String name = extensionPath.getFileName().toString().concat("-spring.xml");
        return extensionPath.resolve(Paths.get("resources", name));
    }

    public SpringConfigGenerator(Path pathToSpringConfig) throws IOException, ParseException {
        this.springConfigPath = pathToSpringConfig;

        final File file = this.springConfigPath.toFile();
        final int length = (int) file.length();
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[length];
            final int read = fis.read(buffer);
            if (read < length) {
                throw new IllegalStateException(String.format("Could only read %d bytes out of %d", read, length));
            }

            this.vtdGen = new VTDGen();
            this.vtdGen.setDoc(buffer);
            this.vtdGen.parse(true);
        }
    }

    public void addBeanForJavaSource(JavaFile implSource, Map<String, String> propertiesRefs,
                        Map<String, String> propertiesValues) throws ModifyException, IOException, NavException, TranscodeException {
        final String className = implSource.packageName.concat(".").concat(implSource.typeSpec.name);
        final String beanName = StringUtils.uncapitalize(implSource.typeSpec.name).replaceFirst("Impl$", "");

        LOG.info(String.format("Writing file: %s", this.springConfigPath));
        this.addBean(className, beanName, propertiesRefs, propertiesValues);
    }

    public void addBean(String className, String beanName, Map<String, String> propertiesRefs,
                        Map<String, String> propertiesValues) throws ModifyException, IOException, NavException, TranscodeException {
        StringBuilder beanBuilder = new StringBuilder();
        beanBuilder.append("<bean id=\"").append(beanName).append("\" ")
                .append(" class=\"").append(className).append("\">\n");
        if (propertiesRefs != null) {
            for (Map.Entry<String, String> propEntry : propertiesRefs.entrySet()) {
                beanBuilder.append("    <property name=\"").append(propEntry.getKey()).append("\" ")
                        .append("ref=\"").append(propEntry.getValue()).append("\"/>\n");
            }
        }

        if (propertiesValues != null) {
            for (Map.Entry<String, String> propEntry : propertiesValues.entrySet()) {
                beanBuilder.append("    <property name=\"").append(propEntry.getKey()).append("\" ")
                        .append("value=\"").append(propEntry.getValue()).append("\"/>\n");
            }
        }

        beanBuilder.append("</bean>\n");

        XMLModifier modifier = new XMLModifier();
        modifier.bind(vtdGen.getNav());
        modifier.insertBeforeTail(beanBuilder.toString());
        modifier.output(springConfigPath.toString());
    }
}
