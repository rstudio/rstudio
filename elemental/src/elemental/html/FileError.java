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
  * Represents an error that occurs while using the <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/FileReader">FileReader</a></code>
 interface.
  */
public interface FileError {

  /**
    * The file operation was aborted, probably due to a call to the <code>FileReader</code> <code>abort()</code>&nbsp;method.
    */

    static final int ABORT_ERR = 3;

  /**
    * The file data cannot be accurately represented in a data URL.
    */

    static final int ENCODING_ERR = 5;

    static final int INVALID_MODIFICATION_ERR = 9;

    static final int INVALID_STATE_ERR = 7;

  /**
    * File not found.
    */

    static final int NOT_FOUND_ERR = 1;

  /**
    * File could not be read.
    */

    static final int NOT_READABLE_ERR = 4;

    static final int NO_MODIFICATION_ALLOWED_ERR = 6;

    static final int PATH_EXISTS_ERR = 12;

    static final int QUOTA_EXCEEDED_ERR = 10;

  /**
    * The file could not be accessed for security reasons.
    */

    static final int SECURITY_ERR = 2;

    static final int SYNTAX_ERR = 8;

    static final int TYPE_MISMATCH_ERR = 11;


  /**
    * The <a title="en/nsIDOMFileError#Error codes" rel="internal" href="https://developer.mozilla.org/en/nsIDOMFileError#Error_codes">error code</a>.
    */
  int getCode();
}
