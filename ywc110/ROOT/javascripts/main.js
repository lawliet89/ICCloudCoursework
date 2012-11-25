// PlayList Manager Object
var PlaylistManager = {
    playlist: null,
    currentPlaylist: 0,
    
    initialisePlaylist: function(){
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
            PlaylistManager.currentPlaylist = data.playlistId;
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
    }
}
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
}); 


