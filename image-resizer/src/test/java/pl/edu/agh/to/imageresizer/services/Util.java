package pl.edu.agh.to.imageresizer.services;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Util {
    public static String readFile(String fileName) {
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            return br.readLine();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
