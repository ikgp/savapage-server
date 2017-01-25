/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2017 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.server.raw;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.savapage.core.PerformanceLogger;
import org.savapage.core.SpException;
import org.savapage.core.SpInfo;
import org.savapage.core.cometd.AdminPublisher;
import org.savapage.core.cometd.PubLevelEnum;
import org.savapage.core.cometd.PubTopicEnum;
import org.savapage.core.concurrent.ReadWriteLockEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.dao.IppQueueDao;
import org.savapage.core.dao.enums.DocLogProtocolEnum;
import org.savapage.core.dao.enums.ReservedIppQueueEnum;
import org.savapage.core.doc.DocContent;
import org.savapage.core.doc.DocContentTypeEnum;
import org.savapage.core.jpa.IppQueue;
import org.savapage.core.print.server.DocContentPrintProcessor;
import org.savapage.core.services.QueueService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.ServiceEntryPoint;
import org.savapage.core.users.AbstractUserSource;
import org.savapage.server.WebApp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Raw IP Printer on default port 9100. The port can be set in the
 * server.properties file.
 *
 * <p>
 * This server is specially created for Windows Print Server, so it can be part
 * of an Active Directory Domain or a Workgroup, and shared among clients.
 * </p>
 * <p>
 * A test in a Windows Workgroup environment shows that the originating user is
 * indeed transferred via the shared printer to the PostScript header.
 * </p>
 */
public final class RawPrintServer extends Thread implements ServiceEntryPoint {

    /**
     * .
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(RawPrintServer.class);

    /**
     * The default port number.
     */
    private static final int DEFAULT_PORT = 9100;

    /**
     * SO_TIMEOUT with the specified timeout.
     */
    private static final int SERVER_SOCKET_SO_TIMEOUT_MSEC = 2000;

    /**
     * Polling period in milliseconds used to monitor active IP Print requests
     * to finish.
     */
    private static final int POLL_FOR_ACTIVE_REQUESTS_MSEC = 1000;

    /**
     * .
     */
    private static final QueueService QUEUE_SERVICE =
            ServiceContext.getServiceFactory().getQueueService();

    /**
     * .
     */
    private boolean keepAcceptingRequests = true;

    /**
     * The total number of print job requests.
     */
    private final AtomicInteger totalRequests = new AtomicInteger(0);

    /**
     * The total number of active print job requests.
     */
    private final AtomicInteger activeRequests = new AtomicInteger(0);

    /**
     * The port initialized with default value.
     */
    private int port = DEFAULT_PORT;

    /**
     * The byte in the {@link InputStream} marking the end of the print job.
     */
    private static final int BYTE_END_OF_PRINTJOB = 4;

    /**
     * CR (carriage return) byte.
     */
    private static final int BYTE_CR = 13;

    /**
     * LF (NL line feed, new line) byte.
     */
    private static final int BYTE_LF = 10;

    /**
     * The Universal Exit Language (UEL) Command causes the printer to exit the
     * active printer language. The printer then returns control to PJL. The UEL
     * command is used at the beginning and end of every PJL job. The syntax is
     * : {@code <ESC>%-12345X}
     * <p>
     * NOTE: This constant is the part <b>after</b> the first {@code <ESC>}
     * character.
     * </p>
     * See <a href="https://en.wikipedia.org/wiki/Printer_Job_Language">
     * Printer_Job_Language</a> in Wikipedia.
     */
    private static final String UEL_SIGNATURE_MINUS_FIRST_ESC = "%-12345X";

    /**
     * The PJL command prefix (@PJL)
     */
    private static final String PJL_COMMAND_PFX = "@PJL";

    /**
     * The initial {@link StringBuilder} capacity for reading a line.
     */
    private static final int STRINGBUILDER_LINE_CAPACITY = 128;

    /**
     *
     */
    class SocketServerThread extends Thread {

        /**
         * Waiting max. 5 seconds while reading the socket for meaningful data.
         */
        private static final int READ_TIMEOUT_MSEC = 5000;

        /**
         * The {@link Socket} with the IP Print job input.
         */
        private final Socket mySocket;

        /**
         * The parent {@link RawPrintServer}.
         */
        private final RawPrintServer myServer;

        /**
         *
         * @param socket
         *            The {@link Socket} with the IP Print job input.
         * @param server
         *            The parent {@link RawPrintServer}.
         */
        public SocketServerThread(final Socket socket,
                final RawPrintServer server) {

            super("SocketServerThread");

            mySocket = socket;
            myServer = server;
        }

        @Override
        public void run() {

            myServer.onRequestStart(mySocket);

            try {

                mySocket.setSoTimeout(READ_TIMEOUT_MSEC);

                // ----------------------------------------------
                // Wait for conversation request from client
                // ----------------------------------------------
                myServer.readAndPrint(mySocket);

            } catch (Exception ex) {

                if (!ConfigManager.isShutdownInProgress()) {

                    final String msg;

                    if (ex instanceof RawPrintException) {

                        msg = ex.getMessage();
                        LOGGER.warn(msg);

                    } else {

                        final String hostAddress;

                        if (mySocket == null) {
                            hostAddress = "?";
                        } else {
                            hostAddress =
                                    mySocket.getInetAddress().getHostAddress();
                        }

                        msg = String.format("%s: %s (IP Print from %s)",
                                ex.getClass().getSimpleName(), ex.getMessage(),
                                hostAddress);

                        LOGGER.error(msg, ex);
                    }

                    AdminPublisher.instance().publish(PubTopicEnum.USER,
                            PubLevelEnum.ERROR, msg);
                }
            } finally {
                // All data exchanged: close socket
                IOUtils.closeQuietly(mySocket);
            }

            myServer.onRequestFinish();
        }
    }

    /**
     *
     * @param iPort
     *            The port to listen to.
     */
    public RawPrintServer(final int iPort) {
        this.port = iPort;
    }

    /**
     * Reads a line (delimited with CR, LF or {@link #BYTE_END_OF_PRINTJOB}.
     *
     * @param istr
     *            {@link InputStream} to read the line from.
     * @param ostr
     *            {@link OutputStream} to write the resulting line to.
     * @return {@code null} when nothing to read.
     * @throws IOException
     *             When read or write error.
     */
    private static String readLine(final InputStream istr,
            final OutputStream ostr) throws IOException {

        StringBuilder line = null;

        int iByte = istr.read();

        while ((-1 < iByte)) {

            ostr.write(iByte);

            if (line == null) {
                line = new StringBuilder(STRINGBUILDER_LINE_CAPACITY);
            }

            if ((iByte == BYTE_LF) || (iByte == BYTE_CR)
                    || (BYTE_END_OF_PRINTJOB == iByte)) {
                break;
            }

            line.append((char) iByte);

            iByte = istr.read();
        }

        if (line == null) {
            return null;
        }
        return line.toString();
    }

    /**
     * Skips any PJL command lines.
     *
     * @param istr
     *            {@link InputStream} to read the line from.
     * @param ostr
     *            {@link OutputStream} to write the resulting line to.
     * @return The first non-PJL command header line, or {@code null} when
     *         nothing to read.
     * @throws IOException
     *             When read or write error.
     */
    private static String skipPJLCommandLines(final InputStream istr,
            final OutputStream ostr) throws IOException {

        String line = readLine(istr, ostr);
        while (line != null && line.startsWith(PJL_COMMAND_PFX)) {
            line = readLine(istr, ostr);
        }
        return line;
    }

    /**
     * Reads and prints data from the socket.
     * <p>
     * Note: the number of copies can NOT be retrieved from the PostScript
     * header. See Mantis #492.
     * </p>
     *
     * @param socket
     *            The socket to read the print job data from.
     * @throws IOException
     *             When connectivity failure.
     * @throws RawPrintException
     *             When no content received (within time), or content is not
     *             PostScript.
     */
    private void readAndPrint(final Socket socket)
            throws IOException, RawPrintException {

        final Date perfStartTime = PerformanceLogger.startTime();

        // --------------------------------------------------------------------
        // Print from Windows Vista / 7
        // --------------------------------------------------------------------
        //
        // %!PS-Adobe-3.0
        // %%Title: Test Page
        // %%Creator: PScript5.dll Version 5.2.2
        // %%CreationDate: 11/17/2014 19:3:48
        // %%For: Rijk Ravestein
        // %%BoundingBox: (atend)
        // %%Pages: (atend)
        // %%Orientation: Portrait
        // %%PageOrder: Special
        // %%DocumentNeededResources: (atend)
        // %%DocumentSuppliedResources: (atend)
        // %%DocumentData: Clean7Bit
        // %%TargetDevice: (SavaPage) (3010.000) 0
        // %%LanguageLevel: 3
        // %%EndComments
        //
        // %%BeginDefaults
        // %%PageBoundingBox: 0 0 595 842
        // %%ViewingOrientation: 1 0 0 1
        // %%EndDefaults
        //
        // %%BeginProlog
        //

        // --------------------------------------------------------------------
        // Print from OS X Mavericks
        // --------------------------------------------------------------------
        //
        // %!PS-Adobe-3.0
        // %APL_DSC_Encoding: UTF8
        // %APLProducer: (Version 10.9.1 (Build 13B42) Quartz PS Context)
        // %%Title: (testprint)
        // %%Creator: (cgpdftops CUPS filter)
        // %%CreationDate: (Monday, January 06 2014 13:49:07 CET)
        // %%For: (rijk)
        //

        // --------------------------------------------------------------------
        // Possible extra header with PJL commands. First line is Universal Exit
        // Language (UEL) Command: the first '?' character represents 0x1B (ESC)
        // --------------------------------------------------------------------
        // ?%-12345X@PJL
        // @PJL JOB NAME = "Document 1" DISPLAY = "9729 john Document 1"
        // @PJL SET USERNAME = "john"
        // @PJL SET JOBATTR="@BANR=OFF"
        // @PJL ENTER LANGUAGE = POSTSCRIPT
        // %!PS-Adobe-3.0
        // %%HiResBoundingBox: 0 0 596.00 842.00
        // %%Creator: GPL Ghostscript 918 (ps2write)
        // %%LanguageLevel: 2
        // %%CreationDate: D:20170125182339+01'00'
        // %%For: (john)
        // %%Title: (Document 1)
        // %RBINumCopies: 1
        // %%Pages: (atend)
        // %%BoundingBox: (atend)
        // %%EndComments
        // %%BeginProlog
        //

        final InputStream istr = socket.getInputStream();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("read print job...");
        }

        String title = null;
        String userid = null;

        final String PFX_TITLE = "%%Title: ";
        final String PFX_USERID = "%%For: ";
        final String PFX_BEGIN_PROLOG = "%%BeginProlog";

        final String originatorIp = socket.getInetAddress().getHostAddress();

        final String authenticatedWebAppUser =
                WebApp.getAuthUserByIpAddr(originatorIp);

        /*
         * First line
         */
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();

        String strline = null;

        try {
            strline = readLine(istr, bos);
        } catch (SocketTimeoutException e) {
            throw new RawPrintException(String.format(
                    "No IP Print data received from "
                            + "[%s] within [%d] msec.",
                    originatorIp, SocketServerThread.READ_TIMEOUT_MSEC));
        }

        /*
         * Just a ping... when does this happen? See Mantis #529
         */
        if (strline == null) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("no data from " + originatorIp);
            }
            return;
        }

        /*
         * Mantis #779: Accept JetDirect PostScript stream with UEL header.
         */
        if (strline.startsWith(UEL_SIGNATURE_MINUS_FIRST_ESC, 1)) {
            strline = skipPJLCommandLines(istr, bos);
        }

        /*
         * Check for PostScript signature.
         */
        if (!strline.startsWith(DocContent.HEADER_PS)) {

            consumeWithoutProcessing(istr);

            throw new RawPrintException("IP Print data from [" + originatorIp
                    + "] is not PostScript. Header ["
                    + StringUtils.substring(strline, 0, 10) + "]");
        }

        final List<String> lines = new ArrayList<>();

        while (strline != null) {

            lines.add(strline);

            if (strline.startsWith(PFX_TITLE)) {
                title = stripParentheses(
                        StringUtils.removeStart(strline, PFX_TITLE));

            } else if (strline.startsWith(PFX_USERID)) {
                userid = stripParentheses(
                        StringUtils.removeStart(strline, PFX_USERID));

            } else if (strline.startsWith(PFX_BEGIN_PROLOG)) {
                break;
            }

            if (title != null && userid != null) {
                break;
            }

            strline = readLine(istr, bos);
        }

        if (title == null || userid == null) {

            consumeWithoutProcessing(istr);

            throw new IOException(
                    "IP Print job from [" + originatorIp + "] has no ["
                            + PFX_TITLE + "] and/or [" + PFX_USERID + "]");
        }

        // Mantis #503
        userid = AbstractUserSource.asDbUserId(userid,
                ConfigManager.isLdapUserSync());

        if (LOGGER.isTraceEnabled()) {

            final int MAX_LINES = 30;
            int i;
            for (i = 0; i < lines.size() && i < MAX_LINES; i++) {
                LOGGER.trace(lines.get(i));
            }
            if (i > MAX_LINES) {
                LOGGER.trace(
                        "... " + Integer.valueOf(i - MAX_LINES) + "more lines");
            }
        }

        ServiceContext.open();

        DocContentPrintProcessor processor = null;
        IppQueue queue = null;
        boolean isAuthorized = false;

        ReadWriteLockEnum.DATABASE_READONLY.setReadLock(true);

        /*
         * NOTE: There is NO top level database transaction. Specialized methods
         * have their own database transaction.
         */
        try {

            final IppQueueDao queueDao =
                    ServiceContext.getDaoContext().getIppQueueDao();

            queue = queueDao.find(ReservedIppQueueEnum.RAW_PRINT);

            /*
             * Allowed to print?
             */
            final String uri = "RAW:" + this.port;

            final boolean clientIpAllowed = QUEUE_SERVICE
                    .hasClientIpAccessToQueue(queue, uri, originatorIp);

            if (clientIpAllowed) {

                processor = new DocContentPrintProcessor(queue, originatorIp,
                        title, authenticatedWebAppUser);

                processor.setReadAheadInputBytes(bos.toByteArray());

                processor.processRequestingUser(userid);

                isAuthorized = clientIpAllowed && processor.isAuthorized();

                if (isAuthorized) {
                    processor.process(istr, DocLogProtocolEnum.RAW, null,
                            DocContentTypeEnum.PS, null);
                }
            }

        } catch (Exception e) {

            if (processor != null) {
                processor.setDeferredException(e);
            } else {
                LOGGER.error(e.getMessage(), e);
            }

        } finally {
            ReadWriteLockEnum.DATABASE_READONLY.setReadLock(false);
            consumeWithoutProcessing(istr);
            ServiceContext.close();
        }

        processor.evaluateErrorState(isAuthorized);

        PerformanceLogger.log(this.getClass(), "readAndPrint", perfStartTime,
                userid);
    }

    /**
     * Strips leading/trailing parenthesis from a string.
     *
     * @param content
     *            The string to strip.
     * @return The stripped string.
     */
    private String stripParentheses(final String content) {
        return StringUtils
                .removeEnd(StringUtils.removeStart(content.trim(), "("), ")");
    }

    /**
     * Consumes the (rest of) the full input stream, without processing it, so
     * the client is fooled that everything is OK, even when things went wrong.
     * If we would NOT do this, the client will try again-and-again, flooding
     * the server with requests.
     * <p>
     * Note: Any {@link IOException} is trace logged.
     * </p>
     *
     * @param istr
     *            The {@link InputStream} to consume.
     */
    private void consumeWithoutProcessing(final InputStream istr) {

        byte[] bytes = new byte[1024];

        try {
            while (istr.read(bytes) > -1) {
                continue;
            }
        } catch (IOException e) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(
                        "consumeWithoutProcessing error: " + e.getMessage());
            }
        }
    }

    @Override
    public void run() {

        Runtime.getRuntime().addShutdownHook(new RawPrintShutdownHook(this));

        final ServerSocket serverSocket;

        try {
            /*
             * From Javadoc: "The maximum queue length for incoming connection
             * indications (a request to connect) is set to <code>50</code>. If
             * a connection indication arrives when the queue is full, the
             * connection is refused."
             */
            serverSocket = new ServerSocket(this.port);

            /**
             * A call to accept() for this ServerSocket will block for only
             * SERVER_SOCKET_SO_TIMEOUT_MSEC amount of time. If the timeout
             * expires, a java.net.SocketTimeoutException is raised, though the
             * ServerSocket is still valid.
             */
            serverSocket.setSoTimeout(SERVER_SOCKET_SO_TIMEOUT_MSEC);

        } catch (IOException ex) {
            throw new SpException(ex);
        }

        SpInfo.instance().log(String
                .format("IP Print Server started on port %d.", this.port));

        while (this.keepAcceptingRequests) {

            try {

                final Socket socket = serverSocket.accept();
                final SocketServerThread st =
                        new SocketServerThread(socket, this);

                st.start();

            } catch (SocketTimeoutException ex) {
                continue;
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
                break;
            }
        }

        IOUtils.closeQuietly(serverSocket);
    }

    /**
     *
     * @return The port number.
     */
    public int getPort() {
        return port;
    }

    /**
     * Increments the number of (active) print job requests.
     *
     * @param client
     *            The client socket.
     */
    protected void onRequestStart(final Socket client) {

        this.totalRequests.incrementAndGet();
        this.activeRequests.incrementAndGet();

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("request #" + this.totalRequests.get() + " from ["
                    + client.getInetAddress().getHostAddress() + "]");
        }

    }

    /**
     * Decrements the number of active print job requests.
     */
    protected void onRequestFinish() {
        this.activeRequests.decrementAndGet();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("request #" + this.totalRequests.get() + " ended");
        }

    }

    /**
     * Closes the IP Print service.
     */
    public void shutdown() {

        this.keepAcceptingRequests = false;

        /*
         * Waiting for active requests to finish.
         */
        while (this.activeRequests.get() > 0) {
            try {
                Thread.sleep(POLL_FOR_ACTIVE_REQUESTS_MSEC);
            } catch (InterruptedException ex) {
                LOGGER.warn("IP Print Server is interrupted.");
                break;
            }
        }
    }

}
