
// forward command key events to the rstudio frame
if (window.parent.learningKeydown)
  window.onkeydown = function(e) {window.parent.learningKeydown(e);}

// manage media playback, atCommands, etc.
function mediaManager(media, atCommands) {

  // track state for at command execution
  nextAtCommandIndex = 0;
  previousTime = 0;
  
  // reset at command index and reload after ending
  media.addEventListener('ended', function(e) { 
    nextAtCommandIndex = 0;
    media.currentTime = 0;
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
  if (players != null) {
     for(var i = 0; i < players.length; i++) {
       if (!players[i].paused) 
         players[i].pause();
     }
  }
}

function pauseAllPlayers() {
   pausePlayers(document.getElementsByTagName('video'));
   pausePlayers(document.getElementsByTagName('audio'));
}
    
    
function notifySlideChanged(indexh) {

  // pause all audio and video
  pauseAllPlayers();
 
  // notify parent of slide changed
  if (window.parent.learningSlideChanged) {
    window.parent.learningSlideChanged(event.indexh, 
                                       commandsForSlide(event.indexh));  
  }
}

function revealEnterFullscreen() {
   
   var element = document.body;

   // Check which implementation is available
   var requestMethod = element.requestFullScreen ||
				           element.webkitRequestFullScreen ||
				           element.mozRequestFullScreen ||
				           element.msRequestFullScreen;

   if (requestMethod) {
      requestMethod.apply( element );
   }
}

