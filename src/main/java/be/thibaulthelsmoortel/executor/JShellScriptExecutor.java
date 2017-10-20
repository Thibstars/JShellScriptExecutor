package be.thibaulthelsmoortel.executor;

import be.thibaulthelsmoortel.executor.model.DatedSnippetEventWrapper;
import be.thibaulthelsmoortel.executor.model.Evaluation;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import jdk.jshell.JShell;
import jdk.jshell.Snippet;
import jdk.jshell.SnippetEvent;
import jdk.jshell.SourceCodeAnalysis;
import org.apache.commons.lang3.StringUtils;

/**
 * Executor of JShell expressions in sequence.
 *
 * @author Thibault Helsmoortel
 */
public class JShellScriptExecutor {

    private final List<PropertyChangeListener> listeners;


    private Evaluation lastSnippetEventEvaluation;

    private Map<String, Map<DatedSnippetEventWrapper, Evaluation>> executions;

    public JShellScriptExecutor() {
        this.listeners = new ArrayList<>();
        this.executions = new HashMap<>();
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        listeners.add(listener);
    }

    public void execute(String scriptFileName) throws IOException {
        JShell jshell = JShell.create();

        HashMap<DatedSnippetEventWrapper, Evaluation> currentExecution = new HashMap<>();
        jshell.onSnippetEvent(snippetEvent -> {
            handleSnippetEvent(snippetEvent, currentExecution);
        });

        String scriptContent = new String(Files.readAllBytes(Paths.get(scriptFileName)));
        String remaining = scriptContent;

        while (true) {
            // Read source line by line until semicolon
            SourceCodeAnalysis.CompletionInfo an = jshell.sourceCodeAnalysis().analyzeCompletion(remaining);
            if (!an.completeness().isComplete()) {
                break;
            }

            // If there are any method or class declarations in new lines, resolve it.
            jshell.eval(trimNewlines(an.source()));

            if (an.remaining().isEmpty()) {
                break;
            }
            // If there is a semicolon, execute next seq
            remaining = an.remaining();
        }

        executions.put(scriptFileName, currentExecution);
    }

    private String trimNewlines(String s) {
        int b = 0;
        while (b < s.length() && s.charAt(b) == '\n') {
            ++b;
        }
        int e = s.length() - 1;
        while (e >= 0 && s.charAt(e) == '\n') {
            --e;
        }
        return s.substring(b, e + 1);
    }

    private void handleSnippetEvent(SnippetEvent snippetEvent,
            HashMap<DatedSnippetEventWrapper, Evaluation> currentExecution) {
        Evaluation evaluation = evaluateSnippetEvent(snippetEvent);

        currentExecution.put(new DatedSnippetEventWrapper(snippetEvent), evaluation);
        changeLastEvaluationProperty(evaluation);
    }

    private Evaluation evaluateSnippetEvent(SnippetEvent snippetEvent) {
        String value = snippetEvent.value();
        if (!Objects.isNull(value) && value.trim().length() > 0) {
            return Evaluation.SUCCESS;
        }

        if (Snippet.Status.REJECTED.equals(snippetEvent.status())) {
            return Evaluation.FAILURE;
        }

        return Evaluation.WARNING;
    }

    private void changeLastEvaluationProperty(Evaluation evaluation) {
        PropertyChangeEvent changeEvent = new PropertyChangeEvent(this, "lastSnippetEventEvaluation",
                lastSnippetEventEvaluation, evaluation);
        listeners.forEach(listener -> listener.propertyChange(changeEvent));
        this.lastSnippetEventEvaluation = evaluation;
    }

    public Evaluation getLastSnippetEventEvaluation() {
        return lastSnippetEventEvaluation;
    }

    public Map<String, Map<DatedSnippetEventWrapper, Evaluation>> getExecutions() {
        return executions;
    }

    public String getScriptEvaluationResult(String scriptName) {
        Map<DatedSnippetEventWrapper, Evaluation> scriptEvaluation = getScriptEvaluation(scriptName);
        if (scriptEvaluation != null) {
            StringBuilder result = new StringBuilder();
            scriptEvaluation.keySet().stream()
                    .sorted()
                    .filter(wrapper -> {
                        // Filter away items that aren't printed in shipped JShell
                        String subKindName = wrapper.getSnippetEvent().snippet().subKind().name();
                        return !subKindName.equals("VAR_DECLARATION_WITH_INITIALIZER_SUBKIND")
                                && !subKindName.equals("TEMP_VAR_EXPRESSION_SUBKIND");
                    })
                    .forEach(snippetEvent -> {
                        if (snippetEvent != null && scriptEvaluation.get(snippetEvent).equals(Evaluation.SUCCESS)) {
                            String value = snippetEvent.getSnippetEvent().value();
                            if (StringUtils.isNotEmpty(value)) {
                                result.append(value);
                                if (StringUtils.isNotBlank(value)) {
                                    result.append("\n");
                                }
                            }
                        }
                    });

            return result.toString();
        }

        return null;
    }

    public Map<DatedSnippetEventWrapper, Evaluation> getScriptEvaluation(String scriptName) {
        return executions.get(scriptName);
    }
}