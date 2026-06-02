/*
 * Copyright (c) 2010, 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.http;

import java.util.Locale;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.http.util.DataChunk;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.glassfish.grizzly.http.util.MimeHeaders;

/**
 * The {@link HttpHeader} object, which represents HTTP response message.
 *
 * @see HttpHeader
 * @see HttpRequestPacket
 *
 * @author Alexey Stashok
 */
public abstract class HttpResponsePacket extends HttpHeader {

    // ----------------------------------------------------- Instance Variables

    /**
     * The request that triggered this response.
     */
    private HttpRequestPacket request;

    /**
     * The {@link Locale} of the entity body being sent by this response.
     */
    private Locale locale;

    /**
     * The value of the <code>Content-Language</code> response header.
     */
    private String contentLanguage;

    /**
     * Status code.
     */
    protected HttpStatus httpStatus;

    /**
     * Status message.
     */
    private final DataChunk reasonPhraseC = DataChunk.newInstance();

    /**
     * Status of the informational (1xx) interim response (e.g. {@link HttpStatus#EARLY_HINTS_103} or
     * {@link HttpStatus#CONINTUE_100}) that is pending serialization. Held separately from {@link #httpStatus} so that
     * serializing an interim response does not disturb the status of the final response. Non-{@code null} only between
     * the moment an interim response is requested and the moment it has been serialized.
     */
    private HttpStatus interimStatus;

    /**
     * Do we allow custom reason phrase.
     */
    private boolean allowCustomReasonPhrase = true;

    /**
     * Do we need to encode HTML sensitive characters in custom reason phrase.
     */
    private boolean isHtmlEncodingCustomReasonPhrase = true;

    /**
     * Returns {@link HttpResponsePacket} builder.
     *
     * @return {@link Builder}.
     */
    public static Builder builder(final HttpRequestPacket request) {
        return new Builder().requestPacket(request);
    }

    // ----------------------------------------------------------- Constructors
    protected HttpResponsePacket() {
    }

    // -------------------- State --------------------

    /**
     * Gets the status code for this response.
     *
     * @return the status code for this response.
     */
    public int getStatus() {
        return getHttpStatus().getStatusCode();
    }

    /**
     * Gets the HTTP status for this response.
     *
     * @return the HTTP status for this response.
     */
    public HttpStatus getHttpStatus() {
        if (httpStatus == null) {
            httpStatus = HttpStatus.OK_200;
        }

        return httpStatus;
    }

    /**
     * Sets the status code for this response.
     *
     * @param status the status code for this response.
     */
    public void setStatus(final int status) {
        // the order is important here as statusDC.setXXX will reset the parsedIntStatus
        httpStatus = HttpStatus.getHttpStatus(status);
    }

    /**
     * Sets the status code for this response.
     *
     * @param status the status for this response.
     */
    public void setStatus(final HttpStatus status) {
        this.httpStatus = status;
        this.reasonPhraseC.recycle();
    }

    /**
     * Returns <code>true</code> if custom status reason phrases are allowed for this response, or <code>false</tt>
     * otherwise.
     *
     * @return <code>true</code> if custom status reason phrases are allowed for this response, or <code>false</tt>
     * otherwise.
     */
    public final boolean isAllowCustomReasonPhrase() {
        return allowCustomReasonPhrase;
    }

    /**
     * Sets if the custom status reason phrases are allowed for this response.
     *
     * @param allowCustomReasonPhrase <code>true</code> if custom status reason phrases are allowed for this response, or
     * <code>false</tt> otherwise.
     */
    public final void setAllowCustomReasonPhrase(final boolean allowCustomReasonPhrase) {
        this.allowCustomReasonPhrase = allowCustomReasonPhrase;
    }

    /**
     * Returns if we need to encode HTML sensitive characters in custom reason phrase.
     */
    public boolean isHtmlEncodingCustomReasonPhrase() {
        return isHtmlEncodingCustomReasonPhrase;
    }

    /**
     * Sets if we need to encode HTML sensitive characters in custom reason phrase.
     */
    public void setHtmlEncodingCustomReasonPhrase(boolean isHtmlEncodingCustomReasonPhrase) {
        this.isHtmlEncodingCustomReasonPhrase = isHtmlEncodingCustomReasonPhrase;
    }

    /**
     * Gets the custom status reason phrase for this response as {@link DataChunk} (avoid creation of a String object}.
     *
     * @return the status reason phrase for this response as {@link DataChunk} (avoid creation of a String object}.
     */
    public final DataChunk getReasonPhraseRawDC() {
        return reasonPhraseC;
    }

    /**
     * Gets the status reason phrase for this response as {@link DataChunk} (avoid creation of a String object}. This
     * implementation takes into consideration the {@link #isAllowCustomReasonPhrase()} property - if the custom reason
     * phrase is allowed and it's value is not null - then the returned result will be equal to
     * {@link #getReasonPhraseRawDC()}, otherwise if custom reason phrase is disallowed or its value is null - the default
     * reason phrase for the HTTP response {@link #getStatus()} will be returned.
     *
     * @return the status reason phrase for this response as {@link DataChunk} (avoid creation of a String object}.
     */
    public final DataChunk getReasonPhraseDC() {
        if (isCustomReasonPhraseSet()) {
            return reasonPhraseC;
        } else {
            reasonPhraseC.setBytes(httpStatus.getReasonPhraseBytes());
            return reasonPhraseC;
        }
    }

    /**
     * Gets the status reason phrase for this response.
     *
     * @return the status reason phrase for this response.
     */
    public final String getReasonPhrase() {
        return getReasonPhraseDC().toString();
    }

    /**
     * Sets the status reason phrase for this response.
     *
     * @param message the status reason phrase for this response.
     */
    public void setReasonPhrase(final String message) {
        reasonPhraseC.setString(message);
    }

    public void setReasonPhrase(final Buffer reason) {
        reasonPhraseC.setBuffer(reason, reason.position(), reason.limit());
    }

    public final boolean isCustomReasonPhraseSet() {
        return allowCustomReasonPhrase && !reasonPhraseC.isNull();
    }

    /**
     * @return the request that triggered this response
     */
    public HttpRequestPacket getRequest() {
        return request;
    }

    /**
     * @return <code>true</code> if an informational (1xx) interim response is pending serialization for this packet.
     */
    public boolean isInterimResponse() {
        return interimStatus != null;
    }

    /**
     * @return the status of the interim response pending serialization, or <code>null</code> if none is pending.
     */
    public HttpStatus getInterimStatus() {
        return interimStatus;
    }

    /**
     * Request the serialization of an informational (1xx) interim response, such as
     * {@link HttpStatus#EARLY_HINTS_103}, using the headers currently set on this packet. This does not affect the
     * status, headers or committed state of the final response.
     *
     * @param interimStatus the 1xx status of the interim response to serialize.
     */
    public void setInterimStatus(final HttpStatus interimStatus) {
        this.interimStatus = interimStatus;
    }

    /**
     * Mark the pending interim response as having been serialized. If the interim status was {@code 100 Continue}, the
     * associated request's expectation flag is also cleared so that subsequent processing no longer treats the request
     * as awaiting acknowledgement.
     */
    public void interimResponseSent() {
        final boolean wasContinue = interimStatus != null && interimStatus.getStatusCode() == HttpStatus.CONINTUE_100.getStatusCode();
        interimStatus = null;
        if (wasContinue) {
            request.requiresAcknowledgement(false);
        }
    }

    // --------------------

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        httpStatus = null;
        interimStatus = null;
        allowCustomReasonPhrase = true;
        isHtmlEncodingCustomReasonPhrase = true;
        reasonPhraseC.recycle();
        locale = null;
        contentLanguage = null;
        request = null;

        super.reset();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean isRequest() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(256);
        sb.append("HttpResponsePacket (\n  status=").append(getStatus()).append("\n  reason=").append(getReasonPhrase()).append("\n  protocol=")
                .append(getProtocol().getProtocolString()).append("\n  content-length=").append(getContentLength()).append("\n  committed=")
                .append(isCommitted()).append("\n  headers=[");
        final MimeHeaders headersLocal = getHeaders();
        for (final String name : headersLocal.names()) {
            for (String value : headersLocal.values(name)) {
                sb.append("\n      ").append(name).append('=').append(value);
            }
        }
        sb.append("]\n)");

        return sb.toString();
    }

    /**
     * @return the {@link Locale} of this response.
     */
    public Locale getLocale() {
        return locale;
    }

    /**
     * Called explicitly by user to set the Content-Language and the default encoding
     */
    public void setLocale(final Locale locale) {

        if (locale == null) {
            return; // throw an exception?
        }

        // Save the locale for use by getLocale()
        this.locale = locale;

        // Set the contentLanguage for header output
        contentLanguage = locale.getLanguage();
        if (contentLanguage != null && contentLanguage.length() > 0) {
            final String country = locale.getCountry();
            if (country != null && country.length() > 0) {
                final StringBuilder value = new StringBuilder(contentLanguage);
                value.append('-');
                value.append(country);
                contentLanguage = value.toString();
            }
        }

    }

    /**
     * @return the value that will be used by the <code>Content-Language</code> response header
     */
    public String getContentLanguage() {
        return contentLanguage;
    }

    /**
     * Set the value that will be used by the <code>Content-Language</code> response header.
     */
    public void setContentLanguage(final String contentLanguage) {
        this.contentLanguage = contentLanguage;
    }

    @Override
    public void setContentLengthLong(long contentLength) {
        setChunked(contentLength < 0);
        super.setContentLengthLong(contentLength);
    }

    // ------------------------------------------------- Package Private Methods

    /**
     * Associates the request that triggered this response.
     * 
     * @param request the request that triggered this response
     */
    public void setRequest(final HttpRequestPacket request) {
        this.request = request;
    }

    // ---------------------------------------------------------- Nested Classes

    /**
     * <tt>HttpResponsePacket</tt> message builder.
     */
    public static class Builder extends HttpHeader.Builder<Builder> {

        protected Integer status;
        protected String reasonPhrase;
        protected HttpRequestPacket requestPacket;

        /**
         * Sets the status code for this response.
         *
         * @param status the status code for this response.
         */
        public Builder status(final int status) {
            this.status = status;
            return this;
        }

        /**
         * Sets the status reason phrase for this response.
         *
         * @param reasonPhrase the status reason phrase for this response.
         */
        public Builder reasonPhrase(final String reasonPhrase) {
            this.reasonPhrase = reasonPhrase;
            return this;
        }

        public Builder requestPacket(final HttpRequestPacket requestPacket) {
            this.requestPacket = requestPacket;
            return this;
        }

        /**
         * Build the <tt>HttpResponsePacket</tt> message.
         *
         * @return <tt>HttpResponsePacket</tt>
         */
        @Override
        public final HttpResponsePacket build() {
            HttpResponsePacket responsePacket = (HttpResponsePacket) super.build();
            if (status != null) {
                responsePacket.setStatus(status);
            }
            if (reasonPhrase != null) {
                responsePacket.setReasonPhrase(reasonPhrase);
            }
            return responsePacket;
        }

        @Override
        public void reset() {
            super.reset();
            status = null;
            reasonPhrase = null;
        }

        @Override
        protected HttpHeader create() {
            if (requestPacket == null) {
                throw new IllegalStateException("Unable to create new HttpResponsePacket.  No HttpRequestPacket available.");
            }
            HttpResponsePacket responsePacket = requestPacket.getResponse();
            if (responsePacket == null) {
                responsePacket = HttpResponsePacketImpl.create();
                responsePacket.setRequest(requestPacket);
                responsePacket.setSecure(requestPacket.isSecure());
            }
            return responsePacket;
        }
    }
}
