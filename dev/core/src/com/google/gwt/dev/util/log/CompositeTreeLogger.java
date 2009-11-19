/**
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
package com.google.gwt.dev.util.log;

import com.google.gwt.core.ext.TreeLogger;

/**
 * Forks logging over two child loggers. This provides the graphics + file
 * logging of DevModeBase's -logfile option.
 */
public class CompositeTreeLogger extends TreeLogger {

  private TreeLogger[] loggers;

  public CompositeTreeLogger(TreeLogger... loggers) {
    this.loggers = loggers;
  }

  @Override
  public TreeLogger branch(Type type, String msg, Throwable caught,
      HelpInfo helpInfo) {
    TreeLogger children[] = new TreeLogger[loggers.length];
    for (int i = 0; i < loggers.length; i++) {
      children[i] = loggers[i].branch(type, msg, caught, helpInfo);
    }
    return new CompositeTreeLogger(children);
  }

  @Override
  public boolean isLoggable(Type type) {
    for (TreeLogger logger : loggers) {
      if (logger.isLoggable(type)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void log(Type type, String msg, Throwable caught, HelpInfo helpInfo) {
    for (TreeLogger logger : loggers) {
      logger.log(type, msg, caught, helpInfo);
    }
  }
}
