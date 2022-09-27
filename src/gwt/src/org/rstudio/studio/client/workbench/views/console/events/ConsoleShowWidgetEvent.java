/*
 * ConsoleShowWidgetEvent.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.console.events;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class ConsoleShowWidgetEvent extends GwtEvent<ConsoleShowWidgetEvent.Handler>
{
    public static final GwtEvent.Type<Handler> TYPE = new GwtEvent.Type<>();
    
    public static class Data extends JavaScriptObject
    {
        protected Data()
        {
        }

        public native final String getURL() /*-{
            return this.url;
        }-*/;

        public native final int getHeight() /*-{
            return this.height;
        }-*/;

    }
    
    public interface Handler extends EventHandler
    {
        void onConsoleShowWidget(ConsoleShowWidgetEvent event);
    }
    
    public ConsoleShowWidgetEvent(Data data)
    {
        data_ = data;
    }
    
    @Override
    public Type<Handler> getAssociatedType()
    {
        return TYPE;
    }

    @Override
    protected void dispatch(Handler handler)
    {
        handler.onConsoleShowWidget(this);
    }
    
    public String getURL() 
    {
        return data_.getURL();
    }

    public int getHeight()
    {
        return data_.getHeight();
    }

    private final Data data_;
}
