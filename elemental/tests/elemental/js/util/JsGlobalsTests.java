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

package elemental.js.util;

import com.google.gwt.junit.client.GWTTestCase;
import static elemental.js.util.JsGlobals.*;

/**
 * Tests {@link JsGlobals}.
 */
public class JsGlobalsTests extends GWTTestCase {
  @Override
  public String getModuleName() {
    return "elemental.Elemental";
  }

  public void testBasicInvocations() {
    assertEquals("chicken dog", decodeURI("chicken%20dog"));
    assertEquals("dog chicken", decodeURIComponent("dog%20chicken"));
    assertEquals("chicken%20dog", encodeURI("chicken dog"));
    assertEquals("dog%20chicken", encodeURIComponent("dog chicken"));
    
    assertEquals(2.032, parseFloat("2.032 raincoats"), 0.0001);
    assertEquals(10, (int)parseInt("10 dogs"));
    
    assertTrue(isFinite(2.0));
    assertFalse(isFinite(parseFloat("butterbean")));
    
    assertFalse(isNaN(2.0));
    assertTrue(isNaN(parseFloat("greenbean")));
  }
}
