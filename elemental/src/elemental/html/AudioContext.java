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
import elemental.events.EventListener;
import elemental.events.EventTarget;

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
public interface AudioContext extends EventTarget {

  int getActiveSourceCount();

  float getCurrentTime();

  AudioDestinationNode getDestination();

  AudioListener getListener();

  EventListener getOncomplete();

  void setOncomplete(EventListener arg);

  float getSampleRate();

  RealtimeAnalyserNode createAnalyser();

  BiquadFilterNode createBiquadFilter();

  AudioBuffer createBuffer(int numberOfChannels, int numberOfFrames, float sampleRate);

  AudioBuffer createBuffer(ArrayBuffer buffer, boolean mixToMono);

  AudioBufferSourceNode createBufferSource();

  AudioChannelMerger createChannelMerger();

  AudioChannelMerger createChannelMerger(int numberOfInputs);

  AudioChannelSplitter createChannelSplitter();

  AudioChannelSplitter createChannelSplitter(int numberOfOutputs);

  ConvolverNode createConvolver();

  DelayNode createDelayNode();

  DelayNode createDelayNode(double maxDelayTime);

  DynamicsCompressorNode createDynamicsCompressor();

  AudioGainNode createGainNode();

  JavaScriptAudioNode createJavaScriptNode(int bufferSize);

  JavaScriptAudioNode createJavaScriptNode(int bufferSize, int numberOfInputChannels);

  JavaScriptAudioNode createJavaScriptNode(int bufferSize, int numberOfInputChannels, int numberOfOutputChannels);

  MediaElementAudioSourceNode createMediaElementSource(MediaElement mediaElement);

  Oscillator createOscillator();

  AudioPannerNode createPanner();

  WaveShaperNode createWaveShaper();

  WaveTable createWaveTable(Float32Array real, Float32Array imag);

  void decodeAudioData(ArrayBuffer audioData, AudioBufferCallback successCallback);

  void decodeAudioData(ArrayBuffer audioData, AudioBufferCallback successCallback, AudioBufferCallback errorCallback);

  void startRendering();
}
