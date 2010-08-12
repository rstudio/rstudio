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

/*
 * IMPLEMENTATION NOTES
 * 
 * This is a useful way to unify various forms of metadata so that clients don't
 * have to be brittle with respect to Java language versions. For example, this
 * mechanism exposes the tag "gwt.typeArgs" in a way that is independent of
 * whether a doc comment was used or (in the future) a concrete instantiation of
 * a generic type was used. The same idea could be useful for to exposing
 * attributes as metadata.
 * 
 * This API has been deprecated in favor of proper Java annotations.
 */
package com.google.gwt.core.ext.typeinfo;

/**
 * @deprecated Formerly used to manage Javadoc-comment style metadata. Replaced
 *             by Java 1.5 annotations. All implementations now return empty
 *             arrays. This interface and all implementations methods will be
 *             removed in a future release.
 */
@Deprecated
public interface HasMetaData {
  /**
   * Gets each list of metadata for the specified tag name.
   * 
   * @deprecated Javadoc comment metadata has been deprecated in favor of proper
   *             Java annotations. See
   *             {@link HasAnnotations#getAnnotation(Class)} for equivalent
   *             functionality.
   */
  @Deprecated
  String[][] getMetaData(String tagName);

  /**
   * Gets the name of available metadata tags.
   * 
   * @deprecated Javadoc comment metadata has been deprecated in favor of proper
   *             Java annotations. The {@link HasAnnotations} interface does not
   *             support a mechanism to enumerate all of the annotations on a
   *             member; the type of the desired annotation must be known.
   */
  @Deprecated
  String[] getMetaDataTags();
}
