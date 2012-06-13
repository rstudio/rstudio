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
package elemental.example.client;

import static elemental.client.Browser.getDocument;
import static elemental.client.Browser.getWindow;

import com.google.gwt.core.client.EntryPoint;

import elemental.html.Window;

import elemental.dom.XMLHttpRequest;
import elemental.events.Event;
import elemental.events.EventListener;
import elemental.html.ButtonElement;
import elemental.html.DivElement;

public class ElementalExample implements EntryPoint {

  @Override
  public void onModuleLoad() {
    final ButtonElement btn = getDocument().createButtonElement();
    btn.setInnerHTML("w00t?");
    btn.getStyle().setColor("red");
    getDocument().getBody().appendChild(btn);

    final DivElement div = getDocument().createDivElement();
    getDocument().getBody().appendChild(div);

    EventListener listener = new EventListener() {
      public void handleEvent(Event evt) {
        final XMLHttpRequest xhr = getWindow().newXMLHttpRequest();
        xhr.setOnLoad(new EventListener() {
          @Override
          public void handleEvent(Event evt) {
            div.setInnerHTML(xhr.getResponseText());
          }
        });
        xhr.open("GET", "/snippet.html");
        xhr.send();

        getWindow().setTimeout(new Window.TimerCallback() {
          @Override
          public void fire() {
            getWindow().alert("timeout fired");
          }
        }, 1000);

        btn.removeEventListener(Event.CLICK, this, false);
      }
    };

    btn.addEventListener(Event.CLICK, listener, false);
  }
}
