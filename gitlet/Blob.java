package gitlet;

import java.io.Serializable;

public class Blob implements Serializable {
    private byte[] contents;

    public Blob(byte[] contents) {
        this.contents = contents;
    }

    public byte[] getContents() {
        return contents;
    }
}
