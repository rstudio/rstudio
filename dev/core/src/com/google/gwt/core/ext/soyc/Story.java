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
package com.google.gwt.core.ext.soyc;

import java.io.Serializable;
import java.util.SortedSet;

/**
 * Represents a (possibly disjoint) region of the JavaScript output for which
 * metadata is available.
 */
public interface Story extends Serializable {
  /**
   * If the Story represents a literal value, this method will return a
   * description of the type of literal. If the Story does not represent a
   * literal, this method will return <code>null</code>.
   */
  // TODO: Consider promoting the Correlation.Literal enum to a public API
  String getLiteralTypeName();

  /**
   * Gets the Members of the compilation that the Story is about.
   */
  SortedSet<Member> getMembers();
}
