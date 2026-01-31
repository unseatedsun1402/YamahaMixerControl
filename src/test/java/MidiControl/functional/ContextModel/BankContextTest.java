package MidiControl.functional.ContextModel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import MidiControl.ContextModel.BankCatalog;
import MidiControl.ContextModel.BankContext;
import MidiControl.ContextModel.BankFilter;

@Tag("unit")
public class BankContextTest {
    @Test
    public void testGetFilter(){
        BankContext testBank = new BankContext();
        BankFilter testFilter = new BankFilter("empty");
        testBank.addFilter(testFilter);
        assertEquals(testFilter, testBank.getFilters().get(0));
    }

    @Test
    public void testPutBank(){
        BankCatalog testCatalog = new BankCatalog();
        BankContext testBank = new BankContext();

        testCatalog.addBankContext("testKey", testBank);
        assertTrue(testCatalog.getAllBanks().values().contains(testBank));
    }

    @Test
    public void testGetBank(){
        BankCatalog testCatalog = new BankCatalog();
        BankContext testBank = new BankContext();

        testCatalog.addBankContext("testKey", testBank);
        assertTrue(testCatalog.getAllBanks().values().contains(testBank));
        assertEquals(testBank,testCatalog.getBank("testKey"));
    }

    @Test
    public void testGetMisses(){
        BankCatalog testCatalog = new BankCatalog();
        BankContext testBank = new BankContext();
        testCatalog.addBankContext("testKey", testBank);
        assertEquals(null,testCatalog.getBank("doesNotExist"));
    }
}
