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

import org.apache.tools.ant.types.ZipScanner;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents the abstract path prefix that goes between the
 * {@link ClassPathEntry} and the rest of resource's abstract path. This concept
 * allows us to specify subsets of path hierarchies orthogonally from path
 * roots. For example, a path root might be <code>/home/gwt/src/</code> and an
 * abstract path prefix might be <code>module/client/</code>. Importantly,
 * you can apply the same abstract path prefix to multiple path roots and find
 * more than one set of resources residing in disjoint locations yet occupying
 * the same logical hierarchy. Sorry this explanation is so abstract; it's how
 * we model things like the GWT module's client source path, public path, and
 * super source path.
 */
public final class PathPrefix {

  /**
   * Represents whether or not a PathPrefix includes a particular file as well
   * as an indicator of the inclusion/exclusion priority. The priority is
   * needed because there can be multiple PathPrefixes for a given directory
   * and the highest priority judgement must be found and honored.
   */
  public enum Judgement {
    EXCLUSION_EXCLUDE(false, 3), FILTER_INCLUDE(true, 2),
    IMPLICIT_EXCLUDE(false, 1);

    private final boolean include;
    private final int priority;

    private Judgement(boolean include, int priority) {
      this.include = include;
      this.priority = priority;
    }

    public int getPriority() {
      return priority;
    }

    public boolean isInclude() {
      return include;
    }
  }

  public static final PathPrefix ALL = new PathPrefix("", null);

  private final Set<String> exclusions;
  private ZipScanner exclusionScanner;
  private final List<ResourceFilter> filters;
  private final String prefix;
  private int priority = -1;
  private final boolean shouldReroot;
  private final String moduleName;

  /**
   * Construct a non-rerooting prefix.
   *
   * @param prefix a string prefix that (1) is the empty string or (2) begins
   *          with something other than a slash and ends with a slash
   * @param filter the resource filter to use, or <code>null</code> for no
   *          filter; note that the filter must always return the same answer
   *          for the same candidate path (doing otherwise will produce
   *          inconsistent behavior in identifying available resources)
   */
  public PathPrefix(String prefix, ResourceFilter filter) {
    this("", prefix, filter, false, null);
  }

  /**
   * Construct a prefix without global exclusions.
   *
   * @param prefix a string prefix that (1) is the empty string or (2) begins
   *          with something other than a slash and ends with a slash
   * @param filter the resource filter to use, or <code>null</code> for no
   *          filter; note that the filter must always return the same answer
   *          for the same candidate path (doing otherwise will produce
   *          inconsistent behavior in identifying available resources)
   * @param shouldReroot if <code>true</code>, any matching {@link Resource}
   *          for this prefix will be rerooted to not include the initial prefix
   *          path; if <code>false</code>, the prefix will be included in a
   *          matching resource's path.
   */
  public PathPrefix(String prefix, ResourceFilter filter, boolean shouldReroot) {
    this("", prefix, filter, shouldReroot, null);
  }

  /**
   * Construct a prefix.
   *
   * @param moduleName the name of the module that contained the Source or
   *          Public entry that this PathPrefix represents
   * @param prefix a string prefix that (1) is the empty string or (2) begins
   *          with something other than a slash and ends with a slash
   * @param filter the resource filter to use, or <code>null</code> for no
   *          filter; note that the filter must always return the same answer
   *          for the same candidate path (doing otherwise will produce
   *          inconsistent behavior in identifying available resources)
   * @param shouldReroot if <code>true</code>, any matching {@link Resource}
   *          for this prefix will be rerooted to not include the initial prefix
   *          path; if <code>false</code>, the prefix will be included in a
   *          matching resource's path.
   * @param excludeList list of globs that should be removed from <i>any</i>
   *          module's resources.
   */
  public PathPrefix(String moduleName, String prefix, ResourceFilter filter,
      boolean shouldReroot, String[] excludeList) {
    assertValidPrefix(prefix);
    this.moduleName = moduleName;
    this.prefix = prefix;
    this.filters = new ArrayList<ResourceFilter>(1);
    this.filters.add(filter);
    this.shouldReroot = shouldReroot;
    this.exclusions = new HashSet<String>();
    if (excludeList != null) {
      for (String exclude : excludeList) {
        exclusions.add(exclude);
      }
    }
  }

  /**
   * Determines the inclusion/exclusion status and priority of a given path.
   * <p>
   * Determination is made using the prefix path, list of exclusions and list
   * of filters (which are constructed from "includes" and "skips" entries in
   * the xml).
   */
  public Judgement getJudgement(String path) {
    if (!path.startsWith(prefix)) {
      return Judgement.IMPLICIT_EXCLUDE;
    }
    if (filters.size() == 0 && exclusions.size() == 0) {
      return Judgement.FILTER_INCLUDE;
    }
    if (shouldReroot) {
      path = getRerootedPath(path);
    }

    createExcludeFilter();
    if (exclusionScanner != null && exclusionScanner.match(path)) {
      return Judgement.EXCLUSION_EXCLUDE;
    }
    for (ResourceFilter filter : filters) {
      if (filter == null || filter.allows(path)) {
        return Judgement.FILTER_INCLUDE;
      }
    }
    return Judgement.IMPLICIT_EXCLUDE;
  }

  /**
   * Equality is based on prefixes representing the same string. Importantly,
   * the filter does not affect equality.
   */
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof PathPrefix) {
      if (prefix.equals(((PathPrefix) obj).prefix)) {
        return true;
      }
    }
    return false;
  }

  /**
   * The prefix.
   *
   * @return the result is guaranteed to be non-<code>null</code>, and
   *         either be the empty string or it will not begin with a slash and
   *         will end with a slash; these guarantees are very useful when
   *         concatenating paths that incorporate prefixes
   */
  public String getPrefix() {
    return prefix;
  }

  public String getRerootedPath(String path) {
    assert (path.startsWith(prefix));
    if (shouldReroot) {
      return path.substring(prefix.length());
    } else {
      return path;
    }
  }

  @Override
  public int hashCode() {
    return prefix.hashCode();
  }

  /**
   * Consolidate a given {@code PathPrefix} with this one, such that resources
   * excluded by neither prefix and included by either are allowed.
   *
   * @param pathPrefix
   */
  public void merge(PathPrefix pathPrefix) {
    assert prefix.equals(pathPrefix.prefix);
    for (ResourceFilter filter : pathPrefix.filters) {
      filters.add(filter);
    }
    exclusions.addAll(pathPrefix.exclusions);
    if (exclusionScanner != null && !exclusions.isEmpty()) {
      exclusionScanner = null;  // lose the stale one; we'll recreate later
    }
  }

  public boolean shouldReroot() {
    return shouldReroot;
  }

  @Override
  public String toString() {
    return prefix + (shouldReroot ? "**" : "*") + (filters.size() == 0 ? "" : "?");
  }

  public String getModuleName() {
    return moduleName;
  }

  int getPriority() {
    return priority;
  }

  void setPriority(int priority) {
    assert (this.priority == -1);
    this.priority = priority;
  }

  private void assertValidPrefix(String prefix) {
    assert (prefix != null);
    assert ("".equals(prefix) || (!prefix.startsWith("/") && prefix.endsWith("/"))) : "malformed prefix";
  }

  private void createExcludeFilter() {
    if (exclusionScanner == null && !exclusions.isEmpty()) {
      exclusionScanner = new ZipScanner();
      exclusionScanner.setIncludes(exclusions.toArray(new String[exclusions.size()]));
      exclusionScanner.init();
      exclusions.clear();
    }
  }
}
