/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.releasequeue.server;
import java.net.*;
import java.io.*;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.ParseException;
import org.json.simple.parser.JSONParser;

import hudson.FilePath;
import org.apache.commons.io.FilenameUtils;

import org.apache.http.impl.client.*;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.*;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.util.EntityUtils;

public class ReleaseQueueServer implements ServerConnection{
    private final String basePath = "/users";
    private final String signInPath = "/signin";
    private URL serverUrl;
    private String email;
    private String password;
    private String token;
    private String userName;
    
    public ReleaseQueueServer(String serverUrl, String email, String password) 
            throws MalformedURLException, IOException{
        setCredentials(serverUrl, email, password);
    }

    @Override
    public final void setCredentials(String serverUrl, String email, String password) throws MalformedURLException{
        this.email = email;
        this.password = password;
        this.serverUrl = serverUrl != null ? new URL(serverUrl) : null;
    }
    
    private void requestToken(String email, String password) throws IOException{       
        URL signInUrl = new URL(this.serverUrl, this.signInPath);
        HttpPost request = new HttpPost(signInUrl.toString());

        JSONObject jsonData = new JSONObject();
        jsonData.put("email", email);
        jsonData.put("password", password);

        JSONObject rezObj = (JSONObject)postJsonRequest(signInUrl, jsonData);
        this.token = rezObj.get("token").toString();
        this.userName = rezObj.get("username").toString();
    }
    
    public HttpResponse uploadPackage(FilePath packagePath, String distribution, String component)
        throws MalformedURLException, IOException {
        requestToken(email, password);
        String repoType = FilenameUtils.getExtension(packagePath.toString());
        
        String uploadPath = String.format("%s/%s/repositories/%s/packages?distribution=%s&component=%s", this.basePath, this.userName, repoType, distribution, component);
        URL uploadPackageUrl = new URL(this.serverUrl, uploadPath);
        
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost uploadFile = new HttpPost(uploadPackageUrl.toString());
        uploadFile.addHeader("x-auth-token", this.token);

        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.addBinaryBody("file", new File(packagePath.toString()), ContentType.APPLICATION_OCTET_STREAM, packagePath.getName());
        HttpEntity multipart = builder.build();

        uploadFile.setEntity(multipart);

        HttpResponse response = httpClient.execute(uploadFile);
        return response;
    }
    
    private Object postJsonRequest(URL url, JSONObject payload) throws IOException{
        HttpPost request = new HttpPost(url.toString());
        request.addHeader("x-auth-token", this.token);
               
        CloseableHttpClient httpClient = HttpClients.createDefault();

        try{
            
            StringWriter data = new StringWriter();
            payload.writeJSONString(data);           
            
            StringEntity params = new StringEntity(data.toString());
            request.addHeader("content-type", "application/json");
            request.setEntity(params);                        
            
            HttpResponse response = httpClient.execute(request);

            String json_string = EntityUtils.toString(response.getEntity());
            JSONParser parser = new JSONParser();

            return parser.parse(json_string);
        }
        catch(ParseException pe){
            throw new RuntimeException("Failed to parse json responce", pe); 
        }
        finally {
            httpClient.getConnectionManager().shutdown();
        }
        
    }
    
    private Object getJsonRequest(URL url) throws IOException{
        HttpGet request = new HttpGet(url.toString());
        request.addHeader("x-auth-token", this.token);
               
        CloseableHttpClient httpClient = HttpClients.createDefault();

        try{
            HttpResponse response = httpClient.execute(request);
            StatusLine statusLine = response.getStatusLine();
            int statusCode = statusLine.getStatusCode();
            if (statusCode >= 200 && statusCode < 300){
                String json_string = EntityUtils.toString(response.getEntity());
                JSONParser parser = new JSONParser();
                return parser.parse(json_string);
            }
        }
        catch(ParseException pe){
            throw new RuntimeException("Failed to parse json responce", pe); 
        }
        finally {
            httpClient.getConnectionManager().shutdown();
        }        
        return null;
    }
    
    private void deleteRequest(URL url) throws IOException{
        HttpDelete request = new HttpDelete(url.toString());
        request.addHeader("x-auth-token", this.token);
               
        CloseableHttpClient httpClient = HttpClients.createDefault();

        HttpResponse response = httpClient.execute(request);
        httpClient.getConnectionManager().shutdown();
    }    
    
    private String getApplicationIdByName(String applicationName) throws IOException {
        JSONArray applications = listApplications();
        String applicationId = null;
        for(Object application: applications){
            if(((JSONObject)application).get("name").toString().equals(applicationName) ){
                applicationId = ((JSONObject)application).get("id").toString();
                break;
            }
        }
        return applicationId;
    }

    private String getSubscriptionIdByUrl(String applicationId, String targetUrl) throws IOException{
        JSONArray subscriptions = listSubscriptions(applicationId);
        String subscriptionId = null;
        for (Object subscr: subscriptions){
            Object subscrTargetUrl = ((JSONObject)subscr).get("target_url");
            if (subscrTargetUrl != null && subscrTargetUrl.toString().equals(targetUrl)){
                subscriptionId = ((JSONObject)subscr).get("id").toString();
                break;
            }
        }
        return subscriptionId;
    }
    
    public JSONArray listApplications() throws IOException {
        if (this.userName == null){
             requestToken(email, password);
        }
        String applicationsPath = String.format("%s/%s/applications", this.basePath, this.userName);
        URL applicationsUrl = new URL(this.serverUrl, applicationsPath);
        
        JSONArray applications = (JSONArray)getJsonRequest(applicationsUrl);        
        return applications;
    } 
    
    public JSONArray listSubscriptions(String applicationId) throws MalformedURLException, IOException {
        String subscriptionsPath = String.format("%s/%s/applications/%s/webhook_subscriptions", this.basePath, this.userName, applicationId);
        URL subscriptionsUrl = new URL(this.serverUrl, subscriptionsPath);
        JSONArray subscriptions = (JSONArray)getJsonRequest(subscriptionsUrl);
        return subscriptions;
    }
    
    public void addWebHookSubscription(String applicationName, String targetUrl)
    throws MalformedURLException, IOException 
    {
        requestToken(email, password);
        String applicationId = getApplicationIdByName(applicationName);

        if (applicationId != null){
            String subscriptionId = getSubscriptionIdByUrl(applicationId, targetUrl);
            if (subscriptionId == null){
                String subscriptionsPath = String.format("%s/%s/applications/%s/webhook_subscriptions", this.basePath, this.userName, applicationId);
                URL subscriptionsUrl = new URL(this.serverUrl, subscriptionsPath);

                JSONObject jsonData = new JSONObject();
                jsonData.put("event_name", "application_version.create");
                jsonData.put("application_id", applicationId);
                jsonData.put("username", this.userName);
                jsonData.put("target_url", targetUrl);
                jsonData.put("payload_type", "json");
                
                postJsonRequest(subscriptionsUrl, jsonData);
            }
            
        }
    }

    public void removeWebHookSubscription(String applicationName, String targetUrl) throws IOException{
        requestToken(email, password);
        
        String applicationsId = getApplicationIdByName(applicationName);
        if (applicationsId == null)
            return;
        
        String subscriptionId = getSubscriptionIdByUrl(applicationsId, targetUrl);        
        if (subscriptionId == null)
            return;
        
        String subscriptionsPath = String.format("%s/%s/applications/%s/webhook_subscriptions/%s", this.basePath, this.userName, applicationsId, subscriptionId);
        URL subscriptionsUrl = new URL(this.serverUrl, subscriptionsPath);
        
        deleteRequest(subscriptionsUrl);
    }
    
}
