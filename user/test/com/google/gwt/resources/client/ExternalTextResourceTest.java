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
package com.google.gwt.resources.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests for ExternalTextResource assembly and use.
 */
public class ExternalTextResourceTest extends GWTTestCase {

  static interface Resources extends ClientBundleWithLookup {
    @Source("hello.txt")
    ExternalTextResource helloWorldExternal();

    @Source("shouldBeEscaped.txt")
    ExternalTextResource needsEscapeExternal();
  }

  private static final String HELLO = "Hello World!";
  private static final String NEEDS_ESCAPE = "\"'\\";

  @Override
  public String getModuleName() {
    return "com.google.gwt.resources.Resources";
  }

  public void testExternal() throws ResourceException {
    final Resources r = GWT.create(Resources.class);
    int numReturned = 0;

    delayTestFinish(2000);

    class TestCallback implements ResourceCallback<TextResource> {
      private final String name;
      private final String contents;
      private TestCallback otherTest;
      private boolean done = false;

      TestCallback(String name, String contents) {
        this.name = name;
        this.contents = contents;
      }

      public boolean isDone() {
        return done;
      }
      
      public void onError(ResourceException e) {
        e.printStackTrace();
        fail("Unable to fetch " + e.getResource().getName());
      }

      public void onSuccess(TextResource resource) {
        assertEquals(name, resource.getName());
        assertEquals(contents, resource.getText());
        done = true;
        if (otherTest.isDone()) {
          finishTest();
        }
      }
      
      public void setOtherTest(TestCallback test) {
        otherTest = test;
      }
    };

    TestCallback needsEscape = new TestCallback(
        r.needsEscapeExternal().getName(), NEEDS_ESCAPE);
    TestCallback helloWorld = new TestCallback(
        r.helloWorldExternal().getName(), HELLO);
    needsEscape.setOtherTest(helloWorld);
    helloWorld.setOtherTest(needsEscape);
    
    r.needsEscapeExternal().getText(needsEscape);
    r.helloWorldExternal().getText(helloWorld);
  }
}
