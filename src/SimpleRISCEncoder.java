import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.stream.IntStream;

public class SimpleRISCEncoder {

	public static final String[] operands = { "add", "sub", "mul", "div", "mod", "cmp", "and", "or", "not", "mov",
			"lsl", "lsr", "asr", "nop", "ld", "st", "beq", "bgt", "b", "call", "ret" };
	public static final String[] zeroArgumentOperands = { "ret", "nop" };
	public static final String[] oneArgumentOperands = { "call", "b", "beq", "bgt" };
	public static final String[] twoArgumentOperands = { "add", "sub", "mul", "div", "mod", "and", "or", "lsl", "lsr",
			"asr" };

	public static void main(String[] args) {
		// =================================================
		// Operand OPCode
		// =================================================
		Map<String, Integer> operandOpcodeMap = new HashMap<>();
		IntStream.range(0, operands.length).forEach(i -> operandOpcodeMap.put(operands[i], i));
		// =================================================
		// IO
		// =================================================
		Scanner scanner = null;
		try {
			scanner = new Scanner(new FileReader(args[0]));
		} catch (FileNotFoundException e) {
			System.out.println("Input File Not Found");
		}
		FileWriter fileWriter = null;
		try {
			fileWriter = new FileWriter(args[1]);
		} catch (IOException e) {
			System.out.println("Output File Not Found");
		}
		// =================================================
		// Maps
		// =================================================
		Map<String, Integer> labelPCMap = new HashMap<>();
		Map<Integer, String> reverselabelPCMap = new HashMap<>();
		TreeMap<Integer, String> nonLabelStatementsPCMap = new TreeMap<>();
		// =================================================
		int PC = 0;
		int maxLineLength = 0;
		while (scanner.hasNext()) {
			String line = scanner.nextLine();
			line = line.replaceFirst("@.+", "").trim();
			if (line.length() > 0) {
				maxLineLength = Math.max(maxLineLength, line.length());
				if (line.contains(":")) {
					labelPCMap.put(line.substring(0, line.indexOf(':')), PC);
					reverselabelPCMap.put(PC, line.substring(0, line.indexOf(':')));
				} else {
					nonLabelStatementsPCMap.put(PC, line);
					PC += 4;
				}
			}
		}
		maxLineLength++;
		// =================================================
		for (Entry<Integer, String> statement : nonLabelStatementsPCMap.entrySet()) {
			// =================================================
			int currentPC = statement.getKey();
			String line = statement.getValue();
			long instruction = 0;
			// =================================================
			String[] words = Arrays.stream(line.split("[\\s,\\[\\]]+")).filter(x -> x.length() > 0)
					.toArray(size -> new String[size]);
			// =================================================
			char lastCharacterOperand = words[0].charAt(words[0].length() - 1);
			// Modifier Bits
			if (lastCharacterOperand == 'h')
				instruction += 1 << 17;
			if (lastCharacterOperand == 'u')
				instruction += 1 << 16;
			// =================================================
			long opcode = -1;
			if (operandOpcodeMap.containsKey(words[0]))
				opcode = operandOpcodeMap.get(words[0]);
			else
				System.out.println("Invalid operand \"" + words[0] + "\".");
			instruction += opcode << 27;
			// =================================================
			if (Arrays.stream(zeroArgumentOperands).anyMatch(x -> x.equals(words[0]))) {
				// Nothing To Do
			} else if (Arrays.stream(oneArgumentOperands).anyMatch(x -> x.equals(words[0]))) {
				int offset = 0;
				if (labelPCMap.containsKey(words[1])) {
					// =================================================
					offset = labelPCMap.get(words[1]) - currentPC;
					String offsetString = Integer.toBinaryString(offset);
					if (offset < 0)
						offsetString = offsetString.substring(offsetString.length() - 27);
					instruction += Integer.parseInt(offsetString, 2);
					// =================================================
				} else
					System.out.println("Label \"" + words[1] + "\" Not Found");
			} else if (Arrays.stream(twoArgumentOperands).anyMatch(x -> x.equals(words[0]))) {
				// =================================================
				int I = words[3].contains("r") ? 0 : 1;
				instruction += I << 26;
				// =================================================
				int rd = registerNumber(words[1]);
				instruction += rd << 22;
				// =================================================
				int rs1 = registerNumber(words[2]);
				instruction += rs1 << 18;
				// =================================================
				if (I == 0) {
					int rs2 = registerNumber(words[3]);
					instruction += rs2 << 14;
				} else {
					instruction += Integer.parseInt(words[3]);
				}
				// =================================================
			} else if (words[0].equals("cmp") || words[0].equals("not") || words[0].equals("mov")) {
				// =================================================
				int I = words[2].contains("r") ? 0 : 1;
				instruction += I << 26;
				// =================================================
				int rx = registerNumber(words[1]);
				if (words[0].equals("cmp")) {
					instruction += rx << 18;
				} else {
					instruction += rx << 22;
				}
				// =================================================
				if (I == 0) {
					int rs2 = registerNumber(words[2]);
					instruction += rs2 << 14;
				} else {
					instruction += Integer.parseInt(words[2]);
				}
				// =================================================
			} else /* ld, st */ {
				// =================================================
				int I = words[2].contains("r") ? 0 : 1;
				instruction += I << 26;
				// =================================================
				int rd = registerNumber(words[1]);
				instruction += rd << 22;
				// =================================================
				int rs1 = registerNumber(words[3]);
				instruction += rs1 << 18;
				// =================================================
				if (I == 0) {
					int rs2 = registerNumber(words[2]);
					instruction += rs2 << 14;
				} else {
					instruction += Integer.parseInt(words[2]);
				}
				// =================================================
			}
			// =================================================
			try {
				fileWriter.write(String.format("0x%02X 0x%08X\n", currentPC, instruction));
			} catch (IOException error) {
				error.printStackTrace();
			}
			// =================================================
			System.out.printf("[%03d] : [\"%-" + maxLineLength + "s] [0x%02X 0x%08X]\n", currentPC, line + "\"",
					currentPC, instruction);
			if (reverselabelPCMap.containsKey(currentPC + 4)) {
				System.out.printf("[   ] : [\"%-" + maxLineLength + "s]\n",
						reverselabelPCMap.get(currentPC + 4) + "\"");
			}
			// =================================================
		}
		// =================================================
		scanner.close();
		try {
			fileWriter.close();
		} catch (IOException error) {
			error.printStackTrace();
		}
		// =================================================
	}

	private static int registerNumber(String rx) {
		// =================================================
		if (rx.equals("sp"))
			return 14;
		// =================================================
		else if (rx.equals("ra"))
			return 15;
		// =================================================
		if (rx.contains("r"))
			return Integer.parseInt(rx.substring(1));
		// =================================================
		System.out.println("Such a register not found");
		return -1;
	}

}
