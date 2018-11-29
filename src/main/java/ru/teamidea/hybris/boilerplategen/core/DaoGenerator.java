package ru.teamidea.hybris.boilerplategen.core;

import com.squareup.javapoet.*;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import ru.teamidea.hybris.boilerplategen.core.data.ModelFieldData;
import ru.teamidea.hybris.boilerplategen.core.data.ModelFileData;

import javax.lang.model.element.Modifier;
import java.io.File;
import java.util.*;

/**
 * Created by Timofey Klyubin on 14.03.18
 */
public class DaoGenerator extends AbstractGenerator {

    @SuppressWarnings("unused")
    private static final Logger LOG = Logger.getLogger(DaoGenerator.class);

    private static final String FS_PACKAGE = "de.hybris.platform.servicelayer.search";
    private static final String FS_QUERY_CLASS = "FlexibleSearchQuery";
    private static final String FS_SERVICE_CLASS = "FlexibleSearchService";
    private static final String FS_SEARCH_RESULT_CLASS = "SearchResult";
    private static final TypeName FS_QUERY_TYPE = ClassName.get(FS_PACKAGE, FS_QUERY_CLASS);
    private static final TypeName FS_SERVICE_TYPE = ClassName.get(FS_PACKAGE, FS_SERVICE_CLASS);
    private static final TypeName FS_SEARCH_RESULT_TYPE = ClassName.get(FS_PACKAGE, FS_SEARCH_RESULT_CLASS);

    private final File platformPath;
    private final ModelFileData fileData;
    private final Set<String> searchFields;

    private final TypeName searchMethodReturnType;
    private final TypeName searchType;

    public DaoGenerator(File platformPath, ModelFileData modelFileData, Set<String> searchFields) {
        this.platformPath = platformPath;
        this.fileData = modelFileData;
        this.searchFields = new HashSet<>(searchFields);

        this.searchType = ClassName.get(fileData.getModelPackage(), fileData.getClassName());
        this.searchMethodReturnType = ParameterizedTypeName.get(
                ClassName.get(Optional.class),
                this.searchType
        );
    }

    public JavaFile generateDaoImplementationFile(boolean withFindAll) {
        TypeSpec daoImpl = generateDaoImpl(withFindAll, ClassName.get(fileData.getModelPackage().concat(".dao.impl"),
                fileData.getClassName().concat("Dao")));

        return JavaFile.builder(fileData.getModelPackage().concat(".dao"), daoImpl)
                       .skipJavaLangImports(true)
                       .build();
    }

    public JavaFile generateDaoInterfaceFile(boolean withFindAll) {
        TypeSpec daoInterface = generateDaoInterface(withFindAll);

        return JavaFile.builder(fileData.getModelPackage().concat(".dao"), daoInterface)
                    .skipJavaLangImports(true)
                    .build();
    }

    private TypeSpec generateDaoInterface(boolean withFindAll) {
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

        TypeSpec.Builder daoInterfaceBuilder = TypeSpec.interfaceBuilder(fileData.getClassName().concat("Dao"))
                .addModifiers(Modifier.PUBLIC)
                .addMethod(getSearchMethod);

        if (withFindAll) {
            MethodSpec getFindAllMethod = MethodSpec.methodBuilder("getAll")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(ParameterizedTypeName.get(ClassName.get(List.class), ClassName.get(fileData.getModelPackage(), fileData.getClassName())))
                .build();

            daoInterfaceBuilder.addMethod(getFindAllMethod);
        }

        return daoInterfaceBuilder.build();
    }

    private TypeName getFieldTypeName(ModelFieldData fieldData) {
        return ClassName.get(fieldData.getFieldTypePackage(), fieldData.getFieldType());
    }

    private TypeSpec generateDaoImpl(boolean withFindAll, TypeName daoInterface) {
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
        final String fqFindAllFieldName = "FQ_FIND_ALL";
        final String fqFindFieldName = "FQ_FIND";
        final String fsServiceFieldName = "flexibleSearchService";
        FieldSpec fqFindAllQueryConst = FieldSpec
                .builder(ClassName.get(String.class), fqFindAllFieldName, Modifier.PRIVATE, Modifier.FINAL, Modifier.STATIC)
                .initializer("\"SELECT {i.\" + $1T.PK + \"} FROM {\" + $1T._TYPECODE + \" AS i}\"", searchType)
                .build();

        FieldSpec fqFindQueryConst = FieldSpec
                .builder(ClassName.get(String.class), fqFindFieldName, Modifier.PRIVATE, Modifier.FINAL, Modifier.STATIC)
                .initializer("\"SELECT {i.\" + $1T.PK + \"} FROM {\" + $1T._TYPECODE + \" AS i} WHERE {i.$2L} = ?$2L\"", searchType, fieldData.getFieldName())
                .build();

        FieldSpec fsServiceField = FieldSpec
                .builder(FS_SERVICE_TYPE, fsServiceFieldName, Modifier.PRIVATE).build();

        // Bean injection methods
        final String setFsServiceMethodName = "setFlexibleSearchService";
        final String getFsServiceMethodName = "getFlexibleSearchService";
        MethodSpec setFsServiceMethod = MethodSpec.methodBuilder(setFsServiceMethodName)
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.VOID)
                .addParameter(FS_SERVICE_TYPE, fsServiceFieldName)
                .addStatement("this.$1N = $1N", fsServiceFieldName)
                .build();

        MethodSpec getFsServiceMethod = MethodSpec.methodBuilder(getFsServiceMethodName)
                .addModifiers(Modifier.PRIVATE)
                .returns(FS_SERVICE_TYPE)
                .addStatement("return this.$1N", fsServiceFieldName)
                .build();

        // Search Methods
        MethodSpec getSearchMethod = MethodSpec.methodBuilder("getBy".concat(capitalized))
                .addModifiers(Modifier.PUBLIC)
                .returns(searchMethodReturnType)
                .addParameter(getFieldTypeName(fieldData), fieldData.getFieldName(), Modifier.FINAL)
                .addStatement("$1T query = new $1T($2N)", FS_QUERY_TYPE, fqFindFieldName)
                .addStatement("query.addQueryParameter($1S, $1N)", fieldData.getFieldName())
                .addStatement("$3T<$1T> result = $2N().search(query)", searchType, getFsServiceMethodName, FS_SEARCH_RESULT_TYPE)
                .beginControlFlow("if (result.getCount() == 0)")
                .addStatement("return $1T.empty()", ClassName.get(Optional.class))
                .endControlFlow()
                .addStatement("return $1T.ofNullable(result.getResult().iterator().next())", ClassName.get(Optional.class))
                .build();

        // Class
        TypeSpec.Builder daoImplBuilder = TypeSpec.classBuilder(fileData.getClassName().concat("DaoImpl"))
               .addModifiers(Modifier.PUBLIC)
               .addSuperinterface(daoInterface)
               .addField(fqFindAllQueryConst)
               .addField(fqFindQueryConst)
               .addField(fsServiceField)
               .addMethod(getSearchMethod)
               .addMethod(setFsServiceMethod)
               .addMethod(getFsServiceMethod);

        if (withFindAll) {
            MethodSpec getFindAllMethod = MethodSpec.methodBuilder("getAll")
               .addModifiers(Modifier.PUBLIC)
               .returns(ParameterizedTypeName.get(ClassName.get(List.class), searchType))
               .addStatement("$1T query = new $1T($2N)", FS_QUERY_TYPE, fqFindAllFieldName)
               .addStatement("$3T<$1T> result = $2N().search(query)", searchType, getFsServiceMethodName, FS_SEARCH_RESULT_TYPE)
               .beginControlFlow("if (result.getCount() == 0)")
               .addStatement("return $1T.emptyList()", ClassName.get(Collections.class))
               .endControlFlow()
               .addStatement("return $1T.unmodifiableList(result.getResult())", ClassName.get(Collections.class))
               .build();

            daoImplBuilder.addMethod(getFindAllMethod);
        }

        return daoImplBuilder.build();
    }
}
