
//package proj1;
import java.io.FileReader;     
import java.io.BufferedReader;
import java.io.FileNotFoundException; 
import java.io.IOException;

public class Proj1 {

    
    static int hashTableSize = 50;
    static DictionaryItem[] hashTable = new DictionaryItem[hashTableSize];
     
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
        String inputFileName;
        
        System.out.println("Project Started");
        
        
        if(args.length>0)
        {
            inputFileName = args[0];
            
           
            
            try
            {
               FileReader fr = new FileReader(inputFileName);
               BufferedReader br = new BufferedReader(fr);
               
               String line;
               
               while((line=br.readLine())!=null)
               {
                   //System.out.println(line);
                   
                   String[] lineTokens = line.split(" ");
                   
                   String out ="";
                   //if there are more than one value on the line,
                   //assume that the line represents a value to store into 
                   //the hash table
                   if(lineTokens.length>1){
                       
                       DictionaryItem item = new DictionaryItem();
                       
                       //set key/value pair for dictionary item
                       item.key = lineTokens[0];
                       item.value = Integer.parseInt(lineTokens[1]);
                       
                       //hash key value
                       int hashValue = GetHashValue(item.key);
                       
                       out += "Key " + item.key + " hashed to position " + hashValue;
                       
                       //if the hash key is empty, store value in that position
                       if(hashTable[hashValue] == null)
                       {
                           hashTable[hashValue] = item;
                       }
                       else{
                           //look for the next position that is empty and store the value there.
                           int hashPosition = hashValue;
                           boolean foundDuplicate = false;
                           
                           while(hashTable[hashPosition] != null && foundDuplicate==false){
                               
                               
                               //if the key is already found, throw an error
                               if(hashTable[hashPosition].key.compareTo(item.key)==0)
                               {
                                   out = "Key " + item.key + " already exists!";
                                   foundDuplicate=true;
                               }
                               else{
                                   hashPosition++;
                                    if(hashPosition>=hashTableSize)
                                    {
                                            hashPosition =0;
                                    }

                                    if(hashPosition == hashValue)
                                    {
                                        //IncreaseHashTableSize
                                    }
                               }
                       
                           }
                           hashTable[hashPosition] = item;
                           hashValue = hashPosition;
                       }
                       out += ", stored in position " + hashValue +"!";
                   }
                   else{
                       if(lineTokens.length==1){
                            //if only one value was supplied, assume
                            //a loookup is desired
                            String lookupKey =  lineTokens[0];
                            int hashedValue = GetHashValue(lookupKey);
                            boolean foundHash = false;

                            //look for the hash key at the assumed hash value.
                            //if the program encounters a empty value in the array
                            //where a field should be, it is assumed that the value
                            //being saught doesn't exist.
                            while(foundHash == false && hashTable[hashedValue]!=null){
                                //if you find it, Huzzah!
                                if(hashTable[hashedValue].key.compareTo(lookupKey)==0)
                                {
                                    foundHash = true;
                                }
                                else
                                {
                                    //if not keep looking.
                                    hashedValue++;
                                    if(hashedValue==hashTableSize){
                                        hashedValue=0;
                                    }
                                }
                            }

                            if(foundHash==true)
                            {
                                out = "Found Key " + lookupKey + 
                                        " at position " + hashedValue + " with value " 
                                        + hashTable[hashedValue].value;
                            }

                            else{
                                out = "Could not find key " + lookupKey ;
                                }
                        }
                   }
                   
                   System.out.println(out);
               }
               
               System.out.println("\n--------------------------");
               System.out.println("Hash Table contains:");
               for(int i=0;i<hashTableSize; i++){
                   if(hashTable[i] != null){
                       System.out.println("Hash Pisition:" + i + "  \tKey: " + hashTable[i].key + "  \t   Value:"+hashTable[i].value);
                   }
               }
            }
            catch(FileNotFoundException ex)
            {
                System.out.println("File " +inputFileName+ " not found!");
            }
            catch(IOException ex)
            {
                System.out.println("There was an error reading the input file:" + ex.getMessage());
            }
        }
        else
        {
            System.out.println("No input file specified");
        }
    }
    
    protected static int GetHashValue(String s){
        
        int stringValue =0;
        char[] chars = s.toCharArray();
        
        for(int i=0; i< chars.length; i++)
        {
            stringValue += (int)chars[i];
        }    
        
        return stringValue%hashTableSize;
    }
}

class DictionaryItem{
     String key;
     int value;
}
