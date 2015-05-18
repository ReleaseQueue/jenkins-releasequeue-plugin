package jenkinsreleasequeueplugin.jenkinsreleasequeue;
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
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.bind.JavaScriptMethod;


import java.util.*;
import javax.servlet.ServletException;
import java.io.*;

public class PackagePublisher extends Notifier {

    private final String distribution;
    private final String component;
    private final String artifactsPattern;


    @DataBoundConstructor
    public PackagePublisher(String distribution, String component, String artifactsPattern) {
        this.distribution = distribution;
        this.component = component;
        this.artifactsPattern = artifactsPattern;
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

        String serverUrl = getDescriptor().getServerUrl(),
            email = getDescriptor().getEmail(),
            password = getDescriptor().getPassword();

        PackageUploader pkgUploader = new PackageUploader(serverUrl, email, password);
        
        FilePath workspace = build.getWorkspace();

        FilePath[] packages = workspace.list(getArtifactsPattern());
        log(listener, String.format("Found files: %d", packages.length));
        
        for(FilePath pkg: packages){
            log(listener, String.format("Uploading: %s", pkg.toString()));
            pkgUploader.uploadPackage(pkg, getDistribution(), getComponent());
        }
        
        return true;
    }

    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        private String serverUrl;
        private String email;
        private String password;

        public DescriptorImpl() {
            super(PackagePublisher.class);
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
