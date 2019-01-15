package node;

import blockchain.Status;
import blockchain.block.Block;
import blockchain.block.BlockHeader;
import blockchain.block.transaction.Transaction;
import blockchain.utility.RawTranslator;
import config.Configuration;
import exception.BlockChainObjectParsingException;
import exception.InvalidBlockChainMessageException;

import javax.net.ssl.SSLSocket;
import java.io.*;

public class ConnectionManager {


    private final DataOutputStream os;
    private final DataInputStream is;
    private SSLSocket socket;
    private boolean isConfirmed;

    public boolean isConfirmed() {
        return isConfirmed;
    }

    public void setConfirmed(boolean confirmed) {
        isConfirmed = confirmed;
    }


    public SSLSocket getSocket() {
        return socket;
    }

    public void setSocket(SSLSocket socket) {
        this.socket = socket;
    }

    public ConnectionManager(SSLSocket socket, boolean isConfirmed) throws IOException {
        this.socket = socket;
        socket.setKeepAlive(true);

        this.os = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        this.is = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        this.isConfirmed =isConfirmed;
    }

    public void close(){

       if(!isClosed())
       {
           try {
               socket.close();
           } catch (IOException e) {
               e.printStackTrace();
           }

       }
    }

    public boolean isClosed()
    {
        return socket.isClosed();
    }


    /*Messagem number
     * 0: status
     * 1: peer nodes list request
     * 2: block header request - byte[][] hash locator - from greater height to smaller height - dense to sparse
     * 3: block request - content =byte[] blockHash
     * 4: peer node list
     * 5: block header list - from smaller height to greater height
     * 6: transaction
     * 7: block
     * */
    public void write(Message message) throws IOException {
        synchronized (os)
        {
            byte[] content =message.content;

            os.writeByte(message.number);
            if(message.number==Configuration.MESSAGE_PEER_NODE_REQUEST) {
                os.writeInt(0);
            }
            else {
                os.writeInt(content.length);
                os.write(content);
            }
            os.flush();
        }
    }

    public Message read() throws IOException, InvalidBlockChainMessageException {

        synchronized (is)
        {
            byte number =is.readByte();
            int length = is.readInt();
            checkLength(number,length);

            byte[] content = new byte[length];
            is.readFully(content);

            return new Message(number,content);
        }
    }


    //currently no limit
    private void checkLength(byte number, int length) throws InvalidBlockChainMessageException {
        switch (number) {
            case Configuration.MESSAGE_STATUS:
                break;
            case Configuration.MESSAGE_PEER_NODE_REQUEST:
                if(length!=0)
                    throw new InvalidBlockChainMessageException(socket.getInetAddress().getHostAddress(),number);
                break;
            case Configuration.MESSAGE_HEADER_REQUEST:
                break;
            case Configuration.MESSAGE_BLOCK_REQUEST:
                if(length!=Configuration.HASH_LENGTH)
                    throw new InvalidBlockChainMessageException(socket.getInetAddress().getHostAddress(),number);
                break;
            case Configuration.MESSAGE_PEER_NODE_LIST:
                break;
            case Configuration.MESSAGE_HEADER_LIST:
                break;
            case Configuration.MESSAGE_TRANSACTION:
                break;
            case Configuration.MESSAGE_BLOCK:
                break;
            default:
                throw new InvalidBlockChainMessageException(socket.getInetAddress().getHostAddress(),number);
        }
    }
}
