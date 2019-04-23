/*
 * SpellingPrefetcher.js
 *
 * Copyright (C) 2009-19 by RStudio, Inc.
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
var SpellingPrefetcher;

(function () {
    "use strict";

    SpellingPrefetcher = function(typoJsCode) {
        if (typeof(Worker) === "undefined" || typeof(URL) === "undefined" || typeof(Blob) === "undefined")
            return;

        if (!!this.w)
            return;

        this.typoJsCode = typoJsCode;
        /*
         *  Worker Definition
         *
         *  We are defining our worker inline like this for "simplicity's"
         *  sake due to our unique runtime environment in a Desktop deployment
         *  See: https://stackoverflow.com/questions/5408406/web-workers-without-a-separate-javascript-file
         *  If interested in deeper research
         */
        let blobURL = URL.createObjectURL(new Blob(['(', `
           function(){
              // inline typo.js
              ${this.typoJsCode}
              let typo = new Typo();
              
              onmessage = function(event) {
                 // input Typo data
                 let inTypo = event.data;
                 
                 // copy specific input Typo data into our local Typo object
                 ['rules', 'dictionaryTable', 'compoundRules', 'compoundRuleCodes', 'replacementTable',
                  'flags', 'memoized', 'loaded'].forEach(e => typo[e] = inTypo[e]);
                  
                 // run the prefetch on the inputwords
                 let s = {};
                 inTypo.inputwords.forEach(word => {
                    typo.suggest(word);
                    s[word] = typo.memoized[word];
                 });
                 this.postMessage(s);
              }
           }`,
            ')()'], {type: 'application/javascript'}));
        this.w = new Worker(blobURL);
        URL.revokeObjectURL(blobURL);
        /*
         * End Worker Definition
        */
    };

    SpellingPrefetcher.prototype = {
        prefetch : function (words, typojs) {
            if (!words || !typojs)
                return;

            // Worker output consumer, bound to local typojs parameter
            this.w.onmessage = function (event) {
                if (!!event.data) {
                    let suggestions = event.data;

                    for (let s in suggestions) {
                        if (suggestions.hasOwnProperty(s) && typeof suggestions[s] !== 'function') {
                            typojs.memoized[s] = suggestions[s];
                        }
                    }
                }
            };

            // Only run worker on words we know we don't have memoized
            // Possibly premature optimization but it can catch some cases
            // where we're calling on only already fetched data
            typojs.inputwords = [];
            words = words.split(',');
            words.forEach(w => {
               if (!typojs.memoized.hasOwnProperty(w)) {
                   typojs.inputwords.push(w);
               }
            });

            // Run Worker
            if (typojs.inputwords.length > 0)
                this.w.postMessage(typojs);
        }
    }
})();

if (typeof module !== 'undefined') {
    module.exports = SpellingPrefetcher;
}