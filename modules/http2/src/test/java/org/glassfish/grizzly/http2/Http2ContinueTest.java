/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package org.glassfish.grizzly.http2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.SocketConnectorHandler;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.Filter;
import org.glassfish.grizzly.filterchain.FilterChain;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.http.HttpContent;
import org.glassfish.grizzly.http.HttpRequestPacket;
import org.glassfish.grizzly.http.HttpResponsePacket;
import org.glassfish.grizzly.http.Method;
import org.glassfish.grizzly.http.Protocol;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.memory.Buffers;
import org.glassfish.grizzly.nio.transport.TCPNIOConnectorHandler;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.junit.After;
import org.junit.Test;

/**
 * Tests <code>100 Continue</code> over HTTP/2. Prior to the {@code interimStatus} unification, the
 * {@code Expect: 100-continue} mechanism was silently dropped for non-HTTP/1.1 protocols at the codec layer; this test
 * exercises the path that now routes the acknowledgement through {@link Response#sendAcknowledgement()} and the HTTP/2
 * output sink.
 */
public class Http2ContinueTest extends AbstractHttp2Test {

    private static final int PORT = 18905;
    private static final int CONTINUE = 100;
    private static final int OK = 200;

    private HttpServer httpServer;

    @After
    public void tearDown() {
        if (httpServer != null) {
            httpServer.shutdownNow();
        }
    }

    @Test
    public void testContinueThenFinalResponse() throws Exception {
        startServer(new HttpHandler() {
            @Override
            public void service(final Request request, final Response response) throws Exception {
                // doHandle has already auto-sent 100-Continue at this point because the request carried
                // "Expect: 100-continue".
                response.setContentType("text/plain");
                response.getWriter().write("done");
            }
        });

        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Throwable> error = new AtomicReference<>();
        final AtomicInteger interimCount = new AtomicInteger();
        final AtomicInteger finalStatus = new AtomicInteger(-1);
        final StringBuilder body = new StringBuilder();

        final Connection<?> c = connect(new ResponseCapturingFilter(latch, error, interimCount, finalStatus, body));
        c.write(postWithExpect("/path"));

        assertTrue("timed out waiting for the final response", latch.await(10, TimeUnit.SECONDS));
        rethrow(error);

        assertEquals("expected exactly one 100 Continue interim response", 1, interimCount.get());
        assertEquals(OK, finalStatus.get());
        assertEquals("done", body.toString());
    }

    // -------------------------------------------------------- Private Methods

    private void startServer(final HttpHandler handler) throws Exception {
        httpServer = createServer(null, PORT, false, true);
        httpServer.getListener("grizzly").getKeepAlive().setIdleTimeoutInSeconds(-1);
        httpServer.getServerConfiguration().addHttpHandler(handler, "/path");
        httpServer.start();
    }

    private Connection<?> connect(final Filter clientFilter) throws Exception {
        final FilterChain clientChain = createClientFilterChainAsBuilder(false, true, clientFilter).build();
        final TCPNIOTransport transport = httpServer.getListener("grizzly").getTransport();
        final SocketConnectorHandler connectorHandler = TCPNIOConnectorHandler.builder(transport).processor(clientChain).build();
        final Future<Connection> connectFuture = connectorHandler.connect("localhost", PORT);
        return connectFuture.get(10, TimeUnit.SECONDS);
    }

    private static HttpContent postWithExpect(final String uri) {
        final HttpRequestPacket request = HttpRequestPacket.builder().method(Method.POST).uri(uri)
                .protocol(Protocol.HTTP_2_0).host("localhost:" + PORT).header("Expect", "100-continue")
                .contentLength(0).build();
        return HttpContent.builder(request).content(Buffers.EMPTY_BUFFER).last(true).build();
    }

    private static void rethrow(final AtomicReference<Throwable> error) {
        final Throwable t = error.get();
        if (t != null) {
            t.printStackTrace();
            fail(String.valueOf(t));
        }
    }

    private static final class ResponseCapturingFilter extends BaseFilter {
        private final CountDownLatch latch;
        private final AtomicReference<Throwable> error;
        private final AtomicInteger interimCount;
        private final AtomicInteger finalStatus;
        private final StringBuilder body;

        private ResponseCapturingFilter(final CountDownLatch latch, final AtomicReference<Throwable> error,
                final AtomicInteger interimCount, final AtomicInteger finalStatus, final StringBuilder body) {
            this.latch = latch;
            this.error = error;
            this.interimCount = interimCount;
            this.finalStatus = finalStatus;
            this.body = body;
        }

        @Override
        public NextAction handleRead(final FilterChainContext ctx) throws IOException {
            final HttpContent httpContent = ctx.getMessage();
            try {
                final HttpResponsePacket response = (HttpResponsePacket) httpContent.getHttpHeader();
                if (response.getStatus() == CONTINUE) {
                    interimCount.incrementAndGet();
                } else {
                    finalStatus.set(response.getStatus());
                }
                if (httpContent.getContent().hasRemaining()) {
                    body.append(httpContent.getContent().toStringContent());
                }
                if (httpContent.isLast()) {
                    latch.countDown();
                }
            } catch (final Throwable t) {
                error.set(t);
                latch.countDown();
            }

            return ctx.getStopAction();
        }
    }
}
