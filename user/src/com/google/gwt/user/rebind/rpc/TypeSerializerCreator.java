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
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.core.ext.BadPropertyValueException;
import com.google.gwt.core.ext.ConfigurationProperty;
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
import com.google.gwt.user.client.rpc.impl.SerializerBase;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class generates a class with name 'typeSerializerClassName' that is able
 * to serialize and deserialize a set of types into or out of a stream. The set
 * of types is obtained from the SerializableTypeOracle object.
 */
public class TypeSerializerCreator {

  /**
   * Configuration property to use type indices instead of type signatures.
   */
  public static final String GWT_ELIDE_TYPE_NAMES_FROM_RPC = "gwt.elideTypeNamesFromRPC";

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

  /**
   * Java system property name to override the above.
   */
  private static final String GWT_CREATEMETHODMAP_SHARD_SIZE = "gwt.typecreator.shard.size";

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

  private final boolean elideTypeNames;

  private final SerializableTypeOracle serializationOracle;

  private final JType[] serializableTypes;

  private final SourceWriter srcWriter;

  private final TypeOracle typeOracle;

  private final String typeSerializerClassName;

  private final Map<JType, String> typeStrings = new IdentityHashMap<JType, String>();

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
    Arrays.sort(serializableTypes,
        SerializableTypeOracleBuilder.JTYPE_COMPARATOR);

    srcWriter = getSourceWriter(logger, context);
    if (shardSize < 0) {
      computeShardSize(logger);
    }
    logger.log(TreeLogger.TRACE, "Using a shard size of " + shardSize
        + " for TypeSerializerCreator createMethodMap");

    try {
      ConfigurationProperty prop
          = context.getPropertyOracle().getConfigurationProperty(
              GWT_ELIDE_TYPE_NAMES_FROM_RPC);
      elideTypeNames = Boolean.parseBoolean(prop.getValues().get(0));
    } catch (BadPropertyValueException e) {
      logger.log(TreeLogger.ERROR, "The configuration property "
          + GWT_ELIDE_TYPE_NAMES_FROM_RPC
          + " was not defined. Is RemoteService.gwt.xml inherited?");
      throw new UnableToCompleteException();
    }
  }

  public Map<JType, String> getTypeStrings() {
    return Collections.unmodifiableMap(typeStrings);
  }

  public String realize(TreeLogger logger) {
    logger = logger.branch(TreeLogger.DEBUG,
        "Generating TypeSerializer for service interface '"
            + getTypeSerializerClassName() + "'", null);
    String typeSerializerName = getTypeSerializerClassName();
    if (srcWriter == null) {
      return typeSerializerName;
    }

    createFieldSerializers(logger, context);

    writeStaticFields();

    writeStaticInitializer();

    writeCreateMethods();

    writeRegisterSignatures();

    writeRegisterMethods();

    writeRaiseSerializationException();

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
    composerFactory.addImport(JsArrayString.class.getName());
    composerFactory.addImport(Serializer.class.getName());
    composerFactory.addImport(SerializationException.class.getName());
    composerFactory.addImport(SerializationStreamReader.class.getName());
    composerFactory.addImport(SerializationStreamWriter.class.getName());

    composerFactory.setSuperclass(SerializerBase.class.getName());
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

  private void writeRaiseSerializationException() {
    srcWriter.println("private static void raiseSerializationException(String msg) throws SerializationException {");
    srcWriter.indentln("throw new SerializationException(msg);");
    srcWriter.println("}");
    srcWriter.println();
  }

  private void writeRegisterMethods() {
    srcWriter.println("private static native void registerMethods() /*-{");
    srcWriter.indent();

    List<JType> filteredTypes = new ArrayList<JType>();
    JType[] types = getSerializableTypes();
    int n = types.length;
    for (int index = 0; index < n; ++index) {
      JType type = types[index];
      if (serializationOracle.maybeInstantiated(type)
          || deserializationOracle.maybeInstantiated(type)) {
        filteredTypes.add(type);
      }
    }

    boolean shard = shardSize > 0 && filteredTypes.size() > shardSize;
    int shardCount = 0;

    if (shard) {
      srcWriter.println("(function() {");
    }

    for (JType type : filteredTypes) {
      if (shard && ++shardCount % shardSize == 0) {
        srcWriter.println("})();");
        srcWriter.println("(function() {");
      }

      srcWriter.println("@com.google.gwt.user.client.rpc.impl.SerializerBase"
          + "::registerMethods("
          + "Lcom/google/gwt/user/client/rpc/impl/SerializerBase$MethodMap;"
          + "Ljava/lang/String;" + "Lcom/google/gwt/core/client/JsArray;)(");

      srcWriter.indentln("@" + typeSerializerClassName + "::methodMap,");

      String typeString = typeStrings.get(type);
      assert typeString != null : "Missing type signature for "
          + type.getQualifiedSourceName();
      srcWriter.indentln("\"" + typeString + "\" , [");

      srcWriter.indent();
      writeTypeMethods(type);
      srcWriter.outdent();

      srcWriter.indentln("]);");
      srcWriter.println();
    }

    if (shard) {
      srcWriter.println("})();");
    }

    srcWriter.outdent();
    srcWriter.println("}-*/;");
    srcWriter.println();
  }

  private void writeRegisterSignatures() {
    srcWriter.println("private static native void registerSignatures() /*-{");
    srcWriter.indent();

    int index = 0;
    boolean shard = shardSize > 0 && getSerializableTypes().length > shardSize;
    int shardCount = 0;

    if (shard) {
      srcWriter.println("(function() {");
    }

    for (JType type : getSerializableTypes()) {

      String typeString;
      if (elideTypeNames) {
        typeString = Integer.toString(++index, Character.MAX_RADIX);
      } else {
        typeString = getTypeString(type);
      }
      typeStrings.put(type, typeString);

      if (!serializationOracle.maybeInstantiated(type)
          && !deserializationOracle.maybeInstantiated(type)) {
        continue;
      }

      String jsniTypeRef;
      jsniTypeRef = TypeOracleMediator.computeBinaryClassName(type.getLeafType());
      while (type.isArray() != null) {
        jsniTypeRef += "[]";
        type = type.isArray().getComponentType();
      }

      if (shard && ++shardCount % shardSize == 0) {
        srcWriter.println("})();");
        srcWriter.println("(function() {");
      }

      srcWriter.println("@com.google.gwt.user.client.rpc.impl.SerializerBase"
          + "::registerSignature("
          + "Lcom/google/gwt/core/client/JsArrayString;" + "Ljava/lang/Class;"
          + "Ljava/lang/String;)(");
      srcWriter.indent();
      srcWriter.println("@" + typeSerializerClassName + "::signatureMap,");
      srcWriter.println("@" + jsniTypeRef + "::class,");
      srcWriter.println("\"" + typeString + "\");");
      srcWriter.outdent();
      srcWriter.println();
    }

    if (shard) {
      srcWriter.println("})();");
    }

    srcWriter.outdent();
    srcWriter.println("}-*/;");
    srcWriter.println();
  }

  private void writeStaticFields() {
    srcWriter.println("private static final MethodMap methodMap = JavaScriptObject.createObject().cast();");
    srcWriter.println("private static final JsArrayString signatureMap = JavaScriptObject.createArray().cast();");
    srcWriter.println("protected MethodMap getMethodMap() { return methodMap; }");
    srcWriter.println("protected JsArrayString getSignatureMap() { return signatureMap; }");
    srcWriter.println();
  }

  private void writeStaticInitializer() {
    srcWriter.println("static {");
    srcWriter.indentln("registerMethods();");
    srcWriter.indentln("registerSignatures();");
    srcWriter.println("}");
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
