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
import com.google.gwt.validation.client.GwtValidation;
import com.google.gwt.validation.client.impl.GwtSpecificValidator;

import javax.validation.Validator;

/**
 * <strong>EXPERIMENTAL</strong> and subject to change. Do not use this in
 * production code.
 * <p>
 * Generates subclasses of {@link Validator} and {@link GwtSpecificValidator}. The generic
 * validator only handles the classes listed in the
 * {@link com.google.gwt.validation.client.GwtValidation} annotation. See
 * {@link com.google.gwt.validation.client.GwtValidation} for usage.
 */
public final class ValidatorGenerator extends Generator {

  private final BeanHelperCache cache;

  // called by the compiler via reflection
  public ValidatorGenerator() {
    this.cache = new BeanHelperCache();
  }

  // called from tests
  public ValidatorGenerator(BeanHelperCache cache) {
    this.cache = cache;
  }

  @Override
  public String generate(TreeLogger logger, GeneratorContext context,
      String typeName) throws UnableToCompleteException {
    TypeOracle typeOracle = context.getTypeOracle();
    assert (typeOracle != null);

    JClassType validatorType = findType(logger, typeOracle, typeName);
    JClassType genericType = findType(logger, typeOracle, Validator.class.getName());
    JClassType gwtSpecificType =
        findType(logger, typeOracle, GwtSpecificValidator.class.getName());

    if (validatorType.isAssignableTo(genericType)) {
      return generateGenericValidator(logger, context, validatorType);
    } else if (validatorType.isAssignableTo(gwtSpecificType)) {
      return generateGwtSpecificValidator(logger, context, validatorType);
    } else {
      logger.log(TreeLogger.ERROR,
          "type is not a ValidatorGenerator or GwtSpecificValidatorGenerator: '" + typeName + "'",
          null);
      throw new UnableToCompleteException();
    }
  }

  private JClassType findType(TreeLogger logger, TypeOracle typeOracle, String typeName)
      throws UnableToCompleteException {
    JClassType result = typeOracle.findType(typeName);
    if (result == null) {
      logger.log(TreeLogger.ERROR, "Unable to find metadata for type '"
          + typeName + "'", null);
      throw new UnableToCompleteException();
    }
    return result;
  }

  private String generateGenericValidator(TreeLogger logger, GeneratorContext context,
      JClassType validatorType) throws UnableToCompleteException {
    String typeName = validatorType.getName();

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
        context, cache);
    return creator.create();
  }

  private String generateGwtSpecificValidator(TreeLogger logger, GeneratorContext context,
      JClassType validatorType) throws UnableToCompleteException {

    JClassType gwtSpecificInterface = getGwtSpecificValidator(logger, validatorType);
    JClassType beanType = getBeanType(logger, validatorType, gwtSpecificInterface);

    BeanHelper beanHelper = cache.createHelper(beanType, logger, context);

    if (beanHelper == null) {
      logger.log(TreeLogger.ERROR, "Unable to create BeanHelper for " + beanType
          + " " + GwtSpecificValidator.class.getSimpleName()
          + ".", null);
      throw new UnableToCompleteException();
    }

    AbstractCreator creator = new GwtSpecificValidatorCreator(validatorType,
        beanType, beanHelper, logger, context, cache);
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
