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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.GWT.UncaughtExceptionHandler;
import com.google.gwt.core.ext.BadPropertyValueException;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.PropertyOracle;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JPackage;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.TypeOracleException;
import com.google.gwt.user.client.ResponseTextHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.InvocationException;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.rpc.ServiceDefTarget.NoServiceEntryPointSpecifiedException;
import com.google.gwt.user.client.rpc.impl.ClientSerializationStreamReader;
import com.google.gwt.user.client.rpc.impl.ClientSerializationStreamWriter;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;

import java.io.PrintWriter;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

/**
 * Creates client-side proxies for remote services.
 */
class ProxyCreator {
  private static final String ENTRY_POINT_TAG = "gwt.defaultEntryPoint";
  private static final String PROXY_SUFFIX = "_Proxy";

  private static final String SERIALIZATION_STREAM_READER_INSTANTIATION = ClientSerializationStreamReader.class.getName()
      + " streamReader = new "
      + ClientSerializationStreamReader.class.getName() + "(SERIALIZER);";

  private static final String SERIALIZATION_STREAM_WRITER_INSTANTIATION = ClientSerializationStreamWriter.class.getName()
      + " streamWriter = new "
      + ClientSerializationStreamWriter.class.getName() + "(SERIALIZER);";

  private static String createOverloadSignature(JMethod method) {
    StringBuffer sb = new StringBuffer();
    sb.append(method.getName());
    sb.append('(');
    JParameter[] params = method.getParameters();
    for (int i = 0; i < params.length; ++i) {
      if (i > 0) {
        sb.append(", ");
      }
      sb.append(params[i].getType().getQualifiedSourceName());
    }
    sb.append(')');
    return sb.toString();
  }

  /*
   * This method returns the real type name. Currently, it only affects
   * JParameterizedType since their names are not legal Java names.
   */
  private static String getJavaTypeName(JType type) {
    JParameterizedType parameterizedType = type.isParameterized();
    if (parameterizedType != null) {
      return parameterizedType.getRawType().getQualifiedSourceName();
    }

    return type.getQualifiedSourceName();
  }

  private boolean enforceTypeVersioning;

  private SerializableTypeOracle serializableTypeOracle;

  private JClassType serviceIntf;

  public ProxyCreator(JClassType serviceIntf,
      SerializableTypeOracle serializableTypeOracle) {
    this.serviceIntf = serviceIntf;
    this.serializableTypeOracle = serializableTypeOracle;

    if (serviceIntf.isInterface() == null) {
      throw new RuntimeException("Expecting a service interface, but "
          + serviceIntf.getQualifiedSourceName() + " is not an interface");
    }
  }

  /**
   * Creates a proxy class for the requested class.
   */
  public String create(TreeLogger logger, GeneratorContext context) {
    assert (isValidServiceInterface(logger, context));

    logger = logger.branch(TreeLogger.SPAM,
        "Generating RPC Proxy for service interface '"
            + serviceIntf.getQualifiedSourceName() + "'", null);
    SourceWriter srcWriter = getSourceWriter(logger, context);
    if (srcWriter == null) {
      return getProxyQualifiedName();
    }

    initializeProperties(logger, context);

    generateProxyFields(srcWriter);

    generateServiceDefTargetImpl(srcWriter);

    generateProxyMethods(srcWriter);

    srcWriter.commit(logger);

    return getProxyQualifiedName();
  }

  /*
   * Give a type emit an expression for deserializing that type from a
   * serialization stream.
   */
  protected final void generateDecodeCall(SourceWriter w, JType type) {
    w.print("streamReader.");
    w.print("read" + Shared.getCallSuffix(type) + "()");
  }

  /*
   * Give a type emit an expression for serializing that type into a
   * serialization stream.
   */
  protected void generateEncodeCall(SourceWriter w, JParameter parameter) {
    JType paramType = parameter.getType();
    w.print("streamWriter.");
    w.print("write" + Shared.getCallSuffix(paramType));
    w.println("(" + parameter.getName() + ");");
  }

  /*
   * Calls the __ version to encode.
   */
  private void generateAsynchronousProxyMethod(SourceWriter w, JMethod method) {
    JType returnType = method.getReturnType();
    JParameter[] params = method.getParameters();

    w.println();
    w.print("public void " + method.getName() + "(");
    int i;
    for (i = 0; i < params.length; i++) {
      JParameter param = params[i];
      w.print((i > 0 ? ", " : "") + getJavaTypeName(param.getType()) + " "
          + param.getName());
    }

    w.println((i > 0 ? ", final " : "final ") + AsyncCallback.class.getName()
        + " callback) {");
    w.indent();
    w.println("final " + SERIALIZATION_STREAM_READER_INSTANTIATION);
    w.println("final " + SERIALIZATION_STREAM_WRITER_INSTANTIATION);
    w.println("try {");
    w.indent();
    {
      w.print("__" + method.getName() + "(streamWriter");
      for (i = 0; i < params.length; i++) {
        w.print(", " + params[i].getName());
      }
      w.println(");");
    }
    w.outdent();
    w.println("} catch (" + SerializationException.class.getName() + " e) {");
    w.indentln("callback.onFailure(new " + InvocationException.class.getName()
        + "(e.getMessage()));");
    w.indentln("return;");
    w.println("}");

    // Generate the async response handler.
    //
    w.println(ResponseTextHandler.class.getName() + " handler = new "
        + ResponseTextHandler.class.getName() + "() {");
    w.indent();
    {
      w.println("public final void onCompletion(String encodedResponse) {");
      w.indent();
      {
        w.println("UncaughtExceptionHandler handler = GWT.getUncaughtExceptionHandler();");
        w.println("if (handler != null)");
        w.indent();
        {
          w.println("onCompletionAndCatch(encodedResponse, handler);");
        }
        w.outdent();
        w.println("else");
        w.indent();
        {
          w.println("onCompletionImpl(encodedResponse);");
        }
        w.outdent();
      }
      w.outdent();
      w.println("}");

      w.println("private void onCompletionAndCatch(String encodedResponse, UncaughtExceptionHandler handler) {");
      w.indent();
      {
        w.println("try {");
        w.indent();
        {
          w.println("onCompletionImpl(encodedResponse);");
        }
        w.outdent();
        w.println("} catch (Throwable e) {");
        w.indent();
        {
          w.println("handler.onUncaughtException(e);");
        }
        w.outdent();
        w.println("}");
      }
      w.outdent();
      w.println("}");

      w.println("private void onCompletionImpl(String encodedResponse) {");
      w.indent();
      {
        w.println("Object result = null;");
        w.println("Throwable caught = null;");
        w.println("try {");
        w.indent();
        {
          w.println("if (encodedResponse.startsWith(\"{OK}\")) {");
          w.indent();
          {
            w.println("streamReader.prepareToRead(encodedResponse.substring(4));");
            w.print("result = ");

            JPrimitiveType primitive = returnType.isPrimitive();
            if (primitive == JPrimitiveType.VOID) {
              w.print("null");
            } else {
              if (primitive != null) {
                w.print("new ");
                w.print(getObjectWrapperName(primitive));
                w.print("(");
                generateDecodeCall(w, returnType);
                w.print(")");
              } else {
                generateDecodeCall(w, returnType);
              }
            }
            w.println(";");
          }
          w.outdent();
          w.println("} else if (encodedResponse.startsWith(\"{EX}\")) {");
          w.indent();
          {
            w.println("streamReader.prepareToRead(encodedResponse.substring(4));");
            w.println("caught = (Throwable) streamReader.readObject();");
          }
          w.outdent();
          w.println("} else {");
          w.indent();
          {
            w.println("caught = new " + InvocationException.class.getName()
                + "(encodedResponse);");
          }
          w.outdent();
          w.println("}");
        }
        w.outdent();
        w.println("} catch (Throwable e) {");
        w.indent();
        {
          w.println("caught = e;");
        }
        w.outdent();
        w.println("}");

        w.println("if (caught == null)");
        w.indent();
        {
          w.println("callback.onSuccess(result);");
        }
        w.outdent();
        w.println("else");
        w.indent();
        {
          w.println("callback.onFailure(caught);");
        }
        w.outdent();
      }
      w.outdent();
      w.println("}");
    }
    w.outdent();
    w.println("};");

    // Make the asynchronous invocation.
    //
    w.println("if (!com.google.gwt.user.client.HTTPRequest.asyncPost(getServiceEntryPoint(), streamWriter.toString(), handler))");
    w.indentln("callback.onFailure(new "
        + InvocationException.class.getName()
        + "(\"Unable to initiate the asynchronous service invocation -- check the network connection\"));");
    w.outdent();

    w.println("}");
  }

  /**
   * Generate the code that addresses the service.
   */
  private void generateProxyEncode(SourceWriter w,
      SerializableTypeOracle serializableTypeOracle, JClassType serviceIntf,
      JMethod method) {
    String methodName = method.getName();
    JParameter[] params = method.getParameters();
    w.println();
    w.print("private void __" + methodName + "("
        + ClientSerializationStreamWriter.class.getName() + " streamWriter");
    for (int i = 0; i < params.length; i++) {
      JParameter param = params[i];
      w.print(", " + getJavaTypeName(param.getType()) + " " + param.getName());
    }

    w.println(") throws " + SerializationException.class.getName() + " {");
    w.indent();

    // Make sure that we have a service def class name specified.
    //
    w.println("if (getServiceEntryPoint() == null)");
    String className = NoServiceEntryPointSpecifiedException.class.getName();
    className = className.replace('$', '.');
    w.indentln("throw new " + className + "();");

    // Generate code to describe just enough meta data for the server to locate
    // the service definition class and resolve the method overload.
    //
    w.println("streamWriter.prepareToWrite();");

    if (!shouldEnforceTypeVersioning()) {
      w.println("streamWriter.addFlags("
          + ClientSerializationStreamReader.class.getName()
          + ".SERIALIZATION_STREAM_FLAGS_NO_TYPE_VERSIONING);");
    }
    w.println("streamWriter.writeString(\""
        + serializableTypeOracle.getSerializedTypeName(serviceIntf) + "\");");
    w.println("streamWriter.writeString(\"" + methodName + "\");");
    w.println("streamWriter.writeInt(" + params.length + ");");
    for (int i = 0; i < params.length; ++i) {
      JParameter param = params[i];
      w.println("streamWriter.writeString(\""
          + serializableTypeOracle.getSerializedTypeName(param.getType())
          + "\");");
    }

    // Encode the arguments.
    //
    for (int i = 0; i < params.length; i++) {
      JParameter param = params[i];
      generateEncodeCall(w, param);
    }

    w.outdent();
    w.println("}");
  }

  /**
   * Generate any fields required by the proxy.
   */
  private void generateProxyFields(SourceWriter srcWriter) {
    String typeSerializerName = serializableTypeOracle.getTypeSerializerQualifiedName(serviceIntf);
    srcWriter.println("private static final " + typeSerializerName
        + " SERIALIZER = new " + typeSerializerName + "();");
  }

  /**
   * 
   * @param ctx
   * @param w
   */
  private void generateProxyMethods(SourceWriter w) {
    Set seenMethods = new HashSet();
    LinkedList workList = new LinkedList();
    workList.addLast(serviceIntf);
    while (!workList.isEmpty()) {
      JClassType curIntf = (JClassType) workList.removeFirst();
      JMethod[] methods = curIntf.getMethods();
      for (int index = 0; index < methods.length; ++index) {
        JMethod method = methods[index];
        assert (method != null);
        String signature = createOverloadSignature(method);
        if (!seenMethods.contains(signature)) {
          seenMethods.add(signature);
          generateProxyEncode(w, serializableTypeOracle, curIntf, method);
          generateAsynchronousProxyMethod(w, method);
        }
      }
      JClassType[] interfaces = curIntf.getImplementedInterfaces();
      for (int index = 0; index < interfaces.length; ++index) {
        workList.addLast(interfaces[index]);
      }
    }
  }

  /**
   * Implements the ServiceDefTarget interface to allow clients to switch which
   * back-end service definition we send calls to.
   */
  private void generateServiceDefTargetImpl(SourceWriter w) {
    String serverDefName = getDefaultServiceDefName();
    if (serverDefName != null) {
      serverDefName = "\"" + serverDefName + "\"";
    } else {
      serverDefName = "null";
    }

    w.println();
    w.println("String fServiceEntryPoint = " + serverDefName + ";");
    w.println();
    w.println("public String getServiceEntryPoint() { return fServiceEntryPoint; }");
    w.println();
    w.println("public void setServiceEntryPoint(String s) { fServiceEntryPoint = s; }");
  }

  private String getAsyncIntfQualifiedName() {
    String asyncIntf = serviceIntf.getQualifiedSourceName() + "Async";
    return asyncIntf;
  }

  /**
   * 
   */
  private String getDefaultServiceDefName() {
    String[][] metaData = serviceIntf.getMetaData(ENTRY_POINT_TAG);
    if (metaData.length == 0) {
      return null;
    }
    return serviceIntf.getMetaData(ENTRY_POINT_TAG)[0][0];
  }

  /*
   * Determine the name of the object wrapper class to instantiate based on the
   * the type of the primitive.
   */
  private String getObjectWrapperName(JPrimitiveType primitive) {
    if (primitive == JPrimitiveType.INT) {
      return "Integer";
    } else if (primitive == JPrimitiveType.CHAR) {
      return "Character";
    }

    return Shared.capitalize(primitive.getSimpleSourceName());
  }

  private String getPackageName() {
    JPackage pkg = serviceIntf.getPackage();
    if (pkg != null) {
      return pkg.getName();
    }

    return "";
  }

  private String getProxyQualifiedName() {
    String[] name = Shared.synthesizeTopLevelClassName(serviceIntf,
        PROXY_SUFFIX);
    return name[0].length() == 0 ? name[1] : name[0] + "." + name[1];
  }

  private String getProxySimpleName() {
    String[] name = Shared.synthesizeTopLevelClassName(serviceIntf,
        PROXY_SUFFIX);
    return name[1];
  }

  private SourceWriter getSourceWriter(TreeLogger logger, GeneratorContext ctx) {
    PrintWriter printWriter = ctx.tryCreate(logger, getPackageName(),
        getProxySimpleName());
    if (printWriter == null) {
      return null;
    }

    ClassSourceFileComposerFactory composerFactory = new ClassSourceFileComposerFactory(
        getPackageName(), getProxySimpleName());

    composerFactory.addImport(GWT.class.getName());
    String className = UncaughtExceptionHandler.class.getName();
    className = className.replace('$', '.');
    composerFactory.addImport(className);

    composerFactory.addImplementedInterface(ServiceDefTarget.class.getName());
    composerFactory.addImplementedInterface(getAsyncIntfQualifiedName());

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

  /**
   * This is really a test method that allows us to assert at the start of the
   * create method that we are in fact dealing with a valid service interface.
   * If you are running with assertions diabled, this code should not be called.
   */
  private boolean isValidServiceInterface(TreeLogger logger,
      GeneratorContext ctx) {
    assert (serviceIntf != null);
    assert (serializableTypeOracle != null);

    ServiceInterfaceValidator siv = new ServiceInterfaceValidator(logger, ctx,
        serializableTypeOracle, serviceIntf);

    try {
      return siv.isValid();
    } catch (TypeOracleException e) {
      logger.branch(TreeLogger.ERROR, "TypeOracleException: ", e);
      // Purposely ignored
      return false;
    }
  }

  private boolean shouldEnforceTypeVersioning() {
    return enforceTypeVersioning;
  }
}
