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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.GwtScriptOnly;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.core.ext.BadPropertyValueException;
import com.google.gwt.core.ext.CachedGeneratorResult;
import com.google.gwt.core.ext.ConfigurationProperty;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.util.log.speedtracer.CompilerEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import com.google.gwt.user.client.rpc.impl.SerializerBase;
import com.google.gwt.user.client.rpc.impl.TypeHandler;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
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
   * Production Mode, but it isn't needed there anyway.
   * 
   * TODO: remove this (and related code) when it is no longer needed.
   */
  private static final String DEFAULT_CREATEMETHODMAP_SHARD_SIZE = "0";

  /**
   * Java system property name to override the above.
   */
  private static final String GWT_CREATEMETHODMAP_SHARD_SIZE = "gwt.typecreator.shard.size";

  private static int shardSize = -1;

  private static void computeShardSize(TreeLogger logger) throws UnableToCompleteException {
    String shardSizeProperty =
        System.getProperty(GWT_CREATEMETHODMAP_SHARD_SIZE, DEFAULT_CREATEMETHODMAP_SHARD_SIZE);
    try {
      shardSize = Integer.valueOf(shardSizeProperty);
      if (shardSize < 0) {
        logger.log(TreeLogger.ERROR, GWT_CREATEMETHODMAP_SHARD_SIZE + " must be non-negative: "
            + shardSizeProperty);
        throw new UnableToCompleteException();
      }
    } catch (NumberFormatException e) {
      logger.log(TreeLogger.ERROR, "Property " + GWT_CREATEMETHODMAP_SHARD_SIZE + " not a number: "
          + shardSizeProperty, e);
      throw new UnableToCompleteException();
    }
  }

  private final GeneratorContext context;

  private final SerializableTypeOracle deserializationOracle;

  private final boolean elideTypeNames;

  private final JType[] serializableTypes;

  private final SerializableTypeOracle serializationOracle;

  private final SourceWriter srcWriter;

  private final TypeOracle typeOracle;

  private final String typeSerializerClassName;

  private final String typeSerializerSimpleName;

  private final Map<JType, String> typeStrings = new IdentityHashMap<JType, String>();

  private final Set<JType> typesNotUsingCustomFieldSerializers;

  private final Set<JType> customFieldSerializersUsed;

  public TypeSerializerCreator(TreeLogger logger, SerializableTypeOracle serializationOracle,
      SerializableTypeOracle deserializationOracle, GeneratorContext context,
      String typeSerializerClassName, String typeSerializerSimpleName)
      throws UnableToCompleteException {
    this.context = context;
    this.typeSerializerClassName = typeSerializerClassName;
    this.typeSerializerSimpleName = typeSerializerSimpleName;
    this.serializationOracle = serializationOracle;
    this.deserializationOracle = deserializationOracle;

    this.typeOracle = context.getTypeOracle();

    Set<JType> typesSet = new HashSet<JType>();
    typesSet.addAll(Arrays.asList(serializationOracle.getSerializableTypes()));
    typesSet.addAll(Arrays.asList(deserializationOracle.getSerializableTypes()));
    serializableTypes = typesSet.toArray(new JType[0]);
    Arrays.sort(serializableTypes, SerializableTypeOracleBuilder.JTYPE_COMPARATOR);

    srcWriter = getSourceWriter(logger, context);
    if (shardSize < 0) {
      computeShardSize(logger);
    }
    if (logger.isLoggable(TreeLogger.TRACE)) {
      logger.log(TreeLogger.TRACE, "Using a shard size of " + shardSize
          + " for TypeSerializerCreator createMethodMap");
    }

    try {
      ConfigurationProperty prop =
          context.getPropertyOracle().getConfigurationProperty(GWT_ELIDE_TYPE_NAMES_FROM_RPC);
      elideTypeNames = Boolean.parseBoolean(prop.getValues().get(0));
    } catch (BadPropertyValueException e) {
      logger.log(TreeLogger.ERROR, "The configuration property " + GWT_ELIDE_TYPE_NAMES_FROM_RPC
          + " was not defined. Is RemoteService.gwt.xml inherited?");
      throw new UnableToCompleteException();
    }

    if (context.isGeneratorResultCachingEnabled()) {
      typesNotUsingCustomFieldSerializers = new HashSet<JType>();
      customFieldSerializersUsed = new HashSet<JType>();
    } else {
      typesNotUsingCustomFieldSerializers = null;
      customFieldSerializersUsed = null;
    }
  }

  public Set<JType> getCustomSerializersUsed() {
    return customFieldSerializersUsed;
  }

  public Set<JType> getTypesNotUsingCustomSerializers() {
    return typesNotUsingCustomFieldSerializers;
  }

  public Map<JType, String> getTypeStrings() {
    return Collections.unmodifiableMap(typeStrings);
  }

  public String realize(TreeLogger logger) {
    logger =
        logger.branch(TreeLogger.DEBUG, "Generating TypeSerializer for service interface '"
            + typeSerializerClassName + "'", null);

    createFieldSerializers(logger, context);

    int index = 0;
    for (JType type : getSerializableTypes()) {

      String typeString;
      if (elideTypeNames) {
        typeString = Integer.toString(++index, Character.MAX_RADIX);
      } else {
        typeString = getTypeString(type);
      }
      typeStrings.put(type, typeString);
    }

    if (srcWriter != null) {
      writeStaticFields();
      writeStaticInitializer();

      if (context.isProdMode()) {
        writeLoadMethodsNative();
        writeLoadSignaturesNative();
      } else {
        writeLoadMethodsJava();
        writeLoadSignaturesJava();
      }

      writeConstructor();
      srcWriter.commit(logger);
    }

    return typeSerializerClassName;
  }

  /*
   * Create a field serializer for a type if it does not have a custom
   * serializer.
   */
  private void createFieldSerializer(TreeLogger logger, GeneratorContext ctx, JType type) {
    Event event = SpeedTracerLogger.start(CompilerEventType.GENERATOR_RPC_FIELD_SERIALIZER);
    try {
      assert (type != null);
      assert (serializationOracle.isSerializable(type) || deserializationOracle
          .isSerializable(type));

      JParameterizedType parameterizedType = type.isParameterized();
      if (parameterizedType != null) {
        createFieldSerializer(logger, ctx, parameterizedType.getRawType());
        return;
      }

      /*
       * Only a JClassType can reach this point in the code. JPrimitives have
       * been removed because their serialization is built in, interfaces have
       * been removed because they are not an instantiable type and
       * parameterized types have been broken down into their raw types.
       */
      assert (type.isClass() != null || type.isArray() != null);

      // get custom field serializer, if available
      JClassType customFieldSerializer =
          SerializableTypeOracleBuilder.findCustomFieldSerializer(typeOracle, type);

      if (ctx.isGeneratorResultCachingEnabled()) {
        // update cached info for next iteration
        if (customFieldSerializer != null) {
          customFieldSerializersUsed.add(customFieldSerializer);
        } else {
          typesNotUsingCustomFieldSerializers.add(type);
        }

        // check the cache for a valid field serializer for the current type
        if (findReusableCachedFieldSerializerIfAvailable(logger, ctx, type,
            customFieldSerializer)) {
          // we can skip re-generation of the field serializer for the current
          // type
          return;
        }
      }

      // generate a new field serializer
      FieldSerializerCreator creator =
          new FieldSerializerCreator(context, serializationOracle, deserializationOracle,
              (JClassType) type, customFieldSerializer);
      creator.realize(logger, ctx);
    } finally {
      event.end();
    }
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

  /*
   * check whether we can use a previously generated version of a
   * FieldSerializer. If so, mark it for reuse, and return true. Otherwise
   * return false.
   */
  private boolean findReusableCachedFieldSerializerIfAvailable(TreeLogger logger,
      GeneratorContext ctx, JType type, JType customFieldSerializer) {

    CachedGeneratorResult lastResult = ctx.getCachedGeneratorResult();
    if (lastResult == null || !ctx.isGeneratorResultCachingEnabled()) {
      return false;
    }

    String fieldSerializerName = SerializationUtils.getStandardSerializerName((JClassType) type);

    if (type instanceof JClassType) {
      // check that it is available for reuse
      if (!lastResult.isTypeCached(fieldSerializerName)) {
        if (logger.isLoggable(TreeLogger.TRACE)) {
          if (ctx.getTypeOracle().findType(fieldSerializerName) == null) {
            logger.log(TreeLogger.TRACE, "No cached field serializer available for "
                + type.getQualifiedSourceName());
          }
        }
        return false;
      }
    } else {
      return false;
    }

    @SuppressWarnings("unchecked")
    CachedRpcTypeInformation cachedTypeInfo =
        (CachedRpcTypeInformation) lastResult.getClientData(ProxyCreator.CACHED_TYPE_INFO_KEY);

    assert cachedTypeInfo != null;
    boolean foundMatch = false;
    if (cachedTypeInfo.checkLastModifiedTime(type)
        && ((customFieldSerializer != null && cachedTypeInfo
            .checkLastModifiedTime(customFieldSerializer)) || (customFieldSerializer == null && cachedTypeInfo
            .checkTypeNotUsingCustomSerializer(type)))) {
      // use cached version, if available
      foundMatch = ctx.tryReuseTypeFromCache(fieldSerializerName);
    }

    if (logger.isLoggable(TreeLogger.TRACE)) {
      String msg;
      if (foundMatch) {
        msg = "Reusing cached field serializer for " + type.getQualifiedSourceName();
      } else {
        msg = "Can't reuse cached field serializer for " + type.getQualifiedSourceName();
      }
      logger.log(TreeLogger.TRACE, msg);
    }

    return foundMatch;
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
    String name[] = getPackageAndClassName(typeSerializerClassName);
    String packageName = name[0];
    String className = name[1];
    PrintWriter printWriter = ctx.tryCreate(logger, packageName, className);
    if (printWriter == null) {
      return null;
    }

    ClassSourceFileComposerFactory composerFactory =
        new ClassSourceFileComposerFactory(packageName, className);

    composerFactory.addImport(GWT.class.getName());
    composerFactory.addImport(JsArrayString.class.getName());
    composerFactory.addImport(TypeHandler.class.getName());
    composerFactory.addImport(HashMap.class.getName());
    composerFactory.addImport(Map.class.getName());
    composerFactory.addImport(GwtScriptOnly.class.getName());

    composerFactory.setSuperclass(SerializerBase.class.getName());
    return composerFactory.createSourceWriter(ctx, printWriter);
  }

  private String getTypeString(JType type) {
    String typeString =
        SerializationUtils.getRpcTypeName(type) + "/"
            + SerializationUtils.getSerializationSignature(typeOracle, type);
    return typeString;
  }

  /**
   * Return <code>true</code> if the custom field serializer has an instantiate
   * method.
   */
  private boolean hasInstantiateMethod(JClassType customSerializer, JType type) {
    return CustomFieldSerializerValidator.getInstantiationMethod(customSerializer,
        (JClassType) type) != null;
  }

  /**
   * Writes constructor.
   * 
   * <pre>
   * public SchoolCalendarService_TypeSerializer() {
   *   super(methodMapJava, methodMapNative, signatureMapJava, signatureMapNative);
   * }
   * </pre>
   */
  private void writeConstructor() {
    srcWriter.println("public " + typeSerializerSimpleName + "() {");
    if (context.isProdMode()) {
      srcWriter.indentln("super(null, methodMapNative, null, signatureMapNative);");
    } else {
      srcWriter.indentln("super(methodMapJava, null, signatureMapJava, null);");
    }
    srcWriter.println("}");
    srcWriter.println();
  }

  /**
   * Writes a method to produce a map of type string -> class name of
   * {@link TypeHandler} for Java.
   * 
   * <pre>
   * private static Map&lt;String, String&gt; loadMethodsJava() {
   *   Map&lt;String, String&gt; result = new HashMap&lt;String, String&gt;();
   *   result.put(
   *       &quot;java.lang.String/2004016611&quot;,
   *       &quot;com.google.gwt.user.client.rpc.core.java.lang.String_FieldSerializer&quot;
   *   ...
   *   return result;
   * }
   * </pre>
   */
  private void writeLoadMethodsJava() {
    srcWriter.println("@SuppressWarnings(\"deprecation\")");
    srcWriter.println("private static Map<String, String> loadMethodsJava() {");
    srcWriter.indent();
    srcWriter.println("Map<String, String> result = new HashMap<String, String>();");

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

    for (JType type : filteredTypes) {
      String typeString = typeStrings.get(type);
      assert typeString != null : "Missing type signature for " + type.getQualifiedSourceName();
      srcWriter.println("result.put(\"" + typeString + "\", \""
          + SerializationUtils.getStandardSerializerName((JClassType) type) + "\");");
    }

    srcWriter.println("return result;");
    srcWriter.outdent();
    srcWriter.println("}");
    srcWriter.println();
  }

  /**
   * Writes a method to produce a native map of type string -> handler funcs.
   * 
   * <pre>
   * private static native MethodMap loadMethodsNative() /&#42;-{
   *     var result = {};
   *     result["java.lang.String/2004016611"] = [
   *         @com.google.gwt.user.client.rpc.core.java.lang.String_CustomFieldSerializer::instantiate(Lcom/google/gwt/user/client/rpc/SerializationStreamReader;),
   *         @com.google.gwt.user.client.rpc.core.java.lang.String_CustomFieldSerializer::deserialize(Lcom/google/gwt/user/client/rpc/SerializationStreamReader;Ljava/lang/String;),
   *         @com.google.gwt.user.client.rpc.core.java.lang.String_CustomFieldSerializer::serialize(Lcom/google/gwt/user/client/rpc/SerializationStreamWriter;Ljava/lang/String;)
   *       ];
   *     ...
   *     return result;
   *   }-&#42;/;
   * </pre>
   */
  private void writeLoadMethodsNative() {
    srcWriter.println("@SuppressWarnings(\"deprecation\")");
    srcWriter.println("@GwtScriptOnly");
    srcWriter.println("private static native MethodMap loadMethodsNative() /*-{");
    srcWriter.indent();
    srcWriter.println("var result = {};");

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

      String typeString = typeStrings.get(type);
      assert typeString != null : "Missing type signature for " + type.getQualifiedSourceName();
      srcWriter.println("result[\"" + typeString + "\"] = [");

      srcWriter.indent();
      writeTypeMethodsNative(type);
      srcWriter.outdent();

      srcWriter.indentln("];");
      srcWriter.println();
    }

    if (shard) {
      srcWriter.println("})();");
    }

    srcWriter.println("return result;");
    srcWriter.outdent();
    srcWriter.println("}-*/;");
    srcWriter.println();
  }

  /**
   * Writes a method to produce a map of class name to type string for Java.
   * 
   * <pre>
   * private static Map&lt;String&lt;?&gt;, String&gt; loadSignaturesJava() {
   *   Map&lt;String&lt;?&gt;, String&gt; result = new HashMap&lt;String&lt;?&gt;, String&gt;();
   *   result.put(
   *       com.google.gwt.user.client.rpc.core.java.lang.String_FieldSerializer.concreteType(),
   *       &quot;java.lang.String/2004016611&quot;);
   *   ...
   *   return result;
   * }
   * </pre>
   */
  private void writeLoadSignaturesJava() {
    srcWriter.println("@SuppressWarnings(\"deprecation\")");
    srcWriter.println("private static Map<String, String> loadSignaturesJava() {");
    srcWriter.indent();
    srcWriter.println("Map<String, String> result = new HashMap<String, String>();");

    for (JType type : getSerializableTypes()) {
      String typeString = typeStrings.get(type);

      if (!serializationOracle.maybeInstantiated(type)
          && !deserializationOracle.maybeInstantiated(type)) {
        continue;
      }

      String typeRef;
      JClassType customSerializer =
          SerializableTypeOracleBuilder.findCustomFieldSerializer(typeOracle, type);
      if (customSerializer != null
          && CustomFieldSerializerValidator.getConcreteTypeMethod(customSerializer) != null) {
        typeRef = customSerializer.getQualifiedSourceName() + ".concreteType()";
      } else {
        typeRef = '"' + SerializationUtils.getRpcTypeName(type) + '"';
      }

      srcWriter.println("result.put(" + typeRef + ", \"" + typeString + "\");");
    }

    srcWriter.println("return result;");
    srcWriter.outdent();
    srcWriter.println("}");
    srcWriter.println();
  }

  /**
   * Writes a method to produce a native map of system hash code to type string.
   * 
   * <pre>
   * private static native JsArrayString loadSignaturesNative() /*-{
   *   var result = [];
   *   result[@com.google.gwt.core.client.impl.Impl::getHashCode(Ljava/lang/Object;)(@java.lang.String::class)] = &quot;java.lang.String/2004016611&quot;;
   *   ...
   *   return result;
   * }-&#42;/;
   * </pre>
   */
  private void writeLoadSignaturesNative() {
    srcWriter.println("@SuppressWarnings(\"deprecation\")");
    srcWriter.println("@GwtScriptOnly");
    srcWriter.println("private static native JsArrayString loadSignaturesNative() /*-{");
    srcWriter.indent();
    srcWriter.println("var result = [];");

    boolean shard = shardSize > 0 && getSerializableTypes().length > shardSize;
    int shardCount = 0;

    if (shard) {
      srcWriter.println("(function() {");
    }

    for (JType type : getSerializableTypes()) {
      String typeString = typeStrings.get(type);

      if (!serializationOracle.maybeInstantiated(type)
          && !deserializationOracle.maybeInstantiated(type)) {
        continue;
      }

      if (shard && ++shardCount % shardSize == 0) {
        srcWriter.println("})();");
        srcWriter.println("(function() {");
      }

      srcWriter
          .println("result[@com.google.gwt.core.client.impl.Impl::getHashCode(Ljava/lang/Object;)(@"
              + type.getQualifiedSourceName() + "::class)] = \"" + typeString + "\";");
    }

    if (shard) {
      srcWriter.println("})();");
    }

    srcWriter.println("return result;");
    srcWriter.outdent();
    srcWriter.println("}-*/;");
    srcWriter.println();
  }

  /**
   * Writes the class's static fields.
   * 
   * <pre>
   * private static final Map&lt;String, String&gt; methodMapJava;
   * private static final MethodMap methodMapNative;
   * private static final Map&lt;String&lt;?&gt;, String&gt; signatureMapJava;
   * private static final JsArrayString signatureMapNative;
   * </pre>
   */
  private void writeStaticFields() {
    if (context.isProdMode()) {
      srcWriter.println("private static final MethodMap methodMapNative;");
      srcWriter.println("private static final JsArrayString signatureMapNative;");
    } else {
      srcWriter.println("private static final Map<String, String> methodMapJava;");
      srcWriter.println("private static final Map<String, String> signatureMapJava;");
    }
    srcWriter.println();
  }

  /**
   * Statically initializes the class fields either for script or JVM.
   * 
   * <pre>
   * static {
   *   if (GWT.isScript()) {
   *     methodMapJava = null;
   *     methodMapNative = loadMethodsNative();
   *     signatureMapJava = null;
   *     signatureMapNative = loadSignaturesNative();
   *   } else {
   *     methodMapJava = loadMethodsJava();
   *     methodMapNative = null;
   *     signatureMapJava = loadSignaturesJava();
   *     signatureMapNative = null;
   *   }
   * }
   * </pre>
   */
  private void writeStaticInitializer() {
    srcWriter.println("static {");
    srcWriter.indent();
    if (context.isProdMode()) {
      srcWriter.println("methodMapNative = loadMethodsNative();");
      srcWriter.println("signatureMapNative = loadSignaturesNative();");
    } else {
      srcWriter.println("methodMapJava = loadMethodsJava();");
      srcWriter.println("signatureMapJava = loadSignaturesJava();");
    }
    srcWriter.outdent();
    srcWriter.println("}");
    srcWriter.println();
  }

  /**
   * Write an entry in the methodMapNative for one type.
   * 
   * @param type type to generate entry for
   */
  private void writeTypeMethodsNative(JType type) {
    srcWriter.indent();
    String serializerName = SerializationUtils.getFieldSerializerName(typeOracle, type);
    JClassType customSerializer =
        SerializableTypeOracleBuilder.findCustomFieldSerializer(typeOracle, type);

    // First the initialization method
    if (deserializationOracle.maybeInstantiated(type)) {
      srcWriter.print("@");
      if (customSerializer != null) {
        if (hasInstantiateMethod(customSerializer, type)) {
          srcWriter.print(serializerName);
        } else {
          srcWriter.print(SerializationUtils.getStandardSerializerName((JClassType) type));
        }
      } else {
        srcWriter.print(serializerName);
      }
      srcWriter.print("::instantiate");
      srcWriter.print("(L" + SerializationStreamReader.class.getName().replace('.', '/') + ";)");
    }
    srcWriter.println(",");

    // Now the deserialization method
    if (deserializationOracle.isSerializable(type)) {
      // Assume param type is the concrete type of the serialized type.
      JType paramType = type;
      if (customSerializer != null) {
        // But a custom serializer may specify a looser type.
        JMethod deserializationMethod =
            CustomFieldSerializerValidator.getDeserializationMethod(customSerializer,
                (JClassType) type);
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
        JMethod serializationMethod =
            CustomFieldSerializerValidator.getSerializationMethod(customSerializer,
                (JClassType) type);
        paramType = serializationMethod.getParameters()[1].getType();
      }
      srcWriter.print("@" + serializerName);
      srcWriter.print("::serialize(L" + SerializationStreamWriter.class.getName().replace('.', '/')
          + ";" + paramType.getJNISignature() + ")");
      srcWriter.println();
    }
    srcWriter.outdent();
  }
}
