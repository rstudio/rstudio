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

import com.google.gwt.dom.builder.shared.DivBuilder;
import com.google.gwt.dom.builder.shared.ElementBuilder;
import com.google.gwt.dom.builder.shared.ElementBuilderFactory;
import com.google.gwt.dom.builder.shared.OptionBuilder;
import com.google.gwt.dom.builder.shared.SelectBuilder;

/**
 * Factory for creating element builders that construct elements using DOM
 * manipulation.
 */
public class DomBuilderFactory extends ElementBuilderFactory {

  private static DomBuilderFactory instance;

  /**
   * Get the instance of the {@link DomBuilderFactory}.
   * 
   * <p>
   * Use {@link ElementBuilderFactory#get()} to fetch a factory optimized for
   * the browser client. However, you can use this factory directly if you want
   * to force the builders to build elements use DOM manipulation.
   * </p>
   * 
   * @return the {@link ElementBuilderFactory}
   */
  public static DomBuilderFactory get() {
    if (instance == null) {
      instance = new DomBuilderFactory();
    }
    return instance;
  }

  /**
   * Created from static factory method.
   */
  public DomBuilderFactory() {
  }

  @Override
  public DivBuilder createDivBuilder() {
    return impl().startDiv();
  }

  @Override
  public OptionBuilder createOptionBuilder() {
    return impl().startOption();
  }

  @Override
  public SelectBuilder createSelectBuilder() {
    return impl().startSelect();
  }

  @Override
  public ElementBuilder trustedCreate(String tagName) {
    return impl().trustedStart(tagName);
  }

  private DomBuilderImpl impl() {
    return new DomBuilderImpl();
  }
}
