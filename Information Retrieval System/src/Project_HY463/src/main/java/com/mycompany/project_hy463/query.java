package com.mycompany.project_hy463;

import static com.mycompany.project_hy463.indexer.listFilesForFolder;
import gr.uoc.csd.hy463.Topic;
import gr.uoc.csd.hy463.TopicsReader;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import mitos.stemmer.Stemmer;

/**
 *
 * @author csd3195,csd3609
 */
public class query
{

    public static class Term
    {

        int df;
        long pointer;

        public Term(int df, long pointer)
        {
            this.df = df;
            this.pointer = pointer;
        }

        public void setDf(int df)
        {
            this.df = df;
        }

        public void setPointer(long pointer)
        {
            this.pointer = pointer;
        }

        public int getDf()
        {
            return this.df;
        }

        public long getPointer()
        {
            return this.pointer;
        }
    }

    public static void main(String[] args) throws FileNotFoundException, IOException, Exception
    {

        String workingDir = System.getProperty("user.dir");
        String type; //Type of query.
        TreeMap<String, Term> vocabularyMap = new TreeMap<>(); //TreeMap with the terms of the vocabulary with the dfs and the pointers.
        TreeMap<String, Double> queryTermsMap = new TreeMap<>(); //TreeMap with the query terms as key, and the frequency as value, wich later will be thje weight.
        TreeMap<String, Double> scoreNumerator = new TreeMap<>(); //TreeMap with key the document pmcid and value the score numerator.
        TreeMap<String, ArrayList<Long>> pointersToPostingMap = new TreeMap<>(); //TreeMap with key the query terms, and values an ArrayList with all the pointers to posting for that term.
        TreeMap<String, Double> scores = new TreeMap<>(); //TreeMap with keys the path of the document and value the score.
        ArrayList<String> stopWordsGR = new ArrayList<>();
        ArrayList<String> stopWordsEN = new ArrayList<>();

        Term tempTerm;
        double maxQueryFreq = Double.MIN_VALUE;
        double queryNorm = 0;
        String[] splittedVocabularyLine = null;
        int topicNumber;

        // Make the arrays with the stopwords.
        File folder = new File(workingDir + "\\stoplists");
        ArrayList<String> stopwordsFilesPath = new ArrayList<>();
        listFilesForFolder(folder, stopwordsFilesPath);
        for (int i = 0; i < stopwordsFilesPath.size(); i++)
        {
            try ( BufferedReader reader = new BufferedReader(new FileReader(stopwordsFilesPath.get(i))))
            {
                String line;
                while ((line = reader.readLine()) != null)
                {
                    if (i == 0)
                    {
                        stopWordsEN.add(line);
                    }
                    else
                    {
                        stopWordsGR.add(line);
                    }
                }
            }
        }

        /* Save in memory the vocabulary file. */
        RandomAccessFile vocabularyFile = new RandomAccessFile(workingDir + "\\CollectionIndex\\VocabularyFile.txt", "rw");
        String vocabularyLine = vocabularyFile.readLine();
        System.out.println("Reading Vocabulary...");
        while (vocabularyLine != null)
        {
            if (!vocabularyLine.trim().isBlank()) // If line is not blank.
            {
                splittedVocabularyLine = vocabularyLine.split(" ");
                tempTerm = new Term(Integer.parseInt(splittedVocabularyLine[1]), Long.valueOf(splittedVocabularyLine[2])); // Arguments are, df and pointer to posting file.
                vocabularyMap.put(splittedVocabularyLine[0], tempTerm);
            }
            vocabularyLine = vocabularyFile.readLine(); //If line is blank, read the next line.
        }
        vocabularyFile.close();

        System.out.print("Vocabulary reding completed!\n");
        
        //Create results file.
        File resultsFile = new File(workingDir + "\\results.txt");
        boolean fdoc = resultsFile.createNewFile();
        if (fdoc)
        {
            System.out.println("results.txt has been created successfully");
        }
        else
        {
            System.out.println("results.txt already present at the specified location");
        }
        
        File eval_resultsFile = new File(workingDir + "\\eval_results.txt");
        boolean fevaldoc = eval_resultsFile.createNewFile();
        if (fevaldoc)
        {
            System.out.println("eval_results.txt has been created successfully");
        }
        else
        {
            System.out.println("eval_results.txt already present at the specified location");
        }
        FileWriter res_writer = new FileWriter(workingDir + "\\results.txt",true);  
        BufferedWriter res_buffer = new BufferedWriter(res_writer); 
        PrintWriter res_out = new PrintWriter(res_buffer);
        DecimalFormat decimalForm = new DecimalFormat("###.########");
        FileWriter eval_writer = new FileWriter(workingDir + "\\eval_results.txt",true);  
        BufferedWriter eval_buffer = new BufferedWriter(eval_writer); 
        PrintWriter eval_out = new PrintWriter(eval_buffer);
        RandomAccessFile postingFile = new RandomAccessFile(workingDir + "\\CollectionIndex\\PostingFile.txt", "rw");
        RandomAccessFile documentsFile = new RandomAccessFile(workingDir + "\\CollectionIndex\\DocumentsFile.txt", "rw");
        
        HashMap<String, Integer> docsQrel = new HashMap<>(); //HashMap with key the document and value the relevance.
        double DCG = 0;
        double IDCG = 0;
        double NDCG = 0;
        int qRes = 0;
        int relevantDocNum = 0;
        long qrelsPointer = 0;
        RandomAccessFile qrelsFile = new RandomAccessFile(workingDir + "\\qrels.txt", "rw");
        
        
        
        ArrayList<Topic> topics = TopicsReader.readTopics(workingDir + "\\topics\\topics.xml");
        System.out.print("\n Populating results.txt and eval_results.txt files.");
        
        //For each topic/query.
        for (Topic topic : topics) 
        {
            
            // Get the query.
            String[] queryArray = topic.getSummary().split(" ");
            // Get the type of the query.
            type = Stemmer.Stem(topic.getType().toString());
            topicNumber = topic.getNumber();
            

            Stemmer.Initialize();
            for (String term : queryArray)
            {
                if (stopWordsGR.contains(term) || stopWordsEN.contains(term)) // If a stopword is found, continue to the next word.
                {
                    continue;
                }

                term = Stemmer.Stem(term);

                // For query type.
                if (vocabularyMap.containsKey(type)) // If type exists in vocabulary map.
                {
                    if (queryTermsMap.containsKey(type)) //If type exists in query Map.
                    {
                        double freq = queryTermsMap.get(type);
                        freq++;
                        queryTermsMap.replace(type, freq);
                    }
                    else // If type not in the queryMap, insert it with frequency 12.
                    {
                        queryTermsMap.put(type, 12.0); // frequency will be 12, because it is of greater value, than the other terms.
                        if (maxQueryFreq < 1.0)
                        {
                            maxQueryFreq = 1.0;
                        }
                    }
                }

                // For query terms.
                if (vocabularyMap.containsKey(term)) // If term exists in vocabulary map.
                {
                    if (queryTermsMap.containsKey(term)) //If term exists in query Map.
                    {
                        double freq = queryTermsMap.get(term);
                        freq++;
                        queryTermsMap.replace(term, freq);
                        if (maxQueryFreq < freq) //Save as max frequency, if greater than the already saved one.
                        {
                            maxQueryFreq = freq;
                        }
                    }
                    else // If term not in the queryMap, insert it with frequency 1.
                    {
                        queryTermsMap.put(term, 1.0);
                        if (maxQueryFreq < 1.0)
                        {
                            maxQueryFreq = 1.0;
                        }
                    }
                }
            }

            double tf;
            double weight;
            for (Map.Entry<String, Double> term : queryTermsMap.entrySet()) // Find the weight of each query term.
            {
                if (vocabularyMap.containsKey(term.getKey()))
                {
                    tf = (double) term.getValue() / maxQueryFreq;
                    weight = tf * vocabularyMap.get(term.getKey()).getDf();
                    queryNorm += Math.pow(weight, 2); //(tf*df)^2
                    queryTermsMap.replace(term.getKey(), weight); // From this moment we don't need the frequnecy of the term
                    // any more, so we replace it with the weight of the term.
                }
            }

            queryNorm = Math.sqrt(queryNorm); // The final norm of the query.

            /* Find the numerator of each document for the score. */
            long pointerToPosting;
            long stopingPointer;
            String nextTerm;
            String postingLine;
            String[] splittedPostingLine;
            double numerator;
            ArrayList<Long> tempArray;

            for (Map.Entry<String, Term> vocabularyTerm : vocabularyMap.entrySet())
            {
                if (queryTermsMap.containsKey(vocabularyTerm.getKey()))
                {
                    pointerToPosting = vocabularyMap.get(vocabularyTerm.getKey()).getPointer(); //Pointer to posting file.

                    nextTerm = vocabularyMap.higherEntry(vocabularyTerm.getKey()).getKey(); //Get next term and find the pointer to posting file, to stop to.
                    if (nextTerm != null)
                    {
                        stopingPointer = vocabularyMap.get(nextTerm).getPointer(); //Pointer to stop to.
                        postingFile.seek(pointerToPosting); //Go to the correct line.
                        postingLine = postingFile.readLine(); //Read the line.
                        splittedPostingLine = postingLine.split(" ");
                        while (pointerToPosting < stopingPointer)
                        {
                            if (pointersToPostingMap.containsKey(vocabularyTerm.getKey())) //Add the pointers of this term to the map.
                            {
                                tempArray = pointersToPostingMap.get(vocabularyTerm.getKey());
                                tempArray.add(pointerToPosting);
                                pointersToPostingMap.replace(vocabularyTerm.getKey(), tempArray);
                            }
                            else
                            {
                                tempArray = new ArrayList<>();
                                tempArray.add(pointerToPosting);
                                pointersToPostingMap.put(vocabularyTerm.getKey(), tempArray);

                            }
                            double df = vocabularyTerm.getValue().getDf();
                            tf = Double.valueOf(splittedPostingLine[1]);
                            double qweight = tf * df;
                            if (scoreNumerator.containsKey(splittedPostingLine[0]))
                            {
                                numerator = scoreNumerator.get(splittedPostingLine[0]);
                                scoreNumerator.replace(splittedPostingLine[0], numerator + qweight); //Update the numerator with the new value.
                            }
                            else
                            {
                                scoreNumerator.put(splittedPostingLine[0], qweight);
                            }
                            postingLine = postingFile.readLine();
                            pointerToPosting = postingFile.getFilePointer();
                        }
                    }
                    else
                    {
                        postingFile.seek(pointerToPosting); //Go to the correct line.
                        postingLine = postingFile.readLine(); //Read the line.
                        splittedPostingLine = postingLine.split(" ");
                        while (postingLine != null)
                        {
                            if (pointersToPostingMap.containsKey(vocabularyTerm.getKey())) //Add the pointers of this term to the map.
                            {
                                tempArray = pointersToPostingMap.get(vocabularyTerm.getKey());
                                tempArray.add(pointerToPosting);
                                pointersToPostingMap.replace(vocabularyTerm.getKey(), tempArray);
                            }
                            else
                            {
                                tempArray = new ArrayList<>();
                                tempArray.add(pointerToPosting);
                                pointersToPostingMap.put(vocabularyTerm.getKey(), tempArray);

                            }
                            double df = vocabularyTerm.getValue().getDf();
                            tf = Double.valueOf(splittedPostingLine[1]);
                            double qweight = tf * df;
                            if (scoreNumerator.containsKey(splittedPostingLine[0]))
                            {
                                numerator = scoreNumerator.get(splittedPostingLine[0]);
                                scoreNumerator.replace(splittedPostingLine[0], numerator + qweight); //Update the numerator with the new value.
                            }
                            else
                            {
                                scoreNumerator.put(splittedPostingLine[0], qweight);
                            }
                            postingLine = postingFile.readLine();
                            if (postingLine == null)
                            {
                                break;
                            }
                            pointerToPosting = postingFile.getFilePointer();
                        }
                    }
                }
            }

            /* Find the final score for each document. */
            double finalScore;
            String docName;
            String documentLine;
            String[] splittedDocumentLine;
            ArrayList<String> documentDone = new ArrayList<>(); // It will contain only the documents, wich have the final score computed.

            for (Map.Entry<String, ArrayList<Long>> pointers : pointersToPostingMap.entrySet())
            {
                ArrayList<Long> pointersArray = pointers.getValue();
                for (Long pointer : pointersArray)
                {
                    postingFile.seek(pointer);
                    postingLine = postingFile.readLine();
                    if (postingLine != null)
                    {
                        splittedPostingLine = postingLine.split(" ");
                        docName = splittedPostingLine[0];
                        if (!documentDone.contains(docName)) //If it does not contain the document, it means that the final score for that document is not caclulated.
                        {
                            if (scoreNumerator.containsKey(docName))
                            {
                                documentDone.add(docName); //Add the name of the document.

                                documentsFile.seek(Long.valueOf(splittedPostingLine[2].replace("\n", "")));
                                documentLine = documentsFile.readLine();
                                splittedDocumentLine = documentLine.split(" ");
                                finalScore = scoreNumerator.get(docName) / (Double.valueOf(splittedDocumentLine[splittedDocumentLine.length - 1]) * queryNorm);
                                scores.put(splittedDocumentLine[0], finalScore); //Doc name and score.
                            }
                        }
                    }
                }
            }
            
            
            // Sorting results in descending order.
            List<String> mapKeys = new ArrayList<>(scores.keySet());
            List<Double> mapValues = new ArrayList<>(scores.values());
            Comparator descending = Collections.reverseOrder();
            Collections.sort(mapValues, descending);
            Collections.sort(mapKeys, descending);
            LinkedHashMap<String, Double> sortedScores = new LinkedHashMap<>();
            Iterator<Double> valueIt = mapValues.iterator();
            while (valueIt.hasNext()) 
            {
                Double val = valueIt.next();
                Iterator<String> keyIt = mapKeys.iterator();

                while (keyIt.hasNext()) 
                {
                    String key = keyIt.next();
                    Double comp1 = scores.get(key);
                    Double comp2 = val;

                    if (comp1.equals(comp2)) 
                    {
                        keyIt.remove();
                        sortedScores.put(key, val);
                        break;
                    }
                }
            }
            
            
            
            // Find relevance from qrels file.
            docsQrel = new HashMap<>();
            qrelsFile.seek(qrelsPointer);
            String line = qrelsFile.readLine();
            String[] splittedLine = line.split("\\s+");
            int number = Integer.parseInt(splittedLine[0]);
            String doc;
            String relevance;
            
            
            //Find the qres only for the topic we want.
            relevantDocNum = 0;
//            System.out.println("!!!number: "+number);
//            System.out.println("!!!topicNumber: "+topicNumber);
            while(number == topicNumber)
            {
                doc = splittedLine[2];
                relevance = splittedLine[3];

                if(Integer.parseInt(relevance) > 0)
                {
                    relevantDocNum++;
                }    
                docsQrel.put(doc, Integer.parseInt(relevance));
//              System.out.println("INSERTED -> number: "+number+" doc: "+doc +" qrel: "+Integer.parseInt(relevance));

                qrelsPointer = qrelsFile.getFilePointer();
                line = qrelsFile.readLine();
                if(line != null)
                {
                    splittedLine = line.split("\\s+");
                    number = Integer.parseInt(splittedLine[0]);
                }
                else
                {
                    break;
                }
            }         
            
            DCG = 0;
            IDCG = 0;
            NDCG = 0;
            qRes = 0;
            
            // Populate results.txt, eval_results.txt file and finding the NDCG.
            int rank = 1;
//          System.out.println("Eurethenta: "+sortedScores.size());
            
            // Gia eurethenta.
            int nonRelevantDocsNum = 0;
            double bprefSum = 0;
            double avepSum = 0;
            int relevantDocNow = 0;
            
            
            System.out.println("\n Calculating metrics for topic "+topicNumber);
            for (Map.Entry<String, Double> documentScore : sortedScores.entrySet())
            {
                res_out.append(topicNumber + " 0 " + documentScore.getKey() + " " + rank + " " + decimalForm.format(documentScore.getValue()) + "\n");  
                
                
                // Calculate DCG,IDCG.
                qRes = 0;
                if(!docsQrel.isEmpty())
                {
                    if(docsQrel.containsKey(documentScore.getKey()))
                    {
                        qRes = docsQrel.get(documentScore.getKey());
//                      System.out.println("documentScore.key(): "+documentScore.getKey());
//                      System.out.println("qres: "+qRes+"\n");

                        if(rank > 2)
                        {
                            if(qRes != 0)
                            {
                                DCG += 1/(Math.log(rank)/Math.log(2)); // 1/logbase2(rank).
                            }
                        }
                        else
                        {                    
                            DCG += qRes;
                        }
                        if(qRes == 0)
                        {
                            nonRelevantDocsNum ++; //For bpref.
                        }
                        else
                        {
                            relevantDocNow ++;
                            bprefSum += 1-((double)nonRelevantDocsNum/(double)relevantDocNum); //For bpref.
                            avepSum += 1*((double)relevantDocNow/(double)rank); // For AVEP. (rel(i)*|Ei tomi Σ|/i).
                        }
                    }
                }
                else
                {
                    NDCG = -1;
                }
                
                rank++;
                
                if(rank > 1000)
                {
                    break;
                }
            }
            
//          System.out.println("SUNAFI: "+relevantDocNum);
            
            
            // Gia sunafi.
            rank = 1;
            if(!docsQrel.isEmpty())
            {
                for (Map.Entry<String, Integer> qrel : docsQrel.entrySet())
                {
                    if(qrel.getValue() !=0)
                    {
                        if(rank > 2)
                        {
                            IDCG += 1/(Math.log(rank)/Math.log(2)); //rel(i)/logbase2(rank).
                        }
                        else
                        {
                            IDCG += 1; //rel(i).
                        } 
                    }
                    rank ++;
                }
            }
            
            
            // Calcucate NDCG.
            if(NDCG != -1)
            {
                if(IDCG != 0)
                {
                    NDCG = DCG/IDCG;
                }
                else
                {
                    NDCG = -1;
                }
            }
            String res = topicNumber + " " + (1/(double)relevantDocNum)*bprefSum + " " + (1/(double)relevantDocNum)*avepSum + " " + NDCG + "\n";
//            System.out.println("RES: "+res);
//            System.out.println("NDCG: "+NDCG);
//            System.out.println("DCG: "+DCG);
//            System.out.println("IDCG: "+IDCG);
//            System.out.println("relevantDocNum: "+relevantDocNum);
//            System.out.println("bprefSum: "+bprefSum);
//            System.out.println("avepSum: "+avepSum);
//            System.out.println("AVEP: "+(1/(double)relevantDocNum)*avepSum);
//            System.out.println("BPREF: "+(1/(double)relevantDocNum)*bprefSum);
            eval_out.append(res);
            
        }  
        System.out.println("\n Populating completed.");
        res_buffer.close();  
        res_out.close();
        res_writer.close();
        eval_buffer.close();  
        eval_out.close();
        eval_writer.close();
        postingFile.close();
        documentsFile.close();
        qrelsFile.close(); 
    }
}
