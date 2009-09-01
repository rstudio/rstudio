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
package com.google.gwt.junit;

import com.google.gwt.i18n.client.Messages;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;

/**
 * Helper to make a fake implementation of any {@link Messages} interface via
 * reflection, for use in JUnit tests. (This will not work in GWTTestCase.) All
 * calls to the returned object return the method name followed by the passed
 * parameters as a list surrounded by [].
 * <p>
 * Note that the default message text is very consciously not made available
 * through the fake, to help tests ensure that specific translations of
 * localized text are not relied upon.
 * <p>
 * Sample use:
 *
 * <pre>interface MyMessages extends Messages {
 *   &#64;DefaultMessage("Isn''t this the fakiest?")
 *   &#64;Description("A sample message to be tested.")
 *   String myMessage();
 * }
 *
 * public void testSimple() {
 *  MyMessages messages = FakeMessagesMaker.create(MyMessages.class);
 *  assertEquals("myMessage", messages.myMessage());
 * }
 * </pre>
 */
public class FakeMessagesMaker implements InvocationHandler {
  public static <T extends Messages> T create(Class<T> messagesClass) {
    return messagesClass.cast(Proxy.newProxyInstance(
        FakeMessagesMaker.class.getClassLoader(), new Class[] {messagesClass},
        new FakeMessagesMaker()));
  }

  public Object invoke(Object proxy, Method method, Object[] args)
      throws Throwable {
    String name = method.getName();

    return (args == null || args.length == 0) ? name : name
        + Arrays.asList(args);
  }
}
