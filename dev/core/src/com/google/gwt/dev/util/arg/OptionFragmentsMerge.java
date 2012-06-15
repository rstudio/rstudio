/*
 * Copyright 2011 Google Inc.
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
package com.google.gwt.dev.util.arg;

/**
 * Enable the new code splitter that auto-partitions.
 */
public interface OptionFragmentsMerge {
  // TODO(acleung): Delete this in favor of -XfragmentCount
  
  // TODO(acleung): This is currently an experimental frag. We should find a
  // use case new splitter. Some possible approache:
  //
  // 1. Magically decide the number of fragments to merge. (May be too hard)
  // 2. All the user to specify number of fragments they want to *keep* instead
  //    of the number they want to merge.
  // 3. Ask the user what is the max (average) size of fragments. (This
  //    can only be an estimated.
  
  int getFragmentsMerge();

  void setFragmentsMerge(int numFragments);
}
