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

import static com.google.gwt.user.client.rpc.RpcRequestBuilder.MODULE_BASE_HEADER;

import com.google.gwt.rpc.client.impl.RemoteException;
import com.google.gwt.user.client.rpc.IncompatibleRemoteServiceException;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.server.rpc.AbstractRemoteServiceServlet;
import com.google.gwt.user.server.rpc.RPCRequest;
import com.google.gwt.user.server.rpc.RPCServletUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.SoftReference;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPOutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * EXPERIMENTAL and subject to change. Do not use this in production code.
 * <p>
 * The servlet base class for your RPC service implementations that
 * automatically deserializes incoming requests from the client and serializes
 * outgoing responses for client/server RPCs.
 */
public class RpcServlet extends AbstractRemoteServiceServlet {

  protected static final String CLIENT_ORACLE_EXTENSION = ".gwt.rpc";
  private static final boolean DUMP_PAYLOAD = Boolean.getBoolean("gwt.rpc.dumpPayload");

  private final Map<String, SoftReference<ClientOracle>> clientOracleCache = new ConcurrentHashMap<String, SoftReference<ClientOracle>>();

  /**
   * The implementation of the service.
   */
  private final Object delegate;

  /**
   * The default constructor used by service implementations that
   * extend this class.  The servlet will delegate AJAX requests to
   * the appropriate method in the subclass.
   */
  public RpcServlet() {
    this.delegate = this;
  }

  /**
   * The wrapping constructor used by service implementations that are
   * separate from this class.  The servlet will delegate AJAX
   * requests to the appropriate method in the given object.
   */
  public RpcServlet(Object delegate) {
    this.delegate = delegate;
  }

  /**
   * This method creates the ClientOracle that will provide data about the
   * remote client. It delegates to
   * {@link #findClientOracleData(String, String)} to obtain access to
   * ClientOracle data emitted by the GWT compiler.
   */
  public ClientOracle getClientOracle() throws SerializationException {
    String permutationStrongName = getPermutationStrongName();
    if (permutationStrongName == null) {
      throw new SecurityException(
          "Blocked request without GWT permutation header (XSRF attack?)");
    }
    String basePath = getRequestModuleBasePath();
    if (basePath == null) {
      throw new SecurityException(
          "Blocked request without GWT base path header (XSRF attack?)");
    }

    ClientOracle toReturn;

    // Fast path if the ClientOracle is already cached.
    if (clientOracleCache.containsKey(permutationStrongName)) {
      toReturn = clientOracleCache.get(permutationStrongName).get();
      if (toReturn != null) {
        return toReturn;
      }
    }

    /* Synchronize to make sure expensive calls are executed only once.
       Double checked locking idiom works here because of volatiles in
       ConcurrentHashMap.*/
    synchronized (clientOracleCache) {
      if (clientOracleCache.containsKey(permutationStrongName)) {
        toReturn = clientOracleCache.get(permutationStrongName).get();
        if (toReturn != null) {
          return toReturn;
        }
      }

      if ("HostedMode".equals(permutationStrongName)) {
        if (!allowHostedModeConnections()) {
          throw new SecurityException("Blocked Development Mode request");
        }
        toReturn = new HostedModeClientOracle();
      } else {
        InputStream in = findClientOracleData(basePath, permutationStrongName);

        try {
          toReturn = WebModeClientOracle.load(in);
        } catch (IOException e) {
          throw new SerializationException(
              "Could not load serialization policy for permutation "
                  + permutationStrongName, e);
        }
      }
      clientOracleCache.put(permutationStrongName,
          new SoftReference<ClientOracle>(toReturn));
    }

    return toReturn;
  }

  /**
   * Process a call originating from the given request. Uses the
   * {@link RPC#invokeAndStreamResponse(Object, java.lang.reflect.Method, Object[], ClientOracle, OutputStream)}
   * method to do the actual work.
   * <p>
   * Subclasses may optionally override this method to handle the payload in any
   * way they desire (by routing the request to a framework component, for
   * instance). The {@link HttpServletRequest} and {@link HttpServletResponse}
   * can be accessed via the {@link #getThreadLocalRequest()} and
   * {@link #getThreadLocalResponse()} methods.
   * </p>
   * This is public so that it can be unit tested easily without HTTP.
   * 
   * @param clientOracle the ClientOracle that will be used to interpret the
   *          request
   * @param payload the UTF-8 request payload
   * @param stream the OutputStream that will receive the encoded response
   * @throws SerializationException if we cannot serialize the response
   */
  public void processCall(ClientOracle clientOracle, String payload,
      OutputStream stream) throws SerializationException {
    assert clientOracle != null : "clientOracle";
    assert payload != null : "payload";
    assert stream != null : "stream";

    try {
      RPCRequest rpcRequest = RPC.decodeRequest(payload, delegate.getClass(),
          clientOracle);
      onAfterRequestDeserialized(rpcRequest);
      RPC.invokeAndStreamResponse(delegate, rpcRequest.getMethod(),
          rpcRequest.getParameters(), clientOracle, stream);
    } catch (RemoteException ex) {
      throw new SerializationException("An exception was sent from the client",
          ex.getCause());
    } catch (IncompatibleRemoteServiceException ex) {
      log(
          "An IncompatibleRemoteServiceException was thrown while processing this call.",
          ex);
      RPC.streamResponseForFailure(clientOracle, stream, ex);
    }
  }

  /**
   * Standard HttpServlet method: handle the POST.
   * 
   * This doPost method swallows ALL exceptions, logs them in the
   * ServletContext, and returns a GENERIC_FAILURE_MSG response with status code
   * 500.
   * 
   * @throws IOException
   * @throws ServletException
   * @throws SerializationException
   */
  @Override
  public final void processPost(HttpServletRequest request,
      HttpServletResponse response) throws ServletException, IOException,
      SerializationException {

    /*
     * Get the ClientOracle before doing anything else, so that if ClientOracle
     * cannot be loaded, we haven't opened the response's OutputStream.
     */
    ClientOracle clientOracle = getClientOracle();

    // Read the request fully.
    String requestPayload = readContent(request);
    if (DUMP_PAYLOAD) {
      System.out.println(requestPayload);
    }

    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");

    // Configure the OutputStream based on configuration and capabilities
    boolean canCompress = RPCServletUtils.acceptsGzipEncoding(request)
        && shouldCompressResponse(request, response);

    OutputStream out;
    if (DUMP_PAYLOAD) {
      out = new ByteArrayOutputStream();

    } else if (canCompress) {
      RPCServletUtils.setGzipEncodingHeader(response);
      out = new GZIPOutputStream(response.getOutputStream());

    } else {
      out = response.getOutputStream();
    }

    // Invoke the core dispatching logic, which returns the serialized result.
    processCall(clientOracle, requestPayload, out);

    if (DUMP_PAYLOAD) {
      byte[] bytes = ((ByteArrayOutputStream) out).toByteArray();
      System.out.println(new String(bytes, "UTF-8"));
      response.getOutputStream().write(bytes);
    } else if (canCompress) {
      /*
       * We want to write the end of the gzip data, but not close the underlying
       * OutputStream in case there are servlet filters that want to write
       * headers after processPost().
       */
      ((GZIPOutputStream) out).finish();
    }
  }

  /**
   * Indicates whether or not an RPC request from a Development Mode client
   * should be serviced. Requests from Development Mode clients will expose
   * unobfuscated identifiers in the payload. It is intended that developers
   * override this method to restrict access based on installation-specific
   * logic (such as a range of IP addresses, checking for certain cookies, etc.)
   * <p>
   * The default implementation allows hosted-mode connections from the local
   * host, loopback addresses (127.*), site local (RFC 1918), link local
   * (169.254/16) addresses, and their IPv6 equivalents.
   *
   * @return <code>true</code> if a Development Mode connection should be
   *         allowed
   * @see #getThreadLocalRequest()
   * @see InetAddress
   */
  protected boolean allowHostedModeConnections() {
    return isRequestFromLocalAddress();
  }

  /**
   * Override this method to control access to permutation-specific data. For
   * instance, the permutation-specific data may be stored in a database in
   * order to support older clients.
   * <p>
   * The default implementation attempts to load the file from the
   * ServletContext as
   * 
   * <code>requestModuleBasePath + permutationStrongName + CLIENT_ORACLE_EXTENSION</code>
   * 
   * @param requestModuleBasePath the module's base path, modulo protocol and
   *          host, as reported by {@link #getRequestModuleBasePath()}
   * @param permutationStrongName the module's strong name as reported by
   *          {@link #getPermutationStrongName()}
   */
  protected InputStream findClientOracleData(String requestModuleBasePath,
      String permutationStrongName) throws SerializationException {
    String resourcePath = requestModuleBasePath + permutationStrongName
        + CLIENT_ORACLE_EXTENSION;
    InputStream in = getServletContext().getResourceAsStream(resourcePath);
    if (in == null) {
      throw new SerializationException(
          "Could not find ClientOracle data for permutation "
              + permutationStrongName);
    }
    return in;
  }

  /**
   * Extract the module's base path from the current request.
   * 
   * @return the module's base path, modulo protocol and host, as reported by
   *         {@link com.google.gwt.core.client.GWT#getModuleBaseURL()} or
   *         <code>null</code> if the request did not contain the
   *         {@value com.google.gwt.user.client.rpc.RpcRequestBuilder#MODULE_BASE_HEADER} header
   */
  protected final String getRequestModuleBasePath() {
    try {
      String header = getThreadLocalRequest().getHeader(MODULE_BASE_HEADER);
      if (header == null) {
        return null;
      }
      String path = new URL(header).getPath();
      String contextPath = getThreadLocalRequest().getContextPath();
      if (!path.startsWith(contextPath)) {
        return null;
      }
      return path.substring(contextPath.length());
    } catch (MalformedURLException e) {
      return null;
    }
  }

  /**
   * Determines whether the response to a given servlet request should or should
   * not be GZIP compressed. This method is only called in cases where the
   * requester accepts GZIP encoding.
   * <p>
   * This implementation currently returns <code>true</code> if the request
   * originates from a non-local address. Subclasses can override this logic.
   * </p>
   * 
   * @param request the request being served
   * @param response the response that will be written into
   * @return <code>true</code> if responsePayload should be GZIP compressed,
   *         otherwise <code>false</code>.
   */
  protected boolean shouldCompressResponse(HttpServletRequest request,
      HttpServletResponse response) {
    return !isRequestFromLocalAddress();
  }

  /**
   * Utility function to determine if the thread-local request originates from a
   * local address.
   */
  private boolean isRequestFromLocalAddress() {
    try {
      InetAddress addr = InetAddress.getByName(getThreadLocalRequest().getRemoteAddr());

      return InetAddress.getLocalHost().equals(addr)
          || addr.isLoopbackAddress() || addr.isSiteLocalAddress()
          || addr.isLinkLocalAddress();
    } catch (UnknownHostException e) {
      return false;
    }
  }
}
