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

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JPackage;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.JTypeParameter;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.TagName;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.event.dom.client.DomEvent.Type;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.uibinder.attributeparsers.AttributeParsers;
import com.google.gwt.uibinder.client.LazyDomElement;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.uibinder.client.UiRenderer;
import com.google.gwt.uibinder.client.impl.AbstractUiRenderer;
import com.google.gwt.uibinder.elementparsers.AttributeMessageParser;
import com.google.gwt.uibinder.elementparsers.BeanParser;
import com.google.gwt.uibinder.elementparsers.ElementParser;
import com.google.gwt.uibinder.elementparsers.IsEmptyParser;
import com.google.gwt.uibinder.elementparsers.UiChildParser;
import com.google.gwt.uibinder.rebind.messages.MessagesWriter;
import com.google.gwt.uibinder.rebind.model.HtmlTemplateMethodWriter;
import com.google.gwt.uibinder.rebind.model.HtmlTemplatesWriter;
import com.google.gwt.uibinder.rebind.model.ImplicitClientBundle;
import com.google.gwt.uibinder.rebind.model.ImplicitCssResource;
import com.google.gwt.uibinder.rebind.model.OwnerClass;
import com.google.gwt.uibinder.rebind.model.OwnerField;
import com.google.gwt.user.client.ui.IsRenderable;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.RenderableStamper;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.beans.Introspector;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Writer for UiBinder generated classes.
 */
public class UiBinderWriter implements Statements {

  static final String RENDER_PARAM_HOLDER_PREFIX = "_renderer_param_holder_";

  private static final String SAFE_VAR_PREFIX =
    "somethingUnlikelyToCollideWithParamNamesWefio";

  private static final String UI_RENDERER_DISPATCHER_PREFIX = "UiRendererDispatcherFor";

  private static final String PACKAGE_URI_SCHEME = "urn:import:";

  // TODO(rjrjr) Another place that we need a general anonymous field
  // mechanism
  private static final String CLIENT_BUNDLE_FIELD =
      "clientBundleFieldNameUnlikelyToCollideWithUserSpecifiedFieldOkay";

  public static String asCommaSeparatedList(String... args) {
    StringBuilder b = new StringBuilder();
    for (String arg : args) {
      if (b.length() > 0) {
        b.append(", ");
      }
      b.append(arg);
    }

    return b.toString();
  }

  /**
   * Escape text that will be part of a string literal to be interpreted at
   * runtime as an HTML attribute value.
   */
  public static String escapeAttributeText(String text) {
    text = escapeText(text, false);

    /*
     * Escape single-quotes to make them safe to be interpreted at runtime as an
     * HTML attribute value (for which we by convention use single quotes).
     */
    text = text.replaceAll("'", "&#39;");
    return text;
  }

  /**
   * Escape text that will be part of a string literal to be interpreted at
   * runtime as HTML, optionally preserving whitespace.
   */
  public static String escapeText(String text, boolean preserveWhitespace) {
    // Replace reserved XML characters with entities. Note that we *don't*
    // replace single- or double-quotes here, because they're safe in text
    // nodes.
    text = text.replaceAll("&", "&amp;");
    text = text.replaceAll("<", "&lt;");
    text = text.replaceAll(">", "&gt;");

    if (!preserveWhitespace) {
      text = text.replaceAll("\\s+", " ");
    }

    return escapeTextForJavaStringLiteral(text);
  }

  /**
   * Escape characters that would mess up interpretation of this string as a
   * string literal in generated code (that is, protect \, \n and " ).
   */
  public static String escapeTextForJavaStringLiteral(String text) {
    text = text.replace("\\", "\\\\");
    text = text.replace("\"", "\\\"");
    text = text.replace("\n", "\\n");

    return text;
  }

  /**
   * Returns a list of the given type and all its superclasses and implemented
   * interfaces in a breadth-first traversal.
   * 
   * @param type the base type
   * @return a breadth-first collection of its type hierarchy
   */
  static Iterable<JClassType> getClassHierarchyBreadthFirst(JClassType type) {
    LinkedList<JClassType> list = new LinkedList<JClassType>();
    LinkedList<JClassType> q = new LinkedList<JClassType>();

    q.add(type);
    while (!q.isEmpty()) {
      // Pop the front of the queue and add it to the result list.
      JClassType curType = q.removeFirst();
      list.add(curType);

      // Add implemented interfaces to the back of the queue (breadth first,
      // remember?)
      for (JClassType intf : curType.getImplementedInterfaces()) {
        q.add(intf);
      }
      // Add then add superclasses
      JClassType superClass = curType.getSuperclass();
      if (superClass != null) {
        q.add(superClass);
      }
    }

    return list;
  }

  private static String capitalizePropName(String propName) {
    return propName.substring(0, 1).toUpperCase() + propName.substring(1);
  }

  /**
   * Searches for methods named onBrowserEvent in a {@code type}.
   */
  private static JMethod[] findEventMethods(JClassType type) {
    List<JMethod> methods = new ArrayList<JMethod>(Arrays.asList(type.getInheritableMethods()));

    for (Iterator<JMethod> iterator = methods.iterator(); iterator.hasNext();) {
      JMethod jMethod = iterator.next();
      if (!jMethod.getName().equals("onBrowserEvent")) {
        iterator.remove();
      }
    }

    return methods.toArray(new JMethod[methods.size()]);
  }

  /**
   * Scan the base class for the getter methods. Assumes getters begin with
   * "get". See {@link #validateRendererGetters(JClassType)} for a method that
   * guarantees this method will succeed.
   */
  private static List<JMethod> findGetterNames(JClassType owner) {
    List<JMethod> ret = new ArrayList<JMethod>();
    for (JMethod jMethod : owner.getInheritableMethods()) {
      String getterName = jMethod.getName();
      if (getterName.startsWith("get")) {
        ret.add(jMethod);
      }
    }
    return ret;
  }

  /**
   * Scans a class for a method named "render". Returns its parameters except
   * for the first one. See {@link #validateRenderParameters(JClassType)} for a
   * method that guarantees this method will succeed.
   */
  private static JParameter[] findRenderParameters(JClassType owner) {
    JMethod[] methods = owner.getInheritableMethods();
    JMethod renderMethod = null;

    for (JMethod jMethod : methods) {
      if (jMethod.getName().equals("render")) {
        renderMethod = jMethod;
      }
    }

    JParameter[] parameters = renderMethod.getParameters();
    return Arrays.copyOfRange(parameters, 1, parameters.length);
  }

  /**
   * Finds methods annotated with {@code @UiHandler} in a {@code type}.
   */
  private static JMethod[] findUiHandlerMethods(JClassType type) {
    ArrayList<JMethod> result = new ArrayList<JMethod>();
    JMethod[] allMethods = type.getInheritableMethods();

    for (JMethod jMethod : allMethods) {
      if (jMethod.getAnnotation(UiHandler.class) != null) {
        result.add(jMethod);
      }
    }

    return result.toArray(new JMethod[result.size()]);
  }

  private static String formatMethodError(JMethod eventMethod) {
    return "\"" + eventMethod.getReadableDeclaration(true, true, true, true, true) + "\""
        + " of " + eventMethod.getEnclosingType().getQualifiedSourceName();
  }

  /**
   * Determine the field name a getter is trying to retrieve. Assumes getters
   * begin with "get".
   */
  private static String getterToFieldName(String name) {
    String fieldName = name.substring(3);
    return Introspector.decapitalize(fieldName);
  }

  private static String renderMethodParameters(JParameter[] renderParameters) {
    StringBuilder builder = new StringBuilder();

    for (int i = 0; i < renderParameters.length; i++) {
      JParameter parameter = renderParameters[i];
      builder.append("final ");
      builder.append(parameter.getType().getQualifiedSourceName());
      builder.append(" ");
      builder.append(parameter.getName());
      if (i < renderParameters.length - 1) {
        builder.append(", ");
      }
    }

    return builder.toString();
  }

  private final MortalLogger logger;

  /**
   * Class names of parsers for various ui types, keyed by the classname of the
   * UI class they can build.
   */
  private final Map<String, String> elementParsers = new HashMap<String, String>();

  private final List<String> initStatements = new ArrayList<String>();
  private final List<String> statements = new ArrayList<String>();
  private final HandlerEvaluator handlerEvaluator;
  private final MessagesWriter messages;
  private final DesignTimeUtils designTime;
  private final Tokenator tokenator = new Tokenator();

  private final String templatePath;
  private final TypeOracle oracle;
  /**
   * The type we have been asked to generated, e.g. MyUiBinder
   */
  private final JClassType baseClass;

  /**
   * The name of the class we're creating, e.g. MyUiBinderImpl
   */
  private final String implClassName;

  private final JClassType uiOwnerType;

  private final JClassType uiRootType;

  private final JClassType isRenderableClassType;

  private final JClassType lazyDomElementClass;

  private final OwnerClass ownerClass;

  private final FieldManager fieldManager;

  private final HtmlTemplatesWriter htmlTemplates;

  private final ImplicitClientBundle bundleClass;

  private final boolean useLazyWidgetBuilders;

  private final boolean useSafeHtmlTemplates;

  private int domId = 0;

  private int fieldIndex;

  private String gwtPrefix;

  private int renderableStamper = 0;

  private String rendered;
  /**
   * Stack of element variable names that have been attached.
   */
  private final LinkedList<String> attachSectionElements = new LinkedList<String>();
  /**
   * Maps from field element name to the temporary attach record variable name.
   */
  private final Map<String, String> attachedVars = new HashMap<String, String>();

  private int nextAttachVar = 0;
  /**
   * Stack of statements to be executed after we detach the current attach
   * section.
   */
  private final LinkedList<List<String>> detachStatementsStack = new LinkedList<List<String>>();
  private final AttributeParsers attributeParsers;

  private final UiBinderContext uiBinderCtx;

  private final String binderUri;
  private final boolean isRenderer;

  public UiBinderWriter(JClassType baseClass, String implClassName, String templatePath,
      TypeOracle oracle, MortalLogger logger, FieldManager fieldManager,
      MessagesWriter messagesWriter, DesignTimeUtils designTime, UiBinderContext uiBinderCtx,
      boolean useSafeHtmlTemplates, boolean useLazyWidgetBuilders, String binderUri)
      throws UnableToCompleteException {
    this.baseClass = baseClass;
    this.implClassName = implClassName;
    this.oracle = oracle;
    this.logger = logger;
    this.templatePath = templatePath;
    this.fieldManager = fieldManager;
    this.messages = messagesWriter;
    this.designTime = designTime;
    this.uiBinderCtx = uiBinderCtx;
    this.useSafeHtmlTemplates = useSafeHtmlTemplates;
    this.useLazyWidgetBuilders = useLazyWidgetBuilders;
    this.binderUri = binderUri;

    this.htmlTemplates = new HtmlTemplatesWriter(fieldManager, logger);

    // Check for possible misuse 'GWT.create(UiBinder.class)'
    JClassType uibinderItself = oracle.findType(UiBinder.class.getCanonicalName());
    if (uibinderItself.equals(baseClass)) {
      die("You must use a subtype of UiBinder in GWT.create(). E.g.,\n"
          + "  interface Binder extends UiBinder<Widget, MyClass> {}\n"
          + "  GWT.create(Binder.class);");
    }

    JClassType[] uiBinderTypes = baseClass.getImplementedInterfaces();
    if (uiBinderTypes.length == 0) {
      throw new RuntimeException("No implemented interfaces for " + baseClass.getName());
    }
    JClassType uiBinderType = uiBinderTypes[0];

    JClassType[] typeArgs = uiBinderType.isParameterized() == null
        ? new JClassType[0] : uiBinderType.isParameterized().getTypeArgs();

    String binderType = uiBinderType.getName();

    JClassType uiRendererClass = getOracle().findType(UiRenderer.class.getName());
    if (uiBinderType.isAssignableTo(uibinderItself)) {
      if (typeArgs.length < 2) {
        throw new RuntimeException("Root and owner type parameters are required for type %s"
            + binderType);
      }
      uiRootType = typeArgs[0];
      uiOwnerType = typeArgs[1];
      isRenderer = false;
    } else if (uiBinderType.isAssignableTo(uiRendererClass)) {
      if (typeArgs.length >= 1) {
        throw new RuntimeException("UiRenderer is not a parameterizable type in " + binderType);
      }
      if (!useSafeHtmlTemplates) {
        die("Configuration property UiBinder.useSafeHtmlTemplates\n"
            + "  must be set to true to generate a UiRenderer");
      }
      if (!useLazyWidgetBuilders) {
        die("Configuration property UiBinder.useLazyWidgetBuilders\n"
            + "  must be set to true to generate a UiRenderer");
      }

      // UiRenderers do not need owners but UiBinder generation needs some type here
      uiOwnerType = uiBinderType;
      uiRootType = null;
      isRenderer = true;
    } else {
      die(baseClass.getName() + " must implement UiBinder or UiRenderer");
      // This is unreachable in practice, but silences not initialized errors
      throw new UnableToCompleteException();
    }

    isRenderableClassType = oracle.findType(IsRenderable.class.getCanonicalName());
    lazyDomElementClass = oracle.findType(LazyDomElement.class.getCanonicalName());

    ownerClass = new OwnerClass(uiOwnerType, logger, uiBinderCtx);
    bundleClass =
        new ImplicitClientBundle(baseClass.getPackage().getName(), this.implClassName,
            CLIENT_BUNDLE_FIELD, logger);
    handlerEvaluator = new HandlerEvaluator(ownerClass, logger, oracle, useLazyWidgetBuilders);

    attributeParsers = new AttributeParsers(oracle, fieldManager, logger);
  }

  /**
   * Add a statement to be executed right after the current attached element is
   * detached. This is useful for doing things that might be expensive while the
   * element is attached to the DOM.
   * 
   * @param format
   * @param args
   * @see #beginAttachedSection(String)
   */
  public void addDetachStatement(String format, Object... args) {
    detachStatementsStack.getFirst().add(String.format(format, args));
  }

  /**
   * Add a statement to be run after everything has been instantiated, in the
   * style of {@link String#format}.
   */
  public void addInitStatement(String format, Object... params) {
    initStatements.add(formatCode(format, params));
  }

  /**
   * Adds a statement to the block run after fields are declared, in the style
   * of {@link String#format}.
   */
  public void addStatement(String format, Object... args) {
    String code = formatCode(format, args);

    if (useLazyWidgetBuilders) {
      /**
       * I'm intentionally over-simplifying this and assuming that the input
       * comes always in the format: field.somestatement(); Thus, field can be
       * extracted easily and the element parsers don't need to be changed all
       * at once.
       */
      int idx = code.indexOf(".");
      String fieldName = code.substring(0, idx);
      fieldManager.require(fieldName).addStatement(format, args);
    } else {
      statements.add(code);
    }
  }

  /**
   * Begin a section where a new attachable element is being parsed--that is,
   * one that will be constructed as a big innerHTML string, and then briefly
   * attached to the dom to allow fields accessing its to be filled (at the
   * moment, HasHTMLParser, HTMLPanelParser, and DomElementParser.).
   * <p>
   * Succeeding calls made to {@link #ensureAttached} and
   * {@link #ensureCurrentFieldAttached} must refer to children of this element,
   * until {@link #endAttachedSection} is called.
   * 
   * @param element Java expression for the generated code that will return the
   *          dom element to be attached.
   */
  public void beginAttachedSection(String element) {
    attachSectionElements.addFirst(element);
    detachStatementsStack.addFirst(new ArrayList<String>());
  }

  /**
   * Declare a field that will hold an Element instance. Returns a token that
   * the caller must set as the id attribute of that element in whatever
   * innerHTML expression will reproduce it at runtime.
   * <P>
   * In the generated code, this token will be replaced by an expression to
   * generate a unique dom id at runtime. Further code will be generated to be
   * run after widgets are instantiated, to use that dom id in a getElementById
   * call and assign the Element instance to its field.
   * 
   * @param fieldName The name of the field being declared
   * @param ancestorField The name of fieldName parent
   */
  public String declareDomField(XMLElement source, String fieldName, String ancestorField)
      throws UnableToCompleteException {
    ensureAttached();
    String name = declareDomIdHolder(fieldName);

    if (useLazyWidgetBuilders) {
      // Create and initialize the dom field with LazyDomElement.
      FieldWriter field = fieldManager.require(fieldName);

      /**
       * But if the owner field is an instance of LazyDomElement then the code
       * can be optimized, no cast is needed and the getter doesn't need to be
       * called in its ancestral.
       */
      if (isOwnerFieldLazyDomElement(fieldName)) {
        field.setInitializer(formatCode("new %s(%s)", field.getQualifiedSourceName(),
            fieldManager.convertFieldToGetter(name)));
      } else {

        field.setInitializer(formatCode("new %s(%s).get().cast()",
            LazyDomElement.class.getCanonicalName(), fieldManager.convertFieldToGetter(name)));

        // The dom must be created by its ancestor.
        fieldManager.require(ancestorField).addAttachStatement(
            fieldManager.convertFieldToGetter(fieldName) + ";");
      }
    } else {
      setFieldInitializer(fieldName, "null");
      addInitStatement("%s = com.google.gwt.dom.client.Document.get().getElementById(%s).cast();",
          fieldName, name);
      addInitStatement("%s.removeAttribute(\"id\");", fieldName);
    }

    return tokenForStringExpression(source, fieldManager.convertFieldToGetter(name));
  }

  /**
   * Declare a variable that will be filled at runtime with a unique id, safe
   * for use as a dom element's id attribute. For {@code UiRenderer} based code,
   * elements corresponding to a ui:field, need and id initialized to a value
   * that depends on the {@code fieldName}. For all other cases let
   * {@code fieldName} be {@code null}.
   * 
   * @param fieldName name of the field corresponding to this variable.
   * @return that variable's name.
   */
  public String declareDomIdHolder(String fieldName) throws UnableToCompleteException {
    String domHolderName = "domId" + domId++;
    FieldWriter domField =
        fieldManager.registerField(FieldWriterType.DOM_ID_HOLDER,
            oracle.findType(String.class.getName()), domHolderName);
    if (isRenderer && fieldName != null) {
      domField.setInitializer("buildInnerId(\"" + fieldName + "\", uiId)");
    } else {
      domField.setInitializer("com.google.gwt.dom.client.Document.get().createUniqueId()");
    }

    return domHolderName;
  }

  /**
   * If this element has a gwt:field attribute, create a field for it of the
   * appropriate type, and return the field name. If no gwt:field attribute is
   * found, do nothing and return null
   * 
   * @return The new field name, or null if no field is created
   */
  public String declareFieldIfNeeded(XMLElement elem) throws UnableToCompleteException {
    String fieldName = getFieldName(elem);
    if (fieldName != null) {

      /**
       * We can switch types if useLazyWidgetBuilders is enabled and the
       * respective owner field is a LazyDomElement.
       */
      if (useLazyWidgetBuilders && isOwnerFieldLazyDomElement(fieldName)) {
        fieldManager.registerFieldForLazyDomElement(findFieldType(elem),
            ownerClass.getUiField(fieldName));
      } else {
        fieldManager.registerField(findFieldType(elem), fieldName);
      }
    }
    return fieldName;
  }

  /**
   * Declare a {@link RenderableStamper} instance that will be filled at runtime
   * with a unique token. This instance can then be used to stamp a single
   * {@link IsRenderable}.
   * 
   * @return that variable's name.
   */
  public String declareRenderableStamper() throws UnableToCompleteException {
    String renderableStamperName = "renderableStamper" + renderableStamper++;
    FieldWriter domField =
        fieldManager.registerField(FieldWriterType.RENDERABLE_STAMPER,
            oracle.findType(RenderableStamper.class.getName()), renderableStamperName);
    domField.setInitializer(formatCode(
        "new %s(com.google.gwt.dom.client.Document.get().createUniqueId())",
        RenderableStamper.class.getName()));

    return renderableStamperName;
  }

  /**
   * Writes a new SafeHtml template to the generated BinderImpl.
   * 
   * @return The invocation of the SafeHtml template function with the arguments
   *         filled in
   */
  public String declareTemplateCall(String html, String fieldName) throws IllegalArgumentException {
    if (!useSafeHtmlTemplates) {
      return '"' + html + '"';
    }
    FieldWriter w = fieldManager.lookup(fieldName);
    HtmlTemplateMethodWriter templateMethod = htmlTemplates.addSafeHtmlTemplate(html, tokenator);
    if (useLazyWidgetBuilders) {
      w.setHtml(templateMethod.getIndirectTemplateCall());
    } else {
      w.setHtml(templateMethod.getDirectTemplateCall());
    }
    return w.getHtml();
  }

  /**
   * Given a string containing tokens returned by
   * {@link #tokenForStringExpression}, {@link #tokenForSafeHtmlExpression} or
   * {@link #declareDomField}, return a string with those tokens replaced by the
   * appropriate expressions. (It is not normally necessary for an
   * {@link XMLElement.Interpreter} or {@link ElementParser} to make this call,
   * as the tokens are typically replaced by the TemplateWriter itself.)
   */
  public String detokenate(String betokened) {
    return tokenator.detokenate(betokened);
  }

  /**
   * Post an error message and halt processing. This method always throws an
   * {@link UnableToCompleteException}
   */
  public void die(String message) throws UnableToCompleteException {
    logger.die(message);
  }

  /**
   * Post an error message and halt processing. This method always throws an
   * {@link UnableToCompleteException}
   */
  public void die(String message, Object... params) throws UnableToCompleteException {
    logger.die(message, params);
  }

  /**
   * Post an error message about a specific XMLElement and halt processing. This
   * method always throws an {@link UnableToCompleteException}
   */
  public void die(XMLElement context, String message, Object... params)
      throws UnableToCompleteException {
    logger.die(context, message, params);
  }

  /**
   * End the current attachable section. This will detach the element if it was
   * ever attached and execute any detach statements.
   * 
   * @see #beginAttachedSection(String)
   */
  public void endAttachedSection() {
    String elementVar = attachSectionElements.removeFirst();
    List<String> detachStatements = detachStatementsStack.removeFirst();
    if (attachedVars.containsKey(elementVar)) {
      String attachedVar = attachedVars.remove(elementVar);
      addInitStatement("%s.detach();", attachedVar);
      for (String statement : detachStatements) {
        addInitStatement(statement);
      }
    }
  }

  /**
   * Ensure that the specified element is attached to the DOM.
   * 
   * @see #beginAttachedSection(String)
   */
  public void ensureAttached() {
    String attachSectionElement = attachSectionElements.getFirst();
    if (!attachedVars.containsKey(attachSectionElement)) {
      String attachedVar = "attachRecord" + nextAttachVar;
      addInitStatement("UiBinderUtil.TempAttachment %s = UiBinderUtil.attachToDom(%s);",
          attachedVar, attachSectionElement);
      attachedVars.put(attachSectionElement, attachedVar);
      nextAttachVar++;
    }
  }

  /**
   * Ensure that the specified field is attached to the DOM. The field must hold
   * an object that responds to Element getElement(). Convenience wrapper for
   * {@link #ensureAttached}<code>(field + ".getElement()")</code>.
   * 
   * @see #beginAttachedSection(String)
   */
  public void ensureCurrentFieldAttached() {
    ensureAttached();
  }

  /**
   * Finds the JClassType that corresponds to this XMLElement, which must be a
   * Widget or an Element.
   * 
   * @throws UnableToCompleteException If no such widget class exists
   * @throws RuntimeException if asked to handle a non-widget, non-DOM element
   */
  public JClassType findFieldType(XMLElement elem) throws UnableToCompleteException {
    String tagName = elem.getLocalName();

    if (!isImportedElement(elem)) {
      return findDomElementTypeForTag(tagName);
    }

    String ns = elem.getNamespaceUri();
    String packageName = ns;
    String className = tagName;

    while (true) {
      JPackage pkg = parseNamespacePackage(packageName);
      if (pkg == null) {
        throw new RuntimeException("No such package: " + packageName);
      }

      JClassType rtn = pkg.findType(className);
      if (rtn != null) {
        return rtn;
      }

      // Try again: shift one element of the class name onto the package name.
      // If the class name has only one element left, fail.
      int index = className.indexOf(".");
      if (index == -1) {
        die(elem, "No class matching \"%s\" in %s", tagName, ns);
      }
      packageName = packageName + "." + className.substring(0, index);
      className = className.substring(index + 1);
    }
  }

  /**
   * Generates the code to set a property value (assumes that 'value' is a valid
   * Java expression).
   */
  public void genPropertySet(String fieldName, String propName, String value) {
    addStatement("%1$s.set%2$s(%3$s);", fieldName, capitalizePropName(propName), value);
  }

  /**
   * Generates the code to set a string property.
   */
  public void genStringPropertySet(String fieldName, String propName, String value) {
    genPropertySet(fieldName, propName, "\"" + value + "\"");
  }

  /**
   * The type we have been asked to generated, e.g. MyUiBinder
   */
  public JClassType getBaseClass() {
    return baseClass;
  }

  public ImplicitClientBundle getBundleClass() {
    return bundleClass;
  }

  /**
   * Returns the {@link DesignTimeUtils}, not <code>null</code>.
   */
  public DesignTimeUtils getDesignTime() {
    return designTime;
  }

  public FieldManager getFieldManager() {
    return fieldManager;
  }

  /**
   * Returns the logger, at least until we get get it handed off to parsers via
   * constructor args.
   */
  public MortalLogger getLogger() {
    return logger;
  }

  /**
   * Get the {@link MessagesWriter} for this UI, generating it if necessary.
   */
  public MessagesWriter getMessages() {
    return messages;
  }

  /**
   * Gets the type oracle.
   */
  public TypeOracle getOracle() {
    return oracle;
  }

  public OwnerClass getOwnerClass() {
    return ownerClass;
  }

  public String getUiFieldAttributeName() {
    return gwtPrefix + ":field";
  }

  public boolean isBinderElement(XMLElement elem) {
    String uri = elem.getNamespaceUri();
    return uri != null && binderUri.equals(uri);
  }

  public boolean isElementAssignableTo(XMLElement elem, Class<?> possibleSuperclass)
      throws UnableToCompleteException {
    JClassType classType = oracle.findType(possibleSuperclass.getCanonicalName());
    return isElementAssignableTo(elem, classType);
  }

  public boolean isElementAssignableTo(XMLElement elem, JClassType possibleSupertype)
      throws UnableToCompleteException {
    /*
     * Things like <W extends IsWidget & IsPlaid>
     */
    JTypeParameter typeParameter = possibleSupertype.isTypeParameter();
    if (typeParameter != null) {
      JClassType[] bounds = typeParameter.getBounds();
      for (JClassType bound : bounds) {
        if (!isElementAssignableTo(elem, bound)) {
          return false;
        }
      }
      return true;
    }

    /*
     * Binder fields are always declared raw, so we're cheating if the user is
     * playing with parameterized types. We're happy enough if the raw types
     * match, and rely on them to make sure the specific types really do work.
     */
    JParameterizedType parameterized = possibleSupertype.isParameterized();
    if (parameterized != null) {
      return isElementAssignableTo(elem, parameterized.getRawType());
    }

    JClassType fieldtype = findFieldType(elem);
    if (fieldtype == null) {
      return false;
    }
    return fieldtype.isAssignableTo(possibleSupertype);
  }

  public boolean isImportedElement(XMLElement elem) {
    String uri = elem.getNamespaceUri();
    return uri != null && uri.startsWith(PACKAGE_URI_SCHEME);
  }

  /**
   * Checks whether the given owner field name is a LazyDomElement or not.
   */
  public boolean isOwnerFieldLazyDomElement(String fieldName) {
    OwnerField ownerField = ownerClass.getUiField(fieldName);
    if (ownerField == null) {
      return false;
    }

    return lazyDomElementClass.isAssignableFrom(ownerField.getType().getRawType());
  }

  public boolean isRenderableElement(XMLElement elem) throws UnableToCompleteException {
    return findFieldType(elem).isAssignableTo(isRenderableClassType);
  }

  public boolean isRenderer() {
    return isRenderer;
  }

  public boolean isWidgetElement(XMLElement elem) throws UnableToCompleteException {
    return isElementAssignableTo(elem, IsWidget.class);
  }

  /**
   * Parses the object associated with the specified element, and returns the
   * field writer that will hold it. The element is likely to make recursive
   * calls back to this method to have its children parsed.
   * 
   * @param elem the xml element to be parsed
   * @return the field holder just created
   */
  public FieldWriter parseElementToField(XMLElement elem) throws UnableToCompleteException {
    if (elementParsers.isEmpty()) {
      registerParsers();
    }

    // Get the class associated with this element.
    JClassType type = findFieldType(elem);

    // Declare its field.
    FieldWriter field = declareField(elem, type.getQualifiedSourceName());

    /*
     * Push the field that will hold this widget on top of the parsedFieldStack
     * to ensure that fields registered by its parsers will be noted as
     * dependencies of the new widget. (See registerField.) Also push the
     * element being parsed, so that the fieldManager can hold that info for
     * later error reporting when field reference left hand sides are validated.
     */
    fieldManager.push(elem, field);

    // Give all the parsers a chance to generate their code.
    for (ElementParser parser : getParsersForClass(type)) {
      parser.parse(elem, field.getName(), type, this);
    }
    fieldManager.pop();

    return field;
  }

  /**
   * Gives the writer the initializer to use for this field instead of the
   * default GWT.create call.
   * 
   * @throws IllegalStateException if an initializer has already been set
   */
  public void setFieldInitializer(String fieldName, String factoryMethod) {
    fieldManager.lookup(fieldName).setInitializer(factoryMethod);
  }

  /**
   * Instructs the writer to initialize the field with a specific constructor
   * invocation, instead of the default GWT.create call.
   * 
   * @param fieldName the field to initialize
   * @param type the type of the field
   * @param args arguments to the constructor call
   */
  public void setFieldInitializerAsConstructor(String fieldName, String... args) {
    JClassType assignableType = fieldManager.lookup(fieldName).getAssignableType();
    setFieldInitializer(fieldName, formatCode("new %s(%s)", assignableType.getQualifiedSourceName(),
        asCommaSeparatedList(args)));
  }

  /**
   * Like {@link #tokenForStringExpression}, but used for runtime expressions
   * that we trust to be safe to interpret at runtime as HTML without escaping,
   * like translated messages with simple formatting. Wrapped in a call to
   * {@link com.google.gwt.safehtml.shared.SafeHtmlUtils#fromSafeConstant} to
   * keep the expression from being escaped by the SafeHtml template.
   * 
   * @param expression must resolve to trusted HTML string
   */
  public String tokenForSafeConstant(XMLElement source, String expression) {
    if (!useSafeHtmlTemplates) {
      return tokenForStringExpression(source, expression);
    }

    expression = "SafeHtmlUtils.fromSafeConstant(" + expression + ")";
    htmlTemplates.noteSafeConstant(expression);
    return nextToken(source, expression);
  }

  /**
   * Like {@link #tokenForStringExpression}, but used for runtime
   * {@link com.google.gwt.safehtml.shared.SafeHtml SafeHtml} instances.
   * 
   * @param expression must resolve to SafeHtml object
   */
  public String tokenForSafeHtmlExpression(XMLElement source, String expression) {
    if (!useSafeHtmlTemplates) {
      return tokenForStringExpression(source, expression + ".asString()");
    }

    htmlTemplates.noteSafeConstant(expression);
    return nextToken(source, expression);
  }

  /**
   * Like {@link #tokenForStringExpression}, but used for runtime
   * {@link com.google.gwt.safehtml.shared.SafeUri SafeUri} instances.
   * 
   * @param expression must resolve to SafeUri object
   */
  public String tokenForSafeUriExpression(XMLElement source, String expression) {
    if (!useSafeHtmlTemplates) {
      return tokenForStringExpression(source, expression);
    }

    htmlTemplates.noteUri(expression);
    return nextToken(source, expression);
  }

  /**
   * Returns a string token that can be used in place the given expression
   * inside any string literals. Before the generated code is written, the
   * expression will be stitched back into the generated code in place of the
   * token, surrounded by plus signs. This is useful in strings to be handed to
   * setInnerHTML() and setText() calls, to allow a unique dom id attribute or
   * other runtime expression in the string.
   * 
   * @param expression must resolve to String
   */
  public String tokenForStringExpression(XMLElement source, String expression) {
    return nextToken(source, "\" + " + expression + " + \"");
  }

  public boolean useLazyWidgetBuilders() {
    return useLazyWidgetBuilders;
  }

  /**
   * @return true of SafeHtml integration is in effect
   */
  public boolean useSafeHtmlTemplates() {
    return useSafeHtmlTemplates;
  }

  /**
   * Post a warning message.
   */
  public void warn(String message) {
    logger.warn(message);
  }

  /**
   * Post a warning message.
   */
  public void warn(String message, Object... params) {
    logger.warn(message, params);
  }

  /**
   * Post a warning message.
   */
  public void warn(XMLElement context, String message, Object... params) {
    logger.warn(context, message, params);
  }

  /**
   * Entry point for the code generation logic. It generates the
   * implementation's superstructure, and parses the root widget (leading to all
   * of its children being parsed as well).
   * 
   * @param doc TODO
   */
  void parseDocument(Document doc, PrintWriter printWriter) throws UnableToCompleteException {
    Element documentElement = doc.getDocumentElement();
    gwtPrefix = documentElement.lookupPrefix(binderUri);

    XMLElement elem =
        new XMLElementProviderImpl(attributeParsers, oracle, logger, designTime).get(documentElement);
    this.rendered = tokenator.detokenate(parseDocumentElement(elem));
    printWriter.print(rendered);
  }

  private void addElementParser(String gwtClass, String parser) {
    elementParsers.put(gwtClass, parser);
  }

  private void addWidgetParser(String className) {
    String gwtClass = "com.google.gwt.user.client.ui." + className;
    String parser = "com.google.gwt.uibinder.elementparsers." + className + "Parser";
    addElementParser(gwtClass, parser);
  }

  /**
   * Declares a field of the given type name, returning the name of the declared
   * field. If the element has a field or id attribute, use its value.
   * Otherwise, create and return a new, private field name for it.
   */
  private FieldWriter declareField(XMLElement source, String typeName)
      throws UnableToCompleteException {
    JClassType type = oracle.findType(typeName);
    if (type == null) {
      die(source, "Unknown type %s", typeName);
    }

    String fieldName = getFieldName(source);
    if (fieldName == null) {
      // TODO(rjrjr) could collide with user declared name, as is
      // also a worry in HandlerEvaluator. Need a general scheme for
      // anonymous fields. See the note in HandlerEvaluator and do
      // something like that, but in FieldManager.
      fieldName = "f_" + source.getLocalName() + ++fieldIndex;
    }
    fieldName = normalizeFieldName(fieldName);
    return fieldManager.registerField(type, fieldName);
  }

  private void dieGettingEventTypeName(JMethod jMethod, Exception e)
      throws UnableToCompleteException {
    die("Could not obtain DomEvent.Type object for first parameter of %s (%s)",
        formatMethodError(jMethod), e.getMessage());
  }

  /**
   * Ensures that all of the internal data structures are cleaned up correctly
   * at the end of parsing the document.
   * 
   * @throws IllegalStateException
   */
  private void ensureAttachmentCleanedUp() {
    if (!attachSectionElements.isEmpty()) {
      throw new IllegalStateException("Attachments not cleaned up: " + attachSectionElements);
    }
    if (!detachStatementsStack.isEmpty()) {
      throw new IllegalStateException("Detach not cleaned up: " + detachStatementsStack);
    }
  }

  /**
   * Add call to {@code com.google.gwt.resources.client.CssResource#ensureInjected()}
   * on each CSS resource field.
   */
  private void ensureInjectedCssFields() {
    for (ImplicitCssResource css : bundleClass.getCssMethods()) {
      String fieldName = css.getName();
      FieldWriter cssField = fieldManager.require(fieldName);
      cssField.addStatement("%s.ensureInjected();", fieldName);
    }
  }

  /**
   * Evaluate whether all @UiField attributes are also defined in the template.
   * Dies if not.
   */
  private void evaluateUiFields() throws UnableToCompleteException {
    if (designTime.isDesignTime()) {
      return;
    }
    for (OwnerField ownerField : getOwnerClass().getUiFields()) {
      String fieldName = ownerField.getName();
      FieldWriter fieldWriter = fieldManager.lookup(fieldName);

      if (fieldWriter == null) {
        die("Template %s has no %s attribute for %s.%s#%s", templatePath,
            getUiFieldAttributeName(), uiOwnerType.getPackage().getName(), uiOwnerType.getName(),
            fieldName);
      }
    }
  }

  /**
   * Given a DOM tag name, return the corresponding JSO subclass.
   */
  private JClassType findDomElementTypeForTag(String tag) {
    JClassType elementClass = oracle.findType("com.google.gwt.dom.client.Element");
    JClassType[] types = elementClass.getSubtypes();
    for (JClassType type : types) {
      TagName annotation = type.getAnnotation(TagName.class);
      if (annotation != null) {
        for (String annotationTag : annotation.value()) {
          if (annotationTag.equals(tag)) {
            return type;
          }
        }
      }
    }
    return elementClass;
  }

  /**
   * Calls {@code getType().getName()} on subclasses of {@code DomEvent}.
   */
  private String findEventTypeName(JMethod jMethod)
      throws UnableToCompleteException {
    // Get the event class name (i.e. ClickEvent)
    String eventTypeName = jMethod.getParameterTypes()[0].getQualifiedSourceName();

    Class<?> domType;

    // Get the class instance
    try {
      domType = Class.forName(eventTypeName);
    } catch (ClassNotFoundException e) {
      die("Could not find type %s in %s", eventTypeName, formatMethodError(jMethod));
      return null;
    }

    // Reflectively obtain the type (i.e. ClickEvent.getType())
    try {
      return ((Type<?>) domType.getMethod("getType", (Class[]) null).invoke(null,
          (Object[]) null)).getName();
    } catch (IllegalArgumentException e) {
      dieGettingEventTypeName(jMethod, e);
    } catch (SecurityException e) {
      dieGettingEventTypeName(jMethod, e);
    } catch (IllegalAccessException e) {
      dieGettingEventTypeName(jMethod, e);
    } catch (InvocationTargetException e) {
      dieGettingEventTypeName(jMethod, e);
    } catch (NoSuchMethodException e) {
      dieGettingEventTypeName(jMethod, e);
    }
    // Unreachable, but appeases the compiler
    return null;
  }

  /**
   * Use this method to format code. It forces the use of the en-US locale, so
   * that things like decimal format don't get mangled.
   */
  private String formatCode(String format, Object... params) {
    String r = String.format(Locale.US, format, params);
    return r;
  }

  /**
   * Inspects this element for a gwt:field attribute. If one is found, the
   * attribute is consumed and its value returned.
   * 
   * @return The field name declared by an element, or null if none is declared
   */
  private String getFieldName(XMLElement elem) throws UnableToCompleteException {
    String fieldName = null;
    boolean hasOldSchoolId = false;
    if (elem.hasAttribute("id") && isWidgetElement(elem)) {
      hasOldSchoolId = true;
      // If an id is specified on the element, use that.
      fieldName = elem.consumeRawAttribute("id");
      warn(elem, "Deprecated use of id=\"%1$s\" for field name. "
          + "Please switch to gwt:field=\"%1$s\" instead. " + "This will soon be a compile error!",
          fieldName);
    }
    if (elem.hasAttribute(getUiFieldAttributeName())) {
      if (hasOldSchoolId) {
        die(elem, "Cannot declare both id and field on the same element");
      }
      fieldName = elem.consumeRawAttribute(getUiFieldAttributeName());
    }
    return fieldName;
  }

  private Class<? extends ElementParser> getParserForClass(JClassType uiClass) {
    // Find the associated parser.
    String uiClassName = uiClass.getQualifiedSourceName();
    String parserClassName = elementParsers.get(uiClassName);
    if (parserClassName == null) {
      return null;
    }

    // And instantiate it.
    try {
      return Class.forName(parserClassName).asSubclass(ElementParser.class);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("Unable to instantiate parser", e);
    } catch (ClassCastException e) {
      throw new RuntimeException(parserClassName + " must extend ElementParser");
    }
  }

  /**
   * Find a set of element parsers for the given ui type.
   * 
   * The list of parsers will be returned in order from most- to least-specific.
   */
  private Iterable<ElementParser> getParsersForClass(JClassType type) {
    List<ElementParser> parsers = new ArrayList<ElementParser>();

    /*
     * Let this non-widget parser go first (it finds <m:attribute/> elements).
     * Any other such should land here too.
     * 
     * TODO(rjrjr) Need a scheme to associate these with a namespace uri or
     * something?
     */
    parsers.add(new AttributeMessageParser());
    parsers.add(new UiChildParser(uiBinderCtx));

    for (JClassType curType : getClassHierarchyBreadthFirst(type)) {
      try {
        Class<? extends ElementParser> cls = getParserForClass(curType);
        if (cls != null) {
          ElementParser parser = cls.newInstance();
          parsers.add(parser);
        }
      } catch (InstantiationException e) {
        throw new RuntimeException("Unable to instantiate " + curType.getName(), e);
      } catch (IllegalAccessException e) {
        throw new RuntimeException("Unable to instantiate " + curType.getName(), e);
      }
    }

    parsers.add(new BeanParser(uiBinderCtx));
    parsers.add(new IsEmptyParser());

    return parsers;
  }

  /**
   * Writes a field setter if the field is not provided and the field class is
   * compatible with its respective template field.
   */
  private void maybeWriteFieldSetter(IndentedWriter niceWriter, OwnerField ownerField,
      JClassType templateClass, String templateField) throws UnableToCompleteException {
    JClassType fieldType = ownerField.getType().getRawType();

    if (!ownerField.isProvided()) {
      /*
       * Normally check that the type the template created can be slammed into
       * the @UiField annotated field in the owning class
       */
      if (!templateClass.isAssignableTo(fieldType)) {
        die("In @UiField %s, template field and owner field types don't match: %s is not assignable to %s",
            ownerField.getName(), templateClass.getQualifiedSourceName(),
            fieldType.getQualifiedSourceName());
      }
      /*
       * And initialize the field
       */
      niceWriter.write("owner.%1$s = %2$s;", ownerField.getName(), templateField);
    } else {
      /*
       * But with @UiField(provided=true) the user builds it, so reverse the
       * direction of the assignability check and do no init.
       */
      if (!fieldType.isAssignableTo(templateClass)) {
        die("In UiField(provided = true) %s, template field and field types don't match: "
            + "@UiField(provided=true)%s is not assignable to %s", ownerField.getName(),
            fieldType.getQualifiedSourceName(), templateClass.getQualifiedSourceName());
      }
    }
  }

  private String nextToken(XMLElement source, String expression) {
    String nextToken = tokenator.nextToken(source, expression);
    return nextToken;
  }

  private String normalizeFieldName(String fieldName) {
    // If a field name has a '.' in it, replace it with '$' to make it a legal
    // identifier. This can happen with the field names associated with nested
    // classes.
    return fieldName.replace('.', '$');
  }

  /**
   * Parse the document element and return the source of the Java class that
   * will implement its UiBinder.
   */
  private String parseDocumentElement(XMLElement elem) throws UnableToCompleteException {
    fieldManager.registerFieldOfGeneratedType(oracle.findType(ClientBundle.class.getName()),
        bundleClass.getPackageName(), bundleClass.getClassName(), bundleClass.getFieldName());

    // Allow GWT.create() to init the field, the default behavior

    FieldWriter rootField = new UiBinderParser(this, messages, fieldManager, oracle, bundleClass,
            binderUri, uiBinderCtx).parse(elem);

    fieldManager.validate();

    StringWriter stringWriter = new StringWriter();
    IndentedWriter niceWriter = new IndentedWriter(new PrintWriter(stringWriter));

    if (isRenderer) {
      ensureInjectedCssFields();
      writeRenderer(niceWriter, rootField);
    } else if (useLazyWidgetBuilders) {
      ensureInjectedCssFields();
      writeBinderForRenderableStrategy(niceWriter, rootField);
    } else {
      writeBinder(niceWriter, rootField);
    }
    ensureAttachmentCleanedUp();
    return stringWriter.toString();
  }

  /**
   * Parses a package uri (e.g., package://com.google...).
   * 
   * @throws UnableToCompleteException on bad package name
   */
  private JPackage parseNamespacePackage(String ns) throws UnableToCompleteException {
    if (ns.startsWith(PACKAGE_URI_SCHEME)) {
      String pkgName = ns.substring(PACKAGE_URI_SCHEME.length());

      JPackage pkg = oracle.findPackage(pkgName);
      if (pkg == null) {
        die("Package not found: " + pkgName);
      }

      return pkg;
    }

    return null;
  }

  private void registerParsers() {
    // TODO(rjrjr): Allow third-party parsers to register themselves
    // automagically

    addElementParser("com.google.gwt.dom.client.Element",
        "com.google.gwt.uibinder.elementparsers.DomElementParser");

    // Register widget parsers.
    addWidgetParser("UIObject");
    addWidgetParser("HasText");
    addWidgetParser("HasHTML");
    addWidgetParser("HasTreeItems");
    addWidgetParser("HasWidgets");
    addWidgetParser("HTMLPanel");
    addWidgetParser("AbsolutePanel");
    addWidgetParser("DockPanel");
    addWidgetParser("StackPanel");
    addWidgetParser("DisclosurePanel");
    addWidgetParser("TabPanel");
    addWidgetParser("MenuItem");
    addWidgetParser("MenuBar");
    addWidgetParser("CellPanel");
    addWidgetParser("CustomButton");
    addWidgetParser("DialogBox");
    addWidgetParser("LayoutPanel");
    addWidgetParser("DockLayoutPanel");
    addWidgetParser("StackLayoutPanel");
    addWidgetParser("TabLayoutPanel");
    addWidgetParser("Image");
    addWidgetParser("ListBox");
    addWidgetParser("Grid");
    addWidgetParser("HasAlignment");
    addWidgetParser("DateLabel");
    addWidgetParser("NumberLabel");
    if (useLazyWidgetBuilders) {
      addWidgetParser("LazyPanel");
      addWidgetParser("RenderablePanel");
    }
  }

  /**
   * Validates each {@code eventMethod} (e.g. {@code onBrowserEvent(HandlerType o, NativeEvent e,
   * Element parent, A a, B b, ...)}).
   * <ul>
   * <li> The second parameter type is {@code NativeEvent}
   * <li> The third parameter type is {@code Element}
   * <li> All the handler methods in the type of the first parameter
   *      (any methods annotated with {@code @UiHandler})
   *      have a signature compatible with the {@code eventMethod}
   * </ul>
   */
  private void validateEventMethod(JMethod eventMethod) throws UnableToCompleteException {
    JParameter[] parameters = eventMethod.getParameters();
    if (parameters.length < 3) {
      die("Too few parameters in %s",
          formatMethodError(eventMethod));
    }

    String nativeEventName = NativeEvent.class.getCanonicalName();
    JClassType nativeEventType = oracle.findType(nativeEventName);
    if (!nativeEventType.equals(parameters[1].getType())) {
      die("Second parameter must be of type %s in %s", nativeEventName,
          formatMethodError(eventMethod));
    }

    String elementName = com.google.gwt.dom.client.Element.class.getCanonicalName();
    JClassType elementType = oracle.findType(elementName);
    if (!elementType.equals(parameters[2].getType())) {
      die("Third parameter must be of type %s in %s", elementName,
          formatMethodError(eventMethod));
    }

    if (parameters[0].getType().isClassOrInterface() == null) {
      die("First parameter must be a class or interface in %s",
          formatMethodError(eventMethod));
    }

    JClassType eventReceiver = parameters[0].getType().isClassOrInterface();

    validateEventReceiver(parameters, eventReceiver, eventMethod);
  }

  /**
   * Validates the signature of all methods annotated with {@code @UiHandler}
   * in the {@code eventReceiver} type. All event handlers must have the same signature
   * where:
   * <ul>
   * <li> The annotation must list valid {@code ui:field}s
   * <li> The first parameter must be assignable to
   *      {@link com.google.gwt.event.dom.client.DomEvent DomEvent}
   * <li> If present, the second parameter must be of type
   *      {@link com.google.gwt.dom.client.Element Element}
   * <li> For all other parameters in position {@code n} must be of the same type as
   *      {@code parameters[n + 1]}
   * </ul>
   */
  private void validateEventReceiver(JParameter[] onBrowserEventParameters,
      JClassType eventReceiver, JMethod sourceMethod)
      throws UnableToCompleteException {

    // Pre-compute the expected parameter types (after the first one, that is)
    JType[] onBrowserEventParamTypes = new JType[onBrowserEventParameters.length - 2];

    // If present, second parameter must be an Element
    onBrowserEventParamTypes[0] = oracle.findType(com.google.gwt.dom.client.Element.class
        .getCanonicalName());
    // And the rest must be the same type
    for (int i = 3; i < onBrowserEventParameters.length; i++) {
      onBrowserEventParamTypes[i - 2] = onBrowserEventParameters[i].getType();
    }

    for (JMethod jMethod : eventReceiver.getInheritableMethods()) {
      Class<UiHandler> annotationClass = UiHandler.class;
      UiHandler annotation = jMethod.getAnnotation(annotationClass);
      // Ignore methods not annotated with @UiHandler
      if (annotation == null) {
        continue;
      }
      // Are the fields in @UiHandler known?
      String[] fields = annotation.value();
      if (fields == null) {
        die("@UiHandler returns null from its value in %s",
            formatMethodError(jMethod));
      }
      for (String fieldName : fields) {
        FieldWriter field = fieldManager.lookup(fieldName);
        if (field == null) {
          die("\"%s\" is not a known field name as listed in the @UiHandler annotation in %s",
              fieldName, formatMethodError(jMethod));
        }
      }

      // First parameter
      JParameter[] eventHandlerParameters = jMethod.getParameters();
      JClassType domEventType = oracle.findType(DomEvent.class.getCanonicalName());
      JClassType firstParamType = eventHandlerParameters[0].getType().isClassOrInterface();
      if (firstParamType == null || !firstParamType.isAssignableTo(domEventType)) {
        die("First parameter must be assignable to com.google.gwt.dom.client.DomEvent in %s",
            formatMethodError(jMethod));
      }

      // All others
      if (onBrowserEventParamTypes.length < eventHandlerParameters.length - 1) {
        die("Too many parameters in %s", formatMethodError(jMethod));
      }
      for (int i = 1; i < eventHandlerParameters.length; i++) {
        if (!eventHandlerParameters[i].getType().equals(onBrowserEventParamTypes[i - 1])) {
          die("Parameter %s in %s is not of the same type as parameter %s in %s",
              eventHandlerParameters[i].getName(), formatMethodError(jMethod),
              onBrowserEventParameters[i + 1].getName(),
              formatMethodError(sourceMethod));
        }
      }
    }
  }

  /**
   * Scan the base class for the getter methods. Assumes getters begin with
   * "get" and validates that each corresponds to a field declared with
   * {@code ui:field}. If the getter return type is assignable to
   * {@code Element}, the getter must have a single parameter and the parameter
   * must be assignable to {@code Element}. If the getter return type is assignable
   * to {@code com.google.gwt.resources.client.CssResource}, the getter must
   * have no parameters.
   */
  private void validateRendererGetters(JClassType owner) throws UnableToCompleteException {
    for (JMethod jMethod : owner.getInheritableMethods()) {
      String getterName = jMethod.getName();
      if (getterName.startsWith("get")) {
        String fieldName = getterToFieldName(getterName);
        FieldWriter field = fieldManager.lookup(fieldName);
        if (field == null || (!FieldWriterType.DEFAULT.equals(field.getFieldType())
            && !FieldWriterType.GENERATED_CSS.equals(field.getFieldType()))) {
          die("%s does not match a \"ui:field='%s'\" declaration in %s, "
              + "or '%s' refers to something other than a ui:style"
              + " or an HTML element in the template", getterName, fieldName,
              owner.getQualifiedSourceName(), fieldName);
        }
        if (FieldWriterType.DEFAULT.equals(field.getFieldType())
             && jMethod.getParameterTypes().length != 1) {
          die("Field getter %s must have exactly one parameter in %s", getterName,
              owner.getQualifiedSourceName());
        } else if (FieldWriterType.GENERATED_CSS.equals(field.getFieldType())
            && jMethod.getParameterTypes().length != 0) {
          die("Style getter %s must have no parameters in %s", getterName,
              owner.getQualifiedSourceName());
        } else if (jMethod.getParameterTypes().length == 1) {
          String elementClassName = com.google.gwt.dom.client.Element.class.getCanonicalName();
          JClassType elementType = oracle.findType(elementClassName);
          JClassType getterParamType =
              jMethod.getParameterTypes()[0].getErasedType().isClassOrInterface();

          if (!elementType.isAssignableFrom(getterParamType)) {
            die("Getter %s must have exactly one parameter of type assignable to %s in %s",
                getterName, elementClassName, owner.getQualifiedSourceName());
          }
        }
      } else if (!getterName.equals("render") && !getterName.equals("onBrowserEvent")
          && !getterName.equals("isParentOrRenderer")) {
        die("Unexpected method \"%s\" found in %s", getterName, owner.getQualifiedSourceName());
      }
    }
  }

  /**
   * Scans a class to validate that it contains a single method called render,
   * which has a {@code void} return type, and its first parameter is of type
   * {@code SafeHtmlBuilder}.
   */
  private void validateRenderParameters(JClassType owner) throws UnableToCompleteException {
    JMethod[] methods = owner.getInheritableMethods();
    JMethod renderMethod = null;

    for (JMethod jMethod : methods) {
      if (jMethod.getName().equals("render")) {
        if (renderMethod == null) {
          renderMethod = jMethod;
        } else {
          die("%s declares more than one method named render", owner.getQualifiedSourceName());
        }
      }
    }

    if (renderMethod == null
        || renderMethod.getParameterTypes().length < 1
        || !renderMethod.getParameterTypes()[0].getErasedType().getQualifiedSourceName().equals(
            SafeHtmlBuilder.class.getCanonicalName())) {
      die("%s does not declare a render(SafeHtmlBuilder ...) method",
          owner.getQualifiedSourceName());
    }
    if (!JPrimitiveType.VOID.equals(renderMethod.getReturnType())) {
      die("%s#render(SafeHtmlBuilder ...) does not return void", owner.getQualifiedSourceName());
    }
  }

  /**
   * Write statements that parsers created via calls to {@link #addStatement}.
   * Such statements will assume that {@link #writeGwtFields} has already been
   * called.
   */
  private void writeAddedStatements(IndentedWriter niceWriter) {
    for (String s : statements) {
      niceWriter.write(s);
    }
  }

  /**
   * Writes the UiBinder's source.
   */
  private void writeBinder(IndentedWriter w, FieldWriter rootField) throws UnableToCompleteException {
    writePackage(w);

    writeImports(w);
    w.newline();

    writeClassOpen(w);
    writeStatics(w);
    w.newline();

    // Create SafeHtml Template
    writeTemplatesInterface(w);
    w.newline();

    // createAndBindUi method
    w.write("public %s createAndBindUi(final %s owner) {",
        uiRootType.getParameterizedQualifiedSourceName(),
        uiOwnerType.getParameterizedQualifiedSourceName());
    w.indent();
    w.newline();

    writeGwtFields(w);
    w.newline();

    designTime.writeAttributes(this);
    writeAddedStatements(w);
    w.newline();

    writeInitStatements(w);
    w.newline();

    writeHandlers(w);
    w.newline();

    writeOwnerFieldSetters(w);

    writeCssInjectors(w);

    w.write("return %s;", rootField.getNextReference());
    w.outdent();
    w.write("}");

    // Close class
    w.outdent();
    w.write("}");
  }

  /**
   * Writes a different optimized UiBinder's source for the renderable strategy.
   */
  private void writeBinderForRenderableStrategy(IndentedWriter w, FieldWriter rootField)
      throws UnableToCompleteException {
    writePackage(w);

    writeImports(w);
    w.newline();

    writeClassOpen(w);
    writeStatics(w);
    w.newline();

    writeTemplatesInterface(w);

    w.newline();

    // createAndBindUi method
    w.write("public %s createAndBindUi(final %s owner) {",
        uiRootType.getParameterizedQualifiedSourceName(),
        uiOwnerType.getParameterizedQualifiedSourceName());
    w.indent();
    w.newline();

    designTime.writeAttributes(this);
    w.newline();

    w.write("return new Widgets(owner).%s;", rootField.getNextReference());
    w.outdent();
    w.write("}");

    // Writes the inner class Widgets.
    w.newline();
    w.write("/**");
    w.write(" * Encapsulates the access to all inner widgets");
    w.write(" */");
    w.write("class Widgets {");
    w.indent();

    String ownerClassType = uiOwnerType.getParameterizedQualifiedSourceName();
    w.write("private final %s owner;", ownerClassType);
    w.newline();

    writeHandlers(w);
    w.newline();

    w.write("public Widgets(final %s owner) {", ownerClassType);
    w.indent();
    w.write("this.owner = owner;");
    fieldManager.initializeWidgetsInnerClass(w, getOwnerClass());
    w.outdent();
    w.write("}");
    w.newline();

    htmlTemplates.writeTemplateCallers(w);

    evaluateUiFields();

    fieldManager.writeFieldDefinitions(w, getOracle(), getOwnerClass(), getDesignTime());

    w.outdent();
    w.write("}");

    // Close class
    w.outdent();
    w.write("}");
  }

  private void writeClassOpen(IndentedWriter w) {
    if (!isRenderer) {
      w.write("public class %s implements UiBinder<%s, %s>, %s {", implClassName,
          uiRootType.getParameterizedQualifiedSourceName(),
          uiOwnerType.getParameterizedQualifiedSourceName(),
          baseClass.getParameterizedQualifiedSourceName());
    } else {
      w.write("public class %s extends %s implements %s {", implClassName,
          AbstractUiRenderer.class.getName(),
          baseClass.getParameterizedQualifiedSourceName());
    }
    w.indent();
  }

  private void writeCssInjectors(IndentedWriter w) {
    for (ImplicitCssResource css : bundleClass.getCssMethods()) {
      w.write("%s.%s().ensureInjected();", bundleClass.getFieldName(), css.getName());
    }
    w.newline();
  }

  /**
   * Write declarations for variables or fields to hold elements declared with
   * gwt:field in the template. For those that have not had constructor
   * generation suppressed, emit GWT.create() calls instantiating them (or die
   * if they have no default constructor).
   * 
   * @throws UnableToCompleteException on constructor problem
   */
  private void writeGwtFields(IndentedWriter niceWriter) throws UnableToCompleteException {
    // For each provided field in the owner class, initialize from the owner
    Collection<OwnerField> ownerFields = getOwnerClass().getUiFields();
    for (OwnerField ownerField : ownerFields) {
      if (ownerField.isProvided()) {
        String fieldName = ownerField.getName();
        FieldWriter fieldWriter = fieldManager.lookup(fieldName);

        // TODO why can this be null?
        if (fieldWriter != null) {
          String initializer;
          if (designTime.isDesignTime()) {
            String typeName = ownerField.getType().getRawType().getQualifiedSourceName();
            initializer = designTime.getProvidedField(typeName, ownerField.getName());
          } else {
            initializer = formatCode("owner.%1$s", fieldName);
          }
          fieldManager.lookup(fieldName).setInitializer(initializer);
        }
      }
    }

    fieldManager.writeGwtFieldsDeclaration(niceWriter);
  }

  private void writeHandlers(IndentedWriter w) throws UnableToCompleteException {
    if (designTime.isDesignTime()) {
      return;
    }
    handlerEvaluator.run(w, fieldManager, "owner");
  }

  private void writeImports(IndentedWriter w) {
    w.write("import com.google.gwt.core.client.GWT;");
    w.write("import com.google.gwt.dom.client.Element;");
    if (!(htmlTemplates.isEmpty())) {
      w.write("import com.google.gwt.safehtml.client.SafeHtmlTemplates;");
      w.write("import com.google.gwt.safehtml.shared.SafeHtml;");
      w.write("import com.google.gwt.safehtml.shared.SafeHtmlUtils;");
      w.write("import com.google.gwt.safehtml.shared.SafeHtmlBuilder;");
      w.write("import com.google.gwt.safehtml.shared.SafeUri;");
      w.write("import com.google.gwt.safehtml.shared.UriUtils;");
      w.write("import com.google.gwt.uibinder.client.UiBinderUtil;");
    }

    if (!isRenderer) {
      w.write("import com.google.gwt.uibinder.client.UiBinder;");
      w.write("import com.google.gwt.uibinder.client.UiBinderUtil;");
      w.write("import %s.%s;", uiRootType.getPackage().getName(), uiRootType.getName());
    } else {
      w.write("import com.google.gwt.text.shared.AbstractSafeHtmlRenderer;");
    }
  }

  /**
   * Write statements created by {@link #addInitStatement}. This code must be
   * placed after all instantiation code.
   */
  private void writeInitStatements(IndentedWriter niceWriter) {
    for (String s : initStatements) {
      niceWriter.write(s);
    }
  }

  /**
   * Write the statements to fill in the fields of the UI owner.
   */
  private void writeOwnerFieldSetters(IndentedWriter niceWriter) throws UnableToCompleteException {
    if (designTime.isDesignTime()) {
      return;
    }
    for (OwnerField ownerField : getOwnerClass().getUiFields()) {
      String fieldName = ownerField.getName();
      FieldWriter fieldWriter = fieldManager.lookup(fieldName);

      if (fieldWriter != null) {
        // ownerField is a widget.
        JClassType type = fieldWriter.getInstantiableType();
        if (type != null) {
          maybeWriteFieldSetter(niceWriter, ownerField, fieldWriter.getInstantiableType(),
              fieldName);
        } else {
          // Must be a generated type
          if (!ownerField.isProvided()) {
            niceWriter.write("owner.%1$s = %1$s;", fieldName);
          }
        }

      } else {
        // ownerField was not found as bundle resource or widget, must die.
        die("Template %s has no %s attribute for %s.%s#%s", templatePath,
            getUiFieldAttributeName(), uiOwnerType.getPackage().getName(), uiOwnerType.getName(),
            fieldName);
      }
    }
  }

  private void writePackage(IndentedWriter w) {
    String packageName = baseClass.getPackage().getName();
    if (packageName.length() > 0) {
      w.write("package %1$s;", packageName);
      w.newline();
    }
  }

  /**
   * Writes the UiRenderer's source for the renderable strategy.
   */
  private void writeRenderer(IndentedWriter w, FieldWriter rootField) throws UnableToCompleteException {
    validateRendererGetters(baseClass);
    validateRenderParameters(baseClass);
    JMethod[] eventMethods = findEventMethods(baseClass);
    for (JMethod jMethod : eventMethods) {
      validateEventMethod(jMethod);
    }

    writePackage(w);

    writeImports(w);
    w.newline();

    writeClassOpen(w);
    writeStatics(w);
    w.newline();

    // Create SafeHtml Template
    writeTemplatesInterface(w);
    w.newline();
    htmlTemplates.writeTemplateCallers(w);

    w.newline();

    JParameter[] renderParameters = findRenderParameters(baseClass);

    writeRenderParameterDefinitions(w, renderParameters);

    String renderParameterDeclarations = renderMethodParameters(renderParameters);
    w.write("public void render(final %s sb%s%s) {", SafeHtmlBuilder.class.getName(),
        renderParameterDeclarations.length() != 0 ? ", " : "", renderParameterDeclarations);
    w.indent();
    w.newline();

    writeRenderParameterInitializers(w, renderParameters);

    w.write("uiId = com.google.gwt.dom.client.Document.get().createUniqueId();");
    w.newline();

    fieldManager.initializeWidgetsInnerClass(w, getOwnerClass());
    w.newline();

    String safeHtml = rootField.getSafeHtml();

    // TODO(rchandia) it should be possible to add the attribute when parsing
    // the UiBinder file
    w.write(
        "sb.append(stampUiRendererAttribute(%s, RENDERED_ATTRIBUTE, uiId));",
        safeHtml);
    w.outdent();

    w.write("}");
    w.newline();

    fieldManager.writeFieldDefinitions(w, getOracle(), getOwnerClass(), getDesignTime());

    writeRendererGetters(w, baseClass, rootField.getName());

    writeRendererEventMethods(w, eventMethods, rootField.getName());

    // Close class
    w.outdent();
    w.write("}");
  }

  private void writeRendererDispatcher(IndentedWriter w, String dispatcherName,
      JClassType targetType, String rootFieldName, JMethod[] uiHandlerMethods, JMethod sourceMethod)
      throws UnableToCompleteException {

    // static class UiRendererDispatcherForFoo extends UiRendererDispatcher<Foo> {
    w.write("static class %s extends UiRendererDispatcher<%s> {", dispatcherName,
        targetType.getQualifiedSourceName());
    w.indent();

    writeRendererDispatcherTableInit(w, rootFieldName, uiHandlerMethods,
        dispatcherName);

    writeRendererDispatcherExtraParameters(w, sourceMethod);

    writeRendererDispatcherFire(w, sourceMethod);

    w.write("@SuppressWarnings(\"rawtypes\")");
    w.write("@Override");
    // public void fireEvent(GwtEvent<?> somethingUnlikelyToCollideWithParamNames) {
    w.write("public void fireEvent(com.google.gwt.event.shared.GwtEvent<?> %sEvent) {",
        SAFE_VAR_PREFIX);
    w.indent();
    //   switch (getMethodIndex()) {
    w.write("switch (getMethodIndex()) {");
    w.indent();
    for (int j = 0; j < uiHandlerMethods.length; j++) {
      JMethod uiMethod = uiHandlerMethods[j];

      // case 0:
      w.write("case %s:", j);
      w.indent();

      //   getEventTarget().onClickRoot((ClickEvent) somethingUnlikelyToCollideWithParamNames,
      //       getRoot(), a, b);
      StringBuffer sb = new StringBuffer();
      JParameter[] sourceParameters = sourceMethod.getParameters();
      // Cat the extra parameters i.e. ", a, b"
      JType[] uiHandlerParameterTypes = uiMethod.getParameterTypes();
      if (uiHandlerParameterTypes.length >= 2) {
        sb.append(", getRoot()");
      }
      for (int k = 2; k < uiHandlerParameterTypes.length; k++) {
        JParameter sourceParam = sourceParameters[k + 1];
        sb.append(", ");
        sb.append(sourceParam.getName());
      }
      w.write("getEventTarget().%s((%s) %sEvent%s);", uiMethod.getName(),
          uiHandlerParameterTypes[0].getQualifiedSourceName(), SAFE_VAR_PREFIX,
          sb.toString());
      //   break;
      w.write("break;");
      w.newline();
      w.outdent();
    }
    //    default:
    w.write("default:");
    w.indent();
    //      break;
    w.write("break;");
    w.outdent();
    w.outdent();
    w.write("}");

    w.outdent();
    w.write("}");

    w.outdent();
    w.write("}");
  }

  private void writeRendererDispatcherExtraParameters(IndentedWriter w, JMethod sourceMethod) {
    for (int i = 3; i < sourceMethod.getParameters().length; i++) {
      JParameter param = sourceMethod.getParameters()[i];

      // private int a;
      // private String b;
      w.write("private %s %s;", param.getType().getParameterizedQualifiedSourceName(),
          param.getName());
    }
  }

  private void writeRendererDispatcherFire(IndentedWriter w, JMethod sourceMethod) {
    // public void fire(Foo o, NativeEvent e, Element parent, int a, String b) {
    w.write("public void fire(");
    w.indent();
    JParameter[] sourceParameters = sourceMethod.getParameters();
    for (int i = 0; i < sourceParameters.length; i++) {
      JParameter param = sourceParameters[i];
      w.write(i == 0 ? "%s %s" : ", %s %s", param.getType().getQualifiedSourceName(), param.getName());
    }
    w.write(") {");
    w.indent();

    // this.a = a;
    for (int i = 3; i < sourceParameters.length; i++) {
      JParameter sourceParam = sourceParameters[i];
      w.write("this.%s = %s;", sourceParam.getName(), sourceParam.getName());
    }

    // fireEvent(o, e, parent);
    w.write("fireEvent(%s, %s, %s);", sourceParameters[0].getName(), sourceParameters[1].getName(),
        sourceParameters[2].getName());

    w.outdent();
    w.write("}");
    w.newline();
  }

  private void writeRendererDispatcherTableInit(IndentedWriter w,
      String rootFieldName, JMethod[] uiHandlerMethods, String dispatcherName)
      throws UnableToCompleteException {
    ArrayList<String> keys = new ArrayList<String>();
    ArrayList<Integer> values = new ArrayList<Integer>();

    // Collect the event types and field names to form the dispatch table
    for (int i = 0; i < uiHandlerMethods.length; i++) {
      JMethod jMethod = uiHandlerMethods[i];
      String eventType = findEventTypeName(jMethod);
      String[] fieldNames = jMethod.getAnnotation(UiHandler.class).value();
      for (String fieldName : fieldNames) {
        if (rootFieldName.equals(fieldName)) {
          fieldName = AbstractUiRenderer.ROOT_FAKE_NAME;
        }
        keys.add(eventType + AbstractUiRenderer.UI_ID_SEPARATOR + fieldName);
        values.add(i);
      }
    }

    // private static String[] somethingUnlikelyToCollideWithParamNames_keys;
    w.write("private static String[] %s_keys;", SAFE_VAR_PREFIX);
    // private static Integer[] somethingUnlikelyToCollideWithParamNames_values;
    w.write("private static Integer[] %s_values;", SAFE_VAR_PREFIX);

    w.write("static {");
    w.indent();
    // private static String[] somethingUnlikelyToCollideWithParamNames_keys = new String[] {
    w.write("%s_keys = new String[] {", SAFE_VAR_PREFIX);
    w.indent();
    for (String key : keys) {
      // "click:aField",
      w.write("\"%s\",", key);
    }
    w.outdent();
    w.write("};");
    w.newline();

    // somethingUnlikelyToCollideWithParamNames_values = {0,1};
    w.write("%s_values = new Integer[] {", SAFE_VAR_PREFIX);
    w.indent();
    StringBuffer commaSeparatedValues = new StringBuffer();
    for (Integer value : values) {
      commaSeparatedValues.append(value);
      commaSeparatedValues.append(",");
    }
    // "0,0,0,1,1,",
    w.write("%s", commaSeparatedValues.toString());
    w.outdent();
    w.write("};");
    w.newline();

    w.outdent();
    w.write("}");
    w.newline();

    // public Foo() {
    w.write("public %s() {", dispatcherName);
    w.indent();
    // initDispatchTable(keys, values);
    w.write("initDispatchTable(%s_keys, %s_values);", SAFE_VAR_PREFIX, SAFE_VAR_PREFIX);

    // This ensures the DomEvent#TYPE fields are properly initialized and registered
    // ClickEvent.getType();
    HashSet<String> eventTypes = new HashSet<String>();
    for (JMethod uiMethod : uiHandlerMethods) {
      eventTypes.add(uiMethod.getParameterTypes()[0].getQualifiedSourceName());
    }
    for (String eventType : eventTypes) {
      w.write("%s.getType();", eventType);
    }


    w.outdent();
    w.write("}");
    w.newline();
  }

  private void writeRendererEventMethods(IndentedWriter w, JMethod[] eventMethods,
      String rootField) throws UnableToCompleteException {
    for (JMethod jMethod : eventMethods) {
      JClassType eventTargetType = jMethod.getParameterTypes()[0].isClassOrInterface();
      String eventTargetSimpleName = eventTargetType.getSimpleSourceName();
      String dispatcherClassName = UI_RENDERER_DISPATCHER_PREFIX + eventTargetSimpleName;
      JMethod[] uiHandlerMethods = findUiHandlerMethods(eventTargetType);

      // public void onBrowserEvent(Foo f, NativeEvent event, Element parent, A a, B b ...) {
      w.write("@Override");
      w.write("public %s {", jMethod.getReadableDeclaration(true, true, true, true, true));

      if (uiHandlerMethods.length != 0) {
        w.indent();
        //  if (singletonUiRendererDispatcherForFoo == null) {
        w.write("if (singleton%s == null) {", dispatcherClassName);
        w.indent();
        // singletonUiRendererDispatcherForFoo = new UiRendererDispatcherForFoo();
        w.write("singleton%s = new %s();", dispatcherClassName, dispatcherClassName);

        w.outdent();
        w.write("}");

        // singletonUiRendererDispatcherForFoo.fire(o, event, parent, a, b);
        StringBuffer sb = new StringBuffer();
        JParameter[] parameters = jMethod.getParameters();
        for (int i = 0; i < parameters.length; i++) {
          JParameter callParam = parameters[i];
          if (i != 0) {
            sb.append(", ");
          }
          sb.append(callParam.getName());
        }
        w.write("singleton%s.fire(%s);", dispatcherClassName, sb.toString());
        w.outdent();
      }

      w.write("}");
      w.newline();

      if (uiHandlerMethods.length != 0) {
        // private static UiRendererDispatcherForFoo singletonUiRendererDispatcherForFoo;
        w.write("private static %s singleton%s;", dispatcherClassName, dispatcherClassName);

        writeRendererDispatcher(w, dispatcherClassName, eventTargetType, rootField, uiHandlerMethods,
            jMethod);
      }
    }
  }

  private void writeRendererGetters(IndentedWriter w, JClassType owner, String rootFieldName) {
    List<JMethod> getters = findGetterNames(owner);

    // For every requested getter
    for (JMethod getter : getters) {
      // public ElementSubclass getFoo(Element parent) {
      w.write("%s {", getter.getReadableDeclaration(false, false, false, false, true));
      w.indent();
      String getterFieldName = getterToFieldName(getter.getName());
      // Is this a CSS resource field?
      FieldWriter fieldWriter = fieldManager.lookup(getterFieldName);
      if (FieldWriterType.GENERATED_CSS.equals(fieldWriter.getFieldType())) {
        // return (CssResourceSubclass) get_styleField;
        w.write("return (%s) %s;", getter.getReturnType().getErasedType().getQualifiedSourceName(),
                FieldManager.getFieldGetter(getterFieldName));
      } else {
        // Else the non-root elements are found by id
        String elementParameter = getter.getParameters()[0].getName();
        if (!getterFieldName.equals(rootFieldName)) {
          // return (ElementSubclass) findInnerField(parent, "foo", RENDERED_ATTRIBUTE);
          w.write("return (%s) findInnerField(%s, \"%s\", RENDERED_ATTRIBUTE);",
              getter.getReturnType().getErasedType().getQualifiedSourceName(), elementParameter,
              getterFieldName);
        } else {
          // return (ElementSubclass) findRootElement(parent);
          w.write("return (%s) findRootElement(%s, RENDERED_ATTRIBUTE);",
              getter.getReturnType().getErasedType().getQualifiedSourceName(), elementParameter);
        }
      }
      w.outdent();
      w.write("}");
    }
  }

  private void writeRenderParameterDefinitions(IndentedWriter w, JParameter[] renderParameters) {
    for (int i = 0; i < renderParameters.length; i++) {
      JParameter parameter = renderParameters[i];
      w.write("private %s %s%s;", parameter.getType().getQualifiedSourceName(),
          RENDER_PARAM_HOLDER_PREFIX, parameter.getName());
      w.newline();
    }
  }

  private void writeRenderParameterInitializers(IndentedWriter w, JParameter[] renderParameters) {
    for (int i = 0; i < renderParameters.length; i++) {
      JParameter parameter = renderParameters[i];
      w.write("%s%s = %s;", RENDER_PARAM_HOLDER_PREFIX, parameter.getName(), parameter.getName());
      w.newline();
    }
  }

  private void writeStaticMessagesInstance(IndentedWriter niceWriter) {
    if (messages.hasMessages()) {
      niceWriter.write(messages.getDeclaration());
    }
  }

  private void writeStatics(IndentedWriter w) {
    writeStaticMessagesInstance(w);
    designTime.addDeclarations(w);
  }

  /**
   * Write statements created by {@link HtmlTemplatesWriter#addSafeHtmlTemplate}
   * . This code must be placed after all instantiation code.
   */
  private void writeTemplatesInterface(IndentedWriter w) {
    if (!(htmlTemplates.isEmpty())) {
      assert useSafeHtmlTemplates : "SafeHtml is off, but templates were made.";
      htmlTemplates.writeInterface(w);
      w.newline();
    }
  }
}
