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
package com.google.gwt.user.server.rpc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPOutputStream;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Utility class containing helper methods used by servlets that integrate with
 * the RPC system.
 */
public class RPCServletUtils {
  
  public static final String CHARSET_UTF8_NAME = "UTF-8";
  
  /**
   * The UTF-8 Charset. Use this to avoid concurrency bottlenecks when
   * converting between byte arrays and Strings.
   * See http://code.google.com/p/google-web-toolkit/issues/detail?id=6398
   */
  public static final Charset CHARSET_UTF8 = Charset.forName(CHARSET_UTF8_NAME);

  /**
   * Package protected for use in tests.
   */
  static final int BUFFER_SIZE = 4096;

  private static final String ACCEPT_ENCODING = "Accept-Encoding";

  private static final String ATTACHMENT = "attachment";

  private static final String CONTENT_DISPOSITION = "Content-Disposition";

  private static final String CONTENT_ENCODING = "Content-Encoding";

  private static final String CONTENT_ENCODING_GZIP = "gzip";

  private static final String CONTENT_TYPE_APPLICATION_JSON_UTF8 = "application/json; charset=utf-8";

  private static final String GENERIC_FAILURE_MSG = "The call failed on the server; see server log for details";

  private static final String GWT_RPC_CONTENT_TYPE = "text/x-gwt-rpc";

  /**
   * Controls the compression threshold at and below which no compression will
   * take place.
   */
  private static final int UNCOMPRESSED_BYTE_SIZE_LIMIT = 256;

  /**
   * Contains cached mappings from character set name to Charset. The
   * null key maps to the default UTF-8 character set.
   */
  private static final ConcurrentHashMap<String, Charset> CHARSET_CACHE =
      new ConcurrentHashMap<String, Charset>();

  /**
   * Pre-populate the character set cache with UTF-8.
   */
  static {
    CHARSET_CACHE.put(CHARSET_UTF8_NAME, CHARSET_UTF8);
  }

  /**
   * Returns <code>true</code> if the {@link HttpServletRequest} accepts Gzip
   * encoding. This is done by checking that the accept-encoding header
   * specifies gzip as a supported encoding.
   *
   * @param request the request instance to test for gzip encoding acceptance
   * @return <code>true</code> if the {@link HttpServletRequest} accepts Gzip
   *         encoding
   */
  public static boolean acceptsGzipEncoding(HttpServletRequest request) {
    assert (request != null);

    String acceptEncoding = request.getHeader(ACCEPT_ENCODING);
    if (null == acceptEncoding) {
      return false;
    }

    return (acceptEncoding.indexOf(CONTENT_ENCODING_GZIP) != -1);
  }

  /**
   * Returns <code>true</code> if the response content's estimated UTF-8 byte
   * length exceeds 256 bytes.
   *
   * @param content the contents of the response
   * @return <code>true</code> if the response content's estimated UTF-8 byte
   *         length exceeds 256 bytes
   */
  public static boolean exceedsUncompressedContentLengthLimit(String content) {
    return (content.length() * 2) > UNCOMPRESSED_BYTE_SIZE_LIMIT;
  }

  /**
   * Get the Charset for a named character set. Caches Charsets to work around
   * a concurrency bottleneck in FastCharsetProvider.
   * See http://code.google.com/p/google-web-toolkit/issues/detail?id=6398
   * @see {@link Charset#forName(String)}
   *
   * @param encoding the name of the Charset to get. If this is null
   *                 the default UTF-8 character set will be returned.
   * @return the named Charset.
   */
  public static Charset getCharset(String encoding) {
        
    if (encoding == null) {
      return CHARSET_UTF8;
    }
    
    Charset charset = CHARSET_CACHE.get(encoding);

    if (charset == null) {
      charset = Charset.forName(encoding);
      CHARSET_CACHE.put(encoding, charset);
    }

    return charset;
  }

  /**
   * Returns true if the {@link java.lang.reflect.Method Method} definition on
   * the service is specified to throw the exception contained in the
   * InvocationTargetException or false otherwise. NOTE we do not check that the
   * type is serializable here. We assume that it must be otherwise the
   * application would never have been allowed to run.
   *
   * @param serviceIntfMethod the method from the RPC request
   * @param cause the exception that the method threw
   * @return true if the exception's type is in the method's signature
   */
  public static boolean isExpectedException(Method serviceIntfMethod,
      Throwable cause) {
    assert (serviceIntfMethod != null);
    assert (cause != null);

    Class<?>[] exceptionsThrown = serviceIntfMethod.getExceptionTypes();
    if (exceptionsThrown.length <= 0) {
      // The method is not specified to throw any exceptions
      //
      return false;
    }

    Class<? extends Throwable> causeType = cause.getClass();

    for (Class<?> exceptionThrown : exceptionsThrown) {
      assert (exceptionThrown != null);

      if (exceptionThrown.isAssignableFrom(causeType)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Returns the content of an {@link HttpServletRequest} by decoding it using
   * <code>expectedCharSet</code>, or <code>UTF-8</code> if
   * <code>expectedCharSet</code> is <code>null</null>.
   *
   * @param request the servlet request whose content we want to read
   * @param expectedContentType the expected content (i.e. 'type/subtype' only)
   *          in the Content-Type request header, or <code>null</code> if no
   *          validation is to be performed, and you are willing to allow for
   *          some types of cross type security attacks
   * @param expectedCharSet the expected request charset, or <code>null</code>
   *          if no charset validation is to be performed and <code>UTF-8</code>
   *          should be assumed
   * @return the content of an {@link HttpServletRequest} by decoding it using
   *         <code>expectedCharSet</code>, or <code>UTF-8</code> if
   *         <code>expectedCharSet</code> is <code>null</code>
   * @throws IOException if the request's input stream cannot be accessed, read
   *         from or closed
   * @throws ServletException if the request's content type does not
   *         equal the supplied <code>expectedContentType</code> or
   *         <code>expectedCharSet</code>
   */
  public static String readContent(HttpServletRequest request,
      String expectedContentType, String expectedCharSet)
      throws IOException, ServletException {
    if (expectedContentType != null) {
      checkContentTypeIgnoreCase(request, expectedContentType);
    }
    if (expectedCharSet != null) {
      checkCharacterEncodingIgnoreCase(request, expectedCharSet);
    }

    /*
     * Need to support 'Transfer-Encoding: chunked', so do not rely on
     * presence of a 'Content-Length' request header.
     */
    InputStream in = request.getInputStream();
    byte[] buffer = new byte[BUFFER_SIZE];
    ByteArrayOutputStream out = new  ByteArrayOutputStream(BUFFER_SIZE);
    try {
      while (true) {
        int byteCount = in.read(buffer);
        if (byteCount == -1) {
          break;
        }
        out.write(buffer, 0, byteCount);
      }
      return new String(out.toByteArray(), getCharset(expectedCharSet));
    } finally {
      if (in != null) {
        in.close();
      }
    }
  }

  /**
   * Returns the content of an {@link HttpServletRequest}, after verifying a
   * <code>gwt/x-gwt-rpc; charset=utf-8</code> content type.
   *
   * @param request the servlet request whose content we want to read
   * @return the content of an {@link HttpServletRequest} by decoding it using
   *         <code>UTF-8</code>
   * @throws IOException if the request's input stream cannot be accessed, read
   *         from or closed
   * @throws ServletException if the request's content type is not
   *         <code>gwt/x-gwt-rpc; charset=utf-8</code>, ignoring case
   */
  public static String readContentAsGwtRpc(HttpServletRequest request)
      throws IOException, ServletException {
      return readContent(request, GWT_RPC_CONTENT_TYPE, CHARSET_UTF8_NAME);
  }

 /**
   * Returns the content of an {@link HttpServletRequest} by decoding it using
   * the UTF-8 charset.
   *
   * @param request the servlet request whose content we want to read
   * @return the content of an {@link HttpServletRequest} by decoding it using
   *         the UTF-8 charset
   * @throws IOException if the requests input stream cannot be accessed, read
   *           from or closed
   * @throws ServletException if the content length of the request is not
   *           specified of if the request's content type is not
   *           'text/x-gwt-rpc' and 'charset=utf-8'
   * @deprecated Use {@link #readContent} instead.
   */
  @Deprecated
  public static String readContentAsUtf8(HttpServletRequest request)
      throws IOException, ServletException {
    return readContent(request, null, null);
  }

  /**
   * Returns the content of an {@link HttpServletRequest} by decoding it using
   * the UTF-8 charset.
   *
   * @param request the servlet request whose content we want to read
   * @param checkHeaders Specify 'true' to check the Content-Type header to see
   *          that it matches the expected value 'text/x-gwt-rpc' and the
   *          content encoding is UTF-8. Disabling this check may allow some
   *          types of cross type security attacks.
   * @return the content of an {@link HttpServletRequest} by decoding it using
   *         the UTF-8 charset
   * @throws IOException if the requests input stream cannot be accessed, read
   *           from or closed
   * @throws ServletException if the content length of the request is not
   *           specified of if the request's content type is not
   *           'text/x-gwt-rpc' and 'charset=utf-8'
   * @deprecated Use {@link #readContent} instead.
   */
  @Deprecated
  public static String readContentAsUtf8(HttpServletRequest request,
      boolean checkHeaders) throws IOException, ServletException {
    return readContent(request, GWT_RPC_CONTENT_TYPE, CHARSET_UTF8_NAME);
  }

  /**
   * Sets the correct header to indicate that a response is gzipped.
   */
  public static void setGzipEncodingHeader(HttpServletResponse response) {
    response.setHeader(CONTENT_ENCODING, CONTENT_ENCODING_GZIP);
  }

  /**
   * Returns <code>true</code> if the request accepts gzip encoding and the the
   * response content's estimated UTF-8 byte length exceeds 256 bytes.
   *
   * @param request the request associated with the response content
   * @param responseContent a string that will be
   * @return <code>true</code> if the request accepts gzip encoding and the the
   *         response content's estimated UTF-8 byte length exceeds 256 bytes
   */
  public static boolean shouldGzipResponseContent(HttpServletRequest request,
      String responseContent) {
    return acceptsGzipEncoding(request)
        && exceedsUncompressedContentLengthLimit(responseContent);
  }

  /**
   * Write the response content into the {@link HttpServletResponse}. If
   * <code>gzipResponse</code> is <code>true</code>, the response content will
   * be gzipped prior to being written into the response.
   *
   * @param servletContext servlet context for this response
   * @param response response instance
   * @param responseContent a string containing the response content
   * @param gzipResponse if <code>true</code> the response content will be gzip
   *          encoded before being written into the response
   * @throws IOException if reading, writing, or closing the response's output
   *           stream fails
   */
  public static void writeResponse(ServletContext servletContext,
      HttpServletResponse response, String responseContent, boolean gzipResponse)
      throws IOException {

    byte[] responseBytes = responseContent.getBytes(CHARSET_UTF8);
    if (gzipResponse) {
      // Compress the reply and adjust headers.
      //
      ByteArrayOutputStream output = null;
      GZIPOutputStream gzipOutputStream = null;
      Throwable caught = null;
      try {
        output = new ByteArrayOutputStream(responseBytes.length);
        gzipOutputStream = new GZIPOutputStream(output);
        gzipOutputStream.write(responseBytes);
        gzipOutputStream.finish();
        gzipOutputStream.flush();
        setGzipEncodingHeader(response);
        responseBytes = output.toByteArray();
      } catch (IOException e) {
        caught = e;
      } finally {
        if (null != gzipOutputStream) {
          gzipOutputStream.close();
        }
        if (null != output) {
          output.close();
        }
      }

      if (caught != null) {
        servletContext.log("Unable to compress response", caught);
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        return;
      }
    }

    // Send the reply.
    //
    response.setContentLength(responseBytes.length);
    response.setContentType(CONTENT_TYPE_APPLICATION_JSON_UTF8);
    response.setStatus(HttpServletResponse.SC_OK);
    response.setHeader(CONTENT_DISPOSITION, ATTACHMENT);
    response.getOutputStream().write(responseBytes);
  }

  /**
   * Called when the servlet itself has a problem, rather than the invoked
   * third-party method. It writes a simple 500 message back to the client.
   *
   * @param servletContext
   * @param response
   * @param failure
   */
  public static void writeResponseForUnexpectedFailure(
      ServletContext servletContext, HttpServletResponse response,
      Throwable failure) {
    servletContext.log("Exception while dispatching incoming RPC call", failure);

    // Send GENERIC_FAILURE_MSG with 500 status.
    //
    try {
      response.setContentType("text/plain");
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      try {
        response.getOutputStream().write(GENERIC_FAILURE_MSG.getBytes(CHARSET_UTF8));
      } catch (IllegalStateException e) {
        // Handle the (unexpected) case where getWriter() was previously used
        response.getWriter().write(GENERIC_FAILURE_MSG);
      }
    } catch (IOException ex) {
      servletContext.log(
          "respondWithUnexpectedFailure failed while sending the previous failure to the client",
          ex);
    }
  }

  /**
   * Performs validation of the character encoding, ignoring case.
   *
   * @param request the incoming request
   * @param expectedCharSet the expected charset of the request
   * @throws ServletException if requests encoding is not <code>null</code> and
   *         does not equal, ignoring case, <code>expectedCharSet</code>
   */
  private static void checkCharacterEncodingIgnoreCase(
      HttpServletRequest request, String expectedCharSet)
      throws ServletException {
    
    assert (expectedCharSet != null);
    boolean encodingOkay = false;
    String characterEncoding = request.getCharacterEncoding();
    if (characterEncoding != null) {
      /*
       * TODO: It would seem that we should be able to use equalsIgnoreCase here
       * instead of indexOf. Need to be sure that servlet engines return a
       * properly parsed character encoding string if we decide to make this
       * change.
       */
      if (characterEncoding.toLowerCase().indexOf(expectedCharSet.toLowerCase())
          != -1) {
        encodingOkay = true;
      }
    }

    if (!encodingOkay) {
      throw new ServletException("Character Encoding is '"
          + (characterEncoding == null ? "(null)" : characterEncoding)
          + "'.  Expected '" + expectedCharSet + "'");
    }
  }

  /**
   * Performs Content-Type validation of the incoming request, ignoring case
   * and any <code>charset</code> parameter.
   *
   * @see   #checkCharacterEncodingIgnoreCase(HttpServletRequest, String)
   * @param request the incoming request
   * @param expectedContentType the expected Content-Type for the incoming
   *        request
   * @throws ServletException if the request's content type is not
   *         <code>null</code> and does not, ignoring case, equal
   *         <code>expectedContentType</code>,
   */
  private static void checkContentTypeIgnoreCase(
      HttpServletRequest request, String expectedContentType)
      throws ServletException {
    
    assert (expectedContentType != null);
    String contentType = request.getContentType();
    boolean contentTypeIsOkay = false;

    if (contentType != null) {
      contentType = contentType.toLowerCase();
      /*
       * NOTE:We use startsWith because some servlet engines, i.e. Tomcat, do
       * not remove the charset component but others do.
       */
      if (contentType.startsWith(expectedContentType.toLowerCase())) {
        contentTypeIsOkay = true;
      }
    }

    if (!contentTypeIsOkay) {
      throw new ServletException("Content-Type was '"
          + (contentType == null ? "(null)" : contentType) + "'. Expected '"
          + expectedContentType + "'.");
    }
  }

  private RPCServletUtils() {
    // Not instantiable
  }
}
