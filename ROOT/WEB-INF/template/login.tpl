<!-- BEGIN: Body-->
<div class="ten columns">
    <h1>Login</h1>
    Please login with your college user name and password.
    <p style="color:red;">{message}</p>
   <form action="/auth" method="POST">
    <div class="row">
        <div class="four columns alpha">User ID</div>
        <div class="six columns omega"><input type="text" name="userId" id="userId" /> </div>
    </div>
    
    <div class="row">
        <div class="four columns alpha">Password</div>
        <div class="six columns omega"><input type="password" name="password" id="password" /> </div>
    </div>
    
    <button type="submit">Login</button>
 </form>
 </div>
 <!-- END: Body-->