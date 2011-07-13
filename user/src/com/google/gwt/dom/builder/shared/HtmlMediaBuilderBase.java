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
package com.google.gwt.dom.builder.shared;

/**
 * Base class for HTML-based implementations of {@link MediaBuilder}.
 * 
 * @param <R> the builder type returned from build methods
 */
public class HtmlMediaBuilderBase<R extends MediaBuilder<?>> extends HtmlElementBuilderBase<R>
    implements MediaBuilder<R> {

  HtmlMediaBuilderBase(HtmlBuilderImpl delegate) {
    super(delegate);
  }

  @Override
  public R autoplay() {
    return trustedAttribute("autoplay", "autoplay");
  }

  @Override
  public R controls() {
    return trustedAttribute("controls", "controls");
  }

  @Override
  public R loop() {
    return trustedAttribute("loop", "loop");
  }

  @Override
  public R muted() {
    return trustedAttribute("muted", "muted");
  }

  @Override
  public R preload(String preload) {
    return trustedAttribute("preload", preload);
  }

  @Override
  public R src(String url) {
    return trustedAttribute("src", url);
  }
}
