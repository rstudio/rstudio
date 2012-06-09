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
package elemental.ranges;
import elemental.dom.Node;
import elemental.html.ClientRect;
import elemental.html.ClientRectList;
import elemental.dom.DocumentFragment;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;

import java.util.Date;

/**
  * <p>The <code>Range</code> object represents a fragment of a document that can contain nodes and parts of text nodes in a given document.</p>
<p>A range can be created using the <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Document.createRange">Document.createRange</a></code>
&nbsp;method of the&nbsp;<code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Document">Document</a></code>
&nbsp;object. Range objects can also be retrieved by using the <code><a rel="internal" href="https://developer.mozilla.org/Article_not_found?uri=en/DOM/Selection.getRangeAt" class="new">Selection.getRangeAt</a></code>
&nbsp;method of the&nbsp;<code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Selection">Selection</a></code>
&nbsp;object.</p>
  */
public interface Range {

    static final int END_TO_END = 2;

    static final int END_TO_START = 3;

    static final int NODE_AFTER = 1;

    static final int NODE_BEFORE = 0;

    static final int NODE_BEFORE_AND_AFTER = 2;

    static final int NODE_INSIDE = 3;

    static final int START_TO_END = 1;

    static final int START_TO_START = 0;


  /**
    * Returns a&nbsp;<code>boolean</code>&nbsp;indicating whether the range's start and end points are at the same position.
    */
  boolean isCollapsed();


  /**
    * Returns the deepest&nbsp;<code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Node">Node</a></code>
&nbsp;that contains the startContainer and endContainer Nodes.
    */
  Node getCommonAncestorContainer();


  /**
    * Returns the&nbsp;<code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Node">Node</a></code>
&nbsp;within which the Range ends.
    */
  Node getEndContainer();


  /**
    * Returns a number representing where in the endContainer the Range ends.
    */
  int getEndOffset();


  /**
    * Returns the&nbsp;<code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Node">Node</a></code>
&nbsp;within which the Range starts.
    */
  Node getStartContainer();


  /**
    * Returns a number representing where in the startContainer the Range starts.
    */
  int getStartOffset();


  /**
    * Returns a&nbsp;<code><a rel="custom" href="https://developer.mozilla.org/en/DOM/DocumentFragment">DocumentFragment</a></code>
&nbsp;copying the nodes of a Range.
    */
  DocumentFragment cloneContents();


  /**
    * Returns a Range object with boundary points identical to the cloned Range.
    */
  Range cloneRange();


  /**
    * Collapses the Range to one of its boundary points.
    */
  void collapse(boolean toStart);


  /**
    * Returns a constant representing whether the&nbsp;<a title="en/DOM/Node" rel="internal" href="https://developer.mozilla.org/en/DOM/Node">Node</a>&nbsp;is before, after, inside, or surrounding the range.
    */
  short compareNode(Node refNode);


  /**
    * Returns -1, 0, or 1 indicating whether the point occurs before, inside, or after the range.
    */
  short comparePoint(Node refNode, int offset);


  /**
    * Returns a&nbsp;<code><a rel="custom" href="https://developer.mozilla.org/en/DOM/DocumentFragment">DocumentFragment</a></code>
&nbsp;created from a given string of code.
    */
  DocumentFragment createContextualFragment(String html);


  /**
    * Removes the contents of a Range from the <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Document">Document</a></code>
.
    */
  void deleteContents();


  /**
    * Releases Range from use to improve performance.
    */
  void detach();

  void expand(String unit);


  /**
    * Moves contents of a Range from the document tree into a&nbsp;<code><a rel="custom" href="https://developer.mozilla.org/en/DOM/DocumentFragment">DocumentFragment</a></code>
.
    */
  DocumentFragment extractContents();


  /**
    * Returns a <code><a rel="internal" href="https://developer.mozilla.org/Article_not_found?uri=en/DOM/ClientRect" class="new">ClientRect</a></code>
 object which bounds the entire contents of the range; this would be the union of all the rectangles returned by <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/range.getClientRects">range.getClientRects()</a></code>
.
    */
  ClientRect getBoundingClientRect();


  /**
    * Returns a list of <code><a rel="internal" href="https://developer.mozilla.org/Article_not_found?uri=en/DOM/ClientRect" class="new">ClientRect</a></code>
 objects that aggregates the results of <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Element.getClientRects">Element.getClientRects()</a></code>
 for all the elements in the range.
    */
  ClientRectList getClientRects();


  /**
    * Insert a&nbsp;<code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Node">Node</a></code>
&nbsp;at the start of a Range.
    */
  void insertNode(Node newNode);


  /**
    * Returns a <code>boolean</code> indicating whether the given node intersects the range.
    */
  boolean intersectsNode(Node refNode);


  /**
    * Returns a <code>boolean</code> indicating whether the given point is in the range.
    */
  boolean isPointInRange(Node refNode, int offset);


  /**
    * Sets the Range to contain the&nbsp;<code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Node">Node</a></code>
&nbsp;and its contents.
    */
  void selectNode(Node refNode);


  /**
    * Sets the Range to contain the contents of a&nbsp;<code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Node">Node</a></code>
.
    */
  void selectNodeContents(Node refNode);


  /**
    * Sets the end position of a Range.
    */
  void setEnd(Node refNode, int offset);


  /**
    * Sets the end position of a Range relative to another&nbsp;<code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Node">Node</a></code>
.
    */
  void setEndAfter(Node refNode);


  /**
    * Sets the end position of a Range relative to another&nbsp;<code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Node">Node</a></code>
.
    */
  void setEndBefore(Node refNode);


  /**
    * Sets the start position of a Range.
    */
  void setStart(Node refNode, int offset);


  /**
    * Sets the start position of a Range relative to another&nbsp;<code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Node">Node</a></code>
.
    */
  void setStartAfter(Node refNode);


  /**
    * Sets the start position of a Range relative to another&nbsp;<code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Node">Node</a></code>
.
    */
  void setStartBefore(Node refNode);


  /**
    * Moves content of a Range into a new&nbsp;<code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Node">Node</a></code>
.
    */
  void surroundContents(Node newParent);
}
