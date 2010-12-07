/*
 * OAuthApprovalEvent.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.events;

import com.google.gwt.event.shared.GwtEvent;
import org.rstudio.studio.client.workbench.model.OAuthApproval;

public class OAuthApprovalEvent extends GwtEvent<OAuthApprovalHandler>
{
   public static final GwtEvent.Type<OAuthApprovalHandler> TYPE =
      new GwtEvent.Type<OAuthApprovalHandler>();
   
   public OAuthApprovalEvent(OAuthApproval approval)
   {
      approval_ = approval;
   }
   
   public OAuthApproval getApproval()
   {
      return approval_;
   }
   
   @Override
   protected void dispatch(OAuthApprovalHandler handler)
   {
      handler.onOAuthApproval(this);
   }

   @Override
   public GwtEvent.Type<OAuthApprovalHandler> getAssociatedType()
   {
      return TYPE;
   }
   
   private final OAuthApproval approval_;
}
