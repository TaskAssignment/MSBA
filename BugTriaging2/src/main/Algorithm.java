package main;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import data.Assignee;
import data.Assignment;
import data.AssignmentStat;
import data.Bug;
import data.Evidence;
import data.Project;
import utils.Constants;
import utils.FileManipulationResult;
import utils.Graph;
import utils.MyUtils;
import utils.StringManipulations;
import utils.TSVManipulations;
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
import utils.Constants.ConditionType;
import utils.Constants.FieldType;
import utils.Constants.LogicalOperation;
import utils.Constants.ProjectType;
import utils.Constants.RUN_TYPE;
import utils.Constants.RUN_TYPE_FOR_SO_CONTRIBUTIONS;
import utils.Constants.SortOrder;

public class Algorithm {//test 9
	public static final int YES = 1;
	public static final int NO = 0;
	public static final int UNKNOWN_RANK = Integer.MAX_VALUE;
	public static final int INDEX_OF__EVIDENCE_TYPE__COMMIT = 1;
	public static final int INDEX_OF__EVIDENCE_TYPE__PR = 2;
	public static final int INDEX_OF__EVIDENCE_TYPE__BUG_COMMENT = 3;
	public static final int INDEX_OF__EVIDENCE_TYPE__COMMIT_COMMENT = 4;
	public static final int INDEX_OF__EVIDENCE_TYPE__PR_COMMENT = 5;

	public static final int INDEX_OF__EVIDENCE_TYPE__MULTI_PROJ__COMMIT = 7;
	public static final int INDEX_OF__EVIDENCE_TYPE__MULTI_PROJ__PR = 8;
	public static final int INDEX_OF__EVIDENCE_TYPE__MULTI_PROJ__BUG_COMMENT = 9;
	public static final int INDEX_OF__EVIDENCE_TYPE__MULTI_PROJ__COMMIT_COMMENT = 10;
	public static final int INDEX_OF__EVIDENCE_TYPE__MULTI_PROJ__PR_COMMENT = 11;

	public static final int INDEX_OF__EVIDENCE_TYPE__SOA = 12;
	public static final int INDEX_OF__EVIDENCE_TYPE__SOQ = 13;

	// In the following method, bug assignment is experimented and the results are shown. Also summary is printed in output file <"results-"+currentDateTime.txt>.
	public static void bugAssignment(String inputPath, String SOInputPath, String outputPath, String outputSummariesTSVFileName, 
			RUN_TYPE runType, RUN_TYPE_FOR_SO_CONTRIBUTIONS runType_SO, int thresholdForNumberOfAssignmentsInAProject, 
			int[] assignmentTypesToTriage, int[] evidenceTypes, int totalEvidenceTypes_count,  
			String experimentTitle, String experimentDetails, 
			BTOption1_whatToAddToAllBugs option1_whatToAddToAllBugs, BTOption2_w option2_w, BTOption3_TF option3_TF, BTOption4_IDF option4_IDF, BTOption5_prioritizePAs option5_prioritizePAs, 
			BTOption6_whatToAddToAllCommits option6_whatToAddToAllCommits,  BTOption7_whenToCountTextLength option7_whenToCountTextLength, BTOption8_recency option8_recency,
			BTOption9_whatToAddToAllPRs option9_whatToAddToAllPRs, BTOption10_whatToAddToAllBCs option10_whatToAddToAllBugComments, 
			BTOption11_whatToAddToAllCCs option11_whatToAddToAllCommitComments, BTOption12_whatToAddToAllPRCs option12_whatToAddToAllPRComments, 
			int[] option13_referenceTypesToConsider, 
			FileManipulationResult fMR,
			boolean wrapOutputInLines, int showProgressInterval, int indentationLevel, long testOrReal, String writeMessageStep) {
		MyUtils.println("-----------------------------------", indentationLevel);
		MyUtils.println(MyUtils.concatTwoWriteMessageSteps(writeMessageStep, "Bug assignment experiment:"), indentationLevel);
		MyUtils.println("Started ...", indentationLevel+1);
		
		float loopExtraTime_readingCommunitiesAndNonAsssignmentEvidence = 0;
		float initialExtraTime__readingGraphBugsAndProjectsInfo = 0;
		float initialExtraTime__readingAssignments_and_readingAndIndexingNonAssignmentEvidence = 0;
		float loopExtraTime_readingAssignmentEvidenceIndexing = 0;
		Date d1 = new Date();
		//The following method (loadGraph) was ran successfully (2016/11/16):
		FileManipulationResult localFMR = new FileManipulationResult();
		FileManipulationResult totalFMR = new FileManipulationResult();
		MyUtils.createFolderIfDoesNotExist(outputPath, localFMR, indentationLevel+1, "Additional step: Initial 'directory checking' ... ");

		Graph graph = new Graph();
		graph.loadGraph(SOInputPath, "nodeWeights.tsv", "edgeWeights.tsv", localFMR, 
				wrapOutputInLines, showProgressInterval*1000, indentationLevel+1, Constants.THIS_IS_REAL, MyUtils.concatTwoWriteMessageSteps(writeMessageStep, "1"));
		totalFMR = MyUtils.addFileManipulationResults(totalFMR, localFMR);
//		//Testing:
//		System.out.println("Testing:");
//		System.out.println("javascript: " + graph.getNodeWeight("javascript"));
//		System.out.println("javascript-->jquery: " + graph.getEdgeWeight("javascript", "jquery")); //0.33523083822630956
//		System.out.println("javascript-->html: " + graph.getEdgeWeight("javascript", "html")); //0.1845487229352718
//		System.out.println(".a-->c: " + graph.getEdgeWeight(".a", "c")); //0.1724137931034483
//		System.out.println("javascript-->android: " + graph.getEdgeWeight("javascript", "android")); //0.
		
		ArrayList<String> titlesToReturn_IS_NOT_NEEDED_AND_USED = new ArrayList<String>();
		
		TreeMap<String, String[]> projects = TSVManipulations.readUniqueKeyAndItsValueFromTSV(inputPath, "7-projects.tsv", null, 
				0, 14, "1$2$3$4$5$6$7$8$9$10$11$12$13", 
				LogicalOperation.NO_CONDITION,
				0, ConditionType.NOTHING, "", FieldType.NOT_IMPORTANT,
				0, ConditionType.NOTHING, "", FieldType.NOT_IMPORTANT,
				wrapOutputInLines, showProgressInterval, indentationLevel+1, Constants.THIS_IS_REAL, MyUtils.concatTwoWriteMessageSteps(writeMessageStep, "2"));
		
		TreeMap<String, String[]> projectIdBugNumberAndTheirBugInfo = TSVManipulations.readUniqueCombinedKeyAndItsValueFromTSV(
				inputPath, "1-bugs-"+Constants.ASSIGNED_BUGS_TYPES__SHORT_DESCRIPTIONS[4]+".tsv", localFMR, null, 
				"0$1",  
				9, "2$3$4$5$6$7$8", 
				LogicalOperation.NO_CONDITION, 
				0, ConditionType.NOTHING, "", FieldType.NOT_IMPORTANT, 
				0, ConditionType.NOTHING, "", FieldType.NOT_IMPORTANT, 
				wrapOutputInLines, showProgressInterval*1000, indentationLevel+1, Constants.THIS_IS_REAL, "3");
		totalFMR = MyUtils.addFileManipulationResults(totalFMR, localFMR);
		
		//projectId --> bugNumber --> developer --> referenceType --> ArrayList<String ReferencedDate>
		HashMap<String, HashMap<String, HashMap<String, ReferenceStatus>>> projectId_bugNumber_developer_referenceStatus
			= new HashMap<String, HashMap<String, HashMap<String, ReferenceStatus>>>();
//		if (!MyUtils.binaryArrayToString(option13_referencesToConsider, 4).equals("0000")){
			//: means that if option13_referencesToConsider is not all zero's. If all of them are zero's, then we don't need to read the references.
		projectId_bugNumber_developer_referenceStatus = AlgPrep.read_ReferencesToDevelopers_(inputPath, "11-referencesToDevelopers_T5.tsv", 
				option13_referenceTypesToConsider, 
				localFMR, 
				wrapOutputInLines, showProgressInterval*10, indentationLevel+1, Constants.THIS_IS_REAL, "4");
		totalFMR = MyUtils.addFileManipulationResults(totalFMR, localFMR);
//		}
//		System.out.println(projectId_bugNumber_developer_referenceType__referenceDates);
		
		//Preparing the 'evidenceTypesToConsider' array for AlgPrep.calculateScoreOfDeveloperForBugAssignment():
		int evidenceTypesToConsider_count = 0;
		for (int j=0; j<totalEvidenceTypes_count; j++) 
			if (evidenceTypes[j] == YES)
				evidenceTypesToConsider_count++;
		int[] evidenceTypesToConsider = new int[evidenceTypesToConsider_count];
		int p;
		if (evidenceTypes[0] == YES){ //: assignedBug
			p = 1; //: reserve the k'th index for ASSIGNMENT_TYPES_TO_TRIAGE values in the loop.
			//evidenceTypesToConsider[0] = <0, 1, 2, 3 or 4> --> this will be set in the loop over five assignment types below.
		}
		else
			p = 0; //: means that 'evidenceTypes[0] == NO', i.e., we shouldn't use assignedBug evidence for triaging.
		for (int j=1; j<6; j++) 
			if (evidenceTypes[j] == YES){
				evidenceTypesToConsider[p] = Constants.EVIDENCE_TYPE[4+j]; //: Something between Constants.EVIDENCE_TYPE_COMMIT and EVIDENCE_TYPE_PR_COMMENT. Item #4 in Constants.EVIDENCE_TYPE is the last assignment-related evidence. 
				p++;
			}
		
		int p2;
		int indexOfMultiProject_assignedBug = 0;
		if (evidenceTypes[6] == YES){ //: assignedBug
			indexOfMultiProject_assignedBug = p;
			p2 = p + 1; //: reserve the k'th index for ASSIGNMENT_TYPES_TO_TRIAGE values in the loop.
			//evidenceTypesToConsider[6] = <20, 21, 22, 23 or 24> --> this will be set in the loop over five assignment types below.
		}
		else
			p2 = p; //: means that 'evidenceTypes[6] == NO', i.e., we shouldn't use multiProj_assignedBug evidence for triaging.
		for (int j=7; j<totalEvidenceTypes_count; j++) 
			if (evidenceTypes[j] == YES){
				evidenceTypesToConsider[p2] = Constants.EVIDENCE_TYPE[8+j]; //: Something between Constants.EVIDENCE_TYPE_COMMIT and EVIDENCE_TYPE_PR_COMMENT. Item #4 in Constants.EVIDENCE_TYPE is the last assignment-related evidence. 
				p2++;
			}
		

		MyUtils.println("------ Note: Types of evidence that will be read in this run:", 1);
		for (int j=0; j<evidenceTypesToConsider_count; j++) {
			if (j==0){
				if (evidenceTypes[0] == YES) //assignedBug
					MyUtils.println("Bug: " + MyUtils.binaryArrayToString(assignmentTypesToTriage, Constants.NUMBER_OF_ASSIGNEE_TYPES), 2);
			}
			else
				if (j!=0 && j==indexOfMultiProject_assignedBug){
					if (evidenceTypes[6] == YES) //MultiProject-->assignedBug
						MyUtils.println("MultiProject --> Bug: " + MyUtils.binaryArrayToString(assignmentTypesToTriage, Constants.NUMBER_OF_ASSIGNEE_TYPES), 2);
				}
				else
					MyUtils.println(Integer.toString(evidenceTypesToConsider[j]), 2);
		}
		MyUtils.println("------", 1);

		Date d2 = new Date();
		initialExtraTime__readingGraphBugsAndProjectsInfo = (float)(d2.getTime()-d1.getTime())/1000;

		if (wrapOutputInLines)
			MyUtils.println("-----------------------------------", indentationLevel+1);
		MyUtils.println(MyUtils.concatTwoWriteMessageSteps(writeMessageStep, "5- Reading needed assignment file(s):"), indentationLevel+1);
		MyUtils.println("Started ...", indentationLevel+2);

		//Defining an arrayList for all five types of assignments:
		ArrayList<TreeMap<String, ArrayList<String[]>>> projectsAndTheirAssignments__AL_forDifferentAssignmetTypes = new ArrayList<TreeMap<String, ArrayList<String[]>>>();
		for (int i=ASSIGNMENT_TYPE_TO_TRIAGE.T1_AUTHOR.ordinal(); i<=ASSIGNMENT_TYPE_TO_TRIAGE.T5_ALL_TYPES.ordinal(); i++){
			TreeMap<String, ArrayList<String[]>> projectsAndTheirAssignments;
			if (assignmentTypesToTriage[i] == YES){
				projectsAndTheirAssignments = TSVManipulations.readNonUniqueKeyAndItsValueFromTSV(
						inputPath, Constants.ASSIGNMENT_FILE_NAMES[i]+".tsv", localFMR, null, 
						0, SortOrder.DEFAULT_FOR_STRING, 7, "1$2$3", titlesToReturn_IS_NOT_NEEDED_AND_USED,
						LogicalOperation.NO_CONDITION, 
						0, ConditionType.NOTHING, "", FieldType.NOT_IMPORTANT, 
						0, ConditionType.NOTHING, "", FieldType.NOT_IMPORTANT, 
						wrapOutputInLines, showProgressInterval*1000, indentationLevel+2, Constants.THIS_IS_REAL, MyUtils.concatTwoWriteMessageSteps(writeMessageStep, "5-"+Integer.toString(i+1)+"- "+Constants.ASSIGNED_BUGS_TYPES__SHORT_DESCRIPTIONS[i]));
				totalFMR = MyUtils.addFileManipulationResults(totalFMR, localFMR);
			}
			else{
				projectsAndTheirAssignments = new TreeMap<String, ArrayList<String[]>>(); //creating an empty TreeMap, just to add to projectsAndTheirAssignments_AL. It is because we want to use static indexes for each type later in readNonAssignmentEvidence. Also in the main loop of assignment, when we iterate over assignments of a specific type.
				MyUtils.println(MyUtils.concatTwoWriteMessageSteps(writeMessageStep, "5-"+Integer.toString(i+1)+"- \""+Constants.ASSIGNED_BUGS_TYPES__SHORT_DESCRIPTIONS[i]+"\" --> is not configured to use or run the prediction algorithm on."), indentationLevel+2);
			}
			//Now, add the created "projectsAndTheirAssignments" (which either is containing assignment information or not [based on assignmentTypesToTriage values]) to projectsAndTheirAssignments_AL:
			projectsAndTheirAssignments__AL_forDifferentAssignmetTypes.add(projectsAndTheirAssignments);
		}
		MyUtils.println("Finished.", indentationLevel+2);
		if (wrapOutputInLines)
			MyUtils.println("-----------------------------------", indentationLevel+1);

		String nextStep = "6";
		HashMap<String, HashSet<String>> projectsAndTheirSharedUsersWithSubProjects = null; 
		if (runType == RUN_TYPE.RUN_FOR_RAILS_AND_ANGULAR_AND_THEIR_FAMILIES__JUST_RUN_ON_SHARED_USERS_WITH_SUB_PROJECTS){
			projectsAndTheirSharedUsersWithSubProjects = AlgPrep.readProjectsAndTheirUsers(inputPath, "12-sharedDevelopersWithSubProjects.tsv", "Reading shared users with subProjects.",
					localFMR,
					wrapOutputInLines, showProgressInterval, indentationLevel+1, testOrReal, nextStep);
			totalFMR = MyUtils.addFileManipulationResults(totalFMR, localFMR);
			nextStep = "7";
		}
		HashMap<String, HashSet<String>> projectsAndTheirSharedUsersWithSO = null; 
		if (runType_SO != RUN_TYPE_FOR_SO_CONTRIBUTIONS.DO_NOT_CONSIDER_SO){
			projectsAndTheirSharedUsersWithSO = AlgPrep.readProjectsAndTheirUsersWhoAreAlsoInSO(inputPath, "13-sharedDevelopersWithSO.tsv", "Reading shared users with SO.",
					localFMR,
					wrapOutputInLines, showProgressInterval, indentationLevel, testOrReal, nextStep);
			totalFMR = MyUtils.addFileManipulationResults(totalFMR, localFMR);
			if (nextStep.equals("6"))
				nextStep = "7";
			else
				nextStep = "8";
		}
		
		String detailedAssignmentResultsSubfolderName = AlgPrep.createFolderForResults(outputPath+"\\"+Constants.ASSIGNMENT_RESULTS_OVERAL_FOLDER_NAME, experimentTitle, runType, runType_SO, localFMR, indentationLevel+1);
		totalFMR = MyUtils.addFileManipulationResults(totalFMR, localFMR);
		Date d3 = new Date();

		if (totalFMR.errors > 0)
			MyUtils.println("There are errors! Stopping ...", indentationLevel+3);
		else{
			initialExtraTime__readingAssignments_and_readingAndIndexingNonAssignmentEvidence = (float)(d3.getTime()-d2.getTime())/1000;
			if (wrapOutputInLines)
				MyUtils.println("-----------------------------------", indentationLevel+1);
			int TOTAL_NUMBER_OF_ASSIGNMENT_TYPES_TO_TRIAGE = 0;
			for (int j=ASSIGNMENT_TYPE_TO_TRIAGE.T1_AUTHOR.ordinal(); j<=ASSIGNMENT_TYPE_TO_TRIAGE.T5_ALL_TYPES.ordinal(); j++)
				if (assignmentTypesToTriage[j] == YES)
					TOTAL_NUMBER_OF_ASSIGNMENT_TYPES_TO_TRIAGE++;
			MyUtils.println(MyUtils.concatTwoWriteMessageSteps(writeMessageStep, nextStep+"- Running prediction algorithm for " + TOTAL_NUMBER_OF_ASSIGNMENT_TYPES_TO_TRIAGE + " assignment file(s):"), indentationLevel+1);
			MyUtils.println("Started ...", indentationLevel+2);

			for (int i=ASSIGNMENT_TYPE_TO_TRIAGE.T1_AUTHOR.ordinal(); i<=ASSIGNMENT_TYPE_TO_TRIAGE.T5_ALL_TYPES.ordinal(); i++){
				String step = MyUtils.concatTwoWriteMessageSteps(writeMessageStep, nextStep+"-"+(i+1));
				if (assignmentTypesToTriage[i] == YES){
					Date d4 = new Date();
					if (wrapOutputInLines)
						MyUtils.println("-----------------------------------", indentationLevel+2);
					MyUtils.println(step+"- Predicting \""+Constants.ASSIGNED_BUGS_TYPES__SHORT_DESCRIPTIONS[i]+"\" assignments:", indentationLevel+2);
					MyUtils.println("Started ...", indentationLevel+3);

					if (evidenceTypes[0] == YES) //: assignedBug
						evidenceTypesToConsider[0] = Constants.EVIDENCE_TYPE[i]; //: this is the 'assignment' evidence type (can be 0 to 4). 
					if (evidenceTypes[6] == YES) //: assignedBug
						evidenceTypesToConsider[indexOfMultiProject_assignedBug] = Constants.EVIDENCE_TYPE[10+i]; //: this is the 'assignment' evidence type (can be 0 to 4). 
					
					
					TreeMap<String, ArrayList<String[]>> projectsAndTheirCommunities = TSVManipulations.readNonUniqueKeyAndItsValueFromTSV(
							inputPath, Constants.COMMUNITY_FILE_NAMES[i]+".tsv", localFMR, null, 
							0, SortOrder.DEFAULT_FOR_STRING, 2, "1", titlesToReturn_IS_NOT_NEEDED_AND_USED,
							LogicalOperation.NO_CONDITION, 
							0, ConditionType.NOTHING, "", FieldType.NOT_IMPORTANT, 
							0, ConditionType.NOTHING, "", FieldType.NOT_IMPORTANT, 
							wrapOutputInLines, showProgressInterval*1000, indentationLevel+3, Constants.THIS_IS_REAL, step+"-1");
					//					wrapOutputInLines, showProgressInterval*1000, indentationLevel+3, Constants.THIS_IS_A_TEST, step+"-2");
					totalFMR = MyUtils.addFileManipulationResults(totalFMR, localFMR);

					//First, converting the community of projects to a proper format to be used in some of the used searches more quickly:
					HashMap<String, HashSet<String>> projectsAndTheirCommunities_HM = new HashMap<String, HashSet<String>>();
					for (Map.Entry<String, ArrayList<String[]>> entry: projectsAndTheirCommunities.entrySet()){
						String pId = entry.getKey();
						ArrayList<String[]> logins_AL_SA = entry.getValue();
						for (String[] login_SA: logins_AL_SA){
							if (projectsAndTheirCommunities_HM.containsKey(pId)){
								HashSet<String> communityOfAProject = projectsAndTheirCommunities_HM.get(pId);
								communityOfAProject.add(login_SA[0]);
							}
							else{
								HashSet<String> communityOfAProject = new HashSet<String>();
								communityOfAProject.add(login_SA[0]);
								projectsAndTheirCommunities_HM.put(pId, communityOfAProject);
							}
						}
					}

					//Filter assignments if needed (if needed to run just on shared users between main project and its subProjects. Similarly for the SO case):
					TreeMap<String, ArrayList<String[]>> projectsAndTheirAssignments = projectsAndTheirAssignments__AL_forDifferentAssignmetTypes.get(i);
					if (runType == RUN_TYPE.RUN_FOR_RAILS_AND_ANGULAR_AND_THEIR_FAMILIES__JUST_RUN_ON_SHARED_USERS_WITH_SUB_PROJECTS)
						AlgPrep.filterAssignmentsTo_sharedUsers(projectsAndTheirAssignments, projectsAndTheirSharedUsersWithSubProjects, indentationLevel+4);
					if (runType_SO == RUN_TYPE_FOR_SO_CONTRIBUTIONS.CONSIDER_SO_AND_FILTER_ON_ASSIGNMENTS_ASSIGNED_TO_SHARED_USERS_BETWEEN_SO_AND_GH)
						AlgPrep.filterAssignmentsTo_sharedUsers(projectsAndTheirAssignments, projectsAndTheirSharedUsersWithSO, indentationLevel+4);
					
					//Moved here since we need communities for sub-projects:
					HashMap<String, HashMap<String, HashMap<String, HashMap<Integer, ArrayList<Evidence>>>>> projectId_Login_Tags_TypesAndTheirEvidence =
							new HashMap<String, HashMap<String, HashMap<String, HashMap<Integer, ArrayList<Evidence>>>>>();
					AlgPrep.readAndIndexNonAssignmentEvidence(inputPath,  
							"2-commits.tsv", "3-PRs.tsv", "4-bugComments.tsv", "5-commitComments.tsv", "6-PRComments.tsv", "14-SOPostsOfRailsOrAngular.tsv", 
							runType, projectsAndTheirAssignments__AL_forDifferentAssignmetTypes, assignmentTypesToTriage, 
							projectsAndTheirSharedUsersWithSO, 
							localFMR, ///*since the second param of this method is empty, the next param is null and won't be considered:*/
							evidenceTypes, 
							projects, null, projectsAndTheirCommunities_HM, 
							projectId_Login_Tags_TypesAndTheirEvidence, 
							graph, 
							option1_whatToAddToAllBugs, option2_w, option3_TF, option4_IDF, option5_prioritizePAs, 
							option6_whatToAddToAllCommits, option7_whenToCountTextLength, 
							option9_whatToAddToAllPRs, option10_whatToAddToAllBugComments, 
							option11_whatToAddToAllCommitComments, option12_whatToAddToAllPRComments, 
							wrapOutputInLines, showProgressInterval*50, indentationLevel+3, MyUtils.concatTwoWriteMessageSteps(writeMessageStep, step+"-2"));
					totalFMR = MyUtils.addFileManipulationResults(totalFMR, localFMR);
					//.//
					
					Date d5 = new Date();
					loopExtraTime_readingCommunitiesAndNonAsssignmentEvidence = loopExtraTime_readingCommunitiesAndNonAsssignmentEvidence + (float)(d5.getTime()-d4.getTime())/1000;

					if (evidenceTypes[0] == YES || evidenceTypes[6] == YES){ //: assignedBug or multiProj_assignedBug
						AlgPrep.indexAssignmentEvidence(i, runType, 
								evidenceTypes, thresholdForNumberOfAssignmentsInAProject, 
								projectsAndTheirAssignments__AL_forDifferentAssignmetTypes, assignmentTypesToTriage,
								projects, projectIdBugNumberAndTheirBugInfo, projectId_Login_Tags_TypesAndTheirEvidence, 
								projectsAndTheirCommunities_HM, 
								graph, localFMR, 
								option1_whatToAddToAllBugs, option2_w, option3_TF, option4_IDF, option5_prioritizePAs, option6_whatToAddToAllCommits, option7_whenToCountTextLength, 
								wrapOutputInLines, showProgressInterval*100, indentationLevel+3, MyUtils.concatTwoWriteMessageSteps(writeMessageStep, step+"-3"));
						totalFMR = MyUtils.addFileManipulationResults(totalFMR, localFMR);
						if (totalFMR.errors > 0){
							MyUtils.println("There are errors! Breaking ...", indentationLevel+3);
							break;
						}
					}

					Date d6 = new Date();
					loopExtraTime_readingAssignmentEvidenceIndexing = loopExtraTime_readingAssignmentEvidenceIndexing + (float)(d6.getTime()-d5.getTime())/1000;

					String subStep = step + "-4";
					if (wrapOutputInLines)
						MyUtils.println("-----------------------------------", indentationLevel+3);
					MyUtils.println(subStep+"- Assigning:", indentationLevel+3);
					MyUtils.println("Started ...", indentationLevel+4);

					TreeMap<String, ArrayList<AssignmentStat>> projectsAndTheirAssignmentStats = new TreeMap<String, ArrayList<AssignmentStat>>();
					TreeMap<String, String> projectNamesAndTheirIds_orderedByName = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
					int projectCounter = 1;
					Random random = new Random();			
					for (String projectId: projectsAndTheirAssignments.keySet()){
						//Considering assignments of one project:
						Date startOfProcessingOfOneProject = new Date();
						Project project = new Project(projects, projectId, indentationLevel+4, localFMR);
						totalFMR = MyUtils.addFileManipulationResults(totalFMR, localFMR);
						ArrayList<String[]> assignmentsOfThisProject = projectsAndTheirAssignments.get(projectId);
						if (AlgPrep.projectType(projectId, project.owner_repo) == ProjectType.FASE_13){
							projectNamesAndTheirIds_orderedByName.put(project.owner_repo, projectId);
							if (AlgPrep.shouldRunTheExperimentOnProject(runType, project, assignmentsOfThisProject, thresholdForNumberOfAssignmentsInAProject) && AlgPrep.additionalCriteriaForAssigningBugsInProject(runType, project)){
								if (wrapOutputInLines)
									MyUtils.println("-----------------------------------", indentationLevel+4);
								MyUtils.println(subStep+"-"+projectCounter+"- "+project.owner_repo+" (projectId: " + projectId + ")", indentationLevel+4);
								
								//Printing the number of developers assigned any bugs and the number of assignments and then continue! 
								//This is just for test purposes and will be commented in the main version:
//								HashSet<String> devs = new HashSet<String>();
//								for (int ii=0; ii<assignmentsOfThisProject.size(); ii++)
//									devs.add(assignmentsOfThisProject.get(ii)[2]);
//								MyUtils.println("#ofDevsWhoAreAsssignedAnyBug: " + devs.size(), indentationLevel+4);
//								MyUtils.println("#ofAssignments: " + assignmentsOfThisProject.size(), indentationLevel+4);
//								if (true)
//									continue;
								 
								ArrayList<String[]> community = projectsAndTheirCommunities.get(projectId);
								HashMap<String, HashMap<String, Integer>> realAssignees = new HashMap<String, HashMap<String, Integer>>(); //bugNumber --> {login --> rank}
								HashMap<String, HashMap<String, HashMap<Integer, ArrayList<Evidence>>>> logins_Tags_TypesAndTheirEvidence = projectId_Login_Tags_TypesAndTheirEvidence.get(projectId);

								HashMap<String, HashMap<String, ReferenceStatus>> bugNumber_developer_referenceStatus 
									= projectId_bugNumber_developer_referenceStatus.get(projectId);
								int numberOfBugsProcessed = 0;
								HashSet<String> previousAssigneesInThisProject = new HashSet<>();
								for (int j=0; j<assignmentsOfThisProject.size(); j++){ 
									Assignment a = new Assignment(assignmentsOfThisProject, j, indentationLevel+5);
									HashMap<String, Double> scores = new HashMap<String, Double>(); 

									Bug queryBug = new Bug(projectId, a.bugNumber, projectIdBugNumberAndTheirBugInfo, localFMR);
									totalFMR = MyUtils.addFileManipulationResults(totalFMR, localFMR);

									int[] originalNumberOfWordsInBugText_array = new int[1];
									String bugText = AlgPrep.getBugText(project, queryBug, originalNumberOfWordsInBugText_array, option1_whatToAddToAllBugs);
									int originalNumberOfWordsInBugText = originalNumberOfWordsInBugText_array[0];
									WordsAndCounts wAC = new WordsAndCounts(bugText, option7_whenToCountTextLength, originalNumberOfWordsInBugText);
									//
									if (wAC.size == 0)
										MyUtils.println("Warning: Empty bug text!", indentationLevel+5);
									//					
									HashMap<String, ReferenceStatus> developer_referenceStatus;
									if (bugNumber_developer_referenceStatus!= null && bugNumber_developer_referenceStatus.containsKey(queryBug.number))
										developer_referenceStatus = bugNumber_developer_referenceStatus.get(queryBug.number);
									else
										developer_referenceStatus = null;
									for (int k=0; k<community.size(); k++){
										String login = community.get(k)[0];
										ReferenceStatus referenceStatus = null;
										if (developer_referenceStatus != null)
											referenceStatus = developer_referenceStatus.get(login);
//										if (referenceStatus != null)
//											System.out.println("aa");
										scores.put(login, 
												AlgPrep.calculateScoreOfDeveloperForBugAssignment(
														login, a, graph, 
														i, evidenceTypesToConsider, evidenceTypesToConsider_count, 
														logins_Tags_TypesAndTheirEvidence, 
														previousAssigneesInThisProject, 
														wAC, originalNumberOfWordsInBugText, 
														j+1, 
														project.overalStartingDate, referenceStatus, 
														option2_w, option4_IDF, option5_prioritizePAs, option8_recency, 
														option13_referenceTypesToConsider, 
														indentationLevel+5));
									}
									//Adding this assignee to the set of assignees of this bug (will be used in measuring the accuracies):
									HashMap<String, Integer> previousAssigneesOfThisBugAndTheirRanks;
									if(realAssignees.containsKey(a.bugNumber)){//: in this case, we don't need to create a HashMap<String, Integer> as previousAssigneesOfThisBugAndTheirRanks. Just retrieve it and add the current assignee to its end, and it will be updated in the hashMap:
										previousAssigneesOfThisBugAndTheirRanks = realAssignees.get(a.bugNumber);
										for (String login: previousAssigneesOfThisBugAndTheirRanks.keySet()) // set all the ranks of the previous assignees of this bug to "unknown" because their rank for the previous assignments may be different from this new assignment (the rank will be calculated later):
											previousAssigneesOfThisBugAndTheirRanks.put(login, UNKNOWN_RANK); //: this rank will be calculated and updated later.
									}
									else{//: in this case, we need to create an HashMap<String, Integer> as previousAssigneesOfThisBugAndTheirRanks. Then put it to the realAssignees:
										previousAssigneesOfThisBugAndTheirRanks = new HashMap<String, Integer>(); 
										realAssignees.put(a.bugNumber, previousAssigneesOfThisBugAndTheirRanks);		
									}
									previousAssigneesOfThisBugAndTheirRanks.put(a.login, UNKNOWN_RANK);
									//Rank the list of all community members, then update the ranks of real assignees in realAssignees. Finally return the assignee with the best rank: 
									Assignee ra = AlgPrep.updateRankOfRealAssigneesAndReturnTheBestAssignee(realAssignees, a.bugNumber, scores, random);
									AlgPrep.updateSuccessfulNumberOfReferencesUpToNow(developer_referenceStatus, realAssignees.get(a.bugNumber), a.date, option13_referenceTypesToConsider);
									
									//Adding this assignment to projectsAndTheirAssignmentStats:
									AssignmentStat assignmentStat = new AssignmentStat(a.bugNumber, a.date, ra.login, ra.rank, realAssignees.get(a.bugNumber));
									ArrayList<AssignmentStat> assignmentStatsOfThisProject;
									if (projectsAndTheirAssignmentStats.containsKey(projectId)) //: means that we had assignmentStats for this project in the current loop before, so we just retrieve it:
										assignmentStatsOfThisProject = projectsAndTheirAssignmentStats.get(projectId);
									else//: means that this is the first bug assignment in this project, so we need to create ArrayList<AssignmentSummary> object:
										assignmentStatsOfThisProject = new ArrayList<AssignmentStat>();
									assignmentStatsOfThisProject.add(assignmentStat);
									projectsAndTheirAssignmentStats.put(projectId, assignmentStatsOfThisProject);

									numberOfBugsProcessed++;
									if (numberOfBugsProcessed % showProgressInterval == 0 && numberOfBugsProcessed > 0)
										MyUtils.println(Constants.integerFormatter.format(numberOfBugsProcessed) + " bug assignments ...", indentationLevel+5);
									if (testOrReal == Constants.THIS_IS_A_TEST)
										if (j >= testOrReal)
											break; //to consider only one project in the test mode.
									if (totalFMR.errors > 0){
										MyUtils.println("There are errors! Breaking ...", indentationLevel+3);
										break;
									}
									previousAssigneesInThisProject.add(a.login); 
								}
								MyUtils.println(Constants.integerFormatter.format(numberOfBugsProcessed) + " bug assignments predicted.", indentationLevel+5);

								projectCounter++;
								if (testOrReal == Constants.THIS_IS_A_TEST)
									break; //to consider only one project in the test mode.
								//					MyUtils.println("Finished.", indentationLevel+5);
								if (wrapOutputInLines)
									MyUtils.println("-----------------------------------", indentationLevel+4);
							}
						}
						Date endOfProcessingOfOneProject = new Date();
						float processingTimeOfOneProject = (float)(endOfProcessingOfOneProject.getTime()-startOfProcessingOfOneProject.getTime())/1000;
						MyUtils.println("Assignment time for this project: " + Constants.floatFormatter.format(processingTimeOfOneProject) + " seconds.", indentationLevel+5);
						startOfProcessingOfOneProject = endOfProcessingOfOneProject;
					}
					MyUtils.println("Finished.", indentationLevel+4);
					if (wrapOutputInLines)
						MyUtils.println("-----------------------------------", indentationLevel+3);

					Date d7 = new Date();
					float totalRunningTimeForThisAssignmentType_inTheLoop = (float)(d7.getTime()-d4.getTime())/1000;
					AlgPrep.writeAssignmentStats(outputPath, outputSummariesTSVFileName, Constants.ASSIGNMENT_RESULTS_OVERAL_FOLDER_NAME , detailedAssignmentResultsSubfolderName, Constants.ASSIGNED_BUGS_TYPES__SHORT_DESCRIPTIONS[i], 
							projectsAndTheirAssignmentStats, 
							projectNamesAndTheirIds_orderedByName, projectsAndTheirCommunities, 
							experimentDetails, 
							localFMR, 
							totalRunningTimeForThisAssignmentType_inTheLoop, 
							wrapOutputInLines, showProgressInterval*100, indentationLevel+3, step+"-5");
					totalFMR = MyUtils.addFileManipulationResults(totalFMR, localFMR);
					if (totalFMR.errors > 0){
						MyUtils.println("There are errors! Breaking ...", indentationLevel+3);
						break;
					}
					MyUtils.println("Finished.", indentationLevel+3);
					if (wrapOutputInLines)
						MyUtils.println("-----------------------------------", indentationLevel+2);
				}
				else
					MyUtils.println(step+"- \""+Constants.ASSIGNED_BUGS_TYPES__SHORT_DESCRIPTIONS[i]+"\" --> is not configured to run the prediction algorithm on.", indentationLevel+2);
			}//for (i.

			if (totalFMR.errors == 0)
				MyUtils.println("Finished.", indentationLevel+2);
			else
				MyUtils.println("There are errors! Process stopped!", indentationLevel+3);
			MyUtils.println("Finished.", indentationLevel+2);
			if (wrapOutputInLines)
				MyUtils.println("-----------------------------------", indentationLevel+1);
		}

		Date d8 = new Date();
		float totalLoopTime = (float)(d8.getTime()-d3.getTime())/1000;
		float netAssignmentTime = (float)(d8.getTime()-d3.getTime())/1000 -loopExtraTime_readingCommunitiesAndNonAsssignmentEvidence-loopExtraTime_readingAssignmentEvidenceIndexing;

		if (totalFMR.errors == 0)
			MyUtils.println("Finished.", indentationLevel+1);
		else{
			MyUtils.println("Finished with " + totalFMR.errors + " critical errors handling i/o files.", indentationLevel+1);
			MyUtils.println("ERRORS! ERRORS! ERRORS!", indentationLevel+1);
		}
		MyUtils.println("-----------------------------------", indentationLevel);
		if (totalFMR.errors == 0)
			System.out.println("Summary (time, etc.):");
		else{
			System.out.println("Summary (time, etc.), ignoring the above ERRORs:");
			fMR.errors = totalFMR.errors;
		}
		MyUtils.println("Max frequency of words in an evidence:" + AlgPrep.maxFreqOfAWordInAnEvidence + " (just FYI).", indentationLevel);
		MyUtils.println("Total time: " + Constants.floatFormatter.format((float)(d8.getTime()-d1.getTime())/1000) + " seconds.", indentationLevel);
		MyUtils.println("Initial time (Reading graph, bug and project info, before the loop): " + Constants.floatFormatter.format(initialExtraTime__readingGraphBugsAndProjectsInfo), indentationLevel+1);
		MyUtils.println("Initial time (Reading assignments, and, reading and indexing non-assignment evidence, before the loop): " + Constants.floatFormatter.format(initialExtraTime__readingAssignments_and_readingAndIndexingNonAssignmentEvidence), indentationLevel+1);
		MyUtils.println("Whole loop time: " + Constants.floatFormatter.format(totalLoopTime) + " seconds.", indentationLevel+1);
		MyUtils.println("Reading communities files (in the loop): " + Constants.floatFormatter.format(loopExtraTime_readingCommunitiesAndNonAsssignmentEvidence), indentationLevel+2);
		MyUtils.println("Reading bugs and indexing extra time (in the loop): " + Constants.floatFormatter.format(loopExtraTime_readingAssignmentEvidenceIndexing), indentationLevel+2);
		MyUtils.println("Net assignment time (in the loop): " + Constants.floatFormatter.format(netAssignmentTime) + " seconds.", indentationLevel+2);
		MyUtils.println("-----------------------------------", indentationLevel);
		MyUtils.println("-----------------------------------", indentationLevel);
	}
	
	//------------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------------
	public static boolean needToConsiderET(int[] evidenceTypes, int evidenceType){
		if (evidenceType == Constants.EVIDENCE_TYPE_BUG)
			if (evidenceTypes[0] ==1) //assignedBug
				return true;
			else
				return false;
		if (evidenceType == Constants.EVIDENCE_TYPE__MULTI_PROJ__BUG)
			if (evidenceTypes[6] ==1) //MultiProject-->assignedBug=b
				return true;
			else
				return false;
		else
			if (evidenceType >= Constants.EVIDENCE_TYPE_COMMIT && evidenceType <= Constants.EVIDENCE_TYPE_PR_COMMENT)
				if (evidenceTypes[evidenceType-10] ==1)
					return true;
				else
					return false;
			else
				if (evidenceType >= Constants.EVIDENCE_TYPE__MULTI_PROJ__COMMIT && evidenceType <= Constants.EVIDENCE_TYPE__MULTI_PROJ__PR_COMMENT)
					if (evidenceTypes[evidenceType-24] ==1)
						return true;
					else
						return false;
				else
					if (evidenceType >= Constants.EVIDENCE_TYPE_SO_QUESTION && evidenceType <= Constants.EVIDENCE_TYPE_SO_ANSWER)
						if (evidenceTypes[evidenceType-24] ==1)
							return true;
						else
							return false;
					else
						return false; //This shouldn't happen. Just added for ignoring the error.
	}
	//------------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------------
	public static void experiment(){
		int[] assignmentTypesToTriage = new int[]{ //At least one of these items should be equal to YES:
				0/*1=YES, 0=NO*//*T1_AUTHOR*/, 
				0/*1=YES, 0=NO*//*T2_COAUTHOR*/, 
				0/*1=YES, 0=NO*//*T3_ADMIN_CLOSER*/, 
				0/*1=YES, 0=NO*//*T4_DRAFTED_A*/, 
				1/*1=YES, 0=NO*//*T5_ALL_TYPES*/, 
				}; 

		//Evidence types to consider: [bugAssignment, commit, PR, bugComment, commitComment, PRComment]
		int[] evidenceTypes = new int[]{
				1/*1=YES, 0=NO*//*assignedBug=b*/, 
				1/*1=YES, 0=NO*//*commit=c*/, 
				1/*1=YES, 0=NO*//*PR=p*/, 
				1/*1=YES, 0=NO*//*bugComment=bC*/, 
				1/*1=YES, 0=NO*//*commitComment=cC*/, 
				0/*1=YES, 0=NO*//*PRComment=pC*/,
				//just for evidence from other projects; runType = RUN_FOR_RAILS_AND_ANGULAR_AND_THEIR_FAMILIES	or RUN_FOR_RAILS_AND_ANGULAR_AND_THEIR_FAMILIES__WITH_SO_CONTRIBUTIONS
				0/*1=YES, 0=NO*//*MultiProject-->assignedBug=b*/, 
				0/*1=YES, 0=NO*//*MultiProject-->commit=c*/, 
				0/*1=YES, 0=NO*//*MultiProject-->PR=p*/, 
				0/*1=YES, 0=NO*//*MultiProject-->bugComment=bC*/, 
				0/*1=YES, 0=NO*//*MultiProject-->commitComment=cC*/, 
				0/*1=YES, 0=NO*//*MultiProject-->PRComment=pC*/,
				//SO:
				0/*1=YES, 0=NO*//*MultiProject-->SO Answers = SOA*/,
				0/*1=YES, 0=NO*//*MultiProject-->SO Questions = SOQ*/
				}; 
		int totalEvidenceTypes_count = 14; //above 4 cases.
//		int[] otherAssignmentOptionsToConsider = new int[]{
//				0, //Constants.OTHER_ASSIGNMENT_OPTIONS__CONCATENATE_PROJECT_TITLE_AND_DESCRIPTION_TO_THE_BUG 		--> concatenate "project title and description" to the bug and assignedBugEvidence
//				0, //Constants.OTHER_ASSIGNMENT_OPTIONS__CONCATENATE_MAIN_LANGUAGES_TO_THE_BUG 						--> concatenate "project main language" to the bug and assignedBugEvidence
//				0, //Constants.OTHER_ASSIGNMENT_OPTIONS__CONCATENATE_PROJECT_TITLE_AND_DESCRIPTION_TO_THE_COMMIT	--> concatenate "project title and description" to the commit evidence
//				0  //Constants.OTHER_ASSIGNMENT_OPTIONS__CONCATENATE_MAIN_LANGUAGES_TO_THE_THE_COMMIT				--> concatenate "project main language" to the commit evidence
//				}; 
		
		int[] option13_referenceTypesToConsider = new int[]{//No initialization is needed at this time. Later, by calling convertToBinaryRepresentation(), we set the items of this array.  
			//index 0 is the least valuable digit.
			0, /*1=YES, 0=NO*/  /*Whether to use Constants.REFERENCE_TYPE.BUG or not*/
			0, /*1=YES, 0=NO*/  /*Whether to use Constants.REFERENCE_TYPE.BUG_COMMENT or not*/
			0, /*1=YES, 0=NO*/  /*Whether to use Constants.REFERENCE_TYPE.COMMIT or not*/
			0  /*1=YES, 0=NO*/  /*Whether to use Constants.REFERENCE_TYPE.COMMIT_COMMENT or not*/
		}; //Later, by calling convertToBinaryRepresentation(), we set the items of this array.
//		boolean isMainRun; 
//		isMainRun = true; //: means that we are running the code for all projects.
//		isMainRun = false; //: means that we are running the code for only three test projects ("adobe/brackets", "fog/fog" and "lift/framework").
		
		RUN_TYPE runType;
		int thresholdForNumberOfAssignmentsInAProject = Constants.THIS_WILL_NOT_BE_USED;
		runType = RUN_TYPE.RUN_FOR_ALL_PROJECTS;
//		runType = RUN_TYPE.RUN_FOR_TUNING_PROJECTS; //: in this case, the tuning projects are defined in: AlgPrep.isAProjectWhichIsUsedForTuning().
//		runType = RUN_TYPE.RUN_FOR_SELECTIVE_PROJECTS; //: in this case, the selected projects are defined in: AlgPrep.isASelectedProjectToRunTheExperimentOn().
//		runType = RUN_TYPE.RUN_FOR_PROJECTS_WITH_NUMBER_OF_ASSIGNMENTS_MORE_THAN_A_THRESHOLD; //: this type needs the threshold (thresholdForNumberOfAssignmentsInAProject) to be initiated as follows:
//		thresholdForNumberOfAssignmentsInAProject = 50;
//		runType = RUN_TYPE.RUN_FOR_ANGULAR__FOR_TUNING;
//		runType = RUN_TYPE.RUN_FOR_ANGULAR_AND_ITS_FAMILY__FOR_TUNING;
//		runType = RUN_TYPE.RUN_FOR_RAILS_AND_ANGULAR_AND_THEIR_FAMILIES;
//		runType = RUN_TYPE.RUN_FOR_RAILS_AND_ANGULAR_AND_THEIR_FAMILIES__JUST_RUN_ON_SHARED_USERS_WITH_SUB_PROJECTS;
		
		RUN_TYPE_FOR_SO_CONTRIBUTIONS runType_SO = utils.Constants.RUN_TYPE_FOR_SO_CONTRIBUTIONS.DO_NOT_CONSIDER_SO;
//		RUN_TYPE_FOR_SO_CONTRIBUTIONS runType_SO = utils.Constants.RUN_TYPE_FOR_SO_CONTRIBUTIONS.CONSIDER_SO_BUT_DO_NOT_FILTER_ASSIGNMENTS;
//		RUN_TYPE_FOR_SO_CONTRIBUTIONS runType_SO = utils.Constants.RUN_TYPE_FOR_SO_CONTRIBUTIONS.CONSIDER_SO_AND_FILTER_ON_ASSIGNMENTS_ASSIGNED_TO_SHARED_USERS_BETWEEN_SO_AND_GH;
		
//for (int num=0; num<2; num++)
		for (BTOption1_whatToAddToAllBugs option1_whatToAddToAllBugs: BTOption1_whatToAddToAllBugs.values()){//: What to be added to the bugs by default.
			if (option1_whatToAddToAllBugs != BTOption1_whatToAddToAllBugs.ADD_ML)//In our previous experiment (results stored in Old4-DecidingAbout4Options folder) it was shown that this (BTOption1.ADD_mL) has the best performance.
				continue;
			//We need to test just these two cases:
//			if (option1_whatToAddToAllBugs != BTOption1_whatToAddToAllBugs.JUST_USE_BUG_TD && option1_whatToAddToAllBugs != BTOption1_whatToAddToAllBugs.ADD_ML)
//				continue;
			for (BTOption2_w option2_w: BTOption2_w.values()){//: Term weighting
				if (option2_w != BTOption2_w.USE_TERM_WEIGHTING)
					continue;
//				if (option2_w != BTOption2_w.NO_TERM_WEIGHTING)
//					continue;
				for (BTOption3_TF option3_TF: BTOption3_TF.values()){//: TF formula.
//					if (option3_TF != BTOption3_TF.LOG_BASED) //In our previous experiment (results stored in Old4-DecidingAbout4Options folder) it was shown that this (BTOption3.LOG_BASED) has the best performance.
//						continue;
//					if (option3_TF != BTOption3_TF.ONE) //In our previous experiment (results stored in Old4-DecidingAbout4Options folder) it was shown that this (BTOption3.LOG_BASED) has the best performance.
//						continue;
					if (option3_TF != BTOption3_TF.FREQ__TOTAL_NUMBER_OF_TERMS) //In our previous experiment (results stored in Old4-DecidingAbout4Options folder) it was shown that this (BTOption3.LOG_BASED) has the best performance.
						continue;
					for (BTOption4_IDF option4_IDF: BTOption4_IDF.values()){//: IDF formula.
						if (option4_IDF != BTOption4_IDF.FREQ) //In our previous experiment (results stored in Old4-DecidingAbout4Options folder) it was shown that this (BTOption4.FREQ) has the best performance.
							continue;
//						if (option4_IDF != BTOption4_IDF.ONE) //In our previous experiment (results stored in Old4-DecidingAbout4Options folder) it was shown that this (BTOption4.FREQ) has the best performance.
//							continue;
						for (BTOption5_prioritizePAs option5_prioritizePAs: BTOption5_prioritizePAs.values()){//: Prioritize previous assignees.
							if (option5_prioritizePAs != BTOption5_prioritizePAs.PRIORITY_FOR_PREVIOUS_ASSIGNEES)
								continue;
//							if (option5_prioritizePAs != BTOption5_prioritizePAs.NO_PRIORITY)
//								continue;
							for (BTOption6_whatToAddToAllCommits option6_whatToAddToAllCommits: BTOption6_whatToAddToAllCommits.values()){//: What to be added to the commits by default.
								if (option6_whatToAddToAllCommits != BTOption6_whatToAddToAllCommits.JUST_USE_COMMIT_M)
									continue; 
//								if (option6_whatToAddToAllCommits != BTOption6_whatToAddToAllCommits.ADD_mL)
//									continue; 
								for (BTOption7_whenToCountTextLength option7_whenToCountTextLength: BTOption7_whenToCountTextLength.values()){//: Text length before/after non-SO terms removal.
//									if (option7_whenToCountTextLength != BTOption7_whenToCountTextLength.USE_TEXT_LENGTH_AFTER_REMOVING_NON_SO_TAGS)
//										continue; 
									if (option7_whenToCountTextLength != BTOption7_whenToCountTextLength.USE_TEXT_LENGTH_BEFORE_REMOVING_NON_SO_TAGS)
										continue; 
									//The two cases of option7_whenToCountTextLength are used (and may make different results) only when at least one of option3_TF or option4_IDF above are set to their FREQ__TOTAL_NUMBER_OF_TERMS value. So we jump if that's not the case.
//									if (option7_whenToCountTextLength == BTOption7_whenToCountTextLength.USE_TEXT_LENGTH_BEFORE_REMOVING_NON_SO_TAGS 
//										&& option3_TF!=BTOption3_TF.FREQ__TOTAL_NUMBER_OF_TERMS 
//										&& option4_IDF!=BTOption4_IDF.FREQ__TOTAL_NUMBER_OF_TERMS)
//										continue; 
									for (BTOption8_recency option8_recency: BTOption8_recency.values()){
										//"bTD": bugTitleDescription		"pTD: projectTitleDescription		"mL": mainLanguages
//										if (option8_recency != BTOption8_recency.NO_RECENCY)
//											continue;
										if (option8_recency != BTOption8_recency.RECENCY2)
											continue;
										for (BTOption9_whatToAddToAllPRs option9_whatToAddToAllPRs: BTOption9_whatToAddToAllPRs.values()){
											if (option9_whatToAddToAllPRs != BTOption9_whatToAddToAllPRs.JUST_USE_PR_TEXT)
												continue;
//											if (option9_whatToAddToAllPRs != BTOption9_whatToAddToAllPRs.ADD_mL)
//												continue;
											for (BTOption10_whatToAddToAllBCs option10_whatToAddToAllBugComments: BTOption10_whatToAddToAllBCs.values()){
												if (option10_whatToAddToAllBugComments != BTOption10_whatToAddToAllBCs.JUST_USE_BC_TEXT)
													continue;
//												if (option10_whatToAddToAllBugComments != BTOption10_whatToAddToAllBugComments.ADD_mL)
//													continue;
												for (BTOption11_whatToAddToAllCCs option11_whatToAddToAllCommitComments: BTOption11_whatToAddToAllCCs.values()){
													if (option11_whatToAddToAllCommitComments != BTOption11_whatToAddToAllCCs.JUST_USE_CC_TEXT)
														continue;
//													if (option11_whatToAddToAllCommitComments != BTOption11_whatToAddToAllCCs.ADD_mL)
//														continue;
													for (BTOption12_whatToAddToAllPRCs option12_whatToAddToAllPRComments: BTOption12_whatToAddToAllPRCs.values()){
														if (option12_whatToAddToAllPRComments != BTOption12_whatToAddToAllPRCs.JUST_USE_PRC_TEXT)
															continue;
//														if (option12_whatToAddToAllPRComments != BTOption12_whatToAddToAllPRCs.ADD_mL)
//															continue;
													for (int option13_counter=0; option13_counter<16; option13_counter++){ 
														if (option13_counter != 11)//3 = 0011 = <COMMIT_COMMENT=0, COMMIT=0, BUG_COMMENT=1. BUG=1>
															continue;
//														if (option13_counter != 0)//3 = 0011 = <COMMIT_COMMENT=0, COMMIT=0, BUG_COMMENT=1. BUG=1>
//															continue;
														String methodology = ""; 
																	switch (option2_w){//: Term weighting:
																		case NO_TERM_WEIGHTING:		methodology = "noW";	break;		case USE_TERM_WEIGHTING:	methodology = "w__";	break;
																	}
																	switch (option3_TF){//: TF formula:
																		case ONE:		methodology = methodology + "+" + "TF_one";		break;		case FREQ:		methodology = methodology + "+" + "TF_fre";		break;		case FREQ__TOTAL_NUMBER_OF_TERMS:		methodology = methodology + "+" + "TF_F_T";		break; //TF_freq_numOfTerms
																		case LOG_BASED:		methodology = methodology + "+" + "TF_Log";		break;
																	}
																	switch (option4_IDF){//: IDF formula:
																		case ONE:		methodology = methodology + "+" + "IDF_one";	break;		case FREQ:		methodology = methodology + "+" + "IDF_fre";	break;		case FREQ__TOTAL_NUMBER_OF_TERMS:		methodology = methodology + "+" + "IDF_F_T";	break;		case LOG_BASED:		methodology = methodology + "+" + "IDF_log";	break;
																	}
																	switch (option5_prioritizePAs){
																		case NO_PRIORITY:		methodology = methodology + "+" + "noP";	break;		case PRIORITY_FOR_PREVIOUS_ASSIGNEES:		methodology = methodology + "+" + "pri";	break;
																	}
																	switch (option7_whenToCountTextLength){
																		case USE_TEXT_LENGTH_BEFORE_REMOVING_NON_SO_TAGS:		methodology = methodology + "+" + "tL_b"; 	break; //tL: text Length     b:before removing SO tags
																		case USE_TEXT_LENGTH_AFTER_REMOVING_NON_SO_TAGS:		methodology = methodology + "+" + "tL_a"; 	break; //textLength: afterRemSOTags
																	}
																	switch (option8_recency){
																		case NO_RECENCY:	methodology = methodology + "+" + "nR"; 	break; //nR: noRecency
																		case RECENCY1:		methodology = methodology + "+" + "r1"; 	break; //r1: recency1
																		case RECENCY2:		methodology = methodology + "+" + "r2"; 	break; //r2: recency2
																	}
																	MyUtils.convertToBinaryRepresentation(option13_counter, option13_referenceTypesToConsider, Constants.MAX_NUMBER_OF_REFERENCE_TYPES);
																	methodology = methodology + "+" + "ref_" + MyUtils.binaryArrayToString(option13_referenceTypesToConsider, Constants.MAX_NUMBER_OF_REFERENCE_TYPES);
													
											String assignedBugAndUsedBugAsEvidence_Text = "bTD";
											if (needToConsiderET(evidenceTypes, Constants.EVIDENCE_TYPE_BUG)){
												if (option1_whatToAddToAllBugs == BTOption1_whatToAddToAllBugs.ADD_PTD || option1_whatToAddToAllBugs == BTOption1_whatToAddToAllBugs.ADD_PTD_ML)
													assignedBugAndUsedBugAsEvidence_Text = assignedBugAndUsedBugAsEvidence_Text + "pTD";
												if (option1_whatToAddToAllBugs == BTOption1_whatToAddToAllBugs.ADD_ML || option1_whatToAddToAllBugs == BTOption1_whatToAddToAllBugs.ADD_PTD_ML)
													assignedBugAndUsedBugAsEvidence_Text = assignedBugAndUsedBugAsEvidence_Text + "mL";
											}
											String usedCommitsEvidence_Text = "c";
											if (needToConsiderET(evidenceTypes, Constants.EVIDENCE_TYPE_COMMIT)){
												if (option6_whatToAddToAllCommits == BTOption6_whatToAddToAllCommits.ADD_PTD || option6_whatToAddToAllCommits == BTOption6_whatToAddToAllCommits.ADD_PTD_mL)
													usedCommitsEvidence_Text = usedCommitsEvidence_Text + "pTD";
												if (option6_whatToAddToAllCommits == BTOption6_whatToAddToAllCommits.ADD_mL || option6_whatToAddToAllCommits == BTOption6_whatToAddToAllCommits.ADD_PTD_mL)
													usedCommitsEvidence_Text = usedCommitsEvidence_Text + "mL";
											} 
											String usedPRsEvidence_Text = "p"; //"p": pullRequest
											if (needToConsiderET(evidenceTypes, Constants.EVIDENCE_TYPE_PR)){
												if (option9_whatToAddToAllPRs == BTOption9_whatToAddToAllPRs.ADD_mL)
													usedPRsEvidence_Text = usedPRsEvidence_Text + "mL";
											} 
											String usedBCsEvidence_Text = "bC"; //"bC": bugComment
											if (needToConsiderET(evidenceTypes, Constants.EVIDENCE_TYPE_BUG_COMMENT)){
												if (option10_whatToAddToAllBugComments == BTOption10_whatToAddToAllBCs.ADD_mL)
													usedBCsEvidence_Text = usedBCsEvidence_Text + "mL";
											} 
											String usedCCsEvidence_Text = "cC"; //"cC": commitComment
											if (needToConsiderET(evidenceTypes, Constants.EVIDENCE_TYPE_COMMIT_COMMENT)){
												if (option11_whatToAddToAllCommitComments == BTOption11_whatToAddToAllCCs.ADD_mL)
													usedCCsEvidence_Text = usedCCsEvidence_Text + "mL";
											} 
											String usedPCsEvidence_Text = "pC"; //"pC": pull request Comment
											if (needToConsiderET(evidenceTypes, Constants.EVIDENCE_TYPE_PR_COMMENT)){
												if (option12_whatToAddToAllPRComments == BTOption12_whatToAddToAllPRCs.ADD_mL)
													usedCCsEvidence_Text = usedCCsEvidence_Text + "mL";
											} 
											
											//just for evidence from other projects; runType = RUN_FOR_RAILS_AND_ANGULAR_AND_THEIR_FAMILIES	or RUN_FOR_RAILS_AND_ANGULAR_AND_THEIR_FAMILIES__WITH_SO_CONTRIBUTIONS
											String multiProject_assignedBugAndUsedBugAsEvidence_Text = "mp_bTD";//sp:subProject
											if (needToConsiderET(evidenceTypes, Constants.EVIDENCE_TYPE__MULTI_PROJ__BUG)){
												if (option1_whatToAddToAllBugs == BTOption1_whatToAddToAllBugs.ADD_PTD || option1_whatToAddToAllBugs == BTOption1_whatToAddToAllBugs.ADD_PTD_ML)
													multiProject_assignedBugAndUsedBugAsEvidence_Text = multiProject_assignedBugAndUsedBugAsEvidence_Text + "pTD";
												if (option1_whatToAddToAllBugs == BTOption1_whatToAddToAllBugs.ADD_ML || option1_whatToAddToAllBugs == BTOption1_whatToAddToAllBugs.ADD_PTD_ML)
													multiProject_assignedBugAndUsedBugAsEvidence_Text = multiProject_assignedBugAndUsedBugAsEvidence_Text + "mL";
											}
											String multiProject_usedCommitsEvidence_Text = "mp_c";
											if (needToConsiderET(evidenceTypes, Constants.EVIDENCE_TYPE__MULTI_PROJ__COMMIT)){
												if (option6_whatToAddToAllCommits == BTOption6_whatToAddToAllCommits.ADD_PTD || option6_whatToAddToAllCommits == BTOption6_whatToAddToAllCommits.ADD_PTD_mL)
													multiProject_usedCommitsEvidence_Text = multiProject_usedCommitsEvidence_Text + "pTD";
												if (option6_whatToAddToAllCommits == BTOption6_whatToAddToAllCommits.ADD_mL || option6_whatToAddToAllCommits == BTOption6_whatToAddToAllCommits.ADD_PTD_mL)
													multiProject_usedCommitsEvidence_Text = multiProject_usedCommitsEvidence_Text + "mL";
											} 
											String multiProject_usedPRsEvidence_Text = "mp_p"; //"p": pullRequest
											if (needToConsiderET(evidenceTypes, Constants.EVIDENCE_TYPE__MULTI_PROJ__PR)){
												if (option9_whatToAddToAllPRs == BTOption9_whatToAddToAllPRs.ADD_mL)
													multiProject_usedPRsEvidence_Text = multiProject_usedPRsEvidence_Text + "mL";
											} 
											String multiProject_usedBCsEvidence_Text = "mp_bC"; //"bC": bugComment
											if (needToConsiderET(evidenceTypes, Constants.EVIDENCE_TYPE__MULTI_PROJ__BUG_COMMENT)){
												if (option10_whatToAddToAllBugComments == BTOption10_whatToAddToAllBCs.ADD_mL)
													multiProject_usedBCsEvidence_Text = multiProject_usedBCsEvidence_Text + "mL";
											} 
											String multiProject_usedCCsEvidence_Text = "mp_cC"; //"cC": commitComment
											if (needToConsiderET(evidenceTypes, Constants.EVIDENCE_TYPE__MULTI_PROJ__COMMIT_COMMENT)){
												if (option11_whatToAddToAllCommitComments == BTOption11_whatToAddToAllCCs.ADD_mL)
													multiProject_usedCCsEvidence_Text = multiProject_usedCCsEvidence_Text + "mL";
											} 
											String multiProject_usedPCsEvidence_Text = "mp_pC"; //"pC": pull request Comment
											if (needToConsiderET(evidenceTypes, Constants.EVIDENCE_TYPE__MULTI_PROJ__PR_COMMENT)){
												if (option12_whatToAddToAllPRComments == BTOption12_whatToAddToAllPRCs.ADD_mL)
													multiProject_usedPCsEvidence_Text = multiProject_usedPCsEvidence_Text + "mL";
											} 
											String used_SOQ_Evidence_Text = "SOQ"; //"SOQ": SO Question
											String used_SOA_Evidence_Text = "SOA"; //"SOQ": SO Question
											
											for (int num=0; num<1; num++){
//												for (int iC=1; iC<=20; iC++)
												{ //: this for-loop is for tuning commit/pR/... weight (0.1, 0.2, ..., 0.9, 1, 1.1, ..., 1).
//													Constants.TYPE_SIMILARITY[Constants.EVIDENCE_TYPE_COMMIT] = 1.0 * iC / 10;
//												for (int iP=1; iP<=10; iP++)
												{ //: this for-loop is for tuning commit/pR/... weight (0.1, 0.2, ..., 0.9, 1, 1.1, ..., 1).
//													Constants.TYPE_SIMILARITY[Constants.EVIDENCE_TYPE_PR] = 1.0 * iP / 10;
//												for (int iBC=1; iBC<=10; iBC++)
												{ //: this for-loop is for tuning commit/pR/... weight (0.1, 0.2, ..., 0.9, 1, 1.1, ..., 1).
//													Constants.TYPE_SIMILARITY[Constants.EVIDENCE_TYPE_BUG_COMMENT] = 1.0 * iBC / 10;
//												for (int iCC=1; iCC<=10; iCC++)
												{ //: this for-loop is for tuning commit/pR/... weight (0.1, 0.2, ..., 0.9, 1, 1.1, ..., 1).
//													Constants.TYPE_SIMILARITY[Constants.EVIDENCE_TYPE_COMMIT_COMMENT] = 1.0 * iCC / 10;
//												for (int iPC=1; iPC<=10; iPC++)
												{ //: this for-loop is for tuning commit/pR/... weight (0.1, 0.2, ..., 0.9, 1, 1.1, ..., 1).
//													Constants.TYPE_SIMILARITY[Constants.EVIDENCE_TYPE_PR_COMMENT] = 1.0 * iPC / 10;
												
												//Determining the MultiProj experiment factors:
//												for (int i_MP_B=1; i_MP_B<=10; i_MP_B++)
												{ //: this for-loop is for tuning commit/pR/... weight (0.1, 0.2, ..., 0.9, 1, 1.1, ..., 1).
													//All the 5 assignment types have the same coefficient:
//													Constants.TYPE_SIMILARITY[Constants.EVIDENCE_TYPE__MULTI_PROJ__BUG] = 1.0 * i_MP_B / 10; 
													//All the followings will have the same weight (which are set in Constants):
													Constants.TYPE_SIMILARITY[20+Constants.ASSIGNMENT_TYPE_TO_TRIAGE.T1_AUTHOR.ordinal()] = Constants.TYPE_SIMILARITY[Constants.EVIDENCE_TYPE__MULTI_PROJ__BUG];
													Constants.TYPE_SIMILARITY[20+Constants.ASSIGNMENT_TYPE_TO_TRIAGE.T2_COAUTHOR.ordinal()] = Constants.TYPE_SIMILARITY[Constants.EVIDENCE_TYPE__MULTI_PROJ__BUG];
													Constants.TYPE_SIMILARITY[20+Constants.ASSIGNMENT_TYPE_TO_TRIAGE.T3_ADMIN_CLOSER.ordinal()] = Constants.TYPE_SIMILARITY[Constants.EVIDENCE_TYPE__MULTI_PROJ__BUG];
													Constants.TYPE_SIMILARITY[20+Constants.ASSIGNMENT_TYPE_TO_TRIAGE.T4_DRAFTED_A.ordinal()] = Constants.TYPE_SIMILARITY[Constants.EVIDENCE_TYPE__MULTI_PROJ__BUG];
													Constants.TYPE_SIMILARITY[20+Constants.ASSIGNMENT_TYPE_TO_TRIAGE.T5_ALL_TYPES.ordinal()] = Constants.TYPE_SIMILARITY[Constants.EVIDENCE_TYPE__MULTI_PROJ__BUG];
												
//												for (int i_MP_C=11; i_MP_C<=20; i_MP_C++)
												{ //: this for-loop is for tuning commit/pR/... weight (0.1, 0.2, ..., 0.9, 1, 1.1, ..., 1).
//													Constants.TYPE_SIMILARITY[Constants.EVIDENCE_TYPE__MULTI_PROJ__COMMIT] = 1.0 * i_MP_C / 10;
//												for (int i_MP_P=1; i_MP_P<=10; i_MP_P++)
												{ //: this for-loop is for tuning commit/pR/... weight (0.1, 0.2, ..., 0.9, 1, 1.1, ..., 1).
//													Constants.TYPE_SIMILARITY[Constants.EVIDENCE_TYPE__MULTI_PROJ__PR] = 1.0 * i_MP_P / 10;
//												for (int i_MP_BC=1; i_MP_BC<=10; i_MP_BC++)
												{ //: this for-loop is for tuning commit/pR/... weight (0.1, 0.2, ..., 0.9, 1, 1.1, ..., 1).
//													Constants.TYPE_SIMILARITY[Constants.EVIDENCE_TYPE__MULTI_PROJ__BUG_COMMENT] = 1.0 * i_MP_BC / 10;
//												for (int i_MP_CC=1; i_MP_CC<=10; i_MP_CC++)
												{ //: this for-loop is for tuning commit/pR/... weight (0.1, 0.2, ..., 0.9, 1, 1.1, ..., 1).
//													Constants.TYPE_SIMILARITY[Constants.EVIDENCE_TYPE__MULTI_PROJ__COMMIT_COMMENT] = 1.0 * i_MP_CC / 10;
//												for (int i_MP_PC=1; i_MP_PC<=10; i_MP_PC++)
												{ //: this for-loop is for tuning commit/pR/... weight (0.1, 0.2, ..., 0.9, 1, 1.1, ..., 1).
//													Constants.TYPE_SIMILARITY[Constants.EVIDENCE_TYPE__MULTI_PROJ__PR_COMMENT] = 1.0 * i_MP_PC / 10;
//												for (int i_SOA=1; i_SOA<=10; i_SOA++)
												{ //: this for-loop is for tuning SOA weight (0.1, 0.2, ..., 0.9, 1).
//													Constants.TYPE_SIMILARITY[Constants.EVIDENCE_TYPE_SO_ANSWER] = 1.0 * i_SOA / 10;
//												for (int i_SOQ=1; i_SOQ<=10; i_SOQ++)
												{ //: this for-loop is for tuning SOQ weight (0.1, 0.2, ..., 0.9, 1).
//													Constants.TYPE_SIMILARITY[Constants.EVIDENCE_TYPE_SO_QUESTION] = 1.0 * i_SOQ / 10;

												String[] evidenceTypesText = new String[]{
														assignedBugAndUsedBugAsEvidence_Text, 
														usedCommitsEvidence_Text+"["+Constants.TYPE_SIMILARITY[Constants.EVIDENCE_TYPE_COMMIT]+"]",
														usedPRsEvidence_Text+"["+Constants.TYPE_SIMILARITY[Constants.EVIDENCE_TYPE_PR]+"]",
														usedBCsEvidence_Text+"["+Constants.TYPE_SIMILARITY[Constants.EVIDENCE_TYPE_BUG_COMMENT]+"]",
														usedCCsEvidence_Text+"["+Constants.TYPE_SIMILARITY[Constants.EVIDENCE_TYPE_COMMIT_COMMENT]+"]",
														usedPCsEvidence_Text+"["+Constants.TYPE_SIMILARITY[Constants.EVIDENCE_TYPE_PR_COMMENT]+"]",
														//just for evidence from other projects; runType = RUN_FOR_RAILS_AND_ANGULAR_AND_THEIR_FAMILIES	or RUN_FOR_RAILS_AND_ANGULAR_AND_THEIR_FAMILIES__WITH_SO_CONTRIBUTIONS
														multiProject_assignedBugAndUsedBugAsEvidence_Text+"["+Constants.TYPE_SIMILARITY[Constants.EVIDENCE_TYPE__MULTI_PROJ__BUG]+"]",
														multiProject_usedCommitsEvidence_Text+"["+Constants.TYPE_SIMILARITY[Constants.EVIDENCE_TYPE__MULTI_PROJ__COMMIT]+"]",
														multiProject_usedPRsEvidence_Text+"["+Constants.TYPE_SIMILARITY[Constants.EVIDENCE_TYPE__MULTI_PROJ__PR]+"]",
														multiProject_usedBCsEvidence_Text+"["+Constants.TYPE_SIMILARITY[Constants.EVIDENCE_TYPE__MULTI_PROJ__BUG_COMMENT]+"]",
														multiProject_usedCCsEvidence_Text+"["+Constants.TYPE_SIMILARITY[Constants.EVIDENCE_TYPE__MULTI_PROJ__COMMIT_COMMENT]+"]",
														multiProject_usedPCsEvidence_Text+"["+Constants.TYPE_SIMILARITY[Constants.EVIDENCE_TYPE__MULTI_PROJ__PR_COMMENT]+"]",
														//SO:
														used_SOA_Evidence_Text+"["+Constants.TYPE_SIMILARITY[Constants.EVIDENCE_TYPE_SO_ANSWER]+"]",
														used_SOQ_Evidence_Text+"["+Constants.TYPE_SIMILARITY[Constants.EVIDENCE_TYPE_SO_QUESTION]+"]",
												};

											//Determining the experiment title automatically based on the chosen evidence types (for the developer) and the algorithm methodology:
												String experimentTitle = "";//"bTD+pTD - bTD+c - tf+w+r1"    /* bugInfo - expertiseInfo - assignmentMethod*/
												for (int j=0; j<totalEvidenceTypes_count; j++)
													if (evidenceTypes[j] == YES)
														experimentTitle = StringManipulations.concatTwoStringsWithDelimiter(experimentTitle, evidenceTypesText[j], "+");
												experimentTitle = StringManipulations.concatTwoStringsWithDelimiter(assignedBugAndUsedBugAsEvidence_Text, experimentTitle, " - ");
												experimentTitle = experimentTitle + " - " + methodology;

												FileManipulationResult fMR = new FileManipulationResult();

												bugAssignment(Constants.DATASET_DIRECTORY_FOR_THE_ALGORITHM__GH__EXPERIMENT, Constants.DATASET_DIRECTORY_FOR_THE_ALGORITHM__SO__EXPERIMENT, Constants.DATASET_DIRECTORY_FOR_THE_ALGORITHM__EXPERIMENT_OUTPUT, "outSum", 
														runType, runType_SO, thresholdForNumberOfAssignmentsInAProject, 
														assignmentTypesToTriage, evidenceTypes, totalEvidenceTypes_count, 
														experimentTitle, "-",
														option1_whatToAddToAllBugs, option2_w, option3_TF, option4_IDF, option5_prioritizePAs, 
														option6_whatToAddToAllCommits, option7_whenToCountTextLength, option8_recency,
														option9_whatToAddToAllPRs, option10_whatToAddToAllBugComments, 
														option11_whatToAddToAllCommitComments, option12_whatToAddToAllPRComments, 
														option13_referenceTypesToConsider, 
														fMR,
														false, 1000, 0, Constants.THIS_IS_REAL, "");		
												if (fMR.errors > 0){
													MyUtils.println("Error in experiment()!", 0);
													return;
												}
												}//for (i_SOQ.
												}//for (i_SOA.
												}//for (i_MP_PC.
												}//for (i_MP_CC.
												}//for (i_MP_BC.
												}//for (i_MP_P.
												}//for (i_MP_C.
												}//for (i_MP_B.
										
												}//for (iPC.
												}//for (iCC.
												}//for (iBC.
												}//for (iP.
												}//for (iC.
												}//for (num.
												
												}//option13_counter
												}//for (btOption12....	
												}//for (btOption11....	
											}//for (btOption10....
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}
	//------------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------------
	public static void main(String[] args) {
		//This method will be called every time to assign bugs to developers and save the results in output files:
		experiment();
	}//main().
}


























