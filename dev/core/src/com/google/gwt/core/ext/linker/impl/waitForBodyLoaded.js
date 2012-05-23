// Setup code which waits for the body to be loaded and then calls the
// callback function
function setupWaitForBodyLoad(callback) {
  // Provides the isBodyLoaded() function
  __IS_BODY_LOADED__
  
  var bodyDone = isBodyLoaded();

  if (bodyDone) {
    callback();
    return;
  }

  // If the page is not already loaded, setup some listeners and timers to
  // detect when it is done.
  function onBodyDone() {
    if (!bodyDone) {
      bodyDone = true;
      callback();

      if ($doc.removeEventListener) {
        $doc.removeEventListener("DOMContentLoaded", onBodyDone, false);
      }
      if (onBodyDoneTimerId) {
        clearInterval(onBodyDoneTimerId);
      }
    }
  }

  // For everyone that supports DOMContentLoaded.
  if ($doc.addEventListener) {
    $doc.addEventListener("DOMContentLoaded", onBodyDone, false);
  }

  // Fallback. If onBodyDone() gets fired twice, it's not a big deal.
  var onBodyDoneTimerId = setInterval(function() {
    if (isBodyLoaded()) {
      onBodyDone();
    }
  }, 50);
}
