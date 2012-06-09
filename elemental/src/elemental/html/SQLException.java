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
public interface SQLException {

    static final int CONSTRAINT_ERR = 6;

    static final int DATABASE_ERR = 1;

    static final int QUOTA_ERR = 4;

    static final int SYNTAX_ERR = 5;

    static final int TIMEOUT_ERR = 7;

    static final int TOO_LARGE_ERR = 3;

    static final int UNKNOWN_ERR = 0;

    static final int VERSION_ERR = 2;

  int getCode();

  String getMessage();
}
