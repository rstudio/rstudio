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
package elemental.xpath;
import elemental.dom.Node;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;

import java.util.Date;

/**
  * Refer to <code><a rel="custom" href="https://developer.mozilla.org/en/XPCOM_Interface_Reference/nsIDOMXPathResult">nsIDOMXPathResult</a></code>
 for more detail.
  */
public interface XPathResult {

  /**
    * A result set containing whatever type naturally results from evaluation of the expression. Note that if the result is a node-set then UNORDERED_NODE_ITERATOR_TYPE is always the resulting type.

    */

    static final int ANY_TYPE = 0;

  /**
    * A result node-set containing any single node that matches the expression. The node is not necessarily the first node in the document that matches the expression.

    */

    static final int ANY_UNORDERED_NODE_TYPE = 8;

  /**
    * A result containing a single boolean value. This is useful for example, in an XPath expression using the <code>not()</code> function.

    */

    static final int BOOLEAN_TYPE = 3;

  /**
    * A result node-set containing the first node in the document that matches the expression.

    */

    static final int FIRST_ORDERED_NODE_TYPE = 9;

  /**
    * A result containing a single number. This is useful for example, in an XPath expression using the <code>count()</code> function.

    */

    static final int NUMBER_TYPE = 1;

  /**
    * A result node-set containing all the nodes matching the expression. The nodes in the result set are in the same order that they appear in the document.

    */

    static final int ORDERED_NODE_ITERATOR_TYPE = 5;

  /**
    * A result node-set containing snapshots of all the nodes matching the expression. The nodes in the result set are in the same order that they appear in the document.

    */

    static final int ORDERED_NODE_SNAPSHOT_TYPE = 7;

  /**
    * A result containing a single string.

    */

    static final int STRING_TYPE = 2;

  /**
    * A result node-set containing all the nodes matching the expression. The nodes may not necessarily be in the same order that they appear in the document.

    */

    static final int UNORDERED_NODE_ITERATOR_TYPE = 4;

  /**
    * A result node-set containing snapshots of all the nodes matching the expression. The nodes may not necessarily be in the same order that they appear in the document.

    */

    static final int UNORDERED_NODE_SNAPSHOT_TYPE = 6;


  /**
    * readonly boolean
    */
  boolean isBooleanValue();


  /**
    * readonly boolean
    */
  boolean isInvalidIteratorState();


  /**
    * readonly float
    */
  double getNumberValue();


  /**
    * readonly integer (short)
    */
  int getResultType();


  /**
    * readonly Node
    */
  Node getSingleNodeValue();


  /**
    * readonly Integer
    */
  int getSnapshotLength();


  /**
    * readonly String
    */
  String getStringValue();

  Node iterateNext();

  Node snapshotItem(int index);
}
