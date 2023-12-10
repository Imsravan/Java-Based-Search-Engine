
import java.util.*;

/**
 * Class that stores the posting list values.
 */

public class postingList {

   private Map<Integer, List<Integer>> docTermsIndexMap = new HashMap<Integer, List<Integer>>();

   private Map<Integer, Integer> docTermsCountMap = new HashMap<Integer, Integer>();

   private List<Integer> termIndexLst = new ArrayList<Integer>();

   private int documentIndex;

   private int termCount;

   private int documentCount;

   private int totalDocument;

   /**
    * Method to get Term count.
    */
   public int getTermCount() {
      return termCount;
   }

   /**
    * Method that sets term count.
    */
   public void setTermCount(int termCount) {
      this.termCount = termCount;
   }

   /**
    * Method that gets total document count
    */
   public int getTotalDocument() {
      return totalDocument;
   }

   public void setTotalDocument(int totalDoc) {
      this.totalDocument = totalDoc;
   }

   public Map<Integer, List<Integer>> getDocTermsIndexMap() {
      return docTermsIndexMap;
   }

   public void setDocTermsIndexMap(Map<Integer, List<Integer>> docTermsIndexMap) {
      this.docTermsIndexMap = docTermsIndexMap;
   }

   public Map<Integer, Integer> getDocTermsCountMap() {
      return docTermsCountMap;
   }

   public void setDocTermsCountMap(Map<Integer, Integer> docTermCountMap) {
      this.docTermsCountMap = docTermCountMap;
   }

   public int getDocumentIndex() {
      return documentIndex;
   }

   public void setDocumentIndex(int docIndex) {
      this.documentIndex = docIndex;
   }

   public List<Integer> getTermIndexLst() {
      return termIndexLst;
   }

   public void setTermIndexLst(List<Integer> termIndexLst) {
      this.termIndexLst = termIndexLst;
   }

   public int getDocumentCount() {
      return documentCount;
   }

   public void setDocumentCount(int docCount) {
      this.documentCount = docCount;
   }

}
