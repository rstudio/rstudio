/*
 * Copyright 2006 Google Inc.
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
package com.google.gwt.dev.shell.log;

import com.google.gwt.dev.util.log.AbstractTreeLogger;

import javax.servlet.ServletContext;

/**
 * Tree logger that logs servlet context information.
 */
public class ServletContextTreeLogger extends AbstractTreeLogger {

  private final ServletContext ctx;

  public ServletContextTreeLogger(ServletContext ctx) {
    this.ctx = ctx;
  }

  @Override
  protected AbstractTreeLogger doBranch() {
    return new ServletContextTreeLogger(ctx);
  }

  @Override
  protected void doCommitBranch(AbstractTreeLogger childBeingCommitted,
      Type type, String msg, Throwable caught, HelpInfo helpInfo) {
    doLog(childBeingCommitted.getBranchedIndex(), type, msg, caught, helpInfo);
  }

  @Override
  protected void doLog(int indexOfLogEntryWithinParentLogger, Type type,
      String msg, Throwable caught, HelpInfo helpInfo) {
    if (caught != null) {
      ctx.log(msg, caught);
    } else {
      ctx.log(msg);
    }
  }
}
