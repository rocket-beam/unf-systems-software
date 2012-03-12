/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package proj3;

import java.awt.Label;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 *
 * @author e5008222
 */
public class Proj3 {

    protected int _sicOpsTableSize = 200;
    protected int _symbolTableSize = 50;
    protected int _defaultStartPosition = 100;
    protected HashTable _sicOps = new HashTable(_sicOpsTableSize);
    protected HashTable _preprocs = new HashTable(20);
    protected HashTable _symbolTable = new HashTable(_symbolTableSize);
    protected SourceCodeLine[] _src = new SourceCodeLine[200];
    ;
    static int startPosition;

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
            Proj3 proj = new Proj3();

            proj.LoadOpCodes(opFileName);
            
            proj.InitPreprocessorCommands();

            if (proj._sicOps.length() > 0) {
                System.out.println(proj._sicOps.length() + " OpCodes loaded into hashtable");
            }

            if (inputFile.length() > 0) {
                proj.AssemblePass1(inputFile);
            }

        } else {
            System.out.println("No Input File specified!");
        }

    }

    
    public void InitPreprocessorCommands(){
        
        _preprocs.Add("Start", "Start");
        _preprocs.Add("END", "End");
        _preprocs.Add("BASE", "Base");
        _preprocs.Add("EQU", "EQU");
        
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


                    //if the line contains more than 1 token, assume it's 
                    //not a comment
                    if (lineTokens.length > 0 && lineTokens[0].trim().startsWith(".")) {

                        src.Source = line;
                        src.Position = currentPosition;
                    } else {
                        src.Source = line;

                        //if there's a label attached tot he line, add it.
                        src.Label = line.substring(0, 7).trim();

                        if (line.length() > 9) {
                            String extender = line.substring(9, 10).trim();
                            if (extender.equals("+")) {
                                src.IsExtended = true;
                            } else {
                                src.IsExtended = false;
                            }


                            if (line.length() >= 15) {
                                src.Nemonic = line.substring(10, 16).trim();
                            }


                            String nixbp = line.substring(18, 19).trim();


                            if (line.length() > 19) {
                                if(line.length()>27)
                                    src.Operand = line.substring(19, 28).trim();
                                else
                                    src.Operand = line.substring(19).trim();
                            }

                            if (line.length() > 29) {
                                src.Comment = line.substring(29).trim();
                            }




                            switch (nixbp) {
                                case "@":
                                    src.IsIndirect = true;
                                    break;
                                case "#":
                                    src.IsImmediate = true;
                                    break;
                                default:
                                    break;
                            }

                            switch (src.Nemonic.trim().toLowerCase()) {
                                case "start":
                                    startPosition = HexToInt(src.Operand);
                                    currentPosition = startPosition;
                                    break;
                                default:
                                    break;
                            }

                            if (startPosition == 0) {
                                startPosition = HexToInt(String.format("%d", _defaultStartPosition));
                                currentPosition = startPosition;
                            }

                        }
                        //try to find nmeunomic in sic operations.
                        try {
                            src.Position = currentPosition;
                            HashValue hashedOp = _sicOps.Find(src.Nemonic);
                            SicOperation op = new SicOperation();

                            //if it can, add the size of the operation ot the current position
                            if (hashedOp != null) {
                                op = (SicOperation) hashedOp.Value;
                                currentPosition += op.Size;
                            }

                            //if extended, add an extra byte;
                            if (src.IsExtended) {
                                currentPosition += 1;
                            }

                            switch (src.Nemonic.toLowerCase().trim()) {
                                case "word":
                                    currentPosition += 3;
                                    break;
                                case "resw":
                                    try {
                                        currentPosition += Integer.parseInt(src.Operand) * 3;
                                    } catch (Exception ex) {
                                        System.out.println("Error Parsing RESW value");
                                    }
                                    break;
                                case "resb":
                                    try {
                                        currentPosition += Integer.parseInt(src.Operand);
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

                    System.out.println(out);

                }

                System.out.println("\n--------------------------");
                System.out.println("Source Code File:");
                for (int i = 0; i < _src.length; i++) {
                    if(_src[i] != null){
                        System.out.println(String.format("%s : %s", Integer.toHexString(_src[i].Position), _src[i].Source));
                    }
                }
                
                for(int i=0; i<_symbolTable.length(); i++){
                    if(_symbolTable._hash[i]!=null){
                        HashValue hash = _symbolTable._hash[i];
                        SourceCodeLine srcLine = (SourceCodeLine)hash.Value;
                        System.out.println(String.format("Symbol %s \t with memory location %s  stored at position %d", hash.Key, Integer.toHexString(srcLine.Position), hash.Position  ));
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

    protected int HexToInt(String hex) {
        return Integer.parseInt(hex, 16);
    }

    /**
     * @param args the command line arguments
     */
    public void LoadOpCodes(String inputFileName) {

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

                    //System.out.println(out);
                }

                System.out.println("\n--------------------------");
                System.out.println("Hash Table contains:");
//                for (int i = 0; i < _sicOpsTableSize; i++) {
//                    if (sicOps[i] != null) {
//                        System.out.println("Hash Pisition:" + i + "  \tKey: " + sicOps[i].Key + "  \t   Value:" + sicOps[i].OpCode);
////                    }
//                }
            } catch (FileNotFoundException ex) {
                System.out.println("File " + inputFileName + " not found!");
            } catch (IOException ex) {
                System.out.println("There was an error reading the input file:" + ex.getMessage());
            }
        } else {
            System.out.println("No input file specified");
        }
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

        int hashValue = GetHashValue(item.Key);

        String out = "Key " + item.Key + " hashed to position " + hashValue + " of " + _hashTableSize;

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

            item.Position = hashPosition;
            _hash[hashPosition] = item;
            occupiedSpace++;
            hashValue = hashPosition;
        }

        if (occupiedSpace > _hashTableSize * 0.8) {
            Rehash();
        }

        System.out.println(out);

        return true;
    }

    public HashValue Find(String key) {

        String out = "";
        int hashedValue = GetHashValue(key);
        boolean foundHash = false;


        while (foundHash == false && _hash[hashedValue] != null) {
            //if you find it, Huzzah!
            if (_hash[hashedValue].Key.compareTo(key) == 0) {
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
            out = "Found Key " + key
                    + " at position " + hashedValue;
            System.out.println(out);
            return _hash[hashedValue];
        } else {
            out = "Could not find key " + key;
            System.out.println(out);
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

    String Label;
    String Nemonic;
    String Operand;
    String Comment;
    Boolean IsExtended;
    Boolean IsImmediate;
    Boolean IsPCRelative;
    Boolean IsBaseRelative;
    Boolean IsIndirect;
    String Source;
    int Position;
}

class SicOperation {

    int Size;
    String OpCode;
}
