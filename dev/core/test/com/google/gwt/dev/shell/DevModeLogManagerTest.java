/*
 * Copyright 2010 Google Inc.
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

import junit.framework.TestCase;

import java.util.logging.LogManager;
import java.util.logging.Logger;

public class DevModeLogManagerTest extends TestCase {
  private static String LOGGER_1_NAME = "Logger1";
  private static String LOGGER_2_NAME = "Logger2";
  
  /**
   * Mocks out the detection of client code since I don't know how to change
   * Thread.currentThread to be an instance of CodeServerThread in a unit test.
   */
  protected static class DevModeManagerMock extends DevModeLogManager {
    private boolean isClientCode = false;
    
    public void setIsClientCode(boolean value) {
      isClientCode = value;
    }
    
    @Override
    protected boolean isClientCode() {
      return isClientCode;
    }
  }
  
  public void testDelegation() {
    Logger logger1 = Logger.getLogger(LOGGER_1_NAME);
    Logger logger2 = Logger.getLogger(LOGGER_2_NAME);
    DevModeManagerMock devManager = new DevModeManagerMock();
    LogManager delegate = devManager.clientLogManager.get();
    assertNull(devManager.getLogger(LOGGER_1_NAME));
    assertNull(devManager.getLogger(LOGGER_2_NAME));
    assertNull(delegate.getLogger(LOGGER_1_NAME));
    assertNull(delegate.getLogger(LOGGER_2_NAME));
        
    // devManager delegates to the delegate
    devManager.setIsClientCode(false);
    devManager.addLogger(logger1);
    assertNotNull(devManager.getLogger(LOGGER_1_NAME));
    assertNull(delegate.getLogger(LOGGER_1_NAME));
      
    // devManager delegates to super
    devManager.setIsClientCode(true);
    devManager.addLogger(logger2);
    assertNotNull(devManager.getLogger(LOGGER_2_NAME));
    assertNotNull(delegate.getLogger(LOGGER_2_NAME));
  }

}
