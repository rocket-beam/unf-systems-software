/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package proj4;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;

/**
 *
 * @author e5008222
 */
public class Proj4 {

    protected int _sicOpsTableSize = 200;
    protected int _symbolTableSize = 50;
    protected int _defaultStartPosition = 100;
    private HashTable _sicOps = new HashTable(_sicOpsTableSize);
    private HashTable _preprocs = new HashTable(20);
    private HashTable _symbolTable = new HashTable(_symbolTableSize);
    private SourceCodeLine[] _src = new SourceCodeLine[200];
    private HashTable _literals = new HashTable(3);
    private HashTable _regOps = new HashTable(6);
    private HashTable _registers = new HashTable(10);
    static int startPosition;
    static int pc;

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

                proj.InitPreprocessorCommands();
                proj.InitRegisters();

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
        _registers.Add("A");
        _registers.Add("X");
        _registers.Add("L");
        _registers.Add("PC");
        _registers.Add("SW");
        _registers.Add("B");
        _registers.Add("S");
        _registers.Add("T");
        _registers.Add("F");
    }

    public void AssemblePass1(String inputFile) {

        int currentPosition = 0;
        int currentLineNumber = 0;
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
                        src.Position = currentPosition;
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

                                        _literals.Add(literal, literal);


                                        System.out.println(String.format("Literal found in '%s' ", line));
                                    }
                                }

                                if (!src.IsImmediate && !src.IsIndirect && !src.IsSic) {
                                    src.IsImmediate = true;
                                    src.IsIndirect = true;
                                }
                            }

                            //preliminary pass 2 checking.
                            //checks base nix

                            if (line.toLowerCase().contains("equ")) {
                                src.IsPreproc = true;
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
                            src.Position = currentPosition;

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
                            if (_preprocs.Find(nemonic) != null) {
                                src.IsPreproc = true;
                            } else {
                                src.IsPreproc = false;
                            }

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

                            //deermines how much space to give to each memory 
                            //declaration
                            if (nemonic.compareTo("word") == 0) {
                                src.Size = 3;
                                currentPosition += src.Size;
                                src.IsAddressOperation = true;
                            } else if (nemonic.compareTo("resw") == 0) {
                                try {
                                    src.Size = Integer.parseInt(src.Operand) * 3;
                                    currentPosition += src.Size;
                                    src.IsAddressOperation = true;
                                } catch (Exception ex) {
                                    System.out.println("Error Parsing RESW value");
                                }
                            } else if (nemonic.compareTo("byte") == 0) {
                                try {
                                    src.Size = (int) (src.Operand.length() / 2 + 0.5);
                                    currentPosition += src.Size;
                                    src.IsAddressOperation = true;
                                } catch (Exception ex) {
                                    System.out.println("Error Parsing BYTE value");
                                }
                            } else if (nemonic.compareTo("resb") == 0) {
                                try {
                                    src.Size = Integer.parseInt(src.Operand);
                                    currentPosition += src.Size;
                                    src.IsAddressOperation = true;
                                } catch (Exception ex) {
                                    System.out.println("Error Parsing RESB value");
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
                        for (int i = 0; i < _literals.length(); i++) {
                            if (_literals._hash[i] != null) {

                                //grab each literal
                                String literalValue = (String) _literals._hash[i].Value;

                                SourceCodeLine literal = new SourceCodeLine();

                                literal.Operand = literalValue.substring(literalValue.indexOf("'") + 1, literalValue.lastIndexOf("'"));
                                literal.IsAddressOperation = true;
                                literal.Label = literalValue;
                                literal.Position = currentPosition;

                                if (literalValue.contains("=C")) {
                                    literal.Source = String.format("%s BYTE     %x", StringExtension.setLength(literalValue, 9, ' '), new BigInteger(literal.Operand.getBytes()));
                                    currentPosition += literal.Operand.length();
                                } else {
                                    literal.Source = String.format("%s BYTE     %s", StringExtension.setLength(literalValue, 9, ' '), literal.Operand);
                                    currentPosition += (int) (literal.Operand.length() / 2 + 0.5);
                                }

                                currentLineNumber++;
                                _src[currentLineNumber] = literal;
                            }
                        }
                    }

                    if (out.length() > 0) {
                        System.out.println(out);
                    }

                }
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

        pc = startPosition;

        for (int i = 0; i < _src.length; i++) {

            SourceCodeLine src = _src[i];

            if (src != null) {
                pc += src.Size;

                int offset=0;
                
                if (!src.IsPreproc && !src.IsAddressOperation) {
                    if (src.OpCode != null) {
                        
                        
                        src.AssembledHex = HexToInt(src.OpCode.OpCode);
                        if (src.IsImmediate) {
                            src.AssembledHex += 2;
                        }
                        if (src.IsIndirect) {
                            src.AssembledHex += 1;
                        }
                        
                        
                        int xbpe = 0;
                        
                        if(src.IsIndexed)
                            xbpe+=8;
                        if(src.IsBaseRelative)
                            xbpe+=4;
                        if(src.IsPCRelative)
                            xbpe+=2;
                        if(src.IsExtended)
                            xbpe+=1;
                        
                        
                        
                        //operand specified
                        
                        if (src.Operand != null && src.Operand.compareTo("") != 0) {
                        
                            String operand;
                            if(src.IsIndexed)
                                 operand = src.Operand.substring(0, src.Operand.indexOf(','));
                            else
                                operand = src.Operand;
                            
                            HashValue hashedSymbol = _symbolTable.Find(operand);
                            SourceCodeLine srcOperand;
                            if(hashedSymbol!= null)
                            {
                                srcOperand = (SourceCodeLine) hashedSymbol.Value;
                                
                                offset = GetPositionDifference(pc, srcOperand.Position);
                                
                            }
                        }

                        src.AssembledLine = String.format("%02x%x%03x",src.AssembledHex, xbpe, offset );
                        
                        
                        System.out.println(String.format("%06x: %6s %s ", src.Position,src.AssembledLine, src.Source));

                    } else {
                        HashValue preproc = _preprocs.Find(src.Operator);
                        if(preproc != null)
                        {
                            src.IsPreproc = true;
                        }
                        
                        if(!src.IsPreproc && !src.IsAddressOperation )
                        {
                            System.out.println(String.format("%06x:        %s ", src.Position, src.Source));
                            src.HasError=true;
                            src.ErrorMessage = "Error finding Opcode for line: " + src.Source; 
                        }
                    }
                    
                    if (src.OpCode != null) {
                        SicOperation opCode = src.OpCode;
                    }
                } 
                
                if(src.IsPreproc || src.IsAddressOperation){
                    System.out.println(String.format("%06x:        %s ", src.Position, src.Source));
                }
                if(src.HasError)
                    System.out.println(src.ErrorMessage);
                
            }
        }

        
        
        
        //print out of source file
//        System.out.println("\n--------------------------");
//        System.out.println("Source Code File:");
//        for (int i = 0; i < _src.length; i++) {
//            try {
//                if (_src[i] != null) {
//
//                    if (_src[i].IsPreproc) {
//                        System.out.println(String.format("%06x :%s", _src[i].Position, _src[i].Source));
//                    } else {
//                        System.out.println(String.format("%06x :%s  \t\t %d%d %d%d%d%d", _src[i].Position, _src[i].Source,
//                                BoolToInt(_src[i].IsImmediate),
//                                BoolToInt(_src[i].IsIndirect),
//                                BoolToInt(_src[i].IsIndexed),
//                                BoolToInt(_src[i].IsBaseRelative),
//                                BoolToInt(_src[i].IsPCRelative),
//                                BoolToInt(_src[i].IsExtended)));
//                    }
//                }
//            } catch (Exception ex) {
//                System.err.println("Exception:" + ex.getMessage());
//            }
//        }

        for (int i = 0; i < _symbolTable.length(); i++) {
            if (_symbolTable._hash[i] != null) {
                HashValue hash = _symbolTable._hash[i];
                SourceCodeLine srcLine = (SourceCodeLine) hash.Value;
                //System.out.println(String.format("Symbol %s \t with memory location %s  stored at position %d", hash.Key, Integer.toHexString(srcLine.Position), hash.Position));
            }
        }
    }
    
    
    private int GetPositionDifference(int pos1, int pos2){

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
            System.out.println(out);
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
                System.out.println(out);
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
    //if a command isn't preproc,
    //it's a sicop
    Boolean IsPreproc = false;
    Boolean IsComment = false;
    //Signifies the line is an Adddress operation (WORD, BYTE, ect)
    Boolean IsAddressOperation = false;
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
    int Position;
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