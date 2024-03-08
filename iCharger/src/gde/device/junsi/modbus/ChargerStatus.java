package gde.device.junsi.modbus;

import java.util.ArrayList;
import java.util.List;

import gde.io.DataParser;

public class ChargerStatus {
	
	enum ErrorStatus
	{
		HC_OK,
		HC_CHECK_WAIT,
		HC_ERR_VIN_OVER,
		HC_ERR_VIN_LOW,
		HC_ERR_VOUT_OVER,
		HC_ERR_BAT_LOW,
		HC_ERR_BAT_OVER,
		HC_ERR_COUT_OVER,
		HC_ERR_COUT_LOW,
		HC_ERR_CIN_OVER,
		HC_ERR_CIN_LOW,	 //10
		HC_ERR_TEMP_OVER, 
		HC_ERR_TEMP_LOW,   
		HC_ERR_CHECK,
		HC_ERR_GND,
		HC_ERR_POLARITY,
		HC_ERR_CONTROL,
		HC_ERR_TIMEOVER,
		HC_ERR_CAP,
		HC_ERR_TEMP,
		HC_ERR_CURRENT_NULL,//20
		HC_ERR_CELLS_LINE,		
		HC_ERR_CELLS_LOW,	
		HC_ERR_CELLS_OVER,
		HC_ERR_CELLS_L_VOUT,
		HC_ERR_CELLS_O_VOUT,
		HC_ERR_CELLS_SET,
		HC_ERR_CELLS_SET_LOW,
		HC_ERR_CELLS_SET_OVER,
		HC_ERR_BAL_PORT,
		HC_ERR_NO_BAL,	//30
		HC_ERR_CELLS_AUTO,	 
		HC_ERR_AWD,
		HC_ERR_SYN_IMBAL,
		HC_ERR_REG_NO_LOAD,
		HC_ERR_CH_OCCUPIED,
		HC_ERR_REG_CAP,
		HC_NULL;
		

		public static String[] VALUES = getValues();

		private static String[] getValues() {
			List<String> list = new ArrayList<String>();
			for (ErrorStatus element : values()) {
				list.add(element.name());
			}
			return list.toArray(new String[0]);
		}

	}

	
	int timeStamp;
	int outputPower;
	short outputCurrent;
	short inputVoltage;
	short outputVoltage;
	int outputCapacity;
	short internalTemperature;
	short externalTemperature;
	
	class CellVoltages {
		short cellVoltage01;
		short cellVoltage02;
		short cellVoltage03;
		short cellVoltage04;
		short cellVoltage05;
		short cellVoltage06;
		short cellVoltage07;
		short cellVoltage08;
		short cellVoltage09;
		short cellVoltage10;
		short cellVoltage11;
		short cellVoltage12;
		short cellVoltage13;
		short cellVoltage14;
		short cellVoltage15;
		short cellVoltage16;
		
		CellVoltages(final byte[] cellVoltageBuffer) {
			this.cellVoltage01 = DataParser.parse2Short(cellVoltageBuffer[0], cellVoltageBuffer[1]);
			this.cellVoltage02 = DataParser.parse2Short(cellVoltageBuffer[2], cellVoltageBuffer[3]);
			this.cellVoltage03 = DataParser.parse2Short(cellVoltageBuffer[4], cellVoltageBuffer[5]);
			this.cellVoltage04 = DataParser.parse2Short(cellVoltageBuffer[6], cellVoltageBuffer[7]);
			this.cellVoltage05 = DataParser.parse2Short(cellVoltageBuffer[8], cellVoltageBuffer[9]);
			this.cellVoltage06 = DataParser.parse2Short(cellVoltageBuffer[10], cellVoltageBuffer[11]);
			this.cellVoltage07 = DataParser.parse2Short(cellVoltageBuffer[12], cellVoltageBuffer[13]);
			this.cellVoltage08 = DataParser.parse2Short(cellVoltageBuffer[14], cellVoltageBuffer[15]);
			this.cellVoltage09 = DataParser.parse2Short(cellVoltageBuffer[16], cellVoltageBuffer[17]);
			this.cellVoltage10 = DataParser.parse2Short(cellVoltageBuffer[18], cellVoltageBuffer[19]);
			this.cellVoltage11 = DataParser.parse2Short(cellVoltageBuffer[20], cellVoltageBuffer[21]);
			this.cellVoltage12 = DataParser.parse2Short(cellVoltageBuffer[22], cellVoltageBuffer[23]);
			this.cellVoltage13 = DataParser.parse2Short(cellVoltageBuffer[24], cellVoltageBuffer[25]);
			this.cellVoltage14 = DataParser.parse2Short(cellVoltageBuffer[26], cellVoltageBuffer[27]);
			this.cellVoltage15 = DataParser.parse2Short(cellVoltageBuffer[28], cellVoltageBuffer[29]);
			this.cellVoltage16 = DataParser.parse2Short(cellVoltageBuffer[30], cellVoltageBuffer[31]);
		}
	}
	CellVoltages cellVoltages;
	
	class CellBalances {
		byte cellBalance01;
		byte cellBalance02;
		byte cellBalance03;
		byte cellBalance04;
		byte cellBalance05;
		byte cellBalance06;
		byte cellBalance07;
		byte cellBalance08;
		byte cellBalance09;
		byte cellBalance10;
		byte cellBalance11;
		byte cellBalance12;
		byte cellBalance13;
		byte cellBalance14;
		byte cellBalance15;
		byte cellBalance16;
		
		CellBalances(final byte[] cellBalanceBuffer) {
			this.cellBalance01 = cellBalanceBuffer[0];
			this.cellBalance02 = cellBalanceBuffer[1];
			this.cellBalance03 = cellBalanceBuffer[2];
			this.cellBalance04 = cellBalanceBuffer[3];
			this.cellBalance05 = cellBalanceBuffer[4];
			this.cellBalance06 = cellBalanceBuffer[5];
			this.cellBalance07 = cellBalanceBuffer[6];
			this.cellBalance08 = cellBalanceBuffer[7];
			this.cellBalance09 = cellBalanceBuffer[8];
			this.cellBalance10 = cellBalanceBuffer[9];
			this.cellBalance11 = cellBalanceBuffer[10];
			this.cellBalance12 = cellBalanceBuffer[11];
			this.cellBalance13 = cellBalanceBuffer[12];
			this.cellBalance14 = cellBalanceBuffer[13];
			this.cellBalance15 = cellBalanceBuffer[14];
			this.cellBalance16 = cellBalanceBuffer[15];
		}
	}
	CellBalances cellBalances;
	
	class CellResistances {

		short cellResistance01;
		short cellResistance02;
		short cellResistance03;
		short cellResistance04;
		short cellResistance05;
		short cellResistance06;
		short cellResistance07;
		short cellResistance08;
		short cellResistance09;
		short cellResistance10;
		short cellResistance11;
		short cellResistance12;
		short cellResistance13;
		short cellResistance14;
		short cellResistance15;
		short cellResistance16;
		
		CellResistances(final byte[] cellResistanceBuffer) {
			this.cellResistance01 = DataParser.parse2Short(cellResistanceBuffer[0], cellResistanceBuffer[1]);
			this.cellResistance02 = DataParser.parse2Short(cellResistanceBuffer[2], cellResistanceBuffer[3]);
			this.cellResistance03 = DataParser.parse2Short(cellResistanceBuffer[4], cellResistanceBuffer[5]);
			this.cellResistance04 = DataParser.parse2Short(cellResistanceBuffer[6], cellResistanceBuffer[7]);
			this.cellResistance05 = DataParser.parse2Short(cellResistanceBuffer[8], cellResistanceBuffer[9]);
			this.cellResistance06 = DataParser.parse2Short(cellResistanceBuffer[10], cellResistanceBuffer[11]);
			this.cellResistance07 = DataParser.parse2Short(cellResistanceBuffer[12], cellResistanceBuffer[13]);
			this.cellResistance08 = DataParser.parse2Short(cellResistanceBuffer[14], cellResistanceBuffer[15]);
			this.cellResistance09 = DataParser.parse2Short(cellResistanceBuffer[16], cellResistanceBuffer[17]);
			this.cellResistance10 = DataParser.parse2Short(cellResistanceBuffer[18], cellResistanceBuffer[19]);
			this.cellResistance11 = DataParser.parse2Short(cellResistanceBuffer[20], cellResistanceBuffer[21]);
			this.cellResistance12 = DataParser.parse2Short(cellResistanceBuffer[22], cellResistanceBuffer[23]);
			this.cellResistance13 = DataParser.parse2Short(cellResistanceBuffer[24], cellResistanceBuffer[25]);
			this.cellResistance14 = DataParser.parse2Short(cellResistanceBuffer[26], cellResistanceBuffer[27]);
			this.cellResistance15 = DataParser.parse2Short(cellResistanceBuffer[28], cellResistanceBuffer[29]);
			this.cellResistance16 = DataParser.parse2Short(cellResistanceBuffer[30], cellResistanceBuffer[31]);
		}
	}
	CellResistances cellResistances;

	short cellTotalResistance;
	short effectiveResistance;
	short cycleCount;
	short controlStatus;
	short runStatus;
	byte errorStatusHigh;
	byte errorStatusLow;
	short dialogBoxId;
	
	class CellCapacities {
		short cellCapacity01;
		short cellCapacity02;
		short cellCapacity03;
		short cellCapacity04;
		short cellCapacity05;
		short cellCapacity06;
		short cellCapacity07;
		short cellCapacity08;
		short cellCapacity09;
		short cellCapacity10;
		short cellCapacity11;
		short cellCapacity12;
		short cellCapacity13;
		short cellCapacity14;
		short cellCapacity15;
		short cellCapacity16;
		
		CellCapacities(final byte[] cellCapacityBuffer) {
			this.cellCapacity01 = DataParser.parse2Short(cellCapacityBuffer[0], cellCapacityBuffer[1]);
			this.cellCapacity02 = DataParser.parse2Short(cellCapacityBuffer[2], cellCapacityBuffer[3]);
			this.cellCapacity03 = DataParser.parse2Short(cellCapacityBuffer[4], cellCapacityBuffer[5]);
			this.cellCapacity04 = DataParser.parse2Short(cellCapacityBuffer[6], cellCapacityBuffer[7]);
			this.cellCapacity05 = DataParser.parse2Short(cellCapacityBuffer[8], cellCapacityBuffer[9]);
			this.cellCapacity06 = DataParser.parse2Short(cellCapacityBuffer[10], cellCapacityBuffer[11]);
			this.cellCapacity07 = DataParser.parse2Short(cellCapacityBuffer[12], cellCapacityBuffer[13]);
			this.cellCapacity08 = DataParser.parse2Short(cellCapacityBuffer[14], cellCapacityBuffer[15]);
			this.cellCapacity09 = DataParser.parse2Short(cellCapacityBuffer[16], cellCapacityBuffer[17]);
			this.cellCapacity10 = DataParser.parse2Short(cellCapacityBuffer[18], cellCapacityBuffer[19]);
			this.cellCapacity11 = DataParser.parse2Short(cellCapacityBuffer[20], cellCapacityBuffer[21]);
			this.cellCapacity12 = DataParser.parse2Short(cellCapacityBuffer[22], cellCapacityBuffer[23]);
			this.cellCapacity13 = DataParser.parse2Short(cellCapacityBuffer[24], cellCapacityBuffer[25]);
			this.cellCapacity14 = DataParser.parse2Short(cellCapacityBuffer[26], cellCapacityBuffer[27]);
			this.cellCapacity15 = DataParser.parse2Short(cellCapacityBuffer[28], cellCapacityBuffer[29]);
			this.cellCapacity16 = DataParser.parse2Short(cellCapacityBuffer[30], cellCapacityBuffer[31]);
		}
	}
	CellCapacities cellCapacities;
	
	public ChargerStatus(final byte[] chargerStatusBuffer) {
		this.timeStamp = DataParser.parse2Int(chargerStatusBuffer, 0);
		this.outputPower = DataParser.parse2Int(chargerStatusBuffer, 4);
		this.outputCurrent = DataParser.parse2Short(chargerStatusBuffer[8], chargerStatusBuffer[9]);
		this.inputVoltage = DataParser.parse2Short(chargerStatusBuffer[10], chargerStatusBuffer[11]);
		this.outputVoltage = DataParser.parse2Short(chargerStatusBuffer[12], chargerStatusBuffer[13]);
		this.outputCapacity = DataParser.parse2Int(chargerStatusBuffer, 14);
		this.internalTemperature = DataParser.parse2Short(chargerStatusBuffer[18], chargerStatusBuffer[19]);
		this.externalTemperature = DataParser.parse2Short(chargerStatusBuffer[20], chargerStatusBuffer[21]);
		byte[] chargerVoltageBuffer = new byte[32];
		System.arraycopy(chargerStatusBuffer, 22, chargerVoltageBuffer, 0, chargerVoltageBuffer.length);
		this.cellVoltages = new CellVoltages(chargerVoltageBuffer);
		byte[] cellBalanceBuffer = new byte[16];
		System.arraycopy(chargerStatusBuffer, 54, cellBalanceBuffer, 0, cellBalanceBuffer.length);
		this.cellBalances = new CellBalances(cellBalanceBuffer);
		byte[] cellResistanceBuffer = new byte[32];
		System.arraycopy(chargerStatusBuffer, 70, cellResistanceBuffer, 0, cellResistanceBuffer.length);
		this.cellResistances = new CellResistances(cellResistanceBuffer);
		this.cellTotalResistance = DataParser.parse2Short(chargerStatusBuffer[102], chargerStatusBuffer[103]);
		this.effectiveResistance = DataParser.parse2Short(chargerStatusBuffer[104], chargerStatusBuffer[105]);
		this.cycleCount = DataParser.parse2Short(chargerStatusBuffer[106], chargerStatusBuffer[107]);
		this.controlStatus = DataParser.parse2Short(chargerStatusBuffer[108], chargerStatusBuffer[109]);
		this.runStatus = DataParser.parse2Short(chargerStatusBuffer[110], chargerStatusBuffer[111]);
		this.errorStatusLow = chargerStatusBuffer[112];
		this.errorStatusHigh = chargerStatusBuffer[113];
		this.dialogBoxId = DataParser.parse2Short(chargerStatusBuffer[114], chargerStatusBuffer[115]);
		byte[] cellCapacityBuffer = new byte[32];
		System.arraycopy(chargerStatusBuffer, 116, cellCapacityBuffer, 0, cellCapacityBuffer.length);
		this.cellCapacities = new CellCapacities(cellCapacityBuffer);
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder().append("\n");
		sb.append(String.format("outputPower \t\t = %d", outputPower)).append("\n");
		sb.append(String.format("inputVoltage \t\t = %d", inputVoltage)).append("\n");
		sb.append(String.format("outputVoltage \t\t = %d", outputVoltage)).append("\n");
		sb.append(String.format("outputCapacity \t\t = %d", outputCapacity)).append("\n");
		sb.append(String.format("internalTemperature \t = %d", internalTemperature)).append("\n");
		sb.append(String.format("externalTemperature \t = %d", externalTemperature)).append("\n");
		sb.append(String.format("cellTotalResistance \t = %d", cellTotalResistance)).append("\n");
		sb.append(String.format("effectiveResistance \t = %d", effectiveResistance)).append("\n");
		sb.append(String.format("cycleCount \t\t = %d", cycleCount)).append("\n");
		sb.append(String.format("controlStatus \t\t = %d", controlStatus)).append("\n");
		sb.append(String.format("runStatus \t\t = %d", runStatus)).append("\n");
		sb.append(String.format("errorStatus \t\t = %s", this.getStatusInfo())).append("\n");
		sb.append(String.format("dialogBoxId \t\t = %d", dialogBoxId)).append("\n");
		return sb.toString();
	}
	
	/**
	 * @return the size in byte
	 */
	public static int getSize() {
		return 87*2;
	}
	/**
	 * @return device status info direct from ChargerInfo.Status
	 */
	public String getStatusInfo() {
		try {
			return String.format("errorStatus \t\t\t= %02d%02d (%s)", this.errorStatusHigh, this.errorStatusLow, ErrorStatus.VALUES[this.errorStatusHigh]);
		}
		catch (Exception e) {
			e.printStackTrace();
			return "unknown";
		}
	}

	/**
	 * @return the timeStamp
	 */
	public int getTimeStamp() {
		return timeStamp;
	}

	/**
	 * @return the outputPower
	 */
	public int getOutputPower() {
		return outputPower;
	}

	/**
	 * @return the inputVoltage
	 */
	public short getInputVoltage() {
		return inputVoltage;
	}

	/**
	 * @return the outputVoltage
	 */
	public short getOutputVoltage() {
		return outputVoltage;
	}

	/**
	 * @return the outputCapacity
	 */
	public int getOutputCapacity() {
		return outputCapacity;
	}

	/**
	 * @return the internalTemperature
	 */
	public short getInternalTemperature() {
		return internalTemperature;
	}

	/**
	 * @return the externalTemperature
	 */
	public short getExternalTemperature() {
		return externalTemperature;
	}

	/**
	 * @return the cellVoltages
	 */
	public CellVoltages getCellVoltages() {
		return cellVoltages;
	}

	/**
	 * @return the cellBalances
	 */
	public CellBalances getCellBalances() {
		return cellBalances;
	}

	/**
	 * @return the cellResistances
	 */
	public CellResistances getCellResistances() {
		return cellResistances;
	}

	/**
	 * @return the cellTotalResistance
	 */
	public short getCellTotalResistance() {
		return cellTotalResistance;
	}

	/**
	 * @return the effectiveResistance
	 */
	public short getEffectiveResistance() {
		return effectiveResistance;
	}

	/**
	 * @return the cycleCount
	 */
	public short getCycleCount() {
		return cycleCount;
	}

	/**
	 * @return the controlStatus
	 */
	public short getControlStatus() {
		return controlStatus;
	}

	/**
	 * @return the runStatus
	 */
	public short getRunStatus() {
		return runStatus;
	}

	/**
	 * @return the errorStatus
	 */
	public short getErrorStatus() {
		return Short.valueOf(this.getStatusInfo());
	}

	/**
	 * @return the dialogBoxId
	 */
	public short getDialogBoxId() {
		return dialogBoxId;
	}

	/**
	 * @return the cellCapacities
	 */
	public CellCapacities getCellCapacities() {
		return cellCapacities;
	}

}
