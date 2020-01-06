package com.github.alinz.reactnativewebviewbridge;

import android.graphics.Bitmap;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.CookieManager;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.UIManagerModule;
import com.facebook.react.uimanager.events.Event;
import com.facebook.react.uimanager.events.EventDispatcher;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.reactnativecommunity.webview.events.TopLoadingErrorEvent;
import com.reactnativecommunity.webview.events.TopLoadingFinishEvent;
import com.reactnativecommunity.webview.events.TopLoadingStartEvent;

import android.webkit.SslErrorHandler;
import android.net.http.SslError;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nullable;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static okhttp3.internal.Util.UTF_8;

public class WebViewBridgeManager extends SimpleViewManager<WebViewBridgeManager.ReactWebView> {
    private static final String REACT_CLASS = "RCTWebViewBridge";

    public static final int COMMAND_SEND_TO_BRIDGE = 101;
    public static final int COMMAND_EVALUTEJS = 102;
    public static final int COMMAND_BACK = 103;
    public static final int COMMAND_FORWARD = 104;
    public static final int COMMAND_RELOAD = 105;
    public static final int COMMAND_LOAD_SOURCE = 106;

    public final static String HEADER_CONTENT_TYPE = "content-type";

    private static final String MIME_TEXT_HTML = "text/html";
    private static final String MIME_UNKNOWN = "application/octet-stream";

    protected static final String HTML_ENCODING = "UTF-8";
    protected static final String HTML_MIME_TYPE = "text/html";
    protected static final String HTTP_METHOD_POST = "POST";
    protected static final String BLANK_URL = "about:blank";


    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @Override
    public
    @Nullable
    Map<String, Integer> getCommandsMap() {
        Map<String, Integer> commandsMap = new HashMap<>();

        commandsMap.put("sendToBridge", COMMAND_SEND_TO_BRIDGE);
        commandsMap.put("evaluateJS", COMMAND_EVALUTEJS);
        commandsMap.put("goBack", COMMAND_BACK);
        commandsMap.put("goForward", COMMAND_FORWARD);
        commandsMap.put("reload", COMMAND_RELOAD);
        commandsMap.put("loadSource", COMMAND_LOAD_SOURCE);


        return commandsMap;
    }

    @Override
    protected ReactWebView createViewInstance(ThemedReactContext reactContext) {
        ReactWebView webView;
        webView = new ReactWebView(reactContext);
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        webView.setLayoutParams(params);
        webView.addJavascriptInterface(new JavascriptBridge(webView), "WebViewBridge");
        webView.setWebChromeClient(new ReactWebChromeClient());
        webView.getSettings().setJavaScriptEnabled(true);

        webView.setWebViewClient(new ReactWebViewClient());
        webView.getSettings().setAllowFileAccessFromFileURLs(true);
        webView.getSettings().setAllowUniversalAccessFromFileURLs(true);

        webView.getSettings().setUserAgentString("Mozilla/5.0 (Linux; Android 4.1.1; Galaxy Nexus Build/JRO03C) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.166 Mobile Safari/535.19");
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setAppCacheEnabled (true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setAllowContentAccess(true);
        webView.getSettings().setLoadsImagesAutomatically(true);
        webView.getSettings().setBlockNetworkImage(false);
        webView.getSettings().setBlockNetworkLoads(false);
        webView.setWebContentsDebuggingEnabled(true);
        return webView;
    }

    @Override
    public void receiveCommand(ReactWebView root, int commandId, @Nullable ReadableArray args) {
        super.receiveCommand(root, commandId, args);

        switch (commandId) {
            case COMMAND_SEND_TO_BRIDGE:
                sendToBridge(root, args.getString(0));
                break;
            case COMMAND_EVALUTEJS:
//                root.evaluateJavascript(args.getString(0), null);
                evaluateJS(root, args.getString(0));
                break;
            case COMMAND_BACK:
                root.goBack();
                break;
            case COMMAND_FORWARD:
                root.goForward();
                break;
            case COMMAND_RELOAD:
                root.reload();
                break;
            case COMMAND_LOAD_SOURCE:
                root.loadUrl(args.getString(0));
                break;
            default:
                //do nothing!!!!
        }
    }

    @ReactProp(name = "domStorageEnabled")
    public void setDomStorageEnabled(WebView view, boolean enabled) {
        view.getSettings().setDomStorageEnabled(enabled);
    }

    @ReactProp(name = "thirdPartyCookiesEnabled")
    public void setThirdPartyCookiesEnabled(WebView view, boolean enabled) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(view, enabled);
        }
    }

    private void sendToBridge(WebView view, String script) {
//        String script = "WebViewBridge.onMessage('" + message + "');";
//        String scrip2 = "WebViewBridge.send(\"aa\")";
        WebViewBridgeManager.evaluateJavascript(view, script);
    }

    static private void evaluateJavascript(WebView root, String javascript) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            root.evaluateJavascript(javascript, null);
        } else {
            root.loadUrl("javascript:" + javascript);
        }
    }

    private void evaluateJS(WebView root, String javascript) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            root.evaluateJavascript(javascript, null);
        } else {
            root.loadUrl("javascript:" + javascript);
        }
    }

    @ReactProp(name = "injectedOnStartLoadingJavaScript")
    public void setInjectedOnStartLoadingJavaScript(WebView view, @Nullable String injectedJavaScript) {
        ((ReactWebView) view).setInjectedOnStartLoadingJavaScript(injectedJavaScript);
    }

/*    @ReactProp(name = "source")
    public void loadUrl(WebView view, String url) {
        try {
            view.loadUrl(url);
        }catch (Exception e){
            e.printStackTrace();
        }
    }*/
    @ReactProp(name = "source")
    public void setSource(WebView view, @Nullable ReadableMap source) {
        if (source != null) {
            if (source.hasKey("html")) {
                String html = source.getString("html");
                String baseUrl = source.hasKey("baseUrl") ? source.getString("baseUrl") : "";
                view.loadDataWithBaseURL(baseUrl, html, HTML_MIME_TYPE, HTML_ENCODING, null);
                return;
            }
            if (source.hasKey("uri")) {
                String url = source.getString("uri");
                String previousUrl = view.getUrl();
                if (previousUrl != null && previousUrl.equals(url)) {
                    return;
                }
                if (source.hasKey("method")) {
                    String method = source.getString("method");
                    if (method.equalsIgnoreCase(HTTP_METHOD_POST)) {
                        byte[] postData = null;
                        if (source.hasKey("body")) {
                            String body = source.getString("body");
                            try {
                                postData = body.getBytes("UTF-8");
                            } catch (UnsupportedEncodingException e) {
                                postData = body.getBytes();
                            }
                        }
                        if (postData == null) {
                            postData = new byte[0];
                        }
                        view.postUrl(url, postData);
                        return;
                    }
                }
                HashMap<String, String> headerMap = new HashMap<>();
                if (source.hasKey("headers")) {
                    ReadableMap headers = source.getMap("headers");
                    ReadableMapKeySetIterator iter = headers.keySetIterator();
                    while (iter.hasNextKey()) {
                        String key = iter.nextKey();
                        if ("user-agent".equals(key.toLowerCase(Locale.ENGLISH))) {
                            if (view.getSettings() != null) {
                                view.getSettings().setUserAgentString(headers.getString(key));
                            }
                        } else {
                            headerMap.put(key, headers.getString(key));
                        }
                    }
                }
                view.loadUrl(url, headerMap);
                return;
            }
        }
        view.loadUrl(BLANK_URL);
    }


    @ReactProp(name = "javaScriptEnabled")
    public void javaScriptEnabled(WebView view, Boolean enable) {
        view.getSettings().setJavaScriptEnabled(enable);
    }

    @ReactProp(name = "injectedJavaScript")
    public void injectedJavaScript(WebView view, String js) {
        view.evaluateJavascript(js, null);
        Log.d("inject", js);
    }

    @ReactProp(name = "allowUniversalAccessFromFileURLs")
    public void setAllowUniversalAccessFromFileURLs(WebView view, boolean allow) {
        view.getSettings().setAllowUniversalAccessFromFileURLs(allow);
    }

//
//    @ReactProp(name = "allowFileAccessFromFileURLs")
//    public void setAllowFileAccessFromFileURLs(WebView root, boolean allows) {
//        root.getSettings().setAllowFileAccessFromFileURLs(allows);
//    }
//
//    @ReactProp(name = "allowUniversalAccessFromFileURLs")
//    public void setAllowUniversalAccessFromFileURLs(WebView root, boolean allows) {
//        root.getSettings().setAllowUniversalAccessFromFileURLs(allows);
//    }

    public static class ReactWebView extends WebView implements LifecycleEventListener {
        private @Nullable
        String injectedJS;
        private @Nullable
        String injectedOnStartLoadingJS;
        private boolean messagingEnabled = false;

        private class ReactWebViewBridge {
            ReactWebView mContext;

            ReactWebViewBridge(ReactWebView c) {
                mContext = c;
            }
        }

        /**
         * WebView must be created with an context of the current activity
         * <p>
         * Activity Context is required for creation of dialogs internally by WebView
         * Reactive Native needed for access to ReactNative internal system functionality
         */
        public ReactWebView(ThemedReactContext reactContext) {
            super(reactContext);
        }

        @Override
        public void onHostResume() {
            // do nothing
        }

        @Override
        public void onHostPause() {
            // do nothing
        }

        @Override
        public void onHostDestroy() {

        }

        public void setInjectedJavaScript(@Nullable String js) {
            injectedJS = js;
        }

        public void setInjectedOnStartLoadingJavaScript(@Nullable String js) {
            injectedOnStartLoadingJS = js;
        }

        public void callInjectedJavaScript() {
            if (getSettings().getJavaScriptEnabled() &&
                    injectedJS != null &&
                    !TextUtils.isEmpty(injectedJS)) {
                loadUrl("javascript:(function() {\n" + injectedJS + ";\n})();");
            }
        }
    }

    public static Boolean responseRequiresJSInjection(Response response) {
        // we don't want to inject JS into redirects
        if (response.isRedirect()) {
            return false;
        }

        // ...okhttp appends charset to content type sometimes, like "text/html; charset=UTF8"
        final String contentTypeAndCharset = response.header(HEADER_CONTENT_TYPE, MIME_UNKNOWN);
        // ...and we only want to inject it in to HTML, really
        return contentTypeAndCharset.startsWith(MIME_TEXT_HTML);
    }

    public static Boolean urlStringLooksInvalid(String urlString) {
        return urlString == null ||
                urlString.trim().equals("") ||
                !(urlString.startsWith("http") && !urlString.startsWith("www")) ||
                urlString.contains("|");
    }

    public WebResourceResponse shouldInterceptRequest(WebResourceRequest request, Boolean onlyMainFrame, ReactWebView webView) {
        Uri url = request.getUrl();
        String urlStr = url.toString();
        
        if(urlStr.contains("mixpanel") || urlStr.contains("cdn.segment.com")){
            Log.d("URL", "LOCK MIXPANEL");
            return new WebResourceResponse("text/html", UTF_8.name(), null);
        }

        if (onlyMainFrame && !request.isForMainFrame()) {
            return null;
        }

        if (urlStringLooksInvalid(urlStr)) {
            return null;
        }

        try {
            Request req = new Request.Builder()
                    .url(urlStr)
                    .header("User-Agent", "")
                    .build();

            OkHttpClient.Builder b = new OkHttpClient.Builder();
            OkHttpClient httpClient = b
                    .followRedirects(false)
                    .followSslRedirects(false)
                    .build();

            Response response = httpClient.newCall(req).execute();

            if (!responseRequiresJSInjection(response)) {
                return null;
            }

            InputStream is = response.body().byteStream();
            MediaType contentType = response.body().contentType();
            Charset charset = contentType != null ? contentType.charset(UTF_8) : UTF_8;

            if (response.code() == HttpURLConnection.HTTP_OK) {
                is = new InputStreamWithInjectedJS(is, webView.injectedOnStartLoadingJS, charset);
            }
            return new WebResourceResponse("text/html", charset.name(), is);
        } catch (IOException e) {
            return null;
        }
    }

    private class ReactWebChromeClient extends WebChromeClient {
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);
            dispatchEvent(view, new TopLoadingFinishEvent(view.getId(), this.createWebViewEvent(newProgress)));
        }

        protected WritableMap createWebViewEvent(int progress) {
            WritableMap event = Arguments.createMap();
            event.putInt("progress", progress);
            return event;
        }
    }

    private class ReactWebViewClient extends WebViewClient {

        protected boolean mLastLoadFailed = false;

        @Override
        public void onPageFinished(WebView webView, String url) {
            super.onPageFinished(webView, url);
            if (!this.mLastLoadFailed) {
                this.emitFinishEvent(webView, url);
            }
        }

        @Override
        public void onPageStarted(WebView webView, String url, Bitmap favicon) {
            super.onPageStarted(webView, url, favicon);
            this.mLastLoadFailed = false;
            dispatchEvent(webView, new TopLoadingStartEvent(webView.getId(), this.createWebViewEvent(webView, url)));
        }

        @Override
        public void onReceivedError(WebView webView, int errorCode, String description, String failingUrl) {
            super.onReceivedError(webView, errorCode, description, failingUrl);
            this.mLastLoadFailed = true;
            this.emitFinishEvent(webView, failingUrl);
            WritableMap eventData = this.createWebViewEvent(webView, failingUrl);
            eventData.putDouble("code", (double) errorCode);
            eventData.putString("description", description);
            dispatchEvent(webView, new TopLoadingErrorEvent(webView.getId(), eventData));
        }

        protected void emitFinishEvent(WebView webView, String url) {
//            dispatchEvent(webView, new TopLoadingFinishEvent(webView.getId(), this.createWebViewEvent(webView, url)));
        }

        protected WritableMap createWebViewEvent(WebView webView, String url) {
            WritableMap event = Arguments.createMap();
            event.putDouble("target", (double) webView.getId());
            event.putString("url", url);
            event.putBoolean("loading", !this.mLastLoadFailed && webView.getProgress() != 100);
            event.putString("title", webView.getTitle());
            event.putBoolean("canGoBack", webView.canGoBack());
            event.putBoolean("canGoForward", webView.canGoForward());
            return event;
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            WebResourceResponse response = WebViewBridgeManager.this.shouldInterceptRequest(request, true, (ReactWebView) view);
            if (response != null) {
                Log.d("GOLDEN", "shouldInterceptRequest / WebViewClient -> return intercept response");
                return response;
            }
            return super.shouldInterceptRequest(view, request);
        }

        
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if(WebViewBridgeManager.urlStringLooksInvalid(url)){
                return true;
            }
            return false;
        }
        
        @Override
        public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
            super.doUpdateVisitedHistory(view, url, isReload);
            dispatchEvent(view, new TopLoadingStartEvent(view.getId(), this.createWebViewEvent(view, url)));
        }
    }

    protected static void dispatchEvent(WebView webView, Event event) {
        ReactContext reactContext = (ReactContext) webView.getContext();
        EventDispatcher eventDispatcher = (reactContext.getNativeModule(UIManagerModule.class)).getEventDispatcher();
        eventDispatcher.dispatchEvent(event);
    }
}
