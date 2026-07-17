package MidiControl.ContextModel;

import java.util.HashMap;
import java.util.Map;

public class BankCatalog {

    private final Map<String, BankContext> banks = new HashMap<>();

    public BankCatalog() {
        buildBanks();
    }

    private void buildBanks() {

        // -------------------------
        // Input Channels Bank
        // -------------------------
        BankContext inputs = new BankContext();
        inputs.addFilter(new BankFilter("channel", null, ContextType.CHANNEL));
        banks.put("bank.inputs", inputs);

        // -------------------------
        // Mix Bus Bank
        // -------------------------
        BankContext mixes = new BankContext();
        mixes.addFilter(
            new BankFilter(
                "mix",
                null,
                ContextType.MIX
            )
        );

        mixes.addFilter(
            new BankFilter(
                "aux",
                null,
                ContextType.MIX
            )
        );

        mixes.addFilter(
            new BankFilter(
                "stereo",
                null,
                ContextType.MIX
            )
        );
        banks.put("bank.mixes", mixes);

        // -------------------------
        // Matrix Bank
        // -------------------------
        BankContext matrix = new BankContext();
        matrix.addFilter(new BankFilter("matrix", null, ContextType.MATRIX));
        banks.put("bank.matrix", matrix);

        // -------------------------
        // DCA Bank
        // -------------------------
        BankContext dca = new BankContext();
        dca.addFilter(new BankFilter("dca", null, ContextType.DCA));
        banks.put("bank.dca", dca);

        // Name Banks
        BankContext names = new BankContext();
        names.addFilter(new BankFilter("name", null, ContextType.NAME)); 
        banks.put("bank.names", names);

        
        BankContext stereo = new BankContext();
        stereo.addFilter(
            new BankFilter(
                "stereo",
                null,
                ContextType.STEREO_OUTPUT
            )
        );
        banks.put("bank.stereo", stereo);

    }

    public BankContext getBank(String bankId) {
        return banks.get(bankId);
    }

    public Map<String, BankContext> getAllBanks() {
        return banks;
    }

    public BankContext addBankContext(String key, BankContext toAdd){
        return banks.putIfAbsent(key, toAdd);
    }
}
