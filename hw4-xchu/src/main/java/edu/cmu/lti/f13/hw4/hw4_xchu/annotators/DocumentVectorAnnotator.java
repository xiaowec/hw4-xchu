package edu.cmu.lti.f13.hw4.hw4_xchu.annotators;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.internal.util.TextTokenizer;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.tcas.Annotation;

import edu.cmu.lti.f13.hw4.hw4_xchu.typesystems.Document;
import edu.cmu.lti.f13.hw4.hw4_xchu.typesystems.Token;
import edu.cmu.lti.f13.hw4.hw4_xchu.utils.Utils;

public class DocumentVectorAnnotator extends JCasAnnotator_ImplBase {

  @Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {

		FSIterator<Annotation> iter = jcas.getAnnotationIndex().iterator();
		if (iter.isValid()) {
			iter.moveToNext();
			Document doc = (Document) iter.get();
			createTermFreqVector(jcas, doc);
		}

	}
	/**
	 * 
	 * @param jcas
	 * @param doc
	 */

	private void createTermFreqVector(JCas jcas, Document doc) {
	  
		String docText = doc.getText();
		Map<String, Integer> termMap = new HashMap<String, Integer>(); 
		
	  //TO DO: construct a vector of tokens and update the tokenList in CAS
		String[] words = docText.split("[ \\,\\;\\.]");
		for (int i = 0; i < words.length; i++) {
		  words[i] = words[i].toLowerCase();
		  if (!Utils.judgeStopword(words[i])) {
        if (termMap.containsKey(words[i])) {
          int freq = termMap.get(words[i]);
          termMap.put(words[i], freq+1);
        }
        else {
          termMap.put(words[i], 1);
        }
      }
    }
		
		//Construct ArrayList of Tokens
		ArrayList<Token> tlist = new ArrayList<Token>();
		Iterator iter = termMap.entrySet().iterator();
		
		//add tokens to ArrayList
		while (iter.hasNext()) {
		  Token token = new Token(jcas);
		  Entry<String, Integer> entry = (Entry<String, Integer>) iter.next();
      token.setText(entry.getKey());
      token.setFrequency(entry.getValue());
      tlist.add(token);
    }
		
		//convert ArrayList to FSList
		doc.setTokenList(Utils.fromCollectionToFSList(jcas, tlist));
	}

}
