/*
 * Copyright 2010 Google Inc.
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

package com.google.gwt.user.client.runasync;

import com.google.gwt.core.client.impl.CrossSiteLoadingStrategy;

/**
 * A variant of {@link CrossSiteLoadingStrategy} used for the
 * {@link com.google.gwt.dev.jjs.test.RunAsyncFailureTest}.
 * It downloads code fragments via a faulty servlet
 * instead of the normal "deferredjs" location.
 */
public class CrossSiteLoadingStrategyForRunAsyncFailureTest extends
    CrossSiteLoadingStrategy {
  @Override
  protected String getDeferredJavaScriptDirectory() {
    return "runAsyncFailure/deferredjs/";
  }
}
