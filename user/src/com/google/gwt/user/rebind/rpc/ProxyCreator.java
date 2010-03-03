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
import com.google.gwt.core.client.impl.Impl;
import com.google.gwt.core.ext.BadPropertyValueException;
import com.google.gwt.core.ext.ConfigurationProperty;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.PropertyOracle;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.GeneratedResource;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JPackage;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.generator.NameFactory;
import com.google.gwt.dev.javac.TypeOracleMediator;
import com.google.gwt.dev.util.Util;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.user.client.rpc.IncompatibleRemoteServiceException;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import com.google.gwt.user.client.rpc.impl.ClientSerializationStreamWriter;
import com.google.gwt.user.client.rpc.impl.FailedRequest;
import com.google.gwt.user.client.rpc.impl.FailingRequestBuilder;
import com.google.gwt.user.client.rpc.impl.RemoteServiceProxy;
import com.google.gwt.user.client.rpc.impl.RequestCallbackAdapter.ResponseReader;
import com.google.gwt.user.linker.rpc.RpcLogArtifact;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;
import com.google.gwt.user.server.rpc.SerializationPolicyLoader;
import com.google.gwt.user.server.rpc.impl.TypeNameObfuscator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Creates a client-side proxy for a
 * {@link com.google.gwt.user.client.rpc.RemoteService RemoteService} interface
 * as well as the necessary type and field serializers.
 */
public class ProxyCreator {
  /**
   * The directory within which RPC manifests are placed for individual
   * permutations.
   */
  public static final String MANIFEST_ARTIFACT_DIR = "rpcPolicyManifest/manifests";

  private static final Map<JPrimitiveType, ResponseReader> JPRIMITIVETYPE_TO_RESPONSEREADER = new HashMap<JPrimitiveType, ResponseReader>();

  private static final String PROXY_SUFFIX = "_Proxy";

  /**
   * Adds a root type for each type that appears in the RemoteService interface
   * methods.
   */
  private static void addRemoteServiceRootTypes(TreeLogger logger,
      TypeOracle typeOracle,
      SerializableTypeOracleBuilder typesSentFromBrowser,
      SerializableTypeOracleBuilder typesSentToBrowser, JClassType remoteService)
      throws NotFoundException {
    logger = logger.branch(TreeLogger.DEBUG, "Analyzing '"
        + remoteService.getParameterizedQualifiedSourceName()
        + "' for serializable types", null);

    JMethod[] methods = remoteService.getOverridableMethods();

    JClassType exceptionClass = typeOracle.getType(Exception.class.getName());

    TreeLogger validationLogger = logger.branch(TreeLogger.DEBUG,
        "Analyzing methods:", null);
    for (JMethod method : methods) {
      TreeLogger methodLogger = validationLogger.branch(TreeLogger.DEBUG,
          method.toString(), null);
      JType returnType = method.getReturnType();
      if (returnType != JPrimitiveType.VOID) {
        TreeLogger returnTypeLogger = methodLogger.branch(TreeLogger.DEBUG,
            "Return type: " + returnType.getParameterizedQualifiedSourceName(),
            null);
        typesSentToBrowser.addRootType(returnTypeLogger, returnType);
      }

      JParameter[] params = method.getParameters();
      for (JParameter param : params) {
        TreeLogger paramLogger = methodLogger.branch(TreeLogger.DEBUG,
            "Parameter: " + param.toString(), null);
        JType paramType = param.getType();
        typesSentFromBrowser.addRootType(paramLogger, paramType);
      }

      JType[] exs = method.getThrows();
      if (exs.length > 0) {
        TreeLogger throwsLogger = methodLogger.branch(TreeLogger.DEBUG,
            "Throws:", null);
        for (JType ex : exs) {
          if (!exceptionClass.isAssignableFrom(ex.isClass())) {
            throwsLogger = throwsLogger.branch(
                TreeLogger.WARN,
                "'"
                    + ex.getQualifiedSourceName()
                    + "' is not a checked exception; only checked exceptions may be used",
                null);
          }

          typesSentToBrowser.addRootType(throwsLogger, ex);
        }
      }
    }
  }

  /**
   * Add the implicit root types that are needed to make RPC work. These would
   * be {@link String} and {@link IncompatibleRemoteServiceException}.
   */
  private static void addRequiredRoots(TreeLogger logger,
      TypeOracle typeOracle, SerializableTypeOracleBuilder stob)
      throws NotFoundException {
    logger = logger.branch(TreeLogger.DEBUG, "Analyzing implicit types");

    // String is always instantiable.
    JClassType stringType = typeOracle.getType(String.class.getName());
    stob.addRootType(logger, stringType);

    // IncompatibleRemoteServiceException is always serializable
    JClassType icseType = typeOracle.getType(IncompatibleRemoteServiceException.class.getName());
    stob.addRootType(logger, icseType);
  }

  /**
   * Take the union of two type arrays, and then sort the results
   * alphabetically.
   */
  private static JType[] unionOfTypeArrays(JType[]... types) {
    Set<JType> typesList = new HashSet<JType>();
    for (JType[] a : types) {
      typesList.addAll(Arrays.asList(a));
    }
    JType[] serializableTypes = typesList.toArray(new JType[0]);
    Arrays.sort(serializableTypes,
        SerializableTypeOracleBuilder.JTYPE_COMPARATOR);
    return serializableTypes;
  }

  protected JClassType serviceIntf;

  private boolean elideTypeNames;

  /**
   * The possibly obfuscated type signatures used to represent a type.
   */
  private Map<JType, String> typeStrings;

  {
    JPRIMITIVETYPE_TO_RESPONSEREADER.put(JPrimitiveType.BOOLEAN,
        ResponseReader.BOOLEAN);
    JPRIMITIVETYPE_TO_RESPONSEREADER.put(JPrimitiveType.BYTE,
        ResponseReader.BYTE);
    JPRIMITIVETYPE_TO_RESPONSEREADER.put(JPrimitiveType.CHAR,
        ResponseReader.CHAR);
    JPRIMITIVETYPE_TO_RESPONSEREADER.put(JPrimitiveType.DOUBLE,
        ResponseReader.DOUBLE);
    JPRIMITIVETYPE_TO_RESPONSEREADER.put(JPrimitiveType.FLOAT,
        ResponseReader.FLOAT);
    JPRIMITIVETYPE_TO_RESPONSEREADER.put(JPrimitiveType.INT, ResponseReader.INT);
    JPRIMITIVETYPE_TO_RESPONSEREADER.put(JPrimitiveType.LONG,
        ResponseReader.LONG);
    JPRIMITIVETYPE_TO_RESPONSEREADER.put(JPrimitiveType.SHORT,
        ResponseReader.SHORT);
    JPRIMITIVETYPE_TO_RESPONSEREADER.put(JPrimitiveType.VOID,
        ResponseReader.VOID);
  }

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
    TypeOracle typeOracle = context.getTypeOracle();

    JClassType serviceAsync = typeOracle.findType(serviceIntf.getQualifiedSourceName()
        + "Async");
    if (serviceAsync == null) {
      logger.branch(TreeLogger.ERROR,
          "Could not find an asynchronous version for the service interface "
              + serviceIntf.getQualifiedSourceName(), null);
      RemoteServiceAsyncValidator.logValidAsyncInterfaceDeclaration(logger,
          serviceIntf);
      throw new UnableToCompleteException();
    }

    SourceWriter srcWriter = getSourceWriter(logger, context, serviceAsync);
    if (srcWriter == null) {
      return getProxyQualifiedName();
    }

    // Make sure that the async and synchronous versions of the RemoteService
    // agree with one another
    //
    RemoteServiceAsyncValidator rsav = new RemoteServiceAsyncValidator(logger,
        typeOracle);
    Map<JMethod, JMethod> syncMethToAsyncMethMap = rsav.validate(logger,
        serviceIntf, serviceAsync);

    final PropertyOracle propertyOracle = context.getPropertyOracle();

    // Load the blacklist/whitelist
    TypeFilter blacklistTypeFilter = new BlacklistTypeFilter(logger,
        propertyOracle);

    // Determine the set of serializable types
    SerializableTypeOracleBuilder typesSentFromBrowserBuilder = new SerializableTypeOracleBuilder(
        logger, propertyOracle, typeOracle);
    typesSentFromBrowserBuilder.setTypeFilter(blacklistTypeFilter);
    SerializableTypeOracleBuilder typesSentToBrowserBuilder = new SerializableTypeOracleBuilder(
        logger, propertyOracle, typeOracle);
    typesSentToBrowserBuilder.setTypeFilter(blacklistTypeFilter);

    addRoots(logger, typeOracle, typesSentFromBrowserBuilder,
        typesSentToBrowserBuilder);

    try {
      ConfigurationProperty prop = context.getPropertyOracle().getConfigurationProperty(
          TypeSerializerCreator.GWT_ELIDE_TYPE_NAMES_FROM_RPC);
      elideTypeNames = Boolean.parseBoolean(prop.getValues().get(0));
    } catch (BadPropertyValueException e) {
      logger.log(TreeLogger.ERROR, "Configuration property "
          + TypeSerializerCreator.GWT_ELIDE_TYPE_NAMES_FROM_RPC
          + " is not defined. Is RemoteService.gwt.xml inherited?");
      throw new UnableToCompleteException();
    }

    // Decide what types to send in each direction.
    // Log the decisions to a string that will be written later in this method
    SerializableTypeOracle typesSentFromBrowser;
    SerializableTypeOracle typesSentToBrowser;
    String rpcLog;
    {
      StringWriter stringWriter = new StringWriter();
      PrintWriter writer = new PrintWriter(stringWriter);

      typesSentFromBrowserBuilder.setLogOutputWriter(writer);
      typesSentToBrowserBuilder.setLogOutputWriter(writer);

      writer.write("====================================\n");
      writer.write("Types potentially sent from browser:\n");
      writer.write("====================================\n\n");
      writer.flush();
      typesSentFromBrowser = typesSentFromBrowserBuilder.build(logger);

      writer.write("===================================\n");
      writer.write("Types potentially sent from server:\n");
      writer.write("===================================\n\n");
      writer.flush();
      typesSentToBrowser = typesSentToBrowserBuilder.build(logger);

      writer.close();
      rpcLog = stringWriter.toString();
    }

    generateTypeHandlers(logger, context, typesSentFromBrowser,
        typesSentToBrowser);

    String serializationPolicyStrongName = writeSerializationPolicyFile(logger,
        context, typesSentFromBrowser, typesSentToBrowser);

    String remoteServiceInterfaceName = elideTypeNames
        ? TypeNameObfuscator.SERVICE_INTERFACE_ID
        : TypeOracleMediator.computeBinaryClassName(serviceIntf);
    generateProxyFields(srcWriter, typesSentFromBrowser,
        serializationPolicyStrongName, remoteServiceInterfaceName);

    generateProxyContructor(srcWriter);

    generateProxyMethods(srcWriter, typesSentFromBrowser,
        syncMethToAsyncMethMap);

    if (elideTypeNames) {
      generateStreamWriterOverride(srcWriter);
    }

    srcWriter.commit(logger);

    // Create an artifact explaining STOB's decisions. It will be emitted by
    // RpcLogLinker
    context.commitArtifact(logger, new RpcLogArtifact(
        serviceIntf.getQualifiedSourceName(), serializationPolicyStrongName,
        rpcLog));

    return getProxyQualifiedName();
  }

  protected void addRoots(TreeLogger logger, TypeOracle typeOracle,
      SerializableTypeOracleBuilder typesSentFromBrowserBuilder,
      SerializableTypeOracleBuilder typesSentToBrowserBuilder)
      throws UnableToCompleteException {
    try {
      addRequiredRoots(logger, typeOracle, typesSentFromBrowserBuilder);
      addRequiredRoots(logger, typeOracle, typesSentToBrowserBuilder);

      addRemoteServiceRootTypes(logger, typeOracle,
          typesSentFromBrowserBuilder, typesSentToBrowserBuilder, serviceIntf);
    } catch (NotFoundException e) {
      logger.log(TreeLogger.ERROR,
          "Unable to find type referenced from remote service", e);
      throw new UnableToCompleteException();
    }
  }

  protected String computeTypeNameExpression(JType paramType) {
    String typeName;
    if (typeStrings.containsKey(paramType)) {
      typeName = typeStrings.get(paramType);
    } else {
      typeName = TypeOracleMediator.computeBinaryClassName(paramType);
    }
    return typeName == null ? null : ('"' + typeName + '"');
  }

  /**
   * Generate the proxy constructor and delegate to the superclass constructor
   * using the default address for the
   * {@link com.google.gwt.user.client.rpc.RemoteService RemoteService}.
   */
  protected void generateProxyContructor(SourceWriter srcWriter) {
    srcWriter.println("public " + getProxySimpleName() + "() {");
    srcWriter.indent();
    srcWriter.println("super(GWT.getModuleBaseURL(),");
    srcWriter.indent();
    srcWriter.println(getRemoteServiceRelativePath() + ", ");
    srcWriter.println("SERIALIZATION_POLICY, ");
    srcWriter.println("SERIALIZER);");
    srcWriter.outdent();
    srcWriter.outdent();
    srcWriter.println("}");
  }

  /**
   * Generate any fields required by the proxy.
   */
  protected void generateProxyFields(SourceWriter srcWriter,
      SerializableTypeOracle serializableTypeOracle,
      String serializationPolicyStrongName, String remoteServiceInterfaceName) {
    // Initialize a field with binary name of the remote service interface
    srcWriter.println("private static final String REMOTE_SERVICE_INTERFACE_NAME = "
        + "\"" + remoteServiceInterfaceName + "\";");
    srcWriter.println("private static final String SERIALIZATION_POLICY =\""
        + serializationPolicyStrongName + "\";");
    String typeSerializerName = SerializationUtils.getTypeSerializerQualifiedName(serviceIntf);
    srcWriter.println("private static final " + typeSerializerName
        + " SERIALIZER = new " + typeSerializerName + "();");
    srcWriter.println();
  }

  /**
   * Generates the client's asynchronous proxy method.
   */
  protected void generateProxyMethod(SourceWriter w,
      SerializableTypeOracle serializableTypeOracle, JMethod syncMethod,
      JMethod asyncMethod) {

    w.println();

    // Write the method signature
    JType asyncReturnType = asyncMethod.getReturnType().getErasedType();
    w.print("public ");
    w.print(asyncReturnType.getQualifiedSourceName());
    w.print(" ");
    w.print(asyncMethod.getName() + "(");

    boolean needsComma = false;
    NameFactory nameFactory = new NameFactory();
    JParameter[] asyncParams = asyncMethod.getParameters();
    for (int i = 0; i < asyncParams.length; ++i) {
      JParameter param = asyncParams[i];

      if (needsComma) {
        w.print(", ");
      } else {
        needsComma = true;
      }

      /*
       * Ignoring the AsyncCallback parameter, if any method requires a call to
       * SerializationStreamWriter.writeObject we need a try catch block
       */
      JType paramType = param.getType();
      paramType = paramType.getErasedType();

      w.print(paramType.getQualifiedSourceName());
      w.print(" ");

      String paramName = param.getName();
      nameFactory.addName(paramName);
      w.print(paramName);
    }

    w.println(") {");
    w.indent();

    String requestIdName = nameFactory.createName("requestId");
    w.println("int " + requestIdName + " = getNextRequestId();");

    String statsMethodExpr = getProxySimpleName() + "." + syncMethod.getName();
    String tossName = nameFactory.createName("toss");
    w.println("boolean " + tossName + " = isStatsAvailable() && stats("
        + "timeStat(\"" + statsMethodExpr + "\", " + requestIdName
        + ", \"begin\"));");

    w.print(SerializationStreamWriter.class.getSimpleName());
    w.print(" ");
    String streamWriterName = nameFactory.createName("streamWriter");
    w.println(streamWriterName + " = createStreamWriter();");
    w.println("// createStreamWriter() prepared the stream");
    w.println("try {");
    w.indent();

    w.println(streamWriterName + ".writeString(REMOTE_SERVICE_INTERFACE_NAME);");

    // Write the method name
    w.println(streamWriterName + ".writeString(\"" + syncMethod.getName()
        + "\");");

    // Write the parameter count followed by the parameter values
    JParameter[] syncParams = syncMethod.getParameters();
    w.println(streamWriterName + ".writeInt(" + syncParams.length + ");");
    for (JParameter param : syncParams) {
      JType paramType = param.getType().getErasedType();
      String typeNameExpression = computeTypeNameExpression(paramType);
      assert typeNameExpression != null : "Could not compute a type name for "
          + paramType.getQualifiedSourceName();
      w.println(streamWriterName + ".writeString(" + typeNameExpression + ");");
    }

    // Encode all of the arguments to the asynchronous method, but exclude the
    // last argument which is the callback instance.
    //
    for (int i = 0; i < asyncParams.length - 1; ++i) {
      JParameter asyncParam = asyncParams[i];
      w.print(streamWriterName + ".");
      w.print(Shared.getStreamWriteMethodNameFor(asyncParam.getType()));
      w.println("(" + asyncParam.getName() + ");");
    }

    String payloadName = nameFactory.createName("payload");
    w.println("String " + payloadName + " = " + streamWriterName
        + ".toString();");

    w.println(tossName + " = isStatsAvailable() && stats(" + "timeStat(\""
        + statsMethodExpr + "\", " + requestIdName
        + ", \"requestSerialized\"));");

    /*
     * Depending on the return type for the async method, return a
     * RequestBuilder, a Request, or nothing at all.
     */
    if (asyncReturnType == JPrimitiveType.VOID) {
      w.print("doInvoke(");
    } else if (asyncReturnType.getQualifiedSourceName().equals(
        RequestBuilder.class.getName())) {
      w.print("return doPrepareRequestBuilder(");
    } else if (asyncReturnType.getQualifiedSourceName().equals(
        Request.class.getName())) {
      w.print("return doInvoke(");
    } else {
      // This method should have been caught by RemoteServiceAsyncValidator
      throw new RuntimeException("Unhandled return type "
          + asyncReturnType.getQualifiedSourceName());
    }

    JParameter callbackParam = asyncParams[asyncParams.length - 1];
    String callbackName = callbackParam.getName();
    JType returnType = syncMethod.getReturnType();
    w.print("ResponseReader." + getResponseReaderFor(returnType).name());
    w.println(", \"" + getProxySimpleName() + "." + syncMethod.getName()
        + "\", " + requestIdName + ", " + payloadName + ", " + callbackName
        + ");");

    w.outdent();
    w.print("} catch (SerializationException ");
    String exceptionName = nameFactory.createName("ex");
    w.println(exceptionName + ") {");
    w.indent();
    if (!asyncReturnType.getQualifiedSourceName().equals(
        RequestBuilder.class.getName())) {
      /*
       * If the method returns void or Request, signal the serialization error
       * immediately. If the method returns RequestBuilder, the error will be
       * signaled whenever RequestBuilder.send() is invoked.
       */
      w.println(callbackName + ".onFailure(" + exceptionName + ");");
    }
    if (asyncReturnType.getQualifiedSourceName().equals(
        RequestBuilder.class.getName())) {
      w.println("return new " + FailingRequestBuilder.class.getName() + "("
          + exceptionName + ", " + callbackName + ");");
    } else if (asyncReturnType.getQualifiedSourceName().equals(
        Request.class.getName())) {
      w.println("return new " + FailedRequest.class.getName() + "();");
    } else {
      assert asyncReturnType == JPrimitiveType.VOID;
    }
    w.outdent();
    w.println("}");

    w.outdent();
    w.println("}");
  }

  protected void generateProxyMethods(SourceWriter w,
      SerializableTypeOracle serializableTypeOracle,
      Map<JMethod, JMethod> syncMethToAsyncMethMap) {
    JMethod[] syncMethods = serviceIntf.getOverridableMethods();
    for (JMethod syncMethod : syncMethods) {

      JMethod asyncMethod = syncMethToAsyncMethMap.get(syncMethod);
      assert (asyncMethod != null);

      JClassType enclosingType = syncMethod.getEnclosingType();
      JParameterizedType isParameterizedType = enclosingType.isParameterized();
      if (isParameterizedType != null) {
        JMethod[] methods = isParameterizedType.getMethods();
        for (int i = 0; i < methods.length; ++i) {
          if (methods[i] == syncMethod) {
            /*
             * Use the generic version of the method to ensure that the server
             * can find the method using the erasure of the generic signature.
             */
            syncMethod = isParameterizedType.getBaseType().getMethods()[i];
          }
        }
      }

      generateProxyMethod(w, serializableTypeOracle, syncMethod, asyncMethod);
    }
  }

  protected void generateStreamWriterOverride(SourceWriter srcWriter) {
    srcWriter.println("@Override");
    srcWriter.println("public SerializationStreamWriter createStreamWriter() {");
    srcWriter.indent();
    /*
     * Need an explicit cast since we've widened the declaration of the method
     * in RemoteServiceProxy.
     */
    srcWriter.println("ClientSerializationStreamWriter toReturn =");
    srcWriter.indentln("(ClientSerializationStreamWriter) super.createStreamWriter();");
    srcWriter.println("toReturn.addFlags(ClientSerializationStreamWriter.FLAG_ELIDE_TYPE_NAMES);");
    srcWriter.println("return toReturn;");
    srcWriter.outdent();
    srcWriter.println("}");
  }

  protected void generateTypeHandlers(TreeLogger logger,
      GeneratorContext context, SerializableTypeOracle typesSentFromBrowser,
      SerializableTypeOracle typesSentToBrowser)
      throws UnableToCompleteException {
    TypeSerializerCreator tsc = new TypeSerializerCreator(logger,
        typesSentFromBrowser, typesSentToBrowser, context,
        SerializationUtils.getTypeSerializerQualifiedName(serviceIntf));
    tsc.realize(logger);

    typeStrings = new HashMap<JType, String>(tsc.getTypeStrings());
    typeStrings.put(serviceIntf, TypeNameObfuscator.SERVICE_INTERFACE_ID);
  }

  protected String getProxySimpleName() {
    String[] name = Shared.synthesizeTopLevelClassName(serviceIntf,
        PROXY_SUFFIX);
    return name[1];
  }

  protected Class<? extends RemoteServiceProxy> getProxySupertype() {
    return RemoteServiceProxy.class;
  }

  protected String getRemoteServiceRelativePath() {
    RemoteServiceRelativePath moduleRelativeURL = serviceIntf.getAnnotation(RemoteServiceRelativePath.class);
    if (moduleRelativeURL != null) {
      return "\"" + moduleRelativeURL.value() + "\"";
    }

    return null;
  }

  protected Class<? extends SerializationStreamWriter> getStreamWriterClass() {
    return ClientSerializationStreamWriter.class;
  }

  protected String writeSerializationPolicyFile(TreeLogger logger,
      GeneratorContext ctx, SerializableTypeOracle serializationSto,
      SerializableTypeOracle deserializationSto)
      throws UnableToCompleteException {
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      OutputStreamWriter osw = new OutputStreamWriter(baos,
          SerializationPolicyLoader.SERIALIZATION_POLICY_FILE_ENCODING);
      TypeOracle oracle = ctx.getTypeOracle();
      PrintWriter pw = new PrintWriter(osw);

      JType[] serializableTypes = unionOfTypeArrays(
          serializationSto.getSerializableTypes(),
          deserializationSto.getSerializableTypes(), new JType[] {serviceIntf});

      for (int i = 0; i < serializableTypes.length; ++i) {
        JType type = serializableTypes[i];
        String binaryTypeName = TypeOracleMediator.computeBinaryClassName(type);
        pw.print(binaryTypeName);
        pw.print(", "
            + Boolean.toString(deserializationSto.isSerializable(type)));
        pw.print(", "
            + Boolean.toString(deserializationSto.maybeInstantiated(type)));
        pw.print(", " + Boolean.toString(serializationSto.isSerializable(type)));
        pw.print(", "
            + Boolean.toString(serializationSto.maybeInstantiated(type)));
        pw.print(", " + typeStrings.get(type));

        /*
         * Include the serialization signature to bump the RPC file name if
         * obfuscated identifiers are used.
         */
        pw.print(", "
            + SerializationUtils.getSerializationSignature(oracle, type));
        pw.print('\n');

        /*
         * Emit client-side field information for classes that may be enhanced
         * on the server. Each line consists of a comma-separated list
         * containing the keyword '@ClientFields', the class name, and a list of
         * all potentially serializable client-visible fields.
         */
        if ((type instanceof JClassType) && ((JClassType) type).isEnhanced()) {
          JField[] fields = ((JClassType) type).getFields();
          JField[] rpcFields = new JField[fields.length];
          int numRpcFields = 0;
          for (JField f : fields) {
            if (f.isTransient() || f.isStatic() || f.isFinal()) {
              continue;
            }
            rpcFields[numRpcFields++] = f;
          }

          pw.print(SerializationPolicyLoader.CLIENT_FIELDS_KEYWORD);
          pw.print(',');
          pw.print(binaryTypeName);
          for (int idx = 0; idx < numRpcFields; idx++) {
            pw.print(',');
            pw.print(rpcFields[idx].getName());
          }
          pw.print('\n');
        }
      }

      // Closes the wrapped streams.
      pw.close();

      byte[] serializationPolicyFileContents = baos.toByteArray();
      String serializationPolicyName = Util.computeStrongName(serializationPolicyFileContents);

      String serializationPolicyFileName = SerializationPolicyLoader.getSerializationPolicyFileName(serializationPolicyName);
      OutputStream os = ctx.tryCreateResource(logger,
          serializationPolicyFileName);
      if (os != null) {
        os.write(serializationPolicyFileContents);
        GeneratedResource resource = ctx.commitResource(logger, os);

        /*
         * Record which proxy class created the resource. A manifest will be
         * emitted by the RpcPolicyManifestLinker.
         */
        emitPolicyFileArtifact(logger, ctx, resource.getPartialPath());
      } else {
        logger.log(TreeLogger.TRACE,
            "SerializationPolicy file for RemoteService '"
                + serviceIntf.getQualifiedSourceName()
                + "' already exists; no need to rewrite it.", null);
      }

      return serializationPolicyName;
    } catch (UnsupportedEncodingException e) {
      logger.log(TreeLogger.ERROR,
          SerializationPolicyLoader.SERIALIZATION_POLICY_FILE_ENCODING
              + " is not supported", e);
      throw new UnableToCompleteException();
    } catch (IOException e) {
      logger.log(TreeLogger.ERROR, null, e);
      throw new UnableToCompleteException();
    }
  }

  private void emitPolicyFileArtifact(TreeLogger logger,
      GeneratorContext context, String partialPath)
      throws UnableToCompleteException {
    try {
      String qualifiedSourceName = serviceIntf.getQualifiedSourceName();
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      Writer writer;
      writer = new OutputStreamWriter(baos,
          SerializationPolicyLoader.SERIALIZATION_POLICY_FILE_ENCODING);
      writer.write("serviceClass: " + qualifiedSourceName + "\n");
      writer.write("path: " + partialPath + "\n");
      writer.close();

      byte[] manifestBytes = baos.toByteArray();
      String md5 = Util.computeStrongName(manifestBytes);
      OutputStream os = context.tryCreateResource(logger, MANIFEST_ARTIFACT_DIR
          + "/" + md5 + ".txt");
      os.write(manifestBytes);

      GeneratedResource resource = context.commitResource(logger, os);
      resource.setPrivate(true);
    } catch (UnsupportedEncodingException e) {
      logger.log(TreeLogger.ERROR,
          SerializationPolicyLoader.SERIALIZATION_POLICY_FILE_ENCODING
              + " is not supported", e);
      throw new UnableToCompleteException();
    } catch (IOException e) {
      logger.log(TreeLogger.ERROR, null, e);
      throw new UnableToCompleteException();
    }
  }

  private String getProxyQualifiedName() {
    String[] name = Shared.synthesizeTopLevelClassName(serviceIntf,
        PROXY_SUFFIX);
    return name[0].length() == 0 ? name[1] : name[0] + "." + name[1];
  }

  private ResponseReader getResponseReaderFor(JType returnType) {
    if (returnType.isPrimitive() != null) {
      return JPRIMITIVETYPE_TO_RESPONSEREADER.get(returnType.isPrimitive());
    }

    if (returnType.getQualifiedSourceName().equals(
        String.class.getCanonicalName())) {
      return ResponseReader.STRING;
    }

    return ResponseReader.OBJECT;
  }

  private SourceWriter getSourceWriter(TreeLogger logger, GeneratorContext ctx,
      JClassType serviceAsync) {
    JPackage serviceIntfPkg = serviceAsync.getPackage();
    String packageName = serviceIntfPkg == null ? "" : serviceIntfPkg.getName();
    PrintWriter printWriter = ctx.tryCreate(logger, packageName,
        getProxySimpleName());
    if (printWriter == null) {
      return null;
    }

    ClassSourceFileComposerFactory composerFactory = new ClassSourceFileComposerFactory(
        packageName, getProxySimpleName());

    String[] imports = new String[] {
        getProxySupertype().getCanonicalName(),
        getStreamWriterClass().getCanonicalName(),
        SerializationStreamWriter.class.getCanonicalName(),
        GWT.class.getCanonicalName(), ResponseReader.class.getCanonicalName(),
        SerializationException.class.getCanonicalName(),
        Impl.class.getCanonicalName()};
    for (String imp : imports) {
      composerFactory.addImport(imp);
    }

    composerFactory.setSuperclass(getProxySupertype().getSimpleName());
    composerFactory.addImplementedInterface(serviceAsync.getErasedType().getQualifiedSourceName());

    return composerFactory.createSourceWriter(ctx, printWriter);
  }
}
