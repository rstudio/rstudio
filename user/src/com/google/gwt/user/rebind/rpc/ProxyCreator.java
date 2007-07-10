/*
 * Copyright 2007 Google Inc.
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
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JPackage;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.generator.NameFactory;
import com.google.gwt.user.client.ResponseTextHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.IncompatibleRemoteServiceException;
import com.google.gwt.user.client.rpc.InvocationException;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.rpc.ServiceDefTarget.NoServiceEntryPointSpecifiedException;
import com.google.gwt.user.client.rpc.impl.ClientSerializationStreamReader;
import com.google.gwt.user.client.rpc.impl.ClientSerializationStreamWriter;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;

import java.io.PrintWriter;

/**
 * Creates a client-side proxy for a
 * {@link com.google.gwt.user.client.rpc.RemoteService RemoteService} interface
 * as well as the necessary type and field serializers.
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

  private JClassType serviceIntf;

  public ProxyCreator(JClassType serviceIntf) {
    assert (serviceIntf.isInterface() != null);

    this.serviceIntf = serviceIntf;
  }

  /**
   * Creates the client-side proxy class.
   * 
   * @throws UnableToCompleteException
   */
  public String create(TreeLogger logger, GeneratorContext context)
      throws UnableToCompleteException {
    SourceWriter srcWriter = getSourceWriter(logger, context);
    if (srcWriter == null) {
      return getProxyQualifiedName();
    }

    TypeOracle typeOracle = context.getTypeOracle();
    
    // Make sure that the async and synchronous versions of the RemoteService
    // agree with one another
    //
    RemoteServiceAsyncValidator rsav = new RemoteServiceAsyncValidator(logger,
        typeOracle);
    rsav.validateRemoteServiceAsync(logger, serviceIntf);

    // Determine the set of serializable types
    SerializableTypeOracleBuilder stob = new SerializableTypeOracleBuilder(
        logger, typeOracle);
    SerializableTypeOracle sto = stob.build(context.getPropertyOracle(),
        serviceIntf);

    TypeSerializerCreator tsc = new TypeSerializerCreator(logger, sto, context,
        serviceIntf);
    tsc.realize(logger);

    enforceTypeVersioning = Shared.shouldEnforceTypeVersioning(logger,
        context.getPropertyOracle());

    generateProxyFields(srcWriter, sto);

    generateServiceDefTargetImpl(srcWriter);

    generateProxyMethods(srcWriter, sto);

    srcWriter.commit(logger);

    return getProxyQualifiedName();
  }

  /*
   * Given a type emit an expression for calling the correct
   * SerializationStreamReader method which reads the corresponding instance out
   * of the stream.
   */
  protected final void generateDecodeCall(SourceWriter w, JType type) {
    w.print("streamReader.");
    w.print("read" + Shared.getCallSuffix(type) + "()");
  }

  /*
   * Given a type emit an expression for calling the correct
   * SerializationStreamWriter method which writes that type into the stream.
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

    NameFactory nameFactory = new NameFactory();

    for (int i = 0; i < params.length; ++i) {
      nameFactory.addName(params[i].getName());
    }

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

    String exceptionName = nameFactory.createName("e");
    w.println("} catch (" + SerializationException.class.getName() + " "
        + exceptionName + ") {");
    w.indentln("callback.onFailure(" + exceptionName + ");");
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
          w.println("if (encodedResponse.startsWith(\"//OK\")) {");
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
          w.println("} else if (encodedResponse.startsWith(\"//EX\")) {");
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
        w.println("} catch (" + SerializationException.class.getName()
            + " e) {");
        w.indent();
        {
          w.println("caught = new "
              + IncompatibleRemoteServiceException.class.getName() + "();");
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
      SerializableTypeOracle serializableTypeOracle, JMethod method) {
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
        + serializableTypeOracle.getSerializedTypeName(method.getEnclosingType())
        + "\");");
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
  private void generateProxyFields(SourceWriter srcWriter,
      SerializableTypeOracle serializableTypeOracle) {
    String typeSerializerName = serializableTypeOracle.getTypeSerializerQualifiedName(serviceIntf);
    srcWriter.println("private static final " + typeSerializerName
        + " SERIALIZER = new " + typeSerializerName + "();");
  }

  private void generateProxyMethods(SourceWriter w,
      SerializableTypeOracle serializableTypeOracle) {

    JMethod[] methods = serviceIntf.getOverridableMethods();
    for (int i = 0; i < methods.length; ++i) {
      JMethod method = methods[i];
      generateProxyEncode(w, serializableTypeOracle, method);
      generateAsynchronousProxyMethod(w, method);
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

  private boolean shouldEnforceTypeVersioning() {
    return enforceTypeVersioning;
  }
}
