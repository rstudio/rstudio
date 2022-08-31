/*
 * AceEditorCoverageHighlighter.java
 *
 * Copyright (C) 2022 by RStudio, PBC
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
package org.rstudio.studio.client.workbench.views.source.editors.text.ace;

import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;

public class AceEditorCoverageHighlighter {
    
    public AceEditorCoverageHighlighter(AceEditor editor)
    {
        editor_ = editor;

        /*
            highlight(120, "#00ff0040");
            highlight(128, "#ff000040");
            highlight(129, "#ff000040");
            highlight(130, "#ff000040");
            highlight(131, "#ff000040");
            highlight(133, "#ff000040");
            highlight(138, "#00ff0040");
            highlight(139, "#00ff0040");
            highlight(140, "#00ff0040");
            highlight(142, "#00ff0040");
            highlight(143, "#00ff0040");
            highlight(144, "#00ff0040");
            highlight(146, "#ff000040");
            highlight(150, "#00ff0040");
            highlight(151, "#00ff0040");
            highlight(152, "#00ff0040");
            highlight(153, "#00ff0040");
        */
    }

    void highlight(int line, String color) {
        Range range = Range.fromPoints(Position.create(line, 1), Position.create(line, 2));
        editor_.getSession().addMarker(range, "ace_highlight-marker", "fullLine", false, "background: " + color + " !important; ");
    }

    private AceEditor editor_;
}
