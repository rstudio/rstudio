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
package com.google.gwt.core.ext;

/**
 * A mode to indicate how incremental generator output should be integrated by
 * the deferred binding implementation. The RebindMode is included as a member
 * of the {@link RebindResult} returned by
 * {@link IncrementalGenerator#generateIncrementally}. It is up to each
 * generator implementation to determine the conditions for reuse of previously
 * generated cached output.
 *
 * @see RebindResult
 * @see IncrementalGenerator#generateIncrementally
 */
public enum RebindMode {

  /**
   * Indicates no generated code is needed to satisfy this rebind. This mode can
   * be used in cases where the requested type can be used directly as a default
   * instantiable class, or in cases where a generator determines it has already
   * run for the requested type in the current context (e.g. via a failed call
   * to {@link GeneratorContext#tryCreate}).
   */
  USE_EXISTING,

  /**
   * Indicates only newly generated output should be used. All generated output
   * will be cached.
   */
  USE_ALL_NEW,

  /**
   * Indicates only newly generated output should be used, and no output should
   * be cached. This mode should be used when no caching can be taken advantage
   * of, such as for generators which don't implement
   * {@link IncrementalGenerator#generateIncrementally}.
   */
  USE_ALL_NEW_WITH_NO_CACHING,

  /**
   * Indicates nothing new was generated, only cached output previously
   * generated should be used.
   */
  USE_ALL_CACHED,

  /**
   * Indicates that a mixture of newly generated and previously cached output
   * should be used. Types marked with a successful call to
   * {@link GeneratorContext#tryReuseTypeFromCache} should be reused from cache,
   * while everything else committed to the context should be treated as freshly
   * generated output. A new composite cache entry will be created which
   * combines the freshly generated output and the output reused from cache.
   */
  USE_PARTIAL_CACHED
}
