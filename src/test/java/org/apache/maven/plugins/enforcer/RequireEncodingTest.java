package org.apache.maven.plugins.enforcer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;

import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.plugin.logging.Log;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class RequireEncodingTest {
  @Rule
  public ExpectedException exception = ExpectedException.none();

  private EnforcerRuleHelper helper;
  private RequireEncoding rule;

  @Before
  public void initFields() {
    helper = mock(EnforcerRuleHelper.class);
    rule = new RequireEncoding();
  }

  @Test
    public void successUTF8ForSimpleAscii() throws Exception {
    
    when(helper.evaluate("${basedir}")).thenReturn(new File("src/test/resources").getAbsolutePath());
    when(helper.evaluate("${project.build.sourceEncoding}")).thenReturn("UTF-8");
    when(helper.getLog()).thenReturn(mock(Log.class));
    
    rule.setIncludes("ascii.txt");

        rule.execute(helper);
    }

    @Test
    public void failUTF8ForIso8591() throws Exception {

        when(helper.evaluate("${basedir}")).thenReturn(new File("src/test/resources").getAbsolutePath());
        when(helper.evaluate("${project.build.sourceEncoding}")).thenReturn("UTF-8");
        when(helper.getLog()).thenReturn(mock(Log.class));

        rule.setIncludes("iso88591.txt");

        exception.expect(EnforcerRuleException.class);
        rule.execute(helper);
    }

    @Test
    public void successUTF8() throws Exception {

        when(helper.evaluate("${basedir}")).thenReturn(new File("src/test/resources").getAbsolutePath());
        when(helper.evaluate("${project.build.sourceEncoding}")).thenReturn("UTF-8");
        when(helper.getLog()).thenReturn(mock(Log.class));

        rule.setIncludes("utf8.txt");

        rule.execute(helper);
    }
}
