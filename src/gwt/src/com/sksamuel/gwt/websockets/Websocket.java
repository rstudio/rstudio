/**
 Copyright 2013 Stephen Samuel

 Licensed under the Apache License,Version2.0(the"License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,software
 distributed under the License is distributed on an"AS IS"BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */
package com.sksamuel.gwt.websockets;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Stephen K Samuel 14 Sep 2012 08:58:55
 */
public class Websocket {

    private static int counter = 1;

    private static native boolean _isWebsocket() /*-{
        return ("WebSocket" in window);
    }-*/;

    public static boolean isSupported() {
        return _isWebsocket();
    }

    private final Set<WebsocketListener> listeners = new HashSet<WebsocketListener>();

    private final String varName;
    private final String url;

    public Websocket(String url) {
        this.url = url;
        this.varName = "gwtws-" + counter++;
    }

    private native void _close(String s) /*-{
        $wnd[s].close();
    }-*/;

    private native void _open(Websocket ws, String s, String url) /*-{
        $wnd[s] = new WebSocket(url);
        $wnd[s].onopen = function() { ws.@com.sksamuel.gwt.websockets.Websocket::onOpen()(); };
        $wnd[s].onclose = function(evt) { ws.@com.sksamuel.gwt.websockets.Websocket::onClose(SLjava/lang/String;Z)(evt.code, evt.reason, evt.wasClean); };
        $wnd[s].onerror = function() { ws.@com.sksamuel.gwt.websockets.Websocket::onError()(); };
        $wnd[s].onmessage = function(msg) { ws.@com.sksamuel.gwt.websockets.Websocket::onMessage(Ljava/lang/String;)(msg.data); }
    }-*/;

    private native void _send(String s, String msg) /*-{
        $wnd[s].send(msg);
    }-*/;

    private native int _state(String s) /*-{
        return $wnd[s].readyState;
    }-*/;

    public void addListener(WebsocketListener listener) {
        listeners.add(listener);
    }
    public void removeListener(WebsocketListener listener) {
        listeners.remove(listener);
    }

    public void close() {
        _close(varName);
    }

    public int getState() {
        return _state(varName);
    }

    protected void onClose(short code, String reason, boolean wasClean) {
        CloseEvent event = new CloseEvent(code, reason, wasClean);
        for (WebsocketListener listener : listeners)
            listener.onClose(event);
    }

    protected void onError() {
        for (WebsocketListener listener : listeners) {
            if (listener instanceof WebsocketListenerExt) {
                ((WebsocketListenerExt)listener).onError();
            }
        }
    }

    protected void onMessage(String msg) {
        for (WebsocketListener listener : listeners) {
            listener.onMessage(msg);
            if (listener instanceof BinaryWebsocketListener) {
                byte[] bytes = Base64Utils.fromBase64(msg);
                ((BinaryWebsocketListener) listener).onMessage(bytes);
            }
        }
    }

    protected void onOpen() {
        for (WebsocketListener listener : listeners)
            listener.onOpen();
    }

    public void open() {
        _open(this, varName, url);
    }

    public void send(String msg) {
        _send(varName, msg);
    }

    public void send(byte[] bytes) {
        String base64 = Base64Utils.toBase64(bytes);
        send(base64);
    }
}