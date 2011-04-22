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

import com.google.gwt.i18n.client.LocalizableResource;
import com.google.gwt.i18n.client.LocalizableResource.DefaultLocale;
import com.google.gwt.i18n.client.LocalizableResource.GenerateKeys;
import com.google.gwt.i18n.server.Message.AlternateFormMapping;
import com.google.gwt.i18n.server.MessageFormatUtils.MessageStyle;
import com.google.gwt.i18n.server.Type.ListType;
import com.google.gwt.i18n.server.impl.ReflectionMessageInterface;
import com.google.gwt.i18n.server.keygen.MD5KeyGenerator;
import com.google.gwt.i18n.server.testing.Child;
import com.google.gwt.i18n.shared.AlternateMessageSelector;
import com.google.gwt.i18n.shared.GwtLocale;
import com.google.gwt.i18n.shared.AlternateMessageSelector.AlternateForm;

import junit.framework.TestCase;

import java.lang.annotation.Documented;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Test base for testing {@link MessageInterface} implementations (and their
 * referenced pieces such as {@link Message}.
 */
public abstract class MessageInterfaceTestBase extends TestCase {

  private class TestMessageInterfaceVisitor extends DefaultVisitor
      implements MessageFormVisitor {

    private final FormVisitorDriver selectorTracker = new FormVisitorDriver();

    public void beginForm(int level, String formName)
        throws MessageProcessingException {
      switch (visitState) {
        case 5: // gender
          assertEquals(0, level);
          // note sorted order, not in-source order
          assertEquals("FEMALE", formName);
          break;
        case 8:
          assertEquals(0, level);
          assertEquals("MALE", formName);
          break;
        case 11:
          assertEquals(0, level);
          assertEquals(AlternateMessageSelector.OTHER_FORM_NAME, formName);
          break;
        case 19: // mutliSelect, first arg
          assertEquals(0, level);
          assertEquals("=0", formName);
          break;
        case 21: // mutliSelect, second arg
        case 51: // =1
        case 81: // =2
        case 111: // one
        case 141: // other
          assertEquals(1, level);
          assertEquals("one", formName);
          break;
        case 23: // mutliSelect, third arg
        case 36:
        case 53: // =1/one
        case 66:
        case 83: // =2/one
        case 96:
        case 113: // one/one
        case 126:
        case 143: // other/one
        case 156:
          assertEquals(2, level);
          assertEquals("FEMALE", formName);
          break;
        case 26: // mutliSelect, third arg
        case 39:
        case 56: // =1/one
        case 69:
        case 86: // =2/one
        case 99:
        case 116: // one/one
        case 129:
        case 146: // other/one
        case 159:
          assertEquals(2, level);
          assertEquals("MALE", formName);
          break;
        case 29: // mutliSelect, third arg
        case 42:
        case 59: // =1/one
        case 72:
        case 89: // =2/one
        case 102:
        case 119: // one/one
        case 132:
        case 149: // other/one
        case 162:
          assertEquals(2, level);
          assertEquals(AlternateMessageSelector.OTHER_FORM_NAME, formName);
          break;
        case 34: // mutliSelect, second arg
        case 64: // =1
        case 94: // =2
        case 124: // one
        case 154: // other
          assertEquals(1, level);
          assertEquals(AlternateMessageSelector.OTHER_FORM_NAME, formName);
          break;
        case 49: // mutliSelect, first arg
          assertEquals(0, level);
          assertEquals("=1", formName);
          break;
        case 79: // mutliSelect, first arg
          assertEquals(0, level);
          assertEquals("=2", formName);
          break;
        case 109: // mutliSelect, first arg
          assertEquals(0, level);
          assertEquals("one", formName);
          break;
        case 139: // mutliSelect, first arg
          assertEquals(0, level);
          assertEquals(AlternateMessageSelector.OTHER_FORM_NAME, formName);
          break;
        case 178: // inheritedMap
          assertEquals(0, level);
          assertEquals("k1", formName);
          break;
        case 181: // inheritedMap
          assertEquals(0, level);
          assertEquals("k2", formName);
          break;
        default:
          fail("Unexpected visitState " + visitState + ", level=" + level
              + ", form " + formName);
          break;
      }
      visitState++;
    }

    public void beginSelector(int level, Parameter selectorArg)
        throws MessageProcessingException {
      switch (visitState) {
        case 4: // gender, first arg
          assertEquals(0, level);
          assertEquals(0, selectorArg.getIndex());
          break;
        case 18: // multiSelect, first arg
          assertEquals(0, level);
          assertEquals(0, selectorArg.getIndex());
          break;
        case 20: // multiSelect, second arg
        case 50: // =1
        case 80: // =2
        case 110: // one
        case 140: // other
          assertEquals(1, level);
          assertEquals(3, selectorArg.getIndex());
          break;
        case 22: // multiSelect, third arg
        case 35:
        case 48:
        case 52: // =1/one
        case 65:
        case 78:
        case 82: // =2/one
        case 95:
        case 108:
        case 112: // one/one
        case 125:
        case 138:
        case 142: // other/one
        case 155:
        case 168:
          assertEquals(2, level);
          assertEquals(4, selectorArg.getIndex());
          break;
        case 177: // inheritedMap
          assertEquals(0, level);
          assertNull(selectorArg);
          break;
        default:
          fail("Unexpected visitState " + visitState + ": level=" + level
              + ", arg=" + selectorArg.getIndex());
          break;
      }
      visitState++;
    }

    public void endForm(int level, String formName) {
      switch (visitState) {
        case 7:
          assertEquals(0, level);
          assertEquals("FEMALE", formName);
          break;
        case 10:
          assertEquals(0, level);
          assertEquals("MALE", formName);
          break;
        case 13:
          assertEquals(0, level);
          assertEquals(AlternateMessageSelector.OTHER_FORM_NAME, formName);
          break;
        case 25: // mutliSelect, third arg
        case 38:
        case 55: // =1/one
        case 68:
        case 85: // =2/one
        case 98:
        case 115: // one/one
        case 128:
        case 145: // other/one
        case 158:
          assertEquals(2, level);
          assertEquals("FEMALE", formName);
          break;
        case 28: // mutliSelect, third arg
        case 41:
        case 58: // =1/one
        case 71:
        case 88: // =2/one
        case 101:
        case 118: // one/one
        case 131:
        case 148: // other/one
        case 161:
          assertEquals(2, level);
          assertEquals("MALE", formName);
          break;
        case 31: // mutliSelect, third arg
        case 44:
        case 61: // =1/one
        case 74:
        case 91: // =2/one
        case 104:
        case 121: // one/one
        case 134:
        case 151: // other/one
        case 164:
          assertEquals(2, level);
          assertEquals(AlternateMessageSelector.OTHER_FORM_NAME, formName);
          break;
        case 33: // mutliSelect, second arg
        case 63: // =1
        case 93: // =2
        case 123: // one
        case 153: // other
          assertEquals(1, level);
          assertEquals("one", formName);
          break;
        case 46: // mutliSelect, second arg
        case 76: // =1
        case 106: // =2
        case 136: // one
        case 166: // other
          assertEquals(1, level);
          assertEquals(AlternateMessageSelector.OTHER_FORM_NAME, formName);
          break;
        case 48: // mutliSelect, first arg
          assertEquals(0, level);
          assertEquals("=0", formName);
          break;
        case 78: // mutliSelect, first arg
          assertEquals(0, level);
          assertEquals("=1", formName);
          break;
        case 108: // mutliSelect, first arg
          assertEquals(0, level);
          assertEquals("=2", formName);
          break;
        case 138: // mutliSelect, first arg
          assertEquals(0, level);
          assertEquals("one", formName);
          break;
        case 168: // mutliSelect, first arg
          assertEquals(0, level);
          assertEquals(AlternateMessageSelector.OTHER_FORM_NAME, formName);
          break;
        case 180: // inheritedMap
          assertEquals(0, level);
          assertEquals("k1", formName);
          break;
        case 183: // inheritedMap
          assertEquals(0, level);
          assertEquals("k2", formName);
          break;
        default:
          fail("Unexpected visitState " + visitState + ", level=" + level
              + ", form " + formName);
          break;
      }
      visitState++;
    }

    @Override
    public void endMessage(Message msg, MessageTranslation trans)
        throws MessageProcessingException {
      selectorTracker.endMessage(msg);
      processDefaultMessageAfter(msg.getMessageStyle(),
          msg.getDefaultMessage());
    }

    @Override
    public void endMessageInterface(MessageInterface msgIntf)
        throws MessageProcessingException {
      // TODO Auto-generated method stub
    }

    public void endSelector(int level, Parameter selectorArg) {
      switch (visitState) {
        case 14:
          assertEquals(0, level);
          assertEquals(0, selectorArg.getIndex());
          break;
        case 32: // multiSelect, third arg
        case 45:
        case 58:
        case 62: // =1/one
        case 75:
        case 88:
        case 92: // =2/one
        case 105:
        case 118:
        case 122: // one/one
        case 135:
        case 148:
        case 152: // other/one
        case 165:
        case 178:
          assertEquals(2, level);
          assertEquals(4, selectorArg.getIndex());
          break;
        case 47: // multiSelect, second arg
        case 77: // =1
        case 107: // =2
        case 137: // one
        case 167: // other
          assertEquals(1, level);
          assertEquals(3, selectorArg.getIndex());
          break;
        case 169: // multiSelect, first arg
          assertEquals(0, level);
          assertEquals(0, selectorArg.getIndex());
          break;
        case 184: // inheritedMap
          assertEquals(0, level);
          assertNull(selectorArg);
          break;
        default:
          fail("Unexpected visitState " + visitState + ": level=" + level
              + ", arg=" + selectorArg.getIndex());
          break;
      }
      visitState++;
    }

    @Override
    public MessageVisitor visitMessage(Message msg, MessageTranslation trans)
        throws MessageProcessingException {
      selectorTracker.initialize(msg, this);

      // in this use, the translation should always just be the message
      assertSame(msg, trans);

      switch (visitState) {
        case 2:
          assertEquals("gender", msg.getMethodName());
          assertNull(msg.getDescription());
          assertEquals("0B47700B9670EA6CE190F3C12EC6CF76", msg.getKey());
          assertEquals("{1} wants to sell their car",
              msg.getDefaultMessage());
          assertEquals(Type.STRING, msg.getReturnType());
          List<Parameter> params = msg.getParameters();
          assertEquals(2, params.size());
          Parameter param = params.get(0);
          assertNotNull(param.getType().getEnumValues());
          assertParamNameEquals("gender", param);
          param = params.get(1);
          assertEquals(Type.STRING, param.getType());
          assertParamNameEquals("name", param);
          break;
        case 16:
          assertEquals("multiSelect", msg.getMethodName());
          assertEquals("test of multiple selectors", msg.getDescription());
          assertEquals("3213633E1B1DCFA944C788487B99A99D", msg.getKey());
          assertEquals("{1}, {2}, and {0} others liked their {3} messages",
              msg.getDefaultMessage());
          params = msg.getParameters();
          assertEquals(5, params.size());
          param = params.get(0);
          assertTrue("not a List", param.getType() instanceof ListType);
          assertParamNameEquals("names", param);
          param = params.get(1);
          assertEquals(Type.STRING, param.getType());
          assertParamNameEquals("name1", param);
          param = params.get(2);
          assertEquals(Type.STRING, param.getType());
          assertParamNameEquals("name2", param);
          param = params.get(3);
          assertEquals(Type.INT, param.getType());
          assertParamNameEquals("msgCount", param);
          param = params.get(4);
          assertEquals(Type.STRING, param.getType());
          assertParamNameEquals("gender", param);
          break;
        case 171:
          assertEquals("inheritedConstant", msg.getMethodName());
          assertNull(msg.getDescription());
          // note that @GenerateKeys is on Child
          assertEquals("inheritedConstant", msg.getKey());
          assertEquals("inherited", msg.getDefaultMessage());
          assertEquals(0, msg.getParameters().size());
          break;
        case 175:
          assertEquals("inheritedMap", msg.getMethodName());
          assertNull(msg.getDescription());
          // note that @GenerateKeys is on Child
          assertEquals("inheritedMap", msg.getKey());
          assertEquals("k1,k2", msg.getDefaultMessage());
          assertEquals(0, msg.getParameters().size());
          break;
        case 186:
          assertEquals("inheritedMessage", msg.getMethodName());
          assertNull(msg.getDescription());
          // note that @GenerateKeys is on Child
          assertEquals("inheritedMessage", msg.getKey());
          assertEquals("inherited", msg.getDefaultMessage());
          assertEquals(0, msg.getParameters().size());
          break;
        default:
          fail("Unexpected visit state " + visitState + ", method="
              + msg.getMethodName() + ", desc=" + msg.getDescription()
              + ", key=" + msg.getKey() + ", def=\"" + msg.getDefaultMessage()
              + "\"");
          break;
      }
      visitState++;
      processDefaultMessageBefore(msg.getMessageStyle(),
          msg.getDefaultMessage());
      return this;
    }

    @Override
    public void visitMessageInterface(MessageInterface msgIntf, GwtLocale sourceLocale)
        throws MessageProcessingException {
      assertEquals(1, visitState++);
      assertSame(MessageInterfaceTestBase.this.msgIntf, msgIntf);
      assertEquals("en_US", sourceLocale.toString());
    }

    @Override
    public void visitTranslation(String[] formNames, boolean isDefault,
        MessageStyle style, String msg) throws MessageProcessingException {
      selectorTracker.visitForms(formNames);
      boolean shouldBeDefault = true;
      for (String form : formNames) {
        if (!AlternateMessageSelector.OTHER_FORM_NAME.equals(form)) {
          shouldBeDefault = false;
          break;
        }
      }
      assertEquals(shouldBeDefault, isDefault);
      MessageStyle expectedStyle = MessageStyle.MESSAGE_FORMAT;
      switch (visitState) {
        case 6:
          assertEquals(1, formNames.length);
          assertEquals("FEMALE", formNames[0]);
          assertFalse(isDefault);
          assertEquals("{1} wants to sell her car", msg);
          break;
        case 9:
          assertEquals(1, formNames.length);
          assertEquals("MALE", formNames[0]);
          assertFalse(isDefault);
          assertEquals("{1} wants to sell his car", msg);
          break;
        case 12:
          assertEquals(1, formNames.length);
          assertEquals(AlternateMessageSelector.OTHER_FORM_NAME, formNames[0]);
          assertTrue(isDefault);
          assertEquals("{1} wants to sell their car", msg);
          break;
        case 24:
          assertEquals(3, formNames.length);
          assertEquals("=0", formNames[0]);
          assertEquals("one", formNames[1]);
          assertEquals("FEMALE", formNames[2]);
          assertEquals("Nobody liked her message", msg);
          break;
        case 27:
          assertEquals(3, formNames.length);
          assertEquals("=0", formNames[0]);
          assertEquals("one", formNames[1]);
          assertEquals("MALE", formNames[2]);
          assertEquals("Nobody liked his message", msg);
          break;
        case 30:
          assertEquals(3, formNames.length);
          assertEquals("=0", formNames[0]);
          assertEquals("one", formNames[1]);
          assertEquals(AlternateMessageSelector.OTHER_FORM_NAME, formNames[2]);
          assertEquals("Nobody liked their message", msg);
          break;
        case 37:
          assertEquals(3, formNames.length);
          assertEquals("=0", formNames[0]);
          assertEquals(AlternateMessageSelector.OTHER_FORM_NAME, formNames[1]);
          assertEquals("FEMALE", formNames[2]);
          assertEquals("Nobody liked her {3} messages", msg);
          break;
        case 40:
          assertEquals(3, formNames.length);
          assertEquals("=0", formNames[0]);
          assertEquals(AlternateMessageSelector.OTHER_FORM_NAME, formNames[1]);
          assertEquals("MALE", formNames[2]);
          assertEquals("Nobody liked his {3} messages", msg);
          break;
        case 43:
          assertEquals(3, formNames.length);
          assertEquals("=0", formNames[0]);
          assertEquals(AlternateMessageSelector.OTHER_FORM_NAME, formNames[1]);
          assertEquals(AlternateMessageSelector.OTHER_FORM_NAME, formNames[2]);
          assertEquals("Nobody liked their {3} messages", msg);
          break;
        case 54:
          assertEquals(3, formNames.length);
          assertEquals("=1", formNames[0]);
          assertEquals("one", formNames[1]);
          assertEquals("FEMALE", formNames[2]);
          assertEquals("{1} liked her message", msg);
          break;
        case 57:
          assertEquals(3, formNames.length);
          assertEquals("=1", formNames[0]);
          assertEquals("one", formNames[1]);
          assertEquals("MALE", formNames[2]);
          assertEquals("{1} liked his message", msg);
          break;
        case 60:
          assertEquals(3, formNames.length);
          assertEquals("=1", formNames[0]);
          assertEquals("one", formNames[1]);
          assertEquals(AlternateMessageSelector.OTHER_FORM_NAME, formNames[2]);
          assertEquals("{1} liked their message", msg);
          break;
        case 67:
          assertEquals(3, formNames.length);
          assertEquals("=1", formNames[0]);
          assertEquals(AlternateMessageSelector.OTHER_FORM_NAME, formNames[1]);
          assertEquals("FEMALE", formNames[2]);
          assertEquals("{1} liked her {3} messages", msg);
          break;
        case 70:
          assertEquals(3, formNames.length);
          assertEquals("=1", formNames[0]);
          assertEquals(AlternateMessageSelector.OTHER_FORM_NAME, formNames[1]);
          assertEquals("MALE", formNames[2]);
          assertEquals("{1} liked his {3} messages", msg);
          break;
        case 73:
          assertEquals(3, formNames.length);
          assertEquals("=1", formNames[0]);
          assertEquals(AlternateMessageSelector.OTHER_FORM_NAME, formNames[1]);
          assertEquals(AlternateMessageSelector.OTHER_FORM_NAME, formNames[2]);
          assertEquals("{1} liked their {3} messages", msg);
          break;
        case 84:
          assertEquals(3, formNames.length);
          assertEquals("=2", formNames[0]);
          assertEquals("one", formNames[1]);
          assertEquals("FEMALE", formNames[2]);
          assertEquals("{1} and {2} liked her message", msg);
          break;
        case 87:
          assertEquals(3, formNames.length);
          assertEquals("=2", formNames[0]);
          assertEquals("one", formNames[1]);
          assertEquals("MALE", formNames[2]);
          assertEquals("{1} and {2} liked his message", msg);
          break;
        case 90:
          assertEquals(3, formNames.length);
          assertEquals("=2", formNames[0]);
          assertEquals("one", formNames[1]);
          assertEquals(AlternateMessageSelector.OTHER_FORM_NAME, formNames[2]);
          assertEquals("{1} and {2} liked their message", msg);
          break;
        case 97:
          assertEquals(3, formNames.length);
          assertEquals("=2", formNames[0]);
          assertEquals(AlternateMessageSelector.OTHER_FORM_NAME, formNames[1]);
          assertEquals("FEMALE", formNames[2]);
          assertEquals("{1} and {2} liked her {3} messages", msg);
          break;
        case 100:
          assertEquals(3, formNames.length);
          assertEquals("=2", formNames[0]);
          assertEquals(AlternateMessageSelector.OTHER_FORM_NAME, formNames[1]);
          assertEquals("MALE", formNames[2]);
          assertEquals("{1} and {2} liked his {3} messages", msg);
          break;
        case 103:
          assertEquals(3, formNames.length);
          assertEquals("=2", formNames[0]);
          assertEquals(AlternateMessageSelector.OTHER_FORM_NAME, formNames[1]);
          assertEquals(AlternateMessageSelector.OTHER_FORM_NAME, formNames[2]);
          assertEquals("{1} and {2} liked their {3} messages", msg);
          break;
        case 114:
          assertEquals(3, formNames.length);
          assertEquals("one", formNames[0]);
          assertEquals("one", formNames[1]);
          assertEquals("FEMALE", formNames[2]);
          assertEquals("{1}, {2}, and one other liked her message", msg);
          break;
        case 117:
          assertEquals(3, formNames.length);
          assertEquals("one", formNames[0]);
          assertEquals("one", formNames[1]);
          assertEquals("MALE", formNames[2]);
          assertEquals("{1}, {2}, and one other liked his message", msg);
          break;
        case 120:
          assertEquals(3, formNames.length);
          assertEquals("one", formNames[0]);
          assertEquals("one", formNames[1]);
          assertEquals(AlternateMessageSelector.OTHER_FORM_NAME, formNames[2]);
          assertEquals("{1}, {2}, and one other liked their message", msg);
          break;
        case 127:
          assertEquals(3, formNames.length);
          assertEquals("one", formNames[0]);
          assertEquals(AlternateMessageSelector.OTHER_FORM_NAME, formNames[1]);
          assertEquals("FEMALE", formNames[2]);
          assertEquals("{1}, {2}, and one other liked her {3} messages", msg);
          break;
        case 130:
          assertEquals("one", formNames[0]);
          assertEquals(AlternateMessageSelector.OTHER_FORM_NAME, formNames[1]);
          assertEquals("MALE", formNames[2]);
          assertEquals("{1}, {2}, and one other liked his {3} messages", msg);
          break;
        case 133:
          assertEquals(3, formNames.length);
          assertEquals("one", formNames[0]);
          assertEquals(AlternateMessageSelector.OTHER_FORM_NAME, formNames[1]);
          assertEquals(AlternateMessageSelector.OTHER_FORM_NAME, formNames[2]);
          assertEquals("{1}, {2}, and one other liked their {3} messages", msg);
          break;
        case 144:
          assertEquals(3, formNames.length);
          assertEquals(AlternateMessageSelector.OTHER_FORM_NAME, formNames[0]);
          assertEquals("one", formNames[1]);
          assertEquals("FEMALE", formNames[2]);
          assertEquals("{1}, {2}, and {0} others liked her message", msg);
          break;
        case 147:
          assertEquals(3, formNames.length);
          assertEquals(AlternateMessageSelector.OTHER_FORM_NAME, formNames[0]);
          assertEquals("one", formNames[1]);
          assertEquals("MALE", formNames[2]);
          assertEquals("{1}, {2}, and {0} others liked his message", msg);
          break;
        case 150:
          assertEquals(3, formNames.length);
          assertEquals(AlternateMessageSelector.OTHER_FORM_NAME, formNames[0]);
          assertEquals("one", formNames[1]);
          assertEquals(AlternateMessageSelector.OTHER_FORM_NAME, formNames[2]);
          assertEquals("{1}, {2}, and {0} others liked their message", msg);
          break;
        case 157:
          assertEquals(3, formNames.length);
          assertEquals(AlternateMessageSelector.OTHER_FORM_NAME, formNames[0]);
          assertEquals(AlternateMessageSelector.OTHER_FORM_NAME, formNames[1]);
          assertEquals("FEMALE", formNames[2]);
          assertEquals("{1}, {2}, and {0} others liked her {3} messages", msg);
          break;
        case 160:
          assertEquals(AlternateMessageSelector.OTHER_FORM_NAME, formNames[0]);
          assertEquals(AlternateMessageSelector.OTHER_FORM_NAME, formNames[1]);
          assertEquals("MALE", formNames[2]);
          assertEquals("{1}, {2}, and {0} others liked his {3} messages", msg);
          break;
        case 163:
          assertEquals(3, formNames.length);
          assertEquals(AlternateMessageSelector.OTHER_FORM_NAME, formNames[0]);
          assertEquals(AlternateMessageSelector.OTHER_FORM_NAME, formNames[1]);
          assertEquals(AlternateMessageSelector.OTHER_FORM_NAME, formNames[2]);
          assertEquals("{1}, {2}, and {0} others liked their {3} messages", msg);
          break;
        case 173:
          assertEquals(0, formNames.length);
          assertEquals("inherited", msg);
          expectedStyle = MessageStyle.PLAIN;
          break;
        case 179:
          assertEquals(1, formNames.length);
          assertEquals("k1", formNames[0]);
          assertEquals("v1", msg);
          expectedStyle = MessageStyle.PLAIN;
          break;
        case 182:
          assertEquals(1, formNames.length);
          assertEquals("k2", formNames[0]);
          assertEquals("v2", msg);
          expectedStyle = MessageStyle.PLAIN;
          break;
        case 188:
          assertEquals(0, formNames.length);
          assertEquals("inherited", msg);
          expectedStyle = MessageStyle.MESSAGE_FORMAT;
          break;
        default:
          fail("Unexpected visitState " + visitState + ", forms="
              + Arrays.deepToString(formNames) + ", msg=\"" + msg + "\"");
          break;
      }
      assertEquals(expectedStyle, style);
      visitState++;
    }

    private void assertParamNameEquals(String expected, Parameter param) {
      // TODO(jat): enable the following test when source lookup is added
      if (!(msgIntf instanceof ReflectionMessageInterface)) {
        assertEquals(expected, param.getName());
      }
    }

    private void processDefaultMessageAfter(MessageStyle style, String msg) {
      switch (visitState) {
        case 15:
          assertEquals(MessageStyle.MESSAGE_FORMAT, style);
          assertEquals("{1} wants to sell their car", msg);
          break;
        case 170:
          assertEquals(MessageStyle.MESSAGE_FORMAT, style);
          assertEquals("{1}, {2}, and {0} others liked their {3} messages",
              msg);
          break;
        case 174:
          assertEquals(MessageStyle.PLAIN, style);
          assertEquals("inherited", msg);
          break;
        case 185:
          assertEquals(MessageStyle.PLAIN, style);
          assertEquals("k1,k2", msg);
          break;
        case 189:
          assertEquals(MessageStyle.MESSAGE_FORMAT, style);
          assertEquals("inherited", msg);
          break;
        default:
          fail("Unexpected visitState " + visitState);
          break;
      }
      visitState++;
    }

    private void processDefaultMessageBefore(MessageStyle style, String msg) {
      switch (visitState) {
        case 3:
          assertEquals(MessageStyle.MESSAGE_FORMAT, style);
          assertEquals("{1} wants to sell their car", msg);
          break;
        case 17:
          assertEquals(MessageStyle.MESSAGE_FORMAT, style);
          assertEquals("{1}, {2}, and {0} others liked their {3} messages",
              msg);
          break;
        case 172:
          assertEquals(MessageStyle.PLAIN, style);
          assertEquals("inherited", msg);
          break;
        case 176:
          assertEquals(MessageStyle.PLAIN, style);
          assertEquals("k1,k2", msg);
          break;
        case 187:
          assertEquals(MessageStyle.MESSAGE_FORMAT, style);
          assertEquals("inherited", msg);
          break;
        default:
          fail("Unexpected visitState " + visitState);
          break;
      }
      visitState++;
    }
  }

  protected static final Class<? extends LocalizableResource> TEST_CLASS
      = Child.class;

  protected final MessageInterface msgIntf;
  protected int visitState = 1;

  protected MessageInterfaceTestBase(MessageInterface msgIntf) {
    this.msgIntf = msgIntf;
  }

  public void testAccept() throws MessageProcessingException {
    msgIntf.accept(new TestMessageInterfaceVisitor());
  }

  public void testGetAnnotation() {
    DefaultLocale defLocale = msgIntf.getAnnotation(DefaultLocale.class);
    assertNotNull(defLocale);
    assertEquals("en-US", defLocale.value());
    GenerateKeys generate = msgIntf.getAnnotation(GenerateKeys.class);
    assertNotNull(generate);
    assertEquals(MD5KeyGenerator.class.getName(), generate.value());
    assertNull(msgIntf.getAnnotation(Documented.class));
  }

  public void testGetClassName() {
    assertEquals(TEST_CLASS.getSimpleName(), msgIntf.getClassName());
  }

  public void testGetMessages() throws MessageProcessingException {
    if (!(msgIntf instanceof AbstractMessageInterface)) {
      return;
    }
    Iterable<Message> messages = ((AbstractMessageInterface)
        msgIntf).getMessages();
    Iterator<Message> msgIter = messages.iterator();
    assertTrue(msgIter.hasNext());
    Message msg = msgIter.next();
    assertEquals("gender", msg.getMethodName());
    Iterable<AlternateFormMapping> altMsgforms = msg.getAllMessageForms();
    Iterator<AlternateFormMapping> formIter = altMsgforms.iterator();
    assertTrue(formIter.hasNext());
    AlternateFormMapping mapping = formIter.next();
    List<AlternateForm> forms = mapping.getForms();
    assertEquals(1, forms.size());
    assertEquals("FEMALE", forms.get(0).getName());
    assertTrue(formIter.hasNext());
    mapping = formIter.next();
    forms = mapping.getForms();
    assertEquals(1, forms.size());
    assertEquals("MALE", forms.get(0).getName());
    assertTrue(formIter.hasNext());
    mapping = formIter.next();
    forms = mapping.getForms();
    assertEquals(1, forms.size());
    assertEquals(AlternateMessageSelector.OTHER_FORM_NAME, forms.get(0).getName());
    assertFalse(formIter.hasNext());
  }

  public void testGetPackageName() {
    assertEquals(TEST_CLASS.getPackage().getName(), msgIntf.getPackageName());
  }

  public void testGetQualifiedName() {
    assertEquals(TEST_CLASS.getCanonicalName(), msgIntf.getQualifiedName());
  }

  public void testIsAnnotationPresent() {
    assertTrue(msgIntf.isAnnotationPresent(DefaultLocale.class));
    assertTrue(msgIntf.isAnnotationPresent(GenerateKeys.class));
    assertFalse(msgIntf.isAnnotationPresent(Documented.class));
  }

  @Override
  protected void setUp() throws Exception {
    // TODO Auto-generated method stub
    super.setUp();
  }
}
