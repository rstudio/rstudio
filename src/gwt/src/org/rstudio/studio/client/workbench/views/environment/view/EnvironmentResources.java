/*
 * EnvironmentResources.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

package org.rstudio.studio.client.workbench.views.environment.view;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;

public interface EnvironmentResources extends ClientBundle
{
   public static final EnvironmentResources INSTANCE =
           GWT.create(EnvironmentResources.class);

   @Source("ExpandIcon_2x.png")
   ImageResource expandIcon2x();

   @Source("CollapseIcon_2x.png")
   ImageResource collapseIcon2x();
   
   @Source("TracedFunction_2x.png")
   ImageResource tracedFunction2x();
   
   @Source("GlobalEnvironment_2x.png")
   ImageResource globalEnvironment2x();
   
   @Source("PackageEnvironment_2x.png")
   ImageResource packageEnvironment2x();
   
   @Source("AttachedEnvironment_2x.png")
   ImageResource attachedEnvironment2x();

   @Source("FunctionEnvironment_2x.png")
   ImageResource functionEnvironment2x();
   
   @Source("ObjectListView_2x.png")
   ImageResource objectListView2x();
   
   @Source("ObjectGridView_2x.png")
   ImageResource objectGridView2x();
   
   @Source("EnvironmentObjects.css")
   EnvironmentStyle environmentStyle();
}

