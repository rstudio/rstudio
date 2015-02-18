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
package com.google.gwt.resources.converter;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.resources.css.ast.Context;
import com.google.gwt.resources.css.ast.CssDef;
import com.google.gwt.resources.css.ast.CssEval;
import com.google.gwt.resources.css.ast.CssUrl;
import com.google.gwt.resources.css.ast.CssVisitor;
import com.google.gwt.thirdparty.guava.common.base.Strings;
import com.google.gwt.thirdparty.guava.common.collect.BiMap;
import com.google.gwt.thirdparty.guava.common.collect.HashBiMap;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * GSS requires that constants are defined in upper case. This visitor will collect all existing
 * constants, create the GSS compatible name of each constant and returns a mapping of all
 * original names with the new generated name.
 * <p/>
 * This visitor lists also all constant nodes.
 */
public class DefCollectorVisitor extends CssVisitor {
  private static final Pattern INVALID_CHAR = Pattern.compile("[^A-Z0-9_]");

  private int renamingCounter = 0;
  private final BiMap<String, String> defMapping;
  private final List<CssDef> constantNodes;
  private final boolean lenient;
  private final TreeLogger treeLogger;

  public DefCollectorVisitor(boolean lenient, TreeLogger treeLogger) {
    this.lenient = lenient;
    this.treeLogger = treeLogger;
    defMapping = HashBiMap.create();
    constantNodes = new LinkedList<CssDef>();
  }

  public Map<String, String> getDefMapping() {
    return defMapping;
  }

  public List<CssDef> getConstantNodes() {
    return constantNodes;
  }

  @Override
  public boolean visit(CssEval x, Context ctx) {
    return handleDefNode(x);
  }

  @Override
  public boolean visit(CssDef x, Context ctx) {
    return handleDefNode(x);
  }

  @Override
  public boolean visit(CssUrl x, Context ctx) {
    return handleDefNode(x);
  }

  private boolean handleDefNode(CssDef def) {
    constantNodes.add(def);

    if (!defMapping.containsKey(def.getKey())) {

      String upperCaseName = toUpperCase(def.getKey());

      String validName = INVALID_CHAR.matcher(upperCaseName).replaceAll("_");

      if (!validName.equals(upperCaseName)) {
        treeLogger.log(Type.WARN, "Invalid characters detected in [" + upperCaseName + "]. They have " +
            "been replaced [" + validName + "]");
      }

      if (defMapping.containsValue(validName)) {
        if (lenient) {
          treeLogger.log(Type.WARN, "Two constants have the same name [" + validName + "] " +
              "after conversion. The second constant will be renamed automatically.");
          validName = renameConstant(validName);
        } else {
          throw new Css2GssConversionException(
              "Two constants have the same name [" + validName +
                  "] after conversion.");
        }
      }

      defMapping.forcePut(def.getKey(), validName);
    }
    return false;
  }

  private String renameConstant(String originalName) {
    String newName;

    do {
      newName = originalName + "__RENAMED__" + renamingCounter;
      renamingCounter++;
    } while (defMapping.containsValue(newName));

    return newName;
  }

  private String toUpperCase(String camelCase) {
    assert !Strings.isNullOrEmpty(camelCase) : "camelCase cannot be null or empty";

    if (isUpperCase(camelCase)) {
      return camelCase;
    }

    StringBuilder output = new StringBuilder().append(Character.toUpperCase(camelCase.charAt(0)));

    for (int i = 1; i < camelCase.length(); i++) {
      char c = camelCase.charAt(i);
      if (Character.isUpperCase(c)) {
        output.append('_').append(c);
      }  else {
        output.append(Character.toUpperCase(c));
      }
    }

    return output.toString();
  }

  private boolean isUpperCase(String camelCase) {
    for (int i = 0; i < camelCase.length(); i++) {
      if (Character.isLowerCase(camelCase.charAt(i))) {
        return false;
      }
    }

    return true;
  }
}
