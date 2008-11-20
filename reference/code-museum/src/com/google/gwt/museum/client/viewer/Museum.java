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
package com.google.gwt.museum.client.viewer;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.LinkElement;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.museum.client.common.AbstractIssue;
import com.google.gwt.museum.client.common.Utility;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.IncrementalCommand;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.ImageBundle;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.MultiWordSuggestOracle;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.SuggestOracle.Suggestion;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * A repository for demonstrating bugs we once faced.
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
   * Class used to implement a callback when a css file is loaded. We add a
   * width style of 10px to isLoaded classes, then test to see if the style is
   * active.
   */
  class Poller implements IncrementalCommand {
    private HTML test;
    private AbstractIssue issue;
    private SimplePanel monitor = new SimplePanel();

    public Poller() {
      RootPanel.get().add(monitor);
    }

    public boolean execute() {
      if (test.getOffsetWidth() == 10) {
        issueContainer.setWidget(issue.createIssue());
        issue.onAttached();
        return false;
      } else {
        return true;
      }
    }

    public void startPolling(AbstractIssue issue) {
      test = new HTML();
      test.setStyleName("isLoaded");
      monitor.setWidget(test);
      this.issue = issue;
      DeferredCommand.addCommand(this);
    }
  }

  /**
   * The images used in this application.
   */
  public static final MuseumImages IMAGES = GWT.create(MuseumImages.class);

  /**
   * A reference for all issues.
   */
  private final ArrayList<AbstractIssue> issues = new ArrayList<AbstractIssue>();

  /**
   * Add an issue to the list of issues.
   */
  private final Poller poller = new Poller();

  /**
   * The panel that contains the current example.
   */
  private final SimplePanel issueContainer = new SimplePanel();

  /**
   * A container to hold the CSS that will be applied to the issue.
   */
  private LinkElement issueLinkElement = null;

  /**
   * A description of the current issue.
   */
  private final HTML issueDescription = new HTML();

  /**
   * The list of all issues.
   */
  private final ListBox issueList = new ListBox();

  /**
   * Add an issue to the museum. Should be called in the inherited constructor.
   * 
   * @param issue the issue to be added
   */
  public void addIssue(AbstractIssue issue) {
    issues.add(issue);
  }

  public void onModuleLoad() {
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
   * Create a suggest box with all current suggestions in it.
   */
  private SuggestBox createIssueFinder() {
    class IssueSuggestion extends MultiWordSuggestOracle.MultiWordSuggestion {
      private AbstractIssue issue;

      public IssueSuggestion(AbstractIssue issue) {
        super("", issue.getHeadline());
        this.issue = issue;
      }

      public AbstractIssue getIssue() {
        return issue;
      }
    }

    SuggestOracle oracle = new SuggestOracle() {

      @Override
      public void requestSuggestions(Request request, Callback callback) {
        String ofInterest = (".*" + request.getQuery() + ".*").toLowerCase();
        ArrayList<Suggestion> suggestions = new ArrayList<Suggestion>();
        HashSet<AbstractIssue> s = new HashSet<AbstractIssue>();
        for (AbstractIssue issue : issues) {
          if (issue.getHeadline().toLowerCase().matches(ofInterest)) {
            s.add(issue);
            suggestions.add(new IssueSuggestion(issue));
          }
        }

        for (AbstractIssue issue : issues) {
          if (!s.contains(issue) && issue.getInstructions().matches(ofInterest)) {
            suggestions.add(new IssueSuggestion(issue));
          }
        }
        callback.onSuggestionsReady(request, new Response(suggestions));
      }

    };

    SuggestBox box = new SuggestBox(oracle);
    box.addSelectionHandler(new SelectionHandler<Suggestion>() {
      public void onSelection(SelectionEvent<Suggestion> event) {
        AbstractIssue issue = ((IssueSuggestion) event.getSelectedItem()).getIssue();
        int index = issues.indexOf(issue);
        issueList.setSelectedIndex(index);
        refreshIssue();
      }
    });
    box.setAnimationEnabled(false);
    return box;
  }

  /**
   * Create the options panel.
   * 
   * @return the options panel
   */
  private Widget createOptionsPanel() {
    // Populate a list box containing all issues
    for (AbstractIssue issue : issues) {
      issueList.addItem(issue.getHeadline());
    }
    issueList.addChangeHandler(new ChangeHandler() {
      public void onChange(ChangeEvent event) {
        refreshIssue();
      }
    });

    // Create a button to refresh the current issue
    Button refreshIssueButton = new Button("Refresh", new ClickHandler() {
      public void onClick(ClickEvent event) {
        refreshIssue();
      }
    });

    // Create a suggest box to search for issues
    SuggestBox suggestBox = createIssueFinder();

    // Create a button to move to the previous issue
    Image prevButton = IMAGES.prevButton().createImage();
    prevButton.setStyleName("prevButton");
    prevButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
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
    nextButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
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
    hPanel.add(suggestBox);
    SimplePanel wrapper = new SimplePanel();
    wrapper.setStyleName("museum-optionsPanel");
    wrapper.setWidget(hPanel);
    return wrapper;
  }

  /**
   * Refresh the current issue in the issue list.
   */
  private void refreshIssue() {
    setIssue(issues.get(issueList.getSelectedIndex()));
  }

  /**
   * Set the current issue in the issue container.
   * 
   * @param issue the issue to set
   */
  private void setIssue(AbstractIssue issue) {
    if (issueLinkElement != null) {
      Utility.getHeadElement().removeChild(issueLinkElement);
      issueLinkElement = null;
    }
    // Fetch the associated style sheet using an HTTP request
    issueLinkElement = issue.createCSS();
    Utility.getHeadElement().appendChild(issueLinkElement);
    issueDescription.setHTML(issue.getInstructions());
    poller.startPolling(issue);
  }
}
