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

import com.google.web.bindery.requestfactory.shared.BaseProxy;
import com.google.web.bindery.requestfactory.shared.EntityProxy;
import com.google.web.bindery.requestfactory.shared.EntityProxyId;
import com.google.web.bindery.requestfactory.shared.Receiver;
import com.google.web.bindery.requestfactory.shared.Request;
import com.google.web.bindery.requestfactory.shared.RequestContext;
import com.google.web.bindery.requestfactory.shared.RequestFactory;

/**
 * A no-op implementation of RequestConext that can be used as a base type for
 * writing unit tests.
 */
public class FakeRequestContext implements RequestContext {

  /**
   * Always returns {@code other}.
   */
  @Override
  public <T extends RequestContext> T append(T other) {
    return other;
  }

  /**
   * Returns {@code null}.
   */
  @Override
  public <T extends BaseProxy> T create(Class<T> clazz) {
    return null;
  }

  /**
   * Always returns {@code object}.
   */
  @Override
  public <T extends BaseProxy> T edit(T object) {
    return object;
  }

  /**
   * Returns {@code null}.
   */
  @Override
  public <P extends EntityProxy> Request<P> find(EntityProxyId<P> proxyId) {
    return null;
  }

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
  public void fire(Receiver<Void> receiver) {
  }

  /**
   * Returns {@code null}.
   */
  @Override
  public RequestFactory getRequestFactory() {
    return null;
  }

  /**
   * Always returns {@code false}.
   */
  @Override
  public boolean isChanged() {
    return false;
  }
}
