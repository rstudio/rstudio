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
package com.google.gwt.dev;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.linker.ArtifactSet;
import com.google.gwt.core.ext.linker.EmittedArtifact;
import com.google.gwt.core.ext.linker.EmittedArtifact.Visibility;
import com.google.gwt.dev.jjs.UnifiedAst;
import com.google.gwt.dev.util.Util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;

/**
 * The result of compilation phase 1, includes a unified AST and metadata
 * relevant to each permutation.
 */
public class Precompilation implements PrecompilationResult {
  /*
   * TODO: don't make this whole class serializable, instead dump the
   * independent members out to a file so that the generated artifacts are
   * optional to deserialize.
   */
  private transient ArtifactSet generatedArtifacts;
  private transient byte[] generatedArtifactsSerialized;
  private final Permutation[] permutations;
  private final UnifiedAst unifiedAst;

  public Precompilation(UnifiedAst unifiedAst,
      Collection<Permutation> permutations, ArtifactSet generatedArtifacts) {
    this(unifiedAst, permutations, 0, generatedArtifacts);
  }

  /**
   * Constructs a new precompilation. We create new Permutations with a new id
   * so that the ids are consecutive and correspond to the index in the array.
   *
   * @param unifiedAst the unified AST used by
   *          {@link com.google.gwt.dev.jjs.JavaToJavaScriptCompiler}
   * @param permutations the set of permutations that can be run
   * @param permutationBase the id to use for the first permutation
   * @param generatedArtifacts the set of artifacts created by generators
   */
  public Precompilation(UnifiedAst unifiedAst,
      Collection<Permutation> permutations, int permutationBase,
      ArtifactSet generatedArtifacts) {

    this.unifiedAst = unifiedAst;
    this.permutations = new Permutation[permutations.size()];
    int i = 0;
    for (Permutation permutation : permutations) {
      this.permutations[i] = new Permutation(i + permutationBase, permutation);
      ++i;
    }
    this.generatedArtifacts = generatedArtifacts;
  }

  /**
   * Returns the set of generated artifacts from the precompile phase.
   */
  public ArtifactSet getGeneratedArtifacts() {
    if (generatedArtifacts == null) {
      try {
        assert generatedArtifactsSerialized != null;
        generatedArtifacts = Util.readStreamAsObject(new ByteArrayInputStream(
            generatedArtifactsSerialized), ArtifactSet.class);
        generatedArtifactsSerialized = null;
      } catch (ClassNotFoundException e) {
        throw new RuntimeException(
            "Unexpected exception deserializing from memory stream", e);
      } catch (IOException e) {
        throw new RuntimeException(
            "Unexpected exception deserializing from memory stream", e);
      }
    }
    return generatedArtifacts;
  }

  public Permutation getPermutation(int id) {
    for (Permutation perm : permutations) {
      if (perm.getId() == id) {
        return perm;
      }
    }
    return null;
  }

  /**
   * Returns the set of permutations to run.
   */
  public Permutation[] getPermutations() {
    return permutations;
  }

  /**
   * Returns the unified AST used by
   * {@link com.google.gwt.dev.jjs.JavaToJavaScriptCompiler}.
   */
  public UnifiedAst getUnifiedAst() {
    return unifiedAst;
  }

  private void readObject(ObjectInputStream stream) throws IOException,
      ClassNotFoundException {
    stream.defaultReadObject();
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    Util.copyNoClose(stream, baos);
    generatedArtifactsSerialized = baos.toByteArray();
  }

  private void writeObject(ObjectOutputStream stream) throws IOException {
    stream.defaultWriteObject();
    Util.writeObjectToStream(stream, generatedArtifacts);
  }

  /**
   * Removes saved source code from the generated artifacts.
   * (This reduces I/O when we're not doing that.)
   */
  public void removeSourceArtifacts(TreeLogger logger) {
    logger.log(Type.DEBUG, "removing source artifacts");

    for (EmittedArtifact artifact : generatedArtifacts.find(EmittedArtifact.class)) {
      if (artifact.getVisibility() == Visibility.Source) {
        generatedArtifacts.remove(artifact);
      }
    }
  }
}
