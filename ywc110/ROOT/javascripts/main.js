// PlayList Manager Object
var PlaylistManager = {
    playlist: null,
    currentPlaylist: 0,
    
    initialisePlaylist: function(){
        // Initialise player
        $("#playlistLoading").fadeIn("fast");
        
        this.playlist = new jPlayerPlaylist({
            jPlayer: "#jquery_jplayer_2",
            cssSelectorAncestor: "#jp_container_2"
        }, [] , {
            playlistOptions: {
                enableRemoveControls: true
            },
            swfPath: "javascripts",
            supplied: "mp3, oga, m4a, wav",
            wmode: "window"
        });
        this.loadPlaylist(0);
        
        // Load Playlists
        this.loadListOfPlaylists();
    },
    
    loadPlaylist: function(playlistId){
        $("#playlistLoading").fadeIn("fast");
        var requestURL;
        if (playlistId == 0 || playlistId == undefined)
            // Default
            requestURL = "/json?list";
        else
            requestURL = "/json?list&id=" + playlistId;
        
        // Clear playlist
        PlaylistManager.playlist.setPlaylist([]);
        
        $.getJSON(requestURL, function(data) {
            $("#playlistLoading").fadeOut("fast");
            PlaylistManager.playlist.setPlaylist(data.items);
            // Change highlighted playlist
            $("#playlist-li-" + PlaylistManager.currentPlaylist).removeClass("highlighted");
            PlaylistManager.currentPlaylist = data.playlistId;
            $("#playlist-li-" + PlaylistManager.currentPlaylist).addClass("highlighted");
            if (data.playlistId == 0){
                $("#playlistNameDisplay").html("All Items");
            }
            else{
                $("#playlistNameDisplay").html(playlistName);
            }
        });
    },
    
    reloadPlaylist: function(){
        this.loadPlaylist(this.currentPlaylist);
    },
    
    deleteItem: function(itemId, playlistId, playlistIndex){        
        if (playlistId == undefined || playlistId == 0){
            if (!confirm("Are you sure you want to delete the file? \n\nThis is irreversible."))
                return;
            // Delete Item
            $("#playlistLoading").fadeIn("fast");
            $.getJSON("/json?remove&itemId=" + itemId + "&nonce=" + NonceManager.nonce, function(data){
                NonceManager.getNewNonce();
                $("#playlistLoading").fadeOut("fast");
                if (data.success){
                    $().toastmessage('showSuccessToast', "File deleted");
                    PlaylistManager.playlist.remove(playlistIndex);
                }
                else{
                    $().toastmessage('showErrorToast', "File could not be deleted.");
                }
            });

        }
    },
    
    loadListOfPlaylists: function(){
        $("#playlistsLoading").fadeIn('fast');
        $.getJSON("/json?playlists", function(data){
            $("#playlistsLoading").fadeOut('fast');
            
            $("#playlistUl").empty();
            
            // All playlists
            var list="<li id='playlist-li-0' data-playlistId='0' onclick='PlaylistManager.loadPlaylist(0);'>All Items</li>";
            
            try{
                $.each(data.playlists, function(index, value){
                    list += '<li id="playlist-li-"' + value.playlistId + '"><span id="playlist-' 
                         + value.playlistId + '" class="playlistEditable" data-playlistId="' + value.playlistId + '" '
                         + 'onclick="PlaylistManager.loadPlaylist(' + value.playlistId + ');"'
                         + '>' + value.playlistName + '</span>'
                         + '<img src="/images/delete.png" onclick="PlaylistManager.deletePlaylist(' + value.playlistId + ');" class="delete-button" title="Delete playlist" />'
                         + '</li>';
                });
            }
            catch (e) {}
            $("#playlistUl").html(list);
            $("#playlist-li-" + PlaylistManager.currentPlaylist).addClass("highlighted");
            PlaylistManager.makePlaylistsEditable();
        });
    },
    
    makePlaylistsEditable: function(){
        // Initialise rename playlist editable
        $("#playlistUl li span.playlistEditable").editable("/json", {
            indicator: '<img src="/images/progress.gif">',
            tooltip: 'Double click to rename.',
            style: 'width: 75%; display:inline-block; padding:0; margin:0;',
            name: 'playlistName',
            event: "dblclick",
            id: "playlistId",
            submitdata: function(value, settings) {
                return {renamePlaylist: '',
                        nonce: NonceManager.nonce
                };
            },
            ajaxoptions: {
                dataType: 'json',
                type : 'GET',
                contentType: 'application/x-www-form-urlencoded; charset=UTF-8'
            },
            callback: function(result, setting){
                NonceManager.getNewNonce();
                // Update
                if (result.success){
                    $().toastmessage('showSuccessToast', "Playlist renamed.");
                    $("#playlist-" + result.playlistId).html(result.playlistName);
                }
                else if (!result.success){
                    $().toastmessage('showErrorToast', "Failed to rename playlist.");
                    $("#" + result.playlistId).html(result.playlistName);
                }
            }
        });
    },
    
    deletePlaylist: function(playlistId){
        if (playlistId == undefined || playlistId < 1)
            return;
            
        
    }
}

var NonceManager = {
    nonce: null,
    getNewNonce: function(){
        $.getJSON("/json?nonce", function(data){
            NonceManager.nonce = data.nonce;
        });
    }
};

// wait for the DOM to be loaded 
$(document).ready(function() { 
    $("#uploadProgress").hide();

    // bind upload form
    $('#uploadForm').ajaxForm({
        dataType: "json",
        resetForm: true,
        success: function(result){
            $.fn.MultiFile.reEnableEmpty();
            var successCount = 0;
            var failureCount = 0;
            try{
                $.each(result.files, function(index, value){
                    if (value.success == true)
                        successCount++;
                    else if (value.success == false)
                        failureCount++;
                });
                if (successCount > 0){
                    $().toastmessage('showSuccessToast', successCount + " files uploaded.");
                }
                if (failureCount > 0){
                    $().toastmessage('showErrorToast', failureCount + " files failed to upload.");
                }
            }
            catch (e) { }
            $(".uploadDiv").slideToggle("fast");
            $("#uploadProgress").slideToggle("fast");
            $('input:file').MultiFile('reset');
            
            PlaylistManager.reloadPlaylist();
        },
        
        beforeSubmit: function(){
            $.fn.MultiFile.disableEmpty();
            $(".uploadDiv").slideToggle("fast");
            $("#uploadProgress").slideToggle("fast");
        }
    }); 
    
    // Load all items
    PlaylistManager.initialisePlaylist();
    
    // Initialise a nonce
    NonceManager.getNewNonce();
    

}); 


