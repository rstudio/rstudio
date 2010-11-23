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
package com.google.gwt.rpc.server;

import com.google.gwt.rpc.client.ast.CommandSink;
import com.google.gwt.rpc.client.ast.HasValues;
import com.google.gwt.rpc.client.ast.ReturnCommand;
import com.google.gwt.rpc.client.ast.RpcCommand;
import com.google.gwt.rpc.client.ast.ThrowCommand;
import com.google.gwt.rpc.client.impl.HasValuesCommandSink;
import com.google.gwt.rpc.client.impl.RemoteException;
import com.google.gwt.user.client.rpc.IncompatibleRemoteServiceException;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.server.rpc.RPCRequest;
import com.google.gwt.user.server.rpc.RPCServletUtils;
import com.google.gwt.user.server.rpc.UnexpectedException;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * EXPERIMENTAL and subject to change. Do not use this in production code.
 * <p>
 * Utility class for integrating with the RPC system.
 */
public class RPC {

  private static final HashMap<String, Class<?>> TYPE_NAMES = new HashMap<String, Class<?>>();

  /**
   * Static map of classes to sets of interfaces (e.g. classes). Optimizes
   * lookup of interfaces for security.
   */
  private static final Map<Class<?>, Set<String>> serviceToImplementedInterfacesMap = new HashMap<Class<?>, Set<String>>();

  static {
    // The space is needed to prevent name collisions
    TYPE_NAMES.put(" Z", boolean.class);
    TYPE_NAMES.put(" B", byte.class);
    TYPE_NAMES.put(" C", char.class);
    TYPE_NAMES.put(" D", double.class);
    TYPE_NAMES.put(" F", float.class);
    TYPE_NAMES.put(" I", int.class);
    TYPE_NAMES.put(" J", long.class);
    TYPE_NAMES.put(" S", short.class);
  }

  public static RPCRequest decodeRequest(String encodedRequest, Class<?> type,
      ClientOracle clientOracle) throws RemoteException {
    if (encodedRequest == null) {
      throw new NullPointerException("encodedRequest cannot be null");
    }

    if (encodedRequest.length() == 0) {
      throw new IllegalArgumentException("encodedRequest cannot be empty");
    }

    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

    try {
      SimplePayloadDecoder decoder;
      try {
        decoder = new SimplePayloadDecoder(clientOracle, encodedRequest);
      } catch (ClassNotFoundException e) {
        throw new IncompatibleRemoteServiceException(
            "Client does not have a type sent by the server", e);
      }
      CommandServerSerializationStreamReader streamReader = new CommandServerSerializationStreamReader();
      if (decoder.getThrownValue() != null) {
        streamReader.prepareToRead(Collections.singletonList(decoder.getThrownValue()));
        try {
          throw new RemoteException((Throwable) streamReader.readObject());
        } catch (ClassCastException e) {
          throw new SerializationException(
              "The remote end threw something other than a Throwable", e);
        } catch (SerializationException e) {
          throw new IncompatibleRemoteServiceException(
              "The remote end threw an exception which could not be deserialized",
              e);
        }
      } else {
        streamReader.prepareToRead(decoder.getValues());
      }

      // Read the name of the RemoteService interface
      String serviceIntfName = streamReader.readString();

      if (type != null) {
        if (!implementsInterface(type, serviceIntfName)) {
          // The service does not implement the requested interface
          throw new IncompatibleRemoteServiceException(
              "Blocked attempt to access interface '" + serviceIntfName
                  + "', which is not implemented by '" + printTypeName(type)
                  + "'; this is either misconfiguration or a hack attempt");
        }
      }

      Class<?> serviceIntf;
      try {
        serviceIntf = getClassFromSerializedName(null, serviceIntfName,
            classLoader);
        if (!RemoteService.class.isAssignableFrom(serviceIntf)) {
          // The requested interface is not a RpcService interface
          throw new IncompatibleRemoteServiceException(
              "Blocked attempt to access interface '"
                  + printTypeName(serviceIntf)
                  + "', which doesn't extend RpcService; "
                  + "this is either misconfiguration or a hack attempt");
        }
      } catch (ClassNotFoundException e) {
        throw new IncompatibleRemoteServiceException(
            "Could not locate requested interface '" + serviceIntfName
                + "' in default classloader", e);
      }

      String serviceMethodName = streamReader.readString();

      int paramCount = streamReader.readInt();
      Class<?>[] parameterTypes = new Class[paramCount];

      for (int i = 0; i < parameterTypes.length; i++) {
        String paramClassName = streamReader.readString();

        try {
          parameterTypes[i] = getClassFromSerializedName(clientOracle,
              paramClassName, classLoader);
        } catch (ClassNotFoundException e) {
          throw new IncompatibleRemoteServiceException("Parameter " + i
              + " of is of an unknown type '" + paramClassName + "'", e);
        }
      }

      try {
        Method method = serviceIntf.getMethod(serviceMethodName, parameterTypes);

        Object[] parameterValues = new Object[parameterTypes.length];
        for (int i = 0; i < parameterValues.length; i++) {
          Object o = CommandSerializationUtil.getAccessor(parameterTypes[i]).readNext(
              streamReader);
          parameterValues[i] = o;
        }

        return new RPCRequest(method, parameterValues, null, null, 0);

      } catch (NoSuchMethodException e) {
        throw new IncompatibleRemoteServiceException(
            formatMethodNotFoundErrorMessage(serviceIntf, serviceMethodName,
                parameterTypes));
      }
    } catch (SerializationException ex) {
      throw new IncompatibleRemoteServiceException(ex.getMessage(), ex);
    }
  }

  public static void invokeAndStreamResponse(Object target,
      Method serviceMethod, Object[] args, ClientOracle clientOracle,
      OutputStream stream) throws SerializationException {
    if (serviceMethod == null) {
      throw new NullPointerException("serviceMethod");
    }

    if (clientOracle == null) {
      throw new NullPointerException("clientOracle");
    }

    CommandSink sink;
    try {
      sink = clientOracle.createCommandSink(stream);
    } catch (IOException e) {
      throw new SerializationException("Unable to initialize output", e);
    }

    try {
      Object result = serviceMethod.invoke(target, args);
      try {
        streamResponse(clientOracle, result, sink, false);
      } catch (SerializationException e) {
        streamResponse(clientOracle, e, sink, true);
      }

    } catch (IllegalAccessException e) {
      SecurityException securityException = new SecurityException(
          formatIllegalAccessErrorMessage(target, serviceMethod));
      securityException.initCause(e);
      throw securityException;
    } catch (IllegalArgumentException e) {
      SecurityException securityException = new SecurityException(
          formatIllegalArgumentErrorMessage(target, serviceMethod, args));
      securityException.initCause(e);
      throw securityException;
    } catch (InvocationTargetException e) {
      // Try to encode the caught exception
      Throwable cause = e.getCause();

      // Don't allow random RuntimeExceptions to be thrown back to the client
      if (!RPCServletUtils.isExpectedException(serviceMethod, cause)) {
        throw new UnexpectedException("Service method '"
            + getSourceRepresentation(serviceMethod)
            + "' threw an unexpected exception: " + cause.toString(), cause);
      }

      streamResponse(clientOracle, cause, sink, true);
    }
    sink.finish();
  }

  public static void streamResponseForFailure(ClientOracle clientOracle,
      OutputStream out, Throwable payload) throws SerializationException {
    CommandSink sink;
    try {
      sink = clientOracle.createCommandSink(out);
    } catch (IOException e) {
      throw new SerializationException("Unable to initialize output", e);
    }
    streamResponse(clientOracle, payload, sink, true);
    sink.finish();
  }

  public static void streamResponseForSuccess(ClientOracle clientOracle,
      OutputStream out, Object payload) throws SerializationException {
    CommandSink sink;
    try {
      sink = clientOracle.createCommandSink(out);
    } catch (IOException e) {
      throw new SerializationException("Unable to initialize output", e);
    }
    streamResponse(clientOracle, payload, sink, false);
    sink.finish();
  }

  private static String formatIllegalAccessErrorMessage(Object target,
      Method serviceMethod) {
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

  private static String formatIllegalArgumentErrorMessage(Object target,
      Method serviceMethod, Object[] args) {
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
  private static Class<?> getClassFromSerializedName(ClientOracle clientOracle,
      String serializedName, ClassLoader classLoader)
      throws ClassNotFoundException {
    Class<?> value = TYPE_NAMES.get(serializedName);
    if (value != null) {
      return value;
    }

    // Interfaces don't exist in the client, so we use unobfuscated names
    if (serializedName.charAt(0) == ' ') {
      serializedName = serializedName.substring(1);
    } else if (clientOracle != null) {
      serializedName = clientOracle.getTypeName(serializedName);
    }
    assert serializedName != null;

    return Class.forName(serializedName, false, classLoader);
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
        while ((service != null) && !RpcServlet.class.equals(service)) {
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
   * Recursive helper for implementsInterface().
   */
  private static boolean implementsInterfaceRecursive(Class<?> clazz,
      String intfName) {
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

  private static void streamResponse(ClientOracle clientOracle, Object payload,
      CommandSink sink, boolean asThrow) throws SerializationException {
    HasValues command;
    if (asThrow) {
      command = new ThrowCommand();
      assert payload instanceof Throwable : "Trying to throw something other than a Throwable";
      // payload = new RemoteException((Throwable) payload);
    } else {
      command = new ReturnCommand();
    }

    CommandServerSerializationStreamWriter out = new CommandServerSerializationStreamWriter(
        clientOracle, new HasValuesCommandSink(command));

    out.writeObject(payload);

    sink.accept((RpcCommand) command);
  }

  private RPC() {
  }
}
