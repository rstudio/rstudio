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
package com.google.gwt.core.ext.linker;

import com.google.gwt.core.ext.Linker;

/**
 * An external script file referenced in the module manifest. The index is
 * important because output order must match module declaration order.
 */
public abstract class ScriptReference extends Artifact<ScriptReference> {
  private final int index;
  private final String src;

  protected ScriptReference(Class<? extends Linker> linkerType, String src,
      int index) {
    super(linkerType);
    this.src = src;
    this.index = index;
  }

  /**
   * The <code>src</code> attribute of the resource. This string is returned
   * raw and may be a partial path or a URL.
   */
  public final String getSrc() {
    return src;
  }

  @Override
  public final int hashCode() {
    return getSrc().hashCode();
  }

  @Override
  public String toString() {
    return "<script src='" + getSrc() + "'>";
  }

  @Override
  protected final int compareToComparableArtifact(ScriptReference o) {
    return index - o.index;
  }

  @Override
  protected final Class<ScriptReference> getComparableArtifactType() {
    return ScriptReference.class;
  }
}
