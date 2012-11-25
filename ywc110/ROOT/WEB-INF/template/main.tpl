<!-- BEGIN: Body-->
<link href="stylesheets/blue.monday/jplayer.blue.monday.css" rel="stylesheet" type="text/css" />
<!-- http://www.fyneworks.com/jquery/multiple-file-upload/ -->
<script src="/javascripts/jquery.MultiFile.pack.js" type="text/javascript" language="javascript"></script> 
<!-- http://www.malsup.com/jquery/form/ -->
<script src="/javascripts/jquery.form.js" type="text/javascript" language="javascript"></script>

<script type="text/javascript" src="javascripts/jquery.jplayer.min.js"></script>
<script type="text/javascript" src="javascripts/jplayer.playlist.min.js"></script>

<script type="text/javascript" src="javascripts/main.js"></script>


<div class="sixteen columns">
    <div class="sixteen columns">
        <h1>Welcome {userId}!</h1>
        <a href="/auth?logout">[Logout]</a>
    </div>
    <!-- Player -->
    <div class="nine columns alpha">
        <h2 id="playlistNameDisplay">Now Playing <img class="hidden" id="playlistLoading" src="/images/progress.gif" alt="" /></h2>
        <a href="" onclick="PlaylistManager.reloadPlaylist(); return false;">[Reload Playlist]</a>
    </div>
    <div class="nine columns alpha">
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
				<div class="jp-playlist">
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
    
    <!-- Upload -->
    <form action="/upload" method="POST" enctype="multipart/form-data" id="uploadForm">
        <div class="five columns omega">
            <h2>Upload Files</h2>
            <input type="file" name="uploadFile" id="uploadFile" class="multi" />
            <br /><button type="submit">Upload</button>
            </div>
        </div>
        <div class="five columns omega" id="uploadProgress" style="text-align: center;">
            <img src="/images/progress.gif" alt="" /> Uploading files...
        </div>
    </form>
    <!-- End Upload -->
    
 </div>
 <!-- END: Body-->
