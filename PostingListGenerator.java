import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

public class PostingListGenerator {

   /*
    * docTermsIndexMap is 1-indexed
    * docTermsCountMap is 0-indexed
    */
   static Map<String, postingList> indexMap = new HashMap<>();

   // Statistics of each document like number of terms etc
   static Map<String, postingList> statisticMap;

   static int totalDocumentCount;

   static class Pair {
      double score;
      int docid, doc, documentFrequency;
      String term;

      Pair(double score, int docid) {
         this.score = score;
         this.docid = docid;
      }

      Pair(int doc, String term) {
         this.doc = doc;
         this.term = term;
      }

      Pair(String term, int documentFrequency) {
         this.term = term;
         this.documentFrequency = documentFrequency;
      }

      int getDoc() {
         return doc;
      }

      int getDocID() {
         return docid;
      }

      public String getTerm() {
         return term;
      }

      public double getScore() {
         return score;
      }

      public int getdocumentFrequency() {
         return documentFrequency;
      }

   }

   /**
    * Method that calculates TF
    */
   private double calculate_TF(int documentFrequency) {

      double tf = 0;

      double documentFreqDouble = (double) documentFrequency;

      tf = (Math.log(documentFreqDouble) / Math.log(2)) + 1;

      return tf;
   }

   static double getLavg() {

      double sumOfLengthOfDocuments = 0;
      for (String s : statisticMap.keySet()) {
         if (!s.equalsIgnoreCase("#")) {
            sumOfLengthOfDocuments = sumOfLengthOfDocuments + statisticMap.get(s).getTermCount();
         }
      }

      return (sumOfLengthOfDocuments / totalDocumentCount);
   }

   public static double tfBM25(String term, int docId, Set<Integer> termDocList) {

      int ftd = indexMap.get(term).getDocTermsIndexMap().get(docId).size();

      int N = totalDocumentCount;
      // Numbers of terms in the document with docID containing the term
      int Nt = termDocList.size();

      double k1 = 1.2;
      double b = 0.75;
      // length of document with docID containing the term
      int l_d = 1;

      for (String s : statisticMap.keySet()) {
         if (s.equalsIgnoreCase("#")) {
            continue;
         }
         int id = Integer.valueOf(s);
         if (id == docId) {
            l_d = statisticMap.get(s).getTermCount();
         }
      }

      double l_avg = getLavg();

      double idf = Math.log(N / (double) Nt) / Math.log(2);
      double tf_bm25 = (ftd * (k1 + 1)) / (ftd + k1 * ((1 - b) + b * (l_d / l_avg)));
      return idf * tf_bm25;
   }

   public static List<Pair> rankBM25DocumentAtATime(List<String> terms, int k) {
      // result = [score, docid]
      ADT indexAdt = new ADT();

      PriorityQueue<Pair> result = new PriorityQueue<>(Comparator.comparingDouble(Pair::getScore));

      for (int i = 0; i < k; i++) {
         result.add(new Pair(0, Integer.MAX_VALUE));
      }

      // result = [nextDoc, term]
      PriorityQueue<Pair> termsPQ = new PriorityQueue<>(Comparator.comparingDouble(Pair::getDoc));

      for (String term : terms) {
         termsPQ.add(new Pair(indexAdt.nextDoc(term, Integer.MIN_VALUE, indexMap), term));
      }

      while (!termsPQ.isEmpty() && termsPQ.peek().getDoc() < Integer.MAX_VALUE) {
         int d = termsPQ.peek().getDoc();
         double score = 0;
         while (!termsPQ.isEmpty() && termsPQ.peek().getDoc() == d) {
            Pair currentTerm = termsPQ.poll();
            String t = currentTerm.getTerm();
            score += tfBM25(t, d, indexMap.get(t).getDocTermsIndexMap().keySet()); // You need to implement tfBM25

            int nextDoc = indexAdt.nextDoc(t, d, indexMap); // You need to implement nextDoc
            termsPQ.add(new Pair(nextDoc, t));
         }

         if (!result.isEmpty() && score > result.peek().getScore()) {
            result.poll();
            result.add(new Pair(score, d));
         }
      }

      List<Pair> allResults = new ArrayList<>();
      while (!result.isEmpty()) {
         Pair pair = result.poll();
         if (pair.getScore() != 0) {
            allResults.add(pair);
         }
      }

      // Adjust k if necessary
      if (allResults.size() < k) {
         k = allResults.size();
      }

      // Get the top k results and reverse the order
      List<Pair> topKResults = allResults.subList(allResults.size() - k, allResults.size());
      Collections.reverse(topKResults);

      // Returning or processing the topKResults
      // Example: Printing top k results
      for (Pair pair : topKResults) {
         System.out.println("DocId: " + pair.getDoc() + ", Score: " + pair.getScore());
      }

      return topKResults;

   }

   /**
    * Method that calculates IDF
    * 
    */
   private double calculate_IDF(int totalDocuments, int termDocumentCount) {

      double idf = 0f;

      double documentRatio = (double) totalDocuments / (double) termDocumentCount;

      idf = (Math.log(documentRatio) / Math.log(2));

      return idf;
   }

   /**
    * Method that generates posting list
    */
   public Map<String, Map<String, postingList>> generateIndex(String fileName) {
      invertedIndexGenerator IndexGen = new invertedIndexGenerator();

      Map<String, Map<String, postingList>> index_StatisticMap = IndexGen.readFile(fileName);

      indexMap = index_StatisticMap.get("index");

      statisticMap = index_StatisticMap.get("statistic");

      totalDocumentCount = statisticMap.get("#").getTotalDocument();

      return index_StatisticMap;
   }

   public Map<Integer, Integer> getTotalTermsInDocument(String query, String fileName) {

      Map<String, Map<String, postingList>> index_StatisticMap = generateIndex(fileName);
      Map<Integer, Integer> documentTermMap = new HashMap<>();

      if (index_StatisticMap == null || index_StatisticMap.isEmpty()) {
         System.out.println("Error!! Cannot calculate index");
      } else {
         Map<String, postingList> statisticMap = index_StatisticMap.get("statistic");

         for (String s : statisticMap.keySet()) {
            if (s.equalsIgnoreCase("#"))
               continue;
            postingList plist = statisticMap.get(s);
            int docId = Integer.parseInt(s);
            documentTermMap.put(docId, plist.getTermCount());
         }
      }

      return documentTermMap;

   }

   /**
    * Method that calculates the cosine rank
    */
   public double[][] calculateCosineRank(String query, String fileName) {

      double[][] sortedDocumentScoreArray = null;

      Map<String, Map<String, postingList>> index_StatisticMap = generateIndex(fileName);

      if (index_StatisticMap == null || index_StatisticMap.isEmpty()) {
         System.out.println("Error!! Cannot calculate index");
      } else {
         indexMap = index_StatisticMap.get("index");

         if (checkIfQueryExists(query, indexMap)) {

            statisticMap = index_StatisticMap.get("statistic");

            double score = 0;

            List<Integer> readDocumentList = new ArrayList<Integer>();

            totalDocumentCount = statisticMap.get("#").getTotalDocument();

            sortedDocumentScoreArray = new double[totalDocumentCount][2];

            Map<Integer, Map<String, Double>> normDocTermTfIdfMap2 = calculate_Doc_Vector(indexMap, statisticMap);

            String[] queryArr = query.split(" ");

            Map<String, Double> normalizedQueryVectorLst = calculateNormalizedQueryVector(queryArr, indexMap,
                  totalDocumentCount);

            Map<Integer, Double> doc_Score_Map = calculate_Doc_Score(
                  normDocTermTfIdfMap2, normalizedQueryVectorLst, totalDocumentCount);

            Object[] scoreArray = doc_Score_Map.values().toArray();

            Arrays.sort(scoreArray);

            int index = 0;

            for (int i = scoreArray.length - 1; i >= 0; i--) {
               score = (Double) scoreArray[i];

               for (int j = 0; j < doc_Score_Map.size(); j++) {
                  if (doc_Score_Map.get(j) == score && !readDocumentList.contains(j)) {
                     readDocumentList.add(j);
                     sortedDocumentScoreArray[index][0] = j;
                     sortedDocumentScoreArray[index][1] = score;
                     index++;
                  }
               }
            }
         }
      }
      return sortedDocumentScoreArray;
   }

   private Map<Integer, Double> calculate_Doc_Score(Map<Integer, Map<String, Double>> normDocTermTfIdfMap2,
         Map<String, Double> normalizedQueryVectorLst, int totalDocCount) {

      Map<Integer, Double> docScores = new HashMap<>();
      for (Integer docId : normDocTermTfIdfMap2.keySet()) {
         Map<String, Double> documentNormalizedVector = normDocTermTfIdfMap2.get(docId);
         Double score = 0.00;
         for (String term : normalizedQueryVectorLst.keySet()) {
            if (documentNormalizedVector.containsKey(term)) {
               score = score + documentNormalizedVector.get(term) * normalizedQueryVectorLst.get(term);
            }
         }
         docScores.put(docId, score);
      }

      return docScores;
   }

   /**
    * Check if a query word is present in the posting list.
    */
   private boolean checkIfQueryExists(String query, Map<String, postingList> indexMap) {
      boolean isPresent = false;

      String[] queryArray = query.split(" ");

      for (int i = 0; i < queryArray.length; i++) {
         if (indexMap.containsKey(queryArray[i].toLowerCase())) {
            isPresent = true;
            break;
         }
      }

      return isPresent;
   }

   private Map<Integer, Map<String, Double>> calculate_Doc_Vector(
         Map<String, postingList> indexMap, Map<String, postingList> statisticMap) {

      Map<Integer, List<Double>> docTermTfIdfMap = new HashMap<Integer, List<Double>>();

      List<Double> termTfIdfLst = new ArrayList<Double>();

      Map<Integer, Map<String, Double>> termTfIdfMap = new HashMap<>();

      Collection<String> termCol;

      ADT indexAdt = new ADT();

      postingList pList = new postingList();

      String term;

      int docId = 0, docFreq = 0, termDocumentCount = 0;

      double tf = 0, idf = 0;

      int totalDocCount = statisticMap.get("#").getTotalDocument();

      termCol = indexMap.keySet();

      Object[] termArray = termCol.toArray();

      Arrays.sort(termArray);

      for (int i = 0; i < totalDocCount; i++) {
         termTfIdfLst = new ArrayList<Double>();

         for (int j = 0; j < termArray.length; j++) {
            docId = i - 1;

            term = (String) termArray[j];
            docId = indexAdt.nextDoc(term, docId, indexMap);

            if (docId == i) {
               pList = indexMap.get(term);
               docFreq = pList.getDocTermsCountMap().get(i);
               tf = calculate_TF(docFreq);

               termDocumentCount = pList.getDocumentCount();
               idf = calculate_IDF(totalDocCount, termDocumentCount);
               termTfIdfLst.add(tf * idf);
               if (termTfIdfMap.containsKey(docId)) {
                  Map<String, Double> map = termTfIdfMap.get(docId);
                  map.put(term, tf * idf);
                  termTfIdfMap.put(docId, map);
               } else {
                  Map<String, Double> map = new HashMap<>();
                  map.put(term, tf * idf);
                  termTfIdfMap.put(docId, map);
               }
            } else {
               termTfIdfLst.add(0d);
            }
         }
         docTermTfIdfMap.put(i, termTfIdfLst);
      }

      Map<Integer, Map<String, Double>> normalizedDocTermTfIdfMap = calculateNormalisedDocumentVector(termTfIdfMap);
      return normalizedDocTermTfIdfMap;
   }

   private Map<Integer, Map<String, Double>> calculateNormalisedDocumentVector(
         Map<Integer, Map<String, Double>> termTfIdfMap) {
      Map<Integer, Map<String, Double>> normalizedDocTermTfIdfMap = new HashMap<>();
      for (Integer docID : termTfIdfMap.keySet()) {
         Map<String, Double> map = termTfIdfMap.get(docID);
         double summation = 0;
         for (String term : map.keySet()) {
            summation = summation + map.get(term) * map.get(term);
         }
         double normalizedSum = Math.sqrt(summation);
         Map<String, Double> normalizedMap = new HashMap<>();
         for (String term : map.keySet()) {
            double normalizedValue = (map.get(term) / normalizedSum);
            normalizedMap.put(term, normalizedValue);
         }

         normalizedDocTermTfIdfMap.put(docID, normalizedMap);
      }
      return normalizedDocTermTfIdfMap;
   }

   /**
    * Calculates the query vector for a given query.
    */
   private Map<String, Double> calculateNormalizedQueryVector(String[] query,
         Map<String, postingList> indexMap, int totalDocCount) {

      double idf = 0, tf = 0;

      Object[] termArray;

      String term = null;

      double normalizedVal = 0;

      postingList pList = new postingList();

      Collection<String> termColumn = indexMap.keySet();

      termArray = termColumn.toArray();

      Arrays.sort(termArray);

      Map<String, Integer> queryFrequencyMap = calculateQueryFreq(query);
      Map<String, Double> queryTfIDFMap = new HashMap<>();
      Map<String, Double> normQueryTfIDFMap = new HashMap<>();

      for (int i = 0; i < termArray.length; i++) {
         term = (String) termArray[i];

         if (queryFrequencyMap.containsKey(term)) {
            pList = indexMap.get(term);

            tf = calculate_TF(queryFrequencyMap.get(term));
            idf = calculate_IDF(totalDocCount, pList.getDocumentCount());

            if (queryTfIDFMap.containsKey(term)) {
               queryTfIDFMap.put(term, tf * idf);
            } else {
               queryTfIDFMap.put(term, tf * idf);
            }

         }
      }

      for (String t : queryTfIDFMap.keySet()) {
         normalizedVal = normalizedVal + (queryTfIDFMap.get(t) * queryTfIDFMap.get(t));
      }

      normalizedVal = Math.sqrt(normalizedVal);

      for (String s : queryTfIDFMap.keySet()) {
         normQueryTfIDFMap.put(s, queryTfIDFMap.get(s) / normalizedVal);
      }

      return normQueryTfIDFMap;
   }

   /**
    * Method that calculates the frequency of occurance of each term in query.
    */
   private Map<String, Integer> calculateQueryFreq(String[] query) {
      Map<String, Integer> queryFrequencyMap = new HashMap<String, Integer>();

      int count = 0;

      for (int i = 0; i < query.length; i++) {
         query[i] = query[i].toLowerCase();

         if (queryFrequencyMap.containsKey(query[i])) {
            count = queryFrequencyMap.get(query[i]);
            count = count + 1;
            queryFrequencyMap.put(query[i], count);
         } else {
            queryFrequencyMap.put(query[i], 1);
         }
      }

      return queryFrequencyMap;
   }
}
