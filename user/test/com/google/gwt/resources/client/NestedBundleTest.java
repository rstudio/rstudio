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
package com.google.gwt.resources.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.resources.client.GwtCreateResource.ClassType;

/**
 * Verify that nested bundles work correctly.
 */
public class NestedBundleTest extends GWTTestCase {

  interface NestedBundle extends ClientBundle {
    @Source("hello.txt")
    TextResource hello();

    NestedBundle nested();

    GwtCreateResource<NestedBundle> nestedCreate();

    @ClassType(NestedBundle.class)
    GwtCreateResource<Object> nestedCreateWithOverride();
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.resources.Resources";
  }

  public void testNestedBundle() {
    NestedBundle b = GWT.create(NestedBundle.class);
    assertSame(b.hello(), b.nested().hello());
  }

  public void testNestedCreate() {
    NestedBundle b = GWT.create(NestedBundle.class);
    NestedBundle q = b.nestedCreate().create();
    NestedBundle r = b.nestedCreate().create();
    assertNotSame(q, r);

    assertSame(q.hello(), r.hello());
  }

  public void testNestedCreateOverride() {
    NestedBundle b = GWT.create(NestedBundle.class);
    Object o = b.nestedCreateWithOverride().create();
    assertTrue(o instanceof NestedBundle);
  }
}
