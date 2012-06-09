/*
 * Copyright 2012 Google Inc.
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
package elemental.svg;
import elemental.events.EventListener;
import elemental.events.Event;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;

import java.util.Date;

/**
  * 
  */
public interface SVGElementInstance {

  SVGElementInstanceList getChildNodes();

  SVGElement getCorrespondingElement();

  SVGUseElement getCorrespondingUseElement();

  SVGElementInstance getFirstChild();

  SVGElementInstance getLastChild();

  SVGElementInstance getNextSibling();

  EventListener getOnabort();

  void setOnabort(EventListener arg);

  EventListener getOnbeforecopy();

  void setOnbeforecopy(EventListener arg);

  EventListener getOnbeforecut();

  void setOnbeforecut(EventListener arg);

  EventListener getOnbeforepaste();

  void setOnbeforepaste(EventListener arg);

  EventListener getOnblur();

  void setOnblur(EventListener arg);

  EventListener getOnchange();

  void setOnchange(EventListener arg);

  EventListener getOnclick();

  void setOnclick(EventListener arg);

  EventListener getOncontextmenu();

  void setOncontextmenu(EventListener arg);

  EventListener getOncopy();

  void setOncopy(EventListener arg);

  EventListener getOncut();

  void setOncut(EventListener arg);

  EventListener getOndblclick();

  void setOndblclick(EventListener arg);

  EventListener getOndrag();

  void setOndrag(EventListener arg);

  EventListener getOndragend();

  void setOndragend(EventListener arg);

  EventListener getOndragenter();

  void setOndragenter(EventListener arg);

  EventListener getOndragleave();

  void setOndragleave(EventListener arg);

  EventListener getOndragover();

  void setOndragover(EventListener arg);

  EventListener getOndragstart();

  void setOndragstart(EventListener arg);

  EventListener getOndrop();

  void setOndrop(EventListener arg);

  EventListener getOnerror();

  void setOnerror(EventListener arg);

  EventListener getOnfocus();

  void setOnfocus(EventListener arg);

  EventListener getOninput();

  void setOninput(EventListener arg);

  EventListener getOnkeydown();

  void setOnkeydown(EventListener arg);

  EventListener getOnkeypress();

  void setOnkeypress(EventListener arg);

  EventListener getOnkeyup();

  void setOnkeyup(EventListener arg);

  EventListener getOnload();

  void setOnload(EventListener arg);

  EventListener getOnmousedown();

  void setOnmousedown(EventListener arg);

  EventListener getOnmousemove();

  void setOnmousemove(EventListener arg);

  EventListener getOnmouseout();

  void setOnmouseout(EventListener arg);

  EventListener getOnmouseover();

  void setOnmouseover(EventListener arg);

  EventListener getOnmouseup();

  void setOnmouseup(EventListener arg);

  EventListener getOnmousewheel();

  void setOnmousewheel(EventListener arg);

  EventListener getOnpaste();

  void setOnpaste(EventListener arg);

  EventListener getOnreset();

  void setOnreset(EventListener arg);

  EventListener getOnresize();

  void setOnresize(EventListener arg);

  EventListener getOnscroll();

  void setOnscroll(EventListener arg);

  EventListener getOnsearch();

  void setOnsearch(EventListener arg);

  EventListener getOnselect();

  void setOnselect(EventListener arg);

  EventListener getOnselectstart();

  void setOnselectstart(EventListener arg);

  EventListener getOnsubmit();

  void setOnsubmit(EventListener arg);

  EventListener getOnunload();

  void setOnunload(EventListener arg);

  SVGElementInstance getParentNode();

  SVGElementInstance getPreviousSibling();

  EventRemover addEventListener(String type, EventListener listener);

  EventRemover addEventListener(String type, EventListener listener, boolean useCapture);

  boolean dispatchEvent(Event event);

  void removeEventListener(String type, EventListener listener);

  void removeEventListener(String type, EventListener listener, boolean useCapture);
}
