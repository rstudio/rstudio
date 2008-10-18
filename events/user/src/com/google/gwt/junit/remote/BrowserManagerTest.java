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
package com.google.gwt.junit.remote;

import java.rmi.Naming;

/**
 * Sanity checks a running {@link BrowserManagerServer} to verify that it is
 * operating correctly. This class is experimental and unsupported.
 */
public class BrowserManagerTest {

  private static final int KEEP_ALIVE_MICROSECONDS = 5000;

  /**
   * Causes the server at the specified URL to launch a window to www.google.com
   * for 5 seconds, then close it.
   * 
   * @param args the url to the remote BrowserManagerServer; e.g.
   *          "rmi://localhost/ie6"
   * @throws Exception
   */
  public static void main(String[] args) throws Exception {
    if (args.length != 1) {
      throw new IllegalArgumentException("Expecting exactly 1 argument.");
    }
    BrowserManager browserManager = (BrowserManager) Naming.lookup(args[0]);
    browserManager.launchNewBrowser("www.google.com", KEEP_ALIVE_MICROSECONDS);
  }

  /**
   * Not instantiable.
   */
  private BrowserManagerTest() {
  }
}
