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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.javac.TypeOracleMediator;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import com.google.gwt.user.client.rpc.impl.Serializer;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class generates a class with name 'typeSerializerClassName' that is able
 * to serialize and deserialize a set of types into or out of a stream. The set
 * of types is obtained from the SerializableTypeOracle object.
 */
public class TypeSerializerCreator {

  /**
   * Default number of types to split createMethodMap entries into. Zero means
   * no sharding occurs. Stored as a string since it is used as a default
   * property value.
   * 
   * Note that the inliner will likely reassemble the shards if it is used in
   * web mode, but it isn't needed there anyway.
   * 
   * TODO: remove this (and related code) when it is no longer needed.
   */
  private static final String DEFAULT_CREATEMETHODMAP_SHARD_SIZE = "0";

  private static final String DESERIALIZE_METHOD_SIGNATURE = "public native void deserialize("
      + "SerializationStreamReader streamReader, Object instance, String typeSignature)"
      + " throws SerializationException";

  /**
   * Java system property name to override the above.
   */
  private static final String GWT_CREATEMETHODMAP_SHARD_SIZE = "gwt.typecreator.shard.size";

  private static final String INSTANTIATE_METHOD_SIGNATURE = "public native Object instantiate("
      + "SerializationStreamReader streamReader, String typeSignature)"
      + " throws SerializationException";

  private static final String SERIALIZE_METHOD_SIGNATURE = "public native void serialize("
      + "SerializationStreamWriter streamWriter, Object instance, String typeSignature)"
      + " throws SerializationException";

  private static int shardSize = -1;

  private static void computeShardSize(TreeLogger logger)
      throws UnableToCompleteException {
    String shardSizeProperty = System.getProperty(
        GWT_CREATEMETHODMAP_SHARD_SIZE, DEFAULT_CREATEMETHODMAP_SHARD_SIZE);
    try {
      shardSize = Integer.valueOf(shardSizeProperty);
      if (shardSize < 0) {
        logger.log(TreeLogger.ERROR, GWT_CREATEMETHODMAP_SHARD_SIZE
            + " must be non-negative: " + shardSizeProperty);
        throw new UnableToCompleteException();
      }
    } catch (NumberFormatException e) {
      logger.log(TreeLogger.ERROR, "Property " + GWT_CREATEMETHODMAP_SHARD_SIZE
          + " not a number: " + shardSizeProperty, e);
      throw new UnableToCompleteException();
    }
  }

  private final GeneratorContext context;

  private final SerializableTypeOracle deserializationOracle;

  private final SerializableTypeOracle serializationOracle;

  private final JType[] serializableTypes;

  private final SourceWriter srcWriter;

  private final TypeOracle typeOracle;

  private final String typeSerializerClassName;

  public TypeSerializerCreator(TreeLogger logger,
      SerializableTypeOracle serializationOracle,
      SerializableTypeOracle deserializationOracle, GeneratorContext context,
      String typeSerializerClassName) throws UnableToCompleteException {
    this.context = context;
    this.typeSerializerClassName = typeSerializerClassName;
    this.serializationOracle = serializationOracle;
    this.deserializationOracle = deserializationOracle;

    this.typeOracle = context.getTypeOracle();

    Set<JType> typesSet = new HashSet<JType>();
    typesSet.addAll(Arrays.asList(serializationOracle.getSerializableTypes()));
    typesSet.addAll(Arrays.asList(deserializationOracle.getSerializableTypes()));
    serializableTypes = typesSet.toArray(new JType[0]);

    srcWriter = getSourceWriter(logger, context);
    if (shardSize < 0) {
      computeShardSize(logger);
    }
    logger.log(TreeLogger.TRACE, "Using a shard size of " + shardSize
        + " for TypeSerializerCreator createMethodMap");
  }

  public String realize(TreeLogger logger) throws UnableToCompleteException {
    logger = logger.branch(TreeLogger.DEBUG,
        "Generating TypeSerializer for service interface '"
            + getTypeSerializerClassName() + "'", null);
    String typeSerializerName = getTypeSerializerClassName();
    if (srcWriter == null) {
      return typeSerializerName;
    }

    createFieldSerializers(logger, context);

    writeStaticFields();

    writeCreateMethods();

    writeCreateMethodMapMethod(logger);

    writeCreateSignatureMapMethod();

    writeRaiseSerializationException();

    writeDeserializeMethod();

    writeGetSerializationSignatureMethod();

    writeInstantiateMethod();

    writeSerializeMethod();

    srcWriter.commit(logger);

    return typeSerializerName;
  }

  /*
   * Create a field serializer for a type if it does not have a custom
   * serializer.
   */
  private void createFieldSerializer(TreeLogger logger, GeneratorContext ctx,
      JType type) {
    assert (type != null);
    assert (serializationOracle.isSerializable(type) || deserializationOracle.isSerializable(type));

    JParameterizedType parameterizedType = type.isParameterized();
    if (parameterizedType != null) {
      createFieldSerializer(logger, ctx, parameterizedType.getRawType());
      return;
    }

    JClassType customFieldSerializer = SerializableTypeOracleBuilder.findCustomFieldSerializer(
        typeOracle, type);
    if (customFieldSerializer != null) {
      return;
    }

    /*
     * Only a JClassType can reach this point in the code. JPrimitives have been
     * removed because their serialization is built in, interfaces have been
     * removed because they are not an instantiable type and parameterized types
     * have been broken down into their raw types.
     */
    assert (type.isClass() != null || type.isArray() != null);

    FieldSerializerCreator creator = new FieldSerializerCreator(typeOracle,
        serializationOracle, deserializationOracle, (JClassType) type);
    creator.realize(logger, ctx);
  }

  /*
   * Create all of the necessary field serializers.
   */
  private void createFieldSerializers(TreeLogger logger, GeneratorContext ctx) {
    JType[] types = getSerializableTypes();
    int typeCount = types.length;
    for (int typeIndex = 0; typeIndex < typeCount; ++typeIndex) {
      JType type = types[typeIndex];
      assert (type != null);

      createFieldSerializer(logger, ctx, type);
    }
  }

  private String getCreateMethodName(JType type) {
    assert (type.isArray() == null);

    return "create_"
        + SerializationUtils.getFieldSerializerName(typeOracle, type).replace(
            '.', '_');
  }

  private String[] getPackageAndClassName(String fullClassName) {
    String className = fullClassName;
    String packageName = "";
    int index = -1;
    if ((index = className.lastIndexOf('.')) >= 0) {
      packageName = className.substring(0, index);
      className = className.substring(index + 1, className.length());
    }
    return new String[] {packageName, className};
  }

  private JType[] getSerializableTypes() {
    return serializableTypes;
  }

  private SourceWriter getSourceWriter(TreeLogger logger, GeneratorContext ctx) {
    String name[] = getPackageAndClassName(getTypeSerializerClassName());
    String packageName = name[0];
    String className = name[1];
    PrintWriter printWriter = ctx.tryCreate(logger, packageName, className);
    if (printWriter == null) {
      return null;
    }

    ClassSourceFileComposerFactory composerFactory = new ClassSourceFileComposerFactory(
        packageName, className);

    composerFactory.addImport(JavaScriptObject.class.getName());
    composerFactory.addImport(Serializer.class.getName());
    composerFactory.addImport(SerializationException.class.getName());
    composerFactory.addImport(SerializationStreamReader.class.getName());
    composerFactory.addImport(SerializationStreamWriter.class.getName());

    composerFactory.addImplementedInterface("Serializer");
    return composerFactory.createSourceWriter(ctx, printWriter);
  }

  private String getTypeSerializerClassName() {
    return typeSerializerClassName;
  }

  /**
   * @param type
   * @return
   */
  private String getTypeString(JType type) {
    String typeString = TypeOracleMediator.computeBinaryClassName(type) + "/"
        + SerializationUtils.getSerializationSignature(typeOracle, type);
    return typeString;
  }

  /**
   * Return <code>true</code> if this type is concrete and has a custom field
   * serializer that does not declare an instantiate method.
   * 
   * @param type
   * @return
   */
  private boolean needsCreateMethod(JType type) {
    // If this type is abstract it will not be serialized into the stream
    //
    if (!deserializationOracle.maybeInstantiated(type)) {
      return false;
    }

    if (type.isArray() != null) {
      return false;
    }

    JClassType customSerializer = SerializableTypeOracleBuilder.findCustomFieldSerializer(
        typeOracle, type);
    if (customSerializer == null) {
      return false;
    }

    JMethod customInstantiate = customSerializer.findMethod(
        "instantiate",
        new JType[] {typeOracle.findType(SerializationStreamReader.class.getName())});
    if (customInstantiate != null) {
      return false;
    }

    return true;
  }

  /**
   * Generate the createMethodMap function, possibly splitting it into smaller
   * pieces if necessary to avoid an old Mozilla crash when dealing with
   * excessively large JS functions.
   * 
   * @param logger TreeLogger instance
   * @throws UnableToCompleteException if an error is logged
   */
  private void writeCreateMethodMapMethod(TreeLogger logger)
      throws UnableToCompleteException {
    ArrayList<JType> filteredTypes = new ArrayList<JType>();
    JType[] types = getSerializableTypes();
    int n = types.length;
    for (int index = 0; index < n; ++index) {
      JType type = types[index];
      if (serializationOracle.maybeInstantiated(type)
          || deserializationOracle.maybeInstantiated(type)) {
        filteredTypes.add(type);
      }
    }
    if (shardSize > 0 && filteredTypes.size() > shardSize) {
      writeShardedCreateMethodMapMethod(filteredTypes, shardSize);
    } else {
      writeSingleCreateMethodMapMethod(filteredTypes);
    }
    srcWriter.println();
  }

  private void writeCreateMethods() {
    JType[] types = getSerializableTypes();
    for (int typeIndex = 0; typeIndex < types.length; ++typeIndex) {
      JType type = types[typeIndex];
      assert (serializationOracle.isSerializable(type) || deserializationOracle.isSerializable(type));

      if (!needsCreateMethod(type)) {
        continue;
      }

      /*
       * Only classes with custom field serializers that do no declare
       * instantiate methods get here
       */
      srcWriter.print("private static native ");
      srcWriter.print(type.getQualifiedSourceName());
      srcWriter.print(" ");
      srcWriter.print(getCreateMethodName(type));
      srcWriter.println("(SerializationStreamReader streamReader) throws SerializationException /*-{");
      srcWriter.indent();
      srcWriter.print("return @");
      srcWriter.print(type.getQualifiedSourceName());
      srcWriter.println("::new()();");
      srcWriter.outdent();
      srcWriter.println("}-*/;");
      srcWriter.println();
    }
  }

  private void writeCreateSignatureMapMethod() {
    srcWriter.println("private static native JavaScriptObject createSignatureMap() /*-" + '{');
    {
      srcWriter.indent();
      srcWriter.println("return {");
      JType[] types = getSerializableTypes();
      boolean needComma = false;
      for (int index = 0; index < types.length; ++index) {
        JType type = types[index];
        if (!serializationOracle.maybeInstantiated(type)
            && !deserializationOracle.maybeInstantiated(type)) {
          continue;
        }
        if (needComma) {
          srcWriter.println(",");
        } else {
          needComma = true;
        }

        srcWriter.print("\"" + TypeOracleMediator.computeBinaryClassName(type)
            + "\":\""
            + SerializationUtils.getSerializationSignature(typeOracle, type)
            + "\"");
      }
      srcWriter.println();
      srcWriter.println("};");
      srcWriter.outdent();
    }
    srcWriter.println("}-*/;");
    srcWriter.println();
  }

  private void writeDeserializeMethod() {
    srcWriter.print(DESERIALIZE_METHOD_SIGNATURE);
    srcWriter.println(" /*-" + '{');
    {
      String serializerTypeName = getTypeSerializerClassName();
      srcWriter.indent();
      srcWriter.println("var methodTable = @" + serializerTypeName
          + "::methodMap[typeSignature];");
      srcWriter.println("if (!methodTable) {");
      srcWriter.indentln("@" + serializerTypeName
          + "::raiseSerializationException(Ljava/lang/String;)(typeSignature);");
      srcWriter.println("}");
      srcWriter.println("methodTable[1](streamReader, instance);");
      srcWriter.outdent();
    }
    srcWriter.println("}-*/;");
    srcWriter.println();
  }

  private void writeGetSerializationSignatureMethod() {
    String serializerTypeName = getTypeSerializerClassName();
    srcWriter.println("public native String getSerializationSignature(String typeName) /*-" + '{');
    srcWriter.indent();
    srcWriter.println("return @" + serializerTypeName
        + "::signatureMap[typeName];");
    srcWriter.outdent();
    srcWriter.println("}-*/;");
    srcWriter.println();
  }

  private void writeInstantiateMethod() {
    srcWriter.print(INSTANTIATE_METHOD_SIGNATURE);
    srcWriter.println(" /*-" + '{');
    {
      String serializerTypeName = getTypeSerializerClassName();
      srcWriter.indent();
      srcWriter.println("var methodTable = @" + serializerTypeName
          + "::methodMap[typeSignature];");
      srcWriter.println("if (!methodTable) {");
      srcWriter.indentln("@" + serializerTypeName
          + "::raiseSerializationException(Ljava/lang/String;)(typeSignature);");
      srcWriter.println("}");
      srcWriter.println("return methodTable[0](streamReader);");
      srcWriter.outdent();
    }
    srcWriter.println("}-*/;");
    srcWriter.println();
  }

  private void writeRaiseSerializationException() {
    srcWriter.println("private static void raiseSerializationException(String msg) throws SerializationException {");
    srcWriter.indentln("throw new SerializationException(msg);");
    srcWriter.println("}");
    srcWriter.println();
  }

  private void writeSerializeMethod() {
    srcWriter.print(SERIALIZE_METHOD_SIGNATURE);
    srcWriter.println(" /*-" + '{');
    {
      String serializerTypeName = getTypeSerializerClassName();
      srcWriter.indent();
      srcWriter.println("var methodTable = @" + serializerTypeName
          + "::methodMap[typeSignature];");
      srcWriter.println("if (!methodTable) {");
      srcWriter.indentln("@" + serializerTypeName
          + "::raiseSerializationException(Ljava/lang/String;)(typeSignature);");
      srcWriter.println("}");
      srcWriter.println("methodTable[2](streamWriter, instance);");
      srcWriter.outdent();
    }
    srcWriter.println("}-*/;");
    srcWriter.println();
  }

  /**
   * Create a createMethodMap method which is sharded into smaller methods. This
   * avoids a crash in old Mozilla dealing with very large JS functions being
   * evaluated.
   * 
   * @param types list of types to include
   * @param shardSize batch size for sharding
   */
  private void writeShardedCreateMethodMapMethod(List<JType> types,
      int shardSize) {
    srcWriter.println("private static JavaScriptObject createMethodMap() {");
    int n = types.size();
    srcWriter.indent();
    srcWriter.println("JavaScriptObject map = JavaScriptObject.createObject();");
    for (int i = 0; i < n; i += shardSize) {
      srcWriter.println("createMethodMap_" + i + "(map);");
    }
    srcWriter.println("return map;");
    srcWriter.outdent();
    srcWriter.println("}");
    srcWriter.println();
    for (int outerIndex = 0; outerIndex < n; outerIndex += shardSize) {
      srcWriter.println("@SuppressWarnings(\"restriction\")");
      srcWriter.println("private static native void createMethodMap_"
          + outerIndex + "(JavaScriptObject map) /*-" + '{');
      srcWriter.indent();
      int last = outerIndex + shardSize;
      if (last > n) {
        last = n;
      }
      for (int i = outerIndex; i < last; ++i) {
        JType type = types.get(i);
        String typeString = getTypeString(type);
        srcWriter.print("map[\"" + typeString + "\"]=[");
        writeTypeMethods(type);
        srcWriter.println("];");
      }
      srcWriter.outdent();
      srcWriter.println("}-*/;");
      srcWriter.println();
    }
  }

  private void writeSingleCreateMethodMapMethod(List<JType> types) {
    srcWriter.println("@SuppressWarnings(\"restriction\")");
    srcWriter.println("private static native JavaScriptObject createMethodMap() /*-" + '{');
    srcWriter.indent();
    srcWriter.println("return {");
    int n = types.size();
    for (int i = 0; i < n; ++i) {
      if (i > 0) {
        srcWriter.println(",");
      }
      JType type = types.get(i);
      String typeString = getTypeString(type);
      srcWriter.print("\"" + typeString + "\":[");
      writeTypeMethods(type);
      srcWriter.print("]");
    }
    srcWriter.println("};");
    srcWriter.outdent();
    srcWriter.println("}-*/;");
  }

  private void writeStaticFields() {
    srcWriter.println("private static final JavaScriptObject methodMap = createMethodMap();");
    srcWriter.println("private static final JavaScriptObject signatureMap = createSignatureMap();");
    srcWriter.println();
  }

  /**
   * Write an entry in the createMethodMap method for one type.
   * 
   * @param type type to generate entry for
   */
  private void writeTypeMethods(JType type) {
    srcWriter.indent();
    String serializerName = SerializationUtils.getFieldSerializerName(
        typeOracle, type);

    // First the initialization method
    if (deserializationOracle.maybeInstantiated(type)) {
      srcWriter.print("@");
      if (needsCreateMethod(type)) {
        srcWriter.print(getTypeSerializerClassName());
        srcWriter.print("::");
        srcWriter.print(getCreateMethodName(type));
      } else {
        srcWriter.print(serializerName);
        srcWriter.print("::instantiate");
      }
      srcWriter.print("(L"
          + SerializationStreamReader.class.getName().replace('.', '/') + ";)");
    }
    srcWriter.println(",");

    JClassType customSerializer = SerializableTypeOracleBuilder.findCustomFieldSerializer(
        typeOracle, type);

    // Now the deserialization method
    if (deserializationOracle.isSerializable(type)) {
      // Assume param type is the concrete type of the serialized type.
      JType paramType = type;
      if (customSerializer != null) {
        // But a custom serializer may specify a looser type.
        JMethod deserializationMethod = CustomFieldSerializerValidator.getDeserializationMethod(
            customSerializer, (JClassType) type);
        paramType = deserializationMethod.getParameters()[1].getType();
      }
      srcWriter.print("@" + serializerName);
      srcWriter.print("::deserialize(L"
          + SerializationStreamReader.class.getName().replace('.', '/') + ";"
          + paramType.getJNISignature() + ")");
    }
    srcWriter.println(",");

    // Now the serialization method
    if (serializationOracle.isSerializable(type)) {
      // Assume param type is the concrete type of the serialized type.
      JType paramType = type;
      if (customSerializer != null) {
        // But a custom serializer may specify a looser type.
        JMethod serializationMethod = CustomFieldSerializerValidator.getSerializationMethod(
            customSerializer, (JClassType) type);
        paramType = serializationMethod.getParameters()[1].getType();
      }
      srcWriter.print("@" + serializerName);
      srcWriter.print("::serialize(L"
          + SerializationStreamWriter.class.getName().replace('.', '/') + ";"
          + paramType.getJNISignature() + ")");
      srcWriter.println();
    }
    srcWriter.outdent();
  }
}
