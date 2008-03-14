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
package com.google.gwt.dev.linker;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;

import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * This class prevents generated resources whose partial path begins with
 * {@value #PREFIX} from being visible.
 */
public class NoDeployResourcesShim extends LinkerContextShim {
  public static final String PREFIX = "no-deploy/";
  private final SortedSet<GeneratedResource> generatedResources;

  public NoDeployResourcesShim(TreeLogger logger, LinkerContext parent)
      throws UnableToCompleteException {
    super(logger, parent);

    SortedSet<GeneratedResource> mutableSet = new TreeSet<GeneratedResource>(
        GENERATED_RESOURCE_COMPARATOR);

    SortedSet<GeneratedResource> view = super.getGeneratedResources();
    for (GeneratedResource res : view) {
      if (!res.getPartialPath().toLowerCase().startsWith(PREFIX)) {
        mutableSet.add(res);
      } else {
        logger.log(TreeLogger.SPAM, "Excluding generated resource "
            + res.getPartialPath(), null);
      }
    }

    assert mutableSet.size() <= view.size();

    if (mutableSet.size() == view.size()) {
      // Reuse the existing view
      generatedResources = view;

    } else {
      // Ensure that the new view is immutable
      generatedResources = Collections.unmodifiableSortedSet(mutableSet);
    }
  }

  @Override
  public SortedSet<GeneratedResource> getGeneratedResources() {
    return generatedResources;
  }
}
