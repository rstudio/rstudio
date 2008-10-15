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
public final class UnifiedAst implements Serializable {

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

  /**
   * Estimated AST memory usage.
   */
  private long astMemoryUsage;

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
   * The serialized form of savedAst.
   */
  private byte[] serializedAst;

  /**
   * If <code>true</code>, only one permutation will be run, so we don't need
   * to serialize our AST (unless this whole object is about to be serialized).
   */
  private transient boolean singlePermutation;

  public UnifiedAst(JJSOptions options, AST initialAst,
      boolean singlePermutation, long astMemoryUsage, Set<String> rebindRequests) {
    this.options = new JJSOptionsImpl(options);
    this.initialAst = initialAst;
    this.singlePermutation = singlePermutation;
    this.astMemoryUsage = astMemoryUsage;
    this.rebindRequests = Collections.unmodifiableSortedSet(new TreeSet<String>(
        rebindRequests));
  }

  /**
   * Returns a rough estimate of how much memory an AST will take up.
   */
  public long getAstMemoryUsage() {
    return astMemoryUsage;
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

  AST getFreshAst() {
    synchronized (myLockObject) {
      if (initialAst != null) {
        if (!singlePermutation && serializedAst == null) {
          // Must preserve a serialized copy for future calls.
          serializeAst();
        }
        AST result = initialAst;
        initialAst = null;
        return result;
      } else {
        if (serializedAst == null) {
          throw new IllegalStateException("No serialized AST was cached.");
        }
        return deserializeAst();
      }
    }
  }

  private AST deserializeAst() {
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

  /**
   * Re-initialize lock object.
   */
  private Object readResolve() {
    myLockObject = new Object();
    return this;
  }

  private void serializeAst() {
    try {
      assert (initialAst != null);
      assert (serializedAst == null);
      PerfLogger.start("serialize");
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ObjectOutputStream os = new ObjectOutputStream(baos);
      os.writeObject(initialAst.getJProgram());
      os.writeObject(initialAst.getJsProgram());
      os.close();
      serializedAst = baos.toByteArray();

      // Very rough heuristic.
      astMemoryUsage = Math.max(astMemoryUsage, serializedAst.length * 4);
    } catch (IOException e) {
      throw new RuntimeException(
          "Should be impossible for memory based streams", e);
    } finally {
      PerfLogger.end();
    }
  }

  /**
   * Force byte serialization of AST before writing.
   */
  private Object writeReplace() {
    synchronized (myLockObject) {
      if (serializedAst == null) {
        serializeAst();
      }
    }
    return this;
  }
}
