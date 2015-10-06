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
        private String email;
        private String password;

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
            email = formData.getString("email");
            password = formData.getString("password");
            save();
            return super.configure(req, formData);
        }

        public FormValidation doCheckServerUrl(@QueryParameter String serverUrl, @QueryParameter String email, @QueryParameter String password){
            if (serverUrl != null && !serverUrl.isEmpty() &&
                email != null && !email.isEmpty() &&
                password != null && !password.isEmpty()){

                try{
                    ReleaseQueueServer server = new ReleaseQueueServer(serverUrl, email, password);
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

        public String getEmail(){
            return email;
        }

        public String getPassword(){
            return password;
        }

    }
}