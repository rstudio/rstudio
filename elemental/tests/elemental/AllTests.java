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
package elemental;

import junit.framework.Test;
import junit.framework.TestSuite;

import elemental.client.BrowserTest;
import elemental.events.EventTargetTest;
import elemental.html.DocumentTest;
import elemental.html.ElementTest;
import elemental.html.WindowTest;
import elemental.js.testing.MockClockTest;
import elemental.js.util.ArrayTests;
import elemental.js.util.JsGlobalsTests;
import elemental.js.util.MapFromIntTests;
import elemental.js.util.MapFromStringTests;
import elemental.js.util.StringUtilTests;
import elemental.json.JsonUtilTest;
import elemental.util.TimerTest;

/**
 * Here lie all my tests.
 */
public class AllTests {
  public static Test suite() {
    final TestSuite suite = new TestSuite();

    // client
    suite.addTestSuite(BrowserTest.class);

    // events
    suite.addTestSuite(EventTargetTest.class);

    // html
    suite.addTestSuite(DocumentTest.class);
    suite.addTestSuite(ElementTest.class);
    suite.addTestSuite(WindowTest.class);

    //json
    suite.addTestSuite(JsonUtilTest.class);

    // util
    suite.addTestSuite(TimerTest.class);

    // js.testing
    suite.addTestSuite(MockClockTest.class);

    // js.util
    suite.addTestSuite(ArrayTests.class);
    suite.addTestSuite(JsGlobalsTests.class);
    suite.addTestSuite(MapFromStringTests.class);
    suite.addTestSuite(MapFromIntTests.class);
    suite.addTestSuite(StringUtilTests.class);

    return suite;
  }
}
