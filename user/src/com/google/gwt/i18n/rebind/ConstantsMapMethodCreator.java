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
package com.google.gwt.i18n.rebind;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.i18n.client.impl.ConstantMap;
import com.google.gwt.i18n.rebind.AbstractResource.MissingResourceException;
import com.google.gwt.i18n.rebind.AbstractResource.ResourceList;
import com.google.gwt.user.rebind.AbstractGeneratorClassCreator;

import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Creator for methods of the form Map getX() .
 */
class ConstantsMapMethodCreator extends AbstractLocalizableMethodCreator {

  static final String GENERIC_STRING_MAP_TYPE = "java.util.Map<java.lang.String, java.lang.String>";

  /**
   * Constructor for localizable returnType method creator.
   * 
   * @param classCreator
   */
  public ConstantsMapMethodCreator(AbstractGeneratorClassCreator classCreator) {
    super(classCreator);
  }

  /**
   * Generates Map of key/value pairs for a list of keys.
   * 
   * @param logger TreeLogger instance for logging
   * @param method method body to create
   * @param mapName name to create map from
   * @param resourceList AbstractResource for key lookup
   * @param locale locale to use for localized string lookup
   */
  @Override
  public void createMethodFor(TreeLogger logger, JMethod method,
      String mapName, ResourceList resourceList, String locale) {
    String methodName = method.getName();
    if (method.getParameters().length > 0) {
      error(
          logger,
          methodName
              + " cannot have parameters; extend Messages instead if you need to create "
              + "parameterized messages");
    }
    // make sure cache exists
    enableCache();
    // check cache for array
    String constantMapClassName = ConstantMap.class.getCanonicalName();
    println(GENERIC_STRING_MAP_TYPE + " args = (" + GENERIC_STRING_MAP_TYPE
        + ") cache.get(" + wrap(methodName) + ");");
    // if not found create Map
    println("if (args == null) {");
    indent();
    println("args = new " + constantMapClassName + "(new String[] {");
    String keyString;
    try {
      keyString = resourceList.getRequiredStringExt(mapName, null);
    } catch (MissingResourceException e) {
      e.setDuring("getting key list");
      throw e;
    }

    String[] keys = ConstantsStringArrayMethodCreator.split(keyString);
    ResourceList resources = getResources();
    SortedMap<String, String> map = new TreeMap<String, String>();
    for (String key : keys) {
      if (key.length() == 0) {
        continue;
      }

      try {
        String value = resources.getRequiredString(key);
        map.put(key, value);
      } catch (MissingResourceException e) {
        e.setDuring("implementing map");
        throw e;
      }
    }

    indent();
    indent();
    for (String key : map.keySet()) {
      println(wrap(key) + ", ");
    }
    outdent();
    println("},");
    indent();
    println("new String[] {");
    for (String key : map.keySet()) {
      String value = map.get(key);
      println(wrap(value) + ",");
    }
    outdent();
    println("});");
    outdent();
    println("cache.put(" + wrap(methodName) + ", args);");
    outdent();
    println("};");
    println("return args;");
  }
}
