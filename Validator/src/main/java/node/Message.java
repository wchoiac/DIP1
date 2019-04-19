package node;

import blockchain.Status;
import blockchain.block.Block;
import blockchain.block.BlockHeader;
import blockchain.block.transaction.Transaction;
import blockchain.utility.RawTranslator;
import config.Configuration;
import exception.BlockChainObjectParsingException;

import java.util.Arrays;

public class Message {
    public final byte number;
    public final int length;
    public final byte[] content;

    public Message(byte number, byte[] content)
    {
        this.number=number;
        if(content==null)
            this.length =0;
        else
            this.length=content.length;
        this.content = content;
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
    public Object parse() throws BlockChainObjectParsingException {
            switch (number) {

                case Configuration.MESSAGE_HELLO:
                    return Hello.parse(content);
                case Configuration.MESSAGE_PEER_NODE_REQUEST:
                    return content;
                case Configuration.MESSAGE_HEADER_REQUEST:
                    return RawTranslator.splitBytesToBytesArray(content,Configuration.HASH_LENGTH);
                case Configuration.MESSAGE_BLOCK_REQUEST:
                    return content;
                case Configuration.MESSAGE_PEER_NODE_LIST:
                    return RawTranslator.parseAddresses(content);
                case Configuration.MESSAGE_HEADER_REQUEST_REPLY:
                    if (content[0] == 0)
                        return BlockHeader.parseArray(Arrays.copyOfRange(content,1,content.length));
                    else
                        return Arrays.copyOfRange(content,1,content.length);
                case Configuration.MESSAGE_TRANSACTION:
                    return Transaction.parse(content);
                case Configuration.MESSAGE_BLOCK:
                    return Block.parse(content);
                default:
                    throw new BlockChainObjectParsingException();
            }
        }

    }

