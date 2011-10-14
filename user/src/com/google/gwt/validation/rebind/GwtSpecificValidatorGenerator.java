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
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.validation.client.impl.GwtSpecificValidator;

/**
 * <strong>EXPERIMENTAL</strong> and subject to change. Do not use this in
 * production code.
 * <p>
 * Generates a {@link com.google.gwt.validation.client.GwtSpecificValidator}.
 * <p>
 * This class is thread safe.
 */
public final class GwtSpecificValidatorGenerator extends Generator {

  @Override
  public String generate(TreeLogger logger, GeneratorContext context,
      String typeName) throws UnableToCompleteException {
    JClassType validatorType = context.getTypeOracle().findType(typeName);
    TypeOracle typeOracle = context.getTypeOracle();
    assert (typeOracle != null);

    JClassType validator = typeOracle.findType(typeName);
    if (validator == null) {
      logger.log(TreeLogger.ERROR, "Unable to find metadata for type '"
          + typeName + "'", null);
      throw new UnableToCompleteException();
    }

    JClassType gwtSpecificInterface = getGwtSpecificValidator(logger, validator);
    JClassType beanType = getBeanType(logger, validator, gwtSpecificInterface);

    BeanHelper beanHelper = BeanHelper.createBeanHelper(beanType,logger,context);

    if (beanHelper == null) {
      logger.log(TreeLogger.ERROR, "Unable to create BeanHelper for " + beanType
          + " " + GwtSpecificValidator.class.getSimpleName()
          + ".", null);
      throw new UnableToCompleteException();
    }

    AbstractCreator creator = new GwtSpecificValidatorCreator(validatorType,
        beanType, beanHelper, logger, context);
    return creator.create();
  }

  private JClassType getBeanType(TreeLogger logger, JClassType validator,
      JClassType gwtSpecificInterface) throws UnableToCompleteException {
    if (gwtSpecificInterface instanceof JParameterizedType) {
      JParameterizedType paramType = (JParameterizedType) gwtSpecificInterface;
      return paramType.getTypeArgs()[0];
    }
    logger.log(TreeLogger.ERROR, validator.getQualifiedSourceName()
        + " must implement " + GwtSpecificValidator.class.getCanonicalName()
        + " with a one generic parameter.", null);
    throw new UnableToCompleteException();
  }

  private JClassType getGwtSpecificValidator(TreeLogger logger,
      JClassType validator) throws UnableToCompleteException {
    for (JClassType interfaceType : validator.getImplementedInterfaces()) {
      if (interfaceType.getQualifiedSourceName().endsWith(
          GwtSpecificValidator.class.getCanonicalName())) {
        return interfaceType;
      }
    }
    logger.log(TreeLogger.ERROR, validator.getQualifiedSourceName()
        + " must implement " + GwtSpecificValidator.class.getCanonicalName(),
        null);
    throw new UnableToCompleteException();
  }
}
