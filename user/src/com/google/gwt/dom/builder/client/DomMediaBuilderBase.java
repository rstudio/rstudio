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
package com.google.gwt.dom.builder.client;

import com.google.gwt.dom.builder.shared.MediaBuilder;
import com.google.gwt.dom.client.MediaElement;

/**
 * Base class for HTML-based implementations of {@link MediaBuilder}.
 * 
 * @param <R> the builder type returned from build methods
 * @param <E> the {@link MediaElement} type
 */
public class DomMediaBuilderBase<R extends MediaBuilder<?>, E extends MediaElement> extends
    DomElementBuilderBase<R, E> implements MediaBuilder<R> {

  DomMediaBuilderBase(DomBuilderImpl delegate) {
    super(delegate);
  }

  @Override
  public R autoplay() {
    assertCanAddAttribute().setAutoplay(true);
    return getReturnBuilder();
  }

  @Override
  public R controls() {
    assertCanAddAttribute().setControls(true);
    return getReturnBuilder();
  }

  @Override
  public R loop() {
    assertCanAddAttribute().setLoop(true);
    return getReturnBuilder();
  }

  @Override
  public R muted() {
    assertCanAddAttribute().setMuted(true);
    return getReturnBuilder();
  }

  @Override
  public R preload(String preload) {
    assertCanAddAttribute().setPreload(preload);
    return getReturnBuilder();
  }

  @Override
  public R src(String url) {
    assertCanAddAttribute().setSrc(url);
    return getReturnBuilder();
  }
}
