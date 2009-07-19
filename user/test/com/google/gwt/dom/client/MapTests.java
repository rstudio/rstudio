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
package com.google.gwt.dom.client;

import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests the {@link MapElement} and {@link AreaElement} classes.
 */
public class MapTests extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.dom.DOMTest";
  }

  /**
   * getAreas.
   */
  public void testGetArea() {
    Document doc = Document.get();
    MapElement map = doc.createMapElement();
    AreaElement area0 = doc.createAreaElement();
    AreaElement area1 = doc.createAreaElement();
    AreaElement area2 = doc.createAreaElement();

    map.appendChild(area0);
    map.appendChild(area1);
    map.appendChild(area2);

    NodeList<AreaElement> areaElems = map.getAreas();
    assertEquals(3, areaElems.getLength());

    assertEquals(area0, areaElems.getItem(0));
    assertEquals(area1, areaElems.getItem(1));
    assertEquals(area2, areaElems.getItem(2));
  }
}
