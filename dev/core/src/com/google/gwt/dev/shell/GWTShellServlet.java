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
package com.google.gwt.dev.shell;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.ArtifactSet;
import com.google.gwt.core.ext.linker.impl.HostedModeLinker;
import com.google.gwt.core.ext.linker.impl.StandardLinkerContext;
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.cfg.ModuleDefLoader;
import com.google.gwt.dev.jjs.JJSOptionsImpl;
import com.google.gwt.dev.resource.Resource;
import com.google.gwt.dev.shell.log.ServletContextTreeLogger;
import com.google.gwt.dev.util.HttpHeaders;
import com.google.gwt.dev.util.Util;
import com.google.gwt.util.tools.Utility;

import org.apache.commons.collections.map.AbstractReferenceMap;
import org.apache.commons.collections.map.ReferenceIdentityMap;
import org.apache.commons.collections.map.ReferenceMap;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Built-in servlet for convenient access to the public path of a specified
 * module.
 */
public class GWTShellServlet extends HttpServlet {

  private static class RequestParts {
    public final String moduleName;

    public final String partialPath;

    public RequestParts(HttpServletRequest request)
        throws UnableToCompleteException {
      String pathInfo = request.getPathInfo();
      if (pathInfo != null) {
        int slash = pathInfo.indexOf('/', 1);
        if (slash != -1) {
          moduleName = pathInfo.substring(1, slash);
          partialPath = pathInfo.substring(slash + 1);
          return;
        } else {
          moduleName = pathInfo.substring(1);
          partialPath = null;
          return;
        }
      }
      throw new UnableToCompleteException();
    }
  }

  /**
   * This the default cache time in seconds for files that aren't either
   * *.cache.*, *.nocache.*.
   */
  private static final int DEFAULT_CACHE_SECONDS = 5;

  private static final String XHTML_MIME_TYPE = "application/xhtml+xml";

  /**
   * Must keep only weak references to ModuleDefs else we permanently pin them.
   */
  @SuppressWarnings("unchecked")
  private final Map<String, ModuleDef> loadedModulesByName = new ReferenceMap(
      AbstractReferenceMap.HARD, AbstractReferenceMap.WEAK);

  /**
   * The lifetime of the module pins the lifetime of the associated servlet;
   * this is because the loaded servlet has a weak backRef to its live module
   * through its context. When the module dies, the servlet needs to die also.
   */
  @SuppressWarnings("unchecked")
  private final Map<ModuleDef, Map<String, HttpServlet>> loadedServletsByModuleAndClassName = new ReferenceIdentityMap(
      AbstractReferenceMap.WEAK, AbstractReferenceMap.HARD, true);

  private final Map<String, String> mimeTypes = new HashMap<String, String>();

  /**
   * Only for backwards compatibility. Shouldn't we remove this now?
   */
  @SuppressWarnings("unchecked")
  private final Map<String, ModuleDef> modulesByServletPath = new ReferenceMap(
      AbstractReferenceMap.HARD, AbstractReferenceMap.WEAK);

  private int nextRequestId;

  private final Object requestIdLock = new Object();

  private TreeLogger topLogger;

  private WorkDirs workDirs;

  public GWTShellServlet() {
    initMimeTypes();
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    processFileRequest(request, response);
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    processFileRequest(request, response);
  }

  protected void processFileRequest(HttpServletRequest request,
      HttpServletResponse response) throws IOException {

    String pathInfo = request.getPathInfo();
    if (pathInfo.length() == 0 || pathInfo.equals("/")) {
      response.setContentType("text/html");
      PrintWriter writer = response.getWriter();
      writer.println("<html><body><basefont face='arial'>");
      writer.println("To launch an application, specify a URL of the form <code>/<i>module</i>/<i>file.html</i></code>");
      writer.println("</body></html>");
      return;
    }

    TreeLogger logger = getLogger();

    // Parse the request assuming it is module/resource.
    //
    RequestParts parts;
    try {
      parts = new RequestParts(request);
    } catch (UnableToCompleteException e) {
      sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND,
          "Don't know what to do with this URL: '" + pathInfo + "'");
      return;
    }

    String partialPath = parts.partialPath;
    String moduleName = parts.moduleName;

    // If the module is renamed, substitute the renamed module name
    ModuleDef moduleDef = loadedModulesByName.get(moduleName);
    if (moduleDef != null) {
      moduleName = moduleDef.getName();
    }

    if (partialPath == null) {
      // Redir back to the same URL but ending with a slash.
      //
      response.sendRedirect(moduleName + "/");
      return;
    } else if (partialPath.length() > 0) {
      // Both the module name and a resource.
      //
      doGetPublicFile(request, response, logger, partialPath, moduleName);
      return;
    } else {
      // Was just the module name, ending with a slash.
      //
      doGetModule(request, response, logger, parts);
      return;
    }
  }

  @Override
  protected void service(HttpServletRequest request,
      HttpServletResponse response) throws ServletException, IOException {

    TreeLogger logger = getLogger();
    int id = allocateRequestId();
    if (logger.isLoggable(TreeLogger.TRACE)) {
      StringBuffer url = request.getRequestURL();

      // Branch the logger in case we decide to log more below.
      logger = logger.branch(TreeLogger.TRACE, "Request " + id + ": " + url,
          null);
    }

    String servletClassName = null;
    ModuleDef moduleDef = null;

    try {
      // Attempt to split the URL into module/path, which we'll use to see
      // if we can map the request to a module's servlet.
      RequestParts parts = new RequestParts(request);

      if ("favicon.ico".equalsIgnoreCase(parts.moduleName)) {
        sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND,
            "Icon not available");
        return;
      }

      // See if the request references a module we know.
      moduleDef = getModuleDef(logger, parts.moduleName);
      if (moduleDef != null) {
        // Okay, we know this module. Do we know this servlet path?
        // It is right to prepend the slash because (1) ModuleDefSchema requires
        // every servlet path to begin with a slash and (2) RequestParts always
        // rips off the leading slash.
        String servletPath = "/" + parts.partialPath;
        servletClassName = moduleDef.findServletForPath(servletPath);

        // Fall-through below, where we check servletClassName.
      } else {
        // Fall-through below, where we check servletClassName.
      }
    } catch (UnableToCompleteException e) {
      // Do nothing, since it was speculative anyway.
    }

    // BEGIN BACKWARD COMPATIBILITY
    if (servletClassName == null) {
      // Try to map a bare path that isn't preceded by the module name.
      // This is no longer the recommended practice, so we warn.
      String path = request.getPathInfo();
      moduleDef = modulesByServletPath.get(path);
      if (moduleDef != null) {
        // See if there is a servlet we can delegate to for the given url.
        servletClassName = moduleDef.findServletForPath(path);

        if (servletClassName != null) {
          TreeLogger branch = logger.branch(TreeLogger.WARN,
              "Use of deprecated hosted mode servlet path mapping", null);
          branch.log(
              TreeLogger.WARN,
              "The client code is invoking the servlet with a URL that is not module-relative: "
                  + path, null);
          branch.log(
              TreeLogger.WARN,
              "Prepend GWT.getModuleBaseURL() to the URL in client code to create a module-relative URL: /"
                  + moduleDef.getName() + path, null);
          branch.log(
              TreeLogger.WARN,
              "Using module-relative URLs ensures correct URL-independent behavior in external servlet containers",
              null);
        }

        // Fall-through below, where we check servletClassName.
      } else {
        // Fall-through below, where we check servletClassName.
      }
    }
    // END BACKWARD COMPATIBILITY

    // Load/get the servlet if we found one.
    if (servletClassName != null) {
      HttpServlet delegatee = tryGetOrLoadServlet(logger, moduleDef,
          servletClassName);
      if (delegatee == null) {
        logger.log(TreeLogger.ERROR, "Unable to dispatch request", null);
        sendErrorResponse(response,
            HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
            "Unable to find/load mapped servlet class '" + servletClassName
                + "'");
        return;
      }

      // Delegate everything to the downstream servlet and we're done.
      delegatee.service(request, response);
    } else {
      // Use normal default processing on this request, since we couldn't
      // recognize it as anything special.
      super.service(request, response);
    }
  }

  private int allocateRequestId() {
    synchronized (requestIdLock) {
      return nextRequestId++;
    }
  }

  /**
   * Handle auto-generated resources.
   * 
   * @return <code>true</code> if a resource was generated
   */
  private boolean autoGenerateResources(HttpServletRequest request,
      HttpServletResponse response, TreeLogger logger, String partialPath,
      String moduleName) throws IOException {

    if (partialPath.equals(moduleName + ".nocache.js")) {
      if (request.getParameter("compiled") == null) {
        // Generate the .js file.
        try {
          String js = genSelectionScript(logger, moduleName);
          setResponseCacheHeaders(response, 0); // do not cache selection script
          response.setStatus(HttpServletResponse.SC_OK);
          response.setContentType("text/javascript");
          response.getWriter().println(js);
          return true;
        } catch (UnableToCompleteException e) {
          // The error will have already been logged. Continue, since this could
          // actually be a request for a static file that happens to have an
          // unfortunately confusing name.
        }
      }
    } else if (partialPath.equals("hosted.html")) {
      String html = HostedModeLinker.getHostedHtml();
      setResponseCacheHeaders(response, DEFAULT_CACHE_SECONDS);
      response.setStatus(HttpServletResponse.SC_OK);
      response.setContentType("text/html");
      response.getWriter().println(html);
      return true;
    }

    return false;
  }

  private void doGetModule(HttpServletRequest request,
      HttpServletResponse response, TreeLogger logger, RequestParts parts)
      throws IOException {

    // Generate a generic empty host page.
    //
    String msg = "The development shell servlet received a request to generate a host page for module '"
        + parts.moduleName + "' ";

    logger = logger.branch(TreeLogger.TRACE, msg, null);

    try {
      // Try to load the module just to make sure it'll work.
      getModuleDef(logger, parts.moduleName);
    } catch (UnableToCompleteException e) {
      sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND,
          "Unable to find/load module '" + Util.escapeXml(parts.moduleName)
              + "' (see server log for details)");
      return;
    }

    response.setContentType("text/html");
    PrintWriter writer = response.getWriter();
    writer.println("<html><head>");
    writer.print("<script language='javascript' src='");
    writer.print(parts.moduleName);
    writer.println(".nocache.js'></script>");

    // Create a property for each query param.
    Map<String, String[]> params = getParameterMap(request);
    for (Map.Entry<String, String[]> entry : params.entrySet()) {
      String[] values = entry.getValue();
      if (values.length > 0) {
        writer.print("<meta name='gwt:property' content='");
        writer.print(entry.getKey());
        writer.print("=");
        writer.print(values[values.length - 1]);
        writer.println("'>");
      }
    }

    writer.println("</head><body>");
    writer.println("<iframe src=\"javascript:''\" id='__gwt_historyFrame' "
        + "style='position:absolute;width:0;height:0;border:0'></iframe>");
    writer.println("<noscript>");
    writer.println("  <div style=\"width: 22em; position: absolute; left: 50%; margin-left: -11em; color: red; background-color: white; border: 1px solid red; padding: 4px; font-family: sans-serif\">");
    writer.println("    Your web browser must have JavaScript enabled");
    writer.println("    in order for this application to display correctly.");
    writer.println("  </div>");
    writer.println("</noscript>");
    writer.println("</body></html>");

    // Done.
  }

  /**
   * Fetch a file and return it as the HTTP response, setting the cache-related
   * headers according to the name of the file (see
   * {@link #getCacheTime(String)}). This function honors If-Modified-Since to
   * minimize the impact of limiting caching of files for development.
   * 
   * @param request the HTTP request
   * @param response the HTTP response
   * @param logger a TreeLogger to use for debug output
   * @param partialPath the path within the module
   * @param moduleName the name of the module
   * @throws IOException
   */
  @SuppressWarnings("deprecation")
  private void doGetPublicFile(HttpServletRequest request,
      HttpServletResponse response, TreeLogger logger, String partialPath,
      String moduleName) throws IOException {

    // Create a logger branch for this request.
    logger = logger.branch(TreeLogger.TRACE,
        "The development shell servlet received a request for '"
        + partialPath + "' in module '" + moduleName + ".gwt.xml' ", null);

    // Handle auto-generation of resources.
    if (shouldAutoGenerateResources()) {
      if (autoGenerateResources(request, response, logger, partialPath,
          moduleName)) {
        return;
      }
    }

    URL foundResource = null;
    try {
      // Look for the requested file on the public path.
      //
      ModuleDef moduleDef = getModuleDef(logger, moduleName);
      if (shouldAutoGenerateResources()) {
        Resource publicResource = moduleDef.findPublicFile(partialPath);
        if (publicResource != null) {
          foundResource = publicResource.getURL();
        }

        if (foundResource == null) {
          // Look for public generated files
          File shellDir = getShellWorkDirs().getShellPublicGenDir(moduleDef);
          File requestedFile = new File(shellDir, partialPath);
          if (requestedFile.exists()) {
            try {
              foundResource = requestedFile.toURI().toURL();
            } catch (MalformedURLException e) {
              // ignore since it was speculative anyway
            }
          }
        }
      }

      /*
       * If the user is coming from compiled web-mode, check the linker output
       * directory for the real bootstrap file.
       */
      if (foundResource == null) {
        File moduleDir = getShellWorkDirs().getCompilerOutputDir(moduleDef);
        File requestedFile = new File(moduleDir, partialPath);
        if (requestedFile.exists()) {
          try {
            foundResource = requestedFile.toURI().toURL();
          } catch (MalformedURLException e) {
            // ignore since it was speculative anyway
          }
        }
      }

      if (foundResource == null) {
        String msg;
        if ("gwt.js".equals(partialPath)) {
          msg = "Loading the old 'gwt.js' bootstrap script is no longer supported; please load '"
              + moduleName + ".nocache.js' directly";
        } else {
          msg = "Resource not found: " + partialPath + "; "
              + "(could a file be missing from the public path or a <servlet> "
              + "tag misconfigured in module " + moduleName + ".gwt.xml ?)";
        }
        logger.log(TreeLogger.WARN, msg, null);
        throw new UnableToCompleteException();
      }
    } catch (UnableToCompleteException e) {
      sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND,
          "Cannot find resource '" + partialPath
              + "' in the public path of module '" + moduleName + "'");
      return;
    }

    // Get the MIME type.
    String path = foundResource.toExternalForm();
    String mimeType = null;
    try {
      mimeType = getServletContext().getMimeType(path);
    } catch (UnsupportedOperationException e) {
      // Certain minimalist servlet containers throw this.
      // Fall through to guess the type.
    }

    if (mimeType == null) {
      mimeType = guessMimeType(path);
      if (logger.isLoggable(TreeLogger.TRACE)) {
        logger.log(TreeLogger.TRACE, "Guessed MIME type '" + mimeType + "'", null);
      }
    }

    maybeIssueXhtmlWarning(logger, mimeType, partialPath);

    long cacheSeconds = getCacheTime(path);

    InputStream is = null;
    try {
      // Check for up-to-datedness.
      URLConnection conn = foundResource.openConnection();
      long lastModified = conn.getLastModified();
      if (isNotModified(request, lastModified)) {
        response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
        setResponseCacheHeaders(response, cacheSeconds);
        return;
      }

      // Set up headers to really send it.
      response.setStatus(HttpServletResponse.SC_OK);
      long now = new Date().getTime();
      response.setHeader(HttpHeaders.DATE,
          HttpHeaders.toInternetDateFormat(now));
      response.setContentType(mimeType);
      String lastModifiedStr = HttpHeaders.toInternetDateFormat(lastModified);
      response.setHeader(HttpHeaders.LAST_MODIFIED, lastModifiedStr);

      // Expiration header. Either immediately stale (requiring an
      // "If-Modified-Since") or infinitely cacheable (not requiring even a
      // freshness check).
      setResponseCacheHeaders(response, cacheSeconds);

      // Content length.
      int contentLength = conn.getContentLength();
      if (contentLength >= 0) {
        response.setHeader(HttpHeaders.CONTENT_LENGTH,
            Integer.toString(contentLength));
      }

      // Send the bytes.
      is = conn.getInputStream();
      streamOut(is, response.getOutputStream(), 1024 * 8);
    } finally {
      Utility.close(is);
    }
  }

  /**
   * Generates a module.js file on the fly. Note that the nocache file that is
   * generated that can only be used for hosted mode. It cannot produce a web
   * mode version, since this servlet doesn't know strong names, since by
   * definition of "hosted mode" JavaScript hasn't been compiled at this point.
   */
  private String genSelectionScript(TreeLogger logger, String moduleName)
      throws UnableToCompleteException {
    if (logger.isLoggable(TreeLogger.TRACE)) {
      logger.log(TreeLogger.TRACE,
          "Generating a script selection script for module " + moduleName);
    }
    ModuleDef module = getModuleDef(logger, moduleName);
    StandardLinkerContext context = new StandardLinkerContext(logger, module,
        new JJSOptionsImpl());
    ArtifactSet artifacts = context.getArtifactsForPublicResources(logger,
        module);
    HostedModeLinker linker = new HostedModeLinker();
    return linker.generateSelectionScript(logger, context, artifacts);
  }

  /**
   * Get the length of time a given file should be cacheable. If the path
   * contains *.nocache.*, it is never cacheable; if it contains *.cache.*, it
   * is infinitely cacheable; anything else gets a default time.
   * 
   * @return cache time in seconds, or 0 if the file is not cacheable at all
   */
  private long getCacheTime(String path) {
    int lastDot = path.lastIndexOf('.');
    if (lastDot >= 0) {
      String prefix = path.substring(0, lastDot);
      if (prefix.endsWith(".cache")) {
        // RFC2616 says to never give a cache time of more than a year
        // http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.21
        return HttpHeaders.SEC_YR;
      } else if (prefix.endsWith(".nocache")) {
        return 0;
      }
    }
    return DEFAULT_CACHE_SECONDS;
  }

  private synchronized TreeLogger getLogger() {
    if (topLogger == null) {
      ServletContext servletContext = getServletContext();
      final String attr = "com.google.gwt.dev.shell.logger";
      topLogger = (TreeLogger) servletContext.getAttribute(attr);
      if (topLogger == null) {
        // No shell available, so wrap the regular servlet context logger.
        //
        topLogger = new ServletContextTreeLogger(servletContext);
      }
    }
    return topLogger;
  }

  /**
   * We don't actually log this on purpose since the client does anyway.
   */
  private ModuleDef getModuleDef(TreeLogger logger, String moduleName)
      throws UnableToCompleteException {
    synchronized (loadedModulesByName) {
      ModuleDef moduleDef = loadedModulesByName.get(moduleName);
      if (moduleDef == null) {
        moduleDef = ModuleDefLoader.loadFromClassPath(logger, moduleName, false);
        loadedModulesByName.put(moduleName, moduleDef);
        loadedModulesByName.put(moduleDef.getName(), moduleDef);

        // BEGIN BACKWARD COMPATIBILITY
        // The following map of servlet path to module is included only
        // for backward-compatibility. We are going to remove this functionality
        // when we go out of beta. The new behavior is that the client should
        // specify the module name as part of the URL and construct it using
        // getModuleBaseURL().
        String[] servletPaths = moduleDef.getServletPaths();
        for (int i = 0; i < servletPaths.length; i++) {
          modulesByServletPath.put(servletPaths[i], moduleDef);
        }
        // END BACKWARD COMPATIBILITY
      }
      return moduleDef;
    }
  }

  @SuppressWarnings("unchecked")
  private Map<String, String[]> getParameterMap(HttpServletRequest request) {
    return request.getParameterMap();
  }

  private synchronized WorkDirs getShellWorkDirs() {
    if (workDirs == null) {
      ServletContext servletContext = getServletContext();
      final String attr = "com.google.gwt.dev.shell.workdirs";
      workDirs = (WorkDirs) servletContext.getAttribute(attr);
      assert (workDirs != null);
    }
    return workDirs;
  }

  private String guessMimeType(String fullPath) {
    int dot = fullPath.lastIndexOf('.');
    if (dot != -1) {
      String ext = fullPath.substring(dot + 1);
      String mimeType = mimeTypes.get(ext);
      if (mimeType != null) {
        return mimeType;
      }

      // Otherwise, fall through.
      //
    }

    // Last resort.
    //
    return "application/octet-stream";
  }

  private void initMimeTypes() {
    mimeTypes.put("abs", "audio/x-mpeg");
    mimeTypes.put("ai", "application/postscript");
    mimeTypes.put("aif", "audio/x-aiff");
    mimeTypes.put("aifc", "audio/x-aiff");
    mimeTypes.put("aiff", "audio/x-aiff");
    mimeTypes.put("aim", "application/x-aim");
    mimeTypes.put("art", "image/x-jg");
    mimeTypes.put("asf", "video/x-ms-asf");
    mimeTypes.put("asx", "video/x-ms-asf");
    mimeTypes.put("au", "audio/basic");
    mimeTypes.put("avi", "video/x-msvideo");
    mimeTypes.put("avx", "video/x-rad-screenplay");
    mimeTypes.put("bcpio", "application/x-bcpio");
    mimeTypes.put("bin", "application/octet-stream");
    mimeTypes.put("bmp", "image/bmp");
    mimeTypes.put("body", "text/html");
    mimeTypes.put("cdf", "application/x-cdf");
    mimeTypes.put("cer", "application/x-x509-ca-cert");
    mimeTypes.put("class", "application/java");
    mimeTypes.put("cpio", "application/x-cpio");
    mimeTypes.put("csh", "application/x-csh");
    mimeTypes.put("css", "text/css");
    mimeTypes.put("dib", "image/bmp");
    mimeTypes.put("doc", "application/msword");
    mimeTypes.put("dtd", "text/plain");
    mimeTypes.put("dv", "video/x-dv");
    mimeTypes.put("dvi", "application/x-dvi");
    mimeTypes.put("eps", "application/postscript");
    mimeTypes.put("etx", "text/x-setext");
    mimeTypes.put("exe", "application/octet-stream");
    mimeTypes.put("gif", "image/gif");
    mimeTypes.put("gtar", "application/x-gtar");
    mimeTypes.put("gz", "application/x-gzip");
    mimeTypes.put("hdf", "application/x-hdf");
    mimeTypes.put("hqx", "application/mac-binhex40");
    mimeTypes.put("htc", "text/x-component");
    mimeTypes.put("htm", "text/html");
    mimeTypes.put("html", "text/html");
    mimeTypes.put("hqx", "application/mac-binhex40");
    mimeTypes.put("ief", "image/ief");
    mimeTypes.put("jad", "text/vnd.sun.j2me.app-descriptor");
    mimeTypes.put("jar", "application/java-archive");
    mimeTypes.put("java", "text/plain");
    mimeTypes.put("jnlp", "application/x-java-jnlp-file");
    mimeTypes.put("jpe", "image/jpeg");
    mimeTypes.put("jpeg", "image/jpeg");
    mimeTypes.put("jpg", "image/jpeg");
    mimeTypes.put("js", "text/javascript");
    mimeTypes.put("jsf", "text/plain");
    mimeTypes.put("jspf", "text/plain");
    mimeTypes.put("kar", "audio/x-midi");
    mimeTypes.put("latex", "application/x-latex");
    mimeTypes.put("m3u", "audio/x-mpegurl");
    mimeTypes.put("mac", "image/x-macpaint");
    mimeTypes.put("man", "application/x-troff-man");
    mimeTypes.put("me", "application/x-troff-me");
    mimeTypes.put("mid", "audio/x-midi");
    mimeTypes.put("midi", "audio/x-midi");
    mimeTypes.put("mif", "application/x-mif");
    mimeTypes.put("mov", "video/quicktime");
    mimeTypes.put("movie", "video/x-sgi-movie");
    mimeTypes.put("mp1", "audio/x-mpeg");
    mimeTypes.put("mp2", "audio/x-mpeg");
    mimeTypes.put("mp3", "audio/x-mpeg");
    mimeTypes.put("mpa", "audio/x-mpeg");
    mimeTypes.put("mpe", "video/mpeg");
    mimeTypes.put("mpeg", "video/mpeg");
    mimeTypes.put("mpega", "audio/x-mpeg");
    mimeTypes.put("mpg", "video/mpeg");
    mimeTypes.put("mpv2", "video/mpeg2");
    mimeTypes.put("ms", "application/x-wais-source");
    mimeTypes.put("nc", "application/x-netcdf");
    mimeTypes.put("oda", "application/oda");
    mimeTypes.put("pbm", "image/x-portable-bitmap");
    mimeTypes.put("pct", "image/pict");
    mimeTypes.put("pdf", "application/pdf");
    mimeTypes.put("pgm", "image/x-portable-graymap");
    mimeTypes.put("pic", "image/pict");
    mimeTypes.put("pict", "image/pict");
    mimeTypes.put("pls", "audio/x-scpls");
    mimeTypes.put("png", "image/png");
    mimeTypes.put("pnm", "image/x-portable-anymap");
    mimeTypes.put("pnt", "image/x-macpaint");
    mimeTypes.put("ppm", "image/x-portable-pixmap");
    mimeTypes.put("ppt", "application/powerpoint");
    mimeTypes.put("ps", "application/postscript");
    mimeTypes.put("psd", "image/x-photoshop");
    mimeTypes.put("qt", "video/quicktime");
    mimeTypes.put("qti", "image/x-quicktime");
    mimeTypes.put("qtif", "image/x-quicktime");
    mimeTypes.put("ras", "image/x-cmu-raster");
    mimeTypes.put("rgb", "image/x-rgb");
    mimeTypes.put("rm", "application/vnd.rn-realmedia");
    mimeTypes.put("roff", "application/x-troff");
    mimeTypes.put("rtf", "application/rtf");
    mimeTypes.put("rtx", "text/richtext");
    mimeTypes.put("sh", "application/x-sh");
    mimeTypes.put("shar", "application/x-shar");
    mimeTypes.put("smf", "audio/x-midi");
    mimeTypes.put("sit", "application/x-stuffit");
    mimeTypes.put("snd", "audio/basic");
    mimeTypes.put("src", "application/x-wais-source");
    mimeTypes.put("sv4cpio", "application/x-sv4cpio");
    mimeTypes.put("sv4crc", "application/x-sv4crc");
    mimeTypes.put("swf", "application/x-shockwave-flash");
    mimeTypes.put("t", "application/x-troff");
    mimeTypes.put("tar", "application/x-tar");
    mimeTypes.put("tcl", "application/x-tcl");
    mimeTypes.put("tex", "application/x-tex");
    mimeTypes.put("texi", "application/x-texinfo");
    mimeTypes.put("texinfo", "application/x-texinfo");
    mimeTypes.put("tif", "image/tiff");
    mimeTypes.put("tiff", "image/tiff");
    mimeTypes.put("tr", "application/x-troff");
    mimeTypes.put("tsv", "text/tab-separated-values");
    mimeTypes.put("txt", "text/plain");
    mimeTypes.put("ulw", "audio/basic");
    mimeTypes.put("ustar", "application/x-ustar");
    mimeTypes.put("xbm", "image/x-xbitmap");
    mimeTypes.put("xht", "application/xhtml+xml");
    mimeTypes.put("xhtml", "application/xhtml+xml");
    mimeTypes.put("xml", "text/xml");
    mimeTypes.put("xpm", "image/x-xpixmap");
    mimeTypes.put("xsl", "text/xml");
    mimeTypes.put("xwd", "image/x-xwindowdump");
    mimeTypes.put("wav", "audio/x-wav");
    mimeTypes.put("svg", "image/svg+xml");
    mimeTypes.put("svgz", "image/svg+xml");
    mimeTypes.put("vsd", "application/x-visio");
    mimeTypes.put("wbmp", "image/vnd.wap.wbmp");
    mimeTypes.put("wml", "text/vnd.wap.wml");
    mimeTypes.put("wmlc", "application/vnd.wap.wmlc");
    mimeTypes.put("wmls", "text/vnd.wap.wmlscript");
    mimeTypes.put("wmlscriptc", "application/vnd.wap.wmlscriptc");
    mimeTypes.put("wrl", "x-world/x-vrml");
    mimeTypes.put("Z", "application/x-compress");
    mimeTypes.put("z", "application/x-compress");
    mimeTypes.put("zip", "application/zip");
  }

  /**
   * Checks to see whether or not a client's file is out of date relative to the
   * original.
   */
  private boolean isNotModified(HttpServletRequest request, long ageOfServerCopy) {
    // The age of the server copy *must* have the milliseconds truncated.
    // Since milliseconds isn't part of the GMT format, failure to truncate
    // will leave the file in a state where it appears constantly out of date
    // and yet it can never get in sync because the Last-Modified date keeps
    // truncating off the milliseconds part on its way out.
    // 
    ageOfServerCopy -= (ageOfServerCopy % 1000);

    long ageOfClientCopy = 0;
    String ifModifiedSince = request.getHeader("If-Modified-Since");
    if (ifModifiedSince != null) {
      // Rip off any additional stuff at the end, such as "; length="
      // (IE does add this).
      //
      int lastSemi = ifModifiedSince.lastIndexOf(';');
      if (lastSemi != -1) {
        ifModifiedSince = ifModifiedSince.substring(0, lastSemi);
      }
      ageOfClientCopy = HttpHeaders.fromInternetDateFormat(ifModifiedSince);
    }

    if (ageOfClientCopy >= ageOfServerCopy) {
      // The client already has a good copy.
      //
      return true;
    } else {
      // The client needs a fresh copy of the requested file.
      //
      return false;
    }
  }

  private void maybeIssueXhtmlWarning(TreeLogger logger, String mimeType,
      String path) {
    if (!XHTML_MIME_TYPE.equals(mimeType)) {
      return;
    }

    String msg = "File was returned with content-type of \"" + mimeType
        + "\". GWT requires browser features that are not available to "
        + "documents with this content-type.";

    int ix = path.lastIndexOf('.');
    if (ix >= 0 && ix < path.length()) {
      String base = path.substring(0, ix);
      msg += " Consider renaming \"" + path + "\" to \"" + base + ".html\".";
    }

    logger.log(TreeLogger.WARN, msg, null);
  }

  private void sendErrorResponse(HttpServletResponse response, int statusCode,
      String msg) throws IOException {
    response.setContentType("text/html");
    response.getWriter().println(msg);
    response.setStatus(statusCode);
  }

  /**
   * Sets the Cache-control and Expires headers in the response based on the
   * supplied cache time.
   * 
   * Expires is used in addition to Cache-control for older clients or proxies
   * which may not properly understand Cache-control.
   * 
   * @param response the HttpServletResponse to update
   * @param cacheTime non-negative number of seconds to cache the response; 0
   *          means specifically do not allow caching at all.
   * @throws IllegalArgumentException if cacheTime is negative
   */
  private void setResponseCacheHeaders(HttpServletResponse response,
      long cacheTime) {
    long expires;
    if (cacheTime < 0) {
      throw new IllegalArgumentException("cacheTime of " + cacheTime
          + " is negative");
    }
    if (cacheTime > 0) {
      // Expire the specified seconds in the future.
      expires = new Date().getTime() + cacheTime * HttpHeaders.MS_SEC;
    } else {
      // Prevent caching by using a time in the past for cache expiration.
      // Use January 2, 1970 00:00:00, to account for timezone changes
      // in case a browser tries to convert to a local timezone first
      // 0=Jan 1, so add 1 day's worth of milliseconds to get Jan 2
      expires = HttpHeaders.SEC_DAY * HttpHeaders.MS_SEC;
    }
    response.setHeader(HttpHeaders.CACHE_CONTROL,
        HttpHeaders.CACHE_CONTROL_MAXAGE + cacheTime);
    String expiresString = HttpHeaders.toInternetDateFormat(expires);
    response.setHeader(HttpHeaders.EXPIRES, expiresString);
  }

  private boolean shouldAutoGenerateResources() {
    ServletContext servletContext = getServletContext();
    final String attr = "com.google.gwt.dev.shell.shouldAutoGenerateResources";
    Boolean attrValue = (Boolean) servletContext.getAttribute(attr);
    if (attrValue == null) {
      return true;
    }
    return attrValue;
  }

  private void streamOut(InputStream in, OutputStream out, int bufferSize)
      throws IOException {
    assert (bufferSize >= 0);

    byte[] buffer = new byte[bufferSize];
    int bytesRead = 0;
    while (true) {
      bytesRead = in.read(buffer);
      if (bytesRead >= 0) {
        // Copy the bytes out.
        out.write(buffer, 0, bytesRead);
      } else {
        // End of input stream.
        out.flush();
        return;
      }
    }
  }

  private HttpServlet tryGetOrLoadServlet(TreeLogger logger,
      ModuleDef moduleDef, String className) {

    // Maps className to live servlet for this module.
    Map<String, HttpServlet> moduleServlets;
    synchronized (loadedServletsByModuleAndClassName) {
      moduleServlets = loadedServletsByModuleAndClassName.get(moduleDef);
      if (moduleServlets == null) {
        moduleServlets = new HashMap<String, HttpServlet>();
        loadedServletsByModuleAndClassName.put(moduleDef, moduleServlets);
      }
    }

    synchronized (moduleServlets) {
      HttpServlet servlet = moduleServlets.get(className);
      if (servlet != null) {
        // Found it.
        //
        return servlet;
      }

      // Try to load and instantiate it.
      //
      Throwable caught = null;
      try {
        Class<?> servletClass = Class.forName(className);
        Object newInstance = servletClass.newInstance();
        if (!(newInstance instanceof HttpServlet)) {
          logger.log(TreeLogger.ERROR,
              "Not compatible with HttpServlet: " + className
                  + " (does your service extend RemoteServiceServlet?)", null);
          return null;
        }

        // Success. Hang onto the instance so we can reuse it.
        //
        servlet = (HttpServlet) newInstance;

        // We create proxies for ServletContext and ServletConfig to enable
        // RemoteServiceServlets to load public and generated resources via
        // ServletContext.getResourceAsStream()
        //
        ServletContext context = new HostedModeServletContextProxy(
            getServletContext(), moduleDef, getShellWorkDirs());
        ServletConfig config = new HostedModeServletConfigProxy(
            getServletConfig(), context);

        servlet.init(config);

        moduleServlets.put(className, servlet);
        return servlet;
      } catch (ClassNotFoundException e) {
        caught = e;
      } catch (InstantiationException e) {
        caught = e;
      } catch (IllegalAccessException e) {
        caught = e;
      } catch (ServletException e) {
        caught = e;
      }
      String msg = "Unable to instantiate '" + className + "'";
      logger.log(TreeLogger.ERROR, msg, caught);
      return null;
    }
  }
}
