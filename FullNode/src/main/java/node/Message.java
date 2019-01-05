package node;

import blockchain.Status;
import blockchain.block.Block;
import blockchain.block.BlockHeader;
import blockchain.block.transaction.Transaction;
import blockchain.utility.RawTranslator;
import config.Configuration;
import exception.BlockChainObjectParsingException;

public class Message{
    public final byte number;
    public final int length;
    public final byte[] content;

    public Message(byte number,byte[] content)
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

                case Configuration.MESSAGE_STATUS:
                    return Status.parse(content);
                case Configuration.MESSAGE_PEER_NODE_REQUEST:
                    return content;
                case Configuration.MESSAGE_HEADER_REQUEST:
                    return RawTranslator.splitBytesToBytesArray(content,Configuration.HASH_LENGTH);
                case Configuration.MESSAGE_BLOCK_REQUEST:
                    return content;
                case Configuration.MESSAGE_PEER_NODE_LIST:
                    return RawTranslator.parseAddresses(content);
                case Configuration.MESSAGE_HEADER_LIST:
                    return BlockHeader.parseArray(content);
                case Configuration.MESSAGE_TRANSACTION:
                    return Transaction.parse(content);
                case Configuration.MESSAGE_BLOCK:
                    return Block.parse(content);
                default:
                    throw new BlockChainObjectParsingException();
            }
        }

    }

