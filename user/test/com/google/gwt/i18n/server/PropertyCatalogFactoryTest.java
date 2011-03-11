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
package com.google.gwt.i18n.server;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * Unit test for {@link PropertyCatalogFactory}.
 */
public class PropertyCatalogFactoryTest extends MessageCatalogFactoryTestBase {

  public void testAlternateFormMessages() throws UnsupportedEncodingException,
      MessageProcessingException, IOException {
    byte[] bytes = run(AlternateFormMessages.class);
    assertResult(bytes, new String[] {
        "# Messages from " + AlternateFormMessages.class.getCanonicalName(),
        "# Source locale en",
        "",
        "# Description: Notification that you gave access to another person",
        "#   0 - arg0, Selector",
        PropertyCatalogFactory.SELECTOR_BOILERPLATE_1,
        PropertyCatalogFactory.SELECTOR_BOILERPLATE_2,
        "1=You gave them access to your profile",
        "1[FEMALE]=You gave her access to your profile",
        "1[MALE]=You gave him access to your profile",
        "",
        "# Description: Time until next meeting, either full or abbreviated",
        "#   0 - arg0, Selector",
        "#   1 - arg1, Plural Count, Example: 3",
        PropertyCatalogFactory.SELECTOR_BOILERPLATE_1,
        PropertyCatalogFactory.SELECTOR_BOILERPLATE_2,
        "2=Your next meeting is in {1} hours",
        "2[other|one]=Your next meeting is in one hour",
        "2[true|one]=Next meeting\\: 1 hr",
        "2[true|other]=Next meeting\\: {1} hrs",
        "",
        "# Description: Number of widgets you have",
        "#   0 - arg0, Plural Count, Example: 42",
        PropertyCatalogFactory.SELECTOR_BOILERPLATE_1,
        PropertyCatalogFactory.SELECTOR_BOILERPLATE_2,
        "3=You have {0} widgets",
        "3[one]=You have one widget",
    });
  }

  public void testMixedConstantsMessages() throws UnsupportedEncodingException,
      MessageProcessingException, IOException {
    byte[] bytes = run(MixedConstantsMessages.class);
    assertResult(bytes, new String[] {
        "# Messages from " + MixedConstantsMessages.class.getCanonicalName(),
        "# Source locale en_US",
        "",
        "# Description: Example string constant with single quote",
        "1=don't quote me",
        "",
        "# Description: Example integer constant",
        "2=42",
        "",
        "# Description: Example string map constant",
        PropertyCatalogFactory.STRINGMAP_BOILERPLATE_1,
        PropertyCatalogFactory.STRINGMAP_BOILERPLATE_2,
        "3=apple,orange,banana,pepper",
        "3[apple]=red",
        "3[banana]=yellow",
        "3[orange]=orange",
        "3[pepper]=green",
        "",
        "# Description: A message that tests MessageFormat-style quoting",
        "#   0 - arg0, Example: arbitrary",
        "4=don''t quote me '{'0'}' - {0}",
        "",
        "# Description: A message with a meaning",
        "# Meaning: the fruit",
        "5=orange",
    });
  }

  @Override
  protected MessageCatalogFactory getMessageCatalogFactory() {
    return new PropertyCatalogFactory();
  }
}
