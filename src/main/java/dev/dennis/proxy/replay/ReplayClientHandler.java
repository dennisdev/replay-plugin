package dev.dennis.proxy.replay;

import dev.dennis.RecordingParser;
import dev.dennis.ReplayPlugin;
import dev.dennis.proxy.record.RecordServerInitializer;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ThreadFactory;

@Slf4j
public class ReplayClientHandler extends SimpleChannelInboundHandler<ByteBuf> {
    public static final int LOGIN_CONNECTION_TYPE = 14;
    public static final int LOGIN_TYPE = 16;

    private final ReplayPlugin replayPlugin;

    private final Path recordingPath;

    private LoginState loginState = LoginState.CONNECT;

    private RecordingParser recordingParser;
    private RecordingReplayer recordingReplayer;

    enum LoginState {
        CONNECT,
        LOGGING_IN,
        LOGGED_IN
    }

    public ReplayClientHandler(ReplayPlugin replayPlugin, Path recordingPath) {
        this.replayPlugin = replayPlugin;
        this.recordingPath = recordingPath;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("Client connected: {}", ctx.channel());
        this.recordingParser = RecordingParser.load(this.recordingPath);
        this.recordingReplayer = new RecordingReplayer(this.recordingParser, ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("Client disconnected: {}", ctx.channel());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
//        log.info("\n{}", ByteBufUtil.prettyHexDump(msg));
        if (this.loginState == LoginState.CONNECT) {
            if (msg.getByte(0) == LOGIN_CONNECTION_TYPE) {
                ctx.writeAndFlush(Unpooled.wrappedBuffer(new byte[9]));
                this.loginState = LoginState.LOGGING_IN;
            }
        } else if (this.loginState == LoginState.LOGGING_IN) {
            if (msg.getByte(0) == LOGIN_TYPE) {
                log.info("Logging in, {}", replayPlugin.getIsaacKey());
                int[] clientKey = this.recordingParser.getIsaacKey();
                int[] serverKey = new int[4];
                for (int i = 0; i < clientKey.length; i++) {
                    serverKey[i] = clientKey[i] + 50;
                }
                this.replayPlugin.seedIsaac(serverKey);
                this.recordingReplayer.start();
                this.loginState = LoginState.LOGGED_IN;
            }
        }

    }
}
