/*
 * Copyright 2010 Google Inc.
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
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.uibinder.rebind.UiBinderWriter;
import com.google.gwt.uibinder.rebind.XMLElement;

/**
 * Custom parsing of Image widgets. Sets ImageResource via constructor, because
 * {@link com.google.gwt.user.client.ui.Image#setResource Image.setResource}
 * clobbers most setter values.
 */
public class ImageParser implements ElementParser {

  public void parse(XMLElement elem, String fieldName, JClassType type,
      UiBinderWriter writer) throws UnableToCompleteException {
    if (hasImageResourceConstructor(writer.getOracle(), type)) {
      String resource = elem.consumeImageResourceAttribute("resource");
      if (null != resource) {
        writer.setFieldInitializerAsConstructor(fieldName, resource);
      }
    }
  }

  private boolean hasImageResourceConstructor(TypeOracle typeOracle,
      JClassType type) {
    JType imageResourceType = typeOracle.findType(ImageResource.class.getName());
    return type.findConstructor(new JType[] {imageResourceType}) != null;
  }
}
