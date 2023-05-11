/*
 * FileHyperlink.java
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
package org.rstudio.core.client.hyperlink;

import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;

import org.rstudio.core.client.FilePosition;
import org.rstudio.core.client.ResultCallback;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.workbench.views.source.SourceColumnManager;
import org.rstudio.studio.client.workbench.views.source.editors.EditingTarget;
import org.rstudio.studio.client.workbench.views.source.model.SourceServerOperations;

public class FileHyperlink extends Hyperlink 
{
    public FileHyperlink(String url, Map<String, String> params, String text, String clazz)
    {
        super(url, params, text, clazz);
        line = -1;
        col = -1;

        Match match = POSITION_FRAGMENT_PATTERN.match(url, 0);
        if (match != null)
        {
            url = url.replaceFirst("#.*$", "");
            
            line = StringUtil.parseInt(match.getGroup(1), -1);

            String second = match.getGroup(2);
            if (second != null)
                col = StringUtil.parseInt(match.getGroup(2).substring(1), -1);
        }
        else 
        {
            if (params.containsKey("line"))
                line = StringUtil.parseInt(params.get("line"), -1);
        
            if (params.containsKey("col"))
                col = StringUtil.parseInt(params.get("col"), -1);    
        }

        if (url.matches("^file:///[a-zA-Z]:"))
            filename = url.replaceFirst("file:///", "");
        else
            filename = url.replaceFirst("file://", "");
        
        server_ = RStudioGinjector.INSTANCE.getServer();
    }

    @Override
    public void onClick()
    {
        server_.createAliasedPath(filename, new SimpleRequestCallback<String>()
        {
            @Override
            public void onResponseReceived(String response)
            {
                if (response.length() > 0)
                {
                    navigateToAliased(response);
                }
                else 
                {
                    RStudioGinjector.INSTANCE.getGlobalDisplay().showErrorMessage(
                        constants_.noSuchFile(),
                        constants_.doesNotExist(filename)
                        );
                }
            }
        });
    }

    private void navigateToAliased(String file) 
    {
        final SourceColumnManager columnManager = RStudioGinjector.INSTANCE.getSourceColumnManager(); 
        
        columnManager.editFile(file, new ResultCallback<EditingTarget, ServerError>()
        {
            @Override
            public void onSuccess(final EditingTarget result)
            {
                if (line != -1) 
                {
                    // give ace time to render before scrolling to position
                    Scheduler.get().scheduleDeferred(() ->
                    {
                        FilePosition position = FilePosition.create(line, Math.max(col, 1));
                        columnManager.scrollToPosition(position, true, () -> {});
                    });
                }
            }
        });
    }

    public static boolean handles(String url)
    {
        return url.startsWith("file://");
    }
    
    private String filename;
    private int line;
    private int col;

    public static final Pattern POSITION_FRAGMENT_PATTERN = Pattern.create("#([0-9]+)(:[0-9]+)?$");

    private SourceServerOperations server_;
    private static final HyperlinkConstants constants_ = GWT.create(HyperlinkConstants.class);
}
