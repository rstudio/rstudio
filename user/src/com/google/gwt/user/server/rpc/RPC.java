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
package com.google.gwt.user.server.rpc;

import com.google.gwt.user.client.rpc.IncompatibleRemoteServiceException;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RpcToken;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.impl.AbstractSerializationStream;
import com.google.gwt.user.server.rpc.impl.DequeMap;
import com.google.gwt.user.server.rpc.impl.LegacySerializationPolicy;
import com.google.gwt.user.server.rpc.impl.SerializabilityUtil;
import com.google.gwt.user.server.rpc.impl.ServerSerializationStreamReader;
import com.google.gwt.user.server.rpc.impl.ServerSerializationStreamWriter;
import com.google.gwt.user.server.rpc.impl.TypeNameObfuscator;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Utility class for integrating with the RPC system. This class exposes methods
 * for decoding of RPC requests, encoding of RPC responses, and invocation of
 * RPC calls on service objects. The operations exposed by this class can be
 * reused by framework implementors such as Spring and G4jsf to support a wide
 * range of service invocation policies.
 * 
 * <h3>Canonical Example</h3> The following example demonstrates the canonical
 * way to use this class.
 * 
 * {@example com.google.gwt.examples.rpc.server.CanonicalExample#processCall(String)}
 * 
 * <h3>Advanced Example</h3> The following example shows a more advanced way of
 * using this class to create an adapter between GWT RPC entities and POJOs.
 * 
 * {@example com.google.gwt.examples.rpc.server.AdvancedExample#doPost}
 */
public final class RPC {

  /**
   * Maps primitive wrapper classes to their corresponding primitive class.
   */
  private static final Map<Class<?>, Class<?>> PRIMITIVE_WRAPPER_CLASS_TO_PRIMITIVE_CLASS =
      new HashMap<Class<?>, Class<?>>();

  /**
   * Static map of classes to sets of interfaces (e.g. classes). Optimizes
   * lookup of interfaces for security.
   */
  private static Map<Class<?>, Set<String>> serviceToImplementedInterfacesMap;

  private static final HashMap<String, Class<?>> TYPE_NAMES;

  static {
    PRIMITIVE_WRAPPER_CLASS_TO_PRIMITIVE_CLASS.put(Boolean.class, Boolean.TYPE);
    PRIMITIVE_WRAPPER_CLASS_TO_PRIMITIVE_CLASS.put(Byte.class, Byte.TYPE);
    PRIMITIVE_WRAPPER_CLASS_TO_PRIMITIVE_CLASS.put(Character.class, Character.TYPE);
    PRIMITIVE_WRAPPER_CLASS_TO_PRIMITIVE_CLASS.put(Double.class, Double.TYPE);
    PRIMITIVE_WRAPPER_CLASS_TO_PRIMITIVE_CLASS.put(Float.class, Float.TYPE);
    PRIMITIVE_WRAPPER_CLASS_TO_PRIMITIVE_CLASS.put(Integer.class, Integer.TYPE);
    PRIMITIVE_WRAPPER_CLASS_TO_PRIMITIVE_CLASS.put(Long.class, Long.TYPE);
    PRIMITIVE_WRAPPER_CLASS_TO_PRIMITIVE_CLASS.put(Short.class, Short.TYPE);

    TYPE_NAMES = new HashMap<String, Class<?>>();
    TYPE_NAMES.put("Z", boolean.class);
    TYPE_NAMES.put("B", byte.class);
    TYPE_NAMES.put("C", char.class);
    TYPE_NAMES.put("D", double.class);
    TYPE_NAMES.put("F", float.class);
    TYPE_NAMES.put("I", int.class);
    TYPE_NAMES.put("J", long.class);
    TYPE_NAMES.put("S", short.class);

    serviceToImplementedInterfacesMap = new HashMap<Class<?>, Set<String>>();
  }

  /**
   * Returns an {@link RPCRequest} that is built by decoding the contents of an
   * encoded RPC request.
   * 
   * <p>
   * This method is equivalent to calling {@link #decodeRequest(String, Class)}
   * with <code>null</code> for the type parameter.
   * </p>
   * 
   * @param encodedRequest a string that encodes the {@link RemoteService}
   *          interface, the service method to call, and the arguments to for
   *          the service method
   * @return an {@link RPCRequest} instance
   * 
   * @throws IncompatibleRemoteServiceException if any of the following
   *           conditions apply:
   *           <ul>
   *           <li>if the types in the encoded request cannot be deserialized</li>
   *           <li>if the {@link ClassLoader} acquired from
   *           <code>Thread.currentThread().getContextClassLoader()</code>
   *           cannot load the service interface or any of the types specified
   *           in the encodedRequest</li>
   *           <li>the requested interface is not assignable to
   *           {@link RemoteService}</li>
   *           <li>the service method requested in the encodedRequest is not a
   *           member of the requested service interface</li>
   *           <li>the type parameter is not <code>null</code> and is not
   *           assignable to the requested {@link RemoteService} interface
   *           </ul>
   */
  public static RPCRequest decodeRequest(String encodedRequest) {
    return decodeRequest(encodedRequest, null);
  }

  /**
   * Returns an {@link RPCRequest} that is built by decoding the contents of an
   * encoded RPC request and optionally validating that type can handle the
   * request. If the type parameter is not <code>null</code>, the implementation
   * checks that the type is assignable to the {@link RemoteService} interface
   * requested in the encoded request string.
   * 
   * <p>
   * Invoking this method with <code>null</code> for the type parameter,
   * <code>decodeRequest(encodedRequest, null)</code>, is equivalent to calling
   * <code>decodeRequest(encodedRequest)</code>.
   * </p>
   * 
   * @param encodedRequest a string that encodes the {@link RemoteService}
   *          interface, the service method, and the arguments to pass to the
   *          service method
   * @param type if not <code>null</code>, the implementation checks that the
   *          type is assignable to the {@link RemoteService} interface encoded
   *          in the encoded request string.
   * @return an {@link RPCRequest} instance
   * 
   * @throws NullPointerException if the encodedRequest is <code>null</code>
   * @throws IllegalArgumentException if the encodedRequest is an empty string
   * @throws IncompatibleRemoteServiceException if any of the following
   *           conditions apply:
   *           <ul>
   *           <li>if the types in the encoded request cannot be deserialized</li>
   *           <li>if the {@link ClassLoader} acquired from
   *           <code>Thread.currentThread().getContextClassLoader()</code>
   *           cannot load the service interface or any of the types specified
   *           in the encodedRequest</li>
   *           <li>the requested interface is not assignable to
   *           {@link RemoteService}</li>
   *           <li>the service method requested in the encodedRequest is not a
   *           member of the requested service interface</li>
   *           <li>the type parameter is not <code>null</code> and is not
   *           assignable to the requested {@link RemoteService} interface
   *           </ul>
   */
  public static RPCRequest decodeRequest(String encodedRequest, Class<?> type) {
    return decodeRequest(encodedRequest, type, null);
  }

  /**
   * Returns an {@link RPCRequest} that is built by decoding the contents of an
   * encoded RPC request and optionally validating that type can handle the
   * request. If the type parameter is not <code>null</code>, the implementation
   * checks that the type is assignable to the {@link RemoteService} interface
   * requested in the encoded request string.
   * 
   * <p>
   * If the serializationPolicyProvider parameter is not <code>null</code>, it
   * is asked for a {@link SerializationPolicy} to use to restrict the set of
   * types that can be decoded from the request. If this parameter is
   * <code>null</code>, then only subtypes of
   * {@link com.google.gwt.user.client.rpc.IsSerializable IsSerializable} or
   * types which have custom field serializers can be decoded.
   * </p>
   * 
   * <p>
   * Invoking this method with <code>null</code> for the type parameter,
   * <code>decodeRequest(encodedRequest, null)</code>, is equivalent to calling
   * <code>decodeRequest(encodedRequest)</code>.
   * </p>
   * 
   * @param encodedRequest a string that encodes the {@link RemoteService}
   *          interface, the service method, and the arguments to pass to the
   *          service method
   * @param type if not <code>null</code>, the implementation checks that the
   *          type is assignable to the {@link RemoteService} interface encoded
   *          in the encoded request string.
   * @param serializationPolicyProvider if not <code>null</code>, the
   *          implementation asks this provider for a
   *          {@link SerializationPolicy} which will be used to restrict the set
   *          of types that can be decoded from this request
   * @return an {@link RPCRequest} instance
   * 
   * @throws NullPointerException if the encodedRequest is <code>null</code>
   * @throws IllegalArgumentException if the encodedRequest is an empty string
   * @throws IncompatibleRemoteServiceException if any of the following
   *           conditions apply:
   *           <ul>
   *           <li>if the types in the encoded request cannot be deserialized</li>
   *           <li>if the {@link ClassLoader} acquired from
   *           <code>Thread.currentThread().getContextClassLoader()</code>
   *           cannot load the service interface or any of the types specified
   *           in the encodedRequest</li>
   *           <li>the requested interface is not assignable to
   *           {@link RemoteService}</li>
   *           <li>the service method requested in the encodedRequest is not a
   *           member of the requested service interface</li>
   *           <li>the type parameter is not <code>null</code> and is not
   *           assignable to the requested {@link RemoteService} interface
   *           </ul>
   */
  public static RPCRequest decodeRequest(String encodedRequest, Class<?> type,
      SerializationPolicyProvider serializationPolicyProvider) {
    if (encodedRequest == null) {
      throw new NullPointerException("encodedRequest cannot be null");
    }

    if (encodedRequest.length() == 0) {
      throw new IllegalArgumentException("encodedRequest cannot be empty");
    }

    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

    try {
      ServerSerializationStreamReader streamReader =
          new ServerSerializationStreamReader(classLoader, serializationPolicyProvider);
      streamReader.prepareToRead(encodedRequest);

      RpcToken rpcToken = null;
      if (streamReader.hasFlags(AbstractSerializationStream.FLAG_RPC_TOKEN_INCLUDED)) {
        // Read the RPC token
        rpcToken = (RpcToken) streamReader.deserializeValue(RpcToken.class);
      }

      // Read the name of the RemoteService interface
      String serviceIntfName = maybeDeobfuscate(streamReader, streamReader.readString());

      if (type != null) {
        if (!implementsInterface(type, serviceIntfName)) {
          // The service does not implement the requested interface
          throw new IncompatibleRemoteServiceException("Blocked attempt to access interface '"
              + serviceIntfName + "', which is not implemented by '" + printTypeName(type)
              + "'; this is either misconfiguration or a hack attempt");
        }
      }

      SerializationPolicy serializationPolicy = streamReader.getSerializationPolicy();
      Class<?> serviceIntf;
      try {
        serviceIntf = getClassFromSerializedName(serviceIntfName, classLoader);
        if (!RemoteService.class.isAssignableFrom(serviceIntf)) {
          // The requested interface is not a RemoteService interface
          throw new IncompatibleRemoteServiceException(
              "Blocked attempt to access interface '"
                  + printTypeName(serviceIntf)
                  + "', which doesn't extend RemoteService; this is either "
                  + "misconfiguration or a hack attempt");
        }
      } catch (ClassNotFoundException e) {
        throw new IncompatibleRemoteServiceException("Could not locate requested interface '"
            + serviceIntfName + "' in default classloader", e);
      }

      String serviceMethodName = streamReader.readString();

      int paramCount = streamReader.readInt();
      if (paramCount > streamReader.getNumberOfTokens()) {
        throw new IncompatibleRemoteServiceException("Invalid number of parameters");
      }
      Class<?>[] parameterTypes = new Class[paramCount];

      for (int i = 0; i < parameterTypes.length; i++) {
        String paramClassName = maybeDeobfuscate(streamReader, streamReader.readString());

        try {
          parameterTypes[i] = getClassFromSerializedName(paramClassName, classLoader);
        } catch (ClassNotFoundException e) {
          throw new IncompatibleRemoteServiceException("Parameter " + i
              + " of is of an unknown type '" + paramClassName + "'", e);
        }
      }

      try {
        Method method = serviceIntf.getMethod(serviceMethodName, parameterTypes);

        // The parameter types we have are the non-parameterized versions in the
        // RPC stream. For stronger message verification, get the parameterized
        // types from the method declaration.
        Type[] methodParameterTypes = method.getGenericParameterTypes();
        DequeMap<TypeVariable<?>, Type> resolvedTypes = new DequeMap<TypeVariable<?>, Type>();

        TypeVariable<Method>[] methodTypes = method.getTypeParameters();
        for (TypeVariable<Method> methodType : methodTypes) {
          SerializabilityUtil.resolveTypes(methodType, resolvedTypes);
        }

        Object[] parameterValues = new Object[parameterTypes.length];
        for (int i = 0; i < parameterValues.length; i++) {
          parameterValues[i] = streamReader.deserializeValue(parameterTypes[i],
              methodParameterTypes[i], resolvedTypes);
        }

        return new RPCRequest(method, parameterValues, rpcToken, serializationPolicy, streamReader
            .getFlags());
      } catch (NoSuchMethodException e) {
        throw new IncompatibleRemoteServiceException(formatMethodNotFoundErrorMessage(serviceIntf,
            serviceMethodName, parameterTypes));
      }
    } catch (SerializationException ex) {
      throw new IncompatibleRemoteServiceException(ex.getMessage(), ex);
    }
  }

  /**
   * Returns a string that encodes an exception. If <code>rpcRequest</code>
   * is <code>null</code> a default serialization policy and default request
   * flags will be used. Otherwise these information are taken from
   * <code>rpcRequest</code>.
   * <p>
   * This method should be used if the RPC request could not be decoded or
   * could not be executed because of an exception thrown, e.g.
   * {@link IncompatibleRemoteServiceException}, {@link RpcTokenException}
   * </p>
   * @param rpcRequest the RPCRequest that failed to execute, may be null
   * @param cause the {@link Throwable} that was thrown
   * @return a String that encodes the exception
   * @throws SerializationException if the result cannot be serialized
   */
  public static String encodeResponseForFailedRequest(RPCRequest rpcRequest, Throwable cause)
      throws SerializationException {
    if (rpcRequest == null) {
      return RPC.encodeResponseForFailure(null, cause,
          getDefaultSerializationPolicy(), AbstractSerializationStream.DEFAULT_FLAGS);
    } else {
      return RPC.encodeResponseForFailure(null, cause,
          rpcRequest.getSerializationPolicy(), rpcRequest.getFlags());
    }
  }

  /**
   * Returns a string that encodes an exception. If method is not
   * <code>null</code>, it is an error if the exception is not in the method's
   * list of checked exceptions.
   * 
   * @param serviceMethod the method that threw the exception, may be
   *          <code>null</code>
   * @param cause the {@link Throwable} that was thrown
   * @return a string that encodes the exception
   * 
   * @throws NullPointerException if the cause is <code>null</code>
   * @throws SerializationException if the result cannot be serialized
   * @throws UnexpectedException if the result was an unexpected exception (a
   *           checked exception not declared in the serviceMethod's signature)
   */
  public static String encodeResponseForFailure(Method serviceMethod, Throwable cause)
      throws SerializationException {
    return encodeResponseForFailure(serviceMethod, cause, getDefaultSerializationPolicy());
  }

  /**
   * Returns a string that encodes an exception. If method is not
   * <code>null</code>, it is an error if the exception is not in the method's
   * list of checked exceptions.
   * 
   * <p>
   * If the serializationPolicy parameter is not <code>null</code>, it is used
   * to determine what types can be encoded as part of this response. If this
   * parameter is <code>null</code>, then only subtypes of
   * {@link com.google.gwt.user.client.rpc.IsSerializable IsSerializable} or
   * types which have custom field serializers may be encoded.
   * </p>
   * 
   * @param serviceMethod the method that threw the exception, may be
   *          <code>null</code>
   * @param cause the {@link Throwable} that was thrown
   * @param serializationPolicy determines the serialization policy to be used
   * @return a string that encodes the exception
   * 
   * @throws NullPointerException if the cause or the serializationPolicy
   *           are <code>null</code>
   * @throws SerializationException if the result cannot be serialized
   * @throws UnexpectedException if the result was an unexpected exception (a
   *           checked exception not declared in the serviceMethod's signature)
   */
  public static String encodeResponseForFailure(Method serviceMethod, Throwable cause,
      SerializationPolicy serializationPolicy) throws SerializationException {
    return encodeResponseForFailure(serviceMethod, cause, serializationPolicy,
        AbstractSerializationStream.DEFAULT_FLAGS);
  }

  public static String encodeResponseForFailure(Method serviceMethod, Throwable cause,
      SerializationPolicy serializationPolicy, int flags) throws SerializationException {
    if (cause == null) {
      throw new NullPointerException("cause cannot be null");
    }

    if (serializationPolicy == null) {
      throw new NullPointerException("serializationPolicy");
    }

    if (serviceMethod != null && !RPCServletUtils.isExpectedException(serviceMethod, cause)) {
      throw new UnexpectedException("Service method '" + getSourceRepresentation(serviceMethod)
          + "' threw an unexpected exception: " + cause.toString(), cause);
    }

    return encodeResponse(cause.getClass(), cause, true, flags, serializationPolicy);
  }

  /**
   * Returns a string that encodes the object. It is an error to try to encode
   * an object that is not assignable to the service method's return type.
   * 
   * @param serviceMethod the method whose result we are encoding
   * @param object the instance that we wish to encode
   * @return a string that encodes the object, if the object is compatible with
   *         the service method's declared return type
   * 
   * @throws IllegalArgumentException if the result is not assignable to the
   *           service method's return type
   * @throws NullPointerException if the service method is <code>null</code>
   * @throws SerializationException if the result cannot be serialized
   */
  public static String encodeResponseForSuccess(Method serviceMethod, Object object)
      throws SerializationException {
    return encodeResponseForSuccess(serviceMethod, object, getDefaultSerializationPolicy());
  }

  /**
   * Returns a string that encodes the object. It is an error to try to encode
   * an object that is not assignable to the service method's return type.
   * 
   * <p>
   * If the serializationPolicy parameter is not <code>null</code>, it is used
   * to determine what types can be encoded as part of this response. If this
   * parameter is <code>null</code>, then only subtypes of
   * {@link com.google.gwt.user.client.rpc.IsSerializable IsSerializable} or
   * types which have custom field serializers may be encoded.
   * </p>
   * 
   * @param serviceMethod the method whose result we are encoding
   * @param object the instance that we wish to encode
   * @param serializationPolicy determines the serialization policy to be used
   * @return a string that encodes the object, if the object is compatible with
   *         the service method's declared return type
   * 
   * @throws IllegalArgumentException if the result is not assignable to the
   *           service method's return type
   * @throws NullPointerException if the serviceMethod or the
   *           serializationPolicy are <code>null</code>
   * @throws SerializationException if the result cannot be serialized
   */
  public static String encodeResponseForSuccess(Method serviceMethod, Object object,
      SerializationPolicy serializationPolicy) throws SerializationException {
    return encodeResponseForSuccess(serviceMethod, object, serializationPolicy,
        AbstractSerializationStream.DEFAULT_FLAGS);
  }

  public static String encodeResponseForSuccess(Method serviceMethod, Object object,
      SerializationPolicy serializationPolicy, int flags) throws SerializationException {
    if (serviceMethod == null) {
      throw new NullPointerException("serviceMethod cannot be null");
    }

    if (serializationPolicy == null) {
      throw new NullPointerException("serializationPolicy");
    }

    Class<?> methodReturnType = serviceMethod.getReturnType();
    if (methodReturnType != void.class && object != null) {
      Class<?> actualReturnType;
      if (methodReturnType.isPrimitive()) {
        actualReturnType = getPrimitiveClassFromWrapper(object.getClass());
      } else {
        actualReturnType = object.getClass();
      }

      if (actualReturnType == null || !methodReturnType.isAssignableFrom(actualReturnType)) {
        throw new IllegalArgumentException("Type '" + printTypeName(object.getClass())
            + "' does not match the return type in the method's signature: '"
            + getSourceRepresentation(serviceMethod) + "'");
      }
    }

    return encodeResponse(methodReturnType, object, false, flags, serializationPolicy);
  }

  /**
   * Returns a default serialization policy.
   * 
   * @return the default serialization policy.
   */
  public static SerializationPolicy getDefaultSerializationPolicy() {
    return LegacySerializationPolicy.getInstance();
  }

  /**
   * Returns a string that encodes the result of calling a service method, which
   * could be the value returned by the method or an exception thrown by it.
   * 
   * <p>
   * This method does no security checking; security checking must be done on
   * the method prior to this invocation.
   * </p>
   * 
   * @param target instance on which to invoke the serviceMethod
   * @param serviceMethod the method to invoke
   * @param args arguments used for the method invocation
   * @return a string which encodes either the method's return or a checked
   *         exception thrown by the method
   * 
   * @throws SecurityException if the method cannot be accessed or if the number
   *           or type of actual and formal arguments differ
   * @throws SerializationException if an object could not be serialized by the
   *           stream
   * @throws UnexpectedException if the serviceMethod throws a checked exception
   *           that is not declared in its signature
   */
  public static String invokeAndEncodeResponse(Object target, Method serviceMethod, Object[] args)
      throws SerializationException {
    return invokeAndEncodeResponse(target, serviceMethod, args, getDefaultSerializationPolicy());
  }

  /**
   * Returns a string that encodes the result of calling a service method, which
   * could be the value returned by the method or an exception thrown by it.
   * 
   * <p>
   * If the serializationPolicy parameter is not <code>null</code>, it is used
   * to determine what types can be encoded as part of this response. If this
   * parameter is <code>null</code>, then only subtypes of
   * {@link com.google.gwt.user.client.rpc.IsSerializable IsSerializable} or
   * types which have custom field serializers may be encoded.
   * </p>
   * 
   * <p>
   * This method does no security checking; security checking must be done on
   * the method prior to this invocation.
   * </p>
   * 
   * @param target instance on which to invoke the serviceMethod
   * @param serviceMethod the method to invoke
   * @param args arguments used for the method invocation
   * @param serializationPolicy determines the serialization policy to be used
   * @return a string which encodes either the method's return or a checked
   *         exception thrown by the method
   * 
   * @throws NullPointerException if the serviceMethod or the
   *           serializationPolicy are <code>null</code>
   * @throws SecurityException if the method cannot be accessed or if the number
   *           or type of actual and formal arguments differ
   * @throws SerializationException if an object could not be serialized by the
   *           stream
   * @throws UnexpectedException if the serviceMethod throws a checked exception
   *           that is not declared in its signature
   */
  public static String invokeAndEncodeResponse(Object target, Method serviceMethod, Object[] args,
      SerializationPolicy serializationPolicy) throws SerializationException {
    return invokeAndEncodeResponse(target, serviceMethod, args, serializationPolicy,
        AbstractSerializationStream.DEFAULT_FLAGS);
  }

  public static String invokeAndEncodeResponse(Object target, Method serviceMethod, Object[] args,
      SerializationPolicy serializationPolicy, int flags) throws SerializationException {
    if (serviceMethod == null) {
      throw new NullPointerException("serviceMethod");
    }

    if (serializationPolicy == null) {
      throw new NullPointerException("serializationPolicy");
    }

    String responsePayload;
    try {
      Object result = serviceMethod.invoke(target, args);

      responsePayload = encodeResponseForSuccess(serviceMethod, result, serializationPolicy, flags);
    } catch (IllegalAccessException e) {
      SecurityException securityException =
          new SecurityException(formatIllegalAccessErrorMessage(target, serviceMethod));
      securityException.initCause(e);
      throw securityException;
    } catch (IllegalArgumentException e) {
      SecurityException securityException =
          new SecurityException(formatIllegalArgumentErrorMessage(target, serviceMethod, args));
      securityException.initCause(e);
      throw securityException;
    } catch (InvocationTargetException e) {
      // Try to encode the caught exception
      //
      Throwable cause = e.getCause();

      responsePayload = encodeResponseForFailure(serviceMethod, cause, serializationPolicy, flags);
    }

    return responsePayload;
  }

  /**
   * Returns a string that encodes the results of an RPC call. Private overload
   * that takes a flag signaling the preamble of the response payload.
   * 
   * @param object the object that we wish to send back to the client
   * @param wasThrown if true, the object being returned was an exception thrown
   *          by the service method; if false, it was the result of the service
   *          method's invocation
   * @return a string that encodes the response from a service method
   * @throws SerializationException if the object cannot be serialized
   */
  private static String encodeResponse(Class<?> responseClass, Object object, boolean wasThrown,
      int flags, SerializationPolicy serializationPolicy) throws SerializationException {

    ServerSerializationStreamWriter stream =
        new ServerSerializationStreamWriter(serializationPolicy);
    stream.setFlags(flags);

    stream.prepareToWrite();
    if (responseClass != void.class) {
      stream.serializeValue(object, responseClass);
    }

    String bufferStr = (wasThrown ? "//EX" : "//OK") + stream.toString();
    return bufferStr;
  }

  private static String formatIllegalAccessErrorMessage(Object target, Method serviceMethod) {
    StringBuffer sb = new StringBuffer();
    sb.append("Blocked attempt to access inaccessible method '");
    sb.append(getSourceRepresentation(serviceMethod));
    sb.append("'");

    if (target != null) {
      sb.append(" on target '");
      sb.append(printTypeName(target.getClass()));
      sb.append("'");
    }

    sb.append("; this is either misconfiguration or a hack attempt");

    return sb.toString();
  }

  private static String formatIllegalArgumentErrorMessage(Object target, Method serviceMethod,
      Object[] args) {
    StringBuffer sb = new StringBuffer();
    sb.append("Blocked attempt to invoke method '");
    sb.append(getSourceRepresentation(serviceMethod));
    sb.append("'");

    if (target != null) {
      sb.append(" on target '");
      sb.append(printTypeName(target.getClass()));
      sb.append("'");
    }

    sb.append(" with invalid arguments");

    if (args != null && args.length > 0) {
      sb.append(Arrays.asList(args));
    }

    return sb.toString();
  }

  private static String formatMethodNotFoundErrorMessage(Class<?> serviceIntf,
      String serviceMethodName, Class<?>[] parameterTypes) {
    StringBuffer sb = new StringBuffer();

    sb.append("Could not locate requested method '");
    sb.append(serviceMethodName);
    sb.append("(");
    for (int i = 0; i < parameterTypes.length; ++i) {
      if (i > 0) {
        sb.append(", ");
      }
      sb.append(printTypeName(parameterTypes[i]));
    }
    sb.append(")'");

    sb.append(" in interface '");
    sb.append(printTypeName(serviceIntf));
    sb.append("'");

    return sb.toString();
  }

  /**
   * Returns the {@link Class} instance for the named class or primitive type.
   * 
   * @param serializedName the serialized name of a class or primitive type
   * @param classLoader the classLoader used to load {@link Class}es
   * @return Class instance for the given type name
   * @throws ClassNotFoundException if the named type was not found
   */
  private static Class<?> getClassFromSerializedName(String serializedName, ClassLoader classLoader)
      throws ClassNotFoundException {
    Class<?> value = TYPE_NAMES.get(serializedName);
    if (value != null) {
      return value;
    }

    return Class.forName(serializedName, false, classLoader);
  }

  /**
   * Returns the {@link java.lang.Class Class} for a primitive type given its
   * corresponding wrapper {@link java.lang.Class Class}.
   * 
   * @param wrapperClass primitive wrapper class
   * @return primitive class
   */
  private static Class<?> getPrimitiveClassFromWrapper(Class<?> wrapperClass) {
    return PRIMITIVE_WRAPPER_CLASS_TO_PRIMITIVE_CLASS.get(wrapperClass);
  }

  /**
   * Returns the source representation for a method signature.
   * 
   * @param method method to get the source signature for
   * @return source representation for a method signature
   */
  private static String getSourceRepresentation(Method method) {
    return method.toString().replace('$', '.');
  }

  /**
   * Used to determine whether the specified interface name is implemented by
   * the service class. This is done without loading the class (for security).
   */
  private static boolean implementsInterface(Class<?> service, String intfName) {
    synchronized (serviceToImplementedInterfacesMap) {
      // See if it's cached.
      //
      Set<String> interfaceSet = serviceToImplementedInterfacesMap.get(service);
      if (interfaceSet != null) {
        if (interfaceSet.contains(intfName)) {
          return true;
        }
      } else {
        interfaceSet = new HashSet<String>();
        serviceToImplementedInterfacesMap.put(service, interfaceSet);
      }

      if (!service.isInterface()) {
        while ((service != null) && !RemoteServiceServlet.class.equals(service)) {
          Class<?>[] intfs = service.getInterfaces();
          for (Class<?> intf : intfs) {
            if (implementsInterfaceRecursive(intf, intfName)) {
              interfaceSet.add(intfName);
              return true;
            }
          }

          // did not find the interface in this class so we look in the
          // superclass
          //
          service = service.getSuperclass();
        }
      } else {
        if (implementsInterfaceRecursive(service, intfName)) {
          interfaceSet.add(intfName);
          return true;
        }
      }

      return false;
    }
  }

  /**
   * Only called from implementsInterface().
   */
  private static boolean implementsInterfaceRecursive(Class<?> clazz, String intfName) {
    assert (clazz.isInterface());

    if (clazz.getName().equals(intfName)) {
      return true;
    }

    // search implemented interfaces
    Class<?>[] intfs = clazz.getInterfaces();
    for (Class<?> intf : intfs) {
      if (implementsInterfaceRecursive(intf, intfName)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Given a type identifier in the stream, attempt to deobfuscate it. Retuns
   * the original identifier if deobfuscation is unnecessary or no mapping is
   * known.
   */
  private static String maybeDeobfuscate(ServerSerializationStreamReader streamReader, String name)
      throws SerializationException {
    int index;
    if (streamReader.hasFlags(AbstractSerializationStream.FLAG_ELIDE_TYPE_NAMES)) {
      SerializationPolicy serializationPolicy = streamReader.getSerializationPolicy();
      if (!(serializationPolicy instanceof TypeNameObfuscator)) {
        throw new IncompatibleRemoteServiceException(
            "RPC request was encoded with obfuscated type names, "
                + "but the SerializationPolicy in use does not implement "
                + TypeNameObfuscator.class.getName());
      }

      String maybe = ((TypeNameObfuscator) serializationPolicy).getClassNameForTypeId(name);
      if (maybe != null) {
        return maybe;
      }
    } else if ((index = name.indexOf('/')) != -1) {
      return name.substring(0, index);
    }
    return name;
  }

  /**
   * Straight copy from
   * {@link com.google.gwt.dev.util.TypeInfo#getSourceRepresentation(Class)} to
   * avoid runtime dependency on gwt-dev.
   */
  private static String printTypeName(Class<?> type) {
    // Primitives
    //
    if (type.equals(Integer.TYPE)) {
      return "int";
    } else if (type.equals(Long.TYPE)) {
      return "long";
    } else if (type.equals(Short.TYPE)) {
      return "short";
    } else if (type.equals(Byte.TYPE)) {
      return "byte";
    } else if (type.equals(Character.TYPE)) {
      return "char";
    } else if (type.equals(Boolean.TYPE)) {
      return "boolean";
    } else if (type.equals(Float.TYPE)) {
      return "float";
    } else if (type.equals(Double.TYPE)) {
      return "double";
    }

    // Arrays
    //
    if (type.isArray()) {
      Class<?> componentType = type.getComponentType();
      return printTypeName(componentType) + "[]";
    }

    // Everything else
    //
    return type.getName().replace('$', '.');
  }

  /**
   * Static classes have no constructability.
   */
  private RPC() {
    // Not instantiable
  }
}
