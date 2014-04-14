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

import java.util.Enumeration;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

/**
 * {@link ServletConfig} proxy which ensures that an un-proxied
 * {@link ServletContext} is never returned to a servlet in hosted mode.
 */
class HostedModeServletConfigProxy implements ServletConfig {
  private final ServletConfig config;
  private final ServletContext context;

  public HostedModeServletConfigProxy(ServletConfig config,
      ServletContext context) {
    this.config = config;
    this.context = context;
  }

  /**
   * @param arg0
   * @return
   * @see javax.servlet.ServletConfig#getInitParameter(java.lang.String)
   */
  @Override
  public String getInitParameter(String arg0) {
    return config.getInitParameter(arg0);
  }

  /**
   * @return
   * @see javax.servlet.ServletConfig#getInitParameterNames()
   */
  @Override
  @SuppressWarnings("unchecked")
  public Enumeration<String> getInitParameterNames() {
    return config.getInitParameterNames();
  }

  /**
   * @return
   * @see javax.servlet.ServletConfig#getServletContext()
   */
  @Override
  public ServletContext getServletContext() {
    return context;
  }

  /**
   * @return
   * @see javax.servlet.ServletConfig#getServletName()
   */
  @Override
  public String getServletName() {
    return config.getServletName();
  }
}
