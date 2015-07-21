/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.releasequeue.jenkins.trigger;

import com.releasequeue.jenkins.connection.ConnectionManager;
import com.releasequeue.jenkins.descriptor.ReleaseQueueGlobalDescriptor;
import com.releasequeue.server.ServerConnection;
import java.io.IOException;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.triggers.Trigger;
import hudson.triggers.TriggerDescriptor;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import java.net.MalformedURLException;
import java.net.URL;
import jenkins.model.Jenkins;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.kohsuke.stapler.QueryParameter;


/**
 *
 * @author adrian
 */
public class ReleaseQueueWebHookTrigger extends Trigger<AbstractProject<?, ?>> {
    private String product;
    private URL triggerUrl;
    private ServerConnection server;
    
    public ReleaseQueueWebHookTrigger(String product, ServerConnection server) throws Exception {
        super();
        this.product = product;
        this.server = server;
    }
    
    @DataBoundConstructor
    public ReleaseQueueWebHookTrigger(String product) throws Exception {
        this(product, ConnectionManager.getConnection());
    }
    
    public String getProduct(){
        return this.product;
    }   

    // Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }
    
    @Override
    public void start(AbstractProject<?, ?> project, boolean newInstance){
        if(product != null && !product.isEmpty() && !product.startsWith("Error:")){
            
            try {
                URL absoluteUrl = new URL(project.getAbsoluteUrl());
                this.triggerUrl = new URL(absoluteUrl, "rqhook/");
                
                ReleaseQueueGlobalDescriptor.DescriptorImpl globalDescriptor = 
                    (ReleaseQueueGlobalDescriptor.DescriptorImpl)Jenkins.getInstance().getDescriptor(ReleaseQueueGlobalDescriptor.class);                      
                String serverUrl = globalDescriptor.getServerUrl(),
                       email = globalDescriptor.getEmail(),
                       password = globalDescriptor.getPassword();
                if (serverUrl != null && email != null && password != null)
                    server.setCredentials(serverUrl, email, password);
                
                server.addWebHookSubscription(product, triggerUrl.toString());
                
                project.addTrigger(this);
                
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            } catch (IOException e){
                throw new RuntimeException(e);
            }
            
            super.start(project, newInstance);
        }
    }

    @Override
    public void stop(){
        try{
            if(product != null && triggerUrl != null){
                server.removeWebHookSubscription(product, triggerUrl.toString());
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } catch (IOException e){
            throw new RuntimeException(e);
        }
    }    
        
    public String getproduct() {
        return this.product;
    }      
    
    @Extension
    public static class DescriptorImpl extends TriggerDescriptor {
        
        @Override
        public boolean isApplicable(Item item) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "ReleaseQueueBuildTrigger";
        }               
        
        public ListBoxModel doFillProductItems() throws InterruptedException, IOException{
            
            ReleaseQueueGlobalDescriptor.DescriptorImpl globalDescriptor = 
                (ReleaseQueueGlobalDescriptor.DescriptorImpl)Jenkins.getInstance().getDescriptor(ReleaseQueueGlobalDescriptor.class);   
            
            String serverUrl = globalDescriptor.getServerUrl(),
                   email = globalDescriptor.getEmail(),
                   password = globalDescriptor.getPassword();

            ListBoxModel items = new ListBoxModel();

            if (serverUrl != null && !serverUrl.isEmpty() &&
                email != null && !email.isEmpty() &&
                password != null && !password.isEmpty()){

                try{
                    ServerConnection server = ConnectionManager.getConnection();
                    JSONArray products = server.listProducts();
                    if (products != null){
                        for (Object product: products){
                            items.add(((JSONObject)product).get("name").toString());
                        }
                    }
                }
                catch (Exception e) {
                    items.add("Error", "Error:" + e.getMessage());
                }
            }
            
            return items;
        }
        
        public FormValidation doCheckProduct(@QueryParameter String value) {

            ReleaseQueueGlobalDescriptor.DescriptorImpl globalDescriptor = 
                (ReleaseQueueGlobalDescriptor.DescriptorImpl)Jenkins.getInstance().getDescriptor(ReleaseQueueGlobalDescriptor.class);               
            
            String serverUrl = globalDescriptor.getServerUrl(),
                   email = globalDescriptor.getEmail(),
                   password = globalDescriptor.getPassword();
            
            if (serverUrl != null && !serverUrl.isEmpty() &&
                email != null && !email.isEmpty() &&
                password != null && !password.isEmpty()){
           
                if(value != null && value.startsWith("Error:")){
                    return FormValidation.error(value);
                }
                else
                    return FormValidation.ok();
            }
            else{ 
                return FormValidation.error("Missing global configuration." + 
                        "Go to 'Config System' and fill in email and user");                
            }
        }
    }
    
}