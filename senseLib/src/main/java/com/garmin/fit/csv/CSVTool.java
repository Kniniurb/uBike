////////////////////////////////////////////////////////////////////////////////
// The following FIT Protocol software provided may be used with FIT protocol
// devices only and remains the copyrighted property of Dynastream Innovations Inc.
// The software is being provided on an "as-is" basis and as an accommodation,
// and therefore all warranties, representations, or guarantees of any kind
// (whether express, implied or statutory) including, without limitation,
// warranties of merchantability, non-infringement, or fitness for a particular
// purpose, are specifically disclaimed.
//
// Copyright 2013 Dynastream Innovations Inc.
////////////////////////////////////////////////////////////////////////////////
// ****WARNING****  This file is auto-generated!  Do NOT edit this file.
// Profile Version = 6.10Release
// Tag = $Name:  $
////////////////////////////////////////////////////////////////////////////////


package com.garmin.fit.csv;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

import com.garmin.fit.Decode;
import com.garmin.fit.FileEncoder;
import com.garmin.fit.Fit;
import com.garmin.fit.MesgDefinitionListener;
import com.garmin.fit.MesgListener;
import com.garmin.fit.test.Tests;

public class CSVTool {
   public static void main(String args[]) {
      final int DATA_OR_DEFINITION_SEARCH_COUNT = 2;
      String in = "";
      String out = "";
      ArrayList<String> mesgDefinitionsToOutput = new ArrayList<String>();
      ArrayList<String> dataMessagesToOutput = new ArrayList<String>();
      boolean fitToCsv = false;
      boolean csvToFit = false;
      boolean test = false;
      boolean checkIntegrity = false;
      int nextArgumentDefinition = 0;
      int nextArgumentData = 0;

      int arg = 0;

      System.out.printf("FIT CSV Tool %d.%d.%d.%d\n", Fit.PROTOCOL_VERSION_MAJOR, Fit.PROTOCOL_VERSION_MINOR, Fit.PROFILE_VERSION_MAJOR, Fit.PROFILE_VERSION_MINOR);

      while (arg < args.length) {
         if (args[arg].equals("-b")) {
            if ((args.length - arg) < 3) {
               printUsage();
               return;
            }

            fitToCsv = true;
            in = args[arg + 1];
            out = args[arg + 2];

            arg += 2;
         } else if (args[arg].equals("-c")) {
            if ((args.length - arg) < 3) {
               printUsage();
               return;
            }

            csvToFit = true;
            in = args[arg + 1];
            out = args[arg + 2];

            arg += 2;
         } else if (args[arg].equals("-t")) {
            test = true;
         } else if (args[arg].equals("-d")) {
            Fit.debug = true;
            test = true;
         } else if (args[arg].equals("-i")) {
            checkIntegrity = true;
         } else if (args[arg].equals("--defn")) {
            nextArgumentDefinition = DATA_OR_DEFINITION_SEARCH_COUNT;
         } else if (args[arg].equals("--data")) {
            nextArgumentData = DATA_OR_DEFINITION_SEARCH_COUNT;
         } else if (args[arg].charAt(0) != '-') {
            
            if(nextArgumentDefinition > 0) {
               mesgDefinitionsToOutput = new ArrayList<String>(Arrays.asList(args[arg].toLowerCase(Locale.US).split(",")));
            }
            else if(nextArgumentData > 0) {
               dataMessagesToOutput = new ArrayList<String>(Arrays.asList(args[arg].toLowerCase(Locale.US).split(",")));
            }
            else {
               in = args[arg];
               if (in.endsWith(".fit")) {
                   fitToCsv = true;
                   out = in.substring(0, in.length()-4) + ".csv";
               } else if (in.endsWith(".csv")) {
                   csvToFit = true;
                   out = in.substring(0, in.length()-4) + ".fit";
               }
            }
         }
         
         if(nextArgumentDefinition > 0) {
            nextArgumentDefinition--;
            if((nextArgumentDefinition == 0) && (mesgDefinitionsToOutput.isEmpty()))
            {
               System.out.println("No mesg definitions defined for --defn option.  Use 'none' if no definitions are desired.");
               return;
            }
         }
         if(nextArgumentData > 0) {
            nextArgumentData--;
            if((nextArgumentData == 0) && (dataMessagesToOutput.isEmpty()))
            {
               System.out.println("No data messages defined for --data option.  Use 'none' if no data is desired.");
               return;
            }
         }
         arg++;
      }

      if (fitToCsv) {
         if ((out.length() >= 4) && (out.substring(out.length()-4, out.length()).compareTo(".csv") == 0))
            out = out.substring(0, out.length()-4); // Remove .csv extension.
         
         if (checkIntegrity) {
            try {
               if (!Decode.checkIntegrity((InputStream) new FileInputStream(in)))
                  throw new RuntimeException("FIT file integrity failure.");
            } catch (java.io.IOException e) {
               throw new RuntimeException(e);
            }
         }

         if (test) {
            Tests tests = new Tests();
            System.out.println("Running FIT verification tests...");
            if (tests.run(in))
               System.out.println("Passed FIT verification.");
            else
               System.out.println("Failed FIT verification.");
         }

         try {
            Decode decode = new Decode();
            MesgCSVWriter mesgWriter = new MesgCSVWriter(out + ".csv");

            MesgFilter mesgFilter = new MesgFilter();
            mesgFilter.setMesgDefinitionsToOutput(mesgDefinitionsToOutput);
            mesgFilter.setDataMessagesToOutput(dataMessagesToOutput);

            MesgDataCSVWriter dataMesgWriter = new MesgDataCSVWriter(out + "_data.csv");

            mesgFilter.addListener((MesgDefinitionListener) mesgWriter);
            mesgFilter.addListener((MesgListener) mesgWriter);
            mesgFilter.addListener((MesgListener) dataMesgWriter);

            decode.addListener((MesgDefinitionListener) mesgFilter);
            decode.addListener((MesgListener) mesgFilter);

            decode.read((InputStream) new FileInputStream(in));
            
            mesgWriter.close();
            dataMesgWriter.close();
         } catch (java.io.IOException e) {
            throw new RuntimeException(e);
         }

         System.out.printf("FIT binary file %s decoded to %s*.csv files.\n", in, out);
      } else if (csvToFit) {
         try {
            FileEncoder encoder = new FileEncoder(new File(out));
            if (!CSVReader.read((InputStream) new FileInputStream(in), encoder, encoder))
               throw new RuntimeException("FIT encoding error.");
            encoder.close();
            
            System.out.printf("%s encoded into FIT binary file %s.\n", in, out);
         } catch (java.io.IOException e) {
            throw new RuntimeException(e);
         }
      } else {
         printUsage();
      }
   }

   private static void printUsage() {
      System.out.println("Usage: java -jar FitCSVTool.jar <options> <file>");
      System.out.println("      -b <FIT FILE> <CSV FILE>  FIT binary to CSV.");
      System.out.println("      -c <CSV FILE> <FIT FILE>  CSV to FIT binary.");
      System.out.println("      -t Enable file verification tests.");
      System.out.println("      -d Enable debug output.");
      System.out.println("      -i Check integrity of FIT file before decoding.");
      System.out.println("      --defn <MESSAGE_STRING_0,MESSAGE_STRING_1,...> Narrows down the");
      System.out.println("          definitions output to CSV. Use 'none' for no definitions");
      System.out.println("          When this option is used only the message definitions");
      System.out.println("          in the comma separated list will be written to the CSV.");
      System.out.println("          eg. --defn file_capabilities,record,file_creator");
      System.out.println("          Note: This option is only compatible with the -b option.");
      System.out.println("      --data <MESSAGE_STRING_0,MESSAGE_STRING_1,...> Narrows down the");
      System.out.println("          data output to CSV. Use 'none' for no definitions.");
      System.out.println("          When this option is used only the data");
      System.out.println("          in the comma separated list will be written to the csv.");
      System.out.println("          eg. --data file_capabilities,record,file_creator");
      System.out.println("          Note: This option is only compatible with the -b option.");
      }
}