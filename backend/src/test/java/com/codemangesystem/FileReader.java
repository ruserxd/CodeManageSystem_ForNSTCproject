package com.codemangesystem;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;

public class FileReader {
    public static String getFileCode(String filePath) {
        java.io.FileReader fr = null;
        StringBuilder fileINFO = new StringBuilder();
        try {
            fr = new java.io.FileReader(filePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        BufferedReader br = new BufferedReader(fr);
        String tmp;

        try {
            while (((tmp = br.readLine()) != null)) {
                fileINFO.append(tmp);
                fileINFO.append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return String.valueOf(fileINFO);
    }
}
