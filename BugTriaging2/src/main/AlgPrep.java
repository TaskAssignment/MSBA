package main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import data.Assignee;
import data.Assignment;
import data.AssignmentStat;
import data.AssignmentStatSummary;
import data.Bug;
import data.Evidence;
import data.Project;
import utils.Constants;
import utils.Constants.ASSIGNMENT_TYPE_TO_TRIAGE;
import utils.Constants.BTOption10_whatToAddToAllBCs;
import utils.Constants.BTOption11_whatToAddToAllCCs;
import utils.Constants.BTOption12_whatToAddToAllPRCs;
import utils.Constants.BTOption1_whatToAddToAllBugs;
import utils.Constants.BTOption2_w;
import utils.Constants.BTOption3_TF;
import utils.Constants.BTOption4_IDF;
import utils.Constants.BTOption5_prioritizePAs;
import utils.Constants.BTOption6_whatToAddToAllCommits;
import utils.Constants.BTOption7_whenToCountTextLength;
import utils.Constants.BTOption8_recency;
import utils.Constants.BTOption9_whatToAddToAllPRs;
import utils.Constants.ProjectType;
import utils.Constants.REFERENCE_TYPE;
import utils.Constants.RUN_TYPE;
import utils.Constants.RUN_TYPE_FOR_SO_CONTRIBUTIONS;
import utils.FileManipulationResult;
import utils.Graph;
import utils.MyUtils;
import utils.StringManipulations;

public class AlgPrep {
	//------------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------------
	private static final String TAB = Constants.TAB;
	public static int maxFreqOfAWordInAnEvidence = 0;
	public static final String ALL_PROJECTS = "ALL_PROJECTS";
	public static Random random = new Random();			
	//------------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------------
	//The following method returns project type (one of the 13 main FASE projects, 3 other projects, project families or other (unknown)).
	public static ProjectType projectType(String projectId, String owner_repo){
		String[] listOf13Projects__owner_repo = {"rails/rails", "yui/yui3", "lift/framework", "fog/fog", "julialang/julia", "angular/angular.js", "elastic/elasticsearch", 
				"travis-ci/travis-ci", "saltstack/salt", "khan/khan-exercises", "adobe/brackets", "html5rocks/www.html5rocks.com", "tryghost/ghost", };
		String[] listOf13Projects__id = {"8514", "85670", "1295197", "203666", "1644196", "460078", "507775", 
				"1420493", "1390248", "1723225", "2935735", "5238231", "9852918", };
		
		String[] listOf3ProjectsWithNoPublicBugs__owner_repo = {"scala/scala", "mozilla-b2g/gaia", "edx/edx-platform"}; 
		String[] listOf3ProjectsWithNoPublicBugs__id = {"2888818", "2317369", "10391073"}; 
		
		String[] listOfFamiliesOfTwoProjects__owner_repo = {"rails/activeresource", "rails/arel", "rails/sprockets", "rails/jquery-rails", 
				"rails/execjs", "rails/sass-rails", "rails/jbuilder", "rails/strong_parameters", "rails/sprockets-rails", 
				"rails/protected_attributes", "rails/spring", "rails/web-console", "rails/globalid", 
				"angular/angular-seed", "angular/angularjs.org", "angular/angular-phonecat", 
				"angular/protractor", "angular/dgeni-packages", "angular/material"}; 
		String[] listOfFamiliesOfTwoProjects__id = {"3711416", "337788", "32104924", "1795951", 
				"32104914", "1795273", "2861056", "3710607", "1784628", 
				"5674986", "7362671", "12496351", "22991474", 
				"1195004", "1343653", "1452079", 
				"7639232", "16757508", "21399598"}; 

		for (int i=0; i<listOf13Projects__owner_repo.length; i++)
			if (owner_repo.equals(listOf13Projects__owner_repo[i]) || projectId.equals(listOf13Projects__id[i]))
				return ProjectType.FASE_13;
		for (int i=0; i<listOfFamiliesOfTwoProjects__owner_repo.length; i++)
			if (owner_repo.equals(listOfFamiliesOfTwoProjects__owner_repo[i]) || projectId.equals(listOfFamiliesOfTwoProjects__id[i]))
				return ProjectType.FASE_13_EXTENSION__PROJECT_FAMILIES_OF_TWO_PROJECTS;
		for (int i=0; i<listOf3ProjectsWithNoPublicBugs__owner_repo.length; i++)
			if (owner_repo.equals(listOf3ProjectsWithNoPublicBugs__owner_repo[i]) || projectId.equals(listOf3ProjectsWithNoPublicBugs__id[i]))
				return ProjectType.FASE_3__NO_PUBLIC_BUGS;
		return ProjectType.OTHERS_UNKNOWN;
	}
	//------------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------------
	//The following method returns true if the project is one of the three projects considered for tuning. Otherwise returns false.
	public static boolean isAProjectWhichIsUsedForTuning(String projectId, String owner_repo){
		String[] listOf3ProjectsForTuning__owner_repo = {"lift/framework", "fog/fog", "adobe/brackets"}; 
		String[] listOf3ProjectsForTuning__id = {"1295197", "203666", "2935735"}; 
		
		for (int i=0; i<listOf3ProjectsForTuning__owner_repo.length; i++)
			if (owner_repo.equals(listOf3ProjectsForTuning__owner_repo[i]) || projectId.equals(listOf3ProjectsForTuning__id[i]))
				return true;
		return false;
	}
	//------------------------------------------------------------------------------------------------------------------------
	//The following method returns true if the project is one of the few specific projects. Otherwise returns false.
	public static boolean isASelectedProjectToRunTheExperimentOn(String projectId, String owner_repo){
		String[] listOfSelectedProjects5__owner_repo = {"angular/angular.js", "rails/rails"}; 
		String[] listOfselectedProjects5__id = {"460078", "8514"}; 
		for (int i=0; i<listOfSelectedProjects5__owner_repo.length; i++)
			if (owner_repo.equals(listOfSelectedProjects5__owner_repo[i]) || projectId.equals(listOfselectedProjects5__id[i]))
				return true;
//		switch (assignmentTypeToTriage){
//		case T1_AUTHOR: 
//			String[] listOfSelectedProjects1__owner_repo = {"lift/framework", "yui/yui3", "TryGhost/Ghost", "JuliaLang/julia", "adobe/brackets", "elastic/elasticsearch", "saltstack/salt", "angular/angular.js", "rails/rails"}; 
//			String[] listOfSelectedProjects1__id = {"1295197", "85670", "9852918", "1644196", "2935735", "507775", "1390248", "460078", "8514"}; 
//			for (int i=0; i<listOfSelectedProjects1__owner_repo.length; i++)
//				if (owner_repo.equals(listOfSelectedProjects1__owner_repo[i]) || projectId.equals(listOfSelectedProjects1__id[i]))
//					return true;
//			break;
//		case T2_COAUTHOR: 
//			String[] listOfSelectedProjects2__owner_repo = {"TryGhost/Ghost", "elastic/elasticsearch", "saltstack/salt", "angular/angular.js", "rails/rails"}; 
//			String[] listOfSelectedProjects2__id = {"9852918", "507775", "1390248", "460078", "8514"}; 
//			for (int i=0; i<listOfSelectedProjects2__owner_repo.length; i++)
//				if (owner_repo.equals(listOfSelectedProjects2__owner_repo[i]) || projectId.equals(listOfSelectedProjects2__id[i]))
//					return true;
//			break;
//		case T3_ADMIN_CLOSER: 
//			String[] listOfSelectedProjects3__owner_repo = {"lift/framework", "html5rocks/www.html5rocks.com", "yui/yui3", "Khan/khan-exercises", "TryGhost/Ghost", "fog/fog", "JuliaLang/julia", "adobe/brackets", "travis-ci/travis-ci", "elastic/elasticsearch", "saltstack/salt", "angular/angular.js", "rails/rails"}; 
//			String[] listOfSeletedProjects3__id = {"1295197", "5238231", "85670", "1723225", "9852918", "203666", "1644196", "2935735", "1420493", "507775", "1390248", "460078", "8514"}; 
//			for (int i=0; i<listOfSelectedProjects3__owner_repo.length; i++)
//				if (owner_repo.equals(listOfSelectedProjects3__owner_repo[i]) || projectId.equals(listOfSeletedProjects3__id[i]))
//					return true;
//			break;
//		case T4_DRAFTED_A: 
//			String[] listOfSelectedProjects4__owner_repo = {"lift/framework", "html5rocks/www.html5rocks.com", "yui/yui3", "Khan/khan-exercises", "TryGhost/Ghost", "JuliaLang/julia", "adobe/brackets", "travis-ci/travis-ci", "elastic/elasticsearch", "saltstack/salt", "angular/angular.js", "rails/rails"}; 
//			String[] listOfSelectedProjects4__id = {"1295197", "5238231", "85670", "1723225", "9852918", "1644196", "2935735", "1420493", "507775", "1390248", "460078", "8514"}; 
//			for (int i=0; i<listOfSelectedProjects4__owner_repo.length; i++)
//				if (owner_repo.equals(listOfSelectedProjects4__owner_repo[i]) || projectId.equals(listOfSelectedProjects4__id[i]))
//					return true;
//			break;
//		case T5_ALL_TYPES: 
//			String[] listOfSelectedProjects5__owner_repo = {"lift/framework", "html5rocks/www.html5rocks.com", "yui/yui3", "Khan/khan-exercises", "TryGhost/Ghost", "fog/fog", "JuliaLang/julia", "adobe/brackets", "travis-ci/travis-ci", "elastic/elasticsearch", "saltstack/salt", "angular/angular.js", "rails/rails"}; 
//			String[] listOfselectedProjects5__id = {"1295197", "5238231", "85670", "1723225", "9852918", "203666", "1644196", "2935735", "1420493", "507775", "1390248", "460078", "8514"}; 
//			for (int i=0; i<listOfSelectedProjects5__owner_repo.length; i++)
//				if (owner_repo.equals(listOfSelectedProjects5__owner_repo[i]) || projectId.equals(listOfselectedProjects5__id[i]))
//					return true;
//			break;
//		}
		return false;
	}
	//------------------------------------------------------------------------------------------------------------------------
	public static Assignee updateRankOfRealAssigneesAndReturnTheBestAssignee(HashMap<String, HashMap<String, Integer>> realAssignees, 
			String bugNumber, HashMap<String, Double> scores, Random random){
		//Rank the list of all community members, then update the ranks of real assignees in realAssignees. Finally return the assignee with the best rank: 
		//This method checks the score of all real assignees of the given bug number against all the other scores.
			//Then updates the ranks of real assignees in realAssignees. Finally returns the assignee with the highest rank.
				//Note1: if two developers has the same score, considers a random ordering.
				//Note2: It does not necessarily sort the scores. For each real assignee, we just want to obtain a and b; 
					//a: how many scores are higher than the score of those real assignees, and b: how many are equal to them.
					//Then the best rank of real assignee is somewhere between a+1 and a+b+1.
		Assignee topRA = new Assignee("", Constants.AN_EXTREMELY_POSITIVE_INT);
		
		HashMap<String, Integer> realAssigneesOfThisBug = realAssignees.get(bugNumber);
		HashSet<Integer> bookedRanks = new HashSet<Integer>();
		for (String login: realAssigneesOfThisBug.keySet()){//:for each of the real assignees of this bug, we need to count a and b:
			//First count the number of developers with higher score than this "real assignee developer"'s score (and also the number of developers equal to it):
			int a = 0;
			int b = 0;
			Double scoreOfThisRA = scores.get(login);
			for (Double aScore: scores.values()){
				if (aScore > scoreOfThisRA)
					a++;
				else
					if (aScore.equals(scoreOfThisRA))
						b++; // this value will be at least one, because the score of a developer is at least equal to his score.
			}
			//Now, obtain the ranks for these "real assignee developer"s: 
			//rank of this assignee = 
				//fairRandomRank = a + random.nextInt(b)+1
					//but we need to make sure there is no other real assignees with the same fairRandomRank.
					//so we check in bookedRanks and if there is any, assign another rank until we find a non-existing fairRandomRank:
			int fairRandomRank;
			if (b == 1) //: if there is no other developer with this rank:
				fairRandomRank = a + 1;
			else{
				fairRandomRank = a + random.nextInt(b) + 1;
				while (bookedRanks.contains(fairRandomRank))//: guarantee that no other assignee gets the same fairRandomRank:
					fairRandomRank = a + random.nextInt(b) + 1;
				bookedRanks.add(fairRandomRank); //: this fairRandomRank is reserved for this real assignee. Record it so for the next real assignees we can check against.
			}
			realAssigneesOfThisBug.put(login, fairRandomRank); //: this is to affect realAssigneesOfThisBug and hence realAssignees. At the end, all the real assignees of this bug are set with their ranks regarding their scores for that bug.
			if (fairRandomRank < topRA.rank){//: set the topRA to be returned at the end:
				topRA.rank = fairRandomRank;
				topRA.login = login;
			}
		}
		return topRA;
	}
	//------------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------------
	public static double calculateScoreOfDeveloperForBugAssignment(String login, Assignment a, 
			Graph graph, 
			int assignmentTypeToTriage, int[] evidenceTypesToConsider, int evidenceTypesToConsider_count, 
			HashMap<String, HashMap<String, HashMap<Integer, ArrayList<Evidence>>>> logins_Tags_TypesAndTheirEvidence_InAProject,
			HashSet<String> previousAssigneesInThisProject, 
			WordsAndCounts wAC, int originalNumberOfWordsInBugText, 
			int seqNum, //seqNum is the sequence number of the bug. It is used for determining the recency based on FASE paper formula (number of bugs between a bugAssignmentEvidence and the current bug). 
			Date beginningDateOfProject, ReferenceStatus referenceStatus, //developer_referenceType__referenceDates is the references to the developers' name in bug(comments) and commit(comments).
			BTOption2_w option2_w, BTOption4_IDF option4_IDF, BTOption5_prioritizePAs option5_prioritizePAs, BTOption8_recency option8_recency, 
			int[] option13_referenceTypesToConsider, 
			int indentationLevel){
		//This method calculates the score of developer "login" for assignment "a". 
		//		It considers the evidence of expertise from beginning of project until the time of "a". 
		//			Later, it also considers the evidence in other projects (the project family experiment) using projectsAndTheirAssignments.
		Double score = 0.0;
		Double subScore = 0.0;
		Double termWeight;
		int errors1 = 0;
		int errors2 = 0;
		int errors3_possibly = 0;
		int errors4_bothAreOne = 0;
		if (logins_Tags_TypesAndTheirEvidence_InAProject.containsKey(login)){ //: means that if this user has an evidence ever!
			Date assignmentDate = a.date;
			Date evidenceDate;
			double recency2 = 1;
			HashMap<String, HashMap<Integer, ArrayList<Evidence>>> tags_TypesAndTheirEvidence_ForADeveloperInAProject = logins_Tags_TypesAndTheirEvidence_InAProject.get(login);
			for (int i=0; i<wAC.size; i++){//: Iterating over keywords (tags) of bug.
				if (tags_TypesAndTheirEvidence_ForADeveloperInAProject.containsKey(wAC.words[i])){//: means that if this user has an evidence including this tag.
					HashMap<Integer, ArrayList<Evidence>> typesAndEvidenceOfThisDeveloperForATag = tags_TypesAndTheirEvidence_ForADeveloperInAProject.get(wAC.words[i]); //: this is assuming that the non-SO-tag keywords are removed from the text of a bug.
					termWeight = graph.getNodeWeight(wAC.words[i]);
					//Considering all different types of evidence (0: Constants.EVIDENCE_TYPE__BUG_TITLE to Constants.EVIDENCE_TYPES__COUNT-1):
					subScore = 0.0;
					for (int et_index=0; et_index<evidenceTypesToConsider_count; et_index++){//et: "evidence type"
						int et = evidenceTypesToConsider[et_index];
						if (typesAndEvidenceOfThisDeveloperForATag.containsKey(et)){
							ArrayList<Evidence> type_x_evidenceOfADeveloperForATag = typesAndEvidenceOfThisDeveloperForATag.get(et); //: get specific evidence types (e.g., bug title, bug description, commit message, etc.)
							int numberOfType_x_evidence = type_x_evidenceOfADeveloperForATag.size();
							for (int j=0; j<numberOfType_x_evidence; j++){//: Iterating over all evidence (of the current user in the current project) for this tag.
								Evidence e = type_x_evidenceOfADeveloperForATag.get(j);
								if (e.date.compareTo(a.date) < 0){ //: Only consider the evidence before the date of assignment "a".  
									evidenceDate = e.date;
									if (et >= Constants.ASSIGNMENT_TYPE_TO_TRIAGE.T1_AUTHOR.ordinal() && et <= ASSIGNMENT_TYPE_TO_TRIAGE.T5_ALL_TYPES.ordinal()){ //(case #1): 0 to 4, which are the assignment types.
										if (e.bASeqNum >= seqNum)
											errors1++;
										if (e.bASeqNum == Constants.SEQ_NUM____THIS_IS_NOT__B_A_EVIDENCE)
											System.out.println("ERROR1!");
										if (option8_recency == BTOption8_recency.RECENCY2) //note: here, we just calculate recency2. recency1 is the same for case #1 and case #2 (will be calculated directly in the subScore formula later).
											recency2 = 1.0/(seqNum - e.bASeqNum); //case #1: This is the recency for bug assignment evidence.
									}
									else
										if ((et >= 20+Constants.ASSIGNMENT_TYPE_TO_TRIAGE.T1_AUTHOR.ordinal() && et <= 20+ASSIGNMENT_TYPE_TO_TRIAGE.T5_ALL_TYPES.ordinal())
												||(et >= Constants.EVIDENCE_TYPE_COMMIT && et <= Constants.EVIDENCE_TYPE_PR_COMMENT)
												||(et >= Constants.EVIDENCE_TYPE__MULTI_PROJ__COMMIT && et <= Constants.EVIDENCE_TYPE__MULTI_PROJ__PR_COMMENT)){ 
											//(case #2): MultiProj assignment types (20 to 24), or, different evidence for simple and multiProj cases (11 to 15 for simple case and 31 to 35 for MultiProj case):
											if (e.nonBA__or_subProj_virtualSeqNum[assignmentTypeToTriage] > seqNum)
												errors2++;
											if (e.nonBA__or_subProj_virtualSeqNum[assignmentTypeToTriage] == seqNum){
												errors3_possibly++;
												System.out.println("seqNum: " + seqNum);
												if (e.nonBA__or_subProj_virtualSeqNum[assignmentTypeToTriage] == 1)
													errors4_bothAreOne++;
											}
											//											System.out.println("ERROR! The sequence number of the bug is smaller than the sequence number of the evidence!");
											if (e.nonBA__or_subProj_virtualSeqNum[assignmentTypeToTriage] == Constants.SEQ_NUM____NO_NEED_TO_TRIAGE_THIS_TYPE___OR___THIS_IS_NOT__NON_B_A_EVIDENCE)
												System.out.println("ERROR2!");
											if (option8_recency == BTOption8_recency.RECENCY2) //note: here, we just calculate recency2. recency1 is the same for case #1 and case #2 (will be calculated directly in the subScore formula later).
												recency2 = 1.0/(seqNum - e.nonBA__or_subProj_virtualSeqNum[assignmentTypeToTriage]); //case #2: This is the recency for other types of evidence.
										}
									//option3_TF is included in the Evidence indexing (addToIndex() and indexAssignmentEvidence() and readAndIndexNonAssignmentEvidence()). Here, we just read the value:
									switch (option8_recency){
										case NO_RECENCY: //Constants.TYPE_SIMILARITY[et] is not considered in the first two options since it is added in expertiment2 (multi-source) and in experiment2 just the last option (recency2) is considered.
											subScore = subScore + e.tf;
											break;
										case RECENCY1: //subScore = subScore + e.tf * recency1:
											subScore = subScore + e.tf*((double)(evidenceDate.getTime()-beginningDateOfProject.getTime())/(long)(assignmentDate.getTime()-beginningDateOfProject.getTime()));
											break;
										case RECENCY2: //subScore = subScore + e.tf * recency2:
											subScore = subScore + e.tf*recency2*Constants.TYPE_SIMILARITY[et]; //: This is the recency that is calculated based on one of the two cases above (case #1 and case #2).
											break;
									}
//									subScore = subScore + e.tf; //*recency*context or *recency*e.type...
//									subScore = subScore + e.freq*recency1; //*recency*context or *recency*e.type...
//									subScore = subScore + e.tf*recency1*Constants.TYPE_SIMILARITY[et]; //*recency*context or *recency*e.type...
//									subScore = subScore + e.tf*recency3*Constants.TYPE_SIMILARITY[et]; //*recency*context or *recency*e.type...
//									subScore = subScore + e.tf*recency2; //*recency*context or *recency*e.type...
								
//									recency1 = ((double)(evidenceDate.getTime()-beginningDateOfProject.getTime())/(long)(assignmentDate.getTime()-beginningDateOfProject.getTime()));
//									recency2 = 1.2 - java.lang.Math.log10(99+(assignmentDate.getTime()-evidenceDate.getTime())/1000)/10;
								}
								else //: Since the evidence are ordered by date, break if the date is not before the date of assignment:
									break;				
							} //for (j
						}
					}
					switch (option2_w){//: Term weighting
					case NO_TERM_WEIGHTING:
						switch (option4_IDF){//: IDF formula
						case ONE:
							score = score + subScore;
							break;
						case FREQ:
							score = score + subScore * wAC.counts[i];
							break;
						case FREQ__TOTAL_NUMBER_OF_TERMS:
							score = score + subScore * wAC.counts[i]/wAC.totalNumberOfWords;
							break;
						case LOG_BASED:
							score = score + subScore * (1+Math.log10(wAC.counts[i]));
							break;
						}
					case USE_TERM_WEIGHTING:
						switch (option4_IDF){//: IDF formula
						case ONE:
							score = score + subScore * termWeight;
							break;
						case FREQ:
							score = score + subScore * termWeight * wAC.counts[i];
							break;
						case FREQ__TOTAL_NUMBER_OF_TERMS:
							score = score + subScore * termWeight * wAC.counts[i]/wAC.totalNumberOfWords;
							break;
						case LOG_BASED:
							score = score + subScore * termWeight * (1+Math.log10(wAC.counts[i]));
							break;
						}
					}
//					score = score + termWeight * subScore * wAC.counts[i]/wAC.totalNumberOfWords; //
//					score = score + subScore * wAC.counts[i];
					
//					score = score + subScore;
//					score = score + subScore;
//					if (wAC.counts[i] == 1)
//						score = score + termWeight * subScore;
//					else
//						score = score + termWeight * subScore * (1+Math.log10(wAC.counts[i]));
					
//					System.out.println(1+Math.log(wAC.counts[i]));

					//testing if the termWeight is wrong:
//						score = score + subScore/termWeight;
				}
			}//for (i
		}
		if (errors1>0)
			System.out.println(errors1 + " ERRORS1 in seqNum1: The sequence number of the assignment evidence is greater than the sequence number of the bug!");
		if (errors2>0)
			System.out.println(errors2 + " ERRORS2 in seqNum2: The sequence number of the non-assignment evidence is greater than the sequence number of the bug!");
		if (errors3_possibly>0)
			System.out.println(errors3_possibly + " Possible ERRORS3 in seqNum2: The sequence number of the non-assignment evidence is equal to the sequence number of the bug!");
		if (errors4_bothAreOne>0)
			System.out.println(errors4_bothAreOne + " Possible ERRORS4 in seqNum2: The sequence number of the non-assignment evidence is equal to the sequence number of the bug and both are equal to 1!");
		if (errors1>0 || errors2>0 || errors3_possibly>0 || errors4_bothAreOne>0)
			System.out.println("ERROR");

//		for (int i=0; i<10; i++)
//			if (score > Constants.highScores[i]){
//				Constants.highScores[i] = score;
//				break;
//			}
		
		//HashMap<String, HashMap<REFERENCE_TYPE, ArrayList<String>>> developer_referenceType__referenceDates
		
		if (option5_prioritizePAs == BTOption5_prioritizePAs.PRIORITY_FOR_PREVIOUS_ASSIGNEES){//: Prioritize previous assignees
			if (previousAssigneesInThisProject.contains(login))
				score = score + 10000;
		}
		if (atLeastOneOfReferenceTypesShouldBeConsidered(option13_referenceTypesToConsider) 
				&& referenceStatus != null){
//			if (referenceStatus.successfulNumberOfReferencesUpToNow > 0){
			if (referenceStatus.successfulNumberOfReferencesUpToNow >= referenceStatus.totalNumberOfReferencesUpToNow/2){//: means that if half of the references were successful.
				HashMap<REFERENCE_TYPE, ArrayList<Date>> references = referenceStatus.references;
				boolean thisDeveloperGotTheScoreOfReference = false;
				for (int i=0; i<Constants.MAX_NUMBER_OF_REFERENCE_TYPES; i++){
					if (!thisDeveloperGotTheScoreOfReference && option13_referenceTypesToConsider[i] == Algorithm.YES){
						REFERENCE_TYPE refType = REFERENCE_TYPE.fromInt(i);
						if (references.containsKey(refType)){
							ArrayList<Date> referenceDates = references.get(refType);
							for (int j=0; j<referenceDates.size(); j++)
								if (referenceDates.get(j).compareTo(a.date)<0){//: means that if the date of reference is prior to the date of assignment.
									if (!thisDeveloperGotTheScoreOfReference){
										score = score + 100000;
//										score = score * 100;
										thisDeveloperGotTheScoreOfReference = true;
									}
								}
								else
									break; //: we do not need the references on the time of the assignment or after that.
						}
					}//if (option13....
				} //for (i.
			}
		}//if (atLeast....
		return score;
	}
	//------------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------------
	//The following method updates developer_referenceStatus for the real assignees who were referenced before the time of assignment (the given time). 
	public static void updateSuccessfulNumberOfReferencesUpToNow(HashMap<String, ReferenceStatus> developer_referenceStatus, 
			HashMap<String, Integer> realAssigneesOfThisBug, Date date, 
			int[] option13_referenceTypesToConsider){
		if (atLeastOneOfReferenceTypesShouldBeConsidered(option13_referenceTypesToConsider) && developer_referenceStatus != null){
			ReferenceStatus referenceStatus;
			for (String login: developer_referenceStatus.keySet()){
				referenceStatus = developer_referenceStatus.get(login);
//				referenceStatus.totalNumberOfReferencesUpToNow = 0;
//				referenceStatus.successfulNumberOfReferencesUpToNow = 0;
				HashMap<REFERENCE_TYPE, ArrayList<Date>> references = referenceStatus.references;
				boolean thisDeveloper_sReferenceIsConsidered = false;
				for (int i=0; i<Constants.MAX_NUMBER_OF_REFERENCE_TYPES; i++){
					if (!thisDeveloper_sReferenceIsConsidered && option13_referenceTypesToConsider[i] == Algorithm.YES){
						REFERENCE_TYPE refType = REFERENCE_TYPE.fromInt(i);
						if (references.containsKey(refType)){
							ArrayList<Date> referenceDates = references.get(refType);
							for (int j=0; j<referenceDates.size(); j++){
								if (referenceDates.get(j).compareTo(date)<0){//: means that if the date of reference is prior to the date of assignment.
									if (!thisDeveloper_sReferenceIsConsidered){
										referenceStatus.totalNumberOfReferencesUpToNow++;
										if (realAssigneesOfThisBug.containsKey(login)){//: means that this login (who is referenced) is also a real assignee.
											referenceStatus.successfulNumberOfReferencesUpToNow++;
											thisDeveloper_sReferenceIsConsidered = true;
										}//if (realAssigneesOf....
									}//if (!thisDeveloper....
								}//if (referenceDates....
								else
									break; //: the other references are later in time (afte the time of the current assignment) and shouldn't be considered at this time.
							}//for (j.
						}//if (references....
					}//if (option13....
				}//for (i.
			}//for (String login.
		}//if (atLeastOne....
	}//updateSuccessfulNumberOfReferencesUpToNow().
	//------------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------------
	public static void writeAssignmentStats(String outputPath, String overalSummariesOutputTSVFileName, String assignmentResultsOveralFolderName, String detailedAssignmentResultsSubfolderName, String detailedSummaryOutputFileNameSuffix,
			TreeMap<String, ArrayList<AssignmentStat>> projectsAndTheirAssignmentStats, 
			TreeMap<String, String> projectNamesAndTheirIds, TreeMap<String, ArrayList<String[]>> projectsAndTheirCommunities, //these two params are for getting project id (ordered by project title) and also the total number of developers in each project.
			String experimentDetails, 
			FileManipulationResult fMR,
			float totalRunningTime, 
			boolean wrapOutputInLines, int showProgressInterval, int indentationLevel, String writeMessageStep){
		if (wrapOutputInLines) 
			MyUtils.println("-----------------------------------", indentationLevel);
		MyUtils.println(writeMessageStep+"- Summarizing assignment statistics and writing:", indentationLevel);
		MyUtils.println("Started ...", indentationLevel+1);
		try {
			//Detailed stats:
			MyUtils.println(writeMessageStep+"-1- Detailed stats (in 5 files in a separate folder) ...", indentationLevel+1);
			FileWriter writer1 = new FileWriter(outputPath+"\\"+assignmentResultsOveralFolderName+"\\"+detailedAssignmentResultsSubfolderName+"\\"+detailedAssignmentResultsSubfolderName+" - "+detailedSummaryOutputFileNameSuffix+".tsv");
			writer1.append("project" + TAB + "bugNumber" + TAB + "assignmentDate" + TAB + "ourTopRecommendedRealAssignee" 
					+ TAB + "ourTopRecommendedRealAssigneeRank" + TAB + "totalCommunityMembers" + TAB + "realAssigneesTillNow" + "\n");
			int totalNOA = 0; //: total number of assignments.
			int min = Integer.MAX_VALUE; //: minNumberOfCommunityMembersInOneProject
			int max = 0; //: maxNumberOfCommunityMembersInOneProject
			HashMap<String, AssignmentStatSummary> projectsAndTheirAssignmentStatSummaries = new HashMap<String, AssignmentStatSummary>(); 
			AssignmentStatSummary aST_overal = new AssignmentStatSummary(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
			for (String owner_repo: projectNamesAndTheirIds.keySet()){
				String projectId = projectNamesAndTheirIds.get(owner_repo);
				if (projectsAndTheirAssignmentStats.containsKey(projectId)){
					int m = projectsAndTheirCommunities.get(projectId).size(); //: Community size.
					if (m > max)
						max = m;
					if (m < min)
						min = m;
					//Adding up to the sum values for this project (will be divided by n later):
						//In fact, these are not the accuracies, etc., but the sum values. 
					ArrayList<AssignmentStat> assignmentStatsOfOneProject = projectsAndTheirAssignmentStats.get(projectId);
					AssignmentStatSummary a = new AssignmentStatSummary(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
					a.n = assignmentStatsOfOneProject.size(); //: number of bug assignments in this project.
					for (int i=0; i<assignmentStatsOfOneProject.size(); i++){
						AssignmentStat as = assignmentStatsOfOneProject.get(i);
						//Writing detailed assignment and prediction info:
						writer1.append(owner_repo + TAB + as.bugNumber + TAB + as.assignmentDate + TAB + as.ourTopRecommendedAssignee
								+ TAB + as.ourTopRecommendedAssigneeRank + TAB + m + TAB + as.getRealAssignees() + "\n");

						//calculating the high level statistics (to be written later [out of this loop]):
						if (as.ourTopRecommendedAssigneeRank < 11){//: <=10
							a.sumTop10++;
							if (as.ourTopRecommendedAssigneeRank < 6){//: <=5
								a.sumTop5++;
								if (as.ourTopRecommendedAssigneeRank == 1){
									a.sumTop1++;
									a.sumPAt1 = a.sumPAt1 + 1; //: "precision at 1" is actually the same as "top 1 accuracy".
									a.sumRAt1 = a.sumRAt1 + 1.0/as.realAssigneesAndTheirRanksForThisAssignment.size();
								}
							}
						}
						double RR = 0; //Reciprocal Rank 
						int topRank = Constants.AN_EXTREMELY_POSITIVE_INT;
						for (int rank: as.realAssigneesAndTheirRanksForThisAssignment.values()){
							RR = RR + 1.0/rank;
							if (rank < topRank)
								topRank = rank;
						}
						RR = RR / as.realAssigneesAndTheirRanksForThisAssignment.size();//: get the average over all real assignees for this assignment.
//						a.sumRR = a.sumRR + RR; //: adding to sumRR, to be divided by total number of bugs later.
						a.sumRR = a.sumRR + 1.0/topRank; //Because MRR only cares about the top assignee's rank. So I commented the above statement and added this line.
						
						//AP (and its sum for different assignments; sumAP) needs to take into account average precision, which needs precision at certain points (i.e., at points that there is an assignee). And precision needs to know #ofRecommendationsMade before a position:
						double AP = 0; //Average Precision
						int numOfRAsInTop5 = 0; //: number of real assignees in top 5 ranks.
						int numOfRAsInTop10 = 0; //: number of real assignees in top 10 ranks.
						if (as.realAssigneesAndTheirRanksForThisAssignment.size() == 1){
							AP = RR;
							int rank = (int)as.realAssigneesAndTheirRanksForThisAssignment.values().toArray()[0];
//							for (int r: as.realAssigneesAndTheirRanksForThisAssignment.values())
//								rank = r; //: there is only one item in the hashMap. So the loop does not repeat!
							if (rank < 11){
								numOfRAsInTop10++;
								if (rank < 6)
									numOfRAsInTop5++;
							}
						}
						else{ //: if there are more than one assignee, we need to process them in order (from the best rank to the worst):
							ArrayList<Integer> ranks = new ArrayList<Integer>();
							for (int rank: as.realAssigneesAndTheirRanksForThisAssignment.values())
								ranks.add(rank);
							Collections.sort(ranks);
							for (int j=0; j<ranks.size(); j++){
								AP = AP + (j+1.0)/ranks.get(j); //: "j+1" is the number of of assignees (hits or appropriate recommendations). "ranks.get(j)" is the rank of real assignee which includes number of (right and wrong) predictions (or number of recommendations made). 
								if (ranks.get(j) < 11){
									numOfRAsInTop10++;
									if (ranks.get(j) < 6)
										numOfRAsInTop5++;
								}
								else //: if this rank is 11+, then the next ranks are worse than that!
									break;
							}
							AP = AP / ranks.size(); //: to obtain the "Average" precision.
						}
						a.sumAP = a.sumAP + AP; 
						a.sumPAt5 = a.sumPAt5 + numOfRAsInTop5/5.0;
						a.sumRAt5 = a.sumRAt5 + (double)numOfRAsInTop5/as.realAssigneesAndTheirRanksForThisAssignment.size();
						a.sumPAt10 = a.sumPAt10 + numOfRAsInTop10/10.0;
						a.sumRAt10 = a.sumRAt10 + (double)numOfRAsInTop10/as.realAssigneesAndTheirRanksForThisAssignment.size();
					}
					totalNOA = totalNOA + a.n; //: total number of assignments.
					projectsAndTheirAssignmentStatSummaries.put(projectId, a); 

					//Adding up to the sum values to be considered later for ALL_PROJECTS (will be divided by n later):
						//In fact, these are not the accuracies, etc., but the sum values. 
					aST_overal.n = aST_overal.n + a.n;
					aST_overal.sumTop1 = aST_overal.sumTop1 + a.sumTop1;
					aST_overal.sumTop5 = aST_overal.sumTop5 + a.sumTop5;
					aST_overal.sumTop10 = aST_overal.sumTop10 + a.sumTop10;
					aST_overal.sumRR = aST_overal.sumRR + a.sumRR;
					aST_overal.sumAP = aST_overal.sumAP + a.sumAP;
					aST_overal.sumPAt1 = aST_overal.sumPAt1 + a.sumPAt1;
					aST_overal.sumRAt1 = aST_overal.sumRAt1 + a.sumRAt1;
					aST_overal.sumPAt5 = aST_overal.sumPAt5 + a.sumPAt5;
					aST_overal.sumRAt5 = aST_overal.sumRAt5 + a.sumRAt5;
					aST_overal.sumPAt10 = aST_overal.sumPAt10 + a.sumPAt10;
					aST_overal.sumRAt10 = aST_overal.sumRAt10 + a.sumRAt10;
				}
				else //: means that there is no assignment in this projec. So just write an empty line.
					writer1.append(owner_repo + TAB + "-" + TAB + "-" + TAB + "-"
							+ TAB + "-" + TAB + "-" + TAB + "-" + "\n");
			}
			writer1.flush();
			writer1.close();
			
			//Overal stats:
			MyUtils.println(writeMessageStep+"-2- Overal stats (in 5 files in the main output folder) ...", indentationLevel+1);
			String outputFileName2_overalStat = outputPath+"\\"+overalSummariesOutputTSVFileName+" - "+detailedSummaryOutputFileNameSuffix+".tsv";
			File file2 = new File(outputFileName2_overalStat);
			boolean needToWriteHeader2 = true;
			if (file2.exists())
				needToWriteHeader2 = false;
			//Writer for overall stats (one separate file for each assignment type):
			FileWriter writer2 = new FileWriter(outputFileName2_overalStat, true);
			//Title line(s):
			String overalTitle;
			if (needToWriteHeader2){
				//First line of the two line title:
				overalTitle = " " + TAB + " " + TAB + "project:" + TAB + "ALL" + TAB + "ALL" + TAB + "#ofBugs:" + TAB + totalNOA 
						+ TAB + "#ofCommunityMembers:" + TAB + min+" - "+max + TAB + "" + TAB + "" + TAB + "" + TAB + "" + TAB + "" + TAB + "";
				for (String owner_repo: projectNamesAndTheirIds.keySet()){
					String projectId = projectNamesAndTheirIds.get(owner_repo);
					if (projectsAndTheirAssignmentStats.containsKey(projectId)){
						int nOA = projectsAndTheirAssignmentStats.get(projectId).size(); //:number of assignments.
						int nOCM = projectsAndTheirCommunities.get(projectId).size(); //: number of community members.
						overalTitle = overalTitle + "project:"+ TAB + owner_repo + TAB + projectId + TAB + "#ofAssignments:" + TAB + nOA + TAB + "#ofCommunityMembers:" 
								+ TAB + nOCM + TAB + "" + TAB + "" + TAB + "" + TAB + "" + TAB + "";
					}
				}				
				overalTitle = overalTitle + "\n";
				//Second line of the two line title:
				overalTitle = overalTitle + "Experiment title" + TAB + "TIME" + TAB + "MRR" + TAB + "MAP" + TAB + "Top 1" + TAB + "Top 5" + TAB + "Top 10" 
						+ TAB + "p@1" + TAB + "r@1" + TAB + "p@5" + TAB + "r@5" + TAB + "p@10" + TAB + "r@10" + TAB + "Any comments for the experiment";
				for (String owner_repo: projectNamesAndTheirIds.keySet()){
					String projectId = projectNamesAndTheirIds.get(owner_repo);
					if (projectsAndTheirAssignmentStats.containsKey(projectId)){
						overalTitle = overalTitle + TAB + "MRR" + TAB + "MAP" + TAB + "Top 1" + TAB + "Top 5" + TAB + "Top 10" 
								+ TAB + "p@1" + TAB + "r@1" + TAB + "p@5" + TAB + "r@5" + TAB + "p@10" + TAB + "r@10";
					}
				}				
				writer2.append(overalTitle + "\n");
			}
			//Adding one body line:
			overalTitle = detailedAssignmentResultsSubfolderName 
					+ TAB + Constants.floatFormatter.format(totalRunningTime)+"Sec"
					+ TAB + Constants.floatPercentageFormatter.format((double)aST_overal.sumRR/aST_overal.n) 
					+ TAB + Constants.floatPercentageFormatter.format((double)100*aST_overal.sumAP/aST_overal.n) 
					+ TAB + Constants.floatPercentageFormatter.format((double)100*aST_overal.sumTop1/aST_overal.n) 
					+ TAB + Constants.floatPercentageFormatter.format((double)100*aST_overal.sumTop5/aST_overal.n) 
					+ TAB + Constants.floatPercentageFormatter.format((double)100*aST_overal.sumTop10/aST_overal.n)
					+ TAB + Constants.floatPercentageFormatter.format((double)100*aST_overal.sumPAt1/aST_overal.n) 
					+ TAB + Constants.floatPercentageFormatter.format((double)100*aST_overal.sumRAt1/aST_overal.n) 
					+ TAB + Constants.floatPercentageFormatter.format((double)100*aST_overal.sumPAt5/aST_overal.n) 
					+ TAB + Constants.floatPercentageFormatter.format((double)100*aST_overal.sumRAt5/aST_overal.n) 
					+ TAB + Constants.floatPercentageFormatter.format((double)100*aST_overal.sumPAt10/aST_overal.n) 
					+ TAB + Constants.floatPercentageFormatter.format((double)100*aST_overal.sumRAt10/aST_overal.n)
					+ TAB + experimentDetails;
			for (String owner_repo: projectNamesAndTheirIds.keySet()){
				String projectId = projectNamesAndTheirIds.get(owner_repo);
				if (projectsAndTheirAssignmentStats.containsKey(projectId)){
					AssignmentStatSummary a = projectsAndTheirAssignmentStatSummaries.get(projectId);
					overalTitle = overalTitle 
							+ TAB + Constants.floatPercentageFormatter.format((double)a.sumRR/a.n) 
							+ TAB + Constants.floatPercentageFormatter.format((double)100*a.sumAP/a.n) 
							+ TAB + Constants.floatPercentageFormatter.format((double)100*a.sumTop1/a.n) 
							+ TAB + Constants.floatPercentageFormatter.format((double)100*a.sumTop5/a.n) 
							+ TAB + Constants.floatPercentageFormatter.format((double)100*a.sumTop10/a.n) 
							+ TAB + Constants.floatPercentageFormatter.format((double)100*a.sumPAt1/a.n) 
							+ TAB + Constants.floatPercentageFormatter.format((double)100*a.sumRAt1/a.n) 
							+ TAB + Constants.floatPercentageFormatter.format((double)100*a.sumPAt5/a.n)
							+ TAB + Constants.floatPercentageFormatter.format((double)100*a.sumRAt5/a.n) 
							+ TAB + Constants.floatPercentageFormatter.format((double)100*a.sumPAt10/a.n) 
							+ TAB + Constants.floatPercentageFormatter.format((double)100*a.sumRAt10/a.n);
				}
				else
					overalTitle = overalTitle 
							+ TAB + "-" 
							+ TAB + "-" 
							+ TAB + "-" 
							+ TAB + "-" 
							+ TAB + "-" 
							+ TAB + "-" 
							+ TAB + "-" 
							+ TAB + "-" 
							+ TAB + "-" 
							+ TAB + "-" 
							+ TAB + "-";
			}
			writer2.append(overalTitle + TAB + "\n");
			writer2.flush();			writer2.close();

			//Writer for overall stats (for all assignment types in a single file):
			MyUtils.println(writeMessageStep+"-2- Summary overal stats (in one file in the main output folder) ...", indentationLevel+1);
			String outputFileName3_overalStat = outputPath+"\\"+overalSummariesOutputTSVFileName+" - ALL_ASSIGNED_TYPES.tsv";
			File file3 = new File(outputFileName3_overalStat);
			boolean needToWriteHeader3 = true;
			if (file3.exists())
				needToWriteHeader3 = false;
			//Writer for overall stats (one separate file for each assignment type):
			FileWriter writer3 = new FileWriter(outputFileName3_overalStat, true);
			//Title line(s):
			if (needToWriteHeader3){//
				//First line of the two line title:
				overalTitle = " " + TAB + " " + TAB + "project:" + TAB + "ALL" + TAB + "ALL" + TAB + "#ofBugs:" + TAB + totalNOA 
						+ TAB + "#ofCommunityMembers:" + TAB + min+" - "+max + TAB + "" + TAB + "" + TAB + "" + TAB + "" + TAB + "" + TAB + "" + TAB + "";
				for (String owner_repo: projectNamesAndTheirIds.keySet()){
					String projectId = projectNamesAndTheirIds.get(owner_repo);
					if (projectsAndTheirAssignmentStats.containsKey(projectId)){
						int nOA = projectsAndTheirAssignmentStats.get(projectId).size(); //:number of assignments.
						int nOCM = projectsAndTheirCommunities.get(projectId).size(); //: number of community members.
						overalTitle = overalTitle + "project:"+ TAB + owner_repo + TAB + projectId + TAB + "#ofAssignments:" + TAB + nOA + TAB + "#ofCommunityMembers:" 
								+ TAB + nOCM + TAB + "" + TAB + "" + TAB + "" + TAB + "" + TAB + "";
					}
					else{
						MyUtils.println("Error! The first time you are running the program you should select \"isMainRun\"=true. Please delete everything in the output folder and re-run the code with that option to create the titles. Then you can set that option to 'false' and run the code again.", indentationLevel+1);
						fMR.errors++;
					}
				}			
				overalTitle = overalTitle + "\n";
				//Second line of the two line title:
				overalTitle = overalTitle + "Experiment title" + TAB + "TIME" + TAB + "Assigned bug type" + TAB + "MRR" + TAB + "MAP" + TAB + "Top 1" + TAB + "Top 5" + TAB + "Top 10" 
						+ TAB + "p@1" + TAB + "r@1" + TAB + "p@5" + TAB + "r@5" + TAB + "p@10" + TAB + "r@10" + TAB + "Any comments for the experiment";
				int numberOfProjects = projectNamesAndTheirIds.size();
				for (int i=0; i<numberOfProjects; i++)
					overalTitle = overalTitle + TAB + "MRR" + TAB + "MAP" + TAB + "Top 1" + TAB + "Top 5" + TAB + "Top 10" 
							+ TAB + "p@1" + TAB + "r@1" + TAB + "p@5" + TAB + "r@5" + TAB + "p@10" + TAB + "r@10";
				
				writer3.append(overalTitle + "\n");
			}
			//Adding one body line:
			String aLineOfBody = detailedAssignmentResultsSubfolderName 
					+ TAB + Constants.floatFormatter.format(totalRunningTime)+"Sec"
					+ TAB + detailedSummaryOutputFileNameSuffix
					+ TAB + Constants.floatPercentageFormatter.format((double)aST_overal.sumRR/aST_overal.n) 
					+ TAB + Constants.floatPercentageFormatter.format((double)100*aST_overal.sumAP/aST_overal.n) 
					+ TAB + Constants.floatPercentageFormatter.format((double)100*aST_overal.sumTop1/aST_overal.n) 
					+ TAB + Constants.floatPercentageFormatter.format((double)100*aST_overal.sumTop5/aST_overal.n) 
					+ TAB + Constants.floatPercentageFormatter.format((double)100*aST_overal.sumTop10/aST_overal.n)
					+ TAB + Constants.floatPercentageFormatter.format((double)100*aST_overal.sumPAt1/aST_overal.n) 
					+ TAB + Constants.floatPercentageFormatter.format((double)100*aST_overal.sumRAt1/aST_overal.n) 
					+ TAB + Constants.floatPercentageFormatter.format((double)100*aST_overal.sumPAt5/aST_overal.n) 
					+ TAB + Constants.floatPercentageFormatter.format((double)100*aST_overal.sumRAt5/aST_overal.n) 
					+ TAB + Constants.floatPercentageFormatter.format((double)100*aST_overal.sumPAt10/aST_overal.n) 
					+ TAB + Constants.floatPercentageFormatter.format((double)100*aST_overal.sumRAt10/aST_overal.n)
					+ TAB + experimentDetails;
			for (String owner_repo: projectNamesAndTheirIds.keySet()){
				String projectId = projectNamesAndTheirIds.get(owner_repo);
				if (projectsAndTheirAssignmentStats.containsKey(projectId)){
					AssignmentStatSummary a = projectsAndTheirAssignmentStatSummaries.get(projectId);
					aLineOfBody = aLineOfBody 
						+ TAB + Constants.floatPercentageFormatter.format((double)a.sumRR/a.n) 
						+ TAB + Constants.floatPercentageFormatter.format((double)100*a.sumAP/a.n) 
						+ TAB + Constants.floatPercentageFormatter.format((double)100*a.sumTop1/a.n) 
						+ TAB + Constants.floatPercentageFormatter.format((double)100*a.sumTop5/a.n) 
						+ TAB + Constants.floatPercentageFormatter.format((double)100*a.sumTop10/a.n) 
						+ TAB + Constants.floatPercentageFormatter.format((double)100*a.sumPAt1/a.n) 
						+ TAB + Constants.floatPercentageFormatter.format((double)100*a.sumRAt1/a.n) 
						+ TAB + Constants.floatPercentageFormatter.format((double)100*a.sumPAt5/a.n)
						+ TAB + Constants.floatPercentageFormatter.format((double)100*a.sumRAt5/a.n) 
						+ TAB + Constants.floatPercentageFormatter.format((double)100*a.sumPAt10/a.n) 
						+ TAB + Constants.floatPercentageFormatter.format((double)100*a.sumRAt10/a.n);
				}
				else{
					aLineOfBody = aLineOfBody 
							+ TAB + "" 
							+ TAB + "" 
							+ TAB + "" 
							+ TAB + "" 
							+ TAB + "" 
							+ TAB + "" 
							+ TAB + "" 
							+ TAB + ""
							+ TAB + "" 
							+ TAB + "" 
							+ TAB + "";
				}
			}
			writer3.append(aLineOfBody + TAB + "\n");			
			writer3.flush();	
			writer3.close();
			fMR.doneSuccessfully = 1;
		} catch (IOException e) {
			e.printStackTrace();
			fMR.errors = 1;
		}
		MyUtils.println("Finished.", indentationLevel+1);
		if (wrapOutputInLines)
			MyUtils.println("-----------------------------------", indentationLevel);
	}
	//------------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------------
	public static void addToIndex(String evidenceText, int evidenceType, int seqNum, int[] virtualSeqNum, //seqNum is the sequence number of the evidence. If it is an assignment, the row number in the assignments file (first assignment in the project is 1 and the next one increment by one). If it is not an assignment (e.g., it is a commit), the seqNum of the last assignment before that evidence will be considered.
			int originalNumberOfWordsInTheText, Graph graph, String projectId, String login, String date, 
			HashMap<String, HashMap<String, HashMap<String, HashMap<Integer, ArrayList<Evidence>>>>> projectId_Login_Tags_TypesAndTheirEvidence, 
			BTOption3_TF option3_TF, BTOption7_whenToCountTextLength option7_whenToCountTextLength,  
			FileManipulationResult fMR){
		String[] words = evidenceText.split(" ");
		for (int j=0; j<words.length; j++){
			if (!words[j].equals("") && graph.hasNode(words[j])){ //: means that if this word is an SO tag.
				//First, start by projectId:
				HashMap<String, HashMap<String, HashMap<Integer, ArrayList<Evidence>>>> login_Tags_TypesAndTheirEvidence;
				if (projectId_Login_Tags_TypesAndTheirEvidence.containsKey(projectId))
					login_Tags_TypesAndTheirEvidence = projectId_Login_Tags_TypesAndTheirEvidence.get(projectId);
				else{
					login_Tags_TypesAndTheirEvidence = new HashMap<String, HashMap<String, HashMap<Integer, ArrayList<Evidence>>>>();
					projectId_Login_Tags_TypesAndTheirEvidence.put(projectId, login_Tags_TypesAndTheirEvidence);
				}
				//Then move on to login:
				HashMap<String, HashMap<Integer, ArrayList<Evidence>>> tags_TypesAndTheirEvidence;
				if (login_Tags_TypesAndTheirEvidence.containsKey(login))
					tags_TypesAndTheirEvidence = login_Tags_TypesAndTheirEvidence.get(login);
				else{
					tags_TypesAndTheirEvidence = new HashMap<String, HashMap<Integer, ArrayList<Evidence>>>();
					login_Tags_TypesAndTheirEvidence.put(login, tags_TypesAndTheirEvidence);
				}
				//After that, check the tags:
				HashMap<Integer, ArrayList<Evidence>> typesAndTheirEvidence;
				if (tags_TypesAndTheirEvidence.containsKey(words[j])) //: means that if there is already at least one evidence containing this tag (for this user and this project): 
					typesAndTheirEvidence = tags_TypesAndTheirEvidence.get(words[j]);
				else{
					typesAndTheirEvidence = new HashMap<Integer, ArrayList<Evidence>>();
					tags_TypesAndTheirEvidence.put(words[j], typesAndTheirEvidence);
				}
				//Now, check the specific type evidence:
				ArrayList<Evidence> type_x_Evidence;
				if (typesAndTheirEvidence.containsKey(evidenceType))
					type_x_Evidence = typesAndTheirEvidence.get(evidenceType);
				else{
					type_x_Evidence = new ArrayList<Evidence>();
					typesAndTheirEvidence.put(evidenceType, type_x_Evidence);
				}
				//And hereby create the evidence and insert it in the arrayList type_x_Evidence:
				//We need the frequency of this word (so we need to count it):
				int freq = 1;
				for (int k=j+1; k<words.length; k++)
					if (words[k].equals(words[j])){
						freq++;
						words[k] = "";
					}
				//TODO: Delete this later (no need to save and print max.... It is just for my own knowledge):
				if (freq > maxFreqOfAWordInAnEvidence)
					maxFreqOfAWordInAnEvidence = freq; //just checking!
				//.
				
				Evidence e;
				try {
					int numberOfWordsInTheText;
					switch (option7_whenToCountTextLength){
						case USE_TEXT_LENGTH_BEFORE_REMOVING_NON_SO_TAGS:
							numberOfWordsInTheText = originalNumberOfWordsInTheText;
							break;
						case USE_TEXT_LENGTH_AFTER_REMOVING_NON_SO_TAGS:
						default:
							numberOfWordsInTheText = words.length;
							break;
					}
					e = new Evidence(Constants.dateFormat.parse(date), seqNum, virtualSeqNum, freq, numberOfWordsInTheText, option3_TF);
					type_x_Evidence.add(e);
				} catch (ParseException e1) {
					fMR.errors++;
					e1.printStackTrace();
				}
			}
		}
	}
	//------------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------------
	public static void indexAssignmentEvidence(int assignmentType_index, RUN_TYPE runType, 
			int[] evidenceTypes, int thresholdForNumberOfAssignmentsInAProject, 
			ArrayList<TreeMap<String, ArrayList<String[]>>> projectsAndTheirAssignments__AL_forDifferentAssignmetTypes, int[] assignmentTypesToTriage,
			TreeMap<String, String[]> projects, TreeMap<String, String[]> projectIdBugNumberAndTheirBugInfo, 
			HashMap<String, HashMap<String, HashMap<String, HashMap<Integer, ArrayList<Evidence>>>>> projectId_Login_Tags_TypesAndTheirEvidence, 
			//HashMap<projId, HashMap<login, HashMap<tag, ArrayList<Evidence>>>>
			HashMap<String, HashSet<String>> projectsAndTheirCommunities_HM, 
			Graph graph, FileManipulationResult fMR, 
			BTOption1_whatToAddToAllBugs option1, BTOption2_w option2_w, BTOption3_TF option3_TF, BTOption4_IDF option4_IDF, BTOption5_prioritizePAs option5_prioritizePAs, BTOption6_whatToAddToAllCommits option6_whatToAddToAllCommits, BTOption7_whenToCountTextLength option7_whenToCountTextLength, 
			boolean wrapOutputInLines, int showProgressInterval, int indentationLevel, String writeMessageStep){
		//This method reads the input files that are not empty ("") in parameter projectId_Login_TagsAndTheirEvidence. 
			//The arrayList should be sorted based on date. 
		if (wrapOutputInLines)
			MyUtils.println("-----------------------------------", indentationLevel+1);
		MyUtils.println(writeMessageStep + "- Reading and indexing assignment evidence of type \"" + Constants.ASSIGNED_BUGS_TYPES__SHORT_DESCRIPTIONS[assignmentType_index] + "\" (in a project by project basis):", indentationLevel);
		MyUtils.println("Started ...", indentationLevel+1);

		try {
			TreeMap<String, ArrayList<String[]>> projectsAndTheirAssignments = projectsAndTheirAssignments__AL_forDifferentAssignmetTypes.get(assignmentType_index);
			for (Map.Entry<String, ArrayList<String[]>> entry : projectsAndTheirAssignments.entrySet()){
				ArrayList<String[]> assignmentsOfOneProject = entry.getValue();
				String projectId = entry.getKey();
				Project project = new Project(projects, projectId, indentationLevel+2, fMR);
				ArrayList<String[]> assignmentsOfThisProject = projectsAndTheirAssignments.get(projectId);
//				if (projectId.equals("460078"))
//					System.out.println("aa");
//				else
//					System.out.println("bb");
				if (AlgPrep.shouldRunTheExperimentOnProject(runType, project, assignmentsOfThisProject, thresholdForNumberOfAssignmentsInAProject)){
					int i = 0;
					for (i=0; i<assignmentsOfOneProject.size(); i++){
						String[] fields = assignmentsOfOneProject.get(i);
						String bugNumber = fields[0];
						String date = fields[1];
						String login = fields[2];//this is the assignee.
						Bug bug = new Bug(projectId, bugNumber, projectIdBugNumberAndTheirBugInfo, fMR);
						//						String labels = bugInfo[2];
						//						String title = bugInfo[3];
						//						String body = bugInfo[4];
						String text;
						int[] originalNumberOfWordsInText_array = new int[1];
						text = getBugText(project, bug, originalNumberOfWordsInText_array, option1);
						int originalNumberOfWordsInText = originalNumberOfWordsInText_array[0];

						AtomicBoolean thisEvidenceShouldBeConsidered = new AtomicBoolean(true);//: if <the experiment is multi-project> and <the current evidence is from a sub-project or rails or angular> and <the user (login) related to this evidence is not in the community of the super project (rails or angular)> then this variable should be set to false:
						AtomicBoolean isMultiProjEvidence = new AtomicBoolean(false); 
						String projectId_or_ParentProjectId = get__ProjectId_or_parentProjectId(runType, project, projectsAndTheirCommunities_HM, login, thisEvidenceShouldBeConsidered, isMultiProjEvidence);
						if (!isMultiProjEvidence.get() || evidenceTypes[6]==Algorithm.YES) //if it is multiProject, then check if we need to consider that type (MultiProject-->assignedBug).
							if (thisEvidenceShouldBeConsidered.get()){
								if (text.length() > 2){ 
									//This is an assignment event, so set the virtualSeqNum (the sequence number of non-assignmentt evidence) to AN_EXTREMELY_NEGATIVE_INT:
									int[] virtualSeqNum = new int[Constants.NUMBER_OF_ASSIGNEE_TYPES];
									for (int j=0; j<Constants.NUMBER_OF_ASSIGNEE_TYPES; j++)
										virtualSeqNum[j] = Constants.SEQ_NUM____NO_NEED_TO_TRIAGE_THIS_TYPE___OR___THIS_IS_NOT__NON_B_A_EVIDENCE;
									int evType = Constants.EVIDENCE_TYPE[assignmentType_index];
									if (isMultiProjEvidence.get()){
										evType = evType + 20;
										determineVirtualSeqNum(assignmentTypesToTriage, projectsAndTheirAssignments__AL_forDifferentAssignmetTypes, 
												projectId_or_ParentProjectId, virtualSeqNum, date);
									}
									addToIndex(text, evType, i+1, virtualSeqNum, originalNumberOfWordsInText, graph, projectId_or_ParentProjectId, login, date, projectId_Login_Tags_TypesAndTheirEvidence, option3_TF, option7_whenToCountTextLength, fMR);
								}
								else{
									//						fMR.errors++;
									//						System.out.println("Error! Assignment evidence length is zero!");
								}
							}
						if (i+1 % showProgressInterval == 0)
							MyUtils.println(Constants.integerFormatter.format(i), indentationLevel+2);
					}
					if (i > 0)
						MyUtils.println(project.owner_repo + " (projectId: " + projectId + "): \texpertise of " + i + " bug assignments indexed.", indentationLevel+1);
					else
						MyUtils.println(project.owner_repo + " (projectId: " + projectId + "): \tWarning: Nothing indexed! No bug assignments to index their expertise!", indentationLevel+1);
				}
			}
		} catch (Exception e) {
			fMR.errors++;
			System.out.println("ERROR3!");
			e.printStackTrace();
		} 

		MyUtils.println("Finished.", indentationLevel+1);
		if (wrapOutputInLines)
			MyUtils.println("-----------------------------------", indentationLevel);
	}
	//------------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------------
	public static void determineVirtualSeqNum(int[] assignmentTypesToTriage, ArrayList<TreeMap<String, ArrayList<String[]>>> projectsAndTheirAssignments__AL_forDifferentAssignmetTypes, 
			String projectId, int[] virtualSeqNum, String date){
		for (int j=ASSIGNMENT_TYPE_TO_TRIAGE.T1_AUTHOR.ordinal(); j<=ASSIGNMENT_TYPE_TO_TRIAGE.T5_ALL_TYPES.ordinal(); j++){
			if (assignmentTypesToTriage[j] == Algorithm.YES){
				TreeMap<String, ArrayList<String[]>> projectsAndTheirAssignments = projectsAndTheirAssignments__AL_forDifferentAssignmetTypes.get(j);
				ArrayList<String[]> assignments = projectsAndTheirAssignments.get(projectId);
				virtualSeqNum[j] = MyUtils.specialBinarySearch2(assignments, 1/*the date field is index 1*/, date) + 1; //: "Plus one"; because binary search returns index, not sequenceNumber
			}
			else
				virtualSeqNum[j] = Constants.SEQ_NUM____NO_NEED_TO_TRIAGE_THIS_TYPE___OR___THIS_IS_NOT__NON_B_A_EVIDENCE;
		}
	}
	//------------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------------
	public static void readAndIndexNonAssignmentEvidence(String inputPath,  
			String commitsInputFileName, String pRsInputFileName, String bugCommentsInputFileName, String commitCommentsInputFileName, String pRCommentsInputFileName, String SOPostsInputFileName, 
			RUN_TYPE runType, ArrayList<TreeMap<String, ArrayList<String[]>>> projectsAndTheirAssignments__AL_forDifferentAssignmetTypes, int[] assignmentTypesToTriage, 
			HashMap<String, HashSet<String>> projectsAndTheirSharedUsersWithSO, 
			FileManipulationResult fMR,
			int[] evidenceTypes, 
			TreeMap<String, String[]> projects, TreeMap<String, String[]> projectIdBugNumberAndTheirBugInfo, HashMap<String, HashSet<String>> projectsAndTheirCommunities_HM, 
			HashMap<String, HashMap<String, HashMap<String, HashMap<Integer, ArrayList<Evidence>>>>> projectId_Login_Tags_TypesAndTheirEvidence, 
			//HashMap<projId, HashMap<login, HashMap<tag, ArrayList<Evidence>>>>
			Graph graph, 
			BTOption1_whatToAddToAllBugs option1, BTOption2_w option2_w, BTOption3_TF option3_TF, BTOption4_IDF option4_IDF, BTOption5_prioritizePAs option5_prioritizePAs, 
			BTOption6_whatToAddToAllCommits option6_whatToAddToAllCommits, BTOption7_whenToCountTextLength option7_whenToCountTextLength,
			BTOption9_whatToAddToAllPRs option9_whatToAddToAllPRs, BTOption10_whatToAddToAllBCs option10_whatToAddToAllBugComments, 
			BTOption11_whatToAddToAllCCs option11_whatToAddToAllCommitComments, BTOption12_whatToAddToAllPRCs option12_whatToAddToAllPRComments, 
			boolean wrapOutputInLines, int showProgressInterval, int indentationLevel, String writeMessageStep){
		//This method reads the input files that are not empty ("") in parameter projectId_Login_TagsAndTheirEvidence. 
			//The arrayList should be sorted based on date. 
		if (wrapOutputInLines)
			MyUtils.println("-----------------------------------", indentationLevel);
		MyUtils.println(writeMessageStep + "- Reading non-assignment evidence and indexing them:", indentationLevel);
		MyUtils.println("Started ...", indentationLevel+1);

		
		try {
			//Commits:
			if (wrapOutputInLines)
				MyUtils.println("-----------------------------------", indentationLevel+1);
			MyUtils.println(writeMessageStep + "-1- commits: \"" + commitsInputFileName + "\"", indentationLevel+1);
			if (evidenceTypes[Algorithm.INDEX_OF__EVIDENCE_TYPE__COMMIT] == Algorithm.YES || evidenceTypes[Algorithm.INDEX_OF__EVIDENCE_TYPE__MULTI_PROJ__COMMIT] == Algorithm.YES){
				MyUtils.println("Started ...", indentationLevel+2);
				if (!commitsInputFileName.equals("")){
					BufferedReader br;
					br = new BufferedReader(new FileReader(inputPath + "\\" + commitsInputFileName));
					int i=0;
					String s;
					br.readLine(); //header.
					while ((s=br.readLine())!=null){
						String[] fields = s.split("\t");
						if (fields.length == 6){ 
//							String sha = fields[0];
							String projectId = fields[1];
							String committer = fields[2];
							String date = fields[3];
							String commitMessage = fields[4];
							int originalNumberOfWordsInText = Integer.parseInt(fields[5]);
							
							if (!committer.equals(" ")){
								String text = "";
								Project project = new Project(projects, projectId, indentationLevel+2, fMR);
								int[] originalNumberOfWordsInText_array = new int[]{1};
								originalNumberOfWordsInText_array[0] = originalNumberOfWordsInText;
								text = getCommitText(project, commitMessage, originalNumberOfWordsInText_array, option6_whatToAddToAllCommits);
								originalNumberOfWordsInText = originalNumberOfWordsInText_array[0];

								AtomicBoolean thisEvidenceShouldBeConsidered = new AtomicBoolean(true);//: if <the experiment is multi-project> and <the current evidence is from a sub-project or rails or angular> and <the user (login) related to this evidence is not in the community of the super project (rails or angular)> then this variable should be set to false:
								AtomicBoolean isMultiProjEvidence = new AtomicBoolean(false);
								String projectId_or_ParentProjectId = get__ProjectId_or_parentProjectId(runType, project, projectsAndTheirCommunities_HM, committer, thisEvidenceShouldBeConsidered, isMultiProjEvidence);
								if ((!isMultiProjEvidence.get() && evidenceTypes[1]==Algorithm.YES) 
										|| (isMultiProjEvidence.get() && evidenceTypes[7]==Algorithm.YES)){ //if it is multiProject, then check if we need to consider that type (MultiProject-->commit).
									if (thisEvidenceShouldBeConsidered.get()){
										//Determining the commit's virtualSeqNum[] values based on assignments in different projectsAndTheirAssignments in projectsAndTheirAssignments_AL. 
										int[] virtualSeqNum = new int[Constants.NUMBER_OF_ASSIGNEE_TYPES];
										determineVirtualSeqNum(assignmentTypesToTriage, projectsAndTheirAssignments__AL_forDifferentAssignmetTypes, 
												projectId_or_ParentProjectId, virtualSeqNum, date);

										int evType = Constants.EVIDENCE_TYPE_COMMIT;
										if (isMultiProjEvidence.get())
											evType = evType + 20;
										if (text.length() > 2)
											addToIndex(text, evType, Constants.SEQ_NUM____THIS_IS_NOT__B_A_EVIDENCE, virtualSeqNum, 
													originalNumberOfWordsInText, graph, projectId_or_ParentProjectId, committer, date, projectId_Login_Tags_TypesAndTheirEvidence, 
													option3_TF, option7_whenToCountTextLength, fMR);
										else{
											//										fMR.errors++;
											//										System.out.println("Error! Non-assignment evidence length is zero!");
										}
									}
								}
							}
						}
						else
							fMR.errors++;
						i++;
						if (i % showProgressInterval == 0)
							MyUtils.println(Constants.integerFormatter.format(i), indentationLevel+2);
					}
					br.close();
				}
				MyUtils.println("Finished.", indentationLevel+2);
			}
			else
				MyUtils.println("Not needed. Cancelled!", indentationLevel+2);

			
			if (wrapOutputInLines)
				MyUtils.println("-----------------------------------", indentationLevel+1);
			MyUtils.println(writeMessageStep + "-2- PRs: \"" + pRsInputFileName + "\"", indentationLevel+1);
			//PRs: 
			if (evidenceTypes[Algorithm.INDEX_OF__EVIDENCE_TYPE__PR] == Algorithm.YES || evidenceTypes[Algorithm.INDEX_OF__EVIDENCE_TYPE__MULTI_PROJ__PR] == Algorithm.YES){
				MyUtils.println("Started ...", indentationLevel+2);
				if (!pRsInputFileName.equals("")){
					BufferedReader br;
					br = new BufferedReader(new FileReader(inputPath + "\\" + pRsInputFileName));
					int i=0;
					String s;
					br.readLine(); //header.
					while ((s=br.readLine())!=null){
						String[] fields = s.split("\t");
						if (fields.length == 9){ 
							String projectId = fields[0];
							String author = fields[2];
							String date = fields[3];
							String pRText = StringManipulations.concatTwoStringsWithSpace(fields[5], fields[7]);
							int originalNumberOfWordsInText = Integer.parseInt(fields[6]) + Integer.parseInt(fields[8]);
							
							if (!author.equals(" ")){
								String text = "";
								Project project = new Project(projects, projectId, indentationLevel+2, fMR);
								int[] originalNumberOfWordsInText_array = new int[]{1};
								originalNumberOfWordsInText_array[0] = originalNumberOfWordsInText;
								text = getPRText(project, pRText, originalNumberOfWordsInText_array, option9_whatToAddToAllPRs);
								originalNumberOfWordsInText = originalNumberOfWordsInText_array[0];

								AtomicBoolean thisEvidenceShouldBeConsidered = new AtomicBoolean(true);//: if <the experiment is multi-project> and <the current evidence is from a sub-project or rails or angular> and <the user (login) related to this evidence is not in the community of the super project (rails or angular)> then this variable should be set to false:
								AtomicBoolean isMultiProjEvidence = new AtomicBoolean(false);
								String projectId_or_ParentProjectId = get__ProjectId_or_parentProjectId(runType, project, projectsAndTheirCommunities_HM, author, thisEvidenceShouldBeConsidered, isMultiProjEvidence);
								if ((!isMultiProjEvidence.get() && evidenceTypes[2]==Algorithm.YES) 
										|| (isMultiProjEvidence.get() && evidenceTypes[8]==Algorithm.YES)){ //if it is multiProject, then check if we need to consider that type (MultiProject-->pR).
									if (thisEvidenceShouldBeConsidered.get()){
										//Determining the pR's virtualSeqNum[] values based on assignments in different projectsAndTheirAssignments in projectsAndTheirAssignments_AL. 
										int[] virtualSeqNum = new int[Constants.NUMBER_OF_ASSIGNEE_TYPES];
										determineVirtualSeqNum(assignmentTypesToTriage, projectsAndTheirAssignments__AL_forDifferentAssignmetTypes, 
												projectId_or_ParentProjectId, virtualSeqNum, date);

										int evType = Constants.EVIDENCE_TYPE_PR;
										if (isMultiProjEvidence.get())
											evType = evType + 20;
										if (text.length() > 2)
											addToIndex(text, evType, Constants.SEQ_NUM____THIS_IS_NOT__B_A_EVIDENCE, virtualSeqNum, 
													originalNumberOfWordsInText, graph, projectId_or_ParentProjectId, author, date, projectId_Login_Tags_TypesAndTheirEvidence, 
													option3_TF, option7_whenToCountTextLength, fMR);
										else{
											//									fMR.errors++;
											//									System.out.println("Error! Non-assignment evidence length is zero!");
										}
									}
								}
							}
						}
						else
							fMR.errors++;
						i++;
						if (i % showProgressInterval == 0)
							MyUtils.println(Constants.integerFormatter.format(i), indentationLevel+2);
					}
					br.close();
				}
				MyUtils.println("Finished.", indentationLevel+2);
			}
			else
				MyUtils.println("Not needed. Cancelled!", indentationLevel+2);

			//bugComments:
			if (wrapOutputInLines)
				MyUtils.println("-----------------------------------", indentationLevel+1);
			MyUtils.println(writeMessageStep + "-3- bugComments: \"" + bugCommentsInputFileName + "\"", indentationLevel+1);
			if (evidenceTypes[Algorithm.INDEX_OF__EVIDENCE_TYPE__BUG_COMMENT] == Algorithm.YES || evidenceTypes[Algorithm.INDEX_OF__EVIDENCE_TYPE__MULTI_PROJ__BUG_COMMENT] == Algorithm.YES){
				MyUtils.println("Started ...", indentationLevel+2);
				if (!bugCommentsInputFileName.equals("")){
					BufferedReader br;
					br = new BufferedReader(new FileReader(inputPath + "\\" + bugCommentsInputFileName));
					int i=0;
					String s;
					br.readLine(); //header.
					while ((s=br.readLine())!=null){
						String[] fields = s.split("\t");
						if (fields.length == 6){ 
							String projectId = fields[0];
							String date = fields[2];
							String commenter = fields[3];
							String bCText = fields[4];
							int originalNumberOfWordsInText = Integer.parseInt(fields[5]);
							
							if (!commenter.equals(" ")){
								String text = "";
								Project project = new Project(projects, projectId, indentationLevel+2, fMR);
								int[] originalNumberOfWordsInText_array = new int[]{1};
								originalNumberOfWordsInText_array[0] = originalNumberOfWordsInText;
								text = getCCText(project, bCText, originalNumberOfWordsInText_array, option11_whatToAddToAllCommitComments);
								originalNumberOfWordsInText = originalNumberOfWordsInText_array[0];
								AtomicBoolean thisEvidenceShouldBeConsidered = new AtomicBoolean(true);//: if <the experiment is multi-project> and <the current evidence is from a sub-project or rails or angular> and <the user (login) related to this evidence is not in the community of the super project (rails or angular)> then this variable should be set to false:
								AtomicBoolean isMultiProjEvidence = new AtomicBoolean(false);
								String projectId_or_ParentProjectId = get__ProjectId_or_parentProjectId(runType, project, projectsAndTheirCommunities_HM, commenter, thisEvidenceShouldBeConsidered, isMultiProjEvidence);
								if ((!isMultiProjEvidence.get() && evidenceTypes[3]==Algorithm.YES) 
										|| (isMultiProjEvidence.get() && evidenceTypes[9]==Algorithm.YES)){ //if it is multiProject, then check if we need to consider that type (MultiProject-->bugComment).
									if (thisEvidenceShouldBeConsidered.get()){
										//Determining the bugComment's virtualSeqNum[] values based on assignments in different projectsAndTheirAssignments in projectsAndTheirAssignments_AL. 
										int[] virtualSeqNum = new int[Constants.NUMBER_OF_ASSIGNEE_TYPES];
										determineVirtualSeqNum(assignmentTypesToTriage, projectsAndTheirAssignments__AL_forDifferentAssignmetTypes, 
												projectId_or_ParentProjectId, virtualSeqNum, date);

										int evType = Constants.EVIDENCE_TYPE_BUG_COMMENT;
										if (isMultiProjEvidence.get())
											evType = evType + 20;
										if (text.length() > 2)
											addToIndex(text, evType, Constants.SEQ_NUM____THIS_IS_NOT__B_A_EVIDENCE, virtualSeqNum, 
													originalNumberOfWordsInText, graph, projectId_or_ParentProjectId, commenter, date, projectId_Login_Tags_TypesAndTheirEvidence, 
													option3_TF, option7_whenToCountTextLength, fMR);
										else{
											//									fMR.errors++;
											//									System.out.println("Error! Non-assignment evidence length is zero!");
										}
									}
								}
							}
						}
						else
							fMR.errors++;
						i++;
						if (i % showProgressInterval == 0)
							MyUtils.println(Constants.integerFormatter.format(i), indentationLevel+2);
					}
					br.close();
				}
				MyUtils.println("Finished.", indentationLevel+2);
			}
			else
				MyUtils.println("Not needed. Cancelled!", indentationLevel+2);
		
			//commitComments:
			if (wrapOutputInLines)
				MyUtils.println("-----------------------------------", indentationLevel+1);
			MyUtils.println(writeMessageStep + "-4- commitComments: \"" + commitCommentsInputFileName + "\"", indentationLevel+1);
			if (evidenceTypes[Algorithm.INDEX_OF__EVIDENCE_TYPE__COMMIT_COMMENT] == Algorithm.YES || evidenceTypes[Algorithm.INDEX_OF__EVIDENCE_TYPE__MULTI_PROJ__COMMIT_COMMENT] == Algorithm.YES){
				MyUtils.println("Started ...", indentationLevel+2);
				if (!commitCommentsInputFileName.equals("")){
					BufferedReader br;
					br = new BufferedReader(new FileReader(inputPath + "\\" + commitCommentsInputFileName));
					int i=0;
					String s;
					br.readLine(); //header.
					while ((s=br.readLine())!=null){
						String[] fields = s.split("\t");
						if (fields.length == 6){ 
							String projectId = fields[0];
							String date = fields[1];
							String commenter = fields[2];
							String cCText = fields[4];
							int originalNumberOfWordsInText = Integer.parseInt(fields[5]);
							
							if (!commenter.equals(" ")){
								String text = "";
								Project project = new Project(projects, projectId, indentationLevel+2, fMR);
								int[] originalNumberOfWordsInText_array = new int[]{1};
								originalNumberOfWordsInText_array[0] = originalNumberOfWordsInText;
								text = getCCText(project, cCText, originalNumberOfWordsInText_array, option11_whatToAddToAllCommitComments);
								originalNumberOfWordsInText = originalNumberOfWordsInText_array[0];

								AtomicBoolean thisEvidenceShouldBeConsidered = new AtomicBoolean(true);//: if <the experiment is multi-project> and <the current evidence is from a sub-project or rails or angular> and <the user (login) related to this evidence is not in the community of the super project (rails or angular)> then this variable should be set to false:
								AtomicBoolean isMultiProjEvidence = new AtomicBoolean(false);
								String projectId_or_ParentProjectId = get__ProjectId_or_parentProjectId(runType, project, projectsAndTheirCommunities_HM, commenter, thisEvidenceShouldBeConsidered, isMultiProjEvidence);
								if ((!isMultiProjEvidence.get() && evidenceTypes[4]==Algorithm.YES) 
										|| (isMultiProjEvidence.get() && evidenceTypes[10]==Algorithm.YES)){ //if it is multiProject, then check if we need to consider that type (MultiProject-->commitComment).
									if (thisEvidenceShouldBeConsidered.get()){
										//Determining the commitComment's virtualSeqNum[] values based on assignments in different projectsAndTheirAssignments in projectsAndTheirAssignments_AL. 
										int[] virtualSeqNum = new int[Constants.NUMBER_OF_ASSIGNEE_TYPES];
										determineVirtualSeqNum(assignmentTypesToTriage, projectsAndTheirAssignments__AL_forDifferentAssignmetTypes, 
												projectId_or_ParentProjectId, virtualSeqNum, date);

										int evType = Constants.EVIDENCE_TYPE_COMMIT_COMMENT;
										if (commenter.equals("igorminar") && date.endsWith("3:52:15.000Z"))
											System.out.println("commitComment ...");
										if (isMultiProjEvidence.get())
											evType = evType + 20;
										if (text.length() > 2)
											addToIndex(text, evType, Constants.SEQ_NUM____THIS_IS_NOT__B_A_EVIDENCE, virtualSeqNum, 
													originalNumberOfWordsInText, graph, projectId_or_ParentProjectId, commenter, date, projectId_Login_Tags_TypesAndTheirEvidence, 
													option3_TF, option7_whenToCountTextLength, fMR);
										else{
											//									fMR.errors++;
											//									System.out.println("Error! Non-assignment evidence length is zero!");
										}
									}
								}
							}
						}
						else
							fMR.errors++;
						i++;
						if (i % showProgressInterval == 0)
							MyUtils.println(Constants.integerFormatter.format(i), indentationLevel+2);
					}
					br.close();
				}
				MyUtils.println("Finished.", indentationLevel+2);
			}
			else
				MyUtils.println("Not needed. Cancelled!", indentationLevel+2);
		
			//pullRequest Comments:
			if (wrapOutputInLines)
				MyUtils.println("-----------------------------------", indentationLevel+1);
			MyUtils.println(writeMessageStep + "-5- pRComments: \"" + pRCommentsInputFileName + "\"", indentationLevel+1);
			if (evidenceTypes[Algorithm.INDEX_OF__EVIDENCE_TYPE__PR_COMMENT] == Algorithm.YES || evidenceTypes[Algorithm.INDEX_OF__EVIDENCE_TYPE__MULTI_PROJ__PR_COMMENT] == Algorithm.YES){
				MyUtils.println("Started ...", indentationLevel+2);
				if (!pRCommentsInputFileName.equals("")){
					BufferedReader br;
					br = new BufferedReader(new FileReader(inputPath + "\\" + pRCommentsInputFileName));
					int i=0;
					String s;
					br.readLine(); //header.
					while ((s=br.readLine())!=null){
						String[] fields = s.split("\t");
						if (fields.length == 6){ 
							String projectId = fields[0];
							String date = fields[2];
							String commenter = fields[3];
							String cCText = fields[4];
							int originalNumberOfWordsInText = Integer.parseInt(fields[5]);
							
							if (!commenter.equals(" ")){
								String text = "";
								Project project = new Project(projects, projectId, indentationLevel+2, fMR);
								int[] originalNumberOfWordsInText_array = new int[]{1};
								originalNumberOfWordsInText_array[0] = originalNumberOfWordsInText;
								text = getPRCText(project, cCText, originalNumberOfWordsInText_array, option12_whatToAddToAllPRComments);
								originalNumberOfWordsInText = originalNumberOfWordsInText_array[0];

								AtomicBoolean thisEvidenceShouldBeConsidered = new AtomicBoolean(true);//: if <the experiment is multi-project> and <the current evidence is from a sub-project or rails or angular> and <the user (login) related to this evidence is not in the community of the super project (rails or angular)> then this variable should be set to false:
								AtomicBoolean isMultiProjEvidence = new AtomicBoolean(false);
								String projectId_or_ParentProjectId = get__ProjectId_or_parentProjectId(runType, project, projectsAndTheirCommunities_HM, commenter, thisEvidenceShouldBeConsidered, isMultiProjEvidence);
								if ((!isMultiProjEvidence.get() && evidenceTypes[5]==Algorithm.YES) 
										|| (isMultiProjEvidence.get() && evidenceTypes[11]==Algorithm.YES)){ //if it is multiProject, then check if we need to consider that type (MultiProject-->pR comment).
									if (thisEvidenceShouldBeConsidered.get()){
										//Determining the pullRequest Comment's virtualSeqNum[] values based on assignments in different projectsAndTheirAssignments in projectsAndTheirAssignments_AL. 
										int[] virtualSeqNum = new int[Constants.NUMBER_OF_ASSIGNEE_TYPES];
										determineVirtualSeqNum(assignmentTypesToTriage, projectsAndTheirAssignments__AL_forDifferentAssignmetTypes, 
												projectId_or_ParentProjectId, virtualSeqNum, date);

										int evType = Constants.EVIDENCE_TYPE_PR_COMMENT;
										if (isMultiProjEvidence.get())
											evType = evType + 20;
										if (text.length() > 2)
											addToIndex(text, evType, Constants.SEQ_NUM____THIS_IS_NOT__B_A_EVIDENCE, virtualSeqNum, 
													originalNumberOfWordsInText, graph, projectId_or_ParentProjectId, commenter, date, projectId_Login_Tags_TypesAndTheirEvidence, 
													option3_TF, option7_whenToCountTextLength, fMR);
										else{
											//									fMR.errors++;
											//									System.out.println("Error! Non-assignment evidence length is zero!");
										}
									}
								}
							}
						}
						else
							fMR.errors++;
						i++;
						if (i % showProgressInterval == 0)
							MyUtils.println(Constants.integerFormatter.format(i), indentationLevel+2);
					}
					br.close();
				}
				MyUtils.println("Finished.", indentationLevel+2);
			}
			else
				MyUtils.println("Not needed. Cancelled!", indentationLevel+2);

			//SO q/a:
			if (wrapOutputInLines)
				MyUtils.println("-----------------------------------", indentationLevel+1);
			MyUtils.println(writeMessageStep + "-6- SO Q/A: \"" + pRCommentsInputFileName + "\"", indentationLevel+1);
			if (evidenceTypes[Algorithm.INDEX_OF__EVIDENCE_TYPE__SOA] == Algorithm.YES || evidenceTypes[Algorithm.INDEX_OF__EVIDENCE_TYPE__SOQ] == Algorithm.YES){
				MyUtils.println("Started ...", indentationLevel+2);
				if (!SOPostsInputFileName.equals("")){
					BufferedReader br;
					br = new BufferedReader(new FileReader(inputPath + "\\" + SOPostsInputFileName));
					int i=0;
					String s;
					br.readLine(); //header.
					while ((s=br.readLine())!=null){
						String[] fields = s.split("\t");
						if (fields.length == 9){ 
//							String projectId = fields[0];
							String date = fields[6]+"Z";
							String poster = fields[2];
							String SOTagsOfThePost = fields[5];
							String postTypeId = fields[1];
							int originalNumberOfWordsInText = StringUtils.countMatches(SOTagsOfThePost, ";;");
							SOTagsOfThePost = StringManipulations.extractspaceSeparatedTags(SOTagsOfThePost);
							
							if (!((poster.equals(" ")||poster.equals(Constants.LOGIN_OF_A_NON_IMPORTANT_USER__WHO_IS_NOT_SHARED_BETWEEN_SO_AND_GH)))){
								if ((evidenceTypes[Algorithm.INDEX_OF__EVIDENCE_TYPE__SOA]==Algorithm.YES && postTypeId.equals("2")) 
										|| (evidenceTypes[Algorithm.INDEX_OF__EVIDENCE_TYPE__SOQ]==Algorithm.YES && postTypeId.equals("1"))){ //if it is an answer and we need to read the answers (or the same for questions) then we need to consider this as an evidence.
									int evType;
									if (postTypeId.equals("1"))
										evType = Constants.EVIDENCE_TYPE_SO_QUESTION;
									else //: means that postTypeId is equal to "2":
										evType = Constants.EVIDENCE_TYPE_SO_ANSWER;
									if (SOTagsOfThePost.length() > 2){
										//See if the user is a member of angular or rails, add the q/a post as the evidence in that project:
										String[] projectIds = {"8514", "460078"};
										for (int j=0; j<2; j++){
											if (projectsAndTheirSharedUsersWithSO.get(projectIds[j]).contains(poster)){
												//Determining the Post's virtualSeqNum[] values based on assignments in different projectsAndTheirAssignments in projectsAndTheirAssignments_AL. 
												int[] virtualSeqNum = new int[Constants.NUMBER_OF_ASSIGNEE_TYPES];
												determineVirtualSeqNum(assignmentTypesToTriage, projectsAndTheirAssignments__AL_forDifferentAssignmetTypes, 
														projectIds[j], virtualSeqNum, date);
												addToIndex(SOTagsOfThePost, evType, Constants.SEQ_NUM____THIS_IS_NOT__B_A_EVIDENCE, virtualSeqNum, 
														originalNumberOfWordsInText, graph, projectIds[j], poster, date, projectId_Login_Tags_TypesAndTheirEvidence, 
														option3_TF, option7_whenToCountTextLength, fMR);
											}//if (projects....
										}//for (j.
									}
									else{
										//									fMR.errors++;
										//									System.out.println("Error! Non-assignment evidence length is zero!");
									}
								}
							}
						}
						else
							fMR.errors++;
						i++;
						if (i % showProgressInterval == 0)
							MyUtils.println(Constants.integerFormatter.format(i), indentationLevel+2);
					}
					br.close();
				}
				MyUtils.println("Finished.", indentationLevel+2);
			}
			else
				MyUtils.println("Not needed. Cancelled!", indentationLevel+2);

		} catch (Exception e) {
			System.out.println("ERROR4!");
			e.printStackTrace();
			fMR.errors++;
		} 

		MyUtils.println("Finished.", indentationLevel+1);
		if (wrapOutputInLines)
			MyUtils.println("-----------------------------------", indentationLevel);
	}
	//------------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------------
	public static String createFolderForResults(String outputPath, String experimentTitle, RUN_TYPE runType, RUN_TYPE_FOR_SO_CONTRIBUTIONS runType_SO, FileManipulationResult fMR, int indentationLevel){
		//This method browses all the folders (in outputPath) starting with a number, dash and space. 
			//Then identifies the largest "starting number" in them and increases it by one (=n), then creates a folder with that name plus other required suffixes (for the results to be put in) and returns it.
		MyUtils.createFolderIfDoesNotExist(outputPath, fMR, indentationLevel, "Initial 'temp directory checking'");
		File file = new File(outputPath);
		String[] directories = file.list(new FilenameFilter() {
			  @Override
			  public boolean accept(File current, String name) {
			    return new File(current, name).isDirectory();
			  }
			});
		int maxFolderPrefixNumber = 0;
		int folderPrefixNumber;
		for (String s: directories)
			if (s.matches("[0-9]+\\-\\s\\S.*")){//:if the folder name starts with a number, then dash followed by a space and at least one non-space (and anything else afterwards).
				String regex = "[0-9]+";//: get the starting number (the rest of numbers [if any] are uesless).
				Pattern p = Pattern.compile(regex);
				Matcher m = p.matcher(s);
				if (m.find()){
					folderPrefixNumber = Integer.parseInt(m.group(0));
					if (folderPrefixNumber > maxFolderPrefixNumber)
						maxFolderPrefixNumber = folderPrefixNumber;
				}
			}
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
		String runType_text = "";
		
		switch (runType){
		case RUN_FOR_ALL_PROJECTS:
			runType_text = "13mP";
			break;
		case RUN_FOR_TUNING_PROJECTS:
			runType_text = "3tP";
			break;
		case RUN_FOR_SELECTIVE_PROJECTS:
			runType_text = "selP";
			break;
		case RUN_FOR_PROJECTS_WITH_NUMBER_OF_ASSIGNMENTS_MORE_THAN_A_THRESHOLD:
			runType_text = "treP";
			break;
		case RUN_FOR_RAILS_AND_ANGULAR_AND_THEIR_FAMILIES:
			runType_text = "mulP";
			break;
		case RUN_FOR_RAILS_AND_ANGULAR_AND_THEIR_FAMILIES__JUST_RUN_ON_SHARED_USERS_WITH_SUB_PROJECTS:
			runType_text = "mulPJustSharedDevs";
			break;
		case RUN_FOR_ANGULAR_AND_ITS_FAMILY__FOR_TUNING:
			runType_text = "angFamP";
			break;
		case RUN_FOR_ANGULAR__FOR_TUNING:
			runType_text = "angP";
			break;
//		case RUN_FOR_RAILS_AND_ANGULAR_AND_THEIR_FAMILIES__WITH_SO_CONTRIBUTIONS:
//			runType_text = "pFam+so";
//			break;
		}
		
		switch (runType_SO){
			case DO_NOT_CONSIDER_SO:
				break;
			case CONSIDER_SO_BUT_DO_NOT_FILTER_ASSIGNMENTS: 
				runType_text = runType_text + " - SO";
				break;
			case CONSIDER_SO_AND_FILTER_ON_ASSIGNMENTS_ASSIGNED_TO_SHARED_USERS_BETWEEN_SO_AND_GH:
				runType_text = runType_text + " - SOSharedDevs";
				break;
		}
		String outputFolderName = Integer.toString(maxFolderPrefixNumber+1) + "- (" + experimentTitle + " - " + runType_text + ") - " + sdf.format(new Date());
		if (!(new File(outputPath+"\\"+outputFolderName).mkdirs())){
			fMR.errors = 1;
			MyUtils.println("Error creating output folder!", indentationLevel);
		}
		return outputFolderName;
	}
	//------------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------------
	public static boolean randomlySelectWithChance(int percentage){
		//This method is used to select a bug randomly with a "percentage" chance (e.g., when we want to triage n% of the bugs):
		if (random.nextInt(100) < percentage)
			return true;
		else
			return false;
	}
	//------------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------------
	public static String getMainLanguages(String mainLanguagesPercentages, int[] originalNumberOfLanguages_array){
		//This method gets the mainLanguagesPercentages in the form of "[julia^66;;c^19]" 
			//(each item in it is a language followed by "^" and its percentage; all languages with more than 15% are presented there), 
			//then extracts the main languages and returns a space-delimited string containing all those languages.
				//every 15% usage adds up another mention to the name of that language.
		//it is assumed that the input includes at least one language. So the input is not like "[]".
		String result = "";
		String[] languages = mainLanguagesPercentages.substring(1, mainLanguagesPercentages.length()-1).split(";;");
		for (int i=0; i<languages.length; i++){
			result = StringManipulations.concatTwoStringsWithDelimiter(result, languages[i].substring(0, languages[i].indexOf("^")), " ");
		}
		originalNumberOfLanguages_array[0] = languages.length; 
		return result;
	}
	//------------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------------
	public static String getBugText(Project project, Bug bug, int[] originalNumberOfWordsInText_array, BTOption1_whatToAddToAllBugs option1){
		//This method concatenates the project title, description and main languages to the bug title and description (all separated by space) and returns the resulting string
			//Also updates the originaltextLength_array[0] with the length of original text (which is the length of its elements before removing the non-SO keywords).
				//For example, if it adds bug.title and bug.body, then the value of originaltextLength_array[0] will be bug.title_numberOfWords+bug.body_numberOfWords. 
		switch (option1){
		case ADD_PTD:
			originalNumberOfWordsInText_array[0] = bug.title_numberOfWords + bug.body_numberOfWords + project.description_numberOfWords;
			return (bug.title + " " + bug.body + " " + project.description).replaceAll("\\s{2,}", " ").trim(); //removing extra spaces.
		case ADD_ML:
			if (project.mainLanguagePercentages.equals("[]")){
				originalNumberOfWordsInText_array[0] = bug.title_numberOfWords + bug.body_numberOfWords;
				return (bug.title + " " + bug.body).replaceAll("\\s{2,}", " ").trim(); //removing extra spaces (that may be added right now by concatenating).
			}
			else{
				int[] originalNumberOfLanguages_array = new int[1];
				String mainLanguages = getMainLanguages(project.mainLanguagePercentages, originalNumberOfLanguages_array);
				int originalNumberOfLanguages = originalNumberOfLanguages_array[0];
				originalNumberOfWordsInText_array[0] = bug.title_numberOfWords + bug.body_numberOfWords + originalNumberOfLanguages;
				return (bug.title + " " + bug.body + " " + mainLanguages).replaceAll("\\s{2,}", " ").trim(); //removing extra spaces (that may be added right now by concatenating).
			}
		case ADD_PTD_ML:
			if (project.mainLanguagePercentages.equals("[]")){
				originalNumberOfWordsInText_array[0] = bug.title_numberOfWords + bug.body_numberOfWords + project.description_numberOfWords;
				return (bug.title + " " + bug.body + " " + project.description).replaceAll("\\s{2,}", " ").trim(); //removing extra spaces.
			}
			else{
				int[] originalNumberOfLanguages_array = new int[1];
				String mainLanguages = getMainLanguages(project.mainLanguagePercentages, originalNumberOfLanguages_array);
				int originalNumberOfLanguages = originalNumberOfLanguages_array[0];
				originalNumberOfWordsInText_array[0] = bug.title_numberOfWords + bug.body_numberOfWords + project.description_numberOfWords + originalNumberOfLanguages;
				return (bug.title + " " + bug.body + " " + project.description + " " + mainLanguages).replaceAll("\\s{2,}", " ").trim(); //removing extra spaces.
			}
		case JUST_USE_BUG_TD:
		default:
			originalNumberOfWordsInText_array[0] = bug.title_numberOfWords + bug.body_numberOfWords;
			return (bug.title + " " + bug.body).replaceAll("\\s{2,}", " ").trim(); //removing extra spaces (that may be added right now by concatenating).
		}
				
//		if ((option1 == BTOption1.ADD_PTD || option1 == BTOption1.ADD_PTD_mL) && !project.description.equals(" "))
//			if ((option1 == BTOption1.ADD_mL || option1 == BTOption1.ADD_PTD_mL) && !project.mainLanguagePercentages.equals("[]"))
//				return (bug.title + " " + bug.body + " " + project.description + " " + getMainLanguages(project.mainLanguagePercentages)).replaceAll("\\s{2,}", " ").trim(); //removing extra spaces.
//			else
//				return (bug.title + " " + bug.body + " " + project.description).replaceAll("\\s{2,}", " ").trim(); //removing extra spaces.
//		else
//			if ((option1 == BTOption1.ADD_mL || option1 == BTOption1.ADD_PTD_mL) && !project.mainLanguagePercentages.equals("[]"))
//				return (bug.title + " " + bug.body + " " + getMainLanguages(project.mainLanguagePercentages)).replaceAll("\\s{2,}", " ").trim(); //removing extra spaces (that may be added right now by concatenating).
//			else
//				return (bug.title + " " + bug.body).replaceAll("\\s{2,}", " ").trim(); //removing extra spaces (that may be added right now by concatenating).
	}
	//------------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------------
	public static String getCommitText(Project project, String commitMessage, int[] originalNumberOfWordsInText_array, BTOption6_whatToAddToAllCommits option6_whatToAddToAllCommits){
		//This method ... (like the getBugText() method).
			//Also updates originalAddedTextLength_array[0] to contain the number of words added to commitMessage (not the commitMessage itself).
		switch (option6_whatToAddToAllCommits){
		case ADD_PTD:
			originalNumberOfWordsInText_array[0] = originalNumberOfWordsInText_array[0] + project.description_numberOfWords;
			return (commitMessage + " " + project.description).replaceAll("\\s{2,}", " ").trim(); //removing extra spaces.
		case ADD_mL:
			if (project.mainLanguagePercentages.equals("[]")){
				originalNumberOfWordsInText_array[0] = originalNumberOfWordsInText_array[0]; //no change.
				return commitMessage;
			}
			else{
				int[] originalNumberOfLanguages_array = new int[1];
				String mainLanguages = getMainLanguages(project.mainLanguagePercentages, originalNumberOfLanguages_array);
				int originalNumberOfLanguages = originalNumberOfLanguages_array[0];
				originalNumberOfWordsInText_array[0] = originalNumberOfWordsInText_array[0] + originalNumberOfLanguages;
				return (commitMessage + " " + mainLanguages).replaceAll("\\s{2,}", " ").trim(); //removing extra spaces (that may be added right now by concatenating).
			}
		case ADD_PTD_mL:
			if (project.mainLanguagePercentages.equals("[]")){
				originalNumberOfWordsInText_array[0] = originalNumberOfWordsInText_array[0] + project.description_numberOfWords;
				return (commitMessage + " " + project.description).replaceAll("\\s{2,}", " ").trim(); //removing extra spaces.
			}
			else{
				int[] originalNumberOfLanguages_array = new int[1];
				String mainLanguages = getMainLanguages(project.mainLanguagePercentages, originalNumberOfLanguages_array);
				int originalNumberOfLanguages = originalNumberOfLanguages_array[0];
				originalNumberOfWordsInText_array[0] = originalNumberOfWordsInText_array[0] + project.description_numberOfWords + originalNumberOfLanguages;
				return (commitMessage + " " + project.description + " " + mainLanguages).replaceAll("\\s{2,}", " ").trim(); //removing extra spaces.
			}
		case JUST_USE_COMMIT_M:
		default:
			return commitMessage;
		}
		
//		if ((option6_whatToAddToAllCommits == BTOption6.ADD_PTD || option6_whatToAddToAllCommits == BTOption6.ADD_PTD_mL) && !project.description.equals(" "))
//			if ((option6_whatToAddToAllCommits == BTOption6.ADD_mL || option6_whatToAddToAllCommits == BTOption6.ADD_PTD_mL) && !project.mainLanguagePercentages.equals("[]"))
//				return (commitMessage + " " + project.description + " " + getMainLanguages(project.mainLanguagePercentages)).replaceAll("\\s{2,}", " ").trim(); //removing extra spaces.
//			else
//				return (commitMessage + " " + project.description).replaceAll("\\s{2,}", " ").trim(); //removing extra spaces.
//		else
//			if ((option6_whatToAddToAllCommits == BTOption6.ADD_mL || option6_whatToAddToAllCommits == BTOption6.ADD_PTD_mL) && !project.mainLanguagePercentages.equals("[]"))
//				return (commitMessage + " " + getMainLanguages(project.mainLanguagePercentages)).replaceAll("\\s{2,}", " ").trim(); //removing extra spaces (that may be added right now by concatenating).
//			else
//				return commitMessage;
	}
	//------------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------------
	public static String getPRText(Project project, String pRText, int[] originalNumberOfWordsInText_array, BTOption9_whatToAddToAllPRs option9_whatToAddToAllPRs){
		//This method ... (like the getBugText() method).
			//Also updates originalAddedTextLength_array[0] to contain the number of words added to pRText (not the pRText itself).
		switch (option9_whatToAddToAllPRs){
			case ADD_mL:
				if (project.mainLanguagePercentages.equals("[]")){
					originalNumberOfWordsInText_array[0] = originalNumberOfWordsInText_array[0]; //no change.
					return pRText;
				}
				else{
					int[] originalNumberOfLanguages_array = new int[1];
					String mainLanguages = getMainLanguages(project.mainLanguagePercentages, originalNumberOfLanguages_array);
					int originalNumberOfLanguages = originalNumberOfLanguages_array[0];
					originalNumberOfWordsInText_array[0] = originalNumberOfWordsInText_array[0] + originalNumberOfLanguages;
					return (pRText + " " + mainLanguages).replaceAll("\\s{2,}", " ").trim(); //removing extra spaces (that may be added right now by concatenating).
				}
			case JUST_USE_PR_TEXT:
			default:
				return pRText;
		}
	}
	//------------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------------
	public static String getBCText(Project project, String bCText, int[] originalNumberOfWordsInText_array, BTOption10_whatToAddToAllBCs option10_whatToAddToAllBugComments){
		//This method ... (like the getBugText() method, but for bugComment).
			//Also updates originalAddedTextLength_array[0] to contain the number of words added to pRText (not the pRText itself).
		switch (option10_whatToAddToAllBugComments){
			case ADD_mL:
				if (project.mainLanguagePercentages.equals("[]")){
					originalNumberOfWordsInText_array[0] = originalNumberOfWordsInText_array[0]; //no change.
					return bCText;
				}
				else{
					int[] originalNumberOfLanguages_array = new int[1];
					String mainLanguages = getMainLanguages(project.mainLanguagePercentages, originalNumberOfLanguages_array);
					int originalNumberOfLanguages = originalNumberOfLanguages_array[0];
					originalNumberOfWordsInText_array[0] = originalNumberOfWordsInText_array[0] + originalNumberOfLanguages;
					return (bCText + " " + mainLanguages).replaceAll("\\s{2,}", " ").trim(); //removing extra spaces (that may be added right now by concatenating).
				}
			case JUST_USE_BC_TEXT:
			default:
				return bCText;
		}
	}
	//------------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------------
	public static String getCCText(Project project, String bCText, int[] originalNumberOfWordsInText_array, BTOption11_whatToAddToAllCCs option11_whatToAddToAllCommitComments){
		//This method ... (like the getBugText() method, but for commitComment).
			//Also updates originalAddedTextLength_array[0] to contain the number of words added to pRText (not the pRText itself).
		switch (option11_whatToAddToAllCommitComments){
			case ADD_mL:
				if (project.mainLanguagePercentages.equals("[]")){
					originalNumberOfWordsInText_array[0] = originalNumberOfWordsInText_array[0]; //no change.
					return bCText;
				}
				else{
					int[] originalNumberOfLanguages_array = new int[1];
					String mainLanguages = getMainLanguages(project.mainLanguagePercentages, originalNumberOfLanguages_array);
					int originalNumberOfLanguages = originalNumberOfLanguages_array[0];
					originalNumberOfWordsInText_array[0] = originalNumberOfWordsInText_array[0] + originalNumberOfLanguages;
					return (bCText + " " + mainLanguages).replaceAll("\\s{2,}", " ").trim(); //removing extra spaces (that may be added right now by concatenating).
				}
			case JUST_USE_CC_TEXT:
			default:
				return bCText;
		}
	}
	//------------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------------
	public static String getPRCText(Project project, String bCText, int[] originalNumberOfWordsInText_array, BTOption12_whatToAddToAllPRCs option12_whatToAddToAllPRComments){
		//This method ... (like the getBugText() method, but for commitComment).
			//Also updates originalAddedTextLength_array[0] to contain the number of words added to pRText (not the pRText itself).
		switch (option12_whatToAddToAllPRComments){
			case ADD_mL:
				if (project.mainLanguagePercentages.equals("[]")){
					originalNumberOfWordsInText_array[0] = originalNumberOfWordsInText_array[0]; //no change.
					return bCText;
				}
				else{
					int[] originalNumberOfLanguages_array = new int[1];
					String mainLanguages = getMainLanguages(project.mainLanguagePercentages, originalNumberOfLanguages_array);
					int originalNumberOfLanguages = originalNumberOfLanguages_array[0];
					originalNumberOfWordsInText_array[0] = originalNumberOfWordsInText_array[0] + originalNumberOfLanguages;
					return (bCText + " " + mainLanguages).replaceAll("\\s{2,}", " ").trim(); //removing extra spaces (that may be added right now by concatenating).
				}
			case JUST_USE_PRC_TEXT:
			default:
				return bCText;
		}
	}
	//------------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------------
					//projectId --> bugNumber --> developer --> ReferenceStatus((referenceType --> ArrayList<String ReferencedDate>))
	public static HashMap<String, HashMap<String, HashMap<String, ReferenceStatus>>>
		read_ReferencesToDevelopers_(String inputPath, String inputFileName, 
			int[] option13_referenceTypesToConsider, 
				FileManipulationResult fMR,
			boolean wrapOutputInLines, int showProgressInterval, int indentationLevel, long testOrReal, String writeMessageStep){
		//This method reads the file containing the "references to the developers" in a hashMap of hashMap of ....:
		if (wrapOutputInLines)
			MyUtils.println("-----------------------------------", indentationLevel);
		HashMap<String, HashMap<String, HashMap<String, ReferenceStatus>>> projectId_bugNumber_developer_referenceStatus
			= new HashMap<String, HashMap<String, HashMap<String, ReferenceStatus>>>();
		try{ 
			MyUtils.println(writeMessageStep + "- Parsing " + inputFileName + ":", indentationLevel);
			if (MyUtils.binaryArrayToString(option13_referenceTypesToConsider, 4).equals("0000"))
				MyUtils.println("It was set to ignore the references, ..., Ignored.", indentationLevel+1);
			else{
				BufferedReader br;
				br = new BufferedReader(new FileReader(inputPath + "\\" + inputFileName)); 
				MyUtils.println("Started ...", indentationLevel+1);
				int i=0;
				String s;
				br.readLine(); //header.
				while ((s=br.readLine())!=null){
					String[] fields = s.split(TAB);
					if (fields.length == 6){
						String referenceType_string = fields[3];
						REFERENCE_TYPE referenceType = REFERENCE_TYPE.fromInt(Integer.parseInt(referenceType_string)-1);
						if (option13_referenceTypesToConsider[referenceType.ordinal()]==1){//only add the reference to the index if it should be considered.
							String projectId = fields[0];
							String bugNumber = fields[1];
							String developer = fields[2];
							String date = fields[4];
//							System.out.println(projectId + TAB + bugNumber + TAB + developer + TAB + referenceType + TAB + date);
							//Start by projectId:
							HashMap<String, HashMap<String, ReferenceStatus>> bugNumber_developer_referenceStatus;
							if (projectId_bugNumber_developer_referenceStatus.containsKey(projectId))
								bugNumber_developer_referenceStatus = projectId_bugNumber_developer_referenceStatus.get(projectId);
							else{
								bugNumber_developer_referenceStatus = new HashMap<String, HashMap<String, ReferenceStatus>>();
								projectId_bugNumber_developer_referenceStatus.put(projectId, bugNumber_developer_referenceStatus);
							}
							//Then, moving on to bugNumber:
							HashMap<String, ReferenceStatus> developer_referenceStatus;
							if (bugNumber_developer_referenceStatus.containsKey(bugNumber))
								developer_referenceStatus = bugNumber_developer_referenceStatus.get(bugNumber);
							else{
								developer_referenceStatus = new HashMap<String, ReferenceStatus>();
								bugNumber_developer_referenceStatus.put(bugNumber, developer_referenceStatus);
							}
							//After that, moving on to developer:
							ReferenceStatus referenceStatus;
							if (developer_referenceStatus.containsKey(developer))
								referenceStatus = developer_referenceStatus.get(developer);
							else{
								referenceStatus = new ReferenceStatus();
								developer_referenceStatus.put(developer, referenceStatus);
							}
							//Last, moving on to ReferenceType:
							if (referenceStatus.references == null)
								referenceStatus.references = new HashMap<REFERENCE_TYPE, ArrayList<Date>>();
							HashMap<REFERENCE_TYPE, ArrayList<Date>> references = referenceStatus.references;
							ArrayList<Date> referenceDates;
							if (references.containsKey(referenceType))
								referenceDates = references.get(referenceType);
							else{
								referenceDates = new ArrayList<Date>();
								references.put(referenceType, referenceDates);
							}
							//Finally, add the reference dates to the arrayList.
							referenceDates.add(Constants.dateFormat.parse(date));
						}//if (option13_....
					}//if.
					else
						fMR.errors++;
					i++;
					if (testOrReal > Constants.THIS_IS_REAL)
						if (i >= testOrReal)
							break;
					if (i % showProgressInterval == 0)
						System.out.println(MyUtils.indent(indentationLevel+1) + Constants.integerFormatter.format(i));
				}//while ((s=br....

				if (fMR.errors == 0)
					MyUtils.println("Finished.", indentationLevel+1);
				else
					MyUtils.println("Finished with errors.", indentationLevel+1);

				br.close();
				}
		}catch (Exception e){
			fMR.errors++;
			e.printStackTrace();
		}
		if (wrapOutputInLines)
			MyUtils.println("-----------------------------------", indentationLevel);

		return projectId_bugNumber_developer_referenceStatus;
	}
	//------------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------------
	public static boolean atLeastOneOfReferenceTypesShouldBeConsidered(int[] option13_referenceTypesToConsider){
		for (int i=0; i< Constants.MAX_NUMBER_OF_REFERENCE_TYPES; i++)
			if (option13_referenceTypesToConsider[i] == Algorithm.YES)
				return true;
		return false;
	}
	//------------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------------
	public static String get__ProjectId_or_parentProjectId(RUN_TYPE runType, Project project, 
			HashMap<String, HashSet<String>> projectsAndTheirCommunities_HM, String login, AtomicBoolean thisEvidenceShouldBeConsidered, AtomicBoolean isMultiProjEvidence){
		//This method checks if we are running the "multi-project" experiment, 
			//and the current evidence is in a sub-project of rails or angular, 
				//then the projectId should be the projectId of rails or angular, not the sub-project (to index the evidence as the evidence of the top project for its assignments. In fact, we do not assign bugs in sub-projects).
				//Also if <the experiment is multi-project> and <the project is a sub-project of rails or angular> and <the user (login) related to the evidence being considered is not in the community of the super project (rails or angular)> then the variable "thisEvidenceShouldBeConsidered" will be set to false. 
		String projectId = project.id;
		if (runType == RUN_TYPE.RUN_FOR_RAILS_AND_ANGULAR_AND_THEIR_FAMILIES 
				|| runType == RUN_TYPE.RUN_FOR_RAILS_AND_ANGULAR_AND_THEIR_FAMILIES__JUST_RUN_ON_SHARED_USERS_WITH_SUB_PROJECTS){
			if (project.owner_repo.startsWith("rails/") && !project.owner_repo.equals("rails/rails")){
				projectId = "8514";//projectId of rails/rails
				thisEvidenceShouldBeConsidered.set(theUserIsInCommunityOfProject(login, projectId, projectsAndTheirCommunities_HM));
				isMultiProjEvidence.set(true);
			}
			else
				if (project.owner_repo.startsWith("angular/") && !project.owner_repo.equals("angular/angular.js")){
					projectId = "460078";//projectId of angular/angular.js
					thisEvidenceShouldBeConsidered.set(theUserIsInCommunityOfProject(login, projectId, projectsAndTheirCommunities_HM));
					isMultiProjEvidence.set(true);
				}
		}
		else
			if (runType == RUN_TYPE.RUN_FOR_ANGULAR_AND_ITS_FAMILY__FOR_TUNING)
				if (project.owner_repo.startsWith("angular/") && !project.owner_repo.equals("angular/angular.js")){
					projectId = "460078";//projectId of angular/angular.js
					thisEvidenceShouldBeConsidered.set(theUserIsInCommunityOfProject(login, projectId, projectsAndTheirCommunities_HM));
					isMultiProjEvidence.set(true);
				}
		return projectId;
	}	
	//------------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------------
	public static boolean theUserIsInCommunityOfProject(String login, String projectId, HashMap<String, HashSet<String>> projectsAndTheirCommunities_HM){
		HashSet<String> community = projectsAndTheirCommunities_HM.get(projectId);
		if (community.contains(login))
			return true;
		else
			return false;
	}
	//------------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------------
	public static boolean shouldRunTheExperimentOnProject(Constants.RUN_TYPE runType, Project project, ArrayList<String[]> assignmentsOfThisProject, int thresholdForNumberOfAssignmentsInAProject){
		if (runType == RUN_TYPE.RUN_FOR_ALL_PROJECTS 
				|| (runType == RUN_TYPE.RUN_FOR_TUNING_PROJECTS && AlgPrep.isAProjectWhichIsUsedForTuning(project.id, project.owner_repo))
				|| (runType == RUN_TYPE.RUN_FOR_SELECTIVE_PROJECTS && AlgPrep.isASelectedProjectToRunTheExperimentOn(project.id, project.owner_repo))
				|| (runType == RUN_TYPE.RUN_FOR_PROJECTS_WITH_NUMBER_OF_ASSIGNMENTS_MORE_THAN_A_THRESHOLD && assignmentsOfThisProject.size() >= thresholdForNumberOfAssignmentsInAProject)
				|| ((runType == RUN_TYPE.RUN_FOR_RAILS_AND_ANGULAR_AND_THEIR_FAMILIES 
							|| runType == RUN_TYPE.RUN_FOR_RAILS_AND_ANGULAR_AND_THEIR_FAMILIES__JUST_RUN_ON_SHARED_USERS_WITH_SUB_PROJECTS) 
						&& (project.owner_repo.startsWith("rails/") || project.owner_repo.startsWith("angular/")))
				|| (runType == RUN_TYPE.RUN_FOR_ANGULAR_AND_ITS_FAMILY__FOR_TUNING && project.owner_repo.startsWith("angular/"))
				|| (runType == RUN_TYPE.RUN_FOR_ANGULAR__FOR_TUNING && project.owner_repo.equals("angular/angular.js"))
				)
			return true;
		else
			return false;
	}
	//------------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------------
	public static boolean additionalCriteriaForAssigningBugsInProject(Constants.RUN_TYPE runType, Project project){
		if (runType == RUN_TYPE.RUN_FOR_ANGULAR_AND_ITS_FAMILY__FOR_TUNING)
			if (project.owner_repo.equals("angular/angular.js"))
				return true;
			else
				return false;
		else
			return true;
	}
	//------------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------------
	public static HashMap<String, HashSet<String>> readProjectsAndTheirUsers(String inputPath, String inputFile, String message, FileManipulationResult fMR, 
			boolean wrapOutputInLines, int showProgressInterval, int indentationLevel, long testOrReal, String writeMessageStep){
		HashMap<String, HashSet<String>> projectsAndTheirUsers = new HashMap<String, HashSet<String>>();
		try{ 
			if (wrapOutputInLines)
				MyUtils.println("-----------------------------------", indentationLevel+1);
			MyUtils.println(MyUtils.concatTwoWriteMessageSteps(writeMessageStep, message), indentationLevel);
				BufferedReader br;
				br = new BufferedReader(new FileReader(inputPath + "\\" + inputFile)); 
				MyUtils.println("Started ...", indentationLevel+1);
				int i=0;
				String s;
				br.readLine(); //header.
				while ((s=br.readLine())!=null){
					String[] fields = s.split(TAB);
					if (fields.length == 2){
						String projectId = fields[0];
						String login = fields[1];
						if (projectsAndTheirUsers.containsKey(projectId)){
							HashSet<String> users = projectsAndTheirUsers.get(projectId);
							users.add(login);
						}
						else{
							HashSet<String> users = new HashSet<String>();
							users.add(login);
							projectsAndTheirUsers.put(projectId, users);
						}
					}//if.
					else
						fMR.errors++;
					i++;
					if (testOrReal > Constants.THIS_IS_REAL)
						if (i >= testOrReal)
							break;
					if (i % showProgressInterval == 0)
						System.out.println(MyUtils.indent(indentationLevel+1) + Constants.integerFormatter.format(i));
				}//while ((s=br....
				if (fMR.errors == 0)
					MyUtils.println("Finished.", indentationLevel+1);
				else
					MyUtils.println("Finished with errors.", indentationLevel+1);
				br.close();
		}catch (Exception e){
			fMR.errors++;
			MyUtils.println("There are errors in reading specific projects and their users.", indentationLevel);
			e.printStackTrace();
		}
		MyUtils.println("Finished.", indentationLevel+2);
		if (wrapOutputInLines)
			MyUtils.println("-----------------------------------", indentationLevel+1);
		return projectsAndTheirUsers;
	}
	//------------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------------
	public static HashMap<String, HashSet<String>> readProjectsAndTheirUsersWhoAreAlsoInSO(String inputPath, String inputFile, String message, FileManipulationResult fMR, 
			boolean wrapOutputInLines, int showProgressInterval, int indentationLevel, long testOrReal, String writeMessageStep){
		HashMap<String, HashSet<String>> projectsAndTheirUsers = new HashMap<String, HashSet<String>>();
		try{ 
			if (wrapOutputInLines)
				MyUtils.println("-----------------------------------", indentationLevel+1);
			MyUtils.println(MyUtils.concatTwoWriteMessageSteps(writeMessageStep, message), indentationLevel);
				BufferedReader br;
				br = new BufferedReader(new FileReader(inputPath + "\\" + inputFile)); 
				MyUtils.println("Started ...", indentationLevel+1);
				int i=0;
				String s;
				br.readLine(); //header.
				while ((s=br.readLine())!=null){
					String[] fields = s.split(TAB);
					if (fields.length == 5){
						String projectId = fields[0];
						String login = fields[2];
						if (projectsAndTheirUsers.containsKey(projectId)){
							HashSet<String> users = projectsAndTheirUsers.get(projectId);
							users.add(login);
						}
						else{
							HashSet<String> users = new HashSet<String>();
							users.add(login);
							projectsAndTheirUsers.put(projectId, users);
						}
					}//if.
					else
						fMR.errors++;
					i++;
					if (testOrReal > Constants.THIS_IS_REAL)
						if (i >= testOrReal)
							break;
					if (i % showProgressInterval == 0)
						System.out.println(MyUtils.indent(indentationLevel+1) + Constants.integerFormatter.format(i));
				}//while ((s=br....
				if (fMR.errors == 0)
					MyUtils.println("Finished.", indentationLevel+1);
				else
					MyUtils.println("Finished with errors.", indentationLevel+1);
				br.close();
		}catch (Exception e){
			fMR.errors++;
			MyUtils.println("There are errors in reading specific projects and their shared users with SO.", indentationLevel);
			e.printStackTrace();
		}
		MyUtils.println("Finished.", indentationLevel+2);
		if (wrapOutputInLines)
			MyUtils.println("-----------------------------------", indentationLevel+1);
		return projectsAndTheirUsers;
	}
	//------------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------------
	public static void filterAssignmentsTo_sharedUsers(TreeMap<String, ArrayList<String[]>> projectsAndTheirAssignments, HashMap<String, HashSet<String>> projectsAndTheirSharedUsersWithSubProjects, 
			int indentationLevel){
		for (String projectId: projectsAndTheirAssignments.keySet()){
			HashSet<String> sharedUsers = projectsAndTheirSharedUsersWithSubProjects.get(projectId);
			ArrayList<String[]> assignmentsOfAProject = projectsAndTheirAssignments.get(projectId);
			for (int i=assignmentsOfAProject.size()-1; i>=0; i--){//: Start from last assignment, see if the assignee is not a shared user, then delete index i of assignments. Otherwise go to the previous one:
				Assignment a = new Assignment(assignmentsOfAProject, i, indentationLevel);
				if (sharedUsers == null || !sharedUsers.contains(a.login))//: means that if the assignee is not a shared user;
					assignmentsOfAProject.remove(i);
			}
		}
	}
	//------------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------------
}


