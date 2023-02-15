package com.merittrac.apollo.rps.services;

import com.merittrac.apollo.data.entity.RpsCandidateResponses;
import com.merittrac.apollo.rps.common.RpsConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class WTSService {

	private static Logger logger = LoggerFactory.getLogger(LogViewerService.class);

	/**
	 * @param interference
	 *            type where we have calculate score
	 * @param candidateResponses
	 *            map of question with there Id as key and response as a value
	 * @return total marks
	 */

	public Map<String, String> getScoreForWTS(String interference, RpsCandidateResponses candidateResponses) {

		int totalScore = 0;
		int criticalThinkingFrinScoreWTS = 0;
		int frinTotalScore = 0;
		String passage = "";
		int level = 0;
		Map<String, String> subInterferenceLevel = new HashMap<>();
		Map<String, String> questionResponseMap = getQuestionLevelResponseMap(candidateResponses);
		switch (interference) {
		case "Critical Thinking":

			passage = "";
			String inferences[] = {
					"Is good at inferring from the knowledge and information gathered over time while understanding or dealing with current situations. Always bases conclusions about people, situations etcetra on background information, past encounters and environmental influences.",
					"Tends to apply past knowledge sometimes while resolving problems. Bases conclusions and decisions on references drawn from knowledge and experiences, most of the time.",
					"Tends to have a fresh approach every time while resolving similar issues. Is unable to connect known knowledge to current situations. Does not probe much to understand situations, problems or people.",
					"Treats each situation as new and does not apply past learning. Believes in impressions cast at first sight. Forms opinions on what meets the eye without much probing" };
			String assumptions[] = {
					"Can make valid and logical assumptions when things are unclear and vague while understanding situations, problems and people. Can always read between lines and understand the underlying meaning of situations and problems without explicit explainations.",
					"Is able to make some assumptions and inferences while understanding issues. Can usually decipher the underlying meaning of unspoken or inexplicit information.",
					"Tends to look for obvious hints to understand situations, may not be able to make valid assumptions. Tends to take things at the face value and not understand hidden information.",
					"Needs explicit explaination and obvious pointers to understand situations, problems and people. Cannot draw inferences when it is not stated clearly." };

			String interpretation[] = {
					"Basis all decisions on substantial proof and empirical support. When looking for information he/she is very clear about the information required and can differentiate between important and unimportant data.",
					"Tends to make decisions based on proof and evidence most often and can differentiate between essential and extra information while gathering data.",
					"Does not base decisions on proof always. Tends to make impulsive decisions. May find it difficult to sort through data to find relevant and unimportant information.",
					"Makes adhoc and impulsive decisions without any real support and proof. May not be able to gather relevant information as s/he cannot differentiate between essential and extra information." };

			String deduction[] = {
					"Can identify the actual cause for a problem amidst possible causes. Is excellent in recognizing the logical connections existing or missing between concepts and situations that form an important part of understanding and solving problems.",
					"Tends to identify the root cause for issues most of the time. Can often recognize existence or absence of logical connections between situations, problems and concepts.",
					"Is not able to identify the actual root cause for a problem among a host of possible causes. Tends to see situations and problems superficially and does not perceive logical connections that underly situations.	",
					"Consider all possible causes to be a potential root cause and works towards identifying the real cause. Perceives situations and problems at the face value and may not be able to recognize logical connections that are not very obvious." };
			String evaluation[] = {
					"Always ponders over the pros and cons of a given situation before making decisions. Always looks for solutions that can be practically worked out and implemented.",
					"Can view both the pros and cons of a decision most of the time before making a decision. Often tries to find and implement practical solutions for problems.",
					"Does not consider the pros and cons of all options before making decisions. Tends to think of solutions which are not practical in nature.",
					"Is very quick to make decisions without considering the pros and cons of other options. Likes to come up with perfect solutions but is not focused on the practical application of the solutions." };

			/** 2/3/27 a)1 b)2 c)3 d)4 else a)4 b) 3 c)2 d)1 */
			for (Map.Entry<String, String> questionAnwser : questionResponseMap.entrySet()) {

				switch (questionAnwser.getKey()) {
				case "quest1":
					frinTotalScore = getFrinFor14233441ForWts(questionAnwser.getValue());
					frinTotalScore = frinTotalScore + getFrinFor11223344ForWts(questionResponseMap.get("quest27"));
					level = getSubInterpretationLevelForWts(frinTotalScore);
					passage = passage.concat(inferences[level]);
					break;
				case "quest2":
					frinTotalScore = getFrinFor11223344ForWts(questionAnwser.getValue());
					frinTotalScore = frinTotalScore + getFrinFor14233441ForWts(questionResponseMap.get("quest25"));
					level = getSubInterpretationLevelForWts(frinTotalScore);
					passage = passage.concat(assumptions[level]);
					break;
				case "quest3":
					frinTotalScore = getFrinFor11223344ForWts(questionAnwser.getValue());
					frinTotalScore = frinTotalScore + getFrinFor14233441ForWts(questionResponseMap.get("quest28"));
					level = getSubInterpretationLevelForWts(frinTotalScore);
					passage = passage.concat(deduction[level]);
					break;
				case "quest4":
					frinTotalScore = getFrinFor14233441ForWts(questionAnwser.getValue());
					frinTotalScore = frinTotalScore + getFrinFor14233441ForWts(questionResponseMap.get("quest29"));
					level = getSubInterpretationLevelForWts(frinTotalScore);
					passage = passage.concat(evaluation[level]);
					break;
				case "quest5":
					frinTotalScore = getFrinFor14233441ForWts(questionAnwser.getValue());
					frinTotalScore = frinTotalScore + getFrinFor14233441ForWts(questionResponseMap.get("quest26"));

					level = getSubInterpretationLevelForWts(frinTotalScore);
					passage = passage.concat(interpretation[level]);
					break;
				}
			}
			break;
		case "Creative Thinking":
			/** 10/32/33/34 a)1 b)2 c)3 d)4 */

			passage = "";
			for (Map.Entry<String, String> questionAnwser : questionResponseMap.entrySet()) {

				String fluency[] = {
						"Is able to think of a number of innovative ideas for a given problem or situation. These ideas would usually be variants of one main idea.",
						"Can often think of creative ideas for a given situation or solve a problem.",
						"Is limited in the number of ideas that s/he can come up with when faced with a problem situation.",
						"Tends to use previously known solution or idea for a given situation or problem." };

				String flexibility[] = {
						"Is able to ideate new solutions belonging to totally different categories. Is most likely to have a routine that changes very often.",
						"Can generate ideas belonging to different categories sometimes. Does not follow one particular routine.",
						"Can generate new ideas which are similar in their basic concept. May have a slightly predictable working style.",
						"Is unable to generate new ideas which are vastly different from each other. Tends to follow one style / pattern. " };

				String originality[] = { "Is inclined towards being extremely original in thought and actions.",
						"Can bring in originality to existing processes by modifying them.",
						"May not be very original in thoughts / approaches.", "Tends to be conventional in approach." };

				String elaboration[] = {
						"Is able to imagine even the minute details of any creative idea that s/he proposes. Can go to the micro level while ideating.",
						"Is able to imagine the intricate details of a creative idea sometimes. Can think of the intricate details of an idea.",
						"May not be able to give details but can give a framework of creative ideas.",
						"Can give only the framework / outline of the creative without getting into details." };

				String abstraction[] = {
						"Can derive creative ideas from abstract concepts. Is able to use any adhoc information or idea that s/he comes across as a basis for generating fresh approaches / ideas.",
						"Can generate ideas from some abstract concepts. Can convert a few good ideas into workable innovative projects / products.",
						"Can generate ideas from concrete concepts but may not be able to do so from abstract ones.",
						"May not be able to draw from abstract concepts / ideas. May not be able to convert ideas into creative frameworks." };
				switch (questionAnwser.getKey()) {

				case "quest9":
					frinTotalScore = getFrinFor14233441ForWts(questionAnwser.getValue());
					frinTotalScore = frinTotalScore + getFrinFor14233441ForWts(questionResponseMap.get("quest31"));
					level = getSubInterpretationLevelForWts(frinTotalScore);
					passage = passage.concat(abstraction[level]);
					break;
				case "quest10":
					frinTotalScore = getFrinFor11223344ForWts(questionAnwser.getValue());
					frinTotalScore = frinTotalScore + getFrinFor11223344ForWts(questionResponseMap.get("quest32"));
					level = getSubInterpretationLevelForWts(frinTotalScore);
					passage = passage.concat(elaboration[level]);
					break;
				case "quest11":
					frinTotalScore = getFrinFor14233441ForWts(questionAnwser.getValue());
					frinTotalScore = frinTotalScore + getFrinFor11223344ForWts(questionResponseMap.get("quest33"));
					level = getSubInterpretationLevelForWts(frinTotalScore);
					passage = passage.concat(flexibility[level]);
					break;
				case "quest7":
					frinTotalScore = getFrinFor14233441ForWts(questionAnwser.getValue());
					frinTotalScore = frinTotalScore + getFrinFor11223344ForWts(questionResponseMap.get("quest34"));
					level = getSubInterpretationLevelForWts(frinTotalScore);
					passage = passage.concat(fluency[level]);
					break;
				case "quest8":
					frinTotalScore = getFrinFor14233441ForWts(questionAnwser.getValue());
					frinTotalScore = frinTotalScore + getFrinFor14233441ForWts(questionResponseMap.get("quest35"));
					level = getSubInterpretationLevelForWts(frinTotalScore);
					passage = passage.concat(originality[level]);
					break;
				}

			}
			break;
		case "Collaboration":
			/** 13/14/37/40 a)4 b) 3 c)2 d)1 */
			passage = "";
			for (Map.Entry<String, String> questionAnwser : questionResponseMap.entrySet()) {
				String achieveSharedGoals[] = {
						"Enjoys tasks which involve a lot of co-ordination and mutual exchange of ideas and skills.",
						"Likes to co-ordinate, share ideas and skills with others while working on tasks.",
						"May not like to interact too much with others and prefers working alone or in small groups.",
						" Prefers working alone without interacting much with others." };

				String adeepCollective[] = {
						"Is inclined to work towards a common goal and has the end goal / big picture in mind.",
						"Can understand the vision behind performing a task and works towards shared goals.",
						"May not be able to see the end result of a task  or the common goal of a group.",
						"Is short - sighted in identifying the big picture and may not be able to work towards it." };

				String coOrdination[] = {
						"Is able to connect the various seemingly unrelated teams to a common goal / big picture.",
						"Can understand the contributions made by everybody to succeed in achieving the end result.	",
						"May not be able to see the underlying connections between different teams working towards a common goal.",
						"Views every team / group as separate and may not be able to form connections between them." };

				String sharingKnowledge[] = {
						"Is able to transcend the boundaries of a particular job role and contirbute to the end result by going that extra mile.",
						"Gives importance to sharing knowledge and skills to achieve results.",
						"May not believe in sharing ideas, knowledge or resources to achieve the end result.",
						"Strictly confines her/himself to their team / group and may not be able to appreciate sharing." };

				String participation[] = { "Is able to extend oneself to help others achieve goals.",
						"Can be helpful to others in meeting their targets most of the time.",
						"May not cater to others needs.", "Is concerned with self acheivements." };

				String bigPicture[] = { "Believes in sharing success and glory with others.",
						"Can be open to sharing success with others.",
						"May not be open to sharing success with others.",
						"Is very cautious about sharing personal success with others." };
				switch (questionAnwser.getKey()) {
				case "quest13":
					frinTotalScore = getFrinFor14233441ForWts(questionAnwser.getValue());
					frinTotalScore = frinTotalScore + getFrinFor11223344ForWts(questionResponseMap.get("quest38"));
					level = getSubInterpretationLevelForWts(frinTotalScore);
					passage = passage.concat(coOrdination[level]);
					break;
				case "quest15":
					frinTotalScore = getFrinFor11223344ForWts(questionAnwser.getValue());
					frinTotalScore = frinTotalScore + getFrinFor14233441ForWts(questionResponseMap.get("quest37"));
					level = getSubInterpretationLevelForWts(frinTotalScore);
					passage = passage.concat(achieveSharedGoals[level]);
					break;
				case "quest14":
					frinTotalScore = getFrinFor14233441ForWts(questionAnwser.getValue());
					frinTotalScore = frinTotalScore + getFrinFor11223344ForWts(questionResponseMap.get("quest39"));
					level = getSubInterpretationLevelForWts(frinTotalScore);
					passage = passage.concat(bigPicture[level]);
					break;
				case "quest16":
					frinTotalScore = getFrinFor11223344ForWts(questionAnwser.getValue());
					frinTotalScore = frinTotalScore + getFrinFor14233441ForWts(questionResponseMap.get("quest40"));
					level = getSubInterpretationLevelForWts(frinTotalScore);
					passage = passage.concat(participation[level]);
					break;
				case "quest17":
					frinTotalScore = getFrinFor11223344ForWts(questionAnwser.getValue());
					frinTotalScore = frinTotalScore + getFrinFor11223344ForWts(questionResponseMap.get("quest41"));
					level = getSubInterpretationLevelForWts(frinTotalScore);
					passage = passage.concat(sharingKnowledge[level]);
					break;

				}

			}

			break;
		case "Customer Centricity":
			passage = "";
			for (Map.Entry<String, String> questionAnwser : questionResponseMap.entrySet()) {
				/** a)4 b) 3 c)2 d)1 */

				String responsive[] = {
						"Is always responsive to the needs of the customer and makes an effort to go the extra mile.",
						"Is most often responsive and enjoys catering to the customer needs.",
						"Is less likely to be responsive to the needs of the customer and prefers less interaction with customers.",
						"Is not responsive to the client needs and is inclined to chose roles that are free of customer interactions." };

				String onGoingFocus[] = {
						"Is always oriented towards customers changing requirements and works towards providing solutions for the same.",
						"Periodically updates self about the changing needs of the customer and works towards meeting them.",
						"May not always be in sync with the customer changing requirements and viewpoints.",
						"Tends to believe that the most important factor of business is a strong relationship with customers rather than upgradation of goods delivered." };

				String accountableAndEmpowered[] = { "Is highly accountable in work and decisions s/he makes.",
						"Takes responsibility for the work s/he does most of the time.",
						"May not be always accountable for the work done.",
						"Tends to shun responsibility when things go wrong." };

				String willingnessChange[] = {
						"Is highly flexible and open to the changing needs of the market and customers.",
						"Tends to be open to change in customer needs and demands.",
						"May tend to be very firm while catering to customer and market needs .",
						"Is very rigid and cannot accept change easily while dealing with customers and the market." };

				String internalCustomerOrientation[] = {
						"Goes the extra mile to cater to customers and has an internal drive towards satisfying their demands and needs.",
						"Has an intrinsic need to be very obliging and flexible while meeting customer demands.",
						"May believe that it is not possible to cater to all demands of the customers.",
						"Tends to be very practical and non-obliging while dealing with unique customer demands." };

				switch (questionAnwser.getKey()) {
				case "quest20":
					frinTotalScore = getFrinFor11223344ForWts(questionAnwser.getValue());
					frinTotalScore = frinTotalScore + getFrinFor11223344ForWts(questionResponseMap.get("quest43"));
					level = getSubInterpretationLevelForWts(frinTotalScore);
					passage = passage.concat(accountableAndEmpowered[level]);

					break;
				case "quest21":
					frinTotalScore = getFrinFor11223344ForWts(questionAnwser.getValue());
					frinTotalScore = frinTotalScore + getFrinFor11223344ForWts(questionResponseMap.get("quest44"));
					level = getSubInterpretationLevelForWts(frinTotalScore);
					passage = passage.concat(internalCustomerOrientation[level]);
					break;
				case "quest19":
					frinTotalScore = getFrinFor14233441ForWts(questionAnwser.getValue());
					frinTotalScore = frinTotalScore + getFrinFor11223344ForWts(questionResponseMap.get("quest45"));
					level = getSubInterpretationLevelForWts(frinTotalScore);
					passage = passage.concat(onGoingFocus[level]);
					break;
				case "quest22":
					frinTotalScore = getFrinFor14233441ForWts(questionAnwser.getValue());
					frinTotalScore = frinTotalScore + getFrinFor11223344ForWts(questionResponseMap.get("quest46"));
					level = getSubInterpretationLevelForWts(frinTotalScore);
					passage = passage.concat(responsive[level]);
					break;
				case "quest23":
					frinTotalScore = getFrinFor11223344ForWts(questionAnwser.getValue());
					frinTotalScore = frinTotalScore + getFrinFor11223344ForWts(questionResponseMap.get("quest47"));
					level = getSubInterpretationLevelForWts(frinTotalScore);
					passage = passage.concat(willingnessChange[level]);
					break;
				}

			}
			break;

		case "Fake Detector":
			int fdTotalScore = 0;

			for (Map.Entry<String, String> questionAnwser : questionResponseMap.entrySet()) {
				switch (questionAnwser.getKey()) {
				case "quest6":
				case "quest12":
				case "quest18":
				case "quest24":
				case "quest30":
				case "quest36":
				case "quest42":
					String answers1[] = questionAnwser.getValue().split("--");
					if (answers1[2].equalsIgnoreCase("CHOICE1")) {
						totalScore++;
					}
					break;
				}

			}
		}
		subInterferenceLevel.put(interference, passage);
		return subInterferenceLevel;
	}

	private int getFrinFor14233441ForWts(String questionAnwser) {
		String answers1[] = questionAnwser.split("--");
		int totalScore = 0;
		switch (answers1[2]) {
		case "CHOICE1":
			totalScore = totalScore + 4;
			break;
		case "CHOICE2":
			totalScore = totalScore + 3;
			break;
		case "CHOICE3":
			totalScore = totalScore + 2;
			break;
		case "CHOICE4":
			totalScore = totalScore + 1;
			break;
		}
		return totalScore;
	}

	/**
	 *
	 * @param candidateMarks
	 * @param competency
	 * @return
	 */
	public String getFirstLevelForWTS(Double candidateMarks, String competency) {
		String criticalThinking[] = { "Knowledge & Awareness", "Comprehension & Application", "Analysis & Synthesis",
				"Evaluation" };
		String creativeThinking[] = { "Innovative", "Practical", "Creative", "Innovative" };
		String collaboration[] = { "Individual Contributor", "Team Player", "Co-ordinator", "Collaborator" };
		String customerCentric[] = { "Self Driven", "Internal Focused", "Customer Focused", "Customer Centric" };
		int level = 0;
		if ((candidateMarks >= 36) && (candidateMarks <= 40)) {
			level = 4;
		} else if ((candidateMarks >= 26) && (candidateMarks <= 35)) {
			level = 3;
		} else if ((candidateMarks >= 16) && (candidateMarks <= 25)) {
			level = 2;
		} else if ((candidateMarks >= 10) && (candidateMarks <= 15)) {
			level = 1;
		}
		switch (competency) {
		case "WTSCLT":
			return criticalThinking[level - 1] + "-LEVEL " + level;
		case "WTSCET":
			return creativeThinking[level - 1] + "-LEVEL " + level;
		case "WTSCO":
			return collaboration[level - 1] + "-LEVEL " + level;
		case "WTSCC":
			return customerCentric[level - 1] + "-LEVEL " + level;
		}
		return RpsConstants.NA;

	}

	/**
	 * @return
	 */
	private int getSubInterpretationLevelForWts(int totalScore) {

		if (totalScore >= 8)
			return 0;
		else if (totalScore >= 6 && totalScore <= 7)
			return 1;
		else if (totalScore >= 4 && totalScore <= 5)
			return 2;
		else if (totalScore >= 2 && totalScore <= 3)
			return 3;
		else
			return 0;

	}

	private int getFrinFor11223344ForWts(String questionAnwser) {
		String answers1[] = questionAnwser.split("--");
		int totalScore = 0;
		switch (answers1[2]) {
		case "CHOICE1":
			totalScore = totalScore + 1;
			break;
		case "CHOICE2":
			totalScore = totalScore + 2;
			break;
		case "CHOICE3":
			totalScore = totalScore + 3;
			break;
		case "CHOICE4":
			totalScore = totalScore + 4;
			break;
		}
		return totalScore;
	}

	Map<String, String> getQuestionLevelResponseMap(RpsCandidateResponses candidateResponses) {
		Map<String, String> questionResponseMap = new LinkedHashMap<>();

		// Critical Thinking Question Storing
		questionResponseMap.put("quest1", candidateResponses.getQNo_1());
		questionResponseMap.put("quest2", candidateResponses.getQNo_2());
		questionResponseMap.put("quest5", candidateResponses.getQNo_5());
		questionResponseMap.put("quest3", candidateResponses.getQNo_3());
		questionResponseMap.put("quest4", candidateResponses.getQNo_4());

		// Creative Thinking Question Storing Process
		questionResponseMap.put("quest7", candidateResponses.getQNo_7());
		questionResponseMap.put("quest11", candidateResponses.getQNo_11());
		questionResponseMap.put("quest8", candidateResponses.getQNo_8());
		questionResponseMap.put("quest10", candidateResponses.getQNo_10());
		questionResponseMap.put("quest9", candidateResponses.getQNo_9());

		// Collaboration Question Storing Process
		questionResponseMap.put("quest15", candidateResponses.getQNo_15());
		questionResponseMap.put("quest13", candidateResponses.getQNo_13());
		questionResponseMap.put("quest17", candidateResponses.getQNo_17());
		questionResponseMap.put("quest16", candidateResponses.getQNo_16());
		questionResponseMap.put("quest14", candidateResponses.getQNo_14());

		// Customer Centricity Question Storing Process
		questionResponseMap.put("quest22", candidateResponses.getQNo_22());
		questionResponseMap.put("quest19", candidateResponses.getQNo_19());
		questionResponseMap.put("quest20", candidateResponses.getQNo_20());
		questionResponseMap.put("quest23", candidateResponses.getQNo_23());
		questionResponseMap.put("quest21", candidateResponses.getQNo_21());

		// Fake Detector Question Storing Process
		questionResponseMap.put("quest6", candidateResponses.getQNo_6());
		questionResponseMap.put("quest12", candidateResponses.getQNo_12());
		questionResponseMap.put("quest18", candidateResponses.getQNo_18());

		questionResponseMap.put("quest24", candidateResponses.getQNo_24());
		questionResponseMap.put("quest25", candidateResponses.getQNo_25());
		questionResponseMap.put("quest26", candidateResponses.getQNo_26());
		questionResponseMap.put("quest27", candidateResponses.getQNo_27());
		questionResponseMap.put("quest28", candidateResponses.getQNo_28());
		questionResponseMap.put("quest29", candidateResponses.getQNo_29());
		questionResponseMap.put("quest30", candidateResponses.getQNo_30());
		questionResponseMap.put("quest31", candidateResponses.getQNo_31());
		questionResponseMap.put("quest32", candidateResponses.getQNo_32());
		questionResponseMap.put("quest33", candidateResponses.getQNo_33());
		questionResponseMap.put("quest34", candidateResponses.getQNo_34());
		questionResponseMap.put("quest35", candidateResponses.getQNo_35());
		questionResponseMap.put("quest36", candidateResponses.getQNo_36());
		questionResponseMap.put("quest37", candidateResponses.getQNo_37());
		questionResponseMap.put("quest38", candidateResponses.getQNo_38());
		questionResponseMap.put("quest39", candidateResponses.getQNo_39());
		questionResponseMap.put("quest40", candidateResponses.getQNo_40());
		questionResponseMap.put("quest41", candidateResponses.getQNo_41());
		questionResponseMap.put("quest42", candidateResponses.getQNo_42());
		questionResponseMap.put("quest43", candidateResponses.getQNo_43());
		questionResponseMap.put("quest44", candidateResponses.getQNo_44());
		questionResponseMap.put("quest45", candidateResponses.getQNo_45());
		questionResponseMap.put("quest46", candidateResponses.getQNo_46());
		questionResponseMap.put("quest47", candidateResponses.getQNo_47());

		return questionResponseMap;
	}

	/**
	 * For cases where the one item tagged to FRIN is positive and the other
	 * item tagged to FRIN is negative Option 1 Option 1 1 Option 1 Option 3 1
	 * Option 2 Option 2 1 Option 2 Option 4 1 Option 3 Option 3 1 Option 3
	 * Option 4 1 Option 4 Option 3 1 Option 4 Option 4 1 else are 0
	 *
	 * @param question1
	 * @param question2
	 * @return
	 */
	private int getFrinForPositiveNeghativeQuestionTagWTS(String question1, String question2) {
		int totalScore = 0;
		String answers1[] = question1.split("--");
		String answers2[] = question2.split("--");
		if (answers1[2].equalsIgnoreCase("CHOICE1")
				&& (answers2[2].equalsIgnoreCase("CHOICE1") || answers2[2].equalsIgnoreCase("CHOICE3"))) {
			totalScore = totalScore + 1;
		}
		if (answers1[2].equalsIgnoreCase("CHOICE2")
				&& (answers2[2].equalsIgnoreCase("CHOICE2") || answers2[2].equalsIgnoreCase("CHOICE4"))) {
			totalScore = totalScore + 1;
		}
		if (answers1[2].equalsIgnoreCase("CHOICE3")
				&& (answers2[2].equalsIgnoreCase("CHOICE3") || answers2[2].equalsIgnoreCase("CHOICE4"))) {
			totalScore = totalScore + 1;
		}
		if (answers1[2].equalsIgnoreCase("CHOICE4")
				&& (answers2[2].equalsIgnoreCase("CHOICE3") || answers2[2].equalsIgnoreCase("CHOICE4"))) {
			totalScore = totalScore + 1;
		}
		return totalScore;
	}

	/**
	 * For cases where the one item tagged to FRIN is positive and the other
	 * item tagged to FRIN is negative Option 1 Option 1 1 Option 1 Option 3 1
	 * Option 2 Option 2 1 Option 2 Option 4 1 Option 3 Option 3 1 Option 3
	 * Option 4 1 Option 4 Option 3 1 Option 4 Option 4 1 else are 0
	 *
	 * @param question1
	 * @param question2
	 * @return
	 */
	private int getFrinForNeghativeNeghativeQuestionTagWTS(String question1, String question2) {
		int totalScore = 0;
		String answers1[] = question1.split("--");
		String answers2[] = question2.split("--");
		if (answers1[2].equalsIgnoreCase("CHOICE1")
				&& (answers2[2].equalsIgnoreCase("CHOICE3") || answers2[2].equalsIgnoreCase("CHOICE4"))) {
			totalScore = totalScore + 1;
		}
		if (answers1[2].equalsIgnoreCase("CHOICE2")
				&& (answers2[2].equalsIgnoreCase("CHOICE3") || answers2[2].equalsIgnoreCase("CHOICE4"))) {
			totalScore = totalScore + 1;
		}
		if (answers1[2].equalsIgnoreCase("CHOICE3")
				&& (answers2[2].equalsIgnoreCase("CHOICE1") || answers2[2].equalsIgnoreCase("CHOICE2"))) {
			totalScore = totalScore + 1;
		}
		if (answers1[2].equalsIgnoreCase("CHOICE4")
				&& (answers2[2].equalsIgnoreCase("CHOICE1") || answers2[2].equalsIgnoreCase("CHOICE2"))) {
			totalScore = totalScore + 1;
		}
		return totalScore;
	}

	/**
	 * @param componant
	 *            type where we have calculate score
	 * @param candidateResponses
	 *            map of question with there Id as key and response as a value
	 * @return total marks
	 */
	public int getFrinForWTS(String componant, RpsCandidateResponses candidateResponses) {
		List<String> neghativeQuestionList = Arrays.asList("quest2", "quest3", "quest10", "quest15", "quest16",
				"quest17", "quest20", "quest21", "quest23", "quest27", "quest32", "quest33", "quest34", "quest38",
				"quest39", "quest41", "quest43", "quest44", "quest45", "quest46", "quest47");

		Map<String, String> questionResponseMap = getQuestionLevelResponseMap(candidateResponses);
		switch (componant) {
		case "Critical Thinking":
			int toatalFrinScore = 0;
			for (Map.Entry<String, String> questionAnwser : questionResponseMap.entrySet()) {
				/** Tag 1 & 27 Tag 2 & 25 Tag 3 & 28 */
				if (questionAnwser.getKey().equals("quest1")) {
					if (neghativeQuestionList.contains("quest1") && neghativeQuestionList.contains("quest27")) {
						toatalFrinScore += getFrinForNeghativeNeghativeQuestionTagWTS(questionAnwser.getValue(),
								questionResponseMap.get("quest27"));
					} else {
						toatalFrinScore += getFrinForPositiveNeghativeQuestionTagWTS(questionAnwser.getValue(),
								questionResponseMap.get("quest27"));
					}
				}
				if (questionAnwser.getKey().equals("quest2")) {
					if (neghativeQuestionList.contains("quest1") && neghativeQuestionList.contains("quest25")) {
						toatalFrinScore += getFrinForNeghativeNeghativeQuestionTagWTS(questionAnwser.getValue(),
								questionResponseMap.get("quest25"));
					} else {
						toatalFrinScore += getFrinForPositiveNeghativeQuestionTagWTS(questionAnwser.getValue(),
								questionResponseMap.get("quest25"));
					}
				}
				if (questionAnwser.getKey().equals("quest3")) {
					if (neghativeQuestionList.contains("quest1") && neghativeQuestionList.contains("quest28")) {
						toatalFrinScore += getFrinForNeghativeNeghativeQuestionTagWTS(questionAnwser.getValue(),
								questionResponseMap.get("quest28"));
					} else {
						toatalFrinScore += getFrinForPositiveNeghativeQuestionTagWTS(questionAnwser.getValue(),
								questionResponseMap.get("quest28"));
					}
				}
			}
			return toatalFrinScore;
		case "Creative Thinking":
			/** Tag 7 & 34 Tag 10 & 32 Tag 11 & 33 */
			toatalFrinScore = 0;
			for (Map.Entry<String, String> questionAnwser : questionResponseMap.entrySet()) {
				if (questionAnwser.getKey().equals("quest7")) {
					if (neghativeQuestionList.contains("quest7") && neghativeQuestionList.contains("quest34")) {
						toatalFrinScore += getFrinForNeghativeNeghativeQuestionTagWTS(questionAnwser.getValue(),
								questionResponseMap.get("quest34"));
					} else {
						toatalFrinScore += getFrinForPositiveNeghativeQuestionTagWTS(questionAnwser.getValue(),
								questionResponseMap.get("quest34"));
					}
				}
				if (questionAnwser.getKey().equals("quest10")) {
					if (neghativeQuestionList.contains("quest10") && neghativeQuestionList.contains("quest32")) {
						toatalFrinScore += getFrinForNeghativeNeghativeQuestionTagWTS(questionAnwser.getValue(),
								questionResponseMap.get("quest32"));
					} else {
						toatalFrinScore += getFrinForPositiveNeghativeQuestionTagWTS(questionAnwser.getValue(),
								questionResponseMap.get("quest32"));
					}
				}
				if (questionAnwser.getKey().equals("quest11")) {
					if (neghativeQuestionList.contains("quest11") && neghativeQuestionList.contains("quest33")) {
						toatalFrinScore += getFrinForNeghativeNeghativeQuestionTagWTS(questionAnwser.getValue(),
								questionResponseMap.get("quest33"));
					} else {
						toatalFrinScore += getFrinForPositiveNeghativeQuestionTagWTS(questionAnwser.getValue(),
								questionResponseMap.get("quest33"));
					}
				}

			}
			return toatalFrinScore;
		case "Collaboration":
			/**
			 * Tag 13 & 38 Tag 15 & 37 Tag 16 & 40
			 */
			toatalFrinScore = 0;
			for (Map.Entry<String, String> questionAnwser : questionResponseMap.entrySet()) {

				if (questionAnwser.getKey().equals("quest13")) {
					if (neghativeQuestionList.contains("quest13") && neghativeQuestionList.contains("quest38")) {
						toatalFrinScore += getFrinForNeghativeNeghativeQuestionTagWTS(questionAnwser.getValue(),
								questionResponseMap.get("quest38"));
					} else {
						toatalFrinScore += getFrinForPositiveNeghativeQuestionTagWTS(questionAnwser.getValue(),
								questionResponseMap.get("quest38"));
					}
				}
				if (questionAnwser.getKey().equals("quest15")) {
					if (neghativeQuestionList.contains("quest15") && neghativeQuestionList.contains("quest37")) {
						toatalFrinScore += getFrinForNeghativeNeghativeQuestionTagWTS(questionAnwser.getValue(),
								questionResponseMap.get("quest37"));
					} else {
						toatalFrinScore += getFrinForPositiveNeghativeQuestionTagWTS(questionAnwser.getValue(),
								questionResponseMap.get("quest37"));
					}
				}
				if (questionAnwser.getKey().equals("quest16")) {
					if (neghativeQuestionList.contains("quest16") && neghativeQuestionList.contains("quest40")) {
						toatalFrinScore += getFrinForNeghativeNeghativeQuestionTagWTS(questionAnwser.getValue(),
								questionResponseMap.get("quest40"));
					} else {
						toatalFrinScore += getFrinForPositiveNeghativeQuestionTagWTS(questionAnwser.getValue(),
								questionResponseMap.get("quest40"));
					}
				}
			}
			return toatalFrinScore;
		case "Customer Centricity":
			toatalFrinScore = 0;
			for (Map.Entry<String, String> questionAnwser : questionResponseMap.entrySet()) {
				/** Tag 21 & 44 Tag 23 & 47 */
				if (questionAnwser.getKey().equals("quest21")) {
					if (neghativeQuestionList.contains("quest21") && neghativeQuestionList.contains("quest44")) {
						toatalFrinScore += getFrinForNeghativeNeghativeQuestionTagWTS(questionAnwser.getValue(),
								questionResponseMap.get("quest44"));
					} else {
						toatalFrinScore += getFrinForPositiveNeghativeQuestionTagWTS(questionAnwser.getValue(),
								questionResponseMap.get("quest44"));
					}
				}
				if (questionAnwser.getKey().equals("quest23")) {
					if (neghativeQuestionList.contains("quest23") && neghativeQuestionList.contains("quest47")) {
						toatalFrinScore += getFrinForNeghativeNeghativeQuestionTagWTS(questionAnwser.getValue(),
								questionResponseMap.get("quest47"));
					} else {
						toatalFrinScore += getFrinForPositiveNeghativeQuestionTagWTS(questionAnwser.getValue(),
								questionResponseMap.get("quest47"));
					}
				}
			}
			return toatalFrinScore;
		case "Fake Detector":
			toatalFrinScore = 0;
			for (Map.Entry<String, String> questionAnwser : questionResponseMap.entrySet()) {
				switch (questionAnwser.getKey()) {
				case "quest6":
				case "quest12":
				case "quest18":
				case "quest24":
				case "quest30":
				case "quest36":
				case "quest42":
					String answers1[] = questionAnwser.getValue().split("--");
					if (answers1[2].equalsIgnoreCase("CHOICE1")) {
						toatalFrinScore++;
					}
					break;
				}

			}
			return toatalFrinScore;
		}

		return 0;
	}

	public String getFrin(String componant, RpsCandidateResponses candidateResponses) {

		logger.info("---IN---getfrin methods for  {} ", componant);
		double frinScore = this.getFrinForWTS(componant, candidateResponses);
		double perc = 0.0;
		if (componant.equals("Fake Detector"))
			perc = (frinScore / 7) * 100;
		else if (componant.equals("Customer Centricity"))
			perc = (frinScore / 2) * 100;
		else
			perc = (frinScore / 3) * 100;
		if (perc > 50)
			return "red";
		else if (perc > 25 && perc < 51) {
			if (componant.equals("Fake Detector"))
				return "yellow";
			else
				return "bar";
		} else
			return "green";
	}

	public String getOverAllFrin(String[] componants, RpsCandidateResponses candidateResponses) {

		double frinScore = 0;
		double perc = 0.0;
		for (String componant : componants) {

			frinScore = frinScore + this.getFrinForWTS(componant, candidateResponses);
			logger.info("---IN---getfrin methods for  {} ", frinScore);

		}
		perc = (frinScore / 11) * 100;
		if (perc > 50)
			return "red";
		else if (perc > 25 && perc < 51) {
			return "bar";
		} else
			return "green";
	}

}
