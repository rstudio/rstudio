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

/**
 * Transform a resource into URL before making a request. This can be overridden
 * if some sort of proxy mechanism is needed.  This is modeled after the logic 
 * originally coded in ResourceInjectsionUtils.java
 */
function computeUrlForResource(resource) {
  /* return an absolute path unmodified */
  if (resource.match(/^\//)) {
    return resource;
  }
  /* return a fully qualified URL unmodified */
  if (resource.match(/^[a-zA-Z]+:\/\//)) {
    return resource;
  }
  return __MODULE_FUNC__.__moduleBase + resource;
}
