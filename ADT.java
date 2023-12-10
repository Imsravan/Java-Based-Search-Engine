import java.util.*;

public class ADT {

   public static HashMap<String, HashMap<Integer, Integer>> nextCache = new HashMap<>();

   public static HashMap<String, HashMap<Integer, Integer>> prevCache = new HashMap<>();

   /**
    * Method that gets the posting list of a term from the map.
    */
   private List<Integer> getDocumentList(String term, Map<String, postingList> indexMap) {
      postingList pList = indexMap.get(term);

      List<Integer> docLst = null;

      if (null != pList) {
         Map<Integer, List<Integer>> positionsInDocument = pList.getDocTermsIndexMap();

         Collection<Integer> docNumCol = positionsInDocument.keySet();

         docLst = new ArrayList<Integer>();

         Iterator<Integer> docIter = docNumCol.iterator();

         while (docIter.hasNext()) {
            docLst.add(docIter.next());
         }

         Collections.sort(docLst);
      }

      return docLst;
   }

   public int nextDoc(String term, int docId, Map<String, postingList> indexMap) {
      int nextDocId = Integer.MAX_VALUE;

      List<Integer> docLst = getDocumentList(term, indexMap);

      for (int i = 0; i < docLst.size(); i++) {
         if (docLst.get(i) > docId) {
            int position = next(term, docLst.get(i), indexMap, -1100);
            if (position > 0)
               return docLst.get(i);
         }
      }

      return nextDocId;
   }

   public int docRight(String query, int docId, Map<String, postingList> indexMap) {
      if (isPhraseOrTerm(query))
         return nextDoc(query, docId, indexMap);

      int nextDocId = Integer.MIN_VALUE;

      if (query.contains("_")) {
         String[] subqueries = query.split("_");
         for (String subquery : subqueries) {
            int right = docRight(subquery, docId, indexMap);
            if (nextDocId < right)
               nextDocId = right;
         }
      }

      return nextDocId;
   }

   public int docLeft(String query, int docId, Map<String, postingList> indexMap) {
      if (isPhraseOrTerm(query))
         return prevDoc(query, docId, indexMap);

      int nextDocId = Integer.MAX_VALUE;

      if (query.contains("_")) {
         String[] subqueries = query.split("_");
         for (String subquery : subqueries) {
            int left = docLeft(subquery, docId, indexMap);
            if (nextDocId > left)
               nextDocId = left;
         }
      }

      return nextDocId;
   }

   public int prevDoc(String term, int docId, Map<String, postingList> indexMap) {
      int prevDocId = Integer.MAX_VALUE;

      List<Integer> docLst = getDocumentList(term, indexMap);

      for (int i = 0; i < docLst.size(); i++) {
         if (docLst.get(i) < docId) {
            int position = next(term, docLst.get(i), indexMap, -1100);
            if (position > 0)
               return docLst.get(i);
         }
      }

      return prevDocId;
   }

   public int next(String term, int docId, Map<String, postingList> indexMap, int current) {
      int pos = -999; // We considered negative values ~ infinity since docs with negative values will
                      // not exist

      if (!indexMap.containsKey(term))
         return pos;

      postingList pList = indexMap.get(term);
      if (!pList.getDocTermsIndexMap().containsKey(docId)) {
         return pos;
      }

      List<Integer> positionsList = pList.getDocTermsIndexMap().get(docId);

      int size = positionsList.size();

      if (positionsList == null || size == 0)
         return pos;

      if (positionsList.get(size - 1) <= current)
         return pos;

      int cachedPos = -999;

      HashMap<Integer, Integer> nextPosition = new HashMap<>();

      if (positionsList.get(0) > current) {
         nextPosition.put(docId, 0);
         nextCache.put(term, nextPosition);
         return positionsList.get(0);
      }

      if (nextCache.containsKey(term)) {
         cachedPos = nextCache.get(term).get(docId);
      }

      int low = 0, high = 0, jump;

      if (cachedPos > 0 && positionsList.get(cachedPos) <= current) {
         low = cachedPos;
      }

      jump = 1;
      high = low + jump;

      while (high < size && positionsList.get(high) <= current) {
         low = high;
         jump = jump * 2;
         high = low + jump;
      }

      if (high > size)
         high = size;

      cachedPos = binarySearchNext(positionsList, low, high, current);
      nextPosition.put(docId, cachedPos);
      nextCache.put(term, nextPosition);

      return positionsList.get(cachedPos);

   }

   public static int binarySearchNext(List<Integer> positionList, int low, int high, int current) {

      int mid = 0;

      while (high - low > 1) {
         mid = (low + high) / 2;
         if (current >= positionList.get(mid)) {
            low = mid;
         } else
            high = mid;
      }
      return high;
   }

   public static int binarySearchPrev(List<Integer> positionList, int low, int high, int current) {

      int mid = 0;

      while (high - low > 1) {
         mid = (low + high) / 2;
         if (current <= positionList.get(mid)) {
            low = mid;
         } else
            high = mid;
      }
      return high;
   }

   public int first(String term, int docId, Map<String, postingList> indexMap) {
      int pos = Integer.MIN_VALUE;

      if (!indexMap.containsKey(term))
         return pos;

      postingList pList = indexMap.get(term);
      if (!pList.getDocTermsIndexMap().containsKey(docId)) {
         return pos;
      }

      List<Integer> positions = pList.getDocTermsIndexMap().get(docId);

      if (positions.size() > 0)
         return positions.get(0);

      return pos;
   }

   public int last(String term, int docId, Map<String, postingList> indexMap) {
      int pos = Integer.MIN_VALUE;

      if (!indexMap.containsKey(term))
         return pos;

      postingList pList = indexMap.get(term);
      if (!pList.getDocTermsIndexMap().containsKey(docId)) {
         return pos;
      }

      List<Integer> positions = pList.getDocTermsIndexMap().get(docId);

      if (positions.size() > 0)
         return positions.get(positions.size() - 1);

      return pos;
   }

   public int prev(String term, int docId, Map<String, postingList> indexMap, int current) {
      int pos = -999;

      if (!indexMap.containsKey(term))
         return pos;

      postingList pList = indexMap.get(term);
      if (!pList.getDocTermsIndexMap().containsKey(docId)) {
         return pos;
      }

      List<Integer> positionsList = pList.getDocTermsIndexMap().get(docId);

      int size = positionsList.size();

      if (positionsList == null || size == 0)
         return pos;

      if (positionsList.get(0) >= current)
         return pos;

      int cachedPos = -999;

      HashMap<Integer, Integer> prevPosition = new HashMap<>();

      if (positionsList.get(size - 1) < current) {
         prevPosition.put(docId, size - 1);
         prevCache.put(term, prevPosition);
         return positionsList.get(size - 1);
      }

      if (prevCache.containsKey(term)) {
         cachedPos = nextCache.get(term).get(docId);
      }

      int low = 0, high = 0, jump;

      if (cachedPos > 0 && positionsList.get(cachedPos) <= current) {
         high = cachedPos + 1;
      } else {
         high = size - 1;
      }

      jump = 1;
      low = high - jump;

      //
      while (low >= 0 && positionsList.get(low) >= current) {
         high = low;
         jump = 2 * jump;
         low = high - jump;
      }

      if (low < 0) {
         low = 0;
      }

      cachedPos = binarySearchPrev(positionsList, low, high, current);
      prevPosition.put(docId, cachedPos);
      prevCache.put(term, prevPosition);

      return positionsList.get(cachedPos);

   }

   public static boolean isPhraseOrTerm(String query) {
      String[] tokens = query.split("\\s+");
      for (String token : tokens) {
         if (token.equalsIgnoreCase("AND") || token.equalsIgnoreCase("OR") || token.equalsIgnoreCase("NOT")) {
            return false;
         }
      }
      return true;
   }

   List<Integer> nextPhrase(String terms[], int docId, Map<String, postingList> indexMap, int position) {
      ArrayList<Integer> res = new ArrayList<>();

      int v = position;
      int n = terms.length;
      for (int i = 0; i < n; i++) {
         v = next(terms[i], docId, indexMap, v);
         if (v < 0) {
            res.add(Integer.MAX_VALUE);
            res.add(Integer.MAX_VALUE);
            return res;
         }
      }
      int u = v;
      for (int i = n - 2; i >= 0; i--) {
         u = prev(terms[i], docId, indexMap, u);
      }

      if (v - u == n - 1) {
         res.add(u);
         res.add(v);
      } else
         return nextPhrase(terms, docId, indexMap, u);

      return res;
   }

   List<Integer> prevPhrase(String terms[], int docId, Map<String, postingList> indexMap, int position) {
      ArrayList<Integer> res = new ArrayList<>();

      int v = position;
      int n = terms.length;
      for (int i = n - 1; i >= 0; i--) {
         v = prev(terms[i], docId, indexMap, v);
         if (v < 0) {
            res.add(Integer.MIN_VALUE);
            return res;
         }
      }

      int u = v;
      for (int i = 0; i < n; i++) {
         u = prev(terms[i], docId, indexMap, u);
      }

      if (u - v == n - 1) {
         res.add(v);
         res.add(u);
      } else
         return prevPhrase(terms, docId, indexMap, u);

      return res;
   }

}
