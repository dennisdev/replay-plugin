package dev.dennis.proxy.record;

import dev.dennis.ReplayPlugin;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RecordClientInitializer extends ChannelInitializer<SocketChannel> {
    private final ReplayPlugin replayPlugin;

    public RecordClientInitializer(ReplayPlugin replayPlugin) {
        this.replayPlugin = replayPlugin;
    }

    private String address;
    private int port;

    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        if (this.address == null) {
            throw new RuntimeException("No address to connect to set");
        }
        ChannelPipeline pipeline = socketChannel.pipeline();
        pipeline.addLast("handler", new RecordClientHandler(this.replayPlugin, this.address, this.port));
    }

    public String getAddress() {
        return this.address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
