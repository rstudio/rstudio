/*
 * Copyright 2014 Google Inc.
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
package com.google.gwt.dev.jjs.test;

import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests JSNI method dispatch.
 */
public class JsniDispatchTest extends GWTTestCase {

  static class SuperSuperSuperFoo {
    public String sayHello(String to) {
      return "Hello " + to + " from SuperSuperSuperFoo";
    }
    private String sayHelloNTimes(int n) {
      return "Hello from SuperSuperSuperFoo " + n + " times";
    }
  }

  static class SuperSuperFoo extends SuperSuperSuperFoo {
    public String sayHello(String to) {
      return "Hello " + to + " from SuperSuperFoo";
    }
    protected String sayHelloNTimes(int n) {
      return "Hello from SuperSuperFoo " + n + " times";
    }
  }

  static class SuperFoo extends SuperSuperFoo {
    public String sayHello(String to) {
      return "Hello " + to + " from SuperFoo";
    }
    private String sayHello(int n) {
      return "Hello from SuperFoo " + n + " times";
    }
    public String sayHelloNTimes(int n) {
      return "Hello from SuperFoo " + n + " times";
    }
  }

  static class Foo extends SuperFoo {
    public String sayHello(String to) {
      return "Hello to " + to + " from Foo";
    }
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.dev.jjs.CompilerSuite";
  }

  public native String callSayHello(Foo foo) /*-{
    return foo.@Foo::sayHello(Ljava/lang/String;)("me");
  }-*/;

  public native String callSayHelloWildcard(Foo foo) /*-{
    return foo.@Foo::sayHello(*)("me");
  }-*/;

  public native String callSayHelloNTimes(Foo foo) /*-{
    return foo.@Foo::sayHelloNTimes(*)(10);
  }-*/;

  public native String callSayHelloNTimesAtSuperSuperFoo(Foo foo) /*-{
    return foo.@SuperSuperFoo::sayHelloNTimes(*)(10);
  }-*/;

  public native String callSayHelloNTimesAtSuperSuperSuperFoo(Foo foo) /*-{
    return foo.@SuperSuperSuperFoo::sayHelloNTimes(*)(10);
  }-*/;

  /**
   * Ensure wildcard matching dispatches to the correct method.
   */
  public void testWildCardDispatch() {
    assertEquals("Hello to me from Foo", callSayHello(new Foo()));
    assertEquals("Hello to me from Foo", callSayHelloWildcard(new Foo()));
    assertEquals("Hello from SuperFoo 10 times", callSayHelloNTimes(new Foo()));
    // Dispatchs to SuperFoo:: due to dynamic dispatch.
    assertEquals("Hello from SuperFoo 10 times", callSayHelloNTimesAtSuperSuperFoo(new Foo()));
    assertEquals("Hello from SuperSuperSuperFoo 10 times",
        callSayHelloNTimesAtSuperSuperSuperFoo(new Foo()));
  }
}
