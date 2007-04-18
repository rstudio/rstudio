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
package com.google.gwt.dev.shell;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.cfg.ModuleDefLoader;
import com.google.gwt.dev.util.HttpHeaders;
import com.google.gwt.dev.util.SelectionScriptGenerator;
import com.google.gwt.dev.util.log.ServletContextTreeLogger;

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
import java.util.Iterator;
import java.util.Map;

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

  private static final String XHTML_MIME_TYPE = "application/xhtml+xml";
  
  private final Map loadedModulesByName = new HashMap();

  private final Map loadedServletsByClassName = new HashMap();

  private final Map mimeTypes = new HashMap();

  private final Map modulesByServletPath = new HashMap();

  private int nextRequestId;

  private File outDir;

  private final Object requestIdLock = new Object();

  private TreeLogger topLogger;

  public GWTShellServlet() {
    initMimeTypes();
  }

  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    processFileRequest(request, response);
  }

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

    try {
      // Attempt to split the URL into module/path, which we'll use to see
      // if we can map the request to a module's servlet.
      RequestParts parts = new RequestParts(request);

      // See if the request references a module we know.
      // Note that we do *not* actually try to load the module here, because
      // we're only looking for servlet invocations, which can only happen
      // when we have *already* loaded the destination module to serve up the
      // client code in the first place.
      ModuleDef moduleDef = (ModuleDef) loadedModulesByName.get(parts.moduleName);
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
      ModuleDef moduleDef = (ModuleDef) modulesByServletPath.get(path);
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
      HttpServlet delegatee = tryGetOrLoadServlet(logger, servletClassName);
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

    // If the request is of the form ".../moduleName.nocache.js" or
    // ".../moduleName.nocache-xs.js" then generate the selection script for
    // them.
    boolean nocacheHtml = partialPath.equals(moduleName + ".nocache.js");
    boolean nocacheScript = !nocacheHtml
        && partialPath.equals(moduleName + ".nocache-xs.js");
    if (nocacheHtml || nocacheScript) {
      // If the '?compiled' request property is specified, don't auto-generate.
      if (request.getParameter("compiled") == null) {
        // Generate the .js file.
        try {
          String js = genSelectionScript(logger, moduleName, nocacheScript);
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
    }

    return false;
  }

  private void doGetModule(HttpServletRequest request,
      HttpServletResponse response, TreeLogger logger, RequestParts parts)
      throws IOException {

    if ("favicon.ico".equalsIgnoreCase(parts.moduleName)) {
      sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND,
          "Icon not available");
      return;
    }

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
          "Unable to find/load module '" + parts.moduleName
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
    //
    Map params = request.getParameterMap();
    for (Iterator iter = params.entrySet().iterator(); iter.hasNext();) {
      Map.Entry entry = (Map.Entry) iter.next();
      String[] values = (String[]) entry.getValue();
      if (values.length > 0) {
        writer.print("<meta name='gwt:property' content='");
        writer.print(entry.getKey());
        writer.print("=");
        writer.print(values[values.length - 1]);
        writer.println("'>");
      }
    }

    writer.println("</head><body>");
    writer.println("<iframe id='__gwt_historyFrame' style='width:0;height:0;border:0'></iframe>");
    writer.println("</body></html>");

    // Done.
  }

  private void doGetPublicFile(HttpServletRequest request,
      HttpServletResponse response, TreeLogger logger, String partialPath,
      String moduleName) throws IOException {

    // Create a logger branch for this request.
    String msg = "The development shell servlet received a request for '"
        + partialPath + "' in module '" + moduleName + "' ";
    logger = logger.branch(TreeLogger.TRACE, msg, null);

    // Handle auto-generation of resources.
    if (autoGenerateResources(request, response, logger, partialPath,
        moduleName)) {
      return;
    }

    URL foundResource;
    try {
      // Look for the requested file on the public path.
      //
      ModuleDef moduleDef = getModuleDef(logger, moduleName);
      foundResource = moduleDef.findPublicFile(partialPath);

      if (foundResource == null) {
        // Look in the place where we write compiled output.
        File moduleDir = new File(getOutputDir(), moduleName);
        File requestedFile = new File(moduleDir, partialPath);
        if (requestedFile.exists()) {
          try {
            foundResource = requestedFile.toURL();
          } catch (MalformedURLException e) {
            // ignore since it was speculative anyway
          }
        }

        if (foundResource == null) {
          msg = "Resource not found: " + partialPath;
          logger.log(TreeLogger.WARN, msg, null);
          throw new UnableToCompleteException();
        }
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
      msg = "Guessed MIME type '" + mimeType + "'";
      logger.log(TreeLogger.TRACE, msg, null);
    }

    maybeIssueXhtmlWarning(logger, mimeType, partialPath);
    
    // Maybe serve it up. Don't let the client cache anything other than
    // xxx.cache.yyy files because this servlet is for development (so user
    // files are assumed to change a lot), although we do honor
    // "If-Modified-Since".

    boolean infinitelyCacheable = isInfinitelyCacheable(path);

    InputStream is = null;
    try {
      // Check for up-to-datedness.
      URLConnection conn = foundResource.openConnection();
      long lastModified = conn.getLastModified();
      if (isNotModified(request, lastModified)) {
        response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
        if (infinitelyCacheable) {
          response.setHeader(HttpHeaders.CACHE_CONTROL,
              HttpHeaders.CACHE_CONTROL_MAXAGE_FOREVER);
        }
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
      String maxAgeStr;
      if (infinitelyCacheable) {
        maxAgeStr = HttpHeaders.CACHE_CONTROL_MAXAGE_FOREVER;
      } else {
        maxAgeStr = HttpHeaders.CACHE_CONTROL_MAXAGE_EXPIRED;
      }
      response.setHeader(HttpHeaders.CACHE_CONTROL, maxAgeStr);

      // Content length.
      int contentLength = conn.getContentLength();
      if (contentLength >= 0) {
        response.setHeader(HttpHeaders.CONTENT_LENGTH,
            Integer.toString(contentLength));
      }

      // Send the bytes.
      is = foundResource.openStream();
      streamOut(is, response.getOutputStream(), 1024 * 8);
    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (IOException swallowed) {
          // Nothing we can do now.
          //
        }
      }
    }
  }

  /**
   * Generates a module.js file on the fly. Note that the nocache file that is
   * generated that can only be used for hosted mode. It cannot produce a web
   * mode version, since this servlet doesn't know strong names, since by
   * definition of "hosted mode" JavaScript hasn't been compiled at this point.
   */
  private String genSelectionScript(TreeLogger logger, String moduleName,
      boolean asScript) throws UnableToCompleteException {
    String msg = asScript ? "Generating a script selection script for module "
        : "Generating an html selection script for module ";
    msg += moduleName;
    logger.log(TreeLogger.TRACE, msg, null);

    ModuleDef moduleDef = getModuleDef(logger, moduleName);
    SelectionScriptGenerator gen = new SelectionScriptGenerator(moduleDef);
    return gen.generateSelectionScript(false, asScript);
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
      ModuleDef moduleDef = (ModuleDef) loadedModulesByName.get(moduleName);
      if (moduleDef == null) {
        moduleDef = ModuleDefLoader.loadFromClassPath(logger, moduleName);
        loadedModulesByName.put(moduleName, moduleDef);

        // BEGIN BACKWARD COMPATIBILITY
        // The following map of servlet path to module is included only
        // for backward-compatibility. We are going to remove this functionality
        // when we go out of beta. The new behavior is that the client should
        // specify the module name as part of the URL and construct it using
        // getModuleBaseURL().
        String[] servletPaths = moduleDef.getServletPaths();
        for (int i = 0; i < servletPaths.length; i++) {
          ModuleDef oldDef = (ModuleDef) modulesByServletPath.put(
              servletPaths[i], moduleDef);
          if (oldDef != null) {
            logger.log(TreeLogger.WARN, "Undefined behavior: Servlet path "
                + servletPaths[i] + " conflicts in modules "
                + moduleDef.getName() + " and " + oldDef.getName(), null);
          }
        }
        // END BACKWARD COMPATIBILITY
      }
      return moduleDef;
    }
  }

  private synchronized File getOutputDir() {
    if (outDir == null) {
      ServletContext servletContext = getServletContext();
      final String attr = "com.google.gwt.dev.shell.outdir";
      outDir = (File) servletContext.getAttribute(attr);
      assert (outDir != null);
    }
    return outDir;
  }

  private String guessMimeType(String fullPath) {
    int dot = fullPath.lastIndexOf('.');
    if (dot != -1) {
      String ext = fullPath.substring(dot + 1);
      String mimeType = (String) mimeTypes.get(ext);
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
   * A file is infinitely cacheable if it ends with ".cache.xxx", where "xxx"
   * can be any extension.
   */
  private boolean isInfinitelyCacheable(String path) {
    int lastDot = path.lastIndexOf('.');
    if (lastDot >= 0) {
      if (path.substring(0, lastDot).endsWith(".cache")) {
        return true;
      }
    }
    return false;
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
        return;
      }
    }
  }

  private HttpServlet tryGetOrLoadServlet(TreeLogger logger, String className) {
    synchronized (loadedServletsByClassName) {
      HttpServlet servlet = (HttpServlet) loadedServletsByClassName.get(className);
      if (servlet != null) {
        // Found it.
        //
        return servlet;
      }

      // Try to load and instantiate it.
      //
      Throwable caught = null;
      try {
        Class servletClass = Class.forName(className);
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

        servlet.init(getServletConfig());

        loadedServletsByClassName.put(className, servlet);
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
