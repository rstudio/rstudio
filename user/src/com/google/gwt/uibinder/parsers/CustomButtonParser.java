/*
 * Copyright 2007 Google Inc.
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

import java.util.HashSet;
import java.util.Set;

/**
 * Parses CustomButton widgets.
 */
public class CustomButtonParser implements ElementParser {

  private static final Set<String> faceNames = new HashSet<String>();
  private static final Object IMAGE_CLASS =
      "com.google.gwt.user.client.ui.Image";

  static {
    faceNames.add("UpFace");
    faceNames.add("DownFace");
    faceNames.add("UpHoveringFace");
    faceNames.add("DownHoveringFace");
    faceNames.add("UpDisabledFace");
    faceNames.add("DownDisabledFace");
  }

  public void parse(XMLElement elem, String fieldName, JClassType type,
      UiBinderWriter writer) throws UnableToCompleteException {
    // Parse children.
    for (XMLElement child : elem.consumeChildElements()) {
      // CustomButton can only contain Face elements.
      String ns = child.getNamespaceUri();
      String faceName = child.getLocalName();

      if (!ns.equals(elem.getNamespaceUri())) {
        writer.die("Invalid CustomButton child namespace: " + ns);
      }
      if (!faceNames.contains(faceName)) {
        writer.die("Invalid CustomButton face: " + faceName);
      }

      // Look for innerHTML first.
      HtmlInterpreter interpreter =
          HtmlInterpreter.newInterpreterForUiObject(writer, fieldName);
      String innerHtml = child.consumeInnerHtml(interpreter).trim();
      if (innerHtml.length() > 0) {
        writer.addStatement("%1$s.get%2$s().setHTML(\"%3$s\");", fieldName, faceName,
            innerHtml);
      }

      // Then look for html, text, and image attributes.
      if (child.hasAttribute("html")) {
        String html = child.consumeAttribute("html");
        writer.addStatement("%1$s.get%2$s().setHTML(\"%3$s\");", fieldName, faceName, html);
      }

      if (child.hasAttribute("text")) {
        String text = child.consumeAttribute("text");
        writer.addStatement("%1$s.get%2$s().setText(\"%3$s\");", fieldName, faceName, text);
      }

      if (child.hasAttribute("image")) {
        String image = child.consumeAttribute("image");
        writer.addStatement("%1$s.get%2$s().setImage(new %3$s(\"%4$s\"));", fieldName,
            faceName, IMAGE_CLASS, image);
      }
    }
  }
}
