package com.chigix.bio.proxy.handler;

import com.chigix.bio.proxy.ChigiProxy;
import com.chigix.bio.proxy.buffer.FixedBuffer;
import com.chigix.bio.proxy.channel.Channel;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.impl.io.DefaultHttpRequestParser;
import org.apache.http.impl.io.DefaultHttpRequestWriter;
import org.apache.http.impl.io.HttpTransportMetricsImpl;
import org.apache.http.impl.io.SessionInputBufferImpl;
import org.apache.http.impl.io.SessionOutputBufferImpl;
import org.apache.http.io.HttpMessageParser;

/**
 *
 * @author Richard Lea <chigix@zoho.com>
 */
public class HttpProxyServerChannelHandler extends ChannelHandler {

    private final BufferInputStream buffer;

    private HttpRequest request;

    private final FixedBuffer<Integer> delimiterCheck;

    private boolean flag_sendBackDirect = false;

    private ChannelHandler proxyChannel = null;

    public HttpProxyServerChannelHandler(Channel channel) {
        super(channel);
        this.buffer = new BufferInputStream();
        this.delimiterCheck = new FixedBuffer<Integer>(3);
        this.flag_sendBackDirect = false;
    }

    @Override
    public void channelRead(Channel channel, int input) {
        if (this.flag_sendBackDirect) {
            try {
                this.proxyChannel.getChannel().getOutputStream().write(input);
            } catch (IOException ex) {
                channel.close();
                this.proxyChannel.getChannel().close();
                System.out.println("PROXY CHANNEL CLOSED SELFLY: " + this.proxyChannel.getChannel().getRemoteHostAddress());
            }
            return;
        }
        System.out.print((char) input + " " + input + ",");
        this.buffer.push(input);
        this.delimiterCheck.offer(input);
        if (input == 10) {
            Integer[] currentCheck = this.delimiterCheck.toArray(new Integer[3]);
            if (currentCheck[0] == 10 || currentCheck[1] == 10) {
                // HTTP Headers Closed.
                System.out.println("PRE-PROXY HEADER CLOSED");
                this.buffer.end();
                while (this.request == null) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ex) {
                    }
                }
                System.out.println(this.request);
                Socket proxySocket = null;
                if (this.request.getRequestLine().getMethod().equalsIgnoreCase("connect")) {
                    Pattern p = Pattern.compile("^(.+):(\\d+)$");
                    Matcher m = p.matcher(this.request.getRequestLine().getUri());
                    if (!m.find()) {
                        Logger.getLogger(HttpProxyServerChannelHandler.class.getName()).log(Level.SEVERE, null, "INVALID CONNECT URI:" + this.request.getRequestLine().getUri());
                    }
                    try {
                        proxySocket = new Socket(m.group(1), Integer.valueOf(m.group(2)));
                    } catch (IOException ex) {
                        Logger.getLogger(HttpProxyServerChannelHandler.class.getName()).log(Level.SEVERE, null, ex);
                        //channel.getOutputStream().write("HTTP/1.1 502 Bad Gateway\r\n\r\n".getBytes());
                    }
                } else {
                    URI uri = null;
                    try {
                        uri = new URI(this.request.getRequestLine().getUri());
                    } catch (URISyntaxException ex) {
                        Logger.getLogger(HttpProxyServerChannelHandler.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    try {
                        if (uri.getPort() == -1) {
                            proxySocket = new Socket(uri.getHost(), 80);
                        } else {
                            proxySocket = new Socket(uri.getHost(), uri.getPort());
                        }
                    } catch (IOException ex) {
                        Logger.getLogger(HttpProxyServerChannelHandler.class.getName()).log(Level.SEVERE, null, ex);
                        //channel.getOutputStream().write("HTTP/1.1 502 Bad Gateway\r\n\r\n".getBytes());
                    }
                }
                System.out.println(Arrays.toString(this.request.getHeaders("Host")));
                String targetHost = null;
                final Channel browserChannel = channel;
                final String targetConnection = proxySocket.getInetAddress().getHostName() + ":" + proxySocket.getPort();
                this.proxyChannel = new ChannelHandler(new Channel(proxySocket)) {

                    @Override
                    public void channelRead(Channel channel, int input) {
                        try {
                            browserChannel.getOutputStream().write(input);
                        } catch (IOException ex) {
                            browserChannel.close();
                            channel.close();
                            System.out.println("BROWSER CLOSED SELFLY: " + targetConnection);
                            return;
                        }
                    }

                    @Override
                    public void channelInactive(Channel channel) {
                        System.out.println("PROXY DISCONNECTED: " + targetConnection);
                        browserChannel.close();
                    }

                    @Override
                    public void channelActive(Channel channel) {
                        System.out.println("PROXY CONNECTED: " + targetConnection);
                        flag_sendBackDirect = true;
                        if (request.getRequestLine().getMethod().equalsIgnoreCase("connect")) {
                            try {
                                browserChannel.getOutputStream().write("HTTP/1.1 200 Tunnel established\r\n\r\n".getBytes());
                            } catch (IOException ex) {
                                browserChannel.close();
                                channel.close();
                                System.out.println("BROWSER CLOSED: " + targetConnection);
                            }
                        } else {
                            SessionOutputBufferImpl buffer = new SessionOutputBufferImpl(new HttpTransportMetricsImpl(), 128);
                            buffer.bind(channel.getOutputStream());
                            try {
                                new DefaultHttpRequestWriter(buffer).write(request);
                                buffer.flush();
                                System.out.println("REQUEST HEADER SENT: " + targetConnection);
                            } catch (IOException ex) {
                                channel.close();
                                browserChannel.close();
                                System.out.println("PROXY CLOSED: " + targetConnection);
                            } catch (HttpException ex) {
                                Logger.getLogger(HttpProxyServerChannelHandler.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    }

                };
                ChigiProxy.getThreadPool().execute(new ChannelHandlerThread(this.proxyChannel));
            }
        }
    }

    @Override
    public void channelInactive(Channel channel) {
        System.out.println("BROWSER CLOSED SELFLY");
        System.out.println(channel.getRemoteHostAddress() + "断开");
        if (this.proxyChannel != null) {
            this.proxyChannel.getChannel().close();
        }
    }

    @Override
    public void channelActive(Channel channel) {
        System.out.println(channel.getRemoteHostAddress() + "连入");
        SessionInputBufferImpl sessionInput = new SessionInputBufferImpl(new HttpTransportMetricsImpl(), 1024);
        sessionInput.bind(this.buffer);
        final HttpMessageParser<HttpRequest> parser = new DefaultHttpRequestParser(sessionInput);
        final Channel browserChannel = channel;
        ChigiProxy.getThreadPool().execute(new Thread() {

            @Override
            public void run() {
                try {
                    System.out.println("PARSING");
                    request = parser.parse();
                    System.out.println("PARSED" + request.toString());
                } catch (IOException ex) {
                    Logger.getLogger(HttpProxyServerChannelHandler.class.getName()).log(Level.SEVERE, null, ex);
                    browserChannel.close();
                    return;
                } catch (HttpException ex) {
                    Logger.getLogger(HttpProxyServerChannelHandler.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

        });
    }

    private class BufferInputStream extends InputStream {

        private ConcurrentLinkedQueue<Integer> buffer;
        private boolean isEnded = false;

        public BufferInputStream() {
            this.buffer = new ConcurrentLinkedQueue<Integer>();
        }

        @Override
        public int read() throws IOException {
            Integer read = -1;
            while ((read = this.buffer.poll()) == null) {
                if (this.isEnded) {
                    return -1;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                }
            }
            return read;
        }

        public void push(int read) {
            this.buffer.add(read);
        }

        public void end() {
            this.isEnded = true;
        }

        public int size() {
            return this.buffer.size();
        }

    }

}
