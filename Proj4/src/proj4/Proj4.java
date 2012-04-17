/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package proj4;

import java.io.*;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Stack;
import java.util.List;

/**
 *
 * @author e5008222
 */
public class Proj4 {

    protected int _sicOpsTableSize = 200;
    protected int _symbolTableSize = 50;
    protected int _defaultStartPosition = 100;
    protected int _defaultSourceSize = 300;
    protected HashTable _symbolTable = new HashTable(_symbolTableSize);
    private boolean truncateLiterals = true;
    private int maxLiteralSize = 6;
    private HashTable _sicOps = new HashTable(_sicOpsTableSize);
    private HashTable _preprocs = new HashTable(20);
    private HashTable _addressOps = new HashTable(20);
    private SourceCodeLine[] _src = new SourceCodeLine[200];
    private HashTable _literals = new HashTable(3);
    private HashTable _regOps = new HashTable(6);
    private HashTable _registers = new HashTable(10);
    static String _projectName = "p4";
    static String _appName = "";
    static int startPosition = -1;
    static int pc = 0;
    static int base = -1;
    static int currentPosition = 0;
    static int currentLineNumber = 0;
    static HashTable operators = new HashTable(6);

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
                proj.InitOperators();
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

    public void InitOperators() {
        operators.Add("+", 0);
        operators.Add("-", 0);
        operators.Add("*", 1);
        operators.Add("/", 1);
        operators.Add("^", 3);
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

    public void InitAddressCommands() {
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
        _regOps.Add("RMO");
        _regOps.Add("CLEAR");
    }

    public void InitSpecOps() {
        //placeholder for special operations.
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



        if (inputFile.length() > 0) {
            try {
                FileReader fr = new FileReader(inputFile);
                BufferedReader br = new BufferedReader(fr);

                String line;
                String currentUseBlockName = " ";

                String[] source = new String[_defaultSourceSize];

                String[] useBlockNames = new String[10];
                int useBlockCount = 1;
                useBlockNames[0] = " ";

                HashTable useBlocks = new HashTable(10);
                useBlocks.Add(" ", new String[0]);

                String[] currentBlock = new String[_defaultSourceSize];

                int j = 0;
                while ((line = br.readLine()) != null) {

                    if (line.trim().toLowerCase().contains("use")) {
                        String useName = "";
                        HashValue useHash;

                        String[] splitSet = line.trim().toLowerCase().split("use");
                        boolean blockExists = false;

                        if (splitSet.length <= 2) {

                            useHash = useBlocks.Find(currentUseBlockName);

                            String[] useBlock = new String[0];

                            if (useHash != null) {
                                useBlock = ((String[]) (useHash.Value));
                                blockExists = true;
                            }
//                                useName = splitSet[1].trim();
//                                useHash = useBlocks.Find(useName);
//                                if (useHash != null) {
//                                    blockExists = true;
//                                    useBlock = (String[]) (useBlocks.Find(useName).Value);
//                                } else {
//                                    useBlockNames[useBlockCount] = useName;
//                                    useBlockCount++;
//                                }
                            try {
                                if (splitSet.length == 0 || splitSet.length == 1 || (splitSet.length == 2 && splitSet[1].equals(""))) {
                                    useName = " ";
                                } else {
                                    useName = splitSet[1].trim();
                                }
                            } catch (Exception ex) {
                            }
                            for (int i = 0; i < j + 1; i++) {
                                if (currentBlock[i] != null) {
                                    useBlock = addElement(useBlock, currentBlock[i]);
                                    currentBlock[i] = null;
                                }
                            }

                            j = 0;

                            if (blockExists) {
                                useBlocks.Replace(currentUseBlockName, useBlock);
                            } else {
                                useBlocks.Add(currentUseBlockName, useBlock);
                                useBlockNames[useBlockCount] = currentUseBlockName;
                                useBlockCount++;

                            }

                            currentUseBlockName = useName;
                            line = "." + line;
                        }
                    }

                    currentBlock[j] = line;
                    j++;

                }

                //at the end of the source file, 
                //dump the source to the proper use block.
                if (j > 0) {
                    HashValue useHash = useBlocks.Find(currentUseBlockName);
                    String[] useBlock = new String[1];
                    if (useHash != null) {
                        useBlock = ((String[]) (useHash.Value));
                    }
                    for (int i = 0; i < j + 1; i++) {
                        if (currentBlock[i] != null && currentBlock[i].trim().compareTo("") != 0) {
                            useBlock = addElement(useBlock, currentBlock[i]);
                            currentBlock[i] = null;
                        }
                    }

                    useBlocks.Replace(currentUseBlockName, useBlock);
                }


                //dumps the use-block split source
                //into one contiguous source.
                int i = 0;
                for (int k = 0; k <= useBlockCount; k++) {
                    String useName = useBlockNames[k];
                    if (useName != null) {
                        HashValue val = useBlocks.Find(useName);

                        if (val != null) {
                            String[] block = ((String[]) (val.Value));
                            for (int p = 0; p < block.length; p++) {
                                if (block[p] != null) {
                                    source[i] = block[p];
                                    i++;
                                }
                            }
                        }
                    }
                }

                AssemblePass1(source);

            } catch (FileNotFoundException ex) {
                System.out.println("File " + inputFile + " not found!");
            } catch (IOException ex) {
                System.out.println("There was an error reading the input file:" + ex.getMessage());
            }
        } else {
            System.out.println("No input file specified");
        }
    }

    public SourceCodeLine[] AssemblePass1(String[] sourceCode) {

        boolean startFound = false;

        _src = new SourceCodeLine[sourceCode.length];

        String out = "";

        for (int k = 0; k < sourceCode.length; k++) {
            if (sourceCode[k] != null) {
                String line = sourceCode[k];

                currentLineNumber++;
                //System.out.println(line);
                SourceCodeLine src = new SourceCodeLine();

                String[] lineTokens = line.split("\\s+");

                int length = line.length();

                //if the line contains more than 1 token, assume it's 
                //not a comment


                if (line.trim().startsWith(".")) {
                    src.IsComment = true;
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

                        }

                        try {
                            if (src.Operand != null) {
                                for (int m = 0; m < src.Operand.length(); m++) {
                                    if (operators.Find(src.Operand.substring(m, m + 1)) != null) {
                                        //if (src.Operand.substring(0, 1).compareTo("-") != 0) {
                                        src.RequiresShunting = true;
                                        //}
                                    }
                                }
                            }
                        } catch (Exception ex) {
                        }

                        if (!src.IsImmediate && !src.IsIndirect && !src.IsSic) {
                            src.IsImmediate = true;
                            src.IsIndirect = true;
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

                            startFound = true;


                            _appName = src.Label;

                            if (_appName == null || _appName == "") {
                                _appName = _projectName;
                            }

                            src.IsPreproc = true;
                            startPosition = HexToInt(src.Operand);

                            if (startPosition < 0) {
                                src.HasError = true;
                                src.ErrorMessage = "Invalid Start position specified.  Defaulting to 100";
                                startPosition = HexToInt("100");
                            }

                            currentPosition = startPosition;
                        }

                        if (startPosition < 0) {
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


                        String nemonic = src.Operator.toLowerCase().trim();

                        //designates whether the code is preproc or
                        //executable code.
//                            if (_preprocs.Find(nemonic) != null || line.toLowerCase().contains("equ")) {
//                                src.IsPreproc = true;
//                            } else {
//                                src.IsPreproc = false;
//                            }

                        if (src.OpCode != null && src.OpCode.OperandCount == 0) {
                            if (src.Operand.length() > 0) {
                                src.HasError = true;
                                src.ErrorMessage = "Operand specified for operation that requires no operand";
                            }
                        }




                        //determines whether the operation is a indexed operation
                        if (src.Operand.contains(",")) {

                            String[] regToRegOps = src.Operand.split(",");

                            if (regToRegOps.length != 2) {
                                src.HasError = true;
                                src.ErrorMessage = "invalid use of ','. Multiple operands expected.";
                            } else {
                                //check to see if opcode is a register-register command
                                //if so, treat it like one.
                                //It is important to note that single register
                                //register-register commands (TIXR, etc) will not reach this code,
                                //since they have no comma associated with their op.  If they do,
                                //they deserve an error.
                                if (op != null && _regOps.Find(op.Name) != null) {
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


                        //if extended, add an extra byte;
                        if (src.IsExtended && !src.IsRegisterOp) {
                            currentPosition++;
                            src.Size++;
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


                    } catch (Exception ex) {

                        System.out.println(ex.getMessage());
                    }
                }


                //add symbol
                if (src.Label != null && src.Label.length() > 0) {
                    if (_symbolTable.Find(src.Label) != null) {
                        src.HasError = true;
                        src.ErrorMessage = "Duplicate Symbol Declaration!";
                    } else {
                        _symbolTable.Add(src.Label, src);
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
        }

        if (!startFound) {
            for (SourceCodeLine src : _src) {
                if (src != null) {
                    src.HasError = true;
                    src.ErrorMessage = "No Start point specified";
                    break;
                }
            }
        }

        DumpLiterals();


        return _src;
    }

    SourceCodeLine[] AssemblePass2() {
        return AssemblePass2(_src);
    }

    SourceCodeLine[] AssemblePass2(SourceCodeLine[] srcList) {

        int pcMin = -2047;
        int pcMax = 2048;
        int baseMin = 0;
        int baseMax = 4096;

        pc = startPosition;

        for (int i = 0; i < srcList.length; i++) {

            SourceCodeLine src = srcList[i], tmpSrc = new SourceCodeLine();
            HashValue tmpHash = new HashValue();

            if (src != null) {

                if (!src.HasError && !src.HasWarning && !src.IsComment) {
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



                    //if the operand includes (+-*/), it requires
                    //an order of operations implementation
                    if (src.Operand != null && src.RequiresShunting) {
                        try {
                            src = ParseOrderOfOps(src.Operand, src);
                        } catch (Exception ex) {
                            src.HasError = true;
                            src.ErrorMessage = ex.getMessage();
                        }
                    }

                    //if this is a regular opCode
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

                                String operand = src.Operand;




                                if (src.IsIndexed) {
                                    operand = src.Operand.substring(0, src.Operand.indexOf(','));
                                } else {
                                    operand = src.Operand;
                                }

                                HashValue hashedSymbol = _symbolTable.Find(operand);

                                if (src.IsRegisterOp) {
                                    String[] registers = src.Operand.split(",");

                                    if (src.IsExtended) {
                                        src.HasWarning = true;
                                        src.ErrorMessage = "Warning: Register-Register Operations do not support extended operator";
                                    }

                                    if (registers.length > src.OpCode.Size) {
                                        src.HasError = true;
                                        src.ErrorMessage = "Too many registers specified in reg-reg operation";
                                    }

                                    src.AssembledLine = String.format("%x", src.AssembledHex);

                                    boolean hasDuplicateRegisters = false;

                                    for (int j = 0; j < registers.length; j++) {
                                        HashValue reg = _registers.Find(registers[j]);

                                        try {
                                            if (registers.length == 2 && j == 1 && registers[0].compareTo(registers[1]) == 0) {
                                                {
                                                    hasDuplicateRegisters = true;
                                                    src.HasWarning = true;
                                                    src.ErrorMessage = "Warning: Duplicate Register specified in Register operation";
                                                }
                                            } else {
                                                src.AssembledLine += (String.format("%d", reg.Value));
                                            }

                                        } catch (Exception ex) {
                                        }
                                        if (reg == null) {
                                            src.HasError = true;
                                            src.ErrorMessage = "Invalid register specified.";
                                        }
                                    }

                                    if (registers.length < src.OpCode.Size || (hasDuplicateRegisters && registers.length - 1 < src.OpCode.Size)) {
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

                                        src.Modifications = GetModifications(src);
                                       
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
                                            src.ErrorMessage = "Invalid operand specified.";
                                        }
                                    }


                                    if (src.IsExtended) {
                                        src.IsBaseRelative = false;
                                        src.IsPCRelative = false;
                                    }

                                    //generates XBPE half-byte for src code
                                    int xbpe = 0;

                                    if (_symbolTable.Find(src.Operand) != null) {
                                        src.IsImmediate = true;
                                        src.IsIndirect = true;
                                    }

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
                            if (src.Operator != null) {
                                HashValue preproc = _preprocs.Find(src.Operator);
                                if (preproc != null) {
                                    src.IsPreproc = true;
                                }

                                if (!src.IsPreproc && !src.IsAddressOperation) {

                                    src.HasError = true;
                                    src.ErrorMessage = "Error finding Opcode for line: " + src.Source;
                                }
                            }
                        }
                    } else {
                        if (src.IsAddressOperation) {

                            String[] ops = BreakOutOperandSet(src.Operand);
                            if(ops.length>1)
                            {
                                for(String op: ops){
                                    if(_symbolTable.Find(op)!=null){
                                        src.Modifications = GetModifications(src);
                                    }
                                }
                            }
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

                            } else {// if ((src.IsIndirect && src.IsImmediate) || (!src.IsIndirect && !src.IsImmediate)) {
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

                                    if (_symbolTable.Find(src.Operand) != null) {
                                        src.AssembledLine = String.format("%06x", ((SourceCodeLine) (_symbolTable.Find(src.Operand).Value)).Address);
                                    } else {
                                        src.AssembledLine = String.format("%" + opLength + "s", src.Operand);
                                    }
                                }
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

        return srcList;
    }

    public String[] GetModifications(SourceCodeLine src){
         for (String mod : src.Modifications) {
                                            if (mod == null) {

                                                String[] _ops = BreakOutOperandSet(src.Operand);
                                                String[] ops = new String[0];
                                                for(String _op: _ops){
                                                    if(_op!=null && _op.trim().length()>0)
                                                        ops = addElement(ops, _op);
                                                }
                                                
                                                try {
                                                    if (ops.length > 1) {
                                                        {
                                                            for (int p = 0; p < ops.length; p++) {
                                                                if (isStackOp(ops[p]) && p < ops.length - 1) {
                                                                    src.Modifications[p] = ops[p] + ops[p + 1];
                                                                }
                                                            }
                                                        }
                                                    }
                                                    
                                                } catch (Exception ex) {
                                                }

                                                src.Modifications[ops.length] = "+" + _appName;
                                                
                                                break;
                                            }

                                        }
         return src.Modifications;
    }
    
    public boolean PrintToFile() {
        return PrintToFile(_projectName);
    }

    public boolean PrintToFile(String projectName) {

        String outputListFileName = projectName + ".lst";
        String outputObjFileName = projectName + ".obj";

        SourceCodeLine[] _src2 = new SourceCodeLine[0];

        for (SourceCodeLine src2 : _src) {
            if (src2 != null) {
                _src2 = addElement(_src2, src2);
            }
        }

        _src = _src2;

        boolean foundErrors = false;

        SourceCodeLine src;

        int maxAssebledLineLength = 0;

        for (int i = 0; i < _src.length; i++) {
            src = _src[i];
            if (src != null) {
                maxAssebledLineLength = maxAssebledLineLength < src.AssembledLine.length() ? src.AssembledLine.length() : maxAssebledLineLength;
            }
        }

        System.out.println("Source Code Length:" + _src.length);

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

            BufferedWriter bw = new BufferedWriter(fw);

            

            boolean writeStartOp = false;

            for (int i = 0; i < _src.length; i++) {
                src = _src[i];
                if (src != null) {

                    bw.write(String.format("%03d   %05X: %S %S ", i, src.Address, StringExtension.setLength(src.AssembledLine, maxAssebledLineLength, ' '), src.Source));
                    bw.newLine();
                    if (src.HasError || src.HasWarning) {
                        if (src.HasError) {
                            foundErrors = true;
                        }
                        bw.write("********" + src.ErrorMessage + "******************");
                        bw.newLine();
                    }
                }
            }

            bw.close();
            fw.close();

            System.out.println("List File " + outputListFileName + " created");


            if (!foundErrors) {



                FileWriter fw2 = new FileWriter(outputObjFileName);

                BufferedWriter bw2 = new BufferedWriter(fw2);

//                for (int i = 0; i < _src.length; i++) {
//                    src = _src[i];
//                    if (src != null) {
//
//                        if (writeStartOp) {
//                            bw2.write(String.format("%06X", src.Address));
//                            bw2.newLine();
//                            bw2.write(startPosition);
//                            bw2.newLine();
//                            writeStartOp = false;
//                        }
//
//                        if (!src.IsPreproc && !src.IsReservedAddress) {
//                            if (src.AssembledLine.trim().length() > 0) {
//                                bw2.write(String.format("%S ", src.AssembledLine));
//
//                                bw2.newLine();
//                            }
//                        } else {
//
//                            if (src.IsReservedAddress || src.Operator.compareToIgnoreCase("END") == 0) {
//                                bw2.write("!");
//                                writeStartOp = true;
//
//                                bw2.newLine();
//                            }
//
//                            if (src.Operator.compareToIgnoreCase("START") == 0) {
//                                startPosition = String.format("%06X ", src.Address);
//                                bw2.write(startPosition);
//                                bw2.newLine();
//                                bw2.write("000000");
//                                writeStartOp = false;
//
//                                bw2.newLine();
//                            }
//
//                        }
//
//                    }
//                }

                String appName = "";
                if (_src.length > 0) {
                    for (SourceCodeLine src1 : _src) {
                        if (src1 != null) {
                            appName = src1.Label;
                            if (appName == null || appName.trim().length() == 0) {
                                appName = projectName;
                            }
                            bw2.write(String.format("H %S %06X %06X", appName, src1.Address, _src[_src.length - 1].Address));
                            bw2.newLine();

                            break;
                        }
                    }


                    String textRecords = "";
                    int textStartAddress = -1;
                    
                    int appStartAddress = -1;

                    for (SourceCodeLine src2 : _src) {
                        if (!src2.IsComment && !src2.IsPreproc && !src2.IsReservedAddress) {
                            if (textStartAddress < 0) {
                                textStartAddress = src2.Address;
                            }

                            
                            textRecords += src2.AssembledLine + " ";
                        } else if (src2.IsReservedAddress && textRecords.length() > 0) {
                            bw2.write(String.format("T %06X %02X %S", textStartAddress, (src2.Address - textStartAddress), textRecords));
                            bw2.newLine();
                            textStartAddress = -1;
                            textRecords = "";
                        }
                        
                        if(src2.Operator != null && src2.Operator.compareToIgnoreCase("END")==0){
                            HashValue op = _symbolTable.Find(src2.Operand);
                            if(op!=null)        
                            {
                                appStartAddress = ((SourceCodeLine)(op.Value)).Address;
                            }
                        }
                    }
                    
                    bw2.write(String.format("T %X %X %S", textStartAddress, (_src[_src.length - 1].Address - textStartAddress), textRecords));
                    bw2.newLine();
                    
                    for(SourceCodeLine src3: _src){
                        for(String mod: src3.Modifications){
                            if(mod!=null)
                                if(src3.IsAddressOperation)
                                {    bw2.write(String.format("M %06X 06 %S", src3.Address, mod));
                                    bw2.newLine();}
                                else{ 
                                    bw2.write(String.format("M %06X 05 %S", src3.Address+1, mod));
                                    bw2.newLine();
                                    
                                    }
                        }
                    }
                    
                    if(appStartAddress<0){
                        bw2.write(String.format("E %06X", startPosition));
                        bw2.newLine();
                    }
                    else{
                        
                        bw2.write(String.format("E %06X", appStartAddress));
                        bw2.newLine();
                    }
                    
                  

                }
                bw2.close();
                fw2.close();


                System.out.println("Object File " + outputObjFileName + " created");

            } else {
                System.out.println("There were errors found in your code.  Subsequently, the object file  " + outputObjFileName + " not generated.  ");
            }

            if (!foundErrors) {
            } else {
            }


        } catch (FileNotFoundException ex) {
            return false;
        } catch (IOException ex) {
            System.out.println("There was an error writing the output file file:" + ex.getMessage());
            return false;
        } catch(Exception ex){
            System.out.println("There was an error writing the output files:" + ex.getMessage());
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
                        item.OperandCount = Integer.parseInt(lineTokens[3]);
                        item.Name = hash.Key;
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

    String[] addElement(String[] org, String added) {
        
        int orgLength= 0;
        for(String o:org)
        {
            if(o!=null && o.trim().length()>0)
                orgLength++;
        }
        
        String[] result = new String[orgLength+1];
        
        int i=0;
        for(String o: org){
            if(o!=null && o.trim().length()>0)
            {
                result[i] = o;
                i++;
            }
        }
        result[orgLength] = added;
        
        return result;
    }

    SourceCodeLine[] addElement(SourceCodeLine[] org, SourceCodeLine added) {
        
        int orgLength= 0;
        for(SourceCodeLine o:org)
        {
            if(o!=null)
                orgLength++;
        }
        
        
        SourceCodeLine[] result = new SourceCodeLine[orgLength+1];
        int i=0;
        for(SourceCodeLine o: org){
            if(o!=null )
            {
                result[i] = o;
                i++;
            }
            
        }
        result[orgLength] = added;
        
        return result;
    }
    //Implementation of Shunting Yard Algorithm for 
    //order of operation processing
    private static int LeftAssociative = 0;
    private static int RightAssociative = 1;

    private boolean isStackOp(String op) {
        if (op != null) {
            return operators.Find(op) != null;
        } else {
            return false;
        }
    }

    private boolean isAssociative(String op, int type) {

        if (isStackOp(op) && Integer.parseInt(operators.Find(op).Value.toString()) == type) {
            return true;
        }
        return false;
    }

    SourceCodeLine ParseOrderOfOps(String val, SourceCodeLine src) throws Exception {

        privateRPN = src;
        
        val = val.replace(" ", "");
        ArrayList<String> operations = new ArrayList<String>();

        String[] operands = BreakOutOperandSet(val);

        Stack<String> opStack = new Stack<String>();

        String output = "";
        int currentPrecedence = 2;

        for (String operand : operands) {

            if (operand != null && operand.length() > 0) {
                if (isStackOp(operand)) {
                    while (!opStack.isEmpty() && isStackOp(opStack.peek())) {
                        if ((isAssociative(operand, LeftAssociative) && ComparePrecedence(
                                operand, opStack.peek()) <= 0)
                                || (isAssociative(operand, RightAssociative) && ComparePrecedence(
                                operand, opStack.peek()) < 0)) {
                            operations.add(opStack.pop());
                            continue;
                        }
                        break;
                    }
                    opStack.push(operand);
                } else if (operand.equals("(")) {
                    opStack.push(operand);
                } else if (operand.equals(")")) {
                    while (!opStack.empty() && !opStack.peek().equals("(")) {
                        operations.add(opStack.pop());
                    }
                    opStack.pop();
                } else {
                    if (!IsInteger(operand)) {
                        if (_symbolTable.Find(operand) != null) {
                           // operand = String.format("%d", ((SourceCodeLine) (_symbolTable.Find(operand).Value)).Address);
                        } else {
                            //throw new Exception("Invalid operand specified in Shunting Yard algorithm: Operands must be symbols or integers.");
                        }
                    }

                    operations.add(operand);
                }



                HashValue hash = _symbolTable.Find(operand);
                if (hash != null) {
                    output += ((SourceCodeLine) (hash.Value)).Address + " ";
                } else {
                    output += operand + " ";
                }


            }

        }

        while (!opStack.empty()) {
            operations.add(opStack.pop());
        }

        String[] outputArray = new String[operations.size()];


        Stack stack = new Stack<String>();

        for (Object op : operations) {
            stack.add((String) op);
        }

        double outInt = 0;

        if (stack.size() > 2) {
            try {
                outInt = ParseRPN(stack);

                val = String.format("%.0f", outInt);
            } catch (Exception ex) {
                throw new Exception("Unable to parse Order of Operands.");
            }
        }

        output = "";
        for (String out : operations.toArray(outputArray)) {
            output += out + " ";
        }

        System.out.println(output);
        src = privateRPN;
        src.Operand = val;
        return src;
    }

    private SourceCodeLine privateRPN = new SourceCodeLine();
    
    private boolean isParen(String val){
        return val.compareTo("(")==0 || val.compareTo(")")==0 ;
    }
    
    private double ParseRPN(Stack<String> ops) throws Exception {
        String tk = ops.pop();
        double x=0, y=0;
        try {
            x = Double.parseDouble(tk);
        } catch (Exception e) {
            try{
                HashValue sym = _symbolTable.Find(tk.toString());
                
                if(sym ==null){
                    y = ParseRPN(ops);
                    x = ParseRPN(ops);
                }
            
            if (tk.equals("+")) {
                if (sym != null) {
                    privateRPN.Modifications = addElement(privateRPN.Modifications, "+" + tk);
                }    
                else{
                    x += y;
                }
            } else if (tk.equals("-")) {
                
                if (sym != null) {
                    privateRPN.Modifications = addElement(privateRPN.Modifications, "-" + tk);
                }    
                else{
                    x -= y;
                }
            } else if (tk.equals("*")) {
                
                if (sym != null) {
                    privateRPN.Modifications = addElement(privateRPN.Modifications, "*" + tk);
                }    
                else{
                    x *= y;
                };
            } else if (tk.equals("/")) {
                
                if (sym != null) {
                    privateRPN.Modifications = addElement(privateRPN.Modifications, "/" + tk);
                }    
                else{
                x /= y;
                }
            } else {
                throw new Exception();
            }
            }
            catch(Exception ex){
                
            }
        }
        return x;
    }

    private int ComparePrecedence(String op1, String op2) {
        if (!isStackOp(op1) || !isStackOp(op2)) {
            throw new IllegalArgumentException("Error parsing operand: Invalid tokens " + op1
                    + " " + op2);
        }
        return Integer.parseInt(operators.Find(op1).Value.toString()) - Integer.parseInt(operators.Find(op2).Value.toString());
    }

    private boolean IsInteger(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    String[] BreakOutOperandSet(String val) {

        boolean popNextOperand = false;
        int opStartIndex = 0;
        int operandCount = 0;

        String[] operands = new String[val.length()+2];

        char[] valArray = val.toCharArray();
        for (int j = 0; j < val.length(); j++) {
            if (operators.Find(String.format("%c", valArray[j])) != null || isParen(String.format("%c", valArray[j]))) {
                operands[operandCount] = val.substring(opStartIndex, j);
                operandCount++;
                
                if(j<val.length()-1)
                    operands[operandCount] = val.substring(j, j + 1);
                else
                    operands[operandCount] = val.substring(j);
                opStartIndex = j + 1;
                operandCount++;
            }
        }
        //adds last value to operand set
        operands[operandCount] = val.substring(opStartIndex);

        String[] ops = new String[0];
        
        for(String op: operands){
            if(op!=null && op.trim().length()>0)
                ops = addElement(ops, op);
        }
        return ops;
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

    public boolean Replace(String key, Object value) {

        int hashedValue = GetHashValue(key);
        boolean foundHash = false;


        while (foundHash == false && _hash[hashedValue] != null) {
            //if you find it, Huzzah!
            if (_hash[hashedValue].Key.toLowerCase().compareTo(key.toLowerCase()) == 0) {
                _hash[hashedValue].Value = value;
                return true;
            } else {
                //if not keep looking.
                hashedValue++;
                if (hashedValue == _hashTableSize) {
                    hashedValue = 0;
                }
            }
        }

        return false;

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
    Boolean RequiresShunting = false;
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
    //represents the .obj modification
    //records needed for this line.
    String[] Modifications = new String[10];
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
    int OperandCount;
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

class ControlSection {

    String Name;
    SourceCodeLine[] Source;
    int _symbolTableSize = 50;
    protected HashTable _symbolTable = new HashTable(_symbolTableSize);
}
