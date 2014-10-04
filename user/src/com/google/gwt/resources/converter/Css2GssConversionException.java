/*
 * Copyright 2014 Google Inc.
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
package com.google.gwt.resources.converter;

import com.google.gwt.resources.css.ast.CssCompilerException;

/**
 * Css2GssConversionException signals a problem with the conversion from CSS to GSS.
 */
public class Css2GssConversionException extends CssCompilerException {

  private static final long serialVersionUID = 4362497787247994365L;

  public Css2GssConversionException(String message) {
    super(message);
  }

  public Css2GssConversionException(String message, Throwable cause) {
    super(message, cause);
  }
}
