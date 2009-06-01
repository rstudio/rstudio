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
package com.google.gwt.core.ext.util;

import com.google.gwt.dev.util.Util;

import junit.framework.TestCase;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

/**
 * Tests for some com.google.gwt.dev.util.Util methods.
 */
public class UtilTest extends TestCase {

  public UtilTest() {
  }

  public void testExpandXml() {
    String[] INPUTS = {
      "ab&yoohoo<ok",
      "ab&yoohoo<",
      "&yoohoo<ok",
      "ab&<ok",
      "ab&yoohoo<ok",
      "&",
      "\'",
      "\"",
      ">",
      "<"
    };
    
    String[] OUTPUTS = {
      "ab&amp;yoohoo&lt;ok",
      "ab&amp;yoohoo&lt;",
      "&amp;yoohoo&lt;ok",
      "ab&amp;&lt;ok",
      "ab&amp;yoohoo&lt;ok",
      "&amp;",
      "&apos;",
      "&quot;",
      "&gt;",
      "&lt;"
    };

    for (int i = 0; i < INPUTS.length; i++) {
      assertEquals(Util.escapeXml(INPUTS[i]), OUTPUTS[i]);
    }
  }
  
  private void testWriteUtf8(String input) throws IOException {
    // Built-in encoder
    ByteArrayOutputStream out1 = new ByteArrayOutputStream();
    Writer writer = new OutputStreamWriter(out1, "UTF-8");
    writer.write(input);
    writer.close();
    byte[] bytes1 = out1.toByteArray();
    
    // Util encoder
    StringBuilder builder = new StringBuilder();
    builder.append(input);
    ByteArrayOutputStream out2 = new ByteArrayOutputStream();
    Util.writeUtf8(builder, out2);
    byte[] bytes2 = out2.toByteArray();
    
    assertEquals(bytes1.length, bytes2.length);
    for (int i = 0; i < bytes1.length; i++) {
      assertEquals("input = " + input + " at byte " + i, bytes1[i], bytes2[i]);
    }
  }
  
  public void testWriteUtf8() {
    String[] INPUTS = {
        "灰色",
        "plain ascii text",
        "té×ţ ŵıŦƕ lŎts œf wƎiƦd ƇhÅrȿ",
        "ĝréý",
        "$ᄋme סf everりthınğ งฆฅฦศ ႳႹႩႨႵ ᄃᄥᄷᄨᎤᎸᎹᎺᴓ",
        "ᴄᴅᴈᴇἓἤἥἨὙΩ№℗℘←↑→↓✤✥✦✧�ﺕﺖꀕꀖꀗ逅逖逡遇違遝遭遷",
        "Surrogate pairs: 𝍠𐀁𐀵𐅇𐅋𐅊𝄐𝄑𝄢𝄫𝄞𝍤𝍦𝍨再善𯾰𯿩"
    };
    
    try {
      for (String input : INPUTS) {
        testWriteUtf8(input);
      }
    } catch (IOException e) {
      fail("Got IOException");
    }
  }
}
