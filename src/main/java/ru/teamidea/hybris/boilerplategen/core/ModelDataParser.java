package ru.teamidea.hybris.boilerplategen.core;

import org.apache.commons.io.IOUtils;
import ru.teamidea.hybris.boilerplategen.core.data.ModelFieldData;
import ru.teamidea.hybris.boilerplategen.core.data.ModelFileData;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Timofey Klyubin on 15.03.18
 */
public final class ModelDataParser {

    private static final Pattern PACKAGE_PT = Pattern.compile("package\\s+([\\w]+[\\w.]*[\\w]+)\\s*;");
    private static final Pattern CLASS_NAME_PT = Pattern.compile("public\\s+class\\s+([\\w\\d_]+)\\s+extends");
    private static final Pattern FIELD_GETTER_PT = Pattern.compile("@Accessor\\s*\\(\\s*qualifier\\s*=\\s*\"([\\w\\d_]+)\"[^)]+\\)[^p]+public\\s+([\\w\\d_]+)\\s+get[\\w\\d_]+\\(", Pattern.DOTALL | Pattern.MULTILINE);
    private static final String TYPE_IMPORT_PT_RAW = "import\\s+([\\w]+[\\w.]*)\\.";

    public ModelFileData parseModelFile(File modelFile) {
        try (FileInputStream fis = new FileInputStream(modelFile)) {
            final String javaSrc = IOUtils.toString(fis, StandardCharsets.UTF_8);

            ModelFileData data = new ModelFileData();
            Matcher packageMatcher = PACKAGE_PT.matcher(javaSrc);
            if (packageMatcher.find()) {
                data.setModelPackage(packageMatcher.group(1));
            }

            Matcher className = CLASS_NAME_PT.matcher(javaSrc);
            if (className.find()) {
                data.setClassName(className.group(1));
            }

            Matcher fieldGetter = FIELD_GETTER_PT.matcher(javaSrc);
            Set<ModelFieldData> fieldDataList = new HashSet<>();
            while (fieldGetter.find()) {
                ModelFieldData fieldData = new ModelFieldData();
                String type = fieldGetter.group(2);
                fieldData.setFieldName(fieldGetter.group(1));
                fieldData.setFieldType(type);
                Pattern pkgPattern = Pattern.compile(TYPE_IMPORT_PT_RAW.concat(type).concat("\\s*;"));
                Matcher pkg = pkgPattern.matcher(javaSrc);
                if (pkg.find()) {
                    fieldData.setFieldTypePackage(pkg.group(1));
                } else {
                    fieldData.setFieldTypePackage("java.lang");
                }

                fieldDataList.add(fieldData);
            }
            data.setFields(fieldDataList);

            return data;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
