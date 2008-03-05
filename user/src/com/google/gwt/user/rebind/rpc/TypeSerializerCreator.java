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
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JPackage;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import com.google.gwt.user.client.rpc.impl.Serializer;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;

import java.io.PrintWriter;

/**
 * This class generates a class that is able to serialize and deserialize a set
 * of types into or out of a stream.
 */
public class TypeSerializerCreator {

  private static final String DESERIALIZE_METHOD_SIGNATURE = "public native void deserialize("
      + "SerializationStreamReader streamReader, Object instance, String typeSignature)"
      + " throws SerializationException";

  private static final String INSTANTIATE_METHOD_SIGNATURE = "public native Object instantiate("
      + "SerializationStreamReader streamReader, String typeSignature)"
      + " throws SerializationException";

  private static final String SERIALIZE_METHOD_SIGNATURE = "public native void serialize("
      + "SerializationStreamWriter streamWriter, Object instance, String typeSignature)"
      + " throws SerializationException";

  private final GeneratorContext context;

  private final boolean enforceTypeVersioning;

  private final JClassType remoteService;

  private final JType[] serializableTypes;

  private final SerializableTypeOracle serializationOracle;

  private final SourceWriter srcWriter;

  private final TypeOracle typeOracle;

  public TypeSerializerCreator(TreeLogger logger,
      SerializableTypeOracle serializationOracle, GeneratorContext context,
      JClassType remoteService) {
    this.context = context;
    this.remoteService = remoteService;
    this.serializationOracle = serializationOracle;
    this.typeOracle = context.getTypeOracle();

    enforceTypeVersioning = Shared.shouldEnforceTypeVersioning(logger,
        context.getPropertyOracle());

    serializableTypes = serializationOracle.getSerializableTypes();

    srcWriter = getSourceWriter(logger, context);
  }

  public String realize(TreeLogger logger) {
    logger = logger.branch(TreeLogger.DEBUG,
        "Generating TypeSerializer for service interface '"
            + getServiceInterface().getQualifiedSourceName() + "'", null);
    String typeSerializerName = serializationOracle.getTypeSerializerQualifiedName(getServiceInterface());
    if (srcWriter == null) {
      return typeSerializerName;
    }

    createFieldSerializers(logger, context);

    writeStaticFields();

    writeCreateMethods();

    writeCreateMethodMapMethod();

    if (shouldEnforceTypeVersioning()) {
      writeCreateSignatureMapMethod();
    }

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
    assert (serializationOracle.isSerializable(type));

    JParameterizedType parameterizedType = type.isParameterized();
    if (parameterizedType != null) {
      createFieldSerializer(logger, ctx, parameterizedType.getRawType());
      return;
    }

    JClassType customFieldSerializer = serializationOracle.hasCustomFieldSerializer(type);
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

    FieldSerializerCreator creator = new FieldSerializerCreator(
        serializationOracle, (JClassType) type);
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
        + serializationOracle.getFieldSerializerName(type).replace('.', '_');
  }

  private JType[] getSerializableTypes() {
    return serializableTypes;
  }

  private JClassType getServiceInterface() {
    return remoteService;
  }

  private SourceWriter getSourceWriter(TreeLogger logger, GeneratorContext ctx) {
    JClassType serviceIntf = getServiceInterface();
    JPackage serviceIntfPackage = serviceIntf.getPackage();
    String packageName = serviceIntfPackage != null
        ? serviceIntfPackage.getName() : "";
    String className = serializationOracle.getTypeSerializerSimpleName(getServiceInterface());
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
    if (!serializationOracle.maybeInstantiated(type)) {
      return false;
    }

    if (type.isArray() != null) {
      return false;
    }

    JClassType customSerializer = serializationOracle.hasCustomFieldSerializer(type);
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

  private boolean shouldEnforceTypeVersioning() {
    return enforceTypeVersioning;
  }

  private void writeCreateMethodMapMethod() {
    srcWriter.println("private static native JavaScriptObject createMethodMap() /*-" + '{');
    {
      srcWriter.indent();
      srcWriter.println("return {");
      JType[] types = getSerializableTypes();
      boolean needComma = false;
      for (int index = 0; index < types.length; ++index) {
        JType type = types[index];
        if (!serializationOracle.maybeInstantiated(type)) {
          continue;
        }

        if (needComma) {
          srcWriter.println(",");
        } else {
          needComma = true;
        }

        String typeString = serializationOracle.getSerializedTypeName(type);
        if (shouldEnforceTypeVersioning()) {
          typeString += "/"
              + serializationOracle.getSerializationSignature(type);
        }

        srcWriter.print("\"" + typeString + "\":");

        // Make a JSON array
        srcWriter.println("[");
        {
          srcWriter.indent();

          String serializerName = serializationOracle.getFieldSerializerName(type);
          {
            // First the initialization method
            srcWriter.print("@");
            if (needsCreateMethod(type)) {
              srcWriter.print(serializationOracle.getTypeSerializerQualifiedName(getServiceInterface()));
              srcWriter.print("::");
              srcWriter.print(getCreateMethodName(type));
            } else {
              srcWriter.print(serializerName);
              srcWriter.print("::instantiate");
            }
            srcWriter.print("(L"
                + SerializationStreamReader.class.getName().replace('.', '/')
                + ";)");
            srcWriter.println(",");
          }

          String jsniSignature = type.getJNISignature();

          {
            // Now the deserialization method
            srcWriter.print("@" + serializerName);
            srcWriter.print("::deserialize(L"
                + SerializationStreamReader.class.getName().replace('.', '/')
                + ";" + jsniSignature + ")");
            srcWriter.println(",");
          }
          {
            // Now the serialization method
            srcWriter.print("@" + serializerName);
            srcWriter.print("::serialize(L"
                + SerializationStreamWriter.class.getName().replace('.', '/')
                + ";" + jsniSignature + ")");
            srcWriter.println();
          }
          srcWriter.outdent();
        }
        srcWriter.print("]");
      }
      srcWriter.println();
      srcWriter.println("};");
      srcWriter.outdent();
    }
    srcWriter.println("}-*/;");
    srcWriter.println();
  }

  private void writeCreateMethods() {
    JType[] types = getSerializableTypes();
    for (int typeIndex = 0; typeIndex < types.length; ++typeIndex) {
      JType type = types[typeIndex];
      assert (serializationOracle.isSerializable(type));

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
        if (!serializationOracle.maybeInstantiated(type)) {
          continue;
        }
        if (needComma) {
          srcWriter.println(",");
        } else {
          needComma = true;
        }

        srcWriter.print("\"" + serializationOracle.getSerializedTypeName(type)
            + "\":\"" + serializationOracle.getSerializationSignature(type)
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
      String serializerTypeName = serializationOracle.getTypeSerializerQualifiedName(getServiceInterface());
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
    if (!shouldEnforceTypeVersioning()) {
      srcWriter.println("public String getSerializationSignature(String typeName) {");
      srcWriter.indentln("return null;");
      srcWriter.println("};");
    } else {
      String serializerTypeName = serializationOracle.getTypeSerializerQualifiedName(getServiceInterface());
      srcWriter.println("public native String getSerializationSignature(String typeName) /*-" + '{');
      srcWriter.indent();
      srcWriter.println("var signature = @" + serializerTypeName
          + "::signatureMap[typeName];");
      srcWriter.println("return signature || null;");
      srcWriter.outdent();
      srcWriter.println("}-*/;");
    }
    srcWriter.println();
  }

  private void writeInstantiateMethod() {
    srcWriter.print(INSTANTIATE_METHOD_SIGNATURE);
    srcWriter.println(" /*-" + '{');
    {
      String serializerTypeName = serializationOracle.getTypeSerializerQualifiedName(getServiceInterface());
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
      String serializerTypeName = serializationOracle.getTypeSerializerQualifiedName(getServiceInterface());
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

  private void writeStaticFields() {
    srcWriter.println("private static final JavaScriptObject methodMap = createMethodMap();");
    if (shouldEnforceTypeVersioning()) {
      srcWriter.println("private static final JavaScriptObject signatureMap = createSignatureMap();");
    }
    srcWriter.println();
  }
}