package ru.teamidea.hybris.boilerplategen.core.data;

/**
 * Created by Timofey Klyubin on 15.03.18
 */
public final class ModelFieldData {
    private String fieldName;
    private String fieldType;
    private String fieldTypePackage;

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldType() {
        return fieldType;
    }

    public void setFieldType(String fieldType) {
        this.fieldType = fieldType;
    }

    public String getFieldTypePackage() {
        return fieldTypePackage;
    }

    public void setFieldTypePackage(String fieldTypePackage) {
        this.fieldTypePackage = fieldTypePackage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ModelFieldData fieldData = (ModelFieldData) o;

        if (fieldName != null ? !fieldName.equals(fieldData.fieldName) : fieldData.fieldName != null) return false;
        if (fieldType != null ? !fieldType.equals(fieldData.fieldType) : fieldData.fieldType != null) return false;
        return fieldTypePackage != null ? fieldTypePackage.equals(fieldData.fieldTypePackage) : fieldData.fieldTypePackage == null;

    }

    @Override
    public int hashCode() {
        int result = fieldName != null ? fieldName.hashCode() : 0;
        result = 31 * result + (fieldType != null ? fieldType.hashCode() : 0);
        result = 31 * result + (fieldTypePackage != null ? fieldTypePackage.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ModelFieldData{" +
                       "fieldName='" + fieldName + '\'' +
                       ", fieldType='" + fieldType + '\'' +
                       ", fieldTypePackage='" + fieldTypePackage + '\'' +
                       '}';
    }
}
