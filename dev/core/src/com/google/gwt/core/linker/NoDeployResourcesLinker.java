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
package com.google.gwt.core.linker;

import com.google.gwt.core.ext.Linker;
import com.google.gwt.core.ext.LinkerContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.ArtifactSet;
import com.google.gwt.core.ext.linker.GeneratedResource;
import com.google.gwt.core.ext.linker.LinkerOrder;
import com.google.gwt.core.ext.linker.LinkerOrder.Order;

/**
 * This class prevents generated resources whose partial path begins with
 * {@value #PREFIX} from being emitted into the output.
 */
@LinkerOrder(Order.PRE)
public class NoDeployResourcesLinker extends Linker {
  public static final String PREFIX = "no-deploy/";

  @Override
  public String getDescription() {
    return "Filter generated resources in the " + PREFIX + " path";
  }

  @Override
  public ArtifactSet link(TreeLogger logger, LinkerContext context,
      ArtifactSet artifacts) throws UnableToCompleteException {

    ArtifactSet toReturn = new ArtifactSet(artifacts);

    for (GeneratedResource artifact : toReturn.find(GeneratedResource.class)) {
      if (artifact.getPartialPath().startsWith(PREFIX)) {
        toReturn.remove(artifact);
      }
    }

    return toReturn;
  }
}
