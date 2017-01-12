(function(){
// add a listener for scroll events that occur at the top level in the bubble
// phase (i.e. are unhandled inside the HTML widget)
window.addEventListener("mousewheel", function(evt) {
   // if not in a iframe, do nothing
   if (!window.frameElement)
      return;

   // ignore if default event action was suppressed
   if (evt.defaultPrevented)
      return;

   // in an iframe, clone the event and treat it as a scroll on the frame's host
   // element
   window.frameElement.parentElement.dispatchEvent(
      new evt.constructor(evt.type, evt));
});
})();

