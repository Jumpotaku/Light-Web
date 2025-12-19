package com.example.lightweb20;

import android.app.Activity;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class ScrollVisibilityController {
    private boolean isToolbarVisible = true;
    private int lastScrollY = 0;

    /**
     * MainActivity の resume 時に呼び出してください。
     * Activity から toolbarGroup と webView を自動取得し、スクロールに応じて
     * toolbarGroup を上下アニメーションで隠したり表示したりします。
     */
    public void injectScrollControl(Activity activity) {
        View toolbarGroup = activity.findViewById(
                activity.getResources().getIdentifier("toolbarGroup", "id", activity.getPackageName()));
        WebView webView = activity.findViewById(
                activity.getResources().getIdentifier("webView",    "id", activity.getPackageName()));

        if (toolbarGroup == null || webView == null) return;

        toolbarGroup.setVisibility(View.VISIBLE);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                injectScrollListener(webView);
            }
        });

        webView.addJavascriptInterface(new Object() {
            @android.webkit.JavascriptInterface
            public void onScrollChanged(final int scrollY) {
                activity.runOnUiThread(() -> {
                    if (scrollY > lastScrollY + 30 && isToolbarVisible) {
                        hideView(toolbarGroup);
                        isToolbarVisible = false;
                    } else if (scrollY < lastScrollY - 30 && !isToolbarVisible) {
                        showView(toolbarGroup);
                        isToolbarVisible = true;
                    }
                    lastScrollY = scrollY;
                });
            }
        }, "ScrollListener");
    }

    private void injectScrollListener(WebView webView) {
        // ページ内 JavaScript でスクロール監視し、onScrollChanged を呼び出す
        String js = "javascript:(function() {"
                + "  window.onscroll = function() {"
                + "    window.ScrollListener.onScrollChanged(window.scrollY);"
                + "  };"
                + "})()";
        webView.loadUrl(js);
    }

    private void hideView(View view) {
        view.animate()
                .translationY(-view.getHeight())
                .setDuration(300)
                .withEndAction(() -> view.setVisibility(View.GONE))
                .start();
    }

    private void showView(View view) {
        view.setVisibility(View.VISIBLE);
        view.animate()
                .translationY(0)
                .setDuration(300)
                .start();
    }
}
