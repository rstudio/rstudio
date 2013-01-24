
function mediaManager(media, atCommands) {

  // track state for at command execution
  nextAtCommandIndex = 0;
  previousTime = 0;
  
  // reset at command index and reload after ending
  media.addEventListener('ended', function(e) { 
    nextAtCommandIndex = 0;
    media.load();
  }, false);
  
  // track time events and fire at commands
  media.addEventListener('timeupdate', function(e) {  
    // adjust command index for backward seeks
    if (!media.seeking) {
       if (media.currentTime < previousTime) {
          nextAtCommandIndex = 0;
          for (i = 0; i<atCommands.length; i++) {
             if (media.currentTime < atCommands[i].at) {
                nextAtCommandIndex = i;
                break;
             }
          }
       }
       previousTime = media.currentTime;
    }
  
    // see if a command needs to be triggered
    if (nextAtCommandIndex < atCommands.length) {
       nextCommand = atCommands[nextAtCommandIndex];
       if (media.currentTime > nextCommand.at) {
          window.parent.dispatchLearningCommand(nextCommand.command);
          nextAtCommandIndex++;
       }
    }
  }, false);
  
  return {
    play: function() { media.play(); }
  };
}

function pausePlayers(players) {
  for(var i = 0; i < players.length; i++) {
    players[i].pause();
  }
}
    
    
function notifySlideChanged(indexh) {

  // pause all audio and video
  pausePlayers(document.getElementsByTagName('video'));
  pausePlayers(document.getElementsByTagName('audio'))
 
  // execute slide-specific commants 
 
  
  if (window.parent.learningSlideChanged)
    window.parent.learningSlideChanged(event.indexh, 
                                       commandsForSlide(event.indexh));  
}

Reveal.initialize({
  controls: true,
  progress: true,
  history: true,
  overview: false,
  center: false,
  rollingLinks: false,

  theme: 'simple', 
  transition: 'linear', 

  dependencies: [
    { src: 'lib/js/classList.js', condition: function() { return !document.body.classList; } }
  ]
});

Reveal.addEventListener( 'ready', function( event ) {
   // notify container
  notifySlideChanged(event.indexh)
} );

Reveal.addEventListener( 'slidechanged', function( event ) {
     
  // notify container
  notifySlideChanged(event.indexh)
 
  // allow mathjax to re-render
  if (window.MathJax)
    window.MathJax.Hub.Rerender(event.currentSlide);  
});

// forward command key events to the rstudio frame
if (window.parent.learningKeydown)
  window.onkeydown = function(e) {window.parent.learningKeydown(e);}
