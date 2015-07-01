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
    private final String basePath = "/api/users";
    private final String signInPath = basePath + "/sign_in";
    private URL serverUrl;
    private String email;
    private String password;
    private String token;
    private String userId;
    
    public ReleaseQueueServer(String serverUrl, String email, String password) 
            throws MalformedURLException, IOException{
        setCredentials(serverUrl, email, password);
    }

    @Override
    public final void setCredentials(String serverUrl, String email, String password) throws MalformedURLException{
        this.email = email;
        this.password = password;
        this.serverUrl = new URL(serverUrl);        
    }
    
    private void requestToken(String email, String password) throws IOException{       
        URL signInUrl = new URL(this.serverUrl, this.signInPath);
        HttpPost request = new HttpPost(signInUrl.toString());

        JSONObject jsonData = new JSONObject();
        jsonData.put("email", email);
        jsonData.put("password", password);

        JSONObject rezObj = (JSONObject)postJsonRequest(signInUrl, jsonData);
        this.token = rezObj.get("auth_token").toString();
        this.userId = rezObj.get("user_id").toString();
    }
    
    public void uploadPackage(FilePath packagePath, String distribution, String component)
        throws MalformedURLException, IOException {
        requestToken(email, password);
        
        String uploadPath = String.format("%s/%s/repositories/deb/%s/%s/packages", this.basePath, this.userId, distribution, component);
        URL uploadPackageUrl = new URL(this.serverUrl, uploadPath);
        
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost uploadFile = new HttpPost(uploadPackageUrl.toString());
        uploadFile.addHeader("x-auth-token", this.token);

        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.addBinaryBody("file", new File(packagePath.toString()), ContentType.APPLICATION_OCTET_STREAM, packagePath.getName());
        HttpEntity multipart = builder.build();

        uploadFile.setEntity(multipart);

        HttpResponse response = httpClient.execute(uploadFile);
        HttpEntity responseEntity = response.getEntity();
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
    
    private String getProductIdByName(String productName) throws IOException {
        JSONArray products = listProducts();
        String productId = null;
        for(Object product: products){
            if(((JSONObject)product).get("name").toString().equals(productName) ){
                productId = ((JSONObject)product).get("id").toString();
                break;
            }
        }
        return productId;
    }

    private String getSubscriptionIdByUrl(String productId, String targetUrl) throws IOException{
        JSONArray subscriptions = listSubscriptions(productId);
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
    
    public JSONArray listProducts() throws IOException {
        String productsPath = String.format("%s/%s/products", this.basePath, this.userId);
        URL productsUrl = new URL(this.serverUrl, productsPath);
        
        JSONArray products = (JSONArray)getJsonRequest(productsUrl);        
        return products;
    } 
    
    public JSONArray listSubscriptions(String productId) throws MalformedURLException, IOException {
        String subscriptionsPath = String.format("%s/%s/products/%s/webhook_subscriptions", this.basePath, this.userId, productId);
        URL subscriptionsUrl = new URL(this.serverUrl, subscriptionsPath);
        JSONArray subscriptions = (JSONArray)getJsonRequest(subscriptionsUrl);
        return subscriptions;
    }
    
    public void addWebHookSubscription(String productName, String targetUrl)
    throws MalformedURLException, IOException 
    {
        requestToken(email, password);
        String productId = getProductIdByName(productName);

        if (productId != null){
            String subscriptionId = getSubscriptionIdByUrl(productId, targetUrl);
            if (subscriptionId == null){
                String subscriptionsPath = String.format("%s/%s/products/%s/webhook_subscriptions", this.basePath, this.userId, productId);
                URL subscriptionsUrl = new URL(this.serverUrl, subscriptionsPath);

                JSONObject jsonData = new JSONObject();
                jsonData.put("event_name", "product_version.create");
                jsonData.put("product_id", productId);
                jsonData.put("user_id", this.userId);
                jsonData.put("target_url", targetUrl);
                jsonData.put("payload_type", "json");
                
                postJsonRequest(subscriptionsUrl, jsonData);
            }
            
        }
    }

    public void removeWebHookSubscription(String productName, String targetUrl) throws IOException{
        requestToken(email, password);
        
        String productId = getProductIdByName(productName);
        if (productId == null)
            return;
        
        String subscriptionId = getSubscriptionIdByUrl(productId, targetUrl);        
        if (subscriptionId == null)
            return;
        
        String subscriptionsPath = String.format("%s/%s/products/%s/webhook_subscriptions/%s", this.basePath, this.userId, productId, subscriptionId);
        URL subscriptionsUrl = new URL(this.serverUrl, subscriptionsPath);
        
        deleteRequest(subscriptionsUrl);
    }
    
}
