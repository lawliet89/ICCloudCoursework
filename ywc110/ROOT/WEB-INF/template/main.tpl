<!-- BEGIN: Body-->

<!-- http://www.fyneworks.com/jquery/multiple-file-upload/ -->
<script src="/javascripts/jquery.MultiFile.pack.js" type="text/javascript" language="javascript"></script> 
<!-- http://www.malsup.com/jquery/form/ -->
<script src="/javascripts/jquery.form.js" type="text/javascript" language="javascript"></script>

<script type="text/javascript">
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
                $('input:file').MultiFile('reset')
            },
            
            beforeSubmit: function(){
                $.fn.MultiFile.disableEmpty();
                $(".uploadDiv").slideToggle("fast");
                $("#uploadProgress").slideToggle("fast");
            }
        }); 
    }); 
</script>

<div class="sixteen columns">
    <h1>Welcome {userId}!</h1>
    <a href="/auth?logout">[Logout]</a>
    
    <!-- Upload -->
    <form action="/upload" method="POST" enctype="multipart/form-data" id="uploadForm">
        <div class="ten columns">
            <h2>Upload Files</h2>
            <div class="six columns alpha uploadDiv">
                <input type="file" name="uploadFile" id="uploadFile" class="multi" />
            </div>
            <div class="four columns omega uploadDiv">
                <button type="submit">Upload</button>
            </div>
        </div>
        <div class="ten columns" id="uploadProgress" style="text-align: center;">
            <img src="/images/progress.gif" alt="" /> Uploading files...
        </div>
    </form>
    <!-- End Upload -->
    
 </div>
 <!-- END: Body-->
