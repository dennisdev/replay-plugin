package dev.dennis.proxy.replay;

import dev.dennis.ReplayPlugin;
import dev.dennis.proxy.record.RecordClientHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;

@Slf4j
public class ReplayClientInitializer extends ChannelInitializer<SocketChannel> {
    private final ReplayPlugin replayPlugin;

    private Path recordingPath;

    public ReplayClientInitializer(ReplayPlugin replayPlugin) {
        this.replayPlugin = replayPlugin;
    }

    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        if (recordingPath == null) {
            throw new RuntimeException("No recording path set");
        }
        ChannelPipeline pipeline = socketChannel.pipeline();
        pipeline.addLast("handler", new ReplayClientHandler(this.replayPlugin, this.recordingPath));
    }

    public Path getRecordingPath() {
        return recordingPath;
    }

    public void setRecordingPath(Path recordingPath) {
        this.recordingPath = recordingPath;
    }
}
