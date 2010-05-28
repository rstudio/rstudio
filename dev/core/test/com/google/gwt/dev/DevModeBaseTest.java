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
package com.google.gwt.dev;

import junit.framework.TestCase;

public class DevModeBaseTest extends TestCase {
  static final String MANAGER_PROPERTY = "java.util.logging.manager";
  static final String NEW_MANAGER = "com.google.gwt.dev.shell.DevModeLogManager";
  static final String OLD_MANAGER_PROPERTY = "java.util.logging.oldLogManager";
  static final String USERS_MANAGER = "UsersLogManager";
  
  public void testSetLogManager() {
    assertEquals(null, System.getProperty(MANAGER_PROPERTY));
    assertEquals(null, System.getProperty(OLD_MANAGER_PROPERTY));
    
    DevModeBase.setLogManager();
    assertEquals(NEW_MANAGER, System.getProperty(MANAGER_PROPERTY));
    assertEquals(null, System.getProperty(OLD_MANAGER_PROPERTY));

    System.setProperty(MANAGER_PROPERTY, USERS_MANAGER);
    DevModeBase.setLogManager();
    assertEquals(NEW_MANAGER, System.getProperty(MANAGER_PROPERTY));
    assertEquals(USERS_MANAGER, System.getProperty(OLD_MANAGER_PROPERTY));
  }

}
