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
public interface MediaKeyError {

    static final int MEDIA_KEYERR_CLIENT = 2;

    static final int MEDIA_KEYERR_DOMAIN = 6;

    static final int MEDIA_KEYERR_HARDWARECHANGE = 5;

    static final int MEDIA_KEYERR_OUTPUT = 4;

    static final int MEDIA_KEYERR_SERVICE = 3;

    static final int MEDIA_KEYERR_UNKNOWN = 1;

  int getCode();
}
