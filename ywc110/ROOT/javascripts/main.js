// PlayList Manager Object
var PlaylistManager = {
    playlist: null,
    currentPlaylist: 0,
    listOfPlaylistCache: null,
    
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
            requestURL = "/json?list&playlistId=" + playlistId;
        
        // Clear playlist
        PlaylistManager.playlist.setPlaylist([]);
        
        $.getJSON(requestURL, function(data) {
            $("#playlistLoading").fadeOut("fast");
            // Destroy all draggable
            //PlaylistManager.destroyDraggable();
            
            PlaylistManager.playlist.setPlaylist(data.items);
            // Change highlighted playlist
            $("#playlist-li-" + PlaylistManager.currentPlaylist).removeClass("highlighted");
            PlaylistManager.currentPlaylist = data.playlistId;
            $("#playlist-li-" + PlaylistManager.currentPlaylist).addClass("highlighted");
            if (data.playlistId == 0){
                $("#playlistNameDisplay").html("All Items");
            }
            else{
                $("#playlistNameDisplay").html(data.playlistName);
            }
            //PlaylistManager.makeDraggable();
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
        else{
            // Remove Item
            $("#playlistLoading").fadeIn("fast");
            $.getJSON("/json?remove&itemId=" + itemId + "&playlistId="+ playlistId +"&nonce=" + NonceManager.nonce, function(data){
                NonceManager.getNewNonce();
                $("#playlistLoading").fadeOut("fast");
                if (data.success){
                    $().toastmessage('showSuccessToast', "Item removed from playlist.");
                    PlaylistManager.playlist.remove(playlistIndex);
                }
                else{
                    $().toastmessage('showErrorToast', "Item could not be removed from playlist.");
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
            
            // Cache
            PlaylistManager.listOfPlaylistCache = data.playlists;
            
            try{
                $.each(data.playlists, function(index, value){
                    list += '<li class="playlistDroppable" id="playlist-li-' + value.playlistId + '" data-playlistId="'+value.playlistId+'"><span id="playlist-' 
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
            PlaylistManager.makeDroppable();
        });
    },
    
    makePlaylistsEditable: function(element){
        if (element == undefined)
            element = "#playlistUl li span.playlistEditable";
        // Initialise rename playlist editable
        $(element).editable("/json", {
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
        if (!confirm("Are you sure you want to delete the playlist? \nThis is irreversible.")) return;
        $("#playlistLoading").fadeIn("fast");
        $.getJSON("/json?deletePlaylist&playlistId="+ playlistId + "&nonce=" + NonceManager.nonce, function(data){
            $("#playlistLoading").fadeIn("fast");

            if (data.success == true){
                NonceManager.getNewNonce();
                $().toastmessage('showSuccessToast', "Playlist deleted.");
                $("#playlist-li-" + data.playlistId).fadeOut(400, function(){
                    $("#playlist-li-" + data.playlistId).remove();
                });                
                
                if (PlaylistManager.currentPlaylist == data.playlistId)
                    PlaylistManager.loadPlaylist(0);
            }
            else{
                $().toastmessage('showErrorToast', "Failed to delete playlist.");
            }
        });
        
    },
    addPlaylist: function(){
        $("#playlistsLoading").fadeIn('fast');
        $.getJSON("/json?newPlaylist&nonce=" + NonceManager.nonce, function(data){
            $("#playlistsLoading").fadeOut('fast');
            NonceManager.getNewNonce();
            if (data.success == true){
                $().toastmessage('showSuccessToast', "Playlist created.");
                var list = '<li class="playlistDroppable" id="playlist-li-' + data.playlistId + '" data-playlistId="'+data.playlistId+'"><span id="playlist-' 
                         + data.playlistId + '" class="playlistEditable" data-playlistId="' + data.playlistId + '" '
                         + 'onclick="PlaylistManager.loadPlaylist(' + data.playlistId + ');"'
                         + '>' + data.playlistName + '</span>'
                         + '<img src="/images/delete.png" onclick="PlaylistManager.deletePlaylist(' + data.playlistId + ');" class="delete-button" title="Delete playlist" />'
                         + '</li>';
                         
                $("#playlistUl").append(list);
                PlaylistManager.makePlaylistsEditable('#playlist-' + data.playlistId);
                
                $("#playlist-"+data.playlistId).trigger("dblclick");
                PlaylistManager.makeDroppable()
            }
            else{
                $().toastmessage('showErrorToast', "Failed to create a new playlist.");
            }
        });
    },
    
    makeDraggable: function(){
        $(".draggableItems").draggable({
            revert: true,
            //appendTo: 'body',
            helper: 'clone',
            scope: 'playlistItems'
        });
    },
    destroyDraggable: function(){
        $(".draggableItems").draggable("destroy");
    },
    makeDroppable: function(){
        $(".playlistDroppable").droppable({
            scope: 'playlistItems',
            hoverClass: 'hoverClass',
            drop: function(event, ui){
                var playlistId = $(this).data("playlistid");
                var itemId = ui.draggable.data("itemid");
                $("#playlistsLoading").fadeOut('fast');
                
                $.getJSON("/json?add&playlistId=" + playlistId + "&itemId=" + itemId + "&nonce=" + NonceManager.nonce, function(data){
                    $("#playlistsLoading").fadeOut('fast');
                    NonceManager.getNewNonce();
                    
                    if(data.success)
                        $().toastmessage('showSuccessToast', "Item added to playlist.");
                    else
                        $().toastmessage('showErrorToast', "Item could not be added to playlist.");
                });
            }
        });
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


