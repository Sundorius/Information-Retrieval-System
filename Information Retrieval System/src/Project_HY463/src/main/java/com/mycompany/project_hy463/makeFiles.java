/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.project_hy463;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;

/**
 *
 * @author csd3609,csd3195
 */
public class makeFiles 
{
        
    public static void main(String[] args) throws FileNotFoundException, IOException, Exception
    {
        String workingDir = System.getProperty("user.dir");
        
        RandomAccessFile resultsFile = new RandomAccessFile(workingDir + "\\results.txt", "rw");
        int loops = 1;
        int topicFromFile = 1;
        int nextTopic = topicFromFile+1;
        String line = resultsFile.readLine();
        String[] splittedLine = line.split(" ");
        String metric = splittedLine[splittedLine.length-1];
        while(loops <= 30)
        {
            File topicResultsFile = new File(workingDir + "\\numbers\\topic"+topicFromFile+".txt");
            boolean fdoc = topicResultsFile.createNewFile();
            if (fdoc)
            {
                System.out.println("topic"+topicFromFile+".txt has been created successfully");
            }
            else
            {
                System.out.println("topic"+topicFromFile+".txt already present at the specified location");
            }
            
            FileWriter res_writer = new FileWriter(workingDir + "\\numbers\\topic"+topicFromFile+".txt",true);  
            BufferedWriter res_buffer = new BufferedWriter(res_writer); 
            PrintWriter res_out = new PrintWriter(res_buffer);
            
            while(topicFromFile != nextTopic)
            {
                res_out.append(metric+"\n");
                
                
                line = resultsFile.readLine();
                if(line != null)
                {
                    splittedLine = line.split(" ");
                    topicFromFile = Integer.valueOf(splittedLine[0]);
                    metric = splittedLine[splittedLine.length-1];
                }
                else
                {
                    break;
                }
            }
            nextTopic = topicFromFile+1;
            res_buffer.close();  
            res_out.close();
            res_writer.close();
            loops++;
        }
        resultsFile.close();     
    }
}
