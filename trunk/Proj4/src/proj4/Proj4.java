/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package proj4;

import java.io.*;
import java.math.BigInteger;

/**
 *
 * @author e5008222
 */
public class Proj4 {

    protected int _sicOpsTableSize = 200;
    protected int _symbolTableSize = 50;
    protected int _defaultStartPosition = 100;
    private boolean truncateLiterals = true;
    private int maxLiteralSize = 6;
    private HashTable _sicOps = new HashTable(_sicOpsTableSize);
    private HashTable _preprocs = new HashTable(20);
    private HashTable _symbolTable = new HashTable(_symbolTableSize);
    private SourceCodeLine[] _src = new SourceCodeLine[200];
    private HashTable _literals = new HashTable(3);
    private HashTable _regOps = new HashTable(6);
    private HashTable _registers = new HashTable(10);
    static String _projectName = "p4";
    static int startPosition = 0;
    static int pc = 0;
    static int base = -1;
    static int currentPosition = 0;
    static int currentLineNumber = 0;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        if (args.length > 0) {
            String opFileName = "SicOps.dat";
            String inputFile;

            if (args.length > 1) {
                opFileName = args[0];
                inputFile = args[1];
            } else {
                inputFile = args[0];
            }

            //load opcodes into opcode hashtable
            Proj4 proj = new Proj4();

            if (proj.LoadOpCodes(opFileName)) {
                System.out.println("Op codes loaded!");
                proj.InitPreprocessorCommands();
                System.out.println("Preprocs loaded!");
                proj.InitRegOps();
                System.out.println("RegOps loaded!");
                proj.InitRegisters();
                System.out.println("Registers Loaded!");

                if (inputFile.length() > 0) {
                    proj.AssemblePass1(inputFile);
                    proj.AssemblePass2();
                }
            }

        } else {
            System.out.println("No Input File specified!");
        }

    }

    public void InitPreprocessorCommands() {
        _preprocs.Add("START");
        _preprocs.Add("END");
        _preprocs.Add("BASE");
        _preprocs.Add("NOBASE");
        _preprocs.Add("EQU");
        _preprocs.Add("RESB");
        _preprocs.Add("RESW");
        _preprocs.Add("WORD");
        _preprocs.Add("BYTE");
        _preprocs.Add("CSECT");
        _preprocs.Add("EXTDEF");
        _preprocs.Add("EXTREF");
        _preprocs.Add("USE");
        _preprocs.Add("LTORG");
    }

    //designates register-register commands
    //for future use.
    public void InitRegOps() {
        _regOps.Add("MULR");
        _regOps.Add("ADDR");
        _regOps.Add("COMPR");
        _regOps.Add("DIVR");
        _regOps.Add("SUBR");
        _regOps.Add("TIXR");
    }

    public void InitRegisters() {
        _registers.Add("A", 0);
        _registers.Add("X", 1);
        _registers.Add("L", 2);
        _registers.Add("PC", 8);
        _registers.Add("SW", 9);
        _registers.Add("B", 3);
        _registers.Add("S", 4);
        _registers.Add("T", 5);
        _registers.Add("F", 6);
    }

    public void AssemblePass1(String inputFile) {



        _projectName = inputFile;

        String out = "";

        if (inputFile.length() > 0) {
            try {
                FileReader fr = new FileReader(inputFile);
                BufferedReader br = new BufferedReader(fr);

                String line;

                while ((line = br.readLine()) != null) {

                    currentLineNumber++;
                    //System.out.println(line);
                    SourceCodeLine src = new SourceCodeLine();

                    String[] lineTokens = line.split("\\s+");

                    int length = line.length();

                    //if the line contains more than 1 token, assume it's 
                    //not a comment

                    if (lineTokens.length > 0 && lineTokens[0].trim().startsWith(".")) {

                        src.Source = line;
                        src.Address = currentPosition;
                    } else {
                        boolean containsPreproc = false;

                        String[] tokens = line.split("\\s+");

                        for (int i = 0; i < tokens.length; i++) {
                            if (_preprocs.Find(tokens[i], true) != null) {
                                containsPreproc = true;
                            }
                        }


                        src.Source = line;

                        //if there's a label attached tot he line, add it.

                        if (length > 7) {
                            src.Label = line.substring(0, 7).trim();
                        } else {
                            src.Label = line.substring(0).trim();
                        }

                        if (length > 9) {
                            String extender = line.substring(9, 10).trim();

                            //extended code
                            if (extender.equals("+")) {
                                src.IsExtended = true;
                            } else {
                                src.IsExtended = false;
                            }

                            //original SIC code
                            if (extender.equals("*")) {
                                src.IsSic = true;
                            } else {
                                src.IsSic = false;
                            }


                            //stores OP CODE (lda, sta, etc)
                            if (length >= 10) {
                                String opCodeName;

                                if (length > 16) {
                                    opCodeName = line.substring(10, 16).trim();
                                } else {
                                    opCodeName = line.substring(10).trim();
                                }

                                src.Operator = opCodeName;

                                HashValue opCodeHash = _sicOps.Find(opCodeName);
                                Object opCode = null;

                                if (opCodeHash != null) {
                                    opCode = opCodeHash.Value;
                                }

                                if (opCode != null) {
                                    src.OpCode = (SicOperation) opCode;
                                }
                            }



                            //set operand before parsing op modifier
                            if (line.length() > 19) {
                                if (line.length() > 27) {
                                    src.Operand = line.substring(19, 28).trim();
                                } else {
                                    src.Operand = line.substring(19).trim();
                                }
                            }

                            if (line.length() > 18) {
                                src.OpModifier = line.charAt(18);

                                if (src.OpModifier == '@') {

                                    src.IsIndirect = true;
                                } else {
                                    src.IsIndirect = false;
                                }

                                if (src.OpModifier == '#') {
                                    src.IsImmediate = true;
                                } else {
                                    src.IsImmediate = false;
                                }

                                if (src.OpModifier == '=') {
                                    String literal = "";

                                    char literalMod = src.Operand.toCharArray()[0];

                                    if (literalMod == 'X' || literalMod == 'C') {


                                        if (literalMod == 'X') {
                                            literal = line.substring(line.indexOf("=X'"), line.lastIndexOf("'") + 1);
                                        } else if (src.Operand.toCharArray()[0] == 'C') {

                                            literal = line.substring(line.indexOf("=C'"), line.lastIndexOf("'") + 1);
                                        }




                                        src.Operand = literal;

                                        _literals.Add(literal, literal);


                                        //System.out.println(String.format("Literal found in '%s' ", line));
                                    }
                                }

                                if (!src.IsImmediate && !src.IsIndirect && !src.IsSic) {
                                    src.IsImmediate = true;
                                    src.IsIndirect = true;
                                }
                            }

                            //preliminary pass 2 checking.
                            //checks base nix

                            if (_regOps.Find(src.Operator) != null) {
                                src.IsRegisterOp = true;
                            }



                            if (line.length() > 29) {
                                src.Comment = line.substring(29).trim();
                            }


                            if (src.Operator.trim().toLowerCase().compareTo("start") == 0) {
                                src.IsPreproc = true;
                                startPosition = HexToInt(src.Operand);
                                currentPosition = startPosition;
                            }

                            if (startPosition == 0) {
                                startPosition = HexToInt(String.format("%d", _defaultStartPosition));
                                currentPosition = startPosition;
                            }
                        }



                        //try to find nmeunomic in sic operations.
                        try {
                            src.Address = currentPosition;

                            SicOperation op = src.OpCode;

                            //if it can, add the size of the operation ot the current position
                            if (op != null) {
                                currentPosition += op.Size;
                                src.Size = op.Size;
                            }

                            //if extended, add an extra byte;
                            if (src.IsExtended) {
                                currentPosition++;
                                src.Size++;
                            }

                            String nemonic = src.Operator.toLowerCase().trim();

                            //designates whether the code is preproc or
                            //executable code.
//                            if (_preprocs.Find(nemonic) != null || line.toLowerCase().contains("equ")) {
//                                src.IsPreproc = true;
//                            } else {
//                                src.IsPreproc = false;
//                            }

                            //determines whether the operation is a indexed operation
                            if (src.Operand.contains(",")) {

                                String[] regToRegOps = src.Operand.split(",");

                                if (regToRegOps.length != 2) {
                                    src.HasError = true;
                                    src.ErrorMessage = "invalid number of registers specified for register to register operation";
                                } else {
                                    //check to see if opcode is a register-register command
                                    //if so, treat it like one.
                                    //It is important to note that single register
                                    //register-register commands (TIXR, etc) will not reach this code,
                                    //since they have no comma associated with their op.  If they do,
                                    //they deserve an error.
                                    if (op != null && _regOps.Find(op.OpCode) != null) {
                                        for (int i = 0; i < 2; i++) {
                                            if (_registers.Find(regToRegOps[i]) == null) {
                                                src.HasWarning = true;
                                                src.ErrorMessage = "Invalid Register found in Register to Register Operation:" + regToRegOps[i];
                                            }
                                        }
                                    } else {
                                        if (!src.IsRegisterOp) {
                                            if (regToRegOps[1].compareTo("X") == 0) {
                                                src.IsIndexed = true;
                                            } else {
                                                src.IsIndexed = false;
                                                src.HasError = true;
                                                src.ErrorMessage = "Invalid use of ',' in Indexed operation.  X (index register) should be used.";
                                            }
                                        }
                                    }
                                }

                            }

                            //deermines how much space to give to each  
                            //address operation
                            if (nemonic.compareTo("word") == 0) {
                                src.Size = 3;
                                currentPosition += src.Size;
                                src.IsAddressOperation = true;
                            } else if (nemonic.compareTo("resw") == 0) {
                                try {
                                    src.Size = Integer.parseInt(src.Operand) * 3;
                                    currentPosition += src.Size;
                                    src.IsAddressOperation = true;
                                    src.IsReservedAddress = true;
                                } catch (Exception ex) {
                                    src.HasError = true;
                                    src.ErrorMessage = "Error Parsing RESW value";
                                }
                            } else if (nemonic.compareTo("byte") == 0) {
                                try {
                                    src.Size = (int) (src.Operand.length() / 2 + 0.5);
                                    currentPosition += src.Size;
                                    src.IsAddressOperation = true;
                                } catch (Exception ex) {
                                    src.HasError = true;
                                    src.ErrorMessage = "Error Parsing BYTE value";
                                }
                            } else if (nemonic.compareTo("resb") == 0) {
                                try {
                                    src.Size = Integer.parseInt(src.Operand);
                                    currentPosition += src.Size;
                                    src.IsAddressOperation = true;
                                    src.IsReservedAddress = true;
                                } catch (Exception ex) {
                                    src.HasError = true;
                                    src.ErrorMessage = "Error Parsing RESB value";
                                }
                            }


                            //add symbol
                            if (src.Label.length() > 0) {
                                _symbolTable.Add(src.Label, src);
                            }
                        } catch (Exception ex) {

                            System.out.println(ex.getMessage());
                        }
                    }



                    _src[currentLineNumber] = src;

                    if (line.contains("LTORG")) {
                        DumpLiterals();
                        _literals = new HashTable(5);
                    }

                    if (out.length() > 0) {
                        //System.out.println(out);
                    }

                }

                DumpLiterals();

            } catch (FileNotFoundException ex) {
                System.out.println("File " + inputFile + " not found!");
            } catch (IOException ex) {
                System.out.println("There was an error reading the input file:" + ex.getMessage());
            }
        } else {
            System.out.println("No input file specified");
        }
    }

    public void AssemblePass2() {

        int pcMin = -2047;
        int pcMax = 2048;
        int baseMin = 0;
        int baseMax = 4096;

        pc = startPosition;

        for (int i = 0; i < _src.length; i++) {

            SourceCodeLine src = _src[i], tmpSrc = new SourceCodeLine();
            HashValue tmpHash = new HashValue();

            if (src != null) {
                pc += src.Size;

                if (src.Operator != null && src.Operator.toUpperCase().compareTo("BASE") == 0) {
                    tmpHash = _symbolTable.Find(src.Operand);
                    if (tmpHash != null) {
                        base = GetRealAddress((SourceCodeLine) tmpHash.Value);
                    } else {
                        src.HasError = true;
                        src.ErrorMessage = "Unknown Symbol found in BASE declaration.";
                    }

                }

                int offset = 0;

                //if this is a regular 
                if (!src.IsPreproc && !src.IsAddressOperation) {
                    if (src.OpCode != null) {

                        //generates NI for second half-byte of address
                        src.AssembledHex = HexToInt(src.OpCode.OpCode);

                        if (!src.IsRegisterOp) {
                            if (src.IsImmediate) {
                                src.AssembledHex += 1;
                            }
                            if (src.IsIndirect) {
                                src.AssembledHex += 2;
                            }
                        }


                        //operand specified
                        if (src.Operand != null && src.Operand.compareTo("") != 0) {

                            String operand;
                            if (src.IsIndexed) {
                                operand = src.Operand.substring(0, src.Operand.indexOf(','));
                            } else {
                                operand = src.Operand;
                            }

                            HashValue hashedSymbol = _symbolTable.Find(operand);

                            if (src.IsRegisterOp) {
                                String[] registers = src.Operand.split(",");

                                if (registers.length > 2) {
                                    src.HasError = true;
                                    src.ErrorMessage = "Too many registers specified in reg-reg operation";
                                }

                                src.AssembledLine = String.format("%x", src.AssembledHex);

                                for (int j = 0; j < registers.length; j++) {
                                    HashValue reg = _registers.Find(registers[j]);

                                    try {
                                        src.AssembledLine += (String.format("%d", reg.Value));
                                    } catch (Exception ex) {
                                    }
                                    if (reg == null) {
                                        src.HasError = true;
                                        src.ErrorMessage = "Invalid register specified.";
                                    }
                                }

                                if (registers.length == 1) {
                                    src.AssembledLine += "0";
                                }

                            } else {


                                String s = String.format("=%s", operand);
                                if (hashedSymbol == null && src.OpModifier == '=') {
                                    hashedSymbol = _symbolTable.Find(s);
                                }

                                SourceCodeLine srcOperand;


                                //if the operand exists within the symbol table
                                //load the operand and go forth and conquer.
                                if (hashedSymbol != null) {
                                    srcOperand = (SourceCodeLine) hashedSymbol.Value;

                                    //if (!src.IsImmediate || (src.IsImmediate && src.IsIndirect) {

                                    offset = GetPositionDifference(pc, srcOperand.Address);

                                    if (offset > pcMax || offset < pcMin) {
                                        if (base == -1) {
                                            src.HasError = true;
                                            src.ErrorMessage = "Address offset too large for PC addressing and BASE not defined.";
                                        } else {
                                            offset = GetPositionDifference(base, srcOperand.Address);
                                            if (offset < baseMin || offset > baseMax) {

                                                src.HasError = true;
                                                src.ErrorMessage = "Address offset too large for BASE  && PC addressing";
                                            } else {
                                                src.IsBaseRelative = true;
                                                src.IsPCRelative = false;
                                            }
                                        }
                                    } else if (src.IsExtended) {
                                        offset = srcOperand.Address;
                                    } else if (src.IsImmediate) {
                                    }
//                                    }
//                                    else{
//                                        if(src.IsImmediate)
//                                        {
//                                            offset = srcOperand.Address;
//                                        }
//                                    }
                                } else {
                                    //if the operand is an int do this.
                                    try {
                                        if (!src.IsLiteral) {
                                            int op = Integer.parseInt(src.Operand);
                                            offset = op;
                                        }
                                    } catch (Exception ex) {
                                        src.HasError = true;
                                        src.ErrorMessage = "Invalid operand specified for immediate addressing.";
                                    }
                                }


                                if (src.IsExtended) {
                                    src.IsBaseRelative = false;
                                    src.IsPCRelative = false;
                                }

                                //generates XBPE half-byte for src code
                                int xbpe = 0;


                                int addrSize = 3;

                                if (src.IsIndexed) {
                                    xbpe += 8;
                                }
                                if (src.IsBaseRelative) {
                                    xbpe += 4;
                                }
                                if (src.IsPCRelative) {
                                    xbpe += 2;
                                }
                                if (src.IsExtended) {
                                    xbpe += 1;
                                    addrSize = 5;
                                }

                                if (src.IsImmediate && !src.IsIndirect) {
                                    xbpe = 0;
                                }

                                if (!src.IsExtended) {
                                    src.Offset = offset;


                                    String offsetString = String.format("%03X", offset);

                                    if (addrSize - offsetString.length() > 0) {
                                        offsetString = String.format("%s%s", StringExtension.setLength("", addrSize - offsetString.length(), '0'), offsetString);
                                    } else {
                                        offsetString = String.format("%s", offsetString);
                                    }

                                    if (offsetString.length() > addrSize) {
                                        offsetString = offsetString.substring(offsetString.length() - addrSize);
                                    }

                                    if (!src.HasError) {
                                        src.AssembledLine = String.format("%02X%X%" + addrSize + "S", src.AssembledHex, xbpe, offsetString);
                                    } else {
                                        src.AssembledLine = "";
                                    }
                                } else {
                                    src.AssembledLine = String.format("%02X%X%05X", src.AssembledHex, xbpe, offset);
                                }
                            }
                        } else {
                            //operand not specified
                            src.AssembledLine = String.format("%2X0000", src.AssembledHex);
                        }
                    } else {
                        HashValue preproc = _preprocs.Find(src.Operator);
                        if (preproc != null) {
                            src.IsPreproc = true;
                        }

                        if (!src.IsPreproc && !src.IsAddressOperation) {

                            src.HasError = true;
                            src.ErrorMessage = "Error finding Opcode for line: " + src.Source;
                        }
                    }
                } else {
                    if (src.IsAddressOperation) {

                        if (src.IsLiteral) {
                            String literalValue = src.OpModifier + src.Operand;

                            if (literalValue.contains("=C'")) {
                                literalValue = literalValue.substring(literalValue.indexOf("=C'") + 3, literalValue.lastIndexOf("'"));
                                src.AssembledLine = String.format("%x", new BigInteger(literalValue.getBytes()));
                            } else if (literalValue.contains("=X'")) {
                                literalValue = literalValue.substring(literalValue.indexOf("=X'") + 3, literalValue.lastIndexOf("'"));
                                try {
                                    Integer hex = HexToInt(literalValue);
                                } catch (Exception ex) {
                                    src.HasError = true;
                                    src.ErrorMessage = "Invalid Hex value specified in literal";
                                }

                                src.AssembledLine = String.format("%s", literalValue);
                            }

                        } else if ((src.IsIndirect && src.IsImmediate) || (!src.IsIndirect && !src.IsImmediate)) {
                            Integer iOperand;
                            Integer opLength = (int) (src.Size);
                            try {
                                iOperand = Integer.parseInt(src.Operand);
                                if (src.IsReservedAddress) {
                                    src.AssembledLine = "";
                                } else {
                                    if (src.Operator.toLowerCase().compareTo("word") == 0) {
                                        src.AssembledLine = String.format("%06x", iOperand);
                                        src.AssembledLine = src.AssembledLine.substring(src.AssembledLine.length() - 6);
                                    } else {
                                        src.AssembledLine = String.format("%0" + opLength + "x", iOperand);
                                    }
                                }
                            } catch (Exception ex) {
                                src.AssembledLine = String.format("%" + opLength + "s", src.Operand);
                            }
                        }


                    }
                }
            }
        }

        PrintToFile();

        for (int i = 0; i < _symbolTable.length(); i++) {
            if (_symbolTable._hash[i] != null) {
                HashValue hash = _symbolTable._hash[i];
                SourceCodeLine srcLine = (SourceCodeLine) hash.Value;
                //System.out.println(String.format("Symbol %s \t with memory location %s  stored at position %d", hash.Key, Integer.toHexString(srcLine.Address), hash.Address));
            }
        }
    }

    public boolean PrintToFile() {
        return PrintToFile(_projectName);
    }

    public boolean PrintToFile(String projectName) {

        String outputListFileName = projectName + ".lst";
        String outputObjFileName = projectName + ".obj";

        SourceCodeLine src;

        int maxAssebledLineLength = 0;

        for (int i = 0; i < _src.length; i++) {
            src = _src[i];
            if (src != null) {
                maxAssebledLineLength = maxAssebledLineLength < src.AssembledLine.length() ? src.AssembledLine.length() : maxAssebledLineLength;
            }
        }


        try {

            File outputListFile = new File(outputListFileName);
            File outputObjFile = new File(outputObjFileName);

            if (outputListFile.exists()) {
                outputListFile.delete();
            }

            if (outputObjFile.exists()) {
                outputObjFile.delete();
            }

            FileWriter fw = new FileWriter(outputListFileName);
            FileWriter fw2 = new FileWriter(outputObjFileName);

            BufferedWriter bw = new BufferedWriter(fw);
            BufferedWriter bw2 = new BufferedWriter(fw2);

            String startPosition = "";
            
            boolean writeStartOp= false;
            
            for (int i = 0; i < _src.length; i++) {
                 src = _src[i];
                if (src != null) {
                   
                    bw.write(String.format("%03d   %05X: %S %S ", i, src.Address, StringExtension.setLength(src.AssembledLine, maxAssebledLineLength, ' '), src.Source));
                    bw.newLine();

                     
                    if(writeStartOp){
                        bw2.write(String.format("%06X", src.Address));
                        bw2.newLine();
                        bw2.write(startPosition);
                        bw2.newLine();
                        writeStartOp = false;
                    }
                    
                    if (!src.IsPreproc && !src.IsReservedAddress) {
                        if(src.AssembledLine.trim().length()>0)
                        {bw2.write(String.format("%S ", src.AssembledLine));
                        
                           bw2.newLine();
                        }
                    } else {
                        
                        if(src.IsReservedAddress || src.Operator.compareToIgnoreCase("END")==0){
                            bw2.write("!");
                            writeStartOp = true;
                            
                            bw2.newLine();
                        }
                        
                        if(src.Operator.compareToIgnoreCase("START")==0){
                            startPosition = String.format("%06X ", src.Address);
                            bw2.write(startPosition);
                            bw2.newLine();
                            bw2.write("000000");
                            writeStartOp = false;
                            
                        bw2.newLine();
                        }
                        
                    }

                }
            }

            bw.close();
            fw.close();
            bw2.close();
            fw2.close();

        } catch (FileNotFoundException ex) {
            return false;
        } catch (IOException ex) {
            System.out.println("There was an error writing the output file file:" + ex.getMessage());
            return false;
        }


        return true;
    }

    private int GetRealAddress(SourceCodeLine src) {
        if (src.IsImmediate && src.IsIndirect) {
            return src.Address;
        } else if (src.IsImmediate) {

            HashValue h = _symbolTable.Find(src.Operand);

            if (h != null) {
                return ((SourceCodeLine) h.Value).Address;
            } else {
                try {
                    int opValue = Integer.parseInt(src.Operand);
                    return opValue;
                } catch (Exception ex) {
                    src.HasError = true;
                    src.ErrorMessage = "Error Parsing immediate operand value: Operands should either be an integer or a Symbol";
                }
            }
        } else if (src.IsIndirect) {
            if (src.Operand == null) {
                src.HasError = true;
                src.ErrorMessage = "No operand specified in indirect address";

            } else {
                return src.Address;
//                HashValue h = _symbolTable.Find(src.Operand);
//                if (h != null) {
//                    return ((SourceCodeLine) (h.Value)).Address;
//                } else {
//                    src.HasError = true;
//                    src.ErrorMessage = "Error finding specified operand";
//                }
            }
        }

        return src.Address;

    }

    private boolean DumpLiterals() {
        for (int i = 0; i < _literals.length(); i++) {
            if (_literals._hash[i] != null) {

                //grab each literal
                String literalValue = (String) _literals._hash[i].Value;

                SourceCodeLine literal = new SourceCodeLine();

                literal.Operand = literalValue.substring(1);
                literal.IsAddressOperation = true;
                literal.Label = literalValue;
                literal.Address = currentPosition;
                literal.OpModifier = '=';
                literal.IsLiteral = true;


                int maxLiteralSize = 10;

                if (literalValue.contains("=C")) {
                    //converst literal operand from character string into hex

                    //literalValue = literalValue.substring(literalValue.indexOf("=C'") + 3, literalValue.lastIndexOf("'"));

                    if (literalValue.length() <= maxLiteralSize) {
                        literal.Source = String.format("%s BYTE     %s", StringExtension.setLength(literalValue, maxLiteralSize, ' '), literal.Operand);
                    } else {
                        literal.Source = String.format("%s BYTE     %s", literalValue.substring(0, maxLiteralSize), literal.Operand);
                    }

                    literal.Size = (int) (literalValue.substring(literalValue.indexOf("'") + 1, literalValue.lastIndexOf("'")).length());
                    currentPosition += literal.Size;

                } else {


                    //literalValue = literalValue.substring(literalValue.indexOf("=X'") + 3, literalValue.lastIndexOf("'"));

                    if (literalValue.length() <= maxLiteralSize) {
                        literal.Source = String.format("%s BYTE     %s", StringExtension.setLength(literalValue, maxLiteralSize, ' '), literal.Operand);
                    } else {
                        literal.Source = String.format("%s BYTE     %s", literalValue.substring(0, maxLiteralSize), literal.Operand);
                    }

                    literal.Size = (int) (literalValue.substring(literalValue.indexOf("'") + 1, literalValue.lastIndexOf("'")).length() / 2 + 0.5);
                    currentPosition += literal.Size;
                }

                currentLineNumber++;
                _symbolTable.Add(literal.Label, literal);
                _src[currentLineNumber] = literal;
            }
        }
        return true;
    }

    //gets the difference in positions between 
    private int GetPositionDifference(int pos1, int pos2) {

        return pos2 - pos1;
    }

    private int BoolToInt(Boolean bool) {
        if (bool == null) {
            return 0;
        } else {
            return bool ? 1 : 0;
        }
    }

    protected int HexToInt(String hex) {
        return Integer.parseInt(hex, 16);
    }

    /**
     * @param args the command line arguments
     */
    public boolean LoadOpCodes(String inputFileName) {

        System.out.println("Project Started");


        if (inputFileName.length() > 0) {
            try {
                FileReader fr = new FileReader(inputFileName);
                BufferedReader br = new BufferedReader(fr);

                String line;

                while ((line = br.readLine()) != null) {
                    //System.out.println(line);

                    String[] lineTokens = line.split("\\s+");

                    //String out = String.format("Processing Line %s", line);
                    //if there are more than one value on the line,
                    //assume that the line represents a value to store into 
                    //the hash table
                    if (lineTokens.length > 2) {

                        SicOperation item = new SicOperation();
                        HashValue hash = new HashValue();

                        //set Key/value pair for dictionary item
                        hash.Key = lineTokens[0];
                        item.OpCode = lineTokens[1];
                        item.Size = Integer.parseInt(lineTokens[2]);

                        hash.Value = item;

                        _sicOps.Add(hash);
                        //hash Key value

//                        out += ", stored in position " + hashValue + "!";
                    } else {
                        if (lineTokens.length == 2) {
                        }
                    }


                }
            } catch (FileNotFoundException ex) {
                System.out.println("File " + inputFileName + " not found!");
                return false;
            } catch (IOException ex) {
                System.out.println("There was an error reading the input file:" + ex.getMessage());
                return false;
            }
        } else {
            System.out.println("No input file specified");
            return false;
        }

        return true;




    }
}

class HashTable {

    private int _hashTableSize = 200;
    public HashValue[] _hash;
    private int occupiedSpace;

    public HashTable(int hashSize) {
        _hashTableSize = hashSize;
        _hash = new HashValue[hashSize];
    }

    public boolean Add(String key) {
        return Add(key, key);
    }

    public boolean Add(String key, Object value) {

        HashValue hash = new HashValue();
        hash.Key = key;
        hash.Value = value;
        return Add(hash);
    }

    private boolean Rehash() {

        HashTable _newHash = new HashTable((int) (_hashTableSize * 2));

        for (int i = 0; i < _hashTableSize; i++) {
            if (_hash[i] != null) {
                _newHash.Add(_hash[i].Key, _hash[i].Value);
            }
        }

        _hash = _newHash._hash;
        _hashTableSize = _newHash.length();

        return true;
    }

    public boolean Add(HashValue item) {

        return Add(item, true);
    }

    public boolean Add(HashValue item, Boolean suppressMessage) {

        int hashValue = GetHashValue(item.Key);

        String out = "";

        if (!suppressMessage) {
            out = "Key " + item.Key + " hashed to position " + hashValue + " of " + _hashTableSize;
        }

        //if the item Key is empty, store value in that position
        if (_hash[hashValue] == null) {
            item.Position = hashValue;
            _hash[hashValue] = item;
            occupiedSpace++;
        } else {
            //look for the next position that is empty and store the value there.
            int hashPosition = hashValue;
            boolean foundDuplicate = false;

            while (_hash[hashPosition] != null && foundDuplicate == false) {

                //if the Key is already found, throw an error
                if (_hash[hashPosition].Key.compareTo(item.Key) == 0) {
                    out = "Key " + item.Key + " already exists!";
                    foundDuplicate = true;
                } else {
                    hashPosition++;
                    if (hashPosition >= _hashTableSize) {
                        hashPosition = 0;
                    }

                    if (hashPosition == hashValue) {
                        //IncreaseHashTableSize
                    }
                }

            }

            if (foundDuplicate != true) {
                item.Position = hashPosition;
                _hash[hashPosition] = item;
                occupiedSpace++;
                hashValue = hashPosition;
            }

        }

        if (occupiedSpace > _hashTableSize * 0.8) {
            Rehash();
        }

        if (out != null && out.compareTo("") != 0) {
            //System.out.println(out);
        }

        return true;
    }

    public HashValue Find(String key) {
        return Find(key, true);
    }

    public HashValue Find(String key, Boolean suppressError) {

        String out = "";
        int hashedValue = GetHashValue(key);
        boolean foundHash = false;


        while (foundHash == false && _hash[hashedValue] != null) {
            //if you find it, Huzzah!
            if (_hash[hashedValue].Key.toLowerCase().compareTo(key.toLowerCase()) == 0) {
                foundHash = true;
            } else {
                //if not keep looking.
                hashedValue++;
                if (hashedValue == _hashTableSize) {
                    hashedValue = 0;
                }
            }
        }

        if (foundHash == true) {
            return _hash[hashedValue];
        } else {
            if (!suppressError) {
                out = "Could not find key " + key;
                //// System.out.println(out);
            }
            return null;
        }
    }

    protected int GetHashValue(String s) {

        int stringValue = 0;
        char[] chars = s.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            stringValue += (int) chars[i];
        }

        return stringValue % _hashTableSize;
    }

    public int length() {
        return _hashTableSize;
    }
}

class HashValue {

    int Position;
    String Key;
    Object Value;
}

class SourceCodeLine {

    //label:  This also gets stored in the Symbol table
    String Label;
    //Pass 2 assembled line
    String AssembledLine = "";
    //Pass 2 assembled hex
    int AssembledHex;
    int XBPE;
    String AssembledAddress;
    //Pass 2 Address offset
    int Offset;
    //SIC Op code
    SicOperation OpCode;
    String Operator;
    String Operand;
    char OpModifier;
    String Comment;
    //NIXBPE boolean values
    //yes this could be handled better.
    //whatever.
    Boolean IsExtended = false;
    Boolean IsImmediate = false;
    Boolean IsIndirect = false;
    Boolean IsIndexed = false;
    Boolean IsPCRelative = true;
    Boolean IsBaseRelative = false;
    Boolean IsLiteral = false;
    //if the boolean is a reserved word/byte
    Boolean IsReservedAddress = false;
    //if a command isn't preproc,
    //it's a sicop
    Boolean IsPreproc = false;
    Boolean IsComment = false;
    //Signifies the line is an Adddress operation (WORD, BYTE, ect)
    Boolean IsAddressOperation = false;
    //Is a register-to-register operation
    Boolean IsRegisterOp = false;
    //is the command legacy
    Boolean IsSic = false;
    //error messages
    Boolean HasError = false;
    Boolean HasWarning = false;
    String ErrorMessage;
    //original source line
    String Source;
    //position within code
    int Size = 0;
    int Address;

    public String ToString() {
        SourceCodeLine src = this;

        String s = "";

        if (src.HasError) {
            src.AssembledLine = "";
        }

        if (src.OpCode != null) {
            s = String.format("%05X:    " + StringExtension.setLength(src.AssembledLine, 10, ' ') + "%S", src.Address, src.Source);
        } else {
            if (IsPreproc) {
                s = String.format("%05X:    " + StringExtension.setLength("", 10, ' ') + "%S ", Address, Source);
            } else if (IsAddressOperation) {

                s = String.format("%05X:    " + StringExtension.setLength(src.AssembledLine, 10, ' ') + "%S ", src.Address, src.Source);
            }

            if (!IsPreproc && !IsAddressOperation) {
                s = String.format("%05x:    " + StringExtension.setLength("", 15, ' ') + "%s", src.Address, src.Source);
            }
        }


        if (src.HasError) {
            s += "\n********" + src.ErrorMessage + "******************";
        }

        return s;
    }
}

class SicOperation {

    int Size;
    String OpCode;
    String Name;
    int OpCodeHex;
}

class StringExtension {

    public static String setLength(String original, int length, char padChar) {
        return justifyLeft(original, length, padChar, false);
    }

    protected static String justifyLeft(String str, final int width, char padWithChar,
            boolean trimWhitespace) {
        // Trim the leading and trailing whitespace ...
        str = str != null ? (trimWhitespace ? str.trim() : str) : "";

        int addChars = width - str.length();
        if (addChars < 0) {
            // truncate
            return str.subSequence(0, width).toString();
        }
        // Write the content ...
        final StringBuilder sb = new StringBuilder();
        sb.append(str);

        // Append the whitespace ...
        while (addChars > 0) {
            sb.append(padWithChar);
            --addChars;
        }

        return sb.toString();
    }
}