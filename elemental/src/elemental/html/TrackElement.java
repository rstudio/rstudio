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
package elemental.html;
import elemental.dom.Element;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;

import java.util.Date;

/**
  * <p>The <code>track</code>&nbsp;element is used as a child of the media elements—<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/audio">&lt;audio&gt;</a></code>
 and <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/video">&lt;video&gt;</a></code>
—and does not represent anything on its own. It lets you specify timed text tracks (or time-based data).</p>
<p>The type of data that <code> track</code> adds to the media is set in the <code>kind</code> attribute, which can take values of <code>subtitles</code>, <code>captions</code>, <code>descriptions</code>, <code>chapters</code> or <code>metadata</code>. The element points to a source file containing timed text that the browser exposes when the user requests additional data. </p>
  */
public interface TrackElement extends Element {

    static final int ERROR = 3;

    static final int LOADED = 2;

    static final int LOADING = 1;

    static final int NONE = 0;


  /**
    * This attribute indicates that the track should be enabled unless the user's preferences indicate that another track is more appropriate. This may only be used on one <code>track</code> element per media element.
    */
  boolean isDefaultValue();

  void setDefaultValue(boolean arg);


  /**
    * Kind of text track. The following keywords are allowed: <ul> <li>subtitles: A transcription or translation of the dialogue.</li> <li>captions: A transcription or translation of the dialogue or other sound effects. Suitable for users who are deaf or when the sound is muted.</li> <li>descriptions: Textual descriptions of the video content. Suitable for users who are blind.</li> <li>chapters: Chapter titles, intended to be used when the user is navigating the media resource.</li> <li>metadata: Tracks used by script. Not visible to the user.</li> </ul>
    */
  String getKind();

  void setKind(String arg);


  /**
    * A user-readable title of the text track Used by the browser when listing available text tracks.
    */
  String getLabel();

  void setLabel(String arg);

  int getReadyState();


  /**
    * Address of the track. Must be a valid URL. This attribute must be defined.
    */
  String getSrc();

  void setSrc(String arg);


  /**
    * Language of the track text data.
    */
  String getSrclang();

  void setSrclang(String arg);

  TextTrack getTrack();
}
