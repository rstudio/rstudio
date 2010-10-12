/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.text.shared;

import java.io.IOException;

/**
 * Abstract implementation of a renderer to make implementation of rendering
 * simpler.
 * 
 * @param <T> the type to render
 */
public abstract class AbstractRenderer<T> implements Renderer<T> {
  public void render(T object, Appendable appendable) throws IOException {
    appendable.append(render(object));
  }
}