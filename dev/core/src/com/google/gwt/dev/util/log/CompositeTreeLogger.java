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

/**
 * Forks logging over two child loggers.  This provides the graphics + file
 * logging of HostedModeBase's -logfile option.
 */
public class CompositeTreeLogger extends AbstractTreeLogger {

  private AbstractTreeLogger[] loggers;
  
  public CompositeTreeLogger(AbstractTreeLogger... loggers) {
    this.loggers = loggers;
  }

  @Override
  protected AbstractTreeLogger doBranch() {
    AbstractTreeLogger children[] = new AbstractTreeLogger[loggers.length];
    for (int i = 0; i < loggers.length; i++) {
      children[i] = loggers[i].doBranch();      
      children[i].indexWithinMyParent = loggers[i].allocateNextChildIndex();
      children[i].parent = loggers[i];
      children[i].logLevel = loggers[i].logLevel;
    }
    return new CompositeTreeLogger(children);
  }

  @Override
  protected void doCommitBranch(AbstractTreeLogger childBeingCommitted,
      Type type, String msg, Throwable caught, HelpInfo helpInfo) {
    CompositeTreeLogger child = (CompositeTreeLogger) childBeingCommitted;
    assert loggers.length == child.loggers.length;
    for (int i = 0; i < loggers.length; i++) {
      loggers[i].doCommitBranch(child.loggers[i], type, msg, caught, helpInfo);      
    }
  }

  @Override
  protected void doLog(int indexOfLogEntryWithinParentLogger, Type type,
      String msg, Throwable caught, HelpInfo helpInfo) {
    for (AbstractTreeLogger logger : loggers) {
      logger.doLog(indexOfLogEntryWithinParentLogger, type, msg, caught,
          helpInfo);
    }
  }
}
