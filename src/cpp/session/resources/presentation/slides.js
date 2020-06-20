

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
          window.parent.dispatchPresentationCommand(nextCommand.command);
          nextAtCommandIndex++;
       }
    }
  }, false);
  
  return {
    play: function() { if (media.play) media.play(); }
  };
}

function pausePlayers(players) {   
  if (players != null) {
     for(var i = 0; i < players.length; i++) {
       if (!players[i].paused && players[i].pause) 
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
  if (window.parent.presentationSlideChanged) {
    window.parent.presentationSlideChanged(indexh, commandsForSlide(indexh));
  }
}

function revealDetectWidth(zoomed) {
  if (window.innerWidth > 0)
  {
    if (zoomed)
      return window.innerWidth;
    else
      return window.innerWidth * 2.3;
  }
  else
  {
    return 960;
  }
}

function revealDetectHeight(zoomed) {
  if (window.innerHeight > 0)
  {
    if (zoomed)
      return window.innerHeight;
    else
      return window.innerHeight * 2.3;
  }
  else
  {
    return 700;
  }
}
