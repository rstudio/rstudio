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
package com.google.gwt.dev.jjs;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.Permutation;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.util.PerfLogger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Represents a unified, non-permutation specific AST. This AST is used to drive
 * per-permutation compiles.
 */
public class UnifiedAst implements Serializable {

  /**
   * Encapsulates the combined programs.
   */
  static final class AST {
    private JProgram jProgram;
    private JsProgram jsProgram;

    public AST(JProgram jProgram, JsProgram jsProgram) {
      this.jProgram = jProgram;
      this.jsProgram = jsProgram;
    }

    JProgram getJProgram() {
      return jProgram;
    }

    JsProgram getJsProgram() {
      return jsProgram;
    }
  }

  private static AST deserializeAst(byte[] serializedAst) {
    try {
      PerfLogger.start("deserialize");
      ByteArrayInputStream bais = new ByteArrayInputStream(serializedAst);
      ObjectInputStream is;
      is = new ObjectInputStream(bais);
      JProgram jprogram = (JProgram) is.readObject();
      JsProgram jsProgram = (JsProgram) is.readObject();
      return new AST(jprogram, jsProgram);
    } catch (IOException e) {
      throw new RuntimeException(
          "Should be impossible for memory based streams", e);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(
          "Should be impossible when deserializing in process", e);
    } finally {
      PerfLogger.end();
    }
  }

  private static byte[] serializeAst(AST ast) {
    try {
      PerfLogger.start("serialize");
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ObjectOutputStream os = new ObjectOutputStream(baos);
      os.writeObject(ast.getJProgram());
      os.writeObject(ast.getJsProgram());
      os.close();
      return baos.toByteArray();
    } catch (IOException e) {
      throw new RuntimeException(
          "Should be impossible for memory based streams", e);
    } finally {
      PerfLogger.end();
    }
  }

  /**
   * The original AST; nulled out once consumed (by the first call to
   * {@link #getFreshAst()}.
   */
  private transient AST initialAst;

  /**
   * Used for internal synchronization.
   */
  private transient Object myLockObject = new Object();

  /**
   * The compilation options.
   */
  private final JJSOptions options;

  /**
   * The set of all live rebind request types in the AST.
   */
  private final SortedSet<String> rebindRequests;

  /**
   * The serialized AST.
   */
  private byte[] serializedAst;

  public UnifiedAst(JJSOptions options, AST initialAst,
      boolean singlePermutation, Set<String> rebindRequests) {
    this.options = new JJSOptionsImpl(options);
    this.initialAst = initialAst;
    this.rebindRequests = Collections.unmodifiableSortedSet(new TreeSet<String>(
        rebindRequests));
    this.serializedAst = singlePermutation ? null : serializeAst(initialAst);
  }

  /**
   * Copy constructor, invalidates the original.
   */
  UnifiedAst(UnifiedAst other) {
    this.options = other.options;
    this.initialAst = other.initialAst;
    other.initialAst = null; // steal its copy
    this.rebindRequests = other.rebindRequests;
    this.serializedAst = other.serializedAst;
  }

  /**
   * Compiles a particular permutation.
   * 
   * @param logger the logger to use
   * @param permutation the permutation to compile
   * @return the permutation result
   * @throws UnableToCompleteException if an error other than
   *           {@link OutOfMemoryError} occurs
   */
  public PermutationResult compilePermutation(TreeLogger logger,
      Permutation permutation) throws UnableToCompleteException {
    return JavaToJavaScriptCompiler.compilePermutation(logger, this,
        permutation);
  }

  /**
   * Returns the active set of JJS options associated with this compile.
   */
  public JJSOptions getOptions() {
    return new JJSOptionsImpl(options);
  }

  /**
   * Returns the set of live rebind requests in the AST.
   */
  public SortedSet<String> getRebindRequests() {
    return rebindRequests;
  }

  /**
   * Internally prepares a new AST for compilation if one is not already
   * prepared.
   */
  public void prepare() {
    synchronized (myLockObject) {
      if (initialAst == null) {
        initialAst = deserializeAst(serializedAst);
      }
    }
  }

  AST getFreshAst() {
    synchronized (myLockObject) {
      if (initialAst != null) {
        AST result = initialAst;
        initialAst = null;
        return result;
      } else {
        if (serializedAst == null) {
          throw new IllegalStateException(
              "No serialized AST was cached and AST was already consumed.");
        }
        return deserializeAst(serializedAst);
      }
    }
  }

  /**
   * Re-initialize lock object.
   */
  private Object readResolve() {
    myLockObject = new Object();
    return this;
  }

  /**
   * Force byte serialization of AST before writing.
   */
  private Object writeReplace() {
    if (serializedAst == null) {
      synchronized (myLockObject) {
        if (initialAst == null) {
          throw new IllegalStateException(
              "No serialized AST was cached and AST was already consumed.");
        }
        serializedAst = serializeAst(initialAst);
      }
    }
    return this;
  }
}
