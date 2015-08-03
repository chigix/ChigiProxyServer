/*
 * This file is part of the ChigiProxyTunnel package.
 * 
 * (c) Richard Lea <chigix@zoho.com>
 * 
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.chigix.chigiproxytunnel;

import com.chigix.chigiproxytunnel.channel.Channel;
import com.chigix.chigiproxytunnel.discard.DiscardClientChannelHandler;
import com.chigix.chigiproxytunnel.discard.DiscardServerChannelHandler;
import com.chigix.chigiproxytunnel.handler.ChannelHandler;
import com.chigix.chigiproxytunnel.handler.ChannelHandlerThread;
import com.chigix.chigiproxytunnel.socks5slave.Socks5SlaveServer;
import com.chigix.chigiproxytunnel.switcher.ChannelNameMap;
import com.chigix.chigiproxytunnel.switcher.ChannelsManager;
import com.chigix.chigiproxytunnel.switcher.CommandChannelExtension;
import com.chigix.chigiproxytunnel.switcher.HealthMonitor;
import com.chigix.chigiproxytunnel.switcher.handler.MasterCommanderHandler;
import com.chigix.chigiproxytunnel.switcher.handler.SlaveCommanderHandler;
import com.chigix.chigiproxytunnel.switcher.SwitcherTable;
import com.chigix.chigiproxytunnel.switcher.goal.Goal;
import com.chigix.chigiproxytunnel.switcher.goal.GoalChannelNotRegisteredException;
import com.chigix.chigiproxytunnel.switcher.goal.ProxyGoal;
import com.ning.compress.lzf.LZFInputStream;
import com.ning.compress.lzf.LZFOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.xml.XmlConfigurationFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;

/**
 *
 * @author Richard Lea <chigix@zoho.com>
 */
public class Application {

    private static final ExecutorService THREAD_POOL;
    private static final LoggerContext LOGGER;

    static {
        THREAD_POOL = Executors.newCachedThreadPool();
        Configuration config = null;
        try {
            config = XmlConfigurationFactory.getInstance().getConfiguration(new ConfigurationSource(Application.class.getResourceAsStream("/log4j.xml")));
        } catch (IOException ex) {
            Logger.getLogger(Application.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
            System.exit(1);
        }
        LoggerConfig loggerconfig = new LoggerConfig("com", Level.ERROR, false);
        ConsoleAppender appender = ConsoleAppender.createDefaultAppenderForLayout(PatternLayout.createDefaultLayout());
        loggerconfig.addAppender(appender, null, null);
        config.addLogger("com", loggerconfig);
        config.addAppender(appender);
        LoggerContext context = new LoggerContext("ApplicationContext");
        context.start(config);
        LOGGER = context;
    }

    public static final ExecutorService getThreadPool() {
        return THREAD_POOL;
    }

    public static final org.apache.logging.log4j.core.Logger getLogger(String name) {
        return LOGGER.getLogger(name);
    }

    public static void main(String[] args) {
        getLogger(Application.class.getName()).log(org.apache.logging.log4j.Level.INFO, "LAUNCHED");
        Options options = new Options();
        options.addOption("targetPort", "target-port", true, "Port number specification for connect.");
        options.addOption("bndPort", "bnd-port", true, "Port number specification for bind.");
        options.addOption("host", "host", true, "Target connect host.");
        options.addOption("name", "name", true, "Name Registered for Slave");
        CommandLineParser parser = new DefaultParser();
        org.apache.commons.cli.CommandLine parsedCli = null;
        try {
            parsedCli = parser.parse(options, args);
        } catch (ParseException ex) {
            getLogger(Application.class.getName()).log(org.apache.logging.log4j.Level.WARN, "ParseException", ex);
            return;
        }
        switch (parsedCli.getArgs()[0]) {
            case "Server":
                launchServer(Integer.valueOf(parsedCli.getOptionValue("bndPort")));
                break;
            case "Client":
                launchClient(parsedCli.getOptionValue("host"), Integer.valueOf(parsedCli.getOptionValue("targetPort")));
                break;
            case "Master":
                launchMaster(Integer.valueOf(parsedCli.getOptionValue("bndPort")));
                break;
            case "Slave":
                if (!parsedCli.hasOption("name")) {
                    getLogger(Application.class.getName()).fatal("The property name is missed.");
                    return;
                }
                launchSlave(parsedCli.getOptionValue("host"), Integer.valueOf(parsedCli.getOptionValue("targetPort")), parsedCli.getOptionValue("name"));
                break;
            case "Socks5Slave":
                launchSocks5Slave(parsedCli.getOptionValue("host"), Integer.valueOf(parsedCli.getOptionValue("targetPort")), parsedCli.getOptionValue("name"), Integer.valueOf(parsedCli.getOptionValue("bndPort")));
                break;
            case "TestClient":
                try {
                    final Socket tmp_test_socket = new Socket("127.0.0.1", 8081);
                    (new Thread() {

                        @Override
                        public void run() {
                            while (true) {
                                int read;
                                try {
                                    read = tmp_test_socket.getInputStream().read();
                                    //System.out.write(read);
                                    System.out.print(read + ",");
                                } catch (IOException ex) {
                                    Logger.getLogger(Application.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
                                    return;
                                }
                            }
                        }

                    }).start();
                    tmp_test_socket.getOutputStream().write(new byte[]{5, 1, 0});
//                    tmp_test_socket.getOutputStream().write(new byte[]{5, 1, 0, 3, 14});
                    //tmp_test_socket.getOutputStream().write(new byte[]{5, 1, 0, 3, 13});
                    tmp_test_socket.getOutputStream().write(new byte[]{5, 1, 0, 3, 18});
//                    tmp_test_socket.getOutputStream().write("api.douban.com".getBytes());
                    //tmp_test_socket.getOutputStream().write("www.baidu.com".getBytes());
                    tmp_test_socket.getOutputStream().write("www.epochtimes.com".getBytes());
                    tmp_test_socket.getOutputStream().write(new byte[]{0, 80});
//                    tmp_test_socket.getOutputStream().write("GET /v2/music/1417415 HTTP/1.1\nHost: api.douban.com\nAccept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8\n\n".getBytes());
                    //tmp_test_socket.getOutputStream().write("GET / HTTP/1.1\nHost: www.baidu.com\nAccept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8\n\n".getBytes());
                    tmp_test_socket.getOutputStream().write("GET / HTTP/1.1\nHost: www.epochtimes.com\nAccept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8\n\n".getBytes());
                } catch (IOException ex) {
                    Logger.getLogger(Application.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
                }
                break;
            default:
                getLogger(Application.class.getName()).log(Level.ALL, "NOT SUPPORTED");
                break;
        }
    }

    protected static void launchServer(int port) {
        ServerSocket bndSocket;
        try {
            bndSocket = new ServerSocket(port);
        } catch (IOException ex) {
            Logger.getLogger(Application.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
            return;
        }
        while (true) {
            Socket channelSocket;
            try {
                channelSocket = bndSocket.accept();
            } catch (IOException ex) {
                Logger.getLogger(Application.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
                continue;
            }
            Channel channel = new Channel(channelSocket);
            try {
                channel.setInputStream(new LZFInputStream(channel.getInputStream()));
                channel.setOutputStream(new LZFOutputStream(channel.getOutputStream()));
            } catch (IOException ex) {
                getLogger(Application.class.getName()).fatal("IOEXCEPTION ON LZF", ex);
            }
            ChannelHandler handler = new DiscardServerChannelHandler() {

                @Override
                public void exceptionCaught(Channel channel, Throwable cause) {
                    getLogger(DiscardServerChannelHandler.class.getName()).fatal(cause.getMessage(), cause);
                }

            };
            getThreadPool().execute(new ChannelHandlerThread(handler, channel));
        }
    }

    protected static void launchClient(String host, int port) {
        Socket channelSocket;
        try {
            channelSocket = new Socket(host, port);
        } catch (IOException ex) {
            getLogger(Application.class.getName()).fatal(ex.getMessage(), ex);
            return;
        }
        Channel channel = new Channel(channelSocket);
        try {
            channel.setInputStream(new LZFInputStream(channel.getInputStream()));
            channel.setOutputStream(new LZFOutputStream(channel.getOutputStream()));
        } catch (IOException ex) {
            getLogger(Application.class.getName()).fatal("IOEXCEPTION ON LZF", ex);
        }
        new Thread(new ChannelHandlerThread(new DiscardClientChannelHandler() {

            @Override
            public void exceptionCaught(Channel channel, Throwable cause) {
                Application.getLogger(DiscardClientChannelHandler.class.getName()).fatal(cause.getMessage(), cause);
            }

        }, channel)).start();
    }

    protected static void launchMaster(int port) {
        ServerSocket bndSocket;
        try {
            bndSocket = new ServerSocket(port);
        } catch (IOException ex) {
            getLogger(Application.class.getName()).fatal(port + "PORT ALREADY BE USED.", ex);
            return;
        }
        MasterCommanderHandler master = new MasterCommanderHandler(new SwitcherTable(ChannelsManager.create()));
        getThreadPool().execute(new HealthMonitor(master.getSwitcher()));
        while (true) {
            Channel channel;
            try {
                channel = new Channel(bndSocket.accept());
            } catch (IOException ex) {
                getLogger(Application.class.getName()).debug("IO Error occurs when master waiting");
                continue;
            }
            ChannelHandler.handleChannel(master, channel);
        }
    }

    protected static void launchSlave(String host, int port, String name) {
        Channel channel;
        try {
            channel = new Channel(new Socket(host, port));
        } catch (IOException ex) {
            getLogger(Application.class.getName()).fatal(ex.getMessage(), ex);
            return;
        }
        SlaveCommanderHandler proxy = new SlaveCommanderHandler(new SwitcherTable(ChannelsManager.create()));
        Goal goal = new ProxyGoal(name, proxy.getSwitcher());
        goal.setCommandChannel(new CommandChannelExtension(goal, channel, ChannelNameMap.generateCommanderName(goal)));
        ChannelNameMap.getInstance().put(channel, goal.getCommandChannel().getChannelName());
        ChannelHandler.handleChannel(proxy, channel);
        proxy.getSwitcher().addGoal(goal);
        try {
            proxy.getSwitcher().open();
        } catch (GoalChannelNotRegisteredException ex) {
            LOGGER.getLogger(Application.class.getName()).fatal(ex.getMessage(), ex);
            System.exit(1);
        }
    }

    protected static void launchSocks5Slave(String host, int targetPort, String name, int bndPort) {
        ServerSocket bndSocket;
        try {
            bndSocket = new ServerSocket(8081);
        } catch (IOException ex) {
            getLogger(Application.class.getName()).fatal(bndPort + " port already be used.");
            return;
        }
        SwitcherTable switcher = new SwitcherTable(ChannelsManager.create());
        Goal g = new ProxyGoal(name, switcher);
        CommandChannelExtension chnEx;
        try {
            chnEx = new CommandChannelExtension(g, new Channel(new Socket(host, targetPort)), ChannelNameMap.generateCommanderName(g));
        } catch (IOException ex) {
            getLogger(Application.class.getName()).fatal(ex.getMessage(), ex);
            return;
        }
        g.setCommandChannel(chnEx);
        switcher.addGoal(g);
        ChannelNameMap.getInstance().put(g.getCommandChannel().getChannel(), g.getCommandChannel().getChannelName());
        ChannelHandler.handleChannel(new SlaveCommanderHandler(switcher), chnEx.getChannel());
        try {
            switcher.open();
        } catch (GoalChannelNotRegisteredException ex) {
            LOGGER.getLogger(Application.class.getName()).fatal(ex.getMessage(), ex);
            System.exit(1);
        }
        Socks5SlaveServer server = new Socks5SlaveServer(chnEx, switcher) {

            @Override
            public void exceptionCaught(Channel channel, Throwable cause) {
                getLogger(Socks5SlaveServer.class.getName()).fatal(cause.getMessage(), cause);
            }

        };
        while (true) {
            Socket s;
            try {
                s = bndSocket.accept();
            } catch (IOException ex) {
                getLogger(Application.class.getName()).debug("Socks5 Slave Waiting connect failed.");
                continue;
            }
            ChannelHandler.handleChannel(server, new Channel(s));
        }
    }

}
