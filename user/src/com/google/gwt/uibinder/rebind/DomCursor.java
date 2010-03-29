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
package com.google.gwt.uibinder.rebind;

import com.google.gwt.core.ext.UnableToCompleteException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

/**
 * DOM Cursor keeps track of a given path in the DOM.  This is useful for
 * plucking out nodes from a DOM tree.
 */
public class DomCursor {
  
  private static class PathComponent {    
    
    private int childIndex;
    private boolean isTableWithoutTbody;
    
    public PathComponent(XMLElement element) {
      this.childIndex = 0;
      this.isTableWithoutTbody = "table".equalsIgnoreCase(element.getLocalName());
    }
    
    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (!(obj instanceof PathComponent)) return false;
      PathComponent other = (PathComponent) obj;      
      if (childIndex != other.childIndex) return false;
      if (isTableWithoutTbody != other.isTableWithoutTbody) return false;
      return true;
    }
    
    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;      
      result = prime * result + childIndex;
      result = prime * result + (isTableWithoutTbody ? 1231 : 1237);
      return result;
    }
    
    public void incrementIndex() {
      childIndex++;
    }    
  }
  
  private class ParagraphTracking {
    private LinkedList<Boolean> paragraphUnSafeNodes = new LinkedList<Boolean>();
    
    public void beginUnsafeTag() {
      paragraphUnSafeNodes.addLast(true);
    }
    
    public void endUnsafeTag() {
      paragraphUnSafeNodes.removeLast();
    }
    
    public boolean isSafeForField() {
      return paragraphUnSafeNodes.isEmpty();
    }
  }
  
  // Tags that cause P tags to end weirdly violating XHTML 
  // http://dev.w3.org/html5/spec/Overview.html#parsing-main-inbody
  private static final Set<String> PARAGRAPH_UNSAFE_NODES = new HashSet<String>(Arrays.asList(
    new String[] {"address", "article", "aside", "blockquote", "center", "details", "dir", "div", 
    "dl", "fieldset", "figure", "footer", "header", "hgroup", "menu", "nav", "ol", "p", "section",
    "ul"}));
  private static final Set<String> TABLE_SECTIONS = new HashSet<String>(Arrays.asList(new String[] {
    "thead", "tbody", "tfoot"}));
  private static final String NON_TEXT_LOOKUP_METHOD = "UiBinderUtil.getNonTextChild";
  private static final String STANDARD_LOOKUP_METHOD = "UiBinderUtil.getChild";
  private static final String TABLE_NON_TEXT_LOOKUP_METHOD = "UiBinderUtil.getTableChild";

  // Cache of path to a local variable that contains that.
  private Map<LinkedList<PathComponent>, String> domPathCache = 
    new HashMap<LinkedList<PathComponent>, String>();
  private LinkedList<ParagraphTracking> paragraphs = new LinkedList<ParagraphTracking>();
  private final String parent;
  private LinkedList<PathComponent> pathComponents = new LinkedList<PathComponent>();
  private boolean preserveWhitespaceNodes = true;
  private boolean walkingTextChildNodes = false;
  private final UiBinderWriter writer;
  
  public DomCursor(String parent, UiBinderWriter writer) {
    this.parent = parent;   
    this.writer = writer;
  }
  
  public void advanceChild() {
    pathComponents.getLast().incrementIndex();
    writer.addInitComment("advance DomCursor %s", this.toString());
  }
  
  public void advanceChildForWhitespaceText() {
    if (preserveWhitespaceNodes) {
      advanceTextChild();
    }
  }
  
  public void advanceTextChild() {
    if (walkingTextChildNodes) {
      advanceChild();
    }
  }
  
  /**
   * Finish visiting this subtree of the DOM.
   */
  public void finishChild(XMLElement elem) {
    if (!writer.getMessages().isMessage(elem) &&
        !isPlaceholderElement(elem)) {
      pathComponents.removeLast();
      writer.addInitComment("finish DomCursor %s", this.toString());
      String tag = elem.getLocalName();
      if ("p".equalsIgnoreCase(tag)) {
        endParagraph();
      }
      if (isInsideParagraph() && isUnsafeParagraphTag(tag)) {
        paragraphs.getLast().endUnsafeTag();        
      }      
    }
  }
  

  public String getAccessExpression() throws UnableToCompleteException {
    return getAccessExpression(null, null);
  }
  
  /**
   * Returns a Java expression for referencing the given node.
   * 
   * @param localVar Optional variable that will be used to cache the result of the expression.
   *        If there is no applicable local variable, callers can pass null.
   *
   * @param tag Optional tag name of the tag that would be referenced.  This can be null.
   * @return Java access expression.
   * @throws UnableToCompleteException 
   */
  public String getAccessExpression(String localVar, String tag) 
      throws UnableToCompleteException {
    // P tags cause all sorts of problems with hierarchy as the HTML spec has lots of weird
    // semantics for block elements inside of P tags.
    if (!safeForExpression()) {
      writer.die("UiBinder no longer allows certain addressable  " +
            "elements inside of <p> tags because of browser " +
            "inconsistency, consider using DIV instead");
    }
    
    String result = getDomWalkAccessExpression(tag);
    if (localVar != null) {
      domPathCache.put(new LinkedList<PathComponent>(pathComponents), localVar);
      return result;
    }
    
    String varName = "intermediate" + writer.getUniqueId();
    writer.addInitStatement("com.google.gwt.dom.client.Node %s = %s;", varName, result); 
    domPathCache.put(new LinkedList<PathComponent>(pathComponents), varName);
    return varName;
  }
  
  /**
   * Visit a child subtree of the DOM.
   * @throws UnableToCompleteException 
   */
  public void visitChild(XMLElement elem) throws UnableToCompleteException {
    if (!writer.getMessages().isMessage(elem) &&
        !isPlaceholderElement(elem)) {
      // If we do see an actual tbody in the uibinder, we should stop trying to account for the
      // automatic one that the browser will insert.
      String tag = elem.getLocalName();
      if ("tbody".equalsIgnoreCase(tag)) {
        pathComponents.getLast().isTableWithoutTbody = false;
      }
      if ("td".equalsIgnoreCase(tag) && !"tr".equalsIgnoreCase(elem.getParent().getLocalName())) {
        writer.die("TD tags must be inside of a TR tag");
      }
      pathComponents.addLast(new PathComponent(elem));
      writer.addInitComment("visit DomCursor %s", this.toString());
      
      if (isInsideParagraph() && isUnsafeParagraphTag(tag)) {
        paragraphs.getLast().beginUnsafeTag();        
      }
      if ("p".equalsIgnoreCase(tag)) {
        beginParagraph();
      }
    }    
  }
  
  private void beginParagraph() {
    paragraphs.addLast(new ParagraphTracking());
  }
  
  private void endParagraph() {
    paragraphs.removeLast();
  }
  
  private String getDomWalkAccessExpression(String tag) {  
    writer.addInitComment("DomWalkAccess %s", this.toString());
    
    // First look and see if we have any part of the path in our variable cache.
    int end = pathComponents.size();
    String var = null;
    for (; end > 0; --end) {
      var = domPathCache.get(pathComponents.subList(0, end));
      if (var != null) {
        break;
      }
    }
    
    // Next, do the remaining DOM walking
    StringBuilder builder = new StringBuilder();
    if (var != null) {
      builder.append(var);
    } else {
      builder.append(parent);
    }
    for (int i = end; i < pathComponents.size(); ++i) {
      PathComponent component = pathComponents.get(i);
      if (i < (pathComponents.size() - 1)) {
        // For partial paths, create an intermediate variable that can be reused
        // by other elements that need to walk.
        String varName = "intermediate" + writer.getUniqueId();
        writer.addInitStatement("com.google.gwt.dom.client.Node %s = %s(%s, %d);", 
            varName, getLookupMethod(component, tag), builder.toString(), 
            component.childIndex);
        domPathCache.put(new LinkedList<PathComponent>(pathComponents.subList(0, i + 1)), varName);
        builder = new StringBuilder(varName);
      } else {
        builder.insert(0, getLookupMethod(component, tag) + "(");
        builder.append(", ").append(component.childIndex).append(")");        
      }
    }
    builder.append(".cast()");
    return builder.toString();
  }
  
  private String getLookupMethod(PathComponent component, String tag) {
    if (walkingTextChildNodes) {
      return STANDARD_LOOKUP_METHOD;
    }
    if (component.isTableWithoutTbody && !isValidDirectTableChild(tag)) {
      return TABLE_NON_TEXT_LOOKUP_METHOD;
    }
    return NON_TEXT_LOOKUP_METHOD;
  }
  
  private String getQuery() {
    StringBuilder query = new StringBuilder();

    for (PathComponent component : pathComponents) {
      if (query.length() > 0) {
        query.append(" > ");
      }
      query.append(":nth-child(").append(component.childIndex + 1).append(")");
    }
    return query.toString();
  }
  
  /**
   * Get an access expression using XPATH or CSS query.  This is currently disabled as it isn't
   * as fast as walking by hand.
   * @return
   */
  private String getQueryAccessExpression() {  
    StringBuilder builder = new StringBuilder("UiBinderUtil.lookupNodeByTreeIndicies(");
    builder.append(parent);
    builder.append(",\"");
    builder.append(getQuery());
    builder.append("\",\"");
    builder.append(getXpath());
    builder.append("\").cast()");
    return builder.toString();
  }
  
  private String getXpath() {
    StringBuilder xpath = new StringBuilder();
    for (PathComponent component : pathComponents) {        
      xpath.append("/*[").append(component.childIndex + 1).append("]");
    }
    return xpath.toString();
  }
  
  private boolean isInsideParagraph() {
    return !paragraphs.isEmpty();
  }

  private boolean isPlaceholderElement(XMLElement elem) {
    return "ph".equalsIgnoreCase(elem.getLocalName());
  }
  
  private boolean isUnsafeParagraphTag(String tag) {
    return PARAGRAPH_UNSAFE_NODES.contains(tag.toLowerCase());
  }
  
  private boolean isValidDirectTableChild(String tag) {
    return tag != null && TABLE_SECTIONS.contains(tag.toLowerCase());
  }
  
  private boolean safeForExpression() {
    for (ParagraphTracking tracking : paragraphs) {
      if (!tracking.isSafeForField()) {
        return false;
      }
    }
    return true;
  }
}
