/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.museum.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.HeadElement;
import com.google.gwt.dom.client.LinkElement;
import com.google.gwt.museum.client.issues.Issue2290;
import com.google.gwt.museum.client.issues.Issue2307;
import com.google.gwt.museum.client.issues.Issue2321;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.ImageBundle;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

import java.util.ArrayList;
import java.util.List;

/**
 * A repository for demonstrating bugs we once faced.
 * 
 * TODO(bruce): make this more like the original bug sink idea. For now, it's
 * just a hacked together set of examples based on past issues.
 */
public class Museum implements EntryPoint {
  /**
   * Images used in the museum.
   */
  public static interface MuseumImages extends ImageBundle {
    AbstractImagePrototype nextButton();

    AbstractImagePrototype prevButton();
  }

  /**
   * The images used in this application.
   */
  public static final MuseumImages IMAGES = GWT.create(MuseumImages.class);

  /**
   * A reference for all issues.
   */
  public static final List<AbstractIssue> ISSUES = new ArrayList<AbstractIssue>();

  /**
   * Add an issue to the list of issues.
   * 
   * @param issue the issue to add
   */
  private static void addIssue(AbstractIssue issue) {
    ISSUES.add(issue);
  }

  /**
   * Convenience method for getting the document's head element.
   * 
   * @return the document's head element
   */
  private static native HeadElement getHeadElement() /*-{
    return $doc.getElementsByTagName("head")[0];
  }-*/;

  /**
   * Populate the list of issues. Add your issue here.
   */
  private static void populateIssues() {
    addIssue(new Issue2290());
    addIssue(new Issue2307());
    addIssue(new Issue2321());
  }

  /**
   * The panel that contains the current example.
   */
  private SimplePanel issueContainer = new SimplePanel();

  /**
   * A container to hold the CSS that will be applied to the issue.
   */
  private LinkElement issueLinkElement = null;

  /**
   * A description of the current issue.
   */
  private HTML issueDescription = new HTML();

  /**
   * The list of all issues.
   */
  private ListBox issueList = new ListBox();

  public void onModuleLoad() {
    populateIssues();

    // Add the options and issue containers to the page
    RootPanel.get().add(createOptionsPanel());
    RootPanel.get().add(issueDescription);
    issueDescription.setStylePrimaryName("museum-issueDescription");
    RootPanel.get().add(issueContainer);
    issueContainer.setStylePrimaryName("museum-issueContainer");

    // Default to the first issue
    refreshIssue();
  }

  /**
   * Create the options panel.
   * 
   * @return the options panel
   */
  private Widget createOptionsPanel() {
    // Populate a list box containing all issues
    for (AbstractIssue issue : ISSUES) {
      issueList.addItem(issue.getHeadline());
    }
    issueList.addChangeListener(new ChangeListener() {
      public void onChange(Widget sender) {
        refreshIssue();
      }
    });

    // Create a button to refresh the current issue
    Button refreshIssueButton = new Button("Refresh", new ClickListener() {
      public void onClick(Widget sender) {
        refreshIssue();
      }
    });

    // Create a button to move to the previous issue
    Image prevButton = IMAGES.prevButton().createImage();
    prevButton.setStyleName("prevButton");
    prevButton.addClickListener(new ClickListener() {
      public void onClick(Widget sender) {
        int selectedIndex = issueList.getSelectedIndex();
        if (selectedIndex > 0) {
          issueList.setSelectedIndex(selectedIndex - 1);
          refreshIssue();
        } else {
          Window.alert("You are already on the first issue");
        }
      }
    });

    // Create a button to move to the next issue
    Image nextButton = IMAGES.nextButton().createImage();
    nextButton.setStyleName("nextButton");
    nextButton.addClickListener(new ClickListener() {
      public void onClick(Widget sender) {
        int selectedIndex = issueList.getSelectedIndex();
        if (selectedIndex < issueList.getItemCount() - 1) {
          issueList.setSelectedIndex(selectedIndex + 1);
          refreshIssue();
        } else {
          Window.alert("You are already on the last issue");
        }
      }
    });

    // Combine the list box and text into a panel
    HorizontalPanel hPanel = new HorizontalPanel();
    hPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
    hPanel.add(new HTML("Select an issue: "));
    hPanel.add(issueList);
    hPanel.add(prevButton);
    hPanel.add(nextButton);
    hPanel.add(refreshIssueButton);
    SimplePanel wrapper = new SimplePanel();
    wrapper.setStyleName("museum-optionsPanel");
    wrapper.setWidget(hPanel);
    return wrapper;
  }

  /**
   * Refresh the current issue in the issue list.
   */
  private void refreshIssue() {
    setIssue(ISSUES.get(issueList.getSelectedIndex()));
  }

  /**
   * Set the current issue in the issue container.
   * 
   * @param issue the issue to set
   */
  private void setIssue(AbstractIssue issue) {
    if (issueLinkElement != null) {
      getHeadElement().removeChild(issueLinkElement);
      issueLinkElement = null;
    }
    issueDescription.setHTML(issue.getDescription());
    issueContainer.setWidget(issue.createIssue());

    // Fetch the associated style sheet using an HTTP request
    if (issue.hasCSS()) {
      String className = issue.getClass().getName();
      className = className.substring(className.lastIndexOf(".") + 1);
      issueLinkElement = Document.get().createLinkElement();
      issueLinkElement.setRel("stylesheet");
      issueLinkElement.setType("text/css");
      issueLinkElement.setHref("issues/" + className + ".css");
      getHeadElement().appendChild(issueLinkElement);
    }
  }
}
