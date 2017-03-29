/*
 * RSConnectAccountResources.java
 *
 * Copyright (C) 2009-15 by RStudio, Inc.
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
package org.rstudio.studio.client.rsconnect.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;

public interface RSConnectResources extends ClientBundle
{
   static RSConnectResources INSTANCE = 
                  (RSConnectResources)GWT.create(RSConnectResources.class);
   
   @Source("localAccountIconSmall_2x.png")
   ImageResource localAccountIconSmall2x();

   @Source("localAccountIcon_2x.png")
   ImageResource localAccountIcon2x();

   @Source("localAccountIconLarge_2x.png")
   ImageResource localAccountIconLarge2x();

   @Source("cloudAccountIconSmall_2x.png")
   ImageResource cloudAccountIconSmall2x();

   @Source("cloudAccountIcon_2x.png")
   ImageResource cloudAccountIcon2x();

   @Source("cloudAccountIconLarge_2x.png")
   ImageResource cloudAccountIconLarge2x();
   
   @Source("publishIcon_2x.png")
   ImageResource publishIcon2x();

   @Source("publishIconLarge_2x.png")
   ImageResource publishIconLarge2x();

   @Source("rpubsPublish_2x.png")
   ImageResource rpubsPublish2x();

   @Source("rpubsPublishLarge_2x.png")
   ImageResource rpubsPublishLarge2x();
   
   @Source("publishSingleRmd_2x.png")
   ImageResource publishSingleRmd2x();

   @Source("publishMultipleRmd_2x.png")
   ImageResource publishMultipleRmd2x();
   
   @Source("publishDocWithSource_2x.png")
   ImageResource publishDocWithSource2x();
   
   @Source("publishDocWithoutSource_2x.png")
   ImageResource publishDocWithoutSource2x();
   
   @Source("previewDoc_2x.png")
   ImageResource previewDoc2x();
   
   @Source("previewPlot_2x.png")
   ImageResource previewPlot2x();
   
   @Source("republishPlot_2x.png")
   ImageResource republishPlot2x();
   
   @Source("previewPresentation_2x.png")
   ImageResource previewPresentation2x();
}