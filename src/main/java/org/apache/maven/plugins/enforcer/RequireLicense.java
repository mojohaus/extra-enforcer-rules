package org.apache.maven.plugins.enforcer;

import org.apache.maven.enforcer.rule.api.EnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.model.License;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Manuel Recena
 * @since TODO
 */
public class RequireLicense implements EnforcerRule {

    public void execute(EnforcerRuleHelper helper) throws EnforcerRuleException {
        Log log = helper.getLog();
        try {
            MavenProject project = (MavenProject) helper.evaluate( "${project}" );
            if (!"pom".equals(project.getPackaging())) {
                List<License> licenses = getLicenses(project);
                if (licenses.size() == 0) {
                    throw new EnforcerRuleException("You must set a license for this project");
                }
            } else {
                log.info("Ignoring " + this.getClass().getSimpleName() + " in this project");
            }
        } catch (ExpressionEvaluationException eee) {
            throw new EnforcerRuleException("Unable to get project.", eee);
        }
    }

    List<License> getLicenses(MavenProject project) {
        MavenProject parent = project.getParent();
        List<License> licenses = new ArrayList<License>();
        licenses.addAll(project.getLicenses());
        while (parent != null) {
            licenses.addAll(parent.getLicenses());
            parent = parent.getParent();
        }
        return licenses;
    }

    public boolean isCacheable() {
        return false;
    }

    public boolean isResultValid(EnforcerRule enforcerRule) {
        return false;
    }

    public String getCacheId() {
        return null;
    }
}
