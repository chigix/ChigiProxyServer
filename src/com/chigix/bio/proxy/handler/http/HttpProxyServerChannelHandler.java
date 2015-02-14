package com.chigix.bio.proxy.handler.http;

import com.chigix.bio.proxy.ChigiProxy;
import com.chigix.bio.proxy.FormatDateTime;
import com.chigix.bio.proxy.buffer.FixedBuffer;
import com.chigix.bio.proxy.channel.Channel;
import com.chigix.bio.proxy.channel.Tunnel;
import com.chigix.bio.proxy.handler.ChannelHandler;
import com.chigix.bio.proxy.handler.ChannelHandlerThread;
import com.chigix.bio.proxy.utils.BufferInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.impl.io.DefaultHttpRequestParser;
import org.apache.http.impl.io.DefaultHttpRequestWriter;
import org.apache.http.impl.io.HttpTransportMetricsImpl;
import org.apache.http.impl.io.SessionInputBufferImpl;
import org.apache.http.impl.io.SessionOutputBufferImpl;
import org.apache.http.io.HttpMessageParser;
import org.apache.http.message.BasicHttpRequest;

/**
 *
 * @author Richard Lea <chigix@zoho.com>
 */
public class HttpProxyServerChannelHandler extends ChannelHandler {

    private BufferInputStream buffer;

    private HttpRequest request;

    private final FixedBuffer<Integer> delimiterCheck;

    //@TODO: REMOVE
    private ByteArrayOutputStream debugHeaderBuffer;

    private HttpProxyOperation operation;

    private Tunnel tunnel;

    private int requestCount;

    public HttpProxyServerChannelHandler(Channel channel) {
        super(channel);
        this.requestCount = 0;
        this.delimiterCheck = new FixedBuffer<Integer>(3);
        this.operation = HttpProxyOperation.DISCARD;
        this.debugHeaderBuffer = new ByteArrayOutputStream();
        this.tunnel = new Tunnel("INIT", channel, null);
    }

    /**
     * Try greatly to make the param uri parsable for java.net.URI
     *
     * @param toParse
     * @return
     * @throws com.chigix.bio.proxy.handler.http.InvalidRequestUriException
     */
    public static URL uriParseUtil(String toParse) throws InvalidRequestUriException {
        URL url = null;
        try {
            url = new URL(toParse);
        } catch (MalformedURLException ex) {
            throw new InvalidRequestUriException(toParse, ex.getMessage());
        }
        return url;
    }

    @Override
    public void channelRead(Channel channel, int input) {
        switch (this.operation) {
            case WAIT_FOR_HEADER_PARSE:
                if (this.request != null) {
                    this.createHttpRequestParsing();
                }
                this.headerReceive(channel, input);
                break;
            case SEND_TO_PROXY_DIRECTLY:
                try {
                    this.tunnel.getTargetHostChannel().getOutputStream().write(input);
                } catch (IOException ex) {
                    this.tunnel.close();
                    System.out.println("PROXY CHANNEL CLOSED SELFLY");
                }
                break;
            case DISCARD:
            default:
                break;
        }
    }

    private void createHttpRequestParsing() {
        this.buffer = new BufferInputStream();
        this.operation = HttpProxyOperation.WAIT_FOR_HEADER_PARSE;
        this.request = null;
        SessionInputBufferImpl sessionInput = new SessionInputBufferImpl(new HttpTransportMetricsImpl(), 1024);
        sessionInput.bind(this.buffer);
        final HttpMessageParser<HttpRequest> parser = new DefaultHttpRequestParser(sessionInput);
        final Channel browserChannel = this.getChannel();
        ChigiProxy.getThreadPool().execute(new Thread() {

            @Override
            public void run() {
                HttpRequest parsed_request;
                try {
                    System.out.println("PARSING");
                    parsed_request = parser.parse();
                    request = parsed_request;
                    buffer.clear();
                    debugHeaderBuffer = new ByteArrayOutputStream();
                    operation = HttpProxyOperation.SEND_TO_PROXY_DIRECTLY;
                    System.out.println(FormatDateTime.toTimeString(new Date()) + " PARSED " + request.toString());
                } catch (org.apache.http.ProtocolException ex) {
                    Logger.getLogger(HttpProxyServerChannelHandler.class.getName()).log(Level.SEVERE, "REQUEST COUNT: " + requestCount + ": " + new String(debugHeaderBuffer.toByteArray()), ex);
                    try {
                        tunnel.getRequestClientChannel().getOutputStream().write("HTTP/1.1 400 Bad Request\r\n\r\n".getBytes());
                    } catch (IOException ex1) {
                    }
                    tunnel.close();
                } catch (ConnectionClosedException ex) {
                    Logger.getLogger(HttpProxyServerChannelHandler.class.getName()).log(Level.SEVERE, Arrays.toString(debugHeaderBuffer.toByteArray()), ex);
                } catch (IOException ex) {
                    Logger.getLogger(HttpProxyServerChannelHandler.class.getName()).log(Level.SEVERE, null, ex);
                    browserChannel.close();
                } catch (HttpException ex) {
                    Logger.getLogger(HttpProxyServerChannelHandler.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

        });
    }

    private void makeTunnel() throws InvalidRequestUriException, IOException {
        final Channel browser_channel = this.getChannel();
        while (this.request == null) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
            }
        }
        final HttpRequest current_request = this.request;
        System.out.println(FormatDateTime.toTimeString(new Date()) + " " + current_request);
        Socket proxy_socket = null;
        if (current_request.getRequestLine().getMethod().equalsIgnoreCase("connect")) {
            Pattern p = Pattern.compile("^(.+):(\\d+)$");
            Matcher m = p.matcher(this.request.getRequestLine().getUri());
            if (!m.find()) {
                throw new InvalidRequestUriException(current_request.getRequestLine().getUri());
            }
            proxy_socket = new Socket(m.group(1), Integer.valueOf(m.group(2)));
        } else {
            URL uri;
            uri = uriParseUtil(current_request.getRequestLine().getUri());
            if (uri.getPort() == -1) {
                proxy_socket = new Socket(uri.getHost(), 80);
            } else {
                proxy_socket = new Socket(uri.getHost(), uri.getPort());
            }
        }
        this.tunnel = new Tunnel("INIT", browser_channel, new Channel(proxy_socket));
        ChigiProxy.getThreadPool().execute(new ChannelHandlerThread(new ChannelHandler(this.tunnel.getTargetHostChannel()) {

            private boolean isTunnel = false;

            @Override
            public void channelRead(Channel channel, int input) {
                if (this.isTunnel && operation != HttpProxyOperation.SEND_TO_PROXY_DIRECTLY) {
                    // For HTTPS Schema:
                    operation = HttpProxyOperation.SEND_TO_PROXY_DIRECTLY;
                } else if (!this.isTunnel && operation != HttpProxyOperation.WAIT_FOR_HEADER_PARSE) {
                    // For HTTP Schema:
                    operation = HttpProxyOperation.WAIT_FOR_HEADER_PARSE;
                }
                try {
                    browser_channel.getOutputStream().write(input);
                } catch (IOException ex) {
                    tunnel.close();
                    System.out.println("BROWSER CLOSED SELFLY: [" + current_request.getRequestLine().getUri() + "]");
                }
            }

            @Override
            public void channelInactive(Channel channel) {
                System.out.println("PROXY DISCONNECTED: [" + current_request.getRequestLine().getUri() + "]");
                tunnel.close();
            }

            @Override
            public void channelActive(Channel channel) {
                System.out.println("PROXY CONNECTED: [" + current_request.getRequestLine().getUri() + "]");
                if (current_request.getRequestLine().getMethod().equalsIgnoreCase("connect")) {
                    this.isTunnel = true;
                }
            }

        }));
    }

    private void forwardHttpRequest(final HttpRequest request) throws HttpProxyException, IOException {
        final SessionOutputBufferImpl outputstream_buffer_to_proxy = new SessionOutputBufferImpl(new HttpTransportMetricsImpl(), 128);
        outputstream_buffer_to_proxy.bind(this.tunnel.getTargetHostChannel().getOutputStream());
        URL origUri;
        origUri = uriParseUtil(request.getRequestLine().getUri());
        StringBuilder abs_path = new StringBuilder(origUri.getPath());
        if (origUri.getQuery() != null) {
            abs_path.append("?").append(origUri.getQuery());
        }
        HttpRequest forward_request = new BasicHttpRequest(request.getRequestLine().getMethod(), abs_path.toString(), request.getRequestLine().getProtocolVersion());
        forward_request.setHeaders(request.getAllHeaders());
        forward_request.removeHeaders("Proxy-Authenticate");
        forward_request.removeHeaders("Proxy-Authorization");
        forward_request.removeHeaders("Transfer-Encoding");
        forward_request.removeHeaders("Upgrade");
        if (!forward_request.containsHeader("Connection") && forward_request.containsHeader("Proxy-Connection")) {
            forward_request.setHeader("Connection", forward_request.getFirstHeader("Proxy-Connection").getValue());
        }
        forward_request.removeHeaders("Proxy-Connection");
        System.out.println("ORIG REQUEST:" + forward_request.toString());
        try {
            new DefaultHttpRequestWriter(outputstream_buffer_to_proxy).write(forward_request);
            outputstream_buffer_to_proxy.flush();
            System.out.println("REQUEST HEADER SENT: [" + request.getRequestLine().getUri() + "]");
        } catch (HttpException ex) {
            Logger.getLogger(HttpProxyServerChannelHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        this.requestCount++;
    }

    private void headerReceive(Channel channel, int input) {
        this.debugHeaderBuffer.write(input);
        this.buffer.push(input);
        this.delimiterCheck.offer(input);
        //System.out.println(new String(this.debugHeaderBuffer.toByteArray()));
        if (input == 10) {
            Integer[] currentCheck = this.delimiterCheck.toArray(new Integer[3]);
            if (currentCheck[0] == 10 || currentCheck[1] == 10) {
                System.out.println("HEADER RECEIVED");
                // HTTP Headers Closed.
                this.buffer.end();
                try {
                    this.makeTunnel();
                } catch (InvalidRequestUriException ex) {
                    Logger.getLogger(HttpProxyServerChannelHandler.class.getName()).log(Level.SEVERE, null, ex);
                    this.operation = HttpProxyOperation.DISCARD;
                    return;
                } catch (UnknownHostException ex) {
                    try {
                        channel.getOutputStream().write("HTTP/1.1 504 Gateway Timeout\r\n\r\n".getBytes());
                    } catch (IOException ex1) {
                    }
                    this.tunnel.close();
                    this.operation = HttpProxyOperation.DISCARD;
                    return;
                } catch (ConnectException ex) {
                    try {
                        channel.getOutputStream().write("HTTP/1.1 502 Bad Gateway\r\n\r\n".getBytes());
                    } catch (IOException ex1) {
                    }
                    this.tunnel.close();
                    this.operation = HttpProxyOperation.DISCARD;
                    return;
                } catch (IOException ex) {
                    Logger.getLogger(HttpProxyServerChannelHandler.class.getName()).log(Level.SEVERE, null, ex);
                    this.operation = HttpProxyOperation.DISCARD;
                    return;
                }
                System.out.println("BANKAI:" + this.request);
                if (this.request.getRequestLine().getMethod().equalsIgnoreCase("connect")) {
                    try {
                        channel.getOutputStream().write("HTTP/1.1 200 Tunnel established\r\n\r\n".getBytes());
                    } catch (IOException ex) {
                        this.tunnel.close();
                        System.out.println("BROWSER CLOSED: [" + request.getRequestLine().getUri() + "]");
                        this.operation = HttpProxyOperation.DISCARD;
                        return;
                    }
                    this.operation = HttpProxyOperation.SEND_TO_PROXY_DIRECTLY;
                } else {
                    try {
                        this.forwardHttpRequest(this.request);
                    } catch (HttpProxyException ex) {
                        this.operation = HttpProxyOperation.DISCARD;
                        this.tunnel.close();
                        Logger.getLogger(HttpProxyServerChannelHandler.class.getName()).log(Level.SEVERE, null, ex);
                        return;
                    } catch (IOException ex) {
                        this.tunnel.close();
                        System.out.println("PROXY CLOSED: [" + request.getRequestLine().getUri() + "]");
                        return;
                    }
                    this.operation = HttpProxyOperation.SEND_TO_PROXY_DIRECTLY;
                }
            }
        }
    }

    @Override
    public void channelInactive(Channel channel) {
        System.out.println("BROWSER CLOSED SELFLY");
        System.out.println(channel.getRemoteHostAddress() + "断开");
        this.tunnel.close();
    }

    @Override
    public void channelActive(Channel channel) {
        System.out.println(channel.getRemoteHostAddress() + "连入");
        this.operation = HttpProxyOperation.WAIT_FOR_HEADER_PARSE;
        this.request = new BasicHttpRequest("TEST", "/");
    }

}
