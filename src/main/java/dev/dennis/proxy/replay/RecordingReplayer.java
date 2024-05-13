package dev.dennis.proxy.replay;

import dev.dennis.RecordingParser;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RecordingReplayer extends Thread {
    private final RecordingParser recordingParser;

    private final Channel channel;

    private int messageIndex;

    public RecordingReplayer(RecordingParser recordingParser, Channel channel) {
        this.recordingParser = recordingParser;
        this.channel = channel;
    }

    @Override
    public void run() {
        int messageOffset = 0;
        long lastTimestamp = -1;
        while (this.messageIndex < this.recordingParser.getMessageCount()) {
            long timestamp = this.recordingParser.getMessageTimestamp(this.messageIndex);
            int messageLength = this.recordingParser.getMessageLength(this.messageIndex);

            if (lastTimestamp != -1) {
                long deltaTime = timestamp - lastTimestamp;
                try {
                    Thread.sleep(deltaTime);
                } catch (InterruptedException e) {
                }
            }

            byte[] data = this.recordingParser.readMessage(messageOffset, messageLength);
            ByteBuf buf = Unpooled.wrappedBuffer(data);


            if (messageIndex >= 2) {
//                log.info("{}\n{}", messageIndex, ByteBufUtil.prettyHexDump(buf));
                this.channel.writeAndFlush(buf);
            }

            lastTimestamp = timestamp;
            messageOffset += messageLength;
            this.messageIndex++;
        }
    }
}
