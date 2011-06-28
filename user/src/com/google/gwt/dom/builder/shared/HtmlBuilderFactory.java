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
 * Factory for creating element builders that use string concatenation to
 * generate HTML.
 */
public class HtmlBuilderFactory extends ElementBuilderFactory {

  private static HtmlBuilderFactory instance;

  /**
   * Get the instance of the {@link HtmlBuilderFactory}.
   * 
   * <p>
   * Use {@link ElementBuilderFactory#get()} to fetch a factory optimized for
   * the browser client. However, you can use this factory directly if you want
   * to force the builders to builder elements using HTML string concatenation
   * and innerHTML. You can also use this factory if you want access to the HTML
   * string, such as when you are building HTML on a server.
   * </p>
   * 
   * @return the {@link ElementBuilderFactory}
   */
  public static HtmlBuilderFactory get() {
    if (instance == null) {
      instance = new HtmlBuilderFactory();
    }
    return instance;
  }

  /**
   * Created from static factory method.
   */
  protected HtmlBuilderFactory() {
  }

  @Override
  public HtmlDivBuilder createDivBuilder() {
    return impl().startDiv();
  }

  @Override
  public HtmlOptionBuilder createOptionBuilder() {
    return impl().startOption();
  }

  @Override
  public HtmlSelectBuilder createSelectBuilder() {
    return impl().startSelect();
  }

  @Override
  public HtmlElementBuilder trustedCreate(String tagName) {
    return impl().trustedStart(tagName);
  }

  private HtmlBuilderImpl impl() {
    return new HtmlBuilderImpl();
  }
}
