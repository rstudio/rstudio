/*
 * signin.js
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

// Global variable; tracks URL for sign-in response
var responseURL = "";

// Global variable; tracks whether an active sign-in is in progress
var activeSignIn = false;

var dbDeleteInProgress = false;

// Interval (ms) between background polls that detect a sign-in from another tab.
var POLL_INTERVAL_MS = 3000;

// Handle for the pending poll timer, so we can cancel/replace it (e.g. when the
// tab becomes visible again) without spawning overlapping poll chains.
var pollTimer = null;

// Whether a poll request is currently in flight; guards against issuing a second
// overlapping request if a visibility change fires mid-request.
var pollInFlight = false;

/**
 * Schedules the next sign-in poll, replacing any already-pending poll so we
 * never end up with more than one poll chain running at a time.
 */
function schedulePoll(delayMs) {
   if (pollTimer !== null) {
      clearTimeout(pollTimer);
   }
   pollTimer = setTimeout(function() {
      pollTimer = null;
      pollForSignin();
   }, delayMs);
}

/**
 * Ensure error region is spoken by a screen reader.
 */
function speakError() {
   document.getElementById("live-error").innerText = document.getElementById("errortext").innerText;
}

/**
 * Marks both credential fields invalid and associates them with the error text.
 * Called only for credential/sign-in errors (empty-field validation in verifyMe);
 * never from the prepare() network/crypto error paths.
 */
function setCredentialErrorState() {
   var u = document.getElementById('username');
   var p = document.getElementById('password');
   if (u !== null) {
      u.setAttribute('aria-invalid', 'true');
      u.setAttribute('aria-describedby', 'errortext');
   }
   if (p !== null) {
      p.setAttribute('aria-invalid', 'true');
      p.setAttribute('aria-describedby', 'errortext');
   }
}

/**
 * Verifies the sign-in form, returning true if sign in should proceed and false
 * if there's a problem.
 */
function verifyMe() {
   // Clear any stale credential-error ARIA state from a previous submit attempt.
   // Done before any early return so a non-credential failure path can never see
   // stale aria-invalid/aria-describedby. These are re-set below only if an
   // empty-field (credential) error fires.
   var uReset = document.getElementById('username');
   var pReset = document.getElementById('password');
   if (uReset !== null) {
      uReset.setAttribute('aria-invalid', 'false');
      uReset.removeAttribute('aria-describedby');
   }
   if (pReset !== null) {
      pReset.setAttribute('aria-invalid', 'false');
      pReset.removeAttribute('aria-describedby');
   }

   // Don't allow submitting the form if disabled
   if (document.getElementById('signinbutton').disabled) {
      return false;
   }

   // If a username is present, ensure it has a value
   var userEle = document.getElementById('username');
   if (userEle !== null) {
     if (userEle.value === '') {
        userEle.focus();
        setCredentialErrorState();
        showError('You must enter a username');
        return false;
     }
   }

   // If a password element is present, ensure it has a value
   var passwordEle = document.getElementById('password');
   if (passwordEle !== null) {
     if (passwordEle.value === '') {
        passwordEle.focus();
        setCredentialErrorState();
        showError('You must enter a password');
        return false;
     }
   }

   // Remember that we have an active sign-in (prevents us from detecting our own sign-in when
  // polling)
   activeSignIn = true;

   // Disable all sign-in controls to prevent attempts to sign in multiple times
   document.getElementById('staySignedIn').readOnly = true;
   document.getElementById('signinbutton').disabled = true;
   document.getElementById('signinbutton').classList.add('disabled');
   document.getElementById('spinner').classList.remove('signin-hidden');
   document.getElementById('progress-message').innerText = "Signing in";

   setTimeout(function () {
      // Disable username/password controls after event loop so they are enabled at the time the
     // form is actually submitted.
      if (userEle !== null)
         userEle.disabled = true;
      if (passwordEle !== null)
         passwordEle.disabled = true;
   }, 0);

   // Form is valid
   return true;
}

/**
 * Displays an error in the designated error panel.
 */
function showError(errorMessage) {
   var errorDiv = document.getElementById('errorpanel');
   errorDiv.innerHTML = '';
   var errorp = document.createElement('p');
   errorp.id = "errortext";
   errorDiv.appendChild(errorp);
   if (typeof(errorp.innerText) === 'undefined')
      errorp.textContent = errorMessage;
   else
      errorp.innerText = errorMessage;
   errorDiv.style.display = 'block';
   speakError();
}

/**
 * Prepares the form to be submitted by encrypting the username and password.
 */
function prepare() {
   // Ensure the form is valid before proceeding
   if (!verifyMe())
      return false;

   try {
      var payload = document.getElementById('username').value + "\n" +
                    document.getElementById('password').value;
      var xhr = new XMLHttpRequest();
      var metas = document.getElementsByTagName("meta");
      var url = "";
      for (var i = 0; i < metas.length; i++) {
         if (metas[i].getAttribute("name") === "public-key-url") {
            url = metas[i].getAttribute("content");
            break;
         }
      }
      if (url === "") {
         showError("Cannot determine server's public key for password encryption;" +
                   "missing <meta> tag.");
         return;
      }

      xhr.open("GET", url, true);
      xhr.onreadystatechange = function() {
         try {
            if (xhr.readyState == 4) {
               if (xhr.status != 200) {
                  var errorMessage;
                  if (xhr.status == 0)
                     errorMessage = "Error: Could not reach server--check your internet connection";
                  else
                     errorMessage = "Error: " + xhr.statusText;
                  showError(errorMessage);
               }
               else {
                  var response = xhr.responseText;
                  var chunks = response.split(':', 2);
                  var exp = chunks[0];
                  var mod = chunks[1];
                  encrypt(payload, exp, mod).then(function (result) {
                     document.getElementById('persist').value = document.getElementById('staySignedIn').checked ? "1" : "0";
                     if (result.alg) {
                        document.getElementById('package').value = '$' + result.alg + '$' + result.ct;
                     } else {
                        document.getElementById('package').value = result.ct;
                     }
                     document.getElementById('clientPath').value = window.location.pathname;
                     document.realform.submit();
                  }).catch(function (exception) {
                     showError("Error: " + exception);
                  });
               }
            }
         } catch (exception) {
            showError("Error: " + exception);
         }
      };
      xhr.send(null);
   } catch (exception) {
      showError("Error: " + exception);
   }
}

/**
 * Submits the sign-in form after preparing by encrypting secrets.
 */
function submitRealForm() {
  if (prepare()) {
    document.realform.submit();
  }
}

/**
 * Checks to see if the user has already signed in via another tab.
 */
function pollForSignin() {
  if (activeSignIn)
     return;

  // Don't poll while the tab is hidden - there's no point checking for a
  // sign-in the user can't see, and it wastes requests. The visibilitychange
  // handler resumes polling immediately when the tab becomes visible again.
  if (document.hidden)
     return;

  // Guard against overlapping requests (e.g. if the tab is made visible while a
  // poll is already in flight).
  if (pollInFlight)
     return;
  pollInFlight = true;

  var xhr = new XMLHttpRequest();
  // Cache-bust the poll: without this a "./" response cached from a previous
  // (signed-in) session can be replayed by the browser, and a stale/cached
  // response can also come back with an empty responseURL - either of which
  // used to be misread as "signed in elsewhere" and spuriously showed the
  // "Signed Out" dialog on a fresh sign-in page.
  xhr.open("GET", "./?rs-signin-poll=" + Date.now(), true);
  xhr.setRequestHeader("Cache-Control", "no-cache");
  xhr.onreadystatechange = function() {
     if (activeSignIn)
       return;
     try {
        if (xhr.readyState === 4) {
           pollInFlight = false;
           // Only queue the next poll if the tab is still visible; otherwise the
           // visibilitychange handler will resume polling when it reappears.
           if (!document.hidden)
              schedulePoll(POLL_INTERVAL_MS);
           if (xhr.status === 200) {
              var url = xhr.responseURL.split('?')[0];
              var href = location.href.split('?')[0];
              var controls = document.getElementById("controls");
              var goback = document.getElementById("goback");
              // Only conclude the user signed in elsewhere on a positive signal:
              // a non-empty responseURL that is genuinely a different page than
              // this sign-in page. An empty responseURL means we can't tell (some
              // browsers leave it empty for cached/opaque responses), so we treat
              // it as "still on the sign-in page" rather than falsely reporting a
              // sign-in.
              if (url && url !== href) {
                 // A different page - the user has signed in via another tab.
                 responseURL = url;
                 controls.classList.add('signinhidden');
                 goback.classList.remove('signinhidden');
              } else {
                 // Still the sign-in page (or indeterminate); keep showing controls.
                 controls.classList.remove('signinhidden');
                 goback.classList.add('signinhidden');
              }
           }
        }
     } catch (exception) {
       showError("Error: " + exception);
     }
   };
   xhr.send(null);
}

window.addEventListener("load", function() {
   // Is this sign-in form interactive? (i.e., must you enter a username?)
   var userEle = document.getElementById('username');

   if (userEle === null) {
      // No username element; place focus on the sign in button if we have one
      var buttonEle = document.getElementById('signinbutton');
      if (buttonEle !== null) {
         buttonEle.focus();
      }
   } else {
      // Place focus on the username element if it exists
      userEle.focus();

      // Begin polling for sign-ins from other tabs (we only do this for interactive forms)
      schedulePoll(POLL_INTERVAL_MS);

      // Pause polling while the tab is hidden and resume immediately when it
      // becomes visible again, so a background tab isn't polling needlessly.
      document.addEventListener("visibilitychange", function() {
         if (!document.hidden && !activeSignIn) {
            schedulePoll(0);
         }
      });
   }


   // If we have an error panel, ensure it is announced to screen readers
   var errorPanel = document.getElementById('errorpanel');

   if (errorPanel !== null) {
     var displayProp = window.getComputedStyle(errorPanel, null).getPropertyValue("display");
     if (displayProp !== "none") {
        document.title = "Error: RStudio Sign In Failed";
        // If error message displayed, give time for screen reader to catch up then
        // copy error message to aria-live region to trigger announcement
        setTimeout(function () {
           speakError();
        }, 2000);
     }
   }
});
