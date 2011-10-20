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

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.uibinder.attributeparsers.CssNameConverter;
import com.google.gwt.uibinder.rebind.model.ImplicitCssResource;
import com.google.gwt.uibinder.rebind.model.OwnerField;

import java.util.Set;

/**
 * Implementation of FieldWriter for an {@link ImplicitCssResource}.
 */
class FieldWriterOfGeneratedCssResource extends AbstractFieldWriter {
  private static final CssNameConverter nameConverter = new CssNameConverter();

  private final ImplicitCssResource css;
  private final JType stringType;

  public FieldWriterOfGeneratedCssResource(FieldManager manager, JType stringType,
      ImplicitCssResource css, MortalLogger logger) {
    super(manager, FieldWriterType.GENERATED_CSS, css.getName(), logger);
    this.stringType = stringType;
    this.css = css;
  }

  public JClassType getAssignableType() {
    return css.getExtendedInterface();
  }

  public JClassType getInstantiableType() {
    return null;
  }

  public String getQualifiedSourceName() {
    return css.getQualifiedSourceName();
  }

  @Override
  public JType getReturnType(String[] path, MonitoredLogger logger) {
    if (path.length == 2) {
      String maybeCssClass = path[1];
      Set<String> cssClassNames = null;
      try {
        cssClassNames = css.getCssClassNames();
        if (cssClassNames.contains(maybeCssClass)
            || cssClassNames.contains(nameConverter.convertName(maybeCssClass))
            || css.getNormalizedCssClassNames().contains(maybeCssClass)) {
          return stringType;
        }
      } catch (UnableToCompleteException e) {
        logger.error("Can't interpret CSS");
      }
    }
    return super.getReturnType(path, logger);
  }

  @Override
  public void writeFieldBuilder(IndentedWriter w,
      int getterCount, OwnerField ownerField) {
    w.write("%s;  // generated css resource must be always created. Type: %s. Precedence: %s",
        FieldManager.getFieldBuilder(getName()), getFieldType(), getBuildPrecedence());
  }
}
