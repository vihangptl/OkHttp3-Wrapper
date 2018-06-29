MIT License

Copyright (c) [2018] [Vihang Patel]

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.text.Html;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OkHttpClientWrapper extends OkHttpClient {

    /**
     * Auth key, this will change according to server side handling change
     */
    private static final String SECURITY_TOKEN = "HEARDER KEY FOR ACCESS TOKEN";
    /**
     * Base url of the server/ website url.
     */
    private static final String BASE_URL = "YOUR API SERVER BASE URL";
    /**
     * Request type
     */
    private static final MediaType REQUEST_CONTENT_TYPE = MediaType.parse("application/json; charset=utf-8");
    /**
     * Time out values in seconds
     */
    private static final long TIME_OUT = 10;

    private Activity mActivity;

    public OkHttpClientWrapper(Activity activity) {
        this.mActivity = activity;
    }

    /**
     * Default read timeout (in milliseconds).
     */
    @Override
    public int readTimeoutMillis() {
        return (int) TimeUnit.SECONDS.toMillis(TIME_OUT);
    }

    @Override
    public int writeTimeoutMillis() {
        return (int) TimeUnit.SECONDS.toMillis(TIME_OUT);
    }

    @Override
    public int connectTimeoutMillis() {
        return (int) TimeUnit.SECONDS.toMillis(TIME_OUT);
    }

    /**
     * @param url           - server url not include base url
     * @param jsonObject    - request parameters in {@link JSONObject} format.
     *                      - Whenever developer request for {@link RequestMethod#GET} set jsonObject = null
     * @param requestMethod - This can be {@link RequestMethod#POST}, {@link RequestMethod#GET}
     * @param callback      - To handle success response and UI components on respective activity or fragment
     */
    public void newCall(String url, RequestMethod requestMethod, @Nullable JSONObject jsonObject, final ServiceCallbackWrapper callback) {

        RequestBody requestBody = null;
        if (jsonObject != null) {
            requestBody = RequestBody.create(REQUEST_CONTENT_TYPE, jsonObject.toString());
        }

        Request request = new ServiceRequestWrapper()
                .url(url)
                .setData(requestMethod, requestBody)
                .addAuthToken()
                .build();
        if (DetectConnectionUtils.checkInternetConnection(mActivity)) {
            super.newCall(request).enqueue(new ServiceResponseWrapper(mActivity, callback) {
            });
        } else {
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    callback.onSuccessResponse(null, new Exception("Please check your internet, Try again later."));
                    Toast.makeText(mActivity, "Please check your internet, Try again later.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    /**
     * Interface to handle callback from OkHttp {@link okhttp3.Callback}
     */
    public interface ServiceCallbackWrapper {

        /**
         * When receive response from server either get responseString or exception.
         *
         * @param responseString - received  response from server,
         *                       developer has to be convert into {@link JSONObject} object before using it.
         */
        void onSuccessResponse(@Nullable String responseString, @Nullable Exception e);
    }

    /**
     * Request Wrapper
     */
    public class ServiceRequestWrapper extends Request.Builder {


        @Override
        public ServiceRequestWrapper url(String url) {
            url = BASE_URL + url;
            super.url(url);
            return this;
        }

        /**
         * @param requestMethod {@link RequestMethod}
         * @param body          {@link RequestBody}
         */
        ServiceRequestWrapper setData(RequestMethod requestMethod, @Nullable RequestBody body) {
            switch (requestMethod) {
                case POST:
                    assert body != null;
                    post(body);
                    break;
                case GET:
                    get();
                    break;
            }
            return this;
        }

        /**
         * This will check if user has auth token store in {@link android.content.SharedPreferences} and accordingly set the auth token
         */
        ServiceRequestWrapper addAuthToken() {
            LogClass.e("Shared Pref", " " + SharedPrefHelper.getPrefsHelper().getPref(SharedPrefHelper.SECURITY_TOKEN, ""));
            if (!SharedPrefHelper.getPrefsHelper().getPref(SharedPrefHelper.SECURITY_TOKEN, "").equals("")) {
                header(SECURITY_TOKEN, SharedPrefHelper.getPrefsHelper().getPref(SharedPrefHelper.SECURITY_TOKEN, ""));
            }
            return this;
        }
    }


    /**
     * Service Response Wrapper Class which extends {@link Callback} to handle response on UI thread like Activity or fragment.
     */
    class ServiceResponseWrapper implements Callback {

        private static final String ERROR = "error";
        private Activity mActivity;
        private ServiceCallbackWrapper serviceCallbackWrapper;

        ServiceResponseWrapper(Activity activity, ServiceCallbackWrapper callback) {
            this.mActivity = activity;
            this.serviceCallbackWrapper = callback;
        }

        @Override
        public void onFailure(@NonNull Call call, @NonNull final IOException e) {
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    LogClass.e("onFailure", "Exception", e);
                    Toast.makeText(mActivity, "Something went wrong. Please try again later.", Toast.LENGTH_SHORT).show();
                    serviceCallbackWrapper.onSuccessResponse(null, e);
                }
            });
        }

        @Override
        public void onResponse(@NonNull Call call, @NonNull final Response response) throws IOException {
            if (response.body() != null) {
                final String responseString = response.body().string();
                LogClass.e("response", " " + responseString);
                assert mActivity != null;
                assert response.body() != null;
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (response.code() == HttpURLConnection.HTTP_OK) {

                                serviceCallbackWrapper.onSuccessResponse(responseString, null);

                            } else if (response.code() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                                serviceCallbackWrapper.onSuccessResponse(null, new Exception("Unauthorized"));
                                //TODO toast
                                redirectLoginScreen();
                            } else if (response.code() == HttpURLConnection.HTTP_FORBIDDEN) {
                                serviceCallbackWrapper.onSuccessResponse(null, new Exception("Forbidden"));

                                JSONObject errorJsonResponse = new JSONObject(responseString);
                                if (errorJsonResponse.has(ERROR)) {
                                    String errorMessage = errorJsonResponse.getString(ERROR);
                                    showErrorDialog(errorMessage, response.message());
                                } else {
                                    Toast.makeText(mActivity, "Something went wrong. Please try again later.", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                serviceCallbackWrapper.onSuccessResponse(null, new Exception(response.message()));
                                showErrorDialog(null, response.message());
                            }
                        } catch (JSONException e) {
                            LogClass.e("onFailure", "JSONException", e);
                            Toast.makeText(mActivity, "Something went wrong. Please try again later.", Toast.LENGTH_SHORT).show();
                            serviceCallbackWrapper.onSuccessResponse(null, e);
                        }
                    }
                });
            }
        }

        /**
         * When receive Http Status code 403, 502, 504 show user there is Something Issue.
         * Please try again later or authorize to access APIs.
         *
         * @param message - get error message from server and display message using dialog.
         */
        private void showErrorDialog(@Nullable String message, String title) {
            if (message == null) {
                message = "Something went wrong. Please try again later.";
            }
            MaterialDialog errorDialog = new MaterialDialog.Builder(mActivity)
                    .title(title)
                    .content(Html.fromHtml(message))
                    .titleColor(ContextCompat.getColor(mActivity, android.R.color.holo_red_light))
                    .contentColor(ContextCompat.getColor(mActivity, android.R.color.black))
                    .positiveText(mActivity.getString("OK"))
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            dialog.dismiss();
                        }
                    })
                    .canceledOnTouchOutside(false)
                    .cancelable(false)
                    .build();

            errorDialog.show();
        }

        /**
         * When get the Unauthorized Http code from server or other code except 200, 403, 502, 504
         * We will do silent logout and redirect user to {@link LoginActivity}
				 * For Developer, You can change this logic according your need.
         */
        private void redirectLoginScreen() {
            SharedPrefHelper.getPrefsHelper().delete(SharedPrefHelper.SECURITY_TOKEN);
            Intent logOutIntent = new Intent(mActivity, LoginActivity.class);
            logOutIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            mActivity.startActivity(logOutIntent);
            mActivity.finish();
        }
    }


    /**
     * Enums for request type like post, get
     */
    public enum RequestMethod {
        POST, GET
    }
}
