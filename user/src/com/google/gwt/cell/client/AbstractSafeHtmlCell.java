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
package com.google.gwt.cell.client;

import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.text.shared.SafeHtmlRenderer;

import java.util.Set;

/**
 * A superclass for {@link Cell}s that render or escape a String argument as
 * HTML.
 *
 * <p>
 * Note: This class is new and its interface subject to change.
 * </p>
 *
 * @param <C> the type that this Cell represents
 */
public abstract class AbstractSafeHtmlCell<C> extends AbstractCell<C> {

  private final SafeHtmlRenderer<C> renderer;

  public AbstractSafeHtmlCell(SafeHtmlRenderer<C> renderer,
      String... consumedEvents) {
    super(consumedEvents);
    if (renderer == null) {
      throw new IllegalArgumentException("renderer == null");
    }
    this.renderer = renderer;
  }

  public AbstractSafeHtmlCell(SafeHtmlRenderer<C> renderer,
      Set<String> consumedEvents) {
    super(consumedEvents);
    if (renderer == null) {
      throw new IllegalArgumentException("renderer == null");
    }
    this.renderer = renderer;
  }

  public SafeHtmlRenderer<C> getRenderer() {
    return renderer;
  }

  @Override
  public void render(C data, Object key, SafeHtmlBuilder sb) {
    if (data == null) {
      render((SafeHtml) null, key, sb);
    } else {
      render(renderer.render(data), key, sb);
    }
  }

  protected abstract void render(SafeHtml data, Object key, SafeHtmlBuilder sb);
}
