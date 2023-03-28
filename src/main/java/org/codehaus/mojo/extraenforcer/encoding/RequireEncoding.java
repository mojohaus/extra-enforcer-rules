package org.codehaus.mojo.extraenforcer.encoding;

import javax.inject.Inject;
import javax.inject.Named;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.enforcer.rule.api.AbstractEnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleError;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.StringUtils;
import org.freebsd.file.FileEncoding;

/**
 * Checks file encodings to see if they match the project.build.sourceEncoding If file encoding can not be determined it
 * is skipped.
 *
 * @see <a href="https://github.com/mikedon/encoding-enforcer">mikedon/encoding-enforcer</a>
 * @see <a href="https://github.com/ericbn/encoding-enforcer">ericbn/encoding-enforcer</a>
 */
@Named("requireEncoding")
public class RequireEncoding extends AbstractEnforcerRule {
    private static final String ISO_8859_15 = "ISO-8859-15";

    /**
     * Validate files match this encoding. If not specified then default to ${project.build.sourceEncoding}.
     */
    private String encoding = "";

    /**
     * Comma (or pipe) separated list of globs do include.
     */
    private String includes = "";

    /**
     * Comma (or pipe) separated list of globs do exclude.
     */
    private String excludes = "";

    /**
     * Enables SCM files exclusions. Enabled by default.
     */
    private boolean useDefaultExcludes = true;

    /**
     * Should the rule fail after the first error or should the errors be aggregated.
     */
    private boolean failFast = true;

    /**
     * Should the rule accept US-ASCII as an subset of UTF-8 and ISO-8859-1/-15.
     */
    private boolean acceptAsciiSubset = false;

    /**
     * Should the rule accept ISO-8859-1 as a subset of ISO-8859-15.
     */
    private boolean acceptIso8859Subset = false;

    private final MavenProject project;

    @Inject
    public RequireEncoding(MavenProject project) {
        this.project = project;
    }

    @Override
    public void execute() throws EnforcerRuleException {
        try {
            if (StringUtils.isBlank(encoding)) {
                encoding = project.getProperties().getProperty("project.build.sourceEncoding", "");
            }

            Set<String> acceptedEncodings = new HashSet<>(Collections.singletonList(encoding));
            if (encoding.equals(StandardCharsets.US_ASCII.name())) {
                getLog().warn("Encoding US-ASCII is hard to detect. Use UTF-8 or ISO-8859-1");
            }

            if (acceptAsciiSubset
                    && (encoding.equals(StandardCharsets.ISO_8859_1.name())
                            || encoding.equals(ISO_8859_15)
                            || encoding.equals(StandardCharsets.UTF_8.name()))) {
                acceptedEncodings.add(StandardCharsets.US_ASCII.name());
            }
            if (acceptIso8859Subset && encoding.equals(ISO_8859_15)) {
                acceptedEncodings.add("ISO-8859-1");
            }

            String basedir = project.getBasedir().getAbsolutePath();
            DirectoryScanner ds = new DirectoryScanner();
            ds.setBasedir(basedir);
            if (StringUtils.isNotBlank(includes)) {
                ds.setIncludes(includes.split("[,|]"));
            }
            if (StringUtils.isNotBlank(excludes)) {
                ds.setExcludes(excludes.split("[,|]"));
            }
            if (useDefaultExcludes) {
                ds.addDefaultExcludes();
            }
            ds.scan();
            StringBuilder filesInMsg = new StringBuilder();
            for (String file : ds.getIncludedFiles()) {
                String fileEncoding = getEncoding(new File(basedir, file));
                getLog().debug(() -> file + "==>" + fileEncoding);
                if (fileEncoding != null && !acceptedEncodings.contains(fileEncoding)) {
                    filesInMsg.append(file);
                    filesInMsg.append("==>");
                    filesInMsg.append(fileEncoding);
                    filesInMsg.append(System.lineSeparator());
                    if (failFast) {
                        throw new EnforcerRuleException(filesInMsg.toString());
                    }
                }
            }
            if (filesInMsg.length() > 0) {
                throw new EnforcerRuleException("Files not encoded in " + encoding + ":\n" + filesInMsg);
            }
        } catch (IOException ex) {
            throw new EnforcerRuleError("Reading Files", ex);
        }
    }

    protected String getEncoding(File file) throws IOException {
        FileEncoding fileEncoding = new FileEncoding();
        if (!fileEncoding.guessFileEncoding(Files.readAllBytes(file.toPath()))) {
            return null;
        }

        getLog().debug(() -> String.format(
                "%s: (%s) %s; charset=%s",
                file, fileEncoding.getCode(), fileEncoding.getType(), fileEncoding.getCodeMime()));

        return fileEncoding.getCodeMime().toUpperCase();
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public String getIncludes() {
        return includes;
    }

    public void setIncludes(String includes) {
        this.includes = includes;
    }

    public String getExcludes() {
        return excludes;
    }

    public void setExcludes(String excludes) {
        this.excludes = excludes;
    }

    public boolean isUseDefaultExcludes() {
        return useDefaultExcludes;
    }

    public void setUseDefaultExcludes(boolean useDefaultExcludes) {
        this.useDefaultExcludes = useDefaultExcludes;
    }
}
