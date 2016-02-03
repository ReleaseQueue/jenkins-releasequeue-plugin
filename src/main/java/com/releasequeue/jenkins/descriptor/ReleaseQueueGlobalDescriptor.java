package com.releasequeue.jenkins.descriptor;

import com.releasequeue.server.ReleaseQueueServer;
import hudson.Extension;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.bind.JavaScriptMethod;

import jenkins.model.Jenkins;
import org.kohsuke.stapler.QueryParameter;

public class ReleaseQueueGlobalDescriptor extends JobProperty<Job<?, ?>>{

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)Jenkins.getInstance().getDescriptor(getClass());
    }

    @Extension
    public static final class DescriptorImpl extends JobPropertyDescriptor {

        private String serverUrl;
        private String userName;
        private String apiKey;

        public DescriptorImpl() {
            super(ReleaseQueueGlobalDescriptor.class);
            load();
        }

        @Override
        @JavaScriptMethod
        public String getDisplayName() {
            return "ReleaseQueue Plugin";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            // To persist global configuration information,
            // set that to properties and call save().
            serverUrl = formData.getString("serverUrl");
            userName = formData.getString("userName");
            apiKey = formData.getString("apiKey");
            save();
            return super.configure(req, formData);
        }

        public FormValidation doCheckServerUrl(@QueryParameter String serverUrl, @QueryParameter String userName, @QueryParameter String apiKey){
            if (serverUrl != null && !serverUrl.isEmpty() &&
                userName != null && !userName.isEmpty() &&
                apiKey != null && !apiKey.isEmpty()){

                try{
                    ReleaseQueueServer server = new ReleaseQueueServer(serverUrl, userName, apiKey);
                    return FormValidation.ok();
                }
                catch(Exception ex){
                    return FormValidation.error("Failed to connect to server");
                }
            }
            else{
                return FormValidation.error("Fill in all fields");
            }
        }

        public String getServerUrl(){
            return serverUrl;
        }

        public String getUserName(){
            return userName;
        }

        public String getApiKey(){
            return apiKey;
        }

        public boolean isValid(){
            return serverUrl != null && !serverUrl.isEmpty() &&
                userName != null && !userName.isEmpty() &&
                apiKey != null && !apiKey.isEmpty();
        }
        
        public String defaultServerUrl(){
            return "https://api.releasequeue.com";
        }
        
    }
}