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
package com.google.gwt.uibinder.rebind;

import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.uibinder.client.UiTemplate;
import com.google.gwt.uibinder.rebind.messages.MessagesWriter;
import com.google.gwt.uibinder.rebind.model.ImplicitClientBundle;

import org.w3c.dom.Document;
import org.xml.sax.SAXParseException;

import java.io.PrintWriter;
import java.net.URL;

/**
 * Generator for implementations of
 * {@link com.google.gwt.uibinder.client.UiBinder}.
 */
public class UiBinderGenerator extends Generator {

  private static final String TEMPLATE_SUFFIX = ".ui.xml";

  static final String BINDER_URI = "urn:ui:com.google.gwt.uibinder";

  /**
   * Given a UiBinder interface, return the path to its ui.xml file, suitable
   * for any classloader to find it as a resource.
   */
  private static String deduceTemplateFile(MortalLogger logger,
      JClassType interfaceType) throws UnableToCompleteException {
    String templateName = null;
    UiTemplate annotation = interfaceType.getAnnotation(UiTemplate.class);
    if (annotation == null) {
      // if the interface is defined as a nested class, use the name of the
      // enclosing type
      if (interfaceType.getEnclosingType() != null) {
        interfaceType = interfaceType.getEnclosingType();
      }
      return slashify(interfaceType.getQualifiedSourceName()) + TEMPLATE_SUFFIX;
    } else {
      templateName = annotation.value();
      if (!templateName.endsWith(TEMPLATE_SUFFIX)) {
        logger.die("Template file name must end with "
            + TEMPLATE_SUFFIX);
      }

      /*
       * If the template file name (minus suffix) has no dots, make it relative
       * to the binder's package, otherwise slashify the dots
       */
      String unsuffixed = templateName.substring(0,
          templateName.lastIndexOf(TEMPLATE_SUFFIX));
      if (!unsuffixed.contains(".")) {
        templateName = slashify(interfaceType.getPackage().getName()) + "/"
            + templateName;
      } else {
        templateName = slashify(unsuffixed) + TEMPLATE_SUFFIX;
      }
    }
    return templateName;
  }

  private static String slashify(String s) {
    return s.replace(".", "/");
  }

  @Override
  public String generate(TreeLogger logger, GeneratorContext genCtx,
      String fqInterfaceName) throws UnableToCompleteException {
    TypeOracle oracle = genCtx.getTypeOracle();
    JClassType interfaceType;
    try {
      interfaceType = oracle.getType(fqInterfaceName);
    } catch (NotFoundException e) {
      throw new RuntimeException(e);
    }

    String implName = interfaceType.getName().replace('.', '_') + "Impl";
    String packageName = interfaceType.getPackage().getName();
    PrintWriterManager writers = new PrintWriterManager(genCtx, logger,
        packageName);
    PrintWriter printWriter = writers.tryToMakePrintWriterFor(implName);

    if (printWriter != null) {
      generateOnce(interfaceType, implName, printWriter, logger, oracle,
          writers);
    }
    return packageName + "." + implName;
  }

  private void generateOnce(JClassType interfaceType, String implName,
      PrintWriter binderPrintWriter, TreeLogger treeLogger, TypeOracle oracle,
      PrintWriterManager writerManager)
 throws UnableToCompleteException {

    MortalLogger logger = new MortalLogger(treeLogger);
    String templatePath = deduceTemplateFile(logger, interfaceType);
    MessagesWriter messages = new MessagesWriter(BINDER_URI, logger,
        templatePath, interfaceType.getPackage().getName(), implName);

    UiBinderWriter uiBinderWriter = new UiBinderWriter(interfaceType, implName,
        templatePath, oracle, logger, new FieldManager(oracle, logger), messages);

    Document doc = getW3cDoc(logger, templatePath);

    uiBinderWriter.parseDocument(doc, binderPrintWriter);

    if (messages.hasMessages()) {
      messages.write(writerManager.makePrintWriterFor(messages.getMessagesClassName()));
    }

    ImplicitClientBundle bundleClass = uiBinderWriter.getBundleClass();
    new BundleWriter(bundleClass, writerManager, oracle, logger).write();

    writerManager.commit();
  }

  private Document getW3cDoc(MortalLogger logger, String templatePath)
      throws UnableToCompleteException {
    URL url = UiBinderGenerator.class.getClassLoader().getResource(templatePath);
    if (null == url) {
      logger.die("Unable to find resource: " + templatePath);
    }
    
    Document doc = null;
    try {
      doc = new W3cDomHelper(logger.getTreeLogger()).documentFor(url);
    } catch (SAXParseException e) {
      logger.die("Error parsing XML (line " + e.getLineNumber() + "): "
          + e.getMessage(), e);
    }
    return doc;
  }
}
