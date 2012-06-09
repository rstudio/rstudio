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
package elemental.js.html;
import elemental.html.WaveShaperNode;
import elemental.html.AudioListener;
import elemental.html.WaveTable;
import elemental.html.AudioPannerNode;
import elemental.html.ArrayBuffer;
import elemental.html.MediaElement;
import elemental.html.RealtimeAnalyserNode;
import elemental.html.DynamicsCompressorNode;
import elemental.html.AudioBufferSourceNode;
import elemental.html.Oscillator;
import elemental.html.Float32Array;
import elemental.html.AudioGainNode;
import elemental.js.events.JsEventListener;
import elemental.html.AudioBuffer;
import elemental.html.DelayNode;
import elemental.html.ConvolverNode;
import elemental.html.AudioDestinationNode;
import elemental.html.AudioContext;
import elemental.html.AudioChannelMerger;
import elemental.html.AudioBufferCallback;
import elemental.html.BiquadFilterNode;
import elemental.html.JavaScriptAudioNode;
import elemental.html.AudioChannelSplitter;
import elemental.html.MediaElementAudioSourceNode;
import elemental.events.EventListener;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.js.stylesheets.*;
import elemental.js.events.*;
import elemental.js.util.*;
import elemental.js.dom.*;
import elemental.js.html.*;
import elemental.js.css.*;
import elemental.js.stylesheets.*;

import java.util.Date;

public class JsAudioContext extends JsElementalMixinBase  implements AudioContext {
  protected JsAudioContext() {}

  public final native int getActiveSourceCount() /*-{
    return this.activeSourceCount;
  }-*/;

  public final native float getCurrentTime() /*-{
    return this.currentTime;
  }-*/;

  public final native JsAudioDestinationNode getDestination() /*-{
    return this.destination;
  }-*/;

  public final native JsAudioListener getListener() /*-{
    return this.listener;
  }-*/;

  public final native EventListener getOncomplete() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.oncomplete);
  }-*/;

  public final native void setOncomplete(EventListener listener) /*-{
    this.oncomplete = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native float getSampleRate() /*-{
    return this.sampleRate;
  }-*/;

  public final native JsRealtimeAnalyserNode createAnalyser() /*-{
    return this.createAnalyser();
  }-*/;

  public final native JsBiquadFilterNode createBiquadFilter() /*-{
    return this.createBiquadFilter();
  }-*/;

  public final native JsAudioBuffer createBuffer(int numberOfChannels, int numberOfFrames, float sampleRate) /*-{
    return this.createBuffer(numberOfChannels, numberOfFrames, sampleRate);
  }-*/;

  public final native JsAudioBuffer createBuffer(ArrayBuffer buffer, boolean mixToMono) /*-{
    return this.createBuffer(buffer, mixToMono);
  }-*/;

  public final native JsAudioBufferSourceNode createBufferSource() /*-{
    return this.createBufferSource();
  }-*/;

  public final native JsAudioChannelMerger createChannelMerger() /*-{
    return this.createChannelMerger();
  }-*/;

  public final native JsAudioChannelMerger createChannelMerger(int numberOfInputs) /*-{
    return this.createChannelMerger(numberOfInputs);
  }-*/;

  public final native JsAudioChannelSplitter createChannelSplitter() /*-{
    return this.createChannelSplitter();
  }-*/;

  public final native JsAudioChannelSplitter createChannelSplitter(int numberOfOutputs) /*-{
    return this.createChannelSplitter(numberOfOutputs);
  }-*/;

  public final native JsConvolverNode createConvolver() /*-{
    return this.createConvolver();
  }-*/;

  public final native JsDelayNode createDelayNode() /*-{
    return this.createDelayNode();
  }-*/;

  public final native JsDelayNode createDelayNode(double maxDelayTime) /*-{
    return this.createDelayNode(maxDelayTime);
  }-*/;

  public final native JsDynamicsCompressorNode createDynamicsCompressor() /*-{
    return this.createDynamicsCompressor();
  }-*/;

  public final native JsAudioGainNode createGainNode() /*-{
    return this.createGainNode();
  }-*/;

  public final native JsJavaScriptAudioNode createJavaScriptNode(int bufferSize) /*-{
    return this.createJavaScriptNode(bufferSize);
  }-*/;

  public final native JsJavaScriptAudioNode createJavaScriptNode(int bufferSize, int numberOfInputChannels) /*-{
    return this.createJavaScriptNode(bufferSize, numberOfInputChannels);
  }-*/;

  public final native JsJavaScriptAudioNode createJavaScriptNode(int bufferSize, int numberOfInputChannels, int numberOfOutputChannels) /*-{
    return this.createJavaScriptNode(bufferSize, numberOfInputChannels, numberOfOutputChannels);
  }-*/;

  public final native JsMediaElementAudioSourceNode createMediaElementSource(MediaElement mediaElement) /*-{
    return this.createMediaElementSource(mediaElement);
  }-*/;

  public final native JsOscillator createOscillator() /*-{
    return this.createOscillator();
  }-*/;

  public final native JsAudioPannerNode createPanner() /*-{
    return this.createPanner();
  }-*/;

  public final native JsWaveShaperNode createWaveShaper() /*-{
    return this.createWaveShaper();
  }-*/;

  public final native JsWaveTable createWaveTable(Float32Array real, Float32Array imag) /*-{
    return this.createWaveTable(real, imag);
  }-*/;

  public final native void decodeAudioData(ArrayBuffer audioData, AudioBufferCallback successCallback) /*-{
    this.decodeAudioData(audioData, $entry(successCallback.@elemental.html.AudioBufferCallback::onAudioBufferCallback(Lelemental/html/AudioBuffer;)).bind(successCallback));
  }-*/;

  public final native void decodeAudioData(ArrayBuffer audioData, AudioBufferCallback successCallback, AudioBufferCallback errorCallback) /*-{
    this.decodeAudioData(audioData, $entry(successCallback.@elemental.html.AudioBufferCallback::onAudioBufferCallback(Lelemental/html/AudioBuffer;)).bind(successCallback), $entry(errorCallback.@elemental.html.AudioBufferCallback::onAudioBufferCallback(Lelemental/html/AudioBuffer;)).bind(errorCallback));
  }-*/;

  public final native void startRendering() /*-{
    this.startRendering();
  }-*/;
}
