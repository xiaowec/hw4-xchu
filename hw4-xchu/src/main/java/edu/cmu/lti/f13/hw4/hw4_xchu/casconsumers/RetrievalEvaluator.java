package edu.cmu.lti.f13.hw4.hw4_xchu.casconsumers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.collection.CasConsumer_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.util.ProcessTrace;

import edu.cmu.lti.f13.hw4.hw4_xchu.typesystems.Document;
import edu.cmu.lti.f13.hw4.hw4_xchu.typesystems.Token;
import edu.cmu.lti.f13.hw4.hw4_xchu.utils.Utils;


public class RetrievalEvaluator extends CasConsumer_ImplBase {

	/** query id number **/
	public ArrayList<Integer> qIdList;

	/** query and text relevant values **/
	public ArrayList<Integer> relList;
	
	/** term and frequency values **/
	public ArrayList<HashMap<String, Integer>> termList;
	
	/** raw sentences **/
	public ArrayList<String> sentenceList;
	
	/** query term **/
	public HashMap<Integer, Integer> queryMap;
	
	/** rank list for result **/
	public ArrayList<Integer> rankList;
	
	public String similarity_method = "cos_similarity";

		
	public void initialize() throws ResourceInitializationException {

		qIdList = new ArrayList<Integer>();

		relList = new ArrayList<Integer>();
		
		termList = new ArrayList<HashMap<String,Integer>>();
		
		sentenceList = new ArrayList<String>();
		
		queryMap = new HashMap<Integer, Integer>();
		
		rankList = new ArrayList<Integer>();
		
	}

	/**
	 * 1. construct the global word dictionary 2. keep the word
	 * frequency for each sentence
	 */
	@Override
	public void processCas(CAS aCas) throws ResourceProcessException {

		JCas jcas;
		try {
			jcas =aCas.getJCas();
		} catch (CASException e) {
			throw new ResourceProcessException(e);
		}

		FSIterator it = jcas.getAnnotationIndex(Document.type).iterator();
	
		if (it.hasNext()) {
			Document doc = (Document) it.next();

			//Make sure that your previous annotators have populated this in CAS
			FSList fsTokenList = doc.getTokenList();
			ArrayList<Token> tokenList = Utils.fromFSListToCollection(fsTokenList, Token.class);
			HashMap<String, Integer> tokenMap = new HashMap<String, Integer>();
			for(Token token:tokenList){
			  tokenMap.put(token.getText(), token.getFrequency());
			}
			
			int queryid = doc.getQueryID();
			int relvalue = doc.getRelevanceValue();
			
			qIdList.add(queryid);
			relList.add(relvalue);
			termList.add(tokenMap);
			sentenceList.add(doc.getText());
			
			if (relvalue == 99) {
        queryMap.put(queryid, termList.indexOf(tokenMap));
      }

		}

	}

	/**
	 * 1. Compute Cosine Similarity and rank the retrieved sentences 2.
	 * Compute the MRR metric
	 */
	@Override
	public void collectionProcessComplete(ProcessTrace arg0)
			throws ResourceProcessException, IOException {

		super.collectionProcessComplete(arg0);
		
		
		//store the result for similarity measure
		double[] scorelist = new double[qIdList.size()];

		//compute the cosine similarity measure
		for (int position = 0; position < qIdList.size(); position++) {
      int queryid = qIdList.get(position);
      int relvalue = relList.get(position);
      
      double similarity = 0.0;
      
      if (relvalue != 99){
        //get document vector
        Map<String, Integer> docVector = termList.get(position);
        
        //get query vector
        int qposition = queryMap.get(queryid);
        Map<String, Integer> queryVector = termList.get(qposition);
        
        if(similarity_method == "cos_similarity"){
          similarity = computeCosineSimilarity(queryVector, docVector);
        }
        else if (similarity_method == "dice_similarity") {
          similarity = computeDiceSimilarity(queryVector, docVector);
        }
        else if (similarity_method == "jaccard_similarity"){
          similarity = computeJaccradSimilarity(queryVector, docVector);
        }
        
      }
      
      //store queryid,position and similarity for each answer
      scorelist[position] = similarity;
      
    }
		
		//compute the rank of retrieved sentences
		int queryid = 0;
		for (int i = 0; i < queryMap.size(); i++) {
      queryid = i+1;
      ArrayList<double[]> tempList = new ArrayList<double[]>();
      
      //sort answers and get rank
      for (int j = 0; j < qIdList.size(); j++) {
        if (qIdList.get(j) == queryid) { 
          double[] temparray = new double[2];
          temparray[0] = j;
          temparray[1] = scorelist[j];
          tempList.add(temparray);
        }
      }
      
      //rank answers
      Collections.sort(tempList, comparator);
      
      for (double[] element : tempList) {
        int position = (int) element[0];
        double score = element[1];
        
        if (relList.get(position) == 1) {
          System.out.print("Score: "+score);
          System.out.print("  rank= "+(tempList.indexOf(element)+1));
          System.out.print("  rel=1 qid="+qIdList.get(position)+" "+sentenceList.get(position));
          System.out.print("\n");
          rankList.add(tempList.indexOf(element)+1);
        }
      }
    }
		
		//compute the metric:: mean reciprocal rank
		double metric_mrr = compute_mrr();
		System.out.println(" (MRR) Mean Reciprocal Rank ::" + metric_mrr);
	}

	/**
	 * 
	 * @return cosine_similarity
	 */
	private double computeCosineSimilarity(Map<String, Integer> queryVector,
			Map<String, Integer> docVector) {
		double cosine_similarity= 0.0;
		double query_lenth = 0.0;
		double doc_lenth = 0.0;

		//compute cosine similarity between two sentences
		for (Entry<String, Integer> entry: queryVector.entrySet()) {
		  System.out.print(entry.getKey()+" ");
		}
		System.out.println("\n");
		for (Entry<String, Integer> entry: docVector.entrySet()) {
		  System.out.print(entry.getKey()+" ");
		}

		for (Entry<String, Integer> entry : queryVector.entrySet()) {
		  String qterm = entry.getKey();
		  if (docVector.containsKey(qterm)) {
        cosine_similarity += entry.getValue() * docVector.get(qterm);
      }
      query_lenth += Math.pow(entry.getValue(), 2.0);
    }
		
		for (Entry<String, Integer> entry : docVector.entrySet()){
		  doc_lenth += Math.pow(entry.getValue(), 2.0);
		}
		
		//normalize query and doc length
		query_lenth = Math.sqrt(query_lenth);
		doc_lenth = Math.sqrt(doc_lenth);
		cosine_similarity = cosine_similarity/(query_lenth * doc_lenth);
		
		System.out.println(cosine_similarity);
		
		return cosine_similarity;
	}
	
	/**
   * 
   * @return jaccard_similarity
   */
	private double computeJaccradSimilarity(Map<String, Integer> queryVector,
	        Map<String, Integer> docVector) {
	  double similarity = 0.0;
	  double intersect = 0.0;
	  double union = 0.0;
	  
	  for (Entry<String, Integer> entry : queryVector.entrySet()){
	    union += 1;
	    if (docVector.containsKey(entry.getKey())) {
        intersect += 1;
      }
	  }
	  
	  for (Entry<String, Integer> entry: docVector.entrySet()) {
      if (!queryVector.containsKey(entry.getKey())) {
        union += 1;
      }
    }
         
	  similarity = intersect/union;
	  return similarity;
	}
	
	/**
   * 
   * @return dice_similarity
   */
	private double computeDiceSimilarity(Map<String, Integer> queryVector,
          Map<String, Integer> docVector){
	  double similarity = 0.0;
	  double intersect = 0.0;
	  double union = 0.0;
	  
	  for(Entry<String, Integer> entry: queryVector.entrySet()){
	    if (docVector.containsKey(entry.getKey())) {
        intersect += 1;
      }
	  }
	  
	  union = queryVector.size() + docVector.size();
	  similarity = 2*intersect/union;
	  
	  return similarity;
	}
	
	/**
	 * 
	 * @return mrr
	 */
	private double compute_mrr() {
		double metric_mrr=0.0;

		//compute Mean Reciprocal Rank (MRR) of the text collection
		for (Integer rank : rankList) {
      metric_mrr += (double)1/rank;
    }
		metric_mrr = metric_mrr/rankList.size();
		
		return metric_mrr;
	}
	
public static Comparator<double[]> comparator = new Comparator<double[]>() {
    
    public int compare(double[] a, double[] b)
    {
      if (a[1] < b[1]) {
        return 1;
      }
      if (a[1] > b[1]) {
        return -1;
      }
      return 0;
    }
  };

}
