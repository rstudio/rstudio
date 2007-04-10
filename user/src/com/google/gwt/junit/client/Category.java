/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.junit.client;

/**
 * A benchmark category. {@link com.google.gwt.junit.client.Benchmark}s which
 * use the GWT annotation, <code>@gwt.benchmark.category</code>, must set it to
 * a class which implements this interface.
 *
 * <p>The following GWT annotations can be set on a <code>Category</code>:
 *
 * <ul>
 *   <li><code>@gwt.benchmark.name</code> The name of the <code>Category</code>
 * </li>
 *  <li><code>@gwt.benchmark.description</code> The description of the
 * <code>Category</code></li>
 * </ul>
 * </p>
 * 
 */
public interface Category {
}
