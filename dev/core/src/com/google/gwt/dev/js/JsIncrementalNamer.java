/*
 * Copyright 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev.js;

import com.google.gwt.dev.cfg.ConfigurationProperties;
import com.google.gwt.dev.jjs.impl.JavaToJavaScriptMap;
import com.google.gwt.dev.js.ast.JsName;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsScope;
import com.google.gwt.thirdparty.guava.common.annotations.VisibleForTesting;
import com.google.gwt.thirdparty.guava.common.base.Objects;
import com.google.gwt.thirdparty.guava.common.collect.HashMultiset;
import com.google.gwt.thirdparty.guava.common.collect.Maps;
import com.google.gwt.thirdparty.guava.common.collect.Multiset;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

/**
 * A namer that creates short but readable identifiers wherever possible. The old ident ->
 * new ident mappings are recorded and can be persisted across multiple compiles.
 */
public class JsIncrementalNamer extends JsNamer {

  /**
   * Encapsulates the complete state of this namer so that state can be persisted and reused.
   */
  public static class JsIncrementalNamerState implements Serializable {

    private int nextObfuscatedId = -1;
    private Map<String, String> renamedIdentByOriginalIdent = Maps.newHashMap();
    private Multiset<String> shortIdentCollisionCounts = HashMultiset.create();
    private Set<String> usedIdents = Sets.newHashSet();

    public void copyFrom(JsIncrementalNamerState that) {
      this.shortIdentCollisionCounts.clear();
      this.renamedIdentByOriginalIdent.clear();
      this.usedIdents.clear();

      this.shortIdentCollisionCounts.addAll(that.shortIdentCollisionCounts);
      this.renamedIdentByOriginalIdent.putAll(that.renamedIdentByOriginalIdent);
      this.usedIdents.addAll(that.usedIdents);
      this.nextObfuscatedId = that.nextObfuscatedId;
    }

    @VisibleForTesting
    public boolean hasSameContent(JsIncrementalNamerState that) {
      return Objects.equal(this.shortIdentCollisionCounts, that.shortIdentCollisionCounts)
          && Objects.equal(this.renamedIdentByOriginalIdent, that.renamedIdentByOriginalIdent)
          && Objects.equal(this.usedIdents, that.usedIdents)
          && Objects.equal(this.nextObfuscatedId, that.nextObfuscatedId);
    }
  }

  @VisibleForTesting
  public static final String RESERVED_IDENT_SUFFIX = "_g$";

  public static void exec(JsProgram program, ConfigurationProperties config, JsIncrementalNamerState state,
      JavaToJavaScriptMap jjsmap, boolean minifyFunctionNames) throws IllegalNameException {
    new JsIncrementalNamer(program, config, state, jjsmap, minifyFunctionNames).execImpl();
  }

  private final JavaToJavaScriptMap jjsmap;

  private final JsIncrementalNamerState state;

  private final boolean minifyFunctionNames;

  public JsIncrementalNamer(JsProgram program, ConfigurationProperties config,
      JsIncrementalNamerState state, JavaToJavaScriptMap jjsmap, boolean minifyFunctionNames) {
    super(program, config);
    this.state = state;
    this.jjsmap = jjsmap;
    this.minifyFunctionNames = minifyFunctionNames;
  }

  @Override
  protected void reset() {
    // Nothing to do.
  }

  @Override
  protected void visit(JsScope scope) throws IllegalNameException {
    // Visit children.
    for (JsScope child : scope.getChildren()) {
      visit(child);
    }

    // Visit all my idents.
    for (JsName name : scope.getAllNames()) {
      if (!name.isObfuscatable()) {
        // Unobfuscatable names become themselves.
        String ident = name.getIdent();
        if (ident.endsWith(RESERVED_IDENT_SUFFIX)) {
          throw new IllegalNameException("Identifier " + ident + " ends with "
              + RESERVED_IDENT_SUFFIX
              + ". This is not allowed since that suffix is used to separate obfuscatable and "
              + "nonobfuscatable names in per-file compiles.");
        }
        name.setShortIdent(ident);
        continue;
      }

      name.setShortIdent(getOrCreateIdent(name));
    }
  }

  private String getOrCreateIdent(JsName name) {
    String originalIdent = name.getIdent();
    String shortIdent = name.getShortIdent();

    // Reuse previous names.
    if (state.renamedIdentByOriginalIdent.containsKey(originalIdent)) {
      return state.renamedIdentByOriginalIdent.get(originalIdent);
    }

    // If the name is for a method.
    if (minifyFunctionNames && jjsmap != null && jjsmap.nameToMethod(name) != null) {
      // Come up with an obfuscated name (since OptionDisplayName will show it properly) and cache
      // it for reuse.
      String obfuscatedIdent = makeObfuscatedIdent();
      state.usedIdents.add(obfuscatedIdent);
      state.renamedIdentByOriginalIdent.put(originalIdent, obfuscatedIdent);
      return obfuscatedIdent;
    }

    // Otherwise come up with a new pretty name and cache it for reuse.
    String prettyIdent = makePrettyName(shortIdent);
    state.usedIdents.add(prettyIdent);
    state.renamedIdentByOriginalIdent.put(originalIdent, prettyIdent);
    return prettyIdent;
  }

  private String makeObfuscatedIdent() {
    while (true) {
      String obfuscatedIdent =
          JsObfuscateNamer.makeObfuscatedIdent(++state.nextObfuscatedId) + RESERVED_IDENT_SUFFIX;
      if (reserved.isAvailable(obfuscatedIdent) && !state.usedIdents.contains(obfuscatedIdent)) {
        return obfuscatedIdent;
      }
    }
  }

  private String makePrettyName(String shortIdent) {
    while (true) {
      String prettyIdent = shortIdent + "_" + state.shortIdentCollisionCounts.count(shortIdent)
          + RESERVED_IDENT_SUFFIX;
      state.shortIdentCollisionCounts.add(shortIdent);
      if (reserved.isAvailable(prettyIdent) && !state.usedIdents.contains(prettyIdent)) {
        return prettyIdent;
      }
    }
  }
}
