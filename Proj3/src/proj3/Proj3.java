/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package proj3;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 *
 * @author e5008222
 */
public class Proj3 {

    static int hashTableSize = 200;
    static HashTable sicOps = new HashTable(hashTableSize);
    static HashTable src;
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
            LoadOpCodes(opFileName);

            if (sicOps.length() > 0) {
                System.out.println(sicOps.length() + " OpCodes loaded into hashtable");
            }

            if (inputFile.length() > 0) {
                AssemblePass1(inputFile);
            }

        } else {
        }

    }

    public static void AssemblePass1(String inputFile) {
        
        
    }

    /**
     * @param args the command line arguments
     */
    public static void LoadOpCodes(String inputFileName) {

        System.out.println("Project Started");


        if (inputFileName.length() > 0) {
            try {
                FileReader fr = new FileReader(inputFileName);
                BufferedReader br = new BufferedReader(fr);

                String line;

                while ((line = br.readLine()) != null) {
                    //System.out.println(line);

                    String[] lineTokens = line.split("\\s+");

                    String out = "";
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

                        sicOps.Add(hash);
                        //hash Key value

//                        out += ", stored in position " + hashValue + "!";
                    } else {
                        if (lineTokens.length == 1) {
                            //if only one value was supplied, assume
                            //a loookup is desired
                            String lookupKey = lineTokens[0];

                            HashValue hash = sicOps.Find(lookupKey);
                                
                            //look for the hash Key at the assumed hash value.
                            //if the program encounters a empty value in the array
                            //where a field should be, it is assumed that the value
                            //being saught doesn't exist.

                        }
                    }

                    System.out.println(out);
                }

                System.out.println("\n--------------------------");
                System.out.println("Hash Table contains:");
                for (int i = 0; i < hashTableSize; i++) {
//                    if (sicOps[i] != null) {
//                        System.out.println("Hash Pisition:" + i + "  \tKey: " + sicOps[i].Key + "  \t   Value:" + sicOps[i].OpCode);
//                    }
                }
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

    private static int _hashTableSize = 200;
    private static HashValue[] _hash;

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

    public boolean Add(HashValue item) {
        
        int hashValue = GetHashValue(item.Key);

        String out = "Key " + item.Key + " hashed to position " + hashValue;

        //if the item Key is empty, store value in that position
        if (_hash[hashValue] == null) {
            item.Position = hashValue;
            _hash[hashValue] = item;
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
            
            hashValue = hashPosition;
        }
        System.out.println(out);
        
        return true;
    }

    public HashValue Find(String key) {

        int hashedValue = GetHashValue(key);
        boolean foundHash = false;
        String out = "";

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

    protected static int GetHashValue(String s) {

        int stringValue = 0;
        char[] chars = s.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            stringValue += (int) chars[i];
        }

        return stringValue % _hashTableSize;
    }
    
    protected static int length(){
        return _hashTableSize;
    }
}

class HashValue {
    int Position;
    String Key;
    Object Value;
}

class SicOperation {

    int Size;
    String OpCode;
}
