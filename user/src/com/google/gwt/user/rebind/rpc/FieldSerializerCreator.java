/*
 * Copyright 2006 Google Inc.
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
import com.google.gwt.core.ext.typeinfo.JPackage;
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

    fSerializationOracle = serializationOracle;
    fSerializableClass = requestedClass;
  }

  public String realize(TreeLogger logger, GeneratorContext ctx) {
    assert (ctx != null);
    assert (fSerializationOracle.isSerializable(getSerializableClass()));

    logger = logger.branch(TreeLogger.SPAM,
        "Generating a field serializer for type '"
            + fSerializableClass.getQualifiedSourceName() + "'", null);
    String fieldSerializerName = fSerializationOracle
        .getFieldSerializerName(getSerializableClass());

    fSourceWriter = getSourceWriter(logger, ctx);
    if (fSourceWriter == null) {
      return fieldSerializerName;
    }
    assert fSourceWriter != null;

    writeFieldAccessors();

    writeSerializeMethod();

    writeDeserializeMethod();

    fSourceWriter.commit(logger);

    return fieldSerializerName;
  }

  /**
   * Returns the binary type name of a given {@link JClassType}.
   * 
   * @param classType
   * @return the binary name of a {@link JClassType}
   * 
   * @see java.lang.Class#getName()
   */
  private String getBinaryTypeName(JClassType classType) {
    return classType.getPackage().getName() + "."
        + classType.getName().replace('.', '$');
  }

  /**
   * Returns the name of the field serializer which will deal with this class.
   * 
   * @return name of the field serializer
   */
  private String getFieldSerializerClassName() {
    String sourceName = fSerializationOracle
        .getFieldSerializerName(getSerializableClass());

    int qualifiedSourceNameStart = sourceName.lastIndexOf('.');
    if (qualifiedSourceNameStart >= 0) {
      sourceName = sourceName.substring(qualifiedSourceNameStart + 1);
    }

    return sourceName;
  }

  /**
   * Returns the package that will contain the field serializer.
   * 
   * @return the package where this field serializer will live
   */
  private JPackage getFieldSerializerPackage() {
    JClassType classType = getSerializableClass();
    return classType.getPackage();
  }

  private JClassType getSerializableClass() {
    return fSerializableClass;
  }

  /**
   * Returns an array of serializable fields declared on this class.
   * 
   * @return array of serializable fields
   */
  private JField[] getSerializableFields() {
    if (fSerializableFields != null) {
      return fSerializableFields;
    }

    fSerializableFields = fSerializationOracle
        .applyFieldSerializationPolicy(getSerializableClass());
    return fSerializableFields;
  }

  /**
   * Returns the next serializable superclass.
   * 
   * @param serializableClass
   * @return next serializable superclass
   */
  private JClassType getSerializableSuperclass(JClassType serializableClass) {
    if (serializableClass == null) {
      return null;
    }

    JClassType superClass = serializableClass.getSuperclass();
    if (superClass != null) {
      SerializableTypeOracle serializationOracle = getSerializationOracle();
      if (serializationOracle.isSerializable(superClass)) {
        return superClass;
      }

      return getSerializableSuperclass(superClass);
    }

    return null;
  }

  private SerializableTypeOracle getSerializationOracle() {
    return fSerializationOracle;
  }

  private SourceWriter getSourceWriter(TreeLogger logger, GeneratorContext ctx) {
    String packageName = getFieldSerializerPackage().getName();
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
     * class that they serialize.  This enables the serializer class to 
     * access all fields except those that are private.
     * 
     * Java Access Levels:
     *   default - package
     *   private - class only
     *   protected - package and all subclasses
     *   public - all
     */
    return field.isPrivate();
  }

  private void writeDeserializeMethod() {
    fSourceWriter.print("public static void deserialize(");
    fSourceWriter.print(SerializationStreamReader.class.getName());
    fSourceWriter.print(" streamReader, ");
    fSourceWriter.print(getSerializableClass().getQualifiedSourceName());
    fSourceWriter.println(" instance) throws "
        + SerializationException.class.getName() + "{");
    fSourceWriter.indent();

    writeFieldDeserializationStatements();

    JClassType serializableClass = getSerializableClass().isClass();
    if (serializableClass != null) {
      JClassType serializableSuperClass = getSerializableSuperclass(serializableClass);
      if (serializableSuperClass != null) {
        String fieldSerializerName = fSerializationOracle
            .getFieldSerializerName(serializableSuperClass);
        fSourceWriter.print(fieldSerializerName);
        fSourceWriter.println(".deserialize(streamReader, instance);");
      }
    }

    fSourceWriter.outdent();
    fSourceWriter.println("}");
    fSourceWriter.println();
  }

  /**
   * This method will generate a native JSNI accessor method for every field
   * that is protected, private using the "Violator" pattern to allow an
   * external class to access the field's value.
   */
  private void writeFieldAccessors() {
    JField[] serializableFields = getSerializableFields();
    int fieldCount = serializableFields.length;

    for (int fieldIndex = 0; fieldIndex < fieldCount; ++fieldIndex) {
      JField serializableField = serializableFields[fieldIndex];

      if (!needsAccessorMethods(serializableField)) {
        continue;
      }

      writeFieldGet(serializableField);
      writeFieldSet(serializableField);
    }
  }

  private void writeFieldDeserializationStatements() {
    JType type = getSerializableClass();
    JArrayType isArray = type.isArray();
    if (isArray != null) {
      fSourceWriter
          .println("for (int itemIndex = 0; itemIndex < instance.length; ++itemIndex) {");
      fSourceWriter.indent();

      JType componentType = isArray.getComponentType();

      fSourceWriter.print("instance[itemIndex] = ");
      if (Shared.typeNeedsCast(componentType)) {
        fSourceWriter
            .print("(" + componentType.getQualifiedSourceName() + ") ");
      }
      String readMethodName = "streamReader.read"
          + Shared.getCallSuffix(componentType);
      fSourceWriter.println(readMethodName + "();");
      fSourceWriter.outdent();
      fSourceWriter.println("}");
    } else {
      JField[] serializableFields = getSerializableFields();
      int fieldCount = serializableFields.length;

      for (int fieldIndex = 0; fieldIndex < fieldCount; ++fieldIndex) {
        JField serializableField = serializableFields[fieldIndex];
        JType fieldType = serializableField.getType();
        boolean needsAccessor = needsAccessorMethods(serializableField);

        String readMethodName = "read" + Shared.getCallSuffix(fieldType);
        String streamReadExpression = "streamReader." + readMethodName + "()";
        if (Shared.typeNeedsCast(fieldType)) {
          streamReadExpression = "(" + fieldType.getQualifiedSourceName()
              + ") " + streamReadExpression;
        }

        if (needsAccessor) {
          fSourceWriter.print("set");
          fSourceWriter.print(Shared.capitalize(serializableField.getName()));
          fSourceWriter.print("(instance, ");
          fSourceWriter.print(streamReadExpression);
          fSourceWriter.println(");");
        } else {
          fSourceWriter.print("instance.");
          fSourceWriter.print(serializableField.getName());
          fSourceWriter.print(" = ");
          fSourceWriter.print(streamReadExpression);
          fSourceWriter.println(";");
        }
      }
    }

    fSourceWriter.println();
  }

  /**
   * Write a getter method for an instance field.
   */
  private void writeFieldGet(JField serializableField) {
    JType fieldType = serializableField.getType();
    String fieldTypeQualifiedSourceName = fieldType.getQualifiedSourceName();
    String fieldName = serializableField.getName();

    fSourceWriter.print("private static native ");
    fSourceWriter.print(fieldTypeQualifiedSourceName);
    fSourceWriter.print(" get");
    fSourceWriter.print(Shared.capitalize(fieldName));
    fSourceWriter.print("(");
    fSourceWriter.print(getSerializableClass().getQualifiedSourceName());
    fSourceWriter.println(" instance) /*-{");
    fSourceWriter.indent();

    fSourceWriter.print("return instance.@");
    fSourceWriter.print(getBinaryTypeName(getSerializableClass()));
    fSourceWriter.print("::");
    fSourceWriter.print(fieldName);
    fSourceWriter.println(";");

    fSourceWriter.outdent();
    fSourceWriter.println("}-*/;");
    fSourceWriter.println();
  }

  private void writeFieldSerializationStatements() {
    JType type = getSerializableClass();
    JArrayType isArray = type.isArray();
    if (isArray != null) {
      fSourceWriter.println("int itemCount = instance.length;");
      fSourceWriter.println();
      fSourceWriter.println("streamWriter.writeInt(itemCount);");
      fSourceWriter.println();
      fSourceWriter
          .println("for (int itemIndex = 0; itemIndex < itemCount; ++itemIndex) {");
      fSourceWriter.indent();
      String writeMethodName = "write"
          + Shared.getCallSuffix(isArray.getComponentType());
      fSourceWriter.print("streamWriter.");
      fSourceWriter.print(writeMethodName);
      fSourceWriter.println("(instance[itemIndex]);");
      fSourceWriter.outdent();
      fSourceWriter.println("}");
    } else {
      JField[] serializableFields = getSerializableFields();
      int fieldCount = serializableFields.length;

      for (int fieldIndex = 0; fieldIndex < fieldCount; ++fieldIndex) {
        JField serializableField = serializableFields[fieldIndex];
        JType fieldType = serializableField.getType();

        String writeMethodName = "write" + Shared.getCallSuffix(fieldType);
        fSourceWriter.print("streamWriter.");
        fSourceWriter.print(writeMethodName);
        fSourceWriter.print("(");

        if (needsAccessorMethods(serializableField)) {
          fSourceWriter.print("get");
          fSourceWriter.print(Shared.capitalize(serializableField.getName()));
          fSourceWriter.println("(instance));");
        } else {
          fSourceWriter.print("instance.");
          fSourceWriter.print(serializableField.getName());
          fSourceWriter.println(");");
        }
      }
    }

    fSourceWriter.println();
  }

  /**
   * Write a setter method for an instance field.
   */
  private void writeFieldSet(JField serializableField) {
    JType fieldType = serializableField.getType();
    String fieldTypeQualifiedSourceName = fieldType.getQualifiedSourceName();
    String serializableClassQualifedName = getSerializableClass()
        .getQualifiedSourceName();
    String fieldName = serializableField.getName();

    fSourceWriter.print("private static native void ");
    fSourceWriter.print(" set");
    fSourceWriter.print(Shared.capitalize(fieldName));
    fSourceWriter.print("(");
    fSourceWriter.print(serializableClassQualifedName);
    fSourceWriter.print(" instance, ");
    fSourceWriter.print(fieldTypeQualifiedSourceName);
    fSourceWriter.println(" value) /*-{");
    fSourceWriter.indent();

    fSourceWriter.print("instance.@");
    fSourceWriter.print(getBinaryTypeName(getSerializableClass()));
    fSourceWriter.print("::");
    fSourceWriter.print(fieldName);
    fSourceWriter.println(" = value;");

    fSourceWriter.outdent();
    fSourceWriter.println("}-*/;");
    fSourceWriter.println();
  }

  private void writeSerializeMethod() {
    fSourceWriter.print("public static void serialize(");
    fSourceWriter.print(SerializationStreamWriter.class.getName());
    fSourceWriter.print(" streamWriter, ");
    fSourceWriter.print(getSerializableClass().getQualifiedSourceName());
    fSourceWriter.println(" instance) throws "
        + SerializationException.class.getName() + " {");
    fSourceWriter.indent();

    writeFieldSerializationStatements();

    JClassType serializableClass = getSerializableClass().isClass();
    if (serializableClass != null) {
      JClassType serializableSuperclass = getSerializableSuperclass(serializableClass);
      if (serializableSuperclass != null) {
        String fieldSerializerName = fSerializationOracle
            .getFieldSerializerName(serializableSuperclass);
        fSourceWriter.print(fieldSerializerName);
        fSourceWriter.println(".serialize(streamWriter, instance);");
      }
    }

    fSourceWriter.outdent();
    fSourceWriter.println("}");
    fSourceWriter.println();
  }

  private JClassType fSerializableClass;
  private JField[] fSerializableFields;
  private SerializableTypeOracle fSerializationOracle;
  private SourceWriter fSourceWriter;

}
