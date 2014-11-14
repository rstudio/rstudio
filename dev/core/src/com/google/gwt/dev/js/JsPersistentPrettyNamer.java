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

import com.google.gwt.dev.cfg.ConfigProps;
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
 * A namer that keeps the short ("pretty") identifier wherever possible. The original ident ->
 * pretty ident mappings are recorded and can be persisted across multiple compiles.
 */
public class JsPersistentPrettyNamer extends JsNamer {

  /**
   * Encapsulates the complete state of this namer so that state can be persisted and reused.
   */
  public static class PersistentPrettyNamerState implements Serializable {

    private Multiset<String> shortIdentCollisionCounts = HashMultiset.create();
    private Map<String, String> prettyIdentByOriginalIdent = Maps.newHashMap();
    private Set<String> usedPrettyIdents = Sets.newHashSet();

    public void copyFrom(PersistentPrettyNamerState that) {
      this.shortIdentCollisionCounts.clear();
      this.prettyIdentByOriginalIdent.clear();
      this.usedPrettyIdents.clear();

      this.shortIdentCollisionCounts.addAll(that.shortIdentCollisionCounts);
      this.prettyIdentByOriginalIdent.putAll(that.prettyIdentByOriginalIdent);
      this.usedPrettyIdents.addAll(that.usedPrettyIdents);
    }

    @VisibleForTesting
    public boolean hasSameContent(PersistentPrettyNamerState that) {
      return Objects.equal(this.shortIdentCollisionCounts, that.shortIdentCollisionCounts)
          && Objects.equal(this.prettyIdentByOriginalIdent, that.prettyIdentByOriginalIdent)
          && Objects.equal(this.usedPrettyIdents, that.usedPrettyIdents);
    }
  }

  @VisibleForTesting
  public static final String RESERVED_IDENT_SUFFIX = "_g$";

  public static void exec(JsProgram program, ConfigProps config, PersistentPrettyNamerState state)
      throws IllegalNameException {
    new JsPersistentPrettyNamer(program, config, state).execImpl();
  }

  private final PersistentPrettyNamerState state;

  public JsPersistentPrettyNamer(JsProgram program, ConfigProps config,
      PersistentPrettyNamerState state) {
    super(program, config);
    this.state = state;
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
        String prettyIdent = name.getIdent();
        if (prettyIdent.endsWith(RESERVED_IDENT_SUFFIX)) {
          throw new IllegalNameException("Identifier " + prettyIdent + " ends with "
              + RESERVED_IDENT_SUFFIX
              + ". This is not allowed since that suffix is used to separate obfuscatable and "
              + "nonobfuscatable names in per-file compiles.");
        }
        name.setShortIdent(prettyIdent);
        continue;
      }

      name.setShortIdent(getOrCreatePrettyIdent(name));
    }
  }

  private String getOrCreatePrettyIdent(JsName name) {
    String originalIdent = name.getIdent();
    String shortIdent = name.getShortIdent();

    // Reuse previous names.
    if (state.prettyIdentByOriginalIdent.containsKey(originalIdent)) {
      return state.prettyIdentByOriginalIdent.get(originalIdent);
    }

    // Otherwise come up with a new pretty name and cache it for reuse.
    String prettyIdent = makePrettyName(shortIdent);
    state.usedPrettyIdents.add(prettyIdent);
    state.prettyIdentByOriginalIdent.put(originalIdent, prettyIdent);
    return prettyIdent;
  }

  private String makePrettyName(String shortIdent) {
    while (true) {
      String prettyIdent = shortIdent + "_" + state.shortIdentCollisionCounts.count(shortIdent)
          + RESERVED_IDENT_SUFFIX;
      state.shortIdentCollisionCounts.add(shortIdent);
      if (reserved.isAvailable(prettyIdent) && !state.usedPrettyIdents.contains(prettyIdent)) {
        return prettyIdent;
      }
    }
  }
}
