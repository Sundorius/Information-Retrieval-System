package com.mycompany.project_hy463;

/**
 *
 * @author csd3195,csd3609
 */
import gr.uoc.csd.hy463.NXMLFileReader;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.text.NumberFormat;
import mitos.stemmer.Stemmer;

public class indexer
{

    public static class Document
    {

        private final String path;
        private double norm;
        private int maxFreq;

        public Document(String path)
        {
            this.path = path;
            this.norm = 0.0;
            this.maxFreq = Integer.MIN_VALUE;
        }

        public void setNorm(double norm)
        {
            this.norm = norm;
        }

        public void setMaxFreq(int maxFreq)
        {
            this.maxFreq = maxFreq;
        }

        public String getPath()
        {
            return this.path;
        }

        public double getNorm()
        {
            return this.norm;
        }

        public int getMaxFreq()
        {
            return this.maxFreq;
        }
    }

    public static class DocOfTerm
    {

        private int frequency;
        private double tf;
        private ArrayList<Long> positions;

        public DocOfTerm()
        {
            positions = new ArrayList<>();
        }

        public void setFreq(int freq)
        {
            this.frequency = freq;
        }

        public void setTf(double tf)
        {
            this.tf = tf;
        }

        public void addPosition(long pos)
        {
            this.positions.add(pos);
        }

        public int getFreq()
        {
            return this.frequency;
        }

        public double getTf()
        {
            return this.tf;
        }

        public ArrayList<Long> getPositions()
        {
            return this.positions;
        }
    }

    // Returns the path of all the files.
    public static void listFilesForFolder(File folder , ArrayList<String> paths )
    {
        //ArrayList<String> paths = new ArrayList<>();
        for (File fileEntry : folder.listFiles())
        {
            if (fileEntry.isDirectory())
            {
                listFilesForFolder(fileEntry, paths);
            }
            else
            {
                paths.add(fileEntry.getAbsolutePath());
            }
        }
        //return paths;
    }

    // Creates the Collections index directory and the Documents File with the Vocabulary File.
    public static void makeVocabularyDocumentsFile() throws IOException
    {
        String workingDir = System.getProperty("user.dir");
//        if (!new File(workingDir + "\\CollectionIndex").mkdir())
//        {
//            throw new IOException("Failed creating directory CollectionIndex, in makeVocabularyDocumentsFile()");
//        }

        if (new File(workingDir + "\\CollectionIndex").mkdir()) // TODO REMOVE AND UNCOMMENT THE ABOVE!!
        {
            //throw new IOException("Failed creating directory CollectionIndex, in makeVocabularyDocumentsFile()");
        }

//        File vocabulary = new File(workingDir + "\\CollectionIndex\\VocabularyFile.txt");
//        boolean fvoc = vocabulary.createNewFile();
//        if (fvoc)
//        {
//            System.out.println("VocabularyFile has been created successfully");
//        }
//        else
//        {
//            System.out.println("VocabularyFile already present at the specified location");
//        }
        File document = new File(workingDir + "\\CollectionIndex\\DocumentsFile.txt");
        boolean fdoc = document.createNewFile();
        if (fdoc)
        {
            System.out.println("DocumentsFile has been created successfully");
        }
        else
        {
            System.out.println("DocumentsFile already present at the specified location");
        }
    }

    // Populates the Documents File.
    public static void populateDocumentFile(TreeMap<String, HashMap<String, DocOfTerm>> termsTree,
            HashMap<String, Document> documentsMap,
            HashMap<String, Long> docsPointerMap)
    {
        String workingDir = System.getProperty("user.dir");

        try
        {
            RandomAccessFile documentsFile = new RandomAccessFile(workingDir + "\\CollectionIndex\\DocumentsFile.txt", "rw");
            Integer freq = 0;
            Integer maxFreq = 0;

            HashMap<String, DocOfTerm> docsTermMap;
            DocOfTerm docTerm;
            Document docTemp;
            
            NumberFormat numFormat = NumberFormat.getInstance();
            numFormat.setMaximumIntegerDigits(2);
            numFormat.setMinimumIntegerDigits(2);
            numFormat.setMaximumFractionDigits(4);
            numFormat.setMinimumFractionDigits(4);
             

            for (Map.Entry<String, HashMap<String, DocOfTerm>> term : termsTree.entrySet()) // Iterate the terms.
            {
                docsTermMap = term.getValue();
                for (Map.Entry<String, DocOfTerm> doc : docsTermMap.entrySet())
                {
                    docTerm = doc.getValue();
                    docTemp = documentsMap.get(doc.getKey()); //Get document from documentsMap.
                    freq = docTerm.getFreq(); //Get frequency from termsTree document.
                    maxFreq = documentsMap.get(doc.getKey()).getMaxFreq(); //Get maxFreq from documentsMap.
                    double tf = (double) freq / maxFreq;
                    docTerm.setTf(tf); //Set TF for termsTree document.
                    docTemp.setNorm(0); // (tf*idf)^2 = weight

                    documentsMap.replace(doc.getKey(), docTemp); //Replace with the new value in documentsMap.
                    docsTermMap.replace(doc.getKey(), docTerm); //Replace with the new value in documents for terms.
                    termsTree.replace(term.getKey(), docsTermMap); //Replace with the new value in termsTree.
                }
            }
//--------------------------------------------------------------------------------------------  

            documentsFile.seek(documentsFile.length()); //Write at the end of the file, if file is not empty, 
            // else write at the  beggining.
            String dummyNorm = numFormat.format(99.9999);
            for (Map.Entry<String, Document> doc : documentsMap.entrySet())
            {
                documentsFile.readLine();
                docsPointerMap.put(doc.getKey(), documentsFile.getFilePointer()); //Stores the offset of the document in the file.

                docTemp = doc.getValue();
                String line = doc.getKey() + " " + docTemp.getPath() + " " + dummyNorm + "\n";
                documentsFile.writeBytes(line);
            }
            documentsFile.close();
            System.gc();
        }
        catch (IOException ex)
        {
            Logger.getLogger(indexer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    // Populates the Posting Files.
    public static void populatePostingFiles(TreeMap<String, HashMap<String, DocOfTerm>> termsTree,
            HashMap<String, Long> postingsPointerMap, HashMap<String, Long> docsPointerMap,
            Queue<File> postingsQueue, int numOfPostings)
    {
        HashMap<String, DocOfTerm> docsMap;
        DocOfTerm docTerm;

        String workingDir = System.getProperty("user.dir");

        try
        {
            File document = new File(workingDir + "\\CollectionIndex\\PostingFile" + numOfPostings + ".txt");
            boolean fdoc = document.createNewFile();
            if (fdoc)
            {
                System.out.println("Postings File has been created successfully");
            }
            else
            {
                System.out.println("Postings File already present at the specified location");
            }

            RandomAccessFile postingFile = new RandomAccessFile(workingDir + "\\CollectionIndex\\PostingFile" + numOfPostings + ".txt", "rw");
            for (Map.Entry<String, HashMap<String, DocOfTerm>> term : termsTree.entrySet()) // Iterate the terms.
            {
                postingFile.readLine();
                postingsPointerMap.put(term.getKey(), postingFile.getFilePointer());
                docsMap = term.getValue();
                for (Map.Entry<String, DocOfTerm> doc : docsMap.entrySet()) // Iterate the documents.
                {
                    docTerm = doc.getValue();
                    postingFile.writeBytes(doc.getKey() + " " + docTerm.getTf() + " " + docsPointerMap.get(doc.getKey()) + "\n");
                }
            }
//            postingFile.close();
//            postingFile = new RandomAccessFile(workingDir + "\\CollectionIndex\\PostingFile" + numOfPostings + ".txt", "rw");
//            for (Map.Entry<String, Long> term : postingsPointerMap.entrySet()) // Iterate the documents.
//            {
//                postingFile.seek(term.getValue());
//                System.out.println("~~~LINE: "+postingFile.readLine());
//            }
            postingFile.close();
            File postFile = new File(workingDir + "\\CollectionIndex\\PostingFile" + numOfPostings + ".txt");
            postingsQueue.add(postFile);
            docsMap = null;
            docTerm = null;
            postFile = null;
        }
        catch (IOException ex)
        {
            Logger.getLogger(indexer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    // Populates the Vocabulary File.
    public static void populateVocabularyFile(TreeMap<String, HashMap<String, DocOfTerm>> termsTree,
            HashMap<String, Long> postingsPointerMap, Queue<File> vocabularyQueue, int numOfVocabularies)
    {
        String workingDir = System.getProperty("user.dir");
        try
        {

            RandomAccessFile vocabularyFile = new RandomAccessFile(workingDir + "\\CollectionIndex\\VocabularyFile" + numOfVocabularies + ".txt", "rw");
            HashMap<String, DocOfTerm> docsMap;

            for (Map.Entry<String, HashMap<String, DocOfTerm>> term : termsTree.entrySet()) // Iterate the terms.
            {
                docsMap = term.getValue();
                vocabularyFile.writeBytes(term.getKey() + " " + docsMap.size() + " " + postingsPointerMap.get(term.getKey()) + "\n");

            }
            vocabularyFile.close();

//            RandomAccessFile vocabularyFi = new RandomAccessFile(workingDir + "\\CollectionIndex\\VocabularyFile" + numOfVocabularies + ".txt", "rw");
//            line = vocabularyFi.readLine();
//            System.out.println("\n!~~START VOC");
//            while (line != null) // Iterate the terms.
//            {
//                //vocabularyFile.seek(term.getValue());
//                System.out.println("~~LINE " + numOfVocabularies + ": " + line);
//                line = vocabularyFi.readLine();
//            }
//            System.out.println("\n!~~END VOC");
//            vocabularyFi.close();
            //vocabularyQueue.add(file);
            File vocFile = new File(workingDir + "\\CollectionIndex\\VocabularyFile" + numOfVocabularies + ".txt");
            vocabularyQueue.add(vocFile);
            System.gc();
        }
        catch (IOException ex)
        {
            Logger.getLogger(indexer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void mergeFiles(Queue<File> vocabulariesQueue, Queue<File> postingsQueue)
    {
        System.out.println("\n\nMerging Started.");
        String workingDir = System.getProperty("user.dir");
        int mergingCounter = 1;
        RandomAccessFile newVocFile;
        File vocFile2;
        File vocFile1;
        RandomAccessFile vocabularyFile1;
        RandomAccessFile vocabularyFile2;
        RandomAccessFile newPostFile;
        File postFile1;
        File postFile2;
        RandomAccessFile postingFile1;
        RandomAccessFile postingFile2;

        String vocabularyLine1;
        String nextVocabularyLine1;
        String vocabularyLine2;
        String nextVocabularyLine2;
        String postingLine1;
        String postingLine2;
        String[] splittedVocabularyLine1;
        String[] nextSplittedVocabularyLine1 = null;
        String[] splittedVocabularyLine2;
        String[] nextSplittedVocabularyLine2 = null;

        while (vocabulariesQueue.size() > 1) // If vocabulariesQueue has size <=1, means that the merging is complete.
        {
            try
            {
                newVocFile = new RandomAccessFile(workingDir + "\\CollectionIndex\\VocabularyFileMerged" + mergingCounter + ".txt", "rw");
                vocFile1 = vocabulariesQueue.poll();
                vocFile2 = vocabulariesQueue.poll();
                vocabularyFile1 = new RandomAccessFile(vocFile1.getAbsolutePath(), "r");
                vocabularyFile2 = new RandomAccessFile(vocFile2.getAbsolutePath(), "r");

                newPostFile = new RandomAccessFile(workingDir + "\\CollectionIndex\\PostingFileMerged" + mergingCounter + ".txt", "rw");
                postFile1 = postingsQueue.poll();
                postFile2 = postingsQueue.poll();
                postingFile1 = new RandomAccessFile(postFile1.getAbsolutePath(), "r");
                postingFile2 = new RandomAccessFile(postFile2.getAbsolutePath(), "r");

                vocabularyLine1 = vocabularyFile1.readLine();
                nextVocabularyLine1 = vocabularyFile1.readLine();
                vocabularyLine2 = vocabularyFile2.readLine();
                nextVocabularyLine2 = vocabularyFile2.readLine();

                postingLine1 = postingFile1.readLine();
                postingLine2 = postingFile2.readLine();

                long newPostingPointer = 5;
                long nextPostingPointer = 5;
                while (vocabularyLine1 != null || vocabularyLine2 != null)
                {
                    if (vocabularyLine1 == null || vocabularyLine2 == null)
                    {
                        break;
                    }

                    splittedVocabularyLine1 = vocabularyLine1.split(" ");
                    if (nextVocabularyLine1 != null)
                    {
                        nextSplittedVocabularyLine1 = nextVocabularyLine1.split(" ");
                    }
                    splittedVocabularyLine2 = vocabularyLine2.split(" ");
                    if (nextVocabularyLine2 != null)
                    {
                        nextSplittedVocabularyLine2 = nextVocabularyLine2.split(" ");
                    }

                    if (splittedVocabularyLine1[0].equals(splittedVocabularyLine2[0])) // If terms are equal.
                    {
                        /* For posting file */
                        newPostFile.readLine(); //Read the line from the new Posting file.
                        newPostingPointer = newPostFile.getFilePointer(); //Save the pointer of the new line.

                        /* For term form Vocabulary File 1 */
                        nextPostingPointer = Long.valueOf(splittedVocabularyLine2[2].replace("\n", ""));
                        if (nextVocabularyLine1 != null)
                        {
                            while (nextPostingPointer < Long.valueOf(nextSplittedVocabularyLine1[2].replace("\n", "")))
                            {
                                newPostFile.writeBytes(postingLine1 + "\n");
                                postingLine1 = postingFile1.readLine();
                                nextPostingPointer = postingFile1.getFilePointer();
                            }
                        }
                        else
                        {
                            while (postingLine1 != null)
                            {
                                newPostFile.writeBytes(postingLine1 + "\n");
                                postingLine1 = postingFile1.readLine();
                            }
                        }

                        /* For term form Vocabulary File 2 */
                        if (nextVocabularyLine2 != null)
                        {
                            while (nextPostingPointer < Long.valueOf(nextSplittedVocabularyLine2[2].replace("\n", "")))
                            {
                                newPostFile.writeBytes(postingLine2 + "\n");
                                postingLine2 = postingFile2.readLine();
                                nextPostingPointer = postingFile2.getFilePointer();
                            }
                        }
                        else
                        {
                            while (postingLine2 != null)
                            {
                                newPostFile.writeBytes(postingLine2 + "\n");
                                postingLine2 = postingFile2.readLine();
                            }
                        }

                        /* For vocabulary file */
                        newVocFile.writeBytes(splittedVocabularyLine1[0] + " "
                                + //Term.
                                (Integer.valueOf(splittedVocabularyLine1[1])
                                + Integer.valueOf(splittedVocabularyLine2[1])) + " "
                                + //Sum of dfs.
                                newPostingPointer + "\n");

                        vocabularyLine1 = nextVocabularyLine1;
                        nextVocabularyLine1 = vocabularyFile1.readLine();
                        vocabularyLine2 = nextVocabularyLine2;
                        nextVocabularyLine2 = vocabularyFile2.readLine();
                    }
                    else if (splittedVocabularyLine1[0].compareTo(splittedVocabularyLine2[0]) < 0) //If term1 < term2
                    {
                        /* For posting file */
                        newPostFile.readLine(); //Read the line from the new Posting file.
                        newPostingPointer = newPostFile.getFilePointer(); //Save the pointer of the new line.
                        nextPostingPointer = Long.valueOf(splittedVocabularyLine1[2].replace("\n", ""));
                        if (nextVocabularyLine1 != null)
                        {
                            while (nextPostingPointer < Long.valueOf(nextSplittedVocabularyLine1[2].replace("\n", "")))
                            {
                                newPostFile.writeBytes(postingLine1 + "\n");
                                postingLine1 = postingFile1.readLine();
                                nextPostingPointer = postingFile1.getFilePointer();
                            }
                        }
                        else
                        {
                            while (postingLine1 != null)
                            {
                                newPostFile.writeBytes(postingLine1 + "\n");
                                postingLine1 = postingFile1.readLine();
                            }
                        }

                        /* For vocabulary file */
                        newVocFile.writeBytes(splittedVocabularyLine1[0] + " "
                                + //Term
                                Integer.valueOf(splittedVocabularyLine1[1]) //DF
                                + " "
                                + newPostingPointer + "\n");
                        vocabularyLine1 = nextVocabularyLine1;
                        nextVocabularyLine1 = vocabularyFile1.readLine();
                    }
                    else if (splittedVocabularyLine1[0].compareTo(splittedVocabularyLine2[0]) > 0) //If term1 > term2
                    {
                        /* For posting file */
                        newPostFile.readLine(); //Read the line from the new Posting file.
                        newPostingPointer = newPostFile.getFilePointer(); //Save the pointer of the new line.
                        nextPostingPointer = Long.valueOf(splittedVocabularyLine2[2].replace("\n", ""));
                        if (nextVocabularyLine2 != null)
                        {
                            while (nextPostingPointer < Long.valueOf(nextSplittedVocabularyLine2[2].replace("\n", "")))
                            {
                                newPostFile.writeBytes(postingLine2 + "\n");
                                postingLine2 = postingFile2.readLine();
                                nextPostingPointer = postingFile2.getFilePointer();
                            }
                        }
                        else
                        {
                            while (postingLine2 != null)
                            {
                                newPostFile.writeBytes(postingLine2 + "\n");
                                postingLine2 = postingFile2.readLine();
                            }
                        }

                        /* For vocabulary file */
                        newVocFile.writeBytes(splittedVocabularyLine2[0] + " "
                                + //Term
                                Integer.valueOf(splittedVocabularyLine2[1]) // //DF 
                                + " "
                                + newPostingPointer + "\n"); //Pointer to line in NEW postings file.
                        vocabularyLine2 = nextVocabularyLine2;
                        nextVocabularyLine2 = vocabularyFile2.readLine();
                    }
                }
                while (vocabularyLine1 != null)
                {
                    newPostFile.readLine(); //Read the line from the new Posting file.
                    newPostingPointer = newPostFile.getFilePointer(); //Save the pointer of the new line.
                    splittedVocabularyLine1 = vocabularyLine1.split(" ");
                    nextPostingPointer = Long.valueOf(splittedVocabularyLine1[2].replace("\n", ""));// Pointer to Posting file.
                    if (nextVocabularyLine1 != null) // Check next line.
                    {
                        nextSplittedVocabularyLine1 = vocabularyLine1.split(" "); // Next line splitted.
                        while (nextPostingPointer != Long.valueOf(nextSplittedVocabularyLine1[2].replace("\n", "")))
                        {
                            newPostFile.writeBytes(postingLine1 + "\n");
                            postingLine1 = postingFile1.readLine();
                            nextPostingPointer = postingFile1.getFilePointer();
                        }
                    }
                    else
                    {
                        while (postingLine1 != null)
                        {
                            newPostFile.writeBytes(postingLine1 + "\n");
                            postingLine1 = postingFile1.readLine();
                        }
                    }

                    /* For vocabulary file */
                    newVocFile.writeBytes(splittedVocabularyLine1[0] + " "
                            + //Term
                            Integer.valueOf(splittedVocabularyLine1[1]) // //DF 
                            + " "
                            + newPostingPointer + "\n"); //Pointer to line in NEW postings file.
                    vocabularyLine1 = nextVocabularyLine1;
                    nextVocabularyLine1 = vocabularyFile1.readLine();
                }
                while (vocabularyLine2 != null)
                {
                    newPostFile.readLine(); //Read the line from the new Posting file.
                    newPostingPointer = newPostFile.getFilePointer(); //Save the pointer of the new line.
                    splittedVocabularyLine2 = vocabularyLine2.split(" ");
                    nextPostingPointer = Long.valueOf(splittedVocabularyLine2[2].replace("\n", ""));// Pointer to Posting file.
                    if (nextVocabularyLine2 != null) // Check next line.
                    {
                        nextSplittedVocabularyLine2 = vocabularyLine2.split(" "); // Next line splitted.
                        while (nextPostingPointer != Long.valueOf(nextSplittedVocabularyLine2[2].replace("\n", "")))
                        {
                            newPostFile.writeBytes(postingLine2 + "\n");
                            postingLine2 = postingFile2.readLine();
                            nextPostingPointer = postingFile2.getFilePointer();
                        }
                    }
                    else
                    {
                        while (postingLine2 != null)
                        {
                            newPostFile.writeBytes(postingLine2 + "\n");
                            postingLine2 = postingFile2.readLine();
                        }
                    }

                    /* For vocabulary file */
                    newVocFile.writeBytes(splittedVocabularyLine2[0] + " "
                            + //Term
                            Integer.valueOf(splittedVocabularyLine2[1]) // //DF 
                            + " "
                            + newPostingPointer + "\n"); //Pointer to line in NEW postings file.
                    vocabularyLine2 = nextVocabularyLine2;
                    nextVocabularyLine2 = vocabularyFile2.readLine();
                }

                vocabularyFile1.close();
                vocabularyFile2.close();
                postingFile1.close();
                postingFile2.close();
                newVocFile.close();
                newPostFile.close();

                // Make the new files of type "File" to insert them in the queues.
                File vocFile = new File(workingDir + "\\CollectionIndex\\VocabularyFileMerged" + mergingCounter + ".txt");
                File postFile = new File(workingDir + "\\CollectionIndex\\PostingFileMerged" + mergingCounter + ".txt");
                vocabulariesQueue.add(vocFile);
                postingsQueue.add(postFile);

                //Delete the old files.
                vocFile1.delete();
                vocFile2.delete();
                postFile1.delete();
                postFile2.delete();
                mergingCounter++;
                System.out.println("\nMerging ended.");
            }
            catch (FileNotFoundException ex)
            {
                Logger.getLogger(indexer.class.getName()).log(Level.SEVERE, null, ex);
            }
            catch (IOException ex)
            {
                Logger.getLogger(indexer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public static void writeNorms(Queue<File> vocabulariesQueue, Queue<File> postingsQueue, HashMap<String, Document> documentsMap)
    {
        String workingDir = System.getProperty("user.dir");
        try
        {
            System.out.println("\n\nWriting Norms started.");
            RandomAccessFile vocabularyFile = new RandomAccessFile(vocabulariesQueue.peek().getAbsolutePath(), "rw");
            RandomAccessFile postingFile = new RandomAccessFile(postingsQueue.peek().getAbsolutePath(), "rw");
            RandomAccessFile documentFile = new RandomAccessFile(workingDir + "\\CollectionIndex\\DocumentsFile.txt", "rw");
            long postingPointer;
            long stopPointer;
            String vocabularyLine = vocabularyFile.readLine();
            String nextVocabularyLine = vocabularyFile.readLine();
            String postingLine;
            String[] splittedVocabularyLine;
            String[] nextSplittedVocabularyLine;
            String[] splittedPostingLine;
            double weight;
            double df;
            double tf;
            Document doc;
            String t;
            System.out.println("\n\n");
            while (vocabularyLine != null) // Do it for every term.
            {
                splittedVocabularyLine = vocabularyLine.split(" ");
                df = Double.valueOf(splittedVocabularyLine[1]);  //Save the df of the term.
                t = splittedVocabularyLine[0];

                postingFile.seek(Long.valueOf(splittedVocabularyLine[2].replace("\n", ""))); //Go to the first entry of the posting file for the specific term.
                postingLine = postingFile.readLine();
                splittedPostingLine = postingLine.split(" ");
                postingPointer = Long.valueOf(splittedVocabularyLine[2].replace("\n", ""));
                if (nextVocabularyLine != null)
                {
                    nextSplittedVocabularyLine = nextVocabularyLine.split(" ");
                    stopPointer = Long.valueOf(nextSplittedVocabularyLine[2].replace("\n", "")); // Pointer to stop to for the posting file.
                    while (postingPointer < stopPointer)
                    {
                        System.out.println("" + postingLine);
                        String docName = splittedPostingLine[0];
                        tf = Double.valueOf(splittedPostingLine[1]);
                        doc = documentsMap.get(docName);
                        weight = Math.pow(tf * df, 2); //(tf*df)^2
                        doc.setNorm(doc.getNorm() + weight);
                        documentsMap.replace(docName, doc);
                        postingLine = postingFile.readLine();
                        postingPointer = postingFile.getFilePointer();
                        if (postingLine != null)
                        {
                            splittedPostingLine = postingLine.split(" ");
                        }
                        else
                        {
                            break;
                        }
                    }
                }
                else
                {
                    while (postingLine != null)
                    {
                        System.out.println("" + postingLine);
                        String docName = splittedPostingLine[0];
                        tf = Double.valueOf(splittedPostingLine[1]);
                        doc = documentsMap.get(docName);
                        weight = Math.pow(tf * df, 2); //(tf*df)^2
                        doc.setNorm(doc.getNorm() + weight);
                        documentsMap.replace(docName, doc);
                        postingLine = postingFile.readLine();
                        if (postingLine != null)
                        {
                            splittedPostingLine = postingLine.split(" ");
                        }
                        else
                        {
                            break;
                        }
                    }
                }

                vocabularyLine = nextVocabularyLine;
                nextVocabularyLine = vocabularyFile.readLine();
                if (vocabularyLine == null)
                {
                    break;
                }
            }
            vocabularyFile.close();
            postingFile.close();

            /* Write the correct norm of each document. */
            Long pointerNow = documentFile.getFilePointer();
            String documentLine = documentFile.readLine();
            String[] splittedDocumentLine = documentLine.split(" ");
            double norm;
            String normAsString;
            int indexOfDecimal;
            String leftPart;
            String rightPart;
            
            
            NumberFormat numFormat = NumberFormat.getInstance();
            numFormat.setMinimumIntegerDigits(2);
            numFormat.setMaximumIntegerDigits(2);
            numFormat.setMinimumFractionDigits(4);
            numFormat.setMaximumFractionDigits(4);
                        
            
            RandomAccessFile newDocumentFile = new RandomAccessFile(workingDir + "\\CollectionIndex\\DocumentsFile.txt", "rw");

            while (documentLine != null)
            {
                System.out.println("OLD LINE:"+documentLine);
                norm = Double.parseDouble(numFormat.format(Math.sqrt(documentsMap.get(splittedDocumentLine[0]).getNorm())));
                
                
                normAsString = String.valueOf(norm);
                indexOfDecimal = normAsString.indexOf(".");
                rightPart = String.valueOf(normAsString.substring(indexOfDecimal));
                leftPart = normAsString.substring(0, indexOfDecimal);

                if(leftPart.length() < 2)
                {
                    leftPart = "0"+leftPart;
                }
                if(rightPart.length() <= 4 )
                {
                    int len =  4-rightPart.length();
                    for(int i=0; i<=len; i++)
                    {
                        rightPart = rightPart+"0";
                    }
                }
                
                normAsString = leftPart+rightPart;
                String line = splittedDocumentLine[0] + " " + splittedDocumentLine[1] + " " + normAsString + "\n";
                

                documentFile.seek(pointerNow);
                documentFile.writeBytes(line);
                
                pointerNow = documentFile.getFilePointer();
                documentLine = documentFile.readLine();
                
                if (documentLine == null)
                {
                    break;
                }
                
                while(documentLine.isBlank() || documentLine.isEmpty())
                {
                    documentLine = documentFile.readLine();
                    if (documentLine == null)
                    {
                        break;
                    }
                }
                
                if (documentLine != null)
                {
                    splittedDocumentLine = documentLine.split(" ");
                }
                else
                {
                    break;
                }
            }
            documentFile.close();
            newDocumentFile.close();

            File filePosting = new File(postingsQueue.peek().getAbsolutePath());
            File newNamePosting = new File(workingDir + "\\CollectionIndex\\PostingFile.txt");
            filePosting.renameTo(newNamePosting);

            File fileVocabulary = new File(vocabulariesQueue.peek().getAbsolutePath());
            File newNameVocabulary = new File(workingDir + "\\CollectionIndex\\VocabularyFile.txt");
            fileVocabulary.renameTo(newNameVocabulary);

            System.out.println("\nWriting Norms ended.");
        }
        catch (FileNotFoundException ex)
        {
            Logger.getLogger(indexer.class.getName()).log(Level.SEVERE, null, ex);
        }
        catch (IOException ex)
        {
            Logger.getLogger(indexer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    // Populates the maps for the term/documents/tags/appearances, term/documents/positions, term/documents/frequency, document/maxFrequency.
    public static void makeTermMap(TreeMap<String, HashMap<String, HashMap<String, Integer>>> termDocTagsMap,
            TreeMap<String, HashMap<String, DocOfTerm>> termsTree, HashMap<String, Document> documentsMap,
            String input, String tag, String document, String docPath)
    {
        String workingDir = System.getProperty("user.dir");

        Stemmer.Initialize();

        ArrayList<String> stopWordsGR = new ArrayList<>();
        ArrayList<String> stopWordsEN = new ArrayList<>();

        if (input.isEmpty()) // If input is empty, do nothing(because some body tags are empty).
        {
            return;
        }
        input = input.toLowerCase();
        ArrayList<String> finalTerms = new ArrayList<>(Arrays.asList(input.split("[\\p{Punct}\\s]+")));

        File folder = new File(workingDir + "\\stoplists");
        ArrayList<String> stopwordsFilesPath = new ArrayList<>();
        listFilesForFolder(folder, stopwordsFilesPath);
        try
        {
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
        }
        catch (FileNotFoundException ex)
        {
            Logger.getLogger(indexer.class.getName()).log(Level.SEVERE, null, ex);
        }
        catch (IOException ex)
        {
            Logger.getLogger(indexer.class.getName()).log(Level.SEVERE, null, ex);
        }

        HashMap<String, HashMap<String, Integer>> documentsTagsMapTemp;
        HashMap<String, Integer> tagMapTemp;

        HashMap<String, DocOfTerm> docsTermMap;
        Document docMax;
        DocOfTerm docTerm;
        int freq = 0;

        for (String term : finalTerms)
        {
            if (stopWordsGR.contains(term) || stopWordsEN.contains(term)) // If a stopword is found, continue to the next word.
            {
                continue;
            }

            term = Stemmer.Stem(term);

            /* For Tree with terms as key, and pmcid and DocOfTerm class as value */
            if (termsTree.containsKey(term)) //If the term exists.
            {
                docsTermMap = termsTree.get(term);
                if (docsTermMap.containsKey(document))
                {
                    docTerm = docsTermMap.get(document);
                    docTerm.setFreq((docTerm.getFreq() + 1)); //Increment frequency.
                    docsTermMap.replace(document, docTerm);
                    termsTree.replace(term, docsTermMap);
                    freq = docTerm.getFreq();
                }
                else
                {
                    docTerm = new DocOfTerm();
                    docTerm.setFreq(1); //First appearance of the term in the document.
                    docsTermMap.put(document, docTerm);
                    termsTree.replace(term, docsTermMap);
                    freq = 1;
                }
            }
            else //If the term does not exist.
            {
                docTerm = new DocOfTerm();
                docTerm.setFreq(1); //First appearance of the term in the document.
                docsTermMap = new HashMap<>();
                docsTermMap.put(document, docTerm);
                termsTree.put(term, docsTermMap);
                freq = 1;
            }

            /* For documentsMap */
            if (documentsMap.containsKey(document)) //If the document exists.
            {
                docMax = documentsMap.get(document);
                if (docMax.getMaxFreq() < freq)
                {
                    docMax.setMaxFreq(freq);
                    documentsMap.replace(document, docMax);
                }
            }
            else //If the document does not exist.
            {
                docMax = new Document(docPath);
                docMax.setMaxFreq(freq);
                documentsMap.put(document, docMax);
            }
        }
        stopWordsGR = null;
        stopWordsEN = null;
        finalTerms = null;
        stopwordsFilesPath = null;
        System.gc();
    }

    public static void main(String[] args) throws UnsupportedEncodingException, IOException
    {
        TreeMap<String, HashMap<String, HashMap<String, Integer>>> termDocTagsMap = new TreeMap<>(); //HashMap with key the term, and value the documents
        // and the tags of the documents with the number of appearances of the term in each tag.
        HashMap<String, Long> docsPointerMap = new HashMap<>(); // HashMap with key the pmcid of the document, and value the offset
        // of each document in the Documents File.
        HashMap<String, Long> postingsPointerMap = new HashMap<>(); // HashMap with key the term , and value the offset
        // of the term in the postings file.(of the first document of each term).

        TreeMap<String, HashMap<String, DocOfTerm>> termsTree = new TreeMap<>(); //TreeMap with key the terms and
        //value the document pmcid with a DocOfTerm class Array.
        HashMap<String, Document> documentsMap = new HashMap<>(); //HashMap with key the pmcid of the document,
        // and value a Document class.

        Queue<File> vocabulariesQueue = new LinkedList<>(); //Queue with partial vocabularies files.
        Queue<File> postingsQueue = new LinkedList<>(); //Queue with partial postings files.
        int numOfVocabularies = 1; //Number of partial vocabularies.
        int numOfPostings = 1; //Number of partial postings.

        int docCounter = 0; //For the loop only.

        makeVocabularyDocumentsFile();
        File folder = new File("C:\\Users\\Sundorius\\Documents\\MedicalCollection");
        
        ArrayList<String> documentsPath = new ArrayList<>();
        listFilesForFolder(folder, documentsPath);
        //System.out.println("Size: " + documentsPath.size());

        int counter = 0; // TODO DELETE IT.
        for (String document : documentsPath)
        {
            File example = new File(document);
            NXMLFileReader xmlFile = new NXMLFileReader(example);
            String pmcid = xmlFile.getPMCID();
            String title = xmlFile.getTitle();
            String abstr = xmlFile.getAbstr();
            String body = xmlFile.getBody();
            String journal = xmlFile.getJournal();
            String publisher = xmlFile.getPublisher();
            ArrayList<String> authors = xmlFile.getAuthors();
            HashSet<String> categories = xmlFile.getCategories();

            System.out.println("File " + pmcid + " start.");

            makeTermMap(termDocTagsMap, termsTree, documentsMap, body, "body", pmcid, document);
            makeTermMap(termDocTagsMap, termsTree, documentsMap, title, "title", pmcid, document);
            makeTermMap(termDocTagsMap, termsTree, documentsMap, abstr, "abstr", pmcid, document);
            makeTermMap(termDocTagsMap, termsTree, documentsMap, journal, "journal", pmcid, document);
            makeTermMap(termDocTagsMap, termsTree, documentsMap, publisher, "publisher", pmcid, document);
            System.out.println("File " + pmcid + " end.");

            //makeTermMap(termDocTagsMap, termDocPosMap, authors, "authors", pmcid);
            //makeTermMap(termDocTagsMap, termDocPosMap, categories, "categories", pmcid);
            //System.out.println("\n\n^^^Free memory (bytes): " + Runtime.getRuntime().freeMemory());
            //long maxMemory = Runtime.getRuntime().maxMemory();
            //System.out.println("^^^Maximum memory (bytes): " + (maxMemory == Long.MAX_VALUE ? "no limit" : maxMemory));
            docCounter++;

            if (docCounter == 20)
            {
                counter++; // TODO DELETE IT.
                populateDocumentFile(termsTree, documentsMap, docsPointerMap);
                System.out.println("    Documents File populated.");

                populatePostingFiles(termsTree, postingsPointerMap, docsPointerMap, postingsQueue, numOfPostings);
                System.out.println("    Postings File populated.");

                populateVocabularyFile(termsTree, postingsPointerMap, vocabulariesQueue, numOfVocabularies);
                System.out.println("    Vocabulary File populated.");
                numOfVocabularies++;
                numOfPostings++;
                postingsPointerMap = new HashMap<>();
                termDocTagsMap = new TreeMap<>();
                termsTree = new TreeMap<>();
                docCounter = 0;
                if (counter == 5)
                {
                    break;
                }
            }
            else if (document.equals(documentsPath.get(documentsPath.size() - 1))) //If it is the last document and it has not done the populations, then do them.
            {
                //counter++; // TODO DELETE IT.
                populateDocumentFile(termsTree, documentsMap, docsPointerMap);
                System.out.println("    Documents File populated.");

                populatePostingFiles(termsTree, postingsPointerMap, docsPointerMap, postingsQueue, numOfPostings);
                System.out.println("    Postings File populated.");

                populateVocabularyFile(termsTree, postingsPointerMap, vocabulariesQueue, numOfVocabularies);
                System.out.println("    Vocabulary File populated.");
                numOfVocabularies++;
                numOfPostings++;
                postingsPointerMap = new HashMap<>();
                termDocTagsMap = new TreeMap<>();
                termsTree = new TreeMap<>();
                docCounter = 0;
            }
        }
        mergeFiles(vocabulariesQueue, postingsQueue);
        writeNorms(vocabulariesQueue, postingsQueue, documentsMap);

        termDocTagsMap = null; // Delete it, we only made it because exercise B1 asked it!
        System.gc();
    }
}
