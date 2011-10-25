/*
 * Copyright 2011 Google Inc.
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

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.Messages;
import com.google.gwt.i18n.client.LocalizableResource.Description;
import com.google.gwt.i18n.client.Messages.DefaultMessage;

import junit.framework.TestCase;

/**
 * Test of {@see GWTMockUtilities}
 */
public class GWTMockUtilitiesTest extends TestCase {
  interface MyMessages extends Messages {
    @DefaultMessage("Isn''t this the fakiest?")
    @Description("A sample message to be tested.")
    String myMessage();
  }

  public void testWithoutDisarm() {
    try {
      GWT.create(MyMessages.class);
      fail("Calling GWT.create() without disarming should have failed.");
    } catch (UnsupportedOperationException ex) {
      // expected this exception
    }
  }
  
  public void testDisarm() {
    GWTMockUtilities.disarm();
    assertNull(GWT.create(MyMessages.class));
    GWTMockUtilities.restore();
  }
  
  public void testReturnMockMessages() {
    GWTMockUtilities.disarm();
    GWTMockUtilities.returnMockMessages();
    assertNotNull(GWT.create(MyMessages.class));
    assertNull(GWT.create(String.class));
    assertNull(GWT.create(null));
    GWTMockUtilities.restore();
  }
}
