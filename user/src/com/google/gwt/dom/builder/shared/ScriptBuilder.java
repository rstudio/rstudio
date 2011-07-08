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
 * Builds an script element.
 */
public interface ScriptBuilder extends ElementBuilderBase<ScriptBuilder> {

  String UNSUPPORTED_HTML = "Script elements do not support html.  Use text() instead.";

  /**
   * Indicates that the user agent can defer processing of the script.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/scripts.html#adef-defer">W3C
   *      HTML Specification</a>
   */
  ScriptBuilder defer(String defer);

  /**
   * URI designating an external script.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/scripts.html#adef-src-SCRIPT">W3C
   *      HTML Specification</a>
   */
  ScriptBuilder src(String src);

  /**
   * The content type of the script language.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/scripts.html#adef-type-SCRIPT">W3C
   *      HTML Specification</a>
   */
  ScriptBuilder type(String type);
}
