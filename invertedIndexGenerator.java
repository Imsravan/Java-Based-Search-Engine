
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Class to generate posting list for the given document.
 */
public class invertedIndexGenerator {

   private int termIndex = 0;

   Map<String, postingList> indexMap = new HashMap<String, postingList>();

   Map<String, postingList> stat_Map = new HashMap<String, postingList>();

   List<Integer> termsIndexList = new ArrayList<Integer>();

   Map<Integer, List<Integer>> documentTermsIndexMap = new HashMap<Integer, List<Integer>>();

   Map<Integer, Integer> documentTermsCountMap = new HashMap<Integer, Integer>();

   /**
    * Deletes special characters from the given string.
    * 
    * @param line
    * @return a string with all special character replaces.
    */
   private String deleteSpecialChars(String line) {

      line = line.replaceAll("[^a-zA-Z\\s]", "");

      return line;
   }

   /**
    * Method that calculates the index of all the words in the document.
    * Map of term and its corresponding posting list.
    */
   private Map<String, postingList> calculateIndex(String[] word, int docNum) {
      for (int i = 0; i < word.length; i++) {

         if (!word[i].isEmpty()) {

            word[i] = word[i].toLowerCase();

            if (word[i].endsWith(".")) {
               word[i] = word[i].substring(0, word[i].length() - 1);
            }

            word[i] = word[i].trim();

            termIndex++;

            // Check if the word is already encountered
            if (!indexMap.containsKey(word[i])) {
               processNewWord(docNum, word[i]);
            } else {
               processOldWord(word[i], docNum);
            }
         }
      }
      return indexMap;
   }

   /**
    * Method to Process new word.
    */
   private void processNewWord(int docNum, String word) {
      int documentCount = 0;

      documentTermsIndexMap = new HashMap<Integer, List<Integer>>();
      documentTermsCountMap = new HashMap<Integer, Integer>();
      termsIndexList = new ArrayList<Integer>();
      postingList pList = new postingList();

      pList.setDocumentIndex(docNum);

      documentTermsCountMap.put(docNum, 1);
      pList.setDocTermsCountMap(documentTermsCountMap);

      termsIndexList.add(termIndex);
      documentTermsIndexMap.put(docNum, termsIndexList);

      pList.setDocTermsIndexMap(documentTermsIndexMap);

      documentCount = pList.getDocumentCount();
      pList.setDocumentCount(documentCount + 1);

      indexMap.put(word, pList);
   }

   /**
    * Method to Process already existing word in the map.
    */
   private void processOldWord(String word, int docNum) {
      postingList pList = new postingList();
      int oldDocNum = 0;
      int docCount = 0;
      int termCount = 0;

      pList = indexMap.get(word);

      oldDocNum = pList.getDocumentIndex();
      documentTermsIndexMap = pList.getDocTermsIndexMap();
      documentTermsCountMap = pList.getDocTermsCountMap();

      if (oldDocNum == docNum) {
         termsIndexList = documentTermsIndexMap.get(docNum);
         termCount = documentTermsCountMap.get(docNum);
         termCount++;
      } else {
         pList.setDocumentIndex(docNum);
         docCount = pList.getDocumentCount();
         pList.setDocumentCount(docCount + 1);

         termsIndexList = new ArrayList<Integer>();
         termCount = 1;
      }

      termsIndexList.add(termIndex);
      documentTermsIndexMap.put(docNum, termsIndexList);
      pList.setDocTermsIndexMap(documentTermsIndexMap);

      documentTermsCountMap.put(docNum, termCount);
      pList.setDocTermsCountMap(documentTermsCountMap);

      indexMap.put(word, pList);
   }

   /**
    * Method that reads the input file and process it to generate the posting list
    * Map of term's posting list and the basic statistics of the file.
    */
   public Map<String, Map<String, postingList>> readFile(String fileName) {
      Scanner fileScanner = null;
      String line;
      String[] word;
      int docNum = 0;
      int newLine = 0;

      Map<String, Map<String, postingList>> indexStatMap = new HashMap<String, Map<String, postingList>>();

      try {
         fileScanner = new Scanner(new File(fileName));

         while (fileScanner.hasNextLine()) {
            line = fileScanner.nextLine();

            if (newLine >= 1 && line.isEmpty()) {
               newLine++;
            } else {
               newLine = 0;
            }

            if (line.isEmpty() && newLine == 0) {
               docNum++;
               newLine++;
               postingList pList = new postingList();
               pList.setTotalDocument(docNum + 1);
               pList.setTermCount(termIndex);
               stat_Map.put("#", pList);
               stat_Map.put("" + (docNum - 1), pList);
               termIndex = 0;
            } else {
               // System.out.println("aabnh");
               line = deleteSpecialChars(line);
               // System.out.println(line);
               word = line.split(" ");
               calculateIndex(word, docNum);
            }
         }
         postingList pList = new postingList();
         pList.setTermCount(termIndex);
         stat_Map.put("" + (docNum), pList);

         indexStatMap.put("index", indexMap);
         indexStatMap.put("statistic", stat_Map);
      } catch (FileNotFoundException e) {
         System.out.println("Could not open the file, please provide a proper file path. ");
      }

      return indexStatMap;
   }
}
