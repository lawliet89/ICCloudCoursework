<!-- BEGIN: Body-->
<link href="stylesheets/blue.monday/jplayer.blue.monday.css" rel="stylesheet" type="text/css" />
<!-- <link href="stylesheets/ui-lightness/jquery-ui-1.9.2.custom.min.css" rel="stylesheet" type="text/css" /> -->
<!-- http://www.fyneworks.com/jquery/multiple-file-upload/ -->
<script src="/javascripts/jquery.MultiFile.pack.js" type="text/javascript" language="javascript"></script> 
<!-- http://www.malsup.com/jquery/form/ -->
<script src="/javascripts/jquery.form.js" type="text/javascript" language="javascript"></script>
<script type="text/javascript" src="javascripts/jquery.jplayer.min.js"></script>
<script type="text/javascript" src="javascripts/jplayer.playlist.js"></script>
<script type="text/javascript" src="javascripts/jquery.jeditable.mini.js"></script>
<script type="text/javascript" src="javascripts/jquery-ui-1.9.2.custom.min.js"></script>
<script type="text/javascript" src="javascripts/main.js"></script>

<style type="text/css">
    #playlistUl {
        width: 100%;
        background-color: #ccc;
        border: 1px solid #009be3;
        max-height:400px;
        overflow: auto;
    }
    
    #playlistUl li{
        border-bottom: 1px solid white;
        margin: 0;
        padding: 5px;
    }
    #playlistUl li:hover{
        background-color: #fff;
    }
    #playlistUl .highlighted{
       font-weight:bold;
       background-color: #eee;
    }
    #playlistUl li span.playlistEditable{
        display: inline-block;
        width: 80%;
        max-width:80%;
        overflow:hidden;
    }
    
    #playlistUl li span.playlistEditable:hover, 
    #playlist-li-0:hover,
    .delete-button:hover {
        cursor: hand; 
        cursor: pointer;
    }
    
    .draggableItems.ui-draggable-dragging{
        background-color: #fff;
        -moz-opacity: 0.85;
        opacity: 0.85;
        -ms-filter:"progid:DXImageTransform.Microsoft.Alpha"(Opacity=85);
        min-width:200px;
        max-width: 360px;
        height: 3.5em;
        overflow:hidden;
    }
    .draggableItems.ui-draggable-dragging .jp-free-media,
    .draggableItems.ui-draggable-dragging .jp-playlist-item-remove {
        visibility:hidden;
        list-style-image: none;
        list-style-position: outside;
        list-style-type: none;
        padding-left:20px;
    }
    
    .hoverClass{
        background-color: #33B5E5;
    }
</style>

<div class="sixteen columns">
    <div class="sixteen columns">
        <h1 style="display:inline; margin-right: 10px;">Welcome {userId}!</h1>
        <a href="/auth?logout" title="Logout"><img src="/images/logout.png" /></a>
        <hr />
    </div>
    <!-- Player -->
    <div class="nine columns alpha">
        <div class="nine columns">
            <h2 id="playlistNameDisplay" style="display:inline; margin-right: 10px;">Now Playing</h2> 
            <a href="" onclick="PlaylistManager.reloadPlaylist(); return false;" title="Reload playlist"><img src="/images/refresh.png" style="display:inline; margin-right: 10px;" /></a>
            <img class="hidden" id="playlistLoading" src="/images/progress.gif" alt="" /> 
        </div>
        <div class="nine columns">
            <div id="jquery_jplayer_2" class="jp-jplayer"></div>
            <div id="jp_container_2" class="jp-audio">
                <div class="jp-type-playlist">
                    <div class="jp-gui jp-interface">
                        <ul class="jp-controls">
                            <li><a href="javascript:;" class="jp-previous" tabindex="1">previous</a></li>
                            <li><a href="javascript:;" class="jp-play" tabindex="1">play</a></li>
                            <li><a href="javascript:;" class="jp-pause" tabindex="1">pause</a></li>
                            <li><a href="javascript:;" class="jp-next" tabindex="1">next</a></li>
                            <li><a href="javascript:;" class="jp-stop" tabindex="1">stop</a></li>
                            <li><a href="javascript:;" class="jp-mute" tabindex="1" title="mute">mute</a></li>
                            <li><a href="javascript:;" class="jp-unmute" tabindex="1" title="unmute">unmute</a></li>
                            <li><a href="javascript:;" class="jp-volume-max" tabindex="1" title="max volume">max volume</a></li>
                        </ul>
                        <div class="jp-progress">
                            <div class="jp-seek-bar">
                                <div class="jp-play-bar"></div>
                            </div>
                        </div>
                        <div class="jp-volume-bar">
                            <div class="jp-volume-bar-value"></div>
                        </div>
                        <div class="jp-time-holder">
                            <div class="jp-current-time"></div>
                            <div class="jp-duration"></div>
                        </div>
                        <ul class="jp-toggles">
                            <li><a href="javascript:;" class="jp-shuffle" tabindex="1" title="shuffle">shuffle</a></li>
                            <li><a href="javascript:;" class="jp-shuffle-off" tabindex="1" title="shuffle off">shuffle off</a></li>
                            <li><a href="javascript:;" class="jp-repeat" tabindex="1" title="repeat">repeat</a></li>
                            <li><a href="javascript:;" class="jp-repeat-off" tabindex="1" title="repeat off">repeat off</a></li>
                        </ul>
                    </div>
                    <div class="jp-playlist" style="max-height:600px; overflow:auto;">
                        <ul>
                            <li></li>
                        </ul>
                    </div>
                    <div class="jp-no-solution">
                        <span>Update Required</span>
                        To play the media you will need to either update your browser to a recent version or update your <a href="http://get.adobe.com/flashplayer/" target="_blank">Flash plugin</a>.
                    </div>
                </div>
            </div>
        </div>
    </div>
    
    
    <div class="five columns omega">
        <!-- Playlists -->
        <div class="five columns">
            <div><h2 style="display:inline; margin-right: 10px;">Playlists</h2> 
            <a href="" onclick="PlaylistManager.addPlaylist(); return false;" title="Add a new playlist"><img src="/images/add.png" style="display:inline; margin-right: 10px;" /></a>
            <a href="" onclick="PlaylistManager.loadListOfPlaylists(); return false;" title="Reload playlists"><img src="/images/refresh.png" style="display:inline; margin-right: 10px;" /></a>
            <img class="hidden" id="playlistsLoading" src="/images/progress.gif" alt="" /> </div>
            
            <ul id="playlistUl">
                <li id="playlist-li-0" class="highlighted" data-playlistId="0">All Items</li>
                <li><span class="playlistEditable" id="playlist-1"  data-playlistId="1">Playlist</span></li>
            </ul>
            
        </div>
        <!-- Upload -->
        <form action="/upload" method="POST" enctype="multipart/form-data" id="uploadForm">
            <div class="five columns">
                <h2>Upload Files</h2>
                <div class="uploadDiv">
                    <input type="file" name="uploadFile" id="uploadFile" class="multi" />
                    <br /><button type="submit">Upload</button>
                </div>
            </div>
            <div class="five columns" id="uploadProgress" style="text-align: center;">
                <img src="/images/progress.gif" alt="" /> Uploading files...
            </div>
        </form>
        <!-- End Upload -->
        
        <div class="five columns">
            <p style="color: #555; font-size: 85%">Tip: Drag and drop songs from the player to the list of playlists to add songs to playlists.</p>
        </div>
    </div>
    

    
 </div>
 <!-- END: Body-->