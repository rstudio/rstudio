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
package com.google.gwt.dev.resource.impl;

import com.google.gwt.dev.util.StringInterner;
import com.google.gwt.dev.util.collect.Maps;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Combines the information conveyed about a set of path prefixes to quickly
 * answer questions regarding an entire set of path prefixes.
 */
public class PathPrefixSet {
  /*
   * (1) TODO(amitmanjhi): Improve the api of the PathPrefixSet so that with one
   * trie-traversal, it could be found out which resources rooted at a directory
   * are allowed?
   */

  private static class TrieNode {
    // TODO(amitmanjhi): Consider the memory-speed tradeoff here
    private Map<String, TrieNode> children = Maps.create();
    private final String part;

    private PathPrefix prefix;

    public TrieNode(String part) {
      this.part = StringInterner.get().intern(part);
    }

    public TrieNode addChild(String part) {
      part = StringInterner.get().intern(part);
      TrieNode newChild = new TrieNode(part);
      assert !children.containsKey(part);
      children = Maps.put(children, part, newChild);
      return newChild;
    }

    public void extendPathPrefix(PathPrefix prefix) {
      if (this.prefix == null) {
        this.prefix = prefix;
      } else {
        this.prefix.merge(prefix);
      }
    }

    public TrieNode findChild(String part) {
      return children.get(part);
    }

    public PathPrefix getPathPrefix() {
      return prefix;
    }

    public boolean hasChildren() {
      return !children.isEmpty();
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      toString(sb, "");
      return sb.toString();
    }

    private void toString(StringBuilder sb, String indent) {
      if (sb.length() > 0) {
        sb.append('\n');
      }
      sb.append(indent);
      sb.append(' ');
      sb.append(part);
      for (TrieNode child : children.values()) {
        child.toString(sb, indent + "  ");
      }
    }
  }

  /**
   * List of all path prefixes in priority order.
   */
  private final List<PathPrefix> prefixes = new ArrayList<PathPrefix>();

  private final TrieNode rootTrieNode = new TrieNode("/");

  /**
   * @param prefix the prefix to add
   * @return <code>true</code> if the prefix was not already in the set;
   *         otherwise, it merged with one having the same prefix, which has
   *         the effect of expanding the filter (the merge works as
   *         <code>union(includes - skips) - union(excludes)</code>)
   */
  public boolean add(PathPrefix prefix) {
    prefix.setPriority(prefixes.size());
    prefixes.add(prefix);

    String pathPrefix = prefix.getPrefix();

    /*
     * An empty prefix means we have no prefix requirement, but we do attach the
     * prefix to the root so that we can apply the filter.
     */
    if ("".equals(pathPrefix)) {
      rootTrieNode.extendPathPrefix(prefix);
      return false;
    }

    // TODO(bruce): consider not using split for speed
    String[] parts = pathPrefix.split("/");
    TrieNode parentNode = rootTrieNode;
    boolean didAdd = false;
    for (String part : parts) {
      TrieNode childNode = parentNode.findChild(part);
      if (childNode != null) {
        // Follow existing branch.
        parentNode = childNode;
      } else {
        // Add a new branch.
        parentNode = parentNode.addChild(part);
        didAdd = true;
      }
    }
    assert (parentNode != null);
    parentNode.extendPathPrefix(prefix);
    return didAdd;
  }

  public int getSize() {
    return prefixes.size();
  }

  /**
   * Determines whether or not a directory might have resources that could be
   * included. The primary purpose of this method is to allow
   * {@link ClassPathEntry} subclasses to avoid descending into directory
   * hierarchies that could not possibly contain resources that would be
   * included by {@link #includesResource(String)}
   * 
   * @param dirPath must be a valid abstract directory name (must not be an
   *          empty string)
   * @return true if some PathPrefix allows the directory
   */
  public boolean includesDirectory(String dirPath) {
    assertValidAbstractDirectoryPathName(dirPath);

    /*
     * There are four cases:
     * 
     * (1) The empty string was specified as a prefix, which causes everything
     * to be included.
     * 
     * (2) As we walk the parts of dirPath, we see a path prefix attached to one
     * of the trie nodes we encounter. This means that there was a specified
     * prefix that this dirPath falls underneath, so it is included.
     * 
     * (3) dirPath is longer than the trie, but we never encounter a path prefix
     * as we walk the trie. This indicates that this directory doesn't fall into
     * any of the specified prefixes.
     * 
     * (4) dirPath is not longer than the trie and stays on the trie the whole
     * time, which means it is included (since at least some longer prefix
     * includes it).
     */

    if (rootTrieNode.getPathPrefix() != null) {
      // Case (1).
      return true;
    }

    TrieNode parentNode = rootTrieNode;
    String[] parts = dirPath.split("/");
    for (String part : parts) {
      assert (!"".equals(part));
      TrieNode childNode = parentNode.findChild(part);
      if (childNode != null) {
        PathPrefix pathPrefix = childNode.getPathPrefix();
        if (pathPrefix != null) {
          // Case (2).
          return true;
        }

        // Haven't found a path prefix yet, so keep walking.
        parentNode = childNode;
      } else {
        // Case (3).
        return false;
      }
    }

    // Case (4).
    return true;
  }

  /**
   * Determines whether or not a given resource should be allowed by this path
   * prefix set and the corresponding filters.
   * 
   * @param resourceAbstractPathName
   * @return matching <code>PathPrefix</code> if the resource matches some
   *         specified prefix and any associated filters don't exclude it.
   *         Otherwise, returns null. So it returns null if either no prefixes
   *         match or the most specific prefix excludes the resource.
   */
  public PathPrefix includesResource(String resourceAbstractPathName) {
    String[] parts = resourceAbstractPathName.split("/");
    return includesResource(resourceAbstractPathName, parts);
  }

  /**
   * Implementation of {@link #includesDirectory(String)}.
   */
  public PathPrefix includesResource(String resourceAbstractPathName, String[] parts) {
    /*
     * Algorithm: dive down the package hierarchy looking for the most specific
     * package that applies to this resource. The filter of the most specific
     * package is the final determiner of inclusion/exclusion, such that more
     * specific subpackages can override the filter settings on less specific
     * superpackages.
     */

    assertValidAbstractResourcePathName(resourceAbstractPathName);

    TrieNode currentNode = rootTrieNode;
    PathPrefix mostSpecificPrefix = rootTrieNode.getPathPrefix();

    // Walk all but the last path part, which is assumed to be a file name.
    for (String part : parts) {
      assert (!"".equals(part));
      TrieNode childNode = currentNode.findChild(part);
      if (childNode != null) {
        // We found a more specific node.
        PathPrefix moreSpecificPrefix = childNode.getPathPrefix();
        if (moreSpecificPrefix != null) {
          mostSpecificPrefix = moreSpecificPrefix;
        }
        currentNode = childNode;
      } else {
        // No valid branch to follow.
        break;
      }
    }

    if (mostSpecificPrefix == null
        || !mostSpecificPrefix.allows(resourceAbstractPathName)) {
      /*
       * Didn't match any specified prefix or the filter of the most specific
       * prefix disallows the resource
       */
      return null;
    }

    return mostSpecificPrefix;
  }

  @Override
  public String toString() {
    return rootTrieNode.toString();
  }

  public Collection<PathPrefix> values() {
    return Collections.unmodifiableCollection(prefixes);
  }

  private void assertValidAbstractDirectoryPathName(String name) {
    assert (name != null);
    // assert ("".equals(name) || (!name.startsWith("/") &&
    // name.endsWith("/")));
    assert (!name.startsWith("/") && name.endsWith("/"));
  }

  private void assertValidAbstractResourcePathName(String name) {
    assert (name != null);
    assert (!"".equals(name));
    assert (!name.startsWith("/") && !name.endsWith("/"));
  }
}
