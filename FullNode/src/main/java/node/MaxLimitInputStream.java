package node;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class MaxLimitInputStream extends FilterInputStream {

    private int length;
    private int limit;

    public MaxLimitInputStream(InputStream in, int limit) {
        super(in);
        this.length=0;
        this.limit=limit;
    }

    public int read() throws IOException {
        ++length;
        if(length>limit)
            throw new IOException("Exceeded the limit");
        return in.read();
    }

    public int read(byte[] data, int offset, int length) throws IOException {

        int result = in.read(data, offset, length);

        length+=result;

        if(length>limit)
            throw new IOException("Exceeded the limit");

        return result;

    }

    public void finish()
    {
        length=0;
    }

    //for debug
    public int get()
    {
        return length;
    }
}
