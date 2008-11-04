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
import com.google.gwt.core.ext.linker.impl.StandardLinkerContext;
import com.google.gwt.dev.ShellOptions;
import com.google.gwt.dev.cfg.ModuleDef;

import org.apache.commons.collections.map.AbstractReferenceMap;
import org.apache.commons.collections.map.ReferenceIdentityMap;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

/**
 * Built-in servlet for convenient access to the public path of a specified
 * module.
 */
public class GWTShellServletFilter implements Filter {

  private final Map<String, ModuleDef> autogenScripts = new HashMap<String, ModuleDef>();
  /**
   * Maintains a persistent map of linker contexts for each module, for
   * incremental re-link with new generated artifacts.
   */
  @SuppressWarnings("unchecked")
  private final Map<ModuleDef, StandardLinkerContext> linkerContextsByModule = new ReferenceIdentityMap(
      AbstractReferenceMap.WEAK, AbstractReferenceMap.HARD, true);

  private TreeLogger logger;

  private final ShellOptions options;

  public GWTShellServletFilter(TreeLogger logger, ShellOptions options,
      ModuleDef[] moduleDefs) {
    this.logger = logger;
    this.options = options;
    for (ModuleDef moduleDef : moduleDefs) {
      String scriptName = moduleDef.getDeployTo() + moduleDef.getName()
          + ".nocache.js";
      autogenScripts.put(scriptName, moduleDef);
    }
  }

  public void destroy() {
  }

  public void doFilter(ServletRequest req, ServletResponse resp,
      FilterChain chain) throws IOException, ServletException {

    if (req instanceof HttpServletRequest) {
      HttpServletRequest request = (HttpServletRequest) req;
      String pathInfo = request.getRequestURI();
      logger.log(TreeLogger.TRACE, "Request for: " + pathInfo);
      ModuleDef moduleDef = autogenScripts.get(pathInfo);
      if (moduleDef != null) {
        /*
         * If the '?compiled' request property is specified, don't
         * auto-generate.
         * 
         * TODO(scottb): does this even do anything anymore?
         * 
         * TODO(scottb): how do we avoid clobbering a compiled selection script?
         */
        if (req.getParameter("compiled") == null) {
          try {
            // Run the linkers for hosted mode.
            hostedModeLink(logger.branch(TreeLogger.TRACE, "Request for '"
                + pathInfo + "' maps to script generator for module '"
                + moduleDef.getName() + "'"), moduleDef);
          } catch (UnableToCompleteException e) {
            /*
             * The error will have already been logged. Continue, since this
             * could actually be a request for a static file that happens to
             * have an unfortunately confusing name.
             */
          }
        }
      }
    }

    // Do normal handling, knowing that the linkers may have run earlier to
    // produce files we are just about to serve.
    chain.doFilter(req, resp);
  }

  public void init(FilterConfig filterConfig) throws ServletException {
  }

  /**
   * Called when new generated artifacts are produced.
   */
  public void relink(TreeLogger logger, ModuleDef moduleDef,
      ArtifactSet newArtifacts) throws UnableToCompleteException {
    StandardLinkerContext context = linkerContextsByModule.get(moduleDef);
    assert context != null;

    ArtifactSet artifacts = context.invokeRelink(logger, newArtifacts);
    dumpArtifacts(logger, moduleDef, context, artifacts);
  }

  private void dumpArtifacts(TreeLogger logger, ModuleDef moduleDef,
      StandardLinkerContext context, ArtifactSet artifacts)
      throws UnableToCompleteException {
    File outputPath = new File(options.getOutDir(), moduleDef.getDeployTo());
    File extraPath = null;
    if (options.getExtraDir() != null) {
      extraPath = new File(options.getExtraDir(), moduleDef.getDeployTo());
    }
    context.produceOutputDirectory(logger, artifacts, outputPath, extraPath);
  }

  private void hostedModeLink(TreeLogger logger, ModuleDef moduleDef)
      throws UnableToCompleteException {
    String moduleName = moduleDef.getName();
    logger.log(TreeLogger.TRACE, "Running linkers for module " + moduleName);

    // TODO: blow away artifacts from a previous link.

    // Perform the initial link.
    StandardLinkerContext context = new StandardLinkerContext(logger,
        moduleDef, options);
    ArtifactSet artifacts = context.invokeLink(logger);
    dumpArtifacts(logger, moduleDef, context, artifacts);

    // Save off a new active link state (which may overwrite an old one).
    linkerContextsByModule.put(moduleDef, context);
  }
}
