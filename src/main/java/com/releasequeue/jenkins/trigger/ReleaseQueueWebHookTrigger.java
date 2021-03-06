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
    private String application;
    private String event;
    private URL triggerUrl;
    private String webhookName;
    private ServerConnection server;

    public ReleaseQueueWebHookTrigger(String application, String event, ServerConnection server) throws Exception {
        super();
        this.application = application;
        this.event = event;
        this.server = server;
    }

    @DataBoundConstructor
    public ReleaseQueueWebHookTrigger(String application, String event) throws Exception {
        this(application, event, ConnectionManager.getConnection());
    }

    public String getApplication(){
        return this.application;
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
        if(application != null && !application.isEmpty() && !application.startsWith("Error:")){
            try {
                URL absoluteUrl = new URL(project.getAbsoluteUrl());
                this.triggerUrl = new URL(absoluteUrl, "rqhook/");

                ReleaseQueueGlobalDescriptor.DescriptorImpl globalDescriptor =
                    (ReleaseQueueGlobalDescriptor.DescriptorImpl)Jenkins.getInstance().getDescriptor(ReleaseQueueGlobalDescriptor.class);
                String serverUrl = globalDescriptor.getServerUrl(),
                       userName = globalDescriptor.getUserName(),
                       apiKey = globalDescriptor.getApiKey();
                
                if (serverUrl != null && userName != null && apiKey != null)
                    server.setCredentials(serverUrl, userName, apiKey);
                
                webhookName = "jenkins_" + project.getFullName();
                server.addWebHookSubscription(application, event, webhookName, triggerUrl.toString());

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
            if(application != null && webhookName != null){
                server.removeWebHookSubscription(application, webhookName);
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } catch (IOException e){
            throw new RuntimeException(e);
        }
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

        public ListBoxModel doFillApplicationItems() throws InterruptedException, IOException{
            ReleaseQueueGlobalDescriptor.DescriptorImpl globalDescriptor =
                (ReleaseQueueGlobalDescriptor.DescriptorImpl)Jenkins.getInstance().getDescriptor(ReleaseQueueGlobalDescriptor.class);

            ListBoxModel items = new ListBoxModel();

            if (globalDescriptor.isValid()){

                try{
                    ServerConnection server = ConnectionManager.getConnection();
                    JSONArray applications = server.listApplications();
                    if (applications != null){
                        for (Object application: applications){
                            items.add(((JSONObject)application).get("name").toString());
                        }
                    }
                }
                catch (Exception e) {
                    items.add("Error", "Error:" + e.getMessage());
                }
            }

            return items;
        }

        public FormValidation doCheckApplication(@QueryParameter String value) {
            ReleaseQueueGlobalDescriptor.DescriptorImpl globalDescriptor =
                (ReleaseQueueGlobalDescriptor.DescriptorImpl)Jenkins.getInstance().getDescriptor(ReleaseQueueGlobalDescriptor.class);

            if (globalDescriptor.isValid()){

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

        public ListBoxModel doFillEventItems() throws InterruptedException, IOException{
            ReleaseQueueGlobalDescriptor.DescriptorImpl globalDescriptor =
                (ReleaseQueueGlobalDescriptor.DescriptorImpl)Jenkins.getInstance().getDescriptor(ReleaseQueueGlobalDescriptor.class);


            ListBoxModel items = new ListBoxModel();

            if (globalDescriptor.isValid()){

                try{
                    ServerConnection server = ConnectionManager.getConnection();
                    JSONArray events = server.listSupportedEvents();
                    if (events != null){
                        for (Object event: events){
                            items.add(((JSONObject)event).get("name").toString());
                        }
                    }
                }
                catch (Exception e) {
                    items.add("Error", "Error:" + e.getMessage());
                }
            }

            return items;
        }

        public FormValidation doCheckEvent(@QueryParameter String value) {
            ReleaseQueueGlobalDescriptor.DescriptorImpl globalDescriptor =
                (ReleaseQueueGlobalDescriptor.DescriptorImpl)Jenkins.getInstance().getDescriptor(ReleaseQueueGlobalDescriptor.class);

            if (globalDescriptor.isValid()){

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