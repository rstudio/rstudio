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

  public void parse(XMLElement elem, String fieldName, JClassType type,
      UiBinderWriter writer) throws UnableToCompleteException {

    // Parse children.
    for (XMLElement child : elem.consumeChildElements()) {
      // CustomButton can only contain Face elements.
      String ns = child.getNamespaceUri();
      String faceName = child.getLocalName();

      if (!ns.equals(elem.getNamespaceUri())) {
        writer.die("In %s, invalid child namespace: %s", elem, ns);
      }
      if (!faceNames.contains(faceName)) {
        writer.die("In %s, invalid CustomButton face: %s:%s", elem, ns, faceName);
      }

      HtmlInterpreter interpreter = HtmlInterpreter.newInterpreterForUiObject(
          writer, fieldName);
      String innerHtml = child.consumeInnerHtml(interpreter).trim();
      if (innerHtml.length() > 0) {
        writer.addStatement("%s.%s().setHTML(\"%s\");", fieldName,
            faceNameGetter(faceName), innerHtml);
      }

      if (child.hasAttribute("image")) {
        String image = child.consumeAttribute("image");
        writer.addStatement("%s.%s().setImage(new %s(%s));",
            fieldName, faceNameGetter(faceName), IMAGE_CLASS, image);
      }
    }
  }

  private String faceNameGetter(String faceName) {
    return "get" + faceName.substring(0, 1).toUpperCase() + faceName.substring(1);
  }
}
