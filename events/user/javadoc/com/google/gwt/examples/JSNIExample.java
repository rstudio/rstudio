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
package com.google.gwt.examples;

public class JSNIExample {

  String myInstanceField;
  static int myStaticField;

  void instanceFoo(String s) {
    // use s
  }

  static void staticFoo(String s) {
    // use s
  }

  public native void bar(JSNIExample x, String s) /*-{
    // Call instance method instanceFoo() on this
    this.@com.google.gwt.examples.JSNIExample::instanceFoo(Ljava/lang/String;)(s);

    // Call instance method instanceFoo() on x
    x.@com.google.gwt.examples.JSNIExample::instanceFoo(Ljava/lang/String;)(s);

    // Call static method staticFoo()
    @com.google.gwt.examples.JSNIExample::staticFoo(Ljava/lang/String;)(s);

    // Read instance field on this
    var val = this.@com.google.gwt.examples.JSNIExample::myInstanceField;

    // Write instance field on x
    x.@com.google.gwt.examples.JSNIExample::myInstanceField = val + " and stuff";

    // Read static field (no qualifier)
    @com.google.gwt.examples.JSNIExample::myStaticField = val + " and stuff";
  }-*/;

}
