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
package com.google.gwt.uibinder.parsers;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.uibinder.rebind.UiBinderWriter;
import com.google.gwt.uibinder.rebind.XMLElement;
import com.google.gwt.user.client.ui.MenuItem;

/**
 * A parser for menu items.
 */
public class MenuItemParser implements ElementParser {

  /**
   * Used by {@link XMLElement#consumeInnerHtml}. Gets to examine
   * each dom element. Removes gwt:MenuBar elements and hands them
   * to the template writer to be interpreted as widgets. Replaces
   * gwt:MenuItemHTML elements with their consumeInnerHtml contents,
   * and warns that they are deprecated.
   */
  private static class MenuItemGutsInterpreter
      implements XMLElement.Interpreter<String> {
    private final String namespaceUri;
    private final HtmlInterpreter htmlInterpreter;
    private final UiBinderWriter writer;
    private final String errorContext;

    private String menuBarField;

    public MenuItemGutsInterpreter(UiBinderWriter writer, String namespaceUri,
        String errorContext, HtmlInterpreter htmlInterpreter) {
      this.writer = writer;
      this.errorContext = errorContext;
      this.namespaceUri = namespaceUri;
      this.htmlInterpreter = htmlInterpreter;
    }

    public String interpretElement(XMLElement elem)
        throws UnableToCompleteException {
      if (isMenuHtml(elem)) {
        writer.warn("In %s, the MenuItemHTML element is no longer required, "
            + "and its contents have been inlined. This will become an error.",
            errorContext);
        return elem.consumeInnerHtml(htmlInterpreter);
      }

      if (isMenuBar(elem)) {
        if (menuBarField != null) {
          writer.die("In %s, only one MenuBar may be contained in a MenuItem",
              errorContext);
        }
        menuBarField = writer.parseElementToField(elem);
        return "";
      }

      return null;
    }

    String getMenuBarField() {
      return menuBarField;
    }

    private boolean isMenuBar(XMLElement child) {
      return namespaceUri.equals(child.getNamespaceUri())
          && child.getLocalName().equals(TAG_MENUBAR);
    }

    private boolean isMenuHtml(XMLElement child) {
      return namespaceUri.equals(child.getNamespaceUri())
          && child.getLocalName().equals(TAG_MENUITEMHTML);
    }
  }
  private static final String TAG_MENUBAR = "MenuBar";

  private static final String TAG_MENUITEMHTML = "MenuItemHTML";

  public void parse(final XMLElement elem, String fieldName, JClassType type,
      final UiBinderWriter writer) throws UnableToCompleteException {
    writer.setFieldInitializerAsConstructor(fieldName,
        writer.getOracle().findType(MenuItem.class.getName()),
        "\"\"", "(com.google.gwt.user.client.Command) null");

    InterpreterPipe<String> interpreter = new InterpreterPipe<String>();

    // Build an interpreter pipeline to handle MenuItemHTML and MenuBar
    // children, and to interpret everything other than those as HTML.
    // TODO(rjrjr) Once MenuItemHTML goes away, we can reduce this to
    // just handling MenuBar, and rely on HasHTMLParser to do the
    // "everything other than those" bit.

    final HtmlInterpreter htmlInterpreter =
        HtmlInterpreter.newInterpreterForUiObject(writer, fieldName);
    MenuItemGutsInterpreter guts =
        new MenuItemGutsInterpreter(writer, elem.getNamespaceUri(),
            elem.toString(), htmlInterpreter);

    interpreter.add(guts);
    interpreter.add(htmlInterpreter);

    String html = elem.consumeInnerHtml(interpreter);
    if (html.trim().length() > 0) {
      writer.genStringPropertySet(fieldName, "HTML", html);
    }
    if (guts.getMenuBarField() != null) {
      writer.genPropertySet(fieldName, "subMenu", guts.getMenuBarField());
    }
  }
}
