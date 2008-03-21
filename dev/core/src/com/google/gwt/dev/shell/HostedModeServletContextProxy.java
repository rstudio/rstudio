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

import com.google.gwt.dev.GWTShell;
import com.google.gwt.dev.cfg.ModuleDef;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Set;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

/**
 * ServletContext proxy that implements the getResource and getResourceAsStream
 * members so that they can work with the {@link GWTShellServlet}.
 */
class HostedModeServletContextProxy implements ServletContext {
  private final ServletContext context;
  /**
   * Avoid pinning my moduleDef.
   */
  private final WeakReference<ModuleDef> moduleDefRef;
  private final File outDir;

  HostedModeServletContextProxy(ServletContext context, ModuleDef moduleDef,
      File outDir) {
    this.context = context;
    this.moduleDefRef = new WeakReference<ModuleDef>(moduleDef);
    this.outDir = outDir;
  }

  /**
   * @param arg0
   * @return
   * @see javax.servlet.ServletContext#getAttribute(java.lang.String)
   */
  public Object getAttribute(String arg0) {
    return context.getAttribute(arg0);
  }

  /**
   * @return
   * @see javax.servlet.ServletContext#getAttributeNames()
   */
  public Enumeration<String> getAttributeNames() {
    return context.getAttributeNames();
  }

  /**
   * @param arg0
   * @return
   * @see javax.servlet.ServletContext#getContext(java.lang.String)
   */
  public ServletContext getContext(String arg0) {
    return context.getContext(arg0);
  }

  /**
   * @param arg0
   * @return
   * @see javax.servlet.ServletContext#getInitParameter(java.lang.String)
   */
  public String getInitParameter(String arg0) {
    return context.getInitParameter(arg0);
  }

  /**
   * @return
   * @see javax.servlet.ServletContext#getInitParameterNames()
   */
  public Enumeration<?> getInitParameterNames() {
    return context.getInitParameterNames();
  }

  /**
   * @return
   * @see javax.servlet.ServletContext#getMajorVersion()
   */
  public int getMajorVersion() {
    return context.getMajorVersion();
  }

  /**
   * @param arg0
   * @return
   * @see javax.servlet.ServletContext#getMimeType(java.lang.String)
   */
  public String getMimeType(String arg0) {
    return context.getMimeType(arg0);
  }

  /**
   * @return
   * @see javax.servlet.ServletContext#getMinorVersion()
   */
  public int getMinorVersion() {
    return context.getMinorVersion();
  }

  /**
   * @param arg0
   * @return
   * @see javax.servlet.ServletContext#getNamedDispatcher(java.lang.String)
   */
  public RequestDispatcher getNamedDispatcher(String arg0) {
    return context.getNamedDispatcher(arg0);
  }

  /**
   * @param arg0
   * @return
   * @see javax.servlet.ServletContext#getRealPath(java.lang.String)
   */
  public String getRealPath(String arg0) {
    return context.getRealPath(arg0);
  }

  /**
   * @param arg0
   * @return
   * @see javax.servlet.ServletContext#getRequestDispatcher(java.lang.String)
   */
  public RequestDispatcher getRequestDispatcher(String arg0) {
    return context.getRequestDispatcher(arg0);
  }

  /**
   * @param arg0
   * @return
   * @throws MalformedURLException
   * @see javax.servlet.ServletContext#getResource(java.lang.String)
   */
  public URL getResource(String path) throws MalformedURLException {
    ModuleDef moduleDef = moduleDefRef.get();
    if (moduleDef == null) {
      return context.getResource(path);
    }

    String moduleContext = "/" + moduleDef.getName() + "/";
    if (!path.startsWith(moduleContext)) {
      // This path is in a different context; just return null
      return null;
    }

    String partialPath = path.substring(moduleContext.length());

    // Try to get the resource from the application's public path
    URL url = moduleDef.findPublicFile(partialPath);
    if (url == null) {
      // Otherwise try the path but rooted in the shell's output directory
      File shellDir = new File(outDir, GWTShell.GWT_SHELL_PATH + File.separator
          + moduleDef.getName());
      File requestedFile = new File(shellDir, partialPath);
      if (requestedFile.exists()) {
        url = requestedFile.toURI().toURL();
      }
    }

    /*
     * If the user is coming from compiled web-mode, check the linker output
     * directory for the file. We'll default to using the output directory of
     * the first linker defined in the <set-linker> tab.
     */
    if (url == null) {
      File requestedFile = new File(new File(outDir, moduleDef.getName()),
          partialPath);
      if (requestedFile.exists()) {
        try {
          url = requestedFile.toURI().toURL();
        } catch (MalformedURLException e) {
          // ignore since it was speculative anyway
        }
      }
    }

    return url;
  }

  /**
   * @param arg0
   * @return
   * @see javax.servlet.ServletContext#getResourceAsStream(java.lang.String)
   */
  public InputStream getResourceAsStream(String arg0) {
    URL url;
    try {
      url = getResource(arg0);
      if (url != null) {
        return url.openStream();
      }
    } catch (MalformedURLException e) {
      // Ignore the exception; return null
    } catch (IOException e) {
      // Ignore the exception; return null
    }

    return null;
  }

  /**
   * 
   * @param path
   * @return
   * @see javax.servlet.ServletContext#getResourcePaths(java.lang.String)
   */
  public Set<String> getResourcePaths(String path) {
    return context.getResourcePaths(path);
  }

  /**
   * @return
   * @see javax.servlet.ServletContext#getServerInfo()
   */
  public String getServerInfo() {
    return context.getServerInfo();
  }

  /**
   * @param arg0
   * @return
   * @throws ServletException
   * @deprecated
   * @see javax.servlet.ServletContext#getServlet(java.lang.String)
   */
  @Deprecated
  public Servlet getServlet(String arg0) throws ServletException {
    return context.getServlet(arg0);
  }

  /**
   * @return
   * @see javax.servlet.ServletContext#getServletContextName()
   */
  public String getServletContextName() {
    return context.getServletContextName();
  }

  /**
   * @return
   * @deprecated
   * @see javax.servlet.ServletContext#getServletNames()
   */
  @Deprecated
  public Enumeration<String> getServletNames() {
    return context.getServletNames();
  }

  /**
   * @return
   * @deprecated
   * @see javax.servlet.ServletContext#getServlets()
   */
  @Deprecated
  public Enumeration<Servlet> getServlets() {
    return context.getServlets();
  }

  /**
   * @param arg0
   * @param arg1
   * @deprecated
   * @see javax.servlet.ServletContext#log(java.lang.Exception,
   *      java.lang.String)
   */
  @Deprecated
  public void log(Exception arg0, String arg1) {
    context.log(arg0, arg1);
  }

  /**
   * @param arg0
   * @see javax.servlet.ServletContext#log(java.lang.String)
   */
  public void log(String arg0) {
    context.log(arg0);
  }

  /**
   * @param arg0
   * @param arg1
   * @see javax.servlet.ServletContext#log(java.lang.String,java.lang.Throwable)
   */
  public void log(String arg0, Throwable arg1) {
    context.log(arg0, arg1);
  }

  /**
   * @param arg0
   * @see javax.servlet.ServletContext#removeAttribute(java.lang.String)
   */
  public void removeAttribute(String arg0) {
    context.removeAttribute(arg0);
  }

  /**
   * @param arg0
   * @param arg1
   * @see javax.servlet.ServletContext#setAttribute(java.lang.String,java.lang.Object)
   */
  public void setAttribute(String arg0, Object arg1) {
    context.setAttribute(arg0, arg1);
  }
}
