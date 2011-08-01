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
package com.google.gwt.uibinder.rebind;

import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.uibinder.attributeparsers.AttributeParsers;

import org.w3c.dom.Element;

/**
 * The default implemenatation of {@link XMLElementProvider}.
 */
public class XMLElementProviderImpl implements XMLElementProvider {
  private final AttributeParsers attributeParsers;
  // bundleParsers for legacy templates
  private final TypeOracle oracle;
  private final MortalLogger logger;
  private final DesignTimeUtils designTime;

  // bundleParsers for legacy templates
  public XMLElementProviderImpl(AttributeParsers attributeParsers,
      TypeOracle oracle, MortalLogger logger,
      DesignTimeUtils designTime) {
    this.attributeParsers = attributeParsers;
    this.oracle = oracle;
    this.logger = logger;
    this.designTime = designTime;
  }

  public XMLElement get(Element e) {
    return new XMLElement(e, attributeParsers, oracle, logger, designTime,
        this);
  }
}
