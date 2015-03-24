/*
 * Copyright 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev.jjs.test;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests that codesplitter places getPermutationId in the right fragment.
 *
 * See issue 8539.
 */
public class CodeSplitterCollapsedPropertiesTest extends GWTTestCase {
  // TODO(rluble): Add a test for {@link Permutation.mergeFrom}.

  private static final int RUNASYNC_TIMEOUT = 30000;

  @Override
  public String getModuleName() {
    return "com.google.gwt.dev.jjs.CodeSplitterCollapsedPropertiesTest";
  }

  private static abstract class Foo {
    public abstract String getColor();
  }

  private static class Bar extends Foo {
    @Override
    public String getColor() {
      return "blue";
    }
  }

  private static class Baz extends Foo {
    @Override
    public String getColor() {
      return "red";
    }
  }

  public void testSplitWithCollapsedProperty() {
    delayTestFinish(RUNASYNC_TIMEOUT);
    GWT.runAsync(new RunAsyncCallback() {
      @Override
      public void onFailure(Throwable reason) {
        fail(reason.getMessage());
      }

      @Override
      public void onSuccess() {
        Foo object = GWT.create(Foo.class);
        assertEquals("blue", object.getColor());
        finishTest();
      }
    });
  }
}
