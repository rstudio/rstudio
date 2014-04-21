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
package com.google.gwt.resources.rebind.context;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.typeinfo.JClassType;

/**
 * This is a refinement that will use data urls for browsers that support them.
 * Only files whose size are smaller than MAX_INLINE_SIZE will be inlined.
 * Larger files will use the standard CacheBundle behavior.
 *
 * @see "RFC 2397"
 */
public final class InlineClientBundleGenerator extends
    AbstractClientBundleGenerator {

  private final ClientBundleContext clientBundleCtx = new ClientBundleContext();

  @Override
  protected AbstractResourceContext createResourceContext(TreeLogger logger,
      GeneratorContext context, JClassType resourceBundleType) {
    return new InlineResourceContext(logger.branch(TreeLogger.DEBUG,
        "Using inline resources", null), context, resourceBundleType,
        clientBundleCtx);
  }
}
