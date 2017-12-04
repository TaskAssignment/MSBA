package main;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import data.Project;
//import utils.CSVManipulations;
import utils.Constants;
import utils.FileManipulationResult;
import utils.Graph;
import utils.JSONToTSV;
import utils.MyUtils;
import utils.StringManipulations;
import utils.TSVManipulations;
import utils.Constants.ConditionType;
import utils.Constants.FieldType;
import utils.Constants.LogicalOperation;
import utils.Constants.ProjectType;
import utils.Constants.SortOrder;
//import utils.JSONToTSV;

public class DataPreparation {
	//------------------------------------------------------------------------------------------------------------------------
	public static final String COMBINED_KEY_SEPARATOR = Constants.COMBINED_KEY_SEPARATOR;
	public static final String TAB = Constants.TAB;
	public static final String DATE1 = "2000-00-00T00:00:00.000Z"; //All the evidence of expertise of developers are filtered to between these two (excluding these two).
	public static final String DATE2 = "2016-11-01T00:00:00.000Z";
	public static final int NO_DATE_FIELD_TO_FILTER = -1;
	public static final String NO_CLEANING_IS_NEEDED = "NO_CLEANING_IS_NEEDED";

	//------------------------------------------------------------------------------------------------------------------------
	public static void separateCommentsFiles(String inputPath, String outputPath, 
			FileManipulationResult fMR, int indentationLevel, String writeMessageStep){
		try{
			MyUtils.println("-----------------------------------", indentationLevel);
			MyUtils.println(writeMessageStep + "- Separating three different ctypes of comments (bug, PR and commit) and saving them in three tsv files:", indentationLevel);
			MyUtils.println("Started ...", indentationLevel+1);

			//1: Read all bugs in the form of HashSet<[projectId;;issueNumber]>:
			MyUtils.println("-----------------------------------", indentationLevel+1);
			MyUtils.println(MyUtils.concatTwoWriteMessageSteps(writeMessageStep, "1") + "- Reading bugs into a HashSet:", indentationLevel+1);
			MyUtils.println("Started ...", indentationLevel+2);
			BufferedReader br1 = new BufferedReader(new FileReader(inputPath + "\\bugs_complete.tsv")); 
			String s = br1.readLine(); //Skip the title line.
			HashSet<String> bugs = new HashSet<String>();
			int i = 0;
			int linesWithError = 0;
			while((s = br1.readLine()) != null) {
				String[] fields = s.split(Constants.TAB);
				if (fields.length == 11){
					String projectId = fields[1];
					String bugNumber = fields[2];
					bugs.add(projectId+Constants.SEPARATOR_FOR_ARRAY_ITEMS+bugNumber);
					i++;
					if (i % 10000 == 0)
						MyUtils.println(Constants.integerFormatter.format(i), 2);
				}
				else
					linesWithError++;
			}
			br1.close();
			MyUtils.println("Number of bugs read: " + Constants.integerFormatter.format(i), indentationLevel+2);
			if (linesWithError > 0)
				MyUtils.println("Finished with " + Constants.integerFormatter.format(linesWithError) + " errors.", indentationLevel+2);
			else
				MyUtils.println("Finished.", indentationLevel+2);
			MyUtils.println("-----------------------------------", indentationLevel+1);

			//2: Read all pullRequests in the form of HashSet<[projectId;;prNumber]>:
			MyUtils.println("-----------------------------------", indentationLevel+1);
			MyUtils.println(writeMessageStep + "-2- Reading pullResuests into a HashMap:", indentationLevel+1);
			MyUtils.println("Started ...", indentationLevel+2);
			BufferedReader br2 = new BufferedReader(new FileReader(inputPath + "\\PRs_complete.tsv")); 
			s = br2.readLine(); //Skip the title line.
			HashSet<String> prs = new HashSet<String>();
			i = 0;
			linesWithError = 0;
			while((s = br2.readLine()) != null) {
				String[] fields = s.split(Constants.TAB);
				if (fields.length == 11){
					String projectId = fields[1];
					String prNumber = fields[2];
					prs.add(projectId+Constants.SEPARATOR_FOR_ARRAY_ITEMS+prNumber);
					i++;
					if (i % 10000 == 0)
						MyUtils.println(Constants.integerFormatter.format(i), indentationLevel+2);
				}
				else
					linesWithError++;
			}
			br2.close();
			MyUtils.println("Number of PRs read: " + Constants.integerFormatter.format(i), indentationLevel+2);
			if (linesWithError > 0)
				MyUtils.println("Finished with " + Constants.integerFormatter.format(linesWithError) + " errors.", indentationLevel+2);
			else
				MyUtils.println("Finished.", indentationLevel+2);
			MyUtils.println("-----------------------------------", indentationLevel+1);

			//3: Read all lines of comments and store each line in one of the three files (bugComments_complete.tsv, PRComments_complete.tsv and commitComments_complete.tsv):
			MyUtils.println("-----------------------------------", 1);
			MyUtils.println(writeMessageStep + "-3- Reading comment lines and saving each line in one of the three files:", indentationLevel+1);
			MyUtils.println("Started ...", indentationLevel+2);
			BufferedReader br3 = new BufferedReader(new FileReader(inputPath + "\\comments.tsv")); 
			s = br3.readLine(); //Skip the title line.
			FileWriter writer1 = new FileWriter(outputPath+"\\bugComments_complete.tsv");
			writer1.append(s + "\n");
			FileWriter writer2 = new FileWriter(outputPath+"\\PRComments_complete.tsv");
			writer2.append(s + "\n");
			FileWriter writer3 = new FileWriter(outputPath+"\\commitComments_complete.tsv");
			writer3.append(s + "\n");
			i = 0;
			linesWithError = 0;
			int bugCommentLines = 0;
			int prCommentLines = 0;
			int commitCommentLines = 0;
			while((s = br3.readLine()) != null) {
				String[] fields = s.split(Constants.TAB);
				if (fields.length == 8){
					String type = fields[4]; //type is one of the two: commit or issue (and, issue means bug or PR).

					if (type.equals("issue")){//: means that the record is a bug comment or PR comment.
						String projectId = fields[1];
						String bugOrPRNumber = fields[6];
						String projectId_bugOrPRNumber = projectId + Constants.SEPARATOR_FOR_ARRAY_ITEMS + bugOrPRNumber;
						if (bugs.contains(projectId_bugOrPRNumber)){//: means that the record is a bug.
							bugCommentLines++;
							writer1.append(s + "\n"); //:write in bugComment file.
						}
						else{
							prCommentLines++;
							writer2.append(s + "\n"); //:write in PRComment file.
						}
					}
					else //: means that the record is a commit comment
						if (type.equals("commit")){//: means that the record is a commit comment.
							commitCommentLines++;
							writer3.append(s + "\n"); //write in commit comment file.
						}
					i++;
				}
				else
					linesWithError++;
				if (i % 100000 == 0)
					MyUtils.println(Constants.integerFormatter.format(i), indentationLevel+2);
				//			if (i ==100000)
				//				break;
			}
			br3.close();
			MyUtils.println("Total Number of comments read: " + Constants.integerFormatter.format(i), indentationLevel+2);
			MyUtils.println("Number of bug comments written: " + Constants.integerFormatter.format(bugCommentLines), indentationLevel+2);
			MyUtils.println("Number of PR comments written: " + Constants.integerFormatter.format(prCommentLines), indentationLevel+2);
			MyUtils.println("Number of commit comments written: " + Constants.integerFormatter.format(commitCommentLines), indentationLevel+2);
			if (i != bugCommentLines+prCommentLines+commitCommentLines)
				MyUtils.println("" + Constants.integerFormatter.format(i-bugCommentLines-prCommentLines-commitCommentLines) + " lines with ambiguous status.", indentationLevel+2);
			if (linesWithError > 0){
				MyUtils.println("Finished with " + Constants.integerFormatter.format(linesWithError) + " errors.", indentationLevel+2);
				fMR.errors = fMR.errors + linesWithError;
			}
			else
				MyUtils.println("Finished.", indentationLevel+2);
			MyUtils.println("-----------------------------------", indentationLevel+1);

			writer1.flush();
			writer1.close();
			writer2.flush();
			writer2.close();
			writer3.flush();
			writer3.close();

			MyUtils.println("Finished.", indentationLevel+1);
			MyUtils.println("-----------------------------------", indentationLevel);
		}catch (Exception e){
			e.printStackTrace();
		}
	}
	//------------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------------
	public static void identifyAssignments(String inputPath, String bugEventsInputFileName, String commitsInputFileName, 
			String outputPath,  
			FileManipulationResult fMR, boolean wrapOutputInLines, int showProgressInterval, int indentationLevel, long testOrReal, String writeMessageStep){
		//This method reads bugEvents.tsv and commits.tsv. 
			//Then, checks the commit dates related to the referenced bugs, 
				//and considers that time as the date of event (because the date of event is usually later than the actual commit). 
				//Also calculates the average, min and max time difference between these two (in this data set, the difference was 1, 3,407,757 and 118,756,430 seconds [min, average and max]).
			//After that, finds the real assignments (all of them, even duplicates), and saves them in output files.
			//Finally, creates 5 extra output files (by adding suffixes to the output file) and saves the assignees in them based on each of the below criteria:
				//ASSIGNEE_TYPE_1_BUG_FIX_CODE_AUTHOR (commit referencing a bug with specific keywords)
				//ASSIGNEE_TYPE_2_BUG_FIX_CODE_COAUTHOR (re-committer, rebaser, merger, etc. or the original committer)
				//ASSIGNEE_TYPE_3_ADMINISTRATIVE_BUG_RESOLVER (close the bug)
				//ASSIGNEE_TYPE_4_OFFICIAL_ASSIGNEE_WHEN_THE_BUG_WAS_CLOSED (being assignee when the bug is closed)
				//ASSIGNEE_TYPE_5_UNION_OF_ALL_TYPES_1_2_3_4 (union of all above 4 cases)
		Date d1 = new Date();
		MyUtils.println("-----------------------------------", 0);
		MyUtils.println("Identifying real assignees using 4 different methods:", indentationLevel);
		MyUtils.println("Started ...", indentationLevel+1);

		FileManipulationResult totalFMR = new FileManipulationResult();
		try{
			//1: Reading all events:
			TreeMap<String, ArrayList<String[]>> projectIdBugNumberAndTheirEvents = TSVManipulations.readNonUniqueCombinedKeyAndItsValueFromTSV(inputPath, bugEventsInputFileName, totalFMR, null, 
					"1$2",  
					SortOrder.ASCENDING_INTEGER, 10, "0$3$4$5$7", 
					LogicalOperation.NO_CONDITION, 
					0, ConditionType.NOTHING, "", FieldType.NOT_IMPORTANT, 
					0, ConditionType.NOTHING, "", FieldType.NOT_IMPORTANT, 
					wrapOutputInLines, showProgressInterval, indentationLevel+1, testOrReal, "1");
			
			//2: Reading all commits:
			TreeMap<String, String[]> commits = TSVManipulations.readUniqueKeyAndItsValueFromTSV(inputPath, commitsInputFileName, 
					null, 0, 5, "1$2$3$4", 
					LogicalOperation.NO_CONDITION, 
					0, ConditionType.NOTHING, "", FieldType.NOT_IMPORTANT, 
					0, ConditionType.NOTHING, "", FieldType.NOT_IMPORTANT, 
					wrapOutputInLines, showProgressInterval*2, indentationLevel+1, Constants.THIS_IS_REAL, "2");

			//3: Checking and saving assignments of different types:
			if (wrapOutputInLines)
				MyUtils.println("-----------------------------------", indentationLevel+1);
			MyUtils.println("3- Investigating and counting assignments (of 4 different types):", indentationLevel+1);
			MyUtils.println("Started ...", indentationLevel+2);

			String titleLine = "projectId\tbugNumer\tdate\tlogin\tassignmentType\teventId\tcommitSHA";
			FileWriter[] writer = new FileWriter[5];// = {new FileWriter(""), new FileWriter(""), new FileWriter(""), new FileWriter(""), new FileWriter("")};
			int[] numberOfAssignments = new int[Constants.NUMBER_OF_ASSIGNEE_TYPES];
			for (int p=0; p<Constants.NUMBER_OF_ASSIGNEE_TYPES; p++){
				writer[p]= new FileWriter(outputPath+"\\" + Constants.ASSIGNMENT_FILE_NAMES[p] + ".tsv");
				writer[p].append(titleLine + "\n");
				numberOfAssignments[p] = 0;
			}
			
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
			int numberOfEqualDates = 0;
			int numberOfPositiveTimeDiffs = 0;
			int numberOfNegativeTimeDiffs = 0;
			int numberOfReferencesFromCommits = 0;
			long totalTimeDiff = 0;
			int commitNotFoundError = 0;
			long minTimeDiff = Constants.AN_EXTREMELY_POSITIVE_LONG;
			long maxTimeDiff = Constants.AN_EXTREMELY_NEGATIVE_LONG;
			int projectIsNotTheSame_Errors = 0;
			int eventActorIsEmpty_Errors = 0;
			int numberOfEventsNotSortedBasedOnDate_Errors = 0;
			int eventIsReferencingButThereIsNoCommit_Errors = 0;
			int numberOfBugsReferencedFromCommitsWithSpecificKeywords = 0;
			int j = 0;
			int k=0;
			for (Map.Entry<String, ArrayList<String[]>> entry: projectIdBugNumberAndTheirEvents.entrySet()){//: for counting the bugs and their events.
				String projectIdAndBugNumber = entry.getKey();
				String projectId = projectIdAndBugNumber.split(COMBINED_KEY_SEPARATOR)[0];
				String bugNumber = projectIdAndBugNumber.split(COMBINED_KEY_SEPARATOR)[1];
//				System.out.println("projectId-BugNumber: " + projectIdAndBugNumber);
				ArrayList<String[]> arrayListOfEventsForThisBug = entry.getValue();
//				System.out.println("	Number of events: " + arrayListOfEventsForThisBug.size());
				HashMap<String, String> assigneesForThisBug = new HashMap<String, String>();
				//Checking all events of this bug:
				String previousEventDate = "";
				for (int i=0; i< arrayListOfEventsForThisBug.size(); i++){//: for counting the events of a bug.
//					System.out.println("	Values: " + arrayListOfEventsForThisBug.get(i)[0] + "        " + arrayListOfEventsForThisBug.get(i)[1] + "        " + arrayListOfEventsForThisBug.get(i)[2] + "        <" + arrayListOfEventsForThisBug.get(i)[3]+ ">");
					String eventId = arrayListOfEventsForThisBug.get(i)[0];
					String eventDate = arrayListOfEventsForThisBug.get(i)[1];
					String eventActor = arrayListOfEventsForThisBug.get(i)[2];
					String eventType = arrayListOfEventsForThisBug.get(i)[3];
					String commitSHA = arrayListOfEventsForThisBug.get(i)[4];
					
					//Checking the date order between events of a bug:
					if (previousEventDate.compareTo(eventDate) > 0){
						numberOfEventsNotSortedBasedOnDate_Errors++;
						totalFMR.errors++;
					}
					previousEventDate = eventDate;

					switch (eventType.toLowerCase()){
					case "referenced": //Investigating type 1 and 2 (and also 5) assignments and recording them in files once we found them:
						if (commitSHA.equals(" "))
							eventIsReferencingButThereIsNoCommit_Errors++;
						else{ //Means that this event is related to a commit.
							if (commits.containsKey(commitSHA)){
								String projectId_fromCommit = commits.get(commitSHA)[0];
								String committer = commits.get(commitSHA)[1];
								if (projectId.equals(projectId_fromCommit)){
									//Checking "reference with specific keywords" in the related commit: 
									String commitDate = commits.get(commitSHA)[2];
									String commitMessage = commits.get(commitSHA)[3];
									String regex = "(?:(?:clos|resolv)(?:e|es|ed|ing)|fix(?:es|ed|ing)?)(?:[\\s\\p{P}]*#[0-9]+)+";
									Pattern specificReferencePattern = Pattern.compile(regex);
									Matcher specificReferenceMatcher = specificReferencePattern.matcher(commitMessage);

									boolean foundACommitReferencingThisBug = false;
									while (specificReferenceMatcher.find()){//Considering all the references (single or multiple references) from the commit:
										String text2 = specificReferenceMatcher.group(0); //This is like "fixes #15" or "resolves #79 #80". So extracting the numbers.
										String regex2 = "[0-9]+";
										Pattern p2 = Pattern.compile(regex2);
										Matcher m2 = p2.matcher(text2);
										while (m2.find()){//Considering the bug number in a single reference (e.g., "fixed #38") or all the bug numbers in a multiple reference (e.g., "fixed #38 #77"):
											String referenceNumber = m2.group(0);
											if (referenceNumber.equals(bugNumber)){
												//Considering time differences (between commit that is referencing the bug and the related event):
												Date eventDate_DF = dateFormat.parse(eventDate);
												Date commitDate_DF = dateFormat.parse(commitDate);
												long timeDiff = 0;
												if (eventDate.equals(commitDate))
													numberOfEqualDates++;
												else{
													timeDiff = (long)(eventDate_DF.getTime()-commitDate_DF.getTime())/1000;
													if (timeDiff > 0){
														numberOfPositiveTimeDiffs++;
														if (timeDiff < minTimeDiff)
															minTimeDiff = timeDiff;
														if (timeDiff > maxTimeDiff)
															maxTimeDiff = timeDiff;
													}
													else{//in this case timeDiff is assumed 0.
														numberOfNegativeTimeDiffs++; //Still this will be considered one (or two) assignments (one for the commit, and one for the event, if it has a different actor and have all the required specifications).
														timeDiff = 0; //Based on the experiment, there are a number (~55) of records that have negative timeDiffs, but in those cases we consider it 0.
													}
												}
												totalTimeDiff = totalTimeDiff + timeDiff;
												numberOfBugsReferencedFromCommitsWithSpecificKeywords++;
												foundACommitReferencingThisBug = true;
												//Writing to assignee files:
												String tempCommitter;
												if (committer.equals(" ")) //See if no committer is recorded, then consider the event actor as the committer:
													tempCommitter = eventActor;
												else
													tempCommitter = committer;
												//Write the original author as type 1 assignee:
												writer[0].append(projectId + TAB + bugNumber + TAB + commitDate + TAB + tempCommitter + TAB + "1" + TAB + eventId + TAB + commitSHA + "\n");
												numberOfAssignments[0]++;
												//The type 5 assignee is a union of all other four types (but in writer5 file, we have the assignmentType field for capturing the source type); write it in writer5 as a type 1:
												writer[4].append(projectId + TAB + bugNumber + TAB + commitDate + TAB + tempCommitter + TAB + "1" + TAB + eventId + TAB + commitSHA + "\n");
												numberOfAssignments[4]++;
												if (eventActor.equals(" ")) //Just in case!
													eventActorIsEmpty_Errors++;
												else
													if (!eventActor.equals(tempCommitter)){//: means that if the event is showing that another developer has done the commit (other than the original author).
														//Write in writer2 as type 2 assignee:
														writer[1].append(projectId + TAB + bugNumber + TAB + eventDate + TAB + eventActor + TAB + "2" + TAB + eventId + TAB + commitSHA + "\n");
														numberOfAssignments[1]++;
														//The type 5 assignee is a union of all other four types (but in writer5 file, we have the assignmentType field for capturing the source type); write it in writer5 as a type 2:
														writer[4].append(projectId + TAB + bugNumber + TAB + eventDate + TAB + eventActor + TAB + "2" + TAB + eventId + TAB + commitSHA + "\n");
														numberOfAssignments[4]++;
													}
												break;
											}
										}//while (m2....
										if (foundACommitReferencingThisBug)//If so, we don't need to consider other references to other bugs. 
											break;
									}
									numberOfReferencesFromCommits++;
								}
								else
									projectIsNotTheSame_Errors++;
							}
							else
								commitNotFoundError++;
						}
						break;
					case "assigned": //Setting the list of assignees (to be used when the bug gets closed later):
						if (eventActor.equals(" ")) //Just in case!
							eventActorIsEmpty_Errors++;
						else
							assigneesForThisBug.put(eventActor, eventDate); //I am aware that in some cases a developer may be assigned again to the same bug. Either if the developer was unassigned before or not, we need to consider this last assignment date (when the bug gets closed) as real assignment date. So we don't check if this key exists or not.
						break;
					case "unassigned": //Updating the list of assignees (to be used when the bug gets closed later):
						assigneesForThisBug.remove(eventActor);
						break;
					case "closed": //Recording type 3 and 4 (and also 5) assignments:
						if (eventActor.equals(" ")) //Just in case!
							eventActorIsEmpty_Errors++;
						else{
							//Write the closer as type 3 assignee:
							writer[2].append(projectId + TAB + bugNumber + TAB + eventDate + TAB + eventActor + TAB + "3" + TAB + eventId + TAB + " " + "\n");
							numberOfAssignments[2]++;
							//The type 5 assignee is a union of all other four types (but in writer5 file, we have the assignmentType field for capturing the source type); write it in writer5 as a type 3:
							writer[4].append(projectId + TAB + bugNumber + TAB + eventDate + TAB + eventActor + TAB + "3" + TAB + eventId + TAB + " " + "\n");
							numberOfAssignments[4]++;
							if (assigneesForThisBug.size() > 0)
								for (Map.Entry<String, String> assignmentEntry: assigneesForThisBug.entrySet()){
									String assignee = assignmentEntry.getKey();
									String assignmentDate = assignmentEntry.getValue();
									//Write the assignee [when the bug is closed] and the time of assignment as type 4 assignee:
									writer[3].append(projectId + TAB + bugNumber + TAB + assignmentDate + TAB + assignee + TAB + "4" + TAB + eventId + TAB + " " + "\n");
									numberOfAssignments[3]++;
									//The type 5 assignee is a union of all other four types (but in writer5 file, we have the assignmentType field for capturing the source type); write it in writer5 as a type 4:
									writer[4].append(projectId + TAB + bugNumber + TAB + assignmentDate + TAB + assignee + TAB + "4" + TAB + eventId + TAB + " " + "\n");
									numberOfAssignments[4]++;
								}
						}
						break;
					}
					k++;
				}
				j++;
				if (testOrReal > Constants.THIS_IS_REAL)
					if (j >= testOrReal)
						break;
				if (j % (showProgressInterval/5) == 0)
					MyUtils.println(Constants.integerFormatter.format(j) + " bugs ...", indentationLevel+2);
			}
			for (int p=0; p<Constants.NUMBER_OF_ASSIGNEE_TYPES; p++){
				writer[p].flush();	
				writer[p].close();
			}
			MyUtils.println(Constants.integerFormatter.format(j) + " bugs (including " + Constants.integerFormatter.format(k) + " events) were processed.", indentationLevel+2);
			MyUtils.println("Finished.", indentationLevel+2);
			if (wrapOutputInLines)
				MyUtils.println("-----------------------------------", indentationLevel+1);
			Date d2 = new Date();

			MyUtils.println("", indentationLevel+1);
			MyUtils.println("Summary:", indentationLevel+1);
			MyUtils.println("Number of bugs processed: " + Constants.integerFormatter.format(j), indentationLevel+2);
			MyUtils.println("Those bugs included " + Constants.integerFormatter.format(k) + " events.", indentationLevel+3);
			MyUtils.println("Number of critical errors (not sorted events [if so, should change the code to sort them]): " + numberOfEventsNotSortedBasedOnDate_Errors, indentationLevel+2);
			MyUtils.println("Number of commitNotFound errors (the commit was in other branches, other projects [that we don't have the data], etc.): " + Constants.integerFormatter.format(commitNotFoundError), indentationLevel+2);
			MyUtils.println("Number of references from commits: " + Constants.integerFormatter.format(numberOfReferencesFromCommits), indentationLevel+2);
			MyUtils.println("Number of positive timeDiffs (commitD < eventD): " + Constants.integerFormatter.format(numberOfPositiveTimeDiffs), indentationLevel+2);
			MyUtils.println("Number of equal dates (commitD = eventD): " + Constants.integerFormatter.format(numberOfEqualDates), indentationLevel+2);
			MyUtils.println("Number of negative timeDiffs [errors] ((commitD > eventD) --> considered zero in summation): " + Constants.integerFormatter.format(numberOfNegativeTimeDiffs), indentationLevel+2);
		
			MyUtils.println("", indentationLevel+1);
			MyUtils.println("Number of \"project not the same\" errors (In fact, we can use this in 'other projects' experiment): " + projectIsNotTheSame_Errors, indentationLevel+2);
			MyUtils.println("Number of \"event is referencing but there is no commitSHA\" errors: " + Constants.integerFormatter.format(eventIsReferencingButThereIsNoCommit_Errors), indentationLevel+2);
			MyUtils.println("Number of \"event actor is empty\" errors: " + eventActorIsEmpty_Errors, indentationLevel+2);

			MyUtils.println("", indentationLevel+1);
			MyUtils.println("Minimum time difference: " + Constants.integerFormatter.format(minTimeDiff) + " seconds", indentationLevel+2);
			MyUtils.println("Average time difference: " + Constants.floatFormatter.format((float)totalTimeDiff/numberOfBugsReferencedFromCommitsWithSpecificKeywords) + " seconds", indentationLevel+2);
			MyUtils.println("Maximum time difference: " + Constants.integerFormatter.format(maxTimeDiff) + " seconds", indentationLevel+2);
			
			MyUtils.println("", indentationLevel+1);
			for (int p=0; p<Constants.NUMBER_OF_ASSIGNEE_TYPES; p++)
				MyUtils.println("Number of type " + (p+1) + " assignments (" + Constants.ASSIGNED_BUGS_TYPES__SHORT_DESCRIPTIONS[p] + "): " + Constants.integerFormatter.format(numberOfAssignments[p]), indentationLevel+2);

			MyUtils.println("", indentationLevel+1);
			MyUtils.println("Total time: " + Constants.floatFormatter.format((float)(d2.getTime()-d1.getTime())/1000)  + " seconds.", indentationLevel+2);
			MyUtils.println("", indentationLevel+1);
			if (totalFMR.errors == 0)
				MyUtils.println("Finished.", indentationLevel+1);
			else{
				MyUtils.println("Finished with " + totalFMR.errors + " critical errors in reading input files.", indentationLevel+1);
				fMR.errors = totalFMR.errors;
			}
		}catch (Exception e){
			e.printStackTrace();
			MyUtils.println("Finished with errors.", 1);
			fMR.errors = totalFMR.errors;
		}
		MyUtils.println("-----------------------------------", 0);
	}
	//------------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------------
	public static void sortAssignments(String iOPath, //output files are the same as input files (the "assignments" file names as defined in Constants class). 
			FileManipulationResult fMR, boolean wrapOutputInLines, int showProgressInterval, int indentationLevel, long testOrReal, String writeMessageStep){
		//This method sorts the assignees in all 5 assignments files based on assignment date, and rewrites the results in the same files.
		Date d1 = new Date();
		MyUtils.println("-----------------------------------", 0);
		MyUtils.println("Sorting assignments:", indentationLevel);
		MyUtils.println("Started ...", indentationLevel+1);

		FileManipulationResult localFMR = new FileManipulationResult();
		FileManipulationResult totalFMR = new FileManipulationResult();
		try{
			for (int i=0; i<Constants.NUMBER_OF_ASSIGNEE_TYPES; i++){ //do the same process for each assignment file:
				//1: Reading all assignments (sorted by date):
				String mainStep, subStep;
				mainStep = Integer.toString(i + 1);
				if (wrapOutputInLines)
					MyUtils.println("-----------------------------------", indentationLevel+1);
				MyUtils.println(MyUtils.concatTwoWriteMessageSteps(writeMessageStep, mainStep)+"- Sorting assignments of type \""+mainStep + "\":", indentationLevel+1);
				MyUtils.println("Started ...", indentationLevel+2);
				subStep = MyUtils.concatTwoWriteMessageSteps(writeMessageStep, mainStep + "-1");
				ArrayList<String> titlesToReturn_IS_NOT_NEEDED_AND_USED = new ArrayList<String>();
				TreeMap<String, ArrayList<String[]>> assignments = TSVManipulations.readNonUniqueKeyAndItsValueFromTSV(
						iOPath, Constants.ASSIGNMENT_FILE_NAMES[i]+".tsv", localFMR, null, 
						2,  
						SortOrder.DEFAULT_FOR_STRING, 7, "0$1$3$4$5$6", titlesToReturn_IS_NOT_NEEDED_AND_USED, 
						LogicalOperation.NO_CONDITION, 
						0, ConditionType.NOTHING, "", FieldType.NOT_IMPORTANT, 
						0, ConditionType.NOTHING, "", FieldType.NOT_IMPORTANT, 
						wrapOutputInLines, showProgressInterval, indentationLevel+2, testOrReal, subStep);
				totalFMR = MyUtils.addFileManipulationResults(totalFMR, localFMR);

				//2: Saving the assignments as separate files:
				subStep = MyUtils.concatTwoWriteMessageSteps(writeMessageStep, mainStep + "-2");
				localFMR = TSVManipulations.saveTreeMapToTSVFile(
						iOPath, Constants.ASSIGNMENT_FILE_NAMES[i]+"-temp.tsv", assignments, "projectId\tbugNumer\tdate\tlogin\tassignmentType\teventId\tcommitSHA", true, 2, 
						wrapOutputInLines, showProgressInterval, indentationLevel+2, testOrReal, subStep);
				totalFMR = MyUtils.addFileManipulationResults(totalFMR, localFMR);
				
				if (totalFMR.errors ==0){//Get rid of old files only if everything went smoothly.
					//3: deleting old file:
					if (wrapOutputInLines)
						MyUtils.println("-----------------------------------", indentationLevel+1);
					subStep = MyUtils.concatTwoWriteMessageSteps(writeMessageStep, mainStep + "-3");
					String fileToBeDeleted = Constants.ASSIGNMENT_FILE_NAMES[i]+".tsv";
					localFMR = MyUtils.deleteTemporaryFiles(iOPath, new String[]{fileToBeDeleted}, true, indentationLevel+2, subStep);
					totalFMR = MyUtils.addFileManipulationResults(totalFMR, localFMR);
					if (wrapOutputInLines)
						MyUtils.println("-----------------------------------", indentationLevel+1);

					//4: Renaming the saved file to original name:
					if (wrapOutputInLines)
						MyUtils.println("-----------------------------------", indentationLevel+1);
					subStep = MyUtils.concatTwoWriteMessageSteps(writeMessageStep, mainStep + "-4");
					localFMR = MyUtils.renameFile(iOPath, Constants.ASSIGNMENT_FILE_NAMES[i]+"-temp.tsv", iOPath, Constants.ASSIGNMENT_FILE_NAMES[i]+".tsv", indentationLevel+2, subStep);
					totalFMR = MyUtils.addFileManipulationResults(totalFMR, localFMR);
					if (wrapOutputInLines)
						MyUtils.println("-----------------------------------", indentationLevel+1);
				}

				MyUtils.println("Finished.", indentationLevel+2);
				if (wrapOutputInLines)
					MyUtils.println("-----------------------------------", indentationLevel+1);
			}
			Date d2 = new Date();
			MyUtils.println("Total time: " + Constants.floatFormatter.format((float)(d2.getTime()-d1.getTime())/1000)  + " seconds.", indentationLevel+2);
			MyUtils.println("", indentationLevel+1);
			if (totalFMR.errors == 0)
				MyUtils.println("Finished.", indentationLevel+1);
			else{
				MyUtils.println("Finished with " + totalFMR.errors + " critical errors in reading input files.", indentationLevel+1);
				fMR.errors = totalFMR.errors;
			}
		}catch (Exception e){
			e.printStackTrace();
			MyUtils.println("Finished with errors (exceptions).", 1);
			fMR.errors = totalFMR.errors;
		}
		MyUtils.println("-----------------------------------", 0);
	}
	//------------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------------
	public static void removeObviousAssignments(String iOPath, //output files are the same as input files (the "assignments" file names as defined in Constants class). 
			FileManipulationResult fMR, boolean wrapOutputInLines, int showProgressInterval, int indentationLevel, long testOrReal, String writeMessageStep){
		//This method removes the obvious assignments (i.e., assignments of the same type, to the same developer, within an hour time limit) in all 5 assignments files.
		Date d1 = new Date();
		MyUtils.println("-----------------------------------", 0);
		MyUtils.println("Identify and Remove obvious assignees: ", indentationLevel);
		MyUtils.println("Started ...", indentationLevel+1);

		FileManipulationResult localFMR = new FileManipulationResult();
		FileManipulationResult totalFMR = new FileManipulationResult();
		try{
			for (int i=0; i<Constants.NUMBER_OF_ASSIGNEE_TYPES; i++){ //do the same process for each assignment file:
				String mainStep, subStep;
				mainStep = Integer.toString(i + 1);
				if (wrapOutputInLines)
					MyUtils.println("-----------------------------------", indentationLevel+1);
				MyUtils.println(MyUtils.concatTwoWriteMessageSteps(writeMessageStep, mainStep)+"- Assignments of type \""+mainStep + "\":", indentationLevel+1);
				MyUtils.println("Started ...", indentationLevel+2);
				
				//i-1: Reading assignments of type i as TreeMap<date-->ArrayList<assignmentRecord>>:
				subStep = MyUtils.concatTwoWriteMessageSteps(writeMessageStep, mainStep + "-1");
				ArrayList<String> titlesToReturn_IS_NOT_NEEDED_AND_USED = new ArrayList<String>();
				TreeMap<String, ArrayList<String[]>> datesAndTheirAssignments = TSVManipulations.readNonUniqueKeyAndItsValueFromTSV(
						iOPath, Constants.ASSIGNMENT_FILE_NAMES[i]+".tsv", localFMR, null, 
						2,  
						SortOrder.DEFAULT_FOR_STRING, 7, "0$1$2$3$4$5$6", titlesToReturn_IS_NOT_NEEDED_AND_USED, 
						LogicalOperation.NO_CONDITION, 
						0, ConditionType.NOTHING, "", FieldType.NOT_IMPORTANT, 
						0, ConditionType.NOTHING, "", FieldType.NOT_IMPORTANT, 
						wrapOutputInLines, showProgressInterval, indentationLevel+2, testOrReal, subStep);
				totalFMR = MyUtils.addFileManipulationResults(totalFMR, localFMR);

				//i-2: Checking obvious assignments:
				subStep = MyUtils.concatTwoWriteMessageSteps(writeMessageStep, mainStep + "-2");
				if (wrapOutputInLines)
					MyUtils.println("-----------------------------------", indentationLevel+2);
				MyUtils.println(subStep+"-2- Checking obvious assignments of type \""+mainStep + "\" and saving the rest in temp file:", indentationLevel+2);
				MyUtils.println("Started ...", indentationLevel+3);
				//Note that each line corresponds an assignment in one of the assignment files:
				FileWriter writer = new FileWriter(iOPath+"\\"+Constants.ASSIGNMENT_FILE_NAMES[i]+"-temp.tsv");
				writer.append("projectId\tbugNumer\tdate\tlogin\tassignmentType\teventId\tcommitSHA\n");
				DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
				String dateOfLastLine = "2000-00-00T00:00:00.000Z";
				ArrayList<String[]> temporaryAssignments = new ArrayList<String[]>();
				boolean shouldBeWritten;
				int j = 0;
				for (Map.Entry<String, ArrayList<String[]>> entry: datesAndTheirAssignments.entrySet()){
					String dateOfThisLine = entry.getKey();
					ArrayList<String[]> allLinesOfThisTime  = entry.getValue();
					//Check all assignments of this time (can be one or more):
					for (int k=0; k<allLinesOfThisTime.size(); k++){
						String[] thisLine = allLinesOfThisTime.get(k);
						shouldBeWritten = true;
						long timeDiff = (dateFormat.parse(dateOfThisLine).getTime() - dateFormat.parse(dateOfLastLine).getTime()) / 1000;
						if (timeDiff > 3600){
							temporaryAssignments.clear();
							temporaryAssignments.add(thisLine);
							dateOfLastLine = thisLine[2];
						}
						else{ //: when the timeDiff between this line and previous line is so small (less than an hour):
							//But the time difference we checked above (in the main "if" statement) is between the new line and the last line which was +3600 seconds after the previous one. Since there may be several lines with a few minutes difference with each other, we remove those older ones here:
							while((dateFormat.parse(dateOfThisLine).getTime() - dateFormat.parse(temporaryAssignments.get(0)[2]).getTime()) / 1000 > 3600)
								temporaryAssignments.remove(0);
							for (int p=0; p<temporaryAssignments.size(); p++)
								if (StringManipulations.specificFieldsOfTwoStringArraysAreEqual(thisLine, temporaryAssignments.get(p), "0$1$3$4")){
									//This means that the same login is assigned with the same type in the same project and for the same bug number (within an hour time difference):
									shouldBeWritten = false;
									break;
								}
							if (shouldBeWritten){//: Should be written (don't touch the variable 'shouldBeWritten'). Also add this line to be considered in comparisons when we proceed to next lines:
								temporaryAssignments.add(thisLine);
								dateOfLastLine = temporaryAssignments.get(0)[2]; //Now that the old lines are deleted the first temporaryAssignment should be in an hour distance (there should be at least one such a record because the "if" section was not true!
							}//otherwise just ignore it (even don't update the dateOfLastLine because that was a sort of repetitive assignment!).
						}
						if (shouldBeWritten)
							writer.append(thisLine[0] + "\t" + thisLine[1] + "\t" + thisLine[2] + "\t" + thisLine[3] + "\t" + thisLine[4] + "\t" + thisLine[5] + "\t" + thisLine[6] + "\t" + "\n");
						j++;
						if (testOrReal > Constants.THIS_IS_REAL)
							if (j >= testOrReal)
								break;
						if (j % (showProgressInterval) == 0)
							MyUtils.println(Constants.integerFormatter.format(j) + " assignments ...", indentationLevel+3);
					}
				}
				writer.flush();
				writer.close();
				MyUtils.println("Finished.", indentationLevel+3);
				if (wrapOutputInLines)
					MyUtils.println("-----------------------------------", indentationLevel+2);
				
				if (totalFMR.errors ==0){//Get rid of old files only if everything went smoothly.
					//i-3: deleting old file:
					if (wrapOutputInLines)
						MyUtils.println("-----------------------------------", indentationLevel+1);
					subStep = MyUtils.concatTwoWriteMessageSteps(writeMessageStep, mainStep + "-3");
					String fileToBeDeleted = Constants.ASSIGNMENT_FILE_NAMES[i]+".tsv";
					localFMR = MyUtils.deleteTemporaryFiles(iOPath, new String[]{fileToBeDeleted}, true, indentationLevel+2, subStep);
					totalFMR = MyUtils.addFileManipulationResults(totalFMR, localFMR);
					if (wrapOutputInLines)
						MyUtils.println("-----------------------------------", indentationLevel+1);

					//i-4: Renaming the saved file to original name:
					if (wrapOutputInLines)
						MyUtils.println("-----------------------------------", indentationLevel+1);
					subStep = MyUtils.concatTwoWriteMessageSteps(writeMessageStep, mainStep + "-4");
					localFMR = MyUtils.renameFile(iOPath, Constants.ASSIGNMENT_FILE_NAMES[i]+"-temp.tsv", iOPath, Constants.ASSIGNMENT_FILE_NAMES[i]+".tsv", indentationLevel+2, subStep);
					totalFMR = MyUtils.addFileManipulationResults(totalFMR, localFMR);
					if (wrapOutputInLines)
						MyUtils.println("-----------------------------------", indentationLevel+1);
				}

				MyUtils.println("Finished.", indentationLevel+2);
				if (wrapOutputInLines)
					MyUtils.println("-----------------------------------", indentationLevel+1);
			}
			Date d2 = new Date();
			MyUtils.println("Total time: " + Constants.floatFormatter.format((float)(d2.getTime()-d1.getTime())/1000)  + " seconds.", indentationLevel+2);
			MyUtils.println("", indentationLevel+1);
			if (totalFMR.errors == 0)
				MyUtils.println("Finished.", indentationLevel+1);
			else{
				MyUtils.println("Finished with " + totalFMR.errors + " critical errors in reading input files.", indentationLevel+1);
				fMR.errors = totalFMR.errors;
			}
		}catch (Exception e){
			e.printStackTrace();
			MyUtils.println("Finished with errors (exceptions).", 1);
			fMR.errors = totalFMR.errors;
		}
		MyUtils.println("-----------------------------------", 0);
	}
	//------------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------------
	public static void identifyCommunityMembers(String iOPath, String commitsInputFileName,
			FileManipulationResult fMR, boolean wrapOutputInLines, int showProgressInterval, int indentationLevel, long testOrReal, String writeMessageStep){
		//This method reads the sorted assignments files (sorted by sortAssignments method above), as well as commits.tsv
			//Then, for each project, creates the community members (union of assiggnees of each type and the committers) and saves them in 5 different files.
		Date d1 = new Date();
		MyUtils.println("-----------------------------------", 0);
		MyUtils.println("Identifying community members:", indentationLevel);
		MyUtils.println("Started ...", indentationLevel+1);

		FileManipulationResult localFMR = new FileManipulationResult();
		FileManipulationResult totalFMR = new FileManipulationResult();
		try{
			//1: Reading all committers in each project:
			TreeMap<String, TreeSet<String>> projectsAndTheirCommitters = TSVManipulations.readNonUniqueKeyAndItsValueAsTreeSetFromTSV(
					iOPath, commitsInputFileName, 
					localFMR, null, 1, SortOrder.ASCENDING_INTEGER, 5, 2, 
					LogicalOperation.NO_CONDITION, 
					0, ConditionType.NOTHING, "", FieldType.NOT_IMPORTANT, 
					0, ConditionType.NOTHING, "", FieldType.NOT_IMPORTANT, 
					wrapOutputInLines, showProgressInterval*2, indentationLevel+1, Constants.THIS_IS_REAL, "1");
			totalFMR = MyUtils.addFileManipulationResults(totalFMR, localFMR);
			
			//2: Reading all assignees (each of 5 different files) and combining them with committers:
			if (wrapOutputInLines)
				MyUtils.println("-----------------------------------", indentationLevel+1);
			MyUtils.println(MyUtils.concatTwoWriteMessageSteps(writeMessageStep, "2")+"- Reading all assignees (each of 5 different files) and combining them with committers:", indentationLevel+1);
			MyUtils.println("Started ...", indentationLevel+2);
			for (int i=0; i<Constants.NUMBER_OF_ASSIGNEE_TYPES; i++){ //do the same process for each assignment file:
				//2-i- Reading assignees of type i and combining them with committers:
				String step = MyUtils.concatTwoWriteMessageSteps(writeMessageStep, "2-"+Integer.toString(i+1));
				if (wrapOutputInLines)
					MyUtils.println("-----------------------------------", indentationLevel+2);
				MyUtils.println(step+"- Reading all assignees of type " + Integer.toString(i+1) + " and combining them with committers:", indentationLevel+2);
				MyUtils.println("Started ...", indentationLevel+3);
				
				//2-i-1: Reading all assignees of type (i+1): 
				TreeMap<String, TreeSet<String>> projectsAndTheirAssignees = TSVManipulations.readNonUniqueKeyAndItsValueAsTreeSetFromTSV(
						iOPath, Constants.ASSIGNMENT_FILE_NAMES[i]+".tsv", 
						localFMR, null, 0, SortOrder.ASCENDING_INTEGER, 7, 3, 
						LogicalOperation.NO_CONDITION, 
						0, ConditionType.NOTHING, "", FieldType.NOT_IMPORTANT, 
						0, ConditionType.NOTHING, "", FieldType.NOT_IMPORTANT, 
						wrapOutputInLines, showProgressInterval*2, indentationLevel+3, Constants.THIS_IS_REAL, step+"-1");
				totalFMR = MyUtils.addFileManipulationResults(totalFMR, localFMR);

				//2-i-2- Combining the assignees of type (i+1) with committers:
				MyUtils.println(step+"-2- Obtaining the combined assignees of type \""+Integer.toString(i+1)+"\" by combining with committers:", indentationLevel+3);
				MyUtils.println("Started ...", indentationLevel+4);
				for(Map.Entry<String,TreeSet<String>> aProjectAndItsAssignees : projectsAndTheirAssignees.entrySet()) {
					String projectId = aProjectAndItsAssignees.getKey();
					TreeSet<String> assigneesInThisProject = aProjectAndItsAssignees.getValue();
					TreeSet<String> committersInThisProject = projectsAndTheirCommitters.get(projectId);
					for (String s: committersInThisProject)
						assigneesInThisProject.add(s);
				}
				MyUtils.println("Finished.", indentationLevel+4);
				if (wrapOutputInLines)
					MyUtils.println("-----------------------------------", indentationLevel+3);
//				sorting community members per project ...
				//2-i-3- Saving the obtained [combined] assignees of type (i+1):
				localFMR = TSVManipulations.saveTreeMapOfStringAndTreeSetToTSVFile(
						iOPath, Constants.COMMUNITY_FILE_NAMES[i]+".tsv", projectsAndTheirAssignees, "projectId\tlogin", 
						wrapOutputInLines, showProgressInterval, indentationLevel+3, testOrReal, step+"-3");
				totalFMR = MyUtils.addFileManipulationResults(totalFMR, localFMR);
				
				MyUtils.println("Finished.", indentationLevel+3);
				if (wrapOutputInLines)
					MyUtils.println("-----------------------------------", indentationLevel+2);
			}
			MyUtils.println("Finished.", indentationLevel+2);
			if (wrapOutputInLines)
				MyUtils.println("-----------------------------------", indentationLevel+1);

			Date d2 = new Date();
			MyUtils.println("Total time: " + Constants.floatFormatter.format((float)(d2.getTime()-d1.getTime())/1000)  + " seconds.", indentationLevel+1);
			MyUtils.println("", indentationLevel+1);
			if (totalFMR.errors == 0)
				MyUtils.println("Finished.", indentationLevel+1);
			else{
				MyUtils.println("Finished with " + totalFMR.errors + " critical errors handling i/o files.", indentationLevel+1);
				fMR.errors = totalFMR.errors;
			}
		}catch (Exception e){
			e.printStackTrace();
			MyUtils.println("Finished with errors (exceptions).", 1);
			fMR.errors = totalFMR.errors;
		}
		MyUtils.println("-----------------------------------", 0);
	}
	//------------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------------
	public static void toLowerCaseAndRemoveEndingDotsAndFilterDatesAndFindEarliestStartingDatePerProject(String iOPath, 
			FileManipulationResult fMR, boolean wrapOutputInLines, int showProgressInterval, int indentationLevel, long testOrReal, String writeMessageStep){
		//This method reads the .tsv files 
			//Then, converts them to lowercase and also removes the ending dots.
			//Also filters all the evidence between DATE1 and DATE2
			//Also finds the startingDate for each type of evidence and writes in a new file, projects.tsv as new columns.
		Date d1 = new Date();
		MyUtils.println("-----------------------------------", indentationLevel);
		MyUtils.println("Converting to lowercase and removing the ending dots:", indentationLevel);
		MyUtils.println("Started ...", indentationLevel+1);

		String[] files = new String[]{"bugs_complete", "commits", "PRs_complete", "bugComments_complete", "commitComments_complete", "PRComments_complete", "bugEvents", "projects_complete"};
		String[] newNamesForFiles = new String[]{"bugs_complete", "commits", "PRs_complete", "bugComments_complete", "commitComments_complete", "PRComments_complete", "bugEvents", "projects_complete_withStartDates"};
		String[] titlesForStartingDatesInFiles = new String[]{"bugs'StartingDate", "commits'StartingDate", "PRs'StartingDate", "bugComments'StartingDate", "commitComments'StartingDate", "PRComments'StartingDate", "bugEvents'StartingDate", "overalStartingDate"};
		String titleSuffixForProjects = TAB + titlesForStartingDatesInFiles[0];
		for (int j=1; j< titlesForStartingDatesInFiles.length; j++)
			titleSuffixForProjects = titleSuffixForProjects + TAB + titlesForStartingDatesInFiles[j];
		
		String[] fieldNumbersToBeCleaned_separatedByDollar = new String[]{"9$10", "5", "9$10", "7", "7", "7", NO_CLEANING_IS_NEEDED, "3"};
		String[] fieldNumbersToBeConveredToLowerCase_separatedByDollar = new String[]{"3$5$7$8", "2", "3$5$7$8", "3", "3", "3", "4$6$7$9$10", "2$4$5"};//These are the fields that just are converted to lowerCase and we do not want to do any other modifications or cleaning (specifically for "login", "actor" etc.).
		int[] dateFieldNumbers = new int[]{6, 3, 6, 2, 2, 2, 3, NO_DATE_FIELD_TO_FILTER};
		int[] projectIdFieldNumbers = new int[]{1, 1, 1, 1, 1, 1, 1, 0};
		int[] totalNumberOfFields = new int[]{11, 6, 11, 8, 8, 8, 11, 6};
		HashMap<String, HashMap<String, String>> projectsAndStartingDatesOfDifferentFiles = new HashMap<String, HashMap<String, String>>(); //projectId --> <<"bugs_complete" --> "1/1/1">, <commits" --> "2/2/2">, ...>   these are 8 startingDates for different files in each project.
		int errors = 0;
		int warnings = 0;
		FileManipulationResult localFMR = new FileManipulationResult();
		FileManipulationResult totalFMR = new FileManipulationResult();
		
		try{
			for (int i=0; i<files.length; i++){
				String step = Integer.toString(i+1);
				//i- Reading files line by line and cleaning and filtering and ...:
				if (wrapOutputInLines)
					MyUtils.println("-----------------------------------", indentationLevel+1);
				MyUtils.println(step+"- \"" + files[i] + ".tsv" + "\":", indentationLevel+1);
				MyUtils.println("Started ...", indentationLevel+2);
				
				if (wrapOutputInLines)
					MyUtils.println("-----------------------------------", indentationLevel+2);
				MyUtils.println(step+"-1- Reading and \""+files[i]+"\" and cleaning and filtering and ...:", indentationLevel+2);
				MyUtils.println("Started ...", indentationLevel+3);
				
				String s;
				String[] fields;
				BufferedReader br = new BufferedReader(new FileReader(iOPath + "\\" + files[i] + ".tsv"));
				//Header:
				s = br.readLine(); 
				FileWriter writer = new FileWriter(iOPath + "\\" + files[i] + "-temp.tsv");
				int counter = 0;
				if (files[i].equals("projects_complete"))
					writer.append(s + titleSuffixForProjects + "\n");
				else
					writer.append(s + "\n");
				//The rest of the lines:
				while ((s=br.readLine())!=null){
					fields = s.split(TAB);
					if (fields.length != totalNumberOfFields[i])
						errors++;
					//Converting to lowerCase:
					if (!fieldNumbersToBeConveredToLowerCase_separatedByDollar[i].equals(NO_CLEANING_IS_NEEDED)){
						String[] fieldNumbersToBeConvertedToLowerCase = fieldNumbersToBeConveredToLowerCase_separatedByDollar[i].split("\\$");
						for (int j=0; j<fieldNumbersToBeConvertedToLowerCase.length; j++)
							fields[Integer.parseInt(fieldNumbersToBeConvertedToLowerCase[j])] = fields[Integer.parseInt(fieldNumbersToBeConvertedToLowerCase[j])].toLowerCase(); 
					}
					//Cleaning:
					if (!fieldNumbersToBeCleaned_separatedByDollar[i].equals(NO_CLEANING_IS_NEEDED)){
						String[] fieldNumbersToBeCleaned = fieldNumbersToBeCleaned_separatedByDollar[i].split("\\$");
						for (int j=0; j<fieldNumbersToBeCleaned.length; j++)
							fields[Integer.parseInt(fieldNumbersToBeCleaned[j])] = fields[Integer.parseInt(fieldNumbersToBeCleaned[j])].toLowerCase().replaceAll("(\\w)\\.(?!\\S)", "$1 "); 
					}
					//Filtering date and other stuff:
					if (dateFieldNumbers[i]==NO_DATE_FIELD_TO_FILTER || (fields[dateFieldNumbers[i]].compareTo(DATE1)>0 && fields[dateFieldNumbers[i]].compareTo(DATE2)<0)){
						String lineSuffixForProjects = "";
						//: if so, this is usually the "projects.tsv" file and should not check the date field (there is no date field),
							//or it is in the correct date range, so should write it:
						if (files[i].equals("projects_complete")){//: if so, create the starting dates (of bugs and commits and ...) in each project to be written later as new columns:
							String projectId = fields[projectIdFieldNumbers[i]];
							HashMap<String, String> filesAndTheirStartingDates = projectsAndStartingDatesOfDifferentFiles.get(projectId);
							String startingDateForAFile = filesAndTheirStartingDates.get(files[0]);
							if (startingDateForAFile == null)
								lineSuffixForProjects = TAB + " ";
							else
								lineSuffixForProjects = TAB + startingDateForAFile;
							String projectStartingDate = DATE2; //or another very big date.
							for (int j=1; j<files.length-1; j++){ 
								startingDateForAFile = filesAndTheirStartingDates.get(files[j]);
								if (startingDateForAFile == null)
									lineSuffixForProjects = lineSuffixForProjects + TAB + " ";
								else
									lineSuffixForProjects = lineSuffixForProjects + TAB + startingDateForAFile;
								
								if ((startingDateForAFile == null) && (AlgPrep.projectType(projectId, fields[2]/*fields[2] is owner_repo*/) == ProjectType.FASE_3__NO_PUBLIC_BUGS)){
									MyUtils.println("Warning: FASE3 project <" + projectId + ": " + fields[2] + "> with empty startingDateForAFile for file " + files[j] + " is detected.", indentationLevel+3);
									warnings++;
								}
								else
									if (startingDateForAFile.compareTo(projectStartingDate) < 0)
										projectStartingDate = startingDateForAFile;
							}
							lineSuffixForProjects = lineSuffixForProjects + TAB + projectStartingDate; //Add the earliest between all startingDates for other files for that project.
						}
						//Writing the cleaned fields and the probable suffix:
						String line = fields[0];
						for (int j=1; j<totalNumberOfFields[i]; j++)
							line = line + TAB + fields[j];
						writer.append(line + lineSuffixForProjects + "\n");
						//Checking the starting dates for date fields in different files (and storing in HashMap, to be used and written when we reach to "projects_complete" file):
						if (dateFieldNumbers[i] != NO_DATE_FIELD_TO_FILTER){//: means that if we are considering a file that have a dateField that we should find the earliest date in it. 
							String projectId = fields[projectIdFieldNumbers[i]];
							String dateField = fields[dateFieldNumbers[i]];
							if (projectsAndStartingDatesOfDifferentFiles.containsKey(projectId)){
								HashMap<String, String> fileNameAndStartingDate_insideAProject = projectsAndStartingDatesOfDifferentFiles.get(projectId);
								if (fileNameAndStartingDate_insideAProject.containsKey(files[i])){
									if (dateField.compareTo(fileNameAndStartingDate_insideAProject.get(files[i])) < 0) //: means that dateField is earlier than the minimum date stored for that file of that project, so replace it:
										fileNameAndStartingDate_insideAProject.put(files[i], dateField);
								}
								else
									fileNameAndStartingDate_insideAProject.put(files[i], dateField);
							}
							else{
								HashMap<String, String> startingDatesForAProject = new HashMap<String, String>();
								startingDatesForAProject.put(files[i], dateField);
								projectsAndStartingDatesOfDifferentFiles.put(projectId, startingDatesForAProject);
							}
						}//Otherwise there is no date field in this file, so do nothing.
					}//Otherwise do not write the line, because it is not in the range.
					counter++;
					if (testOrReal > Constants.THIS_IS_REAL)
						if (counter >= testOrReal)
							break;
					if (counter % showProgressInterval == 0)
						MyUtils.println(Constants.integerFormatter.format(counter), indentationLevel+3);
				}
				writer.flush();
				writer.close();
				br.close();
				MyUtils.println(Constants.integerFormatter.format(counter) + " records cleaned.", indentationLevel+3);
				MyUtils.println("Finished.", indentationLevel+3);
				if (wrapOutputInLines)
					MyUtils.println("-----------------------------------", indentationLevel+2);

				if (errors ==0){//Get rid of old files only if everything went smoothly.
					//2: deleting old file:
					if (wrapOutputInLines)
						MyUtils.println("-----------------------------------", indentationLevel+2);
					localFMR = MyUtils.deleteTemporaryFiles(iOPath, new String[]{files[i] + ".tsv"}, true, indentationLevel+2, step+"-2");
					totalFMR = MyUtils.addFileManipulationResults(totalFMR, localFMR);
					if (wrapOutputInLines)
						MyUtils.println("-----------------------------------", indentationLevel+2);
					//3: Renaming the saved file to original name:
					if (wrapOutputInLines)
						MyUtils.println("-----------------------------------", indentationLevel+2);
					localFMR = MyUtils.renameFile(iOPath, files[i]+"-temp.tsv", iOPath, newNamesForFiles[i]+".tsv", indentationLevel+2, step+"-3");
					totalFMR = MyUtils.addFileManipulationResults(totalFMR, localFMR);
					if (wrapOutputInLines)
						MyUtils.println("-----------------------------------", indentationLevel+2);
				}
				MyUtils.println("Finished.", indentationLevel+2);
				if (wrapOutputInLines)
					MyUtils.println("-----------------------------------", indentationLevel+1);
			}
			
			MyUtils.println("Finished.", indentationLevel+2);
			if (wrapOutputInLines)
				MyUtils.println("-----------------------------------", indentationLevel+1);

			Date d2 = new Date();
			MyUtils.println("Total time: " + Constants.floatFormatter.format((float)(d2.getTime()-d1.getTime())/1000)  + " seconds.", indentationLevel+1);
			MyUtils.println("", indentationLevel+1);

			if (warnings > 0)
				MyUtils.println(warnings + " warnings.", indentationLevel);
			if (errors == 0 && totalFMR.errors == 0)
				MyUtils.println("Finished.", indentationLevel);
			else{
				MyUtils.println("Finished with " + errors+totalFMR.errors + " critical errors handling i/o files.", indentationLevel+1);
				fMR.errors = totalFMR.errors + errors;
			}
		}catch (Exception e){
			e.printStackTrace();
			MyUtils.println("Finished with errors (exceptions).", 1);
			fMR.errors = totalFMR.errors + errors;
		}
		MyUtils.println("-----------------------------------", 0);
	}
	//------------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------------
	//The following method extracts and saves the co-occurrences of SO tags in all SO questions:
	//It gets posts.tsv and outputs the file including "tag1	tag2	co-occurrence"
	public static void extractCoOccurrencesOfTagsInSODataSet(String inputPath, String inputFileName, String outputPath, String outputFileName, 
			int showProgressInterval, int indentationLevel, long testOrReal, String writeMessageStep){
		//Read info of all posts (by field id) with focus on questions: (total posts: 32,209,817  Q: 12,350,818  A (ignored): 19,858,999)
		Date d1 = new Date();
		MyUtils.println("-----------------------------------", indentationLevel);
		MyUtils.println("-----------------------------------", indentationLevel);
		MyUtils.println(writeMessageStep+"-Extracting co-occurrences of SO tags:", indentationLevel);
		MyUtils.println("Started ...", indentationLevel+1);
		TreeMap<String, String[]> posts1ById = TSVManipulations.readUniqueKeyAndItsValueFromTSV(
				inputPath, inputFileName, null, 0, 9, "5", LogicalOperation.IGNORE_THE_SECOND_OPERAND, 1, ConditionType.EQUALS, "1", 
				FieldType.NOT_IMPORTANT, 0, ConditionType.NOTHING, "", FieldType.NOT_IMPORTANT, true, showProgressInterval, indentationLevel+1, testOrReal, writeMessageStep+"-1");
		TreeMap<String, Long> coOccurrences = new TreeMap<String, Long>();

		MyUtils.println("-----------------------------------", indentationLevel+1);
		MyUtils.println(writeMessageStep+"-2- Calculating the co-Occurrences:", indentationLevel+1);
		MyUtils.println("Started ...", indentationLevel+2);
		int i = 0;
		for (Map.Entry<String, String[]> entry: posts1ById.entrySet()){
			String s = entry.getValue()[0];
			//Removing the "[" and "]" from the sides:
			s = s.substring(1, s.length()-1);
			//Separate the tags:
			String[] tags = s.split(Constants.TAGS_SEPARATOR);
			//Count the co-occurrences:
			for (int j=0; j<tags.length; j++)
				for (int k=0; k<j; k++)
					addCoOccurrence(tags[j], tags[k], coOccurrences);
			i++;
			if (i % showProgressInterval == 0)
				MyUtils.println(Constants.integerFormatter.format(i), indentationLevel+2);
		}
		MyUtils.println("Number of posts processed: "+Constants.integerFormatter.format(i), indentationLevel+2);
		MyUtils.println("Finished.", indentationLevel+2);
		MyUtils.println("-----------------------------------", indentationLevel+1);
		
		//Saving:
		String[] titles = {"Tag1", "Tag2", "CoOccurrence"};
		TSVManipulations.saveKeyAndLongValuesAsTSVFile(outputPath, outputFileName, coOccurrences, 3, titles, true, 
				showProgressInterval, indentationLevel+1, testOrReal, writeMessageStep+"-3");

		Date d2 = new Date();
		System.out.println();
		MyUtils.println("Total time (step " + writeMessageStep + "): " + (float)(d2.getTime()-d1.getTime())/1000  + " seconds.", indentationLevel);
		MyUtils.println("Finished.", indentationLevel);
		MyUtils.println("-----------------------------------", indentationLevel);
		MyUtils.println("-----------------------------------", indentationLevel);
	}
	//------------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------------
	public static boolean lengthesAreOkayToBeCombined(String s1, String s2){
		int[] lengths = new int[2];
		lengths[0] = s1.length();
		lengths[1] = s2.length();
//		CheckFeasibility.sortArray(lengths, 2);
		
		if (lengths[0] > 2 && lengths[1] > 2)
			return true;
		else
			return false;
	}
	//------------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------------
	public static boolean lengthesAreOkayToBeCombined(String s1, String s2, String s3){
		int[] lengths = new int[3];
		lengths[0] = s1.length();
		lengths[1] = s2.length();
		lengths[2] = s3.length();
		if (lengths[0]*lengths[1]*lengths[2]<2
				|| lengths[0]+lengths[1]+lengths[2]<6)
			return false;
		else
			return true;
//		sortArray(lengths, 3);
	}
	//------------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------------
	public static boolean lengthesAreOkayToBeCombined(String s1, String s2, String s3, String s4){
		int[] lengths = new int[4];
		lengths[0] = s1.length();
		lengths[1] = s2.length();
		lengths[2] = s3.length();
		lengths[3] = s4.length();
		if (lengths[0]==0 || lengths[3]==0 
				|| lengths[1]*lengths[2]==0
				|| lengths[0]+lengths[1]+lengths[2]+lengths[3]<6)
			return false;
		else
			return true;
//		sortArray(lengths, 3);
	}
	//------------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------------
	public static void addToCorrectedTags(String s, HashMap<String, Integer> correctedTags){
		int temp;
		if (correctedTags.containsKey(s)){
			temp = correctedTags.get(s);
			correctedTags.put(s,  temp+1);
		}
		else
			correctedTags.put(s, 1);
	}//addToCorrectedTags().
	//------------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------------
	public static String correctMisspelledTags(String s, Graph graph, 
			HashMap<String, Integer> correctedTags2, HashMap<String, Integer> correctedTags3, HashMap<String, Integer> correctedTags4){
		String[] keywords = s.split(" ");
		String[] conjunction1 = {"", "-", "."};
		String[] conjunction2 = {"", "-", "."};
		String[] conjunction3 = {"", "-", "."};

		//First, check the 4 word tags:
		int jump = 0;
		for (int i=0; i< keywords.length-3; i=i+1+jump){
			if (lengthesAreOkayToBeCombined(keywords[i], keywords[i+1], keywords[i+2], keywords[i+3])){ 
				for (int j=0; j<3; j++){
					for (int k=0; k<3; k++){
						for (int m=0; m<3; m++){
							String fourKeywordsCombined = (keywords[i] 
									+ conjunction1[j] + keywords[i+1] 
											+ conjunction2[k] + keywords[i+2] 
													+ conjunction3[m] + keywords[i+3]).toLowerCase();
//							if (keywords[i].equals("cloud") && keywords[i+3].equals("init"))
//								System.out.println("Oh");
							if (graph.hasNode(fourKeywordsCombined)){
								addToCorrectedTags("\""+keywords[i]+"\" " 
										+ conjunction1[j] + " \""+keywords[i+1]+"\" " 
												+ conjunction2[k] + " \""+keywords[i+2]+"\" " 
														+ conjunction3[m] + " \""+keywords[i+3]+"\"  ---->  \""+fourKeywordsCombined+"\"", correctedTags4);
								keywords[i] = fourKeywordsCombined;
								keywords[i+1] = " ";
								keywords[i+2] = " ";
								keywords[i+3] = " ";
								jump = 3; //: jump two next 3 keywords (because they are concatenated to this one).
								break; //:means that this combination is a tag, so stop looking for other combinations.
							}
							else 
								jump = 0;
						}
						if (jump ==3)
							break;
					}
					if (jump ==3)
						break;
				}
			}
		}
		
		//Now, check the 3 word tags:
		jump = 0; //TODO: I can delete this line?
		for (int i=0; i< keywords.length-2; i=i+1+jump){
			if (lengthesAreOkayToBeCombined(keywords[i], keywords[i+1], keywords[i+2])){ 
				for (int j=0; j<3; j++){
					for (int k=0; k<3; k++){
						String threeKeywordsCombined = (keywords[i] + conjunction1[j] + keywords[i+1] + conjunction2[k] + keywords[i+2]).toLowerCase();
						if (graph.hasNode(threeKeywordsCombined)){
							addToCorrectedTags("\"" + keywords[i] + "\" " 
									+ conjunction1[j] + " \"" + keywords[i+1] + "\" " 
									+ conjunction2[k] + " \"" + keywords[i+2] + "\"  ---->  \"" 
									+ threeKeywordsCombined + "\"", correctedTags3);
//							if (keywords[j].equals("in") && keywords[i+2].equals("init"))
//								System.out.println("Oh");
							keywords[i] = threeKeywordsCombined;
							keywords[i+1] = " ";
							keywords[i+2] = " ";
							jump = 2; //: jump two next 2 keywords (because they are concatenated to this one).
							break; //:means that this combination is a tag, so stop looking for other combinations.
						}
						else 
							jump = 0;
					}
					if (jump ==2)
						break;
				}
			}
		}

		//Finally, check the 2 word tags:
		jump = 0; //TODO: I can delete this line?
		for (int i=0; i< keywords.length-1; i=i+1+jump){
			if (lengthesAreOkayToBeCombined(keywords[i], keywords[i+1])){ 
				for (int j=0; j<3; j++){
					String twoKeywordsCombined = (keywords[i] + conjunction1[j] + keywords[i+1]).toLowerCase();
					if (graph.hasNode(twoKeywordsCombined)){
						addToCorrectedTags("\"" + keywords[i] + "\" " 
								+ conjunction1[j] + " \"" + keywords[i+1] + "\"  ---->  \"" 
								+ twoKeywordsCombined + "\"", correctedTags2);
						keywords[i] = twoKeywordsCombined;
						keywords[i+1] = " ";
						jump = 1; //: jump two next keyword (because that is concatenated to this one).
						break; //:means that this combination is a tag, so stop looking for other combinations.
					}
					else
						jump = 0;
				}
			}
		}

		//Now, we don't need the extra spaces and will delete remove them:
		String result = "";
		for (int i=0; i<keywords.length; i++)
			if (!keywords[i].equals("") && !keywords[i].equals(" "))
				result = StringManipulations.concatTwoStringsWithSpace(result, keywords[i]);
		return result;
	}
	//------------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------------
	public static int numberOfWords(String s){
		if (s == null || s.equals(""))
				return 0;
		else
			return s.split(" ").length;
	}
	//------------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------------
	public static void writeCorrectedTags(String title, HashMap<String, Integer> correctedTags, String filePathAndName, boolean append, FileManipulationResult fMR, int indentationLevel){
		try{
			FileWriter writer = new FileWriter(filePathAndName, append);
			writer.append(title + "\n");
			for (String correctedTag:correctedTags.keySet())
				writer.append(correctedTag + TAB + correctedTags.get(correctedTag) + "\n");
			writer.append("\n");
			writer.flush();
			writer.close();
		}catch (Exception e){
			e.printStackTrace();
			MyUtils.println("Exception error in writing correctedTags history.", indentationLevel);
			fMR.errors++; 
		}
	}
	//------------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------------
	public static void correctMisspelledTagsInDataSet(String tagsInputPath, String generalInputPath, String outputPath,  
			FileManipulationResult fMR, boolean wrapOutputInLines, int showProgressInterval, int indentationLevel, long testOrReal, String writeMessageStep){
//		In this method, make a new data set, that for each of the textual fields:
//			- adds another field (right after that textual field) for the size of the textual field in its original situation (as it is now, after cleaning of invalid characters, but before applying the merging of the next step [and the cleaning we will do later in other methods to get rid of any non-SO_tag term]).
//			- converts misspelled tags to correct ones: identifies any 2, 3 or 4 words tags with connecting consecutive terms by concatenating them and probably adding "-" or "." between them.
		Date d1 = new Date();
		MyUtils.println("-----------------------------------", indentationLevel);
		MyUtils.println(writeMessageStep+"- Correcting misspelled tags in 7 files:", indentationLevel);
		MyUtils.println("Started ...", indentationLevel+1);

		String[] fileNames = new String[]{"1-bugs", "2-commits", "3-PRs", "4-bugComments", "5-commitComments", "6-PRComments", "7-projects"};
		int[] totalNumberOfFieldsInFiles = new int[]{7, 5, 7, 5, 5, 5, 13};
		String[] textualFields = new String[]{"5$6", "4", "5$6", "4", "4", "4", "2"};
		final String CORRECTED_TAGS_FILE_NAME_PREFIX = "CORRECTED_TAGS";
		FileManipulationResult localFMR = new FileManipulationResult();
		FileManipulationResult totalFMR = new FileManipulationResult();
		MyUtils.createFolderIfDoesNotExist(outputPath, fMR, indentationLevel+1, "Initial 'directory checking / creation'");

		Graph graph = new Graph();
		graph.loadGraph(Constants.DATASET_DIRECTORY_SO_3_TSV_CLEANED, "nodeWeights.tsv", "edgeWeights.tsv", localFMR, 
				wrapOutputInLines, showProgressInterval*1000, indentationLevel+1, Constants.THIS_IS_REAL, MyUtils.concatTwoWriteMessageSteps(writeMessageStep, "1"));
		totalFMR = MyUtils.addFileManipulationResults(totalFMR, localFMR);

//		HashSet<String> tags = TSVManipulations.readUniqueFieldFromTSV(tagsInputPath, "occurrences.tsv", 0, 2, 
//				LogicalOperation.NO_CONDITION, 
//				0, ConditionType.NOTHING, "", FieldType.NOT_IMPORTANT, 
//				0, ConditionType.NOTHING, "", FieldType.NOT_IMPORTANT,
//				true, indentationLevel+1, 100000, Constants.THIS_IS_REAL, MyUtils.concatTwoWriteMessageSteps(writeMessageStep,"1"));

		MyUtils.println("-----------------------------------", indentationLevel+1);
		String mainStep = MyUtils.concatTwoWriteMessageSteps(writeMessageStep, "2");
		MyUtils.println(mainStep+"- Now, checking 7 files and correcting them:", indentationLevel+1);
		MyUtils.println("Started ...", indentationLevel+2);
		int errors = 0;
		try{
			for (int i=0; i<fileNames.length; i++){ //do the same process for each file:
				//1: Reading all assignments (sorted by date):
				String subStep = MyUtils.concatTwoWriteMessageSteps(mainStep, Integer.toString(i + 1));
				if (wrapOutputInLines)
					MyUtils.println("-----------------------------------", indentationLevel+2);
				MyUtils.println(subStep+"- Correcting file \""+fileNames[i]+".tsv\":", indentationLevel+2);
				MyUtils.println("Started ...", indentationLevel+3);

				BufferedReader br;
				br = new BufferedReader(new FileReader(generalInputPath + "\\" + fileNames[i] + ".tsv")); 
				String title = br.readLine();

				String[] sA = textualFields[i].split("\\$"); //StringArray.
				HashSet<Integer> numbersOfTextualFields = new HashSet<Integer>();
				for (int j=0; j<sA.length; j++)
					numbersOfTextualFields.add(Integer.parseInt(sA[j]));

				String[] fields = title.split(TAB);
				String newTitle = "";
				for (int j=0; j<totalNumberOfFieldsInFiles[i]; j++)
					if (numbersOfTextualFields.contains(j))
						newTitle = StringManipulations.concatTwoStringsWithDelimiter(newTitle,  fields[j]+TAB+fields[j]+"_numberOfWords",  TAB);
					else
						newTitle = StringManipulations.concatTwoStringsWithDelimiter(newTitle,  fields[j],  TAB);
				FileWriter writer = new FileWriter(outputPath + "\\" + fileNames[i] + ".tsv");
				writer.append(newTitle + "\n");

				HashMap<String, Integer> correctedTags2 = new HashMap<String, Integer>();
				HashMap<String, Integer> correctedTags3 = new HashMap<String, Integer>();
				HashMap<String, Integer> correctedTags4 = new HashMap<String, Integer>();
				String s, line;
				int k=0;
				while((s = br.readLine()) != null) {
					fields = s.split(TAB);
					line = "";
					if (fields.length == totalNumberOfFieldsInFiles[i])
						for (int j=0; j<fields.length; j++){
							if (numbersOfTextualFields.contains(j)){//: in this case, we need to count the number of words, then correct this field and add it, and finally add the count field afterwards:
								fields[j] = correctMisspelledTags(fields[j], graph, correctedTags2, correctedTags3, correctedTags4);
								line = StringManipulations.concatTwoStringsWithDelimiter(line, fields[j]+TAB+numberOfWords(fields[j]), TAB);
							}
							else
								line = StringManipulations.concatTwoStringsWithDelimiter(line, fields[j],  TAB);
						}
					else{
						errors++;
						break;
					}
					writer.append(line + "\n");
					k++;
					if (k % showProgressInterval == 0){
						MyUtils.println(Constants.integerFormatter.format(k), indentationLevel+3);
//						break;
					}
				}

				br.close();
				writer.flush();    writer.close();
				writeCorrectedTags("4-wrods tags\tNumber", correctedTags4, outputPath + "\\" + CORRECTED_TAGS_FILE_NAME_PREFIX + "_" + fileNames[i] + ".tsv", false, localFMR, indentationLevel+2);
				totalFMR = MyUtils.addFileManipulationResults(totalFMR, localFMR);
				writeCorrectedTags("3-wrods tags\tNumber", correctedTags3, outputPath + "\\" + CORRECTED_TAGS_FILE_NAME_PREFIX + "_" + fileNames[i] + ".tsv", true, localFMR, indentationLevel+2);
				totalFMR = MyUtils.addFileManipulationResults(totalFMR, localFMR);
				writeCorrectedTags("2-wrods tags\tNumber", correctedTags2, outputPath + "\\" + CORRECTED_TAGS_FILE_NAME_PREFIX + "_" + fileNames[i] + ".tsv", true, localFMR, indentationLevel+2);
				totalFMR = MyUtils.addFileManipulationResults(totalFMR, localFMR);
				
				if (errors > 0)
					MyUtils.println("Finished with error.", indentationLevel+3);
				else
					MyUtils.println("Finished.", indentationLevel+3);
				if (wrapOutputInLines)
					MyUtils.println("-----------------------------------", indentationLevel+2);

				if (errors > 0)
					break;
			}
			MyUtils.println("Finished.", indentationLevel+2);
			
			//3:c Copying the rest of the files:
			if (wrapOutputInLines)
				MyUtils.println("-----------------------------------", indentationLevel+1);
			MyUtils.println(MyUtils.concatTwoWriteMessageSteps(writeMessageStep, "3")+"- Now, copying the rest of the files", indentationLevel+1);
			MyUtils.println("Started ...", indentationLevel+2);
			MyUtils.copyFile(generalInputPath, "8-bugEvents.tsv", outputPath, "8-bugEvents.tsv", indentationLevel+2, MyUtils.concatTwoWriteMessageSteps(writeMessageStep, "3-1"));
			for (int i=0; i<Constants.NUMBER_OF_ASSIGNEE_TYPES; i++){
				String subStep = Integer.toString(i+2);
				MyUtils.copyFile(generalInputPath, Constants.ASSIGNMENT_FILE_NAMES[i]+".tsv", outputPath, Constants.ASSIGNMENT_FILE_NAMES[i]+".tsv", indentationLevel+2, MyUtils.concatTwoWriteMessageSteps(writeMessageStep, "3-"+subStep));
			}
			for (int i=0; i<Constants.NUMBER_OF_ASSIGNEE_TYPES; i++){
				String subStep = Integer.toString(i+7);
				MyUtils.copyFile(generalInputPath, Constants.COMMUNITY_FILE_NAMES[i]+".tsv", outputPath, Constants.COMMUNITY_FILE_NAMES[i]+".tsv", indentationLevel+2, MyUtils.concatTwoWriteMessageSteps(writeMessageStep, "3-"+subStep));
			}
			MyUtils.println("Finished.", indentationLevel+2);
			if (wrapOutputInLines)
				MyUtils.println("-----------------------------------", indentationLevel+1);
			Date d2 = new Date();
			
			//Summaries:
			MyUtils.println("Total time: " + Constants.floatFormatter.format((float)(d2.getTime()-d1.getTime())/1000)  + " seconds.", indentationLevel+2);
			MyUtils.println("", indentationLevel+1);
			if (errors == 0)
				MyUtils.println("Finished with no errors.", indentationLevel+1);
			else{
				MyUtils.println("Finished with " + errors + " critical errors in reading input files.", indentationLevel+1);
				localFMR.errors = errors;
			}
			if (wrapOutputInLines)
				MyUtils.println("-----------------------------------", indentationLevel+1);
		}catch (Exception e){
			e.printStackTrace();
			MyUtils.println("Finished with errors (exceptions).", indentationLevel+1);
			localFMR.errors++; 
		}
		MyUtils.println("-----------------------------------", indentationLevel+0);
	}//correctMisspelledTagsInDataSet().
	//------------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------------
	public static String removeNon_SOTag_keywords(String s, Graph graph){
		String[] words = s.split(" ");
		String result = "";
		for (int i=0; i<words.length; i++)
			if (graph.hasNode(words[i]))
				result = StringManipulations.concatTwoStringsWithDelimiter(result, words[i], " ");
		if (result.equals(""))
			result = " ";
		return result;
	}
	//------------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------------
	public static void removeNonSOTagsFromTextualElements(String tagsInputPath, String generalInputPath, String outputPath,  
			FileManipulationResult fMR, boolean wrapOutputInLines, int showProgressInterval, int indentationLevel, long testOrReal, String writeMessageStep){
		//This method, removes the "non SO tags" from each of the textual fields of the data set (and copies the rest of the files to the new data set directory):
		Date d1 = new Date();
		MyUtils.println("-----------------------------------", indentationLevel);
		MyUtils.println(writeMessageStep+"- Removing \"non SO tags\" in 7 files:", indentationLevel);
		MyUtils.println("Started ...", indentationLevel+1);

		String[] fileNames = new String[]{"1-bugs", "2-commits", "3-PRs", "4-bugComments", "5-commitComments", "6-PRComments", "7-projects"};
		int[] totalNumberOfFieldsInFiles = new int[]{9, 6, 9, 6, 6, 6, 14};
		String[] textualFields = new String[]{"5$7", "4", "5$7", "4", "4", "4", "2"};
		MyUtils.createFolderIfDoesNotExist(outputPath, fMR, indentationLevel+1, "Initial 'directory checking / creation'");
		
		FileManipulationResult localFMR = new FileManipulationResult();
		FileManipulationResult totalFMR = new FileManipulationResult();
		
		Graph graph = new Graph();
		graph.loadGraph(Constants.DATASET_DIRECTORY_SO_3_TSV_CLEANED, "nodeWeights.tsv", "edgeWeights.tsv", fMR, 
				wrapOutputInLines, showProgressInterval*1000, indentationLevel+1, Constants.THIS_IS_REAL, MyUtils.concatTwoWriteMessageSteps(writeMessageStep, "1"));
		totalFMR = MyUtils.addFileManipulationResults(totalFMR, localFMR);

		MyUtils.println("-----------------------------------", indentationLevel+1);
		MyUtils.println(MyUtils.concatTwoWriteMessageSteps(writeMessageStep, "2")+"- Now, checking 7 files and removing the redundant keywords (non \"SO tags\"):", indentationLevel+1);
		MyUtils.println("Started ...", indentationLevel+2);
		int errors = 0;
		try{
			for (int i=0; i<fileNames.length; i++){ //do the same process for each file:
				//1: Reading all assignments (sorted by date):
				String mainStep = MyUtils.concatTwoWriteMessageSteps(writeMessageStep, "2-"+Integer.toString(i + 1));
				if (wrapOutputInLines)
					MyUtils.println("-----------------------------------", indentationLevel+2);
				MyUtils.println(mainStep+"- Correcting file \""+fileNames[i]+".tsv\":", indentationLevel+2);
				MyUtils.println("Started ...", indentationLevel+3);

				BufferedReader br;
				br = new BufferedReader(new FileReader(generalInputPath + "\\" + fileNames[i] + ".tsv")); 
				String title = br.readLine();

				String[] sA = textualFields[i].split("\\$"); //sA: StringArray.
				HashSet<Integer> numbersOfTextualFields = new HashSet<Integer>();
				for (int j=0; j<sA.length; j++)
					numbersOfTextualFields.add(Integer.parseInt(sA[j]));

				FileWriter writer = new FileWriter(outputPath + "\\" + fileNames[i] + ".tsv");
				writer.append(title + "\n");

				String[] fields;
				String s, line;
				int k=0;
				while((s = br.readLine()) != null) {
					fields = s.split(TAB);
					line = "";
					if (fields.length == totalNumberOfFieldsInFiles[i]){
						for (int j=0; j<fields.length; j++){
							if (numbersOfTextualFields.contains(j))
								fields[j] = removeNon_SOTag_keywords(fields[j], graph);
							line = StringManipulations.concatTwoStringsWithDelimiter(line, fields[j],  TAB);
						}
					}
					else{
						errors++;
						break;
					}
					writer.append(line + "\n");
					k++;
					if (k % showProgressInterval == 0){
						MyUtils.println(Constants.integerFormatter.format(k), indentationLevel+3);
//						break;
					}
				}

				br.close();
				writer.flush();    writer.close();
				
				if (errors > 0)
					MyUtils.println("Finished with \"" + errors + " errors.", indentationLevel+3);
				else
					MyUtils.println("Finished.", indentationLevel+3);
				if (wrapOutputInLines)
					MyUtils.println("-----------------------------------", indentationLevel+2);

				if (errors > 0)
					break;
			}
			MyUtils.println("Finished.", indentationLevel+2);
			
			//3:c Copying the rest of the files:
			if (wrapOutputInLines)
				MyUtils.println("-----------------------------------", indentationLevel+1);
			MyUtils.println(MyUtils.concatTwoWriteMessageSteps(writeMessageStep, "3")+"- Now, copying the rest of the files", indentationLevel+1);
			MyUtils.println("Started ...", indentationLevel+2);
			MyUtils.copyFile(generalInputPath, "8-bugEvents.tsv", outputPath, "8-bugEvents.tsv", indentationLevel+2, MyUtils.concatTwoWriteMessageSteps(writeMessageStep, "3-1"));
			for (int i=0; i<Constants.NUMBER_OF_ASSIGNEE_TYPES; i++){
				String subStep = MyUtils.concatTwoWriteMessageSteps(writeMessageStep, "3-"+Integer.toString(i+2));
				MyUtils.copyFile(generalInputPath, Constants.ASSIGNMENT_FILE_NAMES[i]+".tsv", outputPath, Constants.ASSIGNMENT_FILE_NAMES[i]+".tsv", indentationLevel+2, subStep);
			}
			for (int i=0; i<Constants.NUMBER_OF_ASSIGNEE_TYPES; i++){
				String subStep = MyUtils.concatTwoWriteMessageSteps(writeMessageStep, "3-"+Integer.toString(i+2+Constants.NUMBER_OF_ASSIGNEE_TYPES));
				MyUtils.copyFile(generalInputPath, Constants.COMMUNITY_FILE_NAMES[i]+".tsv", outputPath, Constants.COMMUNITY_FILE_NAMES[i]+".tsv", indentationLevel+2, subStep);
			}
			MyUtils.println("Finished.", indentationLevel+2);
			if (wrapOutputInLines)
				MyUtils.println("-----------------------------------", indentationLevel+1);
			Date d2 = new Date();
			
			//Summaries:
			MyUtils.println("Total time: " + Constants.floatFormatter.format((float)(d2.getTime()-d1.getTime())/1000)  + " seconds.", indentationLevel+2);
			MyUtils.println("", indentationLevel+1);
			if (errors == 0)
				MyUtils.println("Finished with no errors.", indentationLevel+1);
			else{
				MyUtils.println("Finished with " + errors + " critical errors in reading input files.", indentationLevel+1);
				localFMR.errors = errors;
			}
			if (wrapOutputInLines)
				MyUtils.println("-----------------------------------", indentationLevel+1);
		}catch (Exception e){
			e.printStackTrace();
			MyUtils.println("Finished with errors (exceptions).", indentationLevel+1);
			localFMR.errors++; 
		}
		MyUtils.println("-----------------------------------", indentationLevel+0);
	}//removeNonSOTagsFromTextualElements().
	//------------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------------
	private static void addCoOccurrence(String item1, String item2, TreeMap<String, Long> coOccurrences){
		String coOccurredItem;
		if (item1.compareTo(item2) < 0)
			coOccurredItem = item1 + Constants.TAB  + item2;
		else
			coOccurredItem = item2 + Constants.TAB  + item1;
		Long numberOfTimesCoOccurred = coOccurrences.get(coOccurredItem);
		if (numberOfTimesCoOccurred == null)
			coOccurrences.put(coOccurredItem, (long)1);
		else
			coOccurrences.put(coOccurredItem, numberOfTimesCoOccurred+1);
	}
	//------------------------------------------------------------------------------------------------------------------------
	private static void addOccurrence(String tag, TreeMap<String, Long> occurrences){
		Long numberOfTimesOccurred = occurrences.get(tag);
		if (numberOfTimesOccurred == null)
			occurrences.put(tag, (long)1);
		else
			occurrences.put(tag, numberOfTimesOccurred+1);
	}
	//------------------------------------------------------------------------------------------------------------------------
	public static void extractOccurrencesOfTagsInSODataSet(String inputPath, String inputFileName, String outputPath, String outputFileName, 
			int showProgressInterval, int indentationLevel, long testOrReal, String writeMessageStep){
		//Read info of all posts (by field id) with focus on questions: (total posts: 32,209,817  Q: 12,350,818  A (ignored): 19,858,999)
		Date d1 = new Date();
		MyUtils.println("-----------------------------------", indentationLevel);
		MyUtils.println("-----------------------------------", indentationLevel);
		MyUtils.println(writeMessageStep+"-Extracting occurrences of SO tags:", indentationLevel);
		MyUtils.println("Started ...", indentationLevel+1);
		TreeMap<String, String[]> posts1ById = TSVManipulations.readUniqueKeyAndItsValueFromTSV(
				inputPath, inputFileName, null, 0, 9, "5", LogicalOperation.IGNORE_THE_SECOND_OPERAND, 1, ConditionType.EQUALS, "1", 
				FieldType.NOT_IMPORTANT, 0, ConditionType.NOTHING, "", FieldType.NOT_IMPORTANT, true, showProgressInterval, indentationLevel+1, testOrReal, writeMessageStep+"-1");
		TreeMap<String, Long> occurrences = new TreeMap<String, Long>();

		MyUtils.println("-----------------------------------", indentationLevel+1);
		MyUtils.println(writeMessageStep+"-2- Calculating the occurrences:", indentationLevel+1);
		MyUtils.println("Started ...", indentationLevel+2);
		int i = 0;
		for (Map.Entry<String, String[]> entry: posts1ById.entrySet()){
			String s = entry.getValue()[0];
			//Removing the "[" and "]" from the sides:
			s = s.substring(1, s.length()-1);
			//Separate the tags:
			String[] tags = s.split(Constants.TAGS_SEPARATOR);
			//Count the co-occurrences:
			for (int j=0; j<tags.length; j++)
				addOccurrence(tags[j], occurrences);
			i++;
			if (i % showProgressInterval == 0)
				MyUtils.println(Constants.integerFormatter.format(i), indentationLevel+2);
		}
		MyUtils.println("Number of posts processed: "+Constants.integerFormatter.format(i), indentationLevel+2);
		MyUtils.println("Finished.", indentationLevel+2);
		MyUtils.println("-----------------------------------", indentationLevel+1);
		
		//Saving:
		String[] titles = {"Tag", "Occurrence"};
		TSVManipulations.saveKeyAndLongValuesAsTSVFile(outputPath, outputFileName, occurrences, 2, titles, true, 
				showProgressInterval, indentationLevel+1, testOrReal, writeMessageStep+"-3");

		Date d2 = new Date();
		MyUtils.println("Total time (step " + writeMessageStep + "): " + (float)(d2.getTime()-d1.getTime())/1000  + " seconds.", indentationLevel);
		MyUtils.println("Finished.", indentationLevel);
		MyUtils.println("-----------------------------------", indentationLevel);
		MyUtils.println("-----------------------------------", indentationLevel);
	}
	//------------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------------
	public static void generateNodeAndEdgeWeights(String inputPath, String occurrencesInputFile, String coOccurrencesInputFile, 
			String outputPath, String nodeWeightsOutputFile, String edgeWeightsOutputFile, 
			double confidenceThresholdForEdges, int supportThresholdForEdges_coOccurrence, //the actual support will be supportThresholdForEdges/TOTAL_NUMBER_OF_SO_QUESTIONS
			int showProgressInterval, int indentationLevel, long testOrReal, String writeMessageStep){
		//This method reads info of all tag occurrences and co-occurrences and builds the edge weights above the threshold as well as node weights. Then saves them into two separate files.
		Date d1 = new Date();
		MyUtils.println("-----------------------------------", indentationLevel);
		MyUtils.println("-----------------------------------", indentationLevel);
		MyUtils.println(writeMessageStep+"-Building the weighted graph and saving as two files (\"nodeWeights.tsv\" and \"edgeWeights.tsv\"):", indentationLevel);
		MyUtils.println("Started ...", indentationLevel);
		
		
		//First step: Node weights:
		MyUtils.println("-----------------------------------", indentationLevel+1);
		MyUtils.println(writeMessageStep+"-1- Node weights:", indentationLevel+1);
		MyUtils.println("Started ...", indentationLevel+1);
		
		//1-1:
		TreeMap<String, String[]> occurrences = TSVManipulations.readUniqueKeyAndItsValueFromTSV(inputPath, occurrencesInputFile, 
				null, 0, 2, "1", LogicalOperation.NO_CONDITION, 
				0, ConditionType.NOTHING, "", FieldType.NOT_IMPORTANT, 
				0, ConditionType.NOTHING, "", FieldType.NOT_IMPORTANT, 
				true, showProgressInterval, indentationLevel+2, testOrReal, writeMessageStep+"-1-1");
		
		//1-2:
		MyUtils.println(writeMessageStep+"-1-2- calculating node weights:", indentationLevel+2);
		MyUtils.println("Started ...", indentationLevel+3);
		TreeMap<String, Double> nodeWeights = new TreeMap<String, Double>();
		for (Map.Entry<String, String[]> entry: occurrences.entrySet()){
			String tag = entry.getKey();
			String[] value = entry.getValue();
			long occurrenceOfThisTag = Long.parseLong(value[0]);
			//calculate node weight and normalize it (by dividing it to log10(TOTAL_NUMBER_OF_SO_QUESTIONS)):
			double nodeWeight = Math.log10(((double)Constants.TOTAL_NUMBER_OF_SO_QUESTIONS)/occurrenceOfThisTag) / Math.log10(Constants.TOTAL_NUMBER_OF_SO_QUESTIONS);
			nodeWeights.put(tag, nodeWeight);
		}
		MyUtils.println("Finished.", indentationLevel+3);
		
		//1-3:
		//Saving nodes' file:
		String[] nodeFileTitles = {"Node", "Weight"};
		TSVManipulations.saveKeyAndDoubleValuesAsTSVFile(outputPath, nodeWeightsOutputFile, nodeWeights, 2, nodeFileTitles, true, 
				showProgressInterval, indentationLevel+2, testOrReal, writeMessageStep+"-1-3");
		MyUtils.println("Finished.", indentationLevel+1);
		MyUtils.println("-----------------------------------", indentationLevel+1);
		//End of step 1:


		//Second step: Edge weights:
		MyUtils.println("-----------------------------------", indentationLevel+1);
		MyUtils.println(writeMessageStep+"-2- Edge weights:", indentationLevel+1);
		MyUtils.println("Started ...", indentationLevel+1);
		
		//2-1:
		FileManipulationResult fMR = new FileManipulationResult();
		ArrayList<String> titlesToReturn_IS_NOT_NEEDED_AND_USED = new ArrayList<String>();
		TreeMap<String, ArrayList<String[]>> coOccurrences = TSVManipulations.readNonUniqueKeyAndItsValueFromTSV(
				inputPath, coOccurrencesInputFile, 
				fMR, null, 0, SortOrder.DEFAULT_FOR_STRING, 3, "1$2", titlesToReturn_IS_NOT_NEEDED_AND_USED, 
				LogicalOperation.NO_CONDITION, 
				0, ConditionType.NOTHING, "", FieldType.NOT_IMPORTANT, 
				0, ConditionType.NOTHING, "", FieldType.NOT_IMPORTANT, 
				true, showProgressInterval, indentationLevel+2, testOrReal, writeMessageStep+"-2-1");
	
		//2-2:
		MyUtils.println(writeMessageStep+"-2-2- calculating edge weights:", indentationLevel+2);
		MyUtils.println("Started ...", indentationLevel+3);
		TreeMap<String, Double> edgeWeights = new TreeMap<String, Double>();
		for (Map.Entry<String, ArrayList<String[]>> entry: coOccurrences.entrySet()){
			String tag1 = entry.getKey();
			ArrayList<String[]> tag2AndCoOccurrences = entry.getValue();
			
			String[] value1 = occurrences.get(tag1);
			long occurrenceOfTag1 = Long.parseLong(value1[0]);
			for (int i=0; i< tag2AndCoOccurrences.size(); i++){
				String[] tag2AndNumber = tag2AndCoOccurrences.get(i);
				String tag2 = tag2AndNumber[0];
				
				String[] value2 = occurrences.get(tag2);
				long occurrenceOfTag2 = Long.parseLong(value2[0]);

				String value = tag2AndNumber[1];
				long coOccurrenceOfTag1AndTag2 = Long.parseLong(value);

				if (coOccurrenceOfTag1AndTag2 >= supportThresholdForEdges_coOccurrence){
					//calculating edgeWeight(tag1-->tag2) based on:     confidence(x-->y) = support({x}U{y})/support({x}):
					double tag1ToTag2Confidence = ((double)coOccurrenceOfTag1AndTag2)/occurrenceOfTag1;
					double tag2ToTag1Confidence = ((double)coOccurrenceOfTag1AndTag2)/occurrenceOfTag2;

					if (tag1ToTag2Confidence >= confidenceThresholdForEdges) 
						edgeWeights.put(tag1+"\t"+tag2, tag1ToTag2Confidence);
					if (tag2ToTag1Confidence >= confidenceThresholdForEdges) 
						edgeWeights.put(tag2+"\t"+tag1, tag2ToTag1Confidence);
				}
			}
		}
		MyUtils.println("Finished.", indentationLevel+3);
		
		//2-3:
		//Saving edges' file:
		String[] edgeFileTitles = {"Tag1\tTag2", "Weight"};
		TSVManipulations.saveKeyAndDoubleValuesAsTSVFile(outputPath, edgeWeightsOutputFile, edgeWeights, 2, edgeFileTitles, true, 
				showProgressInterval, indentationLevel+2, testOrReal, writeMessageStep+"-2-3");
		//End of step 1:
		MyUtils.println("Finished.", indentationLevel+1);
		MyUtils.println("-----------------------------------", indentationLevel+1);
		
		Date d2 = new Date();
		MyUtils.println("Total time (step " + writeMessageStep + "): " + (float)(d2.getTime()-d1.getTime())/1000  + " seconds.", indentationLevel);
		if (fMR.errors > 0)
			MyUtils.println("Finished with " + fMR.errors + " errors.", indentationLevel);
		else
			MyUtils.println("Finished.", indentationLevel);
		MyUtils.println("-----------------------------------", indentationLevel);
		MyUtils.println("-----------------------------------", indentationLevel);
	}
	//------------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------------
	public static void prepareDataFiles(){ 
		//This method was moved from Algorithm.java
		
//		//This was ran successfully (2016/11/14):
//		//First, convert the XML SO data set to TSV:
//		XMLParser.xmlToTSV(Constants.DATASET_DIRECTORY_SO_1_XML_EXTERNAL, "Posts.xml", Constants.DATASET_DIRECTORY_SO_2_TSV, "Posts.tsv",  
//		"Id$PostTypeId$OwnerUserId$ParentId$Score$[]Tags$CreationDate$AnswerCount$Title", 
//		"&lt;", "&gt;", "\\&lt\\;", "\\&gt\\;", 
//		"Title",
//		500000, 0, Constants.THIS_IS_REAL, "1");


		//This was ran successfully (2016/11/15):
		//Now, extract the occurrence of each tag:
		extractOccurrencesOfTagsInSODataSet(Constants.DATASET_DIRECTORY_SO_2_TSV, "Posts.tsv", Constants.DATASET_DIRECTORY_SO_3_TSV_CLEANED, "occurrences.tsv", 
				1000000, 0, Constants.THIS_IS_REAL, "2");
		
		
//		//This was ran successfully (2016/11/15):
//		//This method will be called just once (to generate the output files):
//		extractCoOccurrencesOfTagsInSODataSet(Constants.DATASET_DIRECTORY_SO_2_TSV, "Posts.tsv", Constants.DATASET_DIRECTORY_SO_3_TSV_CLEANED, "coOccurrences.tsv", 
//				1000000, 0, Constants.THIS_IS_REAL, "3");
//		//The above method ran successfully with the default downloaded SO data set (XML). Can be run again for the new data set.
//		//In fact, in our tool, we should have had this output file (Parley was working on that but wasn't finished).

		
		
//		//This was ran successfully (2016/11/15):
//		//Now, extract the occurrence of each tag:
//		generateNodeAndEdgeWeights(Constants.DATASET_DIRECTORY_SO_3_TSV_CLEANED, "occurrences.tsv", "coOccurrences.tsv", 
//				Constants.DATASET_DIRECTORY_SO_3_TSV_CLEANED, "nodeWeights.tsv", "edgeWeights.tsv",
//				0.01, 5,
//				1000000, 0, Constants.THIS_IS_REAL, "4");
		
//		//The above methods will be called just once (to generate the output files):

		
		//These methods will be called just once (to generate the output files):
		//Not ran yet! I am calling this method in prepareCompleteFinalData(). We won't need this (following line) anymore:
//		cleanDataSetByConvertingConsecutiveKeywordsToOneTag(inputPath, outputPath, 1000, Constants.THIS_IS_A_TEST);
		
		//extract ground truth (real bug fixes) ...
		//make all files shorter.
		//convert 'a b' to a-b' (if the first one isn't an SO tag and the second one is).
		//clean tsv files (delete unused words)... 
	}//prepareDataFiles().
	//------------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------------
	public static void sort8FilesBasedOnDate(String inputPath, String outputPath, //outputPath should be the "cleaned" directory 
			FileManipulationResult fMR, boolean wrapOutputInLines, int showProgressInterval, int indentationLevel, long testOrReal, String writeMessageStep){
		//This method sorts the 8 different files based on their date, and writes the results as new files in output directory.
			//Also ignores some unwanted fields.
		Date d1 = new Date();
		MyUtils.println("-----------------------------------", 0);
		MyUtils.println("Sorting 8 files based on date:", indentationLevel);
		MyUtils.println("Started ...", indentationLevel+1);
		MyUtils.createFolderIfDoesNotExist(outputPath, fMR, indentationLevel+1, "Initial 'directory checking / creation'");
		
		String[] fileNames = new String[]{"bugs_complete", "commits", "PRs_complete", "bugComments_complete", "commitComments_complete", "PRComments_complete", "projects_complete_withStartDates", "bugEvents"};
		String[] newFileNames = new String[]{"1-bugs", "2-commits", "3-PRs", "4-bugComments", "5-commitComments", "6-PRComments", "7-projects", "8-bugEvents"};
		int[] dateFieldNumbers = new int[]{6, 3, 6, 2, 2, 2, 13, 3};
		int[] numberOfFields = new int[]{11, 6, 11, 8, 8, 8, 14, 11};
		String[] neededFields = new String[]{"1$2$5$6$7$9$10", "0$1$2$3$5", "1$2$5$6$7$9$10", "1$6$2$3$7", "1$2$3$5$7", "1$6$2$3$7", "0$2$3$4$5$6$7$8$9$10$11$12$13", "0$1$2$3$4$5$6$8$9$10"};
		FileManipulationResult localFMR = new FileManipulationResult();
		FileManipulationResult totalFMR = new FileManipulationResult();
		try{
			for (int i=0; i<fileNames.length; i++){ //do the same process for each file:
				//1: Reading all assignments (sorted by date):
				String mainStep, subStep;
				mainStep = Integer.toString(i + 1);
				if (wrapOutputInLines)
					MyUtils.println("-----------------------------------", indentationLevel+1);
				MyUtils.println(MyUtils.concatTwoWriteMessageSteps(writeMessageStep, mainStep)+"- Sorting file \""+fileNames[i]+".tsv\":", indentationLevel+1);
				MyUtils.println("Started ...", indentationLevel+2);
				subStep = MyUtils.concatTwoWriteMessageSteps(writeMessageStep, mainStep + "-1");
				ArrayList<String> titlesOfNeededFields = new ArrayList<String>();
				TreeMap<String, ArrayList<String[]>> fileContents = new TreeMap<String, ArrayList<String[]>>();
				fileContents = TSVManipulations.readNonUniqueKeyAndItsValueFromTSV(
						inputPath, fileNames[i]+".tsv", localFMR, null, 
						dateFieldNumbers[i], 
						SortOrder.DEFAULT_FOR_STRING, numberOfFields[i], neededFields[i], titlesOfNeededFields, 
						LogicalOperation.NO_CONDITION, 
						0, ConditionType.NOTHING, "", FieldType.NOT_IMPORTANT, 
						0, ConditionType.NOTHING, "", FieldType.NOT_IMPORTANT, 
						wrapOutputInLines, showProgressInterval, indentationLevel+2, testOrReal, subStep);
				totalFMR = MyUtils.addFileManipulationResults(totalFMR, localFMR);
			
				if (totalFMR.errors > 0)
					break;
				
				String titles = titlesOfNeededFields.get(0);
				for (int j=1; j<titlesOfNeededFields.size(); j++)
					titles = titles + TAB + titlesOfNeededFields.get(j);
					
				//2: Saving the contents as new files:
				subStep = MyUtils.concatTwoWriteMessageSteps(writeMessageStep, mainStep + "-2");
				localFMR = TSVManipulations.saveTreeMapToTSVFile(
						outputPath, newFileNames[i]+".tsv", fileContents, titles, false, 2, 
						wrapOutputInLines, showProgressInterval, indentationLevel+2, testOrReal, subStep);
				totalFMR = MyUtils.addFileManipulationResults(totalFMR, localFMR);
				
				MyUtils.println("Finished.", indentationLevel+2);
				if (wrapOutputInLines)
					MyUtils.println("-----------------------------------", indentationLevel+1);
		
				if (totalFMR.errors > 0)
					break;
			}
			Date d2 = new Date();
			MyUtils.println("Total time: " + Constants.floatFormatter.format((float)(d2.getTime()-d1.getTime())/1000)  + " seconds.", indentationLevel+2);
			MyUtils.println("", indentationLevel+1);
			if (totalFMR.errors == 0)
				MyUtils.println("Finished.", indentationLevel+1);
			else{
				MyUtils.println("Finished with " + totalFMR.errors + " critical errors in reading input files.", indentationLevel+1);
				fMR.errors = totalFMR.errors;
			}
		}catch (Exception e){
			e.printStackTrace();
			MyUtils.println("Finished with errors (exceptions).", 1);
			fMR.errors = totalFMR.errors;
		}
		MyUtils.println("-----------------------------------", 0);
	}
	//------------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------------
	public static void filterAssignedBugsOfDifferentAssignmentTypes(String iOPath, //output files are 1-bugs-ASSIGNED_TYPE_i.tsv (i=1 to 5). 
			String bugsFileName, 
			boolean wrapOutputInLines, int showProgressInterval, int indentationLevel, long testOrReal, String writeMessageStep){
		//This method creates 5 different files for bugs, each one including bugs of a specific assignment type.
		Date d1 = new Date();
		MyUtils.println("-----------------------------------", indentationLevel);
		MyUtils.println("Filtering assigned bugs of different types and saving in 5 separate files:", indentationLevel);
		MyUtils.println("Started ...", indentationLevel+1);

		try{
			//1: Reading all 5 types of assignments in memory:
			if (wrapOutputInLines)
				MyUtils.println("-----------------------------------", indentationLevel+1);
			MyUtils.println(writeMessageStep+"1- Reading all assignments to memory:", indentationLevel+1);
			MyUtils.println("Started ...", indentationLevel+2);
			FileManipulationResult fMR = new FileManipulationResult();
			FileManipulationResult totalFMR = new FileManipulationResult();
			
			@SuppressWarnings("unchecked")
			TreeMap<String, ArrayList<String[]>>[] assignments = (TreeMap<String, ArrayList<String[]>>[]) new TreeMap[5];
			for (int i=0; i<Constants.NUMBER_OF_ASSIGNEE_TYPES; i++){
				assignments[i] = TSVManipulations.readNonUniqueCombinedKeyAndItsValueFromTSV(iOPath, Constants.ASSIGNMENT_FILE_NAMES[i]+".tsv", fMR, null, 
						"0$1", 
						SortOrder.DEFAULT_FOR_STRING, 7, "4", //: in fact, we do not need any of the other fields. We just need the combined fields. But here I just put "4" for program to read something. "2$3$4$5$6", 
						LogicalOperation.NO_CONDITION, 
						0, ConditionType.NOTHING, "", FieldType.NOT_IMPORTANT, 
						0, ConditionType.NOTHING, "", FieldType.NOT_IMPORTANT, 
						wrapOutputInLines, showProgressInterval, indentationLevel+2, testOrReal, writeMessageStep+"1-"+(i+1));
				totalFMR = MyUtils.addFileManipulationResults(totalFMR, fMR);
			}
			MyUtils.println("Finished.", indentationLevel+2);
			if (wrapOutputInLines)
				MyUtils.println("-----------------------------------", indentationLevel+1);
			
			//2: Reading all bugs and writing them, if they are in either of assignments files:
			if (wrapOutputInLines)
				MyUtils.println("-----------------------------------", indentationLevel+1);
			MyUtils.println(writeMessageStep+"2-Reading all bugs and writing them in appropriate file if they are assigned:", indentationLevel+1);
			MyUtils.println("Started ...", indentationLevel+2);
			BufferedReader br;
			br = new BufferedReader(new FileReader(iOPath + "\\" + bugsFileName + ".tsv")); 
			String title = br.readLine();
			FileWriter[] writer = new FileWriter[5];
			int[] numberOfBugs = {0, 0, 0, 0, 0};
			for (int j=0; j<Constants.NUMBER_OF_ASSIGNEE_TYPES; j++){
				writer[j]= new FileWriter(iOPath+"\\" + bugsFileName + "-" + Constants.ASSIGNED_BUGS_TYPES__SHORT_DESCRIPTIONS[j] + ".tsv");
				writer[j].append(title + "\n");
				numberOfBugs[j] = 0;
			}
			String s;
			String[] fields;
			int i = 0;
			while ((s=br.readLine())!=null){
				fields = s.split(TAB);
				String projectId_bugNumber = fields[0] + TAB + fields[1];
				for (int k=0; k<Constants.NUMBER_OF_ASSIGNEE_TYPES; k++)
					if (assignments[k].containsKey(projectId_bugNumber)){//: means that this bug is assigned, so let's write it in writer[k] file:
						writer[k].append(s + "\n");
						numberOfBugs[k]++;
					}
				i++;
				if (i % showProgressInterval == 0)
					MyUtils.println(Constants.integerFormatter.format(i), indentationLevel+2);
				if (testOrReal > Constants.THIS_IS_REAL)
					if (i >= testOrReal)
						break;
			}
			br.close();
			for (int j=0; j<Constants.NUMBER_OF_ASSIGNEE_TYPES; j++){
				writer[j].flush();
				writer[j].close();
			}
			MyUtils.println("Finished.", indentationLevel+2);
			if (wrapOutputInLines)
				MyUtils.println("-----------------------------------", indentationLevel+1);
			
			Date d2 = new Date();
			MyUtils.println("Total time: " + Constants.floatFormatter.format((float)(d2.getTime()-d1.getTime())/1000)  + " seconds.", indentationLevel+2);
			MyUtils.println("", indentationLevel+1);
			if (totalFMR.errors == 0)
				MyUtils.println("Finished.", indentationLevel+1);
			else
				MyUtils.println("Finished with " + totalFMR.errors + " critical errors in reading input files.", indentationLevel+1);
		}catch (Exception e){
			e.printStackTrace();
			MyUtils.println("Finished with errors (exceptions).", 1);
		}
		MyUtils.println("-----------------------------------", indentationLevel);
	}
	//------------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------------
	public static void addProjectTitleToDecription(String iOPath, String projectsInputFileName,
			FileManipulationResult fMR, boolean wrapOutputInLines, int showProgressInterval, int indentationLevel, long testOrReal, String writeMessageStep){
		//This method, adds the project title to the beginning of project description (after replacing "/" to space) (just adds one of them, if owner and repo [before and after "/"] are the same):
		if (wrapOutputInLines)
			MyUtils.println("-----------------------------------", indentationLevel);
		MyUtils.println(writeMessageStep+"- Adding the project title to the beginning of project description:", indentationLevel);
		MyUtils.println("Started ...", indentationLevel+1);

		int errors = 0;
		try{
				//Reading all projects:
			if (wrapOutputInLines)
				MyUtils.println("-----------------------------------", indentationLevel+1);
			MyUtils.println(MyUtils.concatTwoWriteMessageSteps(writeMessageStep, "1")+"- Doing the fix in a temp file:", indentationLevel+1);
			MyUtils.println("Started ...", indentationLevel+2);
			BufferedReader br;
			br = new BufferedReader(new FileReader(iOPath + "\\" + projectsInputFileName + ".tsv")); 
			String title = br.readLine();

			FileWriter writer = new FileWriter(iOPath + "\\" + projectsInputFileName + "-temp.tsv");
			writer.append(title + "\n");

			String[] fields;
			String s, line;
			int k = 0;
			while((s = br.readLine()) != null) {
				fields = s.split(TAB);
				if (fields.length == 13){
					String projectTitle = fields[1];
					String projectTitleWithSlashReplacedToSpace = projectTitle.replaceAll("/",  " ");
					String[] projectTitleWords = projectTitleWithSlashReplacedToSpace.split(" ");
					if (projectTitleWords[0].equals(projectTitleWords[1])) //: means that project owner is equal to repo, like scala/scala:
						projectTitleWithSlashReplacedToSpace = projectTitleWords[0]; 
					fields[2] = projectTitle + "  " + projectTitleWithSlashReplacedToSpace + "   " + fields[2]; //Adding the project title in its original shape (e.g., "angular/angular.js", plus in its replaced shape ("/" --> " "), to the project description.
				}
				else{
					errors++;
					break;
				}
				line = "";
				for (int i=0; i<fields.length; i++)
					line = StringManipulations.concatTwoStringsWithDelimiter(line, fields[i], TAB);
				writer.append(line + "\n");
				k++;
				if (k % showProgressInterval == 0)
					MyUtils.println(Constants.integerFormatter.format(k), indentationLevel+2);
			}
			br.close();
			writer.flush();    writer.close();

			MyUtils.println("Finished.", indentationLevel+2);
			if (wrapOutputInLines)
				MyUtils.println("-----------------------------------", indentationLevel+1);

			//2: Deleting the old "7-projects.tsv" file:
			MyUtils.deleteTemporaryFiles(iOPath, new String[]{projectsInputFileName+".tsv"}, true, indentationLevel+1, MyUtils.concatTwoWriteMessageSteps(writeMessageStep, "2"));
			
			//3: Renaming the temp file to "7-projects.tsv":
			MyUtils.renameFile(iOPath, projectsInputFileName + "-temp.tsv", iOPath, projectsInputFileName + ".tsv", indentationLevel+1, MyUtils.concatTwoWriteMessageSteps(writeMessageStep, "3"));
			
			MyUtils.println("", indentationLevel+1);
			if (errors == 0)
				MyUtils.println("Finished with no errors.", indentationLevel+1);
			else{
				MyUtils.println("Finished with " + errors + " critical errors in reading input files.", indentationLevel+1);
				fMR.errors = errors;
			}
			if (wrapOutputInLines)
				MyUtils.println("-----------------------------------", indentationLevel+1);
		}catch (Exception e){
			e.printStackTrace();
			MyUtils.println("Finished with errors (exceptions).", indentationLevel+1);
			fMR.errors++; 
		}
		MyUtils.println("-----------------------------------", indentationLevel+0);
	}
	//------------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------------
	public static void createFourOtherVersionsOfDataSet_by_filteringProjects(String inputPath_V1, String outputPath_V2, String outputPath_V3, String outputPath_V4, String outputPath_V5, //outputPath should be the "cleaned" directory 
			FileManipulationResult fMR, boolean wrapOutputInLines, int showProgressInterval, int indentationLevel, long testOrReal, String writeMessageStep){
		if (wrapOutputInLines)
			MyUtils.println("-----------------------------------", indentationLevel);
		MyUtils.println("Create V2 and V3 of data set:", indentationLevel);
		MyUtils.println("Started ...", indentationLevel+1);
		
		String[] fileNames = new String[]{"1-bugs", "1-bugs-"+Constants.ASSIGNED_BUGS_TYPES__SHORT_DESCRIPTIONS[0], "1-bugs-"+Constants.ASSIGNED_BUGS_TYPES__SHORT_DESCRIPTIONS[1], "1-bugs-"+Constants.ASSIGNED_BUGS_TYPES__SHORT_DESCRIPTIONS[2], "1-bugs-"+Constants.ASSIGNED_BUGS_TYPES__SHORT_DESCRIPTIONS[3], "1-bugs-"+Constants.ASSIGNED_BUGS_TYPES__SHORT_DESCRIPTIONS[4],  
				"2-commits", "3-PRs", "4-bugComments", "5-commitComments", "6-PRComments", "7-projects", "8-bugEvents", 
				Constants.ASSIGNMENT_FILE_NAMES[0], Constants.ASSIGNMENT_FILE_NAMES[1], Constants.ASSIGNMENT_FILE_NAMES[2], Constants.ASSIGNMENT_FILE_NAMES[3], Constants.ASSIGNMENT_FILE_NAMES[4], 
				Constants.COMMUNITY_FILE_NAMES[0], Constants.COMMUNITY_FILE_NAMES[1], Constants.COMMUNITY_FILE_NAMES[2], Constants.COMMUNITY_FILE_NAMES[3], Constants.COMMUNITY_FILE_NAMES[4]};
		int[] numberOfFields = new int[]{9, 9, 9, 9, 9, 9, 
				6, 9, 6, 6, 6, 14, 10, 
				7, 7, 7, 7, 7,
				2, 2, 2, 2, 2};
		int[] projectIdFieldNumber = new int[]{0, 0, 0, 0, 0, 0, 
				1, 0, 0, 0, 0, 0, 1, 
				0, 0, 0, 0, 0, 
				0, 0, 0, 0, 0};

		//1: Reading projectId's and their names: 
		TreeMap<String, String[]> projects = TSVManipulations.readUniqueKeyAndItsValueFromTSV(inputPath_V1, "7-projects.tsv", null, 
				0, 14, "1", 
				LogicalOperation.NO_CONDITION,
				0, ConditionType.NOTHING, "", FieldType.NOT_IMPORTANT,
				0, ConditionType.NOTHING, "", FieldType.NOT_IMPORTANT,
				wrapOutputInLines, showProgressInterval, indentationLevel+1, Constants.THIS_IS_REAL, MyUtils.concatTwoWriteMessageSteps(writeMessageStep, "1"));

//		MyUtils.createFolderIfDoesNotExist(outputPath_V2, fMR, indentationLevel+1, MyUtils.concatTwoWriteMessageSteps(writeMessageStep, "2"));
//		MyUtils.createFolderIfDoesNotExist(outputPath_V3, fMR, indentationLevel+1, MyUtils.concatTwoWriteMessageSteps(writeMessageStep, "2"));
//		MyUtils.createFolderIfDoesNotExist(outputPath_V4, fMR, indentationLevel+1, MyUtils.concatTwoWriteMessageSteps(writeMessageStep, "2"));
		MyUtils.createFolderIfDoesNotExist(outputPath_V5, fMR, indentationLevel+1, MyUtils.concatTwoWriteMessageSteps(writeMessageStep, "2"));
		
		String mainStep = MyUtils.concatTwoWriteMessageSteps(writeMessageStep, "3");
		if (wrapOutputInLines)
			MyUtils.println("-----------------------------------", indentationLevel+1);
		MyUtils.println(mainStep + "- Reading the records in each file, filtering their projects and writing them in two output files:", indentationLevel+1);
		MyUtils.println("Started ...", indentationLevel+2);

		try{
			for (int j=0; j<fileNames.length; j++){ //do the same process for each file:
				//1: Reading all records:
				String subStep = mainStep + "-" + Integer.toString(j+1);
				if (wrapOutputInLines)
					MyUtils.println("-----------------------------------", indentationLevel+1);
				MyUtils.println(subStep+"- Processing file \""+fileNames[j]+".tsv\":", indentationLevel+2);
				MyUtils.println("Started ...", indentationLevel+3);
				
				BufferedReader br1 = new BufferedReader(new FileReader(inputPath_V1+"\\"+fileNames[j]+".tsv")); 
				FileWriter writer2 = new FileWriter(outputPath_V2+"\\"+fileNames[j]+".tsv");
				FileWriter writer3 = new FileWriter(outputPath_V3+"\\"+fileNames[j]+".tsv");
				FileWriter writer4 = new FileWriter(outputPath_V4+"\\"+fileNames[j]+".tsv");
				FileWriter writer5 = new FileWriter(outputPath_V5+"\\"+fileNames[j]+".tsv");
				//Title line:
				String s = br1.readLine(); 
				writer2.append(s + "\n");
				writer3.append(s + "\n");
				writer4.append(s + "\n");
				writer5.append(s + "\n");
				//The rest of the lines:
				int i = 0;
				int file2Records = 0;
				int file3Records = 0;
				int file4Records = 0; 
				int file5Records = 0;
				while((s = br1.readLine()) != null) {
					String[] fields = s.split(Constants.TAB);
					if (fields.length == numberOfFields[j]){
						String projectId = fields[projectIdFieldNumber[j]];
						if (projects.containsKey(projectId)){//: means that if the projectId is related to FASE_13 or the families of two projects.
							String owner_repo = projects.get(projectId)[0];
							if ((AlgPrep.projectType(projectId, owner_repo) == ProjectType.FASE_13) || (AlgPrep.projectType(projectId, owner_repo) == ProjectType.FASE_13_EXTENSION__PROJECT_FAMILIES_OF_TWO_PROJECTS)){
								writer2.append(s + "\n");
								file2Records++;
								if (AlgPrep.projectType(projectId, owner_repo) == ProjectType.FASE_13){
									writer3.append(s + "\n");
									file3Records++;
									if (owner_repo.equals(Constants.ELASTIC_ELASTICSEARCH__PROJECT_NAME)){
										writer4.append(s + "\n");
										file4Records++;
									}
								}
								if (owner_repo.startsWith("rails/") || owner_repo.startsWith("angular/")){//for project families.
										writer5.append(s + "\n");
										file5Records++;
									}
							}
						}//else: the projectId is one of three projectId's that do not have public issues anymore (2888818: scala/scala, 2317369: mozilla-b2g/gaia and 10391073: edx/edx-platform).
					}
					else{
						fMR.errors++;
						break;
					}
					i++;
					if (i % showProgressInterval == 0)
						MyUtils.println(Constants.integerFormatter.format(i), indentationLevel+3);
					if (testOrReal > Constants.THIS_IS_REAL)
						if (i >= testOrReal)
							break;
				}
				writer2.flush(); writer2.close();
				writer3.flush(); writer3.close();
				writer4.flush(); writer4.close();
				writer5.flush(); writer5.close();
				br1.close();

				MyUtils.println(i + " records read.", indentationLevel+3);
				MyUtils.println(file2Records + " file2 records written", indentationLevel+3);
				MyUtils.println(file3Records + " file3 records written", indentationLevel+3);
				MyUtils.println(file4Records + " file4 records written", indentationLevel+3);
				MyUtils.println(file5Records + " file5 records written", indentationLevel+3);
				if (wrapOutputInLines)
					MyUtils.println("-----------------------------------", indentationLevel+2);
				if (fMR.errors > 0)
					break;
			}
			if (fMR.errors == 0)
				MyUtils.println("Finished.", indentationLevel+1);
			else{
				MyUtils.println("Stoppeded with critical error in reading input files!", indentationLevel+1);
				MyUtils.println("-----------------------------------", indentationLevel);
			}
		}catch (Exception e){
			fMR.errors++;
			e.printStackTrace();
			MyUtils.println("Stopped with errors (exceptions).", indentationLevel+1);
			MyUtils.println("-----------------------------------", indentationLevel);
		}
	}//createFourOtherVersionsOfDataSet_by_filteringProjects().
	//------------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------------
	//The following method extracts and saves any reference to any developer,
	//The output file is "11-referencesToDevelopers_T5.tsv" and includes the following fields;
			//projectId	bugNumber	developer	referenceType(BUG/COMMIT/BUG_COMMENT/COMMIT_COMMENT)	referenceDate	
	public static void extractAndSaveVIPs(String inputPath, String inputPath2_theDataSetContaining__AtSigns__,
			String outputPath, 
			FileManipulationResult fMR, boolean wrapOutputInLines, int showProgressInterval, int indentationLevel, long testOrReal, String writeMessageStep
			){
		if (wrapOutputInLines)
			MyUtils.println("-----------------------------------", indentationLevel);
		MyUtils.println(writeMessageStep+"- Identifying VIPs and saving them in file:", indentationLevel);
		MyUtils.println("Started ...", indentationLevel+1);

		try{
			//1 (Preprocessing): Reading community of developers in each project:
			if (wrapOutputInLines)
				MyUtils.println("-----------------------------------", indentationLevel+1);
			String step = MyUtils.concatTwoWriteMessageSteps(writeMessageStep, "1");
			MyUtils.println(step + "- Preprocessing; reading communities:", indentationLevel+1);
			MyUtils.println("Started ...", indentationLevel+2);
			
			HashMap<String, HashSet<String>> communities = new HashMap<String, HashSet<String>>();
			//communities: projectId --> <developers>
			BufferedReader br = new BufferedReader(new FileReader(inputPath+"\\"+Constants.COMMUNITY_FILE_NAMES[4]+".tsv")); 
			String s = br.readLine(); //: ignore title line.
			while((s = br.readLine()) != null) {
				String[] fields = s.split(Constants.TAB);
				if (fields.length == 2){
					String projectId = fields[0];
					String developer = fields[1];
					HashSet<String> developers;
					if (communities.containsKey(projectId)){
						developers = communities.get(projectId);
						developers.add(developer);
					}
					else{
						developers = new HashSet<String>();
						developers.add(developer);
						communities.put(projectId, developers);
					}
				}
				else{
					fMR.errors++;
					break;
				}
			}//while (br1.
			br.close();
			MyUtils.println("Finished.", indentationLevel+2);
			if (wrapOutputInLines)
				MyUtils.println("-----------------------------------", indentationLevel+1);

			step = MyUtils.concatTwoWriteMessageSteps(writeMessageStep, "2");
			ArrayList<String> titlesToReturn_IS_NOT_NEEDED_AND_USED = new ArrayList<String>();
			FileManipulationResult localFMR = new FileManipulationResult();
			TreeMap<String, ArrayList<String[]>> sha_sAndTheirAssignments;
			sha_sAndTheirAssignments = TSVManipulations.readNonUniqueKeyAndItsValueFromTSV(
					inputPath, Constants.ASSIGNMENT_FILE_NAMES[4]+".tsv", localFMR, null, 
					6, SortOrder.DEFAULT_FOR_STRING, 7, "0$1$2", titlesToReturn_IS_NOT_NEEDED_AND_USED,
					LogicalOperation.IGNORE_THE_SECOND_OPERAND, 
					6, ConditionType.NOT_EQUALS, " ", FieldType.STRING, 
					0, ConditionType.NOTHING, "", FieldType.NOT_IMPORTANT, 
					wrapOutputInLines, showProgressInterval, indentationLevel+1, Constants.THIS_IS_REAL, step);
			fMR = MyUtils.addFileManipulationResults(fMR, localFMR);

			//--------------------------- End of preprocessings ---------------------------------------------------------------------------------------------
			
			//2: Checking the bugs:
			String fileName = "1-bugs-" + Constants.ASSIGNED_BUGS_TYPES__SHORT_DESCRIPTIONS[4] + ".tsv"; 
			if (wrapOutputInLines)
				MyUtils.println("-----------------------------------", indentationLevel+1);
			step = MyUtils.concatTwoWriteMessageSteps(writeMessageStep, "2");
			MyUtils.println(step + "- Bugs:", indentationLevel+1);
			MyUtils.println("Started ...", indentationLevel+2);

			FileWriter writer = new FileWriter(outputPath+"\\11-referencesToDevelopers_T5.tsv");
			writer.append("projectId" + TAB + "bugNumber" + TAB + "developer" + TAB + "referenceType" + TAB + "date" + TAB + "commitSha\n");
			
			br = new BufferedReader(new FileReader(inputPath2_theDataSetContaining__AtSigns__+"\\"+fileName)); 
			s = br.readLine(); 
			while((s = br.readLine()) != null) {
				String[] fields = s.split(Constants.TAB);
				if (fields.length == 9){
					String projectId = fields[0];
					String bugNumber = fields[1];
					String referenceDate = fields[3];
					String text = fields[5] + " " + fields[7]; //title + body
					String[] words = text.split(" ");
					for (int i=0; i<words.length; i++){
						switch (words[i].length()){
						case 0: break;
						case 1: break;
						default:
							if (words[i].startsWith("@")){
								String dev = words[i].substring(1, words[i].length());
								HashSet<String> developers = communities.get(projectId);
								if (developers.contains(dev)){
									writer.append(projectId + TAB + bugNumber + TAB + dev + TAB + Constants.REFERENCE_TYPE1_BUG + TAB + referenceDate + TAB + " " + "\n");
								}
							}
							break;
						}
					}
				}//if (fields.length.
				else{
					fMR.errors++;
					break;
				}
			}//while (s.
			br.close();
			MyUtils.println("Finished.", indentationLevel+2);
			if (wrapOutputInLines)
				MyUtils.println("-----------------------------------", indentationLevel+1);
			//--------------------------------------------------------- End of bugs ---------------------------------------------------------------
			//3: Checking the bugComments:
			fileName = "4-bugComments.tsv"; 
			if (wrapOutputInLines)
				MyUtils.println("-----------------------------------", indentationLevel+1);
			step = MyUtils.concatTwoWriteMessageSteps(writeMessageStep, "3");
			MyUtils.println(step + "- Bug comments:", indentationLevel+1);
			MyUtils.println("Started ...", indentationLevel+2);
			
			br = new BufferedReader(new FileReader(inputPath2_theDataSetContaining__AtSigns__+"\\"+fileName)); 
			s = br.readLine(); 
//			HashMap<String, Integer> hm = new HashMap<String, Integer>();
			while((s = br.readLine()) != null) {
				String[] fields = s.split(Constants.TAB);
				if (fields.length == 6){
					String projectId = fields[0];
					String bugNumber = fields[1];
					String referenceDate = fields[2];
					String text = fields[4];
					String[] words = text.split(" ");
					for (int i=0; i<words.length; i++){
						switch (words[i].length()){
						case 0: break;
						case 1: break;
						default:
							if (words[i].startsWith("@")){
								String dev = words[i].substring(1, words[i].length());
								HashSet<String> developers = communities.get(projectId);
								if (developers.contains(dev)){
									writer.append(projectId + TAB + bugNumber + TAB + dev + TAB + Constants.REFERENCE_TYPE2_BUG_COMMENT + TAB + referenceDate + TAB + " " + "\n");
								}
							}
							break;
						}
					}
				}//if (fields.length.
				else{
					fMR.errors++;
					break;
				}
			}//while (s.
			br.close();
			MyUtils.println("Finished.", indentationLevel+2);
			if (wrapOutputInLines)
				MyUtils.println("-----------------------------------", indentationLevel+1);
			//--------------------------------------------------------- End of bugComments ---------------------------------------------------------------
			//4: Checking the commits:
			fileName = "2-commits.tsv"; 
			if (wrapOutputInLines)
				MyUtils.println("-----------------------------------", indentationLevel+1);
			step = MyUtils.concatTwoWriteMessageSteps(writeMessageStep, "4");
			MyUtils.println(step + "- Commits:", indentationLevel+1);
			MyUtils.println("Started ...", indentationLevel+2);
			
			br = new BufferedReader(new FileReader(inputPath2_theDataSetContaining__AtSigns__+"\\"+fileName)); 
			s = br.readLine(); 
			while((s = br.readLine()) != null) {
				String[] fields = s.split(Constants.TAB);
				if (fields.length == 6){
					String sha = fields[0];
					String projectIdOfCommit = fields[1];
					String referenceDate = fields[3];
					String text = fields[4];
					String[] words = text.split(" ");
					for (int i=0; i<words.length; i++){
						switch (words[i].length()){
						case 0: break;
						case 1: break;
						default:
							if (words[i].startsWith("@")){
								String dev = words[i].substring(1, words[i].length());
								HashSet<String> developers = communities.get(projectIdOfCommit);
								if (developers.contains(dev)){
									if (sha_sAndTheirAssignments.containsKey(sha)){
										ArrayList<String[]> assignmentsOfThisSha = sha_sAndTheirAssignments.get(sha);
										for (String[] anAssignmentRecord: assignmentsOfThisSha){
											String projectId_of_theAssignmentOfthisSha = anAssignmentRecord[0];
											String bugNumber_of_theAssignmentOfthisSha = anAssignmentRecord[1];
											if (projectId_of_theAssignmentOfthisSha.equals(projectIdOfCommit))
												writer.append(projectId_of_theAssignmentOfthisSha + TAB + bugNumber_of_theAssignmentOfthisSha + TAB + dev + TAB + Constants.REFERENCE_TYPE3_COMMIT + TAB + referenceDate + TAB + sha + "\n");
											else
												fMR.errors++;
										}
									}
								}
							}
							break;
						}
					}
				}//if (fields.length.
				else{
					fMR.errors++;
					break;
				}
			}//while (s.
			br.close();
			MyUtils.println("Finished.", indentationLevel+2);
			if (wrapOutputInLines)
				MyUtils.println("-----------------------------------", indentationLevel+1);
			//--------------------------------------------------------- End of commits ---------------------------------------------------------------
			//5: Checking the commitComments:
			fileName = "5-commitComments.tsv"; 
			if (wrapOutputInLines)
				MyUtils.println("-----------------------------------", indentationLevel+1);
			step = MyUtils.concatTwoWriteMessageSteps(writeMessageStep, "5");
			MyUtils.println(step + "- Commit comments:", indentationLevel+1);
			MyUtils.println("Started ...", indentationLevel+2);
			
			br = new BufferedReader(new FileReader(inputPath2_theDataSetContaining__AtSigns__+"\\"+fileName)); 
			s = br.readLine(); 
//			HashMap<String, Integer> hm = new HashMap<String, Integer>();
			while((s = br.readLine()) != null) {
				String[] fields = s.split(Constants.TAB);
				if (fields.length == 6){
					String projectIdOfCommitComment = fields[0];
					String sha = fields[3];
					String referenceDate = fields[1];
					String text = fields[4];
					String[] words = text.split(" ");
					for (int i=0; i<words.length; i++){
						switch (words[i].length()){
						case 0: break;
						case 1:  //check if it is "@", and then merge with the next one
							//This is commented because there are only three may be false positives (The cases are "@ eval" and "@ 0" and "@ 0"; there is @$eval for example which after removing the $ it remains "@ eval" in the text of the bug report, and this is a false positive). cases and it is not worth it.
//							if (words[i].equals("@")){
//								if (i+1 < words.length && !(words[i+1].startsWith("@"))){
//									String dev = words[i+1];
//									HashSet<String> developers = communities.get(projectId);
//									if (developers.contains(dev)){
//										if (hm.containsKey(projectId))
//											hm.put(projectId, hm.get(projectId)+1);
//										else
//											hm.put(projectId, 1);
//										System.out.println("aaaaaaaa\t" + projectId + "\t" + bugNumber + "\t" + dev);
//									}
//								}
//							}
							break;
						default:
							if (words[i].startsWith("@")){
								String dev = words[i].substring(1, words[i].length());
								HashSet<String> developers = communities.get(projectIdOfCommitComment);
								if (developers.contains(dev)){
									if (sha_sAndTheirAssignments.containsKey(sha)){
										ArrayList<String[]> assignmentsOfThisSha = sha_sAndTheirAssignments.get(sha);
										for (String[] anAssignmentRecord: assignmentsOfThisSha){
											String projectId_of_theAssignmentOfthisSha = anAssignmentRecord[0];
											String bugNumber_of_theAssignmentOfthisSha = anAssignmentRecord[1];
											if (projectId_of_theAssignmentOfthisSha.equals(projectIdOfCommitComment))
												writer.append(projectId_of_theAssignmentOfthisSha + TAB + bugNumber_of_theAssignmentOfthisSha + TAB + dev + TAB + Constants.REFERENCE_TYPE4_COMMIT_COMMENT + TAB + referenceDate + TAB + sha + "\n");
											else
												fMR.errors++;
										}
									}
									//For test purposes:
//									if (hm.containsKey(projectId))
//										hm.put(projectId, hm.get(projectId)+1);
//									else
//										hm.put(projectId, 1);
//									System.out.println("aaaaaaaa\t" + projectId + "\t" + bugNumber + "\t" + dev);
								}
							}
							break;
						}
					}
				}//if (fields.length.
				else{
					fMR.errors++;
					break;
				}
			}//while (s.
			br.close();
//			System.out.println(hm);
//			int sum = 0;
//			for (String ss:hm.keySet())
//				sum = sum + hm.get(ss);
//			System.out.println("Sum: " + sum);
			MyUtils.println("Finished.", indentationLevel+2);
			if (wrapOutputInLines)
				MyUtils.println("-----------------------------------", indentationLevel+1);
			//--------------------------------------------------------- End of commits ---------------------------------------------------------------
			
			br.close();
			writer.flush();
			writer.close();
		}catch (Exception e){
			fMR.errors++;
			e.printStackTrace();
			MyUtils.println("Stopped with errors (exceptions).", indentationLevel+1);
			MyUtils.println("-----------------------------------", indentationLevel);
		}

		if (fMR.errors == 0)
			MyUtils.println("Finished.", indentationLevel+1);
		else
			MyUtils.println("Stoppeded with critical error in reading input files!", indentationLevel+1);
		if (wrapOutputInLines)
			MyUtils.println("-----------------------------------", indentationLevel);
	}
	//------------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------------
	public static void extractSharedDevelopersBetween_angularOrRails_and_theirSubProjects(String inputPath, String outputPath, 
			boolean wrapOutputInLines, int showProgressInterval, int indentationLevel, long testOrReal, String writeMessageStep){
		FileManipulationResult totalFMR = new FileManipulationResult(), localFMR = new FileManipulationResult();
		ArrayList<String> titlesToReturn_IS_NOT_NEEDED_AND_USED = new ArrayList<String>();
		//This method checks the number of developers in angular and rails that are also in their sub-projects;
			//The number of developers in no sub-projects, in 1 sub-project, in 2 sub-projects and so on.
		try {
			TreeMap<String, String[]> projects = TSVManipulations.readUniqueKeyAndItsValueFromTSV(inputPath, "7-projects.tsv", null, 
					0, 14, "1$2$3$4$5$6$7$8$9$10$11$12$13", 
					LogicalOperation.NO_CONDITION,
					0, ConditionType.NOTHING, "", FieldType.NOT_IMPORTANT,
					0, ConditionType.NOTHING, "", FieldType.NOT_IMPORTANT,
					wrapOutputInLines, showProgressInterval, indentationLevel+1, Constants.THIS_IS_REAL, MyUtils.concatTwoWriteMessageSteps(writeMessageStep, "1"));
			totalFMR = MyUtils.addFileManipulationResults(totalFMR, localFMR);

			TreeMap<String, ArrayList<String[]>> projectsAndTheirCommunities = TSVManipulations.readNonUniqueKeyAndItsValueFromTSV(
					inputPath, "10-COMMUNITY_T5_ALL_TYPES.tsv", localFMR, null, 
					0, SortOrder.DEFAULT_FOR_STRING, 2, "1", titlesToReturn_IS_NOT_NEEDED_AND_USED,
					LogicalOperation.NO_CONDITION, 
					0, ConditionType.NOTHING, "", FieldType.NOT_IMPORTANT, 
					0, ConditionType.NOTHING, "", FieldType.NOT_IMPORTANT, 
					wrapOutputInLines, showProgressInterval*1000, indentationLevel+1, Constants.THIS_IS_REAL, "2");

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

			MyUtils.println("3- Sub-projects, their # of shared developers with the main projects, and their total # of developers:", indentationLevel+1);
			HashSet<String> railsCommunity = projectsAndTheirCommunities_HM.get("8514");
			HashSet<String> angularCommunity = projectsAndTheirCommunities_HM.get("460078");
			for (Map.Entry<String, HashSet<String>> entry: projectsAndTheirCommunities_HM.entrySet()){
				String pId = entry.getKey();
				if (!pId.equals("8514") && !pId.equals("460078")){
					int numberOfSharedMembersWithAngular = 0;
					int numberOfSharedMembersWithRails = 0;
					HashSet<String> community = entry.getValue();
					for (String login: community){
						if (angularCommunity.contains(login))
							numberOfSharedMembersWithAngular++;
						if (railsCommunity.contains(login))
							numberOfSharedMembersWithRails++;
					}
					Project project = new Project(projects, pId, indentationLevel+4, localFMR);
					if (project.owner_repo.startsWith("rails/"))
						MyUtils.println(project.owner_repo  + "\t:" + numberOfSharedMembersWithRails + "/" + community.size(), indentationLevel+2);
					else
						MyUtils.println(project.owner_repo  + "\t:" + numberOfSharedMembersWithAngular + "/" + community.size(), indentationLevel+2);
				}
			}

			MyUtils.println("4- # of developers of rails (or angular) that are also in any of its sub-projects:", indentationLevel+1);
			MyUtils.println("4-1- Rails:", indentationLevel+2);
			TreeSet<String> railsSharedDevelopers = new TreeSet<String>();
			ArrayList<Integer> numbersOfSharedDevelopersOfRails = new ArrayList<Integer>();
			for (int i=0; i<14; i++)
				numbersOfSharedDevelopersOfRails.add(0);
			for (String login:railsCommunity){
				int i = 0;
				for (Map.Entry<String, HashSet<String>> entry: projectsAndTheirCommunities_HM.entrySet()){
					String pId = entry.getKey();
					Project project = new Project(projects, pId, indentationLevel+4, localFMR);
					if (!pId.equals("8514") && project.owner_repo.startsWith("rails/")){
						HashSet<String> community = entry.getValue();
						if (community.contains(login)){
							i++; 
							railsSharedDevelopers.add(login);
						}
					}
				}
				numbersOfSharedDevelopersOfRails.set(i, numbersOfSharedDevelopersOfRails.get(i)+1);			
			}
			for (int i=0; i<numbersOfSharedDevelopersOfRails.size(); i++)
				MyUtils.println(i + "\t" + numbersOfSharedDevelopersOfRails.get(i), indentationLevel+3);
			//Saving rails shared users:
			MyUtils.println("Rails shared users: " + railsSharedDevelopers.size(), indentationLevel+3);
			FileWriter writer = new FileWriter(outputPath+"\\12-sharedDevelopersWithSubProjects.tsv");
			writer.append("projectId\tlogin\n");
			for (String login:railsSharedDevelopers)
				writer.append("8514" + TAB + login + "\n");
			MyUtils.println("Saved to output file.", indentationLevel+3);

			MyUtils.println("4-2- Angular:", indentationLevel+2);
			TreeSet<String> angularSharedDevelopers = new TreeSet<String>();
			ArrayList<Integer> numbersOfSharedDevelopersOfAngular = new ArrayList<Integer>();
			for (int i=0; i<8; i++)
				numbersOfSharedDevelopersOfAngular.add(0);
			for (String login:angularCommunity){
				int i = 0;
				for (Map.Entry<String, HashSet<String>> entry: projectsAndTheirCommunities_HM.entrySet()){
					String pId = entry.getKey();
					Project project = new Project(projects, pId, indentationLevel+4, localFMR);
					if (!pId.equals("460078") && project.owner_repo.startsWith("angular/")){
						HashSet<String> community = entry.getValue();
						if (community.contains(login)){
							i++; 
							angularSharedDevelopers.add(login);
						}
					}
				}
				numbersOfSharedDevelopersOfAngular.set(i, numbersOfSharedDevelopersOfAngular.get(i)+1);			
			}
			for (int i=0; i<numbersOfSharedDevelopersOfAngular.size(); i++)
				MyUtils.println(i + TAB + numbersOfSharedDevelopersOfAngular.get(i), indentationLevel+3);
			//Saving angular shared users:
			MyUtils.println("Angular shared users: " + angularSharedDevelopers.size(), indentationLevel+3);
			for (String login:angularSharedDevelopers)
				writer.append("460078" + TAB + login + "\n");
			MyUtils.println("Appended to output file.", indentationLevel+3);

			writer.flush();
			writer.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	//------------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------------
	public static String getMD5(String sInput){
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("MD5");
			md.update(sInput.getBytes());
			byte byteData[] = md.digest();
			//convert the byte to hex format method 1
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < byteData.length; i++) 
				sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
			//convert the byte to hex format method 2
			StringBuffer hexString = new StringBuffer();
			for (int i=0;i<byteData.length;i++) {
				String hex=Integer.toHexString(0xff & byteData[i]);
				if(hex.length()==1) hexString.append('0');
				hexString.append(hex);
			}
			//	System.out.println("Digest(in hex format):: " + hexString.toString());
			return(hexString.toString());
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "";
		}
	}
	//------------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------------
	public static void filterSOPostsToThoseDoneBy_angularOrRails_Users(String sharedDevelopersWithSOInputPath, String SOPostsInputPath, 
			String outputPath, 
			FileManipulationResult fMR, 
			boolean wrapOutputInLines, int showProgressInterval, int indentationLevel, long testOrReal, String writeMessageStep){
		//This method removes SO posts that are not done by a member of rails or angular;
			//Note: posts answered by a rails/angular user still remain in the output file.
			//Also removes extra fields (like title). Then saves the results in file "14-SOPostsByAngularOrRailsUsers.tsv"
		FileManipulationResult totalFMR = new FileManipulationResult();
		try {
			FileManipulationResult localFMR = new FileManipulationResult();
			MyUtils.println("Filtering SO posts that are done by rails/angular users:", indentationLevel);
			MyUtils.println("Started ...", indentationLevel+1);

			MyUtils.println("1- Reading id of SO shared developers:", indentationLevel+1);
			MyUtils.println("Started ...", indentationLevel+2);
			HashMap<String, String> SOIdOfAngularOrRailsUsersAndTheirLogin = new HashMap<String, String>();
			BufferedReader br1 = new BufferedReader(new FileReader(sharedDevelopersWithSOInputPath + "\\13-sharedDevelopersWithSO.tsv")); 
			String s = br1.readLine(); //Skip the title line.
			while((s = br1.readLine()) != null) {
				String[] fields = s.split(Constants.TAB);
				if (fields.length != 5){
					localFMR.errors++;
					break;
				}
				String SOId = fields[1];
				String login = fields[2];
				SOIdOfAngularOrRailsUsersAndTheirLogin.put(SOId, login);
			}
			br1.close();
			MyUtils.println("Finished.", indentationLevel+2);
			totalFMR = MyUtils.addFileManipulationResults(totalFMR, localFMR);

			//2: Do the filteration in two steps (since the questions and answers may be unsorted, i.e., an answer may come before its question, just in case:
			//Scan posts file and read and store the "id" of all questions/answers that are posted by angular/rails members, and questions that are answered by such a member:
			MyUtils.println("2- Reading SO posts and keeping id of the needed ones:", indentationLevel+1);
			MyUtils.println("Started ...", indentationLevel+2);
			BufferedReader br2 = new BufferedReader(new FileReader(SOPostsInputPath + "\\posts.tsv")); 
			s = br2.readLine(); //Skip the title line.
			int i = 0;
			HashSet<String> neededPostIds = new HashSet<String>();
			while((s = br2.readLine()) != null) {
				String[] fields = s.split(Constants.TAB);
				if (fields.length != 9){
					localFMR.errors++;
					break;
				}
				String id = fields[0];
				String postTypeId = fields[1];
				String ownerUserId = fields[2];
				String parentId = fields[3];
				if (SOIdOfAngularOrRailsUsersAndTheirLogin.containsKey(ownerUserId) && (postTypeId.equals("1")||postTypeId.equals("2"))){//:If this is a q/a posted by an angular or rails member:
					neededPostIds.add(id);
					if (postTypeId.equals("2")){//: If this is an answer, then also the related question is needed:
						neededPostIds.add(parentId);
					}
				}
				i++;
				if (testOrReal > Constants.THIS_IS_REAL)
					if (i >= testOrReal)
						break;
				if (i % showProgressInterval == 0)
					MyUtils.println(Constants.integerFormatter.format(i), indentationLevel+2);
			}
			br2.close();
			MyUtils.println(Constants.integerFormatter.format(i) + " records were processed.", indentationLevel+2);
			MyUtils.println("Finished.", indentationLevel+2);

			//3: Reading the posts file again, and see if the id of the post is in the neededPostIds, and it is a question, then obtain its tags (if it is an answer, then its question is already in the neededPostIds).
			MyUtils.println("3- Reading SO posts again, obtain the tags of each id (if it is a question) to be used later:", indentationLevel+1);
			MyUtils.println("Started ...", indentationLevel+2);
			BufferedReader br3 = new BufferedReader(new FileReader(SOPostsInputPath + "\\posts.tsv")); 
			s = br3.readLine(); //Skip the title line.
			i = 0;
			HashMap<String, String> idOfNeededQuestionsAndTheirTags = new HashMap<String, String>();
			while((s = br3.readLine()) != null) {
				String[] fields = s.split(Constants.TAB);
				if (fields.length != 9){
					localFMR.errors++;
					break;
				}
				String id = fields[0];
				String postTypeId = fields[1];
				String tags = fields[5];
				if (postTypeId.equals("1") && neededPostIds.contains(id))//: means that if this is a question that is needed.
					idOfNeededQuestionsAndTheirTags.put(id, tags);
				i++;
				if (testOrReal > Constants.THIS_IS_REAL)
					if (i >= testOrReal)
						break;
				if (i % showProgressInterval == 0)
					MyUtils.println(Constants.integerFormatter.format(i), indentationLevel+2);
			}
			br3.close();
			MyUtils.println(Constants.integerFormatter.format(i) + " records were processed.", indentationLevel+2);
			MyUtils.println("Finished.", indentationLevel+2);

			//4: Open the posts file again, and save in file if each post is needed to be saved:
			MyUtils.println("4- Reading SO posts again, and, write the post if it is needed:", indentationLevel+1);
			MyUtils.println("Started ...", indentationLevel+2);
			int q = 0, a = 0;
			i = 0;
			FileWriter writer = new FileWriter(outputPath+"\\14-SOPostsOfRailsOrAngular.tsv");
			writer.append("Id" + TAB + "PostTypeId" + TAB + "Login" + TAB + "ParentId" + TAB + "Score" + TAB + "Tags" + TAB + "CreationDate" + TAB + "AnswerCount" + TAB + "Title" + "\n");
			br3 = new BufferedReader(new FileReader(SOPostsInputPath + "\\posts.tsv"));
			while((s = br3.readLine()) != null) {
				String[] fields = s.split(Constants.TAB);
				if (fields.length != 9){
					localFMR.errors++;
					break;
				}
				String id = fields[0];
				String postTypeId = fields[1];
				String ownerUserId = fields[2];
				String parentId = fields[3];
				String score = fields[4];
				String tags;
				if (postTypeId.equals("2"))	//: if this is an answer, then get the tags of its related question, otherwise its own tags:
					tags = idOfNeededQuestionsAndTheirTags.get(parentId);
				else
					tags = fields[5];
				String creationDate = fields[6];
				String answerCount = fields[7];
				String title = fields[8];
				if (neededPostIds.contains(id)){//:if this is a needed post as we understood before (either a question or an answer posted by an angular or rails user, or a question that its answer is posted by an angular or rails user)
					String poster; 
					if (SOIdOfAngularOrRailsUsersAndTheirLogin.containsKey(ownerUserId))
						poster = SOIdOfAngularOrRailsUsersAndTheirLogin.get(ownerUserId);
					else
						poster = Constants.LOGIN_OF_A_NON_IMPORTANT_USER__WHO_IS_NOT_SHARED_BETWEEN_SO_AND_GH;
					writer.append(id + TAB + postTypeId + TAB + poster + TAB + parentId + TAB + score + TAB + tags + TAB + creationDate + TAB + answerCount + TAB + title + "\n");
					if (postTypeId.equals("1"))
						q++;
					else
						if (postTypeId.equals("2"))
							a++;
				}
				i++;
				if (testOrReal > Constants.THIS_IS_REAL)
					if (i >= testOrReal)
						break;
				if (i % showProgressInterval == 0)
					MyUtils.println(Constants.integerFormatter.format(i), indentationLevel+2);
			}
			br3.close();
			MyUtils.println(Constants.integerFormatter.format(i) + " records were processed.", indentationLevel+2);
			MyUtils.println("# of q written: " + q, indentationLevel+2);
			MyUtils.println("# of a written: " + a, indentationLevel+2);
			
			totalFMR = MyUtils.addFileManipulationResults(totalFMR, localFMR);
			writer.flush();
			writer.close();
			MyUtils.println("Finished.", indentationLevel+2);
			
			MyUtils.println("Finished.", indentationLevel+1);
			} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			totalFMR.errors++;
		}
		fMR.doneSuccessfully = totalFMR.doneSuccessfully;
		fMR.errors = totalFMR.errors;
		fMR.processed = totalFMR.processed;
	}
	//------------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------------
	public static void extractSharedDevelopersBetween_angularOrRails_and_SO(String communityInputPath, String githubProfilesInputPath, String sOInputPath, 
			String outputPath, 
			FileManipulationResult totalFMR, 
			boolean wrapOutputInLines, int showProgressInterval, int indentationLevel, long testOrReal, String writeMessageStep){
		FileManipulationResult fMR = new FileManipulationResult();
		ArrayList<String> titlesToReturn_IS_NOT_NEEDED_AND_USED = new ArrayList<String>();
		//This method checks the number of developers in angular and rails that are also in SO;
			//Then extracts those developers and saves them in file "13-sharedDevelopersWithSO.tsv" (projectId, SOId, login, email, emailHash).
		try {
			MyUtils.println("Extracting shared users between the users of GH projects and SO:", indentationLevel);
			MyUtils.println("Started ...", indentationLevel+1);

			TreeMap<String, ArrayList<String[]>> projectsAndTheirCommunities = TSVManipulations.readNonUniqueKeyAndItsValueFromTSV(
					communityInputPath, "10-COMMUNITY_T5_ALL_TYPES.tsv", fMR, null, 
					0, SortOrder.DEFAULT_FOR_STRING, 2, "1", titlesToReturn_IS_NOT_NEEDED_AND_USED,
					LogicalOperation.NO_CONDITION, 
					0, ConditionType.NOTHING, "", FieldType.NOT_IMPORTANT, 
					0, ConditionType.NOTHING, "", FieldType.NOT_IMPORTANT, 
					wrapOutputInLines, showProgressInterval*1000, indentationLevel+1, Constants.THIS_IS_REAL, "1");

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
			
			MyUtils.println("2- Reading SO emailHashes and their id's:", indentationLevel+1);
			MyUtils.println("Started ...", indentationLevel+2);
			HashMap<String, String> sOEmailHashesAndTheirIds = new HashMap<String, String>();
			BufferedReader br1 = new BufferedReader(new FileReader(sOInputPath + "\\allUsers-withEmailHash.tsv")); 
			String s = br1.readLine(); //Skip the title line.
			while((s = br1.readLine()) != null) {
				String[] fields = s.split(Constants.TAB);
				if (fields.length < 4){
					fMR.errors++;
					break;
				}
				String emailHash = fields[1];
				String id = fields[0];
				if (!(emailHash.equals("") || emailHash.equals(" ")))
					sOEmailHashesAndTheirIds.put(emailHash, id);
			}
			br1.close();
			MyUtils.println("Finished.", indentationLevel+2);
			totalFMR = MyUtils.addFileManipulationResults(totalFMR, fMR);
			
			FileWriter writer = new FileWriter(outputPath+"\\13-sharedDevelopersWithSO.tsv");
			writer.append("projectId\tSOId\tlogin\temail\temailHash\n");
			int j = 0, k = 0;

			MyUtils.println("3- Reading rails developers and their emails, producing the MD5 and then matching with SO users:", indentationLevel+1);
			MyUtils.println("Started ...", indentationLevel+2);
			BufferedReader br2 = new BufferedReader(new FileReader(githubProfilesInputPath + "\\githubprofiles.tsv")); 
			s = br2.readLine(); //Skip the title line.
			while((s = br2.readLine()) != null) {
				String[] fields = s.split(Constants.TAB);
				if (fields.length != 5){
					fMR.errors++;
					break;
				}
				String login = fields[0];
				String email = fields[1];
				if (!(email.equals("") || email.equals(" "))){
					String emailHash = getMD5(email);
					if (sOEmailHashesAndTheirIds.containsKey(emailHash)){
						HashSet<String> railsCommunity = projectsAndTheirCommunities_HM.get("8514");
						if (railsCommunity.contains(login)){
							writer.append("8514" + TAB + sOEmailHashesAndTheirIds.get(emailHash) + TAB + login + TAB + email + TAB + emailHash + "\n");
							j++;
						}
					}
				}
				k++;
			}
			br2.close();
			MyUtils.println("Finished.", indentationLevel+2);

			MyUtils.println("4- Reading angular developers and their emails, producing the MD5 and then matching with SO users:", indentationLevel+1);
			MyUtils.println("Started ...", indentationLevel+2);
			br2 = new BufferedReader(new FileReader(githubProfilesInputPath + "\\githubprofiles.tsv")); 
			s = br2.readLine(); //Skip the title line.
			while((s = br2.readLine()) != null) {
				String[] fields = s.split(Constants.TAB);
				if (fields.length != 5){
					fMR.errors++;
					break;
				}
				String login = fields[0];
				String email = fields[1];
				if (!(email.equals("") || email.equals(" "))){
					String emailHash = getMD5(email);
					if (sOEmailHashesAndTheirIds.containsKey(emailHash)){
						HashSet<String> angularCommunity = projectsAndTheirCommunities_HM.get("460078");
						if (angularCommunity.contains(login)){
							writer.append("460078" + TAB + sOEmailHashesAndTheirIds.get(emailHash) + TAB + login + TAB + email + TAB + emailHash + "\n");
							j++;
						}
					}
				}
			}
			br2.close();

			writer.flush();
			writer.close();
			MyUtils.println("shared records: " + j, indentationLevel+2);
			MyUtils.println("Total records read from 'githubprofiles.tsv': " + k, indentationLevel+2);
			MyUtils.println("Finished.", indentationLevel+2);
			
			MyUtils.println("Finished.", indentationLevel+1);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	//------------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------------
	//The following method gets JSON files and creates the final data 
		//Cleans the special characters (converts to space). Merges consecutive tags. 
		//
	public static void prepareCompleteFinalData(){
		//"occurrences.tsv":
		final String DATASET_DIRECTORY_SO_TSV = Constants.DATASET_OVERAL_DIRECTORY + "\\SO\\20161110\\3-TSV-Cleaned";
		//6 json files; "bugs", "comments", "commits", "githubissues", "githubprofiles" and "projects":
		final String DATASET_DIRECTORY_GH_JSON = Constants.DATASET_OVERAL_DIRECTORY + "\\GH\\AtLeastUpTo20161001\\1-JSON";
		//projects.csv:
		final String DATASET_DIRECTORY_GH_CSV = Constants.DATASET_OVERAL_DIRECTORY + "\\GH\\AtLeastUpTo20161001\\1-CSV";
		//bugEvents.tsv:
		final String DATASET_DIRECTORY_GH_EVENTS__DOWNLOADED_BY__EGIT_GITHUB__PROJECT = Constants.DATASET_OVERAL_DIRECTORY + "\\GH\\AtLeastUpTo20161001\\1-TSV";
		
		//Temporary folders (can be deleted, or kept for seeing/following the process. Also some reports/statistics about corrected previously misspelled tags are saved in the last file:
		final String TEMP_DIRECTORY = Constants.DATASET_OVERAL_DIRECTORY + "\\GH\\AtLeastUpTo20161001\\4-1-TSV-ATempFolder";
		final String DATASET_DIRECTORY_GH_4_TSV_TEMP1 = TEMP_DIRECTORY + "\\Temp1";
		final String DATASET_DIRECTORY_GH_4_TSV_TEMP2 = TEMP_DIRECTORY + "\\Temp2";
		final String DATASET_DIRECTORY_GH_4_TSV_TEMP3 = TEMP_DIRECTORY + "\\Temp3";
		int negligibleErrorsRelatedToBrokenLinesInProjects_CSV = 1;
		FileManipulationResult fMR = new FileManipulationResult();
		FileManipulationResult totalFMR = new FileManipulationResult();
		
		for (int tempCounter=0; tempCounter<1; tempCounter++){
//			//0: Checking initial temp directory (and making if it does not exist):
//			MyUtils.createFolderIfDoesNotExist(TEMP_DIRECTORY, fMR, 0, "Initial 'temp directory checking'");
//
//			//Was ran successfully at 2017-03-27 - 19:33
//			//1: Generate TSV files from JSON files: 
//			JSONToTSV.generateTSVFromJSONs(DATASET_DIRECTORY_GH_JSON, DATASET_DIRECTORY_GH_4_TSV_TEMP1, fMR, 0, "1");
//			totalFMR.add(fMR);
//			if (totalFMR.errors > 0)
//				break;
//
//			//Was ran successfully at 2017-03-27 - 19:34
//			//2: Merge the two files that have information of issues (bugs and PRs), and then generate two separate file for bugs and PRs:
//			fMR = JSONToTSV.merge_bugsTSV_and_githubissuesTSV(DATASET_DIRECTORY_GH_4_TSV_TEMP1, "bugs", "githubissues", DATASET_DIRECTORY_GH_4_TSV_TEMP1, "bugs_complete", "PRs_Complete",
//					true, 100000, 0, Constants.THIS_IS_REAL, "2");	
//			totalFMR.add(fMR);
//			if (totalFMR.errors > 0)
//				break;
//
//			//Was ran successfully at 2017-03-27 - 19:36
//			//3: separate comments into three separate files:
//			separateCommentsFiles(DATASET_DIRECTORY_GH_4_TSV_TEMP1, DATASET_DIRECTORY_GH_4_TSV_TEMP1, fMR, 0, "3");
//			totalFMR.add(fMR);
//			if (totalFMR.errors > 0)
//				break;
//
//			//Was ran successfully at 2017-03-27 - 19:39
//			//4: complete info of projects.tsv and write as projects_complete.tsv:
//			fMR = CSVManipulations.addGHTorrentIdToTheProjects_and_setMainLanguages(DATASET_DIRECTORY_GH_4_TSV_TEMP1, "projects", 
//					DATASET_DIRECTORY_GH_CSV, "projects", 
//					DATASET_DIRECTORY_GH_4_TSV_TEMP1, "projects_complete",
//					true, 2000000, 0, Constants.THIS_IS_REAL, "4");
//			totalFMR.add(fMR);
//			if (totalFMR.errors > negligibleErrorsRelatedToBrokenLinesInProjects_CSV)
//				break;
//
//			//Was ran successfully at 2017-03-27 - 19:41
//			//5-old (not needed to run anymore): Extracting the events directly from Github, into "bugEvents.tsv":
//			//Note 1: It is needed to pull the events only once. And I did that. So there is no need to run extractBugEvents() again. I just copy the extracted info which are in file "bugEvents.tsv" [size: 61,812 KB] to my temporary working folder, and later (in this method) to the main one:
//			//Just for the first time (if you don't have the "bugEvents.tsv": 
//			//In egit-github project:
//			//Open the java source file org.eclipse.egit.github.core\src\extractor:
//			//call extractBugEvents()
//			//Then, 'Action required!': This output file has a tiny problem (because of internal error in GH api or something else); 	
//			//01010000101001100 is a user id in GH and is converted to 1.01E+15 in bugEvents.tsv. So you should convert it manually. 
//			//Note 2: watch out for the directory names. They may have been changed since last run. You may need to change the paths to new paths that are used in this method [prepareCompleteFinalData()].
//			//5-new: Copying the events file:
//			MyUtils.copyFile(DATASET_DIRECTORY_GH_EVENTS__DOWNLOADED_BY__EGIT_GITHUB__PROJECT, "bugEvents.tsv", DATASET_DIRECTORY_GH_4_TSV_TEMP1, "bugEvents.tsv", 0, "5");
//			if (totalFMR.errors > negligibleErrorsRelatedToBrokenLinesInProjects_CSV)
//				break;
//
//			//Was ran successfully at 2017-03-27 - 19:49
//			//6: Cleaning (converting ending dots to space), toLowerCase, date filtering:
//			toLowerCaseAndRemoveEndingDotsAndFilterDatesAndFindEarliestStartingDatePerProject(DATASET_DIRECTORY_GH_4_TSV_TEMP1,  
//					fMR, true, 50000, 0, Constants.THIS_IS_REAL, "6");
//			totalFMR.add(fMR);
//			if (totalFMR.errors > negligibleErrorsRelatedToBrokenLinesInProjects_CSV)
//				break;
//
//			//Was ran successfully at 2017-03-27 - 19:54
//			//7: Sorting all 8 files by date (and ignoring unwanted fields), renaming them by numbered file names, and MOVING THEM TO NEW FOLDER:
//			sort8FilesBasedOnDate(DATASET_DIRECTORY_GH_4_TSV_TEMP1, DATASET_DIRECTORY_GH_4_TSV_TEMP2,
//					fMR, true, 100000, 0, Constants.THIS_IS_REAL, "7");
//			totalFMR.add(fMR);
//			if (totalFMR.errors > negligibleErrorsRelatedToBrokenLinesInProjects_CSV)
//				break;
//
//			//Was ran successfully at 2017-03-28 - 10:13
//			//8: Identifying the 5 types of assignments (by checking contents of bugEvents.tsv and commits.tsv) and save them in 5 different files (e.g., 9-ASSIGNMENTS_TYPE_1_BUG_FIX_CODE_AUTHOR.tsv to 9-ASSIGNMENTS_TYPE_5_UNION_OF_ALL_TYPES_1_2_3_4.tsv):
//			identifyAssignments(DATASET_DIRECTORY_GH_4_TSV_TEMP2, "8-bugEvents.tsv", "2-commits.tsv", 
//					DATASET_DIRECTORY_GH_4_TSV_TEMP2, 
//					fMR, true, 100000, 0, Constants.THIS_IS_REAL, "8");
//			totalFMR.add(fMR);
//
//			//Was ran successfully at 2017-03-28 - 10:32
//			//9: Sorting assignments by date:
//			sortAssignments(DATASET_DIRECTORY_GH_4_TSV_TEMP2,
//					fMR, true, 100000, 0, Constants.THIS_IS_REAL, "9");
//			totalFMR.add(fMR);
//
//			//Was ran successfully at 2017-03-28 - 11:12
//			//10: Removing obvious assignments (assignments of the same bug, same type to the same person within a short period of time):
//			removeObviousAssignments(DATASET_DIRECTORY_GH_4_TSV_TEMP2,
//					fMR, true, 20000, 0, Constants.THIS_IS_REAL, "10");
//			totalFMR.add(fMR);
//
//			//Was ran successfully at 2017-03-28 - 11:20
//			//11: Extract community members by considering commits and assignments files and save them in 5 different files (e.g., 10-COMMUNITY_TYPE_1.tsv to 10-COMMUNITY_TYPE_5.tsv):
//			identifyCommunityMembers(DATASET_DIRECTORY_GH_4_TSV_TEMP2, "2-commits.tsv", 
//					fMR, true, 100000, 0, Constants.THIS_IS_REAL, "11");
//			totalFMR.add(fMR);
//
//			//Was ran successfully at 2017-03-28 - 11:44
//			//12: Adding project title to the beginning of project description:
//			addProjectTitleToDecription(DATASET_DIRECTORY_GH_4_TSV_TEMP2, "7-projects", 
//					fMR, true, 100000, 0, Constants.THIS_IS_REAL, "12");
//			totalFMR.add(fMR);
//
//			//Was ran successfully at 2017-03-28 - 12:23
//			//13A: Create version A (all-text-data): Correct misspelled two+word tags (and copy the rest of the files to the new folder): 
//			correctMisspelledTagsInDataSet(DATASET_DIRECTORY_SO_TSV, DATASET_DIRECTORY_GH_4_TSV_TEMP2, Constants.DATASET_DIRECTORY_GH_4A1_TSV, 
//					fMR, true, 20000, 0, Constants.THIS_IS_REAL, "13A");
//			totalFMR.add(fMR);
//
//			//Was ran successfully at 2017-03-28 - 12:57
//			//13B: Create version B (just-SOTag-data; for section 14 and 15B and ...; we will remove non 'SO tags' in 14. Now it is just a duplicate version): Correct misspelled two+word tags (and copy the rest of the files to the new [temp] folder) (same as step 13A, again, for another version of the data set): 
//			correctMisspelledTagsInDataSet(DATASET_DIRECTORY_SO_TSV, DATASET_DIRECTORY_GH_4_TSV_TEMP2, DATASET_DIRECTORY_GH_4_TSV_TEMP3 /* in the next step, we will produce DATASET_DIRECTORY_GH_4B1_TSV*/, 
//					fMR, true, 10000, 0, Constants.THIS_IS_REAL, "13B");
//			totalFMR.add(fMR);
//
//			//Was ran successfully at 2017-03-28 - 13:01
//			//14: Version B of data: All-SOTag-data; Remove the "non SO tags" from textual fields of the version B of the data set (and copying the rest of the files [from the temp folder] to the destination folder):
//			removeNonSOTagsFromTextualElements(DATASET_DIRECTORY_SO_TSV, DATASET_DIRECTORY_GH_4_TSV_TEMP3, Constants.DATASET_DIRECTORY_GH_4B1_TSV, 
//					fMR, true, 10000, 0, Constants.THIS_IS_REAL, "14");
//
//			//Was ran successfully at 2017-03-28 - 13:08
//			//15A: Create 5 different files for bugs, each one including bugs of a specific assignment type (for version A; all-text-data):
//			filterAssignedBugsOfDifferentAssignmentTypes(Constants.DATASET_DIRECTORY_GH_4A1_TSV, 
//					"1-bugs", //: this is the name of .tsv file. 
//					true, 100000, 0, Constants.THIS_IS_REAL, "15A");
//
//			//Was ran successfully at 2017-03-28 - 13:12
//			//15B: Create 5 different files for bugs, each one including bugs of a specific assignment type (same as step 15A, again, but for version B; just-SOTag-data):
//			filterAssignedBugsOfDifferentAssignmentTypes(Constants.DATASET_DIRECTORY_GH_4B1_TSV, 
//					"1-bugs", //: this is the name of .tsv file. 
//					true, 100000, 0, Constants.THIS_IS_REAL, "15B");
//
//			//16: Create different versions 4A2 to 4A5 of the full-text version of the data set (4A1):
//			//Was ran successfully at 2017-03-28 - 13:19     and another time at 2017-09-12 - 13:12 just for creating the 5th type (commented the sections related to other types, and the un-commented them):
//			//16A: Filter all-text-data by project: Create version DATASET_DIRECTORY_GH_4A2_TSV (13 main projects and project families of two projects), DATASET_DIRECTORY_GH_4A3_TSV (just 13 main projects) and DATASET_DIRECTORY_GH_4A4_ELASTICSEARCH_TSV (just one project: elastic/elasticSearch).
//			createFourOtherVersionsOfDataSet_by_filteringProjects(Constants.DATASET_DIRECTORY_GH_4A1_TSV, 
//					Constants.DATASET_DIRECTORY_GH_4A2_TSV, /* 13 main FASE projects (with issues enabled) and project families of rails/rails and angular/angular.js */
//					Constants.DATASET_DIRECTORY_GH_4A3_TSV, /* Just 13 main FASE projects (with issues enabled) */
//					Constants.DATASET_DIRECTORY_GH_4A4_ELASTICSEARCH_TSV, /* Just one project (elastic/elasticsearch) */
//					Constants.DATASET_DIRECTORY_GH_4A5_RAILS_AND_ANGULAR_AND_THEIR_FAMILIES_TSV, /* Just two projects (rails/rails and angular/angular.js) and their families */
//					fMR, true, 100000, 0, Constants.THIS_IS_REAL, "16A");
//			totalFMR.add(fMR);
//
//			//16: Create different versions 4B2 to 4B5 of the just-SO-tags version of the data set (4B1):
//			//Was ran successfully at 2017-03-28 - 13:24     and another time at 2017-09-12 - 13:12 just for creating the 5th type (commented the sections related to other types, and the un-commented them):
//			//16B: Filter all-SOTag-data by project: Create version DATASET_DIRECTORY_GH_4B2_TSV (13 main projects and project families of two projects), DATASET_DIRECTORY_GH_4B3_TSV (just 13 main projects) and DATASET_DIRECTORY_GH_4B4_ELASTICSEARCH_TSV (just one project: elastic/elasticSearch).
//			createFourOtherVersionsOfDataSet_by_filteringProjects(Constants.DATASET_DIRECTORY_GH_4B1_TSV,
//					Constants.DATASET_DIRECTORY_GH_4B2_TSV, /* 13 main FASE projects (with issues enabled) and project families of rails/rails and angular/angular.js */
//					Constants.DATASET_DIRECTORY_GH_4B3_TSV, /* Just 13 main FASE projects (with issues enabled) */
//					Constants.DATASET_DIRECTORY_GH_4B4_ELASTICSEARCH_TSV, /* Just one project (elastic/elasticsearch) */
//					Constants.DATASET_DIRECTORY_GH_4B5_RAILS_AND_ANGULAR_AND_THEIR_FAMILIES_TSV, /* Just two projects (rails/rails and angular/angular.js) and their families */
//					fMR, true, 100000, 0, Constants.THIS_IS_REAL, "16B");
//			totalFMR.add(fMR);
//			
//			//Was done successfully at 2017-08-.. - ..:..:
//			//17: extract and save referenced users from bugs, commits [that are fixing bugs and mentioning this as "resolves", etc.] or their comments (known as VIPs):
//			//17-A: Version B3:
//			extractAndSaveVIPs(Constants.DATASET_DIRECTORY_GH_4B3_TSV, Constants.DATASET_DIRECTORY_GH_4A3_TSV,
//					Constants.DATASET_DIRECTORY_GH_4B3_TSV, 
//					fMR, true, 10000, 0, Constants.THIS_IS_REAL, "17");
//			totalFMR = MyUtils.addFileManipulationResults(totalFMR, fMR);
//			
//			//Was done successfully at 2017-09-12 - 15:44:
//			//17-B: Version B5 (other versions are not needed at this time, because we won't test on them now): 
//			extractAndSaveVIPs(Constants.DATASET_DIRECTORY_GH_4B5_RAILS_AND_ANGULAR_AND_THEIR_FAMILIES_TSV, Constants.DATASET_DIRECTORY_GH_4A5_RAILS_AND_ANGULAR_AND_THEIR_FAMILIES_TSV,
//					Constants.DATASET_DIRECTORY_GH_4B5_RAILS_AND_ANGULAR_AND_THEIR_FAMILIES_TSV, 
//					fMR, true, 10000, 0, Constants.THIS_IS_REAL, "17");
//			totalFMR = MyUtils.addFileManipulationResults(totalFMR, fMR);
			//
//			//18: Extracting shared developers between project (angular or rails) and its subProjects:
//			//Was ran successfully around 2017-09-26;
//			extractSharedDevelopersBetween_angularOrRails_and_theirSubProjects(Constants.DATASET_DIRECTORY_GH_4B5_RAILS_AND_ANGULAR_AND_THEIR_FAMILIES_TSV, Constants.DATASET_DIRECTORY_GH_4B5_RAILS_AND_ANGULAR_AND_THEIR_FAMILIES_TSV, 
//					false, 1000, 0, Constants.THIS_IS_REAL, "");
//			
//			//19: Extracting shared developers between project (angular or rails) and SO:
//			//Was ran successfully around 2017-10-02;
//			extractSharedDevelopersBetween_angularOrRails_and_SO(Constants.DATASET_DIRECTORY_GH_4B5_RAILS_AND_ANGULAR_AND_THEIR_FAMILIES_TSV, DATASET_DIRECTORY_GH_4_TSV_TEMP1, DATASET_DIRECTORY_SO_TSV, 
//					Constants.DATASET_DIRECTORY_GH_4B5_RAILS_AND_ANGULAR_AND_THEIR_FAMILIES_TSV,
//					fMR, 
//					false, 1000, 0, Constants.THIS_IS_REAL, "");
//			totalFMR = MyUtils.addFileManipulationResults(totalFMR, fMR);
//			
//			//20: Filter SO posts that are done by rails or angular users:
//			//Was ran successfully around 2017-10-02;
			filterSOPostsToThoseDoneBy_angularOrRails_Users(Constants.DATASET_DIRECTORY_GH_4B5_RAILS_AND_ANGULAR_AND_THEIR_FAMILIES_TSV, DATASET_DIRECTORY_SO_TSV, 
					Constants.DATASET_DIRECTORY_GH_4B5_RAILS_AND_ANGULAR_AND_THEIR_FAMILIES_TSV,
					fMR, 
					false, 5000000, 0, Constants.THIS_IS_REAL, "");
			totalFMR = MyUtils.addFileManipulationResults(totalFMR, fMR);
			}
		if (totalFMR.errors > 0)
			MyUtils.println("Done with " + totalFMR.errors + " errors.", 0);
		else
			MyUtils.println("Done successfully with no errors.", 0);
	}
	//------------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------------
	public static void main(String[] args) { 
//		prepareDataFiles();

		//Was ran successfully at 2016-12-05:
		//		separateCommentsFiles();
		
		//Was ran successfully at 2017-01-24:
//		toLowerCaseAndRemoveEndingDotsAndFilterDatesAndFindEarliestStartingDatePerProject(Constants.DATASET_DIRECTORY_GH_TSV,  
//				true, 50000, 0, Constants.THIS_IS_REAL, "");

//		//Was ran successfully at 2017-01-24
//		identifyAssignments(Constants.DATASET_DIRECTORY_GH_TSV, "bugEvents.tsv", "commits.tsv", 
//				DATASET_DIRECTORY_GH_TSV, "assignees.tsv",
//				true, 100000, 0, Constants.THIS_IS_REAL, "");
		
//		Was ran successfully at 2017-01-24:
//		sortAssignments(Constants.DATASET_DIRECTORY_GH_TSV,
//		true, 100000, 0, Constants.THIS_IS_REAL, "");

//		Was ran successfully at 2017-01-24:
//		removeObviousAssignments(Constants.DATASET_DIRECTORY_GH_TSV,
//				true, 20000, 0, Constants.THIS_IS_REAL, "");

//		//Was ran successfully at 2017-01-24:
//		identifyCommunityMembers(Constants.DATASET_DIRECTORY_GH_TSV, "commits.tsv", 
//				true, 100000, 0, Constants.THIS_IS_REAL, "");

		
//		//Was ran successfully at 2017-01-26:
//		sort8FilesBasedOnDate(Constants.DATASET_DIRECTORY_GH_2_TSV, Constants.DATASET_DIRECTORY_GH_3_TSV,
//		true, 100000, 0, Constants.THIS_IS_REAL, "");


//		filterAssignedBugsOfDifferentAssignmentTypes(Constants.DATASET_DIRECTORY_GH_3_TSV, 
//				"1-bugs", //: this is the name of .tsv file. 
//				true, 100000, 0, Constants.THIS_IS_REAL, "");

		
		
//		String s = ".a%6%3 . s28( *f.s &^0876(*&^(87. sd a.";

		
//		String s = "java html. .net node.js php.";
//		System.out.println(s);
//		s = s.replaceAll("(\\w)\\.(?!\\S)", "$1 ");
//		System.out.println(s);
////		s = s.replaceAll(     "(?=\\S+)\\.(?!\\S)", " ");
//		
		
		
		
		
		prepareCompleteFinalData();
		
		
//		if (StringManipulations.specificFieldsOfTwoStringArraysAreEqual(s1, s2, "0$1$2$3"))
//		System.out.println("yes");    else		System.out.println("no");
		
//		String[] fieldNumbersToBeCleaned = "4".split("\\$");    System.out.println(fieldNumbersToBeCleaned[0]);    System.out.println(fieldNumbersToBeCleaned[1]);    String s = ". abc. d. ef . ghi ... jk.";
//		s = s.replaceAll("(?=\\S+)\\.(?!\\S)", " ");
//		System.out.println(s);
	}
	//------------------------------------------------------------------------------------------------------------------------
}
//
//
//

