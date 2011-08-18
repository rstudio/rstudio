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

import com.google.gwt.safehtml.shared.SafeHtml;

/**
 * A subclass of standard {@link HtmlTableSectionBuilder} that allows directly appending html to
 * a table section.
 * <p>
 * Some browsers do not support setting innerHTML on a table section element (such
 * as a tbody), so both the DOM and HTML based implementations throw an error to
 * ensure consistency. This class exists to allow users to append HTML to a
 * TableSectionBuilder if they opt into the HTML version.
 * </p>
 */
public final class HtmlOnlyTableSectionBuilder extends HtmlTableSectionBuilder {

  /**
   * Create and return a table body section builder.
   */
  public static HtmlOnlyTableSectionBuilder createTBodyBuilder() {
    HtmlBuilderImpl htmlBuilderImpl = new HtmlBuilderImpl();
    htmlBuilderImpl.startTBody();
    return new HtmlOnlyTableSectionBuilder(htmlBuilderImpl);
  }

  /**
   * Create and return a table footer section builder.
   */
  public static HtmlOnlyTableSectionBuilder createTFootBuilder() {
    HtmlBuilderImpl htmlBuilderImpl = new HtmlBuilderImpl();
    htmlBuilderImpl.startTFoot();
    return new HtmlOnlyTableSectionBuilder(htmlBuilderImpl);
  }
    
  /**
   * Create and return a table header section builder.
   */
  public static HtmlOnlyTableSectionBuilder createTHeadBuilder() {
    HtmlBuilderImpl htmlBuilderImpl = new HtmlBuilderImpl();
    htmlBuilderImpl.startTHead();
    return new HtmlOnlyTableSectionBuilder(htmlBuilderImpl);
  }
      
  private HtmlBuilderImpl delegate;
  
  /**
   * Construct a new html table section builder.
   * 
   * @param delegate delegate the html based delegate that builds the element
   */
  private HtmlOnlyTableSectionBuilder(HtmlBuilderImpl delegate) {
    super(delegate);
    this.delegate = delegate;
  }
  
  /**
   * Append html to the builder and validate the correctness of the html.
   * 
   * @param html the html for the table section
   */
  @Override
  public TableSectionBuilder html(SafeHtml html) {
    delegate.html(html);
    return getReturnBuilder();
  }
}
