package com.merittrac.apollo.rps.services.bean;

import java.io.Serializable;

/**
 * Created by Ajit_K .
 *
 * @author Ajit Kadam
 * @author LastChangedBy: ${Ajit_K}
 * @version $Revision: 1595 $,
 * @Date:: 08-06-2015
 */

public class TypingTestEvaluationResultBean implements Serializable {

    /**
     * total words typed by the user
     */
    private int totalSourceWords;
    /**
     * total words typed by the user
     */
    private int totalCorrectWords;
    /**
     * total words typed  by the user irrespective to wrong
     */
    private int noOfTypedWords;
    /**
     * total words typed by the user where spelling is incorrect
     */
    private int spellingMismatch;
    /**
     * total additional words typed by the user
     */
    private int additionalWords;
    /**
     * total missed words by the user
     */
    private int missedWords;
    /**
     * result string containing wrong input string between
     */
    private String typedResultString;

    /**
     * result string containing wrong input string between
     */
    private String prettyHtmlFormat;
    /**
     * no of miss matched blank space in String
     */
    private int noOfMissMatchSpace;
    /**
     * no of correct Character Sequence Typed by user .
     */
    private int correctCharacterSequenceIntervals;
    /**
     * no of correct Character Typed in Sequence Typed by user .
     */
    private int correctCharacterSequenceCount;


    public TypingTestEvaluationResultBean(int totalSourceWords, int totalCorrectWords, int noOfTypedWords, int spellingMismatch,
                                          int additionalWords, int missedWords, String typedResultString, int noOfMissMatchSpace,
                                          int correctCharacterSequenceIntervals, int correctCharacterSequenceCount) {
        this.totalSourceWords = totalSourceWords;
        this.totalCorrectWords = totalCorrectWords;
        this.noOfTypedWords = noOfTypedWords;
        this.spellingMismatch = spellingMismatch;
        this.additionalWords = additionalWords;
        this.missedWords = missedWords;
        this.typedResultString = typedResultString;
        this.noOfMissMatchSpace = noOfMissMatchSpace;
        this.correctCharacterSequenceIntervals = correctCharacterSequenceIntervals;
        this.correctCharacterSequenceCount = correctCharacterSequenceCount;
    }

    public TypingTestEvaluationResultBean() {
        super();
    }

    public int getTotalSourceWords() {
        return totalSourceWords;
    }

    public void setTotalSourceWords(int totalSourceWords) {
        this.totalSourceWords = totalSourceWords;
    }

    public int getNoOfMissMatchSpace() {
        return noOfMissMatchSpace;
    }

    public void setNoOfMissMatchSpace(int noOfMissMatchSpace) {
        this.noOfMissMatchSpace = noOfMissMatchSpace;
    }

    public int getTotalCorrectWords() {
        return totalCorrectWords;
    }

    public void setTotalCorrectWords(int totalCorrectWords) {
        this.totalCorrectWords = totalCorrectWords;
    }

    public int getNoOfTypedWords() {
        return noOfTypedWords;
    }

    public void setNoOfTypedWords(int noOfTypedWords) {
        this.noOfTypedWords = noOfTypedWords;
    }

    public int getSpellingMismatch() {
        return spellingMismatch;
    }

    public void setSpellingMismatch(int spellingMismatch) {
        this.spellingMismatch = spellingMismatch;
    }

    public int getAdditionalWords() {
        return additionalWords;
    }

    public void setAdditionalWords(int additionalWords) {
        this.additionalWords = additionalWords;
    }

    public int getMissedWords() {
        return missedWords;
    }

    public void setMissedWords(int missedWords) {
        this.missedWords = missedWords;
    }

    public String getTypedResultString() {
        return typedResultString;
    }

    public void setTypedResultString(String typedResultString) {
        this.typedResultString = typedResultString;
    }

    public int getCorrectCharacterSequenceIntervals() {
        return correctCharacterSequenceIntervals;
    }

    public void setCorrectCharacterSequenceIntervals(int correctCharacterSequenceIntervals) {
        this.correctCharacterSequenceIntervals = correctCharacterSequenceIntervals;
    }

    public int getCorrectCharacterSequenceCount() {
        return correctCharacterSequenceCount;
    }

    public void setCorrectCharacterSequenceCount(int correctCharacterSequenceCount) {
        this.correctCharacterSequenceCount = correctCharacterSequenceCount;
    }

    public String getPrettyHtmlFormat() {
        return prettyHtmlFormat;
    }

    public void setPrettyHtmlFormat(String prettyHtmlFormat) {
        this.prettyHtmlFormat = prettyHtmlFormat;
    }

    @Override
    public String toString() {
        return "TypingTestEvaluationResultBean{" +
                "totalSourceWords=" + totalSourceWords +
                ", totalCorrectWords=" + totalCorrectWords +
                ", noOfTypedWords=" + noOfTypedWords +
                ", spellingMismatch=" + spellingMismatch +
                ", additionalWords=" + additionalWords +
                ", missedWords=" + missedWords +
                ", typedResultString='" + typedResultString + '\'' +
                ", noOfMissMatchSpace=" + noOfMissMatchSpace +
                ", correctCharacterSequenceIntervals=" + correctCharacterSequenceIntervals +
                ", correctCharacterSequenceCount=" + correctCharacterSequenceCount +
                '}';
    }
}
