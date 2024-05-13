package dev.dennis.proxy;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ThreadFactory;

@Slf4j
public class ProxyServer {
    private final ThreadFactory threadFactory;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    private ChannelFuture channelFuture;

    public ProxyServer() {
        this.threadFactory = new DefaultThreadFactory("client");
    }

    public void start(int port, ChannelHandler childHandler) throws InterruptedException {
        this.bossGroup = new NioEventLoopGroup(this.threadFactory);
        this.workerGroup = new NioEventLoopGroup(this.threadFactory);

        ServerBootstrap bootstrap = new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .handler(new LoggingHandler())
                .childHandler(childHandler);
        this.channelFuture = bootstrap
                .bind(port)
                .sync();

        log.info("Started on port {}", port);
    }

    public void stop() throws InterruptedException {
        this.channelFuture.channel().close();

        if (this.bossGroup != null) {
            this.bossGroup.shutdownGracefully().sync();
            this.bossGroup = null;
        }
        if (this.workerGroup != null) {
            this.workerGroup.shutdownGracefully().sync();
            this.workerGroup = null;
        }
    }
}
