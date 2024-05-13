package dev.dennis;

import java.io.*;
import java.nio.file.Path;

public class RecordingWriter implements Closeable {
    private final FileOutputStream messagesOutput;
    private final DataOutputStream messageMetaOutput;
    private final DataOutputStream isaacOutput;

    private boolean isaacWritten;

    public RecordingWriter(Path path) throws FileNotFoundException {
        this.messagesOutput = new FileOutputStream(path.resolve("messages.dat").toFile(), true);
        this.messageMetaOutput = new DataOutputStream(new FileOutputStream(path.resolve("messages_meta.dat").toFile(), true));
        this.isaacOutput = new DataOutputStream(new FileOutputStream(path.resolve("isaac.dat").toFile()));
    }

    public void write(byte[] data) throws IOException {
        this.messagesOutput.write(data);

        long now = System.currentTimeMillis();
        this.messageMetaOutput.writeLong(now);
        this.messageMetaOutput.writeInt(data.length);
    }

    public void writeIsaac(int[] key) throws IOException {
        for (int part : key) {
            this.isaacOutput.writeInt(part);
        }
        this.isaacWritten = true;
    }

    public void flush() throws IOException {
        this.messagesOutput.flush();
        this.messageMetaOutput.flush();
        this.isaacOutput.flush();
    }

    @Override
    public void close() throws IOException {
        this.messagesOutput.close();
        this.messageMetaOutput.close();
        this.isaacOutput.close();
    }

    public boolean isIsaacWritten() {
        return this.isaacWritten;
    }
}
