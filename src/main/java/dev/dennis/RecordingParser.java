package dev.dennis;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class RecordingParser {
    private static final int MESSAGE_META_STRIDE = 12;

    private final int[] isaacKey;

    private final ByteBuf messagesInput;

    private final ByteBuf messagesMetaInput;

    public static RecordingParser load(Path path) throws IOException {
        DataInputStream dataInputStream = new DataInputStream(new FileInputStream(path.resolve("isaac.dat").toFile()));
        int[] isaacKey = new int[4];
        for (int i = 0; i < isaacKey.length; i++) {
            isaacKey[i] = dataInputStream.readInt();
        }
        ByteBuf messagesInput = Unpooled.wrappedBuffer(Files.readAllBytes(path.resolve("messages.dat")));
        ByteBuf messagesMetaInput = Unpooled.wrappedBuffer(Files.readAllBytes(path.resolve("messages_meta.dat")));
        return new RecordingParser(isaacKey, messagesInput, messagesMetaInput);
    }

    public RecordingParser(int[] isaacKey, ByteBuf messagesInput, ByteBuf messagesMetaInput) {
        this.isaacKey = isaacKey;
        this.messagesInput = messagesInput;
        this.messagesMetaInput = messagesMetaInput;
    }

    public int getMessageCount() {
        return this.messagesMetaInput.writerIndex() / MESSAGE_META_STRIDE;
    }

    public long getMessageTimestamp(int index) {
        int offset = index * MESSAGE_META_STRIDE;
        return this.messagesMetaInput.getLong(offset);
    }

    public int getMessageLength(int index) {
        int offset = index * MESSAGE_META_STRIDE;
        return this.messagesMetaInput.getInt(offset + 8);
    }

    public byte[] readMessage(int offset, int length) {
        byte[] data = new byte[length];
        this.messagesInput.getBytes(offset, data, 0, length);

        return data;
    }

    public int[] getIsaacKey() {
        return isaacKey;
    }
}
