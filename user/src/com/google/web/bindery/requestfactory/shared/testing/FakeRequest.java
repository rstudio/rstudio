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
package com.google.web.bindery.requestfactory.shared.testing;

import com.google.web.bindery.requestfactory.shared.Receiver;
import com.google.web.bindery.requestfactory.shared.Request;
import com.google.web.bindery.requestfactory.shared.RequestContext;

/**
 * A no-op implementation of Request that can be used as a base type for writing
 * unit tests.
 * 
 * @param <T> The return type of objects in the corresponding response.
 */
public class FakeRequest<T> implements Request<T> {

  /**
   * No-op.
   */
  @Override
  public void fire() {
  }

  /**
   * No-op.
   */
  @Override
  public void fire(Receiver<? super T> receiver) {
  }

  /**
   * Returns {@code null}.
   */
  @Override
  public RequestContext getRequestContext() {
    return null;
  }

  /**
   * Returns {@code null}.
   */
  @Override
  public RequestContext to(Receiver<? super T> receiver) {
    return null;
  }

  /**
   * Returns {@code this}.
   */
  @Override
  public Request<T> with(String... propertyRefs) {
    return this;
  }
}
