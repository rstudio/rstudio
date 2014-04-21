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

package com.google.gwt.user.rebind;

import com.google.gwt.core.ext.BadPropertyValueException;
import com.google.gwt.core.ext.ConfigurationProperty;
import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.Generator.RunsLocal;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.PropertyOracle;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.user.client.DocumentModeAsserter;
import com.google.gwt.user.client.DocumentModeAsserter.Severity;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

/**
 * Generator for {@link com.google.gwt.user.client.DocumentModeAsserter}.
 */
@RunsLocal(requiresProperties = {"document.compatMode", "document.compatMode.severity"})
public class DocumentModeGenerator extends Generator {

  @Override
  public String generate(TreeLogger logger, GeneratorContext context, String typeName)
      throws UnableToCompleteException {
    TypeOracle typeOracle = context.getTypeOracle();

    JClassType userType;
    try {
      userType = typeOracle.getType(typeName);
    } catch (NotFoundException e) {
      logger.log(TreeLogger.ERROR, "Unable to find metadata for type: " + typeName, e);
      throw new UnableToCompleteException();
    }
    String packageName = userType.getPackage().getName();
    String className = userType.getName();
    className = className.replace('.', '_');

    if (userType.isInterface() == null) {
      logger.log(TreeLogger.ERROR, userType.getQualifiedSourceName() + " is not an interface", null);
      throw new UnableToCompleteException();
    }

    PropertyOracle propertyOracle = context.getPropertyOracle();

    String severityText;
    try {
      ConfigurationProperty property = propertyOracle.getConfigurationProperty(DocumentModeAsserter.PROPERTY_DOCUMENT_COMPATMODE_SEVERITY);
      severityText = property.getValues().get(0);
    } catch (BadPropertyValueException e) {
      logger.log(TreeLogger.ERROR, "Unable to find value for '"
          + DocumentModeAsserter.PROPERTY_DOCUMENT_COMPATMODE_SEVERITY + "'", e);
      throw new UnableToCompleteException();
    }
    Severity severity;
    try {
      severity = Severity.valueOf(severityText);
    } catch (IllegalArgumentException e) {
      logger.log(TreeLogger.ERROR, "Value '" + severityText + "' for '"
          + DocumentModeAsserter.PROPERTY_DOCUMENT_COMPATMODE_SEVERITY + "' is not one of: "
          + Arrays.toString(Severity.values()), e);
      throw new UnableToCompleteException();
    }

    List<String> documentModes;
    try {
      ConfigurationProperty property = propertyOracle.getConfigurationProperty(DocumentModeAsserter.PROPERTY_DOCUMENT_COMPATMODE);
      documentModes = property.getValues();
    } catch (BadPropertyValueException e) {
      logger.log(TreeLogger.ERROR, "Unable to find value for '"
          + DocumentModeAsserter.PROPERTY_DOCUMENT_COMPATMODE + "'", e);
      throw new UnableToCompleteException();
    }

    ClassSourceFileComposerFactory composerFactory = new ClassSourceFileComposerFactory(
        packageName, className);
    composerFactory.addImplementedInterface(userType.getQualifiedSourceName());
    composerFactory.addImport(DocumentModeAsserter.Severity.class.getCanonicalName());

    PrintWriter pw = context.tryCreate(logger, packageName, className);
    if (pw != null) {
      SourceWriter sw = composerFactory.createSourceWriter(context, pw);

      sw.println();

      sw.println("public String[] getAllowedDocumentModes() {");
      sw.indent();
      sw.println("return new String[] {");
      sw.indent();
      for (String mode : documentModes) {
        sw.println("\"" + mode + "\", ");
      }
      sw.outdent();
      sw.println("};");
      sw.outdent();
      sw.println("}");

      sw.println();

      sw.println("public Severity getDocumentModeSeverity() {");
      sw.indent();
      sw.println("return Severity." + severity.toString() + ";");
      sw.outdent();
      sw.println("}");
      sw.println();

      sw.commit(logger);
    }
    return composerFactory.getCreatedClassName();
  }
}
