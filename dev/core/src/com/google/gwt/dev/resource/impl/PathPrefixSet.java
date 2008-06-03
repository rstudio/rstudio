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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Combines the information conveyed about a set of path prefixes to quickly
 * answer questions regarding an entire set of path prefixes.
 */
public class PathPrefixSet {

  private static class TrieNode {
    // TODO(bruce): test to see if Map would be faster; I'm on the fence
    private final List<TrieNode> children = new ArrayList<TrieNode>();
    private final String part;
    private PathPrefix prefix;

    public TrieNode(String part) {
      this.part = part;
    }

    public TrieNode addChild(String part) {
      assert (findChild(part) == null);
      TrieNode newChild = new TrieNode(part);
      children.add(newChild);
      return newChild;
    }

    public TrieNode findChild(String part) {
      for (TrieNode child : children) {
        if (child.part.equals(part)) {
          return child;
        }
      }
      return null;
    }

    public PathPrefix getPathPrefix() {
      return prefix;
    }

    public boolean hasChildren() {
      return !children.isEmpty();
    }

    public void setPathPrefix(PathPrefix prefix) {
      this.prefix = prefix;
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
      for (TrieNode child : children) {
        child.toString(sb, indent + "  ");
      }
    }
  }

  private int modCount;
  private final Map<String, PathPrefix> prefixes = new HashMap<String, PathPrefix>();
  private final TrieNode rootTrieNode = new TrieNode("/");

  /**
   * @param prefix the prefix to add
   * @return <code>true</code> if the prefix was not already in the set;
   *         otherwise, it replaced an identical one having the same prefix,
   *         which has the effect of changing which filter is used (last one
   *         wins)
   */
  public boolean add(PathPrefix prefix) {
    ++modCount;
    String pathPrefix = prefix.getPrefix();
    prefixes.put(pathPrefix, prefix);

    /*
     * An empty prefix means we have no prefix requirement, but we do attached
     * the prefix to the root so that we can apply the filter.
     */
    if ("".equals(pathPrefix)) {
      rootTrieNode.setPathPrefix(prefix);
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
    // This may clobber an existing one, but that's okay. Last one wins.
    parentNode.setPathPrefix(prefix);
    return didAdd;
  }

  public int getModCount() {
    return modCount;
  }

  /**
   * Determines whether or not a directory might have resources that could be
   * included. The primary purpose of this method is to allow
   * {@link ClassPathEntry} subclasses to avoid descending into directory
   * hierarchies that could not possibly contain resources that would be
   * included by {@link #includesResource(String).
   * 
   * @param dirPath must be a valid abstract directory name or the empty string
   * @return
   */
  public boolean includesDirectory(String dirPath) {
    assertValidAbstractDirectoryPathName(dirPath);

    /*
     * There are five cases:
     * 
     * (0) dirPath is the empty string, which is (a) trivially included unless
     * (b) no prefix paths have been specified at all.
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

    // if ("".equals(dirPath)) {
    // if (rootTrieNode.hasChildren() || rootTrieNode.getPathPrefix() != null) {
    // // Case (0)(a): trivially true.
    // return true;
    // } else {
    // // Case (0)(b): no directories are included.
    // return false;
    // }
    // }
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
   * @return <code>true</code> if the resource matches some specified prefix
   *         and any associated filters don't exclude it
   */
  public boolean includesResource(String resourceAbstractPathName) {
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

    // TODO(bruce): consider not using split for speed
    String[] parts = resourceAbstractPathName.split("/");

    // Walk all but the last path part, which is assumed to be a file name.
    for (int i = 0, n = parts.length - 1; i < n; ++i) {
      String part = parts[i];
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

    if (mostSpecificPrefix == null) {
      // Didn't match any specified prefix.
      return false;
    }

    // Test the filter of the most specific prefix we found.
    return mostSpecificPrefix.allows(resourceAbstractPathName);
  }

  public Collection<PathPrefix> values() {
    return Collections.unmodifiableCollection(prefixes.values());
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
