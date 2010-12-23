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
package com.google.gwt.core.ext;

import com.google.gwt.dev.javac.rebind.RebindResult;
import com.google.gwt.dev.javac.rebind.RebindStatus;

/**
 * EXPERIMENTAL and subject to change. Do not use this in production code.
 * <p>  
 * A wrapper class for using base {@link Generator} implementations where
 * a {@link GeneratorExt} instance is needed.
 */
public class GeneratorExtWrapper extends GeneratorExt {
 
  /**
   * Get a new instance wrapped from a base {@link Generator} implementation.
   */
  public static GeneratorExt newInstance(Generator baseGenerator) {
    return new GeneratorExtWrapper(baseGenerator);
  }

  private final Generator baseGenerator;
 
  public GeneratorExtWrapper(Generator baseGenerator) {
    this.baseGenerator = baseGenerator;
  }
 
  /**
   * Pass through to the base generator's generate method.
   */
  @Override
  public String generate(TreeLogger logger, GeneratorContext context,
      String typeName) throws UnableToCompleteException {
    return this.baseGenerator.generate(logger, context, typeName);
  }

  /**
   * Call base generator's generate method, and don't attempt any caching.
   */
  @Override
  public RebindResult generateIncrementally(TreeLogger logger, 
      GeneratorContextExt context, String typeName) 
      throws UnableToCompleteException {

    RebindStatus status;
    String resultTypeName = generate(logger, context, typeName);
    if (resultTypeName == null) {
      status = RebindStatus.USE_EXISTING;
      resultTypeName = typeName;
    } else {
      status = RebindStatus.USE_ALL_NEW_WITH_NO_CACHING;
    }

    return new RebindResult(status, resultTypeName);
  }
}