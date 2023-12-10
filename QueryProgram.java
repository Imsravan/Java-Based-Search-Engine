import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
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

public class QueryProgram {

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

    public double LMD(List<String> terms) {
        Map<String, Integer> countofTerms = new HashMap<>();

        for (String s : terms) {
            if (!countofTerms.containsKey(s)) {
                countofTerms.put(s, 1);
            } else {
                int c = countofTerms.get(s);
                c++;
                countofTerms.put(s, c);
            }
        }

        for (String s : statisticMap.keySet()) {

        }

        return 0.00;
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

    public static List<Pair> rankBM25DocumentAtATimeRM(List<String> terms, int k, String measure) {
        // result = [score, docid]
        ADT indexAdt = new ADT();

        long startTime = System.nanoTime();

        Map<String, Integer> countTerms = new HashMap<>();

        PriorityQueue<Pair> result = new PriorityQueue<>(Comparator.comparingDouble(Pair::getScore));

        for (int i = 0; i < k; i++) {
            result.add(new Pair(0, Integer.MAX_VALUE));
        }

        // result = [nextDoc, term]
        PriorityQueue<Pair> termsPQ = new PriorityQueue<>(Comparator.comparingDouble(Pair::getDoc));

        for (String term : terms) {
            if (countTerms.containsKey(term)) {
                int c = countTerms.get(term);
                c++;
                countTerms.put(term, c);
            } else {
                countTerms.put(term, 1);

            }
            termsPQ.add(new Pair(indexAdt.nextDoc(term, Integer.MIN_VALUE, indexMap), term));
        }

        while (!termsPQ.isEmpty() && termsPQ.peek().getDoc() < Integer.MAX_VALUE) {
            int d = termsPQ.peek().getDoc();
            double score = 0;
            while (!termsPQ.isEmpty() && termsPQ.peek().getDoc() == d) {
                Pair currentTerm = termsPQ.poll();
                String t = currentTerm.getTerm();
                // score += tfBM25(t, d, indexMap.get(t).getDocTermsIndexMap().keySet()); // You
                // need to implement tfBM25
                double ftd = indexMap.get(t).getDocTermsCountMap().get(d);
                double ld = statisticMap.get(Integer.toString(d)).getTermCount();
                double lam = 0.5;
                double qt = countTerms.get(t);
                double l_t = getLT(t);
                double l_c = getLC();
                double N = k;
                if (measure.equalsIgnoreCase("DFR")) {
                    double dfr_score = qt
                            * (Math.log(1 + (l_t / N) / Math.log(2)) + (ftd * Math.log(1 + (N / l_t))) / Math.log(2))
                            / (ftd + 1);
                    score += dfr_score;
                } else if (measure.equalsIgnoreCase("LMJM")) {
                    double lmjm_score = qt * (Math.log(1 + ((1 - lam) / lam) * (ftd / ld) * (l_c / l_t)) / Math.log(2));
                    score += lmjm_score;
                }
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
        // List<Pair> topKResults = allResults.subList(allResults.size() - k,
        // allResults.size());
        List<Pair> topKResults = allResults;

        Collections.reverse(topKResults);

        // Returning or processing the topKResults
        // Example: Printing top k results
        // for (Pair pair : topKResults) {
        // System.out.println("DocId: " + pair.getDocID() + ", Score: " +
        // pair.getScore());
        // }

        long endTime = System.nanoTime();

        // Calculate time difference
        long timeElapsed = endTime - startTime;

        try {
            // Create FileWriter object with filePath
            FileWriter fileWriter = new FileWriter("trec_top_file.txt");
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

            int query_id = 0;

            for (String s : statisticMap.keySet()) {
                double score = 0;
                if (s.equals("#"))
                    continue;
                int documentId = Integer.parseInt(s);
                for (Pair p : topKResults) {
                    if (p.getDocID() == documentId) {
                        score = p.getScore();
                        break;
                    }
                }
                String formattedString = String.format("%d 0 DOC-%04d 1 %f doc_time", query_id, documentId, score);
                bufferedWriter.write(formattedString.toString());
                bufferedWriter.write("\n");

            }
            bufferedWriter.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return topKResults;

    }

    private static int getLC() {
        int count = 0;
        for (String s : statisticMap.keySet()) {
            if (s.equals("#"))
                continue;
            count = count + statisticMap.get(s).getTermCount();
        }

        return count;
    }

    private static int getLT(String t) {
        int count = 0;
        Map<Integer, Integer> docTermsCountMap = indexMap.get(t).getDocTermsCountMap();
        for (Integer i : docTermsCountMap.keySet()) {
            count = count + docTermsCountMap.get(i);
        }

        return count;
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

    public static void main(String[] args) throws IOException {

        if (args.length < 3) {
            System.out.println("Please input the correct number of agruments");
            System.exit(0);
        }

        String corpus_fileName = args[0];
        String query = args[1];
        String relevance_measure = args[2];

        // String fileName = "test.txt";
        // String index_fileName = "index.txt";
        // String query = "no sir";
        // String relevance_measure = "DFR";
        String q[] = query.split("\\s+");

        ArrayList<String> queryTerms = new ArrayList<>();

        for (int i = 0; i < q.length; i++) {
            queryTerms.add(q[i]);
        }

        IndexProgram ip = new IndexProgram();

        // Map<String, Map<String, postingList>> index_StatisticMap =
        // ip.decodeIndex(index_fileName);
        Map<String, Map<String, postingList>> index_StatisticMap = ip.generateIndex(corpus_fileName);
        indexMap = index_StatisticMap.get("index");

        statisticMap = index_StatisticMap.get("statistic");

        int totalDocumentCount = statisticMap.get("#").getTotalDocument();

        rankBM25DocumentAtATimeRM(queryTerms, totalDocumentCount, relevance_measure);

    }

}
