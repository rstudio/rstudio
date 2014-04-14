/*
 * Copyright 2011 Google Inc.
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
package com.google.gwt.dev.js;

/**
 * Tests the JsDuplicateCaseFolder optimizer.
 */
public class JsDuplicateCaseFolderTest extends OptimizerTestBase {

  public void test1() throws Exception {
    String input = "function a(x){switch(x){case 0:return 17;case 12:case 1:return 18;case 2:return 17;case 3:return 18;}}";
    String expected = "function a(x){switch(x){case 2:case 0:return 17;case 12:case 3:case 1:return 18;}}\n";
    check(expected, input);
  }

  public void test1b() throws Exception {
    String input = "function a(x){switch(x){case 0:return 17;case 12:case 1:return 18;default:return 17;case 3:return 18;}}";
    String expected = "function a(x){switch(x){default:case 0:return 17;case 12:case 3:case 1:return 18;}}\n";
    check(expected, input);
  }

  public void test2() throws Exception {
    // Don't coalesces cases 0 and 2 since 2 can be reached via fallthrough from 1
    String input = "function a(x){var y;switch(x){case 0:return 17;case 1:y=18;case 2:return 17;case 3:y=18;}return y}";
    String expected = "function a(x){var y;switch(x){case 0:return 17;case 1:y=18;case 2:return 17;case 3:y=18;}return y}\n";
    check(expected, input);
  }

  public void test3() throws Exception {
    // cases 1 and 3 can fall through, don't coalesce
    String input = "function a(x){var y;switch(x){case 0:return 17;case 1:y=18;break;case 2:return 17;case 3:y=18;break;}return y}";
    String expected = "function a(x){var y;switch(x){case 2:case 0:return 17;case 3:case 1:y=18;break;}return y}\n";
    check(expected, input);
  }

  public void test4() throws Exception {
    // cases 1 and 3 may be coalesced
    String input = "function a(x,z){var y;switch(x){case 0:return 17;case 1:if (z==0){y=18;break}else{y=19;break}case 2:return 17;case 3:if(z==0){y=18;break}else{y=19;break}}return y}";
    String expected = "function a(x,z){var y;switch(x){case 2:case 0:return 17;case 3:case 1:if(z==0){y=18;break}else{y=19;break}}return y}\n";
    check(expected, input);
  }

  public void test4b() throws Exception {
    // cases 1 and 3 may be coalesced
    // ensure additional fallthroughs are handled correctly
    String input = "function a(x,z){var y;switch(x){case 0:case 22:return 17;case 100:case 1:if (z==0){y=18;break}else{y=19;return 20}case 2:return 17;case 200:case 3:if(z==0){y=18;break}else{y=19;return 20}}return y}";
    String expected = "function a(x,z){var y;switch(x){case 0:case 2:case 22:return 17;case 100:case 1:if(z==0){y=18;break}else{y=19;return 20}case 200:case 3:if(z==0){y=18;break}else{y=19;return 20}}return y}\n";
    check(expected, input);
  }

  public void test5() throws Exception {
    // cases 1 and 3 can fall through due to no else clause, don't coalesce
    String input = "function a(x,z){var y;switch(x){case 0:return 17;case 1:if (z==0){y=18;break}else{y=19}case 2:return 17;case 3:if(z==0){y=18;break}else{y=19}}return y}";
    String expected = "function a(x,z){var y;switch(x){case 0:return 17;case 1:if(z==0){y=18;break}else{y=19}case 2:return 17;case 3:if(z==0){y=18;break}else{y=19}}return y}\n";
    check(expected, input);
  }

  public void test6() throws Exception {
    // cases 1 and 3 can fall through due to no else clause, don't coalesce
    String input = "function a(x,z){var y;switch(x){case 0:y=17;break;case 1:if(z==0){y=18;break}else{y=19}case 2:return 22;case 3:if(z==0){y=18;break}else{y=19}case 4:y=17;break;case 5:y=17;break;case 6:return 22;}return y}";
    String expected = "function a(x,z){var y;switch(x){case 0:y=17;break;case 1:if(z==0){y=18;break}else{y=19}case 6:case 2:return 22;case 3:if(z==0){y=18;break}else{y=19}case 5:case 4:y=17;break;}return y}\n";
    check(expected, input);
  }

  public void test6b() throws Exception {
    // cases 1 and 3 can fall through due to no else clause, don't coalesce
    String input = "function a(x,z){var y;switch(x){case 0:y=17;break;case 1:if(z==0){y=18;break}else{y=19}default:return 22;case 3:if(z==0){y=18;break}else{y=19}case 4:y=17;break;case 5:y=17;break;case 6:return 22;}return y}";
    String expected = "function a(x,z){var y;switch(x){case 0:y=17;break;case 1:if(z==0){y=18;break}else{y=19}case 6:default:return 22;case 3:if(z==0){y=18;break}else{y=19}case 5:case 4:y=17;break;}return y}\n";
    check(expected, input);
  }

  private void check(String expected, String input) throws Exception {
    // Pass the expected code through the parser to normalize it
    expected = super.optimize(expected, new Class[0]);
    String output = optimize(input);
    assertEquals(expected, output);
  }

  private String optimize(String js) throws Exception {
    return optimize(js, JsDuplicateCaseFolder.class);
  }
}
