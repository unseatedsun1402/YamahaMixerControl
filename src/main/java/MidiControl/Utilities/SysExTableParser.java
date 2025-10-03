package MidiControl.Utilities;

// import org.apache.poi.ss.usermodel.*;
// import org.apache.poi.xssf.usermodel.XSSFWorkbook;

// import MidiControl.ChannelMappings.*;
// import MidiControl.Utilities.*;

// import java.io.FileInputStream;
// import java.util.*;

// public class SysExTableParser {

//     public List<SysExMapping> parse(String filePath, String sheetName) {
//         List<SysExMapping> mappings = new ArrayList<>();

//         try (FileInputStream fis = new FileInputStream(filePath);
//              Workbook workbook = new XSSFWorkbook(fis)) {

//             Sheet sheet = workbook.getSheet(sheetName);
//             if (sheet == null) {
//                 System.out.println("Sheet not found: " + sheetName);
//                 return mappings;
//             }

//             for (Row row : sheet) {
//                 if (row.getRowNum() == 0) continue; // Skip header

//                 String elementId = getString(row.getCell(1));
//                 int index = (int) row.getCell(5).getNumericCellValue();
//                 int sourceIndex = (int) row.getCell(3).getNumericCellValue();
//                 int targetIndex = (int) row.getCell(2).getNumericCellValue();
//                 String comment = getString(row.getCell(8));
//                 boolean tx = "O".equalsIgnoreCase(getString(row.getCell(26)));
//                 boolean rx = "O".equalsIgnoreCase(getString(row.getCell(27)));

//                 ChannelType sourceType = ChannelType.INPUT;
//                 ChannelType targetType = ChannelType.MIX;
//                 ControlType controlType = resolveControlType(elementId);

//                 byte[] address = extractAddress(row);
//                 int valueLength = extractValueLength(row);

//                 SysExMapping mapping = new SysExMapping(
//                     elementId,
//                     address,
//                     valueLength,
//                     comment,
//                     tx,
//                     rx,
//                     sourceType,
//                     sourceIndex,
//                     targetType,
//                     targetIndex,
//                     controlType
//                 );
//                 mappings.add(mapping);
//             }
//         }

//         catch (Exception e) {
//             e.printStackTrace();
//         }

//         return mappings;
//     }

//     // 🔹 Utility methods
//     private String getString(Cell cell) {
//         return cell == null ? null : cell.getStringCellValue().trim();
//     }

//     private byte[] extractAddress(Row row) {
//         List<Byte> bytes = new ArrayList<>();
//         for (int i = 8; i <= 16; i++) {
//             Cell cell = row.getCell(i);
//             if (cell != null && cell.getCellType() == CellType.NUMERIC) {
//                 bytes.add((byte) cell.getNumericCellValue());
//             }
//         }
//         return bytes.isEmpty() ? null : toByteArray(bytes);
//     }

//     private int extractValueLength(Row row) {
//         int count = 0;
//         for (int i = 17; i <= 23; i++) {
//             Cell cell = row.getCell(i);
//             if (cell != null) count++;
//         }
//         return count;
//     }

//     private byte[] toByteArray(List<Byte> list) {
//         byte[] array = new byte[list.size()];
//         for (int i = 0; i < list.size(); i++) array[i] = list.get(i);
//         return array;
//     }

//     // 🔹 Mapping logic
//     private ChannelType resolveChannelType(String element) {
//         if (element == null) return ChannelType.UNKNOWN;
//         element = element.trim().toUpperCase();

//         if (element.startsWith("kCH")) return ChannelType.INPUT;
//         if (element.startsWith("kMIX")) return ChannelType.MIX;
//         if (element.startsWith("kST")) return ChannelType.STEREO;
//         if (element.startsWith("kFX")) return ChannelType.EFFECT;
//         if (element.startsWith("kMTR")) return ChannelType.MATRIX;
//         if (element.startsWith("kGPI")) return ChannelType.GPI;
//         if (element.startsWith("kMON")) return ChannelType.MONITOR;
//         if (element.startsWith("kUSER")) return ChannelType.USER_DEFINED;
//         if (element.startsWith("kREMOTE")) return ChannelType.REMOTE;
//         return ChannelType.UNKNOWN;
//     }

//     private ControlType resolveControlType(String functionName) {
//         if (functionName == null) return ControlType.REMOTE;
//         functionName = functionName.toLowerCase();

//         if (functionName.contains("fader")) return ControlType.FADER;
//         if (functionName.contains("pan")) return ControlType.PAN;
//         if (functionName.contains("mute")) return ControlType.MUTE;
//         if (functionName.contains("eq")) return ControlType.EQ;
//         if (functionName.contains("send")) return ControlType.EFFECT_SEND;
//         if (functionName.contains("direct")) return ControlType.DIRECT_OUT;
//         if (functionName.contains("assign")) return ControlType.SLOT_ASSIGN;
//         if (functionName.contains("config")) return ControlType.MACHINE_CONFIG;
//         if (functionName.contains("gpi")) return ControlType.GPI;
//         if (functionName.contains("comment") || functionName.contains("fade")) return ControlType.USER_DEFINED;
//         return ControlType.REMOTE;
//     }

//     private void applyControl(SysExMapping mapping, int value) {
//     // Channel target = channelModel.get(mapping.targetChannelType(), mapping.targetChannelIndex());
//     // Channel source = channelModel.get(mapping.sourceChannelType(), mapping.sourceChannelIndex());

//     // switch (mapping.controlType()) {
//     //     case FADER -> target.setSendLevel(source, FaderMapper.midiToDb(value));
//     //     case PAN   -> target.setPan(source, PanMapper.midiValueToPan(value));
//     //     case MUTE  -> target.setMute(source, value != 0);
//     //     // etc.

//     // TODO: Implement other control types
//     }
// }

