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

import com.google.gwt.resources.client.CssResource;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Helper to make a fake implementation of any {@link CssResource} interface via
 * reflection, for use in JUnit tests. (This will not work in GWTTestCase.) All
 * calls to the returned object return the method name.
 * <p>
 * Sample use:
 *
 * <pre>interface MyCss extends CssResource {
 *   String myStyleName();
 * }
 *
 * public void testSimple() {
 *  MyCss myCss = FakeCssMaker.create(MyCss.class);
 *  assertEquals("myStyleName", messages.myMessage());
 * }
 * </pre>
 */
public class FakeCssMaker implements InvocationHandler {
  public static <T extends CssResource> T create(Class<T> cssClass) {
    return cssClass.cast(Proxy.newProxyInstance(
        FakeCssMaker.class.getClassLoader(), new Class[] {cssClass},
        new FakeCssMaker()));
  }

  public Object invoke(Object proxy, Method method, Object[] args)
      throws Throwable {
    return method.getName();
  }
}
