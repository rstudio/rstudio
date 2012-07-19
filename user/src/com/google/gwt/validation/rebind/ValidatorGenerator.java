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
package com.google.gwt.validation.rebind;

import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.validation.client.GwtValidation;

/**
 * <strong>EXPERIMENTAL</strong> and subject to change. Do not use this in
 * production code.
 * <p>
 * Generates the generic {@link javax.validation.Validator}. The generic
 * validator only handles the classes listed in the
 * {@link com.google.gwt.validation.client.GwtValidation} annotation. See
 * {@link com.google.gwt.validation.client.GwtValidation} for usage.
 */
public final class ValidatorGenerator extends Generator {

  @Override
  public String generate(TreeLogger logger, GeneratorContext context,
      String typeName) throws UnableToCompleteException {
    TypeOracle typeOracle = context.getTypeOracle();
    assert (typeOracle != null);

    JClassType validatorType = typeOracle.findType(typeName);
    if (validatorType == null) {
      logger.log(TreeLogger.ERROR, "Unable to find metadata for type '"
          + typeName + "'", null);
      throw new UnableToCompleteException();
    }

    GwtValidation gwtValidation = validatorType.findAnnotationInTypeHierarchy(GwtValidation.class);

    if (gwtValidation == null) {
      logger.log(TreeLogger.ERROR, typeName + " must be anntotated with "
          + GwtValidation.class.getCanonicalName(), null);
      throw new UnableToCompleteException();
    }

    if (gwtValidation.value().length == 0) {
      logger.log(TreeLogger.ERROR,
          "The @" + GwtValidation.class.getSimpleName() + "  of " + typeName
              + "must specify at least one bean type to validate.", null);
      throw new UnableToCompleteException();
    }

    if (gwtValidation.groups().length == 0) {
      logger.log(TreeLogger.ERROR,
          "The @" + GwtValidation.class.getSimpleName() + "  of " + typeName
              + "must specify at least one validation group.", null);
      throw new UnableToCompleteException();
    }

    TreeLogger validatorLogger = logger.branch(TreeLogger.DEBUG,
        "Generating Validator for  '" + validatorType.getQualifiedSourceName()
            + "'", null);
    AbstractCreator creator = new ValidatorCreator(validatorType,
        gwtValidation,
        validatorLogger,
        context);
    return creator.create();
  }
}
