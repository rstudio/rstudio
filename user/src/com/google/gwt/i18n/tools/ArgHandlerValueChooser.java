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
package com.google.gwt.i18n.tools;

import com.google.gwt.i18n.client.Constants;
import com.google.gwt.i18n.client.ConstantsWithLookup;
import com.google.gwt.i18n.client.Localizable;
import com.google.gwt.i18n.client.Messages;
import com.google.gwt.util.tools.ArgHandler;
import com.google.gwt.util.tools.ArgHandlerFlag;

/**
 * This class holds the '-createConstantsWithLookup' and '-createMessages'
 * ArgHandler classes.  It is shared by both I18NSync and I18NCreator classes.
 *
 * To use this class, call the getConstantsWithLookupArgHandler()
 * and getMessagesArgHandler() methods and add the returned ArgHandler
 * instances to a ToolBase registerHandler() method.  When parsing the arguments
 * is complete, you can retrieve the selected type by calling getArgValue().
 */
class ArgHandlerValueChooser {

  private Class<? extends Localizable> argValue = Constants.class;
  private ArgHandler cwlArgHandler;
  private ArgHandler messagesArgHandler;

  /**
   * Returns one on "Messages.class", "ConstantsWithLookup.class", or
   * "Constants.class" depending on which argument handlers fired.
   *
   * @return A class literal, returns "Constants.class" by default.
   */
  Class<? extends Localizable> getArgValue() {
    return argValue;
  }

  /**
   * Creates scripts for a ConstantsWithLookup interface rather than a Constants one.
   * 
   * @return a flag argument handler
   */
  ArgHandler getConstantsWithLookupArgHandler() {
    if (cwlArgHandler == null) {
      cwlArgHandler = new ArgHandlerFlag() {

        @Override
        public String getPurposeSnippet() {
          return "Create scripts for a ConstantsWithLookup interface "
              + "rather than a Constants one.";
        }

        @Override
        public String getLabel() {
          return "createConstantsWithLookup";
        }

        @Override
        public boolean setFlag(boolean value) {
          if (value && argValue == Messages.class) {
            System.err.println("-createMessages cannot be used with -createConstantsWithLookup");
            return false;
          }
          argValue = value ? ConstantsWithLookup.class : Constants.class;
          return true;
        }

        @Override
        public boolean getDefaultValue() {
          return argValue == ConstantsWithLookup.class;
        }
      };
    }
    return cwlArgHandler;
  }

  /**
   * Creates scripts for a Messages interface rather than a Constants one.
   * 
   * @return a flag argument handler
   */
  ArgHandler getMessagesArgHandler() {
    if (messagesArgHandler == null) {
      messagesArgHandler = new ArgHandlerFlag() {

        @Override
        public String getPurposeSnippet() {
          return "Create scripts for a Messages interface rather than a Constants one.";
        }

        @Override
        public String getLabel() {
          return "createMessages";
        }

        @Override
        public boolean setFlag(boolean value) {
          if (value && argValue == ConstantsWithLookup.class) {
            System.err.println("-createMessages cannot be used with -createConstantsWithLookup");
            return false;
          }
          argValue = value ? Messages.class : Constants.class;
          return true;
        }

        @Override
        public boolean getDefaultValue() {
          return argValue == Messages.class;
        }
      };
    }
    return messagesArgHandler;
  }
}
