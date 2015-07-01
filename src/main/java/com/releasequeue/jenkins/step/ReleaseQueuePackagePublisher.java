package com.releasequeue.jenkins.step;
import com.releasequeue.jenkins.connection.ConnectionManager;
import com.releasequeue.jenkins.descriptor.ReleaseQueueGlobalDescriptor;
import com.releasequeue.server.ServerConnection;
import hudson.Launcher;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.util.FormValidation;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.bind.JavaScriptMethod;

import java.io.*;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.QueryParameter;

public class ReleaseQueuePackagePublisher extends Notifier {

    private final String distribution;
    private final String component;
    private final String artifactsPattern;
    private ServerConnection server;

    public ReleaseQueuePackagePublisher(String distribution, 
            String component, 
            String artifactsPattern, 
            ServerConnection rqserver){
        this.distribution = distribution;
        this.component = component;
        this.artifactsPattern = artifactsPattern;
        this.server = rqserver;
    }
    
    @DataBoundConstructor
    public ReleaseQueuePackagePublisher(String distribution, String component, String artifactsPattern) {
        this(distribution, component, artifactsPattern, ConnectionManager.getConnection());
    }

    public String getDistribution(){
        return this.distribution;
    }
    
    public String getComponent(){
        return this.component;
    }
    
    public String getArtifactsPattern(){
        return this.artifactsPattern;
    }
    
    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    
    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }

    private void log(BuildListener listener, String message){
        listener.getLogger().println(String.format("[ReleaseQueuePackagePublisher] %s", message));
    }

    // Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }
    
    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) 
            throws InterruptedException, IOException {
         
        Result buildResult = build.getResult();
        if (buildResult == Result.FAILURE || buildResult == Result.ABORTED) {
            return true;
        } 
        
        ReleaseQueueGlobalDescriptor.DescriptorImpl globalDescriptor = 
            (ReleaseQueueGlobalDescriptor.DescriptorImpl)Jenkins.getInstance().getDescriptor(ReleaseQueueGlobalDescriptor.class);                      
        server.setCredentials(globalDescriptor.getServerUrl(), globalDescriptor.getEmail(), globalDescriptor.getPassword());
        
        FilePath workspace = build.getWorkspace();

        FilePath[] packages = workspace.list(getArtifactsPattern());
        log(listener, String.format("Found files: %d", packages.length));
        
        for(FilePath pkg: packages){
            log(listener, String.format("Uploading: %s", pkg.toString()));
            server.uploadPackage(pkg, getDistribution(), getComponent());
        }

        return true;
    }

    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        public DescriptorImpl() {
            super(ReleaseQueuePackagePublisher.class);
            load();
        }

        @Override
        @JavaScriptMethod
        public String getDisplayName() {
            return "Push to ReleaseQueue";
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }
        
        private boolean checkGlobalConfig(){
            ReleaseQueueGlobalDescriptor.DescriptorImpl globalDescriptor = 
                (ReleaseQueueGlobalDescriptor.DescriptorImpl)Jenkins.getInstance().getDescriptor(ReleaseQueueGlobalDescriptor.class);               
            
            String serverUrl = globalDescriptor.getServerUrl(),
                   email = globalDescriptor.getEmail(),
                   password = globalDescriptor.getPassword();

             return serverUrl != null && !serverUrl.isEmpty() &&
                    email != null && !email.isEmpty() &&
                    password != null && !password.isEmpty();

        }
        
        private FormValidation missingGlobalConfig(){
            return FormValidation.error("Missing global configuration." + 
                "Go to 'Config System' and fill in email and user");                                
        }            
        
        public FormValidation doCheckDistribution(@QueryParameter String value) {
                if (checkGlobalConfig()){
                    if (value == null || value.isEmpty())
                        return FormValidation.error("Distribution needs to be filled in");
                    else
                        return FormValidation.ok();
                }
                else{
                    return missingGlobalConfig();
                }          
        }

        public FormValidation doCheckComponent(@QueryParameter String value) {
                if (checkGlobalConfig()){
                    if (value == null || value.isEmpty())
                        return FormValidation.error("Component needs to be filled it");
                    else
                        return FormValidation.ok();
                }
                else{
                    return missingGlobalConfig();
                }          
        }

        public FormValidation doCheckArtifactsPattern(@QueryParameter String value) {
                if (checkGlobalConfig()){
                    if (value == null || value.isEmpty())
                        return FormValidation.error("ArtifactsPattern needs to be filled it");
                    else
                        return FormValidation.ok();
                }
                else{
                    return missingGlobalConfig();
                }          
        }
        
        
        
    }
    
}
