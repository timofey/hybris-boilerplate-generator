package ru.teamidea.hybris.boilerplategen.core;

import com.squareup.javapoet.*;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import ru.teamidea.hybris.boilerplategen.core.data.ModelFieldData;
import ru.teamidea.hybris.boilerplategen.core.data.ModelFileData;

import javax.lang.model.element.Modifier;
import java.io.File;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Created by Timofey Klyubin on 13.12.18
 */
public class ServiceGenerator extends AbstractGenerator {

    @SuppressWarnings("unused")
    private static final Logger LOG = Logger.getLogger(ServiceGenerator.class);
    private static final ClassName SPRING_REQUIRED_ANNOTATION_CLASS = ClassName.get("org.springframework.beans.factory.annotation", "Required");

    private final File platformPath;
    private final ModelFileData fileData;
    private final Set<String> searchFields;
    private final JavaFile daoInterface;

    private final TypeName daoTypeName;

    private final TypeName searchMethodReturnType;
    private final TypeName searchType;

    public ServiceGenerator(File platformPath, ModelFileData modelFileData, Set<String> searchFields, JavaFile daoInterface) {
        this.platformPath = platformPath;
        this.fileData = modelFileData;
        this.searchFields = new HashSet<>(searchFields);
        this.daoInterface = daoInterface;

        this.daoTypeName = ClassName.get(this.daoInterface.packageName, this.daoInterface.typeSpec.name);

        this.searchType = ClassName.get(fileData.getModelPackage(), fileData.getClassName());
        this.searchMethodReturnType = ParameterizedTypeName.get(
                ClassName.get(Optional.class),
                this.searchType
        );
    }

    private String generateBasePackage(String corePackage, String suffix) {
        return corePackage.replaceAll(".model([^\\w]|$)", "").concat(suffix);
    }

    public String getDaoFieldName() {
        return StringUtils.uncapitalize(this.daoInterface.typeSpec.name);
    }

    public JavaFile generateServiceImplementationFile(boolean withFindAll) {
        final String packageStr = generateBasePackage(fileData.getModelPackage(), ".service.impl");
        final String interfacePackageStr = generateBasePackage(fileData.getModelPackage(), ".service");
        TypeSpec daoImpl = generateServiceImpl(withFindAll, ClassName.get(interfacePackageStr,
                fileData.getClassName().concat("Service")));

        return JavaFile.builder(packageStr, daoImpl)
                       .indent(INDENT)
                       .skipJavaLangImports(true)
                       .build();
    }

    public JavaFile generateServiceInterfaceFile(boolean withFindAll) {
        TypeSpec daoInterface = generateServiceInterface(withFindAll);
        final String packageStr = generateBasePackage(fileData.getModelPackage(), ".service");

        return JavaFile.builder(packageStr, daoInterface)
                   .skipJavaLangImports(true)
                   .indent(INDENT)
                   .build();
    }

    private TypeSpec generateServiceInterface(boolean withFindAll) {
        ModelFieldData fieldData = null;
        for (ModelFieldData modelFieldData : fileData.getFields()) {
            if (modelFieldData.getFieldName().equals(searchFields.iterator().next())) {
                fieldData = modelFieldData;
            } else {
                // TODO
            }
        }

        String capitalized = StringUtils.capitalize(fieldData.getFieldName());
        MethodSpec getSearchMethod = MethodSpec.methodBuilder("getBy".concat(capitalized))
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(searchMethodReturnType)
                .addParameter(ClassName.get(fieldData.getFieldTypePackage(), fieldData.getFieldType()), fieldData.getFieldName(), Modifier.FINAL)
                .build();

        TypeSpec.Builder serviceInterfaceBuilder = TypeSpec.interfaceBuilder(fileData.getClassName().concat("Service"))
                .addModifiers(Modifier.PUBLIC)
                .addMethod(getSearchMethod);

        if (withFindAll) {
            MethodSpec getFindAllMethod = MethodSpec.methodBuilder("getAll")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(ParameterizedTypeName.get(ClassName.get(List.class), ClassName.get(fileData.getModelPackage(), fileData.getClassName())))
                .build();

            serviceInterfaceBuilder.addMethod(getFindAllMethod);
        }

        return serviceInterfaceBuilder.build();
    }

    private TypeName getFieldTypeName(ModelFieldData fieldData) {
        return ClassName.get(fieldData.getFieldTypePackage(), fieldData.getFieldType());
    }

    private TypeSpec generateServiceImpl(boolean withFindAll, TypeName serviceInterface) {
        ModelFieldData fieldData = null;
        for (ModelFieldData modelFieldData : fileData.getFields()) {
            if (modelFieldData.getFieldName().equals(searchFields.iterator().next())) {
                fieldData = modelFieldData;
            } else {
                // TODO
            }
        }

        String capitalized = StringUtils.capitalize(fieldData.getFieldName());

        // Fields
        final String daoFieldName = getDaoFieldName();
        FieldSpec fsServiceField = FieldSpec
                .builder(this.daoTypeName, daoFieldName, Modifier.PRIVATE).build();

        // Bean injection methods
        final String setDaoMethodName = "set".concat(this.daoInterface.typeSpec.name);
        final String getDaoMethodName = "get".concat(this.daoInterface.typeSpec.name);
        MethodSpec setDaoMethod = MethodSpec.methodBuilder(setDaoMethodName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(SPRING_REQUIRED_ANNOTATION_CLASS)
                .returns(TypeName.VOID)
                .addParameter(this.daoTypeName, daoFieldName)
                .addStatement("this.$1N = $1N", daoFieldName)
                .build();

        MethodSpec getDaoMethod = MethodSpec.methodBuilder(getDaoMethodName)
                .addModifiers(Modifier.PRIVATE)
                .returns(this.daoTypeName)
                                          // TODO: add assertion on parameter
                .addStatement("return this.$1N", daoFieldName)
                .build();

        // Search Methods
        final String searchMethodName = "getBy".concat(capitalized);
        MethodSpec getSearchMethod = MethodSpec.methodBuilder(searchMethodName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(searchMethodReturnType)
                .addParameter(getFieldTypeName(fieldData), fieldData.getFieldName(), Modifier.FINAL)
                .addStatement("return this.$1N().$2N($3N)", getDaoMethod, searchMethodName, fieldData.getFieldName())
                .build();

        // Class
        TypeSpec.Builder serviceImplBuilder = TypeSpec.classBuilder(fileData.getClassName().concat("ServiceImpl"))
               .addModifiers(Modifier.PUBLIC)
               .addSuperinterface(serviceInterface)
               .addField(fsServiceField)
               .addMethod(getSearchMethod)
               .addMethod(setDaoMethod)
               .addMethod(getDaoMethod);

        if (withFindAll) {
            final String getAllMethodName = "getAll";
            MethodSpec getFindAllMethod = MethodSpec.methodBuilder(getAllMethodName)
               .addModifiers(Modifier.PUBLIC)
               .addAnnotation(Override.class)
               .returns(ParameterizedTypeName.get(ClassName.get(List.class), searchType))
               .addStatement("return this.$1N().$2N()", getDaoMethod, getAllMethodName)
               .build();

            serviceImplBuilder.addMethod(getFindAllMethod);
        }

        return serviceImplBuilder.build();
    }
}
