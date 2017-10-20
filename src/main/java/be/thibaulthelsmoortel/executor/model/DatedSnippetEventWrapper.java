package be.thibaulthelsmoortel.executor.model;

import java.time.LocalDateTime;
import jdk.jshell.SnippetEvent;

/**
 * Wrapper class for adding a date and time to a {@link jdk.jshell.SnippetEvent}.
 *
 * @author Thibault Helsmoortel
 */
public class DatedSnippetEventWrapper implements Comparable {

    private LocalDateTime localDateTime;
    private SnippetEvent snippetEvent;

    public DatedSnippetEventWrapper(SnippetEvent snippetEvent) {
        this(LocalDateTime.now(), snippetEvent);
    }

    public DatedSnippetEventWrapper(LocalDateTime localDateTime, SnippetEvent snippetEvent) {
        this.localDateTime = localDateTime;
        this.snippetEvent = snippetEvent;
    }

    public LocalDateTime getLocalDateTime() {
        return localDateTime;
    }

    public SnippetEvent getSnippetEvent() {
        return snippetEvent;
    }

    @Override
    public int compareTo(Object o) {
        return localDateTime.compareTo(((DatedSnippetEventWrapper) o).getLocalDateTime());
    }
}
