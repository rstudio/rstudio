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
package com.google.gwt.dev.js;

import com.google.gwt.core.ext.BadPropertyValueException;
import com.google.gwt.core.ext.ConfigurationProperty;
import com.google.gwt.core.ext.PropertyOracle;
import com.google.gwt.dev.js.ast.JsContext;
import com.google.gwt.dev.js.ast.JsForIn;
import com.google.gwt.dev.js.ast.JsFunction;
import com.google.gwt.dev.js.ast.JsLabel;
import com.google.gwt.dev.js.ast.JsName;
import com.google.gwt.dev.js.ast.JsNameOf;
import com.google.gwt.dev.js.ast.JsNameRef;
import com.google.gwt.dev.js.ast.JsParameter;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsScope;
import com.google.gwt.dev.js.ast.JsVars;
import com.google.gwt.dev.js.ast.JsVisitor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A class that allocates unique identifiers for JsNames.
 */
public abstract class JsNamer {

  static final String BLACKLIST = "js.identifier.blacklist";
  static final String BLACKLIST_SUFFIXES =
      "js.identifier.blacklist.suffixes";

  private static Set<JsName> collectReferencedNames(JsProgram program) {
    final Set<JsName> referenced = new HashSet<JsName>();
    new JsVisitor() {
      @Override
      public void endVisit(JsForIn x, JsContext ctx) {
        reference(x.getIterVarName());
      }

      @Override
      public void endVisit(JsFunction x, JsContext ctx) {
        reference(x.getName());
      };

      @Override
      public void endVisit(JsLabel x, JsContext ctx) {
        reference(x.getName());
      };

      @Override
      public void endVisit(JsNameOf x, JsContext ctx) {
        reference(x.getName());
      };

      @Override
      public void endVisit(JsNameRef x, JsContext ctx) {
        reference(x.getName());
      };

      @Override
      public void endVisit(JsParameter x, JsContext ctx) {
        reference(x.getName());
      };

      @Override
      public void endVisit(JsVars.JsVar x, JsContext ctx) {
        reference(x.getName());
      };

      private void reference(JsName name) {
        if (name != null) {
          referenced.add(name);
        }
      }
    }.accept(program);
    return referenced;
  }

  protected final JsProgram program;

  protected final Set<JsName> referenced;

  protected final Set<String> blacklistedIdents;

  protected final List<String> blacklistedSuffixes;

  public JsNamer(JsProgram program, PropertyOracle[] propertyOracles) {
    this.program = program;
    referenced = collectReferencedNames(program);
    Set<String> blacklist = new HashSet<String>();
    List<String> blacklistSuffixes = new ArrayList<String>();
    if (propertyOracles != null) {
      for (PropertyOracle propOracle : propertyOracles) {
        maybeAddToBlacklist(BLACKLIST, blacklist, propOracle);
        maybeAddToBlacklist(BLACKLIST_SUFFIXES, blacklistSuffixes, propOracle);
      }
    }
    blacklistedIdents = Collections.unmodifiableSet(blacklist);
    blacklistedSuffixes = Collections.unmodifiableList(blacklistSuffixes);
  }

  private void maybeAddToBlacklist(String propName, Collection<String> blacklist,
      PropertyOracle propOracle) {
    try {
      ConfigurationProperty configProp =
          propOracle.getConfigurationProperty(propName);
      // supports multivalue property
      for (String bannedSymbols : configProp.getValues()) {
        // and comma separated list
        String [] idents = bannedSymbols.split(",");
        for (String ident : idents) {
          String trimmedIdent = ident.trim();
          if (trimmedIdent.length() > 0) {
            blacklist.add(trimmedIdent);
          }
        }
      }
    } catch (BadPropertyValueException e) {
      // thrown if not set to anything
    }
  }

  protected final void execImpl() {
    reset();
    visit(program.getScope());
    reset();
    visit(program.getObjectScope());
  }

  protected abstract void reset();

  protected abstract void visit(JsScope scope);

  protected boolean isAvailableIdent(String newIdent) {
    if (JsKeywords.isKeyword(newIdent)) {
      return false;
    }
    String lcIdent = newIdent.toLowerCase();
    for (String suffix : blacklistedSuffixes) {
      if (lcIdent.endsWith(suffix.toLowerCase())) {
        return false;
      }
    }
    return !blacklistedIdents.contains(newIdent);
  }
}
