package main.src;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

import java.io.*;
import java.util.*;

public class MigrateDocxToXML {

    private final static String inputPath = "src/io/input/";
    private final static String outputPath = "src/io/output/";

    private final static String fileEnding = ".docx";
    /**
     * HIER DEN FILENAMEN!!!
     */
    private final static String fileName = "Krugman_12e_tb_08_MC";
    private static final Map<Integer, List<String>> realToOriginialQuestionNumberMap = new HashMap<>();

    private static int questionNameNumber = 0;
    private static BufferedWriter writer = null;


    public static void main(String[] args) throws Exception {

        if (fileName.isEmpty()) {
            System.out.println("Geb den Dateinamen an!");
            return;
        }

        List<String> lines = readDocxFile(inputPath + fileName + fileEnding);
        createXMLFile(outputPath + fileName + ".xml", lines);

        System.out.println();
        System.out.println("QUESTION MAPPING:");
        System.out.println();
        System.out.println("—————————————————————————————————————————————————————");
        System.out.println("|realQuestionNumber\t|originalQuestionNumber\t|ignored|\t");
        realToOriginialQuestionNumberMap.forEach((realQuestionNumber, originalQuestionNumber) -> System.out.println("|"
                + realQuestionNumber + (realQuestionNumber.toString().length() < 3 ? "\t\t\t\t\t" : "\t\t\t\t") + "|"
                + originalQuestionNumber.get(0) + "\t\t\t\t\t\t|" + (originalQuestionNumber.get(1).equals("false") ? "\t\t|" : "YES\t|")));
        System.out.println("—————————————————————————————————————————————");

    }

    private static void createXMLFile(String targePath, List<String> lines) throws Exception {
        File targetFile = createNewFile(targePath);

        writer = new BufferedWriter(new FileWriter(targetFile.getPath()));
        writeLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        writeLine("<quiz>");

        String question = "";
        boolean searchForQuestion = true;

        String answerA = "";
        boolean searchForAnswerA = false;

        String answerB = "";
        boolean searchForAnswerB = false;

        String answerC = "";
        boolean searchForAnswerC = false;

        String answerD = "";
        boolean searchForAnswerD = false;

        String answerE = "";
        boolean searchForAnswerE = false;

        String questionFooter = "";
        boolean searchForQuestionFooter = false;

        for (String line : lines) {

            // Handle emptyLines
            if (line.isEmpty()) {
                writeLine("");
                continue;
            }

            char firstChar = line.charAt(0);
            searchForQuestion = searchForQuestion || Character.isDigit(firstChar);

            if (searchForQuestion && searchForQuestionFooter) {
                searchForQuestionFooter = false;

                questionNameNumber++;
                int i = question.indexOf(")");
                String originalQuestionNameNumber = question.substring(0, i);
                realToOriginialQuestionNumberMap.put(questionNameNumber, Arrays.asList(originalQuestionNameNumber, "false"));

                if (questionFooter.startsWith("Answer:  ADifficulty") || questionFooter.startsWith("Answer:  BDifficulty")
                        || questionFooter.startsWith("Answer:  CDifficulty") || questionFooter.startsWith("Answer:  DDifficulty")
                        || questionFooter.startsWith("Answer:  EDifficulty")) {
                    writeQuestion(questionNameNumber + ")", question.substring(i + 2));


                    String[] answerSplit = questionFooter.split(":");
                    char answerChar = answerSplit[1].charAt(2);


                    writeAnswer(answerA, answerChar == 'A');
                    writeAnswer(answerB, answerChar == 'B');
                    writeAnswer(answerC, answerChar == 'C');
                    writeAnswer(answerD, answerChar == 'D');
                    writeAnswer(answerE, answerChar == 'E');

                    writeLine("</question>");
                } else {
                    realToOriginialQuestionNumberMap.get(questionNameNumber).set(1, "true");
                }
                question = "";
                answerA = "";
                answerB = "";
                answerC = "";
                answerD = "";
                answerE = "";
                questionFooter = "";
            }

            // Handle Question
            if (searchForQuestion) {
                // The question is over if the line starts with 'A)'
                if (line.startsWith("A)")) {
                    searchForQuestion = false;
                    searchForAnswerA = true;
                    answerA = line;
                } else {
                    question += line;
                }
            }

            // Handle Answers
            else {
                searchForQuestionFooter = searchForQuestionFooter || line.startsWith("Answer:");
                searchForAnswerE = (searchForAnswerE || line.startsWith("E)")) && !searchForQuestionFooter;
                searchForAnswerD = (searchForAnswerD || line.startsWith("D)")) && !searchForAnswerE && !searchForQuestionFooter;
                searchForAnswerC = (searchForAnswerC || line.startsWith("C)")) && !searchForAnswerD && !searchForQuestionFooter;
                searchForAnswerB = (searchForAnswerB || line.startsWith("B)")) && !searchForAnswerC && !searchForQuestionFooter;
                searchForAnswerA = searchForAnswerA && !searchForAnswerB && !searchForQuestionFooter;

                if (searchForAnswerA) {
                    answerA += line;
                } else if (searchForAnswerB) {
                    answerB += line;
                } else if (searchForAnswerC) {
                    answerC += line;
                } else if (searchForAnswerD) {
                    answerD += line;
                } else if (searchForAnswerE) {
                    answerE += line;
                } else if (searchForQuestionFooter) {
                    questionFooter += line;
                }


            }
        }

        writeLine("</quiz>");
        writer.close();
    }

    private static void writeAnswer(String answer, boolean isCorrect) throws Exception {
        if (!answer.isEmpty()) {

            int i = answer.indexOf(")");
            String answerText = answer.substring(i + 2);

            writeLine("\t<answer fraction=\"" + (isCorrect ? 100 : 0) + "\" format=\"html\">");
            writeLine("\t\t<text>");
            writeLine("\t\t\t<![CDATA[ <p>" + answerText + "</p> ]]>");
            writeLine("\t\t</text>");
            writeLine("\t\t<feedback format=\"html\">");
            writeLine("\t\t\t<text/>");
            writeLine("\t\t</feedback>");
            writeLine("\t</answer>");
        }
    }

    private static void writeQuestion(String questionName, String questionText) throws Exception {
        writeLine("<question type=\"multichoice\">");

        writeLine("\t<name>");
        writeLine("\t\t<text>" + questionName + "</text>");
        writeLine("\t</name>");

        writeLine("\t<questiontext format=\"html\">");
        writeLine("\t\t<text>");
        writeLine("\t\t\t<![CDATA[ <p class=\"NormalText\">" + questionText + "</p> ]]>");
        writeLine("\t\t</text>");
        writeLine("\t</questiontext>");

        writeLine("\t<generalfeedback format=\"html\">");
        writeLine("\t\t<text/>");
        writeLine("\t</generalfeedback>");

        writeLine("\t<defaultgrade>1</defaultgrade>");
        writeLine("\t<penalty>0.3333333</penalty>");
        writeLine("\t<hidden>0</hidden>");
        writeLine("\t<idnumber/>");
        writeLine("\t<single>true</single>");
        writeLine("\t<shuffleanswers>true</shuffleanswers>");
        writeLine("\t<answernumbering>abc</answernumbering>");
        writeLine("\t<showstandardinstruction>0</showstandardinstruction>");

        writeLine("\t<correctfeedback format=\"html\">");
        writeLine("\t\t<text>");
        writeLine("\t\t\t<![CDATA[ <p>Die Antwort ist richtig.</p> ]]>");
        writeLine("\t\t</text>");
        writeLine("\t</correctfeedback>");

        writeLine("\t<partiallycorrectfeedback format=\"html\">");
        writeLine("\t\t<text>");
        writeLine("\t\t\t<![CDATA[ <p>Die Antwort ist teilweise richtig.</p> ]]>");
        writeLine("\t\t</text>");
        writeLine("\t</partiallycorrectfeedback>");

        writeLine("\t<incorrectfeedback format=\"html\">");
        writeLine("\t\t<text>");
        writeLine("\t\t\t<![CDATA[ <p>Die Antwort ist falsch.</p> ]]>");
        writeLine("\t\t</text>");
        writeLine("\t</incorrectfeedback>");

        writeLine("\t<shownumcorrect/>");
    }

    private static List<String> readDocxFile(String sourcePath) throws IOException {

        File file = new File(sourcePath);
        FileInputStream fis = new FileInputStream(file.getAbsolutePath());

        XWPFDocument document = new XWPFDocument(fis);
        List<XWPFParagraph> paragraphs = document.getParagraphs();

        List<String> lines = new ArrayList<>();
        for (XWPFParagraph para : paragraphs) {
            lines.add(para.getText());
        }
        fis.close();

        return lines;
    }

    public static File createNewFile(String targetPath) throws IOException {
        File newFile = new File(targetPath);
        if (newFile.createNewFile()) {
            System.out.println("File created: " + newFile.getName());
        } else {
            System.out.println("File already exists.");
        }
        return newFile;
    }


    private static void writeLine(String line) throws Exception {
        if (writer == null) {
            throw new Exception("Writer ist null");
        }
        writer.write(line + "\n");
    }

}
