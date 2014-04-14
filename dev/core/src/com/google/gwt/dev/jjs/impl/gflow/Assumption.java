/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.dev.jjs.impl.gflow;

/**
 * Assumptions are members of the lattice used in the analysis. Assumptions
 * are associated with graph edges and depict analysis knowledge about the flow.
 *
 * So far we need only join operation to perform analysis.
 *
 * Lattice's bottom should be represented by <code>null</null>.
 *
 * Assumption should implement correct equals() and hashCode() methods.
 *
 * @param <Self> self type.
 */
public interface Assumption<Self extends Assumption<?>> {
    /**
     * Compute least upper bound. Should accept <code>null</null>.
     * It's allowed to return value, equal either to <code>this</code> or to
     * <code>other</code>. If you're going to modify result of join operation,
     * you have to copy it.
     */
    Self join(Self other);
}
