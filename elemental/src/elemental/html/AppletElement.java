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
package elemental.html;
import elemental.dom.Element;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;

import java.util.Date;

/**
  * Obsolete
  */
public interface AppletElement extends Element {


  /**
    * This attribute is used to position the applet on the page relative to content that might flow around it. The HTML 4.01 specification defines values of bottom, left, middle, right, and top, whereas Microsoft and Netscape also might support <strong>absbottom</strong>, <strong>absmiddle</strong>, <strong>baseline</strong>, <strong>center</strong>, and <strong>texttop</strong>.
    */
  String getAlign();

  void setAlign(String arg);


  /**
    * This attribute causes a descriptive text alternate to be displayed on browsers that do not support Java. Page designers should also remember that content enclosed within the <code>&lt;applet&gt;</code> element may also be rendered as alternative text.
    */
  String getAlt();

  void setAlt(String arg);


  /**
    * This attribute refers to an archived or compressed version of the applet and its associated class files, which might help reduce download time.
    */
  String getArchive();

  void setArchive(String arg);


  /**
    * This attribute specifies the URL of the applet's class file to be loaded and executed. Applet filenames are identified by a .class filename extension. The URL specified by code might be relative to the <code>codebase</code> attribute.
    */
  String getCode();

  void setCode(String arg);

  String getCodeBase();

  void setCodeBase(String arg);


  /**
    * This attribute specifies the height, in pixels, that the applet needs.
    */
  String getHeight();

  void setHeight(String arg);


  /**
    * This attribute specifies additional horizontal space, in pixels, to be reserved on either side of the applet.
    */
  String getHspace();

  void setHspace(String arg);


  /**
    * This attribute assigns a name to the applet so that it can be identified by other resources; particularly scripts.
    */
  String getName();

  void setName(String arg);


  /**
    * This attribute specifies the URL of a serialized representation of an applet.
    */
  String getObject();

  void setObject(String arg);


  /**
    * This attribute specifies additional vertical space, in pixels, to be reserved above and below the applet.
    */
  String getVspace();

  void setVspace(String arg);


  /**
    * This attribute specifies in pixels the width that the applet needs.
    */
  String getWidth();

  void setWidth(String arg);
}
