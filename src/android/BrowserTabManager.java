package com.jonathanreisdorf.plugin.InAppBrowserXwalk;

import com.jonathanreisdorf.plugin.InAppBrowserXwalk.BrowserResourceClient;

import java.util.ArrayList;
import java.util.ListIterator;

import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONException;

import org.xwalk.core.XWalkNavigationItem;
import org.xwalk.core.XWalkView;

import android.app.Activity;
import android.graphics.Color;
import android.view.ViewParent;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;


public class BrowserTabManager {
    private ArrayList<XWalkView> tabs = new ArrayList<>();
    private ArrayList<BrowserResourceClient> resourceClients = new ArrayList<>();

    private XWalkView currentTab = null;
    private XWalkView previousTab = null;
    private BrowserResourceClient currentResourceClient = null;
    private BrowserResourceClient previousResourceClient = null;

    private Activity activity;
    private LinearLayout mainLayout;
    private CallbackContext callbackContext;
    private XWalkView navigationWebView;


    BrowserTabManager(Activity activity, LinearLayout mainLayout, CallbackContext callbackContext, XWalkView navigationWebView) {
        this.activity = activity;
        this.mainLayout = mainLayout;
        this.callbackContext = callbackContext;
        this.navigationWebView = navigationWebView;
    }

    public XWalkView initialize(String url) {
        return this.addTab(url, null, false, false);
    }

    public XWalkView addTab(String url) {
        return this.addTab(url, null, false, true);
    }

    public XWalkView addTab(String url, boolean systemTab) {
        return this.addTab(url, null, systemTab, true);
    }

    public XWalkView addTab(String url, String customUserAgentString, boolean systemTab, boolean openTab) {
        XWalkView xWalkWebView = new XWalkView(this.activity, this.activity);
        BrowserResourceClient browserResourceClient = new BrowserResourceClient(xWalkWebView, this.callbackContext, this.navigationWebView);

        xWalkWebView.setResourceClient(browserResourceClient);
        xWalkWebView.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, (float) 1));

        if (customUserAgentString != null && customUserAgentString != "") {
            xWalkWebView.setUserAgentString(customUserAgentString);
        }

        if (url != null && url != "") {
            xWalkWebView.load(url, "");
        }

        if (systemTab) {
            xWalkWebView.setBackgroundColor(Color.BLACK);
            browserResourceClient.isSystem = true;
            browserResourceClient.triggerJavascriptHandler("onSystemTabOpen", null);
        } else {
            this.tabs.add(xWalkWebView);
            this.resourceClients.add(browserResourceClient);
        }

        if (this.currentTab == null) {
            this.currentTab = xWalkWebView;
            this.currentResourceClient = browserResourceClient;
            browserResourceClient.isActive = true;
        }

        if (openTab) {
            this.openTab(xWalkWebView, browserResourceClient);
        }

        return xWalkWebView;
    }

    public void closeSystemTab() {
        this.closeSystemTab(null, null);
    }

    public void closeSystemTab(XWalkView newTab, BrowserResourceClient newResourceClient) {
        if (newTab == null || newResourceClient == null) {
            newTab = this.previousTab;
            newResourceClient = this.previousResourceClient;
        }

        if (newTab == null || newResourceClient == null || !this.currentResourceClient.isSystem) {
            return;
        }

        this.openTab(newTab, newResourceClient);

        this.previousTab.onDestroy();
        this.previousTab = null;
        this.previousResourceClient = null;

        newResourceClient.triggerJavascriptHandler("onSystemTabClose", null);
    }

    public void closeTabByIndex(final int index) {
        XWalkView tab = this.tabs.get(index);
        BrowserResourceClient resourceClient = this.resourceClients.get(index);

        this.tabs.remove(tab);
        this.resourceClients.remove(resourceClient);
        tab.onDestroy();


        boolean isEmptyList = this.tabs.size() < 1;
        if (isEmptyList) {
            this.addTab("about:blank", null, false, false);
        }

        if (isEmptyList || this.previousTab == null || this.previousResourceClient == null) {
            int lastTabIndex = this.tabs.size() - 1;
            this.previousTab = this.tabs.get(lastTabIndex);
            this.previousResourceClient = this.resourceClients.get(lastTabIndex);
        }

        if (isEmptyList) {
            this.closeSystemTab();
        }
    }

    private void openTab(final XWalkView newTab, final BrowserResourceClient newResourceClient) {
        this.previousTab = this.currentTab;
        this.previousResourceClient = this.currentResourceClient;

        this.currentTab = newTab;
        this.currentResourceClient = newResourceClient;

        if (this.previousTab != null && this.previousResourceClient != null) {
            this.previousTab.stopLoading();
            this.previousResourceClient.isActive = false;
        }

        this.currentResourceClient.isActive = true;
        this.currentResourceClient.broadcastNavigationItemDetails(this.currentTab);

        this.mainLayout.removeViewAt(0);
        this.mainLayout.addView(this.currentTab, 0);
        this.mainLayout.invalidate();
    }

    public void openTabByIndex(final int index, final boolean closeSystemTab) {
        XWalkView newTab = this.tabs.get(index);
        BrowserResourceClient newResourceClient = this.resourceClients.get(index);

        if (closeSystemTab) {
            this.closeSystemTab(newTab, newResourceClient);
        } else {
            this.openTab(newTab, newResourceClient);
        }
    }

    public void load(String url) {
        this.currentTab.load(url, "");
    }

    public JSONArray getTabsArray() {
        JSONArray items = new JSONArray();
        XWalkNavigationItem navigationItem;

        for (int index = 0; index < this.tabs.size(); index++) {
            navigationItem = this.tabs.get(index).getNavigationHistory().getCurrentItem();
            items.put(this.resourceClients.get(index).getNavigationItemDetails(navigationItem));
        }

        return items;
    }

    /*public JSONArray getNavigationHistoryArray(int tabIndex) {
        XWalkNavigationHistory navigationHistory = this.tabs.get(tabIndex).getNavigationHistory();

        JSONArray history = new JSONArray();
        XWalkNavigationItem navigationItem;

        for (int index = 0; index < navigationHistory.size(); index++) {
            navigationItem = navigationHistory.getItemAt(index);
            history.put(this.resourceClients.get(tabIndex).getNavigationItemDetails(navigationItem));
        }

        return history;
    }*/
}
