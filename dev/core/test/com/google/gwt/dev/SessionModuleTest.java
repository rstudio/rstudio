/*
 * Copyright 2009 Google Inc.
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

/**
 * Tests for SessionModule.
 */
public class SessionModuleTest extends TestCase {

  public static final Disconnectable mockDisc1 = new Disconnectable() {
    @Override
    public void disconnect() {
    }

    @Override
    public boolean isDisconnected() {
      return false;
    }
  };

  public static final Disconnectable mockDisc2 = new Disconnectable() {
    @Override
    public void disconnect() {
    }

    @Override
    public boolean isDisconnected() {
      return false;
    }
  };

  public void testInstanceCache() {
    String sessionKey1 = "session1";
    String moduleName1 = "module1";
    SessionModule sm1 = SessionModule.create(sessionKey1, mockDisc1,
        moduleName1);
    SessionModule sm2 = SessionModule.create(sessionKey1, mockDisc1,
        moduleName1);
    assertSame(sm1, sm2);
    String sessionKey2 = "session" + "1"; // make sure it is a new string
    sm2 = SessionModule.create(sessionKey2, mockDisc1,
        moduleName1);
    assertSame(sm1, sm2);
    sessionKey2 = "session21";
    sm2 = SessionModule.create(sessionKey2, mockDisc1,
        moduleName1);
    assertNotSame(sm1, sm2);
    sm2 = SessionModule.create(sessionKey1, mockDisc2,
        moduleName1);
    assertNotSame(sm1, sm2);
    String moduleName2 = "module2";
    sm2 = SessionModule.create(sessionKey1, mockDisc1,
        moduleName2);
    assertNotSame(sm1, sm2);
  }
}
