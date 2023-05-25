package dev.plom.ide;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileManagement {
    public static void copyStreams(InputStream in, OutputStream out) throws IOException {
        byte[] data = new byte[8192];
        int numRead = in.read(data);
        while (numRead > 0)
        {
            out.write(data, 0, numRead);
            numRead = in.read(data);
        }
    }
}
