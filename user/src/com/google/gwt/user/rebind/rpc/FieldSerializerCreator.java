/*
 * Copyright 2008 Google Inc.
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

import com.google.gwt.core.client.UnsafeNativeLong;
import com.google.gwt.core.client.impl.WeakMapping;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.typeinfo.JArrayType;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JEnumType;
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.javac.TypeOracleMediator;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import com.google.gwt.user.client.rpc.core.java.lang.Object_Array_CustomFieldSerializer;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;

import java.io.PrintWriter;

/**
 * Creates a field serializer for a class that implements
 * {@link com.google.gwt.user.client.rpc.IsSerializable IsSerializable} or
 * {@link java.io.Serializable Serializable}. The field serializer is emitted
 * into the same package as the class that it serializes.
 * 
 * TODO(mmendez): Need to make the generated field serializers final
 * TODO(mmendez): Would be nice to be able to have imports, rather than using
 * fully qualified type names everywhere
 */
public class FieldSerializerCreator {
  
  private static final String WEAK_MAPPING_CLASS_NAME = WeakMapping.class.getName();

  private final JClassType serializableClass;

  private final JField[] serializableFields;

  private SourceWriter sourceWriter;

  private final SerializableTypeOracle typesSentFromBrowser;

  private final SerializableTypeOracle typesSentToBrowser;

  private final TypeOracle typeOracle;

  /**
   * Constructs a field serializer for the class.
   */
  public FieldSerializerCreator(TypeOracle typeOracle,
      SerializableTypeOracle typesSentFromBrowser,
      SerializableTypeOracle typesSentToBrowser, JClassType requestedClass) {
    assert (requestedClass != null);
    assert (requestedClass.isClass() != null || requestedClass.isArray() != null);

    this.typeOracle = typeOracle;
    this.typesSentFromBrowser = typesSentFromBrowser;
    this.typesSentToBrowser = typesSentToBrowser;
    serializableClass = requestedClass;
    serializableFields = SerializationUtils.getSerializableFields(typeOracle,
        requestedClass);
  }

  public String realize(TreeLogger logger, GeneratorContext ctx) {
    assert (ctx != null);
    assert (typesSentFromBrowser.isSerializable(serializableClass) || typesSentToBrowser.isSerializable(serializableClass));

    logger = logger.branch(TreeLogger.DEBUG,
        "Generating a field serializer for type '"
            + serializableClass.getQualifiedSourceName() + "'", null);

    String fieldSerializerName = SerializationUtils.getFieldSerializerName(
        typeOracle, serializableClass);

    sourceWriter = getSourceWriter(logger, ctx);
    if (sourceWriter == null) {
      return fieldSerializerName;
    }
    assert sourceWriter != null;

    writeFieldAccessors();

    writeDeserializeMethod();

    maybeWriteInstatiateMethod();

    writeSerializeMethod();

    sourceWriter.commit(logger);

    return fieldSerializerName;
  }

  private String createArrayInstantiationExpression(JArrayType array) {
    StringBuilder sb = new StringBuilder();

    sb.append("new ");
    sb.append(array.getLeafType().getQualifiedSourceName());
    sb.append("[rank]");
    for (int i = 0; i < array.getRank() - 1; ++i) {
      sb.append("[]");
    }

    return sb.toString();
  }

  private SourceWriter getSourceWriter(TreeLogger logger, GeneratorContext ctx) {
    String qualifiedSerializerName = SerializationUtils.getFieldSerializerName(
        typeOracle, serializableClass);
    int packageNameEnd = qualifiedSerializerName.lastIndexOf('.');
    String className;
    String packageName;
    if (packageNameEnd != -1) {
      className = qualifiedSerializerName.substring(packageNameEnd + 1);
      packageName = qualifiedSerializerName.substring(0, packageNameEnd);
    } else {
      className = qualifiedSerializerName;
      packageName = "";
    }

    PrintWriter printWriter = ctx.tryCreate(logger, packageName, className);
    if (printWriter == null) {
      return null;
    }

    ClassSourceFileComposerFactory composerFactory = new ClassSourceFileComposerFactory(
        packageName, className);

    return composerFactory.createSourceWriter(ctx, printWriter);
  }

  private void maybeSuppressLongWarnings(JType fieldType) {
    if (fieldType == JPrimitiveType.LONG) {
      /**
       * Accessing long from JSNI causes a error, but field serializers need to
       * be able to do just that in order to bypass java accessibility
       * restrictions.
       */
      sourceWriter.println("@" + UnsafeNativeLong.class.getName());
    }
  }

  private void maybeWriteInstatiateMethod() {
    if (serializableClass.isEnum() == null
        && (serializableClass.isAbstract() || !serializableClass.isDefaultInstantiable())) {
      /*
       * Field serializers are shared by all of the RemoteService proxies in a
       * compilation. Therefore, we have to generate an instantiate method even
       * if the type is not instantiable relative to the RemoteService which
       * caused this field serializer to be created. If the type is not
       * instantiable relative to any of the RemoteService proxies, dead code
       * optimizations will cause the method to be removed from the compiled
       * output.
       * 
       * Enumerated types require an instantiate method even if they are
       * abstract. You will have an abstract enum in cases where the enum type
       * is sub-classed. Non-default instantiable classes cannot have
       * instantiate methods.
       */
      return;
    }

    JArrayType isArray = serializableClass.isArray();
    JEnumType isEnum = serializableClass.isEnum();
    boolean isNative = (isArray == null) && (isEnum == null);

    sourceWriter.print("public static" + (isNative ? " native " : " "));
    String qualifiedSourceName = serializableClass.getQualifiedSourceName();
    sourceWriter.print(qualifiedSourceName);
    sourceWriter.print(" instantiate(");
    sourceWriter.print(SerializationStreamReader.class.getName());
    sourceWriter.println(" streamReader) throws "
        + SerializationException.class.getName() + (isNative ? "/*-{" : "{"));
    sourceWriter.indent();

    if (isArray != null) {
      sourceWriter.println("int rank = streamReader.readInt();");
      sourceWriter.println("return "
          + createArrayInstantiationExpression(isArray) + ";");
    } else if (isEnum != null) {
      sourceWriter.println("int ordinal = streamReader.readInt();");
      sourceWriter.println(qualifiedSourceName + "[] values = "
          + qualifiedSourceName + ".values();");
      sourceWriter.println("assert (ordinal >= 0 && ordinal < values.length);");
      sourceWriter.println("return values[ordinal];");
    } else {
      sourceWriter.println("return @" + qualifiedSourceName + "::new()();");
    }

    sourceWriter.outdent();
    sourceWriter.println(isNative ? "}-*/;" : "}");
    sourceWriter.println();
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

  private void writeArrayDeserializationStatements(JArrayType isArray) {
    JType componentType = isArray.getComponentType();
    String readMethodName = Shared.getStreamReadMethodNameFor(componentType);

    if ("readObject".equals(readMethodName)) {
      // Optimize and use the default object custom serializer...
      sourceWriter.println(Object_Array_CustomFieldSerializer.class.getName()
          + ".deserialize(streamReader, instance);");
    } else {
      sourceWriter.println("for (int i = 0, n = instance.length; i < n; ++i) {");
      sourceWriter.indent();
      sourceWriter.print("instance[i] = streamReader.");
      sourceWriter.println(readMethodName + "();");
      sourceWriter.outdent();
      sourceWriter.println("}");
    }
  }

  private void writeArraySerializationStatements(JArrayType isArray) {
    JType componentType = isArray.getComponentType();
    String writeMethodName = Shared.getStreamWriteMethodNameFor(componentType);
    if ("writeObject".equals(writeMethodName)) {
      // Optimize and use the default object custom serializer...
      sourceWriter.println(Object_Array_CustomFieldSerializer.class.getName()
          + ".serialize(streamWriter, instance);");
    } else {
      sourceWriter.println("streamWriter.writeInt(instance.length);");
      sourceWriter.println();
      sourceWriter.println("for (int i = 0, n = instance.length; i < n; ++i) {");
      sourceWriter.indent();
      sourceWriter.print("streamWriter.");
      sourceWriter.print(writeMethodName);
      sourceWriter.println("(instance[i]);");
      sourceWriter.outdent();
      sourceWriter.println("}");
    }
  }

  private void writeClassDeserializationStatements() {
    /**
     * If the type is capable of making a round trip between the client and
     * server, store additional server-only field data using {@link WeakMapping}.
     */
    if (typesSentToBrowser.maybeEnhanced(serializableClass)
        && typesSentFromBrowser.maybeEnhanced(serializableClass)) {
      sourceWriter.println(WEAK_MAPPING_CLASS_NAME + ".set(instance, "
          + "\"server-enhanced-data\", streamReader.readString());");
    }
    
    for (JField serializableField : serializableFields) {
      JType fieldType = serializableField.getType();

      String readMethodName = Shared.getStreamReadMethodNameFor(fieldType);
      String streamReadExpression = "streamReader." + readMethodName + "()";
      if (Shared.typeNeedsCast(fieldType)) {
        streamReadExpression = "(" + fieldType.getQualifiedSourceName() + ") "
            + streamReadExpression;
      }

      if (needsAccessorMethods(serializableField)) {
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

    sourceWriter.println();

    JClassType superClass = serializableClass.getSuperclass();
    if (superClass != null
        && (typesSentFromBrowser.isSerializable(superClass) || typesSentToBrowser.isSerializable(superClass))) {
      String fieldSerializerName = SerializationUtils.getFieldSerializerName(
          typeOracle, superClass);
      sourceWriter.print(fieldSerializerName);
      sourceWriter.println(".deserialize(streamReader, instance);");
    }
  }

  private void writeClassSerializationStatements() {
    /**
     * If the type is capable of making a round trip between the client and
     * server, retrieve the additional server-only field data from {@link WeakMapping}.
     */
    
    if (typesSentToBrowser.maybeEnhanced(serializableClass)
        && typesSentFromBrowser.maybeEnhanced(serializableClass)) {
      sourceWriter.println("streamWriter.writeString((String) "
          + WEAK_MAPPING_CLASS_NAME + ".get(instance, \"server-enhanced-data\"));");
    }
    
    for (JField serializableField : serializableFields) {
      JType fieldType = serializableField.getType();

      String writeMethodName = Shared.getStreamWriteMethodNameFor(fieldType);
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

    sourceWriter.println();

    JClassType superClass = serializableClass.getSuperclass();
    if (superClass != null
        && (typesSentFromBrowser.isSerializable(superClass) || typesSentToBrowser.isSerializable(superClass))) {
      String fieldSerializerName = SerializationUtils.getFieldSerializerName(
          typeOracle, superClass);
      sourceWriter.print(fieldSerializerName);
      sourceWriter.println(".serialize(streamWriter, instance);");
    }
  }

  private void writeDeserializeMethod() {
    sourceWriter.print("public static void deserialize(");
    sourceWriter.print(SerializationStreamReader.class.getName());
    sourceWriter.print(" streamReader, ");
    sourceWriter.print(serializableClass.getQualifiedSourceName());
    sourceWriter.println(" instance) throws "
        + SerializationException.class.getName() + "{");
    sourceWriter.indent();

    JArrayType isArray = serializableClass.isArray();
    if (isArray != null) {
      writeArrayDeserializationStatements(isArray);
    } else if (serializableClass.isEnum() != null) {
      writeEnumDeserializationStatements();
    } else {
      writeClassDeserializationStatements();
    }
      
    sourceWriter.outdent();
    sourceWriter.println("}");
    sourceWriter.println();
  }

  private void writeEnumDeserializationStatements() {
    sourceWriter.println("// Enum deserialization is handled via the instantiate method");
  }

  private void writeEnumSerializationStatements() {
    sourceWriter.println("assert (instance != null);");
    sourceWriter.println("streamWriter.writeInt(instance.ordinal());");
  }

  /**
   * This method will generate a native JSNI accessor method for every field
   * that is protected, private using the "Violator" pattern to allow an
   * external class to access the field's value.
   */
  private void writeFieldAccessors() {
    for (JField serializableField : serializableFields) {
      if (!needsAccessorMethods(serializableField)) {
        continue;
      }

      writeFieldGet(serializableField);
      writeFieldSet(serializableField);
    }
  }

  /**
   * Write a getter method for an instance field.
   */
  private void writeFieldGet(JField serializableField) {
    JType fieldType = serializableField.getType();
    String fieldTypeQualifiedSourceName = fieldType.getQualifiedSourceName();
    String fieldName = serializableField.getName();

    maybeSuppressLongWarnings(fieldType);
    sourceWriter.print("private static native ");
    sourceWriter.print(fieldTypeQualifiedSourceName);
    sourceWriter.print(" get");
    sourceWriter.print(Shared.capitalize(fieldName));
    sourceWriter.print("(");
    sourceWriter.print(serializableClass.getQualifiedSourceName());
    sourceWriter.println(" instance) /*-{");
    sourceWriter.indent();

    sourceWriter.print("return instance.@");
    sourceWriter.print(TypeOracleMediator.computeBinaryClassName(serializableClass));
    sourceWriter.print("::");
    sourceWriter.print(fieldName);
    sourceWriter.println(";");

    sourceWriter.outdent();
    sourceWriter.println("}-*/;");
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

    maybeSuppressLongWarnings(fieldType);
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
    sourceWriter.print(TypeOracleMediator.computeBinaryClassName(serializableClass));
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

    JArrayType isArray = serializableClass.isArray();
    if (isArray != null) {
      writeArraySerializationStatements(isArray);
    } else if (serializableClass.isEnum() != null) {
      writeEnumSerializationStatements();
    } else {
      writeClassSerializationStatements();
    }

    sourceWriter.outdent();
    sourceWriter.println("}");
    sourceWriter.println();
  }

}
