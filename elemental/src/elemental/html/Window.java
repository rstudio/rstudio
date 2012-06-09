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
import elemental.dom.Node;
import elemental.dom.Element;
import elemental.util.Indexable;
import elemental.events.EventTarget;
import elemental.dom.RequestAnimationFrameCallback;
import elemental.css.CSSRuleList;
import elemental.events.EventListener;
import elemental.dom.TimeoutHandler;
import elemental.dom.Document;
import elemental.css.CSSStyleDeclaration;
import elemental.events.Event;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;
import elemental.xpath.*;
import elemental.xml.*;


import java.util.Date;

/**
  * 
  */
public interface Window extends EventTarget {
  void clearOpener();

    static final int PERSISTENT = 1;

    static final int TEMPORARY = 0;

  ApplicationCache getApplicationCache();

  Navigator getClientInformation();

  boolean isClosed();

  Console getConsole();

  Crypto getCrypto();

  String getDefaultStatus();

  void setDefaultStatus(String arg);

  String getDefaultstatus();

  void setDefaultstatus(String arg);

  double getDevicePixelRatio();

  Document getDocument();

  Event getEvent();

  Element getFrameElement();

  Window getFrames();

  History getHistory();

  int getInnerHeight();

  int getInnerWidth();

  int getLength();

  Storage getLocalStorage();

  Location getLocation();

  void setLocation(Location arg);

  BarProp getLocationbar();

  BarProp getMenubar();

  String getName();

  void setName(String arg);

  Navigator getNavigator();

  boolean isOffscreenBuffering();

  EventListener getOnabort();

  void setOnabort(EventListener arg);

  EventListener getOnbeforeunload();

  void setOnbeforeunload(EventListener arg);

  EventListener getOnblur();

  void setOnblur(EventListener arg);

  EventListener getOncanplay();

  void setOncanplay(EventListener arg);

  EventListener getOncanplaythrough();

  void setOncanplaythrough(EventListener arg);

  EventListener getOnchange();

  void setOnchange(EventListener arg);

  EventListener getOnclick();

  void setOnclick(EventListener arg);

  EventListener getOncontextmenu();

  void setOncontextmenu(EventListener arg);

  EventListener getOndblclick();

  void setOndblclick(EventListener arg);

  EventListener getOndevicemotion();

  void setOndevicemotion(EventListener arg);

  EventListener getOndeviceorientation();

  void setOndeviceorientation(EventListener arg);

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

  EventListener getOndurationchange();

  void setOndurationchange(EventListener arg);

  EventListener getOnemptied();

  void setOnemptied(EventListener arg);

  EventListener getOnended();

  void setOnended(EventListener arg);

  EventListener getOnerror();

  void setOnerror(EventListener arg);

  EventListener getOnfocus();

  void setOnfocus(EventListener arg);

  EventListener getOnhashchange();

  void setOnhashchange(EventListener arg);

  EventListener getOninput();

  void setOninput(EventListener arg);

  EventListener getOninvalid();

  void setOninvalid(EventListener arg);

  EventListener getOnkeydown();

  void setOnkeydown(EventListener arg);

  EventListener getOnkeypress();

  void setOnkeypress(EventListener arg);

  EventListener getOnkeyup();

  void setOnkeyup(EventListener arg);

  EventListener getOnload();

  void setOnload(EventListener arg);

  EventListener getOnloadeddata();

  void setOnloadeddata(EventListener arg);

  EventListener getOnloadedmetadata();

  void setOnloadedmetadata(EventListener arg);

  EventListener getOnloadstart();

  void setOnloadstart(EventListener arg);

  EventListener getOnmessage();

  void setOnmessage(EventListener arg);

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

  EventListener getOnoffline();

  void setOnoffline(EventListener arg);

  EventListener getOnonline();

  void setOnonline(EventListener arg);

  EventListener getOnpagehide();

  void setOnpagehide(EventListener arg);

  EventListener getOnpageshow();

  void setOnpageshow(EventListener arg);

  EventListener getOnpause();

  void setOnpause(EventListener arg);

  EventListener getOnplay();

  void setOnplay(EventListener arg);

  EventListener getOnplaying();

  void setOnplaying(EventListener arg);

  EventListener getOnpopstate();

  void setOnpopstate(EventListener arg);

  EventListener getOnprogress();

  void setOnprogress(EventListener arg);

  EventListener getOnratechange();

  void setOnratechange(EventListener arg);

  EventListener getOnreset();

  void setOnreset(EventListener arg);

  EventListener getOnresize();

  void setOnresize(EventListener arg);

  EventListener getOnscroll();

  void setOnscroll(EventListener arg);

  EventListener getOnsearch();

  void setOnsearch(EventListener arg);

  EventListener getOnseeked();

  void setOnseeked(EventListener arg);

  EventListener getOnseeking();

  void setOnseeking(EventListener arg);

  EventListener getOnselect();

  void setOnselect(EventListener arg);

  EventListener getOnstalled();

  void setOnstalled(EventListener arg);

  EventListener getOnstorage();

  void setOnstorage(EventListener arg);

  EventListener getOnsubmit();

  void setOnsubmit(EventListener arg);

  EventListener getOnsuspend();

  void setOnsuspend(EventListener arg);

  EventListener getOntimeupdate();

  void setOntimeupdate(EventListener arg);

  EventListener getOntouchcancel();

  void setOntouchcancel(EventListener arg);

  EventListener getOntouchend();

  void setOntouchend(EventListener arg);

  EventListener getOntouchmove();

  void setOntouchmove(EventListener arg);

  EventListener getOntouchstart();

  void setOntouchstart(EventListener arg);

  EventListener getOnunload();

  void setOnunload(EventListener arg);

  EventListener getOnvolumechange();

  void setOnvolumechange(EventListener arg);

  EventListener getOnwaiting();

  void setOnwaiting(EventListener arg);

  EventListener getOnwebkitanimationend();

  void setOnwebkitanimationend(EventListener arg);

  EventListener getOnwebkitanimationiteration();

  void setOnwebkitanimationiteration(EventListener arg);

  EventListener getOnwebkitanimationstart();

  void setOnwebkitanimationstart(EventListener arg);

  EventListener getOnwebkittransitionend();

  void setOnwebkittransitionend(EventListener arg);

  Window getOpener();

  int getOuterHeight();

  int getOuterWidth();

  PagePopupController getPagePopupController();

  int getPageXOffset();

  int getPageYOffset();

  Window getParent();

  Performance getPerformance();

  BarProp getPersonalbar();

  Screen getScreen();

  int getScreenLeft();

  int getScreenTop();

  int getScreenX();

  int getScreenY();

  int getScrollX();

  int getScrollY();

  BarProp getScrollbars();

  Window getSelf();

  Storage getSessionStorage();

  String getStatus();

  void setStatus(String arg);

  BarProp getStatusbar();

  StyleMedia getStyleMedia();

  BarProp getToolbar();

  Window getTop();

  IDBFactory getWebkitIndexedDB();

  NotificationCenter getWebkitNotifications();

  StorageInfo getWebkitStorageInfo();

  Window getWindow();

  EventRemover addEventListener(String type, EventListener listener);

  EventRemover addEventListener(String type, EventListener listener, boolean useCapture);

  void alert(String message);

  String atob(String string);

  void blur();

  String btoa(String string);

  void captureEvents();

  void clearInterval(int handle);

  void clearTimeout(int handle);

  void close();

  boolean confirm(String message);

  boolean dispatchEvent(Event evt);

  boolean find(String string, boolean caseSensitive, boolean backwards, boolean wrap, boolean wholeWord, boolean searchInFrames, boolean showDialog);

  void focus();

  CSSStyleDeclaration getComputedStyle(Element element, String pseudoElement);

  CSSRuleList getMatchedCSSRules(Element element, String pseudoElement);

  Selection getSelection();

  MediaQueryList matchMedia(String query);

  void moveBy(float x, float y);

  void moveTo(float x, float y);

  Window open(String url, String name);

  Window open(String url, String name, String options);

  Database openDatabase(String name, String version, String displayName, int estimatedSize, DatabaseCallback creationCallback);

  Database openDatabase(String name, String version, String displayName, int estimatedSize);

  void postMessage(Object message, String targetOrigin);

  void postMessage(Object message, String targetOrigin, Indexable messagePorts);

  void print();

  String prompt(String message, String defaultValue);

  void releaseEvents();

  void removeEventListener(String type, EventListener listener);

  void removeEventListener(String type, EventListener listener, boolean useCapture);

  void resizeBy(float x, float y);

  void resizeTo(float width, float height);

  void scroll(int x, int y);

  void scrollBy(int x, int y);

  void scrollTo(int x, int y);

  int setInterval(TimeoutHandler handler, int timeout);

  int setTimeout(TimeoutHandler handler, int timeout);

  Object showModalDialog(String url);

  Object showModalDialog(String url, Object dialogArgs);

  Object showModalDialog(String url, Object dialogArgs, String featureArgs);

  void stop();

  void webkitCancelAnimationFrame(int id);

  void webkitCancelRequestAnimationFrame(int id);

  Point webkitConvertPointFromNodeToPage(Node node, Point p);

  Point webkitConvertPointFromPageToNode(Node node, Point p);

  void webkitPostMessage(Object message, String targetOrigin);

  void webkitPostMessage(Object message, String targetOrigin, Indexable transferList);

  int webkitRequestAnimationFrame(RequestAnimationFrameCallback callback);

  void webkitRequestFileSystem(int type, double size, FileSystemCallback successCallback, ErrorCallback errorCallback);

  void webkitRequestFileSystem(int type, double size, FileSystemCallback successCallback);

  void webkitResolveLocalFileSystemURL(String url, EntryCallback successCallback);

  void webkitResolveLocalFileSystemURL(String url);

  void webkitResolveLocalFileSystemURL(String url, EntryCallback successCallback, ErrorCallback errorCallback);

  AudioElement newAudioElement(String src);

  CSSMatrix newCSSMatrix(String cssValue);

  DOMParser newDOMParser();

  DOMURL newDOMURL();

  DeprecatedPeerConnection newDeprecatedPeerConnection(String serverConfiguration, SignalingCallback signalingCallback);

  EventSource newEventSource(String scriptUrl);

  FileReader newFileReader();

  FileReaderSync newFileReaderSync();

  Float32Array newFloat32Array(int length);

  Float32Array newFloat32Array(IndexableNumber list);

  Float32Array newFloat32Array(ArrayBuffer buffer, int byteOffset, int length);

  Float64Array newFloat64Array(int length);

  Float64Array newFloat64Array(IndexableNumber list);

  Float64Array newFloat64Array(ArrayBuffer buffer, int byteOffset, int length);

  IceCandidate newIceCandidate(String label, String candidateLine);

  Int16Array newInt16Array(int length);

  Int16Array newInt16Array(IndexableNumber list);

  Int16Array newInt16Array(ArrayBuffer buffer, int byteOffset, int length);

  Int32Array newInt32Array(int length);

  Int32Array newInt32Array(IndexableNumber list);

  Int32Array newInt32Array(ArrayBuffer buffer, int byteOffset, int length);

  Int8Array newInt8Array(int length);

  Int8Array newInt8Array(IndexableNumber list);

  Int8Array newInt8Array(ArrayBuffer buffer, int byteOffset, int length);

  MediaController newMediaController();

  MediaStream newMediaStream(MediaStreamTrackList audioTracks, MediaStreamTrackList videoTracks);

  MessageChannel newMessageChannel();

  Notification newNotification(String title, Mappable options);

  OptionElement newOptionElement(String data, String value, boolean defaultSelected, boolean selected);

  PeerConnection00 newPeerConnection00(String serverConfiguration, IceCallback iceCallback);

  SessionDescription newSessionDescription(String sdp);

  ShadowRoot newShadowRoot(Element host);

  SharedWorker newSharedWorker(String scriptURL, String name);

  SpeechGrammar newSpeechGrammar();

  SpeechGrammarList newSpeechGrammarList();

  SpeechRecognition newSpeechRecognition();

  TextTrackCue newTextTrackCue(String id, double startTime, double endTime, String text, String settings, boolean pauseOnExit);

  Uint16Array newUint16Array(int length);

  Uint16Array newUint16Array(IndexableNumber list);

  Uint16Array newUint16Array(ArrayBuffer buffer, int byteOffset, int length);

  Uint32Array newUint32Array(int length);

  Uint32Array newUint32Array(IndexableNumber list);

  Uint32Array newUint32Array(ArrayBuffer buffer, int byteOffset, int length);

  Uint8Array newUint8Array(int length);

  Uint8Array newUint8Array(IndexableNumber list);

  Uint8Array newUint8Array(ArrayBuffer buffer, int byteOffset, int length);

  Uint8ClampedArray newUint8ClampedArray(int length);

  Uint8ClampedArray newUint8ClampedArray(IndexableNumber list);

  Uint8ClampedArray newUint8ClampedArray(ArrayBuffer buffer, int byteOffset, int length);

  Worker newWorker(String scriptUrl);

  XMLHttpRequest newXMLHttpRequest();

  XMLSerializer newXMLSerializer();

  XPathEvaluator newXPathEvaluator();

  XSLTProcessor newXSLTProcessor();
}
