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
package com.google.gwt.bikeshed.sample.stocks.client;

import com.google.gwt.bikeshed.cells.client.Cell;
import com.google.gwt.bikeshed.list.client.SimpleCellList;
import com.google.gwt.bikeshed.list.shared.ListModel;
import com.google.gwt.bikeshed.sample.stocks.shared.PlayerInfo;
import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

import java.util.List;

/**
 * Widget to display player scores.
 */
public class PlayerScoresWidget extends Composite {

  interface Binder extends UiBinder<Widget, PlayerScoresWidget> {
  }

  private static final Binder binder = GWT.create(Binder.class);

  /**
   * A {@link Cell} that displays the status of a single player.
   */
  private static final class PlayerInfoCell extends Cell<PlayerInfo> {
    @Override
    public void render(PlayerInfo value, StringBuilder sb) {
      sb.append("<div class='playerScoreBox'>");
      sb.append("<b>Name: </b>");
      sb.append(value.getDisplayName());
      sb.append("<br><b>Net Worth: </b>");
      sb.append(StockSample.getFormattedPrice(value.getNetWorth()));
      sb.append("<br><b>Cash: </b>");
      sb.append(StockSample.getFormattedPrice(value.getCash()));
      
      List<String> status = value.getStatus();
      if (status != null) {
        for (String s : status) {
          sb.append("<br>");
          sb.append(s);
        }
      }
      sb.append("</div>");
    }
  }

  @UiField
  SimpleCellList<PlayerInfo> listView;

  private final ListModel<PlayerInfo> model;

  public PlayerScoresWidget(ListModel<PlayerInfo> model) {
    this.model = model;
    initWidget(binder.createAndBindUi(this));
  }

  @UiFactory
  SimpleCellList<PlayerInfo> createListView() {
    return new SimpleCellList<PlayerInfo>(model, new PlayerInfoCell());
  }
}
