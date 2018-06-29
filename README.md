### **Welcome!**

This class is used to simplify the API call. Like we have to add header parameters like access token, API URL, OkHttp3 Callbacks with fewer efforts.
This class just give you single callback with two parameters.
<pre><code>new OkHttpClientWrapper(this)
                .newCall(/*URL*/,
                        /*Request Method, Either POST OR GET*/,
                        /*Json Object with desired parameters*/,
                        new OkHttpClientWrapper.ServiceCallbackWrapper() {
                            @Override
                            public void onSuccessResponse(@Nullable String responseString, @Nullable Exception e) {
                                // Handle your UI componets like hide/dismiss your progressbar
                                if (responseString != null) {
                                   // Got the success response from the server
                                } else {
                                  // Got the any kind of exception. either from server side or cleint side
                                }
                            }
                        });</code></pre>
    

For this class, You should add below dependencies in your App levelGradle File.
<pre><code>dependencies {
	// ... other dependencies here
    implementation 'com.afollestad.material-dialogs:core:0.9.6.0'
    implementation 'com.squareup.okhttp3:okhttp:3.10.0'
}</code></pre>
