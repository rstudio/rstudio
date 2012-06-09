/*
 * Copyright 2012 Google Inc.
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
import elemental.util.Indexable;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;

import java.util.Date;

/**
  * 
  */
public interface JavaScriptCallFrame {

    static final int CATCH_SCOPE = 4;

    static final int CLOSURE_SCOPE = 3;

    static final int GLOBAL_SCOPE = 0;

    static final int LOCAL_SCOPE = 1;

    static final int WITH_SCOPE = 2;

  JavaScriptCallFrame getCaller();

  int getColumn();

  String getFunctionName();

  int getLine();

  Indexable getScopeChain();

  int getSourceID();

  Object getThisObject();

  String getType();

  void evaluate(String script);

  int scopeType(int scopeIndex);
}
