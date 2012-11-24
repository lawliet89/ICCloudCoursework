<!-- BEGIN: Body-->
<div class="sixteen columns">
    <h1>Welcome {userId}!</h1>
    <a href="/auth?logout">[Logout]</a>
    <form action="/upload" method="POST" enctype="multipart/form-data">
        <div class="ten columns">
            <h2>Upload File</h2>
            <div class="six columns alpha">
                <input type="file" name="uploadFile" id="uploadFile" />
            </div>
            <div class="four columns omega">
                <button type="submit">Upload</button>
            </div>
        </div>
    </form>
 </div>
 <!-- END: Body-->
