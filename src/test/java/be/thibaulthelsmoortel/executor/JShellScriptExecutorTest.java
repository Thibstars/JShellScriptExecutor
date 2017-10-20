package be.thibaulthelsmoortel.executor;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import be.thibaulthelsmoortel.executor.model.DatedSnippetEventWrapper;
import be.thibaulthelsmoortel.executor.model.Evaluation;
import be.thibaulthelsmoortel.executor.util.ResourceUtil;
import java.io.IOException;
import java.util.Map;
import lombok.extern.log4j.Log4j2;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Thibault Helsmoortel
 */
@Log4j2
public class JShellScriptExecutorTest {

    private JShellScriptExecutor scriptExecutor;

    @Before
    public void setUp() throws Exception {
        this.scriptExecutor = new JShellScriptExecutor();
    }

    @Test
    public void shouldExecuteScriptWithoutFailureEvaluation() throws Exception {
        String script = ResourceUtil.getResource("mySnippets.jsh").getPath();
        scriptExecutor.execute(script);
        scriptExecutor.addPropertyChangeListener(evt -> {
            if (evt.getNewValue().equals(Evaluation.FAILURE)) {
                fail("A snippet got evaluated as a failure.");
            }
        });

        System.out.println(scriptExecutor.getScriptEvaluationResult(script));
    }

    @Test
    public void shouldExecuteWithFailureEvaluation() throws Exception {
        scriptExecutor.execute(ResourceUtil.getResource("mySnippetsNoSemiColons.jsh").getPath());
        assertThat("Last line must be evaluated as a failure.", scriptExecutor.getLastSnippetEventEvaluation(),
                equalTo(Evaluation.FAILURE));
    }

    @Test(expected = IOException.class)
    public void shouldNotFindFile() throws Exception {
        scriptExecutor.execute("doesNotExist");
    }

    @Test
    public void shouldExecuteOneFailure() throws Exception {
        String script = ResourceUtil.getResource("mySnippetsOneFailure.jsh").getPath();
        scriptExecutor.execute(script);

        Map<DatedSnippetEventWrapper, Evaluation> scriptEvaluation = scriptExecutor.getExecutions().get(script);
        int expectedFailures = 1;
        final int[] failureCount = {0};
        scriptEvaluation.forEach((snippetEvent, evaluation) -> {
            if (evaluation.equals(Evaluation.FAILURE)) {
                failureCount[0]++;
            }
        });

        assertThat(String.format("Script must execute with %d failure(s).", expectedFailures), failureCount[0],
                equalTo(expectedFailures));
    }
}
