/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Yegor Bugayenko
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.takes.http;

import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.jcabi.http.mock.MkAnswer;
import com.jcabi.http.mock.MkContainer;
import com.jcabi.http.mock.MkGrizzlyContainer;
import com.jcabi.http.request.JdkRequest;
import com.jcabi.http.response.RestResponse;
import com.jcabi.matchers.RegexMatchers;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URI;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.takes.Request;
import org.takes.Response;
import org.takes.Take;
import org.takes.facets.fork.FkRegex;
import org.takes.facets.fork.TkFork;
import org.takes.rq.RqWithHeaders;
import org.takes.rs.RsEmpty;
import org.takes.tk.TkText;

/**
 * Test case for {@link BkBasic}.
 *
 * @author Dmitry Zaytsev (dmitry.zaytsev@gmail.com)
 * @version $Id$
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @checkstyle MultipleStringLiteralsCheck (500 lines)
 * @todo #306:30min At the moment we don't support HTTP
 * persistent connections. Would be great to implement
 * this feature. BkBasic.accept should handle more
 * than one HTTP request in one connection.
 * @since 0.15.2
 */
@SuppressWarnings("PMD.ExcessiveImports")
public final class BkBasicTest {
    /**
     * Carriage return constant.
     */
    private static final String CRLF = "\r\n";

    /**
     * POST header constant.
     */
    private static final String POST = "POST / HTTP/1.1";

    /**
     * Host header constant.
     */
    private static final String HOST = "Host:localhost";

    /**
     * BkBasic can handle socket data.
     *
     * @throws IOException If some problem inside
     */
    @Test
    public void handlesSocket() throws IOException {
        final Socket socket = createMockSocket();
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Mockito.when(socket.getOutputStream()).thenReturn(baos);
        new BkBasic(new TkText("Hello world!")).accept(socket);
        MatcherAssert.assertThat(
            baos.toString(),
            Matchers.containsString("Hello world")
        );
    }

    /**
     * BkBasic can return HTTP status 404 when accessing invalid URL.
     *
     * @throws IOException if any I/O error occurs.
     */
    @Test
    public void returnsProperResponseCodeOnInvalidUrl() throws IOException {
        new FtRemote(
            new TkFork(
                new FkRegex("/path/a", new TkText("a")),
                new FkRegex("/path/b", new TkText("b"))
            )
        ).exec(
            new FtRemote.Script() {
                @Override
                public void exec(final URI home) throws IOException {
                    new JdkRequest(String.format("%s/path/c", home))
                        .fetch()
                        .as(RestResponse.class)
                        .assertStatus(HttpURLConnection.HTTP_NOT_FOUND);
                }
            }
        );
    }
    /**
     * BkBasic produces headers with addresses without slashes.
     *
     * @throws IOException If some problem inside
     */
    @Test
    public void addressesInHeadersAddedWithoutSlashes() throws IOException {
        final Socket socket = BkBasicTest.createMockSocket();
        final AtomicReference<Request> ref = new AtomicReference<Request>();
        new BkBasic(
            new Take() {
                @Override
                public Response act(final Request req) {
                    ref.set(req);
                    return new RsEmpty();
                }
            }
        ).accept(socket);
        final Request request = ref.get();
        MatcherAssert.assertThat(
            request,
            Matchers.is(
                Matchers.notNullValue()
            )
        );
        MatcherAssert.assertThat(
            request,
            Matchers.is(
                Matchers.instanceOf(
                    RqWithHeaders.class
                )
            )
        );
        final Collection<String> head = Lists.newArrayList(request.head());
        MatcherAssert.assertThat(head, Matchers.not(Matchers.<String>empty()));
        final Collection<String> found = Collections2.filter(
            head,
            new Predicate<String>() {
                @Override
                public boolean apply(final String header) {
                    return header.contains("X-Takes")
                        && header.contains("Address");
                }
            }
        );
        for (final String header : found) {
            MatcherAssert.assertThat(
                header,
                Matchers.not(
                    Matchers.containsString("/")
                )
            );
        }
    }

    /**
     * BkBasic can handle two requests in one connection.
     *
     * @throws Exception If some problem inside
     */
    @Ignore
    @Test
    public void handlesTwoRequestInOneConnection() throws Exception {
        final String text = "Hello world!";
        final MkContainer container = new MkGrizzlyContainer().next(
            new MkAnswer.Simple(
                Joiner.on(BkBasicTest.CRLF).join(
                    POST,
                    HOST,
                    "Content-Length: 4",
                    "",
                    "hi",
                    "POST / HTTP/1.1",
                    "Host: localhost",
                    "Content-Length: 4",
                    "",
                    "hi"
                )
            )
        ).start();
        final URI uri = container.home();
        final Socket socket = new Socket(uri.getHost(), uri.getPort());
        new BkBasic(new TkText(text)).accept(socket);
        new BkBasic(new TkText(text)).accept(socket);
        container.stop();
        MatcherAssert.assertThat(
            socket.getOutputStream().toString(),
            RegexMatchers.containsPattern(text + ".*?" + text)
        );
    }

    /**
     * BkBasic can return HTTP status 411 when a persistent connection request
     * has no Content-Length.
     *
     * @throws Exception If some problem inside
     */
    @Ignore
    @Test
    public void returnsProperResponseCodeOnNoContentLength() throws Exception {
        final MkContainer container = new MkGrizzlyContainer().next(
            new MkAnswer.Simple(
                Joiner.on(BkBasicTest.CRLF).join(
                    POST,
                    HOST,
                    "",
                    "hi"
                )
            )
        ).start();
        final URI uri = container.home();
        final Socket socket = new Socket(uri.getHost(), uri.getPort());
        new BkBasic(new TkText("411 Test")).accept(socket);
        container.stop();
        MatcherAssert.assertThat(
            socket.getOutputStream().toString(),
            Matchers.containsString("HTTP/1.1 411 Length Required")
        );
    }

    /**
     * BkBasic can accept no content-length on closed connection.
     *
     * @throws Exception If some problem inside
     */
    @Ignore
    @Test
    public void acceptsNoContentLengthOnClosedConnection() throws Exception {
        final String text = "Close Test";
        final MkContainer container = new MkGrizzlyContainer().next(
            new MkAnswer.Simple(
                Joiner.on(BkBasicTest.CRLF).join(
                    POST,
                    HOST,
                    "Connection: Close",
                    "",
                    "hi"
                )
            )
        ).start();
        final URI uri = container.home();
        final Socket socket = new Socket(uri.getHost(), uri.getPort());
        new BkBasic(new TkText(text)).accept(socket);
        container.stop();
        MatcherAssert.assertThat(
            socket.getOutputStream().toString(),
            Matchers.containsString(text)
        );
    }

    /**
     * Creates Socket mock for reuse.
     *
     * @return Prepared Socket mock
     * @throws IOException If some problem inside
     */
    private static Socket createMockSocket() throws IOException {
        final Socket socket = Mockito.mock(Socket.class);
        Mockito.when(socket.getInputStream()).thenReturn(
            new ByteArrayInputStream(
                Joiner.on(BkBasicTest.CRLF).join(
                    "GET / HTTP/1.1",
                    "Host:localhost",
                    "Content-Length: 2",
                    "",
                    "hi"
                ).getBytes()
            )
        );
        Mockito.when(socket.getLocalAddress()).thenReturn(
            InetAddress.getLocalHost()
        );
        Mockito.when(socket.getLocalPort()).thenReturn(0);
        Mockito.when(socket.getInetAddress()).thenReturn(
            InetAddress.getLocalHost()
        );
        Mockito.when(socket.getPort()).thenReturn(0);
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Mockito.when(socket.getOutputStream()).thenReturn(baos);
        return socket;
    }
}
