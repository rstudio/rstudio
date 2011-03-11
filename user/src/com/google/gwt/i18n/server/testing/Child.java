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
package com.google.gwt.i18n.server.testing;

import com.google.gwt.i18n.client.LocalizableResource.GenerateKeys;
import com.google.gwt.safehtml.shared.SafeHtml;

import java.util.List;

/**
 * Child interface used for test.
 */
@GenerateKeys
public interface Child extends Parent {

  @DefaultMessage("{1} wants to sell their car")
  @AlternateMessage({
    "MALE", "{1} wants to sell his car",
    "FEMALE", "{1} wants to sell her car"
  })
  String gender(@Select Gender gender, String name);

  @Description("test of multiple selectors")
  @DefaultMessage("{1}, {2}, and {0} others liked their {3} messages")
  @AlternateMessage({
    "=0|other|other", "Nobody liked their {3} messages",
    "=0|other|FEMALE", "Nobody liked her {3} messages",
    "=0|other|MALE", "Nobody liked his {3} messages",
    "=0|one|other", "Nobody liked their message",
    "=0|one|FEMALE", "Nobody liked her message",
    "=0|one|MALE", "Nobody liked his message",
    "=1|other|other", "{1} liked their {3} messages",
    "=1|other|FEMALE", "{1} liked her {3} messages",
    "=1|other|MALE", "{1} liked his {3} messages",
    "=1|one|other", "{1} liked their message",
    "=1|one|FEMALE", "{1} liked her message",
    "=1|one|MALE", "{1} liked his message",
    "=2|other|other", "{1} and {2} liked their {3} messages",
    "=2|other|FEMALE", "{1} and {2} liked her {3} messages",
    "=2|other|MALE", "{1} and {2} liked his {3} messages",
    "=2|one|other", "{1} and {2} liked their message",
    "=2|one|FEMALE", "{1} and {2} liked her message",
    "=2|one|MALE", "{1} and {2} liked his message",
    "one|other|other", "{1}, {2}, and one other liked their {3} messages",
    "one|other|FEMALE", "{1}, {2}, and one other liked her {3} messages",
    "one|other|MALE", "{1}, {2}, and one other liked his {3} messages",
    "one|one|other", "{1}, {2}, and one other liked their message",
    "one|one|FEMALE", "{1}, {2}, and one other liked her message",
    "one|one|MALE", "{1}, {2}, and one other liked his message",
    "other|one|other", "{1}, {2}, and {0} others liked their message",
    "other|one|MALE", "{1}, {2}, and {0} others liked his message",
    "other|one|FEMALE", "{1}, {2}, and {0} others liked her message",
    "other|other|MALE", "{1}, {2}, and {0} others liked his {3} messages",
    "other|other|FEMALE", "{1}, {2}, and {0} others liked her {3} messages"
  })
  SafeHtml multiSelect(@PluralCount @Offset(2) List<String> names,
      String name1, String name2, @PluralCount int msgCount,
      @Select String gender);
}
