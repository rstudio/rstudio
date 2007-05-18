/*
 * Copyright 2007 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.user.rebind.rpc;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.typeinfo.JArrayType;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;

import java.io.PrintWriter;

/**
 * <p>
 * This class creates field serializers for a particular class. If the class has
 * a custom serializer then that class is used rather than creating one.
 * 
 * <p>
 * Generated field serializers are emitted into the same package as the class
 * that they serialize.
 * 
 * <p>
 * Fields are considered serializable if:
 * <ul>
 * <li>Field is not static
 * <li>Field is not transient
 * </ul>
 * 
 * TODO(mmendez): Need to make the generated field serializers final
 * TODO(mmendez): Would be nice to be able to have imports, rather than using
 * fully qualified type names everywhere
 */
public class FieldSerializerCreator {

  private final JClassType serializableClass;

  private final JField[] serializableFields;

  private final SerializableTypeOracle serializationOracle;

  private SourceWriter sourceWriter;

  /**
   * Constructs a field serializer for the class.
   * 
   * @param serializationOracle
   * @param requestedClass
   */
  public FieldSerializerCreator(SerializableTypeOracle serializationOracle,
      JClassType requestedClass) {
    assert (requestedClass != null);
    assert (requestedClass.isClass() != null);

    this.serializationOracle = serializationOracle;
    serializableClass = requestedClass;
    serializableFields = serializationOracle.getSerializableFields(requestedClass);
  }

  public String realize(TreeLogger logger, GeneratorContext ctx) {
    assert (ctx != null);
    assert (serializationOracle.isSerializable(serializableClass));

    logger = logger.branch(TreeLogger.DEBUG,
        "Generating a field serializer for type '"
            + serializableClass.getQualifiedSourceName() + "'", null);
    String fieldSerializerName = serializationOracle.getFieldSerializerName(serializableClass);

    sourceWriter = getSourceWriter(logger, ctx);
    if (sourceWriter == null) {
      return fieldSerializerName;
    }
    assert sourceWriter != null;

    writeFieldAccessors();

    writeSerializeMethod();

    writeDeserializeMethod();

    sourceWriter.commit(logger);

    return fieldSerializerName;
  }

  /**
   * Returns the name of the field serializer which will deal with this class.
   * 
   * @return name of the field serializer
   */
  private String getFieldSerializerClassName() {
    String sourceName = serializationOracle.getFieldSerializerName(serializableClass);

    int qualifiedSourceNameStart = sourceName.lastIndexOf('.');
    if (qualifiedSourceNameStart >= 0) {
      sourceName = sourceName.substring(qualifiedSourceNameStart + 1);
    }

    return sourceName;
  }

  private SourceWriter getSourceWriter(TreeLogger logger, GeneratorContext ctx) {
    String packageName = serializableClass.getPackage().getName();
    String className = getFieldSerializerClassName();

    PrintWriter printWriter = ctx.tryCreate(logger, packageName, className);
    if (printWriter == null) {
      return null;
    }

    ClassSourceFileComposerFactory composerFactory = new ClassSourceFileComposerFactory(
        packageName, className);

    return composerFactory.createSourceWriter(ctx, printWriter);
  }

  /**
   * Returns true if we will need a get/set method pair for a field.
   * 
   * @return true if the the field requires accessor methods
   */
  private boolean needsAccessorMethods(JField field) {
    /*
     * Field serializers are always emitted into the the same package as the
     * class that they serialize. This enables the serializer class to access
     * all fields except those that are private.
     * 
     * Java Access Levels: default - package private - class only protected -
     * package and all subclasses public - all
     */
    return field.isPrivate();
  }

  private void writeDeserializeMethod() {
    sourceWriter.print("public static void deserialize(");
    sourceWriter.print(SerializationStreamReader.class.getName());
    sourceWriter.print(" streamReader, ");
    sourceWriter.print(serializableClass.getQualifiedSourceName());
    sourceWriter.println(" instance) throws "
        + SerializationException.class.getName() + "{");
    sourceWriter.indent();

    writeFieldDeserializationStatements();

    JClassType superClass = serializableClass.getSuperclass();
    if (superClass != null && serializationOracle.isSerializable(superClass)) {
      String fieldSerializerName = serializationOracle.getFieldSerializerName(superClass);
      sourceWriter.print(fieldSerializerName);
      sourceWriter.println(".deserialize(streamReader, instance);");
    }

    sourceWriter.outdent();
    sourceWriter.println("}");
    sourceWriter.println();
  }

  /**
   * This method will generate a native JSNI accessor method for every field
   * that is protected, private using the "Violator" pattern to allow an
   * external class to access the field's value.
   */
  private void writeFieldAccessors() {
    JField[] jFields = serializableFields;
    int fieldCount = jFields.length;

    for (int fieldIndex = 0; fieldIndex < fieldCount; ++fieldIndex) {
      JField serializableField = jFields[fieldIndex];

      if (!needsAccessorMethods(serializableField)) {
        continue;
      }

      writeFieldGet(serializableField);
      writeFieldSet(serializableField);
    }
  }

  private void writeFieldDeserializationStatements() {
    JType type = serializableClass;
    JArrayType isArray = type.isArray();
    if (isArray != null) {
      sourceWriter.println("for (int itemIndex = 0; itemIndex < instance.length; ++itemIndex) {");
      sourceWriter.indent();

      JType componentType = isArray.getComponentType();

      sourceWriter.print("instance[itemIndex] = ");
      if (Shared.typeNeedsCast(componentType)) {
        sourceWriter.print("(" + componentType.getQualifiedSourceName() + ") ");
      }
      String readMethodName = "streamReader.read"
          + Shared.getCallSuffix(componentType);
      sourceWriter.println(readMethodName + "();");
      sourceWriter.outdent();
      sourceWriter.println("}");
    } else {
      JField[] jFields = serializableFields;
      int fieldCount = jFields.length;

      for (int fieldIndex = 0; fieldIndex < fieldCount; ++fieldIndex) {
        JField serializableField = jFields[fieldIndex];
        JType fieldType = serializableField.getType();
        boolean needsAccessor = needsAccessorMethods(serializableField);

        String readMethodName = "read" + Shared.getCallSuffix(fieldType);
        String streamReadExpression = "streamReader." + readMethodName + "()";
        if (Shared.typeNeedsCast(fieldType)) {
          streamReadExpression = "(" + fieldType.getQualifiedSourceName()
              + ") " + streamReadExpression;
        }

        if (needsAccessor) {
          sourceWriter.print("set");
          sourceWriter.print(Shared.capitalize(serializableField.getName()));
          sourceWriter.print("(instance, ");
          sourceWriter.print(streamReadExpression);
          sourceWriter.println(");");
        } else {
          sourceWriter.print("instance.");
          sourceWriter.print(serializableField.getName());
          sourceWriter.print(" = ");
          sourceWriter.print(streamReadExpression);
          sourceWriter.println(";");
        }
      }
    }

    sourceWriter.println();
  }

  /**
   * Write a getter method for an instance field.
   */
  private void writeFieldGet(JField serializableField) {
    JType fieldType = serializableField.getType();
    String fieldTypeQualifiedSourceName = fieldType.getQualifiedSourceName();
    String fieldName = serializableField.getName();

    sourceWriter.print("private static native ");
    sourceWriter.print(fieldTypeQualifiedSourceName);
    sourceWriter.print(" get");
    sourceWriter.print(Shared.capitalize(fieldName));
    sourceWriter.print("(");
    sourceWriter.print(serializableClass.getQualifiedSourceName());
    sourceWriter.println(" instance) /*-{");
    sourceWriter.indent();

    sourceWriter.print("return instance.@");
    sourceWriter.print(serializationOracle.getSerializedTypeName(serializableClass));
    sourceWriter.print("::");
    sourceWriter.print(fieldName);
    sourceWriter.println(";");

    sourceWriter.outdent();
    sourceWriter.println("}-*/;");
    sourceWriter.println();
  }

  private void writeFieldSerializationStatements() {
    JType type = serializableClass;
    JArrayType isArray = type.isArray();
    if (isArray != null) {
      sourceWriter.println("int itemCount = instance.length;");
      sourceWriter.println();
      sourceWriter.println("streamWriter.writeInt(itemCount);");
      sourceWriter.println();
      sourceWriter.println("for (int itemIndex = 0; itemIndex < itemCount; ++itemIndex) {");
      sourceWriter.indent();
      String writeMethodName = "write"
          + Shared.getCallSuffix(isArray.getComponentType());
      sourceWriter.print("streamWriter.");
      sourceWriter.print(writeMethodName);
      sourceWriter.println("(instance[itemIndex]);");
      sourceWriter.outdent();
      sourceWriter.println("}");
    } else {
      JField[] jFields = serializableFields;
      int fieldCount = jFields.length;

      for (int fieldIndex = 0; fieldIndex < fieldCount; ++fieldIndex) {
        JField serializableField = jFields[fieldIndex];
        JType fieldType = serializableField.getType();

        String writeMethodName = "write" + Shared.getCallSuffix(fieldType);
        sourceWriter.print("streamWriter.");
        sourceWriter.print(writeMethodName);
        sourceWriter.print("(");

        if (needsAccessorMethods(serializableField)) {
          sourceWriter.print("get");
          sourceWriter.print(Shared.capitalize(serializableField.getName()));
          sourceWriter.println("(instance));");
        } else {
          sourceWriter.print("instance.");
          sourceWriter.print(serializableField.getName());
          sourceWriter.println(");");
        }
      }
    }

    sourceWriter.println();
  }

  /**
   * Write a setter method for an instance field.
   */
  private void writeFieldSet(JField serializableField) {
    JType fieldType = serializableField.getType();
    String fieldTypeQualifiedSourceName = fieldType.getQualifiedSourceName();
    String serializableClassQualifedName = serializableClass.getQualifiedSourceName();
    String fieldName = serializableField.getName();

    sourceWriter.print("private static native void ");
    sourceWriter.print(" set");
    sourceWriter.print(Shared.capitalize(fieldName));
    sourceWriter.print("(");
    sourceWriter.print(serializableClassQualifedName);
    sourceWriter.print(" instance, ");
    sourceWriter.print(fieldTypeQualifiedSourceName);
    sourceWriter.println(" value) /*-{");
    sourceWriter.indent();

    sourceWriter.print("instance.@");
    sourceWriter.print(serializationOracle.getSerializedTypeName(serializableClass));
    sourceWriter.print("::");
    sourceWriter.print(fieldName);
    sourceWriter.println(" = value;");

    sourceWriter.outdent();
    sourceWriter.println("}-*/;");
    sourceWriter.println();
  }

  private void writeSerializeMethod() {
    sourceWriter.print("public static void serialize(");
    sourceWriter.print(SerializationStreamWriter.class.getName());
    sourceWriter.print(" streamWriter, ");
    sourceWriter.print(serializableClass.getQualifiedSourceName());
    sourceWriter.println(" instance) throws "
        + SerializationException.class.getName() + " {");
    sourceWriter.indent();

    writeFieldSerializationStatements();

    JClassType superClass = serializableClass.getSuperclass();
    if (superClass != null && serializationOracle.isSerializable(superClass)) {
      String fieldSerializerName = serializationOracle.getFieldSerializerName(superClass);
      sourceWriter.print(fieldSerializerName);
      sourceWriter.println(".serialize(streamWriter, instance);");
    }
    
    sourceWriter.outdent();
    sourceWriter.println("}");
    sourceWriter.println();
  }

}
