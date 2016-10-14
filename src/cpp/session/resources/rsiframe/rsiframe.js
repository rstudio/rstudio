/*jshint browser:true, strict:false, curly:false, indent:3*/
(function(){
// returns the value of a querystring variable passed to this script, or null
// if the variable is not specified
var getQueryVal = function(name) {
   // find the rsiframe script in the doc so we can examine its params
   var queryStr = "";
   for (var i = 0; i < document.scripts.length; i++) {
      var src = document.scripts[i].src;
      if (src.indexOf("/rsiframe.js?") > 0) {
        queryStr = src.substr(src.indexOf("?") + 1);
        break;
      }
   }

   // parse the querystring and find the sought variable
   var pairs = queryStr.split("&");
   for (var i = 0; i < pairs.length; i++) {
      var pair = pairs[i].split("=");
      if (pair.length === 2 && pair[0] === name) {
         return pair[1];
      }
   }
   return null;
};

// returns the origin we should post messages to (if on same host), or null if
// no suitable origin is found
var getOrigin = function() {
   var origin = null; 

   // if an origin is supplied, use it directly. origins are supplied directly
   // on Qt, since it doesn't return a value for document.referrer reliably.
   var queryOrigin = getQueryVal("origin");
   if (queryOrigin !== null && queryOrigin.length > 0) {
      return window.location.protocol + "//" + queryOrigin;
   }

   // function to normalize hostnames
   var normalize = function(hostname) {
     if (hostname == "127.0.0.1")
       return "localhost";
     else
       return hostname;
   };

   // construct the parent origin if the hostnames match
   var parentUrl = (parent !== window) ? document.referrer : null;

   if (parentUrl) {
     // parse the parent href
     var a = document.createElement('a');
     a.href = parentUrl;

     if (normalize(a.hostname) == normalize(window.location.hostname)) {
       var protocol = a.protocol.replace(':',''); // browser compatability
       origin = protocol + '://' + a.hostname;
       if (a.port)
         origin = origin + ':' + a.port;
     } 
   }
   return origin;
};

// property application ------------------------------------------------------

// sets the location hash
var setHash = function(hash) {
   // special case for ioslides: if the hash is numeric and we have a slide
   // controller, use it to change the slides (ioslides doesn't respond to
   // onhashchange)
   if (/[0-9]+/.test(hash) && window.slidedeck) {
      window.slidedeck.loadSlide(parseInt(hash));
   } else {
      location.hash = hash;
   }
};

// sets the document scroll position. this could be called before that part of
// the document has loaded, so if the position specified is not yet available,
// wait a few ms and try again.
var setScrollPos = function(pos) {
   if (pos > document.body.scrollHeight) {
      window.setTimeout(function() { setScrollPos(pos); }, 100);
   } else {
      document.body.scrollTop = pos;
   }
};

// cross-domain communication ------------------------------------------------

// obtain and validate the origin
var origin = getOrigin();
if (origin === null)
   return;

// set up cross-domain send/receive
var send = function(data) {
   data.type = "ShinyFrameEvent";
   parent.postMessage(data, origin);
};

var recv = function(evt) {
   // validate that message is from expected origin (i.e. our parent)
   if (evt.origin !== origin) 
      return;

   switch (evt.data.method) {
   case "rs_set_scroll_pos":
      setScrollPos(evt.data.arg);
      break;
   case "rs_set_hash": 
      setHash(evt.data.arg);
      break;
   }
};

window.addEventListener("message", recv, false); 

// document event handlers ---------------------------------------------------

// notify parent when scroll stops changing for ~0.25 s (don't spam during
// continuous/smooth scrolling)
var scrollTimer = 0;
var onScroll = function(pos) {
   if (scrollTimer !== 0)
      window.clearTimeout(scrollTimer);
   scrollTimer = window.setTimeout(function() {
      send({ event: "doc_scroll_change", data: document.body.scrollTop });
   }, 250);
};

// test the href for changes every 100ms, and notify parent if it has changed.
// ordinarily we'd hook an event handler to 'hashchange' here, but in some 
// cases (e.g. ioslides) the document can change the hash without triggering
// a hashchange event.
var currentHref = location.href;
var testHrefChange = function() {
   if (currentHref !== location.href) {
      currentHref = location.href;
      send({ event: "doc_hash_change", data: location.href });
   }
};
window.setInterval(testHrefChange, 100);

window.addEventListener("scroll", onScroll, false); 

// let parent know we're ready once the event loop finishes
window.setTimeout(function() {
   send({ event: "doc_ready", data: null });
}, 0);

// mathjax setup -------------------------------------------------------------

// if this is a Qt-based browser on Windows, inject the MathJax configuration
// data.  at the time this runs it's not possible to know whether the document
// uses MathJax or not.
if (window.navigator.userAgent.indexOf(" Qt/") > 0 &&
    window.navigator.userAgent.indexOf("Windows") > 0) {
   var s = document.createElement("script");
   s.type = "text/x-mathjax-config";
   s.textContent = 
      'MathJax.Hub.Config({' + 
      '  "HTML-CSS": { minScaleAdjust: 125, availableFonts: [] } ' +
      '});';
   document.head.appendChild(s);
}
})();

