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
package com.google.gwt.i18n.rebind;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.user.rebind.AbstractGeneratorClassCreator;

/**
 * Creator for methods of the form String[] getX().
 */
class ConstantsStringArrayMethodCreator extends
    AbstractLocalizableMethodCreator {

  static String[] split(String target) {
    // We add an artificial end character to avoid the odd split() behavior
    // that drops the last item if it is only whitespace.
    target = target + "~";

    // Do not split on escaped commas.
    String[] args = target.split("(?<![\\\\]),");

    // Now remove the artificial ending we added above. 
    // We have to do it before we escape and trim because otherwise
    // the artificial trailing '~' would prevent the last item from being
    // properly trimmed.
    if (args.length > 0) {
      int last = args.length - 1;
      args[last] = args[last].substring(0, args[last].length() - 1);
    }
    
    for (int i = 0; i < args.length; i++) {
      args[i] = args[i].replaceAll("\\\\,", ",").trim();
    }

    return args;
  }

  /**
   * Constructor for localizable string array method creator.
   * 
   * @param classCreator
   */
  public ConstantsStringArrayMethodCreator(
      AbstractGeneratorClassCreator classCreator) {
    super(classCreator);
  }

  public void createMethodFor(TreeLogger logger, JMethod method, String template) {
    String methodName = method.getName();
    // Make sure cache exists.
    enableCache();
    // Check cache for array.
    println("String args[] = (String[]) cache.get(" + wrap(methodName) + ");");
    // If not found, create String[].
    print("if (args == null){\n  String [] writer= {");
    String[] args = split(template);
    for (int i = 0; i < args.length; i++) {
      if (i != 0) {
        print(", ");
      }
      String toPrint = args[i].replaceAll("\\,", ",");
      print(wrap(toPrint));
    }
    println("}; ");
    // add to cache, and return
    println("cache.put(" + wrap(methodName) + ", writer);");
    println("return writer;");
    println("} else");
    println("return args;");
  }
}
