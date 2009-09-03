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
import com.google.gwt.uibinder.rebind.model.ImplicitBundle;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Enumeration;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Generator for implementations of
 * {@link com.google.gwt.uibinder.client.UiBinder}.
 */
public class UiBinderGenerator extends Generator {

  private static final String TEMPLATE_SUFFIX = ".ui.xml";

  private static void commit(TreeLogger logger, GeneratorContext genCtx,
      String packageName, PrintWriter pw, UiBinderWriter uiWriter) {
    uiWriter.write(pw);
    genCtx.commit(logger, pw);

    MessagesWriter messages = uiWriter.getMessages();
    if (messages.hasMessages()) {
      PrintWriter messagesPrintWriter = genCtx.tryCreate(logger, packageName,
          messages.getMessagesClassName());
      if (messagesPrintWriter == null) {
        throw new RuntimeException("Tried to gen messages twice.");
      }

      messages.write(messagesPrintWriter);
      genCtx.commit(logger, messagesPrintWriter);
    }

    ImplicitBundle bundleClass = uiWriter.getBundleClass();
    PrintWriter bundlePrintWriter = genCtx.tryCreate(logger, packageName,
        bundleClass.getClassName());
    if (bundlePrintWriter == null) {
      throw new RuntimeException("Tried to gen bundle twice.");
    }

    new BundleWriter(bundleClass, bundlePrintWriter, genCtx.getTypeOracle()).write();
    genCtx.commit(logger, bundlePrintWriter);
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

    String implName = interfaceType.getName();
    implName = implName.replace('.', '_') + "Impl";

    String packageName = interfaceType.getPackage().getName();
    PrintWriter printWriter = genCtx.tryCreate(logger, packageName, implName);

    if (printWriter != null) {
      String templateName = deduceTemplateFile(logger, interfaceType);

      Document document;
      try {
        document = parseXmlResource(logger, templateName);
      } catch (SAXParseException e) {
        logger.log(TreeLogger.ERROR, "Error parsing XML (line "
            + e.getLineNumber() + "): " + e.getMessage(), e);
        throw new UnableToCompleteException();
      }

      UiBinderWriter uiBinderWriter = new UiBinderWriter(interfaceType,
          implName, templateName, oracle, logger);
      uiBinderWriter.parseDocument(document);
      commit(logger, genCtx, packageName, printWriter, uiBinderWriter);
    }
    return packageName + "." + implName;
  }

  /**
   * Given a UiBinder interface, return the path to its ui.xml file, suitable
   * for any classloader to find it as a resource.
   */
  private String deduceTemplateFile(TreeLogger logger, JClassType interfaceType)
      throws UnableToCompleteException {
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
        logger.log(TreeLogger.ERROR, "Template file name must end with "
            + TEMPLATE_SUFFIX);
        throw new UnableToCompleteException();
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

  private Document parseXmlResource(TreeLogger logger, final String resourcePath)
      throws SAXParseException, UnableToCompleteException {
    // Get the document builder. We need namespaces, and automatic expanding
    // of entity references (the latter of which makes life somewhat easier
    // for XMLElement).
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    factory.setExpandEntityReferences(true);
    DocumentBuilder builder;
    try {
      builder = factory.newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      throw new RuntimeException(e);
    }

    try {
      ClassLoader classLoader = UiBinderGenerator.class.getClassLoader();
      Enumeration<URL> urls = classLoader.getResources(resourcePath);
      if (!urls.hasMoreElements()) {
        logger.log(TreeLogger.ERROR, "Unable to find resource: " + resourcePath);
        throw new UnableToCompleteException();
      }
      URL url = urls.nextElement();

      InputStream stream = url.openStream();
      InputSource input = new InputSource(stream);
      input.setSystemId(url.toExternalForm());

      builder.setEntityResolver(new GwtResourceEntityResolver());

      return builder.parse(input);
    } catch (SAXParseException e) {
      // Let SAXParseExceptions through.
      throw e;
    } catch (SAXException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private String slashify(String s) {
    return s.replace(".", "/");
  }
}
