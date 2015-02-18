/*
 * Copyright 2014 Google Inc.
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
package com.google.gwt.resources.gss;

import com.google.gwt.resources.ext.ResourceContext;
import com.google.gwt.thirdparty.common.css.compiler.ast.GssFunction;
import com.google.gwt.thirdparty.common.css.compiler.gssfunctions.DefaultGssFunctionMapProvider;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableMap;

import java.util.Map;

/**
 * {@link GssFunctionMapProvider} that adds the mapping of GssFunction implemented for GWT.
 */
public class GwtGssFunctionMapProvider extends DefaultGssFunctionMapProvider {
  private final ResourceContext context;

  public GwtGssFunctionMapProvider(ResourceContext context) {
    this.context = context;
  }

  @Override
  public Map<String, GssFunction> get() {
    Map<String, GssFunction> gssFunctionMap = super.get();

    return ImmutableMap.<String, GssFunction>builder().putAll(gssFunctionMap)
        // TODO add a namespace for gwt-specific function ?
        .put(EvalFunction.getName(), new EvalFunction())
        .put(ValueFunction.getName(), new ValueFunction())
        .put(ResourceUrlFunction.getName(), new ResourceUrlFunction(context))
        .build();
  }
}
