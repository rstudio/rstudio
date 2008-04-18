package com.google.gwt.museum.client;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CaptionPanel;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

public class Issue2307 {

  private CaptionPanel captionPanel;

  private class ControlPanel extends Composite {
    private final Grid grid = new Grid(3, 2);

    public ControlPanel() {
      initWidget(grid);

      final TextBox textBox = new TextBox();
      grid.setWidget(0, 1, textBox);
      grid.setWidget(0, 0, new Button("setCaptionText", new ClickListener() {
        public void onClick(Widget sender) {
          captionPanel.setCaptionText(textBox.getText());
        }
      }));

      final TextBox htmlBox = new TextBox();
      grid.setWidget(1, 1, htmlBox);
      grid.setWidget(1, 0, new Button("setCaptionHTML", new ClickListener() {
        public void onClick(Widget sender) {
          captionPanel.setCaptionHTML(htmlBox.getText());
        }
      }));

      final TextBox contentBox = new TextBox();
      grid.setWidget(2, 1, contentBox);
      grid.setWidget(2, 0, new Button("setContentWidget", new ClickListener() {
        public void onClick(Widget sender) {
          captionPanel.setContentWidget(new Button(contentBox.getText()));
        }
      }));
    }
  }

  public Issue2307(Panel p) {
    captionPanel = new CaptionPanel("CaptionedPanel");
    p.add(captionPanel);
    p.add(new ControlPanel());
  }

}
