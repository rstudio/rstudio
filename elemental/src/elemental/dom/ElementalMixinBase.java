/*
 * Copyright 2012 Google Inc.
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
package elemental.dom;
import elemental.svg.SVGAnimatedRect;
import elemental.svg.SVGAnimatedString;
import elemental.svg.SVGAnimatedTransformList;
import elemental.svg.SVGAnimatedBoolean;
import elemental.svg.SVGStringList;
import elemental.svg.SVGRect;
import elemental.css.CSSValue;
import elemental.svg.SVGAnimatedLength;
import elemental.events.EventListener;
import elemental.svg.SVGMatrix;
import elemental.svg.SVGElement;
import elemental.svg.SVGAnimatedPreserveAspectRatio;
import elemental.css.CSSStyleDeclaration;
import elemental.events.Event;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;

import java.util.Date;

/**
  * 
  */
public interface ElementalMixinBase {

  int getChildElementCount();

  SVGAnimatedString getAnimatedClassName();

  SVGAnimatedBoolean getExternalResourcesRequired();

  SVGElement getFarthestViewportElement();

  Element getFirstElementChild();

  SVGAnimatedLength getAnimatedHeight();

  SVGAnimatedString getAnimatedHref();

  Element getLastElementChild();

  SVGElement getNearestViewportElement();

  Element getNextElementSibling();

  SVGAnimatedPreserveAspectRatio getPreserveAspectRatio();

  Element getPreviousElementSibling();

  SVGStringList getRequiredExtensions();

  SVGStringList getRequiredFeatures();

  SVGAnimatedString getAnimatedResult();

  CSSStyleDeclaration getSvgStyle();

  SVGStringList getSystemLanguage();

  SVGAnimatedTransformList getAnimatedTransform();

  SVGAnimatedRect getViewBox();

  SVGAnimatedLength getAnimatedWidth();

  SVGAnimatedLength getAnimatedX();

  String getXmllang();

  void setXmllang(String arg);

  String getXmlspace();

  void setXmlspace(String arg);

  SVGAnimatedLength getAnimatedY();

  int getZoomAndPan();

  void setZoomAndPan(int arg);

  EventRemover addEventListener(String type, EventListener listener);

  EventRemover addEventListener(String type, EventListener listener, boolean useCapture);

  boolean dispatchEvent(Event event);

  void removeEventListener(String type, EventListener listener);

  void removeEventListener(String type, EventListener listener, boolean useCapture);

  SVGRect getBBox();

  SVGMatrix getCTM();

  SVGMatrix getScreenCTM();

  SVGMatrix getTransformToElement(SVGElement element);

  void beginElement();

  void beginElementAt(float offset);

  void endElement();

  void endElementAt(float offset);

  boolean hasExtension(String extension);

  Element querySelector(String selectors);

  NodeList querySelectorAll(String selectors);

  CSSValue getPresentationAttribute(String name);
}
