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
package com.google.gwt.junit;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;

/**
 * An abstract class that handles the details of launching a browser.
 */
abstract class RunStyle {

  protected int tries = 1;
  
  /**
   * The containing shell.
   */
  protected final JUnitShell shell;

  /**
   * Constructor for RunStyle.  Any subclass must provide a constructor with
   * the same signature since this will be how the RunStyle is created via
   * reflection.
   * 
   * @param shell the containing shell
   */
  public RunStyle(JUnitShell shell) {
    this.shell = shell;
  }

  /**
   * Tests whether the test was interrupted.
   * 
   * @return the interrupted hosts, or null if not interrupted
   */
  public String[] getInterruptedHosts() {
    return null;
  }

  /**
   * Returns the number of times this test should be tried to run. A test
   * succeeds if it succeeds even once.
   */
  public int getTries() {
    return tries;
  }

  /**
   * Initialize the runstyle with any supplied arguments.
   * 
   * @param args arguments passed in -runStyle option, null if none supplied
   * @return true if this runstyle is initialized successfully, false if it
   *     was unsuccessful
   */
  public boolean initialize(String args) {
    return true;
  }

  /**
   * Requests initial launch of the browser. This should only be called once per
   * instance of RunStyle.
   * 
   * @param moduleName the module to run
   * @throws UnableToCompleteException
   */
  public abstract void launchModule(String moduleName)
      throws UnableToCompleteException;

  public void setTries(int tries) {
    this.tries = tries;
  }

  /**
   * Setup this RunStyle for the selected mode. 
   * 
   * @param logger TreeLogger to use for any messages 
   * @param developmentMode true if we are running in development mode
   *     rather that web/production mode
   * @return false if we should abort processing due to an unsupported mode
   *     or an error setting up for that mode
   */
  public boolean setupMode(TreeLogger logger, boolean developmentMode) {
    return true;
  }

  /**
   * Whether the embedded server should ever generate resources.  Hosted mode
   * needs this, but not noserver hosted.  TODO(spoon) does web mode get
   * simpler if this is turned on?
   */
  public boolean shouldAutoGenerateResources() {
    return true;
  }

  /**
   * Gets the shell logger.
   */
  protected TreeLogger getLogger() {
    return shell.getTopLogger();
  }

}
