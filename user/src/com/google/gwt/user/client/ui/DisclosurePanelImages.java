/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.user.client.ui;

/**
 * An {@link ImageBundle} that provides images for {@link DisclosurePanel}.
 */
public interface DisclosurePanelImages extends ImageBundle {
  
  /**
   * An image indicating an open disclosure panel.
   * 
   * @return a prototype of this image
   */
  AbstractImagePrototype disclosurePanelOpen();
  
  /**
   * An image indicating a closed disclosure panel.
   * 
   * @return a prototype of this image
   */
  AbstractImagePrototype disclosurePanelClosed();
}


/**
 * A bundle containing the RTL versions of the images for DisclosurePanel. Right now, we
 * only need to override the disclosurePanelClosed() method, as the image that we provide
 * for disclosurePanelOpen() is direction-agnostic.
 * 
 * Notice that this interface is package protected. This interface need not be
 * publicly exposed, as it is only used by the DisclosurePanel class to provide RTL
 * versions of the images in the case the the user does not pass in their own
 * bundle. However, we cannot make this class private, because the generated
 * class needs to be able to extend this class.
 */
interface DisclosurePanelImagesRTL extends DisclosurePanelImages {
  /**
   * An image indicating a closed disclosure panel for a RTL context.
   * 
   * @return a prototype of this image
   */
  @Resource("disclosurePanelClosed_rtl.png")
  AbstractImagePrototype disclosurePanelClosed();
}