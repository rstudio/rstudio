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
import com.google.gwt.i18n.shared.GwtLocale;
import com.google.gwt.user.rebind.AbstractGeneratorClassCreator;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

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
      String mapName, ResourceList resourceList, GwtLocale locale) {
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
      keyString = resourceList.getRequiredString(mapName);
    } catch (MissingResourceException e) {
      e.setDuring("getting key list");
      throw e;
    }

    String[] keys = ConstantsStringArrayMethodCreator.split(keyString);
    ResourceList resources = getResources();
    // Use a LinkedHashMap to preserve declaration order (but remove dups).
    Map<String, String> map = new LinkedHashMap<String, String>();
    for (String key : keys) {
      if (key.length() == 0) {
        continue;
      }

      try {
        // check for "map[key]=value" first
        String value = resources.getStringExt(mapName, key);
        if (value == null) {
          // for backwards compatibility, check for "key=value", which must be
          // present if the form above isn't present.
          value = resources.getRequiredString(key);
        }
        map.put(key, value);
      } catch (MissingResourceException e) {
        e.setDuring("implementing map");
        throw e;
      }
    }

    indent();
    indent();
    Set<Entry<String, String>> entries = map.entrySet();
    for (Entry<String, String> entry : entries) {
      println(wrap(entry.getKey()) + ", ");
    }
    outdent();
    println("},");
    indent();
    println("new String[] {");
    for (Entry<String, String> entry : entries) {
      println(wrap(entry.getValue()) + ", ");
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
