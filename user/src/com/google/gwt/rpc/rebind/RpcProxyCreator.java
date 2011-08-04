/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.rpc.rebind;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.impl.ArtificialRescue;
import com.google.gwt.core.client.impl.Impl;
import com.google.gwt.core.client.impl.ArtificialRescue.Rescue;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JArrayType;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JEnumType;
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.util.collect.Lists;
import com.google.gwt.rpc.client.impl.CommandToStringWriter;
import com.google.gwt.rpc.client.impl.RpcServiceProxy;
import com.google.gwt.rpc.client.impl.TypeOverrides;
import com.google.gwt.rpc.linker.RpcDataArtifact;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import com.google.gwt.user.client.rpc.impl.RemoteServiceProxy;
import com.google.gwt.user.linker.rpc.RpcLogArtifact;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;
import com.google.gwt.user.rebind.rpc.CustomFieldSerializerValidator;
import com.google.gwt.user.rebind.rpc.ProxyCreator;
import com.google.gwt.user.rebind.rpc.SerializableTypeOracle;
import com.google.gwt.user.rebind.rpc.SerializableTypeOracleBuilder;
import com.google.gwt.user.rebind.rpc.SerializationUtils;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Generates async proxy implementations using the RPC system.
 */
public class RpcProxyCreator extends ProxyCreator {
  private String typeOverrideName;

  public RpcProxyCreator(JClassType type) {
    super(type);
  }

  @Override
  protected String computeTypeNameExpression(JType paramType) {
    if (paramType.isClass() != null) {
      return "GWT.isScript() ? Impl.getNameOf(\"@"
          + paramType.getQualifiedSourceName() + "\") : \""
          + SerializationUtils.getRpcTypeName(paramType) + "\"";
    } else {
      /*
       * Consider the case of service methods that have interface parameters;
       * these types don't necessarily exist in the client code, so we want to
       * encode these type names in a way that can always be distinguished from
       * obfuscated type names.
       */
      return "\" " + SerializationUtils.getRpcTypeName(paramType)
          + "\"";
    }
  }

  @Override
  protected void generateProxyContructor(SourceWriter srcWriter) {
    srcWriter.println("public " + getProxySimpleName() + "() {");
    srcWriter.indent();
    srcWriter.println("super(GWT.getModuleBaseURL(),");
    srcWriter.indent();
    srcWriter.println(getRemoteServiceRelativePath() + ",");
    srcWriter.println("OVERRIDES);");
    srcWriter.outdent();
    srcWriter.outdent();
    srcWriter.println("}");
  }

  /**
   * Generate any fields required by the proxy.
   */
  @Override
  protected void generateProxyFields(SourceWriter srcWriter,
      SerializableTypeOracle serializableTypeOracle,
      String serializationPolicyStrongName, String remoteServiceInterfaceName) {
    // Initialize a field with binary name of the remote service interface
    srcWriter.println("private static final String REMOTE_SERVICE_INTERFACE_NAME = "
        + "\"" + remoteServiceInterfaceName + "\";");
    srcWriter.println("private static final " + TypeOverrides.class.getName()
        + " OVERRIDES = GWT.isScript() ? " + typeOverrideName
        + ".create() : null;");
    srcWriter.println();
  }

  @Override
  protected void generateStreamWriterOverride(SourceWriter srcWriter) {
    // Intentional no-op. Called if elideTypeNames is on, which is ignored
  }

  @Override
  protected void generateTypeHandlers(TreeLogger logger, GeneratorContext ctx,
      SerializableTypeOracle serializationSto,
      SerializableTypeOracle deserializationSto)
      throws UnableToCompleteException {
    String simpleName = serviceIntf.getSimpleSourceName()
        + "_TypeOverridesFactory";
    PrintWriter out = ctx.tryCreate(logger, serviceIntf.getPackage().getName(),
        simpleName);
    if (out == null) {
      return;
    }

    TypeOracle typeOracle = ctx.getTypeOracle();
    JClassType objectType = typeOracle.getJavaLangObject();
    Set<JType> classLiterals = new LinkedHashSet<JType>();
    Map<JType, JMethod> serializerMethods = new LinkedHashMap<JType, JMethod>();
    Map<JType, List<String>> fields = new LinkedHashMap<JType, List<String>>();

    StringBuilder sb = writeArtificialRescues(typeOracle, serializationSto,
        deserializationSto);

    ClassSourceFileComposerFactory composerFactory = new ClassSourceFileComposerFactory(
        serviceIntf.getPackage().getName(), simpleName);
    composerFactory.addImport(ArtificialRescue.class.getCanonicalName());
    composerFactory.addImport(GWT.class.getCanonicalName());
    composerFactory.addImport(Impl.class.getCanonicalName());
    composerFactory.addImport(Rescue.class.getCanonicalName());
    composerFactory.addImport(TypeOverrides.class.getCanonicalName());
    composerFactory.addImport(TypeOverrides.SerializeFunction.class.getCanonicalName());

    composerFactory.addAnnotationDeclaration(sb.toString());
    SourceWriter sw = composerFactory.createSourceWriter(ctx, out);

    sw.println("public static TypeOverrides create() {");
    sw.indent();
    sw.println("TypeOverrides toReturn = TypeOverrides.create();");
    for (JType type : serializationSto.getSerializableTypes()) {
      JClassType classType = type.isClass();
      if (classType == null) {
        continue;
      }

      /*
       * Figure out which fields should be serialized and if there's a CFS. This
       * is done by crawling the supertype chain until we hit Object or a type
       * with a CFS.
       */
      boolean allFieldsAreSerializable = true;
      List<String> fieldRefs = new ArrayList<String>();
      JMethod serializerMethod = null;
      do {
        JClassType customSerializer = SerializableTypeOracleBuilder.findCustomFieldSerializer(
            typeOracle, classType);
        serializerMethod = customSerializer == null ? null
            : CustomFieldSerializerValidator.getSerializationMethod(
                customSerializer, type.isClass());

        if (serializerMethod != null) {
          // Don't include any fields in the type
          break;
        }

        JField[] serializableFields = SerializationUtils.getSerializableFields(
            typeOracle, classType);
        allFieldsAreSerializable &= serializableFields.length == classType.getFields().length;
        for (JField field : serializableFields) {
          fieldRefs.add("@" + field.getEnclosingType().getQualifiedSourceName()
              + "::" + field.getName());
        }
        classType = classType.getSuperclass();
      } while (classType != objectType);

      if (allFieldsAreSerializable && serializerMethod == null) {
        // We can just inspect the object at runtime; best for code size
        continue;
      }

      if (serializerMethod != null || !fieldRefs.isEmpty()) {
        classLiterals.add(type);

        /*
         * toReturn.set(class_foo_Bar().getName(), serializer_foo_Bar(),
         * fields_foo_Bar());
         */
        String mangledTypeName = type.getQualifiedSourceName().replace('.', '_');
        sw.println("toReturn.set(class_" + mangledTypeName + "().getName()");
        if (serializerMethod == null) {
        } else {
          serializerMethods.put(type, serializerMethod);
          sw.indentln(",serializer_" + mangledTypeName + "()");
        }
        if (fieldRefs.isEmpty()) {
          sw.indentln(");");
        } else {
          fields.put(type, fieldRefs);
          sw.indentln(",fields_" + mangledTypeName + "());");
        }
      }
    }

    sw.println("return toReturn;");
    sw.outdent();
    sw.println("}");

    for (JType classLiteral : classLiterals) {
      sw.println("public static native Class class_"
          + classLiteral.getQualifiedSourceName().replace('.', '_') + "() /*-{");
      sw.indentln("return @" + classLiteral.getQualifiedSourceName()
          + "::class;");
      sw.println("}-*/;");
      sw.println();
    }

    for (Map.Entry<JType, JMethod> entry : serializerMethods.entrySet()) {
      sw.println("public static native "
          + TypeOverrides.SerializeFunction.class.getSimpleName()
          + " serializer_"
          + entry.getKey().getQualifiedSourceName().replace('.', '_')
          + "() /*-{");
      sw.indentln("return " + entry.getValue().getJsniSignature() + ";");
      sw.println("}-*/;");
      sw.println();
    }

    for (Map.Entry<JType, List<String>> entry : fields.entrySet()) {
      sw.println("public static String[] fields_"
          + entry.getKey().getQualifiedSourceName().replace('.', '_') + "() {");
      sw.print("return new String[] {");
      for (String fieldRef : entry.getValue()) {
        sw.print("Impl.getNameOf(\"" + fieldRef + "\"),");
      }
      sw.println("};");
      sw.println("}");
      sw.println();
    }

    sw.commit(logger);

    typeOverrideName = composerFactory.getCreatedClassName();
  }

  @Override
  protected Class<? extends RemoteServiceProxy> getProxySupertype() {
    return RpcServiceProxy.class;
  }

  @Override
  protected Class<? extends SerializationStreamWriter> getStreamWriterClass() {
    return CommandToStringWriter.class;
  }

  @Override
  protected String writeSerializationPolicyFile(TreeLogger logger,
      GeneratorContext ctx, SerializableTypeOracle serializationSto,
      SerializableTypeOracle deserializationSto)
      throws UnableToCompleteException {

    RpcDataArtifact data = new RpcDataArtifact(
        serviceIntf.getQualifiedSourceName());

    for (JType type : deserializationSto.getSerializableTypes()) {
      if (!(type instanceof JClassType)) {
        continue;
      }

      JField[] serializableFields = SerializationUtils.getSerializableFields(
          ctx.getTypeOracle(), (JClassType) type);

      List<String> names = Lists.create();
      for (int i = 0, j = serializableFields.length; i < j; i++) {
        names = Lists.add(names, serializableFields[i].getName());
      }

      data.setFields(SerializationUtils.getRpcTypeName(type), names);
    }

    ctx.commitArtifact(logger, data);

    return RpcLogArtifact.UNSPECIFIED_STRONGNAME;
  }

  private StringBuilder writeArtificialRescues(TypeOracle typeOracle,
      SerializableTypeOracle serializationSto,
      SerializableTypeOracle deserializationSto) {
    Set<JType> serializableTypes = new LinkedHashSet<JType>();
    Collections.addAll(serializableTypes,
        serializationSto.getSerializableTypes());
    Collections.addAll(serializableTypes,
        deserializationSto.getSerializableTypes());
    for (JMethod m : serviceIntf.getOverridableMethods()) {
      // Pick up any primitive return types, which get sent boxed
      JPrimitiveType mustBox = m.getReturnType().isPrimitive();
      if (mustBox != null) {
        serializableTypes.add(m.getReturnType());
      }
    }

    StringBuilder sb = new StringBuilder("@ArtificialRescue({");
    for (JType serializableType : serializableTypes) {

      JArrayType serializableArray = serializableType.isArray();
      JClassType serializableClass = serializableType.isClass();
      JPrimitiveType serializablePrimitive = serializableType.isPrimitive();
      if (serializableArray != null) {
        sb.append("\n@Rescue(className = \"");
        sb.append(serializableArray.getQualifiedSourceName());
        sb.append("\",\n instantiable = true),");
      } else if (serializableClass != null) {
        writeSingleRescue(typeOracle, deserializationSto, sb, serializableClass);
      } else if (serializablePrimitive != null) {
        JClassType boxedClass = typeOracle.findType(serializablePrimitive.getQualifiedBoxedSourceName());
        assert boxedClass != null : "No boxed version of "
            + serializablePrimitive.getQualifiedSourceName();
        writeSingleRescue(typeOracle, deserializationSto, sb, boxedClass);
      }
    }
    sb.append("})");
    return sb;
  }

  /**
   * Writes the rescue of a serializable type and its custom serialization
   * logic.
   */
  private void writeSingleRescue(TypeOracle typeOracle,
      SerializableTypeOracle deserializationOracle, StringBuilder sb,
      JClassType serializableClass) {
    boolean shouldDeserialize = deserializationOracle.isSerializable(serializableClass);

    // Pull the two custom serialization methods
    JClassType customSerializer;
    JMethod deserialize = null;
    JMethod instantiate = null;

    // An automatically-serializable subclass of a manually serialized class
    boolean hybridSerialization = false;

    {
      JClassType search = serializableClass;
      do {
        customSerializer = SerializableTypeOracleBuilder.findCustomFieldSerializer(
            typeOracle, search);

        if (customSerializer != null) {
          instantiate = CustomFieldSerializerValidator.getInstantiationMethod(
              customSerializer, search);

          deserialize = CustomFieldSerializerValidator.getDeserializationMethod(
              customSerializer, search);

          hybridSerialization = search != serializableClass;
          break;
        }

        search = search.getSuperclass();
      } while (search != null);
    }

    // The fields that should be preserved from being pruned
    JField[] serializableFields;
    JEnumType enumType = serializableClass.isEnum();
    if (enumType != null) {
      serializableFields = enumType.getFields();
    } else {
      serializableFields = SerializationUtils.getSerializableFields(typeOracle,
          serializableClass);
    }

    /*
     * We need to rescue the constructor if there is no instantiate method and
     * there is a custom deserialize method.
     */
    boolean rescueConstructor = instantiate == null && deserialize != null;

    /*
     * There may be either no custom serializer or a custom serializer that
     * doesn't define the instantiate method.
     */
    if (shouldDeserialize || rescueConstructor
        || (customSerializer == null && serializableFields.length > 0)) {

      /*
       * @Rescue(className="package.Foo$Inner", instantiable=true, fields={..},
       * methods={..}),
       */
      sb.append("\n@Rescue(className = \"").append(
          serializableClass.getQualifiedSourceName()).append("\"");

      sb.append(",\n instantiable = ").append(shouldDeserialize);
      sb.append(",\n fields = {");
      if (customSerializer == null || hybridSerialization) {
        for (JField field : serializableFields) {
          sb.append("\"").append(field.getName()).append("\",");
        }
      }
      sb.append("},\n methods = {");
      if (rescueConstructor) {
        sb.append("\"").append(serializableClass.getName().replace('.', '$')).append(
            "()\"");
      }
      sb.append("}),");
    }

    // Rescue the custom serialization logic if any exists
    if (customSerializer != null) {
      sb.append("\n@Rescue(className = \"").append(
          customSerializer.getQualifiedSourceName()).append("\",\n methods = {");
      if (instantiate != null) {
        String jsniSignature = instantiate.getJsniSignature();
        sb.append("\"").append(
            jsniSignature.substring(jsniSignature.lastIndexOf(':') + 1)).append(
            "\",");
      }
      if (deserialize != null) {
        String jsniSignature = deserialize.getJsniSignature();
        sb.append("\"").append(
            jsniSignature.substring(jsniSignature.lastIndexOf(':') + 1)).append(
            "\",");
      }
      sb.append("}),");
    }
  }
}
