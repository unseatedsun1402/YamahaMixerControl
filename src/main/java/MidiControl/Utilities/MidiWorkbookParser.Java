package MidiControl.Utilities;

public class MidiWorkbookParser {

    public void parseWorkbook(String filePath) {
        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            for (Sheet sheet : workbook) {
                String name = sheet.getSheetName().toLowerCase();

                if (name.contains("CH TABLE")) {
                    new ChannelTableParser().parse(sheet);
                } else if (name.contains("Parameter Change List")) {
                    new NrpnTableParser().parse(sheet);
                } else if (name.contains("Parameter Change List")) {
                    new SysExTableParser().parse(sheet);
                } else if (name.contains("meter") && name.contains("step")) {
                    new MeterStepParser().parse(sheet);
                } else if (name.contains("Meter ID Assign")) {
                    new MeterDataParser().parse(sheet);
                } else if (name.contains("History")) {
                    System.out.println("Skipping release notes sheet: " + name);
                } else {
                    System.out.println("Unknown sheet: " + name);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
