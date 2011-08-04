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
package com.google.gwt.uibinder.rebind.messages;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.i18n.client.Messages;
import com.google.gwt.uibinder.rebind.IndentedWriter;
import com.google.gwt.uibinder.rebind.MortalLogger;
import com.google.gwt.uibinder.rebind.UiBinderWriter;
import com.google.gwt.uibinder.rebind.XMLAttribute;
import com.google.gwt.uibinder.rebind.XMLElement;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * TemplateWriter for messages.
 */
public class MessagesWriter {

  public static final String ATTRIBUTE = "attribute";

  private static final String NAME = "name";
  private static final String[] EMPTY_ARRAY = {};

  private final String messagesNamespaceURI;
  private final String packageName;
  private final String messagesClassName;
  private final MortalLogger logger;
  private final List<MessageWriter> messages = new ArrayList<MessageWriter>();
  private final String generatedFrom;

  private String baseInterface;
  private String defaultLocale;
  private String messagesPrefix;
  private String generateKeys;
  private GenerateAnnotationWriter generate;

  private Map<XMLElement, Collection<AttributeMessage>> elemToAttributeMessages =
      new HashMap<XMLElement, Collection<AttributeMessage>>();

  private TypeOracle oracle;

  public MessagesWriter(TypeOracle oracle, String nameSpaceUri, MortalLogger mortalLogger, String generatedFrom,
      String packageName, String uiBinderImplClassName) {
    this.messagesNamespaceURI = nameSpaceUri;
    this.generatedFrom = generatedFrom;
    this.packageName = packageName;

    // Localizable classes cannot have underscores in their names.
    this.messagesClassName = uiBinderImplClassName.replaceAll("_", "") + "GenMessages";

    this.logger = mortalLogger;
    this.oracle = oracle;

    baseInterface = Messages.class.getCanonicalName();
  }

  /**
   * Call {@link #consumeAttributeMessages}, but instead of returning the
   * results store them for retrieval by a later call to
   * {@link #retrieveMessageAttributesFor}.
   *
   * @throws UnableToCompleteException on error
   */
  public void consumeAndStoreMessageAttributesFor(XMLElement elem)
      throws UnableToCompleteException {
    Collection<AttributeMessage> attributeMessages =
        consumeAttributeMessages(elem);
    if (!attributeMessages.isEmpty()) {
      elemToAttributeMessages.put(elem, attributeMessages);
    }
  }

  /**
   * Examine the children of the given element. Consume those tagged m:attribute
   * and return a set of {@link AttributeMessage} instances. E.g.:
   * <p>
   *
   * <pre>
   * &lt;img src="blueSky.jpg" alt="A blue sky">
   *   &lt;ui:attribute name="alt" description="blue sky image alt text"/>
   * &lt;/img>
   * </pre>
   *
   * <p>
   */
  public Collection<AttributeMessage> consumeAttributeMessages(XMLElement elem)
      throws UnableToCompleteException {
    Collection<XMLElement> messageChildren = getAttributeMessageChildren(elem);
    if (messageChildren.isEmpty()) {
      return Collections.emptySet();
    }

    Set<AttributeMessage> attributeMessages = new HashSet<AttributeMessage>();
    for (XMLElement child : messageChildren) {
      String attributeName = consumeMessageElementAttribute(NAME, child);
      if (attributeName.length() == 0) {
        logger.die(child, "Missing name attribute");
      }
      if (!elem.hasAttribute(attributeName)) {
        logger.die(child, "The enclosing element needs to provide a "
            + "default value for attribute \"%s\"", attributeName);
      }
      XMLAttribute attribute = elem.getAttribute(attributeName);
      if (attribute.hasComputedValue()) {
        logger.die(elem, "Attribute \"%s\" has a field reference and "
            + "so cannot be marked for localization, but found %s",
            attributeName, child);
      }

      String defaultMessage = MessageWriter.escapeMessageFormat(elem.consumeRawAttribute(attributeName));
      defaultMessage = UiBinderWriter.escapeTextForJavaStringLiteral(defaultMessage);
      attributeMessages.add(new AttributeMessage(attributeName, declareMessage(
          child, defaultMessage)));
    }
    return attributeMessages;
  }

  /**
   * Consume an m:blah attribute on a non-message element, e.g.
   * {@code <span m:ph="fnord"/>}
   * 
   * @param attName name of the attribute (to be prefixed with "msgprefix:")
   * @param elem element to search
   * @return attribute value, or an empty string if not found
   */
  public String consumeMessageAttribute(String attName, XMLElement elem) {
    return consumeMessageAttribute(attName, elem, "");
  }

  /**
   * Consume an m:blah attribute on a non-message element, e.g.
   * {@code <span m:ph="fnord"/>}
   * 
   * @param attName name of the attribute (to be prefixed with "msgprefix:")
   * @param elem element to search
   * @param defaultValue default value to return if the attribute is not present
   * @return attribute value, or {@code defaultValue} if not found
   */
  public String consumeMessageAttribute(String attName, XMLElement elem, String defaultValue) {
    String fullAttName = getMessagesPrefix() + ":" + attName;
    return elem.consumeRawAttribute(fullAttName, defaultValue);
  }

  /**
   * Declares a message created by a previous call to {@link #newMessage}, and
   * returns its invocation expression to be stitched into an innerHTML block.
   */
  public String declareMessage(MessageWriter newMessage) {
    messages.add(newMessage);
    return String.format("messages.%s", newMessage.getInvocation());
  }

  /**
   * Expected to be called with the root element, to allow configuration from
   * various messages related attributes.
   * 
   * @throws UnableToCompleteException 
   */
  public void findMessagesConfig(XMLElement elem) throws UnableToCompleteException {
    String prefix = elem.lookupPrefix(getMessagesUri());
    if (prefix != null) {
      messagesPrefix = prefix;
      String baseInterfaceAttr = consumeMessageAttribute("baseMessagesInterface", elem, null);
      if (baseInterfaceAttr != null) {
        JClassType baseInterfaceType = oracle.findType(baseInterfaceAttr);
        if (baseInterfaceType == null) {
          logger.die(elem, "Could not find class %s", baseInterfaceAttr);
        }
        if (baseInterfaceType.isInterface() == null) {
          logger.die(elem, "%s must be an interface", baseInterfaceAttr);
        }
        JClassType msgType = oracle.findType(Messages.class.getCanonicalName());
        if (msgType == null) {
          throw new RuntimeException("Internal Error: Messages interface not found");
        }
        if (!msgType.isAssignableFrom(baseInterfaceType)) {
          logger.die(elem, "Interface %s must extend Messages", baseInterfaceAttr);
        }
        baseInterface = baseInterfaceAttr;
      }
      defaultLocale = consumeMessageAttribute("defaultLocale", elem);
      generateKeys = consumeMessageAttribute("generateKeys", elem);
      generate =
          new GenerateAnnotationWriter(getMessageAttributeStringArray(
              "generateFormat", elem), consumeMessageAttribute(
              "generateFilename", elem), getMessageAttributeStringArray(
              "generateLocales", elem));
    }
  }

  /**
   * Returns the expression that will instantiate the Messages interface.
   */
  public String getDeclaration() {
    return String.format(
        "static %1$s messages = (%1$s) GWT.create(%1$s.class);",
        getMessagesClassName());
  }

  public String getMessagesClassName() {
    return messagesClassName;
  }

  /**
   * Returns the namespace prefix (not including :) declared by the template for
   * message elements and attributes.
   */
  public String getMessagesPrefix() {
    return messagesPrefix;
  }

  /**
   * Confirm existence of an m:blah attribute on a non-message element, e.g.
   * {@code <span ui:ph="fnord"/>}
   */
  public boolean hasMessageAttribute(String attName, XMLElement elem) {
    String fullAttName = getMessagesPrefix() + ":" + attName;
    return elem.hasAttribute(fullAttName);
  }

  /**
   * Returns true iff any messages have been declared.
   */
  public boolean hasMessages() {
    return !messages.isEmpty();
  }

  public boolean isMessage(XMLElement elem) {
    return isMessagePrefixed(elem) && "msg".equals(elem.getLocalName());
  }

  /**
   * Creates a new MessageWriter instance with description, key and meaning
   * values consumed from the given XMLElement. Note that this message will not
   * be written in the generated code unless it is later declared via
   * {@link #declareMessage(MessageWriter)}
   */
  public MessageWriter newMessage(XMLElement elem) {
    MessageWriter newMessage =
        new MessageWriter(consumeMessageElementAttribute("description", elem),
            consumeMessageElementAttribute("key", elem),
            consumeMessageElementAttribute("meaning", elem),
            nextMessageName());
    return newMessage;
  }

  /**
   * Returns the set of AttributeMessages that were found in elem and stored by a
   * previous call to {@link #consumeAndStoreMessageAttributesFor}.
   */
  public Collection<AttributeMessage> retrieveMessageAttributesFor(
      XMLElement elem) {
    return elemToAttributeMessages.get(elem);
  }

  public void write(PrintWriter printWriter) {
    IndentedWriter writer = new IndentedWriter(printWriter);

    // Package declaration.
    if (packageName.length() > 0) {
      writer.write("package %1$s;", packageName);
      writer.newline();
    }

    // Imports.
    writer.write("import static com.google.gwt.i18n.client.LocalizableResource.*;");
    writer.newline();

    // Open interface.
    genInterfaceAnnotations(writer);
    writer.write("public interface %s extends %s {", getMessagesClassName(), baseInterface);
    writer.newline();
    writer.indent();

    // Write message methods
    for (MessageWriter m : messages) {
      m.writeDeclaration(writer);
    }

    // Close interface.
    writer.outdent();
    writer.write("}");
  }

  /**
   * Consume an attribute on a messages related element (as oppposed to a
   * messages attribute in some other kind of element), e.g. the description in
   * {@code <m:msg description="described!">}
   */
  String consumeMessageElementAttribute(String attName, XMLElement elem) {
    if (elem.hasAttribute(attName)) {
      return UiBinderWriter.escapeTextForJavaStringLiteral(elem.consumeRawAttribute(attName));
    }

    String fullAttName = getMessagesPrefix() + ":" + attName;
    if (elem.hasAttribute(fullAttName)) {
      String value = elem.consumeRawAttribute(fullAttName);
      logger.warn(elem,
          "Deprecated prefix \"%s:\" on \"%s\". Use \"%s\" instead.",
          getMessagesPrefix(), fullAttName, attName);
      return value;
    }

    return "";
  }

  String consumeRequiredMessageElementAttribute(String attName,
      XMLElement elem) throws UnableToCompleteException {
    String value = consumeMessageElementAttribute(attName, elem);
    if ("".equals(value)) {
      logger.die(elem, "Missing required attribute %s", attName);
    }
    return value;
  }

  boolean isMessagePrefixed(XMLElement elem) {
    String uri = elem.getNamespaceUri();
    return uri != null && uri.startsWith(getMessagesUri());
  }

  private String declareMessage(XMLElement elem, String defaultMessage) {
    List<PlaceholderWriter> emptyList = Collections.emptyList();
    MessageWriter newMessage = newMessage(elem);
    newMessage.setDefaultMessage(defaultMessage);
    for (PlaceholderWriter placeholder : emptyList) {
      newMessage.addPlaceholder(placeholder);
    }
    return declareMessage(newMessage);
  }

  private void genInterfaceAnnotations(IndentedWriter pw) {
    pw.write("@GeneratedFrom(\"%s\")", generatedFrom);
    if (defaultLocale.length() > 0) {
      pw.write("@DefaultLocale(\"%s\")", defaultLocale);
    }
    if (generateKeys.length() > 0) {
      pw.write("@GenerateKeys(\"%s\")", generateKeys);
    }
    generate.write(pw);
  }

  private Collection<XMLElement> getAttributeMessageChildren(
      final XMLElement elem) throws UnableToCompleteException {
    return elem.consumeChildElements(new XMLElement.Interpreter<Boolean>() {
      public Boolean interpretElement(XMLElement child)
          throws UnableToCompleteException {
        if (isAttributeMessage(child)) {
          if (child.hasChildNodes()) {
            logger.die(child, "Illegal body.", child, elem);
          }
          return true;
        }

        return false;
      }
    });
  }

  private String[] getMessageAttributeStringArray(String attName,
      XMLElement elem) {
    String value = consumeMessageAttribute(attName, elem, null);
    if (value == null) {
      return EMPTY_ARRAY;
    }
    return value.split("\\s*,\\s*");
  }

  private String getMessagesUri() {
    return messagesNamespaceURI;
  }

  private boolean isAttributeMessage(XMLElement elem) {
    return isMessagePrefixed(elem) && ATTRIBUTE.equals(elem.getLocalName());
  }

  private String nextMessageName() {
    return String.format("message%d", messages.size() + 1);
  }
}
