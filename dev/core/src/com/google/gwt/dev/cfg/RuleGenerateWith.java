/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.dev.cfg;

import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.javac.StandardGeneratorContext;

/**
 * A rule to replace the type being rebound with a class whose name is
 * determined by a generator class. Generators usually generate new classes
 * during the deferred binding process, but it is not required.
 */
public class RuleGenerateWith extends Rule {

  private Generator generator;

  public RuleGenerateWith(Generator generator) {
    this.generator = generator;
  }

  public String realize(TreeLogger logger, GeneratorContext context,
      String typeName) throws UnableToCompleteException {

    String msg = "Invoking " + toString();
    logger = logger.branch(TreeLogger.DEBUG, msg, null);

    if (context instanceof StandardGeneratorContext) {
      ((StandardGeneratorContext) context).setCurrentGenerator(generator.getClass());
    }

    long before = System.currentTimeMillis();
    try {
      String className = generator.generate(logger, context, typeName);
      long after = System.currentTimeMillis();
      if (className == null) {
        msg = "Generator returned null, so the requested type will be used as is";
      } else {
        msg = "Generator returned class '" + className + "'";
      }
      logger.log(TreeLogger.DEBUG, msg, null);

      msg = "Finished in " + (after - before) + " ms";
      logger.log(TreeLogger.DEBUG, msg, null);

      return className;
    } catch (RuntimeException e) {
      logger.log(TreeLogger.ERROR, "Generator '"
          + generator.getClass().getName()
          + "' threw threw an exception while rebinding '" + typeName + "'", e);
      throw new UnableToCompleteException();
    }
  }

  public String toString() {
    return "<generate-with class='" + generator.getClass().getName() + "'/>";
  }

  @Override
  protected void dispose() {
    generator = null;
  }
}
