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
 * Runs in Production Mode waiting for the user to contact the server with their own
 * browser.
 */
class RunStyleManual extends RunStyle {

  private int numClients;

  public RunStyleManual(JUnitShell shell) {
    super(shell);
  }

  @Override
  public int initialize(String args) {
    numClients = 1;
    if (args != null) {
      try {
        numClients = Integer.parseInt(args);
      } catch (NumberFormatException e) {
        getLogger().log(TreeLogger.ERROR,
            "Error parsing argument \"" + args + "\"", e);
        return -1;
      }
    }
    return numClients;
  }

  @Override
  public void launchModule(String moduleName) throws UnableToCompleteException {
    if (numClients == 1) {
      System.out.println("Please navigate your browser to this URL:");
    } else {
      System.out.println("Please navigate " + numClients
          + " browsers to this URL:");
    }
    System.out.println(shell.getModuleUrl(moduleName));
  }
}
