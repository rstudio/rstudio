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

package elemental.html;

import com.google.gwt.junit.client.GWTTestCase;

import static elemental.client.Browser.getWindow;

/**
 * Tests {@link Document}.
 */
public class DocumentTest extends GWTTestCase {
  @Override
  public String getModuleName() {
    return "elemental.Elemental";
  }

  /**
   * Tests {@link Document#write}.
   */
  public void testWrite() {
    final Window window = getWindow().open();
    final Document document = window.getDocument();
    document.write("<body>drink and drink and drink AND FIGHT</body>");
    assertTrue(document.getBody().getTextContent().indexOf("drink and drink and drink AND FIGHT") != -1);
  }
}
