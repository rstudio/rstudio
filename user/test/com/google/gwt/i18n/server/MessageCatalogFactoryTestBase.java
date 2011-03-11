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

import com.google.gwt.i18n.client.Constants;
import com.google.gwt.i18n.client.LocalizableResource;
import com.google.gwt.i18n.client.LocalizableResource.DefaultLocale;
import com.google.gwt.i18n.client.Messages;
import com.google.gwt.i18n.server.MessageCatalogFactory.Context;
import com.google.gwt.i18n.server.MessageCatalogFactory.Writer;
import com.google.gwt.i18n.server.impl.ReflectionMessageInterface;
import com.google.gwt.i18n.server.testing.MockMessageCatalogContext;
import com.google.gwt.i18n.shared.GwtLocaleFactory;
import com.google.gwt.safehtml.shared.SafeHtml;

import junit.framework.TestCase;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Map;

/**
 * Base class for tests of {@link MessageCatalogFactory} implementations.
 */
public abstract class MessageCatalogFactoryTestBase extends TestCase {

  // NOTE: ALL ARGUMENT NAMES ARE ARG0 ETC DUE TO CURRENT LIMITATIONS IN THE
  // REFLECTION-BASED IMPLEMENTATION

  /**
   * Test various messages with alternate forms.
   */
  public interface AlternateFormMessages extends Messages {
    @Description("Notification that you gave access to another person")
    @DefaultMessage("You gave them access to your profile")
    @AlternateMessage({
        "MALE", "You gave him access to your profile",
        "FEMALE", "You gave her access to your profile"})
    @Key("1")
    String genderSelect(@Select Gender arg0);

    @Description("Time until next meeting, either full or abbreviated")
    @DefaultMessage("Your next meeting is in {1} hours")
    @AlternateMessage({
        "true|one", "Next meeting: 1 hr",
        "other|one", "Your next meeting is in one hour",
        "true|other", "Next meeting: {1} hrs"
    })
    @Key("2")
    String nextMeeting(@Select boolean arg0,
        @PluralCount @Example("3") int arg1);

    @Description("Number of widgets you have")
    @DefaultMessage("You have {0} widgets")
    @AlternateMessage({"one", "You have one widget"})
    @Key("3")
    String onePlural(@PluralCount @Example("42") int arg0);
  }

  /**
   * Represents gender of a person.
   */
  public enum Gender {
    MALE, FEMALE, UNKNOWN,
  }

  /**
   * Test interface that mixes Messages and Constants together.
   */
  @DefaultLocale("en_US")
  public interface MixedConstantsMessages extends Messages, Constants {
    @Description("Example string constant with single quote")
    @DefaultStringValue("don't quote me")
    @Key("1")
    String constantSingleQuote();

    @Description("Example integer constant")
    @DefaultIntValue(42)
    @Key("2")
    int lifeTheUniverseAndEverything();

    @Description("Example string map constant")
    @DefaultStringMapValue({
      "apple", "red",
      "orange", "orange",
      "banana", "yellow",
      "pepper", "green"
    })
    @Key("3")
    Map<String, String> mapFruitsToColors();

    @Description("A message that tests MessageFormat-style quoting")
    @DefaultMessage("don''t quote me '{'0'}' - {0}")
    @Key("4")
    SafeHtml messageSingleQuote(@Example("arbitrary") String arg0);

    @Description("A message with a meaning")
    @Meaning("the fruit")
    @DefaultMessage("orange")
    @Key("5")
    String orange();
  }

  protected static BufferedReader getReader(byte[] bytes)
      throws UnsupportedEncodingException {
    return new BufferedReader(new InputStreamReader(
        new ByteArrayInputStream(bytes), "UTF8"));
  }

  protected final GwtLocaleFactory localeFactory = new GwtLocaleFactoryImpl();

  protected abstract MessageCatalogFactory getMessageCatalogFactory();

  protected byte[] run(Class<? extends LocalizableResource> clazz)
      throws MessageProcessingException, IOException {
    MessageCatalogFactory factory = getMessageCatalogFactory();
    MessageInterface msgIntf = new ReflectionMessageInterface(localeFactory,
        clazz);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    Context ctx = new MockMessageCatalogContext(baos);
    Writer writer = factory.getWriter(ctx,
        clazz.getCanonicalName());
    try {
      MessageInterfaceVisitor cv = writer.visitClass();
      msgIntf.accept(cv);
    } finally {
      writer.close();
    }
    return baos.toByteArray();
  }

  protected void assertResult(byte[] bytes, String... lines) throws IOException {
    String error = "Mismatch from:\n" + new String(bytes, "UTF-8");
    BufferedReader reader = getReader(bytes);
    int lineNum = 0;
    for (String line : lines) {
      assertEquals(error + " - line " + (++lineNum), line, reader.readLine());
    }
  }
}
