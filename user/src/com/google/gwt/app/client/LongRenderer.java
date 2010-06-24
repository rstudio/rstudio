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
package com.google.gwt.app.client;

import com.google.gwt.text.shared.AbstractRenderer;
import com.google.gwt.text.shared.Renderer;

/**
 * <span style="color:red">Experimental API: This class is still under rapid
 * development, and is very likely to be deleted. Use it at your own risk.
 * </span>
 * <p>
 * Renderer of Long values.
 */
public class LongRenderer extends AbstractRenderer<Long> {
  private static LongRenderer INSTANCE;

  /**
   * @return the instance
   */
  public static Renderer<Long> instance() {
    if (INSTANCE == null) {
      INSTANCE = new LongRenderer();
    }
    return INSTANCE;
  }

  protected LongRenderer() {
  }

  public String render(Long object) {
    return String.valueOf(object);
  }
}
