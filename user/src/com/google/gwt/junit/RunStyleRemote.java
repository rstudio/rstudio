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

import com.google.gwt.core.ext.UnableToCompleteException;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Runs remotely in web mode. This feature is experimental and is not officially
 * supported.
 */
abstract class RunStyleRemote extends RunStyle {

  public RunStyleRemote(JUnitShell shell) {
    super(shell);
  }

  @Override
  public boolean isLocal() {
    return false;
  }

  @Override
  public void maybeCompileModule(String moduleName)
      throws UnableToCompleteException {
    System.out.println("Compiling " + moduleName + "...");
    shell.compileForWebMode(moduleName, JUnitShell.getRemoteUserAgents());
    System.out.println(" success.");
  }

  protected String getMyUrl(String moduleName) {
    try {
      String localhost = InetAddress.getLocalHost().getHostAddress();
      return "http://" + localhost + ":" + shell.getPort() + "/"
          + getUrlSuffix(moduleName);
    } catch (UnknownHostException e) {
      throw new RuntimeException("Unable to determine my ip address", e);
    }
  }
}
