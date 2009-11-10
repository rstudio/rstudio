if (window != top && location.href.indexOf("hosted.html") >= 0) {
  var port = chrome.extension.connect();
  port.onMessage.addListener(function(msg) {
    if (msg.name == "tabId") {
      var doc = window.document;
      var div = document.createElement("div");
      div.id = "$__gwt_tab_id";
      div.textContent = "" + msg.tabId;
      doc.body.appendChild(div);
      // console.log("record_tab_id.js " + msg.tabId);
    }
  });
}
