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
 * Builds an source element.
 */
public interface SourceBuilder extends ElementBuilderBase<SourceBuilder> {

  /**
   * Sets the source URL for the media.
   * 
   * @param url a String URL
   */
  SourceBuilder src(String url);

  /**
   * Sets the type of media represented by the src. The browser will look at the
   * type when deciding which source files to request from the server.
   * 
   * 
   * @param type the media type
   * @see com.google.gwt.dom.client.SourceElement#setType(String)
   */
  SourceBuilder type(String type);
}
