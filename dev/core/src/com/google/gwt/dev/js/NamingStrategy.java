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
package com.google.gwt.dev.js;

import com.google.gwt.dev.js.ast.JsName;
import com.google.gwt.dev.js.ast.JsObfuscatableName;
import com.google.gwt.dev.js.ast.JsScope;
import com.google.gwt.dev.js.ast.JsUnobfuscatableName;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

public abstract class NamingStrategy {

  private class RootScopeHandler {

    public RootScopeHandler(JsScope rootScope) {
      this.rootScope = rootScope;
    }

    public String getObfuscatedName(JsObfuscatableName name) {
      String result = (String) resultByName.get(name);
      if (result == null) {
        String baseIdent = getBaseIdent(name);
        String tailIdent = "";
        int counter = 0;
        JsScope scope = name.getScope();
        do {
          result = obfuscate(baseIdent + tailIdent, scope, rootScope);

          // check for conflicts!
          if (!conflicts(result, scope)) {
            resultByName.put(name, result);
            Map/* <String, JsName> */nameByResult = (Map) nameByResultByScope
              .get(scope);
            if (nameByResult == null) {
              nameByResult = new HashMap();
              nameByResultByScope.put(scope, nameByResult);
            }
            nameByResult.put(result, name);
            break;
          }

          // try this ident with a counter on it
          tailIdent = String.valueOf(counter++);
        } while (true);
      }
      return result;
    }

    private boolean conflicts(String result, JsScope scope) {
      if (JsKeywords.isKeyword(result.toCharArray())) {
        return true;
      }
      if (scope.hasUnobfuscatableName(result)) {
        return true;
      }
      if (scopeConflicts(result, scope)) {
        return true;
      }
      if (parentConflicts(result, scope)) {
        return true;
      }
      return childConflicts(result, scope);
    }

    private boolean scopeConflicts(String result, JsScope scope) {
      Map/* <String, JsName> */nameByResult = (Map) nameByResultByScope
        .get(scope);
      if (nameByResult != null) {
        return nameByResult.containsKey(result);
      }
      return false;
    }

    private boolean childConflicts(String result, JsScope scope) {
      for (int i = 0; i < scope.getChildren().size(); i++) {
        JsScope child = (JsScope) scope.getChildren().get(i);
        if (scopeConflicts(result, child)) {
          return true;
        }
        if (childConflicts(result, child)) {
          return true;
        }
      }
      return false;
    }

    private boolean parentConflicts(String result, JsScope scope) {
      JsScope parent = scope.getParent();
      if (parent == null) {
        return false;
      }
      if (scopeConflicts(result, parent)) {
        return true;
      }
      return parentConflicts(result, parent);
    }

    private final JsScope rootScope;
    private final Map/* <JsObfuscatableName, String> */resultByName = new IdentityHashMap();
    private final Map/* <JsScope, Map<String, JsName>> */nameByResultByScope = new IdentityHashMap();
  }

  public final String getIdent(JsName name) {
    if (name instanceof JsUnobfuscatableName) {
      return name.getIdent();
    }
    RootScopeHandler handler = findHandler(name.getScope());
    return handler.getObfuscatedName((JsObfuscatableName) name);
  }

  protected abstract String getBaseIdent(JsObfuscatableName name);

  protected abstract String obfuscate(String name, JsScope scope,
      JsScope rootScope);

  private RootScopeHandler findHandler(JsScope scope) {
    RootScopeHandler handler = (RootScopeHandler) handlersByScope.get(scope);
    if (handler == null) {
      JsScope parent = scope.getParent();
      if (parent == null) {
        handler = new RootScopeHandler(scope);
        handlersByScope.put(scope, handler);
      } else {
        handler = findHandler(parent);
      }
    }
    return handler;
  }

  private final Map/* <JsScope, RootScopeHandler> */handlersByScope = new IdentityHashMap();
}
