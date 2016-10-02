import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.IntStream;

public class SimpleRISCEncoder {

	public static final String[] op1 = { "add", "sub", "mul", "div", "mod", "cmp", "and", "or", "not", "mov", "lsl",
			"lsr", "asr", "nop", "ld", "st", "beq", "bgt", "b", "call", "ret" };
	public static final String[] zai = { "ret", "nop" };
	public static final String[] oai = { "call", "b", "beq", "bgt" };
	public static final String[] tai = { "add", "sub", "mul", "div", "mod", "and", "or", "lsl", "lsr", "asr" };

	public static void main(String[] args) throws Exception {
		Map<String, Integer> op2 = new HashMap<>();
		IntStream.range(0, op1.length).forEach(i -> op2.put(op1[i], i));
		Scanner sc = new Scanner(new FileReader("input.txt"));
		List<String> instructions = new ArrayList<>();
		FileWriter fw = new FileWriter("/home/aditya/workspace/C++/CSL211/simple RISC emulator/test/test.mem");
		while (sc.hasNext()) {
			instructions.add(sc.nextLine());
		}
		int pc = 0;
		for (int i = 0; i < instructions.size(); i++) {
			String line = instructions.get(i);
			if (line.contains(":")) {
				System.out.println(line);
				continue;
			}
			long instruction = 0;
			String[] words = Arrays.stream(line.split("[\\s,\\[\\]]+")).filter(x -> x.length() > 0)
					.toArray(size -> new String[size]);
			char lc = words[0].charAt(words[0].length() - 1);
			if (lc == 'h')
				instruction += 1 << 17;
			if (lc == 'u')
				instruction += 1 << 16;
			long opcode = op2.get(words[0]);
			instruction += opcode << 27;
			if (Arrays.stream(zai).anyMatch(x -> x.equals(words[0]))) {

			} else if (Arrays.stream(oai).anyMatch(x -> x.equals(words[0]))) {
				int off = 0;
				for (int j = 0; j < instructions.size(); j++) {
					if (instructions.get(j).contains(words[1] + ":")) {
						for (int x = (j > i) ? i + 1 : i - 1; (j > i) ? x <= j : x >= j; x += (j > i) ? 1 : -1) {
							for (String st : op1)
								if (instructions.get(x).contains(st) && !instructions.get(x).contains(":")) {
									off += (j > i) ? 1 : -1;
									break;
								}
						}
						if (j > i)
							off++;
						break;
					}
				}
				String offs = Integer.toBinaryString(off);
				if (off < 0)
					offs = offs.substring(offs.length() - 27);
				instruction += Integer.parseInt(offs, 2);
			} else if (Arrays.stream(tai).anyMatch(x -> x.equals(words[0]))) {
				int I = words[3].contains("r") ? 0 : 1;
				instruction += I << 26;
				int rd = reg(words[1]);
				instruction += rd << 22;
				int rs1 = reg(words[2]);
				instruction += rs1 << 18;
				if (I == 0) {
					int rs2 = reg(words[3]);
					instruction += rs2 << 14;
				} else {
					instruction += Integer.parseInt(words[3]);
				}
			} else if (words[0].equals("cmp") || words[0].equals("not") || words[0].equals("mov")) {
				int I = words[2].contains("r") ? 0 : 1;
				instruction += I << 26;
				int rx = reg(words[1]);
				if (words[0].equals("cmp")) {
					instruction += rx << 18;
				} else {
					instruction += rx << 22;
				}
				if (I == 0) {
					int rs2 = reg(words[2]);
					instruction += rs2 << 14;
				} else {
					instruction += Integer.parseInt(words[2]);
				}
			} else {
				int I = words[2].contains("r") ? 0 : 1;
				instruction += I << 26;
				int rd = reg(words[1]);
				instruction += rd << 22;
				int rs1 = reg(words[3]);
				instruction += rs1 << 18;
				if (I == 0) {
					int rs2 = reg(words[2]);
					instruction += rs2 << 14;
				} else {
					instruction += Integer.parseInt(words[2]);
				}
			}
			String ins = Long.toHexString(instruction).toUpperCase();
			while (ins.length() < 8)
				ins = "0" + ins;
			fw.write("0x" + Integer.toHexString(pc).toUpperCase() + " 0x" + ins + "\n");
			System.out.println(pc + ":" + line + " -> 0x" + Integer.toHexString(pc).toUpperCase() + " 0x" + ins);
			pc += 4;
		}
		sc.close();
		fw.close();
	}

	private static int reg(String rx) {
		if (rx.equals("sp"))
			return 14;
		else if (rx.equals("ra"))
			return 15;
		if (rx.contains("r"))
			return Integer.parseInt(rx.substring(1));
		throw new RuntimeException();
	}
}
