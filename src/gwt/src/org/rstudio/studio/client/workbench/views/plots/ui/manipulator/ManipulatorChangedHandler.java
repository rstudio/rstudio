/**
 * 
 */
package org.rstudio.studio.client.workbench.views.plots.ui.manipulator;

import com.google.gwt.json.client.JSONObject;

public interface ManipulatorChangedHandler
{
   void onManipulatorChanged(JSONObject values);
}