package ru.teamidea.hybris.boilerplategen.core.data;

import java.util.Set;

/**
 * Created by Timofey Klyubin on 15.03.18
 */
public final class ModelFileData {

    private String modelPackage;
    private String className;
    private Set<ModelFieldData> fields;

    public String getModelPackage() {
        return modelPackage;
    }

    public void setModelPackage(String modelPackage) {
        this.modelPackage = modelPackage;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public Set<ModelFieldData> getFields() {
        return fields;
    }

    public void setFields(Set<ModelFieldData> fields) {
        this.fields = fields;
    }
}
