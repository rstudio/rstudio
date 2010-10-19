// $Id: PathImpl.java 17744 2009-10-14 14:38:57Z hardy.ferentschik $
/*
* JBoss, Home of Professional Open Source
* Copyright 2009, Red Hat, Inc. and/or its affiliates, and individual contributors
* by the @authors tag. See the copyright.txt in the distribution for a
* full listing of individual contributors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
// Modified by Google: Replace java.util.Pattern with gwt RegExp
package org.hibernate.validator.engine;

import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.validation.Path;

/**
 * @author Hardy Ferentschik
 */
public class PathImpl implements Path, Serializable {

  private static final long serialVersionUID = 7564511574909882392L;

  /**
   * Regular expression used to split a string path into its elements.
   *
   * @see <a href="http://www.regexplanet.com/simple/index.jsp">Regular expression tester</a>
   */
  private static final RegExp pathPattern = RegExp.compile("(\\w+)(\\[(\\w*)\\])?(\\.(.*))*");

  private static final String PROPERTY_PATH_SEPERATOR = ".";

  private final List<Node> nodeList;

  /**
   * Returns a {@code Path} instance representing the path described by the given string. To create a root node the empty string should be passed.
   *
   * @param propertyPath the path as string representation.
   *
   * @return a {@code Path} instance representing the path described by the given string.
   *
   * @throws IllegalArgumentException in case {@code property == null} or {@code property} cannot be parsed.
   */
  public static PathImpl createPathFromString(String propertyPath) {
    if ( propertyPath == null ) {
      throw new IllegalArgumentException( "null is not allowed as property path." );
    }

    if ( propertyPath.length() == 0 ) {
      return createNewPath( null );
    }

    return parseProperty( propertyPath );
  }

  public static PathImpl createNewPath(String name) {
    PathImpl path = new PathImpl();
    NodeImpl node = new NodeImpl( name );
    path.addNode( node );
    return path;
  }

  public static PathImpl createShallowCopy(Path path) {
    return path == null ? null : new PathImpl( path );
  }

  private PathImpl(Path path) {
    this.nodeList = new ArrayList<Node>();
    for ( Object aPath : path ) {
      nodeList.add( new NodeImpl( ( Node ) aPath ) );
    }
  }

  private PathImpl() {
    nodeList = new ArrayList<Node>();
  }

  private PathImpl(List<Node> nodeList) {
    this.nodeList = new ArrayList<Node>();
    for ( Node node : nodeList ) {
      this.nodeList.add( new NodeImpl( node ) );
    }
  }

  public boolean isRootPath() {
    return nodeList.size() == 1 && nodeList.get( 0 ).getName() == null;
  }

  public PathImpl getPathWithoutLeafNode() {
    List<Node> nodes = new ArrayList<Node>( nodeList );
    PathImpl path = null;
    if ( nodes.size() > 1 ) {
      nodes.remove( nodes.size() - 1 );
      path = new PathImpl( nodes );
    }
    return path;
  }

  public void addNode(Node node) {
    nodeList.add( node );
  }

  public Node removeLeafNode() {
    if ( nodeList.size() == 0 ) {
      throw new IllegalStateException( "No nodes in path!" );
    }
    if ( nodeList.size() == 1 ) {
      throw new IllegalStateException( "Root node cannot be removed!" );
    }
    return nodeList.remove( nodeList.size() - 1 );
  }

  public NodeImpl getLeafNode() {
    if ( nodeList.size() == 0 ) {
      throw new IllegalStateException( "No nodes in path!" );
    }
    return ( NodeImpl ) nodeList.get( nodeList.size() - 1 );
  }

  public Iterator<Path.Node> iterator() {
    return nodeList.iterator();
  }

  public boolean isSubPathOf(Path path) {
    Iterator<Node> pathIter = path.iterator();
    Iterator<Node> thisIter = iterator();
    while ( pathIter.hasNext() ) {
      Node pathNode = pathIter.next();
      if ( !thisIter.hasNext() ) {
        return false;
      }
      Node thisNode = thisIter.next();
      if ( !thisNode.equals( pathNode ) ) {
        return false;
      }
    }
    return true;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    Iterator<Path.Node> iter = iterator();
    while ( iter.hasNext() ) {
      Node node = iter.next();
      builder.append( node.toString() );
      if ( iter.hasNext() ) {
        builder.append( PROPERTY_PATH_SEPERATOR );
      }
    }
    return builder.toString();
  }

  @Override
  public boolean equals(Object o) {
    if ( this == o ) {
      return true;
    }
    if ( o == null || getClass() != o.getClass() ) {
      return false;
    }

    PathImpl path = ( PathImpl ) o;
    if ( nodeList != null && !nodeList.equals( path.nodeList ) ) {
      return false;
    }
    if ( nodeList == null && path.nodeList != null ) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return nodeList != null ? nodeList.hashCode() : 0;
  }

  private static PathImpl parseProperty(String property) {
    PathImpl path = new PathImpl();
    String tmp = property;
    do {
      MatchResult matcher = pathPattern.exec(tmp);
      if (matcher != null) {
        String value = matcher.getGroup(1);
        String indexed = matcher.getGroup(2);
        String index = matcher.getGroup(3);
        NodeImpl node = new NodeImpl( value );
        if ( indexed != null ) {
          node.setInIterable( true );
        }
        if ( index != null && index.length() > 0 ) {
          try {
            Integer i = Integer.parseInt( index );
            node.setIndex( i );
          }
          catch ( NumberFormatException e ) {
            node.setKey( index );
          }
        }
        path.addNode( node );
        tmp = matcher.getGroup(5);
      }
      else {
        throw new IllegalArgumentException( "Unable to parse property path " + property );
      }
    } while ( tmp != null );
    return path;
  }

}
