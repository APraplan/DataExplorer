import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * 
 */

/**
 * @author brueg
 *
 */
public class PortmonReader {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			//BufferedReader br = new BufferedReader( new InputStreamReader( new FileInputStream("c:\\Programs\\Sysinternals\\HoTTManager_Connect_LogData_cappuccino_files.LOG")));
			BufferedReader br = new BufferedReader( new InputStreamReader( new FileInputStream("c:\\Programs\\Sysinternals\\Modelldatareadmx16.LOG")));
			String line;
			StringBuilder sb = new StringBuilder();
			while ((line = br.readLine()) != null) {
				if (line.contains("SUCCESS	Length")) {
					String[] hexBytes = line.split(":")[1].trim().split(" ");
					sb = new StringBuilder();
					if (line.contains("IRP_MJ_WRITE")) sb.append("write : ");
					else sb.append("read  : ");
					for (String hex : hexBytes) {
						sb.append(hex).append(" ");
					}
					sb.append("\n");
					if (line.contains("IRP_MJ_WRITE")) sb.append("write : ");
					else sb.append("read  : ");
					for (String hex : hexBytes) {
						sb.append(String.format("%c",Integer.parseInt(hex, 16))).append(" ");
					}
					System.out.println(sb.toString());
//					if (hexBytes.length > 10 && line.contains("IRP_MJ_WRITE")) {
//						byte[] bytes = new byte[hexBytes.length-5]; //0x00, cntUp, cntDwn, CRC1, CRC2
//						for (int i = 0; i < bytes.length; i++) {
//							bytes[i] = (byte) Integer.parseInt(hexBytes[i+3], 16);
//						}
//						System.out.println(String.format("CRC16      = %04X", Checksum.CRC16(bytes, 0)));
//						System.out.println(String.format("CRC16CCITT = %04X", Checksum.CRC16CCITT(bytes, 0)));
//					}
					
				}
			}
			br.close();
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}

	}

}
