/*
 * MemoryUsageSummary.java
 *
 * Copyright (C) 2021 by RStudio, PBC
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
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.studio.client.workbench.views.environment.model.MemoryUsageReport;

public class MemoryUsageSummary extends Composite
{
   public MemoryUsageSummary(MemoryUsageReport report)
   {
      report_ = report;

      initWidget(GWT.<MemoryUsageSummary.Binder>create(MemoryUsageSummary.Binder.class).createAndBindUi(this));
   }

   public interface Binder extends UiBinder<Widget, MemoryUsageSummary>
   {
   }

   private final MemoryUsageReport report_;
}
