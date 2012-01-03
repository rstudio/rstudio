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

import com.google.gwt.core.ext.BadPropertyValueException;
import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.PropertyOracle;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.resource.Resource;
import com.google.gwt.dev.resource.ResourceOracle;
import com.google.gwt.dev.util.Util;
import com.google.gwt.uibinder.client.UiTemplate;
import com.google.gwt.uibinder.rebind.messages.MessagesWriter;
import com.google.gwt.uibinder.rebind.model.ImplicitClientBundle;

import org.w3c.dom.Document;
import org.xml.sax.SAXParseException;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * Generator for implementations of
 * {@link com.google.gwt.uibinder.client.UiBinder}.
 */
public class UiBinderGenerator extends Generator {

  private static final String BINDER_URI = "urn:ui:com.google.gwt.uibinder";

  private static final String TEMPLATE_SUFFIX = ".ui.xml";

  private static final String XSS_SAFE_CONFIG_PROPERTY = "UiBinder.useSafeHtmlTemplates";
  private static final String LAZY_WIDGET_BUILDERS_PROPERTY = "UiBinder.useLazyWidgetBuilders";
  
  private static boolean gaveSafeHtmlWarning;
  private static boolean gaveLazyBuildersWarning;

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
      return slashify(interfaceType.getQualifiedBinaryName()) + TEMPLATE_SUFFIX;
    } else {
      templateName = annotation.value();
      if (!templateName.endsWith(TEMPLATE_SUFFIX)) {
        logger.die("Template file name must end with " + TEMPLATE_SUFFIX);
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
    return s.replace(".", "/").replace("$", ".");
  }

  private final UiBinderContext uiBinderCtx = new UiBinderContext();

  @Override
  public String generate(TreeLogger logger, GeneratorContext genCtx,
      String fqInterfaceName) throws UnableToCompleteException {
    TypeOracle oracle = genCtx.getTypeOracle();
    ResourceOracle resourceOracle = genCtx.getResourcesOracle();

    JClassType interfaceType;
    try {
      interfaceType = oracle.getType(fqInterfaceName);
    } catch (NotFoundException e) {
      throw new RuntimeException(e);
    }

    DesignTimeUtils designTime;
    if (DesignTimeUtilsImpl.isDesignTime(fqInterfaceName)) {
      designTime = new DesignTimeUtilsImpl();
    } else {
      designTime = DesignTimeUtilsStub.EMPTY;
    }

    String implName = interfaceType.getName().replace('.', '_') + "Impl";
    implName = designTime.getImplName(implName);

    String packageName = interfaceType.getPackage().getName();
    PrintWriterManager writers = new PrintWriterManager(genCtx, logger,
        packageName);
    PrintWriter printWriter = writers.tryToMakePrintWriterFor(implName);

    if (printWriter != null) {
      generateOnce(interfaceType, implName, printWriter, logger, oracle,
          resourceOracle, genCtx.getPropertyOracle(), writers, designTime);
    }
    return packageName + "." + implName;
  }

  private Boolean extractConfigProperty(MortalLogger logger,
      PropertyOracle propertyOracle, String configProperty, boolean defaultValue) {
    List<String> values;
    try {
      values = propertyOracle.getConfigurationProperty(configProperty).getValues();
    } catch (BadPropertyValueException e) {
      logger.warn("No value found for configuration property %s.", configProperty);
      return defaultValue;
    }

    String value = values.get(0);
    if (!value.equals(Boolean.FALSE.toString()) && !value.equals(Boolean.TRUE.toString())) {
      logger.warn("Unparseable value \"%s\" found for configuration property %s", value,
          configProperty);
      return defaultValue;
    }

    return Boolean.valueOf(value);
  }

  private void generateOnce(JClassType interfaceType, String implName,
      PrintWriter binderPrintWriter, TreeLogger treeLogger, TypeOracle oracle,
      ResourceOracle resourceOracle, PropertyOracle propertyOracle,
      PrintWriterManager writerManager,  DesignTimeUtils designTime)
  throws UnableToCompleteException {

    MortalLogger logger = new MortalLogger(treeLogger);
    String templatePath = deduceTemplateFile(logger, interfaceType);
    MessagesWriter messages = new MessagesWriter(oracle, BINDER_URI, logger,
        templatePath, interfaceType.getPackage().getName(), implName);

    boolean useLazyWidgetBuilders =
        useLazyWidgetBuilders(logger, propertyOracle) && !designTime.isDesignTime();
    FieldManager fieldManager = new FieldManager(oracle, logger, useLazyWidgetBuilders);

    UiBinderWriter uiBinderWriter = new UiBinderWriter(interfaceType, implName,
        templatePath, oracle, logger, fieldManager, messages, designTime, uiBinderCtx,
        useSafeHtmlTemplates(logger, propertyOracle), useLazyWidgetBuilders, BINDER_URI);

    Document doc = getW3cDoc(logger, designTime, resourceOracle, templatePath);
    designTime.rememberPathForElements(doc);

    uiBinderWriter.parseDocument(doc, binderPrintWriter);

    if (messages.hasMessages()) {
      messages.write(writerManager.makePrintWriterFor(messages.getMessagesClassName()));
    }

    ImplicitClientBundle bundleClass = uiBinderWriter.getBundleClass();
    new BundleWriter(bundleClass, writerManager, oracle, logger).write();

    writerManager.commit();
  }

  private Document getW3cDoc(MortalLogger logger, DesignTimeUtils designTime,
      ResourceOracle resourceOracle, String templatePath)
      throws UnableToCompleteException {

    Resource resource = resourceOracle.getResourceMap().get(templatePath);
    if (null == resource) {
      logger.die("Unable to find resource: " + templatePath);
    }

    Document doc = null;
    try {
      String content = designTime.getTemplateContent(templatePath);
      if (content == null) {
        content = Util.readStreamAsString(resource.openContents());
      }
      doc = new W3cDomHelper(logger.getTreeLogger(), resourceOracle).documentFor(
          content, resource.getPath());
    } catch (IOException iex) {
      logger.die("Error opening resource:" + resource.getLocation(), iex);
    } catch (SAXParseException e) {
      logger.die(
          "Error parsing XML (line " + e.getLineNumber() + "): "
              + e.getMessage(), e);
    }
    return doc;
  }

  private Boolean useLazyWidgetBuilders(MortalLogger logger, PropertyOracle propertyOracle) {
    Boolean rtn = extractConfigProperty(logger, propertyOracle, LAZY_WIDGET_BUILDERS_PROPERTY, true);
    if (!gaveLazyBuildersWarning && !rtn) {
      logger.warn("Configuration property %s is false. Deprecated code generation is in play. " +
                  "This property will soon become a no-op.",
                  LAZY_WIDGET_BUILDERS_PROPERTY);
      gaveLazyBuildersWarning = true;
    }
    return rtn;
  }

  private Boolean useSafeHtmlTemplates(MortalLogger logger, PropertyOracle propertyOracle) {
    Boolean rtn = extractConfigProperty(
        logger, propertyOracle, XSS_SAFE_CONFIG_PROPERTY, true);

    if (!gaveSafeHtmlWarning && !rtn) {
      logger.warn("Configuration property %s is false! UiBinder SafeHtml integration is off, "
          + "leaving your users more vulnerable to cross-site scripting attacks. This property " +
          "will soon become a no-op, and SafeHtml integration will always be on.",
          XSS_SAFE_CONFIG_PROPERTY);
      gaveSafeHtmlWarning = true;
    }
    return rtn;
  }
}
