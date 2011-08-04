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
import com.google.gwt.core.ext.typeinfo.JConstructor;
import com.google.gwt.core.ext.typeinfo.JEnumType;
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.JTypeParameter;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import com.google.gwt.user.client.rpc.core.java.lang.Object_Array_CustomFieldSerializer;
import com.google.gwt.user.client.rpc.impl.ReflectionHelper;
import com.google.gwt.user.client.rpc.impl.TypeHandler;
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

  /*
   * NB: FieldSerializerCreator generates two different sets of code for DevMode
   * and ProdMode. In ProdMode, the generated code uses the JSNI violator
   * pattern to access private class members. In DevMode, the generated code
   * uses ReflectionHelper instead of JSNI to avoid the many JSNI
   * boundary-crossings which are slow in DevMode.
   */

  private static final String WEAK_MAPPING_CLASS_NAME = WeakMapping.class.getName();

  private final GeneratorContext context;

  private final JClassType customFieldSerializer;

  private final boolean customFieldSerializerHasInstantiate;

  private final String fieldSerializerName;

  private final boolean isJRE;

  private final boolean isProd;

  private final String methodEnd;

  private final String methodStart;

  private final JClassType serializableClass;

  private final JField[] serializableFields;

  private SourceWriter sourceWriter;

  private final SerializableTypeOracle typesSentFromBrowser;

  private final SerializableTypeOracle typesSentToBrowser;

  private final TypeOracle typeOracle;

  /**
   * Constructs a field serializer for the class.
   */
  public FieldSerializerCreator(GeneratorContext context,
      SerializableTypeOracle typesSentFromBrowser, SerializableTypeOracle typesSentToBrowser,
      JClassType requestedClass, JClassType customFieldSerializer) {
    this.context = context;
    this.isProd = context.isProdMode();
    methodStart = isProd ? "/*-{" : "{";
    methodEnd = isProd ? "}-*/;" : "}";
    this.customFieldSerializer = customFieldSerializer;
    assert (requestedClass != null);
    assert (requestedClass.isClass() != null || requestedClass.isArray() != null);

    this.typeOracle = context.getTypeOracle();
    this.typesSentFromBrowser = typesSentFromBrowser;
    this.typesSentToBrowser = typesSentToBrowser;
    serializableClass = requestedClass;
    serializableFields = SerializationUtils.getSerializableFields(typeOracle, requestedClass);
    this.fieldSerializerName = SerializationUtils.getStandardSerializerName(serializableClass);
    this.isJRE =
        SerializableTypeOracleBuilder.isInStandardJavaPackage(serializableClass
            .getQualifiedSourceName());
    this.customFieldSerializerHasInstantiate =
        (customFieldSerializer != null && CustomFieldSerializerValidator.hasInstantiationMethod(
            customFieldSerializer, serializableClass));
  }

  public String realize(TreeLogger logger, GeneratorContext ctx) {
    assert (ctx != null);
    assert (typesSentFromBrowser.isSerializable(serializableClass) || typesSentToBrowser
        .isSerializable(serializableClass));

    logger =
        logger.branch(TreeLogger.DEBUG, "Generating a field serializer for type '"
            + serializableClass.getQualifiedSourceName() + "'", null);

    sourceWriter = getSourceWriter(logger, ctx);
    if (sourceWriter == null) {
      return fieldSerializerName;
    }
    assert sourceWriter != null;

    writeFieldAccessors();

    writeDeserializeMethod();

    maybeWriteInstatiateMethod();

    writeSerializeMethod();

    maybeWriteTypeHandlerImpl();

    sourceWriter.commit(logger);

    return fieldSerializerName;
  }

  private boolean classIsAccessible() {
    JClassType testClass = serializableClass;
    while (testClass != null) {
      if (testClass.isPrivate() || (isJRE && !testClass.isPublic())) {
        return false;
      }
      testClass = testClass.getEnclosingType();
    }
    return true;
  }

  private String createArrayInstantiationExpression(JArrayType array) {
    StringBuilder sb = new StringBuilder();

    sb.append("new ");
    sb.append(array.getLeafType().getQualifiedSourceName());
    sb.append("[size]");
    for (int i = 0; i < array.getRank() - 1; ++i) {
      sb.append("[]");
    }

    return sb.toString();
  }

  private boolean ctorIsAccessible() {
    JConstructor ctor = serializableClass.findConstructor(new JType[0]);
    if (ctor.isPrivate() || (isJRE && !ctor.isPublic())) {
      return false;
    }
    return true;
  }

  /**
   * Returns the depth of the given class in the class hierarchy (where the
   * depth of java.lang.Object == 0).
   */
  private int getDepth(JClassType clazz) {
    int depth = 0;
    while ((clazz = clazz.getSuperclass()) != null) {
      depth++;
    }
    return depth;
  }

  private SourceWriter getSourceWriter(TreeLogger logger, GeneratorContext ctx) {
    int packageNameEnd = fieldSerializerName.lastIndexOf('.');
    String className;
    String packageName;
    if (packageNameEnd != -1) {
      className = fieldSerializerName.substring(packageNameEnd + 1);
      packageName = fieldSerializerName.substring(0, packageNameEnd);
    } else {
      className = fieldSerializerName;
      packageName = "";
    }

    PrintWriter printWriter = ctx.tryCreate(logger, packageName, className);
    if (printWriter == null) {
      return null;
    }

    ClassSourceFileComposerFactory composerFactory =
        new ClassSourceFileComposerFactory(packageName, className);
    composerFactory.addImport(SerializationException.class.getCanonicalName());
    composerFactory.addImport(SerializationStreamReader.class.getCanonicalName());
    composerFactory.addImport(SerializationStreamWriter.class.getCanonicalName());
    composerFactory.addImport(ReflectionHelper.class.getCanonicalName());
    composerFactory.addAnnotationDeclaration("@SuppressWarnings(\"deprecation\")");
    if (needsTypeHandler()) {
      composerFactory.addImplementedInterface(TypeHandler.class.getCanonicalName());
    }
    return composerFactory.createSourceWriter(ctx, printWriter);
  }

  private String getTypeSig(JMethod deserializationMethod) {
    JTypeParameter[] typeParameters = deserializationMethod.getTypeParameters();
    String typeSig = "";
    if (typeParameters.length > 0) {
      StringBuilder sb = new StringBuilder();
      sb.append('<');
      for (JTypeParameter typeParameter : typeParameters) {
        sb.append(typeParameter.getFirstBound().getQualifiedSourceName());
        sb.append(',');
      }
      sb.setCharAt(sb.length() - 1, '>');
      typeSig = sb.toString();
    }
    return typeSig;
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

  /**
   * Writes an instantiate method. Examples:
   * 
   * <h2>Class</h2>
   * 
   * <pre>
   * public static com.google.gwt.sample.client.Student instantiate(
   *     SerializationStreamReader streamReader) throws SerializationException {
   *   return new com.google.gwt.sample.client.Student();
   * }
   * </pre>
   * 
   * <h2>Class with private ctor</h2>
   * 
   * <pre>
   * public static native com.google.gwt.sample.client.Student instantiate(
   *     SerializationStreamReader streamReader) throws SerializationException /*-{
   * return @com.google.gwt.sample.client.Student::new()();
   * }-&#42;/;
   * </pre>
   * 
   * <h2>Array</h2>
   * 
   * <pre>
   * public static com.google.gwt.sample.client.Student[] instantiate(
   *     SerializationStreamReader streamReader) throws SerializationException {
   *   int size = streamReader.readInt();
   *   return new com.google.gwt.sample.client.Student[size];
   * }
   * </pre>
   * 
   * <h2>Enum</h2>
   * 
   * <pre>
   * public static com.google.gwt.sample.client.Role instantiate(
   *     SerializationStreamReader streamReader) throws SerializationException {
   *   int ordinal = streamReader.readInt();
   *   com.google.gwt.sample.client.Role[] values = com.google.gwt.sample.client.Role.values();
   *   assert (ordinal &gt;= 0 &amp;&amp; ordinal &lt; values.length);
   *   return values[ordinal];
   * }
   * </pre>
   */
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

    if (customFieldSerializerHasInstantiate) {
      // The custom field serializer already defined it.
      return;
    }

    JArrayType isArray = serializableClass.isArray();
    JEnumType isEnum = serializableClass.isEnum();
    JClassType isClass = serializableClass.isClass();

    boolean useViolator = false;
    boolean isAccessible = true;
    if (isEnum == null && isClass != null) {
      isAccessible = classIsAccessible() && ctorIsAccessible();
      useViolator = !isAccessible && isProd;
    }

    sourceWriter.print("public static" + (useViolator ? " native " : " "));
    String qualifiedSourceName = serializableClass.getQualifiedSourceName();
    sourceWriter.print(qualifiedSourceName);
    sourceWriter
        .println(" instantiate(SerializationStreamReader streamReader) throws SerializationException "
            + (useViolator ? "/*-{" : "{"));
    sourceWriter.indent();

    if (isArray != null) {
      sourceWriter.println("int size = streamReader.readInt();");
      sourceWriter.println("return " + createArrayInstantiationExpression(isArray) + ";");
    } else if (isEnum != null) {
      sourceWriter.println("int ordinal = streamReader.readInt();");
      sourceWriter.println(qualifiedSourceName + "[] values = " + qualifiedSourceName
          + ".values();");
      sourceWriter.println("assert (ordinal >= 0 && ordinal < values.length);");
      sourceWriter.println("return values[ordinal];");
    } else if (!isAccessible) {
      if (isProd) {
        sourceWriter.println("return @" + qualifiedSourceName + "::new()();");
      } else {
        sourceWriter.println("return ReflectionHelper.newInstance(" + qualifiedSourceName
            + ".class);");
      }
    } else {
      sourceWriter.println("return new " + qualifiedSourceName + "();");
    }

    sourceWriter.outdent();
    sourceWriter.println(useViolator ? "}-*/;" : "}");
    sourceWriter.println();
  }

  /**
   * Implement {@link TypeHandler} for the class, used by Java.
   * 
   * <pre>
   * public void deserial(SerializationStreamReader reader, Object object)
   *     throws SerializationException {
   *   com.google.gwt.sample.client.Student_FieldSerializer.deserialize(
   *       reader, (com.google.gwt.sample.client.Student) object);
   * }
   * 
   * public Object create(SerializationStreamReader reader)
   *     throws SerializationException {
   *   return com.google.gwt.sample.client.Student_FieldSerializer.instantiate(reader);
   * }
   * 
   * public void serial(SerializationStreamWriter writer, Object object)
   *     throws SerializationException {
   *   com.google.gwt.sample.client.Student_FieldSerializer.serialize(
   *       writer, (com.google.gwt.sample.client.Student) object);
   * }
   * </pre>
   */
  private void maybeWriteTypeHandlerImpl() {
    if (!needsTypeHandler()) {
      return;
    }

    // Create method
    sourceWriter
        .println("public Object create(SerializationStreamReader reader) throws SerializationException {");
    sourceWriter.indent();
    if (serializableClass.isEnum() != null || serializableClass.isDefaultInstantiable()
        || customFieldSerializerHasInstantiate) {
      sourceWriter.print("return ");
      String typeSig;
      if (customFieldSerializer != null && customFieldSerializerHasInstantiate) {
        sourceWriter.print(customFieldSerializer.getQualifiedSourceName());
        JMethod instantiationMethod =
            CustomFieldSerializerValidator.getInstantiationMethod(customFieldSerializer,
                serializableClass);
        typeSig = getTypeSig(instantiationMethod);
      } else {
        sourceWriter.print(fieldSerializerName);
        typeSig = "";
      }
      sourceWriter.print("." + typeSig + "instantiate");
      sourceWriter.println("(reader);");
    } else {
      sourceWriter.println("return null;");
    }
    sourceWriter.outdent();
    sourceWriter.println("}");
    sourceWriter.println();

    // Deserial method
    sourceWriter
        .println("public void deserial(SerializationStreamReader reader, Object object) throws SerializationException {");
    if (customFieldSerializer != null) {
      JMethod deserializationMethod =
          CustomFieldSerializerValidator.getDeserializationMethod(customFieldSerializer,
              serializableClass);
      JType castType = deserializationMethod.getParameters()[1].getType();
      String typeSig = getTypeSig(deserializationMethod);
      sourceWriter.indentln(customFieldSerializer.getQualifiedSourceName() + "." + typeSig
          + "deserialize(reader, (" + castType.getQualifiedSourceName() + ")object);");
    } else {
      sourceWriter.indentln(fieldSerializerName + ".deserialize(reader, ("
          + serializableClass.getQualifiedSourceName() + ")object);");
    }
    sourceWriter.println("}");
    sourceWriter.println();

    // Serial method
    sourceWriter
        .println("public void serial(SerializationStreamWriter writer, Object object) throws SerializationException {");
    if (customFieldSerializer != null) {
      JMethod serializationMethod =
          CustomFieldSerializerValidator.getSerializationMethod(customFieldSerializer,
              serializableClass);
      JType castType = serializationMethod.getParameters()[1].getType();
      String typeSig = getTypeSig(serializationMethod);
      sourceWriter.indentln(customFieldSerializer.getQualifiedSourceName() + "." + typeSig
          + "serialize(writer, (" + castType.getQualifiedSourceName() + ")object);");
    } else {
      sourceWriter.indentln(fieldSerializerName + ".serialize(writer, ("
          + serializableClass.getQualifiedSourceName() + ")object);");
    }

    sourceWriter.println("}");
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

  /**
   * Enumerated types can be instantiated even if they are abstract. You will
   * have an abstract enum in cases where the enum type is sub-classed.
   * Non-default instantiable classes cannot have instantiate methods.
   */
  private boolean needsTypeHandler() {
    return serializableClass.isEnum() != null || !serializableClass.isAbstract();
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
     * server, store additional server-only field data using {@link WeakMapping}
     * .
     */
    if (serializableClass.isEnhanced()) {
      sourceWriter.println(WEAK_MAPPING_CLASS_NAME + ".set(instance, " + "\"server-enhanced-data-"
          + getDepth(serializableClass) + "\", streamReader.readString());");
    }

    for (JField serializableField : serializableFields) {
      JType fieldType = serializableField.getType();

      String readMethodName = Shared.getStreamReadMethodNameFor(fieldType);
      String streamReadExpression = "streamReader." + readMethodName + "()";
      if (Shared.typeNeedsCast(fieldType)) {
        streamReadExpression =
            "(" + fieldType.getQualifiedSourceName() + ") " + streamReadExpression;
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
        && (typesSentFromBrowser.isSerializable(superClass) || typesSentToBrowser
            .isSerializable(superClass))) {
      String superFieldSerializer =
          SerializationUtils.getFieldSerializerName(typeOracle, superClass);
      sourceWriter.print(superFieldSerializer);
      sourceWriter.println(".deserialize(streamReader, instance);");
    }
  }

  private void writeClassSerializationStatements() {
    /**
     * If the type is capable of making a round trip between the client and
     * server, retrieve the additional server-only field data from
     * {@link WeakMapping}.
     */

    if (serializableClass.isEnhanced()) {
      sourceWriter.println("streamWriter.writeString((String) " + WEAK_MAPPING_CLASS_NAME
          + ".get(instance, \"server-enhanced-data-" + getDepth(serializableClass) + "\"));");
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
        && (typesSentFromBrowser.isSerializable(superClass) || typesSentToBrowser
            .isSerializable(superClass))) {
      String superFieldSerializer =
          SerializationUtils.getFieldSerializerName(typeOracle, superClass);
      sourceWriter.print(superFieldSerializer);
      sourceWriter.println(".serialize(streamWriter, instance);");
    }
  }

  private void writeDeserializeMethod() {
    if (customFieldSerializer != null) {
      return;
    }
    sourceWriter.print("public static void deserialize(SerializationStreamReader streamReader, ");
    sourceWriter.print(serializableClass.getQualifiedSourceName());
    sourceWriter.println(" instance) throws SerializationException {");
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
    if (customFieldSerializer != null) {
      return;
    }
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
    String serializableClassQualifedName = serializableClass.getQualifiedSourceName();
    String fieldName = serializableField.getName();

    maybeSuppressLongWarnings(fieldType);
    sourceWriter.print("private static " + (isProd ? "native " : ""));
    sourceWriter.print(fieldTypeQualifiedSourceName);
    sourceWriter.print(" get");
    sourceWriter.print(Shared.capitalize(fieldName));
    sourceWriter.print("(");
    sourceWriter.print(serializableClassQualifedName);
    sourceWriter.print(" instance) ");
    sourceWriter.println(methodStart);

    sourceWriter.indent();

    if (context.isProdMode()) {
      sourceWriter.print("return instance.@");
      sourceWriter.print(SerializationUtils.getRpcTypeName(serializableClass));
      sourceWriter.print("::");
      sourceWriter.print(fieldName);
      sourceWriter.println(";");
    } else {
      sourceWriter.print("return ");
      JPrimitiveType primType = fieldType.isPrimitive();
      if (primType != null) {
        sourceWriter.print("(" + primType.getQualifiedBoxedSourceName() + ") ");
      } else {
        sourceWriter.print("(" + fieldTypeQualifiedSourceName + ") ");
      }
      sourceWriter.println("ReflectionHelper.getField(" + serializableClassQualifedName
          + ".class, instance, \"" + fieldName + "\");");
    }

    sourceWriter.outdent();
    sourceWriter.println(methodEnd);
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
    sourceWriter.print("private static " + (isProd ? "native " : "") + "void");
    sourceWriter.print(" set");
    sourceWriter.print(Shared.capitalize(fieldName));
    sourceWriter.print("(");
    sourceWriter.print(serializableClassQualifedName);
    sourceWriter.print(" instance, ");
    sourceWriter.print(fieldTypeQualifiedSourceName);
    sourceWriter.println(" value) ");
    sourceWriter.println(methodStart);

    sourceWriter.indent();

    if (context.isProdMode()) {
      sourceWriter.print("instance.@");
      sourceWriter.print(SerializationUtils.getRpcTypeName(serializableClass));
      sourceWriter.print("::");
      sourceWriter.print(fieldName);
      sourceWriter.println(" = value;");
    } else {
      sourceWriter.println("ReflectionHelper.setField(" + serializableClassQualifedName
          + ".class, instance, \"" + fieldName + "\", value);");
    }

    sourceWriter.outdent();
    sourceWriter.println(methodEnd);
    sourceWriter.println();
  }

  private void writeSerializeMethod() {
    if (customFieldSerializer != null) {
      return;
    }
    sourceWriter.print("public static void serialize(SerializationStreamWriter streamWriter, ");
    sourceWriter.print(serializableClass.getQualifiedSourceName());
    sourceWriter.println(" instance) throws SerializationException {");
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
