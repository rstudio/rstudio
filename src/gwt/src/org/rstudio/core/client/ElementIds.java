/*
 * ElementIds.java
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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
package org.rstudio.core.client;

import com.google.gwt.dom.client.Element;

public class ElementIds
{
   public static void assignElementId(Element ele, String id)
   {
      ele.setId(ID_PREFIX + id);
   }
   
   public static String getElementId(String id)
   {
      return ID_PREFIX + id;
   }
   
   public static String idFromLabel(String label)
   {
      // replace all non-alphanumerics with underscores
      String id = label.replaceAll("[^a-zA-Z0-0]", "_");
      
      // collapse multiple underscores to a single underscore
      id = id.replaceAll("_+", "_");
      
      // clean up leading/trailing underscores
      id = id.replaceAll("^_+", "");
      id = id.replaceAll("_+$", "");
      
      // convert to lowercase and return
      return ID_PREFIX + "label_" + id.toLowerCase();
   }
   
   public final static String ID_PREFIX = "rstudio_";
   
   // global list of specific IDs we assign -- we keep this list centralized in this class as a
   // so that we can be sure an ID is not used elsewhere in the product
   public final static String CONSOLE_INPUT = "console_input";
   public final static String CONSOLE_OUTPUT = "console_output";
   public final static String DEPLOY_CONTENT = "deploy_content";
   public final static String FIND_REPLACE_BAR = "find_replace_bar";
   public final static String HELP_FRAME = "help_frame";
   public final static String LOADING_SPINNER = "loading_image";
   public final static String PLOT_IMAGE_FRAME = "plot_image_frame";
   public final static String POPUP_COMPLETIONS = "popup_completions";
   public final static String PREFERENCES_CONFIRM = "preferences_confirm";
   public final static String PUBLISH_CONNECT = "publish_connect";
   public final static String PUBLISH_DISCONNECT = "publish_disconnect";
   public final static String PUBLISH_ITEM = "publish_item";
   public final static String PUBLISH_RECONNECT = "publish_reconnect";
   public final static String PUBLISH_SHOW_DEPLOYMENTS = "show_deployments";
   public final static String RSC_SERVER_URL = "rsc_server_url";
   public final static String SHELL_WIDGET = "shell_widget";
   public final static String SOURCE_TEXT_EDITOR = "source_text_editor";
   public final static String XTERM_WIDGET = "xterm_widget";
}
