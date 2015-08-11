/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.releasequeue.server;

import hudson.FilePath;
import java.io.IOException;
import java.net.MalformedURLException;
import org.apache.http.HttpResponse;
import org.json.simple.JSONArray;

/**
 *
 * @author adrian
 */
public interface ServerConnection {

    public void setCredentials(String serverUrl, String email, String password) throws MalformedURLException;
    public HttpResponse uploadPackage(FilePath packagePath, String distribution, String component) throws MalformedURLException, IOException;
    public JSONArray listApplications() throws IOException;
    public JSONArray listSubscriptions(String applicationName) throws MalformedURLException, IOException;
    public void addWebHookSubscription(String applicationName, String webhookName, String targetUrl) throws MalformedURLException, IOException;
    public void removeWebHookSubscription(String applicationName, String webhookName) throws IOException;
    
}
