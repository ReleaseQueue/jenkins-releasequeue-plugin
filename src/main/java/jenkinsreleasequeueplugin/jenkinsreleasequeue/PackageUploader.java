/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package jenkinsreleasequeueplugin.jenkinsreleasequeue;
import java.net.*;
import java.io.*;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.json.simple.parser.JSONParser;

import hudson.FilePath;

import org.apache.http.impl.client.*;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.util.EntityUtils;

public class PackageUploader {
    private final URL serverUrl;
    private final String signInPath = "/api/users/sign_in";
    private String token;
    private String userId;
    
    public PackageUploader(String serverUrl, String email, String password) 
            throws MalformedURLException, IOException{
        
        this.serverUrl = new URL(serverUrl);
        requestToken(email, password);
    }

    public final void requestToken(String email, String password) throws IOException{
        CloseableHttpClient httpClient = HttpClients.createDefault();
        
        try{
            URL signInUrl = new URL(this.serverUrl, this.signInPath);
            HttpPost request = new HttpPost(signInUrl.toString());
            
            JSONObject jsonData = new JSONObject();
            jsonData.put("email", email);
            jsonData.put("password", password);
            StringWriter data = new StringWriter();
            jsonData.writeJSONString(data);           
            
            StringEntity params = new StringEntity(data.toString());
            request.addHeader("content-type", "application/json");
            request.setEntity(params);            
            
            HttpResponse response = httpClient.execute(request);
            String json_string = EntityUtils.toString(response.getEntity());
            JSONParser parser = new JSONParser();
            JSONObject rezObj = (JSONObject)parser.parse(json_string);
            this.token = rezObj.get("auth_token").toString();
            this.userId = rezObj.get("user_id").toString();
            
        } 
        catch(ParseException pe){
            throw new RuntimeException("Failed to parse json responce", pe); 
        }
        finally {
            httpClient.getConnectionManager().shutdown();
        }        
    }
    
    public void uploadPackage(FilePath packagePath, String distribution, String component)
        throws MalformedURLException, IOException {
        
        String uploadPath = String.format("/api/users/%s/repositories/deb/%s/%s/packages", this.userId, distribution, component);
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
    
}
