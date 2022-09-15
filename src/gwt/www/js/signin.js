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

/**
 * Ensure error region is spoken by a screen reader.
 */
function speakError() {
   document.getElementById("live-error").innerText = document.getElementById("errortext").innerText;
}

/**
 * Verifies the sign-in form, returning true if sign in should proceed and false
 * if there's a problem.
 */
function verifyMe() {
   // Don't allow submitting the form if disabled
   if (document.getElementById('signinbutton').disabled) {
      return false;
   }

   // If a username is present, ensure it has a value
   var userEle = document.getElementById('username');
   if (userEle !== null) {
     if (userEle.value === '') {
        userEle.focus();
        showError('You must enter a username');
        return false;
     }
   }

   // If a password element is present, ensure it has a value
   var passwordEle = document.getElementById('password');
   if (passwordEle !== null) {
     if (passwordEle.value === '') {
        passwordEle.focus();
        showError('You must enter a password');
        return false;
     }
   }

   // Remember that we have an active sign-in (prevents us from detecting our own sign-in when
  // polling)
   activeSignIn = true;

   // Disable all sign-in controls to prevent attempts to sign in multiple times
   document.getElementById('staySignedIn').disabled = true;
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
                  var encrypted = encrypt(payload, exp, mod);
                  document.getElementById('persist').value = document.getElementById('staySignedIn').checked ? "1" : "0";
                  document.getElementById('package').value = encrypted;
                  document.getElementById('clientPath').value = window.location.pathname;
                  document.realform.submit();
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

  var xhr = new XMLHttpRequest();
  xhr.open("GET", "./", true);
  xhr.onreadystatechange = function() {
     if (activeSignIn)
       return;
     try {
        if (xhr.readyState === 4) {
           setTimeout(pollForSignin, 1000);
           if (xhr.status === 200) {
              var isSignIn = false;
              var url = xhr.responseURL.split('?')[0];
              var href = location.href.split('?')[0];
              var isSignIn = url === href;
              var controls = document.getElementById("controls");
              var goback = document.getElementById("goback");
              if (isSignIn) {
                 // This is the sign-in page; no external sign-in has occurred
                 controls.classList.remove('signinhidden');
                 goback.classList.add('signinhidden');
              } else {
                 // This is a different page; the user has signed in via another tab.
                 responseURL = url;
                 controls.classList.add('signinhidden');
                 goback.classList.remove('signinhidden');
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
      setTimeout(pollForSignin, 1000);
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
