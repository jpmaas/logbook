package org.zalando.logbook.servlet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.zalando.logbook.DefaultHttpLogFormatter;
import org.zalando.logbook.DefaultSink;
import org.zalando.logbook.HttpLogFormatter;
import org.zalando.logbook.HttpLogWriter;
import org.zalando.logbook.HttpMessage;
import org.zalando.logbook.HttpRequest;
import org.zalando.logbook.HttpResponse;
import org.zalando.logbook.Logbook;

import javax.servlet.DispatcherType;
import java.io.IOException;

import static java.util.Collections.emptyMap;
import static javax.servlet.RequestDispatcher.ERROR_EXCEPTION_TYPE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hobsoft.hamcrest.compose.ComposeMatchers.hasFeature;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Verifies that {@link LogbookFilter} handles {@link DispatcherType#ERROR} correctly.
 */
public final class ErrorDispatchTest {

    private final HttpLogFormatter formatter = spy(new ForwardingHttpLogFormatter(new DefaultHttpLogFormatter()));
    private final HttpLogWriter writer = mock(HttpLogWriter.class);

    private final MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new ExampleController())
            .addFilter(new LogbookFilter(Logbook.builder()
                    .sink(new DefaultSink(formatter, writer))
                    .build()))
            .build();

    @BeforeEach
    public void setUp() {
        reset(formatter, writer);

        when(writer.isActive()).thenReturn(true);
    }

    @Test
    void shouldFormatErrorResponse() throws Exception {
        mvc.perform(get("/api/not-found")
                .content("Hello")
                .requestAttr(ERROR_EXCEPTION_TYPE, "java.lang.Exception"));

        final HttpRequest request = interceptRequest();
        final HttpResponse response = interceptResponse();

        assertThat(request, hasFeature(this::getBodyAsString, is("<skipped>")));
        assertThat(response, hasFeature("status", HttpResponse::getStatus, is(404)));
        assertThat(response, hasFeature("headers", HttpResponse::getHeaders, is(emptyMap())));
    }

    private String getBodyAsString(final HttpMessage message) {
        try {
            return message.getBodyAsString();
        } catch (final IOException e) {
            throw new AssertionError(e);
        }
    }

    private HttpRequest interceptRequest() throws IOException {
        final ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(formatter).format(any(), captor.capture());
        return captor.getValue();
    }

    private HttpResponse interceptResponse() throws IOException {
        final ArgumentCaptor<HttpResponse> captor = ArgumentCaptor.forClass(HttpResponse.class);
        verify(formatter).format(any(), captor.capture());
        return captor.getValue();
    }

}
