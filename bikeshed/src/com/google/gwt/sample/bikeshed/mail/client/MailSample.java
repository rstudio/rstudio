/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.sample.bikeshed.mail.client;

import com.google.gwt.bikeshed.cells.client.CheckboxCell;
import com.google.gwt.bikeshed.cells.client.FieldUpdater;
import com.google.gwt.bikeshed.list.client.PagingTableListView;
import com.google.gwt.bikeshed.list.client.SimpleColumn;
import com.google.gwt.bikeshed.list.client.TextColumn;
import com.google.gwt.bikeshed.list.shared.ListListModel;
import com.google.gwt.bikeshed.list.shared.SelectionModel.DefaultSelectionModel;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

/**
 * A demo of selection features.
 */
public class MailSample implements EntryPoint, ClickHandler {

  class MailSelectionModel extends DefaultSelectionModel<Message> {
    private static final int ALL = 0;
    private static final int NONE = 1;
    private static final int READ = 2;
    private static final int SENDER = 3;
    private static final int SUBJECT = 4;
    private static final int UNREAD = 5;

    // Use a TreeMap in order to get sorted diagnostic output
    private Map<Integer, Boolean> exceptions = new TreeMap<Integer, Boolean>();

    private String search;
    private int type = NONE;

    public boolean isSelected(Message object) {
      // Check exceptions
      int id = object.id;
      Boolean exception = exceptions.get(id);
      if (exception != null) {
        return exception.booleanValue();
      }
      // If not in exceptions, return the default
      return isDefaultSelected(object);
    }

    public void setSearch(String search) {
      this.search = canonicalize(search);
      updateListeners();
    }
    
    public void setSelected(List<Message> objects, boolean selected) {
      for (Message object : objects) {
        addException(object.id, selected);
      }
      updateListeners();
    }

    public void setSelected(Message object, boolean selected) {
      addException(object.id, selected);
      updateListeners();
    }

    public void setType(int type) {
      this.type = type;
      exceptions.clear();
      updateListeners();
    }
    
    public String toString() {
      StringBuilder sb = new StringBuilder();
      switch (type) {
        case NONE:
          sb.append("NONE ");
          break;
        case ALL:
          sb.append("ALL ");
          break;
        case READ:
          sb.append("READ ");
          break;
        case UNREAD:
          sb.append("UNREAD ");
          break;
        case SENDER:
          sb.append("SENDER ");
          sb.append(search);
          sb.append(' ');
          break;
        case SUBJECT:
          sb.append("SUBJECT ");
          sb.append(search);
          sb.append(' ');
          break;
      }

      boolean first = true;
      for (int i : exceptions.keySet()) {
        if (exceptions.get(i) != Boolean.TRUE) {
          continue;
        }
        if (first) {
          first = false;
          sb.append("+msg(s) ");
        }
        sb.append(i);
        sb.append(' ');
      }

      first = true;
      for (int i : exceptions.keySet()) {
        if (exceptions.get(i) != Boolean.FALSE) {
          continue;
        }
        if (first) {
          first = false;
          sb.append("-msg(s) ");
        }
        sb.append(i);
        sb.append(' ');
      }

      return sb.toString();
    }

    public void updateListeners() {
      super.updateListeners();
      selectionLabel.setText("Selected " + this.toString());
    }

    private void addException(int id, boolean selected) {
      Boolean currentlySelected = exceptions.get(id);
      if (currentlySelected != null && currentlySelected.booleanValue() != selected) {
        exceptions.remove(id);
      } else {
        exceptions.put(id, selected);
      }      
    }

    private String canonicalize(String input) {
      return input.toUpperCase();
    }

    private boolean isDefaultSelected(Message object) {
      switch (type) {
        case NONE:
          return false;
        case ALL:
          return true;
        case READ:
          return object.isRead();
        case UNREAD:
          return !object.isRead();
        case SENDER:
          if (search == null || search.length() == 0) {
            return false;
          }
          return canonicalize(object.getSender()).contains(search);
        case SUBJECT:
          if (search == null || search.length() == 0) {
            return false;
          }
          return canonicalize(object.getSubject()).contains(search);
        default:
          throw new IllegalStateException("type = " + type);
      }
    }
  }

  class Message {
    int id;
    boolean isRead;
    String sender;
    String subject;

    public Message(int id, String sender, String subject) {
      super();
      this.id = id;
      this.sender = sender;
      this.subject = subject;
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof Message)) {
        return false;
      }
      return id == ((Message) obj).id;
    }

    public int getId() {
      return id;
    }

    public String getSender() {
      return sender;
    }

    public String getSubject() {
      return subject;
    }

    @Override
    public int hashCode() {
      return id;
    }

    public boolean isRead() {
      return isRead;
    }

    @Override
    public String toString() {
      return "Message [id=" + id + ", sender=" + sender + ", subject="
          + subject + ", read=" + isRead + "]";
    }
  }

  private static final String[] senders = {
      "test@example.com", "spam1@spam.com", "gwt@google.com", "Mai Oleta",
      "Barbara Myles", "Celsa Ocie", "Elwood Holloway", "Bolanle Alford",
      "Amaka Ackland", "Afia Audley", "Pearlene Cher", "Pei Sunshine",
      "Zonia Dottie", "Krystie Jetta", "Alaba Banvard", "Ines Azalee",
      "Kaan Boulier", "Emilee Naoma", "Atino Alice", "Debby Renay",
      "Versie Nereida", "Ramon Erikson", "Karole Crissy", "Nelda Olsen",
      "Mariana Dann", "Reda Cheyenne", "Edelmira Jody", "Agueda Shante",
      "Marla Dorris"
  };

  private static final String[] subjects = {
      "GWT rocks", "What's a widget?", "Money in Nigeria",
      "Impress your colleagues with bling-bling", "Degree available",
      "Rolex Watches", "Re: Re: yo bud", "Important notice"
  };

  private Button allButton = new Button("Select All");
  private Button allOnPageButton = new Button("Select All On This Page");
  private Button noneButton = new Button("Select None");
  private Button readButton = new Button("Select Read");
  private Label selectionLabel = new Label();
  private MailSelectionModel selectionModel = new MailSelectionModel();
  private Button senderButton = new Button("Search Senders");
  private Button subjectButton = new Button("Search Subject");

  private PagingTableListView<Message> table;

  private Button unreadButton = new Button("Select Unread");

  // Handle events for all buttons here in order to avoid creating multiple
  // ClickHandlers
  public void onClick(ClickEvent event) {
    Button source = (Button) event.getSource();
    if (source == noneButton) {
      selectionModel.setType(MailSelectionModel.NONE);
    } else if (source == allOnPageButton) {
      selectionModel.setSelected(table.getDisplayedItems(), true);
    } else if (source == allButton) {
      selectionModel.setType(MailSelectionModel.ALL);
    } else if (source == readButton) {
      selectionModel.setType(MailSelectionModel.READ);
    } else if (source == unreadButton) {
      selectionModel.setType(MailSelectionModel.UNREAD);
    } else if (source == senderButton) {
      selectionModel.setType(MailSelectionModel.SENDER);
    } else if (source == subjectButton) {
      selectionModel.setType(MailSelectionModel.SUBJECT);
    }
  }

  public void onModuleLoad() {
    ListListModel<Message> listModel = new ListListModel<Message>();
    List<Message> messages = listModel.getList();
    Random rand = new Random();
    for (int i = 0; i < 1000; i++) {
      Message message = new Message(10000 + i,
          senders[rand.nextInt(senders.length)],
          subjects[rand.nextInt(subjects.length)]);
      message.isRead = rand.nextBoolean();
      messages.add(message);
    }

    table = new PagingTableListView<Message>(listModel, 10);
    table.setSelectionModel(selectionModel);

    // The state of the checkbox is taken from the selection model
    SimpleColumn<Message, Boolean> selectedColumn = new SimpleColumn<Message, Boolean>(
        new CheckboxCell()) {
      @Override
      public Boolean getValue(Message object) {
        return selectionModel.isSelected(object);
      }
    };
    // Update the selection model when the checkbox is changed manually
    selectedColumn.setFieldUpdater(new FieldUpdater<Message, Boolean, Void>() {
      public void update(int index, Message object, Boolean value, Void viewData) {
        selectionModel.setSelected(object, value);
      }
    });
    table.addColumn(selectedColumn, "Selected");

    TextColumn<Message> isReadColumn = new TextColumn<Message>() {
      @Override
      public String getValue(Message object) {
        return object.isRead ? "read" : "unread";
      }
    };
    table.addColumn(isReadColumn, "Read");

    TextColumn<Message> senderColumn = new TextColumn<Message>() {
      @Override
      public String getValue(Message object) {
        return object.getSender();
      }
    };
    table.addColumn(senderColumn, "Sender");

    TextColumn<Message> subjectColumn = new TextColumn<Message>() {
      @Override
      public String getValue(Message object) {
        return object.getSubject();
      }
    };
    table.addColumn(subjectColumn, "Subject");
    
    Label searchLabel = new Label("Search Sender or Subject:");
    final TextBox searchBox = new TextBox();
    searchBox.addKeyUpHandler(new KeyUpHandler() {
      public void onKeyUp(KeyUpEvent event) {
        selectionModel.setSearch(searchBox.getText());
      }
    });

    noneButton.addClickHandler(this);
    allOnPageButton.addClickHandler(this);
    allButton.addClickHandler(this);
    readButton.addClickHandler(this);
    unreadButton.addClickHandler(this);
    senderButton.addClickHandler(this);
    subjectButton.addClickHandler(this);
    
    HorizontalPanel panel = new HorizontalPanel();
    panel.add(searchLabel);
    panel.add(searchBox);
    
    RootPanel.get().add(panel);
    RootPanel.get().add(new HTML("<br>"));
    RootPanel.get().add(table);
    RootPanel.get().add(new HTML("<br>"));
    RootPanel.get().add(noneButton);
    RootPanel.get().add(allOnPageButton);
    RootPanel.get().add(allButton);
    RootPanel.get().add(readButton);
    RootPanel.get().add(unreadButton);
    RootPanel.get().add(subjectButton);
    RootPanel.get().add(senderButton);
    RootPanel.get().add(new HTML("<hr>"));
    RootPanel.get().add(selectionLabel);
  }
}
