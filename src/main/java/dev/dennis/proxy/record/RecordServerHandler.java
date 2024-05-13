package dev.dennis.proxy.record;

import dev.dennis.RecordingWriter;
import dev.dennis.ReplayPlugin;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Calendar;

@Slf4j
public class RecordServerHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private final ReplayPlugin replayPlugin;

    private final ChannelHandlerContext clientChannel;

    private RecordingWriter recordingWriter;

    public RecordServerHandler(ReplayPlugin replayPlugin, ChannelHandlerContext clientChannel) {
        this.replayPlugin = replayPlugin;
        this.clientChannel = clientChannel;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("Connected to the server: {}, {}", ctx.channel(), this.replayPlugin.getIsaacKey());
        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(Calendar.getInstance().getTime());
        Path recordingPath = Paths.get("recordings", timestamp);
        Files.createDirectories(recordingPath);
        this.recordingWriter = new RecordingWriter(recordingPath);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("Disconnected from the server: {}", ctx.channel());
        this.clientChannel.close();
        if (this.recordingWriter != null) {
            this.recordingWriter.close();
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        int startIndex = msg.readerIndex();

        byte[] data = new byte[msg.readableBytes()];
        msg.readBytes(data);

        msg.readerIndex(startIndex);

        this.clientChannel.writeAndFlush(msg.retain());

//        log.info("\n{}", ByteBufUtil.prettyHexDump(msg));

        if (this.recordingWriter != null) {
            if (!this.recordingWriter.isIsaacWritten()) {
                int[] isaacKey = replayPlugin.getIsaacKey();
                if (isaacKey != null) {
                    log.info("Writing isaac: {}", isaacKey);
                    this.recordingWriter.writeIsaac(isaacKey);
                }
            }

            this.recordingWriter.write(data);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        if (this.recordingWriter != null) {
            this.recordingWriter.flush();
        }
    }
}
