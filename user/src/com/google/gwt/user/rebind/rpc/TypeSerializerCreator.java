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

// CHECKSTYLE_OFF: Fighting a checkstyle bug in this file.

package com.google.gwt.user.rebind.rpc;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.ext.BadPropertyValueException;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.PropertyOracle;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.typeinfo.JArrayType;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JPackage;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import com.google.gwt.user.client.rpc.impl.Serializer;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

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

  public TypeSerializerCreator(SerializableTypeOracle serializationOracle) {
    if (serializationOracle == null) {
      throw new NullPointerException();
    }
    this.serializationOracle = serializationOracle;
  }

  public String realize(TreeLogger logger, GeneratorContext context,
      JClassType serviceInterface) {

    TypeOracle typeOracle = context.getTypeOracle();
    assert (typeOracle != null);

    setServiceInterface(serviceInterface);

    initializeProperties(logger, context);

    logger = logger.branch(TreeLogger.SPAM,
        "Generating TypeSerializer for service interface '"
            + serviceInterface.getQualifiedSourceName() + "'", null);
    String generatedTypeNames = serializationOracle.getTypeSerializerQualifiedName(getServiceInterface());

    generatedTypeNames += ";" + createFieldSerializers(logger, context);

    srcWriter = getSourceWriter(logger, context);
    if (srcWriter == null) {
      return generatedTypeNames;
    }

    write_StaticFields();

    writeCreate_Methods();

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

    return generatedTypeNames;
  }

  protected void write_StaticFields() {
    srcWriter.println("private static final JavaScriptObject methodMap = createMethodMap();");
    if (shouldEnforceTypeVersioning()) {
      srcWriter.println("private static final JavaScriptObject signatureMap = createSignatureMap();");
    }
    srcWriter.println();
  }

  protected void writeCreate_Methods() {
    JType[] types = getSerializableTypes();
    for (int typeIndex = 0; typeIndex < types.length; ++typeIndex) {
      JType type = types[typeIndex];
      assert (serializationOracle.isSerializable(type));

      // If this type is abstract it will not be serialized into the stream
      //
      if (isAbstractType(type)) {
        continue;
      }

      JMethod instantiate = serializationOracle.getCustomFieldSerializerInstantiateMethodForType(type);

      if (instantiate != null) {
        // There is a custom serializer that implements an instantiation method
        // so we do not need one here.
        //
        continue;
      }

      srcWriter.print("private static ");
      srcWriter.print(type.getQualifiedSourceName());
      srcWriter.print(" ");
      srcWriter.print(getInstantiationMethodName(type));
      srcWriter.println("(SerializationStreamReader streamReader) throws SerializationException {");
      srcWriter.indent();

      JArrayType array = type.isArray();
      if (array != null) {
        writeArrayInstantiationMethod(array);
      } else {
        srcWriter.print("return new ");
        srcWriter.print(type.getQualifiedSourceName());
        srcWriter.println("();");
      }

      srcWriter.outdent();
      srcWriter.println("}");

      srcWriter.println();
    }
  }

  protected void writeCreateMethodMapMethod() {
    srcWriter.println("private static native JavaScriptObject createMethodMap() /*-" + '{');
    {
      srcWriter.indent();
      srcWriter.println("return {");
      JType[] types = getSerializableTypes();
      boolean needComma = false;
      for (int index = 0; index < types.length; ++index) {
        JType type = types[index];
        if (isAbstractType(type)) {
          continue;
        }

        if (needComma) {
          srcWriter.println(",");
        } else {
          needComma = true;
        }

        String typeString = shouldEnforceTypeVersioning()
          ? serializationOracle.encodeSerializedInstanceReference(type)
          : serializationOracle.getSerializedTypeName(type);

        srcWriter.print("\"" + typeString + "\":");

        // Make a JSON array
        srcWriter.println("[");
        {
          srcWriter.indent();
          {
            // First the initialization method
            JMethod instantiationMethod = serializationOracle.getCustomFieldSerializerInstantiateMethodForType(type);

            JClassType customSerializer = serializationOracle.hasCustomFieldSerializer(type);
            if (customSerializer != null && instantiationMethod == null) {
              JMethod[] methods = customSerializer.getMethods();
              for (int i = 0; i < methods.length; ++i) {
                if (CustomFieldSerializerValidator.isValidInstantiateMethod(
                  methods[i], type)) {
                  instantiationMethod = methods[i];
                  break;
                }
              }
            }

            srcWriter.print("function(x){return ");
            if (instantiationMethod != null) {
              srcWriter.print("@"
                + instantiationMethod.getEnclosingType().getQualifiedSourceName());
              srcWriter.print("::");
              srcWriter.print(instantiationMethod.getName());
            } else {
              srcWriter.print("@"
                + serializationOracle.getTypeSerializerQualifiedName(getServiceInterface()));
              srcWriter.print("::");
              srcWriter.print(getInstantiationMethodName(type));
            }
            srcWriter.print("(L"
              + SerializationStreamReader.class.getName().replace('.', '/')
              + ";)");
            srcWriter.print("(x);}");
            srcWriter.println(",");
          }

          String jsniSignature = normalizeJSNIInstanceSerializationMethodSignature(type);
          String serializerName = serializationOracle.getFieldSerializerName(type);
          {
            // Now the deserialization method
            srcWriter.print("function(x,y){");
            srcWriter.print("@" + serializerName);
            srcWriter.print("::deserialize(L"
              + SerializationStreamReader.class.getName().replace('.', '/')
              + ";" + jsniSignature + ")");
            srcWriter.print("(x,y);}");
            srcWriter.println(",");
          }
          {
            // Now the serialization method
            srcWriter.print("function(x,y){");
            srcWriter.print("@" + serializerName);
            srcWriter.print("::serialize(L"
              + SerializationStreamWriter.class.getName().replace('.', '/')
              + ";" + jsniSignature + ")");
            srcWriter.print("(x,y);}");
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

  protected void writeCreateSignatureMapMethod() {
    srcWriter.println("private static native JavaScriptObject createSignatureMap() /*-" + '{');
    {
      srcWriter.indent();
      srcWriter.println("return {");
      JType[] types = getSerializableTypes();
      boolean needComma = false;
      for (int index = 0; index < types.length; ++index) {
        JType type = types[index];
        if (isAbstractType(type)) {
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

  protected void writeDeserializeMethod() {
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

  protected void writeGetSerializationSignatureMethod() {
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
      srcWriter.println("if (!signature) {");
      srcWriter.indentln("@" + serializerTypeName
        + "::raiseSerializationException(Ljava/lang/String;)(typeName);");
      srcWriter.println("}");
      srcWriter.println("return signature;");
      srcWriter.outdent();
      srcWriter.println("}-*/;");
    }
    srcWriter.println();
  }

  protected void writeInstantiateMethod() {
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

  protected void writeRaiseSerializationException() {
    srcWriter.println("private static void raiseSerializationException(String msg) throws SerializationException {");
    srcWriter.indentln("throw new SerializationException(msg);");
    srcWriter.println("}");
    srcWriter.println();
  }

  protected void writeSerializeMethod() {
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

  private String buildArrayInstantiationExpression(JArrayType array) {
    String expression = "[rank]";
    JType componentType = array.getComponentType();
    while (true) {
      array = componentType.isArray();
      if (array == null) {
        break;
      }

      expression += "[0]";

      componentType = array.getComponentType();
    }

    expression = componentType.getQualifiedSourceName() + expression;

    return expression;
  }

  /*
   * Create a field serializer for a type if it does not have a custom
   * serializer.
   */
  private String createFieldSerializer(TreeLogger logger, GeneratorContext ctx,
      JType type) {
    assert (type != null);
    assert (serializationOracle.isSerializable(type));

    JParameterizedType parameterizedType = type.isParameterized();
    if (parameterizedType != null) {
      return createFieldSerializer(logger, ctx, parameterizedType.getRawType());
    }

    TypeOracle typeOracle = ctx.getTypeOracle();
    assert (typeOracle != null);

    JClassType customFieldSerializer = serializationOracle.hasCustomFieldSerializer(type);

    if (customFieldSerializer != null) {
      return customFieldSerializer.getQualifiedSourceName();
    }

    /*
     * Only a JClassType can reach this point in the code. JPrimitives have been
     * removed because their serialization is built in, interfaces have been
     * removed because they are not an instantiable type, JArrays have custom
     * field serializers, and parameterized types have been broken down into
     * their raw types.
     */
    assert (type.isClass() != null);
    FieldSerializerCreator creator = new FieldSerializerCreator(
        serializationOracle, type.isClass());

    return creator.realize(logger, ctx);
  }

  /*
   * Create all of the necessary field serializers.
   */
  private String createFieldSerializers(TreeLogger logger, GeneratorContext ctx) {
    JType[] types = getSerializableTypes();
    int typeCount = types.length;
    String fieldSerializerNames = "";
    for (int typeIndex = 0; typeIndex < typeCount; ++typeIndex) {
      JType type = types[typeIndex];
      assert (type != null);

      String fieldSerializerName = createFieldSerializer(logger, ctx, type);
      if (fieldSerializerName != null) {
        fieldSerializerNames += fieldSerializerName + ";";
      }
    }

    return fieldSerializerNames;
  }

  private String getInstantiationMethodName(JType type) {
    JArrayType arrayType = type.isArray();
    if (arrayType != null) {
      JType leafType = arrayType.getLeafType();
      return "create_" + leafType.getQualifiedSourceName().replace('.', '_')
          + "_Array_Rank_" + arrayType.getRank();
    }

    return "create_"
        + serializationOracle.getFieldSerializerName(type).replace('.', '_');
  }

  private JType[] getSerializableTypes() {
    if (serializableTypes == null) {
      serializableTypes = initializeSerializableTypes();
    }

    return serializableTypes;
  }

  private JClassType getServiceInterface() {
    return svcInterface;
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

  private void initializeProperties(TreeLogger logger, GeneratorContext context) {
    PropertyOracle propertyOracle = context.getPropertyOracle();
    try {
      String propVal = propertyOracle.getPropertyValue(logger,
          Shared.RPC_PROP_ENFORCE_TYPE_VERSIONING);
      if (propVal != null && propVal.equals("false")) {
        enforceTypeVersioning = false;
      } else {
        enforceTypeVersioning = true;
      }

      return;
    } catch (BadPropertyValueException e) {
      // Purposely ignored, because we want to enforce RPC versioning if
      // the property is not defined
      //
    }

    enforceTypeVersioning = true;
  }

  private JType[] initializeSerializableTypes() {
    ArrayList list = new ArrayList();

    JType[] types = serializationOracle.getSerializableTypes();
    for (int index = 0; index < types.length; ++index) {
      JType type = types[index];

      // Ignore primitives
      if (type.isPrimitive() != null) {
        continue;
      }

      // Ignore interfaces
      if (type.isInterface() != null) {
        continue;
      }

      // Ignore parameterizedTypes
      if (type.isParameterized() != null) {
        continue;
      }

      list.add(type);
    }

    JType[] serTypes = (JType[]) list.toArray(new JType[list.size()]);

    // Sort the types by name so that generated code will be repeatable.
    //
    Arrays.sort(serTypes, new Comparator() {
      public int compare(Object o1, Object o2) {
        String n1 = ((JType) o1).getQualifiedSourceName();
        String n2 = ((JType) o2).getQualifiedSourceName();
        return n1.compareTo(n2);
      }
    });

    return serTypes;
  }

  private boolean isAbstractType(JType type) {
    JClassType classType = type.isClassOrInterface();
    if (classType != null) {
      if (classType.isAbstract()) {
        return true;
      }
    }

    // Primitives, arrays, and non-abstract classes fall-through to here.
    //
    return false;
  }

  /**
   * Given a type determine what JSNI signature to use in the serialize or
   * deserialize method of a custom serializer.
   * 
   * @param type
   */
  private String normalizeJSNIInstanceSerializationMethodSignature(JType type) {
    String jsniSignature;
    JArrayType arrayType = type.isArray();

    if (arrayType != null) {
      JType componentType = arrayType.getComponentType();
      JPrimitiveType primitiveType = componentType.isPrimitive();
      if (primitiveType != null) {
        jsniSignature = "[" + primitiveType.getJNISignature();
      } else {
        jsniSignature = "[" + "Ljava/lang/Object;";
      }
    } else {
      jsniSignature = type.getJNISignature();
    }

    return jsniSignature;
  }

  private void setServiceInterface(JClassType serviceInterface) {
    assert (serviceInterface != null);
    svcInterface = serviceInterface;
  }

  private boolean shouldEnforceTypeVersioning() {
    return enforceTypeVersioning;
  }

  private void writeArrayInstantiationMethod(JArrayType array) {
    srcWriter.println("int rank = streamReader.readInt();");
    srcWriter.println("return new " + buildArrayInstantiationExpression(array)
        + ";");
  }

  private boolean enforceTypeVersioning;
  private JType[] serializableTypes;
  private SerializableTypeOracle serializationOracle;
  private SourceWriter srcWriter;
  private JClassType svcInterface;
}

// CHECKSTYLE_ON
