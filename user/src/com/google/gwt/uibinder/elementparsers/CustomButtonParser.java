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
package com.google.gwt.uibinder.elementparsers;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.uibinder.rebind.UiBinderWriter;
import com.google.gwt.uibinder.rebind.XMLElement;
import com.google.gwt.uibinder.rebind.XMLElement.Interpreter;
import com.google.gwt.user.client.ui.Image;

import java.util.HashSet;
import java.util.Set;

/**
 * Parses CustomButton widgets.
 */
public class CustomButtonParser implements ElementParser {

  private static final Set<String> faceNames = new HashSet<String>();
  private static final String IMAGE_CLASS = Image.class.getCanonicalName();

  static {
    faceNames.add("upFace");
    faceNames.add("downFace");
    faceNames.add("upHoveringFace");
    faceNames.add("downHoveringFace");
    faceNames.add("upDisabledFace");
    faceNames.add("downDisabledFace");
  }

  public void parse(final XMLElement elem, final String fieldName,
      JClassType type, final UiBinderWriter writer)
      throws UnableToCompleteException {

    /*
     * Parse children. Use an interpreter to leave text in place for
     * HasHTMLParser to find.
     */
    elem.consumeChildElements(new Interpreter<Boolean>() {
      public Boolean interpretElement(XMLElement child)
          throws UnableToCompleteException {
        // CustomButton can only contain Face elements.
        String ns = child.getNamespaceUri();
        String faceName = child.getLocalName();

        if (!ns.equals(elem.getNamespaceUri())) {
          writer.die(elem, "Invalid child namespace: %s", ns);
        }
        if (!faceNames.contains(faceName)) {
          writer.die(elem, "Invalid CustomButton face: %s:%s", ns, faceName);
        }

        HtmlInterpreter interpreter = HtmlInterpreter.newInterpreterForUiObject(
            writer, fieldName);
        String innerHtml = child.consumeInnerHtml(interpreter);
        if (innerHtml.length() > 0) {
          writer.addStatement("%s.%s().setHTML(%s);", fieldName,
              faceNameGetter(faceName), writer.declareTemplateCall(innerHtml,
                  fieldName));
        }

        if (child.hasAttribute("image")) {
          String image = child.consumeImageResourceAttribute("image");
          writer.addStatement("%s.%s().setImage(new %s(%s));", fieldName,
              faceNameGetter(faceName), IMAGE_CLASS, image);
        }
        return true; // We consumed it
      }
    });
  }

  private String faceNameGetter(String faceName) {
    return "get" + faceName.substring(0, 1).toUpperCase()
        + faceName.substring(1);
  }
}
