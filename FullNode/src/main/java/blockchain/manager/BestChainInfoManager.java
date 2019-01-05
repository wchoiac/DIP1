package blockchain.manager;

import blockchain.Status;
import config.Configuration;
import exception.BlockChainObjectParsingException;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;

public class BestChainInfoManager {


    public static void save(Status status) throws IOException {

        if (!Configuration.BLOCKCHAIN_DATA_FOLDER.exists()) {
            Configuration.BLOCKCHAIN_DATA_FOLDER.mkdirs();
        }
        try (FileOutputStream os = new FileOutputStream(Configuration.BEST_CHAIN_FILE);){
            os.write(status.getRaw());
        }
    }

    public static Status load() throws BlockChainObjectParsingException, IOException {

        if (!Configuration.BEST_CHAIN_FILE.exists())
            throw new BlockChainObjectParsingException();


        byte[] bestChainInfoAllBytes = Files.readAllBytes(Configuration.BEST_CHAIN_FILE.toPath());
        return Status.parse(bestChainInfoAllBytes);

    }

}
