/*
 * Copyright 2013 Google Inc.
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

import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;

/**
 * Helper to make a fake implementation of any {@link SafeHtmlTemplates}
 * interface via reflection, for use in JUnit tests. (This will not work in
 * GWTTestCase.) All calls to the returned object return the method name
 * followed by the passed parameters as a list surrounded by [].
 * <p>
 * Sample use:
 *
 * <pre>interface MyTemplates extends SafeHtmlTemplates {
 *   &#64;Template("<span class=\"{3}\">{0}: <a href=\"{1}\">{2}</a></span>")
 *   SafeHtml messageWithLink(SafeHtml message, String url, String linkText,
 *     String style);
 * }
 *
 * public void testWithArgs() {
 *   MyTemplates templates = FakeSafeHtmlTemplatesMaker.create(MyTemplates.class);
 *   SafeHtml message = SafeHtmlUtils.fromString("message");
 *   assertEquals("messageWithLink[message, url, link, style]",
 *     templates.messageWithLink(message, "url", "link", "style").asString());
 * }
 * </pre>
 */
public class FakeSafeHtmlTemplatesMaker implements InvocationHandler {
  public static <T extends SafeHtmlTemplates> T create(Class<T> templatesClass) {
    return templatesClass.cast(Proxy.newProxyInstance(
        FakeSafeHtmlTemplatesMaker.class.getClassLoader(),
        new Class[] {templatesClass},
        new FakeSafeHtmlTemplatesMaker()));
  }

  public Object invoke(Object proxy, Method method, Object[] args)
      throws Throwable {
    String name = method.getName();

    if (args == null || args.length == 0) {
      return SafeHtmlUtils.fromString(name);
    }

    // SafeHtml does not implement toString(), so use asString() instead.
    for (int i = 0; i < args.length; i++) {
      if (args[i] instanceof SafeHtml) {
        args[i] = ((SafeHtml) args[i]).asString();
      }
    }

    String result = name + Arrays.asList(args);
    return SafeHtmlUtils.fromString(result);
  }
}
