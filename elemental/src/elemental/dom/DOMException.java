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
package elemental.dom;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;

import java.util.Date;

/**
  * <p>The following are the <strong>DOMException</strong> codes:</p>
<table class="standard-table"> <thead> <tr> <th scope="col">Code</th> <th scope="col">'Abstract' Constant name</th> </tr> </thead> <tbody> <tr> <th colspan="2">Level 1</th> </tr> <tr> <td><code>1</code></td> <td><code>INDEX_SIZE_ERR</code></td> </tr> <tr> <td><code>2</code></td> <td><code>DOMSTRING_SIZE_ERR</code></td> </tr> <tr> <td><code>3</code></td> <td><code>HIERARCHY_REQUEST_ERR</code></td> </tr> <tr> <td><code>4</code></td> <td><code>WRONG_DOCUMENT_ERR</code></td> </tr> <tr> <td><code>5</code></td> <td><code>INVALID_CHARACTER_ERR</code></td> </tr> <tr> <td><code>6</code></td> <td><code>NO_DATA_ALLOWED_ERR</code></td> </tr> <tr> <td><code>7</code></td> <td><code>NO_MODIFICATION_ALLOWED_ERR</code></td> </tr> <tr> <td><code>8</code></td> <td><code>NOT_FOUND_ERR</code></td> </tr> <tr> <td><code>9</code></td> <td><code>NOT_SUPPORTED_ERR</code></td> </tr> <tr> <td><code>10</code></td> <td><code>INUSE_ATTRIBUTE_ERR</code></td> </tr> <tr> <th colspan="2">Level 2</th> </tr> <tr> <td><code>11</code></td> <td><code>INVALID_STATE_ERR</code></td> </tr> <tr> <td><code>12</code></td> <td><code>SYNTAX_ERR</code></td> </tr> <tr> <td><code>13</code></td> <td><code>INVALID_MODIFICATION_ERR</code></td> </tr> <tr> <td><code>14</code></td> <td><code>NAMESPACE_ERR</code></td> </tr> <tr> <td><code>15</code></td> <td><code>INVALID_ACCESS_ERR</code></td> </tr> <tr> <th colspan="2"><strong>Level 3</strong></th> </tr> <tr> <td><code>16</code></td> <td><code>VALIDATION_ERR</code></td> </tr> <tr> <td><code>17</code></td> <td><code>TYPE_MISMATCH_ERR</code></td> </tr> </tbody>
</table>
  */
public interface DOMException {

    static final int ABORT_ERR = 20;

    static final int DATA_CLONE_ERR = 25;

    static final int DOMSTRING_SIZE_ERR = 2;

    static final int HIERARCHY_REQUEST_ERR = 3;

    static final int INDEX_SIZE_ERR = 1;

    static final int INUSE_ATTRIBUTE_ERR = 10;

    static final int INVALID_ACCESS_ERR = 15;

    static final int INVALID_CHARACTER_ERR = 5;

    static final int INVALID_MODIFICATION_ERR = 13;

    static final int INVALID_NODE_TYPE_ERR = 24;

    static final int INVALID_STATE_ERR = 11;

    static final int NAMESPACE_ERR = 14;

    static final int NETWORK_ERR = 19;

    static final int NOT_FOUND_ERR = 8;

    static final int NOT_SUPPORTED_ERR = 9;

    static final int NO_DATA_ALLOWED_ERR = 6;

    static final int NO_MODIFICATION_ALLOWED_ERR = 7;

    static final int QUOTA_EXCEEDED_ERR = 22;

    static final int SECURITY_ERR = 18;

    static final int SYNTAX_ERR = 12;

    static final int TIMEOUT_ERR = 23;

    static final int TYPE_MISMATCH_ERR = 17;

    static final int URL_MISMATCH_ERR = 21;

    static final int VALIDATION_ERR = 16;

    static final int WRONG_DOCUMENT_ERR = 4;

  int getCode();

  String getMessage();

  String getName();
}
